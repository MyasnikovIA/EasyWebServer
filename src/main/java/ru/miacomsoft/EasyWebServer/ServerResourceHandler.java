package ru.miacomsoft.EasyWebServer;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.util.structObject.JavaInnerClassObject;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.miacomsoft.EasyWebServer.HttpExchange.parseErrorRunJava;
import static ru.miacomsoft.EasyWebServer.ServerResource.componentListClass;
import static ru.miacomsoft.EasyWebServer.WebServer.WriteToFile;
import static ru.miacomsoft.EasyWebServer.WebServer.callbackProcedure;

/**
 * Обработчик HTTP-запросов.
 * Отвечает за:
 * - чтение заголовков запроса,
 * - определение типа запроса (GET/POST/TERM),
 * - обработку параметров,
 * - выбор ресурса для ответа,
 * - вызов callback-функций,
 * - отправку HTML-ответа клиенту.
 */
public class ServerResourceHandler implements Runnable {
    /**
     * Глобальный объект для хранения пользовательских данных на сервере.
     */
    public static JSONObject SERVER_SHARE = new JSONObject();

    /**
     * Кэш статических ресурсов (HTML, CSS, JS).
     */
    private final Map<String, Resource> resources = new HashMap<>();

    /**
     * Хранит дату последней модификации файлов.
     */
    private final Map<String, String> resourcesDateTime = new HashMap<>();

    /**
     * Список активных сессий.
     */
    public static HashMap<String, HashMap<String, Object>> sessionList = new HashMap<>();

    /**
     * Утилита для выполнения Java-кода из строки.
     */
    public static JavaStrExecut javaStrExecut = new JavaStrExecut();

    /**
     * Текущий объект запроса.
     */
    private HttpExchange query;

    /**
     * Конструктор.
     *
     * @param socket клиентский сокет
     * @throws IOException   если произошла ошибка при работе с сокетом
     * @throws JSONException если не удалось создать JSON-объект
     */
    public ServerResourceHandler(Socket socket) throws IOException, JSONException {
        this.query = new HttpExchange(socket, null);
    }

