package ru.kvdl.kevlight;

import ru.kvdl.kevlight.annotations.KL404Handler;
import ru.kvdl.kevlight.annotations.KLCmdRequestHandler;
import ru.kvdl.kevlight.annotations.KLObserver;
import ru.kvdl.kevlight.annotations.KLRequestHandler;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {
    // Set by user
    final private Object app;
    final private int port;
    private int REQUEST_TIMEOUT = 10000;
    private String commandPrefix = "CMD";
    private Method observer;
    private ResponseAction on404 = null;

    public Server(Object app, int port) {
        this.app = app;
        this.port = port;
    }


    // --------------
    // PUBLIC METHODS
    // --------------

    // Set server timeout
    public void setTimeout(int timeout) {
        this.REQUEST_TIMEOUT = timeout;
    }

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
                if (handler.isAnnotationPresent(KL404Handler.class)) {
                    this.on404 = new ResponseAction(this.app, handler, false);
                    continue;
                }
                this.request.put(handler.getAnnotation(KLRequestHandler.class).request(), new ResponseAction(this.app, handler, false));
            // Команды
            } else if (handler.isAnnotationPresent(KLCmdRequestHandler.class)) {
                this.cmdResponses.put(handler.getAnnotation(KLCmdRequestHandler.class).command(), new ResponseAction(this.app, handler, true));
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
    final private Map<String, ResponseAction> request = new HashMap<String, ResponseAction>();
    final private Map<String, ResponseAction> cmdResponses = new HashMap<String, ResponseAction>();
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
                    if (headers==null) {
                        responser.sendResponse("Error! Bad connection.", "524 A Timeout Occurred");
                        input.close();
                        output.close();
                        continue;
                    }
                    final byte[] content = getRequestContent(input, headers);

                    final String fstLine = headers[0];
                    final String request = fstLine.substring(fstLine.indexOf(" ")+2, fstLine.lastIndexOf(" "));
                    final String type = fstLine.substring(0, fstLine.indexOf(" "));

                    final String ip = socket.getInetAddress().toString().substring(1);

                    responser =  new Responser(output, this.on404, request, headers.clone(), content, ip);

                    if (content==null) {
                        responser.sendResponse("Error 400", "400 Bad Request");
                        output.close();
                        input.close();
                        continue;
                    }

                    // Отвечаем, если смотртитель одобрил подключение
                    if ( observer ==null || this.getObserverPermission(request, headers, ip, responser) ) {

                        // Если запрос является командой
                        if (request.contains(commandPrefix+"<>")) {
                            this.cmdRequestHandler(request, headers, ip, content);
                        } else {
                            this.requestHandler(request, type, headers, ip, content);
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
    private byte[] getRequestContent(InputStream input, String[] headers) {
        int contentSize;
        try {
            String cs = Arrays.stream(headers)
                    .filter(x -> x.startsWith("Content-Length"))
                    .findFirst()
                    .get();
            contentSize = Integer.parseInt(cs.substring(cs.lastIndexOf(" ")+1));
        } catch (NoSuchElementException e) {
            contentSize = -1;
        }

        try {
            int count = 0;
            while (input.available()==0) {
                count++;
                if (count>REQUEST_TIMEOUT) {
                    return new byte[] {};
                }
            }
            if (contentSize==-1) {
                return null;
            }
            return input.readNBytes(contentSize);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Get headers from BufferedReader
    private String[] getRequestHeaders(InputStream request) {
        ArrayList<String> l = new ArrayList<String>();
        try {
            int count = 0;
            while (input.available()==0) {
                count++;
                if (count>REQUEST_TIMEOUT) {
                    return null;
                }
            }

            final StringBuilder sb = new StringBuilder();

            while (input.available() != 0) {
                final char c = (char) request.read();
                if (c=='\n') {
                    if (sb.length() != 1) {
                        l.add(URLDecoder.decode(sb.toString().strip(), StandardCharsets.UTF_8));
                        sb.setLength(0);
                        continue;
                    }

                    break;
                } else {
                    sb.append(c);
                }
            }
            return l.toArray(new String[0]);
        } catch (IOException e) {
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
    private void cmdRequestHandler(String request, String[] headers, String ip, byte[] content) {
        String cmd = request.split(commandPrefix)[0] + request.split("<>")[1];
        if (this.cmdResponses.containsKey(cmd)) {
            String[] args = request.substring(request.indexOf("<>", request.indexOf(commandPrefix)+5)+2).split("<>");
            createResponseThread(this.cmdResponses.get(cmd), request, args, ip, content);
        } else {
            this.send404();
        }
    }

    // Common requests handler 
    private void requestHandler(String request, String type, String[] headers, String ip, byte[] content) {
        // If response exist
        if (this.request.containsKey(request) && this.request.get(request).requestType.equals(type)) {
            createResponseThread(this.request.get(request), request, headers, ip, content);
        // Else looking for response, where responseIfStartsWith=true
        } else {
            // Ищем тот ответ, с которого начинается запрос
            for (Map.Entry<String, ResponseAction> el: this.request.entrySet()) {
                if (
                        !el.getValue().requestType.equals(type) ||
                        !el.getValue().isStartedWith ||
                        !request.startsWith(el.getKey()) ||
                        el.getKey().isEmpty()
                ) continue;

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
