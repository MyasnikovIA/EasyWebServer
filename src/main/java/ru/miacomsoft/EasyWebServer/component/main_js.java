package ru.miacomsoft.EasyWebServer.component;

import ru.miacomsoft.EasyWebServer.HttpExchange;
import ru.miacomsoft.EasyWebServer.ServerConstant;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Полная версия main_js библиотеки со всем функционалом
 */
public class main_js {

    protected static final ConcurrentHashMap<String, byte[]> JS_CACHE = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    protected static String cachedHash = null;
    private static long lastModified = 0;

    private static final String JS_SOURCE_CODE =
            "//=============================================================================\n" +
                    "// main_js - полная версия со всем функционалом\n" +
                    "//=============================================================================\n" +
                    "\n" +
                    "(function() {\n" +
                    "    //=============================================================================\n" +
                    "    // Глобальные переменные\n" +
                    "    //=============================================================================\n" +
                    "    window.GLOBAL_VARS = window.GLOBAL_VARS || {};\n" +
                    "    window.GLOBAL_SESSION = window.GLOBAL_SESSION || {};\n" +
                    "    window.GLOBAL_DATA_SET = window.GLOBAL_DATA_SET || {};\n" +
                    "    window.GLOBAL_ACTION = window.GLOBAL_ACTION || {};\n" +
                    "    \n" +
                    "    //=============================================================================\n" +
                    "    // Полная замена jQuery - совместимый API\n" +
                    "    //=============================================================================\n" +
                    "    \n" +
                    "    // Простая реализация селектора и методов jQuery\n" +
                    "    window.$ = function(selector) {\n" +
                    "        // Если передана функция - выполняем при загрузке DOM\n" +
                    "        if (typeof selector === 'function') {\n" +
                    "            if (document.readyState === 'loading') {\n" +
                    "                document.addEventListener('DOMContentLoaded', selector);\n" +
                    "            } else {\n" +
                    "                selector();\n" +
                    "            }\n" +
                    "            return {\n" +
                    "                ready: function(fn) {\n" +
                    "                    if (document.readyState === 'loading') {\n" +
                    "                        document.addEventListener('DOMContentLoaded', fn);\n" +
                    "                    } else {\n" +
                    "                        fn();\n" +
                    "                    }\n" +
                    "                    return this;\n" +
                    "                }\n" +
                    "            };\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Если передана строка - ищем элементы\n" +
                    "        if (typeof selector === 'string') {\n" +
                    "            // Проверяем, начинается ли с '<' (создание элемента)\n" +
                    "            if (selector.trim().startsWith('<') && selector.trim().endsWith('>')) {\n" +
                    "                // Создание HTML элемента\n" +
                    "                var div = document.createElement('div');\n" +
                    "                div.innerHTML = selector;\n" +
                    "                var element = div.firstChild;\n" +
                    "                return new JQueryElement([element]);\n" +
                    "            }\n" +
                    "            \n" +
                    "            // Поиск элементов по селектору\n" +
                    "            var elements;\n" +
                    "            if (selector.startsWith('#')) {\n" +
                    "                // ID селектор\n" +
                    "                var id = selector.substring(1);\n" +
                    "                var element = document.getElementById(id);\n" +
                    "                elements = element ? [element] : [];\n" +
                    "            } else if (selector.startsWith('.')) {\n" +
                    "                // Class селектор\n" +
                    "                var className = selector.substring(1);\n" +
                    "                elements = Array.from(document.getElementsByClassName(className));\n" +
                    "            } else if (selector.startsWith('[name=\"') && selector.endsWith('\"]')) {\n" +
                    "                // Name селектор\n" +
                    "                var name = selector.substring(7, selector.length - 2);\n" +
                    "                elements = Array.from(document.getElementsByName(name));\n" +
                    "            } else if (selector.startsWith('[') && selector.endsWith(']')) {\n" +
                    "                // Attribute селектор (упрощенный)\n" +
                    "                var attrParts = selector.substring(1, selector.length - 1).split('=');\n" +
                    "                if (attrParts.length === 2) {\n" +
                    "                    var attrName = attrParts[0].trim();\n" +
                    "                    var attrValue = attrParts[1].trim().replace(/\"/g, '');\n" +
                    "                    var allElements = document.getElementsByTagName('*');\n" +
                    "                    elements = [];\n" +
                    "                    for (var i = 0; i < allElements.length; i++) {\n" +
                    "                        if (allElements[i].getAttribute(attrName) === attrValue) {\n" +
                    "                            elements.push(allElements[i]);\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    elements = [];\n" +
                    "                }\n" +
                    "            } else {\n" +
                    "                // Tag селектор\n" +
                    "                elements = Array.from(document.getElementsByTagName(selector));\n" +
                    "            }\n" +
                    "            \n" +
                    "            return new JQueryElement(elements);\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Если передан DOM элемент\n" +
                    "        if (selector instanceof HTMLElement) {\n" +
                    "            return new JQueryElement([selector]);\n" +
                    "        }\n" +
                    "        \n" +
                    "        return new JQueryElement([]);\n" +
                    "    };\n" +
                    "    \n" +
                    "    // Класс-обертка для jQuery-подобных методов\n" +
                    "    function JQueryElement(elements) {\n" +
                    "        this.elements = elements || [];\n" +
                    "        this.length = this.elements.length;\n" +
                    "        \n" +
                    "        // Перебор элементов\n" +
                    "        for (var i = 0; i < this.elements.length; i++) {\n" +
                    "            this[i] = this.elements[i];\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Метод each\n" +
                    "        this.each = function(callback) {\n" +
                    "            for (var i = 0; i < this.elements.length; i++) {\n" +
                    "                callback.call(this.elements[i], i, this.elements[i]);\n" +
                    "            }\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод find\n" +
                    "        this.find = function(selector) {\n" +
                    "            var results = [];\n" +
                    "            this.each(function() {\n" +
                    "                var found = this.querySelectorAll(selector);\n" +
                    "                for (var i = 0; i < found.length; i++) {\n" +
                    "                    if (results.indexOf(found[i]) === -1) {\n" +
                    "                        results.push(found[i]);\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return new JQueryElement(results);\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод text\n" +
                    "        this.text = function(value) {\n" +
                    "            if (value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return '';\n" +
                    "                return this.elements[0].textContent || '';\n" +
                    "            } else {\n" +
                    "                // Setter\n" +
                    "                this.each(function() {\n" +
                    "                    this.textContent = value;\n" +
                    "                });\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод html\n" +
                    "        this.html = function(value) {\n" +
                    "            if (value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return '';\n" +
                    "                return this.elements[0].innerHTML || '';\n" +
                    "            } else {\n" +
                    "                // Setter\n" +
                    "                this.each(function() {\n" +
                    "                    this.innerHTML = value;\n" +
                    "                });\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод val\n" +
                    "        this.val = function(value) {\n" +
                    "            if (value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return '';\n" +
                    "                var el = this.elements[0];\n" +
                    "                if (el.tagName === 'SELECT' && el.multiple) {\n" +
                    "                    var values = [];\n" +
                    "                    for (var i = 0; i < el.options.length; i++) {\n" +
                    "                        if (el.options[i].selected) {\n" +
                    "                            values.push(el.options[i].value);\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    return values;\n" +
                    "                }\n" +
                    "                return el.value || '';\n" +
                    "            } else {\n" +
                    "                // Setter\n" +
                    "                this.each(function() {\n" +
                    "                    this.value = value;\n" +
                    "                });\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод attr\n" +
                    "        this.attr = function(name, value) {\n" +
                    "            if (value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return null;\n" +
                    "                return this.elements[0].getAttribute(name);\n" +
                    "            } else {\n" +
                    "                // Setter\n" +
                    "                this.each(function() {\n" +
                    "                    this.setAttribute(name, value);\n" +
                    "                });\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод removeAttr\n" +
                    "        this.removeAttr = function(name) {\n" +
                    "            this.each(function() {\n" +
                    "                this.removeAttribute(name);\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод css\n" +
                    "        this.css = function(prop, value) {\n" +
                    "            if (typeof prop === 'string' && value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return '';\n" +
                    "                return window.getComputedStyle(this.elements[0])[prop];\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (typeof prop === 'string') {\n" +
                    "                // Setter одного свойства\n" +
                    "                this.each(function() {\n" +
                    "                    this.style[prop] = value;\n" +
                    "                });\n" +
                    "            } else {\n" +
                    "                // Setter объекта свойств\n" +
                    "                this.each(function() {\n" +
                    "                    for (var key in prop) {\n" +
                    "                        if (prop.hasOwnProperty(key)) {\n" +
                    "                            this.style[key] = prop[key];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            }\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод show\n" +
                    "        this.show = function() {\n" +
                    "            this.each(function() {\n" +
                    "                this.style.display = '';\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод hide\n" +
                    "        this.hide = function() {\n" +
                    "            this.each(function() {\n" +
                    "                this.style.display = 'none';\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод toggle\n" +
                    "        this.toggle = function() {\n" +
                    "            this.each(function() {\n" +
                    "                if (this.style.display === 'none') {\n" +
                    "                    this.style.display = '';\n" +
                    "                } else {\n" +
                    "                    this.style.display = 'none';\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод addClass\n" +
                    "        this.addClass = function(className) {\n" +
                    "            this.each(function() {\n" +
                    "                if (this.classList) {\n" +
                    "                    this.classList.add(className);\n" +
                    "                } else {\n" +
                    "                    this.className += ' ' + className;\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод removeClass\n" +
                    "        this.removeClass = function(className) {\n" +
                    "            this.each(function() {\n" +
                    "                if (this.classList) {\n" +
                    "                    this.classList.remove(className);\n" +
                    "                } else {\n" +
                    "                    this.className = this.className.replace(new RegExp('(^|\\\\b)' + className.split(' ').join('|') + '(\\\\b|$)', 'gi'), ' ');\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод hasClass\n" +
                    "        this.hasClass = function(className) {\n" +
                    "            if (this.elements.length === 0) return false;\n" +
                    "            if (this.elements[0].classList) {\n" +
                    "                return this.elements[0].classList.contains(className);\n" +
                    "            }\n" +
                    "            return new RegExp('(^| )' + className + '( |$)', 'gi').test(this.elements[0].className);\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод toggleClass\n" +
                    "        this.toggleClass = function(className) {\n" +
                    "            this.each(function() {\n" +
                    "                if (this.classList) {\n" +
                    "                    this.classList.toggle(className);\n" +
                    "                } else {\n" +
                    "                    if (new RegExp('(^| )' + className + '( |$)', 'gi').test(this.className)) {\n" +
                    "                        this.className = this.className.replace(new RegExp('(^|\\\\b)' + className.split(' ').join('|') + '(\\\\b|$)', 'gi'), ' ');\n" +
                    "                    } else {\n" +
                    "                        this.className += ' ' + className;\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод on\n" +
                    "        this.on = function(event, selector, handler) {\n" +
                    "            if (typeof selector === 'function') {\n" +
                    "                handler = selector;\n" +
                    "                selector = null;\n" +
                    "            }\n" +
                    "            \n" +
                    "            this.each(function() {\n" +
                    "                var element = this;\n" +
                    "                \n" +
                    "                if (selector) {\n" +
                    "                    // Делегирование событий\n" +
                    "                    var delegateHandler = function(e) {\n" +
                    "                        var target = e.target;\n" +
                    "                        while (target && target !== element) {\n" +
                    "                            if (target.matches(selector)) {\n" +
                    "                                handler.call(target, e);\n" +
                    "                                return;\n" +
                    "                            }\n" +
                    "                            target = target.parentNode;\n" +
                    "                        }\n" +
                    "                    };\n" +
                    "                    element.addEventListener(event, delegateHandler);\n" +
                    "                    \n" +
                    "                    // Сохраняем обработчик для возможного удаления\n" +
                    "                    if (!element._eventHandlers) element._eventHandlers = {};\n" +
                    "                    if (!element._eventHandlers[event]) element._eventHandlers[event] = [];\n" +
                    "                    element._eventHandlers[event].push({selector: selector, handler: delegateHandler, original: handler});\n" +
                    "                } else {\n" +
                    "                    element.addEventListener(event, handler);\n" +
                    "                    \n" +
                    "                    // Сохраняем обработчик\n" +
                    "                    if (!element._eventHandlers) element._eventHandlers = {};\n" +
                    "                    if (!element._eventHandlers[event]) element._eventHandlers[event] = [];\n" +
                    "                    element._eventHandlers[event].push({handler: handler});\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод off\n" +
                    "        this.off = function(event, selector, handler) {\n" +
                    "            if (typeof selector === 'function') {\n" +
                    "                handler = selector;\n" +
                    "                selector = null;\n" +
                    "            }\n" +
                    "            \n" +
                    "            this.each(function() {\n" +
                    "                var element = this;\n" +
                    "                if (element._eventHandlers && element._eventHandlers[event]) {\n" +
                    "                    var handlers = element._eventHandlers[event];\n" +
                    "                    for (var i = handlers.length - 1; i >= 0; i--) {\n" +
                    "                        var h = handlers[i];\n" +
                    "                        if ((!selector || h.selector === selector) && (!handler || h.original === handler)) {\n" +
                    "                            element.removeEventListener(event, h.handler || h.original);\n" +
                    "                            handlers.splice(i, 1);\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод trigger\n" +
                    "        this.trigger = function(event, data) {\n" +
                    "            var customEvent;\n" +
                    "            if (data) {\n" +
                    "                customEvent = new CustomEvent(event, { detail: data });\n" +
                    "            } else {\n" +
                    "                customEvent = new Event(event);\n" +
                    "            }\n" +
                    "            \n" +
                    "            this.each(function() {\n" +
                    "                this.dispatchEvent(customEvent);\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод prop\n" +
                    "        this.prop = function(name, value) {\n" +
                    "            if (value === undefined) {\n" +
                    "                // Getter\n" +
                    "                if (this.elements.length === 0) return undefined;\n" +
                    "                return this.elements[0][name];\n" +
                    "            } else {\n" +
                    "                // Setter\n" +
                    "                this.each(function() {\n" +
                    "                    this[name] = value;\n" +
                    "                });\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод is\n" +
                    "        this.is = function(selector) {\n" +
                    "            if (this.elements.length === 0) return false;\n" +
                    "            var element = this.elements[0];\n" +
                    "            \n" +
                    "            if (selector.startsWith(':')) {\n" +
                    "                // Псевдо-селекторы\n" +
                    "                if (selector === ':checked') {\n" +
                    "                    return element.checked === true;\n" +
                    "                }\n" +
                    "                if (selector === ':visible') {\n" +
                    "                    return !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length);\n" +
                    "                }\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (element.matches) {\n" +
                    "                return element.matches(selector);\n" +
                    "            }\n" +
                    "            return false;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод filter\n" +
                    "        this.filter = function(selector) {\n" +
                    "            var filtered = [];\n" +
                    "            this.each(function() {\n" +
                    "                if (this.matches && this.matches(selector)) {\n" +
                    "                    filtered.push(this);\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return new JQueryElement(filtered);\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод append\n" +
                    "        this.append = function(content) {\n" +
                    "            this.each(function() {\n" +
                    "                if (typeof content === 'string') {\n" +
                    "                    this.insertAdjacentHTML('beforeend', content);\n" +
                    "                } else if (content instanceof JQueryElement) {\n" +
                    "                    content.each(function() {\n" +
                    "                        this.appendChild(this.cloneNode(true));\n" +
                    "                    });\n" +
                    "                } else if (content instanceof HTMLElement) {\n" +
                    "                    this.appendChild(content.cloneNode(true));\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод remove\n" +
                    "        this.remove = function() {\n" +
                    "            this.each(function() {\n" +
                    "                if (this.parentNode) {\n" +
                    "                    this.parentNode.removeChild(this);\n" +
                    "                }\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Метод empty\n" +
                    "        this.empty = function() {\n" +
                    "            this.each(function() {\n" +
                    "                this.innerHTML = '';\n" +
                    "            });\n" +
                    "            return this;\n" +
                    "        };\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Статический метод ajax\n" +
                    "    $.ajax = function(options) {\n" +
                    "        if (typeof options === 'string') {\n" +
                    "            options = { url: options };\n" +
                    "        }\n" +
                    "        \n" +
                    "        var url = options.url || '';\n" +
                    "        var method = (options.method || options.type || 'GET').toUpperCase();\n" +
                    "        var data = options.data || null;\n" +
                    "        var contentType = options.contentType || 'application/x-www-form-urlencoded';\n" +
                    "        var dataType = options.dataType || 'text';\n" +
                    "        var success = options.success || function() {};\n" +
                    "        var error = options.error || function() {};\n" +
                    "        var complete = options.complete || function() {};\n" +
                    "        \n" +
                    "        var xhr = new XMLHttpRequest();\n" +
                    "        xhr.open(method, url, true);\n" +
                    "        \n" +
                    "        if (contentType) {\n" +
                    "            xhr.setRequestHeader('Content-Type', contentType);\n" +
                    "        }\n" +
                    "        \n" +
                    "        if (options.headers) {\n" +
                    "            for (var key in options.headers) {\n" +
                    "                if (options.headers.hasOwnProperty(key)) {\n" +
                    "                    xhr.setRequestHeader(key, options.headers[key]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        xhr.onreadystatechange = function() {\n" +
                    "            if (xhr.readyState === 4) {\n" +
                    "                if (xhr.status >= 200 && xhr.status < 300) {\n" +
                    "                    var response = xhr.responseText;\n" +
                    "                    if (dataType === 'json') {\n" +
                    "                        try {\n" +
                    "                            response = JSON.parse(response);\n" +
                    "                        } catch (e) {\n" +
                    "                            error(xhr, 'parsererror', e);\n" +
                    "                            complete(xhr, 'error');\n" +
                    "                            return;\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    success(response, 'success', xhr);\n" +
                    "                    complete(xhr, 'success');\n" +
                    "                } else {\n" +
                    "                    error(xhr, 'error', xhr.statusText);\n" +
                    "                    complete(xhr, 'error');\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        if (data && typeof data === 'object' && !(data instanceof FormData)) {\n" +
                    "            if (contentType === 'application/json') {\n" +
                    "                data = JSON.stringify(data);\n" +
                    "            } else {\n" +
                    "                // Преобразуем в URL encoded\n" +
                    "                var encoded = [];\n" +
                    "                for (var key in data) {\n" +
                    "                    if (data.hasOwnProperty(key)) {\n" +
                    "                        encoded.push(encodeURIComponent(key) + '=' + encodeURIComponent(data[key]));\n" +
                    "                    }\n" +
                    "                }\n" +
                    "                data = encoded.join('&');\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        xhr.send(data);\n" +
                    "        \n" +
                    "        return {\n" +
                    "            done: function(callback) {\n" +
                    "                success = callback;\n" +
                    "                return this;\n" +
                    "            },\n" +
                    "            fail: function(callback) {\n" +
                    "                error = callback;\n" +
                    "                return this;\n" +
                    "            },\n" +
                    "            always: function(callback) {\n" +
                    "                complete = callback;\n" +
                    "                return this;\n" +
                    "            }\n" +
                    "        };\n" +
                    "    };\n" +
                    "    \n" +
                    "    //=============================================================================\n" +
                    "    // Глобальные функции для работы с переменными\n" +
                    "    //=============================================================================\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Установка переменной\n" +
                    "     */\n" +
                    "    window.setVar = function(name, value) {\n" +
                    "        var oldValue = window.GLOBAL_VARS[name];\n" +
                    "        window.GLOBAL_VARS[name] = value;\n" +
                    "        \n" +
                    "        // Триггерим событие\n" +
                    "        var event = new CustomEvent('varChanged', { \n" +
                    "            detail: { name: name, oldValue: oldValue, newValue: value } \n" +
                    "        });\n" +
                    "        document.dispatchEvent(event);\n" +
                    "        \n" +
                    "        console.log('Variable set:', name, '=', value);\n" +
                    "        return value;\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Получение переменной\n" +
                    "     */\n" +
                    "    window.getVar = function(name, defValue) {\n" +
                    "        return window.GLOBAL_VARS[name] !== undefined ? window.GLOBAL_VARS[name] : defValue;\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Получение всех переменных\n" +
                    "     */\n" +
                    "    window.getVars = function() {\n" +
                    "        return window.GLOBAL_VARS;\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Установка нескольких переменных\n" +
                    "     */\n" +
                    "    window.setVars = function(obj) {\n" +
                    "        for (var name in obj) {\n" +
                    "            if (obj.hasOwnProperty(name)) {\n" +
                    "                window.setVar(name, obj[name]);\n" +
                    "            }\n" +
                    "        }\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Установка сессионной переменной\n" +
                    "     */\n" +
                    "    window.setSession = function(name, value) {\n" +
                    "        var oldValue = window.GLOBAL_SESSION[name];\n" +
                    "        window.GLOBAL_SESSION[name] = value;\n" +
                    "        \n" +
                    "        // Отправляем на сервер\n" +
                    "        $.ajax({\n" +
                    "            url: '/ru/miacomsoft/EasyWebServer/component/session',\n" +
                    "            method: 'POST',\n" +
                    "            data: JSON.stringify({\n" +
                    "                action: 'set',\n" +
                    "                name: name,\n" +
                    "                value: value\n" +
                    "            }),\n" +
                    "            contentType: 'application/json',\n" +
                    "            success: function() {\n" +
                    "                var event = new CustomEvent('sessionChanged', { \n" +
                    "                    detail: { name: name, oldValue: oldValue, newValue: value } \n" +
                    "                });\n" +
                    "                document.dispatchEvent(event);\n" +
                    "            }\n" +
                    "        });\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Получение сессионной переменной\n" +
                    "     */\n" +
                    "    window.getSession = function(name, defValue) {\n" +
                    "        return window.GLOBAL_SESSION[name] !== undefined ? window.GLOBAL_SESSION[name] : defValue;\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Получение элемента по имени\n" +
                    "     */\n" +
                    "    window.getElementByName = function(name) {\n" +
                    "        var elements = document.getElementsByName(name);\n" +
                    "        return elements.length > 0 ? elements[0] : null;\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Парсинг атрибута vars\n" +
                    "     */\n" +
                    "    window.parseVarsAttribute = function(varsAttr) {\n" +
                    "        if (!varsAttr) return {};\n" +
                    "        try {\n" +
                    "            // Заменяем одинарные кавычки на двойные\n" +
                    "            var jsonStr = varsAttr.replace(/'/g, '\"');\n" +
                    "            return JSON.parse(jsonStr);\n" +
                    "        } catch (e) {\n" +
                    "            console.warn('Error parsing vars attribute:', e);\n" +
                    "            return {};\n" +
                    "        }\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Обновление dataset\n" +
                    "     */\n" +
                    "    window.refreshDataSet = function(name, callback) {\n" +
                    "        console.log('Refreshing dataset:', name);\n" +
                    "        \n" +
                    "        // Получаем элемент dataset\n" +
                    "        var datasetElem = document.querySelector('[name=\"' + name + '\"]');\n" +
                    "        if (!datasetElem) {\n" +
                    "            console.error('Dataset element not found:', name);\n" +
                    "            if (callback) callback(null);\n" +
                    "            return;\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Получаем переменные из атрибута\n" +
                    "        var varsAttr = datasetElem.getAttribute('vars');\n" +
                    "        var vars = window.parseVarsAttribute(varsAttr);\n" +
                    "        \n" +
                    "        // Формируем данные для отправки\n" +
                    "        var requestData = {};\n" +
                    "        for (var key in vars) {\n" +
                    "            if (vars.hasOwnProperty(key)) {\n" +
                    "                var varConfig = vars[key];\n" +
                    "                requestData[key] = {\n" +
                    "                    srctype: varConfig.srctype || 'var',\n" +
                    "                    value: window.GLOBAL_VARS[key] !== undefined ? \n" +
                    "                           window.GLOBAL_VARS[key] : (varConfig.defaultVal || ''),\n" +
                    "                    defaultVal: varConfig.defaultVal || ''\n" +
                    "                };\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Получаем dataset_name из атрибута\n" +
                    "        var datasetName = datasetElem.getAttribute('dataset_name') || name;\n" +
                    "        var queryType = datasetElem.getAttribute('query_type') || 'sql';\n" +
                    "        \n" +
                    "        // Отправляем AJAX запрос\n" +
                    "        $.ajax({\n" +
                    "            url: '/' + name + '?query_type=' + queryType + '&dataset_name=' + datasetName,\n" +
                    "            method: 'POST',\n" +
                    "            data: JSON.stringify(requestData),\n" +
                    "            contentType: 'application/json',\n" +
                    "            dataType: 'json',\n" +
                    "            success: function(response) {\n" +
                    "                console.log('Dataset response:', response);\n" +
                    "                \n" +
                    "                // Обновляем GLOBAL_DATA_SET\n" +
                    "                if (!window.GLOBAL_DATA_SET[name]) {\n" +
                    "                    window.GLOBAL_DATA_SET[name] = { data: [] };\n" +
                    "                }\n" +
                    "                window.GLOBAL_DATA_SET[name].data = response.data || [];\n" +
                    "                \n" +
                    "                // Обновляем переменные из ответа\n" +
                    "                if (response.vars) {\n" +
                    "                    for (var key in response.vars) {\n" +
                    "                        if (response.vars.hasOwnProperty(key)) {\n" +
                    "                            var varObj = response.vars[key];\n" +
                    "                            if (varObj && varObj.value !== undefined) {\n" +
                    "                                window.setVar(key, varObj.value);\n" +
                    "                            }\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "                \n" +
                    "                if (callback) callback(response.data || []);\n" +
                    "            },\n" +
                    "            error: function(xhr, status, error) {\n" +
                    "                console.error('Error refreshing dataset:', error);\n" +
                    "                if (callback) callback(null);\n" +
                    "            }\n" +
                    "        });\n" +
                    "    };\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Выполнение action\n" +
                    "     */\n" +
                    "    window.refreshAction = function(name, callback) {\n" +
                    "        console.log('Refreshing action:', name);\n" +
                    "        \n" +
                    "        var actionElem = document.querySelector('[name=\"' + name + '\"]');\n" +
                    "        if (!actionElem) {\n" +
                    "            console.error('Action element not found:', name);\n" +
                    "            if (callback) callback(null);\n" +
                    "            return;\n" +
                    "        }\n" +
                    "        \n" +
                    "        var varsAttr = actionElem.getAttribute('vars');\n" +
                    "        var vars = window.parseVarsAttribute(varsAttr);\n" +
                    "        \n" +
                    "        var requestData = {};\n" +
                    "        for (var key in vars) {\n" +
                    "            if (vars.hasOwnProperty(key)) {\n" +
                    "                var varConfig = vars[key];\n" +
                    "                requestData[key] = {\n" +
                    "                    srctype: varConfig.srctype || 'var',\n" +
                    "                    value: window.GLOBAL_VARS[key] !== undefined ? \n" +
                    "                           window.GLOBAL_VARS[key] : (varConfig.defaultVal || ''),\n" +
                    "                    defaultVal: varConfig.defaultVal || ''\n" +
                    "                };\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        var actionName = actionElem.getAttribute('action_name') || name;\n" +
                    "        var queryType = actionElem.getAttribute('query_type') || 'sql';\n" +
                    "        \n" +
                    "        $.ajax({\n" +
                    "            url: '/' + name + '?query_type=' + queryType + '&action_name=' + actionName,\n" +
                    "            method: 'POST',\n" +
                    "            data: JSON.stringify(requestData),\n" +
                    "            contentType: 'application/json',\n" +
                    "            dataType: 'json',\n" +
                    "            success: function(response) {\n" +
                    "                console.log('Action response:', response);\n" +
                    "                \n" +
                    "                if (response.vars) {\n" +
                    "                    for (var key in response.vars) {\n" +
                    "                        if (response.vars.hasOwnProperty(key)) {\n" +
                    "                            var varObj = response.vars[key];\n" +
                    "                            if (varObj && varObj.value !== undefined) {\n" +
                    "                                window.setVar(key, varObj.value);\n" +
                    "                            }\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "                \n" +
                    "                if (callback) callback(response);\n" +
                    "            },\n" +
                    "            error: function(xhr, status, error) {\n" +
                    "                console.error('Error refreshing action:', error);\n" +
                    "                if (callback) callback(null);\n" +
                    "            }\n" +
                    "        });\n" +
                    "    };\n" +
                    "    \n" +
                    "    //=============================================================================\n" +
                    "    // D3Api объект\n" +
                    "    //=============================================================================\n" +
                    "    \n" +
                    "    window.D3Api = new function () {\n" +
                    "        var self = this;\n" +
                    "        var GLOBAL_VARS = window.GLOBAL_VARS;\n" +
                    "        var GLOBAL_SESSION = window.GLOBAL_SESSION;\n" +
                    "        var GLOBAL_CTRL = {};\n" +
                    "        \n" +
                    "        // Хранилища для callback-функций отслеживания изменений\n" +
                    "        var VAR_WATCHERS = {};\n" +
                    "        var VALUE_WATCHERS = {};\n" +
                    "        var CAPTION_WATCHERS = {};\n" +
                    "        var SESSION_WATCHERS = {};\n" +
                    "        \n" +
                    "        this.Form = {};\n" +
                    "        this.forms = {};\n" +
                    "        this.GLOBAL_ACTION = window.GLOBAL_ACTION;\n" +
                    "        this.GLOBAL_DATA_SET = window.GLOBAL_DATA_SET;\n" +
                    "        this.platform = 'windows';\n" +
                    "    \n" +
                    "        /**\n" +
                    "         * Инициализация проекта\n" +
                    "         */\n" +
                    "        this.init = function(body) {\n" +
                    "            D3Api.MainDom = body || document.body;\n" +
                    "            D3Api.D3MainContainer = D3Api.MainDom;\n" +
                    "            if (!D3Api.D3MainContainer || D3Api.D3MainContainer.length == 0) {\n" +
                    "                var tagArr = document.getElementsByTagName('html');\n" +
                    "                if (tagArr.length > 0) {\n" +
                    "                    D3Api.D3MainContainer = tagArr[0];\n" +
                    "                }\n" +
                    "            }\n" +
                    "            return D3Api;\n" +
                    "        };\n" +
                    "    \n" +
                    "        /**\n" +
                    "         * Работа с переменными страницы\n" +
                    "         */\n" +
                    "        this.setVar = function(name, value) {\n" +
                    "            return window.setVar(name, value);\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getVar = function(name, defValue) {\n" +
                    "            return window.getVar(name, defValue);\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getVars = function() {\n" +
                    "            return window.getVars();\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.onChangeVar = function(name, callback) {\n" +
                    "            if (typeof callback !== 'function') return null;\n" +
                    "            \n" +
                    "            if (!VAR_WATCHERS[name]) {\n" +
                    "                VAR_WATCHERS[name] = [];\n" +
                    "            }\n" +
                    "            \n" +
                    "            var watcherId = 'var_' + name + '_' + Date.now() + '_' + Math.random();\n" +
                    "            VAR_WATCHERS[name].push({\n" +
                    "                id: watcherId,\n" +
                    "                callback: callback\n" +
                    "            });\n" +
                    "            \n" +
                    "            return watcherId;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.offChangeVar = function(nameOrId, callback) {\n" +
                    "            if (typeof nameOrId === 'string' && nameOrId.indexOf('var_') === 0) {\n" +
                    "                for (var name in VAR_WATCHERS) {\n" +
                    "                    if (VAR_WATCHERS.hasOwnProperty(name)) {\n" +
                    "                        VAR_WATCHERS[name] = VAR_WATCHERS[name].filter(function(w) {\n" +
                    "                            return w.id !== nameOrId;\n" +
                    "                        });\n" +
                    "                        if (VAR_WATCHERS[name].length === 0) {\n" +
                    "                            delete VAR_WATCHERS[name];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            } else if (typeof nameOrId === 'string') {\n" +
                    "                if (callback && typeof callback === 'function') {\n" +
                    "                    if (VAR_WATCHERS[nameOrId]) {\n" +
                    "                        VAR_WATCHERS[nameOrId] = VAR_WATCHERS[nameOrId].filter(function(w) {\n" +
                    "                            return w.callback !== callback;\n" +
                    "                        });\n" +
                    "                        if (VAR_WATCHERS[nameOrId].length === 0) {\n" +
                    "                            delete VAR_WATCHERS[nameOrId];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    delete VAR_WATCHERS[nameOrId];\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        /**\n" +
                    "         * Работа с сессионными переменными\n" +
                    "         */\n" +
                    "        this.setSession = function(name, value) {\n" +
                    "            window.setSession(name, value);\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getSession = function(name, defValue) {\n" +
                    "            return window.getSession(name, defValue);\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.onChangeSession = function(name, callback) {\n" +
                    "            if (typeof callback !== 'function') return null;\n" +
                    "            \n" +
                    "            if (!SESSION_WATCHERS[name]) {\n" +
                    "                SESSION_WATCHERS[name] = [];\n" +
                    "            }\n" +
                    "            \n" +
                    "            var watcherId = 'sess_' + name + '_' + Date.now() + '_' + Math.random();\n" +
                    "            SESSION_WATCHERS[name].push({\n" +
                    "                id: watcherId,\n" +
                    "                callback: callback\n" +
                    "            });\n" +
                    "            \n" +
                    "            return watcherId;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.offChangeSession = function(nameOrId, callback) {\n" +
                    "            if (typeof nameOrId === 'string' && nameOrId.indexOf('sess_') === 0) {\n" +
                    "                for (var name in SESSION_WATCHERS) {\n" +
                    "                    if (SESSION_WATCHERS.hasOwnProperty(name)) {\n" +
                    "                        SESSION_WATCHERS[name] = SESSION_WATCHERS[name].filter(function(w) {\n" +
                    "                            return w.id !== nameOrId;\n" +
                    "                        });\n" +
                    "                        if (SESSION_WATCHERS[name].length === 0) {\n" +
                    "                            delete SESSION_WATCHERS[name];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            } else if (typeof nameOrId === 'string') {\n" +
                    "                if (callback && typeof callback === 'function') {\n" +
                    "                    if (SESSION_WATCHERS[nameOrId]) {\n" +
                    "                        SESSION_WATCHERS[nameOrId] = SESSION_WATCHERS[nameOrId].filter(function(w) {\n" +
                    "                            return w.callback !== callback;\n" +
                    "                        });\n" +
                    "                        if (SESSION_WATCHERS[nameOrId].length === 0) {\n" +
                    "                            delete SESSION_WATCHERS[nameOrId];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    delete SESSION_WATCHERS[nameOrId];\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        /**\n" +
                    "         * Работа с подписями контролов\n" +
                    "         */\n" +
                    "        this.setCaption = function(name, text) {\n" +
                    "            var ctrl = this.getControl(name);\n" +
                    "            if (ctrl && ctrl.length > 0) {\n" +
                    "                var oldText = this.getCaption(name);\n" +
                    "                var newText = text;\n" +
                    "                \n" +
                    "                if (CAPTION_WATCHERS[name]) {\n" +
                    "                    for (var i = 0; i < CAPTION_WATCHERS[name].length; i++) {\n" +
                    "                        var watcher = CAPTION_WATCHERS[name][i];\n" +
                    "                        var result = watcher.callback(newText, oldText);\n" +
                    "                        if (result !== undefined) {\n" +
                    "                            newText = result;\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "                \n" +
                    "                var captionEl = ctrl.find('[block=\"caption\"]');\n" +
                    "                if (captionEl.length > 0) {\n" +
                    "                    captionEl.text(newText);\n" +
                    "                } else {\n" +
                    "                    ctrl.text(newText);\n" +
                    "                }\n" +
                    "                \n" +
                    "                $(document).trigger('captionChanged', [name, newText, oldText]);\n" +
                    "                return true;\n" +
                    "            }\n" +
                    "            return false;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getCaption = function(name) {\n" +
                    "            var ctrl = this.getControl(name);\n" +
                    "            if (ctrl && ctrl.length > 0) {\n" +
                    "                var captionEl = ctrl.find('[block=\"caption\"]');\n" +
                    "                if (captionEl.length > 0) {\n" +
                    "                    return captionEl.text();\n" +
                    "                } else {\n" +
                    "                    return ctrl.text();\n" +
                    "                }\n" +
                    "            }\n" +
                    "            return null;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.onChangeCaption = function(name, callback) {\n" +
                    "            if (typeof callback !== 'function') return null;\n" +
                    "            \n" +
                    "            if (!CAPTION_WATCHERS[name]) {\n" +
                    "                CAPTION_WATCHERS[name] = [];\n" +
                    "            }\n" +
                    "            \n" +
                    "            var watcherId = 'cap_' + name + '_' + Date.now() + '_' + Math.random();\n" +
                    "            CAPTION_WATCHERS[name].push({\n" +
                    "                id: watcherId,\n" +
                    "                callback: callback\n" +
                    "            });\n" +
                    "            \n" +
                    "            return watcherId;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.offChangeCaption = function(nameOrId, callback) {\n" +
                    "            if (typeof nameOrId === 'string' && nameOrId.indexOf('cap_') === 0) {\n" +
                    "                for (var name in CAPTION_WATCHERS) {\n" +
                    "                    if (CAPTION_WATCHERS.hasOwnProperty(name)) {\n" +
                    "                        CAPTION_WATCHERS[name] = CAPTION_WATCHERS[name].filter(function(w) {\n" +
                    "                            return w.id !== nameOrId;\n" +
                    "                        });\n" +
                    "                        if (CAPTION_WATCHERS[name].length === 0) {\n" +
                    "                            delete CAPTION_WATCHERS[name];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            } else if (typeof nameOrId === 'string') {\n" +
                    "                if (callback && typeof callback === 'function') {\n" +
                    "                    if (CAPTION_WATCHERS[nameOrId]) {\n" +
                    "                        CAPTION_WATCHERS[nameOrId] = CAPTION_WATCHERS[nameOrId].filter(function(w) {\n" +
                    "                            return w.callback !== callback;\n" +
                    "                        });\n" +
                    "                        if (CAPTION_WATCHERS[nameOrId].length === 0) {\n" +
                    "                            delete CAPTION_WATCHERS[nameOrId];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    delete CAPTION_WATCHERS[nameOrId];\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        /**\n" +
                    "         * Работа со значениями контролов\n" +
                    "         */\n" +
                    "        this.setValue = function(name, value) {\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            if (ctrlObj.length === 0) return false;\n" +
                    "            \n" +
                    "            var oldValue = this.getValue(name);\n" +
                    "            var newValue = value;\n" +
                    "            \n" +
                    "            if (VALUE_WATCHERS[name]) {\n" +
                    "                for (var i = 0; i < VALUE_WATCHERS[name].length; i++) {\n" +
                    "                    var watcher = VALUE_WATCHERS[name][i];\n" +
                    "                    var result = watcher.callback(newValue, oldValue);\n" +
                    "                    if (result !== undefined) {\n" +
                    "                        newValue = result;\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "            \n" +
                    "            ctrlObj.val(newValue);\n" +
                    "            \n" +
                    "            if (ctrlObj.attr('type') === 'checkbox') {\n" +
                    "                ctrlObj.prop('checked', newValue === 'on' || newValue === true || newValue === 'true');\n" +
                    "            } else if (ctrlObj.attr('type') === 'radio') {\n" +
                    "                ctrlObj.filter('[value=\"' + newValue + '\"]').prop('checked', true);\n" +
                    "            }\n" +
                    "            \n" +
                    "            ctrlObj.trigger('change');\n" +
                    "            $(document).trigger('valueChanged', [name, newValue, oldValue]);\n" +
                    "            return true;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getValue = function(name, defValue) {\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            if (ctrlObj.length === 0) return defValue;\n" +
                    "            \n" +
                    "            if (ctrlObj.attr('type') === 'checkbox') {\n" +
                    "                return ctrlObj.is(':checked');\n" +
                    "            } else if (ctrlObj.attr('type') === 'radio') {\n" +
                    "                var checked = ctrlObj.filter(':checked');\n" +
                    "                return checked.length > 0 ? checked.val() : defValue;\n" +
                    "            } else {\n" +
                    "                var val = ctrlObj.val();\n" +
                    "                return val !== undefined && val !== null ? val : defValue;\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.onChangeValue = function(name, callback) {\n" +
                    "            if (typeof callback !== 'function') return null;\n" +
                    "            \n" +
                    "            if (!VALUE_WATCHERS[name]) {\n" +
                    "                VALUE_WATCHERS[name] = [];\n" +
                    "            }\n" +
                    "            \n" +
                    "            var watcherId = 'val_' + name + '_' + Date.now() + '_' + Math.random();\n" +
                    "            VALUE_WATCHERS[name].push({\n" +
                    "                id: watcherId,\n" +
                    "                callback: callback\n" +
                    "            });\n" +
                    "            \n" +
                    "            return watcherId;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.offChangeValue = function(nameOrId, callback) {\n" +
                    "            if (typeof nameOrId === 'string' && nameOrId.indexOf('val_') === 0) {\n" +
                    "                for (var name in VALUE_WATCHERS) {\n" +
                    "                    if (VALUE_WATCHERS.hasOwnProperty(name)) {\n" +
                    "                        VALUE_WATCHERS[name] = VALUE_WATCHERS[name].filter(function(w) {\n" +
                    "                            return w.id !== nameOrId;\n" +
                    "                        });\n" +
                    "                        if (VALUE_WATCHERS[name].length === 0) {\n" +
                    "                            delete VALUE_WATCHERS[name];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            } else if (typeof nameOrId === 'string') {\n" +
                    "                if (callback && typeof callback === 'function') {\n" +
                    "                    if (VALUE_WATCHERS[nameOrId]) {\n" +
                    "                        VALUE_WATCHERS[nameOrId] = VALUE_WATCHERS[nameOrId].filter(function(w) {\n" +
                    "                            return w.callback !== callback;\n" +
                    "                        });\n" +
                    "                        if (VALUE_WATCHERS[nameOrId].length === 0) {\n" +
                    "                            delete VALUE_WATCHERS[nameOrId];\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                } else {\n" +
                    "                    delete VALUE_WATCHERS[nameOrId];\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        /**\n" +
                    "         * Получение контрола по имени\n" +
                    "         */\n" +
                    "        this.getControl = function(name) {\n" +
                    "            if (GLOBAL_CTRL[name]) {\n" +
                    "                return GLOBAL_CTRL[name];\n" +
                    "            }\n" +
                    "            var ctrl = $('[name=\"' + name + '\"]');\n" +
                    "            if (ctrl.length > 0) {\n" +
                    "                GLOBAL_CTRL[name] = ctrl;\n" +
                    "            }\n" +
                    "            return ctrl;\n" +
                    "        };\n" +
                    "        \n" +
                    "        /**\n" +
                    "         * Инициализация сессии\n" +
                    "         */\n" +
                    "        this.initSession = function() {\n" +
                    "            $.ajax({\n" +
                    "                url: '/ru/miacomsoft/EasyWebServer/component/session',\n" +
                    "                method: 'GET',\n" +
                    "                data: { action: 'getAll' },\n" +
                    "                dataType: 'json',\n" +
                    "                success: function(data) {\n" +
                    "                    window.GLOBAL_SESSION = data;\n" +
                    "                    $(document).trigger('sessionLoaded', [data]);\n" +
                    "                }\n" +
                    "            });\n" +
                    "        };\n" +
                    "        \n" +
                    "        // Методы для обратной совместимости\n" +
                    "        this.setAction = function(name, obj) {\n" +
                    "            this.GLOBAL_ACTION[name] = obj;\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.setActionAuto = function(name) {\n" +
                    "            this.GLOBAL_ACTION[name] = {};\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.setDatasetAuto = function(name) {\n" +
                    "            this.GLOBAL_DATA_SET[name] = {\"data\":[]};\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.setDataset = function(name, obj) {\n" +
                    "            this.GLOBAL_DATA_SET[name] = obj;\n" +
                    "            Object.defineProperty(this.GLOBAL_DATA_SET[name], 'data', {\n" +
                    "               get: function() {\n" +
                    "                 return this._data || [];\n" +
                    "               },\n" +
                    "               set: function(value) {\n" +
                    "                 this._data = value;\n" +
                    "                 $(document).trigger('datasetChanged', [name, value]);\n" +
                    "               }\n" +
                    "            });\n" +
                    "            this.GLOBAL_DATA_SET[name]._data = obj.data || [];\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.getDataset = function(name) {\n" +
                    "            return this.GLOBAL_DATA_SET[name];\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.setControlAuto = function(name, obj) {\n" +
                    "            GLOBAL_CTRL[name] = $(obj);\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setLabel = function(name, text) {\n" +
                    "            if (GLOBAL_CTRL[name]) {\n" +
                    "                GLOBAL_CTRL[name].find('[block=\"label\"]').text(text);\n" +
                    "                return true;\n" +
                    "            } else {\n" +
                    "               var ctrlObj;\n" +
                    "               if (typeof name === 'object') {\n" +
                    "                   ctrlObj = name;\n" +
                    "               } else {\n" +
                    "                   ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "               }\n" +
                    "               var ctrl = ctrlObj.find('[name=\"' + name + '_ctrl\"]');\n" +
                    "               if (ctrl.length === 0) {\n" +
                    "                   ctrl = this.getCtrl(name);\n" +
                    "               }\n" +
                    "               if (ctrl.length > 0) {\n" +
                    "                  ctrl[0].innerText = text;\n" +
                    "                  return true;\n" +
                    "               }\n" +
                    "            }\n" +
                    "            return false;\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.getLabel = function(name) {\n" +
                    "            if (GLOBAL_CTRL[name]) {\n" +
                    "                return GLOBAL_CTRL[name].find('[block=\"label\"]').text();\n" +
                    "            }\n" +
                    "            return null;\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setLabels = function(obj) {\n" +
                    "            for (var name in obj) {\n" +
                    "                if (obj.hasOwnProperty(name)) {\n" +
                    "                    this.setLabel(name, obj[name]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.getLabels = function() {\n" +
                    "            var ctrlList = $('[schema]');\n" +
                    "            var res = {};\n" +
                    "            for (var i = 0; i < ctrlList.length; i++) {\n" +
                    "                var name = ctrlList[i].getAttribute('name');\n" +
                    "                res[name] = this.getLabel(name);\n" +
                    "            }\n" +
                    "            return res;\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.getCtrl = function(name) {\n" +
                    "            var ctrlName = $('[name=\"' + name + '\"]').attr('ctrl');\n" +
                    "            return $('[name=\"' + ctrlName + '\"]');\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.getValues = function() {\n" +
                    "            var ctrlList = $('[schema]');\n" +
                    "            var res = {};\n" +
                    "            if (!ctrlList) return res;\n" +
                    "            for (var i = 0; i < ctrlList.length; i++) {\n" +
                    "                var name = ctrlList[i].getAttribute('name');\n" +
                    "                res[name] = this.getValue(name);\n" +
                    "            }\n" +
                    "            return res;\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setValues = function(obj) {\n" +
                    "            for (var name in obj) {\n" +
                    "                if (obj.hasOwnProperty(name)) {\n" +
                    "                    this.setValue(name, obj[name]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setDisabled = function(name, bool) {\n" +
                    "            bool = (bool == true || bool);\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            var ctrl = this.getCtrl(name);\n" +
                    "            if (ctrlObj.attr('type') === 'accordion') {\n" +
                    "                bool ? ctrl.accordion('disable') : ctrl.accordion('enable');\n" +
                    "            } else if (ctrlObj.attr('type') === 'tabs') {\n" +
                    "                bool ? ctrl.tabs('disable') : ctrl.tabs('enable');\n" +
                    "            } else {\n" +
                    "                if (bool) {\n" +
                    "                    ctrl.prop('disabled', true);\n" +
                    "                } else {\n" +
                    "                    ctrl.prop('disabled', false);\n" +
                    "                    ctrl.removeAttr('disabled');\n" +
                    "                    if (ctrl.hasClass('ui-button-disabled')) ctrl.removeClass('ui-button-disabled');\n" +
                    "                    if (ctrl.hasClass('ui-state-disabled')) ctrl.removeClass('ui-state-disabled');\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setDisableds = function(obj) {\n" +
                    "            for (var name in obj) {\n" +
                    "                if (obj.hasOwnProperty(name)) {\n" +
                    "                    this.setDisabled(name, obj[name]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setDisabledArr = function(arr, val) {\n" +
                    "            for (var ind in arr) {\n" +
                    "                if (arr.hasOwnProperty(ind)) {\n" +
                    "                    var ctrlName = arr[ind].trim();\n" +
                    "                    if (ctrlName.length > 0) this.setDisabled(ctrlName, val);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setVisible = function(name, bool) {\n" +
                    "            bool = (bool == true || bool);\n" +
                    "            var ctrl = D3Api.getControl(name);\n" +
                    "            bool ? ctrl.css('visibility', 'visible') : ctrl.css('visibility', 'hidden');\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.setVisibles = function(obj) {\n" +
                    "            for (var name in obj) {\n" +
                    "                if (obj.hasOwnProperty(name)) {\n" +
                    "                    this.setVisible(name, obj[name]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.setStyle = function(name, propObject) {\n" +
                    "            var ctrl = D3Api.getControl(name);\n" +
                    "            for (var key in propObject) {\n" +
                    "                if (propObject.hasOwnProperty(key)) {\n" +
                    "                    ctrl.css(key, propObject[key]);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.move = function(name, bool) {\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            if (bool) {\n" +
                    "                if (ctrlObj.draggable) ctrlObj.draggable().draggable('enable');\n" +
                    "                if (ctrlObj.resizable) {\n" +
                    "                    ctrlObj.resizable({animate: true});\n" +
                    "                    ctrlObj.resizable('enable');\n" +
                    "                }\n" +
                    "                this.setDisabled(name, true);\n" +
                    "            } else {\n" +
                    "                if (ctrlObj.draggable) ctrlObj.draggable('disable');\n" +
                    "                if (ctrlObj.resizable) ctrlObj.resizable('disable');\n" +
                    "                this.setDisabled(name, false);\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.draggable = function(name, bool) {\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            if (ctrlObj.draggable) {\n" +
                    "                bool ? ctrlObj.draggable().draggable('enable') : ctrlObj.draggable('disable');\n" +
                    "            }\n" +
                    "        };\n" +
                    "    \n" +
                    "        this.resizable = function(name, bool) {\n" +
                    "            var ctrlObj = $('[name=\"' + name + '\"]');\n" +
                    "            if (ctrlObj.resizable) {\n" +
                    "                if (bool) {\n" +
                    "                    ctrlObj.resizable({animate: true});\n" +
                    "                    ctrlObj.resizable('enable');\n" +
                    "                    this.setDisabled(name, true);\n" +
                    "                } else {\n" +
                    "                    ctrlObj.resizable('disable');\n" +
                    "                    this.setDisabled(name, false);\n" +
                    "                }\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        this.msgbox = function(text, buttontext, callback) {\n" +
                    "            buttontext = buttontext || 'OK';\n" +
                    "            var div = document.createElement('div');\n" +
                    "            div.style.cssText = 'position:fixed; top:50%; left:50%; transform:translate(-50%,-50%); background:white; padding:20px; border:1px solid #ccc; box-shadow:0 2px 10px rgba(0,0,0,0.1); z-index:10000;';\n" +
                    "            div.innerHTML = '<div style=\"margin-bottom:15px;\">' + text + '</div>';\n" +
                    "            \n" +
                    "            var button = document.createElement('button');\n" +
                    "            button.textContent = buttontext;\n" +
                    "            button.style.cssText = 'padding:5px 15px; background:#4CAF50; color:white; border:none; border-radius:3px; cursor:pointer;';\n" +
                    "            button.onclick = function() {\n" +
                    "                document.body.removeChild(div);\n" +
                    "                if (callback) callback();\n" +
                    "            };\n" +
                    "            \n" +
                    "            div.appendChild(button);\n" +
                    "            document.body.appendChild(div);\n" +
                    "        };\n" +
                    "    };\n" +
                    "    \n" +
                    "    window.d3 = D3Api.init(document.body);\n" +
                    "    \n" +
                    "    // Автоматическая инициализация при загрузке DOM\n" +
                    "    if (document.readyState === 'loading') {\n" +
                    "        document.addEventListener('DOMContentLoaded', function() {\n" +
                    "            D3Api.initSession();\n" +
                    "            \n" +
                    "            // Инициализация контролов\n" +
                    "            var allElements = document.querySelectorAll('[name][cmptype]');\n" +
                    "            for (var i = 0; i < allElements.length; i++) {\n" +
                    "                var ctrlObj = allElements[i];\n" +
                    "                if (ctrlObj.getAttribute('cmptype')) {\n" +
                    "                    var cmptype = ctrlObj.getAttribute('cmptype').toLowerCase();\n" +
                    "                    if (cmptype === 'action' || cmptype === 'dataset') continue;\n" +
                    "                }\n" +
                    "                var nameCtrl = ctrlObj.getAttribute('name');\n" +
                    "                D3Api.setControlAuto(nameCtrl, ctrlObj);\n" +
                    "            }\n" +
                    "        });\n" +
                    "    } else {\n" +
                    "        // Если DOM уже загружен, выполняем сразу\n" +
                    "        D3Api.initSession();\n" +
                    "        \n" +
                    "        var allElements = document.querySelectorAll('[name][cmptype]');\n" +
                    "        for (var i = 0; i < allElements.length; i++) {\n" +
                    "            var ctrlObj = allElements[i];\n" +
                    "            if (ctrlObj.getAttribute('cmptype')) {\n" +
                    "                var cmptype = ctrlObj.getAttribute('cmptype').toLowerCase();\n" +
                    "                if (cmptype === 'action' || cmptype === 'dataset') continue;\n" +
                    "            }\n" +
                    "            var nameCtrl = ctrlObj.getAttribute('name');\n" +
                    "            D3Api.setControlAuto(nameCtrl, ctrlObj);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    console.log('main_js loaded successfully. Functions available:', {\n" +
                    "        setVar: typeof window.setVar,\n" +
                    "        getVar: typeof window.getVar,\n" +
                    "        getVars: typeof window.getVars,\n" +
                    "        refreshDataSet: typeof window.refreshDataSet,\n" +
                    "        refreshAction: typeof window.refreshAction,\n" +
                    "        D3Api: typeof window.D3Api\n" +
                    "    });\n" +
                    "})();\n";

    private static String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return String.valueOf(input.hashCode());
        }
    }

    private static byte[] getCompiledJs() {
        String currentHash = getMd5Hash(JS_SOURCE_CODE);

        if (cachedHash != null && cachedHash.equals(currentHash) && JS_CACHE.containsKey(currentHash)) {
            return JS_CACHE.get(currentHash);
        }

        synchronized (LOCK) {
            currentHash = getMd5Hash(JS_SOURCE_CODE);
            if (cachedHash != null && cachedHash.equals(currentHash) && JS_CACHE.containsKey(currentHash)) {
                return JS_CACHE.get(currentHash);
            }

            byte[] compiledJs = JS_SOURCE_CODE.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            JS_CACHE.put(currentHash, compiledJs);
            cachedHash = currentHash;
            lastModified = System.currentTimeMillis();

            System.out.println("main_js: библиотека скомпилирована и закэширована (hash: " + currentHash + ")");

            return compiledJs;
        }
    }

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript";

        query.responseHeaders.put("Cache-Control", "public, max-age=86400");
        query.responseHeaders.put("ETag", "\"" + cachedHash + "\"");

        if (query.headers.containsKey("If-None-Match")) {
            String ifNoneMatch = (String) query.headers.get("If-None-Match");
            if (ifNoneMatch.replace("\"", "").equals(cachedHash)) {
                query.responseHeaders.put("Status", "304 Not Modified");
                return new byte[0];
            }
        }

        return getCompiledJs();
    }
}