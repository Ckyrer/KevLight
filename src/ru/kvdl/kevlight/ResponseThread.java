package ru.kvdl.kevlight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ResponseThread implements Runnable {
    private final ResponseAction action;
    private final OutputStream output;
    private final InputStream input;
    private final String request;
    private final String[] args;
    private final String ip;
    private final byte[] content;
    private final Responser resp;

    public ResponseThread(ResponseAction act, String req, String[] args, String ip, OutputStream out, InputStream in, byte[] content, Responser responser) {
        this.request = req;
        this.action = act;
        this.output = out;
        this.input = in;
        this.ip = ip;
        this.resp = responser;
        this.content = content;
        this.args = args;
    }

    public void run() {
        action.response(request, args, ip, content, resp);

        try {this.input.close();} catch (IOException e) {e.printStackTrace();}
        try {this.output.close();} catch (IOException e) {e.printStackTrace();}
    }
}