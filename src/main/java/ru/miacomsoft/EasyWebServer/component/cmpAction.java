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
    public cmpAction(Document doc, Element element) {
        super(doc, element, "teaxtarea");
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
        String functionName = (doc.attr("doc_path").substring(0, doc.attr("doc_path").length() - 5).substring(doc.attr("rootPath").length())).replaceAll("/", "_") + "___" + element.attr("name");
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
                String srctype = RemoveArrKeyRtrn(attrsItem, "srctype", "");
                String len = RemoveArrKeyRtrn(attrsItem, "len", "");
                String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");
                jsonVar.append("'" + nameItem + "':{");
                jsonVar.append("'src':'" + src + "',");
                jsonVar.append("'srctype':'" + srctype + "'");
                if (defaultVal.length()>0) jsonVar.append("',defaultVal':'" + defaultVal.replaceAll("'", "\\'") + "'");
                if (len.length()>0) jsonVar.append(",'len':'" + len + "'");
                jsonVar.append("},");
            }
        }
        String jsonVarStr = jsonVar.toString();
        if (jsonVarStr.length()>0) {
            jsonVarStr = jsonVarStr.substring(0, jsonVarStr.length() - 1);
        }
        this.attr("vars", "{" + jsonVarStr + "}");
        attrsDst.add("query_type", query_type);
        attrsDst.add("db", db);

        if (element.hasText()) {
            if (query_type.equals("java")) {
                JSONObject infoCompile = new JSONObject();
                if (!ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.compile(functionName, importPacket, jarResourse, element.text().trim(), infoCompile)) {
                    // Разбор ошибок при компиляции
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
                // Создаем SQL функцию в Postgre (пользователя и пароль берем из конфигурационного файла config.ini)
                createSQLFunctionPG(ServerConstant.config.APP_NAME+"_" + functionName, this, element);
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

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript"; // Изменить mime ответа
        Map<String, Object> session = query.session;
        JSONObject result = new JSONObject();
        JSONObject queryProperty = query.requestParam;
        JSONObject vars = new JSONObject(new String(query.postCharBody));
        String query_type = queryProperty.getString("query_type");
        String action_name = ServerConstant.config.APP_NAME+"_" + queryProperty.getString("action_name");
        if (ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.existJavaFunction(action_name)) {
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
                        varFun.put(key, varOne.getString("value"));
                    } else if (varOne.has("defaultVal")) {
                        varFun.put(key, varOne.getString("defaultVal"));
                    } else {
                        varFun.put(key, "");
                    }
                }
            }
            JSONObject resFun = ru.miacomsoft.EasyWebServer.ServerResourceHandler.javaStrExecut.runFunction(action_name, varFun, session, null);
            for (String key : resFun.keySet()) {
                Object keyvalue = resFun.get(key);
                if (vars.has(key)) {
                    vars.getJSONObject(key).put("value", keyvalue);
                } else {
                    JSONObject newVar = new JSONObject();
                    newVar.put("defaultVal", "");
                    newVar.put("get", "");
                    newVar.put("len", "");
                    newVar.put("put", "");
                    newVar.put("src", key);
                    newVar.put("value",keyvalue);
                    vars.put(key, newVar);
                }
            }
            if (resFun.has("JAVA_ERROR")) {
                result.put("ERROR", resFun.get("JAVA_ERROR"));
            }
            // System.out.println("vars " + vars);
        } else if (query_type.equals("sql")) {
            try {
                if (procedureList.containsKey(action_name)) {
                    HashMap<String, Object> param = procedureList.get(action_name);
                    CallableStatement cs;
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
                            System.out.println("cmpAction (conn == null) ");
                            result = new JSONObject();
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
                        // иначе берем подключение системного пользователя
                        // cs = (CallableStatement) param.get("CallableStatement");
                        result.put("redirect", ServerConstant.config.LOGIN_PAGE);
                        return result.toString().getBytes();
                    }
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
                        cs.setString(ind, valueStr);
                    }
                    cs.execute();
                    ind = 0;
                    for (String varNameOne : varsArr) {
                        ind++;
                        String outParam = cs.getString(ind);  // Получение ответа
                        JSONObject varOne = vars.getJSONObject(varNameOne);
                        if (varOne.getString("srctype").equals("session")) {
                            session.put(varNameOne, outParam);
                        } else {
                            varOne.put("value", outParam);
                        }
                    }
                }
            } catch (Exception e) {
                result.put("ERROR", (e.getClass().getName() + ": " + e.getMessage()).split("\n"));
            }
        }
        // ((JSONObject) vars.get("LPU_TEXT")).put("value", "12121212");
        result.put("vars", vars);
        String resultText = result.toString();
        return resultText.getBytes();
    }

    private void createSQLFunctionPG(String functionName, Element elementThis, Element element) {
        //if (procedureList.containsKey(functionName)) {
        //    return;
        //}
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME, ServerConstant.config.DATABASE_USER_PASS);
        StringBuffer vars = new StringBuffer();
        StringBuffer varsColl = new StringBuffer();
        Attributes attrs = element.attributes();
        HashMap<String, Object> param = new HashMap<String, Object>();
        String language = RemoveArrKeyRtrn(attrs, "language", "plpgsql");
        param.put("language", language);
        List<String> varsArr = new ArrayList<>();
        for (int numChild = 0; numChild < element.childrenSize(); numChild++) {
            Element itemElement = element.child(numChild);
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
            String defaultVal = RemoveArrKeyRtrn(attrsItem, "default", "");
            vars.append(nameItem);
            varsArr.add(nameItem);
            vars.append(" INOUT ");
            /*
            if (srctype.equals("session")) {
                vars.append(" INOUT ");
            } else if ((get.length() == 0) && (put.length() == 0)) {
                vars.append(" INOUT ");
            } else if ((get.length() != 0) && (put.length() == 0)) {
                vars.append(" IN ");
            } else if ((get.length() == 0) && (put.length() != 0)) {
                vars.append(" OUT ");
            }
            */
            vars.append(typeVar);
            vars.append(",");
            varsColl.append("?,");
        }
        param.put("vars", varsArr);
        StringBuffer sb = new StringBuffer("" +
                " CREATE OR REPLACE PROCEDURE " +
                functionName +
                "(" +
                vars.toString().substring(0, vars.toString().length() - 1) +
                ") language " +
                language +
                "  AS $$ \n" +
                " BEGIN \n" +
                element.text().trim() +
                " \nEND;$$\n");
        // System.out.println(sb);
        createProcedure(conn, functionName, sb.toString());
        String prepareCall = "call " + functionName + "(" + varsColl.toString().substring(0, varsColl.toString().length() - 1) + ");";
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





/*
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

        String code = "public class HelloWorld {\n" +
                "  public static void main(String[] args) {\n" +
                "    System.out.println(\"Hello, world!\");\n" +
                "  }\n" +
                "}";

        // Get a compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Compile the code
        int result = compiler.run(null, null, null, code);
        if (result != 0) {
            System.out.println("Compilation failed");
            return;
        }

        // Load and execute the compiled class
        Class<?> helloWorldClass = Class.forName("HelloWorld");
        helloWorldClass.getMethod("main", String[].class).invoke(null, (Object) null);

 */
