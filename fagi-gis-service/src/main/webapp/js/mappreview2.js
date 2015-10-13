// Attempting to be structured for once.....
var FAGI = new Object();

// Map
var map;

// WKT
var wkt;

//BBox
var box;
var transform;

//Utilities class
FAGI.Utilities = {
    
     getPropertyName: function ( property ) {
        var trunc_pos = property.lastIndexOf("#");
        var trunc = property;
        if (trunc_pos < 0)
            trunc_pos = property.lastIndexOf("/");
        if (trunc_pos >= 0)
            trunc = property.substring(trunc_pos + 1);
        
        return trunc;
    },
     
     getPropertyOntology: function ( property ) {
         var trunc_pos = property.lastIndexOf("#");
        var trunc = property;
        if (trunc_pos < 0)
            trunc_pos = property.lastIndexOf("/");
        if (trunc_pos >= 0)
            trunc = property.substring(0, trunc_pos);
        
        return trunc;
     }
     
};

var MAX_CLUSTERS = 10;
// Vector Layer Styles
var clusterColors = ['silver', 'gray', 'black', 'navy', 'maroon', 'yellow', 'olive', 'lime', 'aqua', 'teal'];

var contextB = {
    getSubject: function (feature) {
        return feature.attributes.a;
    },
    getTitle: function (feature) {
        return "Name : " + feature.attributes.a + "\nCluster : " + feature.attributes.cluster;
    },
    getOpacity: function (feature) {
        return feature.attributes.opacity + 0.5;
    },
    getColor: function (feature) {
        return 'green';
        if (feature.attributes.cluster == 'Unset') {
            return 'green';
        } else {
            return clusterColors[feature.attributes.cluster];
        }
    }      
};

var contextA = {
    getSubject: function (feature) {
        return feature.attributes.a;
    },
    getTitle: function (feature) {
        return "Name : " + feature.attributes.a + "\nCluster : " + feature.attributes.cluster;
    },
    getOpacity: function (feature) {
        return feature.attributes.opacity + 0.5;
    },
    getColor: function (feature) {
        return 'blue';
        if (feature.attributes.cluster == 'Unset') {
            return 'blue';
        } else {
            return clusterColors[feature.attributes.cluster];
        }
    }
};

var contextLink = {
    getSubject: function (feature) {
        return feature.attributes.a;
    },
    getTitle: function (feature) {
        return "Name : " + feature.attributes.a + "\nCluster : " + feature.attributes.cluster;
    },
    getOpacity: function (feature) {
        return feature.attributes.opacity;
    },
    getColor: function (feature) {
        if (!feature.validated) {
            return "darkblue";
        } else {
            if (feature.attributes.cluster == 'Unset') {
                return 'red';
            } else {
                return clusterColors[feature.attributes.cluster];
            }
        }
    },
    getLineStyle: function (feature) {
        if (!feature.validated) {
            return "longdash";
        } else {
            return "solid";
        }
    }
};

 var styleA = new OpenLayers.Style({
 strokeColor: "${getColor}",
 strokeWidth: 3,
 pointRadius: 1,
 cursor: "pointer",
 fillColor: "${getColor}",
 fillOpacity: "${getOpacity}",
 //strokeOpacity: "${getOpacity}",
 title: "${getTitle}"
 }, {context: contextA}); 
 
 var styleB = new OpenLayers.Style({
 strokeColor: "${getColor}",
 strokeWidth: 3,
 pointRadius: 1,
 cursor: "pointer",
 fillColor: "${getColor}",
 fillOpacity: "${getOpacity}",
 //strokeOpacity: "${getOpacity}",
 title: "${getTitle}"
 }, {context: contextB}); 
 
var styleLinks = new OpenLayers.Style({
    strokeColor: "${getColor}",
    cursor: "pointer",
    fillColor: "${getColor}",
    fillOpacity: "${getOpacity}",
    pointRadius: 1,
    strokeOpacity: "${getOpacity}",
    strokeWidth: 3,
    strokeDashstyle: "${getLineStyle}",
    title: '${getTitle}'
 }, {context: contextLink}); 
 
var styleBBox = {
    strokeColor: "red",
    cursor: "pointer",
    fillColor: "red",
    pointRadius: 1,
    strokeOpacity: 0.3,
    strokeWidth: 3,
    title: 'first',
    fillOpacity: 0.3
};

var styleFused = new OpenLayers.Style({
    strokeColor: "darkblue",
    strokeWidth: 3,
    pointRadius: 3,
    cursor: "pointer",
    fillColor: "darkblue",
    strokeOpacity: 0.5,
    fillOpacity: 0.5,
    title: "${getSubject}"
}, {context: contextA});

