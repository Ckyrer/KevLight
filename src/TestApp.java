import ru.kvdl.kevlight.Server;

public class TestApp {
    public static void main(String[] args) {
        Server host = new Server(new Handler(), 7070);

        host.start();
    }
}