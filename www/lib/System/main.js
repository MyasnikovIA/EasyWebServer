window.GLOBAL_VARS = {};
window.GLOBAL_SESSION = {};

D3Api = new function () {
    var GLOBAL_VARS = {};
    var GLOBAL_SESSION = {};
    var GLOBAL_CTRL = {};

    // Хранилища для callback-функций отслеживания изменений
    var VAR_WATCHERS = {};      // для переменных (srctype="var")
    var VALUE_WATCHERS = {};    // для значений контролов (srctype="ctrl")
    var CAPTION_WATCHERS = {};  // для подписей (srctype="caption")
    var SESSION_WATCHERS = {};  // для сессионных переменных (srctype="session")

    this.Form = {}
    this.forms = {};
    this.GLOBAL_ACTION = {};
    this.GLOBAL_DATA_SET = {};
    this.platform = "windows";

    /**
     * @property {Function} Инициализация проекта
     * @returns void
     */
    this.init = function(body) {
        D3Api.MainDom = body || document.body;
        D3Api.D3MainContainer = D3Api.MainDom;
        if (!D3Api.D3MainContainer || D3Api.D3MainContainer.length == 0) {
            let tagArr = document.getElementsByTagName('html');
            if (tagArr.length>0) {
                D3Api.D3MainContainer = tagArr[0];
            }
        }
    }

    /**
     * Работа с переменными страницы (srctype="var")
     */
    this.setVar = function(name, value) {
        var oldValue = GLOBAL_VARS[name];
        var newValue = value;

        // Вызываем watchers перед изменением
        if (VAR_WATCHERS[name]) {
            for (var i = 0; i < VAR_WATCHERS[name].length; i++) {
                var watcher = VAR_WATCHERS[name][i];
                var result = watcher.callback(newValue, oldValue);
                // Если callback вернул значение (не undefined), используем его
                if (result !== undefined) {
                    newValue = result;
                }
            }
        }

        GLOBAL_VARS[name] = newValue;
        $(document).trigger('varChanged', [name, newValue, oldValue]);
    }

    this.getVar = function(name, defValue) {
        return GLOBAL_VARS[name] !== undefined ? GLOBAL_VARS[name] : defValue;
    }

    /**
     * Добавление отслеживания изменения переменной
     * @param {string} name - Имя переменной
     * @param {function} callback - Функция обратного вызова (newValue, oldValue)
     * @returns {string} ID подписки для возможного удаления
     */
    this.onChangeVar = function(name, callback) {
        if (typeof callback !== 'function') return null;

        if (!VAR_WATCHERS[name]) {
            VAR_WATCHERS[name] = [];
        }

        var watcherId = 'var_' + name + '_' + Date.now() + '_' + Math.random();
        VAR_WATCHERS[name].push({
            id: watcherId,
            callback: callback
        });

        return watcherId;
    }

    /**
     * Удаление отслеживания изменения переменной
     * @param {string|function} nameOrId - Имя переменной или ID подписки
     * @param {function} [callback] - Опционально, конкретный callback для удаления
     */
    this.offChangeVar = function(nameOrId, callback) {
        // Если передан ID подписки (строка, начинающаяся с 'var_')
        if (typeof nameOrId === 'string' && nameOrId.indexOf('var_') === 0) {
            for (var name in VAR_WATCHERS) {
                VAR_WATCHERS[name] = VAR_WATCHERS[name].filter(function(w) {
                    return w.id !== nameOrId;
                });
                if (VAR_WATCHERS[name].length === 0) {
                    delete VAR_WATCHERS[name];
                }
            }
        }
        // Если передано имя переменной
        else if (typeof nameOrId === 'string') {
            if (callback && typeof callback === 'function') {
                // Удаляем конкретный callback по имени
                if (VAR_WATCHERS[nameOrId]) {
                    VAR_WATCHERS[nameOrId] = VAR_WATCHERS[nameOrId].filter(function(w) {
                        return w.callback !== callback;
                    });
                    if (VAR_WATCHERS[nameOrId].length === 0) {
                        delete VAR_WATCHERS[nameOrId];
                    }
                }
            } else {
                // Удаляем все callback для указанной переменной
                delete VAR_WATCHERS[nameOrId];
            }
        }
    }

    /**
     * Работа с сессионными переменными (srctype="session")
     */
    this.setSession = function(name, value) {
        var oldValue = GLOBAL_SESSION[name];
        var newValue = value;

        // Вызываем watchers перед изменением
        if (SESSION_WATCHERS[name]) {
            for (var i = 0; i < SESSION_WATCHERS[name].length; i++) {
                var watcher = SESSION_WATCHERS[name][i];
                var result = watcher.callback(newValue, oldValue);
                // Если callback вернул значение (не undefined), используем его
                if (result !== undefined) {
                    newValue = result;
                }
            }
        }

        GLOBAL_SESSION[name] = newValue;

        $.ajax({
            url: '/{component}/session',
            method: 'POST',
            data: JSON.stringify({
                action: 'set',
                name: name,
                value: newValue
            }),
            contentType: 'application/json',
            success: function(response) {
                $(document).trigger('sessionSaved', [name, newValue]);
            }
        });

        $(document).trigger('sessionChanged', [name, newValue, oldValue]);
    }

    this.getSession = function(name, defValue) {
        return GLOBAL_SESSION[name] !== undefined ? GLOBAL_SESSION[name] : defValue;
    }

    /**
     * Добавление отслеживания изменения сессионной переменной
     * @param {string} name - Имя переменной
     * @param {function} callback - Функция обратного вызова (newValue, oldValue)
     * @returns {string} ID подписки
     */
    this.onChangeSession = function(name, callback) {
        if (typeof callback !== 'function') return null;

        if (!SESSION_WATCHERS[name]) {
            SESSION_WATCHERS[name] = [];
        }

        var watcherId = 'sess_' + name + '_' + Date.now() + '_' + Math.random();
        SESSION_WATCHERS[name].push({
            id: watcherId,
            callback: callback
        });

        return watcherId;
    }

    /**
     * Удаление отслеживания изменения сессионной переменной
     * @param {string|function} nameOrId - Имя переменной или ID подписки
     * @param {function} [callback] - Опционально, конкретный callback для удаления
     */
    this.offChangeSession = function(nameOrId, callback) {
        // Если передан ID подписки
        if (typeof nameOrId === 'string' && nameOrId.indexOf('sess_') === 0) {
            for (var name in SESSION_WATCHERS) {
                SESSION_WATCHERS[name] = SESSION_WATCHERS[name].filter(function(w) {
                    return w.id !== nameOrId;
                });
                if (SESSION_WATCHERS[name].length === 0) {
                    delete SESSION_WATCHERS[name];
                }
            }
        }
        // Если передано имя переменной
        else if (typeof nameOrId === 'string') {
            if (callback && typeof callback === 'function') {
                // Удаляем конкретный callback по имени
                if (SESSION_WATCHERS[nameOrId]) {
                    SESSION_WATCHERS[nameOrId] = SESSION_WATCHERS[nameOrId].filter(function(w) {
                        return w.callback !== callback;
                    });
                    if (SESSION_WATCHERS[nameOrId].length === 0) {
                        delete SESSION_WATCHERS[nameOrId];
                    }
                }
            } else {
                // Удаляем все callback для указанной переменной
                delete SESSION_WATCHERS[nameOrId];
            }
        }
    }

    /**
     * Работа с подписями контролов (srctype="caption")
     */
    this.setCaption = function(name, text) {
        var ctrl = this.getControl(name);
        if (ctrl && ctrl.length > 0) {
            var oldText = this.getCaption(name);
            var newText = text;

            // Вызываем watchers перед изменением
            if (CAPTION_WATCHERS[name]) {
                for (var i = 0; i < CAPTION_WATCHERS[name].length; i++) {
                    var watcher = CAPTION_WATCHERS[name][i];
                    var result = watcher.callback(newText, oldText);
                    // Если callback вернул значение (не undefined), используем его
                    if (result !== undefined) {
                        newText = result;
                    }
                }
            }

            var captionEl = ctrl.find('[block="caption"]');
            if (captionEl.length > 0) {
                captionEl.text(newText);
            } else {
                ctrl.text(newText);
            }

            $(document).trigger('captionChanged', [name, newText, oldText]);
            return true;
        }
        return false;
    }

    this.getCaption = function(name) {
        var ctrl = this.getControl(name);
        if (ctrl && ctrl.length > 0) {
            var captionEl = ctrl.find('[block="caption"]');
            if (captionEl.length > 0) {
                return captionEl.text();
            } else {
                return ctrl.text();
            }
        }
        return null;
    }

    /**
     * Добавление отслеживания изменения подписи
     * @param {string} name - Имя контрола
     * @param {function} callback - Функция обратного вызова (newText, oldText)
     * @returns {string} ID подписки
     */
    this.onChangeCaption = function(name, callback) {
        if (typeof callback !== 'function') return null;

        if (!CAPTION_WATCHERS[name]) {
            CAPTION_WATCHERS[name] = [];
        }

        var watcherId = 'cap_' + name + '_' + Date.now() + '_' + Math.random();
        CAPTION_WATCHERS[name].push({
            id: watcherId,
            callback: callback
        });

        return watcherId;
    }

    /**
     * Удаление отслеживания изменения подписи
     * @param {string|function} nameOrId - Имя контрола или ID подписки
     * @param {function} [callback] - Опционально, конкретный callback для удаления
     */
    this.offChangeCaption = function(nameOrId, callback) {
        // Если передан ID подписки
        if (typeof nameOrId === 'string' && nameOrId.indexOf('cap_') === 0) {
            for (var name in CAPTION_WATCHERS) {
                CAPTION_WATCHERS[name] = CAPTION_WATCHERS[name].filter(function(w) {
                    return w.id !== nameOrId;
                });
                if (CAPTION_WATCHERS[name].length === 0) {
                    delete CAPTION_WATCHERS[name];
                }
            }
        }
        // Если передано имя контрола
        else if (typeof nameOrId === 'string') {
            if (callback && typeof callback === 'function') {
                // Удаляем конкретный callback по имени
                if (CAPTION_WATCHERS[nameOrId]) {
                    CAPTION_WATCHERS[nameOrId] = CAPTION_WATCHERS[nameOrId].filter(function(w) {
                        return w.callback !== callback;
                    });
                    if (CAPTION_WATCHERS[nameOrId].length === 0) {
                        delete CAPTION_WATCHERS[nameOrId];
                    }
                }
            } else {
                // Удаляем все callback для указанного контрола
                delete CAPTION_WATCHERS[nameOrId];
            }
        }
    }

    /**
     * Работа со значениями контролов (srctype="ctrl")
     */
    this.setValue = function(name, value) {
        var ctrlObj = $('[name="'+name+'"]');
        if (ctrlObj.length === 0) return false;

        var oldValue = this.getValue(name);
        var newValue = value;

        // Вызываем watchers перед изменением
        if (VALUE_WATCHERS[name]) {
            for (var i = 0; i < VALUE_WATCHERS[name].length; i++) {
                var watcher = VALUE_WATCHERS[name][i];
                var result = watcher.callback(newValue, oldValue);
                // Если callback вернул значение (не undefined), используем его
                if (result !== undefined) {
                    newValue = result;
                }
            }
        }

        ctrlObj.val(newValue);

        if (ctrlObj.attr('type') === 'checkbox') {
            ctrlObj.prop('checked', newValue === 'on' || newValue === true || newValue === 'true');
        } else if (ctrlObj.attr('type') === 'radio') {
            ctrlObj.filter('[value="' + newValue + '"]').prop('checked', true);
        }

        ctrlObj.trigger('change');
        $(document).trigger('valueChanged', [name, newValue, oldValue]);
        return true;
    }

    this.getValue = function(name, defValue) {
        var ctrlObj = $('[name="'+name+'"]');
        if (ctrlObj.length === 0) return defValue;

        if (ctrlObj.attr('type') === 'checkbox') {
            return ctrlObj.is(':checked');
        } else if (ctrlObj.attr('type') === 'radio') {
            var checked = ctrlObj.filter(':checked');
            return checked.length > 0 ? checked.val() : defValue;
        } else {
            var val = ctrlObj.val();
            return val !== undefined && val !== null ? val : defValue;
        }
    }

    /**
     * Добавление отслеживания изменения значения контрола
     * @param {string} name - Имя контрола
     * @param {function} callback - Функция обратного вызова (newValue, oldValue)
     * @returns {string} ID подписки
     */
    this.onChangeValue = function(name, callback) {
        if (typeof callback !== 'function') return null;

        if (!VALUE_WATCHERS[name]) {
            VALUE_WATCHERS[name] = [];
        }

        var watcherId = 'val_' + name + '_' + Date.now() + '_' + Math.random();
        VALUE_WATCHERS[name].push({
            id: watcherId,
            callback: callback
        });

        return watcherId;
    }

    /**
     * Удаление отслеживания изменения значения контрола
     * @param {string|function} nameOrId - Имя контрола или ID подписки
     * @param {function} [callback] - Опционально, конкретный callback для удаления
     */
    this.offChangeValue = function(nameOrId, callback) {
        // Если передан ID подписки
        if (typeof nameOrId === 'string' && nameOrId.indexOf('val_') === 0) {
            for (var name in VALUE_WATCHERS) {
                VALUE_WATCHERS[name] = VALUE_WATCHERS[name].filter(function(w) {
                    return w.id !== nameOrId;
                });
                if (VALUE_WATCHERS[name].length === 0) {
                    delete VALUE_WATCHERS[name];
                }
            }
        }
        // Если передано имя контрола
        else if (typeof nameOrId === 'string') {
            if (callback && typeof callback === 'function') {
                // Удаляем конкретный callback по имени
                if (VALUE_WATCHERS[nameOrId]) {
                    VALUE_WATCHERS[nameOrId] = VALUE_WATCHERS[nameOrId].filter(function(w) {
                        return w.callback !== callback;
                    });
                    if (VALUE_WATCHERS[nameOrId].length === 0) {
                        delete VALUE_WATCHERS[nameOrId];
                    }
                }
            } else {
                // Удаляем все callback для указанного контрола
                delete VALUE_WATCHERS[nameOrId];
            }
        }
    }

    /**
     * Получение контрола по имени
     */
    this.getControl = function(name) {
        if (GLOBAL_CTRL[name]) {
            return GLOBAL_CTRL[name];
        }
        var ctrl = $('[name="'+name+'"]');
        if (ctrl.length > 0) {
            GLOBAL_CTRL[name] = ctrl;
        }
        return ctrl;
    }

    /**
     * Инициализация сессии при загрузке страницы
     */
    this.initSession = function() {
        $.ajax({
            url: '/{component}/session',
            method: 'GET',
            data: { action: 'getAll' },
            dataType: 'json',
            success: function(data) {
                GLOBAL_SESSION = data;
                $(document).trigger('sessionLoaded', [data]);
            }
        });
    }

    // Методы для обратной совместимости
    this.setAction = function(name, obj) {
        this.GLOBAL_ACTION[name] = obj;
    }

    this.setActionAuto = function(name) {
        this.GLOBAL_ACTION[name] = {};
    }

    this.setDatasetAuto = function(name) {
        this.GLOBAL_DATA_SET[name] = {"data":[]};
    }

    this.setDataset = function(name, obj) {
        this.GLOBAL_DATA_SET[name] = obj;
        Object.defineProperty(this.GLOBAL_DATA_SET[name], 'data', {
            get: function() {
                return this._data || [];
            },
            set: function(value) {
                this._data = value;
                $(document).trigger('datasetChanged', [name, value]);
            }
        });
        this.GLOBAL_DATA_SET[name]._data = obj.data || [];
    }

    this.getDataset = function(name) {
        return this.GLOBAL_DATA_SET[name];
    }

    this.setControlAuto = function(name, obj) {
        GLOBAL_CTRL[name] = $(obj);
    }

    this.setLabel = function(name, text) {
        if (GLOBAL_CTRL[name]) {
            (GLOBAL_CTRL[name]).find('[block="label"]').text(text);
            return true;
        } else {
            var ctrlObj;
            if (typeof name === 'object') {
                ctrlObj = name;
            } else {
                ctrlObj = $('[name="'+name+'"]');
            }
            var ctrl = ctrlObj.find('[name="'+name+'_ctrl"]');
            if (ctrl.length === 0) {
                ctrl = this.getCtrl(name);
            }
            if (ctrl.length > 0) {
                ctrl[0].innerText = text;
                return true;
            }
        }
        return false;
    }

    this.getLabel = function(name) {
        if (GLOBAL_CTRL[name]) {
            return (GLOBAL_CTRL[name]).find('[block="label"]').text();
        }
        return null;
    }

    this.setLabels = function(obj) {
        for (const name in obj) {
            this.setLabel(name, obj[name]);
        }
    }

    this.getLabels = function() {
        var ctrlList = $('[schema]');
        var res = {};
        for (var i = 0; i < ctrlList.length; i++) {
            var name = ctrlList[i].getAttribute('name');
            res[name] = this.getLabel(name);
        }
        return res;
    }

    this.getCtrl = function(name) {
        let ctrlName = $('[name="'+name+'"]').attr('ctrl');
        return  $('[name="'+ctrlName+'"]');
    }

    this.getValues = function() {
        let ctrlList = $('[schema]');
        let res = {};
        if (!ctrlList) return res;
        for (let i = 0; i < ctrlList.length; i++) {
            let name = ctrlList[i].getAttribute('name');
            res[name] = this.getValue(name);
        }
        return res;
    }

    this.setValues = function(obj) {
        for (const name in obj) {
            this.setValue(name, obj[name]);
        }
    }

    this.setDisabled = function(name, bool) {
        bool = (bool == true || bool);
        var ctrlObj = $('[name="'+name+'"]');
        var ctrl =  this.getCtrl(name);
        var schema =  ctrlObj.attr("schema");
        if (ctrlObj.attr('type') === 'accordion') {
            bool ? ctrl.accordion( 'disable' ) : ctrl.accordion( 'enable' );
        } else if (ctrlObj.attr('type') === 'tabs') {
            bool ? ctrl.tabs( 'disable' ) : ctrl.tabs( 'enable' );
        } else {
            if (bool) {
                ctrl.prop( "disabled", true);
            } else {
                ctrl.prop( "disabled", false);
                ctrl.removeAttr('disabled');
                if (ctrl.hasClass( "ui-button-disabled" )) ctrl.removeClass( "ui-button-disabled" );
                if (ctrl.hasClass( "ui-state-disabled" )) ctrl.removeClass( "ui-state-disabled" );
            }
        }
    }

    this.setDisableds = function(obj) {
        for (const name in obj) {
            this.setDisabled(name, obj[name]);
        }
    }

    this.setDisabledArr = function(arr, val) {
        for (const ind in arr) {
            var ctrlName = arr[ind].trim();
            if (ctrlName.length > 0) this.setDisabled(ctrlName, val);
        }
    }

    this.setVisible = function(name, bool) {
        bool = (bool == true || bool);
        var ctrl = D3Api.getControl(name);
        bool ? ctrl.css("visibility", "visible") : ctrl.css("visibility", "hidden");
    }

    this.setVisibles = function(obj) {
        for (const name in obj) {
            this.setVisible(name, obj[name]);
        }
    }

    this.setStyle = function (name, propObject) {
        let ctrl = D3Api.getControl(name);
        for (const key in propObject) {
            ctrl.css(key, propObject[key]);
        }
    }

    this.move = function(name, bool) {
        let ctrlObj = $('[name="'+name+'"]');
        if (bool) {
            ctrlObj.draggable().draggable( 'enable' );
            ctrlObj.resizable({animate: true});
            ctrlObj.resizable( 'enable' );
            this.setDisabled(name, true);
        } else {
            ctrlObj.draggable( 'disable' );
            ctrlObj.resizable( 'disable' );
            this.setDisabled(name, false);
        }
    }

    this.draggable = function(name, bool) {
        let ctrlObj = $('[name="'+name+'"]');
        if (bool) {
            ctrlObj.draggable().draggable( 'enable' );
        } else {
            ctrlObj.draggable( 'disable' );
        }
    }

    this.resizable = function(name, bool) {
        let ctrlObj = $('[name="'+name+'"]');
        if (bool) {
            ctrlObj.resizable({animate: true});
            ctrlObj.resizable( 'enable' );
            this.setDisabled(name, true);
        } else {
            ctrlObj.resizable( 'disable' );
            this.setDisabled(name, false);
        }
    }

    this.msgbox = function(text, buttontext, callback) {
        buttontext = buttontext || "OK";
        $( "<div>" + text + "</div>" ).dialog({
            dialogClass: "no-close",
            buttons: [
                {
                    text: buttontext,
                    click: function() {
                        $( this ).dialog( "close" );
                        $(this).remove();
                        if (callback) callback();
                    }
                }
            ]
        });
    }
}

