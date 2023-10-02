import ru.kvdl.kevlight.*;
import ru.kvdl.kevlight.annotations.KLCmdRequestHandler;
import ru.kvdl.kevlight.annotations.KLObserver;
import ru.kvdl.kevlight.annotations.KLRequestHandler;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class TestApp {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Server host = new Server(new TestApp(), 7070);

        host.start();
    }

    public void test(String arg) {
        System.out.println(arg);
    }

    @KLObserver
    public boolean overwatch(String req, String[] head, String ip, Responser resp) {
        System.out.println(ip+" "+req);
        return true;
    }

    @KLRequestHandler(request = "post", requestType = "POST", args = {KLParam.CONTENT})
    public void print(byte[] content, Responser resp) {
        System.out.print("Posted this: ");
        System.out.print(new String(content));
        resp.sendResponse("aboba", "200 OK");
    }

    @KLCmdRequestHandler(command = "echo")
    public void test(String[] args, String ip, Responser resp) {
        if (args.length==1) {
            resp.sendResponse(args[0], "200 OK");
        } else {
            resp.send404Response();
        }
    }

    @KLRequestHandler(request = "")
    public void home(Responser resp) {
        System.out.println("Hi");
        resp.sendResponse("Hello there!", "200 OK");
    }

    @KLRequestHandler(request = "json")
    public void onJSON(Responser resp) {
        resp.sendJSON(new File(new File("").getAbsolutePath()+"/resources/test.json"));
    }

    @KLRequestHandler(request = "404")
    public void on404(Responser resp) {
        System.out.println("404");
        resp.sendResponse("Oh no( 404", "404 Not Found");
    }
}