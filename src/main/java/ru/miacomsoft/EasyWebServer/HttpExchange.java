package ru.miacomsoft.EasyWebServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        if (!ServerConstant.config.WEBAPP_DIRS.isEmpty()) {
            this.webappDir = ServerConstant.config.WEBAPP_DIRS.get(0);
        } else {
            this.webappDir = ServerConstant.config.WEBAPP_DIR;
        }

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
    public void sendFile(File file) {
        String filename = file.getName().toLowerCase();
        String mimeType = ServerConstant.config.MIME_MAP.getOrDefault(
                getFileExtension(filename),
                "application/octet-stream"
        );
        sendFile(file, mimeType);
    }
    public void sendFile(File file, String contentType) {
        try (InputStream in = new FileInputStream(file);
             OutputStream out = socket.getOutputStream()) {

            // Подготавливаем заголовки
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            headers.write("HTTP/1.1 200 OK\r\n".getBytes());
            headers.write(("Content-Type: " + contentType + "\r\n").getBytes());
            headers.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            headers.write("Accept-Ranges: bytes\r\n".getBytes());

            // Для изображений добавляем кеширование
            if (contentType.startsWith("image/")) {
                headers.write("Cache-Control: public, max-age=86400\r\n".getBytes());
            }

            // Пользовательские заголовки
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                headers.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes());
            }

            headers.write("\r\n".getBytes());

            // Отправляем заголовки и файл
            out.write(headers.toByteArray());

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Для бинарного контента нужно отправлять с правильными заголовками
        if (this.mimeType != null && this.mimeType.startsWith("text/")) {
            // Текстовый контент - используем существующую логику
            sendHtml(new String(content, StandardCharsets.UTF_8));
        } else {
            // Бинарный контент - отправляем с заголовками
            sendResponse(content, false);
        }
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
        try (InputStream in = new FileInputStream(file);
             OutputStream out = socket.getOutputStream()) {

            // Определяем MIME-тип по расширению файла
            String fileExtension = getFileExtension(file.getName());
            String contentType = ServerConstant.config.MIME_MAP.getOrDefault(
                    fileExtension.toLowerCase(),
                    "application/octet-stream"
            );

            // Отправляем заголовки
            out.write("HTTP/1.1 200 OK\r\n".getBytes());
            out.write(("Content-Type: " + contentType + "\r\n").getBytes());
            out.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            out.write(("Accept-Ranges: bytes\r\n").getBytes());

            // Добавляем кеширование для изображений
            if (fileExtension.matches("(?i)(jpg|jpeg|png|gif|ico|svg|webp)")) {
                out.write("Cache-Control: public, max-age=86400\r\n".getBytes());
            }

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

    // Вспомогательный метод для получения расширения файла
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1) : "";
    }
    public void sendImageFile(File file) {
        try (InputStream in = new FileInputStream(file);
             OutputStream out = socket.getOutputStream()) {

            String filename = file.getName().toLowerCase();
            String contentType;

            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (filename.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else {
                contentType = "application/octet-stream";
            }

            // Отправляем заголовки
            out.write("HTTP/1.1 200 OK\r\n".getBytes());
            out.write(("Content-Type: " + contentType + "\r\n").getBytes());
            out.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            out.write(("Accept-Ranges: bytes\r\n").getBytes());
            out.write("Cache-Control: public, max-age=86400\r\n".getBytes());

            out.write("\r\n".getBytes());

            // Отправляем файл
            byte[] buffer = new byte[8192];
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
        String tmpStr = "";
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

    /**
     * Выполняет SQL запрос и возвращает результат в формате JSONArray
     * Автоматически создает схему в PostgreSQL, если она не существует
     *
     * @param sql SQL запрос для выполнения
     * @return результат запроса в формате JSONArray
     */
    public JSONArray SQL(String sql) {
        JSONArray result = new JSONArray();
        Connection conn = null;

        try {
            // Проверяем наличие сессии и DATABASE
            if (session == null || !session.containsKey("DATABASE")) {
                System.err.println("No database connection available in session");
                return result; // Возвращаем пустой результат без ошибки
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> dataBase = (Map<String, Object>) session.get("DATABASE");

            if (dataBase.containsKey("CONNECT")) {
                conn = (Connection) dataBase.get("CONNECT");
            } else {
                // Проверяем наличие данных для подключения
                if (!dataBase.containsKey("DATABASE_USER_NAME") || !dataBase.containsKey("DATABASE_USER_PASS")) {
                    System.err.println("Database credentials not found in session");
                    return result;
                }

                String user = (String) dataBase.get("DATABASE_USER_NAME");
                String pass = (String) dataBase.get("DATABASE_USER_PASS");

                conn = PostgreQuery.getConnect(user, pass);
                if (conn != null) {
                    // Проверяем и создаем схему если нужно
                    ensureSchemaExists(conn, sql);
                    dataBase.put("CONNECT", conn);
                } else {
                    System.err.println("Failed to connect to database");
                    return result;
                }
            }

            if (conn == null || conn.isClosed()) {
                // Пытаемся переподключиться
                if (!dataBase.containsKey("DATABASE_USER_NAME") || !dataBase.containsKey("DATABASE_USER_PASS")) {
                    System.err.println("Cannot reconnect - missing credentials");
                    return result;
                }

                String user = (String) dataBase.get("DATABASE_USER_NAME");
                String pass = (String) dataBase.get("DATABASE_USER_PASS");
                conn = PostgreQuery.getConnect(user, pass);

                if (conn == null) {
                    System.err.println("Failed to reconnect to database");
                    return result;
                }

                dataBase.put("CONNECT", conn);

                // Проверяем и создаем схему если нужно
                ensureSchemaExists(conn, sql);
            }

            // Выполняем SQL запрос
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

            // Проверяем, не связана ли ошибка с отсутствием схемы и есть ли соединение
            if (conn != null && e.getMessage() != null && e.getMessage().contains("schema") &&
                    (e.getMessage().contains("does not exist") || e.getMessage().contains("not found"))) {

                // Извлекаем имя схемы из сообщения об ошибке
                String schemaName = extractSchemaNameFromError(e.getMessage());
                if (schemaName != null && !schemaName.isEmpty()) {
                    try {
                        // Пытаемся создать схему
                        if (createSchemaIfNotExists(conn, schemaName)) {
                            // Повторяем запрос
                            return SQL(sql);
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to create schema: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Ловим любые другие ошибки, чтобы не нарушать работу без БД
            System.err.println("Unexpected error in SQL method: " + e.getMessage());
        }

        return result;
    }



    /**
     * Извлекает имя схемы из сообщения об ошибке PostgreSQL
     */
    private String extractSchemaNameFromError(String errorMessage) {
        if (errorMessage == null) return null;

        try {
            // Пример сообщения: ERROR: schema "schema_name" does not exist
            int startQuote = errorMessage.indexOf("\"");
            if (startQuote >= 0) {
                int endQuote = errorMessage.indexOf("\"", startQuote + 1);
                if (endQuote > startQuote) {
                    return errorMessage.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        return null;
    }

    /**
     * Создает схему в PostgreSQL, если она не существует
     */
    private boolean createSchemaIfNotExists(Connection conn, String schemaName) {
        // Проверяем наличие соединения
        if (conn == null || schemaName == null || schemaName.isEmpty()) {
            return false;
        }

        try {
            // Проверяем, что соединение открыто
            if (conn.isClosed()) {
                System.err.println("Connection is closed, cannot create schema");
                return false;
            }

            try (Statement stmt = conn.createStatement()) {
                // Проверяем существование схемы
                String checkSql = "SELECT 1 FROM information_schema.schemata WHERE schema_name = '" + schemaName + "'";
                try (ResultSet rs = stmt.executeQuery(checkSql)) {
                    if (!rs.next()) {
                        // Схема не существует - создаем её
                        String createSql = "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"";
                        stmt.executeUpdate(createSql);
                        System.out.println("Created schema: " + schemaName);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating schema: " + e.getMessage());
            // Не пробрасываем исключение дальше
        } catch (Exception e) {
            System.err.println("Unexpected error creating schema: " + e.getMessage());
        }
        return false;
    }


    /**
     * Проверяет существование схемы в SQL запросе и создает её при необходимости
     */
    private void ensureSchemaExists(Connection conn, String sql) {
        // Проверяем наличие соединения
        if (conn == null || sql == null || sql.isEmpty()) {
            return;
        }

        try {
            // Проверяем, что соединение открыто
            if (conn.isClosed()) {
                return;
            }

            // Ищем имена схем в SQL запросе (паттерн: schema_name.table_name)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:from|join|into|update|table)\\s+(?:([\"`]?)([\\w\\.]+?)\\1\\.)?([\"`]?)([\\w]+)\\3",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

            java.util.regex.Matcher matcher = pattern.matcher(sql);
            while (matcher.find()) {
                String possibleSchema = matcher.group(2);
                if (possibleSchema != null && !possibleSchema.isEmpty()) {
                    // Убираем кавычки если есть
                    possibleSchema = possibleSchema.replace("\"", "").replace("`", "");

                    // Проверяем, не является ли это частью сложного имени
                    if (!possibleSchema.contains(".")) {
                        createSchemaIfNotExists(conn, possibleSchema);
                    }
                }
            }

            // Дополнительно ищем явное указание схемы с кавычками
            pattern = java.util.regex.Pattern.compile(
                    "schema\\s+[\"`]?([\\w]+)[\"`]?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

            matcher = pattern.matcher(sql);
            while (matcher.find()) {
                String schemaName = matcher.group(1);
                createSchemaIfNotExists(conn, schemaName);
            }
        } catch (Exception e) {
            // Игнорируем все ошибки при проверке схем - не критично
            System.err.println("Error in ensureSchemaExists: " + e.getMessage());
        }
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
    /**
     * Получить режим отладки для текущей сессии
     * @return true если включен режим отладки
     */
    public boolean isDebugMode() {
        if (session != null && session.containsKey("debug_mode")) {
            return (boolean) session.get("debug_mode");
        }
        return false;
    }
}