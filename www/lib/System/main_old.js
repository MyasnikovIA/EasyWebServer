window.GLOBAL_VARS = {};
D3Api = new function () {
    var GLOBAL_VARS = {};
    var GLOBAL_CTRL = {};
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
        //D3Api.init = undefined;
        //delete D3Api.init;
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
        var ctrlName = $('[name="'+name+'"]').attr('ctrl');
        return  $('[name="'+ctrlName+'"]');
    }


    this.getValues = function() {
        var ctrlList = $('[schema]');
        var res = {};
        for (var i = 0; i < ctrlList.length; i++) {
            var name = ctrlList[i].getAttribute('name');
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
        var ctrl = D3Api.getControl(name);
        for (const key in propObject) {
            ctrl.css(key, propObject[key]);
        }
    }

    this.move = function(name,bool) {
        var ctrlObj = $('[name="'+name+'"]');
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
        var ctrlObj = $('[name="'+name+'"]');
        if (bool) {
            ctrlObj.draggable().draggable( 'enable' );
        } else {
            ctrlObj.draggable( 'disable' );
        }
    }

    this.resizable = function(name, bool) {
        var ctrlObj = $('[name="'+name+'"]');
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
    // После загрузки страницы находим все тэги с атрибутами "name" и добавляем их в контролы, для возможности модификации из D3
    const elementsWithNameAttribute = document.querySelectorAll('[name]');
    const elementsArray = Array.from(elementsWithNameAttribute);
    for (let ctrlObj of elementsArray) {
        if (ctrlObj.getAttribute('cmptype')) {
            const cmptype = ctrlObj.getAttribute('cmptype').toLocaleLowerCase();
            if ((cmptype ==='action') || (cmptype ==='dataset')) continue;
        }
        const nameCtrl = ctrlObj.getAttribute('name');
        D3Api.setControlAuto(nameCtrl, $('[name="'+nameCtrl+'"]'));
    }
});



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
    var jsonVars = JSON.parse(ctrlObj[0].getAttribute('vars').replaceAll("'",'"'));
    var query_type = ctrlObj[0].getAttribute('query_type');
    var db = ctrlObj[0].getAttribute('db');
    var action_name = ctrlObj[0].getAttribute('action_name');
    for (var key in jsonVars) {
        switch(jsonVars[key]['srctype']) {
          case 'var':
              jsonVars[key]['value'] = getVar(jsonVars[key]['src']);
              break;
          case 'ctrl':
              jsonVars[key]['value'] = D3Api.getValue(jsonVars[key]['src']);
              break;
          case '"session"':
              jsonVars[key]['value'] = null;
              break;
        }
    }
    $.ajax({
        url: '/{component}/cmpAction?query_type='+query_type+"&db&action_name="+action_name,
        method: 'POST',
        data: JSON.stringify(jsonVars),
        dataType: 'json',
        success: function(dataObj) {
            if ('redirect' in dataObj) {
                saveDirect("loginDirect");
                window.location.href = dataObj['redirect'];
                return;
            }
            var data = dataObj['vars'];
            for (var key in data) {
                switch(data[key]['srctype']) {
                  case 'var':
                      switch(data[key]['value']) {
                          case 'null':
                              data[key]['value'] = null;
                              break;
                          case 'true':
                              data[key]['value'] = true;
                              break;
                          case 'false':
                              data[key]['value'] = false;
                              break;
                          default:
                              break;
                      }
                      setVar(data[key]['src'], data[key]['value'])
                      break;
                  case 'ctrl':
                      switch(data[key]['value']) {
                          case 'null':
                              data[key]['value'] = '';
                              break;
                          case 'true':
                              data[key]['value'] = 1;
                              break;
                          case 'false':
                              data[key]['value'] = 0;
                              break;
                          default:
                              break;
                      }
                      D3Api.setValue(data[key]['src'],data[key]['value']);
                      break
                  case 'session':
                      data[key]['value'] = null;
                      break;
                }
            }
            D3Api.setAction(nameAction, dataObj);
            callBack && callBack(data);
        }
    });
}
function refreshDataSet(nameDataset, callBack) {
    var ctrlObj = $('[name="'+nameDataset+'"]');
    var jsonVars = JSON.parse(ctrlObj[0].getAttribute('vars').replaceAll("'",'"'));
    var query_type = ctrlObj[0].getAttribute('query_type');
    var db = ctrlObj[0].getAttribute('db');
    var dataset_name = ctrlObj[0].getAttribute('dataset_name');
    for (var key in jsonVars) {
        switch(jsonVars[key]['srctype']) {
          case 'var':
              jsonVars[key]['value'] = getVar(jsonVars[key]['src']);
              break;
          case 'ctrl':
              jsonVars[key]['value'] = D3Api.getValue(jsonVars[key]['src']);
          case '"session"':
              jsonVars[key]['value'] = null;
              break;
        }
    }
    $.ajax({
        url: '/{component}/cmpDataset?query_type='+query_type+"&db&dataset_name="+dataset_name,
        method: 'POST',
        data: JSON.stringify(jsonVars),
        dataType: 'json',
        success: function(dataObj) {
            if ('redirect' in dataObj) {
                saveDirect("loginDirect");
                window.location.href = dataObj['redirect'];
                return;
            }
            if (dataObj['vars_out']) {
                var data = dataObj['vars_out'];
                for (var key in data) {
                    switch(data[key]['srctype']) {
                      case 'var':
                          switch(data[key]['value']) {
                              case 'null':
                                  data[key]['value'] = null;
                                  break;
                              case 'true':
                                  data[key]['value'] = true;
                                  break;
                              case 'false':
                                  data[key]['value'] = false;
                                  break;
                              default:
                                  break;
                          }
                          setVar(data[key]['src'], data[key]['value'])
                          break;
                      case 'ctrl':
                          switch(data[key]['value']) {
                              case 'null':
                                  data[key]['value'] = '';
                                  break;
                              case 'true':
                                  data[key]['value'] = 1;
                                  break;
                              case 'false':
                                  data[key]['value'] = 0;
                                  break;
                              default:
                                  break;
                          }
                          D3Api.setValue(data[key]['src'],data[key]['value']);
                          break
                      case 'session':
                          data[key]['value'] = null;
                          break;
                    }
                }
            }
            // D3Api.setDataset(nameDataset, dataObj);
            callBack && callBack(dataObj['data']);
        }
    });
}