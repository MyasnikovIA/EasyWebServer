package ru.miacomsoft.EasyWebServer.component;

import ru.miacomsoft.EasyWebServer.HttpExchange;
import org.json.JSONObject;
import java.util.Map;

/**
 * Страница для сохранения произвольных объектов в пользовательской сессии
 */
public class session {
    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript"; // Изменить mime ответа
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject vars = new JSONObject(new String(query.postCharBody));
        JSONObject result = new JSONObject("{}");
        if (queryProperty.has("set_session")) {
            session.put(queryProperty.getString("set_session"), vars);
        } else if (queryProperty.has("get_session")) {
            String key = queryProperty.getString("get_session");
            if (session.containsKey(key)) {
                try {
                    result = (JSONObject) session.get(key);
                }catch (Exception e){
                    result.put("Error",e.toString());
                }
            }
        }
        return result.toString().getBytes();
    }
}
