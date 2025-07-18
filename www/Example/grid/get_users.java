
import org.json.JSONObject;
import ru.miacomsoft.EasyWebServer.HttpExchange;

public class get_users {
   public byte[] onPage(HttpExchange query) {
        JSONObject data = new JSONObject("{\"total\":\"14187\",\"rows\":[{\"id\":\"8739\",\"firstname\":\"fred\",\"lastname\":\"flint000\",\"phone\":\"444\",\"email\":\"test@test.it\"},{\"id\":\"8741\",\"firstname\":\"wer\",\"lastname\":\"redf\",\"phone\":\"1234\",\"email\":\"34545@gm.com\"},{\"id\":\"8742\",\"firstname\":\"wer\",\"lastname\":\"redf\",\"phone\":\"1234\",\"email\":\"9999@gm.com\"},{\"id\":\"8743\",\"firstname\":\"wer\",\"lastname\":\"redf\",\"phone\":\"1234\",\"email\":\"34545@gm.com\"},{\"id\":\"8744\",\"firstname\":\"wer\",\"lastname\":\"redf\",\"phone\":\"1234\",\"email\":\"34545@gm.com\"},{\"id\":\"8745\",\"firstname\":\"mansur\",\"lastname\":\"mohamad\",\"phone\":\"087867756664646\",\"email\":\"w@gmail.com\"},{\"id\":\"8751\",\"firstname\":\"12\",\"lastname\":\"12\",\"phone\":\"122112\",\"email\":\"email@email.com\"},{\"id\":\"8752\",\"firstname\":\"rr\",\"lastname\":\"rr\",\"phone\":\"rere\",\"email\":\"343@qq.com\"},{\"id\":\"8756\",\"firstname\":\"thomas\",\"lastname\":\"max\",\"phone\":\"1234567\",\"email\":\"l@a.de\"},{\"id\":\"8757\",\"firstname\":\"thomas\",\"lastname\":\"max\",\"phone\":\"1234567\",\"email\":\"l@a.de\"}]}");
        StringBuffer sb = new StringBuffer();
        sb.append(data.toString(4));
        query.mimeType = "text/plain";
        return sb.toString().getBytes();
    }
}