package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;

import java.util.Map;

/**
 * Страница для сохранения произвольных объектов в пользовательской сессии
 */
public class session {
    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json"; // Изменено с application/javascript на application/json
        Map<String, Object> session = query.session;
        JSONObject queryProperty = query.requestParam;
        JSONObject result = new JSONObject();

        // Обработка POST запросов
        if ("POST".equalsIgnoreCase(query.typeQuery)) {
            try {
                String postBodyStr = new String(query.postCharBody);
                if (postBodyStr != null && !postBodyStr.isEmpty()) {
                    JSONObject vars = new JSONObject(postBodyStr);

                    if (queryProperty.has("set_session")) {
                        String key = queryProperty.getString("set_session");
                        session.put(key, vars);
                        result.put("success", true);
                        result.put("message", "Session saved: " + key);
                    }
                }
            } catch (Exception e) {
                result.put("error", e.getMessage());
            }
        }
        // Обработка GET запросов
        else if ("GET".equalsIgnoreCase(query.typeQuery)) {
            if (queryProperty.has("get_session")) {
                String key = queryProperty.getString("get_session");
                if (session.containsKey(key)) {
                    try {
                        Object value = session.get(key);
                        if (value instanceof JSONObject) {
                            result = (JSONObject) value;
                        } else if (value instanceof String) {
                            result.put("value", value);
                        } else {
                            result.put("value", String.valueOf(value));
                        }
                    } catch (Exception e) {
                        result.put("error", e.toString());
                    }
                }
            } else if (queryProperty.has("action") && "getAll".equals(queryProperty.getString("action"))) {
                // Возвращаем все сессионные данные
                for (Map.Entry<String, Object> entry : session.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof JSONObject) {
                        result.put(key, value);
                    } else {
                        result.put(key, String.valueOf(value));
                    }
                }
            }
        }

        return result.toString().getBytes();
    }
}