package ru.kvdl.kevlight;

import java.lang.reflect.Method;

class ResponseAction  {
    final boolean isStart;
    final String type;
    private final Object app;
    private final Method func;
    private final Method funcCmd;

    public ResponseAction(Object app, Method func, String type, boolean isStart) {
        this.app = app;
        this.isStart = isStart;
        this.type = type;
        this.func = func;
        this.funcCmd = null;
    }

    public ResponseAction(Object app, Method func) {
        this.app = app;
        this.isStart = false;
        this.type = null;
        this.func = null;
        this.funcCmd = func;
    }

    public void response(String request, String[] args, String ip, Responser resp) {
        if (this.func!=null) {
            try {
                this.func.invoke(app, request, args, ip, resp);
            } catch (ReflectiveOperationException e) {e.printStackTrace();}
        } else {
            try {
                this.funcCmd.invoke(app, args, ip, resp);
            } catch (ReflectiveOperationException e) {e.printStackTrace();}
        }
    }

}
