package ru.kvdl.kevlight;

import ru.kvdl.kevlight.annotations.KLCmdRequestHandler;
import ru.kvdl.kevlight.annotations.KLRequestHandler;
import ru.kvdl.kevlight.utils.KLParam;

import java.lang.reflect.Method;

class ResponseAction  {
    final boolean isStartedWith;
    final String requestType;
    private final Object app;
    private final Method func;
    private final boolean isCmd;

    public ResponseAction(Object app, Method func, boolean isCmd) {
        this.isCmd = isCmd;
        if (isCmd) {
            this.app = app;
            this.isStartedWith = false;
            this.requestType = null;
        } else {
            final KLRequestHandler ann = func.getAnnotation(KLRequestHandler.class);
            this.app = app;
            this.isStartedWith = ann.startsWith();
            this.requestType = ann.requestType();
        }
        this.func = func;
    }

    private void invokeRequest(String req, String[] headers, String ip, byte[] content, Responser resp) {
        KLRequestHandler ann = this.func.getAnnotation(KLRequestHandler.class);
        Object[] argsToPass = new Object[ann.args().length+1];
        for (int i=0; i<ann.args().length; i++) {
            KLParam arg = ann.args()[i];
            switch (arg) {
                case REQUEST:
                    argsToPass[i] = req;
                    break;
                case HTTP_HEADERS:
                    argsToPass[i] = headers;
                    break;
                case IP:
                    argsToPass[i] = ip;
                    break;
                case CONTENT:
                    argsToPass[i] = content;
                    break;
            }
        }
        argsToPass[argsToPass.length-1] = resp;
        try {
            this.func.invoke(app, argsToPass);
        } catch (ReflectiveOperationException e) {e.printStackTrace();}
    }

    private void invokeCommandRequest(String req, String[] args, String ip, byte[] content, Responser resp) {
        KLCmdRequestHandler ann = this.func.getAnnotation(KLCmdRequestHandler.class);
        Object[] argsToPass = new Object[ann.args().length+1];
        for (int i=0; i<ann.args().length; i++) {
            KLParam arg = ann.args()[i];
            switch (arg) {
                case REQUEST:
                    argsToPass[i] = req;
                    break;
                case CMD_ARGUMENTS:
                    argsToPass[i] = args;
                    break;
                case IP:
                    argsToPass[i] = ip;
                    break;
                case CONTENT:
                    argsToPass[i] = content;
                    break;
            }
        }
        argsToPass[argsToPass.length-1] = resp;
        try {
            this.func.invoke(app, argsToPass);
        } catch (ReflectiveOperationException e) {e.printStackTrace();}
    }

    public void response(String request, String[] args, String ip, byte[] content, Responser resp) {
        if (isCmd) {
            invokeCommandRequest(request, args, ip, content, resp);
        } else {
            invokeRequest(request, args, ip, content, resp);
        }
    }

}
