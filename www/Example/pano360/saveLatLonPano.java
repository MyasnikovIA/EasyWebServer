import org.json.JSONArray;
import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;
import ru.miacomsoft.EasyWebServer.util.onPage;
import java.io.FileWriter;
import java.io.IOException;

import java.io.File;

public class saveLatLonPano {
    @onPage(url="saveLatLonPano.java",ext="json")
    public byte[] onPage(HttpExchange query) {
        JSONObject postObj;
        JSONObject result = new JSONObject();
        if (query.typeQuery.equals("POST") && (query.postByte!=null)) {
            postObj = new JSONObject(new String(query.postByte));
        } else {
            return "{}".getBytes();
        }
        StringBuffer sb = new StringBuffer();
        sb.append(ServerConstant.config.WEBAPP_DIR);
        sb.append(File.separator);
        sb.append("Example");
        sb.append(File.separator);
        sb.append("pano360");
        sb.append(File.separator);
        sb.append("point_info");
        sb.append(File.separator);
        File dirPointInfo = new File(sb.toString());
        if (!dirPointInfo.exists()) {
            dirPointInfo.mkdirs();
        }
        sb.append(postObj.getString("filename"));
        sb.append(".json");
        File filePointInfo = new File(sb.toString());
        String absolutePathWebDir = new File (ServerConstant.config.WEBAPP_DIR).getAbsolutePath();
        if (filePointInfo.exists()) {
            filePointInfo.delete();
        }
        result.put("hotSpotDebug",false);
        result.put("hotPointDebug",true);
        result.put("sceneFadeDuration",1000);
        result.put("default",new JSONObject("{\"firstScene\": \"scene1\"}"));
        result.put("scenes",new JSONObject());
        result.getJSONObject("scenes").put("scene1",new JSONObject());

        JSONObject scene1 = result.getJSONObject("scenes").getJSONObject("scene1");
        scene1.put("title",postObj.getString("filename"));
        scene1.put("panorama",postObj.getString("fullPath"));
        scene1.put("crossOrigin","use-credentials");
        scene1.put("autoLoad",true);
        scene1.put("hotSpots",new JSONArray());
        scene1.put("yaw",postObj.getBigDecimal("yaw"));
        scene1.put("pitch",postObj.getBigDecimal("pitch"));
        if (postObj.has("lat")) scene1.put("lat",postObj.getBigDecimal("lat"));
        if (postObj.has("lon"))scene1.put("lon",postObj.getBigDecimal("lon"));
        System.out.println(result.toString(4));
        try {
            FileWriter writer = new FileWriter(filePointInfo.getAbsolutePath());
            writer.write(result.toString(4));
            writer.close();
        } catch (IOException e) {
            System.out.println("Ошибка при записи в файл");
            e.printStackTrace();
        }
        query.mimeType = "application/json";
        return "[]".getBytes();
    }
}
