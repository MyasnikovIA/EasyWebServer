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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.miacomsoft.EasyWebServer.PostgreQuery.*;

public class cmpAction extends Base {

    public cmpAction(Document doc, Element element) {
        super(doc, element, "textarea");
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

        String functionName = (doc.attr("doc_path").substring(0, doc.attr("doc_path").length() - 5)
                .substring(doc.attr("rootPath").length())).replaceAll("/", "_") + "___" +
                element.attr("name");

        this.attr("style", "display:none");
        this.attr("action_name", functionName);
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
        attrsDst.add("query_type", query_type);
        attrsDst.add("db", db);

        if (element.hasText()) {
            if (query_type.equals("java")) {
                JSONObject infoCompile = new JSONObject();
                if (!ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse,
                        element.text().trim(), infoCompile)) {
                    this.removeAttr("style");
                    this.html(JavaStrExecut.parseErrorCompile(infoCompile));
                    return;
                }
            } else if (query_type.equals("sql")) {
                createSQLFunctionPG(ServerConstant.config.APP_NAME + "_" + functionName, this, element);
            }
        }

        this.text("");
        for (Attribute attr : element.attributes().asList()) {
            if ("error".equals(attr.getKey())) continue;
            this.removeAttr(attr.getKey());
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<script> $(function() {");
        sb.append("  D3Api.setActionAuto('" + name + "');");
        sb.append("}); </script>");
        Elements elements = doc.getElementsByTag("body");
        elements.append(sb.toString());
    }

    private void processVariables(JSONObject vars, JSONObject varFun,
                                  Map<String, Object> session,
                                  JSONObject queryProperty) {
        Iterator<String> keys = vars.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject varOne = vars.optJSONObject(key);
            if (varOne == null) continue;

            String srctype = varOne.optString("srctype", "var");

            switch (srctype) {
                case "var":
                    // Переменные страницы
                    if (queryProperty.has(key)) {
                        varFun.put(key, queryProperty.optString(key));
                    } else {
                        varFun.put(key, varOne.optString("defaultVal", ""));
                    }
                    break;

                case "ctrl":
                    // Значения из контролов
                    String ctrlValue = varOne.optString("value", "");
                    varFun.put(key, ctrlValue);
                    break;

                case "caption":
                    // Текст подписи контрола
                    String caption = varOne.optString("caption", "");
                    varFun.put(key + "_caption", caption);
                    if (varOne.has("value")) {
                        varFun.put(key, varOne.optString("value"));
                    }
                    break;

                case "session":
                    // Значения из сессии
                    if (session.containsKey(key)) {
                        varFun.put(key, session.get(key));
                    } else {
                        varFun.put(key, varOne.optString("defaultVal", ""));
                    }
                    break;

                default:
                    // Обычные переменные
                    if (varOne.has("value")) {
                        varFun.put(key, varOne.optString("value"));
                    } else if (varOne.has("defaultVal")) {
                        varFun.put(key, varOne.optString("defaultVal"));
                    }
            }
        }
    }

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript";
        Map<String, Object> session = query.session;
        JSONObject result = new JSONObject();
        JSONObject queryProperty = query.requestParam;

        String postBodyStr = new String(query.postCharBody != null ? query.postCharBody : new char[0]);
        JSONObject vars;
        try {
            vars = new JSONObject(postBodyStr);
        } catch (Exception e) {
            vars = new JSONObject();
        }

        String query_type = queryProperty.optString("query_type", "sql");
        String action_name = ServerConstant.config.APP_NAME + "_" +
                queryProperty.optString("action_name", "");

        if (ServerResourceHandler.javaStrExecut.existJavaFunction(action_name)) {
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

        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        Map<String, Object> param = new ConcurrentHashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();

        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
            Attributes attrsItem = itemElement.attributes();

            String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
            String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "var");
            String len = RemoveArrKeyRtrn(attrsItem, "len", "");
            String typeVar = "VARCHAR";

            if (len.length() > 0) {
                typeVar = "VARCHAR(" + len + ")";
            }
            typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);

            vars.append(nameItem);
            varsArr.add(nameItem);

            // Для SQL процедур используем INOUT для всех типов,
            // чтобы получать измененные значения обратно
            vars.append(" INOUT ");
            vars.append(typeVar);
            vars.append(",");
            varsColl.append("?,");
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

        StringBuffer sb = new StringBuffer(
                " CREATE OR REPLACE PROCEDURE " +
                        functionName +
                        "(" + varsStr + ")" +
                        " language " + language +
                        " AS $$ \n" +
                        " BEGIN \n" +
                        element.text().trim() +
                        " \nEND;$$\n"
        );

        createProcedure(conn, functionName, sb.toString());

        String prepareCall = "call " + functionName + "(" + varsCollStr + ");";

        try {
            CallableStatement cs = conn.prepareCall(prepareCall);
            int ind = 0;
            for (String varOne : varsArr) {
                ind++;
                cs.registerOutParameter(ind, Types.VARCHAR);
            }
            param.put("CallableStatement", cs);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        param.put("connect", conn);
        param.put("varsArr", varsArr);
        param.put("SQL", sb.toString());
        param.put("prepareCall", prepareCall);
        procedureList.put(functionName, param);
    }
}