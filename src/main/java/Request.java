import org.apache.http.NameValuePair;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Request {

    private final String method;
    private final String path;
    private List<NameValuePair> query;

    public Request(String method, String path, List<NameValuePair> query) {
        this.method = method;
        this.path = path;
        this.query = query;
    }

    public Request(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQuery() {
        return query;
    }

    public List<NameValuePair> getQuery(String name) throws URISyntaxException {
        return getQuery()
                .stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }


}