// MAp Layers
var vectorsA = new OpenLayers.Layer.Vector('Dataset A Layer',{isBaseLayer: false, styleMap: new OpenLayers.StyleMap(styleA)});
var vectorsB = new OpenLayers.Layer.Vector('Dataset B Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(styleB)});
var vectorsFused = new OpenLayers.Layer.Vector('Fused Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(styleFused)});
var vectorsLinks = new OpenLayers.Layer.Vector('Links Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(styleLinks)});
//var vectorsFused = new OpenLayers.Layer.Vector('Fused Layer',{isBaseLayer: false, style: styleFused});
//var vectorsA = new OpenLayers.Layer.Vector('Dataset A Layer', {isBaseLayer: false, style: styleA});
//var vectorsB = new OpenLayers.Layer.Vector('Dataset B Layer', {isBaseLayer: false, style: styleB});
//var vectorsLinks = new OpenLayers.Layer.Vector('Links Layer', {isBaseLayer: false, style: styleLinks});
var vectorsLinksTemp = new OpenLayers.Layer.Vector('Links Layer Temp', {isBaseLayer: false, style: styleLinks});
var bboxLayer = new OpenLayers.Layer.Vector('BBox Layer', {isBaseLayer: false, style: styleBBox});
var linksPreviewed = false;

// SPARQL Editors
var sparqlEditorA = null;
var sparqlEditorB = null;
var sparqlFetchEditorA = null;
var sparqlFetchEditorB = null;

// Map controls
var dragControlA;
var dragControlB;
var selectControl;
var multipleSelector;
var transformControl;
var drawControls;

// Projection of geometry for storing in Post-GIS
var WGS84 = new OpenLayers.Projection("EPSG:4326");

// Utilities 
function repeat(fn, times) {
    for(var i = 0; i < times; i++) fn();
}

//proj4.defs["CART"] = "";

//var cart = proj4.defs["CART"];
//var merc = proj4.defs('EPSG:4326');

// Currently enabled shifting action
var transType = 1;
var MOVE_TRANS = 1;
var ROTATE_TRANS = 2;
var SCALE_TRANS = 3;

// FAGI state
var multipleEnabled = false;
var lastPo = null;
var nowPo = null;
var cancelLink = false;

//var activeFeatureCluster = new Array();
var activeFeatureClusterA = {};
var activeFeatureClusterB = {};

// Disable caching for AJAX calls
$.ajaxSetup({cache: false});

// Helper variables
var mouse = {x: 0, y: 0};
var current_tip = {x: 0, y: 0};
var current_feature;
var feature_is_selected = false;
var previewed = false;
var mselectActive = false;
var polygonFeature;
var polygonFeatureW;
var globalOffsetAX = 0.0;
var globalOffsetAY = 0.0;
var globalOffsetBX = 0.0;
var globalOffsetBY = 0.0;
var globalOffsetVecAX = 0.0;
var globalOffsetVecAY = 0.0;
var globalOffsetVecBX = 0.0;
var globalOffsetVecBY = 0.0;

// On page load
$(document).ready(function () {
    /*map = new OpenLayers.Map('map', {
     projection: 'EPSG:3857',
     layers: [
     new OpenLayers.Layer.Google(
     "Google Physical",
     {type: google.maps.MapTypeId.TERRAIN}
     ),
     new OpenLayers.Layer.Google(
     "Google Streets", // the default
     {numZoomLevels: 20}
     ),
     new OpenLayers.Layer.Google(
     "Google Hybrid",
     {type: google.maps.MapTypeId.HYBRID, numZoomLevels: 20}
     ),
     new OpenLayers.Layer.Google(
     "Google Satellite",
     {type: google.maps.MapTypeId.SATELLITE, numZoomLevels: 22}
     )
     ],
     center: new OpenLayers.LonLat(10.2, 48.9)
     // Google.v3 uses web mercator as projection, so we have to
     // transform our coordinates
     .transform('EPSG:4326', 'EPSG:3857'),
     zoom: 5
     });
     alert('tom');
     map.addControl(new OpenLayers.Control.LayerSwitcher());
     alert('tom');
     // add behavior to html
     var animate = document.getElementById("animate");
     animate.onclick = function() {
     for (var i=map.layers.length-1; i>=0; --i) {
     map.layers[i].animationEnabled = this.checked;
     }
     };*/
    /*var map = new OpenLayers.Map('map');
     var wms = new OpenLayers.Layer.WMS( "OpenLayers WMS",
     "http://vmap0.tiles.osgeo.org/wms/vmap0", {layers: 'basic'} );
     var dm_wms = new OpenLayers.Layer.WMS(
     "Canadian Data",
     "http://www2.dmsolutions.ca/cgi-bin/mswms_gmap",
     {
     layers: "bathymetry,land_fn,park,drain_fn,drainage," +
     "prov_bound,fedlimit,rail,road,popplace",
     format: 'image/png',
     transparent: true
     },
     {singleTile: true, ratio: 1, 
     opacity: 0.5,
     isBaseLayer: false,
     visibility:true
     }
     );
     map.addLayers([wms, dm_wms]);
     map.zoomToMaxExtent();*/
    //map = new OpenLayers.Map('map');
    //alert("triying layer");

    /* NEW INTERFACE */
    hideAllPanels();
    
    //$( "#speed" ).selectmenu();
    
    
    $("#dialog").dialog({
        position: {
            my: 'left top',
            at: 'left top',
            of: $('#map')
        },
        //autoOpen: false,
        beforeClose: beforeClosePanel,
        draggable: false
    });

    //$("#slider").slider();
    $("#slider").slider({
        value: 0,
        min: 0,
        max: 10,
        step: 1,
        slide: function (event, ui) {
            $("#clusterCount").val(ui.value);
        }
    });
    $( "#clusterCount" ).val( $( "#slider" ).slider( "value" ) );
    $("#connVecDirCheck").buttonset();
    $("#connVecLenCheck").buttonset();
    $("#connCoverageCheck").buttonset();
    $("#bFusionToggle").buttonset();
    $(".ui-buttonset .ui-button-text").css("line-height", "0.5");
    
    /*
    batchOffsetDialog = $("#batch-offset-dialog").dialog({
        //autoOpen: false,
        position: "center",
        draggable: true,
        autoOpen: true,
        height: 200,
        width: 350,
        //modal: true,
        buttons: {
            "Create an account": function () {
            },
            Cancel: function () {
                batchOffsetDialog.dialog("close");
            }
        },
        close: function () {
        }
    });
    */
   
    //$( "#batch-offset-dialog" ).css("z-index", "100000");
    //batchOffsetDialog.dialog("open");
    
    /*
    $("#bFusionToggle input:radio").change( function () {
        console.log($(this).text());
        console.log($(this).val());
        console.log($(this).html());
        return true;
    });
    */
    
    $("#bFusionToggle").data("batchMode", false);
    $("#batch-on-radio").click( function () {
        $("#bFusionToggle").data("batchMode", true);
        $("#bFusionOptions").css("display", "inline");
        
        return true;
    });
    
    $("#batch-off-radio").click( function () {
        $("#bFusionToggle").data("batchMode", false);
        $("#bFusionOptions").css("display", "none");
        
        return true;
    });
    
    
    $('#typeListA').change(function () {
        //alert( $(this).find("option:selected").text() );      
        var selection = $(this).find("option:selected").text();
        var query = defaultTypeQueryA.replace('?type', '<'+selection+'>');
        sparqlEditorA.setValue(query);
    });
    
    $('#typeListB').change(function () {
        //alert( $(this).find("option:selected").text() );      
        var selection = $(this).find("option:selected").text();
        var query = defaultTypeQueryB.replace('?type', '<'+selection+'>');
        sparqlEditorB.setValue(query);
    });
    
    $("#bboxMenu").menu();
    $("#transformMenu").menu();
    $("#validateMenu").menu();

    //$("#batch-toggle-table").css("display", "none");
    
    $(".ui-menu-item").css("background-color", "darkblue");
    $(".ui-menu").css("background-color", "darkblue");
    $(".ui-menu").css("color", "white");
    $(".ui-menu").css("border-style", "none");
    $(".ui-dialog").css("z-index", "10");
    $("div#batch-offset-dialog.ui-dialog").css("z-index", "1000");
    $("#mainPanel").css("overflow-y", "auto");
    $(".ui-dialog-content").css("width", "100%");
    $(".ui-widget-content").css("width", "100%");
    $(".ui-dialog").css("float", "left");
    $(".ui-dialog").css("width", "0%");
    $(".ui-dialog").css("height", "95%");
    $(".dropdown-menu").css("background", "black");
    $(".dropdown-menu").mouseleave(function () {
        $(".dropdown").removeClass("open");
    });
    $(".dropdown").mouseleave(function () {
        $(".dropdown").removeClass("open");
    });
    $(".dropdown").mouseover(function () {
        $(".dropdown").addClass("open");
    });
    $(".dropdown-menu").mouseover(function () {
        $(".dropdown").addClass("open");
    });
    
    $("#close-findlink-menu-btn").click(function () {
        transType = MOVE_TRANS;
    
        document.getElementById("popupFindLinkMenu").style.opacity = 0.0;
        document.getElementById("popupFindLinkMenu").style.display = 'none'; 
    });
    $("#close-bbox-menu-btn").click(function () {
        transType = MOVE_TRANS;
    
        bboxLayer.destroyFeatures();
        
        document.getElementById("popupBBoxMenu").style.opacity = 0.0;
        document.getElementById("popupBBoxMenu").style.display = 'none'; 
    });
    $("#close-validate-menu-btn").click(function () {
        transType = MOVE_TRANS;
    
        document.getElementById("popupValidateMenu").style.opacity = 0.0;
        document.getElementById("popupValidateMenu").style.display = 'none'; 
    });
    $("#close-transform-menu-btn").click(function () {
        transType = MOVE_TRANS;
    
        document.getElementById("popupTransformMenu").style.opacity = 0.0;
        document.getElementById("popupTransformMenu").style.display = 'none'; 
    });

    var options = {
        numZoomLevels: 32,
        projection: "EPSG:3857",
        maxExtent: new OpenLayers.Bounds(-200000, -200000, 200000, 200000),
        center: new OpenLayers.LonLat(-12356463.476333, 5621521.4854095)
    };
    //map = new OpenLayers.Map("map", options);
    var map_controls = [new OpenLayers.Control.OverviewMap(), new OpenLayers.Control.LayerSwitcher()];
    map = new OpenLayers.Map("map", {
        transitionEffect: null,
        zoomMethod: null,
        projection: new OpenLayers.Projection("EPSG:900913")
        /*eventListeners: {
        featureover: function(e) {
            e.feature.renderIntent = "select";
            e.feature.layer.drawFeature(e.feature);
            console.log("Map says: Pointer entered " + e.feature.id + " on " + e.feature.layer.name);
        },
        featureout: function(e) {
            e.feature.renderIntent = "default";
            e.feature.layer.drawFeature(e.feature);
            console.log("Map says: Pointer left " + e.feature.id + " on " + e.feature.layer.name);
        },
        featureclick: function(e) {
            console.console.log("Map says: " + e.feature.id + " clicked on " + e.feature.layer.name);
        }
    }*/
    });


    var streetLayer = new OpenLayers.Layer.Google(
            "Google Streets", // the default
            {'sphericalMercator': true,
                'numZoomLevels': 32,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });
    var myBaseLayer = new OpenLayers.Layer.Google("Google Satellite",
            {'sphericalMercator': true,
                'numZoomLevels': 32,
                'type': google.maps.MapTypeId.SATELLITE,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });
    /*map.addLayer(myBaseLayer);*/
//alert("tried layer");

    map.addControl(new OpenLayers.Control.LayerSwitcher());
    var inside_panel = new OpenLayers.Control.Panel({
        displayClass: 'insidePanel'
    });
    map.addControl(inside_panel);
    var zoom_max_inside = new OpenLayers.Control.ZoomToMaxExtent({
        displayClass: 'myZoomToMaxInside'
    });

// Get control of the right-click event:
    document.getElementById('map').oncontextmenu = function (e) {
        e = e ? e : window.event;
        if (e.preventDefault)
            e.preventDefault(); // For non-IE browsers.
        else
            return false; // For IE browsers.
    };

    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': true,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false
        },
        handleRightClicks: true,
        initialize: function (options) {
            this.handlerOptions = OpenLayers.Util.extend(
                    {}, this.defaultHandlerOptions
                    );
            OpenLayers.Control.prototype.initialize.apply(
                    this, arguments
                    );
            this.handler = new OpenLayers.Handler.Click(
                    this, this.eventMethods, this.handlerOptions
                    );
        },
        CLASS_NAME: "OpenLayers.Control.Click"

    });

    wkt = new OpenLayers.Format.WKT();
    //Proj4js.reportError = function(msg) {alert(msg);};
// Add an instance of the Click control that listens to various click events:
    var oClick = new OpenLayers.Control.Click({eventMethods: {
            'rightclick': function (e) {
                if ( lastPo != null ) {
                    vectorsLinksTemp.destroyFeatures();
                    lastPo = null;
                    nowPo = null;

                    document.getElementById("popupTransformMenu").style.opacity = 0;
                    document.getElementById("popupTransformMenu").style.display = 'none';

                    prevActiveFeature = null;
                    activeFeature = null;

                    //return;
                }
            }
        }});
    map.addControl(oClick);
    oClick.activate();

    inside_panel.addControls([zoom_max_inside]);
    var myBaseLayer2 = new OpenLayers.Layer.Google("Google Streets",
            {'sphericalMercator': true,
                'units': 'km',
                'numZoomLevels': 32,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });
    var OSMLayer = new OpenLayers.Layer.OSM("OSM","",{isBaseLayer:true, zoomOffset:0, 'numZoomLevels': 40}); 
    //var OSMLayer = new OpenLayers.Layer.OSM("OSM", { 'numZoomLevel': 32 });
    map.addLayer(OSMLayer);
    map.addLayer(myBaseLayer2);
    map.addLayer(myBaseLayer);


    box = new OpenLayers.Control.DrawFeature(bboxLayer, OpenLayers.Handler.RegularPolygon, {
        handlerOptions: {
            sides: 4,
            snapAngle: 90,
            irregular: true,
            persist: true
        }
    });
    box.handler.callbacks.done = endDragBox;
    map.addControl(box);
    //box.activate();
    map.addLayer(bboxLayer);

    transformControl = new OpenLayers.Control.TransformFeature(vectorsB, {
        rotate: true,
        irregular: true
    });
    transformControl.events.register("transformcomplete", transformControl, transDone);
    map.addControl(transformControl);
    
    transform = new OpenLayers.Control.TransformFeature(bboxLayer, {
        rotate: true,
        irregular: true
    });
    transform.events.register("transformcomplete", transform, boxResize);
    map.addControl(transform);

    map.events.register("mousemove", map, function (e) {
        mouse.x = e.pageX;
        mouse.y = e.pageY;
        if (lastPo != null ) {
            var curPo = new OpenLayers.Geometry.Point();
            e.xy.y+=3;
            e.xy.x+=3;
            curPo.x = map.getLonLatFromPixel(e.xy).lon;
            curPo.y = map.getLonLatFromPixel(e.xy).lat;
            
            var line2 = new OpenLayers.Geometry.LineString([lastPo, curPo]);
                //alert('Length ' + line2.getLength());
                //alert('Length Geo ' + line2.getGeodesicLength(map.getProjectionObject()));
                //alert('Point dist ' + po1.distanceTo(po2));
                //alert(line2);
                linkFeature = new OpenLayers.Feature.Vector(line2);
                //alert('Lon Lat');
                linkFeature.attributes = {'a': 'aalala'};
                linkFeature.style = {
                    strokeColor: "red",
                    cursor: "pointer",
                    fillColor: "red",
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    fillOpacity: 0.5};
                vectorsLinksTemp.destroyFeatures();
                vectorsLinksTemp.addFeatures([linkFeature]);
                vectorsLinksTemp.drawFeature(linkFeature);
        }
        //alert(mouse.x+' '+mouse.y);
    });
    map.events.register("mouseover", map, function (e) {
        //alert("mousein");
    });
    map.events.on({"zoomend": function () {
            //alert("mouseroll");
        }});
    map.zoomToProxy = map.zoomTo;
    map.zoomTo = function (zoom, xy) {
        // if you want zoom to go through call
        //alert(zoom);
        //alert(xy)
        ;       // 
        if (transType == MOVE_TRANS) {
            map.zoomToProxy(zoom, xy);
        }
        //else do nothing and map wont zoom
    };
    
     OpenLayers.Control.MouseWheel = OpenLayers.Class(OpenLayers.Control, {                
     defaultHandlerOptions: {
     'cumulative': true
     },
     
     initialize: function(options) {
     this.handlerOptions = OpenLayers.Util.extend(
     {}, this.defaultHandlerOptions
     );
     OpenLayers.Control.prototype.initialize.apply(
     this, arguments
     ); 
     this.handler = new OpenLayers.Handler.MouseWheel(
     this,
     {'zoomstart': this.onPause},
     this.handlerOptions
     );
     }, 
     
     onPause: function(evt) {
     //alert(evt);
     }
     });
     
     var nav = new OpenLayers.Control.Navigation({'zoomWheelEnabled': true});
     nav.wheelChange = luda;
     map.addControl(nav);
     
     var controlers = {
     "cumulative": new OpenLayers.Control.MouseWheel({
     handlerOptions: {
     "cumulative": true
     }
     })
     };
     
     var control;
     for(var key in controlers) {
     control = controlers[key];
     // only to route output here
     control.key = key;
     //alert(key);
     map.addControl(control);
     }
     control.activate();
     var wms3 = new OpenLayers.Layer.WMS( "OpenLayers WMS", {isBaseLayer: true},
     "http://vmap0.tiles.osgeo.org/wms/vmap0", {layers: 'basic'} );
     //map.addLayer(wms3);
     //alert('tom');
     
     /*map.addLayer(new OpenLayers.Layer.OSM("Open Street Map (OSM)",
     {'sphericalMercator': true,
     'numZoomLevels': 32,
     'maxExtent': new OpenLayers.Bounds(-20037508.34,-20037508.34,20037508.34,20037508.34)
     }));*/
    //create a style object

    // allow testing of specific renderers via "?renderer=Canvas", etc
    var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
    renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;

    vectors = new OpenLayers.Layer.Vector("Vector Layer", {isBaseLayer: false,
        renderers: renderer
    });


    map.addControl(new OpenLayers.Control.LayerSwitcher());
    map.addControl(new OpenLayers.Control.MousePosition());

    dragControlA = new OpenLayers.Control.DragFeature(vectorsA, {
        onEnter: onFeatureOver,
        onStart: startDragA,
        onDrag: doDragA,
        onComplete: endDragA
    });
    dragControlB = new OpenLayers.Control.DragFeature(vectorsB, {
        onEnter: onFeatureOver,
        onStart: startDragB,
        onDrag: doDragB,
        onComplete: endDragB
    });
    //dragControlA.moveFeature = onFeatureOver2;
    map.addControls([dragControlA, dragControlB]);

    dragControlA.activate();
    dragControlB.activate();
    map.setCenter(new OpenLayers.LonLat(0, 0), 3);
    var select = new OpenLayers.Control.SelectFeature(vectors, {
        onSelect: addSelected,
        onUnselect: clearSelected
    });
    //map.addControl(select);
    //select.activate();
    map.addLayer(vectorsA);
    map.addLayer(vectorsB);
    map.addLayer(vectorsLinks);
    map.addLayer(vectorsLinksTemp);
    map.addLayer(bboxLayer);
    map.addLayer(vectorsFused);

    var lays = [vectorsLinks];
    multipleSelector = new OpenLayers.Control.SelectFeature(lays, {
        box: true,
        multiple: true,
        //onSelect : function() { alert("select mul"); },
        //onUnselect : function() { alert("unselect mul"); }
    });
    map.addControl(multipleSelector);

    drawControls = {
        line: new OpenLayers.Control.DrawFeature(vectorsLinks,
                OpenLayers.Handler.Path)
    };

    for (var key in drawControls) {
        map.addControl(drawControls[key]);
    }
        
    //multipleSelector.activate();
    //map.addLayer(vectors);
    //map.setLayerIndex(vectors,1);
    map.setLayerIndex(vectorsA, 2);
    map.setLayerIndex(vectorsB, 3);
    map.setLayerIndex(vectorsLinks, 4);
    map.setLayerIndex(vectorsLinksTemp, 5);
    map.setLayerIndex(vectorsFused, 6);
    map.setLayerIndex(bboxLayer, 7);

    var laysAll = [vectorsA, vectorsB, vectorsFused, vectorsLinks, bboxLayer];
    selectControl = new OpenLayers.Control.SelectFeature(laysAll, {
        highlightOnly: true,
        toggle: true,
        hover: false});
    map.addControl(selectControl);

    selectControl.activate();
    //alert(map.events.BROWSER_EVENTS);
    vectorsA.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        'featureclick': function(e){alert('thee');},
        scope: vectorsA
    });
    vectorsB.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        'featureclick': function(e){alert('thee');},
        scope: vectorsB
    });
    vectorsLinks.events.on({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: vectorsLinks
    });
    vectorsFused.events.on({
        'featureselected': onFusedSelect,
        'featureunselected': onFusedUnselect,
        scope: vectorsLinks
    });
    bboxLayer.events.on({
        'featureselected': onBBoxSelect,
        'featureunselected': onBBoxUnselect,
        scope: bboxLayer
    });
    //var polygonFeature = wkt.read("POLYGON(15.37412 51.32847,15.374159 51.328592,15.374441 51.328552,15.374586 51.328532,15.374659 51.328521,15.37462 51.328399,15.37412 51.32847)");
    polygonFeature = wkt.read("POLYGON((20 37, 20 39, 22 39, 22 37, 20 37))");
    //polygonFeatureT1 = wkt.read("POLYGON((-74.0085826803402 40.7421376304449,-74.0084536803402 40.7420846304449,-74.0086146803402 40.7418666304449,-74.0086036803402 40.7418596304449,-74.0086136803402 40.7418486304449,-74.0086386803402 40.7418596304449,-74.0085826803402 40.7421376304449))");
    polygonFeatureT1 = wkt.read("POLYGON((-74.008619 40.7421519999998,-74.00849 40.7420989999998,-74.008651 40.7418809999998,-74.00864 40.7418739999998,-74.00865 40.7418629999998,-74.008675 40.7418739999998,-74.008619 40.7421519999998))");
    polygonFeatureT = wkt.read("LINESTRING(-74.008617 40.741867,-74.008629 40.741872,-74.008581 40.742146,-74.008452 40.742091,-74.008601 40.741889,-74.008617 40.741867)");
    //polygonFeatureT = wkt.read("LINESTRING(-74.0087622786393 40.7419244782195,-74.0087742786393 40.7419294782196,-74.0087262786393 40.7422034782196,-74.0085972786393 40.7421484782195,-74.0087462786393 40.7419464782195,-74.0087622786393 40.7419244782195)");
    
    polygonFeature.attributes.a = "asasa";
    //polygonFeature.style = styleA;
    polygonFeatureW = wkt.read("POLYGON((24 41, 24 43, 26 43, 26 41, 24 41))");
    //polygonFeatureW = wkt.read("POLYGON((20.000001 37.000001, 20.000001 39.000001, 22.000001 39.000001, 22.000001 37.000001, 20.000001 37.000001))");
    //var polygonFeature = wkt.read("POINT(-25.8203125 2.4609375)");
    polygonFeature.geometry.transform(WGS84, map.getProjectionObject());
    polygonFeatureT.geometry.transform(WGS84, map.getProjectionObject());
    polygonFeatureT1.geometry.transform(WGS84, map.getProjectionObject());
    //vectorsFused.addFeatures([polygonFeature]);
    //alert("OL");
    var polygonFeature2 = wkt.read("POLYGON((20.7240456428877 37.9908366236946,20.7241422428877 37.9906675236946,20.7238686428877 37.9905829236946,20.7238364428877 37.9908197236946,20.7240456428877 37.9908366236946))");
    polygonFeatureW.geometry.transform(WGS84, map.getProjectionObject());

    var start_point = polygonFeature.geometry.getCentroid(true);
    var end_point = polygonFeatureW.geometry.getCentroid(true);
    var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
    linkFeature.attributes = {'la': polygonFeature, 'a': 'ludacris', 'lb': polygonFeatureW, 'cluster': 'Unset', 'opacity': 0.8};
    var links = [];
    //links.push(linkFeature);
    polygonFeature.attributes = {'a': 'tomaras', 'links': links, 'cluster': 'Unset', 'opacity': 0.3};
    polygonFeatureW.attributes = {'a': 'tomaras2', 'links': links, 'cluster': 'Unset', 'opacity': 0.3};
    //vectorsLinks.addFeatures([linkFeature]);
    //window.setInterval(function() {rotateFeature(polygonFeature, 360 / 20, polygonFeature.geometry.getCentroid(true));}, 100);
    //vectorsB.addFeatures([polygonFeatureW]);
    //vectorsA.addFeatures([polygonFeature, polygonFeatureT, polygonFeatureT1]);
    map.zoomToMaxExtent();
    //map.zoomToExtent(vectorsA.getDataExtent())
    map.updateSize();
    map.render('map');
    
    map.events.register("click", map, function (e) {
        mselectActive = false;
        
        //document.getElementById("fg-debug-popup").style.opacity = 0.8;
            //document.getElementById("fg-debug-popup").style.display = 'inline';
            //document.getElementById("fg-debug-popup").style.top = mouse.y;
        //document.getElementById("fg-debug-popup").style.left = mouse.x; 
        
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';

        document.getElementById("popupFindLinkMenu").style.opacity = 0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';
        
        document.getElementById("popupValidateMenu").style.opacity = 0;
        document.getElementById("popupValidateMenu").style.display = 'none';
        
        document.getElementById("popupFindLinkMenu").style.opacity = 0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';
        
        //activeFeatureCluster = new Array();
       return true;
    });
});

