window.GLOBAL_VARS = {};
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
                // todo: дописать обновление  зависимостей, после обновления данных в датасете
            },
            set: function(value) {
                // todo: дописать обновление  зависимостей, после обновления данных в датасете
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
            (GLOBAL_CTRL[name]).find('[block="label"]').text(text);
            return true;
        } else {
            var ctrlObj;
            if (typeof name === 'object') { // Если на вход подали в место имени jquery объект (контрола),тогда работаем с ним
                ctrlObj = name;
            } else {
                ctrlObj = $('[name="'+name+'"]');
            }
            var ctrl = ctrlObj.find('[name="'+name+'_ctrl"]');
            if (ctrl.length === 0) {
                ctrl = this.getCtrl(name);
            }
            if (ctrl.length >0) {
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

    this.setLabels = function(objText) {
        for (const name in obj) {
            this.getLabel(name,obj[name]);
        }
    }

    this.getLabels = function(objText) {
        var ctrlList = $('[schema]');
        var res = {};
        for (var i = 0; i < ctrlList.length; i++) {
            var name = ctrlList[i].getAttribute('name');
            res[name] = this.getLabel(name);
        }
        return res;
    }

    this.getValue = function(name, defValue) {
        var ctrlObj;
        if (typeof name === 'object') { // Если на вход подали в место имени jquery объект (контрола),тогда работаем с ним
            ctrlObj = name;
        } else {
            ctrlObj = $('[name="'+name+'"]');
        }
        return ctrlObj.val();
// todo: Дописать получение значенийи  для разных элементов дома
//        if (+ctrlObj.length === 0) {
//            return defValue;
//        }
//        var ctrl = ctrlObj.find('[name="'+name+'_ctrl"]')
//        if (ctrlObj.attr('type') === 'checkbox') {
//            return ctrl.is(':checked');
//        } else if (ctrlObj.attr('type') === 'radio') {
//            var valItemObject = ctrlObj.find('input[type="radio"]:checked');
//            if (valItemObject.length !== 0) {
//               valItemObject.attr('value');
//               return valItemObject.attr('value');
//            } else {
//               return defValue;
//            }
//        } else if (ctrlObj.attr('type') === 'accordion') {
//           return  ctrl.accordion('option', 'active' );
//        } else if (ctrlObj.attr('type') === 'dialog') {
//           return  ctrl.dialog('option', 'active' );
//        } else if (ctrlObj.attr('type') === 'tabs') {
//           return  ctrl.tabs('option', 'active');
//        } else {
//            return ctrl.val() || defValue;
//        }
    }

    this.setValue = function(name, value) {
        var ctrlObj = $('[name="'+name+'"]');
        ctrlObj.val(value);
// todo: Дописать присвоение  значенийи  для разных элементов дома
//        var ctrl = ctrlObj.find('[name="'+name+'_ctrl"]');
//        if (ctrl.length === 0) {
//            ctrl = this.getCtrl(name);
//        }
//        var val = value;
//        ctrlObj.val(val);
//        if (ctrlObj.attr('type') === 'checkbox') {
//            val = (val==='on' || val);
//            ctrl.prop('checked', val);
//        } else if (ctrlObj.attr('type') === 'radio') {
//             var ctrlItems = ctrlObj.find('[type="radio"]');
//             if (ctrlItems.length>0) {
//                 for (var i = 0; i < ctrlItems.length; i++) {
//                     var valItem = (ctrlObj.find('[type="radio"]')[i]).getAttribute('value');
//                     if (valItem == value) {
//                         (ctrlObj.find('[type="radio"]')[i]).setAttribute('checked','checked')
//                     }
//                 }
//             }
//        } else if (ctrlObj.attr('type') === 'accordion') {
//           ctrl.accordion("option", {active: false})
//           ctrl.accordion("option", {active: value});
//        } else if (ctrlObj.attr('type') === 'dialog') {
//           if (val) {
//               ctrl.dialog("open");
//           } else {
//               ctrl.dialog("close");
//           }
//        } else if (ctrlObj.attr('type') === 'tabs') {
//           ctrl.tabs("option", {active: value});
//        } else {
//            ctrl.val(val);
//        }
//        if ('trigger' in ctrl) {
//            ctrl.trigger("change");
//        }
//        var schema =  ctrlObj.attr("schema");
//        if (schema.length > 0) {
//            ctrl[schema]("refresh");
//        }
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
            this.setValue(name,obj[name]);
        }
    }


    this.setDisabled = function(name, bool) {
        bool = (bool == true || bool);
        var ctrlObj = $('[name="'+name+'"]');
        var ctrl =  this.getCtrl(name);
        var schema =  ctrlObj.attr("schema");
        if (ctrlObj.attr('type') === 'accordion') {
            bool ? ctrl.accordion( 'disable' ) : ctrl.accordion( 'enable' ) ;
        } else if (ctrlObj.attr('type') === 'tabs') {
            bool ? ctrl.tabs( 'disable' ) : ctrl.tabs( 'enable' ) ;
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
            this.setDisabled(name,obj[name]);
        }
    }

    this.setDisabledArr = function(arr,val) {
        for (const ind in arr) {
            var ctrlName = arr[ind].trim();
            if (ctrlName.length>0) this.setDisabled(ctrlName,val);
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

    this.move = function(name,bool) {
        let ctrlObj = $('[name="'+name+'"]');
        if (bool) {
            ctrlObj.draggable().draggable( 'enable' );
            ctrlObj.resizable({animate: true});
            ctrlObj.resizable( 'enable' );
            this.setDisabled(name,true);

        } else {
            ctrlObj.draggable( 'disable' );
            ctrlObj.resizable( 'disable' );
            this.setDisabled(name,false);
        }
    }

    this.draggable = function(name,bool) {
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
            this.setDisabled(name,true);
        } else {
            ctrlObj.resizable( 'disable' );
            this.setDisabled(name,false);
        }
    }
    this.msgbox = function(text, buttontext, collb) {
        // D3Api.msgbox("Нажми ок","OK")
        buttontext = buttontext || "OK"
        $( "<div>" + text + "</div>" ).dialog({
            dialogClass: "no-close",
            buttons: [
                {
                    text: buttontext,
                    click: function() {
                        $( this ).dialog( "close" );
                        $(this).remove();
                    }
                }
            ]
        });
    }
}

window.d3  = new D3Api.init(document.getElementsByTagName("body"));

document.addEventListener("DOMContentLoaded", function() {
    // После загрузки страницы находим все тэги с атрибутами "name" "cmptype" и добавляем их в контролы, для возможности модификации из D3
    const elementsWithNameAttribute = D3Api.D3MainContainer.querySelectorAll('[name][cmptype]');
    const elementsArray = Array.from(elementsWithNameAttribute);
    for (let ctrlObj of elementsArray) {
        if (ctrlObj.getAttribute('cmptype')) {
            const cmptype = ctrlObj.getAttribute('cmptype').toLocaleLowerCase();
            if ((cmptype ==='action') || (cmptype ==='dataset')) continue;
        }
        const nameCtrl = ctrlObj.getAttribute('name');
        D3Api.setControlAuto(nameCtrl,ctrlObj);
    }
});

// ============== Глобальные функции из main_old.js ==============

function getVars() {
    return window.GLOBAL_VARS;
};

function setVars(obj) {
    for (let key in obj) {
        window.GLOBAL_VARS[key] = obj[key];
    }
};

function setVar(name,value) {
    window.GLOBAL_VARS[name] = value;
};

function getVar(name,defaultValue) {
    if (name in window.GLOBAL_VARS){
        return window.GLOBAL_VARS[name];
    } else {
        return defaultValue;
    }
};

function logout() {
    $.ajax({
        url: '/{component}/loginDataBase?logoff=1',
        method: 'POST',
        dataType: 'json',
        data: null,
        success: function(dataObj) {
            if (!dataObj['connect']) {
                D3Api.setLabel('ctrlErrorInfo', dataObj['error']);
            }
            if ('redirect' in dataObj) {
                window.location.href = dataObj['redirect'];
            }
        }
    });
}

function setSession(name,objJson) {
    $.ajax({
        url: '/{component}/session?set_session='+name,
        method: 'POST',
        dataType: 'json',
        data: JSON.stringify(objJson),
        async:false,
        success: function(dataObj) {
            console.log("dataObj");
        }
    });
}

function getSession(name) {
    var resObj={};
    $.ajax({
        url: '/{component}/session?get_session='+name,
        method: 'POST',
        dataType: 'json',
        data: "{}",
        async:false,
        success: function(dataObj) {
            resObj = dataObj;
        }
    });
    return resObj;
}

function saveDirect(name) {
    if (typeof name === 'undefined') {
        name = 'local';
    }
    $.ajax({
        url: '/{component}/sessionDirect?set_direct='+name,
        method: 'POST',
        data: window.location.href,
        success: function(dataObj) {

        }
    });
}

function loadDirect(name) {
    if (typeof name === 'undefined') {
        name = 'local';
    }
    $.ajax({
        url: '/{component}/sessionDirect?get_direct='+name,
        method: 'POST',
        data: "{}",
        dataType: 'json',
        success: function(dataObj) {
            if ('redirect' in dataObj) {
                window.location.href = dataObj['redirect'];
            }
        }
    });
}


function executeAction(nameAction, callBack) {
    var ctrlObj = $('[name="'+nameAction+'"]');
    if (!ctrlObj || ctrlObj.length === 0) {
        console.error('Action not found:', nameAction);
        return;
    }

    // Получаем атрибуты
    var varsString = ctrlObj[0].getAttribute('vars');
    console.log('Raw vars string:', varsString);

    // Парсим vars - улучшенная версия
    var jsonVars = {};

    try {
        // Пробуем распарсить как JSON (с заменой кавычек)
        var fixedString = varsString
            .replace(/'/g, '"')           // заменяем одинарные кавычки на двойные
            .replace(/(\w+):/g, '"$1":')  // добавляем кавычки вокруг ключей
            .replace(/,\s*}/g, '}');       // убираем лишние запятые

        console.log('Fixed string:', fixedString);
        jsonVars = JSON.parse(fixedString);
    } catch (e) {
        console.log('JSON parse failed, trying manual parse:', e);

        // Ручной парсинг
        try {
            // Удаляем внешние фигурные скобки
            var cleanStr = varsString.trim();
            if (cleanStr.startsWith('{') && cleanStr.endsWith('}')) {
                cleanStr = cleanStr.substring(1, cleanStr.length - 1);
            }

            // Разбиваем на основные пары (учитывая вложенные объекты)
            var pairs = [];
            var depth = 0;
            var current = '';

            for (var i = 0; i < cleanStr.length; i++) {
                var c = cleanStr[i];

                if (c === '{') depth++;
                else if (c === '}') depth--;

                if (c === ',' && depth === 0) {
                    pairs.push(current);
                    current = '';
                } else {
                    current += c;
                }
            }
            if (current.trim()) {
                pairs.push(current);
            }

            // Обрабатываем каждую пару
            for (var p of pairs) {
                var colonIndex = p.indexOf(':');
                if (colonIndex === -1) continue;

                var key = p.substring(0, colonIndex).trim().replace(/['"]/g, '');
                var valueStr = p.substring(colonIndex + 1).trim();

                // Парсим значение (объект)
                if (valueStr.startsWith('{') && valueStr.endsWith('}')) {
                    var obj = {};
                    var innerStr = valueStr.substring(1, valueStr.length - 1);
                    var innerPairs = innerStr.split(',');

                    for (var inner of innerPairs) {
                        var innerColon = inner.indexOf(':');
                        if (innerColon === -1) continue;

                        var innerKey = inner.substring(0, innerColon).trim().replace(/['"]/g, '');
                        var innerValue = inner.substring(innerColon + 1).trim().replace(/['"]/g, '');
                        obj[innerKey] = innerValue;
                    }
                    jsonVars[key] = obj;
                }
            }

            console.log('Manually parsed vars:', jsonVars);
        } catch (e2) {
            console.error('Manual parse failed:', e2);
        }
    }

    var query_type = ctrlObj[0].getAttribute('query_type') || 'java';
    var action_name = ctrlObj[0].getAttribute('action_name');

    console.log('Action info:', {query_type, action_name});
    console.log('Parsed vars:', jsonVars);

    // Формируем данные для отправки
    var requestData = {};

    for (var key in jsonVars) {
        var varInfo = jsonVars[key];
        if (!varInfo) continue;

        var value = '';
        var src = varInfo.src || key;
        var srctype = varInfo.srctype || 'var';
        var defaultVal = varInfo.defaultVal || '';
        var len = varInfo.len || '';

        // Получаем значение в зависимости от типа
        if (srctype === 'var') {
            value = getVar(src) || defaultVal;
        } else if (srctype === 'ctrl') {
            value = D3Api.getValue(src) || defaultVal;
        } else if (srctype === 'session') {
            value = defaultVal;
        }

        // Создаем объект переменной
        requestData[key] = {
            'srctype': srctype,
            'src': src,
            'value': String(value),
            'defaultVal': defaultVal
        };

        if (len) {
            requestData[key].len = len;
        }
    }

    console.log('Sending request data:', requestData);
    console.log('URL:', '/{component}/cmpAction?query_type=' + query_type + '&action_name=' + action_name);

    $.ajax({
        url: '/{component}/cmpAction?query_type=' + query_type + '&action_name=' + action_name,
        method: 'POST',
        data: JSON.stringify(requestData),
        dataType: 'json',
        success: function(dataObj) {
            console.log('Response received:', dataObj);

            if (dataObj.redirect) {
                saveDirect('loginDirect');
                window.location.href = dataObj.redirect;
                return;
            }

            if (dataObj.ERROR) {
                console.error('Action error:', dataObj.ERROR);
            }

            // Обрабатываем выходные переменные
            if (dataObj.vars) {
                var data = dataObj.vars;
                for (var key in data) {
                    var varInfo = data[key];
                    if (typeof varInfo === 'object') {
                        var value = varInfo.value;
                        var srctype = varInfo.srctype || 'var';
                        var src = varInfo.src || key;

                        // Преобразуем специальные значения
                        if (value === 'null') value = null;
                        else if (value === 'true') value = true;
                        else if (value === 'false') value = false;

                        console.log('Setting output:', {key, srctype, src, value});

                        if (srctype === 'var') {
                            setVar(src, value);
                        } else if (srctype === 'ctrl') {
                            if (value === null) value = '';
                            D3Api.setValue(src, value);
                        }
                    }
                }
            }

            D3Api.setAction(nameAction, dataObj);

            if (callBack) {
                callBack(dataObj.vars || {});
            }
        },
        error: function(xhr, status, error) {
            console.error('AJAX error:', error);
            console.error('Status:', status);
            console.error('Response:', xhr.responseText);
        }
    });
}

/**
 * Функция обновления Dataset (SQL запросы преобразованные в PostgreSQL функции)
 * @param nameDataset - имя датасета из cmpDataset
 * @param callBack - функция обратного вызова
 */
function refreshDataSet(nameDataset, callBack) {
    var ctrlObj = $('[name="'+nameDataset+'"]');
    if (!ctrlObj || ctrlObj.length === 0) {
        console.error('Dataset not found:', nameDataset);
        return;
    }

    // Получаем строку с атрибутом vars
    var varsString = ctrlObj[0].getAttribute('vars');
    console.log('Raw vars string:', varsString);

    var jsonVars;

    // Специальный парсер для формата с одинарными кавычками
    function parseVarsString(str) {
        var result = {};

        // Удаляем внешние фигурные скобки
        str = str.trim();
        if (str.startsWith('{') && str.endsWith('}')) {
            str = str.substring(1, str.length - 1);
        }

        // Разбиваем на пары ключ-значение
        var pairs = [];
        var depth = 0;
        var current = '';
        var inString = false;

        for (var i = 0; i < str.length; i++) {
            var c = str[i];

            if (c === '{') depth++;
            else if (c === '}') depth--;
            else if (c === "'" && str[i-1] !== '\\') inString = !inString;

            if (c === ',' && depth === 0 && !inString) {
                pairs.push(current);
                current = '';
            } else {
                current += c;
            }
        }
        if (current.trim()) {
            pairs.push(current);
        }

        // Обрабатываем каждую пару
        for (var p of pairs) {
            var colonIndex = p.indexOf(':');
            if (colonIndex === -1) continue;

            var key = p.substring(0, colonIndex).trim().replace(/^'|'$/g, '');
            var valueStr = p.substring(colonIndex + 1).trim();

            // Парсим значение (объект)
            if (valueStr.startsWith('{') && valueStr.endsWith('}')) {
                var obj = {};
                var innerStr = valueStr.substring(1, valueStr.length - 1);
                var innerPairs = innerStr.split(',');

                for (var inner of innerPairs) {
                    var innerColon = inner.indexOf(':');
                    if (innerColon === -1) continue;

                    var innerKey = inner.substring(0, innerColon).trim().replace(/^'|'$/g, '');
                    var innerValue = inner.substring(innerColon + 1).trim().replace(/^'|'$/g, '');
                    obj[innerKey] = innerValue;
                }
                result[key] = obj;
            }
        }

        return result;
    }

    try {
        jsonVars = parseVarsString(varsString);
        console.log('Parsed vars:', jsonVars);
    } catch (e) {
        console.error('Failed to parse vars attribute:', e);
        return;
    }

    var query_type = ctrlObj[0].getAttribute('query_type');
    var db = ctrlObj[0].getAttribute('db');
    var dataset_name = ctrlObj[0].getAttribute('dataset_name');

    console.log('Dataset info:', {query_type, db, dataset_name});

    // Формируем данные для отправки
    var requestData = {};

    for (var key in jsonVars) {
        var varInfo = jsonVars[key];
        var value = '';

        // Получаем значение в зависимости от типа
        if (varInfo['srctype'] === 'var') {
            value = getVar(varInfo['src']) || varInfo['defaultVal'] || '';
        } else if (varInfo['srctype'] === 'ctrl') {
            value = D3Api.getValue(varInfo['src']) || varInfo['defaultVal'] || '';
        } else {
            value = varInfo['defaultVal'] || '';
        }

        // Создаем объект переменной
        requestData[key] = {
            'srctype': varInfo['srctype'],
            'src': varInfo['src'],
            'value': value,
            'defaultVal': varInfo['defaultVal'] || ''
        };

        if (varInfo['len']) {
            requestData[key]['len'] = varInfo['len'];
        }
    }

    console.log('Sending request data:', requestData);

    $.ajax({
        url: '/{component}/cmpDataset?query_type=' + query_type + '&dataset_name=' + dataset_name,
        method: 'POST',
        data: JSON.stringify(requestData),
        dataType: 'json',
        success: function(dataObj) {
            console.log('Response received:', dataObj);

            if (dataObj['redirect']) {
                saveDirect('loginDirect');
                window.location.href = dataObj['redirect'];
                return;
            }

            // Обрабатываем выходные переменные
            if (dataObj['vars_out']) {
                var outVars = dataObj['vars_out'];
                for (var key in outVars) {
                    var varInfo = outVars[key];
                    var value = varInfo['value'];

                    // Преобразуем специальные значения
                    if (value === 'null') value = null;
                    else if (value === 'true') value = true;
                    else if (value === 'false') value = false;

                    if (varInfo['srctype'] === 'var') {
                        setVar(varInfo['src'], value);
                    } else if (varInfo['srctype'] === 'ctrl') {
                        if (value === null) value = '';
                        D3Api.setValue(varInfo['src'], value);
                    }
                }
            }

            // Сохраняем данные в глобальном хранилище
            if (!D3Api.GLOBAL_DATA_SET[nameDataset]) {
                D3Api.setDatasetAuto(nameDataset);
            }
            D3Api.GLOBAL_DATA_SET[nameDataset].data = dataObj['data'] || [];

            console.log('Dataset data:', dataObj['data']);

            // Вызываем callback
            if (callBack) {
                callBack(dataObj['data']);
            }
        },
        error: function(xhr, status, error) {
            console.error('AJAX error:', error);
            console.error('Status:', status);
            console.error('Response:', xhr.responseText);
        }
    });
}