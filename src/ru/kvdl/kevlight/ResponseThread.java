package ru.kvdl.kevlight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

class ResponseThread implements Runnable {
    private final OutputStream output;
    private final BufferedReader input;
    private final String ip;
    private final ResponseAction action;
    private final Responser resp;
    private final String[] args;

    public ResponseThread(ResponseAction act, String[] args, String ip, OutputStream out, BufferedReader in,  Responser responser) {
        this.action = act;
        this.output = out;
        this.input = in;
        this.ip = ip;
        this.resp = responser;
        this.args = args;
    }

    public void run() {
        action.response(ip, args, resp);

        try {this.input.close();} catch (IOException e) {e.printStackTrace();}
        try {this.output.close();} catch (IOException e) {e.printStackTrace();}
    }
}