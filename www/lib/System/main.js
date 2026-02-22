window.GLOBAL_VARS = {};

// Полифиллы для старых браузеров
if (!Element.prototype.matches) {
    Element.prototype.matches = Element.prototype.msMatchesSelector || Element.prototype.webkitMatchesSelector;
}

if (!Element.prototype.closest) {
    Element.prototype.closest = function(s) {
        var el = this;
        do {
            if (el.matches(s)) return el;
            el = el.parentElement || el.parentNode;
        } while (el !== null && el.nodeType === 1);
        return null;
    };
}

D3Api = new function () {
    var GLOBAL_VARS = {};
    var GLOBAL_CTRL = {};
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
     * Установка переменной
     * @param name {string} - Имя переменной
     * @param value {string} - значение.
     */
    this.setVar = function(name, value) {
        GLOBAL_VARS[name] = value;
    }

    /**
     * Получение значений от имени переменной
     * @param name {string} - Имя переменной
     * @param defValue {string} - значение по умолчанию
     * @returns {string}
     */
    this.getVar = function(name, defValue) {
        return GLOBAL_VARS[name] || defValue;
    }

    this.setAction = function(name, obj) {
        this.GLOBAL_ACTION[name] = obj;
    }

    // setActionAuto удален - теперь в cmpAction_js

    this.setDatasetAuto = function(name) {
        this.GLOBAL_DATA_SET[name] = {"data":[]};
    }

    this.setDataset = function(name, obj) {
        this.GLOBAL_DATA_SET[name] = obj;
        Object.defineProperty(this.GLOBAL_DATA_SET[name], 'data', {
            get: function() {
                // todo: дописать обновление зависимостей, после обновления данных в датасете
            },
            set: function(value) {
                // todo: дописать обновление зависимостей, после обновления данных в датасете
            }
        });
    }

    this.getDataset = function(name) {
        return this.GLOBAL_DATA_SET[name];
    }

    this.setControlAuto = function(name, obj) {
        GLOBAL_CTRL[name] = obj;
    }

    this.getControl = function(name, obj) {
        return GLOBAL_CTRL[name];
    }

    this.setLabel = function(name, text) {
        if (GLOBAL_CTRL[name]) {
            var labelElement = GLOBAL_CTRL[name].querySelector('[block="label"]');
            if (labelElement) {
                labelElement.textContent = text;
                return true;
            }
        } else {
            var ctrlObj;
            if (typeof name === 'object') { // Если на вход подали вместо имени DOM элемент (контрола), тогда работаем с ним
                ctrlObj = name;
            } else {
                ctrlObj = document.querySelector('[name="' + name + '"]');
            }

            if (ctrlObj) {
                var ctrl = ctrlObj.querySelector('[name="' + name + '_ctrl"]');
                if (!ctrl) {
                    ctrl = this.getCtrl(name);
                }
                if (ctrl) {
                    ctrl.innerText = text;
                    return true;
                }
            }
        }
        return false;
    }

    this.getLabel = function(name) {
        if (GLOBAL_CTRL[name]) {
            var labelElement = GLOBAL_CTRL[name].querySelector('[block="label"]');
            return labelElement ? labelElement.textContent : null;
        }
        return null;
    }

    this.setLabels = function(objText) {
        for (const name in objText) {
            this.setLabel(name, objText[name]);
        }
    }

    this.getLabels = function() {
        var ctrlList = document.querySelectorAll('[schema]');
        var res = {};
        for (var i = 0; i < ctrlList.length; i++) {
            var name = ctrlList[i].getAttribute('name');
            res[name] = this.getLabel(name);
        }
        return res;
    }

    this.getValue = function(name, defValue) {
        var ctrlObj;
        if (typeof name === 'object') { // Если на вход подали вместо имени DOM элемент (контрола), тогда работаем с ним
            ctrlObj = name;
        } else {
            ctrlObj = document.querySelector('[name="' + name + '"]');
        }

        if (!ctrlObj) return defValue;

        // Получаем значение для разных типов элементов
        var tagName = ctrlObj.tagName.toLowerCase();

        if (tagName === 'input') {
            var type = ctrlObj.type.toLowerCase();
            if (type === 'checkbox') {
                return ctrlObj.checked;
            } else if (type === 'radio') {
                var radioName = ctrlObj.getAttribute('name');
                var checkedRadio = document.querySelector('input[name="' + radioName + '"]:checked');
                return checkedRadio ? checkedRadio.value : defValue;
            } else {
                return ctrlObj.value;
            }
        } else if (tagName === 'select' || tagName === 'textarea') {
            return ctrlObj.value;
        } else {
            return ctrlObj.textContent;
        }
    }

    this.setValue = function(name, value) {
        var ctrlObj = document.querySelector('[name="' + name + '"]');
        if (!ctrlObj) return;

        var tagName = ctrlObj.tagName.toLowerCase();

        if (tagName === 'input') {
            var type = ctrlObj.type.toLowerCase();
            if (type === 'checkbox') {
                ctrlObj.checked = (value === true || value === 'on' || value === 'true');
            } else if (type === 'radio') {
                var radioName = ctrlObj.getAttribute('name');
                var radios = document.querySelectorAll('input[name="' + radioName + '"]');
                for (var i = 0; i < radios.length; i++) {
                    if (radios[i].value == value) {
                        radios[i].checked = true;
                        break;
                    }
                }
            } else {
                ctrlObj.value = value;
            }
        } else if (tagName === 'select' || tagName === 'textarea') {
            ctrlObj.value = value;
        } else {
            ctrlObj.textContent = value;
        }

        // Генерируем событие change
        var changeEvent = new Event('change', { bubbles: true });
        ctrlObj.dispatchEvent(changeEvent);
    }

    this.getCtrl = function(name) {
        var ctrlElement = document.querySelector('[name="' + name + '"]');
        if (!ctrlElement) return null;

        var ctrlName = ctrlElement.getAttribute('ctrl');
        return ctrlName ? document.querySelector('[name="' + ctrlName + '"]') : null;
    }

    this.getValues = function() {
        var ctrlList = document.querySelectorAll('[schema]');
        var res = {};
        if (!ctrlList) return res;

        for (var i = 0; i < ctrlList.length; i++) {
            var name = ctrlList[i].getAttribute('name');
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
        bool = (bool == true);
        var ctrlObj = document.querySelector('[name="' + name + '"]');
        if (!ctrlObj) return;

        var ctrl = this.getCtrl(name);
        if (!ctrl) return;

        var schema = ctrlObj.getAttribute('schema');
        var type = ctrlObj.getAttribute('type');

        if (type === 'accordion' || type === 'tabs') {
            // Для компонентов easyui
            if (bool) {
                ctrl.setAttribute('disabled', 'disabled');
                ctrl.classList.add('ui-state-disabled');
            } else {
                ctrl.removeAttribute('disabled');
                ctrl.classList.remove('ui-state-disabled', 'ui-button-disabled');
            }
        } else {
            if (bool) {
                ctrl.setAttribute('disabled', 'disabled');
            } else {
                ctrl.removeAttribute('disabled');
                ctrl.classList.remove('ui-state-disabled', 'ui-button-disabled');
            }
        }
    }

    this.setDisableds = function(obj) {
        for (const name in obj) {
            this.setDisabled(name, obj[name]);
        }
    }

    this.setDisabledArr = function(arr, val) {
        for (var ind = 0; ind < arr.length; ind++) {
            var ctrlName = arr[ind].trim();
            if (ctrlName.length > 0) this.setDisabled(ctrlName, val);
        }
    }

    this.setVisible = function(name, bool) {
        bool = (bool == true);
        var ctrl = D3Api.getControl(name);
        if (ctrl) {
            ctrl.style.visibility = bool ? 'visible' : 'hidden';
        }
    }

    this.setVisibles = function(obj) {
        for (const name in obj) {
            this.setVisible(name, obj[name]);
        }
    }

    this.setStyle = function(name, propObject) {
        var ctrl = D3Api.getControl(name);
        if (!ctrl) return;

        for (var key in propObject) {
            ctrl.style[key] = propObject[key];
        }
    }

    this.move = function(name, bool) {
        var ctrlObj = document.querySelector('[name="' + name + '"]');
        if (!ctrlObj) return;

        if (bool) {
            ctrlObj.setAttribute('draggable', 'true');
            ctrlObj.style.resize = 'both';
            ctrlObj.style.overflow = 'auto';
            this.setDisabled(name, true);
        } else {
            ctrlObj.setAttribute('draggable', 'false');
            ctrlObj.style.resize = 'none';
            this.setDisabled(name, false);
        }
    }

    this.draggable = function(name, bool) {
        var ctrlObj = document.querySelector('[name="' + name + '"]');
        if (ctrlObj) {
            ctrlObj.setAttribute('draggable', bool ? 'true' : 'false');
        }
    }

    this.resizable = function(name, bool) {
        var ctrlObj = document.querySelector('[name="' + name + '"]');
        if (!ctrlObj) return;

        if (bool) {
            ctrlObj.style.resize = 'both';
            ctrlObj.style.overflow = 'auto';
            this.setDisabled(name, true);
        } else {
            ctrlObj.style.resize = 'none';
            this.setDisabled(name, false);
        }
    }

    this.msgbox = function(text, buttontext, callback) {
        buttontext = buttontext || "OK";

        var dialog = document.createElement('div');
        dialog.textContent = text;
        dialog.style.cssText = 'position:fixed; top:50%; left:50%; transform:translate(-50%,-50%); background:white; border:1px solid #ccc; padding:20px; z-index:10000; box-shadow:0 2px 10px rgba(0,0,0,0.1);';

        var button = document.createElement('button');
        button.textContent = buttontext;
        button.style.cssText = 'margin-top:15px; padding:5px 15px; background:#007bff; color:white; border:none; cursor:pointer;';

        button.addEventListener('click', function() {
            document.body.removeChild(dialog);
            if (callback) callback();
        });

        dialog.appendChild(button);
        document.body.appendChild(dialog);
    }
}

window.d3 = new D3Api.init(document.getElementsByTagName("body")[0]);

document.addEventListener("DOMContentLoaded", function() {
    // После загрузки страницы находим все тэги с атрибутами "name" "cmptype" и добавляем их в контролы
    var elementsWithNameAttribute = D3Api.D3MainContainer.querySelectorAll('[name][cmptype]');

    for (var i = 0; i < elementsWithNameAttribute.length; i++) {
        var ctrlObj = elementsWithNameAttribute[i];
        if (ctrlObj.getAttribute('cmptype')) {
            var cmptype = ctrlObj.getAttribute('cmptype').toLowerCase();
            if ((cmptype === 'action') || (cmptype === 'dataset')) continue;
        }
        var nameCtrl = ctrlObj.getAttribute('name');
        D3Api.setControlAuto(nameCtrl, ctrlObj);
    }
});

// ============== Глобальные функции ==============

function getVars() {
    return window.GLOBAL_VARS;
};

function setVars(obj) {
    for (var key in obj) {
        window.GLOBAL_VARS[key] = obj[key];
    }
};

function setVar(name, value) {
    window.GLOBAL_VARS[name] = value;
};

function getVar(name, defaultValue) {
    if (name in window.GLOBAL_VARS) {
        return window.GLOBAL_VARS[name];
    } else {
        return defaultValue;
    }
};

function logout() {
    fetch('/{component}/loginDataBase?logoff=1', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(dataObj => {
            if (!dataObj['connect']) {
                D3Api.setLabel('ctrlErrorInfo', dataObj['error']);
            }
            if ('redirect' in dataObj) {
                window.location.href = dataObj['redirect'];
            }
        })
        .catch(error => console.error('Error:', error));
}

function setSession(name, objJson) {
    fetch('/{component}/session?set_session=' + name, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(objJson)
    })
        .then(response => response.json())
        .then(dataObj => {
            console.log("Session saved:", name);
        })
        .catch(error => console.error('Error:', error));
}

function getSession(name) {
    var resObj = {};

    // Синхронный XHR для совместимости (async=false)
    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/{component}/session?get_session=' + name, false);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send('{}');

    if (xhr.status === 200) {
        try {
            resObj = JSON.parse(xhr.responseText);
        } catch (e) {
            console.error('JSON parse error:', e);
        }
    }

    return resObj;
}

function saveDirect(name) {
    if (typeof name === 'undefined') {
        name = 'local';
    }

    fetch('/{component}/sessionDirect?set_direct=' + name, {
        method: 'POST',
        body: window.location.href
    })
        .catch(error => console.error('Error:', error));
}

function loadDirect(name) {
    if (typeof name === 'undefined') {
        name = 'local';
    }

    fetch('/{component}/sessionDirect?get_direct=' + name, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: '{}'
    })
        .then(response => response.json())
        .then(dataObj => {
            if ('redirect' in dataObj) {
                window.location.href = dataObj['redirect'];
            }
        })
        .catch(error => console.error('Error:', error));
}

