import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Main {


    public static void main(String[] args) throws IOException {
        var server = new Server(9999, 64);

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages",
                (request, responseStream) -> {
                    server.resourceNotFound(responseStream);
                });


        server.addHandler("POST", "/messages",
                (request, responseStream) -> {
                    server.resourceNotFound(responseStream);
                });

        server.start();


    }
}


