import java.io.BufferedOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface Handler {
    public void handler (Request request, BufferedOutputStream responseStream) throws IOException;
}
