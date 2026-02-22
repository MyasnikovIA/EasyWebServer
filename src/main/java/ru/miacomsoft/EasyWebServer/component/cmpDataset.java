package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;
import ru.miacomsoft.EasyWebServer.ServerResourceHandler;

import java.sql.*;
import java.util.*;

import static ru.miacomsoft.EasyWebServer.PostgreQuery.getConnect;
import static ru.miacomsoft.EasyWebServer.PostgreQuery.procedureList;


public class cmpDataset extends Base {

    // Кэш для хранения информации о существовании функций в БД
    private static Map<String, Boolean> functionExistsCache = new HashMap<>();

    // Добавляем поле для хранения режима отладки
    private boolean debugMode = false;

    // Конструктор с тремя параметрами
    public cmpDataset(Document doc, Element element, String tag) {
        super(doc, element, tag);
        // Сохраняем режим отладки из документа
        if (doc != null && doc.hasAttr("debug_mode")) {
            debugMode = Boolean.parseBoolean(doc.attr("debug_mode"));
        }
        initialize(doc, element);
    }

    // Конструктор с двумя параметрами
    public cmpDataset(Document doc, Element element) {
        super(doc, element, "teaxtarea");
        // Сохраняем режим отладки из документа
        if (doc != null && doc.hasAttr("debug_mode")) {
            debugMode = Boolean.parseBoolean(doc.attr("debug_mode"));
        }
        initialize(doc, element);
    }

    // Выносим общую логику инициализации в отдельный метод
    private void initialize(Document doc, Element element) {
        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();
        attrsDst.add("schema", "Dataset");
        String name = attrs.get("name");
        this.attr("name", name);
        attrsDst.add("name", name);
        this.initCmpType(element);

        String db = RemoveArrKeyRtrn(attrs, "DB", "DB");
        String query_type = "sql";
        if (element.attributes().hasKey("query_type")) {
            query_type = element.attributes().get("query_type");
        }

        // Получаем схему PostgreSQL (по умолчанию "public")
        String pgSchema = RemoveArrKeyRtrn(attrs, "schema", "public");
        attrsDst.add("pg_schema", pgSchema);

        // Исправленное формирование имени функции
        String docPath = doc != null ? doc.attr("doc_path") : "";
        String rootPath = doc != null ? doc.attr("rootPath") : "";
        String relativePath = "";
        if (docPath.length() > rootPath.length() && docPath.length() > 5) {
            relativePath = docPath.substring(rootPath.length(), docPath.length() - 5); // убираем .html/.java
        }
        // Заменяем все разделители путей на подчеркивания
        relativePath = relativePath.replaceAll("[/\\\\]", "_");
        // Убираем возможные лишние символы
        relativePath = relativePath.replaceAll("[^a-zA-Z0-9_]", "");

        String pathHash = getMd5Hash(relativePath);
        if (pathHash.length() > 8) {
            pathHash = pathHash.substring(0, 8); // Берем первые 8 символов хэша
        }

        // Убеждаемся, что имя функции не начинается с цифры
        if (pathHash.length() > 0 && Character.isDigit(pathHash.charAt(0))) {
            pathHash = "f" + pathHash; // Добавляем префикс 'f' если имя начинается с цифры
        }

        // Получаем имя файла без расширения
        String fileName = "";
        if (relativePath.lastIndexOf('/') > 0) {
            fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            if (fileName.lastIndexOf('.') > 0) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
        }
        String functionName = (pathHash + "_" + fileName + "_" + element.attr("name")).toLowerCase();

        // Дополнительная проверка - убеждаемся, что имя начинается с буквы или подчеркивания
        if (functionName.length() > 0 && Character.isDigit(functionName.charAt(0))) {
            functionName = "f_" + functionName;
        }

        // Ограничиваем длину имени функции (PostgreSQL ограничение 63 символа)
        if (functionName.length() > 60) {
            functionName = functionName.substring(0, 60);
        }

        this.attr("style", "display:none");
        this.attr("dataset_name", functionName);
        this.attr("name", element.attr("name"));

        StringBuffer jsonVar = new StringBuffer();
        ArrayList<String> jarResourse = new ArrayList<String>();
        ArrayList<String> importPacket = new ArrayList<String>();

        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            Attributes attrsItem = itemElement.attributes();
            if (itemElement.tag().toString().toLowerCase().indexOf("import") != -1) {
                if (attrsItem.hasKey("path")) {
                    jarResourse.add(attrsItem.get("path"));
                }
                if (attrsItem.hasKey("packet")) {
                    importPacket.add(attrsItem.get("packet"));
                }
            } else if (itemElement.tag().toString().toLowerCase().indexOf("var") != -1) {
                String nameItem = attrsItem.get("name");
                String src = RemoveArrKeyRtrn(attrsItem, "src", nameItem);
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");
                jsonVar.append("'" + nameItem + "':{");
                jsonVar.append("'src':'" + src + "',");
                jsonVar.append("'srctype':'" + srctype + "'");
                if (len.length()>0) jsonVar.append(",'len':'" + len + "'");
                if (defaultVal.length()>0) jsonVar.append(",'defaultVal':'" + defaultVal.replaceAll("'", "\\'") + "'");
                jsonVar.append("},");
            }
        }
        String jsonVarStr = jsonVar.toString();
        if (jsonVarStr.length()>0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }

