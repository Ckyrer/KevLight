package ru.kvdl.kevlight;

import ru.kvdl.kevlight.annotations.KLCmdRequestHandler;
import ru.kvdl.kevlight.annotations.KLObserver;
import ru.kvdl.kevlight.annotations.KLRequestHandler;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server {
    // Set by user
    final private Object app;
    final private int port;
    private String commandPrefix = "CMD";
    private Method observer;

    public Server(Object app, int port) {
        this.app = app;
        this.port = port;
    }


    // --------------
    // PUBLIC METHODS
    // --------------

    // Set prefix for command requests
    public void setCommandPrefix(String prefix) {
        this.commandPrefix = prefix;
    }

    // Start server
    public void start() {
        // Поиск методов с аннотациями
        for ( Method handler : app.getClass().getDeclaredMethods() ) {
            Class<?>[] types = handler.getParameterTypes();
            // Прочие запросы
            if (handler.isAnnotationPresent(KLRequestHandler.class)) {
                this.commonResponses.put(handler.getAnnotation(KLRequestHandler.class).request(), new ResponseAction(this.app, handler, false));
            // Команды
            } else if (handler.isAnnotationPresent(KLCmdRequestHandler.class)) {
                KLCmdRequestHandler ann = handler.getAnnotation(KLCmdRequestHandler.class);

                // Проверка на верные параметры
                if (
                    types.length==3 &&
                    types[0] == String[].class && types[1] == String.class && types[2] == Responser.class
                ) {
                    this.commandResponses.put(ann.command(), new ResponseAction(this.app, handler, true));
                } else {
                    throw new RuntimeException("Неверные аргументы метода для использования аннотации KLCmdRequestHandler: " + handler.getName());
                }
            // Наблюдатель
            } else if (handler.isAnnotationPresent(KLObserver.class)) {
                // Проверка на верные параметры и возвращаемое значение
                if (handler.getReturnType() != boolean.class) {
                    throw new RuntimeException("Неверный тип возвращаемого значения метода для использования аннотации KLObserver: " + handler.getName());
                }

                if (
                    types.length == 4 &&
                    types[0] == String.class && types[1] == String[].class && types[2] == String.class && types[3] == Responser.class
                ) {
                    this.observer = handler;
                } else {
                    throw new RuntimeException("Неверные аргументы метода для использования аннотации KLObserver: " + handler.getName());
                }
            }
        }

        // Запуск главного цикла
        this.mainLoop();

    }

    // Set by server
    final private Map<String, ResponseAction> commonResponses = new HashMap<String, ResponseAction>();
    final private Map<String, ResponseAction> commandResponses = new HashMap<String, ResponseAction>();
    private OutputStream output;
    private InputStream input;
    private Responser responser;

    // Главный цикл
    private void mainLoop() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                while (true) {
                    // ожидание подключения
                    Socket socket = serverSocket.accept();

                    input = socket.getInputStream();
                    output = socket.getOutputStream();

                    // чтение всего, что было отправлено клиентом
                    final String[] headers = getRequestHeaders(input);
                    final byte[] content = getRequestContent(input);

                    final String fstLine = headers[0];
                    final String request = fstLine.substring(fstLine.indexOf(' ')+2, fstLine.lastIndexOf(' '));
                    final String type = fstLine.substring(0, fstLine.indexOf(' '));

                    final String ip = socket.getInetAddress().toString().substring(1);

                    responser =  new Responser(output, this.commonResponses.get("404"), request, headers, content, ip);
                    
                    // Отвечаем, если смотртитель одобрил подключение
                    if ( observer ==null || this.getObserverPermission(request, headers, ip, responser) ) {

                        // Если запрос является командой
                        if (request.contains(commandPrefix+"<>")) {
                            this.commandRequestHandler(request, headers, ip, content);
                        } else {
                            this.commonRequestHandler(request, type, headers, ip, content);
                        }
                    }                
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    // ---------------
    // PRIVATE METHODS
    // ---------------

    // Запустить наблюдателя
    private boolean getObserverPermission(String request, String[] args, String ip, Responser resp) {
        try {
            return (boolean) this.observer.invoke(this.app, request, args, ip, resp);
        } catch (ReflectiveOperationException e) {e.printStackTrace();}
        return false;
    }

    // Получить контент запроса(если есть)
    private byte[] getRequestContent(InputStream input) {
        try {
            if (input.available()==0) {return null;}
            return input.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[] {};
        }
    }

    // Get headers from BufferedReader
    private String[] getRequestHeaders(InputStream request) {
        ArrayList<String> l = new ArrayList<String>();
        try {
            final StringBuilder sb = new StringBuilder();
            int b;
            while ( (b =  request.read())!=-1 ) {
                final char c = (char) b;
                if (c=='\n') {
                    if (sb.length()==1) {
                        l.add(URLDecoder.decode(sb.toString(), StandardCharsets.UTF_8));
                        String[] res = new String[l.size()];
                        l.toArray(res);
                        return res;
                    }
                    l.add(URLDecoder.decode(sb.toString(), StandardCharsets.UTF_8));
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Create and start response thread
    private void createResponseThread(ResponseAction action, String req, String[] args, String ip, byte[] content) {
        new Thread(
            new ResponseThread(action, req, args, ip, output, input, content, responser)
        ).start();
    }

    // Command requests handler
    private void commandRequestHandler(String request, String[] headers, String ip, byte[] content) {
        String cmd = request.split(commandPrefix)[0] + request.split("<>")[1];
        if (this.commandResponses.containsKey(cmd)) {
            String[] args = request.substring(request.indexOf("<>", request.indexOf(commandPrefix)+5)+2).split("<>");
            createResponseThread(this.commandResponses.get(cmd), request, args, ip, content);
        } else {
            this.send404();
        }
    }

    // Common requests handler 
    private void commonRequestHandler(String request, String type, String[] headers, String ip, byte[] content) {
        // If response exist
        if (this.commonResponses.containsKey(request) && this.commonResponses.get(request).type.equals(type)) {
            createResponseThread(this.commonResponses.get(request), request, headers, ip, content);
        // Else looking for response, where responseIfStartsWith=true
        } else {
            // Ищем тот ответ, с которого начинается запрос
            for (Map.Entry<String, ResponseAction> el: this.commonResponses.entrySet()) {
                if ( !el.getValue().type.equals(type) && !request.startsWith(el.getKey()) || el.getKey().isEmpty() || !el.getValue().isStart) continue;

                createResponseThread(el.getValue(), request, headers, ip, content);
                return;
            }
            
            // Иначе отправляем 404
            this.send404();
        }
    }

    private void send404() {
        responser.send404Response();
        try {this.input.close();} catch (IOException e) {e.printStackTrace();}
        try {this.output.close();} catch (IOException e) {e.printStackTrace();}
    }
}
