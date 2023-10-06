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
    public boolean overwatch(Responser resp) {
        System.out.println(resp.getIp()+" "+resp.getRequest());
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
    @KLCmdRequestHandler(command = "echo")
    public void test(Responser resp) {
        if (resp.getArgs().length==1) {
            resp.sendResponse(resp.getArgs()[0], "200 OK");
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
### Возможности Responser
Responser - это класс, с помощью которого осуществляется ответ на запросы.
Методы Responser:

    getArgs() - возвращает аргументы команда(в KlCmdRequestHandler)
        или HTTP заголовки(в LKRequestHandler).
    getRequest() - возращает запрос, отправленный на сервер("", "images", "CMD<>login" и т. д.).
    getOutput() - возвращает OutputStream, с помощью которого можно вручную отправлять
        данные клиенту.
    getContent() - возвращает массив байтов, отправленный клиентом(можеть быть null).
    getIp() - возвращает IP адресс клиента, отправившего запрос.

    sendString(String content) - отправить строку
    sendResponse(String content, String status) - отправить строку с определённым HTTP статусом
    sendResponse(byte[] content, String status) - отправить массив байтов с
        определённым HTTP статусом
    sendResponse(String content, String status, String[] headers) - отправить строку с
        определённым HTTP статусом и заголовками
    sendResponse(byte[] content, String status, String[] headers) - отправить массив байтов с
        определённым HTTP статусом и заголовками
    sendJSON(File file) - отправить файл
    sendResponse(InputStream in, String status, String[] headers, int bufferSize) - прочесть
        файл и отправить содержимое, используя byte[] указанного размера, HTTP статус и заголовки
    
### Аннотации
#### @KLRequestHandler()
    String request - запрос, на который срабатывает метод
    String requestType = "GET" - тип запроса, на который срабатвыает метод(по умолчанию "GET")

#### @KLCmdRequestHandler()
    String cmd - команда, на которую срабатывает метод

#### @KLObserver()
    Метод будет выполняться перед любым запросом(см. пример)

#### @KL404Handler()
    Используется вместе с @KLRequestHandler(). 
    Метод будет вызываться при ошибке 404 или responser.send404Response()
