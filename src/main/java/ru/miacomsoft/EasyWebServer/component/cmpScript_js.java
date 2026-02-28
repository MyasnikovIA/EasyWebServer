package ru.miacomsoft.EasyWebServer.component;

import ru.miacomsoft.EasyWebServer.HttpExchange;

/**
 * JavaScript библиотека для компонента cmpScript
 * Обеспечивает динамическую загрузку и управление скриптами
 */
public class cmpScript_js {

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript";

        StringBuilder js = new StringBuilder();
        js.append("""
                (function() {
                    if (window.cmpScriptInitialized) return;
                    
                    // Функция ожидания инициализации D3Api
                    function waitForD3Api(callback) {
                        function checkD3Api() {
                            if (typeof window.D3Api !== 'undefined' && 
                                window.D3Api !== null) {
                                callback();
                                return;
                            }
                            requestAnimationFrame(checkD3Api);
                        }
                        checkD3Api();
                    }
                    
                    function initialize() {
                        if (window.cmpScriptInitialized) return;
                        window.cmpScriptInitialized = true;
                        
                        console.log('cmpScript: JavaScript library initialized');
                        
                        // ============== ИНИЦИАЛИЗАЦИЯ ОБЪЕКТА FORM ==============
                        // Создаем объект Form в глобальной области видимости, если его еще нет
                        if (typeof window.Form === 'undefined') {
                            window.Form = {};
                            console.log('cmpScript: Form object created');
                        }
                        
                        // Добавляем базовые методы для Form
                        window.Form.getVar = function(name, defValue) {
                            // Если D3Api уже загружен, используем его методы
                            if (window.D3Api && window.D3Api.getVar) {
                                return window.D3Api.getVar(name, defValue);
                            }
                            return defValue;
                        };
                        
                        window.Form.setVar = function(name, value) {
                            if (window.D3Api && window.D3Api.setVar) {
                                window.D3Api.setVar(name, value);
                            }
                        };
                        
                        window.Form.getValue = function(name, defValue) {
                            if (window.D3Api && window.D3Api.getValue) {
                                return window.D3Api.getValue(name, defValue);
                            }
                            return defValue;
                        };
                        
                        window.Form.setValue = function(name, value) {
                            if (window.D3Api && window.D3Api.setValue) {
                                window.D3Api.setValue(name, value);
                            }
                        };
                        
                        window.Form.getCaption = function(name) {
                            if (window.D3Api && window.D3Api.getCaption) {
                                return window.D3Api.getCaption(name);
                            }
                            return '';
                        };
                        
                        window.Form.setCaption = function(name, value) {
                            if (window.D3Api && window.D3Api.setCaption) {
                                window.D3Api.setCaption(name, value);
                            }
                        };
                        
                        window.Form.close = function(result) {
                            if (window.D3Api && window.D3Api.close) {
                                window.D3Api.close(result);
                            } else if (window.close) {
                                window.close(result);
                            }
                        };
                        
                        // Добавляем метод для получения DOM формы
                        window.Form.getDOM = function() {
                            return document.querySelector('[cmptype="Form"]');
                        };
                        
                        console.log('cmpScript: Form object initialized with basic methods');
                        
                        // Проверяем наличие ControlBaseProperties и определяем его если нет
                        if (typeof D3Api.ControlBaseProperties !== 'function') {
                            console.log('cmpScript: Defining D3Api.ControlBaseProperties');
                            D3Api.ControlBaseProperties = function(controlAPI) {
                                this._API_ = controlAPI || D3Api.BaseCtrl;
                                this.name = {get: D3Api.BaseCtrl.getName, set: D3Api.BaseCtrl.setName, type: 'string'};
                                this.value = {get: D3Api.BaseCtrl.getValue, set: D3Api.BaseCtrl.setValue, type: 'string'};
                                this.caption = {get: D3Api.BaseCtrl.getCaption, set: D3Api.BaseCtrl.setCaption, type: 'string'};
                                this.width = {get: D3Api.BaseCtrl.getWidth, set: D3Api.BaseCtrl.setWidth, type: 'string'};
                                this.height = {get: D3Api.BaseCtrl.getHeight, set: D3Api.BaseCtrl.setHeight, type: 'string'};
                                this.real_width = {get: D3Api.BaseCtrl.getRealWidth, type: 'number'};
                                this.real_height = {get: D3Api.BaseCtrl.getRealHeight, type: 'number'};
                                this.enabled = {get: D3Api.BaseCtrl.getEnabled, set: D3Api.BaseCtrl.setEnabled, type: 'boolean'};
                                this.visible = {get: D3Api.BaseCtrl.getVisible, set: D3Api.BaseCtrl.setVisible, type: 'boolean'};
                                this.hint = {get: D3Api.BaseCtrl.getHint, set: D3Api.BaseCtrl.setHint, type: 'string'};
                                this.focus = {set: D3Api.BaseCtrl.setFocus, type: 'boolean'};
                                this.warning = {set: D3Api.BaseCtrl.setWarning, get: D3Api.BaseCtrl.getWarning, type: 'boolean'};
                                this.error = {set: D3Api.BaseCtrl.setError, get: D3Api.BaseCtrl.getError, type: 'boolean'};
                                this.html = {get: D3Api.BaseCtrl.getHtml, set: D3Api.BaseCtrl.setHtml, type: 'string'};
                                this.input = {get: D3Api.BaseCtrl.getInput, type: 'dom'};
                            };
                        }
                        
                        // Проверяем наличие BaseCtrl и определяем его если нет
                        if (typeof D3Api.BaseCtrl === 'undefined') {
                            console.log('cmpScript: D3Api.BaseCtrl not found, waiting for it...');
                            // Пробуем еще раз через небольшую задержку
                            setTimeout(function() {
                                waitForD3Api(initialize);
                            }, 100);
                            return;
                        }
                        debugger
                        // Хранилище для загруженных скриптов
                        var loadedScripts = {};
                        var scriptPromises = {};
                        
                        /**
                         * Инициализация всех скриптов на странице
                         */
                        function initScripts() {
                            var scripts = document.querySelectorAll('[cmptype="Script"]');
                            console.log('cmpScript: Found ' + scripts.length + ' script components');
                            
                            scripts.forEach(function(script) {
                                var name = script.getAttribute('name');
                                var src = script.getAttribute('src');
                                var type = script.getAttribute('type') || 'text/javascript';
                                var async = script.hasAttribute('async');
                                var defer = script.hasAttribute('defer');
                                
                                // Сохраняем информацию о скрипте
                                script.D3Store = script.D3Store || {};
                                script.D3Store.loaded = false;
                                script.D3Store.error = null;
                                
                                if (src) {
                                    // Внешний скрипт - загружаем динамически если нужно
                                    if (!loadedScripts[src]) {
                                        script.D3Store.promise = loadExternalScript(script, src, async, defer);
                                    } else {
                                        script.D3Store.loaded = true;
                                        script.D3Store.promise = Promise.resolve();
                                    }
                                } else {
                                    // Встроенный скрипт - просто выполняем
                                    var content = script.textContent || script.value || '';
                                    if (content.trim()) {
                                        try {
                                            // Выполняем скрипт
                                            executeScript(content, name);
                                            script.D3Store.loaded = true;
                                            script.D3Store.promise = Promise.resolve();
                                            
                                            // После выполнения скрипта проверяем, добавил ли он методы в Form
                                            console.log('cmpScript: Script executed, Form methods:', Object.keys(window.Form));
                                        } catch (e) {
                                            script.D3Store.error = e;
                                            script.D3Store.loaded = false;
                                            console.error('cmpScript: Error executing inline script', name, e);
                                        }
                                    }
                                }
                            });
                        }
                        
                        /**
                         * Загрузка внешнего скрипта
                         */
                        function loadExternalScript(scriptElement, src, async, defer) {
                            return new Promise(function(resolve, reject) {
                                if (loadedScripts[src]) {
                                    resolve();
                                    return;
                                }
                                
                                var script = document.createElement('script');
                                script.src = src;
                                script.type = scriptElement.getAttribute('type') || 'text/javascript';
                                
                                if (async) script.async = true;
                                if (defer) script.defer = true;
                                
                                var crossorigin = scriptElement.getAttribute('crossorigin');
                                if (crossorigin) script.crossorigin = crossorigin;
                                
                                var integrity = scriptElement.getAttribute('integrity');
                                if (integrity) script.integrity = integrity;
                                
                                var nonce = scriptElement.getAttribute('nonce');
                                if (nonce) script.nonce = nonce;
                                
                                script.onload = function() {
                                    loadedScripts[src] = true;
                                    scriptElement.D3Store.loaded = true;
                                    console.log('cmpScript: Loaded external script', src);
                                    
                                    // Генерируем событие
                                    var event = new CustomEvent('scriptLoaded', {
                                        detail: { src: src, name: scriptElement.getAttribute('name') }
                                    });
                                    scriptElement.dispatchEvent(event);
                                    
                                    resolve();
                                };
                                
                                script.onerror = function(error) {
                                    scriptElement.D3Store.error = error;
                                    scriptElement.D3Store.loaded = false;
                                    console.error('cmpScript: Failed to load external script', src, error);
                                    
                                    var event = new CustomEvent('scriptError', {
                                        detail: { src: src, error: error }
                                    });
                                    scriptElement.dispatchEvent(event);
                                    
                                    reject(error);
                                };
                                
                                document.head.appendChild(script);
                            });
                        }
                        
                        /**
                         * Выполнение встроенного скрипта
                         */
                        function executeScript(content, name) {
                            try {
                                // Используем Function для создания функции в глобальной области видимости
                                // Передаем объект Form как параметр, чтобы скрипт мог его расширять
                                var scriptFunction = new Function('Form', 'window', 'document', content);
                                scriptFunction.call(window, window.Form, window, document);
                                console.log('cmpScript: Executed inline script', name);
                                return true;
                            } catch (e) {
                                console.error('cmpScript: Error in inline script', name, e);
                                throw e;
                            }
                        }
                        
                        // Расширение D3Api
                        if (typeof D3Api !== 'undefined') {
                            
                            // Создаем объект ScriptCtrl
                            D3Api.ScriptCtrl = {
                                /**
                                 * Загрузка скрипта
                                 */
                                load: function(name, src, async, defer) {
                                    var script = this.getScriptElement(name);
                                    if (!script) {
                                        // Создаем новый скрипт если не существует
                                        script = document.createElement('script');
                                        script.setAttribute('cmptype', 'Script');
                                        script.setAttribute('name', name);
                                        if (src) script.setAttribute('src', src);
                                        if (async) script.setAttribute('async', 'async');
                                        if (defer) script.setAttribute('defer', 'defer');
                                        document.body.appendChild(script);
                                    }
                                    
                                    if (src) {
                                        return loadExternalScript(script, src, async, defer);
                                    }
                                    return Promise.resolve();
                                },
                                
                                /**
                                 * Выполнение скрипта
                                 */
                                execute: function(name, content) {
                                    if (content) {
                                        return executeScript(content, name);
                                    } else {
                                        var script = this.getScriptElement(name);
                                        if (script) {
                                            content = script.textContent || script.value || '';
                                            return executeScript(content, name);
                                        }
                                    }
                                    return false;
                                },
                                
                                /**
                                 * Получение элемента скрипта по имени
                                 */
                                getScriptElement: function(name) {
                                    return document.querySelector('[cmptype="Script"][name="' + name + '"]');
                                },
                                
                                /**
                                 * Проверка загружен ли скрипт
                                 */
                                isLoaded: function(name) {
                                    var script = this.getScriptElement(name);
                                    return script ? script.D3Store && script.D3Store.loaded : false;
                                },
                                
                                /**
                                 * Получение статуса загрузки скрипта
                                 */
                                getStatus: function(name) {
                                    var script = this.getScriptElement(name);
                                    if (!script) return { exists: false };
                                    return {
                                        exists: true,
                                        loaded: script.D3Store ? script.D3Store.loaded : false,
                                        error: script.D3Store ? script.D3Store.error : null,
                                        src: script.getAttribute('src')
                                    };
                                },
                                
                                /**
                                 * Ожидание загрузки скрипта
                                 */
                                waitFor: function(name) {
                                    var script = this.getScriptElement(name);
                                    if (!script) return Promise.reject('Script not found: ' + name);
                                    if (script.D3Store && script.D3Store.promise) {
                                        return script.D3Store.promise;
                                    }
                                    return Promise.resolve();
                                },
                                
                                /**
                                 * Динамическое создание и выполнение скрипта
                                 */
                                create: function(name, content, options) {
                                    options = options || {};
                                    
                                    // Удаляем старый скрипт если есть
                                    var oldScript = this.getScriptElement(name);
                                    if (oldScript) {
                                        oldScript.remove();
                                    }
                                    
                                    // Создаем новый элемент
                                    var script = document.createElement('script');
                                    script.setAttribute('cmptype', 'Script');
                                    script.setAttribute('name', name);
                                    
                                    if (options.src) {
                                        script.setAttribute('src', options.src);
                                    }
                                    
                                    if (options.type) {
                                        script.setAttribute('type', options.type);
                                    }
                                    
                                    if (options.async) {
                                        script.setAttribute('async', 'async');
                                    }
                                    
                                    if (options.defer) {
                                        script.setAttribute('defer', 'defer');
                                    }
                                    
                                    script.textContent = content || '';
                                    
                                    document.body.appendChild(script);
                                    
                                    // Инициализируем
                                    if (options.src) {
                                        return loadExternalScript(script, options.src, options.async, options.defer);
                                    } else if (content) {
                                        return executeScript(content, name);
                                    }
                                    
                                    return Promise.resolve();
                                }
                            };
                            
                            // Регистрируем API для Script
                            D3Api.controlsApi = D3Api.controlsApi || {};
                            D3Api.controlsApi['Script'] = new D3Api.ControlBaseProperties(D3Api.ScriptCtrl);
                            
                            // Добавляем свойства
                            D3Api.controlsApi['Script']['loaded'] = {
                                get: function(dom) {
                                    return dom && dom.D3Store ? dom.D3Store.loaded : false;
                                }
                            };
                            
                            D3Api.controlsApi['Script']['content'] = {
                                get: function(dom) {
                                    return dom ? (dom.textContent || dom.value || '') : '';
                                },
                                set: function(dom, value) {
                                    if (dom) {
                                        dom.textContent = value;
                                        if (dom.D3Store && dom.D3Store.loaded) {
                                            // Если скрипт уже был загружен, выполняем новый
                                            executeScript(value, dom.getAttribute('name'));
                                        }
                                    }
                                }
                            };
                            
                            console.log('cmpScript: D3Api extended with script functionality');
                        }
                        
                        // Инициализация после загрузки DOM
                        if (document.readyState === 'loading') {
                            document.addEventListener('DOMContentLoaded', function() {
                                initScripts();
                            });
                        } else {
                            initScripts();
                        }
                    }
                    
                    waitForD3Api(initialize);
                })();
                """);

        return js.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}