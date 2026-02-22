/**
 * Компонент для ввода текстовой информации
 * Поддерживает различные типы input элементов
 *
 * Примеры использования:
 * <cmpEdit name="login" type="text" required="true"/>
 * <cmpEdit name="phone" placeholder="Телефон"/>
 * <cmpEdit name="email" type="email" required="true"/>
 * <cmpEdit name="birthday" type="date"/>
 * <cmpEdit name="pwd" type="password"/>
 * <cmpEdit name="message" type="textarea"/>
 *
 * <!-- Поле с подписью -->
 * <cmpEdit name="username" label="Имя пользователя" value="John"/>
 */
package ru.miacomsoft.EasyWebServer.component;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Компонент для ввода текстовой информации
 */
public class cmpEdit extends Base {

    // Конструктор с тремя параметрами
    public cmpEdit(Document doc, Element element, String tag) {
        super(doc, element, tag);
        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();

        // Устанавливаем базовые атрибуты
        attrsDst.add("schema", "Edit");
        attrsDst.add("cmptype", "Edit");

        String name = attrs.get("name");
        this.attr("name", name);

        // Определяем тип элемента (input или textarea)
        String type = attrs.hasKey("type") ? attrs.get("type") : "text";
        String tagName = "input";

        if ("textarea".equals(type)) {
            tagName = "textarea";
            attrs.remove("type");
        }

        // Создаем соответствующий элемент
        Element inputElement = new Element(tagName);

        // Копируем стандартные атрибуты
        copyInputAttributes(attrs, inputElement.attributes());

        // Обработка подписи (label)
        String label = RemoveArrKeyRtrn(attrs, "label", "");
        if (!label.isEmpty()) {
            createLabel(doc, name, label, inputElement);
        } else {
            // Если нет подписи, добавляем input напрямую в body
            Elements body = doc.getElementsByTag("body");
            body.append(inputElement.toString());
        }

        // Автоматическое подключение JavaScript библиотеки для cmpEdit
        Elements head = doc.getElementsByTag("head");

        // Проверяем, не подключена ли уже библиотека
        Elements existingScripts = head.select("script[src*='cmpEdit_js']");
        if (existingScripts.isEmpty()) {
            // Добавляем ссылку на JS библиотеку
            String jsPath = "{component}/cmpEdit_js";
            head.append("<script cmp=\"edit-lib\" src=\"" + jsPath + "\" type=\"text/javascript\"></script>");
            System.out.println("cmpEdit: JavaScript library auto-included for edit: " + name);
        }
    }

    /**
     * Копирование стандартных атрибутов input/textarea элемента
     */
    private void copyInputAttributes(Attributes src, Attributes dst) {
        String[] inputAttrs = {
                "type", "value", "placeholder", "size", "maxlength",
                "minlength", "readonly", "disabled", "required",
                "autocomplete", "autofocus", "pattern", "title",
                "rows", "cols", "wrap", "name", "id", "class", "style"
        };

        for (String attr : inputAttrs) {
            String val = RemoveArrKeyRtrn(src, attr, null);
            if (val != null) {
                dst.add(attr, val);
            }
        }

        // По умолчанию добавляем атрибут name
        if (!dst.hasKey("name") && src.hasKey("name")) {
            dst.add("name", src.get("name"));
        }
    }

    /**
     * Создание подписи для поля ввода
     */
    private void createLabel(Document doc, String name, String labelText, Element inputElement) {
        Elements body = doc.getElementsByTag("body");

        StringBuilder label = new StringBuilder();
        label.append("<div class=\"edit-container\" name=\"" + name + "_container\">");
        label.append("  <label for=\"" + name + "\" block=\"caption\">" + labelText + "</label>");
        label.append("  " + inputElement.toString());
        label.append("</div>");

        body.append(label.toString());
    }
}