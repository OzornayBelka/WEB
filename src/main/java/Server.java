import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;

public class Server {

    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js");
    private final int port;
    private final int threads;


    public Server(int port, int threads) {
        this.port = port;
        this.threads = threads;
    }

    public void startServer() throws IOException {
        try(final var serverSocket = new ServerSocket(port)){
            final var threadPool = Executors.newFixedThreadPool(threads);
            while (!serverSocket.isClosed()){
                final var socket = serverSocket.accept();
                threadPool.submit(() -> {
                    try {
                        connectionProcessing(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

        public void connectionProcessing(Socket socket) throws IOException {
        try(
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }
            final var path = parts[1];
            if(!validPaths.contains(path)){
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                        ).getBytes());
                out.flush();
                return;
            }
            final var filePath = Path.of(".", "Public", path);
            final var fileType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);

            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + fileType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + fileType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
                    ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
        }
}
