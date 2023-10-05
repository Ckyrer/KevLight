# KevLight - простой Java фреймворк
### Простой сайт, написанный на KevLight:
```
public class App {
    final static String PATH = new File("").getAbsolutePath()+"/resources/";
    
    public static void main(String[] args)  {
        Server host = new Server(new TestApp(), 7070);

        host.start();
    }
    
    // Метод overwatch будет вызываться перед всеми другими методами при любом запросе.
    // Если метод вернёт false, то соединение закроется и начнётся обработка следующего.
    // Если метод вернёт true, то обработка запроса продолжится.
    @KLObserver
    public boolean overwatch(String req, String[] head, String ip, Responser resp) {
        System.out.println("Новый запрос: "+ip+" "+req);
        return true;
    }
    
    // Отправить начальную страницу
    @KLRequestHandler(request = "")
    public void home(Responser resp) {
        // DHOpertaor.build(path) - возращает HTML разметку, с JS и/или CSS внутри,
        // если в директории path есть main.js и/или style.css
        // в директории обязательно должен быть файл index.html
        resp.sendString(DHOperator.buildPage(PATH+"pages/home"))
    }

    // Метод test будет вызываться при запросе, который начинается с "COMMAND_PREFIX<>echo"
    // COMMAND_PREFIX = "CMD" (по умолчанию, можно изменить с помощью Server.setCommandPrefix())
    // args - аргументы, которые будут переданы при вызове метода(см. KLParam)
    @KLCmdRequestHandler(command = "echo", args = {KLParam.CMD_ARGUMENTS})
    public void test(String[] args, Responser resp) {
        if (args.length==1) {
            resp.sendResponse(args[0], "200 OK");
        } else {
            resp.send404Response();
        }
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
    KLParam[] - параметры, которые необходимо передать в метод(см. пример)

#### @KLCmdRequestHandler()
    String cmd - команда, на которую срабатывает метод
    KLParam[] - параметры, которые необходимо передать в метод(см. пример)

#### @KLObserver()
    Метод будет выполняться перед любым запросом(см. пример)

#### @KL404Handler()
    Используется вместе с @KLRequestHandler(). 
    Метод будет вызыватсья при ошибке 404 или responser.send404Response()
