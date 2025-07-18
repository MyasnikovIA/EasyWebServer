import org.json.JSONArray;
import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;
import ru.miacomsoft.EasyWebServer.util.onPage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;

public class gridPanoListPhoto {
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

            if (files != null) {
                // Сортируем файлы по дате создания
                insertionSort(files);
                for (File file : files) {
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
            } else {
                JSONObject row=new JSONObject();
                row.put("error","Директория пуста или не существует.");
                result.getJSONArray("rows").put(row);
            }
        }
        String JsonText =  result.toString(4);
        query.contentLength = JsonText.length();
        query.mimeType = "application/json";
        return JsonText.getBytes();
    }
    private static void insertionSort(File[] files) {
        for (int i = 1; i < files.length; i++) {
            File key = files[i];
            int j = i - 1;

            while (j >= 0 && getCreationTime(files[j]).compareTo(getCreationTime(key)) > 0) {
                files[j + 1] = files[j];
                j = j - 1;
            }
            files[j + 1] = key;
        }
    }

    private static java.nio.file.attribute.FileTime getCreationTime(File file) {
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attr.creationTime();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
