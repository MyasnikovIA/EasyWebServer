/**
 * Компонент для ввода текстовой информации с поддержкой масок
 * Поддерживаемые маски:
 * - date - дата (дд.мм.гггг)
 * - datetime - дата и время (дд.мм.гггг чч:мм)
 * - time - время (чч:мм)
 * - password - пароль (скрытый ввод)
 * - custom - произвольная маска (например: +7 (999) 999-99-99)
 *
 * Примеры использования:
 * <cmpEdit name="phone" mask="+7 (999) 999-99-99" placeholder="Телефон"/>
 * <cmpEdit name="birthday" mask="date" value="01.01.2000"/>
 * <cmpEdit name="login" type="text" required="true"/>
 * <cmpEdit name="pwd" mask="password"/>
 * <!-- Поле с маской телефона -->
 * <cmpEdit name="phone" mask="+7 (999) 999-99-99" placeholder="+7 (___) ___-__-__" label="Телефон"/>
 *
 * <!-- Поле с маской даты, которое можно отключать -->
 * <cmpEdit name="birthday" mask="date" label="Дата рождения"/>
 *
 * <script>
 * // Динамическое управление масками
 * $(function() {
 *     // Переключатель маски для телефона
 *     $('#togglePhoneMask').click(function() {
 *         var currentState = $('[name="phone"]').data('mask-enabled');
 *         D3Api.toggleMask('phone', !currentState);
 *     });
 *
 *     // Смена маски в зависимости от выбора
 *     $('#maskType').change(function() {
 *         var type = $(this).val();
 *         if (type === 'phone') {
 *             D3Api.setMask('dynamicField', '+7 (999) 999-99-99');
 *         } else if (type === 'date') {
 *             D3Api.setMask('dynamicField', '99.99.9999');
 *         } else if (type === 'none') {
 *             D3Api.setMask('dynamicField', null, false);
 *         }
 *     });
 *
 *     // Валидация всех полей перед отправкой
 *     $('#submitBtn').click(function() {
 *         D3Api.validateAllFields().then(function(results) {
 *             var allValid = results.every(r => r.valid);
 *             if (allValid) {
 *                 $('#myForm').submit();
 *             } else {
 *                 console.log('Есть ошибки валидации');
 *             }
 *         });
 *     });
 * });
 * </script>
 *
 */
package ru.miacomsoft.EasyWebServer.component;

import org.json.JSONObject;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Компонент для ввода текстовой информации с поддержкой масок
 * Поддерживаемые маски:
 * - date - дата (дд.мм.гггг)
 * - datetime - дата и время (дд.мм.гггг чч:мм)
 * - time - время (чч:мм)
 * - password - пароль (скрытый ввод)
 * - custom - произвольная маска (например: +7 (999) 999-99-99)
 */
public class cmpEdit extends Base {

    // Флаг для однократной инициализации D3Api расширений
    private static final AtomicBoolean d3ApiExtensionsInitialized = new AtomicBoolean(false);

    // Карта предопределенных масок
    private static final Map<String, String> PREDEFINED_MASKS = new HashMap<>();

    static {
        PREDEFINED_MASKS.put("date", "99.99.9999");
        PREDEFINED_MASKS.put("datetime", "99.99.9999 99:99");
        PREDEFINED_MASKS.put("time", "99:99");
        PREDEFINED_MASKS.put("password", "password"); // специальный тип, не маска
    }

