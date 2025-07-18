package ru.miacomsoft;

import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.util.onTerminal;
import ru.miacomsoft.EasyWebServer.util.queryType.Get;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static ru.miacomsoft.EasyWebServer.HttpExchange.BROADCAST_MESSAGE_LIST;
import static ru.miacomsoft.device_esp8266_web.GLOBAL_LIST_DEVICE;

public class device_esp8266_terminal {

    /*
TERM /device_esp8266_terminal_message.java
device_name: HowerBord_001
UserName: user1


TERM /device_esp8266_terminal_message_send.java
device_name: Oper_001
device_name_connect: HowerBord_001

     */

    public static HashMap<String,HttpExchange> GLOBAL_LIST_DEVICE_CONNECT = new HashMap<>();

    @onTerminal(url="device_esp8266_terminal_message.java")
    public void onPage(HttpExchange query) {
        String DeviceName = "";
        String UserName = "";
        String lastCommandName = "";
        String DeviceNameSendTo = "";
        System.out.println("query.headers " + query.headers);
        if (query.headers.containsKey("device_name")) {
            DeviceName = (String) query.headers.get("device_name");
        }
        GLOBAL_LIST_DEVICE_CONNECT.put(DeviceName, query);
        query.write(("{\"register\":\"" + DeviceName + "\"}").getBytes());
        query.write("\r\n".getBytes());
        while (true) {
            String message = query.readLineTerm();
            if(message==null) {
                break;
            }
            if (query.queryPtP != null) {
                query.queryPtP.write((message).getBytes());
            }
        }
    }

    @onTerminal(url="device_esp8266_terminal_message_send.java")
    public void onSend(HttpExchange query) {
        String DeviceName = "";
        String DeviceNameConnect = "";
        if (query.headers.containsKey("device_name")) {
            DeviceName = (String) query.headers.get("device_name");
        }
        if (query.headers.containsKey("device_name_connect")) {
            DeviceNameConnect = (String) query.headers.get("device_name_connect");
        }
        if (!GLOBAL_LIST_DEVICE_CONNECT.containsKey(DeviceNameConnect)) {
            query.write(("{\"error\":\"" + DeviceNameConnect + "\",\"message\":\"устройство не найдено\"}").getBytes());
        }
        GLOBAL_LIST_DEVICE_CONNECT.put(DeviceName, query);
        HttpExchange querySend = (HttpExchange) GLOBAL_LIST_DEVICE_CONNECT.get(DeviceNameConnect);
        querySend.queryPtP = query;
        query.queryPtP = querySend;
        query.write(("{\"register\":\"" + DeviceName + "\"}").getBytes());
        while (true) {
            String message = query.readLineTerm();
            if(message==null) {
                break;
            }
            querySend.write((message).getBytes());
        }
    }

    @Get(url="device_esp8266_view_terminal.java",ext="json")
    public JSONObject onPageView(HttpExchange query) {
        System.out.println(BROADCAST_MESSAGE_LIST);
        System.out.println(GLOBAL_LIST_DEVICE_CONNECT);
        JSONObject res = new JSONObject();
        for(Map.Entry<String, HttpExchange> entry : GLOBAL_LIST_DEVICE_CONNECT.entrySet()) {
            String key = entry.getKey();
            HttpExchange value = entry.getValue();
            res.put(key,value.toString());
        }
        return res;
    }

}
