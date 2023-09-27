import ru.kvdl.kevlight.KLRequestHandler;
import ru.kvdl.kevlight.Responser;

public class Handler {
    @KLRequestHandler(request = "wad")
    public void home(String req, String[] args, String ip, Responser resp) {
        resp.sendResponse("Hello", "200 OK");
    }
}
