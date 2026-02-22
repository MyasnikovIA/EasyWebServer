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

    // Кэш для хранения времени модификации файлов и соответствующих функций
    private static Map<String, Long> functionFileModifiedMap = new HashMap<>();

    // Добавляем недостающий конструктор с тремя параметрами
    public cmpDataset(Document doc, Element element, String tag) {
        super(doc, element, tag); // Вызываем конструктор родительского класса
        initialize(doc, element); // Выносим логику инициализации в отдельный метод
    }

    // Оставляем существующий конструктор для обратной совместимости
    public cmpDataset(Document doc, Element element) {
        super(doc, element, "teaxtarea");
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

        // Исправленное формирование имени функции
        String docPath = doc.attr("doc_path");
        String rootPath = doc.attr("rootPath");
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
        // Получаем имя файла без расширения
        String fileName = "";
        if (relativePath.lastIndexOf('/') > 0) {
            fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            if (fileName.lastIndexOf('.') > 0) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
        }
        String functionName = (pathHash + "_" + fileName + "_" + element.attr("name")).toLowerCase();

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
                if (defaultVal.length()>0) jsonVar.append("'defaultVal':'" + defaultVal.replaceAll("'", "\\'") + "'");
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
                // Получаем время модификации файла
                long fileModifiedTime = 0;
                try {
                    java.io.File file = new java.io.File(docPath);
                    if (file.exists()) {
                        fileModifiedTime = file.lastModified();
                    }
                } catch (Exception e) {
                    System.err.println("Error getting file modification time: " + e.getMessage());
                }

                // Проверяем, нужно ли создавать функцию
                boolean needToCreate = ServerConstant.config.DEBUG; // В режиме debug всегда создаем

                if (!needToCreate) {
                    // Проверяем по кэшу
                    Long cachedTime = functionFileModifiedMap.get(functionName);
                    if (cachedTime == null || cachedTime != fileModifiedTime) {
                        needToCreate = true;
                    } else {
                        // Дополнительно проверяем существование функции в БД
                        needToCreate = !functionExistsInDB(functionName);
                    }
                }

                if (needToCreate) {
                    createSQL(ServerConstant.config.APP_NAME + "_" + functionName, this, element, docPath + " (" + element.attr("name") + ")");
                    // Обновляем кэш
                    functionFileModifiedMap.put(functionName, fileModifiedTime);
                } else {
                    System.out.println("Function " + functionName + " is up to date, skipping creation");
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
        Elements elements = doc.getElementsByTag("body");
        elements.append(sb.toString());

        // Автоматическое подключение JavaScript библиотеки для cmpDataset
        Elements head = doc.getElementsByTag("head");

        // Проверяем, не подключена ли уже библиотека
        Elements existingScripts = head.select("script[src*='cmpDataset_js']");
        if (existingScripts.isEmpty()) {
            // Добавляем ссылку на JS библиотеку
            String jsPath = "{component}/cmpDataset_js";
            head.append("<script cmp=\"dataset-lib\" src=\"" + jsPath + "\" type=\"text/javascript\"></script>");
            System.out.println("cmpDataset: JavaScript library auto-included for dataset: " + name);
        }
    }

    /**
     * Проверка существования функции в БД
     */
    private boolean functionExistsInDB(String functionName) {
        Connection conn = null;
        try {
            conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
            if (conn == null) return false;

            String fullFunctionName = ServerConstant.config.APP_NAME + "_" + functionName;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_proc WHERE proname = '" + fullFunctionName + "'"
            );
            return rs.next();
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

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript"; // Изменить mime ответа
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject vars;
        String postBodyStr = new String(query.postCharBody);
        if (postBodyStr.indexOf("{")==-1) {
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
            vars = new JSONObject(postBodyStr);
        }
        JSONObject result = new JSONObject();
        result.put("data", new JSONArray("[]"));
        String query_type = queryProperty.getString("query_type");
        String dataset_name = (ServerConstant.config.APP_NAME+"_" + queryProperty.getString("dataset_name")).toLowerCase();
        if (ServerResourceHandler.javaStrExecut.existJavaFunction(dataset_name)) {
            JSONObject varFun = new JSONObject();
            Iterator<String> keys = vars.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject varOne = vars.getJSONObject(key);
                if (varOne.getString("srctype").equals("session")) {
                    if (session.containsKey(key)) {
                        varFun.put(key, session.get(key));
                    } else {
                        varFun.put(key, varOne.getString("defaultVal"));
                    }
                } else {
                    if (varOne.has("value")) {
                        varFun.put(key, varOne.getString("defaultVal"));
                    } else if (varOne.has("defaultVal")) {
                        varFun.put(key, varOne.getString("defaultVal"));
                    } else {
                        varFun.put(key, "");
                    }
                }
            }

            JSONArray dataRes = new JSONArray();
            JSONObject resFun = ServerResourceHandler.javaStrExecut.runFunction(dataset_name, varFun, session, dataRes);
            JSONObject returnVar = new JSONObject();
            keys = vars.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject varOne = vars.getJSONObject(key);
                if (varOne.has("len")) {
                    varOne.put("value",varFun.get(key));
                    returnVar.put(key,varOne);
                }
            }
            if (resFun.has("JAVA_ERROR")) {
                result.put("ERROR", resFun.get("JAVA_ERROR"));
            }
            result.put("data", dataRes);
            result.put("vars_out", returnVar);
        } else if (query_type.equals("sql")) {
            try {
                if (procedureList.containsKey(dataset_name)) {
                    CallableStatement selectFunctionStatement = null;
                    HashMap<String, Object> param = procedureList.get(dataset_name);
                    String prepareCall = (String) param.get("prepareCall");
                    if (session.containsKey("DATABASE")) {
                        // Если в сессии есть информация о подключении к БД, тогда подключаемся
                        HashMap<String, Object> data_base = (HashMap<String, Object>) session.get("DATABASE");
                        Connection conn = null;
                        if (data_base.containsKey("CONNECT")) {
                            conn = (Connection) data_base.get("CONNECT");
                        } else {
                            conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")), String.valueOf(data_base.get("DATABASE_USER_PASS")));
                            data_base.put("CONNECT", conn);
                        }
                        if (conn == null) {
                            // переадресация на страницу регистрации
                            result = new JSONObject();
                            result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                            return result.toString().getBytes();
                        }
                        selectFunctionStatement = conn.prepareCall(prepareCall);
                    } else {
                        // иначе берем подключение системного пользователя
                        // selectFunctionStatement = (CallableStatement) param.get("selectFunctionStatement");
                        result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                        return result.toString().getBytes();
                    }
                    // Connection conn = (Connection) param.get("connect");
                    // String prepareCall = (String) param.get("prepareCall");
                    List<String> varsArr = (List<String>) param.get("vars");
                    if (ServerConstant.config.DEBUG) {
                        result.put("SQL", ((String) param.get("SQL")).split("\n"));
                    }
                    int ind = 0;
                    for (String varNameOne : varsArr) {
                        JSONObject varOne = vars.getJSONObject(varNameOne);
                        String valueStr = "";
                        if (varOne.getString("srctype").equals("session")) {
                            if (session.get(varNameOne) == null) {
                                valueStr = varOne.getString("defaultVal");
                            } else {
                                valueStr = String.valueOf(session.get(varNameOne));
                            }
                        } else {
                            if (vars.has(varNameOne)) {
                                JSONObject varObj = vars.getJSONObject(varNameOne);
                                if (varObj.has("value")) {
                                    valueStr = String.valueOf(varObj.get("value")); // Входящие переменные
                                }
                                if (valueStr.length() == 0) {
                                    if (varObj.has("defaultVal")) {
                                        valueStr = String.valueOf(varObj.get("defaultVal"));
                                    }
                                }
                            }
                        }
                        ind++;
                        // System.out.println(" ind "+ind+ " varNameOne:"+varNameOne +"  valueStr:"+valueStr);
                        selectFunctionStatement.setString(ind, valueStr);
                    }
                    boolean hasResults = selectFunctionStatement.execute();
                    while (hasResults) {

                        ResultSet rs = selectFunctionStatement.getResultSet();
                        if (rs != null) {
                            if (rs.next()) {
                                result.put("data", new JSONArray(rs.getString(1))); // получить результат JSON
                            }
                            rs.close();
                        }
                        hasResults = selectFunctionStatement.getMoreResults();
                    }
                    /*
                    ind = 0;
                    for (String varNameOne : varsArr) {
                        ind++;
                        // применяется при использовании  INOUT типа
                        //String outParam = selectFunctionStatement.getString(ind);  // Получение ответа
                        JSONObject varOne = vars.getJSONObject(varNameOne);
                        if (varOne.getString("srctype").equals("session")) {
                            session.put(varNameOne, outParam);
                        } else {
                            varOne.put("value", outParam);
                        }
                    }
                    */
                }
            } catch (Exception e) {
                result.put("ERROR", (e.getClass().getName() + ": " + e.getMessage()).split("\n"));
            }
        }
        // ((JSONObject) vars.get("LPU_TEXT")).put("value", "12121212");
        result.put("vars", vars);
        return result.toString().getBytes();
    }

    /**
     * Вспомогательный метод для вычисления MD5 хэша
     */
    private String getMd5Hash(String input) {
        if (input == null) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private void createSQL(String functionName, Element elementThis, Element element, String fileName) {
        if (procedureList.containsKey(functionName) && !ServerConstant.config.DEBUG) {
            // Если функция уже создана в БД и режим отладки отключен, тогда пропускаем создание новой функции
            return;
        }
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        HashMap<String, Object> param = new HashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();
        String beforeCodeBloc = "";
        String declareBlocText = "";

        // Обрабатываем дочерние элементы
        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            String tagName = itemElement.tag().toString().toLowerCase();

            if (tagName.equals("before")) {
                // Блок BEFORE содержит код до основного запроса
                String beforeText = itemElement.text().trim();
                if (beforeText.length() > 0) {
                    // Убираем лишние пробелы
                    // beforeText = beforeText.replaceAll("(?<!\r?\n)[\\t\\x0B\\f ]+(?!\r?\n)", " ");

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
            } else if (tagName.indexOf("var") != -1) {
                Attributes attrsItem = itemElement.attributes();
                String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String typeVar = "VARCHAR";
                if (len.length() > 0) {
                    typeVar = "VARCHAR(" + len + ")";
                }
                typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);

                vars.append(nameItem);
                varsArr.add(nameItem);
                vars.append(" IN ");
                vars.append(typeVar);
                vars.append(",");
                varsColl.append("?,");
            }
        }

        // Убираем последнюю запятую
        String jsonVarStr = vars.toString();
        if (jsonVarStr.length() > 0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }

        String varsCollStr = varsColl.toString();
        if (varsCollStr.length() > 0) {
            varsCollStr = varsCollStr.substring(0, varsCollStr.length() - 1);
        }

        param.put("vars", varsArr);

        // Получаем основной SQL запрос
        String mainSql = element.text().trim();
        // Убираем лишние точки с запятой
        mainSql = mainSql.replaceAll(";+$", "");

        // Формируем функцию
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE OR REPLACE FUNCTION ").append(functionName).append("(");
        sb.append(jsonVarStr);
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
        sb.append("END;\n");
        sb.append("$$\n");
        sb.append("LANGUAGE ").append(language).append(";");

        String createFunctionSQL = sb.toString();
        System.out.println("Creating function with SQL:\n" + createFunctionSQL);

        try {
            Statement stmt = conn.createStatement();

            // Удаляем старую функцию если существует
            try {
                stmt.execute("DROP FUNCTION IF EXISTS " + functionName + " CASCADE;");
            } catch (SQLException e) {
                System.err.println("Error dropping function: " + e.getMessage());
            }

            // Создаем новую функцию
            PreparedStatement createFunctionStatement = conn.prepareStatement(createFunctionSQL);
            createFunctionStatement.execute();

            String prepareCall = "SELECT " + functionName + "(" + varsCollStr + ");";
            CallableStatement selectFunctionStatement = conn.prepareCall(prepareCall);

            param.put("selectFunctionStatement", selectFunctionStatement);
            param.put("prepareCall", prepareCall);
            param.put("connect", conn);
            param.put("SQL", createFunctionSQL);

            procedureList.put(functionName, param);

            System.out.println("Function " + functionName + " created successfully");

        } catch (SQLException e) {
            System.err.println("Error creating function: " + e.getMessage());
            System.err.println("SQL was:\n" + createFunctionSQL);
            throw new RuntimeException(e);
        }
    }

}