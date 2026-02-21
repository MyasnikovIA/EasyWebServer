window.GLOBAL_VARS = {};
window.GLOBAL_SESSION = {};

D3Api = new function () {
    var GLOBAL_VARS = {};
    var GLOBAL_SESSION = {};
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
     * Работа с переменными страницы (srctype="var")
     */
    this.setVar = function(name, value) {
        GLOBAL_VARS[name] = value;
        $(document).trigger('varChanged', [name, value]);
    }

    this.getVar = function(name, defValue) {
        return GLOBAL_VARS[name] !== undefined ? GLOBAL_VARS[name] : defValue;
    }

    /**
     * Работа с сессионными переменными (srctype="session")
     */
    this.setSession = function(name, value) {
        GLOBAL_SESSION[name] = value;
        $.ajax({
            url: '/{component}/session',
            method: 'POST',
            data: JSON.stringify({
                action: 'set',
                name: name,
                value: value
            }),
            contentType: 'application/json',
            success: function(response) {
                $(document).trigger('sessionSaved', [name, value]);
            }
        });
        $(document).trigger('sessionChanged', [name, value]);
    }

    this.getSession = function(name, defValue) {
        return GLOBAL_SESSION[name] !== undefined ? GLOBAL_SESSION[name] : defValue;
    }

    /**
     * Работа с подписями контролов (srctype="caption")
     */
    this.setCaption = function(name, text) {
        var ctrl = this.getControl(name);
        if (ctrl && ctrl.length > 0) {
            var captionEl = ctrl.find('[block="caption"]');
            if (captionEl.length > 0) {
                captionEl.text(text);
            } else {
                ctrl.text(text);
            }
            $(document).trigger('captionChanged', [name, text]);
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
     * Работа со значениями контролов (srctype="ctrl")
     */
    this.setValue = function(name, value) {
        var ctrlObj = $('[name="'+name+'"]');
        var oldValue = this.getValue(name);

        if (ctrlObj.length === 0) return false;

        ctrlObj.val(value);

        if (ctrlObj.attr('type') === 'checkbox') {
            ctrlObj.prop('checked', value === 'on' || value === true || value === 'true');
        } else if (ctrlObj.attr('type') === 'radio') {
            ctrlObj.filter('[value="' + value + '"]').prop('checked', true);
        }

        ctrlObj.trigger('change');
        $(document).trigger('valueChanged', [name, value, oldValue]);
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

    /**
     * Установка переменной (для обратной совместимости)
     */
    this.setVar = function(name, value) {
        GLOBAL_VARS[name] = value;
    }

    /**
     * Получение значений от имени переменной (для обратной совместимости)
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

    this.getControl = function(name) {
        return GLOBAL_CTRL[name];
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