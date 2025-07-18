package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class cmpScript extends Base {
    public cmpScript(Document doc, Element element, String tag) {
        super(doc, element, "textarea");
        Attributes attrsDst = this.attributes();
        attrsDst.add("style", "display:none;");
        String ownText = element.ownText();
        this.text(ownText);
    }
}
