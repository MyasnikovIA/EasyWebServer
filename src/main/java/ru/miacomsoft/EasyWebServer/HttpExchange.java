package ru.miacomsoft.EasyWebServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static ru.miacomsoft.EasyWebServer.ServerResourceHandler.expandArray;
import static ru.miacomsoft.EasyWebServer.ServerResourceHandler.findSequence;

public class HttpExchange {
    public Socket socket;
    public final Map<String, Object> headers = new HashMap<>();
    public final Map<String, String> cookie = new HashMap<>();
    public final Map<String, String> responseHeaders = new HashMap<>();
    public static final JSONObject SHARE = new JSONObject();
    public String sessionID = "";
    public Map<String, Object> session;
    public InputStreamReader inputStreamReader;
    public String typeQuery = "";
    public String mimeType = "text/html";
    public long contentLength = 0;
    public String requestText = "";
    public String requestPath = "";
    public String webappDir = "";
    public String expansion = "";
    public StringBuffer headSrc = new StringBuffer();
    public char[] postCharBody = null;
    public byte[] postByte = null;
    public JSONObject requestParam = new JSONObject();
    public JSONArray requestParamArray = new JSONArray();
    public String message = ""; // Raw message from client
    public JSONObject messageJson = new JSONObject();
    public int countQuery = 0;

    /// PtP - объект для обмена данными между устройствами "точка ту точка"
    public HttpExchange queryPtP=null;

    // Thread-safe shared resources
    public static final Map<String, HttpExchange> DevList = new ConcurrentHashMap<>();
    public static final Map<String, String> MESSAGE_LIST = new ConcurrentHashMap<>();
    public static final Map<String, List<String>> BROADCAST_MESSAGE_LIST = new ConcurrentHashMap<>();

    public HttpExchange(Socket socket, Map<String, Object> session) throws IOException, JSONException {
        this.SHARE.put("server", "WebServerLite");
        this.socket = socket;
        if (this.socket != null) {
            this.socket.setSoTimeout(86400000);
            this.inputStreamReader = new InputStreamReader(socket.getInputStream());
        }
        this.responseHeaders.put("Connection", "close");
        this.responseHeaders.put("Server", "EasyWebServer");
        this.webappDir = ServerConstant.config.WEBAPP_DIR;
        this.session = session;
    }

    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isConnected() {
        try {
            return socket != null && !socket.isClosed() && socket.isConnected() && inputStreamReader.ready();
        } catch (IOException e) {
            return false;
        }
    }

    public Map<String, Object> getRequestHeaders() {
        return headers;
    }

    public void sendFile(String pathFile) {
        File file = new File(pathFile);
        if (!file.exists()) return;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("#")) {
                    line = line.split("#")[0];
                }
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendHtml(sb.toString());
    }

    public boolean write(byte[] content) {
        try {
            socket.getOutputStream().write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void send(byte[] content) {
        write(content);
    }

    public void send(String content) {
        send(content.getBytes(Charset.forName("UTF-8")));
    }

    public void sendHtml(String content) {
        sendResponse(content.getBytes(), false);
    }

    public void sendHtmlCrosDomen(String content) {
        sendResponse(content.getBytes(), true);
    }

    public void sendHtmlMime(byte[] content, String mime) {
        this.mimeType = mime;
        sendResponse(content, false);
    }

    private void sendResponse(byte[] content, boolean corsEnabled) {
        try (OutputStream out = socket.getOutputStream()) {
            out.write("HTTP/1.1 200 OK\r\n".getBytes());

            out.write(("Content-Type: " + mimeType + "; charset=utf-8\r\n").getBytes());
            out.write(("Content-Length: " + content.length + "\r\n").getBytes());

            if (corsEnabled) {
                out.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                out.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                out.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            }

            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                out.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes());
            }

            out.write("\r\n".getBytes());
            out.write(content);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendByteFile(File file) {
        try (InputStream in = new FileInputStream(file); OutputStream out = socket.getOutputStream()) {
            out.write("HTTP/1.1 200 OK\r\n".getBytes());
            out.write(("Content-Type: " + mimeType + "\r\n").getBytes());
            out.write(("Content-Length: " + file.length() + "\r\n").getBytes());

            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                out.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes());
            }

            out.write("\r\n".getBytes());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readLine() {
        StringBuilder sb = new StringBuilder();
        int c;
        try {
            while ((c = inputStreamReader.read()) != -1) {
                char ch = (char) c;
                sb.append(ch);
                if (ch == '\n') break;
            }
        } catch (IOException e) {
            return null;
        }
        return sb.toString();
    }

    public String readLineTerm() {
        String tmpStr = null;
        try {
            byte[] temp = new byte[16384];
            int bytesRead;
            while ((bytesRead = socket.getInputStream().read(temp)) != -1 && socket.isConnected()) {
                tmpStr = new String(temp, 0, bytesRead);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return tmpStr;
    }

    public String read() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            byte[] temp = new byte[1024];
            int bytesRead;
            while ((bytesRead = socket.getInputStream().read(temp)) != -1) {
                buffer.write(temp, 0, bytesRead);
                String tmpStr = new String(temp, 0, bytesRead);
                if ((tmpStr.contains("\r\n\r\n") || tmpStr.contains("\r\r")) || tmpStr.contains("\n\n")) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String messageStr = buffer.toString(Charset.forName("UTF-8"));
        this.message = messageStr;

        for (String line : messageStr.split("\r\n")) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                headers.put(parts[0].trim(), parts[1].trim());
            }
        }

        countQuery++;
        return messageStr;
    }

    public JSONArray SQL(String sql) {
        JSONArray result = new JSONArray();
        Connection conn = null;

        try {
            if (session.containsKey("DATABASE")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataBase = (Map<String, Object>) session.get("DATABASE");

                if (dataBase.containsKey("CONNECT")) {
                    conn = (Connection) dataBase.get("CONNECT");
                } else {
                    String user = (String) dataBase.get("DATABASE_USER_NAME");
                    String pass = (String) dataBase.get("DATABASE_USER_PASS");
                    conn = PostgreQuery.getConnect(user, pass);
                    dataBase.put("CONNECT", conn);
                }
            }

            if (conn == null || conn.isClosed()) return null;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String colName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(colName, value == null ? "null" : value);
                    }
                    result.put(row);
                }

            }

        } catch (SQLException | JSONException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }

        return result;
    }

    public static String parseErrorRunJava(Exception exception) {
        StringBuilder sbError = new StringBuilder();
        sbError.append("<pre>\n");
        sbError.append("Произошла ошибка: ").append(exception.getMessage()).append("\n");
        sbError.append("Тип ошибки: ").append(exception.getClass().getName()).append("\n");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        sbError.append("Подробное описание:\n").append(sw);

        sbError.append("</pre>");
        return sbError.toString();
    }
}