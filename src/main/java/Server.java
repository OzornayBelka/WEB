import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class Server {
    private final List<String> validPaths = List.of("/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final int port;
    private final int threads;
    private final int REQUEST_LINE_LENGTH = 3;


    //храним обработчики
    private Map<String, Map<String, Handler>> handlers = new HashMap<>();

    //конструктор сервера
    public Server(int port, int threads) {
        this.port = port;
        this.threads = threads;
    }


    public void start() throws IOException {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер стартовал");
            final var executorService = Executors.newFixedThreadPool(threads);
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> {
                    try {
                        connectionProcessing(socket);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    public void connectionProcessing(Socket socket) throws IOException, URISyntaxException {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != REQUEST_LINE_LENGTH) {
                return;
            }

            final var method = parts[0];//метод
            final var pathAndQuery = parts[1];//путь который может содержать query
            final var pathNoQuery = pathAndQuery.split("\\?")[0];//получили путь без параметров
            final var query = URLEncodedUtils.parse(new URI(pathAndQuery), Charset.defaultCharset());//распарсили параметры
            query.forEach(System.out::println);//проверяем как распарсились параметры
            final var request = new Request(method, pathNoQuery, query);//создали обьект Request

            if (validPaths.contains(request.getPath())) {
                requestPositiveHandler(request, out);//если путь обьекта равен валидным путям то отправляем ответ
            }

            if (!handlers.containsKey(request.getMethod())) {
                resourceNotFound(out);//если в списке обработчиков нет метода, который есть в обьекте, то 404
            } else {
                var handlerMap = handlers.get(request.getMethod());//получили обработчик по методу
                if (!handlerMap.containsKey(request.getPath())) {
                    resourceNotFound(out);//если нет нужного пути в найденном методе, то 404
                } else {
                    handlerMap.get(pathAndQuery).handler(request, out);// достаем из мапы путь и вызываем обработчик
                }
            }
        }
    }

    public void requestPositiveHandler(Request request, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", request.getPath());//путь к файлу
        final var mimeType = Files.probeContentType(filePath);//тип файла

        if (request.getPath().equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return;
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }


    public void resourceNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public synchronized void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {//если коллекция не содержит метод
            handlers.put(method, new HashMap<>());//добавляем метод  и создаем новую мапу
        }
        if (!handlers.get(method).containsKey(path)) {//сравниваем метод и путь, если они не равны
            handlers.get(method).put(path, handler);
        }

    }

}

