package ru.kvdl.kevlight;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.SocketException;

public class Responser {
    public final OutputStream out;
    private final ResponseAction on404;
    private final String request;
    private final String[] headers;
    private final String ip;

    protected Responser(OutputStream out, ResponseAction on404, String req, String[] headers, String ip) {
        this.request = req;
        this.out = out;
        this.on404 = on404;
        this.headers = headers;
        this.ip = ip;
    }

    // Эта так скажем основа, это база
    private void sendBaseResponse(String status, String[] headers, byte[] content) {
        try {
            try {
                out.write( ("HTTP/1.1 "+status+"\n").getBytes() );
                for (String h : headers) {
                    out.write( (h+"\n").getBytes() );
                }
                if (content!=null) {
                    out.write( "\n".getBytes() );
                    out.write( content );
                }
                out.flush();
            } catch (SocketException e) {
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    /** 
     * Send string with status 200 OK, content-type: text/html.
     * @param content A string to send
     * @param status HTTP status code
     */
    public void sendResponse(String content, String status) {
        sendBaseResponse(
            status,
            new String[] {"Content-Type: text/html; charset=utf-8"},
            content.getBytes()
        );
    }

    /** 
     * Send byte[] with status 200 OK, content-type: text/html.
     * @param content A byte[] to send
     * @param status HTTP status code
     */
    public void sendResponse(byte[] content, String status) {
        sendBaseResponse(
            status,
            new String[] {"Content-Type: text/html; charset=utf-8"},
            content
        );
    }

    /** 
     * Send string.
     * @param content A string to send
     * @param status HTTP status code
     * @param headers HTTP headers
     */
    public void sendResponse(String content, String status, String[] headers) {
        sendBaseResponse(
            status,
            headers,
            content.getBytes()
        );
    }

    /** 
     * Send byte[].
     * @param content A byte[] to send
     * @param status HTTP status code
     * @param headers HTTP headers
     */
    public void sendResponse(byte[] content, String status, String[] headers) {
        sendBaseResponse(
            status,
            headers,
            content
        );
    }

    /**
     * Send InputStream content by chunks and close InputStream.
     * @param in InputStream to send
     * @param status HTTP status code
     * @param headers HTTP headers
     * @param bufferSize Size of buffer
     */
    public void sendResponse(InputStream in, String status, String[] headers, int bufferSize) {
        try {
            try {
                out.write(("HTTP/1.1 "+status+"\n").getBytes());
                out.write("\n".getBytes());
                
                byte[] buffer = new byte[bufferSize];
            
                int l;
                while((l = in.read(buffer)) != -1) {
                    out.write(buffer, 0, l);
                }
                
                out.flush();
            } catch (SocketException e) {
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send 404 error page
     */
    public void send404Response() {
        on404.response(request, headers, ip, this);
    }

}
