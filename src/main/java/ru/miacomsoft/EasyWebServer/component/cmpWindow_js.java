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
                
                // Функция ожидания инициализации D3Api с колбэком
                function waitForD3Api(callback) {
                    function checkD3Api() {
                        if (typeof window.D3Api !== 'undefined' && 
                            window.D3Api !== null && 
                            typeof window.D3Api.setVar === 'function') {
                            callback();
                            return;
                        }
                        // Используем requestAnimationFrame вместо setTimeout для колбэка
                        requestAnimationFrame(checkD3Api);
                    }
                    checkD3Api();
                }
                
                function initialize() {
                    if (window.cmpWindowInitialized) return;
                    window.cmpWindowInitialized = true;
                    
                    // Хранилище для всех созданных окон
                    if (!window.__d3Windows) window.__d3Windows = {};
                    
                    // Хранилище для индивидуальных объектов D3Api каждого окна
                    if (!window.__d3WindowApis) window.__d3WindowApis = {};
                    
                    // Текущее активное окно
                    var _activeWindow = null;
                    
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
                     * Функция для получения активного окна (глобальная)
                     */
                    window.getPage = function() {
                        return _activeWindow ? _activeWindow.windowD3Api : null;
                    };

                    /**
                     * Глобальная функция для закрытия текущего окна и передачи результата
                     */
                    window.close = function(result) {
                        if (_activeWindow) {
                            _activeWindow.close(result);
                        } else {
                            // Если нет активного окна, пробуем найти последнее созданное
                            var windows = Object.values(window.__d3Windows);
                            if (windows.length > 0) {
                                windows[windows.length - 1].close(result);
                            }
                        }
                    };

                    /**
                     * Генерация HTML для окна с использованием DIV верстки
                     */
                    function getWindowXml(options) {
                        options = options || {};
                        let modal = options.modal !== false;
                        let width = options.width || 500;
                        let height = options.height || 400;
                        let caption = options.caption || '';
                        let theme = options.theme || 'modern';
                        let url = options.url || '';
                        
                        // Создаем overlay
                        let overlay = '';
                        if (modal) {
                            overlay = '<div class="win_overlow"></div>';
                        }
                        
                        // Создаем окно на DIV-ах
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
                                    <iframe class="window-iframe" data-role="iframe" src="${url}" frameborder="0" style="width: 100%; height: 100%; border: none; display: block;"></iframe>
                                </div>
                                
                                <!-- Handles для изменения размера -->
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
                            this.iframeOverlay = null; // Временный DIV для перекрытия iframe
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
                            
                            // Индивидуальный D3Api для этого окна
                            this.windowD3Api = null;
                            
                            // Сохраняем ссылку на окно в глобальном хранилище
                            this.windowId = 'win_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                            window.__d3Windows[this.windowId] = this;
                            
                            // Создаем индивидуальный D3Api для этого окна
                            this.createWindowD3Api();
                            
                            this.init();
                        }
                        
                        /**
                         * Создает индивидуальный объект D3Api для этого окна
                         */
                        createWindowD3Api() {
                            var self = this;
                            
                            // Базовый объект D3Api для окна
                            this.windowD3Api = {
                                _windowId: self.windowId,
                                _vars: {},
                                _session: {},
                                _ctrl: {},
                                
                                // Работа с переменными окна
                                setVar: function(name, value) {
                                    self._vars = self._vars || {};
                                    self._vars[name] = value;
                                    this._vars[name] = value;
                                },
                                
                                getVar: function(name, defValue) {
                                    self._vars = self._vars || {};
                                    return self._vars[name] !== undefined ? self._vars[name] : defValue;
                                },
                                
                                // Работа с сессией окна
                                setSession: function(name, value) {
                                    self._session = self._session || {};
                                    self._session[name] = value;
                                    this._session[name] = value;
                                },
                                
                                getSession: function(name, defValue) {
                                    self._session = self._session || {};
                                    return self._session[name] !== undefined ? self._session[name] : defValue;
                                },
                                
                                // Работа с контролами окна
                                setValue: function(name, value) {
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
                                },
                                
                                getValue: function(name, defValue) {
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
                                },
                                
                                // Метод для получения самого окна (для расширенного доступа)
                                getWindow: function() {
                                    return self;
                                },
                                
                                // Закрытие окна
                                close: function(result) {
                                    self.close(result);
                                },
                                
                                // Отправка сообщения в родительское окно
                                sendToParent: function(message) {
                                    if (window.parent && window.parent !== window) {
                                        window.parent.postMessage(message, '*');
                                    }
                                },
                                
                                // Добавление слушателя событий
                                on: function(event, callback) {
                                    self.addListener(event, callback);
                                    return this;
                                },
                                
                                // Установка заголовка
                                setCaption: function(text) {
                                    self.setCaption(text);
                                    return this;
                                },
                                
                                // Изменение размера
                                setSize: function(width, height) {
                                    self.setSize(width, height);
                                    return this;
                                },
                                
                                // Центрирование
                                center: function() {
                                    self.center();
                                    return this;
                                },
                                
                                // Показать
                                show: function() {
                                    self.show();
                                    return this;
                                },
                                
                                // Скрыть
                                hide: function() {
                                    self.hide();
                                    return this;
                                },
                                
                                // Перезагрузить iframe
                                reload: function() {
                                    self.reload();
                                    return this;
                                }
                            };
                            
                            // Сохраняем в глобальном хранилище
                            window.__d3WindowApis[this.windowId] = this.windowD3Api;
                        }
                        
                        init() {
                            
                            // Создаем временный контейнер и парсим HTML
                            let temp = document.createElement('div');
                            temp.innerHTML = getWindowXml(this.options);
                            
                            // Находим overlay (первый дочерний элемент)
                            if (this.modal) {
                                this.overlay = temp.querySelector('.win_overlow');
                            }
                            
                            // Находим окно
                            this.element = temp.querySelector('.window');
                            
                            if (!this.element) {
                                console.error('Failed to create window element');
                                return;
                            }
                            
                            // Находим элементы внутри окна
                            this.iframe = this.element.querySelector('[data-role="iframe"]');
                            this.title = this.element.querySelector('[data-role="title"]');
                            this.titleRow = this.element.querySelector('[data-role="title-row"]');
                            
                            // Добавляем в DOM
                            if (this.overlay) {
                                document.body.appendChild(this.overlay);
                                this.overlay.style.display = 'none';
                            }
                            document.body.appendChild(this.element);
                            
                            // Устанавливаем display: none для элемента (на всякий случай)
                            this.element.style.display = 'none';
                            
                            // Проверяем, что все элементы найдены
                            if (!this.iframe) {
                                console.warn('Iframe element not found in window');
                            }
                            
                            if (!this.titleRow) {
                                console.warn('Title row element not found in window');
                            }
                            
                            // Инициализируем обработчики
                            this.initEventHandlers();
                            
                            // Сохраняем оригинальные размеры и позицию
                            this.originalSize = { width: this.width, height: this.height };
                            
                            // Настраиваем обработку сообщений от iframe
                            this.setupIframeMessaging();
                        }
                        
                        /**
                         * Создает временный DIV для перекрытия iframe
                         */
                        createIframeOverlay() {
                            if (!this.iframe || this.iframeOverlay) return;
                            
                            // Определяем курсор в зависимости от режима
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
                            
                            // Создаем DIV-оверлей
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
                            
                            // Добавляем оверлей в контейнер контента (поверх iframe)
                            let contentDiv = this.element.querySelector('.window-content');
                            if (contentDiv) {
                                contentDiv.appendChild(this.iframeOverlay);
                            }
                        }
                        
                        /**
                         * Удаляет временный DIV для перекрытия iframe
                         */
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
                            
                            // Используем стрелочные функции для сохранения контекста this
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
                                
                                // Удаляем оверлей после отпускания кнопки мыши
                                this.removeIframeOverlay();
                            };

                            // Перемещение окна
                            if (this.titleRow) {
                                this.titleRow.addEventListener('mousedown', (e) => {
                                    // Проверяем, что нажата левая кнопка мыши (button 0)
                                    if (e.button !== 0) return;
                                    this.startDrag(e);
                                });
                                this.titleRow.addEventListener('dblclick', (e) => this.toggleMaximize(e));
                            }

                            // Изменение размера - все возможные handle-ы
                            let resizeHandles = this.element.querySelectorAll('[data-role^="resize-"]');
                            resizeHandles.forEach(handle => {
                                handle.addEventListener('mousedown', (e) => {
                                    // Проверяем, что нажата левая кнопка мыши (button 0)
                                    if (e.button !== 0) return;
                                    let role = handle.getAttribute('data-role');
                                    this.startResize(e, role);
                                });
                            });

                            // Кнопки управления
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

                            // Предотвращаем выделение при перетаскивании
                            this.element.addEventListener('selectstart', (e) => e.preventDefault());

                            // Добавляем глобальные обработчики один раз
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
                            
                            // Создаем оверлей для iframe
                            this.createIframeOverlay();
                        }
                        
                        onDrag(e) {
                            if (!this.dragging) return;
                            
                            e.preventDefault();
                            
                            let left = e.clientX - this.dragOffset.x;
                            let top = e.clientY - this.dragOffset.y;
                            
                            // Ограничиваем перемещение в пределах окна просмотра
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
                            
                            // Создаем оверлей для iframe
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
                            
                            // Применяем новые размеры и позицию
                            this.element.style.width = newWidth + 'px';
                            this.element.style.height = newHeight + 'px';
                            this.element.style.left = newLeft + 'px';
                            this.element.style.top = newTop + 'px';
                            this.element.style.transform = 'none';
                            
                            // Уведомляем iframe об изменении размера
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
                                // Восстанавливаем размеры
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
                                // Сохраняем текущие размеры
                                let rect = this.element.getBoundingClientRect();
                                this.originalPosition = { left: rect.left, top: rect.top };
                                this.originalSize = { width: rect.width, height: rect.height };
                                
                                // На весь экран
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
                            if (this.iframe) {
                                this.iframe.src = this.iframe.src;
                                this.sendToIframe({ command: 'reloaded' });
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
                            if (this.iframe) {
                                this.url = url;
                                this.iframe.src = url;
                            }
                        }
                        
                        setSize(width, height) {
                            this.element.style.width = width + 'px';
                            this.element.style.height = height + 'px';
                            this.width = width;
                            this.height = height;
                        }
                        
                        show() {
                            if (this.closed) return;
                            
                            // Устанавливаем как активное окно
                            _activeWindow = this;
                            
                            // Показываем окно
                            this.element.style.display = 'flex';
                            
                            // Показываем overlay если нужно
                            if (this.overlay && this.modal) {
                                this.overlay.style.display = 'block';
                            }
                            
                            // Добавляем анимацию
                            this.element.classList.add('animate');
                            
                            // !!! ТОЧКА ЗАПУСКА onshow !!!
                            // Здесь вызывается колбэк onshow после отображения окна
                            var self = this;
                            requestAnimationFrame(function() {
                                self.element.classList.remove('animate');
                                if (self.options.onshow && typeof self.options.onshow === 'function') {
                                    self.options.onshow.call(self.windowD3Api, self.windowD3Api);
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
                            
                            // !!! ТОЧКА ЗАПУСКА beforeClose !!!
                            this.dispatchEvent('beforeClose', result);
                            
                            this.sendToIframe({ command: 'close', result: result });
                            
                            if (this.element) {
                                removeElement(this.element);
                            }
                            if (this.overlay) {
                                removeElement(this.overlay);
                            }
                            
                            // Удаляем из глобальных хранилищ
                            delete window.__d3Windows[this.windowId];
                            delete window.__d3WindowApis[this.windowId];
                            
                            // !!! ТОЧКА ЗАПУСКА onclose !!!
                            this.dispatchEvent('close', result);
                            
                            // Сбрасываем активное окно если это было оно
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
                        
                        console.log('openD3Form: creating window with url:', url, 'data:', data);
                        
                        let win = new DWindow({
                            modal: modal,
                            width: data.width || 500,
                            height: data.height || 400,
                            caption: data.caption || 'Окно',
                            theme: data.theme || 'modern',
                            url: url,
                            onshow: data.onshow  // Сохраняем колбэк onshow
                        });
                        
                        // Проверяем, что окно создалось успешно
                        if (!win.element || !win.iframe) {
                            console.error('Failed to create window properly');
                            return null;
                        }
                        
                        // Добавляем обработчик загрузки iframe
                        win.iframe.addEventListener('load', function() {
                            console.log('Iframe loaded for window, sending init data:', data.vars);
                            
                            // Отправляем переменные в iframe
                            win.sendToIframe({
                                command: 'init',
                                data: data.vars || {}  // Передаем vars в iframe
                            });
                            
                            // Показываем окно после загрузки iframe
                            win.show();
                            
                            // !!! ТОЧКА ЗАПУСКА oncreate !!!
                            // Здесь вызывается колбэк oncreate после создания окна
                            if (data.oncreate && typeof data.oncreate === 'function') {
                                data.oncreate.call(win.windowD3Api, win.windowD3Api);
                            }
                        });
                        
                        // Добавляем обработчик ошибки загрузки iframe
                        win.iframe.addEventListener('error', function() {
                            console.error('Failed to load iframe content:', url);
                            let contentDiv = win.element.querySelector('.window-content');
                            if (contentDiv) {
                                contentDiv.innerHTML = 
                                    '<div style="color: red; padding: 20px; text-align: center;">' +
                                    '<h3>Ошибка загрузки</h3>' +
                                    '<p>Не удалось загрузить: ' + url + '</p>' +
                                    '<button onclick="this.closest(\\'.window\\').__win.close()">Закрыть</button>' +
                                    '</div>';
                            }
                            win.show();
                        });
                        
                        // Обработчик сообщений от iframe
                        win.addListener('message', (msg) => {
                            console.log('Message from iframe:', msg);
                            
                            if (msg.command === 'ready') {
                                console.log('Iframe ready');
                            } else if (msg.command === 'close') {
                                win.close(msg.result);
                            } else if (msg.command === 'resize') {
                                if (msg.width && msg.height) {
                                    win.setSize(msg.width, msg.height);
                                }
                            } else if (msg.command === 'setCaption') {
                                win.setCaption(msg.caption);
                            }
                        });
                        
                        // Сохраняем обработчики onclose
                        if (data.onclose) {
                            // Если передан массив функций
                            if (Array.isArray(data.onclose)) {
                                data.onclose.forEach(callback => {
                                    if (typeof callback === 'function') {
                                        win.addListener('close', (result) => callback(result));
                                    }
                                });
                            }
                            // Если передана одна функция
                            else if (typeof data.onclose === 'function') {
                                win.addListener('close', (result) => data.onclose(result));
                            }
                        }
                        
                        // Сохраняем ссылку на окно в элементе для доступа из кнопок
                        win.element.__win = win;
                        
                        console.log('openD3Form: window created');
                        return win.windowD3Api; // Возвращаем индивидуальный D3Api объекта, а не само окно
                    };
                    
                    // ============== Добавление методов в глобальный D3Api ==============
                    
                    // Добавляем метод для получения активного окна
                    window.D3Api.getPage = function() {
                        return _activeWindow ? _activeWindow.windowD3Api : null;
                    };
                    
                    // Добавляем метод для закрытия текущего окна
                    window.D3Api.close = function(result) {
                        if (_activeWindow) {
                            _activeWindow.close(result);
                        } else {
                            // Если нет активного окна, пробуем найти последнее созданное
                            var windows = Object.values(window.__d3Windows);
                            if (windows.length > 0) {
                                windows[windows.length - 1].close(result);
                            }
                        }
                    };
                    
                    window.D3Api.Window = DWindow;
                    
                    window.D3Api.openWindow = function(options) {
                        return new DWindow(options);
                    };
                    
                    window.D3Api.openD3Form = function(name, modal, data) {
                        return window.openD3Form(name, modal, data);
                    };
                    
                    window.D3Api.showModal = function(content, options) {
                        options = options || {};
                        options.modal = true;
                        
                        if (typeof content === 'string' && !content.startsWith('http')) {
                            options.url = 'data:text/html;charset=utf-8,' + encodeURIComponent(content);
                        } else {
                            options.url = content;
                        }
                        
                        let win = new DWindow(options);
                        win.show();
                        
                        return win.windowD3Api;
                    };
                    
                    window.D3Api.showAlert = function(message, title, callback) {
                        let content = `
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; text-align: center; }
                                    .message { margin: 30px 0; font-size: 16px; }
                                    button { padding: 8px 25px; background: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
                                    button:hover { background: #45a049; }
                                </style>
                            </head>
                            <body>
                                <div class="message">${message}</div>
                                <button onclick="window.parent.postMessage({command: 'close'}, '*')">OK</button>
                                <script>
                                    window.parent.postMessage({command: 'ready'}, '*');
                                </script>
                            </body>
                            </html>
                        `;
                        
                        return window.D3Api.showModal(content, {
                            width: 350,
                            height: 200,
                            caption: title || 'Сообщение',
                            modal: true,
                            onclose: callback
                        });
                    };
                    
                    window.D3Api.showConfirm = function(message, title, confirmCallback, cancelCallback) {
                        let content = `
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; text-align: center; }
                                    .message { margin: 30px 0; font-size: 16px; }
                                    .buttons { display: flex; justify-content: center; gap: 10px; }
                                    button { padding: 8px 25px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
                                    .confirm { background: #4CAF50; color: white; }
                                    .confirm:hover { background: #45a049; }
                                    .cancel { background: #f44336; color: white; }
                                    .cancel:hover { background: #da190b; }
                                </style>
                            </head>
                            <body>
                                <div class="message">${message}</div>
                                <div class="buttons">
                                    <button class="confirm" onclick="window.parent.postMessage({command: 'confirm', result: true}, '*')">OK</button>
                                    <button class="cancel" onclick="window.parent.postMessage({command: 'confirm', result: false}, '*')">Отмена</button>
                                </div>
                                <script>
                                    window.parent.postMessage({command: 'ready'}, '*');
                                </script>
                            </body>
                            </html>
                        `;
                        
                        let win = window.D3Api.showModal(content, {
                            width: 350,
                            height: 220,
                            caption: title || 'Подтверждение',
                            modal: true
                        });
                        
                        win.onMessage('confirm', (data) => {
                            win.close();
                            if (data.result && confirmCallback) {
                                confirmCallback();
                            } else if (!data.result && cancelCallback) {
                                cancelCallback();
                            }
                        });
                        
                        return win;
                    };
                }
                
                waitForD3Api(initialize);
            })();
            """);

        return js.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}