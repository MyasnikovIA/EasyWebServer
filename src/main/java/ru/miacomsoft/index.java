package ru.miacomsoft;

import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.util.queryType.Get;
import ru.miacomsoft.EasyWebServer.util.queryType.Post;

public class index {
    @Post(url="index.java", ext="json")
    public JSONObject saveData(HttpExchange query) {
        System.out.println(query);
        // обработка POST данных
        return new JSONObject().put("result", "saved");
    }
    @Get(url="index.java", ext="json")
    public JSONObject getData(HttpExchange query) {
        return new JSONObject().put("status", "ok");
    }
}
