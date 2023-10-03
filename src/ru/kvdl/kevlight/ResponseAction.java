package ru.kvdl.kevlight;

import ru.kvdl.kevlight.annotations.KLRequestHandler;

import java.lang.reflect.Method;

class ResponseAction  {
    final boolean isStartedWith;
    final String requestType;
    private final Object app;
    private final Method func;

    public ResponseAction(Object app, Method func, boolean isCmd) {
        if (isCmd) {
            this.isStartedWith = false;
            this.requestType = null;
        } else {
            final KLRequestHandler ann = func.getAnnotation(KLRequestHandler.class);
            this.isStartedWith = ann.startsWith();
            this.requestType = ann.requestType();
        }
        this.app = app;
        this.func = func;
    }

    public void response(Responser resp) {
        try {
            this.func.invoke(app, resp);
        } catch (Exception e) {
            throw new RuntimeException("Неверные параметры у метода: "+func.getName());
        }
    }

}