function newPolygonAdded(a, b) {
    alert("clicked");
}

function closeAllPanels(activePanel) {
    $("#connectionPanel").currentlyOpened = false;
    $("#datasetPanel").currentlyOpened = false;
    $("#fusionPanel").currentlyOpened = false;
    $("#fusionPanel").currentlyOpened = false;
    $("#linksPanel").currentlyOpened = false;

    $(activePanel).currentlyOpened = false;
}

var spinnerEnabled = true;
function enableSpinner() {
    if (!spinnerEnabled) {
        $("#fg-screen-dimmer").show();
        $("#fg-loading-spinner").show();
        
        spinnerEnabled = true;
    }
}

function disableSpinner() {
    if (spinnerEnabled) {
        $("#fg-screen-dimmer").hide();
        $("#fg-loading-spinner").hide();
        
        spinnerEnabled = false;
    }
}

function enableBBoxTransform() {
    alert('transform');
}

var activeBBox;
function fetchContainedAndLink() {
    //alert("Bacth Find Links");
    var feature = new OpenLayers.Feature.Vector(activeBBox.toGeometry());
    //alert(feature.geometry);
    feature.geometry.transform(map.getProjectionObject(), WGS84);
    //alert(feature.geometry.getBounds());
    var bboxJSON = new Object();
    bboxJSON.barea = wkt.write(feature);
    bboxJSON.left = feature.geometry.getBounds().left;
    bboxJSON.bottom = feature.geometry.getBounds().bottom;
    bboxJSON.right = feature.geometry.getBounds().right;
    bboxJSON.top = feature.geometry.getBounds().top;
    //alert('Bounding Area Poly '+bboxJSON.barea);
    //JSON.stringify(bboxJSON);
    feature.geometry.transform(WGS84, map.getProjectionObject());

    document.getElementById("popupBBoxMenu").style.opacity = 0;
    document.getElementById("popupBBoxMenu").style.display = 'none';
    enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "BatchFindLinkServlet",
        // the data to send (will be converted to a query string)
        data: {bboxJSON: JSON.stringify(bboxJSON)},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            //alert(responseJson);
            //bboxLayer.destroyFeatures();
            //addUnlinkedMapDataJson(responseJson);
            bboxLayer.destroyFeatures();
            addUnlinkedMapDataJsonWithLinks(responseJson);
            disableSpinner();
            //$('#popupBBoxMenu').hide();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            disableSpinner();
            alert("Sorry, there was a problem!");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });
}

function fetchContained() {
    var feature = new OpenLayers.Feature.Vector(activeBBox.toGeometry());
    //alert(feature.geometry);
    feature.geometry.transform(map.getProjectionObject(), WGS84);
    //alert(feature.geometry.getBounds());
    var bboxJSON = new Object();
    bboxJSON.barea = wkt.write(feature);
    bboxJSON.left = feature.geometry.getBounds().left;
    bboxJSON.bottom = feature.geometry.getBounds().bottom;
    bboxJSON.right = feature.geometry.getBounds().right;
    bboxJSON.top = feature.geometry.getBounds().top;
    //alert('Bounding Area Poly '+bboxJSON.barea);
    //JSON.stringify(bboxJSON);
    feature.geometry.transform(WGS84, map.getProjectionObject());

    var qASend = "";
    if ( sparqlFetchEditorA != null ) qASend = sparqlFetchEditorA.getValue();
    var qBSend = "";
    if ( sparqlFetchEditorB != null ) qBSend = sparqlFetchEditorB.getValue();
    
    enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FetchUnlinkedServlet",
        // the data to send (will be converted to a query string)
        data: {bboxJSON: JSON.stringify(bboxJSON), queryA: qASend, queryB: qBSend},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            //alert(responseJson);
            bboxLayer.destroyFeatures();
            addUnlinkedMapDataJson(responseJson);
            
            document.getElementById("popupBBoxMenu").style.opacity = 0;
            document.getElementById("popupBBoxMenu").style.display = 'none';
            
            disableSpinner();
            //$('#popupBBoxMenu').hide();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            disableSpinner();
            alert("Sorry, there was a problem!");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });
}

function endDragBox(bbox) {
    var bounds = bbox.getBounds();
    activeBBox = bounds;
    //setBounds(bounds);
    drawBox(bounds);
    box.deactivate();
}

function boxResize(event) {
    console.log(event.feature.geometry.bounds);
}

function boxResize(event) {
    alert(event.feature.geometry.bounds);
}

function onBBoxSelect(e) {
    //alert('bbox select');
    document.getElementById("popupBBoxMenu").style.opacity = 0.7;
    //feature_is_selected = false;
    document.getElementById("popupBBoxMenu").style.display = 'inline';
    document.getElementById("popupBBoxMenu").style.top = mouse.y;
    document.getElementById("popupBBoxMenu").style.left = mouse.x;
}

function onBBoxUnselect(event) {
    alert('bbox unselect');
}

function transDone() {
    alert(event.feature.geometry.bounds);
}

function drawBox(bounds) {
    var feature = new OpenLayers.Feature.Vector(bounds.toGeometry());

    bboxLayer.addFeatures(feature);
    //transform.setFeature(feature);
}

function clearSelected(event) {
    if (multipleEnabled) {
        //alert('multiple end');
        //setSingleMapControls();
        multipleEnabled = false;
        multipleSelector.unselectAll();
        
        multipleSelector.deactivate();
        selectControl.activate();
        
        // register multiple feature callbacks
        vectorsLinks.events.un({
            'featureselected': addSelected,
            'featureunselected': clearSelected,
            scope: vectorsLinks
        });
    
        vectorsLinks.events.on({
            'featureselected': onLinkFeatureSelect,
            'featureunselected': onLinkFeatureUnselect,
            scope: vectorsLinks
        });
    
        //alert(JSON.stringify(activeFeatureClusterA));
        //if ( activeFeatureCluster.length === 0 ) {
            
        //}
        
        //activeFeatureCluster = new Array();
    }
}

function addSelected(event) {
    if ( typeof activeFeatureClusterA[event.feature.attributes.la.attributes.a] != "undefined" )
        return;
    
    if ( typeof activeFeatureClusterB[event.feature.attributes.lb.attributes.a] != "undefined" )
        return;
        
    //var clusterLink = new Object();
    //clusterLink.nodeA = event.feature.attributes.la.attributes.a;
    //clusterLink.nodeB = event.feature.attributes.lb.attributes.a;
    //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
    console.log($('#fg-info-popup').width());
    console.log($('#map').width());
    
    $("#fg-info-label").html($("#fg-info-label").html() + '  ' + event.feature.attributes.la.attributes.a);
    document.getElementById("fg-info-popup").style.top = $('#map').height() - $('#map').height() * 0.95;
    document.getElementById("fg-info-popup").style.left = $('#map').width() - ($('#fg-info-popup').width() + 10);
    //$("#fg-info-popup").width($('#map').width() - ($('#fg-info-popup').width()));
    
    console.log($('#fg-info-popup').width());
    console.log($('#map').width());
    console.log(event.feature.attributes.la.attributes.a);
    activeFeatureClusterA[event.feature.attributes.la.attributes.a] = event.feature;
    activeFeatureClusterB[event.feature.attributes.lb.attributes.a] = event.feature;
}

function setMultipleMapControls() {
    // unregister single feature callbacks
    vectorsA.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsA
    });
    vectorsB.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsB
    });
    vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: vectorsLinks
    });

    // register multiple feature callbacks
    vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: vectorsLinks
    });
}

function setSingleMapControls() {
    // unregister multiple feature callbacks
    vectorsLinks.events.un({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: vectorsLinks
    });

    // register multiple feature callbacks
    vectorsA.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsA
    });
    vectorsB.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsB
    });
    vectorsLinks.events.on({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: vectorsLinks
    });
}

function activateMultipleTool() {
    //alert("multiple");
    //activeFeatureCluster = new Array();
    //alert($('#clusterSelector option[value="9999"]').length);
    
    if ( !$('#clusterSelector option[value="9999"]').length )
        $("#clusterSelector").append("<option value=\""+9999+"\" >Custom Cluster </option>");
    
    multipleEnabled = true;
    multipleSelector.activate();
    selectControl.deactivate();
    
    vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: vectorsLinks
    });
   
    // register multiple feature callbacks
    vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: vectorsLinks
    });
}

function activateBBoxTool() {
    //alert("multiple");
    //activeFeatureCluster = new Array();
    //alert($('#clusterSelector option[value="9999"]').length);
    
    if ( !$('#clusterSelector option[value="9999"]').length )
        $("#clusterSelector").append("<option value=\""+9999+"\" >Custom Cluster </option>");
    
    multipleEnabled = true;
    multipleSelector.activate();
    selectControl.deactivate();
    
    vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: vectorsLinks
    });
    
    /*
    vectorsA.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsA
    });
    vectorsB.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: vectorsB
    });
    */
   
    // register multiple feature callbacks
    vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: vectorsLinks
    });
    
    /*
    vectorsA.events.on({
        'featureselected': addSelectedFromA,
        'featureunselected': clearSelectedFromA,
        scope: vectorsA
    });
    vectorsB.events.on({
        'featureselected': addSelectedFromB,
        'featureunselected': clearSelectedFromB,
        scope: vectorsB
    });
    */
}

