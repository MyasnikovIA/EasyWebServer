package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.ServerConstant;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Base extends Element {
    public String name = "";
    public String id = "";
    public String cmptype = "base";

    // Флаг для отслеживания, была ли уже добавлена библиотека main_js
    private static final AtomicBoolean mainJsAdded = new AtomicBoolean(false);

    public Base(Document doc, Element element, String tag) {
        super(tag);

        // Добавляем общие библиотеки (CSS и JS)
        if (doc.select("[cmp=\"common\"]").isEmpty()) {
            Elements elements = doc.getElementsByTag("head");
            if (!elements.isEmpty()) {
                Element head = elements.first();
                for (String cssPath : ServerConstant.config.LIB_CSS) {
                    if (cssPath == null || cssPath.isEmpty()) continue;
                    head.append("<link cmp=\"common\" href=\"" + cssPath + "\" rel=\"stylesheet\" type=\"text/css\"/>");
                }
                for (String jsPath : ServerConstant.config.LIB_JS) {
                    if (jsPath == null || jsPath.isEmpty()) continue;
                    head.append("<script cmp=\"common\" src=\"" + jsPath + "\" type=\"text/javascript\"></script>");
                }
            }
        }

        // Добавляем main_js библиотеку (только один раз и в самом начале head)
        if (!mainJsAdded.get()) {
            synchronized (mainJsAdded) {
                if (!mainJsAdded.get()) {
                    Elements elements = doc.getElementsByTag("head");
                    if (!elements.isEmpty()) {
                        Element head = elements.first();

                        // Формируем правильный путь к main_js
                        String mainJsPath = "{component}/main_js";

                        // Добавляем ссылку на main_js библиотеку в самое начало head
                        head.prepend("<script cmp=\"core\" src=\"" + mainJsPath + "\" type=\"text/javascript\"></script>");

                        System.out.println("Base: main_js библиотека добавлена в документ по пути: " + mainJsPath);
                    } else {
                        // Если нет head, создаем его
                        Element html = doc.getElementsByTag("html").first();
                        if (html != null) {
                            Element head = html.appendElement("head");
                            String mainJsPath = "/ru/miacomsoft/EasyWebServer/component/main_js";
                            head.append("<script cmp=\"core\" src=\"" + mainJsPath + "\" type=\"text/javascript\"></script>");
                            System.out.println("Base: создан head и добавлена main_js библиотека");
                        } else {
                            System.err.println("Base: Warning - no head or html element found in document");
                        }
                    }
                    mainJsAdded.set(true);
                }
            }
        }

        if (element.hasAttr("name")) {
            this.attr("name", element.attr("name"));
        } else {
            this.attr("name", genUUID());
        }
    }

    public static String genUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void initCmpType(Element element) {
        String className = this.getClass().getSimpleName();
        if (element.hasAttr("cmptype")) {
            this.attr("cmptype", element.attr("cmptype"));
        } else {
            if (className.length() > 3 && className.substring(0, 3).equals("cmp")) {
                this.attr("cmptype", className.substring(3));
            }
        }
    }

    public void initCmpId(Element element) {
        if (element.hasAttr("id")) {
            this.attr("id", element.attr("id"));
            this.removeAttr("id");
        } else {
            this.attr("id", genUUID());
        }
    }

    public String getCssArrKeyRemuve(Attributes arr, String key, boolean remove) {
        String value = "";
        if (arr.hasKey(key)) {
            value = key + ":" + arr.get(key) + ";";
            if (remove) {
                arr.remove(key);
            }
        }
        return value;
    }

    public String getJsArrKeyRemuve(Attributes arr, String key, boolean remove) {
        String value = "";
        if (arr.hasKey(key)) {
            value = ", "+key + ":" + arr.get(key) + "";
            if (remove) {
                arr.remove(key);
            }
        }
        return value;
    }

    public String RemoveArrKeyRtrn(Attributes arr, String key, String defaultValue) {
        String value = "";
        if (arr.hasKey(key)) {
            value = arr.get(key);
            arr.remove(key);
        } else if (defaultValue != null) {
            value = defaultValue;
        } else {
            return null;
        }
        return value;
    }

    public String RemoveArrKeyRtrn(Attributes arr, String key) {
        return RemoveArrKeyRtrn(arr, key, "");
    }

    public String getAttrRemove(String name, String value, Attributes attrs) {
        String val = "";
        if (attrs.hasKey(name)) {
            val = attrs.get(name);
            if (val.isEmpty()) {
                val = value;
            }
            if ("true".equals(val)) {
                val = name;
            }
            attrs.remove(name);
            return val;
        } else if (value != null) {
            return value;
        }
        return "";
    }

    public String getDomAttrRemove(String name, String value, Attributes attrs) {
        String val = "";
        if (attrs.hasKey(name)) {
            val = attrs.get(name);
            if (val.isEmpty()) {
                val = value;
            }
            if ("true".equals(val)) {
                val = name;
            }
            val = val.replace("\"", "\\\"");
            attrs.remove(name);
            return " " + name + "=\"" + val + "\"";
        } else if (value != null) {
            return " " + name + "=\"" + value + "\"";
        }
        return "";
    }

    public void copyEventRemove(Attributes attrsSRC, Attributes attrsDst, boolean remove) {
        copyEventRemove(attrsSRC, attrsDst, remove, "on");
    }

    public void copyEventRemove(Attributes attrsSRC, Attributes attrsDst, boolean remove, String prefix) {
        for (Attribute attr : attrsSRC.asList()) {
            if (attr.getKey().length() >= prefix.length() &&
                    prefix.equals(attr.getKey().substring(0, prefix.length()))) {
                attrsDst.add(attr.getKey(), attr.getValue());
                if (remove) {
                    attrsSRC.remove(attr.getKey());
                }
            }
        }
    }

    public String getJQueryEventString(String ctrlName, Attributes attrsSRC, boolean removekey) {
        StringBuilder sb = new StringBuilder();
        for (Attribute attr : attrsSRC.asList()) {
            if (attr.getKey().length() >= 2 && "on".equals(attr.getKey().substring(0, 2))) {
                sb.append("\n.on('").append(attr.getKey().substring(2)).append("', function(event, ui){");
                sb.append(attr.getValue());
                sb.append(";}) ");
                if (removekey) {
                    attrsSRC.remove(attr.getKey());
                }
            }
        }
        if (sb.length() > 0) {
            return "$('[name=\"" + ctrlName + "\"]')" + sb + ";";
        }
        return "";
    }

    public String getNotEventString(Attributes attrsSRC, boolean removekey) {
        StringBuilder sb = new StringBuilder();
        for (Attribute attr : attrsSRC.asList()) {
            if (attr.getKey().length() < 2 || !"on".equals(attr.getKey().substring(0, 2))) {
                if (removekey) {
                    attrsSRC.remove(attr.getKey());
                }
                sb.append(attr.getKey()).append("=\"").append(attr.getValue().replaceAll("\"", "\\\\\"")).append("\" ");
            }
        }
        return sb.toString().trim();
    }

    public static void clearCache() {
        synchronized (main_js.class) {
            main_js.JS_CACHE.clear();
            main_js.cachedHash = null;
            Base.mainJsAdded.set(false);
            System.out.println("main_js: кэш сброшен");
        }
    }
}