package ru.miacomsoft.EasyWebServer.component;

import ru.miacomsoft.EasyWebServer.HttpExchange;

/**
 * JavaScript библиотека для компонента модального окна (DIV-based версия)
 * Предоставляет полную функциональность для создания и управления модальными окнами
 * Использует современную DIV верстку вместо таблиц
 */
public class cmpWindow_js {

    public static byte[] onPage(HttpExchange query) {
        query.mimeType = "application/javascript";

        StringBuilder js = new StringBuilder();
        js.append("""
                (function() {
                    if (window.cmpWindowInitialized) return;
                
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
                        if (window.cmpWindowInitialized) return;
                        window.cmpWindowInitialized = true;
                
                        console.log('cmpWindow: Initializing window component');
                
                        // Хранилище для всех созданных окон
                        if (!window.__d3Windows) window.__d3Windows = {};
                
                        // Текущее активное окно
                        var _activeWindow = null;
                
                        // Хранилище для колбэков при переходе на новую страницу
                        if (!window.__pageCallbacks) window.__pageCallbacks = {};
                
                        // ID текущей навигации (для страниц)
                        var _currentNavId = null;
                
                        function removeElement(el) {
                            if (el && el.parentNode) {
                                el.parentNode.removeChild(el);
                            }
                        }
                
                        function addClass(el, className) {
                            if (el && className) {
                                el.classList.add(className);
                            }
                        }
                
                        function removeClass(el, className) {
                            if (el && className) {
                                el.classList.remove(className);
                            }
                        }
                
                        function getDocumentSize() {
                            return {
                                width: Math.max(document.documentElement.clientWidth, window.innerWidth || 0),
                                height: Math.max(document.documentElement.clientHeight, window.innerHeight || 0)
                            };
                        }
                
                        function getElementSize(el) {
                            return {
                                width: el.offsetWidth,
                                height: el.offsetHeight
                            };
                        }
                
                        function setPosition(el, left, top) {
                            if (el) {
                                el.style.left = left + 'px';
                                el.style.top = top + 'px';
                                el.style.transform = 'none';
                            }
                        }
                
                        /**
                         * Функция для получения активного окна
                         */
                        window.getPage = function() {
                            return _activeWindow ? _activeWindow.D3Api : null;
                        };
                
                        /**
                         * Глобальная функция для закрытия текущего окна
                         * Эта функция вызывается из дочернего окна
                         */
                        window.close = function(result) {
                            console.log('window.close called with result:', result);
                
                            // Проверяем, есть ли активное окно (модальное)
                            if (_activeWindow) {
                                _activeWindow.close(result);
                                return;
                            }
                
                            // Проверяем, есть ли сохраненный navId для возврата
                            var navId = sessionStorage.getItem('currentNavId');
                            if (navId) {
                                var returnUrl = sessionStorage.getItem('returnUrl_' + navId);
                                if (returnUrl) {
                                    // Сохраняем результат для родительской страницы
                                    sessionStorage.setItem('pageResult_' + navId, JSON.stringify(result));
                                    // Возвращаемся на родительскую страницу
                                    window.location.href = returnUrl;
                                    return;
                                }
                            }
                
                            // Если ничего не нашли, просто идем назад
                            window.history.back();
                            if (result) {
                                sessionStorage.setItem('pageResult', JSON.stringify(result));
                            }
                        };
                
                        /**
                         * Функция для перехода на новую страницу
                         */
                        function navigateToPage(url, data) {
                            var navigationId = 'nav_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                
                            // Сохраняем текущий navId
                            sessionStorage.setItem('currentNavId', navigationId);
                
                            // Сохраняем URL для возврата
                            sessionStorage.setItem('returnUrl_' + navigationId, window.location.href);
                
                            // Сохраняем все колбэки
                            var callbacks = {
                                onclose: data.onclose ? data.onclose.toString() : null,
                                oncreate: data.oncreate ? data.oncreate.toString() : null,
                                onshow: data.onshow ? data.onshow.toString() : null,
                                vars: data.vars || {},
                                navigationId: navigationId,
                                returnUrl: window.location.href
                            };
                
                            sessionStorage.setItem('pageCallbacks_' + navigationId, JSON.stringify(callbacks));
                
                            var separator = url.indexOf('?') === -1 ? '?' : '&';
                            var targetUrl = url + separator + '_navId=' + encodeURIComponent(navigationId);
                
                            console.log('Navigating to:', targetUrl);
                            window.location.href = targetUrl;
                        }
                
                        /**
                         * Функция для восстановления колбэков при загрузке страницы
                         */
                        function restorePageCallbacks() {
                            var urlParams = new URLSearchParams(window.location.search);
                            var navId = urlParams.get('_navId');
                
                            if (navId) {
                                console.log('Restoring page callbacks for navId:', navId);
                                _currentNavId = navId;
                
                                var callbacksJson = sessionStorage.getItem('pageCallbacks_' + navId);
                                if (callbacksJson) {
                                    try {
                                        var callbacks = JSON.parse(callbacksJson);
                
                                        var restoredData = {
                                            vars: callbacks.vars || {},
                                            returnUrl: callbacks.returnUrl
                                        };
                
                                        if (callbacks.onclose) {
                                            restoredData.onclose = new Function('return ' + callbacks.onclose)();
                                        }
                
                                        if (callbacks.oncreate) {
                                            restoredData.oncreate = new Function('return ' + callbacks.oncreate)();
                                        }
                
                                        if (callbacks.onshow) {
                                            restoredData.onshow = new Function('return ' + callbacks.onshow)();
                                        }
                
                                        window.__pageCallbacks[navId] = restoredData;
                
                                        // Создаем объект страницы с правильным методом close
                                        var pageObject = {
                                            D3Api: Object.create(window.D3Api)
                                        };
                
                                        // Добавляем методы для работы с этой страницей
                                        pageObject.D3Api.close = function(result) {
                                            console.log('Page close called with result:', result);
                                            if (restoredData.returnUrl) {
                                                sessionStorage.setItem('pageResult_' + navId, JSON.stringify(result));
                                                window.location.href = restoredData.returnUrl;
                                            }
                                        };
                
                                        pageObject.D3Api.getVar = function(name, defValue) {
                                            return restoredData.vars ? restoredData.vars[name] : defValue;
                                        };
                
                                        pageObject.D3Api.setVar = function(name, value) {
                                            if (!restoredData.vars) restoredData.vars = {};
                                            restoredData.vars[name] = value;
                                        };
                
                                        _activeWindow = pageObject;
                
                                        // Вызываем oncreate если есть
                                        if (restoredData.oncreate) {
                                            console.log('Calling oncreate');
                                            restoredData.oncreate.call(pageObject.D3Api, pageObject.D3Api);
                                        }
                
                                        // Вызываем onshow если есть
                                        if (restoredData.onshow) {
                                            console.log('Calling onshow');
                                            requestAnimationFrame(function() {
                                                restoredData.onshow.call(pageObject.D3Api, pageObject.D3Api);
                                            });
                                        }
                
                                    } catch (e) {
                                        console.error('Failed to restore page callbacks:', e);
                                    }
                                }
                            } else {
                                // Проверяем, нет ли результата от дочерней страницы
                                checkForPageResult();
                            }
                        }
                
                        /**
                         * Проверяет наличие результата от дочерней страницы
                         */
                        function checkForPageResult() {
                            // Ищем все ключи pageResult_ в sessionStorage
                            var resultKeys = [];
                            for (var i = 0; i < sessionStorage.length; i++) {
                                var key = sessionStorage.key(i);
                                if (key && key.startsWith('pageResult_')) {
                                    resultKeys.push(key);
                                }
                            }
                
                            if (resultKeys.length > 0) {
                                // Берем первый найденный результат
                                var resultKey = resultKeys[0];
                                try {
                                    var resultJson = sessionStorage.getItem(resultKey);
                                    var result = JSON.parse(resultJson);
                                    sessionStorage.removeItem(resultKey);
                
                                    var navId = resultKey.replace('pageResult_', '');
                                    console.log('Found page result for navId:', navId, result);
                
                                    // Очищаем связанные данные
                                    sessionStorage.removeItem('currentNavId');
                                    sessionStorage.removeItem('returnUrl_' + navId);
                
                                    var callbacks = window.__pageCallbacks[navId];
                                    if (callbacks && callbacks.onclose) {
                                        console.log('Calling onclose with result:', result);
                                        callbacks.onclose(result);
                                    }
                
                                    // Очищаем колбэки
                                    sessionStorage.removeItem('pageCallbacks_' + navId);
                                    delete window.__pageCallbacks[navId];
                
                                } catch (e) {
                                    console.error('Failed to process page result:', e);
                                }
                            }
                        }
                
                        /**
                         * Генерация HTML для окна
                         */
                        function getWindowXml(options) {
                            options = options || {};
                            let modal = options.modal !== false;
                            let width = options.width || 500;
                            let height = options.height || 400;
                            let caption = options.caption || '';
                            let theme = options.theme || 'modern';
                            let url = options.url || '';
                
                            let overlay = '';
                            if (modal) {
                                overlay = '<div class="win_overlow"></div>';
                            }
                
                            // Создаем iframe без src, он будет заполнен позже через fetch
                            let windowHtml = overlay + `
                                <div class="window ${theme}" style="width: ${width}px; height: ${height}px; left: 50%; top: 50%; transform: translate(-50%, -50%); display: none; position: fixed; z-index: 9999; background: white; border: 1px solid #ccc; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.2);">
                                    <div class="window-header" data-role="title-row" style="color: rgb(50,50,50);display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #f0f0f0; border-bottom: 1px solid #ccc; cursor: move; border-radius: 8px 8px 0 0;">
                                        <div class="window-title" data-role="title" style="font-weight: bold; font-size: 14px;">${caption}</div>
                                        <div class="window-controls" style="display: flex; gap: 8px;">
                                            <div class="window-control reload" data-role="reload" title="Обновить" style="color: rgb(50,50,50);cursor: pointer; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;">↻</div>
                                            <div class="window-control maximize" data-role="maximize" title="Развернуть" style="color: rgb(50,50,50);cursor: pointer; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;">🗖</div>
                                            <div class="window-control close" data-role="close" title="Закрыть" style="color: rgb(50,50,50);cursor: pointer; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;">✕</div>
                                        </div>
                                    </div>
                                    <div class="window-content" style="flex: 1; overflow: auto; height: calc(100% - 40px); position: relative;">
                                        <iframe class="window-iframe" data-role="iframe" frameborder="0" style="width: 100%; height: 100%; border: none; display: block;"></iframe>
                                    </div>
                
                                    <div class="window-resize-handle resize-n" data-role="resize-n" style="position: absolute; top: 0; left: 5px; right: 5px; height: 5px; cursor: n-resize;"></div>
                                    <div class="window-resize-handle resize-s" data-role="resize-s" style="position: absolute; bottom: 0; left: 5px; right: 5px; height: 5px; cursor: s-resize;"></div>
                                    <div class="window-resize-handle resize-e" data-role="resize-e" style="position: absolute; top: 5px; right: 0; bottom: 5px; width: 5px; cursor: e-resize;"></div>
                                    <div class="window-resize-handle resize-w" data-role="resize-w" style="position: absolute; top: 5px; left: 0; bottom: 5px; width: 5px; cursor: w-resize;"></div>
                                    <div class="window-resize-handle resize-ne" data-role="resize-ne" style="position: absolute; top: 0; right: 0; width: 10px; height: 10px; cursor: ne-resize;"></div>
                                    <div class="window-resize-handle resize-nw" data-role="resize-nw" style="position: absolute; top: 0; left: 0; width: 10px; height: 10px; cursor: nw-resize;"></div>
                                    <div class="window-resize-handle resize-se" data-role="resize-se" style="position: absolute; bottom: 0; right: 0; width: 15px; height: 15px; cursor: se-resize;"></div>
                                    <div class="window-resize-handle resize-sw" data-role="resize-sw" style="position: absolute; bottom: 0; left: 0; width: 10px; height: 10px; cursor: sw-resize;"></div>
                                </div>
                            `;
                
                            return windowHtml;
                        }
                
                        class DWindow {
                            constructor(options) {
                                this.options = options || {};
                                this.modal = this.options.modal !== false;
                                this.width = this.options.width || 500;
                                this.height = this.options.height || 400;
                                this.caption = this.options.caption || 'Окно';
                                this.theme = this.options.theme || 'modern';
                                this.url = this.options.url || '';
                
                                this.element = null;
                                this.overlay = null;
                                this.iframe = null;
                                this.iframeOverlay = null;
                                this.title = null;
                                this.titleRow = null;
                
                                this.dragging = false;
                                this.resizing = false;
                                this.resizeType = null;
                                this.dragOffset = { x: 0, y: 0 };
                
                                this.minWidth = 250;
                                this.minHeight = 150;
                                this.maximized = false;
                                this.closed = false;
                
                                this.listeners = {};
                                this.originalPosition = null;
                                this.originalSize = null;
                                this.messageHandlers = {};
                
                                // Локальные переменные окна
                                this._vars = {};
                
                                // Создаем объект D3Api для этого окна на основе глобального D3Api
                                this.D3Api = Object.create(window.D3Api);
                
                                // Объект Form для этого окна
                                this.Form = {};
                
                                var self = this;
                
                                // Переопределяем методы для работы с локальными переменными
                                this.D3Api.setVar = function(name, value) {
                                    self._vars[name] = value;
                                };
                
                                this.D3Api.getVar = function(name, defValue) {
                                    return self._vars[name] !== undefined ? self._vars[name] : defValue;
                                };
                
                                // Переопределяем методы для работы с контролами окна
                                this.D3Api.setValue = function(name, value) {
                                    var ctrl = self.element.querySelector('[name="' + name + '"]');
                                    if (ctrl) {
                                        if (ctrl.tagName.toLowerCase() === 'input') {
                                            if (ctrl.type === 'checkbox') {
                                                ctrl.checked = (value === true || value === 'on' || value === 'true');
                                            } else {
                                                ctrl.value = value;
                                            }
                                        } else if (ctrl.tagName.toLowerCase() === 'select' || ctrl.tagName.toLowerCase() === 'textarea') {
                                            ctrl.value = value;
                                        } else {
                                            ctrl.textContent = value;
                                        }
                                    }
                                };
                
                                this.D3Api.getValue = function(name, defValue) {
                                    var ctrl = self.element.querySelector('[name="' + name + '"]');
                                    if (ctrl) {
                                        if (ctrl.tagName.toLowerCase() === 'input') {
                                            if (ctrl.type === 'checkbox') {
                                                return ctrl.checked;
                                            } else {
                                                return ctrl.value || defValue;
                                            }
                                        } else if (ctrl.tagName.toLowerCase() === 'select' || ctrl.tagName.toLowerCase() === 'textarea') {
                                            return ctrl.value || defValue;
                                        } else {
                                            return ctrl.textContent || defValue;
                                        }
                                    }
                                    return defValue;
                                };
                
                                // Переопределяем методы для работы с подписями
                                this.D3Api.setCaption = function(text) {
                                    self.setCaption(text);
                                };
                
                                this.D3Api.getCaption = function() {
                                    return self.title ? self.title.textContent : '';
                                };
                
                                // Добавляем метод закрытия окна
                                this.D3Api.close = function(result) {
                                    self.close(result);
                                };
                
                                // Добавляем методы для работы со скриптами в окне
                                this.D3Api.loadScript = function(name, src, async, defer) {
                                    if (self.iframe && self.iframe.contentWindow && self.iframe.contentWindow.D3Api) {
                                        return self.iframe.contentWindow.D3Api.loadScript(name, src, async, defer);
                                    }
                                    return Promise.reject('Not available');
                                };
                
                                this.D3Api.executeScript = function(name, content) {
                                    if (self.iframe && self.iframe.contentWindow && self.iframe.contentWindow.D3Api) {
                                        return self.iframe.contentWindow.D3Api.executeScript(name, content);
                                    }
                                    return false;
                                };
                
                                this.D3Api.getScriptStatus = function(name) {
                                    if (self.iframe && self.iframe.contentWindow && self.iframe.contentWindow.D3Api) {
                                        return self.iframe.contentWindow.D3Api.getScriptStatus(name);
                                    }
                                    return { exists: false, error: 'Iframe not ready' };
                                };
                
                                this.D3Api.waitForScript = function(name) {
                                    if (self.iframe && self.iframe.contentWindow && self.iframe.contentWindow.D3Api) {
                                        return self.iframe.contentWindow.D3Api.waitForScript(name);
                                    }
                                    return Promise.reject('Iframe not ready');
                                };
                
                                // Сохраняем ссылку на окно
                                this.windowId = 'win_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                                window.__d3Windows[this.windowId] = this;
                
                                this.init();
                            }
                
                            init() {
                                var temp = document.createElement('div');
                                temp.innerHTML = getWindowXml(this.options);
                
                                if (this.modal) {
                                    this.overlay = temp.querySelector('.win_overlow');
                                }
                
                                this.element = temp.querySelector('.window');
                
                                if (!this.element) {
                                    console.error('Failed to create window element');
                                    return;
                                }
                
                                this.iframe = this.element.querySelector('[data-role="iframe"]');
                                this.title = this.element.querySelector('[data-role="title"]');
                                this.titleRow = this.element.querySelector('[data-role="title-row"]');
                
                                if (this.overlay) {
                                    document.body.appendChild(this.overlay);
                                    this.overlay.style.display = 'none';
                                }
                                document.body.appendChild(this.element);
                
                                this.element.style.display = 'none';
                
                                this.initEventHandlers();
                                this.originalSize = { width: this.width, height: this.height };
                                this.setupIframeMessaging();
                            }
                
                            createIframeOverlay() {
                                if (!this.iframe || this.iframeOverlay) return;
                
                                let cursor = 'default';
                                if (this.dragging) {
                                    cursor = 'move';
                                } else if (this.resizing) {
                                    switch (this.resizeType) {
                                        case 'resize-n': cursor = 'n-resize'; break;
                                        case 'resize-s': cursor = 's-resize'; break;
                                        case 'resize-e': cursor = 'e-resize'; break;
                                        case 'resize-w': cursor = 'w-resize'; break;
                                        case 'resize-ne': cursor = 'ne-resize'; break;
                                        case 'resize-nw': cursor = 'nw-resize'; break;
                                        case 'resize-se': cursor = 'se-resize'; break;
                                        case 'resize-sw': cursor = 'sw-resize'; break;
                                    }
                                }
                
                                this.iframeOverlay = document.createElement('div');
                                this.iframeOverlay.className = 'window-iframe-overlay';
                                this.iframeOverlay.style.cssText = `
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    width: 100%;
                                    height: 100%;
                                    background: transparent;
                                    z-index: 10000;
                                    cursor: ${cursor};
                                `;
                
                                let contentDiv = this.element.querySelector('.window-content');
                                if (contentDiv) {
                                    contentDiv.appendChild(this.iframeOverlay);
                                }
                            }
                
                            removeIframeOverlay() {
                                if (this.iframeOverlay && this.iframeOverlay.parentNode) {
                                    this.iframeOverlay.parentNode.removeChild(this.iframeOverlay);
                                    this.iframeOverlay = null;
                                }
                            }
                
                            setupIframeMessaging() {
                                if (!this.iframe) return;
                
                                window.addEventListener('message', (event) => {
                                    if (event.source === this.iframe.contentWindow) {
                                        if (event.data && event.data.command) {
                                            switch (event.data.command) {
                                                case 'close':
                                                    this.close(event.data.result);
                                                    break;
                                                case 'resize':
                                                    if (event.data.width && event.data.height) {
                                                        this.setSize(event.data.width, event.data.height);
                                                    }
                                                    break;
                                                case 'setCaption':
                                                    this.setCaption(event.data.caption);
                                                    break;
                                                case 'scriptLoaded':
                                                    console.log('Script loaded in iframe:', event.data.name);
                                                    this.dispatchEvent('scriptLoaded', event.data);
                                                    break;
                                                case 'scriptError':
                                                    console.error('Script error in iframe:', event.data.name, event.data.error);
                                                    this.dispatchEvent('scriptError', event.data);
                                                    break;
                                                default:
                                                    if (this.messageHandlers[event.data.command]) {
                                                        this.messageHandlers[event.data.command](event.data);
                                                    }
                                            }
                                        }
                                        this.dispatchEvent('message', event.data);
                                    }
                                });
                            }
                
                            sendToIframe(message) {
                                if (this.iframe && this.iframe.contentWindow) {
                                    this.iframe.contentWindow.postMessage(message, '*');
                                }
                            }
                
                            onMessage(command, handler) {
                                this.messageHandlers[command] = handler;
                            }
                
                            initEventHandlers() {
                                if (!this.element) return;
                
                                this.handleMouseMove = (e) => {
                                    if (this.dragging) {
                                        this.onDrag(e);
                                    } else if (this.resizing) {
                                        this.onResize(e);
                                    }
                                };
                
                                this.handleMouseUp = (e) => {
                                    if (this.dragging) {
                                        this.stopDrag(e);
                                    } else if (this.resizing) {
                                        this.stopResize(e);
                                    }
                                    this.removeIframeOverlay();
                                };
                
                                if (this.titleRow) {
                                    this.titleRow.addEventListener('mousedown', (e) => {
                                        if (e.button !== 0) return;
                                        this.startDrag(e);
                                    });
                                    this.titleRow.addEventListener('dblclick', (e) => this.toggleMaximize(e));
                                }
                
                                let resizeHandles = this.element.querySelectorAll('[data-role^="resize-"]');
                                resizeHandles.forEach(handle => {
                                    handle.addEventListener('mousedown', (e) => {
                                        if (e.button !== 0) return;
                                        let role = handle.getAttribute('data-role');
                                        this.startResize(e, role);
                                    });
                                });
                
                                let closeBtn = this.element.querySelector('[data-role="close"]');
                                if (closeBtn) {
                                    closeBtn.addEventListener('click', (e) => this.close());
                                }
                
                                let maximizeBtn = this.element.querySelector('[data-role="maximize"]');
                                if (maximizeBtn) {
                                    maximizeBtn.addEventListener('click', (e) => this.toggleMaximize(e));
                                }
                
                                let reloadBtn = this.element.querySelector('[data-role="reload"]');
                                if (reloadBtn) {
                                    reloadBtn.addEventListener('click', (e) => this.reload());
                                }
                
                                this.element.addEventListener('selectstart', (e) => e.preventDefault());
                
                                document.addEventListener('mousemove', this.handleMouseMove);
                                document.addEventListener('mouseup', this.handleMouseUp);
                            }
                
                            startDrag(e) {
                                if (this.maximized) return;
                
                                e.preventDefault();
                
                                let rect = this.element.getBoundingClientRect();
                                this.dragOffset = {
                                    x: e.clientX - rect.left,
                                    y: e.clientY - rect.top
                                };
                
                                this.dragging = true;
                                this.resizing = false;
                                document.body.classList.add('noselect');
                                if (this.titleRow) {
                                    this.titleRow.classList.add('dragging');
                                }
                
                                this.originalPosition = {
                                    left: rect.left,
                                    top: rect.top
                                };
                
                                this.createIframeOverlay();
                            }
                
                            onDrag(e) {
                                if (!this.dragging) return;
                
                                e.preventDefault();
                
                                let left = e.clientX - this.dragOffset.x;
                                let top = e.clientY - this.dragOffset.y;
                
                                let maxLeft = window.innerWidth - this.element.offsetWidth;
                                let maxTop = window.innerHeight - this.element.offsetHeight;
                
                                left = Math.max(0, Math.min(left, maxLeft));
                                top = Math.max(0, Math.min(top, maxTop));
                
                                this.element.style.left = left + 'px';
                                this.element.style.top = top + 'px';
                                this.element.style.transform = 'none';
                            }
                
                            stopDrag(e) {
                                if (!this.dragging) return;
                
                                this.dragging = false;
                                document.body.classList.remove('noselect');
                                if (this.titleRow) {
                                    this.titleRow.classList.remove('dragging');
                                }
                
                                this.dispatchEvent('move', {
                                    left: parseInt(this.element.style.left),
                                    top: parseInt(this.element.style.top)
                                });
                            }
                
                            startResize(e, type) {
                                e.preventDefault();
                
                                this.resizing = true;
                                this.dragging = false;
                                this.resizeType = type;
                
                                let rect = this.element.getBoundingClientRect();
                                this.startResizeData = {
                                    x: e.clientX,
                                    y: e.clientY,
                                    width: rect.width,
                                    height: rect.height,
                                    left: rect.left,
                                    top: rect.top,
                                    right: rect.right,
                                    bottom: rect.bottom
                                };
                
                                document.body.classList.add('noselect');
                                this.createIframeOverlay();
                            }
                
                            onResize(e) {
                                if (!this.resizing) return;
                                e.preventDefault();
                
                                let dx = e.clientX - this.startResizeData.x;
                                let dy = e.clientY - this.startResizeData.y;
                
                                let newWidth = this.startResizeData.width;
                                let newHeight = this.startResizeData.height;
                                let newLeft = this.startResizeData.left;
                                let newTop = this.startResizeData.top;
                
                                switch (this.resizeType) {
                                    case 'resize-se':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width + dx);
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height + dy);
                                        break;
                                    case 'resize-e':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width + dx);
                                        break;
                                    case 'resize-s':
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height + dy);
                                        break;
                                    case 'resize-w':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width - dx);
                                        newLeft = this.startResizeData.right - newWidth;
                                        break;
                                    case 'resize-n':
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height - dy);
                                        newTop = this.startResizeData.bottom - newHeight;
                                        break;
                                    case 'resize-ne':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width + dx);
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height - dy);
                                        newTop = this.startResizeData.bottom - newHeight;
                                        break;
                                    case 'resize-nw':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width - dx);
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height - dy);
                                        newLeft = this.startResizeData.right - newWidth;
                                        newTop = this.startResizeData.bottom - newHeight;
                                        break;
                                    case 'resize-sw':
                                        newWidth = Math.max(this.minWidth, this.startResizeData.width - dx);
                                        newHeight = Math.max(this.minHeight, this.startResizeData.height + dy);
                                        newLeft = this.startResizeData.right - newWidth;
                                        break;
                                }
                
                                this.element.style.width = newWidth + 'px';
                                this.element.style.height = newHeight + 'px';
                                this.element.style.left = newLeft + 'px';
                                this.element.style.top = newTop + 'px';
                                this.element.style.transform = 'none';
                
                                this.sendToIframe({
                                    command: 'resized',
                                    width: newWidth,
                                    height: newHeight
                                });
                            }
                
                            stopResize(e) {
                                if (!this.resizing) return;
                
                                this.resizing = false;
                                document.body.classList.remove('noselect');
                
                                this.dispatchEvent('resize', {
                                    width: this.element.offsetWidth,
                                    height: this.element.offsetHeight
                                });
                            }
                
                            toggleMaximize(e) {
                                if (this.maximized) {
                                    this.element.style.width = this.originalSize.width + 'px';
                                    this.element.style.height = this.originalSize.height + 'px';
                                    if (this.originalPosition) {
                                        this.element.style.left = this.originalPosition.left + 'px';
                                        this.element.style.top = this.originalPosition.top + 'px';
                                    } else {
                                        this.element.style.left = '50%';
                                        this.element.style.top = '50%';
                                        this.element.style.transform = 'translate(-50%, -50%)';
                                    }
                                    this.element.classList.remove('maximized');
                                    if (this.overlay && this.modal) {
                                        this.overlay.style.display = 'block';
                                    }
                                } else {
                                    let rect = this.element.getBoundingClientRect();
                                    this.originalPosition = { left: rect.left, top: rect.top };
                                    this.originalSize = { width: rect.width, height: rect.height };
                
                                    this.element.style.left = '0';
                                    this.element.style.top = '0';
                                    this.element.style.width = '100%';
                                    this.element.style.height = '100%';
                                    this.element.style.transform = 'none';
                                    this.element.classList.add('maximized');
                                }
                
                                this.maximized = !this.maximized;
                                this.dispatchEvent('maximize', { maximized: this.maximized });
                
                                this.sendToIframe({
                                    command: 'maximize',
                                    maximized: this.maximized
                                });
                            }
                
                            reload() {
                                if (this.iframe && this.url) {
                                    this.loadContent(this.url);
                                }
                            }
                
                            setCaption(text) {
                                if (this.title) {
                                    this.title.textContent = text;
                                }
                                this.dispatchEvent('captionChange', { caption: text });
                                this.sendToIframe({ command: 'captionChanged', caption: text });
                            }
                
                            setUrl(url) {
                                this.url = url;
                                if (this.iframe) {
                                    this.loadContent(url);
                                }
                            }
                
                            setSize(width, height) {
                                this.element.style.width = width + 'px';
                                this.element.style.height = height + 'px';
                                this.width = width;
                                this.height = height;
                            }
                
                            /**
                             * Загрузка содержимого через fetch и вставка в iframe
                             */
                            loadContent(url) {
                                const self = this;
                                console.log('Loading content via fetch:', url);
                
                                // Показываем индикатор загрузки
                                if (this.iframe && this.iframe.contentDocument) {
                                    const loadingDiv = this.iframe.contentDocument.createElement('div');
                                    loadingDiv.style.cssText = 'position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); text-align:center;';
                                    loadingDiv.innerHTML = '<div style="border:4px solid #f3f3f3; border-top:4px solid #3498db; border-radius:50%; width:40px; height:40px; animation:spin 1s linear infinite; margin:0 auto;"></div><p style="margin-top:10px;">Загрузка...</p>';
                                    this.iframe.contentDocument.body.innerHTML = '';
                                    this.iframe.contentDocument.body.appendChild(loadingDiv);
                                }
                
                                fetch(url)
                                    .then(response => {
                                        if (!response.ok) {
                                            throw new Error(`HTTP error ${response.status}`);
                                        }
                                        return response.text();
                                    })
                                    .then(html => {
                                        console.log('Content loaded, length:', html.length);
                                        self.processContent(html);
                                    })
                                    .catch(error => {
                                        console.error('Failed to load content:', error);
                                        if (self.iframe && self.iframe.contentDocument) {
                                            self.iframe.contentDocument.body.innerHTML = 
                                                '<div style="color: red; padding: 20px; text-align: center;">' +
                                                '<h3>Ошибка загрузки</h3>' +
                                                '<p>Не удалось загрузить: ' + url + '</p>' +
                                                '<p>' + error.message + '</p>' +
                                                '</div>';
                                        }
                                        self.show();
                                    });
                            }
                
                            /**
                             * Обработка загруженного HTML контента
                             */
                            processContent(html) {
                                const self = this;
                                const iframe = this.iframe;
                                const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                
                                // Очищаем iframe
                                iframeDoc.open();
                                iframeDoc.write('<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body></body></html>');
                                iframeDoc.close();
                
                                // Парсим HTML
                                const parser = new DOMParser();
                                const doc = parser.parseFromString(html, 'text/html');
                
                                // ============== КОПИРОВАНИЕ CSS СТИЛЕЙ ==============
                                // Копируем все стили из head загруженного документа
                                const head = doc.querySelector('head');
                                if (head) {
                                    // Копируем все элементы head (стили, мета-теги, ссылки)
                                    Array.from(head.children).forEach(el => {
                                        try {
                                            // Для link и style элементов
                                            if (el.tagName === 'LINK' || el.tagName === 'STYLE') {
                                                const clonedEl = el.cloneNode(true);
                                                iframeDoc.head.appendChild(clonedEl);
                                                console.log('Copied style element:', el.tagName, el.getAttribute('href') || 'inline');
                                            } else if (el.tagName !== 'TITLE') {
                                                // Для других элементов head
                                                iframeDoc.head.appendChild(el.cloneNode(true));
                                            }
                                        } catch (e) {
                                            console.warn('Failed to copy head element:', e);
                                        }
                                    });
                                }
                
                                // Извлекаем заголовок
                                const title = doc.querySelector('title');
                                if (title) {
                                    iframeDoc.title = title.textContent;
                                }
                
                                // Сохраняем body для последующей вставки
                                const bodyContent = doc.querySelector('body');
                                if (bodyContent) {
                                    // Сохраняем HTML строку для последующей вставки
                                    this.bodyHTML = bodyContent.innerHTML;
                                }
                
                                // ============== ИНИЦИАЛИЗАЦИЯ ОБЪЕКТА FORM ==============
                                // Создаем объект Form в контексте iframe
                                if (typeof iframe.contentWindow.Form === 'undefined') {
                                    iframe.contentWindow.Form = this.Form;
                                    console.log('Form object linked to iframe');
                                }
                
                                // Добавляем базовые методы для Form, если их нет
                                if (!this.Form.getVar) {
                                    this.Form.getVar = function(name, defValue) {
                                        return defValue;
                                    };
                                    this.Form.setVar = function(name, value) {};
                                    this.Form.getValue = function(name, defValue) {
                                        return defValue;
                                    };
                                    this.Form.setValue = function(name, value) {};
                                    this.Form.getCaption = function(name) {
                                        return '';
                                    };
                                    this.Form.setCaption = function(name, value) {};
                                    this.Form.close = function(result) {
                                        if (self.D3Api && self.D3Api.close) {
                                            self.D3Api.close(result);
                                        }
                                    };
                                }
                
                                // ============== КОМПИЛЯЦИЯ ФУНКЦИЙ ИЗ СКРИПТОВ ==============
                                // Извлекаем все скрипты с атрибутом cmptype="Script"
                                const scripts = doc.querySelectorAll('[cmptype="Script"]');
                                console.log('Found ' + scripts.length + ' script components to compile');
                
                                scripts.forEach(script => {
                                    if (script.textContent && script.textContent.trim()) {
                                        try {
                                            console.log('Compiling script content');
                                            
                                            // Создаем функцию с параметром Form и выполняем её
                                            // Это позволит скрипту расширять объект Form
                                            const scriptFunction = new Function('Form', script.textContent);
                                            scriptFunction.call(iframe.contentWindow, this.Form);
                
                                            console.log('Script compiled successfully');
                                        } catch (e) {
                                            console.error('Error compiling script:', e);
                                        }
                                    }
                                });
                
                                // Логируем все методы, которые теперь есть в Form
                                console.log('Form methods after compilation:', Object.keys(this.Form));
                
                                // ============== ВСТАВКА ТЕЛА ДОКУМЕНТА ==============
                                // Вставляем body после компиляции скриптов
                                if (this.bodyHTML) {
                                    iframeDoc.body.innerHTML = this.bodyHTML;
                                    delete this.bodyHTML;
                                }
                
                                // ============== ЗАГРУЗКА БИБЛИОТЕК ==============
                                this.loadLibraries(iframe, doc, this.options.vars || {});
                            }
                
                            /**
                             * Загрузка библиотек
                             */
                            loadLibraries(iframe, doc, vars) {
                                const self = this;
                                const iframeWin = iframe.contentWindow;
                                const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                
                                // Функция для инъекции базового D3Api в iframe
                                function injectBaseD3Api() {
                                    return new Promise((resolve, reject) => {
                                        try {
                                            const baseScript = iframeDoc.createElement('script');
                                            baseScript.type = 'text/javascript';
                                            baseScript.textContent = `
                                                // Копируем базовые определения из родительского окна
                                                if (typeof window.D3Api === 'undefined') {
                                                    window.D3Api = {};
                                                }
                                                
                                                // Копируем ControlBaseProperties если он существует
                                                if (window.parent.D3Api && window.parent.D3Api.ControlBaseProperties) {
                                                    window.D3Api.ControlBaseProperties = window.parent.D3Api.ControlBaseProperties;
                                                }
                                                
                                                // Копируем BaseCtrl если он существует
                                                if (window.parent.D3Api && window.parent.D3Api.BaseCtrl) {
                                                    window.D3Api.BaseCtrl = window.parent.D3Api.BaseCtrl;
                                                }
                                                
                                                // Копируем базовые методы
                                                var methodsToCopy = [
                                                    'stopEvent', 'getEvent', 'addEvent', 'removeEvent',
                                                    'getControl', 'setValue', 'getValue', 'setVar', 'getVar',
                                                    'setCaption', 'getCaption', 'setDisabled', 'getBoolean',
                                                    'hasProperty', 'getProperty', 'setProperty',
                                                    'getChildTag', 'hideDom', 'showDom', 'createDom',
                                                    'stringTrim', 'parseDate', 'hours2time', 'debug_msg'
                                                ];
                                                
                                                methodsToCopy.forEach(function(method) {
                                                    if (window.parent.D3Api && window.parent.D3Api[method]) {
                                                        window.D3Api[method] = window.parent.D3Api[method];
                                                    }
                                                });
                                                
                                                console.log('Base D3Api injected into iframe');
                                            `;
                                            
                                            iframeDoc.head.appendChild(baseScript);
                                            resolve();
                                        } catch (e) {
                                            console.error('Failed to inject base D3Api:', e);
                                            reject(e);
                                        }
                                    });
                                }
                
                                // Функция для загрузки скрипта
                                function loadScriptSync(src) {
                                    return new Promise((resolve, reject) => {
                                        const script = iframeDoc.createElement('script');
                                        script.src = src;
                                        script.type = 'text/javascript';
                                        script.setAttribute('cmp', 'jslib');
                
                                        script.onload = () => {
                                            console.log(`Script loaded: ${src}`);
                                            resolve();
                                        };
                
                                        script.onerror = () => {
                                            console.error(`Failed to load script: ${src}`);
                                            reject(new Error(`Failed to load script: ${src}`));
                                        };
                
                                        iframeDoc.head.appendChild(script);
                                    });
                                }
                
                                // Функция для загрузки всех библиотек
                                async function loadLibrariesAsync() {
                                    try {
                                        await injectBaseD3Api();
                                        
                                        const componentPath = window.component || '';
                
                                        const scripts = [
                                            `/{component}/cmpBase_js`,
                                            `/{component}/main_js`,
                                            `/{component}/md5`,
                                            `/{component}/cmpScript_js`,
                                            `/{component}/cmpEdit_js`,
                                            `/{component}/cmpButton_js`
                                        ];
                
                                        for (const scriptSrc of scripts) {
                                            await loadScriptSync(scriptSrc);
                                        }
                
                                        console.log('All libraries loaded successfully');
                                        return true;
                                    } catch (error) {
                                        console.error('Error loading libraries:', error);
                                        return false;
                                    }
                                }
                
                                // Загружаем библиотеки
                                loadLibrariesAsync().then(success => {
                                    if (success && iframeWin.D3Api) {
                                        // Обновляем методы Form с реальными D3Api методами
                                        self.Form.getVar = function(name, defValue) {
                                            return iframeWin.D3Api.getVar ? iframeWin.D3Api.getVar(name, defValue) : defValue;
                                        };
                                        self.Form.setVar = function(name, value) {
                                            if (iframeWin.D3Api.setVar) iframeWin.D3Api.setVar(name, value);
                                        };
                                        self.Form.getValue = function(name, defValue) {
                                            return iframeWin.D3Api.getValue ? iframeWin.D3Api.getValue(name, defValue) : defValue;
                                        };
                                        self.Form.setValue = function(name, value) {
                                            if (iframeWin.D3Api.setValue) iframeWin.D3Api.setValue(name, value);
                                        };
                                        self.Form.getCaption = function(name) {
                                            return iframeWin.D3Api.getCaption ? iframeWin.D3Api.getCaption(name) : '';
                                        };
                                        self.Form.setCaption = function(name, value) {
                                            if (iframeWin.D3Api.setCaption) iframeWin.D3Api.setCaption(name, value);
                                        };
                                        self.Form.close = function(result) {
                                            if (iframeWin.D3Api.close) {
                                                iframeWin.D3Api.close(result);
                                            } else if (self.D3Api && self.D3Api.close) {
                                                self.D3Api.close(result);
                                            }
                                        };
                
                                        // Находим элемент Form и сохраняем его
                                        const formEl = iframeDoc.querySelector('[cmptype="Form"]');
                                        if (formEl) {
                                            self.caption = formEl.getAttribute('caption');
                                            const title = self.element.querySelector('[data-role="title"]');
                                            if (title) title.innerText = self.caption;
                                            self.Form._DOM_ = formEl;
                                        }
                
                                        // Отправляем команду init
                                        self.sendToIframe({
                                            command: 'init',
                                            data: vars
                                        });
                
                                        // Показываем окно
                                        self.show();
                
                                        // Вызываем onCreate если он определен
                                        if (typeof self.Form.onCreate === 'function') {
                                            try {
                                                self.Form.onCreate.call(iframeWin.D3Api || iframeWin, self);
                                            } catch (e) {
                                                console.error('Error in Form.onCreate:', e);
                                            }
                                        }
                
                                        // Вызываем oncreate из данных если есть
                                        if (self.options.oncreate && typeof self.options.oncreate === 'function') {
                                            self.options.oncreate.call(self.D3Api, self);
                                        }
                                    } else {
                                        console.error('Failed to load required libraries');
                                        self.show();
                                    }
                                });
                            }
                
                            show() {
                                if (this.closed) return;
                
                                _activeWindow = this;
                
                                this.element.style.display = 'flex';
                
                                if (this.overlay && this.modal) {
                                    this.overlay.style.display = 'block';
                                }
                
                                this.element.classList.add('animate');
                
                                var self = this;
                                requestAnimationFrame(function() {
                                    self.element.classList.remove('animate');
                                    if (self.options.onshow && typeof self.options.onshow === 'function') {
                                        self.options.onshow.call(self.D3Api, self);
                                    }
                                    self.dispatchEvent('show');
                                });
                
                                this.sendToIframe({ command: 'show' });
                            }
                
                            hide() {
                                this.element.style.display = 'none';
                                if (this.overlay) {
                                    this.overlay.style.display = 'none';
                                }
                                this.dispatchEvent('hide');
                                this.sendToIframe({ command: 'hide' });
                            }
                
                            close(result) {
                                if (this.closed) return;
                
                                this.closed = true;
                
                                this.dispatchEvent('beforeClose', result);
                
                                this.sendToIframe({ command: 'close', result: result });
                
                                if (this.element) {
                                    removeElement(this.element);
                                }
                                if (this.overlay) {
                                    removeElement(this.overlay);
                                }
                
                                delete window.__d3Windows[this.windowId];
                
                                this.dispatchEvent('close', result);
                
                                if (_activeWindow === this) {
                                    _activeWindow = null;
                                }
                            }
                
                            center() {
                                let size = getDocumentSize();
                                let winSize = getElementSize(this.element);
                                setPosition(
                                    this.element,
                                    (size.width - winSize.width) / 2,
                                    (size.height - winSize.height) / 2
                                );
                            }
                
                            addListener(event, callback) {
                                if (!this.listeners[event]) {
                                    this.listeners[event] = [];
                                }
                                this.listeners[event].push(callback);
                            }
                
                            removeListener(event, callback) {
                                if (this.listeners[event]) {
                                    this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
                                }
                            }
                
                            dispatchEvent(event, data) {
                                if (this.listeners[event]) {
                                    this.listeners[event].forEach(callback => {
                                        try {
                                            callback(data, this);
                                        } catch (e) {
                                            console.error('Error in window event handler:', e);
                                        }
                                    });
                                }
                            }
                        }
                
                        window.openD3Form = function(name, modal, data) {
                            data = data || {};
                            data.modal = modal;
                
                            let url = name;
                            if (name.indexOf('.') === -1) {
                                url = name + '.html';
                            }
                
                            console.log('openD3Form:', modal ? 'modal' : 'page', url);
                
                            if (modal === false) {
                                navigateToPage(url, data);
                                return {
                                    close: function(result) {
                                        window.close(result);
                                    },
                                    loadScript: function() {
                                        return Promise.reject('Not available in page mode');
                                    },
                                    executeScript: function() {
                                        return false;
                                    },
                                    getScriptStatus: function() {
                                        return { exists: false };
                                    },
                                    waitForScript: function() {
                                        return Promise.reject('Not available');
                                    },
                                    setVar: function() {},
                                    getVar: function() {},
                                    setValue: function() {},
                                    getValue: function() {},
                                    setCaption: function() {},
                                    getCaption: function() {}
                                };
                            }
                
                            let win = new DWindow({
                                modal: modal,
                                width: data.width || 500,
                                height: data.height || 400,
                                caption: data.caption || 'Окно',
                                theme: data.theme || 'modern',
                                url: url,
                                onshow: data.onshow
                            });
                
                            if (!win.element || !win.iframe) {
                                console.error('Failed to create window properly');
                                return null;
                            }
                
                            // Загружаем содержимое через fetch
                            win.loadContent(url);
                
                            if (data.onclose) {
                                if (Array.isArray(data.onclose)) {
                                    data.onclose.forEach(callback => {
                                        if (typeof callback === 'function') {
                                            win.addListener('close', (result) => callback(result));
                                        }
                                    });
                                } else if (typeof data.onclose === 'function') {
                                    win.addListener('close', (result) => data.onclose(result));
                                }
                            }
                
                            win.element.__win = win;
                            return win.D3Api;
                        };
                
                        // Расширяем глобальный D3Api методами для работы с окнами
                        window.D3Api.openD3Form = function(name, modal, data) {
                            return window.openD3Form(name, modal, data);
                        };
                
                        window.D3Api.getPage = function() {
                            return _activeWindow ? _activeWindow.D3Api : null;
                        };
                
                        window.D3Api.close = function(result) {
                            window.close(result);
                        };
                
                        console.log('cmpWindow: Component initialized');
                
                        // Восстанавливаем колбэки при загрузке страницы
                        if (document.readyState === 'loading') {
                            document.addEventListener('DOMContentLoaded', restorePageCallbacks);
                        } else {
                            restorePageCallbacks();
                        }
                    }
                
                    waitForD3Api(initialize);
                })();
                """);

        return js.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}