    // ИСПРАВЛЕННЫЙ КОНСТРУКТОР - добавлен третий параметр String tag
    public cmpEdit(Document doc, Element element, String tag) {
        super(doc, element, tag); // передаем tag в super
        Attributes attrs = element.attributes();
        Attributes attrsDst = this.attributes();

        // Устанавливаем базовые атрибуты
        attrsDst.add("schema", "Edit");
        attrsDst.add("cmptype", "Edit");

        String name = attrs.get("name");
        this.attr("name", name);

        // Копируем стандартные атрибуты input
        copyInputAttributes(attrs, attrsDst);

        // Обработка маски ввода
        String mask = RemoveArrKeyRtrn(attrs, "mask", "");
        if (!mask.isEmpty()) {
            processMask(mask, attrsDst);
        }

        // Обработка валидации
        processValidation(attrs, attrsDst);

        // Обработка событий
        processEvents(attrs, attrsDst, name);

        // Обработка стилей
        processStyles(attrs, attrsDst);

        // Обработка подписи (label)
        String label = RemoveArrKeyRtrn(attrs, "label", "");
        if (!label.isEmpty()) {
            createLabel(doc, name, label, attrsDst);
        }

        // Копируем оставшиеся атрибуты
        for (Attribute attr : attrs.asList()) {
            attrsDst.add(attr.getKey(), attr.getValue());
        }

        // Инициализация типа компонента
        this.initCmpType(element);

        // Добавляем JavaScript для инициализации маски (только если есть маска)
        if (!mask.isEmpty() && !mask.equals("password")) {
            addMaskScript(doc, name, mask);
        }

        // Добавляем расширения D3Api (только один раз)
        initializeD3ApiExtensions(doc);
    }

