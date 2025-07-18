import java.util.*;
import com.sun.net.httpserver.*;
import java.io.*;
public class EventSource {
    public byte[] onPage(HashMap<String, Object> query) {
        //https://liakh-aliaksandr.medium.com/server-sent-events-sse-in-spring-5-with-web-mvc-and-web-flux-44c926b59f36
        HashMap<String, Object> session = (HashMap<String, Object>) query.get("session");   // Получение session объекта
        HashMap<String, Object> vars = (HashMap<String, Object>) query.get("vars");         // переменные (из URL строки)
        HashMap<String, Object> property = (HashMap<String, Object>) query.get("property"); // атребуты загодовка запроса
        HttpExchange httpExchange = (HttpExchange) query.get("httpExchange");               // получение серверного обхъекта
        String method = httpExchange.getRequestMethod();                                    // метод запроса GET или POST
        OutputStream body = (OutputStream) query.get("body");
        // System.out.print("body"+body);
        // os.write("messageBytes".getBytes());
        /*
        String resultText ="data: {\"ok\":\"false\"}\n\n";
        byte[] messageBytes = resultText.getBytes();
        httpExchange.getResponseHeaders().set("Cache-Control", "no-store");
        httpExchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        try {
            OutputStream os = httpExchange.getResponseBody();
            while (1==1) {
                os.write(messageBytes);
            }
            //os.close();
        } catch (IOException e) {

        }
         */
        return "-------null------".getBytes();
    }
}