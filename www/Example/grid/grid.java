import org.json.JSONArray;
import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;
import ru.miacomsoft.EasyWebServer.util.onPage;
import java.io.File;

public class grid {
    public byte[] onPage(HttpExchange query) {
        JSONObject paramFilter = new JSONObject();
        if (query.typeQuery.equals("POST") && (query.postByte!=null)) {
            String paramStr = new String(query.postByte);
            for (String paramOne : paramStr.split("&")) {
                String [] param =   paramOne.split("=");
                String key = param[0].replaceAll("\r","").replaceAll("\n","");
                paramFilter.put(key,param[1]);
            }
        }
        String filterName = "";
        if (paramFilter.has("name")) {
            filterName = paramFilter.getString("name").toLowerCase();
        }
        StringBuffer sb = new StringBuffer();
        sb.append(ServerConstant.config.WEBAPP_DIR);
        sb.append(File.separator);
        sb.append("img");
        sb.append(File.separator);
        sb.append("PANO_2024-06-06");
        sb.append(File.separator);
        File filePhoto = new File(sb.toString());
        String absolutePathWebDir = new File (ServerConstant.config.WEBAPP_DIR).getAbsolutePath();
        JSONObject result=new JSONObject();
        result.put("rows",new JSONArray());
        if (filePhoto.exists()) {
            File[] files = filePhoto.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    JSONObject row=new JSONObject();
                    String nameFile = file.getName();
                    String nameFileLowerCase = nameFile.toLowerCase();
                    if ((filterName.length()>0) && (nameFileLowerCase.indexOf(filterName)==-1)) {
                        continue;
                    }
                    row.put("name",nameFile);
                    row.put("absolutePath",file.getAbsolutePath());
                    row.put("fullPath",file.getAbsolutePath().substring(absolutePathWebDir.length()));
                    row.put("filename",nameFile.substring(0,nameFile.lastIndexOf(".")));
                    result.getJSONArray("rows").put(row);
                }
            }
        }
        String JsonText =  result.toString(4);
        query.contentLength = JsonText.length();
        query.mimeType = "application/json";
        return JsonText.getBytes();
    }
}