    /**
     * Метод запуска потока.
     * Вызывает обработчики запросов и завершает соединение.
     */
    @Override
    public void run() {
        try {
            if (readRequestHeader()) {
                sendResponseQuery();
            }
        } catch (Exception ex) {
            Logger.getLogger(ServerResourceHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                query.close();
                query = null;
            } catch (Exception ex) {
                Logger.getLogger(ServerResourceHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Расширяет массив символов, добавляя новые элементы.
     *
     * @param original   исходный массив
     * @param additional дополнительные данные
     * @param lenArr     количество добавляемых элементов
     * @return новый массив
     */
    public static char[] expandArray(char[] original, char[] additional, int lenArr) {
        char[] newArray = new char[original.length + lenArr];
        System.arraycopy(original, 0, newArray, 0, original.length);
        System.arraycopy(additional, 0, newArray, original.length, lenArr);
        return newArray;
    }

    /**
     * Ищет последовательность символов в массиве.
     *
     * @param buffer       массив для поиска
     * @param sequence     строка, которую ищем
     * @param lastPosition если true, возвращает позицию после найденной последовательности
     * @return индекс последовательности или -1
     */
    public static int findSequence(char[] buffer, String sequence, boolean lastPosition) {
        char[] seqChars = sequence.toCharArray();
        for (int i = 0; i <= buffer.length - seqChars.length; i++) {
            boolean found = true;
            for (int j = 0; j < seqChars.length; j++) {
                if (buffer[i + j] != seqChars[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return lastPosition ? i + sequence.length() : i;
            }
        }
        return -1;
    }

    private boolean readRequestHeader() throws IOException {
        int charInt;
        StringBuffer sb = new StringBuffer();
        StringBuffer sbTmp = new StringBuffer();
        // читаем заголовок HTML запроса
        char[] bufferHead = new char[1024];
        char[] bufferArrHead = new char[0];
        char[] bufferArrPost = new char[0];
        while ((charInt = query.inputStreamReader.read(bufferHead)) > 0) {
            bufferArrHead = expandArray(bufferArrHead, bufferHead, charInt);
            //String strHead = new String(bufferArrHead);
            int index = findSequence(bufferArrHead, "\r\r", true);
            if (index == -1) {
                index = findSequence(bufferArrHead, "\r\n\r\n", true);
            }
            if (index == -1) {
                index = findSequence(bufferArrHead, "\n\n", true);
            }
            if (index != -1) {
                char[] beforeSequence = new char[index];
                bufferArrPost = new char[bufferArrHead.length - index];
                // Копируем данные до последовательности
                System.arraycopy(bufferArrHead, 0, beforeSequence, 0, index);
                sb.append(new String(beforeSequence));
                // Копируем данные после последовательности и помещаем его в тело post запроса
                System.arraycopy(bufferArrHead, index, bufferArrPost, 0, bufferArrPost.length);
                break;
            }
        }

        query.typeQuery = "GET";
        query.headSrc = sb;
        if ((sb.toString().toLowerCase().indexOf("content-length: ")) != -1) {
            // Читаем тело POST запроса
            String sbTmp2 = "0";
            if (sb.toString().indexOf("Content-Length: ") != -1) {
                sbTmp2 = sb.toString().substring(sb.toString().indexOf("Content-Length: ") + "Content-Length: ".length(), sb.toString().length());
            } else if ((sb.toString().toLowerCase().indexOf("content-length: ")) != -1) {
                sbTmp2 = sb.toString().substring(sb.toString().indexOf("content-length: ") + "Content-Length: ".length(), sb.toString().length());
            }
            String lengPostStr = sbTmp2;
            if (sbTmp2.indexOf("\n") != -1) {
                lengPostStr = sbTmp2.substring(0, sbTmp2.indexOf("\n")).replace("\r", "");
            }
            int LengPOstBody = Integer.valueOf(lengPostStr);
            if (LengPOstBody > 0) {
                int maxArrBuf = 1024;
                if (maxArrBuf > LengPOstBody) {
                    maxArrBuf = LengPOstBody;
                }
                int syzeDifference = LengPOstBody - bufferArrPost.length;
                if ((syzeDifference == 4) || (syzeDifference == 2)) {
                    //todo:
                    // Подумать как переписать этот костыль.
                } else if (bufferArrPost.length < LengPOstBody) {
                    char[] bufferArr = new char[maxArrBuf];
                    boolean isBreak = false;
                    while ((charInt = query.inputStreamReader.read(bufferArr)) > 0 && isBreak == false) {
                        bufferArrPost = expandArray(bufferArrPost, bufferArr, charInt);
                        if (charInt < bufferArr.length) {
                            break;
                        }
                        if (query.socket.isConnected() == false) {
                            return false;
                        }
                        if (bufferArrPost.length > LengPOstBody) {
                            isBreak = true;
                            break;
                        }
                    }
                }
                query.postCharBody = bufferArrPost;
                query.postByte = new String(bufferArrPost).getBytes(StandardCharsets.UTF_8);
                query.typeQuery = "POST";
                if (query.headers.containsKey("Content-Type")) {
                    String contentType = ((String) query.headers.get("Content-Type")).toLowerCase();
                    switch (contentType) {
                        case "application/json":
                            // Если тело — JSON, можно попытаться распарсить
                            try {
                                query.messageJson = new JSONObject(bufferArrPost);
                            } catch (JSONException ignored) {
                                /// todo: вернуть ошибку парсинга в JSON объект
                                ignored.printStackTrace();
                            }
                            break;
                        case "application/x-www-form-urlencoded":
                            for (String param : new String(bufferArrPost).split("&")) {
                                if (param.isEmpty()) continue;
                                String[] keyValue = param.split("=", 2);
                                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                                try {
                                    query.requestParam.put(key, value);
                                    JSONObject obj = new JSONObject();
                                    obj.put(key, value);
                                    query.requestParamArray.put(obj);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        default:
                            /// todo: уточнить какие бывают форматы и дописать их обработку
                    }
                    ;
                }
            }
        }
        // Парсим заголовок
        int indLine = 0;
        for (String titleLine : sb.toString().split("\r")) {
            titleLine = titleLine.replace("\n", "");
            indLine++;
            if (indLine == 1) {
                int lenTitle = titleLine.length();
                titleLine.substring(0, titleLine.indexOf(" "));
                query.typeQuery = titleLine.substring(0, titleLine.indexOf(" "));
                titleLine = titleLine.substring(titleLine.indexOf(query.typeQuery) + query.typeQuery.length() + 2);
                if (titleLine.indexOf(" HTTP/") != -1) {
                    titleLine = titleLine.substring(0, titleLine.length() - (titleLine.substring(titleLine.lastIndexOf(" HTTP/"))).length());
                }
                if (titleLine.trim().length() == 0) {
                    titleLine = ServerConstant.config.INDEX_PAGE;
                }
                titleLine = URLDecoder.decode(titleLine, StandardCharsets.UTF_8.toString());
                query.requestText = titleLine;
                query.requestPath = titleLine;

                // Разбираем параметры из URI
                if (titleLine.contains("?")) {
                    String[] parts = titleLine.split("\\?", 2);
                    query.requestPath = parts[0];

                    for (String param : parts[1].split("&")) {
                        if (param.isEmpty()) continue;
                        String[] keyValue = param.split("=", 2);
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                        String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                        try {
                            query.requestParam.put(key, value);
                            JSONObject obj = new JSONObject();
                            obj.put(key, value);
                            query.requestParamArray.put(obj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    query.requestPath = query.requestText.substring(0, query.requestText.indexOf("?"));
                }
            } else {
                if (titleLine.indexOf(":") != -1) {
                    String key = titleLine.substring(0, titleLine.indexOf(":")).trim();
                    String val = titleLine.substring(titleLine.indexOf(":") + 1).trim();
                    query.headers.put(key, val);
                    switch (key) {
                        case "Cookie":
                            for (String elem : val.split("; ")) {
                                String[] valSubArr = elem.split("=");
                                if (valSubArr.length == 2) {
                                    query.cookie.put(valSubArr[0], valSubArr[1]);
                                    if ((valSubArr[0].trim()).toLowerCase().equals("session")) {
                                        query.sessionID = valSubArr[1];
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }
        query.expansion = getFileExt(query.requestPath).toLowerCase();
        query.session = getSession(query); // генерация или зпгрузка старой сессии
        if (query.typeQuery.toUpperCase().equals("TERM")) { // Обработка запроса с терминала
            sendResponseTerminal();
            return false;
        }
        query.mimeType = getFileMime(query.requestPath);
        return true;
    }

    /**
     * Полностью переписанная реализация чтения HTTP-запроса.
     * Поддерживает:
     * - GET / POST / PUT / PATCH / DELETE / HEAD / OPTIONS / TRACE / CONNECT / TERM
     * - Чтение заголовков, GET-параметров, Cookie
     * - Чтение тела запроса (URL-encoded или JSON)
     *
     * @return true, если запрос успешно прочитан, false — если это не HTTP-запрос
     * @throws IOException при ошибках ввода-вывода
     */
    private boolean readRequestHeaderOld() throws IOException {
        // Сюда будем читать весь заголовок
        StringBuilder headerBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;

        while ((bytesRead = query.inputStreamReader.read(buffer)) > 0) {
            headerBuilder.append(buffer, 0, bytesRead);

            // Если найдены два CRLF подряд, значит заголовок закончился
            if (headerBuilder.indexOf("\r\n\r\n") != -1 || headerBuilder.indexOf("\n\n") != -1) {
                break;
            }

            // Ограничиваем размер буфера, чтобы избежать DoS
            if (headerBuilder.length() > ServerConstant.config.MAX_HEADER_SIZE) {
                throw new IOException("HTTP Header too large");
            }
        }

        String rawHeader = headerBuilder.toString();

        // Проверяем, что это действительно HTTP-запрос
        if (!rawHeader.matches("(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|TRACE|CONNECT|TERM)\\s+.*")) {
            return false;
        }

        // Разбиваем заголовок по строкам
        String[] lines = rawHeader.split("\\r\\n|\\n");

        // Первая строка: метод, путь, версия
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) {
            throw new IOException("Invalid HTTP request line");
        }

        // Сохраняем тип запроса
        query.typeQuery = requestLine[0].toUpperCase(); // GET / POST / PUT / PATCH / DELETE / HEAD / OPTIONS / TRACE / CONNECT / TERM

        // URI и протокол
        String uri = requestLine[1];                    // URI
        String protocol = requestLine.length > 2 ? requestLine[2] : "HTTP/1.1"; // протокол

        // Декодируем URI
        try {
            uri = URLDecoder.decode(uri, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IOException("UTF-8 not supported", e);
        }

        // Сохраняем URI
        query.requestText = uri;
        query.requestPath = uri;

        // Разбираем параметры из URI
        if (uri.contains("?")) {
            String[] parts = uri.split("\\?", 2);
            query.requestPath = parts[0];

            for (String param : parts[1].split("&")) {
                if (param.isEmpty()) continue;
                String[] keyValue = param.split("=", 2);
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                String value = keyValue.length > 1 ?
                        URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                try {
                    query.requestParam.put(key, value);
                    JSONObject obj = new JSONObject();
                    obj.put(key, value);
                    query.requestParamArray.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Парсим остальные заголовки
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex == -1) continue;

            String headerName = line.substring(0, colonIndex).trim();
            String headerValue = line.substring(colonIndex + 1).trim();

            query.headers.put(headerName, headerValue);

            // Обработка Cookie
            if ("Cookie".equalsIgnoreCase(headerName)) {
                for (String cookiePair : headerValue.split(";")) {
                    String[] pair = cookiePair.trim().split("=", 2);
                    if (pair.length == 2) {
                        query.cookie.put(pair[0], pair[1]);
                        if ("session".equalsIgnoreCase(pair[0])) {
                            query.sessionID = pair[1];
                        }
                    }
                }
            }
        }

        // Устанавливаем MIME-тип ответа
        query.mimeType = getFileMime(query.requestPath);

        // Обработка тела запроса (только для POST, PUT, PATCH)
        if (Arrays.asList("POST", "PUT", "PATCH").contains(query.typeQuery) && query.headers.containsKey("Content-Length")) {
            try {
                long contentLength = Long.parseLong((String) query.headers.get("Content-Length"));
                if (contentLength > 0) {
                    ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
                    byte[] bodyBuffer = new byte[4096];
                    int totalRead = 0;

                    while (totalRead < contentLength && (bytesRead = query.socket.getInputStream().read(bodyBuffer)) > 0) {
                        bodyStream.write(bodyBuffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }

                    String body = bodyStream.toString(StandardCharsets.UTF_8.name());

                    // Сохраняем как raw строка и как байты
                    query.postCharBody = body.toCharArray();
                    query.postByte = body.getBytes(StandardCharsets.UTF_8);

                    // Если тело — JSON, можно попытаться распарсить
                    if ("application/json".equalsIgnoreCase((String) query.headers.get("Content-Type"))) {
                        try {
                            query.messageJson = new JSONObject(body);
                        } catch (JSONException ignored) {
                        }
                    } else {
                        // Для application/x-www-form-urlencoded
                        for (String param : body.split("&")) {
                            if (param.isEmpty()) continue;
                            String[] keyValue = param.split("=", 2);
                            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                            String value = keyValue.length > 1 ?
                                    URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                            try {
                                query.requestParam.put(key, value);
                                JSONObject obj = new JSONObject();
                                obj.put(key, value);
                                query.requestParamArray.put(obj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Content-Length");
            }
        }
        // Получаем или создаём сессию
        query.session = getSession(query);
        if (query.typeQuery.toUpperCase().equals("TERM")) { // Обработка запроса с терминала
            sendResponseTerminal();
            return false;
        }
        return true;
    }


    /**
     * Обрабатывает запрос от терминала.
     */
    private void sendResponseTerminal() {
        if (!javaStrExecut.runJavaTerminalFile(query)) {

        }
    }

    /**
     * Отправляет HTTP-ответ клиенту (браузеру).
     */
    /**
     * Процедура отправки ответа клиенту (браузеру)
     */
    private void sendResponseQuery() {
        // Установка MIME-типа
        setMimeType();

        // Обработка callback
        if (callbackProcedure != null) {
            callbackProcedure.call(query);
            return;
        }

        // Установка режима отладки
        setDebugMode();

        String ext = getFileExt(query.requestPath).toLowerCase();

        // Обработка компонентов
        if (query.requestPath.contains("{component}")) {
            ServerResourceHandler.javaStrExecut.runJavaServerlet(query);
            return;
        } else if ("component".equals(ext)) {
            ServerResourceHandler.javaStrExecut.runJavaComponent(query);
            return;
        }

        // Обработка основного запроса
        processMainRequest();
    }

// Вспомогательные методы

    private void setMimeType() {
        query.mimeType = getFileMime(query.requestPath);
        if (query.requestParam.has("mime")) {
            query.mimeType = getFileMime(query.requestParam.getString("mime"));
        } else if (query.requestParam.has("mimetype")) {
            query.mimeType = query.requestParam.getString("mimetype");
        }
    }

    private void setDebugMode() {
        if (query.requestParam.has("debug")) {
            ServerConstant.config.DEBUG = "1".equals(query.requestParam.getString("debug"));
        }
    }

    private void processMainRequest() {
        String resourcePath = buildResourcePath();
        File file = new File(resourcePath);

        // Проверка существования файла в системном каталоге
        if (!file.exists() && !ServerConstant.config.WEBAPP_SYSTEM_DIR.isEmpty()) {
            resourcePath = ServerConstant.config.WEBAPP_SYSTEM_DIR + "/" + query.requestPath;
            file = new File(resourcePath);
        }

        // Обработка разных типов ресурсов
        if (ServerResource.pagesJavaInnerClass.containsKey(query.requestPath)) {
            processJavaInnerClass();
        } else if (ServerResource.pagesListContent.containsKey(query.requestPath)) {
            query.sendHtml(ServerResource.pagesListContent.get(query.requestPath).toString());
        } else if (ServerResource.pagesList.containsKey(query.requestPath)) {
            processPageListResource(resourcePath);
        } else if (file.exists()) {
            processFileResource(file, resourcePath);
        } else {
            handleNotFound();
        }
    }

    private String buildResourcePath() {
        return ServerResource.pagesListFile.containsKey(query.requestPath)
                ? ServerResource.pagesListFile.get(query.requestPath).getAbsolutePath()
                : ServerConstant.config.WEBAPP_DIR + "/" + query.requestPath;
    }

    private void processJavaInnerClass() {
        JavaInnerClassObject page = ServerResource.pagesJavaInnerClass.get(query.requestPath);
        query.mimeType = page.mime;

        try {
            Object result = page.method.invoke(page.ObjectInstance, query);
            byte[] messageBytes = (page.queryType == null)
                    ? (byte[]) result
                    : ((JSONObject) result).toString().getBytes();

            if (messageBytes != null) {
                query.sendHtml(new String(messageBytes));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            query.sendHtml(parseErrorRunJava(e));
        }
    }

    private void processPageListResource(String resourcePath) {
        byte[] res = ServerResource.pagesList.get(query.requestPath).call(query);
        if (res != null) {
            if ("html".equals(query.mimeType)) {
                res = readResourceContent(new String(res), resourcePath,
                        ServerConstant.config.WEBAPP_DIR, "");
            }
            query.sendHtml(new String(res));
        }
    }

    private void processFileResource(File file, String resourcePath) {
        String ext = getFileExt(query.requestPath).toLowerCase();

        if ("java".equals(ext)) {
            ServerResourceHandler.javaStrExecut.runJavaFile(query);
            return;
        }

        if (file.length() > ServerConstant.config.LENGTH_CAHE) {
            query.sendByteFile(file);
            return;
        }

        String lastModified = String.valueOf(file.lastModified());
        Resource res;

        if (!ServerConstant.config.DEBUG) {
            res = getCachedResource(file, resourcePath, lastModified);
        } else {
            res = new Resource(readResource(file, query, resourcePath, ServerConstant.config.WEBAPP_DIR, ""));
        }

        query.sendHtml(new String(res.content));
    }

    private Resource getCachedResource(File file, String resourcePath, String lastModified) {
        if (resources.get(resourcePath) == null ||
                !resourcesDateTime.get(resourcePath).equals(lastModified)) {
            Resource newRes = new Resource(readResource(file, query, resourcePath,
                    ServerConstant.config.WEBAPP_DIR, ""));
            resources.put(resourcePath, newRes);
            resourcesDateTime.put(resourcePath, lastModified);
            return newRes;
        }
        return resources.get(resourcePath);
    }

    private void handleNotFound() {
        query.mimeType = "text/html";
        String page404Path = ServerConstant.config.PAGE_404;

        if (page404Path.isEmpty()) {
            query.sendHtml("Page not found");
        } else {
            File user404 = new File(ServerConstant.config.WEBAPP_DIR + page404Path);
            File system404 = new File(ServerConstant.config.WEBAPP_SYSTEM_DIR + page404Path);

            if (user404.exists()) {
                query.sendFile(user404.getAbsolutePath());
            } else if (system404.exists()) {
                query.sendFile(system404.getAbsolutePath());
            } else {
                query.sendHtml("Page not found");
            }
        }

        logNotFoundRequest();
    }

    private void logNotFoundRequest() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        String timestamp = dtf.format(LocalDateTime.now());

        String logMessage = String.format(
                "------------------------------------%n" +
                        "%s (NSO +7)%n" +
                        "query.requestPath %s%n" +
                        "query.requestParam %s%n" +
                        "%s%n" +
                        "------------------------------------%n",
                timestamp,
                query.requestPath,
                query.requestParam,
                query.headSrc.toString()
        );

        WriteToFile(logMessage);
    }

    /**
     * Парсит содержимое HTML-файла и заменяет специальные теги.
     *
     * @param fileContent  исходный HTML
     * @param resourcePath путь к файлу
     * @param rootPath     корневая директория сайта
     * @param fragmentName имя фрагмента (опционально)
     * @return байты обработанного контента
     */
    private byte[] readResourceContent(String fileContent, String resourcePath, String rootPath, String fragmentName) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            Document doc = Jsoup.parse(fileContent);
            Element elsDst;

            if (fragmentName.isEmpty()) {
                Element els = doc.getElementsByTag("body").get(0);
                doc.attr("doc_path", resourcePath);
                doc.attr("rootPath", rootPath);
                parseElementV2(doc, els, null);
                doc.removeAttr("doc_path");
                doc.removeAttr("rootPath");
                elsDst = doc.getElementsByTag("html").get(0);
            } else {
                Elements elements = doc.select("[name=" + fragmentName + "]");
                elsDst = doc;
                for (Element element : elements) {
                    parseElementV2(doc, element, null);
                }
            }

            bout.write(elsDst.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bout.toByteArray();
    }

    /**
     * Читает ресурс (файл) и возвращает его в виде байтов.
     *
     * @param file         файл
     * @param query        объект запроса
     * @param resourcePath путь к файлу
     * @param rootPath     корневая директория
     * @param fragmentName имя фрагмента (опционально)
     * @return байты файла
     */
    private byte[] readResource(File file, HttpExchange query, String resourcePath, String rootPath, String fragmentName) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            String ext = getFileExt(resourcePath).toLowerCase();
            if ("html".equals(ext)) {
                Path path = Paths.get(resourcePath);
                byte[] bytes = Files.readAllBytes(path);
                Document doc = Jsoup.parse(new String(bytes));
                Element els = doc.getElementsByTag("body").get(0);
                doc.attr("doc_path", resourcePath);
                doc.attr("rootPath", rootPath);
                parseElementV2(doc, els, null);
                doc.removeAttr("doc_path");
                doc.removeAttr("rootPath");
                doc.getElementsByTag("body").get(0).replaceWith(els);
                Element elsDst = doc.getElementsByTag("html").get(0);
                bout.write(elsDst.toString().getBytes());
            } else {
                InputStream in = new FileInputStream(resourcePath);
                byte[] bs = new byte[4096];
                int lenReadByts;
                while ((lenReadByts = in.read(bs)) >= 0) {
                    bout.write(bs, 0, lenReadByts);
                }
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bout.toByteArray();
    }

    /**
     * Возвращает сессию по ID.
     *
     * @param sessionKey ID сессии
     * @return объект сессии
     */
    public static HashMap<String, Object> getSession(String sessionKey) {
        return sessionList.get(sessionKey);
    }

    /**
     * Получает или создаёт новую сессию.
     *
     * @param httpExchange текущий запрос
     * @return объект сессии
     */
    public static HashMap<String, Object> getSession(HttpExchange httpExchange) {
        HashMap<String, Object> userSession;
        if (httpExchange.sessionID.isEmpty()) {
            UUID uuid = UUID.randomUUID();
            httpExchange.sessionID = uuid.toString();
            httpExchange.responseHeaders.put("Set-Cookie", "session=" + uuid + "; debug=" + ServerConstant.config.DEBUG + ";");
        }
        if (!sessionList.containsKey(httpExchange.sessionID)) {
            userSession = new HashMap<>();
            sessionList.put(httpExchange.sessionID, userSession);
        } else {
            userSession = sessionList.get(httpExchange.sessionID);
        }
        return userSession;
    }

    /**
     * Возвращает MIME-тип по расширению файла.
     *
     * @param path путь к файлу
     * @return MIME-тип
     */
    public static String getFileMime(String path) {
        String ext = getFileExt(path).toLowerCase();
        return ServerConstant.config.MIME_MAP.getOrDefault(ext, ServerConstant.config.APPLICATION_OCTET_STREAM);
    }

    /**
     * Возвращает расширение файла.
     *
     * @param path путь к файлу
     * @return расширение
     */
    public static String getFileExt(String path) {
        int dotIndex = path.lastIndexOf('.');
        return (dotIndex >= 0) ? path.substring(dotIndex + 1) : "";
    }

    /**
     * Структура для хранения статического контента.
     */
    private static class Resource {
        public final byte[] content;
        public String mimeType;

        public Resource(byte[] content) {
            this.content = content;
            this.mimeType = "text/html";
        }

        public Resource(byte[] content, String mimeType) {
            this.content = content;
            this.mimeType = mimeType;
        }
    }


    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    /**
     * Парсит XML-структуру и заменяет специальные теги.
     *
     * @param doc           документ
     * @param element       элемент
     * @param elementParent родительский элемент
     */
    public static void parseElementV2(Document doc, Element element, Element elementParent) {
        String tagName = element.tag().getName();
        if (!tagName.equals("cmpform") && tagName.length() > 3) {
            if (tagName.substring(0, 3).equals("cmp")) {
                try {
                    if (ServerConstant.config.COMPONENT_PATH.length() == 0) {
                        ServerConstant.config.COMPONENT_PATH = ServerResourceHandler.class.getPackage().getName() + ".component";
                    }
                    String classNameCmp = ServerConstant.config.COMPONENT_PATH + ".cmp" + tagName.substring(3, 4).toUpperCase() + tagName.substring(4);
                    Class classComponent = componentListClass.getOrDefault(classNameCmp, Class.forName(classNameCmp));
                    if (!componentListClass.containsKey(classNameCmp)) {
                        componentListClass.put(classNameCmp, classComponent);
                    }
                    Constructor constructor = classComponent.getConstructor(Document.class, Element.class, String.class);
                    Object newClassObject = constructor.newInstance(doc, element, tagName);

                    // заменить старый тэг новым (сгенерированным)
                    element.replaceWith((Element) newClassObject);
                } catch (ClassNotFoundException e) {
                    Element errBody = new Element("error");
                    errBody.text("not found " + ServerConstant.config.COMPONENT_PATH + ".cmp" + tagName.substring(3, 4).toUpperCase() + tagName.substring(4));
                    element.replaceWith(errBody);
                } catch (Exception e) {
                    Element errBody = new Element("error");
                    errBody.text(e.getLocalizedMessage() + " : " + e.getMessage() + ": \n " + parseErrorRunJava(e));
                    element.replaceWith(errBody);
                }
                return;
            }
        }
        for (Attribute attr : element.attributes().asList()) {
            //  System.out.println("\t\t" + attr.getKey() + " = " + attr.getValue());
        }
        if (element.hasText()) {
            //  System.out.println(element.tagName() + " element.text().trim() " + element.text().trim());
        }
        if (element.childNodeSize() == 0) {

        } else {
            int childrenint = 0;
            for (int numChildNode = 0; numChildNode < element.childNodeSize(); numChildNode++) {
                // System.out.println(numChildNode + ")  " + element.childNode(numChildNode).nodeName() + " element.childrenSize() " + element.childrenSize() + " numChildNode " + numChildNode);
                if (element.childNode(numChildNode).nodeName().equals("#text")) {

                } else if (element.childNode(numChildNode).nodeName().equals("#comment")) {

                } else if (element.childNode(numChildNode).nodeName().equals("#cdata")) {

                } else {
                    if (childrenint < element.childrenSize()) {
                        parseElementV2(doc, element.child(childrenint), element);
                        childrenint++;
                    }
                }
            }
        }
        if (elementParent == null) {

        }
    }


    /**
     * Перегруженный метод для упрощения вызова.
     */
    public static void parseElementV2(Document doc, Element element) {
        parseElementV2(doc, element, null);
    }

    /**
     * Парсит строку как элемент.
     */
    public static Element parseElementV2(Document doc, String elementText) {
        Element el = new Element(elementText);
        parseElementV2(doc, el, null);
        return el;
    }

    /**
     * Парсит подформу из внешнего файла.
     */
    public static String parseSubElementV2(Document doc, String path, String SelectorQuery) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(doc.attr("rootPath") + path))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) > 0) {
                sb.append((char) c);
            }
            return parseStrElementV2(doc, sb.toString(), SelectorQuery);
        } catch (IOException e) {
            return "Error reading subform: " + e.getMessage();
        }
    }

    /**
     * Парсит строку как HTML и применяет правила замены.
     */
    public static String parseStrElementV2(Document doc, String htmlText, String SelectorQuery) {
        Document docForm = Jsoup.parse(htmlText);
        if (SelectorQuery.isEmpty()) {
            Element els = docForm.getElementsByTag("body").get(0);
            parseElementV2(docForm, els, null);
            return els.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Element elOne : doc.select(SelectorQuery)) {
                parseElementV2(docForm, elOne, null);
                sb.append(elOne.toString());
            }
            return sb.toString();
        }
    }
}