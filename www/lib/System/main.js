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

