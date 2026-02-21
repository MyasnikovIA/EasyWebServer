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
import java.util.concurrent.ConcurrentHashMap;

import static ru.miacomsoft.EasyWebServer.PostgreQuery.getConnect;
import static ru.miacomsoft.EasyWebServer.PostgreQuery.procedureList;


public class cmpDataset extends Base {
    public cmpDataset(Document doc, Element element) {
        super(doc, element, "teaxtarea");
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
        // String functionName = getMd5Hash(doc.attr("doc_path").replaceAll("/", "_")) + "#" + getMd5Hash(element.attr("name"));
        String functionName = ((doc.attr("doc_path").substring(0, doc.attr("doc_path").length() - 5).substring(doc.attr("rootPath").length())).replaceAll("/", "_") + "___" + element.attr("name")).toLowerCase();
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
                } else {
                    //  HashMap<String, Object> vars = new HashMap<>();   // инициализация переменных
                    //  HashMap<String, Object> res =ServerResourceHandler.javaStrExecut.runFunction(functionName,vars,null); // запуск выполнения скрипта
                    //  System.out.println("==="+res.get("test")+"===");  // парсим результат
                    //  System.out.println(functionName + "-------- Compile OK-------------");
                }
            } else if (query_type.equals("sql")) {
                createSQL(ServerConstant.config.APP_NAME+"_" + functionName, this, element);
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
                    ConcurrentHashMap<String, Object> param = (ConcurrentHashMap<String, Object>) procedureList.get(dataset_name);
                    String prepareCall = (String) param.get("prepareCall");
                    if (session.containsKey("DATABASE")) {
                        // Если в сессии есть информация о подключении к БД, тогда подключаемся
                        ConcurrentHashMap<String, Object> data_base = (ConcurrentHashMap<String, Object>) session.get("DATABASE");
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

    private void createSQL(String functionName, Element elementThis, Element element) {
        if (procedureList.containsKey(functionName) && !ServerConstant.config.DEBUG) {
            // Если функция уже созданна в БД и режим отладки отключен, тогда пропускаем создание новой функции
            return;
        }
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
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
            if (itemElement.text().length() > 0) {  // если вложенный тэг имеет текст, тогда помещаем его в начала скрипта функции
                String beforeCode = itemElement.text().toLowerCase();
                if (beforeCode.indexOf("declare") != -1) { // переносим блок дикларации в заголовок функции
                    declareBlocText = itemElement.text().substring(0, beforeCode.indexOf("begin"));
                    befireCodeBloc = itemElement.text().substring(declareBlocText.length() + "begin".length(), beforeCode.lastIndexOf("end;"));
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
        if (jsonVarStr.length()>0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }
        param.put("vars", varsArr);
        StringBuffer sb = new StringBuffer("CREATE OR REPLACE FUNCTION " + functionName + "(" + jsonVarStr + ")\n" +
                "RETURNS JSON AS\n" +
                "$$\n" +
                declareBlocText +
                "BEGIN\n" +
                befireCodeBloc +
                "\n RETURN (\n" +
                "SELECT COALESCE(json_agg(tempTab), '[]'::json) FROM (\n" +
                element.text().trim().replaceAll(";"," ") +
                ") tempTab\n" +
                ");\n" +
                "END;\n" +
                "$$\n" +
                "LANGUAGE " + language + ";");
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP FUNCTION IF EXISTS " + functionName + ";");
            PreparedStatement createFunctionStatement = conn.prepareStatement(sb.toString());
            createFunctionStatement.execute();

            String varsCollStr = varsColl.toString();
            if (varsCollStr.length()>0) {
                varsCollStr = varsCollStr.substring(0, varsCollStr.length() - 1);
            }
            String prepareCall = "select " + functionName + "(" + varsCollStr + ");";
            CallableStatement selectFunctionStatement = conn.prepareCall(prepareCall);
            // нужно понять почему нет возможности использовать INOUT атребуты
            //int ind=0;
            //for (String varOne : varsArr) {
            //    ind++;
            //    selectFunctionStatement.registerOutParameter(ind, Types.VARCHAR);
            //}
            param.put("selectFunctionStatement", selectFunctionStatement);
            param.put("prepareCall", prepareCall);
            param.put("connect", conn);
            param.put("SQL", sb.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        procedureList.put(functionName, param);
    }

}