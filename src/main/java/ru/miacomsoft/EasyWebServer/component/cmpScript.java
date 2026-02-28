package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class cmpScript extends Base {

    public cmpScript(Document doc, Element element, String tag) {
        super(doc, element, tag);

        Attributes attrs = element.attributes();

        // Получаем атрибуты
        String name = attrs.hasKey("name") ? attrs.get("name") : genUUID();
        String type = RemoveArrKeyRtrn(attrs, "type", "text/javascript");
        String src = RemoveArrKeyRtrn(attrs, "src", "");
        String async = RemoveArrKeyRtrn(attrs, "async", null);
        String defer = RemoveArrKeyRtrn(attrs, "defer", null);
        String charset = RemoveArrKeyRtrn(attrs, "charset", "UTF-8");
        String crossorigin = RemoveArrKeyRtrn(attrs, "crossorigin", "");
        String integrity = RemoveArrKeyRtrn(attrs, "integrity", "");
        String referrerpolicy = RemoveArrKeyRtrn(attrs, "referrerpolicy", "");
        String nomodule = RemoveArrKeyRtrn(attrs, "nomodule", null);
        String nonce = RemoveArrKeyRtrn(attrs, "nonce", "");

        // Получаем содержимое скрипта
        String scriptContent = element.html().trim();

        // Обрабатываем возможные CDATA секции
        if (scriptContent.startsWith("<![CDATA[") && scriptContent.endsWith("]]>")) {
            scriptContent = scriptContent.substring(9, scriptContent.length() - 3).trim();
        }

        // Создаем элемент script
        Element scriptElement = new Element("script");
        scriptElement.attr("name", name);
        scriptElement.attr("cmptype", "Script");
        scriptElement.attr("type", type);
        scriptElement.attr("charset", charset);

        // Добавляем атрибуты если они есть
        if (!src.isEmpty()) {
            scriptElement.attr("src", src);
        }

        if (async != null) {
            scriptElement.attr("async", "async");
        }

        if (defer != null) {
            scriptElement.attr("defer", "defer");
        }

        if (!crossorigin.isEmpty()) {
            scriptElement.attr("crossorigin", crossorigin);
        }

        if (!integrity.isEmpty()) {
            scriptElement.attr("integrity", integrity);
        }

        if (!referrerpolicy.isEmpty()) {
            scriptElement.attr("referrerpolicy", referrerpolicy);
        }

        if (nomodule != null) {
            scriptElement.attr("nomodule", "nomodule");
        }

        if (!nonce.isEmpty()) {
            scriptElement.attr("nonce", nonce);
        }

        // Устанавливаем содержимое скрипта
        if (!scriptContent.isEmpty()) {
            scriptElement.text(scriptContent);
        }

        // Определяем куда добавить скрипт
        String target = RemoveArrKeyRtrn(attrs, "target", "body"); // head или body

        Elements targetElement;
        if (target.equalsIgnoreCase("head")) {
            targetElement = doc.getElementsByTag("head");
        } else {
            targetElement = doc.getElementsByTag("body");
        }

        // Добавляем скрипт в документ
        if (targetElement != null && targetElement.size() > 0) {
            targetElement.append(scriptElement.toString());
        }

        // Очищаем исходный элемент (он будет заменен)
        element.empty();

        // Если скрипт внешний и мы в head, проверяем не подключен ли он уже
        if (!src.isEmpty() && target.equalsIgnoreCase("head")) {
            Elements existingScripts = doc.head().select("script[src='" + src + "']");
            if (existingScripts.size() > 1) {
                // Удаляем дубликат (оставляем первый)
                existingScripts.last().remove();
            }
        }
    }
}