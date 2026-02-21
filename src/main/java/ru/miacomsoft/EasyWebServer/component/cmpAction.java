package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONArray;
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

import static ru.miacomsoft.EasyWebServer.PostgreQuery.*;

public class cmpAction extends Base {

    /**
     * Конструктор с тремя параметрами (для совместимости с ServerResourceHandler.parseElementV2)
     */
    public cmpAction(Document doc, Element element, String tag) {
        super(doc, element, tag);
        initialize(doc, element);
    }

    /**
     * Конструктор с двумя параметрами (для обратной совместимости)
     */
    public cmpAction(Document doc, Element element) {
        super(doc, element, "textarea");
        initialize(doc, element);
    }

    /**
     * Общая логика инициализации
     */
    private void initialize(Document doc, Element element) {
        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();
        attrsDst.add("schema", "Action");

        String name = attrs.get("name");
        this.attr("name", name);
        attrsDst.add("name", name);
        this.initCmpType(element);

        String db = RemoveArrKeyRtrn(attrs, "DB", "DB");
        String query_type = "sql";
        if (element.attributes().hasKey("query_type")) {
            query_type = element.attributes().get("query_type");
        }

        // Формируем имя функции на основе пути к документу и имени компонента
        String docPath = doc.attr("doc_path");
        String rootPath = doc.attr("rootPath");

        String functionName = "";
        if (docPath != null && !docPath.isEmpty() && docPath.length() > 5) {
            // Получаем путь без расширения .html
            String pathPart = docPath.substring(0, docPath.length() - 5);

            // Убираем корневой путь
            if (rootPath != null && !rootPath.isEmpty() && pathPart.startsWith(rootPath)) {
                pathPart = pathPart.substring(rootPath.length());
            }

            // Заменяем все недопустимые символы на подчеркивание
            pathPart = pathPart.replaceAll("[\\\\/:*?\"<>|]", "_");

            // Имя компонента тоже очищаем
            String elementName = name.replaceAll("[\\\\/:*?\"<>|]", "_");

            functionName = pathPart + "___" + elementName;
        } else {
            String elementName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
            functionName = "action___" + elementName + "_" + System.currentTimeMillis();
        }

        // Заменяем все возможные недопустимые символы
        functionName = functionName.replaceAll("[^a-zA-Z0-9_]", "_");

        // Убираем множественные подчеркивания
        functionName = functionName.replaceAll("_+", "_");

        // Убираем подчеркивания в начале и конце
        functionName = functionName.replaceAll("^_+|_+$", "");

        // Приводим к нижнему регистру для PostgreSQL
        functionName = functionName.toLowerCase();

        // Если имя получилось пустым, генерируем уникальное
        if (functionName.isEmpty()) {
            functionName = "action_" + System.currentTimeMillis();
        }

        System.out.println("📌 Generated action function name: " + functionName);

        this.attr("style", "display:none");
        this.attr("action_name", functionName);
        this.attr("name", element.attr("name"));

        StringBuffer jsonVar = new StringBuffer();
        ArrayList<String> jarResourse = new ArrayList<String>();
        ArrayList<String> importPacket = new ArrayList<String>();

        // Обрабатываем дочерние элементы
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

        // Обрабатываем текст компонента (SQL запрос или Java код)
        if (element.hasText()) {
            String elementText = element.text().trim();

            if (query_type.equals("java")) {
                JSONObject infoCompile = new JSONObject();
                if (!ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse,
                        elementText, infoCompile)) {
                    this.removeAttr("style");
                    this.html(JavaStrExecut.parseErrorCompile(infoCompile));
                    return;
                }
            } else if (query_type.equals("sql")) {
                createSQLFunctionPG(ServerConstant.config.APP_NAME + "_" + functionName, this, element);
            }
        }

        // Очищаем содержимое и удаляем обработанные атрибуты
        this.text("");
        for (Attribute attr : element.attributes().asList()) {
            if ("error".equals(attr.getKey())) continue;
            this.removeAttr(attr.getKey());
        }

        // Добавляем JavaScript для инициализации
        StringBuffer sb = new StringBuffer();
        sb.append("<script> $(function() {");
        sb.append("  D3Api.setActionAuto('" + name + "');");
        sb.append("}); </script>");