function fetchSPARQLContained() {
    fetchContained();
    /*
    var bbox = $("#fg-fetch-queries-submit").prop("bbox");
    $.ajax({
        url: 'FetchUnlinkedServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseText) {
            console.log(JSON.stringify(responseText));
            $("#linksList").html(responseText.links);
        },
        error: function (responseText) {
            alert("All bad " + responseText);
            alert("Error");
        },
        data: {'queryA': sparqlEditorA.getValue(), 'queryB': sparqlEditorB.getValue()}
        //Options to tell jQuery not to process data or worry about content-type.
    });
    */
}

function linksSPARQLFilter() {
    //console.log(sparqlEditorA.getValue());
    //console.log(sparqlEditorB.getValue());
    enableSpinner();
    $.ajax({
        url: 'SPARQLFilterServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseText) {
            disableSpinner();
            console.log(JSON.stringify(responseText));
            $("#linksList").html(responseText.links);
        },
        error: function (responseText) {
            disableSpinner();
            alert("All bad " + responseText);
            alert("Error");
        },
        data: {'queryA': sparqlEditorA.getValue(), 'queryB': sparqlEditorB.getValue()}
        //Options to tell jQuery not to process data or worry about content-type.
    });
        
}

function activateFecthUnlinked() {
    box.activate();
}

function activateMultipleSelect() {
    //multipleSelector.activate();
    //setMultipleMapControls();
    box.activate();
    //transform.activate();
}

function hideAllPanels( ) {
    $("#connectionPanel").hide();
    $("#datasetPanel").hide();
    $("#fusionPanel").hide();
    $('#matchingPanel').hide();
    $("#fusionPanel").hide();
    $("#linksPanel").hide();
    $("#clusteringPanel").hide();
    $("#fg-fetch-sparql-panel").hide();
    $("#previewPanel").hide();
}

var defaultBBoxQueryStartA = 'SELECT ?subject ?geometry WHERE {\n\
    ?subject \n\
    ?predicate\n\
    ?object .\n\
    ?subject \n\
    <http://geovocab.org/geometry#geometry>\n\
    ?geolink .\n\
    ?geolink \n\
    <http://www.opengis.net/ont/geosparql#asWKT>\n\
    ?geometry \n\
\n\
    FILTER ( <fagi-gis:contained_in> ( ';
var defaultBBoxQueryEndA = '))\n\
}';

var defaultBBoxQueryStartB = 'SELECT ?subject ?geometry WHERE {\n\
    ?subject \n\
    ?predicate\n\
    ?object .\n\
    ?subject \n\
    <http://geovocab.org/geometry#geometry>\n\
    ?geolink .\n\
    ?geolink \n\
    <http://www.opengis.net/ont/geosparql#asWKT>\n\
    ?geometry \n\
\n\
    FILTER ( <fagi-gis:contained_in> ( ';
var defaultBBoxQueryEndB = '))\n\
}';

var defaultTypeQueryA = 'SELECT ?subjectA ?subjectB WHERE {\n\
    GRAPH <fagi-gis:links> {\n\
        ?subjectA \n\
        <http://www.w3.org/2002/07/owl#sameAs>\n\
        ?subjectB \n\
    } .\n\
    GRAPH <fagi-gis:metadata> {\n\
        ?subjectA \n\
        <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>\n\
        ?type\n\
    }\n\
}';

var defaultTypeQueryB = 'SELECT ?subjectA ?subjectB WHERE {\n\
    GRAPH <fagi-gis:links> {\n\
        ?subjectA \n\
        <http://www.w3.org/2002/07/owl#sameAs>\n\
        ?subjectB \n\
    } .\n\
    GRAPH <fagi-gis:metadata> {\n\
        ?subjectA \n\
        <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>\n\
        ?type\n\
    }\n\
}';

var defaultQuery = 'SELECT ?subjectA ?subjectB WHERE { \n\
    GRAPH <fagi-gis:links> { \n\
        ?subjectA \n\
        <http://www.w3.org/2002/07/owl#sameAs> \n\
        ?subjectB \n\
    }\n\
}'
;
var activeQueryA = defaultQuery;
var activeQueryB = defaultQuery;
var activeBBoxQueryA = defaultQuery;
var activeBBoxQueryB = defaultQuery;