window.d3 = new D3Api.init(document.getElementsByTagName("body"));

document.addEventListener("DOMContentLoaded", function() {
    D3Api.initSession();

    const elementsWithNameAttribute = D3Api.D3MainContainer.querySelectorAll('[name][cmptype]');
    const elementsArray = Array.from(elementsWithNameAttribute);
    for (let ctrlObj of elementsArray) {
        if (ctrlObj.getAttribute('cmptype')) {
            const cmptype = ctrlObj.getAttribute('cmptype').toLocaleLowerCase();
            if ((cmptype === 'action') || (cmptype === 'dataset')) continue;
        }
        const nameCtrl = ctrlObj.getAttribute('name');
        D3Api.setControlAuto(nameCtrl, ctrlObj);
    }
});

// Примеры использования новых методов:
/*
// 1. Отслеживание изменения переменной с переопределением значения
D3Api.onChangeVar('userName', function(newValue, oldValue) {
    console.log('userName изменяется с', oldValue, 'на', newValue);
    // Переопределяем значение - добавляем префикс (только если newValue не undefined)
    if (newValue) return 'Mr. ' + newValue;
});

// 2. Отслеживание без переопределения (нет return)
D3Api.onChangeVar('age', function(newValue, oldValue) {
    console.log('Возраст изменился с', oldValue, 'на', newValue);
    // Нет return - значение не меняется
});

// 3. Отслеживание изменения значения контрола
D3Api.onChangeValue('inputField', function(newValue, oldValue) {
    console.log('Поле ввода изменилось');
    return newValue.toUpperCase(); // преобразуем в верхний регистр
});

// 4. Отслеживание изменения подписи
D3Api.onChangeCaption('statusLabel', function(newText, oldText) {
    console.log('Подпись изменена');
    return newText + ' (' + new Date().toLocaleTimeString() + ')'; // добавляем время
});

// 5. Отслеживание изменения сессионной переменной
D3Api.onChangeSession('userId', function(newValue, oldValue) {
    console.log('ID пользователя в сессии изменился с', oldValue, 'на', newValue);
    if (newValue) {
        // Автоматически загружаем данные пользователя
        D3Api.setVar('currentUserId', newValue);
    }
});

// 6. Удаление подписок разными способами
var watcherId = D3Api.onChangeVar('test', function(v) { console.log(v); });

// По ID
D3Api.offChangeVar(watcherId);

// По имени переменной (удалить все подписки для 'test')
D3Api.offChangeVar('test');

// По имени переменной и конкретному callback
function myHandler(v) { console.log(v); }
D3Api.onChangeVar('test', myHandler);
D3Api.offChangeVar('test', myHandler);
*/