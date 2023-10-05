package ru.kvdl.kevlight;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public class Responser {
    private final OutputStream out;
    private final ResponseAction on404;

    public OutputStream getOutput() {
        return out;
    }

    public String getRequest() {
        return request;
    }

    public String[] getArgs() {
        return headers;
    }

    public byte[] getContent() {
        if (content.length!=0) return content;
        return null;
    }

    public String getIp() {
        return ip;
    }

    private final String request;
    private final String[] headers;
    private final byte[] content;
    private final String ip;

    protected Responser(OutputStream out, ResponseAction on404, String req, String[] headers, byte[] content, String ip) {
        this.request = req;
        this.out = out;
        this.on404 = on404;
        this.headers = headers;
        this.content = content;
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
                e.printStackTrace();
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    public void sendString(String content) {
        sendBaseResponse(
            "200 OK",
            new String[] {"Content-Type: text/html; charset=utf-8"},
            content.getBytes()
        );
    }

    public void sendJSON(File json) {
        sendBaseResponse(
                "200 OK",
                new String[] {"Content-Type: text/json; charset=utf-8"},
                DHOperator.readFileBytes(json)
        );
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
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send 404 error page
     */
    public void send404Response() {
        if (on404==null) {
            this.sendResponse("Error 404", "200 OK");
            return;
        }
        on404.response(this);
    }

}
