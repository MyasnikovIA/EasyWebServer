package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONObject;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static ru.miacomsoft.EasyWebServer.PostgreQuery.*;

public class cmpAction extends Base {

    // Конструктор с тремя параметрами
    public cmpAction(Document doc, Element element, String tag) {
        super(doc, element, tag);
        initialize(doc, element);
    }

    // Конструктор с двумя параметрами
    public cmpAction(Document doc, Element element) {
        super(doc, element, "teaxtarea");
        initialize(doc, element);
    }

    // Общая логика инициализации
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

        // Формирование имени функции
        String docPath = doc.attr("doc_path");
        String rootPath = doc.attr("rootPath");
        String relativePath = "";
        if (docPath.length() > rootPath.length() && docPath.length() > 5) {
            relativePath = docPath.substring(rootPath.length(), docPath.length() - 5);
        }
        // Заменяем разделители путей на подчеркивания
        relativePath = relativePath.replaceAll("[/\\\\]", "_");
        // Убираем недопустимые символы
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
        // String functionName = (relativePath + "___" + element.attr("name")).toLowerCase();
        String functionName = (pathHash + "_" + fileName + "_" + element.attr("name")).toLowerCase();

        this.attr("style", "display:none");
        this.attr("action_name", functionName);
        this.attr("name", element.attr("name"));

        StringBuffer jsonVar = new StringBuffer();
        ArrayList<String> jarResourse = new ArrayList<String>();
        ArrayList<String> importPacket = new ArrayList<String>();

        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            Attributes attrsItem = itemElement.attributes();

            String tagName = itemElement.tag().toString().toLowerCase();

