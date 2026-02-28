package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class cmpScript extends Base {

    public cmpScript(Document doc, Element element, String tag) {
        super(doc, element, "textarea");

        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();

        // Копируем все атрибуты из исходного элемента
        for (org.jsoup.nodes.Attribute attr : attrs.asList()) {
            attrsDst.add(attr.getKey(), attr.getValue());
        }

        // Устанавливаем атрибуты по умолчанию
        attrsDst.add("cmptype", "Script");

        if (!attrsDst.hasKey("style")) {
            attrsDst.add("style", "display:none;");
        }

        if (!attrsDst.hasKey("name")) {
            attrsDst.add("name", genUUID());
        }

        // Получаем текст скрипта (содержимое между открывающим и закрывающим тегами)
        String scriptContent = element.html().trim();

        // Обрабатываем возможные CDATA секции
        if (scriptContent.startsWith("<![CDATA[") && scriptContent.endsWith("]]>")) {
            scriptContent = scriptContent.substring(9, scriptContent.length() - 3).trim();
        }

        // Устанавливаем текст скрипта
        this.text(scriptContent);

        // Очищаем исходный элемент
        element.empty();

        // Добавляем в body документа
        Elements body = doc.getElementsByTag("body");
        if (body != null && body.size() > 0) {
            body.append(this.toString());
        }
    }
}