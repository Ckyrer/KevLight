package ru.kvdl.kevlight;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ResponseThread implements Runnable {
    private final ResponseAction action;
    private final OutputStream output;
    private final InputStream input;
    private final Responser resp;

    public ResponseThread(ResponseAction act, OutputStream out, InputStream in, Responser responser) {
        this.action = act;
        this.output = out;
        this.input = in;
        this.resp = responser;
    }

    public void run() {
        action.response(resp);

        try {this.input.close();} catch (IOException e) {e.printStackTrace();}
        try {this.output.close();} catch (IOException e) {e.printStackTrace();}
    }
}