var DURATION = 200;
function animatePanel(strech, opened) {
    var int = self.setInterval(periodicUpdate, 1);
    //alert(strech);
    if (!opened) {
        $("#map").animate({
            width: (100 - strech) + '%'
        }, {duration: DURATION, queue: false, complete: function () {
                map.updateSize();
                window.clearInterval(int);
                return false;
            }});
        $(".ui-dialog").animate({
            width: strech + '%'
        }, {duration: DURATION, queue: false, complete: function () {
                $("#fg-links-sparql-editor-a").html("");
                $("#fg-links-sparql-editor-b").html("");
                if ($(lastClickedMenu).is($("#linksPanel"))) {
                    sparqlEditorA = CodeMirror($("#fg-links-sparql-editor-a").get(0), {
                        value: activeQueryA,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    sparqlEditorB = CodeMirror($("#fg-links-sparql-editor-b").get(0), {
                        value: activeQueryB,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    $(".CodeMirror-vscrollbar").css("overflow-y","hidden");
                }
                if ($(lastClickedMenu).is($("#fg-fetch-sparql-panel"))) {
                    var feature = new OpenLayers.Feature.Vector(activeBBox.toGeometry());
                    feature.geometry.transform(map.getProjectionObject(), WGS84);
                    var wktRep = wkt.write(feature);
                    $("#fg-fetch-queries-submit").prop("bbox", wktRep);
                    sparqlFetchEditorA = CodeMirror($("#fg-fetch-sparql-editor-a").get(0), {
                        value: defaultBBoxQueryStartA + wktRep + defaultBBoxQueryEndA,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    sparqlFetchEditorB = CodeMirror($("#fg-fetch-sparql-editor-b").get(0), {
                        value: defaultBBoxQueryStartB + wktRep + defaultBBoxQueryEndB,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    $(".CodeMirror-vscrollbar").css("overflow-y","hidden");
                }
                
                return false;
            }});
    } else {
        $("#map").animate({
            width: '100%'
        }, {duration: DURATION, queue: false, complete: function () {
                map.updateSize();
                window.clearInterval(int);
                return false;
            }});
        $(".ui-dialog").animate({
            width: '0%'
        }, {duration: DURATION, queue: false, complete: function () {
                sparqlFetchEditorA = null;
                sparqlFetchEditorB = null;
                
                return false;
            }});
    }
}

var dialogOpened = false;
var lastClickedMenu = null;
function expandConnectionPanel() {
    hideAllPanels();
    //alert(lastClickedMenu);
    //alert($(lastClickedMenu).is($("#connectionPanel")));
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#connectionPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#connectionPanel");

    $('#dialog').dialog('option', 'title', 'Connection');
    $("#connectionPanel").show();
    //alert($("#connectionPanel").data("opened"));
    if ($("#connectionPanel").data("opened")) {
        animatePanel(0, $("#connectionPanel").data("opened"));
        $("#connectionPanel").data("opened", false);
    } else {
        animatePanel(33, $("#connectionPanel").data("opened"));
        $("#connectionPanel").data("opened", true);
    }
}

function expandMatchingPanel() {
    hideAllPanels();
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#matchingPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#matchingPanel");
    $('#dialog').dialog('option', 'title', 'Property Matching');
    $("#matchingPanel").show();
    if ($("#matchingPanel").data("opened")) {
        animatePanel(0, $("#matchingPanel").data("opened"));
        $("#matchingPanel").data("opened", false);
    } else {
        animatePanel(70, $("#matchingPanel").data("opened"));
        $("#matchingPanel").data("opened", true);
    }
}

function expandPreviewPanel() {
    hideAllPanels();
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#previewPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#previewPanel");
    //alert('here');
    $('#dialog').dialog('option', 'title', 'Preview');
    $("#previewPanel").show();
    //alert($("#datasetPanel").data("opened"));
    if ($("#previewPanel").data("opened")) {
        animatePanel(0, $("#previewPanel").data("opened"));
        $("#previewPanel").data("opened", false);
    } else {
        animatePanel(70, $("#previewPanel").data("opened"));
        $("#previewPanel").data("opened", true);
    }
}

function expandSPARQLFetchPanel() {
    hideAllPanels();
    
    document.getElementById("popupBBoxMenu").style.opacity = 0.0;
    document.getElementById("popupBBoxMenu").style.display = 'none'; 

    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#fg-fetch-sparql-panel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#fg-fetch-sparql-panel");
    //alert('here');
    $('#dialog').dialog('option', 'title', 'SPARQL Fetch');
    $("#fg-fetch-sparql-panel").show();
    //alert($("#datasetPanel").data("opened"));
    if ($("#fg-fetch-sparql-panel").data("opened")) {
        animatePanel(0, $("#fg-fetch-sparql-panel").data("opened"));
        $("#fg-fetch-sparql-panel").data("opened", false);
    } else {
        animatePanel(70, $("#fg-fetch-sparql-panel").data("opened"));
        $("#fg-fetch-sparql-panel").data("opened", true);
    }
}

function expandClusteringPanel() {
    hideAllPanels();
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#clusteringPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#clusteringPanel");
    //alert('here');
    $('#dialog').dialog('option', 'title', 'Clustering');
    $("#clusteringPanel").show();
    //alert($("#datasetPanel").data("opened"));
    if ($("#clusteringPanel").data("opened")) {
        animatePanel(0, $("#clusteringPanel").data("opened"));
        $("#clusteringPanel").data("opened", false);
    } else {
        animatePanel(33, $("#clusteringPanel").data("opened"));
        $("#clusteringPanel").data("opened", true);
    }
}

function expandDatasetPanel() {
    hideAllPanels();
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#datasetPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#datasetPanel");
    $('#dialog').dialog('option', 'title', 'Dataset');
    $("#datasetPanel").show();
    //alert($("#datasetPanel").data("opened"));
    if ($("#datasetPanel").data("opened")) {
        animatePanel(0, $("#datasetPanel").data("opened"));
        $("#datasetPanel").data("opened", false);
    } else {
        animatePanel(33, $("#datasetPanel").data("opened"));
        $("#datasetPanel").data("opened", true);
    }
}

function expandLinksPanel() {
    hideAllPanels();
    if ((lastClickedMenu != null) && (!$(lastClickedMenu).is($("#linksPanel")))) {
        //alert($(lastClickedMenu));
        $(lastClickedMenu).data("opened", false);
    }
    lastClickedMenu = $("#linksPanel");
    $('#dialog').dialog('option', 'title', 'Linking');
    $("#linksPanel").show();
    //alert($("#connectionPanel").data("opened"));
    if ($("#linksPanel").data("opened")) {
        animatePanel(0, $("#linksPanel").data("opened"));
        $("#linksPanel").data("opened", false);
    } else {
        animatePanel(70, $("#linksPanel").data("opened"));
        $("#linksPanel").data("opened", true);
    }
    
}

function beforeClosePanel() {
    //alert('closing');
    animatePanel(0, true);
    lastClickedMenu.data("opened", false);
    //$(this).hide();
    
    return false;
}

function periodicUpdate() {
    map.updateSize();
}
//function(){
//alert('tom');
//map.updateSize();

var selectedGeom = null;
var selectedGeomA = null;
var selectedGeomB = null;
var lastPiuxel;

function luda(event, deltaZ) {
    if (transType == ROTATE_TRANS) {

    } else if (transType == SCALE_TRANS) {

    }
}

var selectedGeom = null;
var selectedGeomA = null;
var selectedGeomB = null;
var lastPiuxel;

function luda2(e) {
    // alert('a');
}

var activeFeature = null;
var prevActiveFeature = null;

function luda(event, deltaZ) {
    var angle = 5.0;
    var scale = 2.0;
    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none';
    if (transType == ROTATE_TRANS) {
        if (deltaZ < 0)
            angle = -angle;
        activeFeature.geometry.rotate(angle, activeFeature.geometry.getCentroid(true));
        activeFeature.layer.drawFeature(activeFeature);
        //alert(angle);
    } else if (transType == SCALE_TRANS) {
        if (deltaZ < 0)
            scale = 0.9;
        else
            scale = 1.1;
        activeFeature.geometry.resize(scale, activeFeature.geometry.getCentroid(true));
        activeFeature.layer.drawFeature(activeFeature);
    }
}

function rotateFeature(feature, angle, origin) {
    //alert("tha me deis");
    feature.geometry.rotate(angle, origin);
    feature.layer.drawFeature(feature);
}

/*
 function startDrag(feature, pixel) {
 if ( selectedGeom == null ) {
 selectedGeom = feature;
 }
 lastPixel = pixel;
 }
 
 // Feature moving 
 function doDrag(feature, pixel) {
 //alert('Do drag');
 if ( selectedGeom != null ) {
 var res = map.getResolution();
 selectedGeom.geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
 //vectorsLinks.destroyFeatures([selectedGeom.attributes.links[0]]);
 //var start_point = selectedGeom.geometry.getCentroid(true);
 //var end_point = selectedGeom.attributes.links[0].attributes.la.geometry.getCentroid(true);
 //alert(start_point);
 //alert(end_point);
 //var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
 //linkFeature.attributes = {'la': selectedGeom.attributes.links[0].attributes.la,  'a': 'aalala', 'lb': selectedGeom.attributes.links[0].attributes.lb};
 //selectedGeom.attributes.links[0] = linkFeature;
 //vectorsLinks.addFeatures([linkFeature]);
 //vectors.drawFeature(selectedGeom);
 //linkFeature.state = OpenLayers.State.UPDATE;
 }
 lastPixel = pixel;
 }
 
 // Featrue stopped moving 
 function endDrag(feature, pixel) {
 if ( selectedGeom != null ) {
 //selectedGeom.geometry.transform(map.getProjectionObject(), WGS84);
 
 //vectorsLinks.destroyFeatures([selectedGeom.attributes.links[0]]);
 //var start_point = selectedGeom.geometry.getCentroid(true);
 //var end_point = selectedGeom.attributes.links[0].attributes.la.geometry.getCentroid(true);
 //alert(start_point);
 //alert(end_point);
 //var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
 //vectorsLinks.addFeatures([linkFeature]);
 //alert('End drag '+wkt.write(selectedGeom));
 //alert('End drag '+wkt.write(selectedGeom.attributes.links[0]));
 
 //selectedGeom.geometry.transform(WGS84, map.getProjectionObject());
 selectedGeom.state = OpenLayers.State.UPDATE;
 //linkFeature.state = OpenLayers.State.UPDATE;
 selectedGeom = null;
 }
 }
 */

function startDragA(feature, pixel) {
    //console.log( "drag A" );

    //if (selectedGeomA == null) {
        selectedGeomA = feature;
    //}
    
     if ( mselectActive ) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        activeFeatureClusterA[feature.attributes.links[0].attributes.la.attributes.a] = feature.attributes.links[0];
        activeFeatureClusterB[feature.attributes.links[0].attributes.lb.attributes.a] = feature.attributes.links[0];
        
        return;
    }
    
    if (lastPo != null) {
        if (prevActiveFeature != null) {
            if (prevActiveFeature == feature) {
                $('#createLinkButton').html("Cancel Link");
                //console.log(prevActiveFeature.fid);
                //console.log(feature.fid);
                //console.log("Prev with current " + (prevActiveFeature == feature));
                //console.log("Active with current " + activeFeature.fid === feature.fid);
            }
        }
    }
    
    //console.log(feature.attributes.links.length);    
    if ( feature.attributes.links.length ) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }
    
    transType = MOVE_TRANS;
    
    $('#createLinkButton').css("color", "red");
    
    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = mouse.y;
    document.getElementById("popupTransformMenu").style.left = mouse.x;           
   
    prevActiveFeature = activeFeature;
    activeFeature = feature;
    
    lastPixel = pixel;
}

// Feature moving 
function doDragA(feature, pixel) {
    //alert('Do drag');
    transType = MOVE_TRANS;
    
    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none'; 
    
    if (selectedGeomA != null) {
        //alert('nick');
        //if (feature != selectedGeomA ) {
        //alert('nick');
        if (selectedGeomA.attributes.links.length > 0) {
            // Redraw all links
            var i;
            for (i = 0; i < selectedGeomA.attributes.links.length; i++) {
                var validated = selectedGeomA.attributes.links[i].validated;
                var otherEnd = selectedGeomA.attributes.links[i].attributes.lb;
                var otherEndLinkIdx;
                for (var j = 0; j < otherEnd.attributes.links.length; j++) {
                    if ( otherEnd.attributes.links[j] == selectedGeomA.attributes.links[i] ) {
                        console.log('other end link' + otherEndLinkIdx);
                        console.log('other end link' + (otherEnd.attributes.links[j] == selectedGeomA.attributes.links[i]));
                        console.log('other end link' + (otherEnd.attributes.links[j].attributes.a == selectedGeomA.attributes.links[i].attributes.a));
                        otherEndLinkIdx = j;
                        
                        break;
                    } 
                }
                vectorsLinks.destroyFeatures([selectedGeomA.attributes.links[i]]);
                var start_point = selectedGeomA.geometry.getCentroid(true);
                var end_point = selectedGeomA.attributes.links[i].attributes.lb.geometry.getCentroid(true);

                selectedGeomA.geometry.transform(map.getProjectionObject(), WGS84);
                selectedGeomA.attributes.links[i].attributes.lb.geometry.transform(map.getProjectionObject(), WGS84);

                var start_point_wgs = selectedGeomA.geometry.getCentroid(true);
                var end_point_wgs = selectedGeomA.attributes.links[i].attributes.lb.geometry.getCentroid(true);

                globalOffsetAX = end_point_wgs.x - start_point_wgs.x;
                globalOffsetAY = end_point_wgs.y - start_point_wgs.y;

                selectedGeomA.geometry.transform(WGS84, map.getProjectionObject());
                selectedGeomA.attributes.links[i].attributes.lb.geometry.transform(WGS84, map.getProjectionObject());

                $('#offset-x-a').val(globalOffsetAX);
                $('#offset-y-a').val(globalOffsetAY);
                var vecLen = Math.sqrt(globalOffsetAX * globalOffsetAX + globalOffsetAY * globalOffsetAY);
                globalOffsetVecBX = globalOffsetAX / vecLen;
                globalOffsetVecBY = globalOffsetAY / vecLen;
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                linkFeature.attributes = {'la': selectedGeomA.attributes.links[0].attributes.la,
                    'a': selectedGeomA.attributes.links[i].attributes.a,
                    'lb': selectedGeomA.attributes.links[i].attributes.lb,
                    'cluster': selectedGeomA.attributes.links[i].attributes.cluster,
                    'opacity': selectedGeomA.attributes.links[i].attributes.opacity};
                linkFeature.prev_fused = false;
                linkFeature.validated = validated;
                if ( !validated ) {
                    linkFeature.jIndex = selectedGeomA.attributes.links[i].jIndex;
                    linkFeature.dist = selectedGeomA.attributes.links[i].dist;
                }
                selectedGeomA.attributes.links[i] = linkFeature;
                otherEnd.attributes.links[otherEndLinkIdx] = linkFeature;
                vectorsLinks.addFeatures([linkFeature]);
                //alert('nick');
                var res = map.getResolution();
                //selectedGeomA.geometry.move(pixel.x, pixel.y);
                vectorsLinks.drawFeature(linkFeature);
                //alert('nick');
                //}
            }
            vectorsA.drawFeature(selectedGeomA);
        }
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragA(feature, pixel) {
    if (selectedGeomA != null) {
        //alert('End drag '+selectedGeomA);
        //alert('End drag '+wkt.write(selectedGeomA));
        //alert('End drag '+wkt.write(selectedGeomA.linls[0]));
        selectedGeomA.geometry.transform(map.getProjectionObject(), WGS84);
        selectedGeomA.geometry.transform(WGS84, map.getProjectionObject());
        //selectedGeomA.state = OpenLayers.State.UPDATE;
        selectedGeomA = null;
        //alert('End drag '+selectedGeomA);
        //alert('End drag '+wkt.write(selectedGeomA.linls[0]));
    }
}

function startDragB(feature, pixel) {
    //if (selectedGeomB == null) {
        selectedGeomB = feature;
    //}
    transType = MOVE_TRANS;
    
    if ( mselectActive ) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        activeFeatureClusterA[feature.attributes.links[0].attributes.la.attributes.a] = feature.attributes.links[0];
        activeFeatureClusterB[feature.attributes.links[0].attributes.lb.attributes.a] = feature.attributes.links[0];
        
        return;
    }
    
    if (lastPo != null) {
        if (prevActiveFeature != null) {
            if (prevActiveFeature == feature) {
                $('#createLinkButton').html("Cancel Link");

                //console.log("Prev with current " + prevActiveFeature == feature);
                //console.log("Active with current " + activeFeature.fid === feature.fid);
            }
        }
    }
    
    //console.log(feature.attributes.links.length);    
    if ( feature.attributes.links.length ) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }
    
    $('#createLinkButton').css("color", "red");
    
    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = mouse.y;
    document.getElementById("popupTransformMenu").style.left = mouse.x;           
   
    prevActiveFeature = activeFeature;
    activeFeature = feature;
    
    lastPixel = pixel;
}

// Feature moving 
function doDragB(feature, pixel) {
    transType = MOVE_TRANS;
    
    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none'; 
    if (selectedGeomB != null) {
        //if (feature != selectedGeomB ) {
        if (selectedGeomB.attributes.links.length > 0) {
            // Redraw all links
            //console.log(selectedGeomB.attributes.links.length);
            var i;
            for (i = 0; i < selectedGeomB.attributes.links.length; i++) {
                var validated = selectedGeomB.attributes.links[i].validated;
                var otherEnd = selectedGeomB.attributes.links[i].attributes.la;
                var otherEndLinkIdx = 0;
                
                
                for (var j = 0; j < otherEnd.attributes.links.length; j++) {
                    if ( otherEnd.attributes.links[j] == selectedGeomB.attributes.links[i] ) {
                        otherEndLinkIdx = j;
                        
                        break;
                    } 
                }
                vectorsLinks.destroyFeatures([selectedGeomB.attributes.links[i]]);
                var start_point = selectedGeomB.geometry.getCentroid(true);
                var end_point = selectedGeomB.attributes.links[i].attributes.la.geometry.getCentroid(true);

                selectedGeomB.geometry.transform(map.getProjectionObject(), WGS84);
                selectedGeomB.attributes.links[i].attributes.la.geometry.transform(map.getProjectionObject(), WGS84);

                var start_point_wgs = selectedGeomB.geometry.getCentroid(true);
                var end_point_wgs = selectedGeomB.attributes.links[i].attributes.la.geometry.getCentroid(true);

                //console.log(globalOffsetBX);
                //console.log(end_point_wgs.x + " " + end_point_wgs.x);

                globalOffsetBX = end_point_wgs.x - start_point_wgs.x;
                globalOffsetBY = end_point_wgs.y - start_point_wgs.y;

                //console.log(globalOffsetBX);
                //console.log(globalOffsetBY);

                selectedGeomB.geometry.transform(WGS84, map.getProjectionObject());
                selectedGeomB.attributes.links[i].attributes.la.geometry.transform(WGS84, map.getProjectionObject());

                $('#offset-x-b').val(globalOffsetBX);
                $('#offset-y-b').val(globalOffsetBY);

                var vecLen = Math.sqrt(globalOffsetBX * globalOffsetBX + globalOffsetBY * globalOffsetBY);
                globalOffsetVecBX = globalOffsetBX / vecLen;
                globalOffsetVecBY = globalOffsetBY / vecLen;
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                linkFeature.attributes = {'la': selectedGeomB.attributes.links[i].attributes.la,
                    'a': selectedGeomB.attributes.links[i].attributes.a,
                    'lb': selectedGeomB.attributes.links[i].attributes.lb,
                    'cluster': selectedGeomB.attributes.links[i].attributes.cluster};
                linkFeature.prev_fused = false;
                linkFeature.validated = validated;
                if ( !validated ) {
                    linkFeature.jIndex = selectedGeomB.attributes.links[i].jIndex;
                    linkFeature.dist = selectedGeomB.attributes.links[i].dist;
                }
                selectedGeomB.attributes.links[i] = linkFeature;
                otherEnd.attributes.links[otherEndLinkIdx] = linkFeature;
                vectorsLinks.addFeatures([linkFeature]);
                var res = map.getResolution();
                //selectedGeomB.geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
                vectorsLinks.drawFeature(linkFeature);
                //alert('sth fishy');
                //}
            }
            vectorsB.drawFeature(selectedGeomB);
        }
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragB(feature, pixel) {
     if (selectedGeomB != null) {
        selectedGeomB = null;
    }
}

function activateVisibleSelect() {
    //console.log("Fusing");
    var bounds = map.calculateBounds();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseVisibleServlet",
        // the data to send (will be converted to a query string)
        data: {"left": bounds.left,
            "bottom": bounds.bottom,
            "right": bounds.right,
            "top": bounds.top},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseText);
            addMapDataJson(responseJson);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            alert("Sorry, there was a problem!");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });
}

$("#link_tooltip").bind("transitionend webkitTransitionEnd oTransitionEnd MSTransitionEnd", function () {
    if (!feature_is_selected) {
        //document.getElementById("link_tooltip").style.display = 'none';
    }
});

//proj4.defs('CART', "+title= (long/lat) +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees");

var epsg900913 = new OpenLayers.Projection('EPSG:900913');
var epsg3035 = new OpenLayers.Projection("EPSG:4326");
//var epsg900913 = new OpenLayers.Projection('EPSG:3035');
//var epsg3035   = new OpenLayers.Projection('EPSG:900913');

function onFeatureOver(event) {
    //alert('Over');
}

function onFeatureOver2(event) {
    //alert('Over 2');
}

function onFeatureUnselect(event) {
    transType = MOVE_TRANS;
    
    console.log("Unselect");
    
    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none'; 
    
    //prevActiveFeature = event.feature;
    //activeFeature = null;
    
    //document.getElementById("transformSelect").style.display = 'none';
    //activeFeature = null;
}

function setTransformation() {
    //alert('Set Transformation');
    //var nameValue = document.getElementById("transForm").value;
    //alert($('input[name=t]:checked', '#transForm').val());
    if ($('input[name=t]:checked', '#transForm').val() == 'tra') {
        transType = MOVE_TRANS;
        dragControlA.activate();
        dragControlB.activate();
    } else if ($('input[name=t]:checked', '#transForm').val() == 'rot') {
        transType = ROTATE_TRANS;
        dragControlA.deactivate();
        dragControlB.deactivate();
        //alert('tom');
    } else {
        transType = SCALE_TRANS;
        dragControlA.deactivate();
        dragControlB.deactivate();
    }
}

function onFeatureSelect(event) {
    //alert(mselectActive);
    if ( mselectActive ) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        activeFeatureClusterA[event.feature.attributes.links[0].attributes.la.attributes.a] = event.feature.attributes.links[0];
        activeFeatureClusterB[event.feature.attributes.links[0].attributes.lb.attributes.a] = event.feature.attributes.links[0];
        
        return;
    }
    //console.log(event.feature.attributes.links.length);    
    if ( event.feature.attributes.links.length ) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }
    
    $('#createLinkButton').css("color", "red");
    $('#createLinkButton').html("Create Link");
    
    //console.log("Select");
    
    if (lastPo != null) {
        if (prevActiveFeature != null) {
            if (prevActiveFeature == event.feature) {
                $('#createLinkButton').html("Cancel Link");

                console.log("Prev with current " + prevActiveFeature == event.feature);
                //console.log("Active with current " + activeFeature.fid === event.feature.fid);
            }
        }
    }
    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = mouse.y;
    document.getElementById("popupTransformMenu").style.left = mouse.x;           
   
    prevActiveFeature = activeFeature;
    activeFeature = event.feature;
}

function onFeatureUnselectB(event) { 
    alert('Unselect B');
}

function onFeatureSelectB(event) {
    alert('Select B');
}

function onFeatureUnselectA(event) {
    alert('Unselect A');
}

function onFeatureSelectA(event) {
    alert('Select A');
}

function onLinkedFeatureUnselect(event) {
    //document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;
}

function isUrlValid(url) {
    return /^(https?|s?ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(url);
}

function previewTriples(data) {
    var $tbl = $("#previewTable");
    $tbl.html("");
    $.each(data.triples, function (index, element) {
        var opt = document.createElement("tr");
        var sub = element.s;
        var pre = element.p;
        var obj = element.o;
        
        if ( isUrlValid( sub ) )
            sub = "<a style=\"text-decoration: none; font-size: 80%;\" href=\""+sub+"\" target=\"_blank\">"+sub+"</a>";
        
        if ( isUrlValid( pre ) )
            pre = "<a style=\"text-decoration: none; font-size: 80%;\" href=\""+pre+"\" target=\"_blank\">"+pre+"</a>";
        
        if ( isUrlValid( obj ) )
            obj = "<a style=\"text-decoration: none; font-size: 80%;\"  href=\""+obj+"\" target=\"_blank\">"+obj+"</a>";
        
        
        var entry =
                " <td>" + sub + "</td>\n" +
                " <td>" + pre + "</td>\n" +
                " <td>" + obj + "</td>\n";

        opt.innerHTML = entry;
        //alert(opt.innerHTML);
        $tbl.append($(opt));
    });
}

function onFusedSelect(event) {
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FetchLinkDataServlet",
        // the data to send (will be converted to a query string)
        data: {subject: event.feature.attributes.a},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            //fusionPanel(event.feature, responseJson);
            //alert(responseJson);
            previewTriples(responseJson);
            //map.zoomToExtent(event.feature.geometry.getBounds());
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            //alert("Sorry, there was a problem with the second AJAX");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });

    if (typeof event.feature.attributes.la !== "undefined") {
        event.feature.attributes.la.style = null;
        event.feature.attributes.lb.style = null;
    }

    //selectControl.deactivate();
    dragControlA.deactivate();
    dragControlB.deactivate();

    vectorsA.redraw();
    vectorsB.redraw();
        
    expandPreviewPanel();
}

function onFusedUnselect(event) {
    expandPreviewPanel();
    
    selectControl.deactivate();
    dragControlB.activate();
    dragControlA.activate();
    selectControl.activate();
    
    event.feature.attributes.la.style = {display: 'none'};
    event.feature.attributes.lb.style = {display: 'none'};

    vectorsA.redraw();
    vectorsB.redraw();
}

function onLinkFeatureSelect(event) {
    //alert(event.feature.prev_fused);
    if ( multipleEnabled === true )
        return;
    
    if ( !event.feature.validated ) {
        
        document.getElementById("popupValidateMenu").style.opacity = 0.7;
        document.getElementById("popupValidateMenu").style.display = 'inline';
        document.getElementById("popupValidateMenu").style.top = mouse.y;
        document.getElementById("popupValidateMenu").style.left = mouse.x; 
    
        $('#valButton').prop("link",event.feature);
        
        return;
    }
    if (event.feature.prev_fused === true) {
        feature_is_selected = true;
        
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FetchLinkDataServlet",
            // the data to send (will be converted to a query string)
            data: {subject: event.feature.attributes.a},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                //$('#connLabel').text(responseJson);
                //fusionPanel(event.feature, responseJson);
                //alert(responseJson);
                previewTriples(responseJson);
            },
            // code to run if the request fails; the raw request and
            // status codes are passed to the function
            error: function (xhr, status, errorThrown) {
                alert("Sorry, there was a problem with the second AJAX");
                console.log("Error: " + errorThrown);
                console.log("Status: " + status);
                console.dir(xhr);
            },
            // code to run regardless of success or failure
            complete: function (xhr, status) {
                //$('#connLabel').text("connected");
            }
        });
        
        expandPreviewPanel();
        
        event.feature.attributes.la.style = null;
        event.feature.attributes.lb.style = null;
        
        //selectControl.deactivate();
        dragControlA.deactivate();
        dragControlB.deactivate();
        
        vectorsA.redraw();
        vectorsB.redraw();
        //vectorsLinks.refresh();

        return;
    }
    
    feature_is_selected = true;
    //alert("Feature "+event.feature.attributes.a);
    //alert("Feature "+event.feature.attributes.la);
    current_feature = event.feature;

    //alert('tom');
    hideAllPanels();
    $("#fusionPanel").show();
    //alert($("#fusionPanel").data("opened"));
    if ($("#fusionPanel").data("opened")) {
        animatePanel(0, $("#fusionPanel").data("opened"));
        $("#fusionPanel").data("opened", false);
    } else {
        animatePanel(70, $("#fusionPanel").data("opened"));
        $("#fusionPanel").data("fusionPanel", true);
    }

    var sendData = new Array();
    sendData[sendData.length] = event.feature.attributes.a;
    var list = document.getElementById("matchList");
    var linkList = document.getElementById("linkMatchList");
    var listItem = list.getElementsByTagName("li");
    
    if (typeof listItem !== "undefined" && ( listItem.length > 0 ) ) {
        var inputItem = listItem[0].getElementsByTagName("input");
        sendData[sendData.length] = inputItem[0].value;
        sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";
        for (var i = 1; i < listItem.length; i++) {
            //node = document.createElement("li");
            //node.onclick = matchedSchemaClicked;
            var inputItem = listItem[i].getElementsByTagName("input");
            //alert('Long name = '+listItem[i].long_name);
            sendData[sendData.length] = inputItem[0].value;
            //alert(selectedProperties['id'+(i-1)]);
            sendData[sendData.length] = listItem[i].long_name;
            //alert(sendData[sendData.length-1]);
        }
    } else {
        sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";
        sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";
    }
   
    enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "LinkSchemasServlet",
        // the data to send (will be converted to a query string)
        data: {"subject": event.feature.attributes.a},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            linkMatchesJSON = responseJson;
            $('#linkNameA').text(responseJson.p.nodeA);
            $('#linkNameB').text(responseJson.p.nodeB);
            document.getElementById("linkMatchList").innerHTML = "";
            var list = document.getElementById("matchList");
            var linkList = document.getElementById("linkMatchList");
            var listItem = list.getElementsByTagName("li");
            var inputItem = listItem[0].getElementsByTagName("input");
            var node = document.createElement("li");
            //node.onclick = matchedSchemaClicked;
            node.innerHTML = listItem[0].innerHTML;
            node.long_name = listItem[0].long_name;
            node.newPred = listItem[0].newPred;
            node.rowIndex = listItem[0].rowIndex;
            $(node).on('input', function (e) {
                var row = $("#fusionTable tr")[this.rowIndex];
                this.newPred = e.target.value;
                $(row).get(0).newPred = e.target.value;
                $(row).find("td")[1].innerHTML = e.target.value;
            });
            console.log(node.innerHTML);
            console.log(node.long_name);
            console.log(node.newPred);
            console.log(node.rowIndex);
            document.getElementById("linkMatchList").appendChild(node);
            for (var i = 1; i < listItem.length; i++) {
                node = document.createElement("li");
                //node.onclick = matchedSchemaClicked;
                node.innerHTML = listItem[i].innerHTML;
                node.long_name = listItem[i].long_name;
                node.newPred = listItem[i].newPred;
                node.rowIndex = listItem[i].rowIndex;
                $(node).on('input', function (e) {
                    var row = $("#fusionTable tr")[this.rowIndex];
                    this.newPred = e.target.value;
                    $(row).get(0).newPred = e.target.value;
                    $(row).find("td")[1].innerHTML = e.target.value;
                });
                console.log(node.innerHTML);
                console.log(node.long_name);
                console.log(node.newPred);
                console.log(node.rowIndex);
                document.getElementById("linkMatchList").appendChild(node);
                updateFusionTable(node);
            }
            var schemaListA = document.getElementById("linkSchemasA");
            schemaListA.innerHTML = "";
            $.each(responseJson.p.propsFullA, function (index, element) {
                console.log(element.short_rep);
                if (element.short_rep.indexOf("posSeq") >= 0) {
                    //return;
                }
                var opt = document.createElement("li");
                var optlbl = document.createElement("div");
                $(optlbl).addClass("scored");
                optlbl.innerHTML = "";
                decodeURIComponent(element.short_rep);
                opt.innerHTML = decodeURIComponent(element.short_rep);
                opt.long_name = element.long_rep;
                //alert(element+" "+responseJson.propsA[index]);
                opt.onclick = linkPropSelectedA;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListA.appendChild(opt);
            });
            var schemaListB = document.getElementById("linkSchemasB");
            schemaListB.innerHTML = "";
            $.each(responseJson.p.propsFullB, function (index, element) {
                if (element.short_rep.indexOf("posSeq") >= 0) {
                    //return;
                }
                var opt = document.createElement("li");
                var optlbl = document.createElement("div");
                $(optlbl).addClass("scored");
                optlbl.innerHTML = "";
                opt.innerHTML = decodeURIComponent(element.short_rep);
                opt.long_name = element.long_rep;
                opt.onclick = linkPropSelectedB;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListB.appendChild(opt);
            });
            disableSpinner();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            disableSpinner();
            alert("Sorry, there was a problem with the first AJAX");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });


    var list = document.getElementById("matchList");
    var listItem = list.getElementsByTagName("li");
    if (listItem.length == 1) {
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FusionServlet",
            // the data to send (will be converted to a query string)
            data: {props: sendData},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                //$('#connLabel').text(responseJson);
                fusionPanel(event.feature, responseJson);
                //map.zoomToExtent(event.feature.geometry.getBounds());
            },
            // code to run if the request fails; the raw request and
            // status codes are passed to the function
            error: function (xhr, status, errorThrown) {
                alert("Sorry, there was a problem with the second AJAX");
                console.log("Error: " + errorThrown);
                console.log("Status: " + status);
                console.dir(xhr);
            },
            // code to run regardless of success or failure
            complete: function (xhr, status) {
                //$('#connLabel').text("connected");
            }
        });
    }
    
}

