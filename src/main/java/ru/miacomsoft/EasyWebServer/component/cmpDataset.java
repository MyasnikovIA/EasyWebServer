package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.JavaStrExecut;
import ru.miacomsoft.EasyWebServer.ServerConstant;
import ru.miacomsoft.EasyWebServer.ServerResourceHandler;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.miacomsoft.EasyWebServer.PostgreQuery.getConnect;
import static ru.miacomsoft.EasyWebServer.PostgreQuery.procedureList;

@SuppressWarnings("unchecked")
public class cmpDataset extends Base {

    /**
     * Конструктор с тремя параметрами (для совместимости с ServerResourceHandler.parseElementV2)
     */
    public cmpDataset(Document doc, Element element, String tag) {
        super(doc, element, tag);
        initialize(doc, element);
    }

    /**
     * Конструктор с двумя параметрами (для обратной совместимости)
     */
    public cmpDataset(Document doc, Element element) {
        super(doc, element, "textarea");
        initialize(doc, element);
    }

    /**
     * Общая логика инициализации
     */
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

        // Формируем имя функции
        String docPath = doc.attr("doc_path");
        String rootPath = doc.attr("rootPath");
        String functionName = generateFunctionName(docPath, rootPath, name);

        System.out.println("📌 Generated dataset function name: " + functionName);

        this.attr("style", "display:none");
        this.attr("dataset_name", functionName);
        this.attr("name", element.attr("name"));

        // Собираем переменные и разделяем before блок и основной запрос
        StringBuffer jsonVar = new StringBuffer();
        ArrayList<String> jarResourse = new ArrayList<String>();
        ArrayList<String> importPacket = new ArrayList<String>();

        String beforeCode = "";
        String mainQuery = "";

        // Сначала ищем тег <before>
        Elements beforeElements = element.getElementsByTag("before");
        if (!beforeElements.isEmpty()) {
            beforeCode = beforeElements.first().text().trim();
            System.out.println("📝 Before block found: " + beforeCode.substring(0, Math.min(50, beforeCode.length())) + "...");
        }

        // Получаем основной текст элемента (CDATA с SQL запросом)
        if (element.hasText()) {
            mainQuery = element.text().trim();

            // Если нашли before блок, удаляем его из mainQuery
            if (!beforeCode.isEmpty()) {
                // Разные варианты поиска before блока в тексте
                String beforeCodeLower = beforeCode.toLowerCase();
                String mainQueryLower = mainQuery.toLowerCase();

                // Ищем по ключевым словам Declare/BEGIN/END
                int declarePos = mainQueryLower.indexOf("declare");
                int beginPos = mainQueryLower.indexOf("begin");
                int endPos = mainQueryLower.indexOf("end;");

                if (declarePos >= 0 && endPos > declarePos) {
                    // Нашли完整的 PL/pgSQL блок, удаляем его
                    int blockEnd = endPos + 4; // "end;".length()
                    if (blockEnd < mainQuery.length()) {
                        mainQuery = mainQuery.substring(blockEnd).trim();
                    } else {
                        mainQuery = "";
                    }
                } else {
                    // Если не нашли по ключевым словам, удаляем текст before блока
                    int beforePos = mainQueryLower.indexOf(beforeCodeLower);
                    if (beforePos >= 0) {
                        mainQuery = mainQuery.substring(0, beforePos) +
                                mainQuery.substring(beforePos + beforeCode.length());
                    }
                }
            }

            // Очищаем mainQuery от лишних символов
            mainQuery = mainQuery.trim();

            // Убираем точку с запятой в конце
            if (mainQuery.endsWith(";")) {
                mainQuery = mainQuery.substring(0, mainQuery.length() - 1);
            }

            if (!mainQuery.isEmpty()) {
                System.out.println("📝 Main query after cleanup: " + mainQuery.substring(0, Math.min(50, mainQuery.length())) + "...");
            }
        }

        // Обрабатываем остальные дочерние элементы (import, var)
        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            String tagName = itemElement.tag().toString().toLowerCase();
            Attributes attrsItem = itemElement.attributes();

