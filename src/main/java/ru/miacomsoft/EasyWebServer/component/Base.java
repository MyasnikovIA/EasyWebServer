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
        if (doc.select("[cmp=\"common\"]").toString().length() == 0) {
            Elements elements = doc.getElementsByTag("head");
            for (String cssPath : ServerConstant.config.LIB_CSS) {
                if (cssPath.length() == 0) continue;
                elements.append("<link cmp=\"common\" href=\"" + cssPath + "\" rel=\"stylesheet\" type=\"text/css\"/>");
            }
            for (String jsPath : ServerConstant.config.LIB_JS) {
                if (jsPath.length() == 0) continue;
                elements.append("<script cmp=\"common\" src=\"" + jsPath + "\" type=\"text/javascript\"/>");
            }
        }

        // Добавляем main_js библиотеку (только один раз)
        if (!mainJsAdded.get()) {
            synchronized (mainJsAdded) {
                if (!mainJsAdded.get()) {
                    Elements elements = doc.getElementsByTag("head");
                    // Добавляем ссылку на скомпилированную main_js библиотеку
                    elements.append("<script cmp=\"core\" src=\"{component}/main_js\" type=\"text/javascript\"></script>");
                    mainJsAdded.set(true);
                    System.out.println("Base: main_js библиотека добавлена в документ");
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
            if (className.substring(0, 3).equals("cmp")) {
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
        } else {
            return "";
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
        } else {
            return "";
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
            if (val.length() == 0) {
                val = value;
            }
            if ("true".equals(val)) {
                val = name;
            }
            attrs.remove(name);
            return val;
        } else if (value != null) {
            return value;
        } else {
            return "";
        }
    }

    public String getDomAttrRemove(String name, String value, Attributes attrs) {
        String val = "";
        if (attrs.hasKey(name)) {
            val = attrs.get(name);
            if (val.length() == 0) {
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
        } else {
            return "";
        }
    }

    public void copyEventRemove(Attributes attrsSRC, Attributes attrsDst, boolean remove) {
        copyEventRemove(attrsSRC, attrsDst, remove, "on");
    }

    public void copyEventRemove(Attributes attrsSRC, Attributes attrsDst, boolean remove, String prefix) {
        for (Attribute attr : attrsSRC.asList()) {
            if (prefix.equals(attr.getKey().substring(0, prefix.length()))) {
                attrsDst.add(attr.getKey(), attr.getValue());
                if (remove) {
                    attrsSRC.remove(attr.getKey());
                }
            }
        }
    }

    public String getJQueryEventString(String ctrlName, Attributes attrsSRC, boolean removekey) {
        StringBuffer sb = new StringBuffer();
        for (Attribute attr : attrsSRC.asList()) {
            if ("on".equals(attr.getKey().substring(0, 2))) {
                sb.append("\n.on('" + attr.getKey().substring(2) + "', function(event, ui){");
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
        StringBuffer sb = new StringBuffer();
        for (Attribute attr : attrsSRC.asList()) {
            if (!"on".equals(attr.getKey().substring(0, 2))) {
                if (removekey) {
                    attrsSRC.remove(attr.getKey());
                }
                sb.append(attr.getKey() + "=\"" + attr.getValue().replaceAll("\"", "\\\"") + "\"");
            }
        }
        return sb.toString();
    }
    /**
     * Метод для принудительного сброса кэша (можно вызывать при изменении конфигурации)
     */
    public static void clearCache() {
        synchronized (main_js.class) {
            main_js.JS_CACHE.clear();
            main_js.cachedHash = null;
            Base.mainJsAdded.set(false);
            System.out.println("main_js: кэш сброшен");
        }
    }

}