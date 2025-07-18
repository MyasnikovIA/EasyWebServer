window.osm = (function(window, document, undefined) {
'use strict';
function Viewer(container, initialConfig) {
    var _this = this;
    var config = {};
    if (initialConfig) {
        config = initialConfig;
    } else {
        config['lon']=0;
        config['lat']=0;
        config['zoom']=0;
    }

    var fromProjection = new OpenLayers.Projection("EPSG:4326");
    var toProjection   = new OpenLayers.Projection("EPSG:900913");
    var map = null;
    var marker_layer = null;
    var marker = null;
    var markerList={};
    var wms_layer = null;
    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': false,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false
        },
        initialize: function(options) {
            this.handlerOptions = OpenLayers.Util.extend({}, this.defaultHandlerOptions);
            OpenLayers.Control.prototype.initialize.apply(
                this, arguments
            );
            this.handler = new OpenLayers.Handler.Click(
                this, {
                    'click': this.trigger
                }, this.handlerOptions
            );
        },
        trigger: function(e) {
            var lonlat = map.getLonLatFromPixel(e.xy).transform( toProjection,fromProjection);
            if (config['onClickMap']) {
                config['onClickMap'](lonlat);
            } else {
                console.log('Location click: '+lonlat.lon+','+lonlat.lat);
            }
        },
    });

    function onMakerMouseDown(evt) {
        if (config['onClickMarker']) {
            config['onClickMarker'](evt.object);
        } else {
            console.log('Marker: ',evt.object);
        }
        OpenLayers.Event.stop(evt);
    }

    var wms_layer = new OpenLayers.Layer.OSM();
    map = new OpenLayers.Map(container);
    map.addLayer(wms_layer);

    //todo: дописать механизм подгрузки точек из внешних ресурсов
    //var pois = new OpenLayers.Layer.Text( "My Points",{location: "textfile.txt"});
    //map.addLayer(pois);

    var lonLat;
    if (config['lon'] && config['lat']) {
        lonLat = new OpenLayers.LonLat(config['lon'], config['lat']).transform( fromProjection, toProjection)   ;
        var zoom=17;
        if (config['zoom']) {
            zoom=config['zoom'];
        }
        map.setCenter (lonLat, zoom);
    }

    // добавляем событие клика на карту
    var click = new OpenLayers.Control.Click();
    map.addControl(click);
    click.activate();

    marker_layer = new OpenLayers.Layer.Markers("markers");
    map.addLayers([wms_layer, marker_layer]);


    //todo: Удалить пример
    // this.addMarker(98.29712077182248,7.82793609271676, '111111111111111');

    //todo: Костыль для удаления логотипа (необходимо удалить)
    let timerId = setTimeout(function tick() {
        let ctrlDelete = document.querySelector('[class="olControlAttribution olControlNoSelect"]');
        if (!ctrlDelete) {
            timerId = setTimeout(tick, 1000);
        } else {
            ctrlDelete.remove();
            clearTimeout(timerId);
        }
    }, 1000);

    this.addMarker = function(name,x, y, data) {
        this.delMarker(name);
        marker = new OpenLayers.Marker(new OpenLayers.LonLat(x, y).transform( fromProjection, toProjection) );
        // marker.setOpacity(opacity);
        marker.events.register('mousedown', marker, onMakerMouseDown);
        marker_layer.addMarker(marker);
        marker.data=data;
        markerList[name] = marker;
    };

    this.existMarker = function(name) {
        let result = false;
        for (let markerName in markerList) {
            if (name === markerName) {
                result = true;
            };
        }
        return result;
    };

    this.getMarker = function(name) {
        let result = null;
        for (let markerName in markerList) {
            if (name === markerName) {
                marker = markerList[name];
                let indMarker = -1;
                for (let markerIndex in marker_layer.markers) {
                    if (marker == marker_layer.markers[markerIndex]) {
                        indMarker = markerIndex;
                    }
                }
                result = marker_layer.markers[indMarker];
                break;
            };
        }
        return result;
    };

    this.getMarkers = function(name) {
        return markerList;
    };

    this.getMarkerLayer = function(name) {
        return marker_layer;
    };

    this.delMarker = function(name) {
        if (markerList[name]) {
            marker = markerList[name];
            marker_layer.removeMarker(marker);
            delete markerList[name];
        }
    };

    this.moveMarkerTo = function(name, x, y) {
        if (markerList[name]) {
            marker = markerList[name];
            let indMarker = -1;
            for (let markerIndex in marker_layer.markers) {
                if (marker === marker_layer.markers[markerIndex]) {
                    indMarker = markerIndex;
                    break;
                }
            }
            if (marker_layer.markers[indMarker]['data']) {
                let Clone_object = Object.assign({}, marker_layer.markers[indMarker]['data']);
                marker_layer.removeMarker(marker);
                delete markerList[name];
                this.addMarker(name,x, y, Clone_object);
            }
        }
    };

    this.clearMarker = function() {
        for (let markerName in markerList) {
            this.delMarker(markerName);
        }
    };

    this.getCenter = function() {
        var lonLat = map.getCenter().transform(toProjection, fromProjection);
        return {lon:lonLat.lon, lat:lonLat.lat};
    };

    this.setCenter = function(lon, lat, zoom) {
        if (!zoom) zoom = 17;
        var lonLat = new OpenLayers.LonLat(lon, lat).transform( fromProjection, toProjection)   ;
        map.setCenter (lonLat, zoom);
    };

    this.onClickMap = function(functionColl) {
        config['onClickMap'] = functionColl;
    };

    this.onClickMarker = function(functionColl) {
        config['onClickMarker'] = functionColl;
    };

};


return {
    viewer: function(container, config) {
        return new Viewer(container, config);
    }
};
})(window, document);
