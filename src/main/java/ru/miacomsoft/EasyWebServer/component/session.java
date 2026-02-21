package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import java.util.Map;

/**
 * Компонент для работы с сессионными переменными
 * Поддерживает srctype="session"
 */
public class session {
    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json";
        Map<String, Object> session = query.session;
        JSONObject result = new JSONObject();

        try {
            String action = query.requestParam.optString("action", "");
            JSONObject data = new JSONObject(new String(query.postCharBody));

            switch (action) {
                case "set":
                    String name = data.optString("name", "");
                    Object value = data.opt("value");
                    if (!name.isEmpty()) {
                        session.put(name, value);
                        result.put("success", true);
                        result.put("name", name);
                        result.put("value", value);
                    }
                    break;

                case "get":
                    name = data.optString("name", "");
                    if (session.containsKey(name)) {
                        result.put(name, session.get(name));
                    }
                    break;

                case "getAll":
                    // Возвращаем все сессионные переменные
                    for (Map.Entry<String, Object> entry : session.entrySet()) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                    break;

                case "remove":
                    name = data.optString("name", "");
                    if (session.containsKey(name)) {
                        session.remove(name);
                        result.put("success", true);
                    }
                    break;

                case "clear":
                    session.clear();
                    result.put("success", true);
                    break;
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result.toString().getBytes();
    }
}