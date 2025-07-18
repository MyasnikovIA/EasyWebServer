package ru.miacomsoft.EasyWebServer.component;

public class main_js {
    // <script cmp="common" src="{component}/main_js" type="text/javascript"></script>
    public static byte[] onPage(ru.miacomsoft.EasyWebServer.HttpExchange query) {
        query.mimeType = "application/javascript";
        StringBuffer sb = new StringBuffer("");
        return sb.toString().getBytes();
    }
}