// upda
function updateBFusionTable(node) {
    var $list = $("#matchList");
    var $tbl = $("#bFusionTable");
    var rowCount = $("#bFusionTable tr").length;
    
    var property = node.newPred;
    var opt = document.createElement("tr");
    opt.long_name = node.long_name;
    opt.newPred = property;
    node.rowIndex = rowCount+1;
    
    var trunc = FAGI.Utilities.getPropertyName(porperty);
    
    var entry = " <td>" + "Metadata from A" + "</td>\n" +
            " <td>" + trunc + "</td>\n" +
            " <td>" + "Metadata from B" + "</td>\n" +
            " <td><select style=\"color: black; width: 100%;\">" + avail_meta_trans + "</select></td>\n"

    opt.innerHTML = entry;

    $tbl.append($(opt));
}

function updateFusionTable(node) {
    console.log(current_feature == null);
    if (current_feature != null) {
        var sendData = new Array();
        sendData[sendData.length] = current_feature.attributes.a;
        var list = document.getElementById("matchList");
        var listItem = list.getElementsByTagName("li");
        var inputItem = listItem[0].getElementsByTagName("input");
        sendData[sendData.length] = inputItem[0].value;
        sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";

        list = document.getElementById("linkMatchList");
        listItem = list.getElementsByTagName("li");
        for (var i = 1; i < listItem.length; i++) {
            var inputItem = listItem[i].getElementsByTagName("input");
            sendData[sendData.length] = inputItem[0].value;
            sendData[sendData.length] = listItem[i].long_name;
            console.log("Long name : "+listItem[i].long_name);
        }
        //alert(event.feature.attributes.a);
        //alert("done");
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FusionServlet",
            // the data to send (will be converted to a query string)
            data: {props: sendData},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                //$('#connLabel').text(responseJson);
                fusionPanel(current_feature, responseJson, node);
            },
            // code to run if the request fails; the raw request and
            // status codes are passed to the function
            error: function (xhr, status, errorThrown) {
                alert("Sorry, there was a problem with the second AJAX");
                console.log("Error: " + errorThrown);
                console.log("Status: " + status);
                console.dir(xhr);
            },
            // code to run regardless of success or failure
            complete: function (xhr, status) {
                //$('#connLabel').text("connected");
            }
        });
    }
}

