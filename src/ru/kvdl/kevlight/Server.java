package ru.kvdl.kevlight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server {
    // Set by server
    final private Map<String, ResponseAction> responses = new HashMap<String, ResponseAction>();
    final private Map<String, ResponseAction> commands = new HashMap<String, ResponseAction>();
    private Responser responser;

    // Set by user
    final private int port;
    private String commandPrefix = "CMD";
    final private Overwatch overwatch;

    // Response on 404
    private Action action404 = (String[] headers, String ip, Responser responser) -> {
        responser.sendResponse("Error 404", "404 Not Found");
    };

    public Server(int port) {
        this.port = port;
        this.overwatch = null;
    }

    public Server(int port, Overwatch overwatch) {
        this.port = port;
        this.overwatch = overwatch;
    }


    // --------------
    // PUBLIC METHODS
    // --------------

    // Set prefix for command requests
    public void setCommandPrefix(String prefix) {
        this.commandPrefix = prefix;
    }

    // Add request handler for common handler
    public void addRequestHandler(String request, boolean responseIfStartsWith, Action handler) {
        if (request.equals("404")) {
            action404 = handler;
            return;
        }
        this.responses.put(request, new ResponseAction(handler, responseIfStartsWith));
    }

    // Add request handler for COMMAND handler
    public void addCommandRequestHandler(String request, ActionCMD handler) {
        this.commands.put(request, new ResponseAction(handler));
    }

    // Start server
    public void start() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                
                while (true) {
                    // ожидание подключения
                    Socket socket = serverSocket.accept();

                    final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final OutputStream output = socket.getOutputStream();

                    // ожидание первой строки запроса
                    while (!input.ready()) ;

                    // чтение всего, что было отправлено клиентом
                    final String[] headers = getRequestHeaders(input);
                    final String fstLine = headers[0];
                    final String requestedResource = fstLine.substring(fstLine.indexOf(" ")+2, fstLine.lastIndexOf(" "));
                    final String ip = socket.getInetAddress().toString().substring(1);
                    
                    responser = new Responser(output, action404, headers, ip);
                    
                    // Отвечаем, если смотртитель одобрил подключение
                    if ( overwatch==null || overwatch.checkpoint(requestedResource, ip, headers, responser) ) {

                        // Если запрос является командой
                        if (requestedResource.contains(commandPrefix+"<>")) {
                            commandRequestHandler(requestedResource, headers, ip, output, input);
                        } else {
                            commonRequestHandler(requestedResource, headers, ip, output, input);
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
            while ( !(str=request.readLine()).equals("") ) {
                l.add(URLDecoder.decode(str, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {e.printStackTrace();}
        String[] res = new String[l.size()];
        l.toArray(res);
        return res;
    }

    // Create and start response thread
    private void createResponseThread(ResponseAction action, String[] args, String ip, BufferedReader in, OutputStream out) {
        new Thread(
            new ResponseThread(action, args, ip, out, in, responser)
        ).start();
    }

    // Command requests handler
    private void commandRequestHandler(String request, String[] headers, String ip, OutputStream out, BufferedReader in) { 
        String cmd = request.split(commandPrefix)[0] + request.split("<>")[1];
        if (this.commands.containsKey(cmd)) {
            String[] args = request.substring(request.indexOf("<>", request.indexOf(commandPrefix)+5)+2).split("<>");
            createResponseThread(this.commands.get(cmd), args, ip, in, out);
        } else {
            responser.sendResponse("NONE", "404 Not Found");
        }
    }

    // Common requests handler 
    private void commonRequestHandler(String request, String[] headers, String ip, OutputStream out, BufferedReader in) {
        // If response is exist
        if (this.responses.containsKey(request)) {
            createResponseThread(this.responses.get(request), headers, ip, in, out);
        // Else looking for response, where responseIfStartsWith=true
        } else {
            // Ищем тот ответ, с которого начинается запрос
            for (Map.Entry<String, ResponseAction> el: responses.entrySet()) {
                if ( !request.startsWith(el.getKey()) || el.getKey().equals("") ) continue;

                createResponseThread(el.getValue(), headers, ip, in, out);
                return;
            }
            
            // Иначе отправляем 404
            createResponseThread(new ResponseAction(action404, false), headers, ip, in, out);
        }
    }
}
