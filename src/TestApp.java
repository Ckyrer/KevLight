import ru.kvdl.kevlight.*;
import ru.kvdl.kevlight.annotations.KL404Handler;
import ru.kvdl.kevlight.annotations.KLCmdRequestHandler;
import ru.kvdl.kevlight.annotations.KLObserver;
import ru.kvdl.kevlight.annotations.KLRequestHandler;
import ru.kvdl.kevlight.utls.KLParam;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestApp {
    final static String PATH = new File("").getAbsolutePath();
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Server host = new Server(new TestApp(), 7070);

        host.start();
    }

    @KLObserver
    public boolean overwatch(String req, String[] head, String ip, Responser resp) {
        System.out.println(ip+" "+req);
        return true;
    }

    @KLRequestHandler(request = "test")
    public void test(Responser resp) throws IOException {
        resp.out.write( ("HTTP/1.1 200 OK\n").getBytes() );
        resp.out.write( ("Content-Type: text/html; encoding=utf-8\n").getBytes() );
        resp.out.write( ("\n").getBytes() );
        resp.out.write( ("Hello\n").getBytes() );
    }


    @KLRequestHandler(request = "post", requestType = "POST", args = {KLParam.CONTENT})
    public void print(byte[] content, Responser resp) {
        System.out.print("Posted this: ");
        System.out.print(new String(content));
        resp.sendResponse("Success", "200 OK");
    }

    @KLCmdRequestHandler(command = "echo", args = {KLParam.CMD_ARGUMENTS})
    public void test(String[] args, Responser resp) {
        if (args.length==1) {
            resp.sendResponse(args[0], "200 OK");
        } else {
            resp.send404Response();
        }
    }

    @KLRequestHandler(request = "")
    public void home(Responser resp) {
        resp.sendResponse("Hello there!", "200 OK");
    }

    @KLRequestHandler(request = "json")
    public void onJSON(Responser resp) {
        resp.sendJSON(new File(PATH+"/resources/test.json"));
    }

    @KL404Handler
    @KLRequestHandler(request = "404")
    public void on404(Responser resp) {
        resp.sendResponse("Oh no( 404", "404 Not Found");
    }
}