/*
 * 
 * @param {type} val
 * @returns {undefined}
 */

var avail_trans = "";
var avail_meta_trans = "";
    
function fusionPanel(event, val, node) {
    var recommendation = new Object();
    recommendation.geometryA = val.geomsA[0];
    recommendation.geometryB = val.geomsB[0];
    recommendation.owlClassA = new Array();
    recommendation.owlClassB = new Array();
    recommendation.fusionAction = "";
    $.each(val.classesA, function (index, element) {
        recommendation.owlClassA[recommendation.owlClassA.length] = element;
    });
    $.each(val.classesB, function (index, element) {
        recommendation.owlClassB[recommendation.owlClassB.length] = element;
    });
    //alert(JSON.stringify(recommendation));
    
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "LearningServlet",
        // the data to send (will be converted to a query string)
        data: {'actions':JSON.stringify(recommendation)},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //alert(JSON.stringify(responseJson));
            $('#classRecommendation').html("");
            /*$('#classRecommendation').append($("<option></option>")
                .attr("value",responseJson.tagA)
                .text(responseJson.tagA)); 
            $('#classRecommendation').append($("<option></option>")
                .attr("value",responseJson.tagB)
                .text(responseJson.tagB)); 
            */
            $.each(responseJson.tagList, function (index, element) {
                $('#classRecommendation').append($("<option></option>")
                .attr("value",element)
                .text(element)); 
            });
            $('#geoTrans option[value="'+responseJson.predictedFusionAction+'"]').attr('selected', 'selected');
            /*avail_classes = "";
            $.each(responseJson.tag, function (index, element) {
                avail_trans += "<option value=\"" + element + "\">" + element + "</option>";
            });*/
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            alert("Sorry, there was a problem with the second AJAX");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });
    
    var geom_typeA = val.geomsA[0].substring(0, val.geomsA[0].indexOf("("));
    var geom_typeB = val.geomsB[0].substring(0, val.geomsB[0].indexOf("("));
    avail_trans = "";
    avail_meta_trans = "";
     $.each(val.geomTransforms, function (index, element) {
        avail_trans += "<option value=\""+element+"\">" + element + "</option>";
    });
    $.each(val.metaTransforms, function (index, element) {
        avail_meta_trans += "<option value=\""+element+"\">" + element + "</option>";
    });

    var s = "<p class=\"geoinfo\" id=\"link_name\">Ludacris</p>\n" +
//" <div class=\"checkboxes\">\n"+
//" <label for=\"chk1\"><input type=\"checkbox\" name=\"chk1\" id=\"chk1\" />Flag as misplaced fusion</label><br />\n"+
//" </div>\n"+
//" Description: <textarea name=\"textarea\" style=\"width:99%;height:50px;\" class=\"centered\"></textarea>\n"+
            " <table class=\"rwd-table\" border=1 id=\"fusionTable\">\n" +
            " <tr>\n" +
            " <td>Value from " + $('#idDatasetA').val() + "</td>\n" +
            " <td>Predicate</td>\n" +
            " <td>Value from " + $('#idDatasetB').val() + "</td>\n" +
            " <td>Action</td>\n" +
//" <td style=\"width:20%; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
            " </tr>\n" +
            " <tr>\n" +
            " <td title=\"" + val.geomsA[0] + "\">" + geom_typeA + "</td>\n" +
            " <td>asWKT</td>\n" +
            " <td title=\"" + val.geomsB[0] + "\">" + geom_typeB + "</td>\n" +
            " <td><select id=\"geoTrans\" style=\"color: black; width: 100%;\">" + avail_trans + "</select></td>\n" +
//" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Fused Geom</td>\n"+
            " </tr>\n" +
            " </table>" +
            " <table border=0 id=\"shiftPanel\">" +
            " <tr>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Shift (%):</td>\n" +
            " <td style=\"width:10px; text-align: center;\" align=\"left\" valign=\"bottom\"><input style=\"width:50px;\" type=\"text\" id=\"shift\" name=\"shift\" value=\"100\"/></td>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Scale:</td>\n" +
            " <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"scale_fac\" name=\"x_scale\" value=\"1.0\"/></td>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Rotate:</td>\n" +
            " <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"rotate_fac\" name=\"x_rotate\" value=\"0.0\"/></td>\n" +
//" <td style=\"width:21px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
            " </tr>\n" +
//" <tr>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\">Scale X:</td>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"x_scale\" name=\"x_scale\" value=\"1.0\"/></td>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\">Scale Y:</td>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"y_scale\"name=\"y_scale\" value=\"1.0\"/></td>\n"+
//" <td style=\"width:21px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
//" </tr>\n"+
//" <tr>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\">Rotate X:</td>\n"+
//" <td style=\"text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"x_rotate\" name=\"x_rotate\" value=\"1.0\"/></td>\n"+
//" <td style=\"width:21px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
//" </tr>\n"+
            " </table>" +
            " <input id=\"fuseButton\" type=\"submit\" value=\"Fuse\" style=\"float:right\" onclick=\"return false;\"/>\n";

    document.getElementById("linkFusionTable").innerHTML = s;
    var tbl = document.getElementById("fusionTable");
    $('#fuseButton').click(performFusion);
    $('#fuseButton').prop("recommendation", recommendation);
    
    //alert ("props");
    //alert (val['properties']);
    //alert (val.properties);
    //$.each(val, function(index, element) {
    if (typeof val.properties !== "undefined") {
        // Works
        $.each(val.properties, function (index1, element1) {
            var opt = document.createElement("tr");
            opt.long_name = element1.propertyLong;
            console.log("Long name" + opt.long_name);
            if ( opt.long_name == node.long_name ) {
                node.rowIndex = $("#fusionTable").find("tr").length;
            }
            
            var trunc = FAGI.Utilities.getPropertyName(element1.property);
            
            var entry = " <td>" + element1.valueA + "</td>\n" +
                    " <td>" + trunc + "</td>\n" +
                    " <td>" + element1.valueB + "</td>\n" +
                    " <td><select style=\"color: black; width: 100%;\">" + avail_meta_trans + "</select></td>\n"
//" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n";

            opt.innerHTML = entry;
            //alert(opt);
            //alert("HURRAY");
            tbl.appendChild(opt);
            //alert(tbl);
            //alert("HURRAY");
            //});
        });
        //});
        //alert("reached");
        //alert(document.getElementById("link_tooltip").innerHTML);
    }
    //alert("Nick 1" );
    //alert("Nick");
    document.getElementById("link_name").innerHTML = event.attributes.a;

    //$('#geoTrans option[value="ShiftAToB"]').attr('selected', 'selected');
    var preSelected = $('#geoTrans').find("option:selected").text();
    if (preSelected !== "ShiftAToB" && preSelected !== "ShiftBToA") {
        $('#scale_fac').attr('disabled', 'disabled');
        $('#rotate_fac').attr('disabled', 'disabled');
        $('#shift').attr('disabled', 'disabled');
    }
    $('#geoTrans').change(function () {
        //alert( $(this).find("option:selected").text() );      
        var selection = $(this).find("option:selected").text();
        if (selection === "ShiftAToB" || selection === "ShiftBToA") {
            $('#scale_fac').removeAttr('disabled');
            $('#rotate_fac').removeAttr('disabled');
            $('#shift').removeAttr('disabled');
        } else {
            $('#scale_fac').attr('disabled', 'disabled');
            $('#rotate_fac').attr('disabled', 'disabled');
            $('#shift').attr('disabled', 'disabled');
        }
    });

    //alert("Nick");
}

function performFusion() {
    var tbl = document.getElementById("fusionTable");
    //alert('so close 2');
    var tblBody = document.getElementById("fusionTable");
    //alert(tblBody);
    var tblRows = tblBody.getElementsByTagName("tr");
    //alert(tblRows);
    //alert(tblRows[0]);
    var sendData = new Array();
    var sendJSON = new Array();
    var shiftValuesJSON = null;
   
    if (!$('#scale_fac').prop('disabled')) {
        shiftValuesJSON = new Object();
        shiftValuesJSON.shift = $('#shift').val();
        shiftValuesJSON.scaleFact = $('#scale_fac').val();
        shiftValuesJSON.rotateFact = $('#rotate_fac').val();
    }
    
    //classRecommendation
    var send = $('#classRecommendation').val();
    
    if ( send == null ) 
        send = "";
    
    //alert(send);
    //alert(current_feature == null);
    var geomCells = tblRows[1].getElementsByTagName("td");
    var geomFuse = new Object();
    current_feature.attributes.la.geometry.transform(map.getProjectionObject(), WGS84);
    current_feature.attributes.lb.geometry.transform(map.getProjectionObject(), WGS84);

    geomFuse.valA = wkt.write(current_feature.attributes.la);
    geomFuse.pre = geomCells[1].innerHTML;
    //alert('after pre');
    geomFuse.preL = tblRows[0].long_name;
    //lert('after prel');
    if (typeof geomFuse.preL === "undefined") {
        geomFuse.preL = "dummy";
    }

    geomFuse.valB = wkt.write(current_feature.attributes.lb);
    var tmpGeomAction = geomCells[3].getElementsByTagName("select");
    //alert('after valB '+tmpGeomAction.length+' '+geomCells.length);
    if (tmpGeomAction.length == 1) {
        geomFuse.action = tmpGeomAction[0].value;
    }
    
    
    var teach = $('#fuseButton').prop("recommendation");
    teach.fusionAction = geomFuse.action;
    //alert(JSON.stringify(teach));
    /*
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "LearningServlet",
        // the data to send (will be converted to a query string)
        data: {'actions':JSON.stringify(teach)},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            alert("Sorry, there was a problem with the second AJAX");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });
    */
    current_feature.attributes.la.geometry.transform(WGS84, map.getProjectionObject());
    current_feature.attributes.lb.geometry.transform(WGS84, map.getProjectionObject());

    sendJSON[sendJSON.length] = geomFuse;
    for (var i = 2; i < tblRows.length; i++) {
        var cells = tblRows[i].getElementsByTagName("td");
        var propFuse = new Object();

        propFuse.valA = cells[0].innerHTML;
        propFuse.pre = cells[1].innerHTML;
        propFuse.preL = tblRows[i].long_name;

        if (typeof propFuse.preL === "undefined") {
            propFuse.preL = "dummy";
        }

        propFuse.valB = cells[2].innerHTML;
        var tmpAction = cells[3].getElementsByTagName("select");
        if (tmpAction.length == 1) {
            propFuse.action = tmpAction[0].value;
        }

        sendJSON[sendJSON.length] = propFuse;

        for (var j = 0; j < cells.length; j++) {
            //alert(cells[j].getElementsByTagName("select").length);
            //alert(cells[j]);
            var action = cells[j].getElementsByTagName("select");
            //alert(action[0]);
            if (action.length == 1) {
                //alert(action[0].innerHTML);
                //alert(action[0].value);
                var cell = action[0].value;
                sendData[sendData.length] = cell;
            } else if (j == 1) {
                var cell = cells[j].long_name;
                //alert(cell);
                sendData[sendData.length] = cell;
            } else {
                var cell = cells[j].innerHTML;
                sendData[sendData.length] = cell;
            }
            ;
            //alert(cell);
        }
    }

    var sndJSON = JSON.stringify(sendJSON);
    var sndShiftJSON = JSON.stringify(shiftValuesJSON);

    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseLinkServlet",
        // the data to send (will be converted to a query string)
        data: {props: sendData, propsJSON: sndJSON, factJSON: sndShiftJSON, classes: send},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            //alert(responseJson);
            previewLinkedGeom(responseJson);
            //fusionPanel(event, responseJson);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            alert("Sorry, there was a problem!");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            console.dir(xhr);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            //$('#connLabel').text("connected");
        }
    });

}

function addFusedMapDataJson(jsongeoms) {
    //alert('tom');
    $.each(jsongeoms.geoms, function (index, element) {
        //console.log(element.geom);
        var linkFeature = wkt.read(element.geom);
        if (linkFeature instanceof Array) {
            for (var i = 0; i < linkFeature.length; i++) {
                linkFeature[i].geometry.transform(WGS84, map.getProjectionObject());
                linkFeature[i].attributes = {'a': element.subject};
            }
            vectorsFused.addFeatures(linkFeature);
        } else {
            //alert(linkFeature.geometry);
            linkFeature.geometry.transform(WGS84, map.getProjectionObject());
            linkFeature.attributes = {'a': element.subject};
            linkFeature.attributes.a = element.subject;
            vectorsFused.addFeatures([linkFeature]);
        }
    });
    vectorsFused.redraw();
}

