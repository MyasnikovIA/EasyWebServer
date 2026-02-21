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

public class cmpDataset extends Base {

    public cmpDataset(Document doc, Element element) {
        super(doc, element, "textarea");
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

        String functionName = ((doc.attr("doc_path").substring(0, doc.attr("doc_path").length() - 5)
                .substring(doc.attr("rootPath").length())).replaceAll("/", "_") + "___" +
                element.attr("name")).toLowerCase();

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
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "var");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");

                jsonVar.append("'" + nameItem + "':{");
                jsonVar.append("'src':'" + src + "',");
                jsonVar.append("'srctype':'" + srctype + "'");
                if (len.length() > 0) jsonVar.append(",'len':'" + len + "'");
                if (defaultVal.length() > 0) jsonVar.append(",'defaultVal':'" + defaultVal.replaceAll("'", "\\\\'") + "'");
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
                createSQL(ServerConstant.config.APP_NAME + "_" + functionName, this, element);
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
        JSONObject queryProperty = query.requestParam;
        JSONObject vars;

        String postBodyStr = new String(query.postCharBody != null ? query.postCharBody : new char[0]);

        if (postBodyStr.indexOf("{") == -1) {
            vars = new JSONObject();
            int indParam = 0;
            for (String par : postBodyStr.split("&")) {
                String[] val = par.split("=");
                if (val.length == 2) {
                    try {
                        vars.put(val[0], val[1]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    indParam++;
                    try {
                        vars.put("param" + indParam, val[0]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            try {
                vars = new JSONObject(postBodyStr);
            } catch (JSONException e) {
                vars = new JSONObject();
                e.printStackTrace();
            }
        }

        JSONObject result = new JSONObject();
        result.put("data", new JSONArray());

        String query_type = queryProperty.optString("query_type", "sql");
        String dataset_name = (ServerConstant.config.APP_NAME + "_" +
                queryProperty.optString("dataset_name", "")).toLowerCase();

        if (ServerResourceHandler.javaStrExecut.existJavaFunction(dataset_name)) {
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

            JSONArray dataRes = new JSONArray();
            JSONObject resFun = ServerResourceHandler.javaStrExecut.runFunction(dataset_name, varFun, session, dataRes);

            JSONObject returnVar = new JSONObject();
            keys = vars.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject varOne = vars.optJSONObject(key);
                if (varOne != null && varOne.has("len")) {
                    varOne.put("value", varFun.opt(key));
                    returnVar.put(key, varOne);
                }
            }

            if (resFun.has("JAVA_ERROR")) {
                result.put("ERROR", resFun.get("JAVA_ERROR"));
            }
            result.put("data", dataRes);
            result.put("vars_out", returnVar);

        } else if (query_type.equals("sql")) {
            try {
                String fullDatasetName = ServerConstant.config.APP_NAME + "_" +
                        queryProperty.optString("dataset_name", "").toLowerCase();

                if (procedureList.containsKey(fullDatasetName)) {
                    CallableStatement selectFunctionStatement = null;
                    ConcurrentHashMap<String, Object> param = (ConcurrentHashMap<String, Object>)
                            procedureList.get(fullDatasetName);
                    String prepareCall = (String) param.get("prepareCall");

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

                        selectFunctionStatement = conn.prepareCall(prepareCall);
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
                        if (varOne == null) continue;

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
                        selectFunctionStatement.setString(ind, valueStr);
                    }

                    boolean hasResults = selectFunctionStatement.execute();
                    while (hasResults) {
                        ResultSet rs = selectFunctionStatement.getResultSet();
                        if (rs != null) {
                            if (rs.next()) {
                                String jsonData = rs.getString(1);
                                if (jsonData != null && !jsonData.isEmpty()) {
                                    result.put("data", new JSONArray(jsonData));
                                }
                            }
                            rs.close();
                        }
                        hasResults = selectFunctionStatement.getMoreResults();
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

    private void createSQL(String functionName, Element elementThis, Element element) {
        if (procedureList.containsKey(functionName) && !ServerConstant.config.DEBUG) {
            return;
        }

        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS);
        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        Map<String, Object> param = new ConcurrentHashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();
        String befireCodeBloc = "";
        String declareBlocText = "";

        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);

            if (itemElement.text().length() > 0) {
                String beforeCode = itemElement.text().toLowerCase();
                if (beforeCode.indexOf("declare") != -1) {
                    declareBlocText = itemElement.text().substring(0, beforeCode.indexOf("begin"));
                    befireCodeBloc = itemElement.text().substring(
                            declareBlocText.length() + "begin".length(),
                            beforeCode.lastIndexOf("end;"));
                } else {
                    befireCodeBloc = itemElement.text();
                }
                itemElement.text("");
            } else if (itemElement.tag().toString().toLowerCase().indexOf("var") != -1) {
                Attributes attrsItem = itemElement.attributes();
                String nameItem = RemoveArrKeyRtrn(attrsItem, "name", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String typeVar = "VARCHAR";

                if (len.length() > 0) {
                    typeVar = "VARCHAR(" + len + ")";
                }
                typeVar = RemoveArrKeyRtrn(attrsItem, "type", typeVar);
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");

                vars.append(nameItem);
                varsArr.add(nameItem);
                vars.append(" IN ");
                vars.append(typeVar);
                vars.append(",");
                varsColl.append("?,");
            }
        }

        String jsonVarStr = vars.toString();
        if (jsonVarStr.length() > 0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }
        param.put("vars", varsArr);

        String varsCollStr = varsColl.toString();
        if (varsCollStr.length() > 0) {
            varsCollStr = varsCollStr.substring(0, varsCollStr.length() - 1);
        }

        StringBuffer sb = new StringBuffer(
                "CREATE OR REPLACE FUNCTION " + functionName + "(" + jsonVarStr + ")\n" +
                        "RETURNS JSON AS\n" +
                        "$$\n" +
                        declareBlocText +
                        "BEGIN\n" +
                        befireCodeBloc +
                        "\n RETURN (\n" +
                        "SELECT COALESCE(json_agg(tempTab), '[]'::json) FROM (\n" +
                        element.text().trim().replaceAll(";", " ") +
                        "\n) tempTab\n" +
                        ");\n" +
                        "END;\n" +
                        "$$\n" +
                        "LANGUAGE " + language + ";"
        );

        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP FUNCTION IF EXISTS " + functionName + ";");
            PreparedStatement createFunctionStatement = conn.prepareStatement(sb.toString());
            createFunctionStatement.execute();

            String prepareCall = "select " + functionName + "(" + varsCollStr + ");";
            CallableStatement selectFunctionStatement = conn.prepareCall(prepareCall);

            param.put("selectFunctionStatement", selectFunctionStatement);
            param.put("prepareCall", prepareCall);
            param.put("connect", conn);
            param.put("SQL", sb.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        procedureList.put(functionName, param);
    }
}