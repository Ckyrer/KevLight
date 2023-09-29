package ru.kvdl.kevlight;

import ru.kvdl.kevlight.annotations.KLRequestHandler;

import java.lang.reflect.Method;

class ResponseAction  {
    final boolean isStart;
    final String type;
    private final Object app;
    private final Method func;
    private final Method funcCmd;

    public ResponseAction(Object app, Method func, boolean isCmd) {
        if (isCmd) {
            this.app = app;
            this.isStart = false;
            this.type = null;
            this.func = null;
            this.funcCmd = func;
        } else {
            final KLRequestHandler ann = func.getAnnotation(KLRequestHandler.class);
            this.app = app;
            this.isStart = ann.startsWith();
            this.type = ann.requestType();
            this.func = func;
            this.funcCmd = null;
        }
    }

    private void invokeCommonRequest(Object req, String[] headers, String ip, byte[] content, Responser resp) {
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

    public void response(String request, String[] args, String ip, byte[] content, Responser resp) {
        if (this.func!=null) {
            invokeCommonRequest(request, args, ip, content, resp);
        } else {
            try {
                this.funcCmd.invoke(app, args, ip, resp);
            } catch (ReflectiveOperationException e) {e.printStackTrace();}
        }
    }

}