function previewLinkedGeom(resp) {
    var tempGeomA = null;
    var tempGeomB = null;
    if ( current_feature.attributes.la.style == null ) {
        tempGeomA = wkt.read(current_feature.attributes.la.attributes.oGeom);
        tempGeomA.attributes = {'a': current_feature.attributes.la.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(tempGeomA)};
        tempGeomA.style = { display : 'none' };
        vectorsA.removeFeatures([current_feature.attributes.la]);
        vectorsA.addFeatures([tempGeomA]);
    }
    
    if ( current_feature.attributes.lb.style == null ) {
        tempGeomB = wkt.read(current_feature.attributes.lb.attributes.oGeom);
        tempGeomB.attributes = {'a': current_feature.attributes.lb.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(tempGeomB)};
        tempGeomB.style = { display : 'none' };
        vectorsB.removeFeatures([current_feature.attributes.lb]);
        vectorsB.addFeatures([tempGeomB]);
    }
    
    current_feature.attributes.la = tempGeomA;
    current_feature.attributes.lb = tempGeomB;

    var linkFeature = wkt.read(resp.geom);
    //alert(resp.geom);
    if (Object.prototype.toString.call(linkFeature) === '[object Array]') {
        //alert('Array');
        for (var i = 0; i < linkFeature.length; i++) {
            linkFeature[i].geometry.transform(WGS84, map.getProjectionObject());
            linkFeature[i].attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb, 'cluster': current_feature.attributes.cluster};
            
            linkFeature[i].prev_fused = true;
            linkFeature[i].validated = true;
            //vectorsLinks.addFeatures([linkFeature[i]]);
            vectorsFused.addFeatures([linkFeature[i]]);
            //alert('done');
        }
        vectorsLinks.removeFeatures([current_feature]);
    } else {
        //alert('reached');
        linkFeature.geometry.transform(WGS84, map.getProjectionObject());
        linkFeature.attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb, 'cluster': current_feature.attributes.cluster};
        
        //alert('done feature '+linkFeature);
        linkFeature.prev_fused = true;
        linkFeature.validated = true;
        //alert('reached 2');
        vectorsLinks.removeFeatures([current_feature]);
        //vectorsLinks.addFeatures([linkFeature]);
        vectorsFused.addFeatures([linkFeature]);
    }

    vectorsA.redraw();
    vectorsB.redraw();
    vectorsLinks.refresh();
    vectorsFused.refresh();
    //alert('done');
    current_feature = null;
    feature_is_selected = false;
}

function onLinkFeatureUnselect(event) {
    if ( multipleEnabled === true )
        return;
    //document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;
    //alert('a');
    if (event.feature.prev_fused === true) {
        event.feature.attributes.la.style = {display: 'none'};
        event.feature.attributes.lb.style = {display: 'none'};
        
        //selectControl.activate();
        dragControlA.activate();
        dragControlB.activate();
        
        vectorsA.redraw();
        vectorsB.redraw();
    }
    //document.getElementById("link_tooltip").style.display = 'none';
    //document.getElementById("link_tooltip").fadeOut('slow', 'linear');
}

function onFeatureSelectFromLinks(event) {
    // fetch the cluster's latlon and set the map center to it and call zoomin function
    // which takes you to a one level zoom in and I hope this solves your purpose :)    
    map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    map.zoomToExtent(event.feature.geometry.getBounds(), true);
    map.zoomIn();
    //alert('tom');
    $("testid2").html("your new header");
    $("testid2").html('your new header');
    document.getElementById("testid2").innerText = "public offers";
    document.getElementById("testid2").innerHTML = event.feature.style.title;
    feature_is_selected = true;
    //current_tip = mouse;

    //current_feature = event.feature;

    //alert(event.feature.attributes.a);
}

function onFeatureUnselectFromLinks(event) {
    //document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;
    //document.getElementById("link_tooltip").style.display = 'none';

    //document.getElementById("link_tooltip").fadeOut('slow', 'linear');
}


function onFeatureSelectFromLinks(event) {
    // fetch the cluster's latlon and set the map center to it and call zoomin function
    // which takes you to a one level zoom in and I hope this solves your purpose :)    
    map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    map.zoomToExtent(event.feature.geometry.getBounds(), true);
    map.zoomIn();
    //alert('tom');
    $("testid2").html("your new header");
    $("testid2").html('your new header');
    document.getElementById("testid2").innerText = "public offers";
    document.getElementById("testid2").innerHTML = event.feature.style.title;
    feature_is_selected = true;
    //current_tip = mouse;

    //current_feature = event.feature;
    //var s='<p class="geoinfo" id="link_name">Ludacris</p>\n';
    /*" <div class=\"checkboxes\">\n"+
     " <label for=\"chk1\"><input type=\"checkbox\" name=\"chk1\" id=\"chk1\" />Flag as misplaced fusion</label><br />\n"+
     " </div>\n"+
     " Description: <textarea name=\"textarea\" style=\"width:99%;height:50px;\" class=\"centered\"></textarea>\n"+
     " <input id=\"repButton\" type=\"submit\" value=\"Report\" style=\"float:right\" onclick=\"return false;\"/>\n"+
     " <table id=\"fusionTable\" style=\"width: 100%;\">\n"+
     " <tbody>\n"+
     " <tr>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">valueA</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">predicate</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">valueB</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Action</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
     " </tr>\n"+
     " <tr>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">valueA</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">predicate</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">valueB</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Action</td>\n"+
     " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\"><select style=\"width: 100%;\"></select></td>\n"+
     " </tr>\n"+
     " </tbody></table>"*/

    //alert(document.getElementById("link_tooltip").innerHTML);
}

var unlinkedEntityStyleA = new OpenLayers.Style(
        {strokeColor: "yellow",
            strokeWidth: 3,
            cursor: "pointer",
            fillColor: "yellow",
            fillOpacity: 0.5,
            title: 'luda'}
);
var unlinkedEntityStyleB = new OpenLayers.Style(
        {strokeColor: "orange",
            strokeWidth: 3,
            cursor: "pointer",
            fillColor: "orange",
            fillOpacity: 0.5,
            title: 'luda'}
);

function addUnlinkedMapDataJsonWithLinks(jsonentitiess) {
    alert(jsonentitiess.dataset);
    $('#valAllButton').data("dataset", jsonentitiess.dataset);
    alert($('#valAllButton').data("dataset"));
    if (jsonentitiess.dataset == "A") {
        $.each(jsonentitiess.entitiesA, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureA = wkt.read(element);
            var links = [];
            polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
            polygonFeatureA.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureA)};

            vectorsA.addFeatures([polygonFeatureA]);
        });
        $.each(jsonentitiess.entitiesB, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureB = wkt.read(element);
            var links = [];
            polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
            polygonFeatureB.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureB)};

            vectorsB.addFeatures([polygonFeatureB]);
        });
        $.each(jsonentitiess.links, function (index, element) {
            //console.log("Sub A "+element.subA);
            //console.log("Sub B "+element.subB);
            //console.log("Geo A "+element.geomA);
            //console.log("Geo B "+element.geomB);
            
            var featA = null;
            if (!element.geomA) {
                featA = vectorsA.getFeaturesByAttribute("a", element.subA)[0];
            } else {
                featA = wkt.read(element.geomA);
                featA.geometry.transform(WGS84, map.getProjectionObject());
                featA.attributes = {'links': [], 'a': element.subA, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(featA)};
            }
            var featB = null;
            if (!element.geomB) {
                featB = vectorsB.getFeaturesByAttribute("a", element.subB)[0];
            } else {
                featB = wkt.read(element.geomB);
                featB.geometry.transform(WGS84, map.getProjectionObject());
                featB.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(featB)};
            }
            

            var retFeat = createUnvalidatedLink(featA, featB);

            retFeat.jIndex = element.jIndex;
            retFeat.dist = element.dist;
            
            //console.log(featA.attributes.links.length);

            featA.attributes.links[featA.attributes.links.length] = retFeat;
            featA.attributes.a = retFeat.attributes.la.attributes.a;

            featB.attributes.links[featB.attributes.links.length] = retFeat;
            featB.attributes.a = retFeat.attributes.lb.attributes.a;

            vectorsA.addFeatures([featA]);
            vectorsB.addFeatures([featB]);

            //createUnvalidatedLink(featA, featB);
        });
    } else {
        $.each(jsonentitiess.entitiesB, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureA = wkt.read(element);
            var links = [];
            polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
            polygonFeatureA.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureA)};

            vectorsA.addFeatures([polygonFeatureA]);
        });
        $.each(jsonentitiess.entitiesA, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureB = wkt.read(element);
            var links = [];
            polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
            polygonFeatureB.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureB)};

            vectorsB.addFeatures([polygonFeatureB]);
        });
        $.each(jsonentitiess.links, function (index, element) {
            var featA = null;
            if (!element.geomB) {
                featA = vectorsA.getFeaturesByAttribute("a", element.subB)[0];
            } else {
                featA = wkt.read(element.geomB);
                featA.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(featA)};
                featA.geometry.transform(WGS84, map.getProjectionObject());
            }
            var featB = null;
            if (!element.geomA) {
                featB = vectorsB.getFeaturesByAttribute("a", element.subA)[0];
            } else {
                featB = wkt.read(element.geomA);
                featB.attributes = {'links': [], 'a': element.subA, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(featB)};
                featB.geometry.transform(WGS84, map.getProjectionObject());
            }
            
            var retFeat = createUnvalidatedLink(featA, featB);

            retFeat.jIndex = element.jIndex;
            retFeat.dist = element.dist;
            
            //console.log(retFeat.attributes.la.attributes.a);

            featA.attributes.links[featA.attributes.links.length] = retFeat;
            featA.attributes.a = retFeat.attributes.la.attributes.a;

            featB.attributes.links[featB.attributes.links.length] = retFeat;
            featB.attributes.a = retFeat.attributes.lb.attributes.a;

            vectorsA.addFeatures([featA]);
            vectorsB.addFeatures([featB]);

            //createUnvalidatedLink(featA, featB);
        });
    }
}
    
function addUnlinkedMapDataJson(jsonentitiess) {
    $.each(jsonentitiess.entitiesA, function (index, element) {
        var polygonFeatureA = wkt.read(element.geom);
        var links = [];
        polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureA.attributes = {'links': links, 'a': element.sub, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureA)};

        vectorsA.addFeatures([polygonFeatureA]);
    });
    $.each(jsonentitiess.entitiesB, function (index, element) {
        var polygonFeatureB = wkt.read(element.geom);
        var links = [];
        polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureB.attributes = {'links': links, 'a': element.sub, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeatureB)};

        vectorsB.addFeatures([polygonFeatureB]);
    });
}

function addMapDataJson(jsongeoms) {
    $.each(jsongeoms.linked_ents, function (index, element) {
        //alert(element.subA);

        var polygonFeatureA = wkt.read(element.geomB);
        polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureA.attributes = {'a': element.subB, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': wkt.write(polygonFeatureA)};

        var polygonFeatureB = wkt.read(element.geomA);
        polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureB.attributes = {'a': element.subA, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': wkt.write(polygonFeatureB)};

        //alert(polygonFeatureA.geometry.getCentroid());
        //alert(polygonFeatureB.geometry.getCentroid());

        var start_point = polygonFeatureA.geometry.getCentroid(true);
        var end_point = polygonFeatureB.geometry.getCentroid(true);
        var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
        linkFeature.attributes = {'la': polygonFeatureA, 'a': element.subA, 'lb': polygonFeatureB, 'cluster': 'Unset', 'opacity': 0.8};
        linkFeature.prev_fused = false;
        linkFeature.validated = true;
        //alert(linkFeature.attributes.la);
        //alert(linkFeature.fid);
        //alert(linkFeature.attributes.la);
        vectorsLinks.addFeatures([linkFeature]);
        vectorsA.addFeatures([polygonFeatureB]);
        vectorsB.addFeatures([polygonFeatureA]);
    });
    //alert('done');
}

function addMapData(geom) {
    var step = 0;
    var prev = 0;
    var first;
    var second;
    var third;
    var fourth;
    var polygonFeatureA;
    var polygonFeatureB;

    var geoms = geom.split("::");
    //alert(geoms[0]);
    for (var i = 0; i < geoms[0].length; i++)
    {
        if (geoms[0].charAt(i) == ';') {
            if (step === 0) {
                first = geoms[0].substring(prev, i);
                //alert("first "+first);
                prev = i + 1;
                step++;
            } else if (step === 1) {
                second = geoms[0].substring(prev, i);
                polygonFeatureA = wkt.read(second);
                polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
                polygonFeatureA.attributes = {'a': first, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': second};
                
                vectorsB.addFeatures([polygonFeatureA]);
                prev = i + 1;
                step++;
            } else if (step === 2) {
                third = geoms[0].substring(prev, i);
                //alert("third "+third);
                prev = i + 1;
                step++;
            } else {
                fourth = geoms[0].substring(prev, i);
                //alert(second);
                polygonFeatureB = wkt.read(fourth);
                polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
                polygonFeatureB.attributes = {'a': third, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': fourth};
                
                var start_point = polygonFeatureA.geometry.getCentroid(true);
                var end_point = polygonFeatureB.geometry.getCentroid(true);
                
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                var links = [];
                links.push(linkFeature);
                polygonFeatureA.attributes = {'links': links, 'a': first, 'cluster': 'Unset', 'opacity': 0.3,  'oGeom': wkt.write(polygonFeatureA)};
                polygonFeatureB.attributes = {'links': links, 'a': third, 'cluster': 'Unset', 'opacity': 0.3,  'oGeom': wkt.write(polygonFeatureB)};
                linkFeature.attributes = {'a': third, 'b': first, 'la': polygonFeatureB, 'lb': polygonFeatureA, 'cluster': 'Unset', 'opacity': 0.8};
                linkFeature.prev_fused = false;
                linkFeature.validated = true;
                //console.log(linkFeature.attributes.la + " " + linkFeature.attributes.lb);
                vectorsLinks.addFeatures([linkFeature]);
                vectorsA.addFeatures([polygonFeatureB]);

                prev = i + 1;
                step = 0;
            }
        }
    }
}