# KevLight - простой Java фреймворк
### Простой сайт, написанный на KevLight:
```
public class App {
    final static String PATH = new File("").getAbsolutePath()+"/resources/";
    
    public static void main(String[] args)  {
        Server host = new Server(new TestApp(), 7070);

        host.start();
    }

    @KLObserver
    public boolean overwatch(String req, String[] head, String ip, Responser resp) {
        System.out.println(ip+" "+req);
        return true;
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
        resp.sendString(DHOperator.buildPage(PATH+"pages/home"))
    }
    
    // Этот метод будет вызываться при responser.send404Response()
    @KL404Handler
    @KLRequestHandler(request = "404") // request может быть равен любому значению т. к. используется аннотация KL404Handler
    public void on404(Responser resp) {
        resp.sendResponse("О нет! Страница не найдена", "404 Not Found");
    }
}
```
# Документация
Полная документация здесь: <URL HERE>
### Возможности Responser
Responser - это класс, с помощью которого осуществляется ответ на запросы.
Методы Responser:

    sendString(String content) - отправить строку
    sendResponse(String content, String status) - отправить строку с определённым HTTP статусом
    sendResponse(byte[] content, String status) - отпарвить массив байтов с определённым HTTP статусом
    sendResponse(String content, String status, String[] headers) - отправить строку с определённым HTTP статусом и заголовками
    sendResponse(byte[] content, String status, String[] headers) - отправить массив байтов с определённым HTTP статусом и заголовками
    sendJSON(File file) - отправить файл
    sendResponse(InputStream in, String status, String[] headers, int bufferSize) - прочесть файл и отправить содержимое, используя byte[] указанного размера, HTTP статус и заголовки
    
### Аннотации
#### @KLRequestHandler()
    String request - запрос, на который срабатывает метод
    String requestType = "GET" - тип запроса, на который срабатвыает метод(по умолчанию "GET")
    KLParam[] - 