    /**
     * Однократная инициализация расширений D3Api
     */
    private void initializeD3ApiExtensions(Document doc) {
        if (!d3ApiExtensionsInitialized.get()) {
            synchronized (d3ApiExtensionsInitialized) {
                if (!d3ApiExtensionsInitialized.get()) {
                    Elements body = doc.getElementsByTag("body");

                    StringBuilder script = new StringBuilder();
                    script.append("<script>");
                    script.append("(function() {");
                    script.append("  if (window.D3ApiExtensionsInitialized) return;");
                    script.append("  window.D3ApiExtensionsInitialized = true;");
                    script.append("  ");
                    script.append("  // Расширение D3Api для работы с cmpEdit");
                    script.append("  if (typeof D3Api !== 'undefined') {");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Установка маски для поля ввода");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @param {string} mask - Маска (например: '+7 (999) 999-99-99')");
                    script.append("     * @param {boolean} enabled - Включить/выключить маску");
                    script.append("     */");
                    script.append("    D3Api.setMask = function(name, mask, enabled) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return false;");
                    script.append("      ");
                    script.append("      if (enabled === false) {");
                    script.append("        // Выключаем маску");
                    script.append("        input.removeAttr('data-mask');");
                    script.append("        input.removeAttr('data-mask-type');");
                    script.append("        input.off('input.mask');");
                    script.append("        input.off('keydown.mask');");
                    script.append("      } else {");
                    script.append("        // Включаем или обновляем маску");
                    script.append("        input.attr('data-mask', mask);");
                    script.append("        // Переинициализация маски");
                    script.append("        initializeMask(input, mask);");
                    script.append("      }");
                    script.append("      return true;");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Получить текущую маску поля");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @returns {string|null} - Маска или null если маска не установлена");
                    script.append("     */");
                    script.append("    D3Api.getMask = function(name) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return null;");
                    script.append("      return input.data('mask') || null;");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Включить/выключить маску");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @param {boolean} enabled - true - включить, false - выключить");
                    script.append("     */");
                    script.append("    D3Api.toggleMask = function(name, enabled) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return false;");
                    script.append("      ");
                    script.append("      var currentMask = input.data('mask');");
                    script.append("      if (!currentMask) return false;");
                    script.append("      ");
                    script.append("      if (enabled) {");
                    script.append("        input.attr('data-mask-enabled', 'true');");
                    script.append("        initializeMask(input, currentMask);");
                    script.append("      } else {");
                    script.append("        input.attr('data-mask-enabled', 'false');");
                    script.append("        input.off('input.mask');");
                    script.append("        input.off('keydown.mask');");
                    script.append("      }");
                    script.append("      return true;");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Валидация поля на сервере");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @returns {Promise} - Promise с результатом валидации");
                    script.append("     */");
                    script.append("    D3Api.validateField = function(name) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return Promise.reject('Field not found');");
                    script.append("      ");
                    script.append("      var rules = input.data('validate');");
                    script.append("      var value = input.val();");
                    script.append("      var mask = input.data('mask');");
                    script.append("      var maskEnabled = input.data('mask-enabled') !== 'false';");
                    script.append("      ");
                    script.append("      return $.ajax({");
                    script.append("        url: '/{component}/cmpEdit',");
                    script.append("        method: 'POST',");
                    script.append("        data: {");
                    script.append("          action: 'validate',");
                    script.append("          name: name,");
                    script.append("          value: value,");
                    script.append("          rules: rules || '',");
                    script.append("          mask: maskEnabled ? (mask || '') : ''");
                    script.append("        },");
                    script.append("        dataType: 'json'");
                    script.append("      });");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Форматирование значения по маске");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @param {string} value - Значение для форматирования");
                    script.append("     * @returns {Promise} - Promise с отформатированным значением");
                    script.append("     */");
                    script.append("    D3Api.formatWithMask = function(name, value) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return Promise.reject('Field not found');");
                    script.append("      ");
                    script.append("      var mask = input.data('mask');");
                    script.append("      var maskEnabled = input.data('mask-enabled') !== 'false';");
                    script.append("      if (!mask || !maskEnabled) return Promise.resolve({formatted: value});");
                    script.append("      ");
                    script.append("      return $.ajax({");
                    script.append("        url: '/{component}/cmpEdit',");
                    script.append("        method: 'POST',");
                    script.append("        data: {");
                    script.append("          action: 'format',");
                    script.append("          name: name,");
                    script.append("          value: value,");
                    script.append("          mask: mask");
                    script.append("        },");
                    script.append("        dataType: 'json'");
                    script.append("      });");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Получение чистого значения (без маски)");
                    script.append("     * @param {string} name - Имя поля");
                    script.append("     * @returns {string} - Значение без символов маски");
                    script.append("     */");
                    script.append("    D3Api.getRawValue = function(name) {");
                    script.append("      var input = $('[name=\"'+name+'\"]');");
                    script.append("      if (input.length === 0) return '';");
                    script.append("      ");
                    script.append("      var value = input.val();");
                    script.append("      var mask = input.data('mask');");
                    script.append("      var maskEnabled = input.data('mask-enabled') !== 'false';");
                    script.append("      if (!mask || !maskEnabled) return value;");
                    script.append("      ");
                    script.append("      // Удаляем все символы, кроме цифр");
                    script.append("      return value.replace(/[^0-9]/g, '');");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Валидация всех полей на форме");
                    script.append("     * @returns {Promise} - Promise с результатами валидации");
                    script.append("     */");
                    script.append("    D3Api.validateAllFields = function() {");
                    script.append("      var promises = [];");
                    script.append("      $('[schema=\"Edit\"]').each(function() {");
                    script.append("        var name = $(this).attr('name');");
                    script.append("        if (name) {");
                    script.append("          promises.push(D3Api.validateField(name));");
                    script.append("        }");
                    script.append("      });");
                    script.append("      return Promise.all(promises);");
                    script.append("    };");
                    script.append("    ");
                    script.append("    /**");
                    script.append("     * Внутренняя функция инициализации маски");
                    script.append("     */");
                    script.append("    function initializeMask(input, mask) {");
                    script.append("      if (!input || !mask) return;");
                    script.append("      ");
                    script.append("      // Удаляем старые обработчики");
                    script.append("      input.off('input.mask');");
                    script.append("      input.off('keydown.mask');");
                    script.append("      ");
                    script.append("      // Добавляем новые обработчики");
                    script.append("      input.on('input.mask', function(e) {");
                    script.append("        var value = this.value;");
                    script.append("        var masked = '';");
                    script.append("        var valueIndex = 0;");
                    script.append("        var digitCount = 0;");
                    script.append("        ");
                    script.append("        for (var i = 0; i < mask.length; i++) {");
                    script.append("          if (valueIndex >= value.length) break;");
                    script.append("          if (mask[i] === '9') {");
                    script.append("            if (/\\d/.test(value[valueIndex])) {");
                    script.append("              masked += value[valueIndex];");
                    script.append("              valueIndex++;");
                    script.append("              digitCount++;");
                    script.append("            } else {");
                    script.append("              valueIndex++;");
                    script.append("              i--;");
                    script.append("            }");
                    script.append("          } else {");
                    script.append("            masked += mask[i];");
                    script.append("            if (valueIndex < value.length && value[valueIndex] === mask[i]) {");
                    script.append("              valueIndex++;");
                    script.append("            }");
                    script.append("          }");
                    script.append("        }");
                    script.append("        ");
                    script.append("        if (this.value !== masked) {");
                    script.append("          this.value = masked;");
                    script.append("        }");
                    script.append("      });");
                    script.append("      ");
                    script.append("      input.on('keydown.mask', function(e) {");
                    script.append("        // Разрешаем backspace, delete, стрелки, tab, enter");
                    script.append("        if (e.keyCode === 8 || e.keyCode === 46 || e.keyCode === 37 || e.keyCode === 39 || e.keyCode === 9 || e.keyCode === 13) {");
                    script.append("          return;");
                    script.append("        }");
                    script.append("        // Разрешаем цифры");
                    script.append("        if (e.keyCode < 48 || e.keyCode > 57) {");
                    script.append("          e.preventDefault();");
                    script.append("        }");
                    script.append("      });");
                    script.append("      ");
                    script.append("      input.attr('data-mask-enabled', 'true');");
                    script.append("    }");
                    script.append("  }");
                    script.append("})();");
                    script.append("</script>");

                    body.append(script.toString());
                    d3ApiExtensionsInitialized.set(true);
                    System.out.println("cmpEdit: D3Api расширения инициализированы");
                }
            }
        }
    }

    /**
     * Копирование стандартных атрибутов input элемента
     */
    private void copyInputAttributes(Attributes src, Attributes dst) {
        String[] inputAttrs = {
                "type", "value", "placeholder", "size", "maxlength",
                "minlength", "readonly", "disabled", "required",
                "autocomplete", "autofocus", "pattern", "title"
        };

        for (String attr : inputAttrs) {
            String val = RemoveArrKeyRtrn(src, attr, null);
            if (val != null) {
                if (attr.equals("type") && val.equals("password")) {
                    dst.add("type", "password");
                } else {
                    dst.add(attr, val);
                }
            }
        }

        // По умолчанию тип text
        if (!dst.hasKey("type")) {
            dst.add("type", "text");
        }
    }

    /**
     * Обработка маски ввода
     */
    private void processMask(String mask, Attributes dst) {
        String inputType = dst.hasKey("type") ? dst.get("type") : "text";

        // Если тип уже password, игнорируем маску
        if (inputType.equals("password")) {
            return;
        }

        // Проверяем предопределенные маски
        if (PREDEFINED_MASKS.containsKey(mask)) {
            String predefinedMask = PREDEFINED_MASKS.get(mask);
            if (predefinedMask.equals("password")) {
                dst.add("type", "password");
            } else {
                dst.add("data-mask", predefinedMask);
                dst.add("data-mask-type", mask);
                dst.add("data-mask-enabled", "true");
            }
        } else {
            // Произвольная маска
            dst.add("data-mask", mask);
            dst.add("data-mask-type", "custom");
            dst.add("data-mask-enabled", "true");
        }
    }

    /**
     * Обработка валидации
     */
    private void processValidation(Attributes src, Attributes dst) {
        Map<String, String> validations = new HashMap<>();
        String[] validationAttrs = {
                "min", "max", "minlength", "maxlength", "pattern",
                "required", "email", "url", "number", "integer"
        };

        for (String attr : validationAttrs) {
            String val = RemoveArrKeyRtrn(src, attr, null);
            if (val != null) {
                if (attr.equals("required") && val.equals("true")) {
                    validations.put("required", "true");
                } else if (attr.equals("email") && val.equals("true")) {
                    validations.put("type", "email");
                } else if (attr.equals("url") && val.equals("true")) {
                    validations.put("type", "url");
                } else if (attr.equals("number") && val.equals("true")) {
                    validations.put("type", "number");
                } else if (attr.equals("integer") && val.equals("true")) {
                    validations.put("type", "integer");
                } else {
                    validations.put(attr, val);
                }
            }
        }

        if (!validations.isEmpty()) {
            dst.add("data-validate", validations.toString()
                    .replace("{", "")
                    .replace("}", "")
                    .replace("=", ":"));
        }
    }

    /**
     * Обработка событий
     */
    private void processEvents(Attributes src, Attributes dst, String name) {
        StringBuffer events = new StringBuffer();

        for (Attribute attr : src.asList()) {
            if (attr.getKey().startsWith("on")) {
                String event = attr.getKey().substring(2);
                String handler = attr.getValue();

                events.append("$('[name=\"" + name + "\"]').on('" + event + "', function(event) {")
                        .append(handler)
                        .append("});");

                src.remove(attr.getKey());
            }
        }

        if (events.length() > 0) {
            dst.add("data-events", events.toString());
        }
    }

    /**
     * Обработка стилей
     */
    private void processStyles(Attributes src, Attributes dst) {
        String[] styleAttrs = {"width", "height", "class", "style"};

        for (String attr : styleAttrs) {
            String val = RemoveArrKeyRtrn(src, attr, null);
            if (val != null) {
                dst.add(attr, val);
            }
        }
    }

    /**
     * Создание подписи для поля ввода
     */
    private void createLabel(Document doc, String name, String labelText, Attributes attrs) {
        Elements body = doc.getElementsByTag("body");

        StringBuilder label = new StringBuilder();
        label.append("<div class=\"edit-container\" name=\"" + name + "_container\">");
        label.append("  <label for=\"" + name + "\" block=\"caption\">" + labelText + "</label>");
        label.append("  <input type=\"" + attrs.get("type") + "\" name=\"" + name + "\"");

        // Копируем атрибуты в input внутри label
        for (Attribute attr : attrs.asList()) {
            if (!attr.getKey().equals("type") && !attr.getKey().equals("name")) {
                label.append(" " + attr.getKey() + "=\"" + attr.getValue() + "\"");
            }
        }

        label.append(">");
        label.append("</div>");

        body.append(label.toString());
    }

    /**
     * Добавление JavaScript для инициализации маски
     */
    private void addMaskScript(Document doc, String name, String mask) {
        Elements body = doc.getElementsByTag("body");

        StringBuilder script = new StringBuilder();
        script.append("<script>");
        script.append("$(function() {");
        script.append("  var input = $('[name=\"" + name + "\"]');");
        script.append("  if (input.length === 0) return;");
        script.append("  var mask = input.data('mask');");
        script.append("  if (mask && window.initializeMask) {");
        script.append("    window.initializeMask(input, mask);");
        script.append("  }");
        script.append("});");
        script.append("</script>");

        body.append(script.toString());
    }

    /**
     * Метод для обработки запросов от клиента
     */
    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/json";
        JSONObject result = new JSONObject();

        String action = query.requestParam.optString("action", "");
        String name = query.requestParam.optString("name", "");
        String value = query.requestParam.optString("value", "");
        String rules = query.requestParam.optString("rules", "");
        String mask = query.requestParam.optString("mask", "");

        switch (action) {
            case "validate":
                result = validateField(name, value, rules, mask);
                break;
            case "format":
                result = formatValue(value, mask);
                break;
            default:
                result.put("error", "Unknown action");
                result.put("success", false);
        }

        return result.toString().getBytes();
    }

    /**
     * Валидация поля
     */
    private static JSONObject validateField(String name, String value, String rules, String mask) {
        JSONObject result = new JSONObject();
        JSONObject errors = new JSONObject();
        boolean isValid = true;

        // Применяем маску к значению для валидации
        String rawValue = value;
        if (!mask.isEmpty() && !mask.equals("password")) {
            rawValue = value.replaceAll("[^0-9]", "");
        }

        if (rules.contains("required") && rawValue.isEmpty()) {
            isValid = false;
            errors.put("required", "Поле обязательно для заполнения");
        }

        if (rules.contains("email") && !rawValue.isEmpty()) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!rawValue.matches(emailRegex)) {
                isValid = false;
                errors.put("email", "Некорректный email адрес");
            }
        }

        if (rules.contains("url") && !rawValue.isEmpty()) {
            String urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
            if (!rawValue.matches(urlRegex)) {
                isValid = false;
                errors.put("url", "Некорректный URL");
            }
        }

        if (rules.contains("number") && !rawValue.isEmpty()) {
            try {
                Double.parseDouble(rawValue);
            } catch (NumberFormatException e) {
                isValid = false;
                errors.put("number", "Должно быть число");
            }
        }

        if (rules.contains("integer") && !rawValue.isEmpty()) {
            try {
                Integer.parseInt(rawValue);
            } catch (NumberFormatException e) {
                isValid = false;
                errors.put("integer", "Должно быть целое число");
            }
        }

        // Проверка длины
        if (rules.contains("minlength")) {
            String[] ruleParts = rules.split(",");
            for (String part : ruleParts) {
                if (part.startsWith("minlength:")) {
                    int minLength = Integer.parseInt(part.split(":")[1]);
                    if (rawValue.length() < minLength) {
                        isValid = false;
                        errors.put("minlength", "Минимальная длина: " + minLength);
                    }
                    break;
                }
            }
        }

        if (rules.contains("maxlength")) {
            String[] ruleParts = rules.split(",");
            for (String part : ruleParts) {
                if (part.startsWith("maxlength:")) {
                    int maxLength = Integer.parseInt(part.split(":")[1]);
                    if (rawValue.length() > maxLength) {
                        isValid = false;
                        errors.put("maxlength", "Максимальная длина: " + maxLength);
                    }
                    break;
                }
            }
        }

        // Проверка маски
        if (!mask.isEmpty() && !mask.equals("password") && !rawValue.isEmpty()) {
            int expectedDigits = countDigitsInMask(mask);
            if (rawValue.length() != expectedDigits) {
                isValid = false;
                errors.put("mask", "Неполное заполнение маски");
            }
        }

        result.put("valid", isValid);
        result.put("errors", errors);
        result.put("name", name);
        result.put("success", true);

        return result;
    }

    /**
     * Подсчет количества цифр в маске
     */
    private static int countDigitsInMask(String mask) {
        int count = 0;
        for (char c : mask.toCharArray()) {
            if (c == '9') {
                count++;
            }
        }
        return count;
    }

    /**
     * Форматирование значения по маске
     */
    private static JSONObject formatValue(String value, String mask) {
        JSONObject result = new JSONObject();

        String formatted = applyMask(value, mask);
        result.put("formatted", formatted);
        result.put("success", true);

        return result;
    }

    /**
     * Применение маски к значению
     */
    private static String applyMask(String value, String mask) {
        if (mask.isEmpty() || mask.equals("password") || value.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        int valueIndex = 0;
        String digits = value.replaceAll("[^0-9]", "");

        for (int i = 0; i < mask.length() && valueIndex < digits.length(); i++) {
            char maskChar = mask.charAt(i);

            if (maskChar == '9') {
                result.append(digits.charAt(valueIndex));
                valueIndex++;
            } else {
                result.append(maskChar);
                // Если следующий символ во входящем значении совпадает с литералом, пропускаем его
                if (valueIndex < digits.length() && value.charAt(valueIndex) == maskChar) {
                    valueIndex++;
                }
            }
        }

        return result.toString();
    }
}