            if (tagName.indexOf("import") != -1) {
                if (attrsItem.hasKey("path")) {
                    jarResourse.add(attrsItem.get("path"));
                }
                if (attrsItem.hasKey("packet")) {
                    importPacket.add(attrsItem.get("packet"));
                }
            } else if (tagName.indexOf("var") != -1 || tagName.indexOf("cmpactionvar") != -1) {
                String nameItem = attrsItem.get("name");
                String src = RemoveArrKeyRtrn(attrsItem, "src", nameItem);
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");

                jsonVar.append("'" + nameItem + "':{");
                jsonVar.append("'src':'" + src + "',");
                jsonVar.append("'srctype':'" + srctype + "'");
                if (defaultVal.length() > 0) {
                    jsonVar.append(",'defaultVal':'" + defaultVal.replaceAll("'", "\\\\'") + "'");
                }
                if (len.length() > 0) {
                    jsonVar.append(",'len':'" + len + "'");
                }
                jsonVar.append("},");
            }
        }

        String jsonVarStr = jsonVar.toString();
        if (jsonVarStr.length() > 0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }
        jsonVarStr = "{" + jsonVarStr + "}";

        this.attr("vars", jsonVarStr);
        attrsDst.add("query_type", query_type);
        attrsDst.add("db", db);

        if (element.hasText()) {
            if (query_type.equals("java")) {
                JSONObject infoCompile = new JSONObject();
                if (!ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse, element.text().trim(), infoCompile)) {
                    this.removeAttr("style");
                    this.html(ru.miacomsoft.EasyWebServer.JavaStrExecut.parseErrorCompile(infoCompile));
                    return;
                } else {
                    System.out.println("Compiled Java function: " + functionName);
                }
            } else if (query_type.equals("sql")) {
                createSQLFunctionPG(ServerConstant.config.APP_NAME + "_" + functionName, this, element, docPath+" ("+element.attr("name")+")");
            }
        }

        this.text("");
        for (Attribute attr : element.attributes().asList()) {
            if ("error".equals(attr.getKey())) continue;
            this.removeAttr(attr.getKey());
        }

        // Автоматическое подключение JavaScript библиотеки для cmpAction
        Elements head = doc.getElementsByTag("head");

        // Проверяем, не подключена ли уже библиотека
        Elements existingScripts = head.select("script[src*='cmpAction_js']");
        if (existingScripts.isEmpty()) {
            // Добавляем ссылку на JS библиотеку
            String jsPath = "{component}/cmpAction_js";
            head.append("<script cmp=\"action-lib\" src=\"" + jsPath + "\" type=\"text/javascript\"></script>");
            System.out.println("cmpAction: JavaScript library auto-included for action: " + name);
        }

        // Удаляем старый скрипт с setActionAuto, так как он теперь в библиотеке
        // StringBuffer sb = new StringBuffer();
        // sb.append("<script> $(function() {");
        // sb.append("  D3Api.setActionAuto('" + name + "');");
        // sb.append("}); </script>");
        // Elements elements = doc.getElementsByTag("body");
        // elements.append(sb.toString());
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

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript";
        Map<String, Object> session = query.session;
        JSONObject result = new JSONObject();
        JSONObject queryProperty = query.requestParam;

        // Получаем тело запроса
        String postBodyStr = new String(query.postCharBody);
        JSONObject vars;

        try {
            vars = new JSONObject(postBodyStr);
        } catch (Exception e) {
            // Если не удалось распарсить как JSON, создаем пустой объект
            vars = new JSONObject();
        }

        System.out.println("Action called - Raw vars: " + vars.toString());

        String query_type = queryProperty.optString("query_type", "java");
        String action_name = ServerConstant.config.APP_NAME + "_" + queryProperty.optString("action_name", "");
        action_name = action_name.replaceAll("[^a-zA-Z0-9_]", "");

        System.out.println("Action name: " + action_name);
        System.out.println("Query type: " + query_type);

        if (ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.existJavaFunction(action_name)) {
            // Подготавливаем переменные для Java функции - все как строки
            JSONObject varFun = new JSONObject();
            Iterator<String> keys = vars.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object varValue = vars.get(key);

                if (varValue instanceof JSONObject) {
                    JSONObject varObj = (JSONObject) varValue;
                    // Всегда берем строковое значение
                    String value = varObj.optString("value", "");
                    if (value.isEmpty()) {
                        value = varObj.optString("defaultVal", "");
                    }
                    varFun.put(key, value);
                    System.out.println("Variable " + key + " = " + value + " (from object)");
                } else {
                    varFun.put(key, varValue.toString());
                    System.out.println("Variable " + key + " = " + varValue + " (direct)");
                }
            }

            System.out.println("Calling Java function with vars: " + varFun.toString());

            // Вызываем Java функцию
            JSONObject resFun = ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.runFunction(action_name, varFun, session, null);

            System.out.println("Java function result: " + resFun.toString());

            // Обрабатываем результаты
            if (resFun.has("JAVA_ERROR")) {
                // Если есть ошибка, добавляем её в результат
                result.put("ERROR", resFun.get("JAVA_ERROR"));

                // Возвращаем исходные vars без изменений, чтобы клиент не потерял данные
                result.put("vars", vars);
            } else {
                // Обрабатываем успешный результат
                keys = resFun.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object keyvalue = resFun.get(key);

                    if (key.equals("JAVA_ERROR")) {
                        continue;
                    }

                    // Обновляем значения в vars, сохраняя структуру
                    if (vars.has(key) && vars.get(key) instanceof JSONObject) {
                        // Сохраняем как строку
                        vars.getJSONObject(key).put("value", keyvalue.toString());
                    } else {
                        JSONObject newVar = new JSONObject();
                        newVar.put("value", keyvalue.toString());
                        newVar.put("src", key);
                        newVar.put("srctype", "var");
                        vars.put(key, newVar);
                    }
                }
                result.put("vars", vars);
            }
        } else if (query_type.equals("sql")) {
            // Обработка SQL запросов
            try {
                if (procedureList.containsKey(action_name)) {
                    HashMap<String, Object> param = procedureList.get(action_name);
                    CallableStatement cs;

                    if (session.containsKey("DATABASE")) {
                        HashMap<String, Object> data_base = (HashMap<String, Object>) session.get("DATABASE");
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

                        List<String> varsArr = (List<String>) param.get("vars");

                        if (ServerConstant.config.DEBUG) {
                            result.put("SQL", ((String) param.get("SQL")).split("\n"));
                        }

                        ind = 0;
                        for (String varNameOne : varsArr) {
                            JSONObject varOne = vars.optJSONObject(varNameOne);
                            String valueStr = "";

                            if (varOne != null) {
                                if (varOne.optString("srctype").equals("session")) {
                                    valueStr = String.valueOf(session.getOrDefault(varNameOne,
                                            varOne.optString("defaultVal", "")));
                                } else {
                                    valueStr = varOne.optString("value", varOne.optString("defaultVal", ""));
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
                                if (varOne.optString("srctype").equals("session")) {
                                    session.put(varNameOne, outParam);
                                } else {
                                    varOne.put("value", outParam);
                                }
                            }
                        }
                    } else {
                        result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                        return result.toString().getBytes();
                    }
                }
            } catch (Exception e) {
                result.put("ERROR", e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            result.put("ERROR", "Function not found: " + action_name);
        }

        result.put("vars", vars);
        String resultText = result.toString();
        System.out.println("Action response: " + resultText);
        return resultText.getBytes();
    }

    private void createSQLFunctionPG(String functionName, Element elementThis, Element element,String fileName) {
        // Очищаем имя функции от недопустимых символов
        functionName = functionName.replaceAll("[^a-zA-Z0-9_]", "");

        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS);
        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        HashMap<String, Object> param = new HashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();

        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            String tagName = itemElement.tag().toString().toLowerCase();

            if (tagName.indexOf("var") != -1 || tagName.indexOf("cmpactionvar") != -1) {
                Attributes attrsItem = itemElement.attributes();
                String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
                String src = RemoveArrKeyRtrn(attrsItem, "src", nameItem);
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "");
                String get = RemoveArrKeyRtrn(attrsItem, "get", "");
                String put = RemoveArrKeyRtrn(attrsItem, "put", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String typeVar = "VARCHAR";
                if (len.length() > 0) {
                    typeVar = "VARCHAR(" + len + ")";
                }
                typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);

                vars.append(nameItem);
                varsArr.add(nameItem);
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

        StringBuffer sb = new StringBuffer();
        sb.append("CREATE OR REPLACE PROCEDURE ");
        sb.append(functionName);
        sb.append("(");
        sb.append(varsStr);
        sb.append(") LANGUAGE ");
        sb.append(language);
        sb.append(" AS $$ \n");
        sb.append("BEGIN \n");
        sb.append("--cmpAction fileName:");
        sb.append(fileName);
        sb.append("\n");
        sb.append(element.text().trim());
        sb.append("\nEND;$$\n");

        System.out.println("Creating procedure: " + functionName);
        System.out.println("SQL: " + sb.toString());

        createProcedure(conn, functionName, sb.toString());

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
            throw new RuntimeException(e);
        }

        param.put("connect", conn);
        param.put("varsArr", varsArr);
        param.put("SQL", sb.toString());
        param.put("prepareCall", prepareCall);
        procedureList.put(functionName, param);
    }
}