package ru.kvdl.kevlight;

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
            // Прочие запросы
            if (handler.isAnnotationPresent(KLRequestHandler.class)) {
                KLRequestHandler ann = handler.getAnnotation(KLRequestHandler.class);

                // Проверка на верные параметры
                if (validateMethodArguments(handler, false)) {
                    this.commonResponses.put(ann.request(), new ResponseAction(this.app, handler, ann.startsWith()));
                } else {
                    throw new RuntimeException("Неверные аргументы метода для использования аннотации KLRequestHandler: " + handler.getName());
                }
            // Команды
            } else if (handler.isAnnotationPresent(KLCmdRequestHandler.class)) {
                KLCmdRequestHandler ann = handler.getAnnotation(KLCmdRequestHandler.class);

                // Проверка на верные параметры
                if (validateMethodArguments(handler, true)) {
                    this.commandResponses.put(ann.command(), new ResponseAction(this.app, handler));
                } else {
                    throw new RuntimeException("Неверные аргументы метода для использования аннотации KLCmdRequestHandler: " + handler.getName());
                }
            // Наблюдатель
            } else if (handler.isAnnotationPresent(KLObserver.class)) {
                Class<?>[] types = handler.getParameterTypes();

                // Проверка на верные параметры и возвращаемое значение
                if (handler.getReturnType() != boolean.class) {
                    throw new RuntimeException("Неверный тип возвращаемого значения метода для использования аннотации KLObserver: " + handler.getName());
                }

                if (types.length == 4 &&
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
    private BufferedReader input;
    private Responser responser;

    // Главный цикл
    private void mainLoop() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                while (true) {
                    // ожидание подключения
                    Socket socket = serverSocket.accept();

                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    output = socket.getOutputStream();

                    // ожидание первой строки запроса
                    while (!input.ready());

                    // чтение всего, что было отправлено клиентом
                    final String[] headers = getRequestHeaders(input);

                    final String fstLine = headers[0];
                    final String request = fstLine.substring(fstLine.indexOf(" ")+2, fstLine.lastIndexOf(" "));

                    final String ip = socket.getInetAddress().toString().substring(1);

                    responser =  new Responser(output, this.commonResponses.get("404"), request, headers, ip);
                    
                    // Отвечаем, если смотртитель одобрил подключение
                    if ( observer ==null || this.getObserverPermission(request, headers, ip, responser) ) {

                        // Если запрос является командой
                        if (request.contains(commandPrefix+"<>")) {
                            this.commandRequestHandler(request, headers, ip);
                        } else {
                            this.commonRequestHandler(request, headers, ip);
                        }
                    }                
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    // ---------------
    // PRIVATE METHODS
    // ---------------

    // Проверить на правильность аргументы метода
    private boolean validateMethodArguments(Method method, boolean isCmd) {
        Class<?>[] types = method.getParameterTypes();
        if (isCmd) {
            return types.length==3 && types[0] == String[].class && types[1] == String.class && types[2] == Responser.class;
        }
        return types.length==4 && types[0] == String.class && types[1] == String[].class && types[2] == String.class && types[3] == Responser.class;
    }

    // Запустить наблюдателя
    private boolean getObserverPermission(String request, String[] args, String ip, Responser resp) {
        try {
            return (boolean) this.observer.invoke(this.app, request, args, ip, resp);
        } catch (ReflectiveOperationException e) {e.printStackTrace();}
        return false;
    }

    // Get headers from BufferedReader
    private String[] getRequestHeaders(BufferedReader request) {
        ArrayList<String> l = new ArrayList<String>();
        try {
            String str;
            while (!(str = request.readLine()).isEmpty()) {
                l.add(URLDecoder.decode(str, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {e.printStackTrace();}
        String[] res = new String[l.size()];
        l.toArray(res);
        return res;
    }

    // Create and start response thread
    private void createResponseThread(ResponseAction action, String req, String[] args, String ip) {
        new Thread(
            new ResponseThread(action, req, args, ip, output, input, responser)
        ).start();
    }

    // Command requests handler
    private void commandRequestHandler(String request, String[] headers, String ip) {
        String cmd = request.split(commandPrefix)[0] + request.split("<>")[1];
        if (this.commandResponses.containsKey(cmd)) {
            String[] args = request.substring(request.indexOf("<>", request.indexOf(commandPrefix)+5)+2).split("<>");
            createResponseThread(this.commandResponses.get(cmd), request, args, ip);
        } else {
            this.send404();
        }
    }

    // Common requests handler 
    private void commonRequestHandler(String request, String[] headers, String ip) {
        // If response exist
        if (this.commonResponses.containsKey(request)) {
            createResponseThread(this.commonResponses.get(request), request, headers, ip);
        // Else looking for response, where responseIfStartsWith=true
        } else {
            // Ищем тот ответ, с которого начинается запрос
            for (Map.Entry<String, ResponseAction> el: this.commonResponses.entrySet()) {
                if ( !request.startsWith(el.getKey()) || el.getKey().isEmpty() || !el.getValue().isStart) continue;

                createResponseThread(el.getValue(), request, headers, ip);
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