            if (tagName.contains("import")) {
                if (attrsItem.hasKey("path")) {
                    jarResourse.add(attrsItem.get("path"));
                }
                if (attrsItem.hasKey("packet")) {
                    importPacket.add(attrsItem.get("packet"));
                }
            } else if (tagName.contains("var")) {
                String nameItem = attrsItem.get("name");
                String src = RemoveArrKeyRtrn(attrsItem, "src", nameItem);
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "var");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");

                jsonVar.append("'" + nameItem + "':{");
                jsonVar.append("'src':'" + src + "',");
                jsonVar.append("'srctype':'" + srctype + "'");
                if (len.length() > 0) jsonVar.append(",'len':'" + len + "'");
                if (defaultVal.length() > 0)
                    jsonVar.append(",'defaultVal':'" + defaultVal.replaceAll("'", "\\\\'") + "'");
                jsonVar.append("},");
            }
        }

        String jsonVarStr = jsonVar.toString();
        if (jsonVarStr.length() > 0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }

        this.attr("vars", "{" + jsonVarStr + "}");
        this.attr("query_type", query_type);
        this.attr("db", db);

        // Получаем список переменных из JSON
        List<String> varsArr = varsArrFromJson(jsonVarStr);

        // Обрабатываем Java код
        if (query_type.equals("java")) {
            JSONObject infoCompile = new JSONObject();
            if (!ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse,
                    mainQuery, infoCompile)) {
                this.removeAttr("style");
                this.html(JavaStrExecut.parseErrorCompile(infoCompile));
                return;
            }
        }
        // Обрабатываем SQL
        else if (query_type.equals("sql") && !mainQuery.isEmpty()) {
            createSQLFunction(ServerConstant.config.APP_NAME + "_" + functionName, beforeCode, mainQuery, varsArr);
        }

        // Очищаем содержимое и удаляем обработанные атрибуты
        this.text("");
        for (Attribute attr : element.attributes().asList()) {
            if ("error".equals(attr.getKey())) continue;
            this.removeAttr(attr.getKey());
        }

        // Добавляем JavaScript для инициализации (БЕЗ ИСПОЛЬЗОВАНИЯ $)
        StringBuffer sb = new StringBuffer();
        sb.append("<script>\n");
        sb.append("  (function() {\n");
        sb.append("    if (window.D3Api && typeof window.D3Api.setDatasetAuto === 'function') {\n");
        sb.append("      window.D3Api.setDatasetAuto('" + name + "');\n");
        sb.append("    } else {\n");
        sb.append("      // Если D3Api еще не загружен, ждем загрузки страницы\n");
        sb.append("      document.addEventListener('DOMContentLoaded', function() {\n");
        sb.append("        if (window.D3Api) window.D3Api.setDatasetAuto('" + name + "');\n");
        sb.append("      });\n");
        sb.append("    }\n");
        sb.append("  })();\n");
        sb.append("</script>\n");

        Elements elements = doc.getElementsByTag("body");
        if (elements.size() > 0) {
            elements.append(sb.toString());
        }
    }

    /**
     * Получение списка переменных из JSON строки
     */
    private List<String> varsArrFromJson(String jsonVarStr) {
        List<String> varsArr = new ArrayList<>();
        if (jsonVarStr != null && !jsonVarStr.isEmpty()) {
            try {
                String jsonStr = jsonVarStr.replace("'", "\"");
                JSONObject vars = new JSONObject("{" + jsonStr + "}");
                Iterator<String> keys = vars.keys();
                while (keys.hasNext()) {
                    varsArr.add(keys.next());
                }
            } catch (JSONException e) {
                System.err.println("Error parsing vars JSON: " + e.getMessage());
            }
        }
        return varsArr;
    }

    /**
     * Генерация безопасного имени функции
     */
    private String generateFunctionName(String docPath, String rootPath, String elementName) {
        String functionName;

        if (docPath != null && !docPath.isEmpty() && docPath.length() > 5) {
            String pathPart = docPath.substring(0, docPath.length() - 5);

            if (rootPath != null && !rootPath.isEmpty() && pathPart.startsWith(rootPath)) {
                pathPart = pathPart.substring(rootPath.length());
            }

            pathPart = pathPart.replaceAll("[\\\\/:*?\"<>|]", "_");
            String cleanElementName = elementName.replaceAll("[\\\\/:*?\"<>|]", "_");
            functionName = pathPart + "___" + cleanElementName;
        } else {
            String cleanElementName = elementName.replaceAll("[\\\\/:*?\"<>|]", "_");
            functionName = "dataset___" + cleanElementName + "_" + System.currentTimeMillis();
        }

        functionName = functionName.replaceAll("[^a-zA-Z0-9_]", "_");
        functionName = functionName.replaceAll("_+", "_");
        functionName = functionName.replaceAll("^_+|_+$", "");
        functionName = functionName.toLowerCase();

        if (functionName.isEmpty()) {
            functionName = "dataset_" + System.currentTimeMillis();
        }

        return functionName;
    }

    /**
     * Создание SQL функции в PostgreSQL
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void createSQLFunction(String functionName, String beforeCode, String mainQuery, List<String> varsArr) {
        if (procedureList.containsKey(functionName) && !ServerConstant.config.DEBUG) {
            return;
        }

        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS);

        if (conn == null) {
            System.err.println("❌ Cannot connect to database to create function: " + functionName);
            return;
        }

        // Формируем переменные для SQL функции
        StringBuilder vars = new StringBuilder();
        StringBuilder varsColl = new StringBuilder();

        for (String varName : varsArr) {
            if (vars.length() > 0) {
                vars.append(", ");
            }
            vars.append(varName).append(" VARCHAR");

            if (varsColl.length() > 0) {
                varsColl.append(", ");
            }
            varsColl.append("?");
        }

        // Формируем SQL
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE FUNCTION ").append(functionName).append("(");
        sql.append(vars.toString());
        sql.append(")\n");
        sql.append("RETURNS JSON AS\n");
        sql.append("$$\n");

        // Обрабатываем before блок
        if (beforeCode != null && !beforeCode.isEmpty()) {
            String beforeLower = beforeCode.toLowerCase();
            int declarePos = beforeLower.indexOf("declare");
            int beginPos = beforeLower.indexOf("begin", declarePos > 0 ? declarePos : 0);

            if (declarePos >= 0 && beginPos > declarePos) {
                // Есть DECLARE секция
                String declareSection = beforeCode.substring(declarePos + "declare".length(), beginPos).trim();
                if (!declareSection.isEmpty()) {
                    sql.append("DECLARE\n");
                    sql.append(declareSection).append("\n");
                }

                // Извлекаем тело BEGIN...END
                int endPos = beforeLower.lastIndexOf("end;");
                if (endPos > beginPos) {
                    String beginSection = beforeCode.substring(beginPos + "begin".length(), endPos).trim();
                    sql.append("BEGIN\n");
                    sql.append(beginSection).append("\n");
                } else {
                    sql.append("BEGIN\n");
                }
            } else {
                sql.append("BEGIN\n");
                sql.append(beforeCode).append("\n");
            }
        } else {
            sql.append("BEGIN\n");
        }

        // Добавляем RETURN с основным запросом
        sql.append("    RETURN (\n");
        sql.append("        SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)\n");
        sql.append("        FROM (\n");
        sql.append("            ").append(mainQuery).append("\n");
        sql.append("        ) t\n");
        sql.append("    );\n");
        sql.append("END;\n");
        sql.append("$$\n");
        sql.append("LANGUAGE plpgsql;");

        String createFunctionSQL = sql.toString();

        System.out.println("📝 Creating function with SQL:\n" + createFunctionSQL);

        try {
            // Удаляем старую функцию
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP FUNCTION IF EXISTS " + functionName + " CASCADE;");
            }

            // Создаем новую функцию
            try (PreparedStatement createFunctionStatement = conn.prepareStatement(createFunctionSQL)) {
                createFunctionStatement.execute();
            }

            String prepareCall = "SELECT " + functionName + "(" + varsColl + ");";

            Map<String, Object> param = new ConcurrentHashMap<>();
            param.put("prepareCall", prepareCall);
            param.put("connect", conn);
            param.put("SQL", createFunctionSQL);
            param.put("function_name", functionName);
            param.put("vars", varsArr);

            System.out.println("✅ Function created: " + functionName);
            procedureList.put(functionName, param);

        } catch (SQLException e) {
            System.err.println("❌ Error creating function " + functionName + ": " + e.getMessage());
            System.err.println("Problematic SQL:\n" + createFunctionSQL);
            e.printStackTrace();
            throw new RuntimeException("Error creating database function: " + e.getMessage(), e);
        }
    }

    /**
     * Обработчик HTTP запроса для dataset
     */
    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json";
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject vars;

        String postBodyStr = new String(query.postCharBody != null ? query.postCharBody : new char[0]);

        try {
            vars = new JSONObject(postBodyStr);
        } catch (JSONException e) {
            vars = new JSONObject();
        }

        JSONObject result = new JSONObject();
        result.put("data", new JSONArray());

        String query_type = queryProperty.optString("query_type", "sql");
        String dataset_name = (ServerConstant.config.APP_NAME + "_" +
                queryProperty.optString("dataset_name", "")).toLowerCase();

        if (ServerResourceHandler.javaStrExecut.existJavaFunction(dataset_name)) {
            // Java implementation
            JSONObject varFun = new JSONObject();
            Iterator<String> keys = vars.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject varOne = vars.optJSONObject(key);
                if (varOne == null) continue;

                String srctype = varOne.optString("srctype", "var");
                switch (srctype) {
                    case "var":
                        if (queryProperty.has(key)) {
                            varFun.put(key, queryProperty.optString(key));
                        } else {
                            varFun.put(key, varOne.optString("defaultVal", ""));
                        }
                        break;
                    case "session":
                        if (session.containsKey(key)) {
                            varFun.put(key, session.get(key));
                        } else {
                            varFun.put(key, varOne.optString("defaultVal", ""));
                        }
                        break;
                    default:
                        varFun.put(key, varOne.optString("value", varOne.optString("defaultVal", "")));
                }
            }

            JSONArray dataRes = new JSONArray();
            JSONObject resFun = ServerResourceHandler.javaStrExecut.runFunction(dataset_name, varFun, session, dataRes);

            if (resFun.has("JAVA_ERROR")) {
                result.put("ERROR", resFun.get("JAVA_ERROR"));
            }
            result.put("data", dataRes);

        } else if (query_type.equals("sql") && procedureList.containsKey(dataset_name)) {
            // SQL implementation
            try {
                ConcurrentHashMap<String, Object> param = (ConcurrentHashMap<String, Object>) procedureList.get(dataset_name);
                String prepareCall = (String) param.get("prepareCall");

                if (!session.containsKey("DATABASE")) {
                    result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                    return result.toString().getBytes();
                }

                ConcurrentHashMap<String, Object> data_base = (ConcurrentHashMap<String, Object>) session.get("DATABASE");
                Connection conn = (Connection) data_base.get("CONNECT");

                if (conn == null || conn.isClosed()) {
                    conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")),
                            String.valueOf(data_base.get("DATABASE_USER_PASS")));
                    data_base.put("CONNECT", conn);
                }

                if (conn == null) {
                    result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                    return result.toString().getBytes();
                }

                try (CallableStatement stmt = conn.prepareCall(prepareCall)) {
                    List<String> varsArr = (List<String>) param.get("vars");

                    for (int i = 0; i < varsArr.size(); i++) {
                        String varName = varsArr.get(i);
                        JSONObject varObj = vars.optJSONObject(varName);
                        String value = "";

                        if (varObj != null) {
                            String srctype = varObj.optString("srctype", "var");
                            if ("session".equals(srctype) && session.containsKey(varName)) {
                                value = String.valueOf(session.get(varName));
                            } else {
                                value = varObj.optString("value", varObj.optString("defaultVal", ""));
                            }
                        }

                        stmt.setString(i + 1, value);
                    }

                    boolean hasResults = stmt.execute();
                    while (hasResults) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            if (rs != null && rs.next()) {
                                String jsonData = rs.getString(1);
                                if (jsonData != null && !jsonData.isEmpty()) {
                                    result.put("data", new JSONArray(jsonData));
                                }
                            }
                        }
                        hasResults = stmt.getMoreResults();
                    }
                }
            } catch (Exception e) {
                result.put("ERROR", e.getMessage());
                e.printStackTrace();
            }
        }

        return result.toString().getBytes();
    }
}