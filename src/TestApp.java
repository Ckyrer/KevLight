//import ru.kvdl.kevlight.*;
//
//public class TestApp {
//    public static void main(String[] args) {
//        Server host = new Server(new TestApp(), 7070);
//
//        host.start();
//    }
//
//    @KLObserver
//    public boolean overwatch(String req, String[] head, String ip, Responser resp) {
//        System.out.println(ip+" "+req);
//        return true;
//    }
//
//    @KLCmdRequestHandler(command = "echo")
//    public void test(String[] args, String ip, Responser resp) {
//        if (args.length==1) {
//            resp.sendResponse(args[0], "200 OK");
//        } else {
//            resp.send404Response();
//        }
//    }
//
//    @KLRequestHandler(request = "")
//    public void home(String req, String[] head, String ip, Responser resp) {
//        System.out.println("Hi");
//        resp.sendResponse("Hello there!", "200 OK");
//    }
//
//    @KLRequestHandler(request = "404")
//    public void on404(String req, String[] head, String ip, Responser resp) {
//        System.out.println("404");
//        resp.sendResponse("Oh no( 404", "404 Not Found");
//    }
//}