        this.attr("vars", "{" + jsonVarStr + "}");
        this.attr("query_type", query_type);
        this.attr("db", db);

        if (element.hasText()) {
            if (query_type.equals("java")) {
                JSONObject infoCompile = new JSONObject();
                if (!ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse, element.text().trim(), infoCompile)) {
                    this.removeAttr("style");
                    this.html(ru.miacomsoft.EasyWebServer.JavaStrExecut.parseErrorCompile(infoCompile));
                    return;
                }
            } else if (query_type.equals("sql")) {
                String fullFunctionName = pgSchema + "." + functionName;

                // Проверяем, нужно ли создавать функцию
                boolean needToCreate = debugMode; // В режиме debug всегда создаем

                if (!needToCreate) {
                    // Проверяем кэш наличия функции
                    Boolean exists = functionExistsCache.get(fullFunctionName);
                    if (exists == null) {
                        // Если в кэше нет, делаем запрос к БД
                        exists = checkFunctionExistsInDB(fullFunctionName, pgSchema);
                        functionExistsCache.put(fullFunctionName, exists);
                        System.out.println("Function " + fullFunctionName + " exists in DB: " + exists);
                    }

                    // Если функции нет в БД, создаем её
                    needToCreate = !exists;
                }

                if (needToCreate) {
                    // Передаем debugMode в метод createSQL
                    createSQL(fullFunctionName, pgSchema, element, docPath + " (" + element.attr("name") + ")", debugMode);
                    // После создания обновляем кэш
                    functionExistsCache.put(fullFunctionName, true);
                } else {
                    System.out.println("Function " + fullFunctionName + " already exists, skipping creation");
                }
            }
        }
        this.text("");
        for (Attribute attr : element.attributes().asList()) {
            if ("error".equals(attr.getKey())) continue;
            this.removeAttr(attr.getKey());
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<script> $(function() {");
        sb.append("  D3Api.setDatasetAuto('" + name + "');");
        sb.append("}); </script>");
        if (doc != null) {
            Elements elements = doc.getElementsByTag("body");
            if (elements != null && elements.size() > 0) {
                elements.append(sb.toString());
            }
        }

        // Автоматическое подключение JavaScript библиотеки для cmpDataset
        if (doc != null) {
            Elements head = doc.getElementsByTag("head");

            // Проверяем, не подключена ли уже библиотека
            if (head != null && head.size() > 0) {
                Elements existingScripts = head.select("script[src*='cmpDataset_js']");
                if (existingScripts.isEmpty()) {
                    // Добавляем ссылку на JS библиотеку
                    String jsPath = "{component}/cmpDataset_js";
                    head.append("<script cmp=\"dataset-lib\" src=\"" + jsPath + "\" type=\"text/javascript\"></script>");
                    System.out.println("cmpDataset: JavaScript library auto-included for dataset: " + name);
                }
            }
        }
    }

    /**
     * Проверка существования функции в БД с использованием EXISTS с учетом схемы
     */
    private boolean checkFunctionExistsInDB(String fullFunctionName, String schema) {
        Connection conn = null;
        try {
            conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
            if (conn == null) return false;

            Statement stmt = conn.createStatement();
            // Извлекаем имя функции без схемы
            String functionName = fullFunctionName;
            if (fullFunctionName.contains(".")) {
                functionName = fullFunctionName.substring(fullFunctionName.lastIndexOf('.') + 1);
            }

            String sql = "SELECT EXISTS(SELECT 1 FROM pg_proc p " +
                    "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                    "WHERE n.nspname = '" + schema + "' AND p.proname = '" + functionName + "') AS function_exists";
            System.out.println("Checking function existence: " + sql);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error checking function existence: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Проверка существования функции в БД (для обратной совместимости)
     */
    private boolean functionExistsInDB(String functionName) {
        String fullFunctionName = ServerConstant.config.APP_NAME + "_" + functionName;
        return checkFunctionExistsInDB(fullFunctionName, "public");
    }

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json"; // Изменить mime ответа
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject vars;
        String postBodyStr = new String(query.postCharBody);

        System.out.println("=== cmpDataset onPage called ===");
        System.out.println("queryProperty: " + queryProperty.toString());
        System.out.println("postBodyStr: " + postBodyStr);

        // Парсим входные переменные
        if (postBodyStr.indexOf("{") == -1) {
            vars = new JSONObject();
            int indParam = 0;
            for (String par : postBodyStr.split("&")) {
                String[] val = par.split("=");
                if (val.length == 2) {
                    vars.put(val[0], val[1]);
                } else {
                    indParam++;
                    vars.put("param" + indParam, val[0]);
                }
            }
        } else {
            try {
                vars = new JSONObject(postBodyStr);
            } catch (Exception e) {
                vars = new JSONObject();
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
        }

        System.out.println("Parsed vars: " + vars.toString());

        JSONObject result = new JSONObject();
        result.put("data", new JSONArray());
        result.put("vars", vars); // Возвращаем vars в любом случае

        String query_type = queryProperty.optString("query_type", "sql");
        String dataset_name = queryProperty.optString("dataset_name", "");
        String pg_schema = queryProperty.optString("pg_schema", "public");

        // Формируем полное имя функции со схемой
        String fullDatasetName;
        if (dataset_name.contains(".")) {
            fullDatasetName = dataset_name; // уже содержит схему
        } else {
            fullDatasetName = pg_schema + "." + dataset_name;
        }

        System.out.println("Full dataset name: " + fullDatasetName);
        System.out.println("Query type: " + query_type);

        // Проверяем режим отладки из сессии
        boolean debugMode = false;
        if (query.session != null && query.session.containsKey("debug_mode")) {
            debugMode = (boolean) query.session.get("debug_mode");
        }

        if (ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.existJavaFunction(fullDatasetName)) {
            // Обработка Java функции
            try {
                JSONObject varFun = new JSONObject();
                Iterator<String> keys = vars.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = vars.get(key);
                    if (val instanceof JSONObject) {
                        JSONObject varOne = (JSONObject) val;
                        String value = "";

                        if (varOne.optString("srctype").equals("session")) {
                            if (session.containsKey(key)) {
                                value = String.valueOf(session.get(key));
                            } else {
                                value = varOne.optString("defaultVal", "");
                            }
                        } else {
                            value = varOne.optString("value", varOne.optString("defaultVal", ""));
                        }
                        varFun.put(key, value);
                    } else {
                        varFun.put(key, val.toString());
                    }
                }

                JSONArray dataRes = new JSONArray();
                JSONObject resFun = ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.runFunction(fullDatasetName, varFun, session, dataRes);

                if (resFun.has("JAVA_ERROR")) {
                    result.put("ERROR", resFun.get("JAVA_ERROR"));
                } else {
                    result.put("data", dataRes);
                }
            } catch (Exception e) {
                result.put("ERROR", "Java function error: " + e.getMessage());
                e.printStackTrace();
            }

        } else if (query_type.equals("sql")) {
            // Обработка SQL запроса
            Connection conn = null;
            CallableStatement selectFunctionStatement = null;
            ResultSet rs = null;

            try {
                if (!procedureList.containsKey(fullDatasetName)) {
                    result.put("ERROR", "Procedure not found: " + fullDatasetName);
                    System.err.println("Procedure not found: " + fullDatasetName);
                    return result.toString().getBytes();
                }

                System.out.println("Found procedure in list: " + fullDatasetName);

                HashMap<String, Object> param = procedureList.get(fullDatasetName);

                // Получаем соединение с БД
                if (session.containsKey("DATABASE")) {
                    HashMap<String, Object> data_base = (HashMap<String, Object>) session.get("DATABASE");

                    if (data_base.containsKey("CONNECT")) {
                        conn = (Connection) data_base.get("CONNECT");
                        // Проверяем, не закрыто ли соединение
                        try {
                            if (conn == null || conn.isClosed()) {
                                System.out.println("Connection is closed, reconnecting...");
                                conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")),
                                        String.valueOf(data_base.get("DATABASE_USER_PASS")));
                                if (conn != null) {
                                    data_base.put("CONNECT", conn);
                                }
                            }
                        } catch (SQLException e) {
                            System.err.println("Error checking connection: " + e.getMessage());
                            conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")),
                                    String.valueOf(data_base.get("DATABASE_USER_PASS")));
                            if (conn != null) {
                                data_base.put("CONNECT", conn);
                            }
                        }
                    } else {
                        System.out.println("Creating new connection...");
                        conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")),
                                String.valueOf(data_base.get("DATABASE_USER_PASS")));
                        if (conn != null) {
                            data_base.put("CONNECT", conn);
                        }
                    }
                } else {
                    // Используем системного пользователя
                    System.out.println("Using system connection...");
                    conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                            ServerConstant.config.DATABASE_USER_PASS);
                }

                if (conn == null) {
                    result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                    result.put("ERROR", "Database connection failed");
                    System.err.println("Database connection failed");
                    return result.toString().getBytes();
                }

                String prepareCall = (String) param.get("prepareCall");
                System.out.println("PrepareCall: " + prepareCall);

                selectFunctionStatement = conn.prepareCall(prepareCall);

                if (debugMode) {
                    result.put("SQL", ((String) param.get("SQL")).split("\n"));
                }

                List<String> varsArr = (List<String>) param.get("vars");
                System.out.println("Vars array: " + varsArr);

                // Устанавливаем параметры
                int ind = 0;
                for (String varNameOne : varsArr) {
                    ind++;
                    String valueStr = "";

                    if (vars.has(varNameOne)) {
                        Object varObj = vars.get(varNameOne);
                        if (varObj instanceof JSONObject) {
                            JSONObject varOne = (JSONObject) varObj;

                            if (varOne.optString("srctype").equals("session")) {
                                Object sessionVal = session.get(varNameOne);
                                if (sessionVal != null) {
                                    valueStr = String.valueOf(sessionVal);
                                } else {
                                    valueStr = varOne.optString("defaultVal", "");
                                }
                            } else {
                                valueStr = varOne.optString("value", varOne.optString("defaultVal", ""));
                            }
                        } else {
                            valueStr = varObj.toString();
                        }
                    }

                    System.out.println("Setting parameter " + ind + " (" + varNameOne + "): " + valueStr);
                    selectFunctionStatement.setString(ind, valueStr);
                }

                // Выполняем запрос с таймаутом
                System.out.println("Executing query...");
                boolean hasResults = selectFunctionStatement.execute();
                System.out.println("Query executed, hasResults: " + hasResults);

                while (hasResults) {
                    rs = selectFunctionStatement.getResultSet();
                    if (rs != null) {
                        if (rs.next()) {
                            String jsonResult = rs.getString(1);
                            System.out.println("JSON result: " + jsonResult);
                            try {
                                result.put("data", new JSONArray(jsonResult));
                            } catch (Exception e) {
                                System.err.println("Error parsing JSON result: " + e.getMessage());
                                result.put("data", new JSONArray());
                            }
                        } else {
                            System.out.println("No data in result set");
                            result.put("data", new JSONArray());
                        }
                        rs.close();
                        rs = null;
                    }
                    hasResults = selectFunctionStatement.getMoreResults();
                }

                System.out.println("Query completed successfully");

            } catch (SQLException e) {
                System.err.println("SQL Error: " + e.getMessage());
                e.printStackTrace();
                result.put("ERROR", "SQL Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                result.put("ERROR", "Error: " + e.getMessage());
            } finally {
                // Закрываем ресурсы
                try {
                    if (rs != null) rs.close();
                } catch (Exception e) {}
                try {
                    if (selectFunctionStatement != null) selectFunctionStatement.close();
                } catch (Exception e) {}
                // НЕ закрываем connection, так как он может использоваться повторно
            }
        } else {
            result.put("ERROR", "Unsupported query type: " + query_type);
        }

        String resultText = result.toString();
        System.out.println("Response: " + resultText);
        return resultText.getBytes();
    }

    /**
     * Вспомогательный метод для вычисления MD5 хэша
     * Возвращает строку, которая гарантированно начинается с буквы
     */
    private String getMd5Hash(String input) {
        if (input == null) return "f_empty";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String hash = sb.toString();
            // Убеждаемся, что хэш не начинается с цифры
            if (hash.length() > 0 && Character.isDigit(hash.charAt(0))) {
                return "f" + hash;
            }
            return hash;
        } catch (Exception e) {
            return "f_" + Integer.toHexString(input.hashCode());
        }
    }

    // Исправленный метод createSQL - поддержка INOUT параметров
    private void createSQL(String functionName, String schema, Element element, String fileName, boolean debugMode) {
        // Очищаем имя функции
        String cleanFunctionName = functionName;
        if (functionName.contains(".")) {
            cleanFunctionName = functionName.substring(functionName.lastIndexOf('.') + 1);
        }

        // Убеждаемся, что имя функции не начинается с цифры
        if (cleanFunctionName.length() > 0 && Character.isDigit(cleanFunctionName.charAt(0))) {
            cleanFunctionName = "f_" + cleanFunctionName;
        }

        if (procedureList.containsKey(functionName) && !debugMode) {
            // Если функция уже создана в БД и режим отладки отключен, тогда пропускаем создание новой функции
            return;
        }

        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
        if (conn == null) {
            System.err.println("Cannot connect to database for creating function");
            return;
        }

        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        StringBuffer varsOut = new StringBuffer();
        Attributes attrs = element.attributes();
        HashMap<String, Object> param = new HashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();
        List<String> varsOutArr = new ArrayList<>();
        String beforeCodeBloc = "";
        String declareBlocText = "";
        String afterCodeBloc = "";

        // Обрабатываем дочерние элементы
        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            String tagName = itemElement.tag().toString().toLowerCase();

            if (tagName.equals("before")) {
                // Блок BEFORE содержит код до основного запроса
                String beforeText = itemElement.text().trim();
                if (beforeText.length() > 0) {
                    // Проверяем наличие DECLARE блока
                    String lowerBefore = beforeText.toLowerCase();
                    if (lowerBefore.contains("declare") && lowerBefore.contains("begin")) {
                        int declarePos = lowerBefore.indexOf("declare");
                        int beginPos = lowerBefore.indexOf("begin", declarePos);

                        if (declarePos >= 0 && beginPos > declarePos) {
                            declareBlocText = beforeText.substring(declarePos + "declare".length(), beginPos).trim();
                            beforeCodeBloc = beforeText.substring(beginPos + "begin".length()).trim();

                            // Убираем END; если есть
                            if (beforeCodeBloc.toLowerCase().endsWith("end;")) {
                                beforeCodeBloc = beforeCodeBloc.substring(0, beforeCodeBloc.length() - 4).trim();
                            }
                        }
                    } else {
                        beforeCodeBloc = beforeText;
                    }
                }
                itemElement.text(""); // Очищаем после обработки
            } else if (tagName.equals("after")) {
                // Блок AFTER содержит код после основного запроса
                afterCodeBloc = itemElement.text().trim();
                itemElement.text(""); // Очищаем после обработки
            } else if (tagName.indexOf("var") != -1) {
                Attributes attrsItem = itemElement.attributes();
                String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String direction = RemoveArrKeyRtrn(attrsItem, "direction", "IN"); // IN, OUT, INOUT
                String typeVar = "VARCHAR";
                if (len.length() > 0) {
                    typeVar = "VARCHAR(" + len + ")";
                }
                typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);

                varsArr.add(nameItem);

                if (direction.equalsIgnoreCase("OUT")) {
                    // OUT параметры только для возврата
                    varsOut.append(nameItem);
                    varsOut.append(" OUT ");
                    varsOut.append(typeVar);
                    varsOut.append(",");
                    varsOutArr.add(nameItem);
                } else if (direction.equalsIgnoreCase("INOUT")) {
                    // INOUT параметры
                    vars.append(nameItem);
                    vars.append(" INOUT ");
                    vars.append(typeVar);
                    vars.append(",");
                    varsColl.append("?,");
                    varsOutArr.add(nameItem);
                } else {
                    // IN параметры (по умолчанию)
                    vars.append(nameItem);
                    vars.append(" IN ");
                    vars.append(typeVar);
                    vars.append(",");
                    varsColl.append("?,");
                }
            }
        }

        // Убираем последнюю запятую
        String varsStr = vars.toString();
        if (varsStr.length() > 0) {
            varsStr = varsStr.substring(0, varsStr.length() - 1);
        }

        String varsOutStr = varsOut.toString();
        if (varsOutStr.length() > 0) {
            varsOutStr = varsOutStr.substring(0, varsOutStr.length() - 1);
        }

        String varsCollStr = varsColl.toString();
        if (varsCollStr.length() > 0) {
            varsCollStr = varsCollStr.substring(0, varsCollStr.length() - 1);
        }

        // Объединяем IN и OUT параметры
        String allParams = varsStr;
        if (varsOutStr.length() > 0) {
            if (allParams.length() > 0) {
                allParams += "," + varsOutStr;
            } else {
                allParams = varsOutStr;
            }
        }

        param.put("vars", varsArr);
        param.put("varsOut", varsOutArr);

        // Получаем основной SQL запрос
        String mainSql = element.text().trim();
        // Убираем лишние точки с запятой
        mainSql = mainSql.replaceAll(";+$", "");

        // Формируем функцию
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE OR REPLACE FUNCTION ").append(schema).append(".").append(cleanFunctionName).append("(");
        sb.append(allParams);
        sb.append(")\n");
        sb.append("RETURNS JSON AS\n");
        sb.append("$$\n");

        if (declareBlocText.length() > 0) {
            sb.append("DECLARE\n");
            sb.append(declareBlocText).append("\n");
        }

        sb.append("BEGIN\n");
        sb.append("-- cmpDataset fileName:");
        sb.append(fileName);
        sb.append("\n");

        if (beforeCodeBloc.length() > 0) {
            sb.append(beforeCodeBloc);
            if (!beforeCodeBloc.endsWith(";") && !beforeCodeBloc.endsWith("\n")) {
                sb.append(";\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("RETURN (\n");
        sb.append("SELECT COALESCE(json_agg(row_to_json(tempTab)), '[]'::json) FROM (\n");
        sb.append(mainSql);
        sb.append("\n) tempTab\n");
        sb.append(");\n");

        if (afterCodeBloc.length() > 0) {
            sb.append(afterCodeBloc);
            if (!afterCodeBloc.endsWith(";") && !afterCodeBloc.endsWith("\n")) {
                sb.append(";\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("END;\n");
        sb.append("$$\n");
        sb.append("LANGUAGE ").append(language).append(";");

        String createFunctionSQL = sb.toString();
        System.out.println("Creating function with SQL:\n" + createFunctionSQL);

        try {
            Statement stmt = conn.createStatement();

            // Удаляем старую функцию если существует
            try {
                stmt.execute("DROP FUNCTION IF EXISTS " + schema + "." + cleanFunctionName + " CASCADE;");
            } catch (SQLException e) {
                System.err.println("Error dropping function: " + e.getMessage());
            }

            // Создаем новую функцию
            PreparedStatement createFunctionStatement = conn.prepareStatement(createFunctionSQL);
            createFunctionStatement.execute();

            String prepareCall = "SELECT " + schema + "." + cleanFunctionName + "(" + varsCollStr + ");";
            CallableStatement selectFunctionStatement = conn.prepareCall(prepareCall);

            param.put("selectFunctionStatement", selectFunctionStatement);
            param.put("prepareCall", prepareCall);
            param.put("connect", conn);
            param.put("SQL", createFunctionSQL);

            procedureList.put(schema + "." + cleanFunctionName, param);

            System.out.println("Function " + schema + "." + cleanFunctionName + " created successfully");

        } catch (SQLException e) {
            System.err.println("Error creating function: " + e.getMessage());
            System.err.println("SQL was:\n" + createFunctionSQL);
            throw new RuntimeException(e);
        }
    }

}