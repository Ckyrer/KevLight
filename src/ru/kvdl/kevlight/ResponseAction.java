package ru.kvdl.kevlight;

class ResponseAction  {
    final boolean isStart;
    final Action func;
    final ActionCMD funcCmd;

    public ResponseAction(Action func, boolean isStart) {
        this.isStart = isStart;
        this.func = func;
        this.funcCmd = null;
    }

    public ResponseAction(ActionCMD func) {
        this.isStart = false;
        this.func = null;
        this.funcCmd = func;
    }

    public void response(String ip, String[] args, Responser resp) {
        if (this.func!=null) {
            this.func.response(args, ip, resp);
        } else {
            this.funcCmd.response(args, ip, resp);
        }
    }

}
