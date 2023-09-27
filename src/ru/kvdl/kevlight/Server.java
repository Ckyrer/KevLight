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
    final private Class<?> app;
    final private int port;
    private String commandPrefix = "CMD";
    private Method overwatch;

    public Server(Class<?> app, int port) {
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
    public void start(Object mainClass) {
        // Поиск методов с аннотацией KLRequestHandler и KLObserver
        for ( Method handler : mainClass.getClass().getDeclaredMethods() ) {
            if (!handler.isAnnotationPresent(KLRequestHandler.class)) {
                if (handler.isAnnotationPresent(KLObserver.class)) {
                    Class<?>[] types = handler.getParameterTypes();
                    if (types[0] == String.class && types[1] == String[].class && types[2] == Responser.class) {
                        this.overwatch = handler;
                    } else {
                        throw new RuntimeException("Неверные аргументы метода для использования аннотации KLRequestHandler");
                    }
                }
                continue;
            }

            KLRequestHandler ann = handler.getAnnotation(KLRequestHandler.class);
            Class<?>[] types = handler.getParameterTypes();

            // Проверка на верные аргументы
            if (types.length==4 && types[0] == String.class && types[1] == String[].class && types[2] == String.class && types[3] == Responser.class) {
                this.commonResponses.put(ann.request(), new ResponseAction(this.app, handler, ann.startsWith()));
            } else {
                throw new RuntimeException("Неверные аргументы метода для использования аннотации KLRequestHandler");
            }

        }

        // Поиск методов с аннотацией KLCmdRequestHandler
        for ( Method handler : mainClass.getClass().getDeclaredMethods() ) {
            if (!handler.isAnnotationPresent(KLCmdRequestHandler.class)) continue;

            KLCmdRequestHandler ann = handler.getAnnotation(KLCmdRequestHandler.class);
            Class<?>[] types = handler.getParameterTypes();

            // Проверка на верные аргументы
            if (types.length==3 && types[0] == String[].class && types[1] == String.class && types[2] == Responser.class) {
                this.commandResponses.put(ann.command(), new ResponseAction(this.app, handler));
            } else {
                throw new RuntimeException("Неверные аргументы метода для использования аннотации KLCmdRequestHandler");
            }
        }

        // Запуск главного цикла
        mainLoop();

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
                    while (!input.ready()) ;

                    // чтение всего, что было отправлено клиентом
                    final String[] headers = getRequestHeaders(input);

                    final String fstLine = headers[0];
                    final String request = fstLine.substring(fstLine.indexOf(" ")+2, fstLine.lastIndexOf(" "));

                    final String ip = socket.getInetAddress().toString().substring(1);

                    responser =  new Responser(output, this.commonResponses.get("404"), request, headers, ip);
                    
                    // Отвечаем, если смотртитель одобрил подключение
                    if ( overwatch==null || overwatch.checkpoint(request, ip, headers, responser) ) {

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
            this.on404(request, headers, ip);
        }
    }

    // Common requests handler 
    private void commonRequestHandler(String request, String[] headers, String ip) {
        // If response exist
        if (this.commandResponses.containsKey(request)) {
            createResponseThread(this.commonResponses.get(request), request, headers, ip);
        // Else looking for response, where responseIfStartsWith=true
        } else {
            // Ищем тот ответ, с которого начинается запрос
            for (Map.Entry<String, ResponseAction> el: this.commonResponses.entrySet()) {
                if ( !request.startsWith(el.getKey()) || el.getKey().isEmpty()) continue;

                createResponseThread(el.getValue(), request, headers, ip);
                return;
            }
            
            // Иначе отправляем 404
            this.on404(request, headers, ip);
        }
    }

    private void on404(String request, String[] args, String ip) {
        if (this.commonResponses.containsKey("404")) {
            createResponseThread(this.commonResponses.get("404"), request, args, ip);
        } else {
            responser.sendResponse("Error 404", "200 OK");
        }
    }

}
