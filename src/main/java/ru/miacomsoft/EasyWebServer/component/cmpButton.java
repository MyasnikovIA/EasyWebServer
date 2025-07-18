package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class cmpButton extends Base{
    public cmpButton(Document doc, Element element, String tag) {
        super(doc, element, tag);
        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();
        attrsDst.add("schema", "Action");
        String name = attrs.get("name");
        this.attr("name", name);
        //attrsDst.add("name", name);
        this.initCmpType(element);
        //StringBuffer sb = new StringBuffer();
        //sb.append("<script>");
        //sb.append("  D3Api.setActionAuto('" + name + "');");
        //sb.append("</script>");
        //Elements elements = doc.getElementsByTag("body");
        //elements.append(sb.toString());
    }
}