        Elements elements = doc.getElementsByTag("body");
        if (elements.size() > 0) {
            elements.append(sb.toString());
        }
    }

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json";
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject vars;

        String postBodyStr = new String(query.postCharBody != null ? query.postCharBody : new char[0]);

        try {
            vars = new JSONObject(postBodyStr);
        } catch (Exception e) {
            vars = new JSONObject();
        }

        JSONObject result = new JSONObject();

        String query_type = queryProperty.optString("query_type", "sql");
        String action_name = ServerConstant.config.APP_NAME + "_" +
                queryProperty.optString("action_name", "");

        if (ServerResourceHandler.javaStrExecut.existJavaFunction(action_name)) {
            // Java реализация
            JSONObject varFun = new JSONObject();

            // Обработка переменных с учетом srctype
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

                    case "ctrl":
                        varFun.put(key, varOne.optString("value", ""));
                        break;

                    case "caption":
                        varFun.put(key + "_caption", varOne.optString("caption", ""));
                        if (varOne.has("value")) {
                            varFun.put(key, varOne.optString("value"));
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
                        if (varOne.has("value")) {
                            varFun.put(key, varOne.optString("value"));
                        } else if (varOne.has("defaultVal")) {
                            varFun.put(key, varOne.optString("defaultVal"));
                        }
                }
            }

            JSONObject resFun = ServerResourceHandler.javaStrExecut.runFunction(
                    action_name, varFun, session, null);

            // Обработка результатов
            for (String key : resFun.keySet()) {
                Object keyvalue = resFun.get(key);

                // Проверяем, есть ли такая переменная в исходных vars
                boolean found = false;
                keys = vars.keys();
                while (keys.hasNext()) {
                    String varKey = keys.next();
                    if (varKey.equals(key)) {
                        JSONObject varOne = vars.optJSONObject(varKey);
                        if (varOne != null) {
                            String srctype = varOne.optString("srctype", "var");
                            if (srctype.equals("session")) {
                                session.put(key, keyvalue);
                            } else {
                                varOne.put("value", keyvalue);
                            }
                        }
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // Новая переменная, добавляем
                    JSONObject newVar = new JSONObject();
                    newVar.put("defaultVal", "");
                    newVar.put("value", keyvalue);
                    newVar.put("src", key);
                    newVar.put("srctype", "var");
                    vars.put(key, newVar);
                }
            }

            if (resFun.has("JAVA_ERROR")) {
                result.put("ERROR", resFun.get("JAVA_ERROR"));
            }

        } else if (query_type.equals("sql")) {
            try {
                if (procedureList.containsKey(action_name)) {
                    ConcurrentHashMap<String, Object> param =
                            (ConcurrentHashMap<String, Object>) procedureList.get(action_name);
                    CallableStatement cs;

                    if (session.containsKey("DATABASE")) {
                        ConcurrentHashMap<String, Object> data_base =
                                (ConcurrentHashMap<String, Object>) session.get("DATABASE");
                        Connection conn = null;

                        if (data_base.containsKey("CONNECT")) {
                            conn = (Connection) data_base.get("CONNECT");
                        } else {
                            conn = getConnect(String.valueOf(data_base.get("DATABASE_USER_NAME")),
                                    String.valueOf(data_base.get("DATABASE_USER_PASS")));
                            data_base.put("CONNECT", conn);
                        }

                        if (conn == null) {
                            result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                            return result.toString().getBytes();
                        }

                        cs = conn.prepareCall((String) param.get("prepareCall"));

                        int ind = 0;
                        for (String varOne : (List<String>) param.get("varsArr")) {
                            ind++;
                            cs.registerOutParameter(ind, Types.VARCHAR);
                        }
                    } else {
                        result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                        return result.toString().getBytes();
                    }

                    List<String> varsArr = (List<String>) param.get("vars");

                    if (ServerConstant.config.DEBUG) {
                        result.put("SQL", ((String) param.get("SQL")).split("\n"));
                    }

                    int ind = 0;
                    for (String varNameOne : varsArr) {
                        JSONObject varOne = vars.optJSONObject(varNameOne);
                        if (varOne == null) {
                            varOne = new JSONObject();
                            vars.put(varNameOne, varOne);
                        }

                        String valueStr = "";
                        String srctype = varOne.optString("srctype", "var");

                        switch (srctype) {
                            case "session":
                                if (session.get(varNameOne) == null) {
                                    valueStr = varOne.optString("defaultVal", "");
                                } else {
                                    valueStr = String.valueOf(session.get(varNameOne));
                                }
                                break;

                            case "var":
                                if (queryProperty.has(varNameOne)) {
                                    valueStr = queryProperty.optString(varNameOne);
                                } else {
                                    valueStr = varOne.optString("defaultVal", "");
                                }
                                break;

                            case "ctrl":
                                valueStr = varOne.optString("value", "");
                                break;

                            case "caption":
                                valueStr = varOne.optString("caption", "");
                                break;

                            default:
                                if (varOne.has("value")) {
                                    valueStr = varOne.optString("value");
                                } else if (varOne.has("defaultVal")) {
                                    valueStr = varOne.optString("defaultVal");
                                }
                        }

                        ind++;
                        cs.setString(ind, valueStr);
                    }

                    cs.execute();

                    ind = 0;
                    for (String varNameOne : varsArr) {
                        ind++;
                        String outParam = cs.getString(ind);
                        JSONObject varOne = vars.optJSONObject(varNameOne);

                        if (varOne != null) {
                            String srctype = varOne.optString("srctype", "var");

                            if (srctype.equals("session")) {
                                session.put(varNameOne, outParam);
                            } else {
                                varOne.put("value", outParam);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                JSONArray errorArr = new JSONArray();
                errorArr.put(e.getClass().getName() + ": " + e.getMessage());
                result.put("ERROR", errorArr);
                e.printStackTrace();
            }
        }

        result.put("vars", vars);
        return result.toString().getBytes();
    }

    private void createSQLFunctionPG(String functionName, Element elementThis, Element element) {
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS);

        if (conn == null) {
            System.err.println("❌ Cannot connect to database to create procedure: " + functionName);
            return;
        }

        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        Map<String, Object> param = new ConcurrentHashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();

        // Обрабатываем дочерние элементы для переменных
        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            if (itemElement.tag().toString().toLowerCase().indexOf("var") != -1) {
                Attributes attrsItem = itemElement.attributes();

                String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "var");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String typeVar = "VARCHAR";

                if (len.length() > 0 && !len.equals("-1")) {
                    typeVar = "VARCHAR(" + len + ")";
                } else if (len.equals("-1")) {
                    typeVar = "TEXT";
                }
                typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);

                vars.append(nameItem);
                varsArr.add(nameItem);

                // Для SQL процедур используем INOUT
                vars.append(" INOUT ");
                vars.append(typeVar);
                vars.append(",");
                varsColl.append("?,");
            }
        }

        param.put("vars", varsArr);

        String varsStr = vars.toString();
        if (varsStr.length() > 0) {
            varsStr = varsStr.substring(0, varsStr.length() - 1);
        }

        String varsCollStr = varsColl.toString();
        if (varsCollStr.length() > 0) {
            varsCollStr = varsCollStr.substring(0, varsCollStr.length() - 1);
        }

        // Текст процедуры
        String procedureText = element.text().trim();
        // Убираем точку с запятой в конце если есть
        if (procedureText.endsWith(";")) {
            procedureText = procedureText.substring(0, procedureText.length() - 1);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("CREATE OR REPLACE PROCEDURE ");
        sb.append(functionName);
        sb.append("(").append(varsStr).append(")\n");
        sb.append("LANGUAGE ").append(language).append("\n");
        sb.append("AS $$\n");
        sb.append("BEGIN\n");
        sb.append("    ").append(procedureText).append(";\n");
        sb.append("END;\n");
        sb.append("$$;");

        String createProcedureSQL = sb.toString();

        System.out.println("📝 Creating procedure with SQL:\n" + createProcedureSQL);

        try {
            // Удаляем старую процедуру если есть
            Statement stmt = conn.createStatement();
            stmt.execute("DROP PROCEDURE IF EXISTS " + functionName + " CASCADE;");
            stmt.close();

            // Создаем новую процедуру
            PreparedStatement createProcedureStatement = conn.prepareStatement(createProcedureSQL);
            createProcedureStatement.execute();
            createProcedureStatement.close();

            String prepareCall = "CALL " + functionName + "(" + varsCollStr + ");";

            try {
                CallableStatement cs = conn.prepareCall(prepareCall);
                int ind = 0;
                for (String varOne : varsArr) {
                    ind++;
                    cs.registerOutParameter(ind, Types.VARCHAR);
                }
                param.put("CallableStatement", cs);
            } catch (SQLException e) {
                System.err.println("⚠️ Warning: Could not prepare CallableStatement: " + e.getMessage());
            }

            param.put("connect", conn);
            param.put("varsArr", varsArr);
            param.put("SQL", createProcedureSQL);
            param.put("prepareCall", prepareCall);
            param.put("procedure_name", functionName);

            procedureList.put(functionName, param);

            System.out.println("✅ SQL Procedure created: " + functionName);

        } catch (SQLException e) {
            System.err.println("❌ Error creating procedure " + functionName + ": " + e.getMessage());
            System.err.println("Problematic SQL:\n" + createProcedureSQL);
            e.printStackTrace();
            throw new RuntimeException("Error creating database procedure: " + e.getMessage(), e);
        }
    }
}