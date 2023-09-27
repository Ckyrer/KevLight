package ru.kvdl.kevlight;

import java.lang.reflect.Method;

class ResponseAction  {
    final boolean isStart;
    private final Class<?> app;
    private final Method func;
    private final Method funcCmd;

    public ResponseAction(Class<?> app, Method func, boolean isStart) {
        this.app = app;
        this.isStart = isStart;
        this.func = func;
        this.funcCmd = null;
    }

    public ResponseAction(Class<?> app, Method func) {
        this.app = app;
        this.isStart = false;
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
