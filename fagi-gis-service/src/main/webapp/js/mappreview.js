// Map
var map;

// WKT
var wkt;

 // Vector Layer Styles
var styleA = {
                strokeColor: "blue",
                strokeWidth: 1,
                pointRadius: 1,
                cursor: "pointer",
                title: 'second',
                fillColor: "blue",
                fillOpacity: 0.5
            }; 
var styleB = {
                strokeColor: "green",
                cursor: "pointer",
                fillColor: "green",
                strokeOpacity: 0.5,
                strokeWidth: 3,
                title: 'first'
             };
var styleLinks = {
                    strokeColor: "red",
                    cursor: "pointer",
                    fillColor: "red",
                    pointRadius: 1,
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    title: 'first',
                    fillOpacity: 0.5
                 };
                 
// MAp Layers
var vectorsA = new OpenLayers.Layer.Vector('Dataset A Layer',{isBaseLayer: false, style: styleA});
var vectorsB = new OpenLayers.Layer.Vector('Dataset B Layer', {isBaseLayer: false, style: styleB});
var vectorsLinks = new OpenLayers.Layer.Vector('Links Layer', {isBaseLayer: false, style: styleLinks});

// Map controls
 var dragControlA;
 var dragControlB;
 var selectControl;
 
// Projection of geometry for storing in Post-GIS
var WGS84 = new OpenLayers.Projection("EPSG:4326");

// Currently enabled shifting action
var transType = 1;
var MOVE_TRANS = 1;
var ROTATE_TRANS = 2;
var SCALE_TRANS = 3;

// Disable caching for AJAX calls
$.ajaxSetup({ cache: false });

// Helper variables
var mouse = {x: 0, y: 0};
var current_tip = {x: 0, y: 0};
var current_feature;
var feature_is_selected = false;

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
$( "#dialog" ).dialog({
  position: { 
    my: 'left top',
    at: 'left top',
    of: $('#map')
  },
  //autoOpen: false,
  draggable: true
});

//$( "p" ).addClass("highlight");
//$( "p" ).css("text-color", "blue");
//$(".ui-dialog-titlebar").css("background", "blue");
$(".ui-dialog").css("background", "black");
$(".ui-dialog").css("z-index", "99999999");
//$(".ui-dialog").css("left", "0");
//$(".ui-dialog").css("top", "0");
$(".ui-dialog").css("float", "left");
$(".ui-dialog").css("width", "0%");
$(".ui-dialog").css("height", "95%");
//$(".ui-dialog").parent().css("position", "relative");
//$(".ui-dialog").css("position", "relative");
//$(".ui-dialog").css("postion", "absolute");
//$(".ui-dialog").css("padding-left", "5px");
//$(".ui-dialog").css("padding-top", "5px");
$(".dropdown-menu").css("background", "black");
$(".dropdown-menu").mouseleave(function(){
      $(".dropdown").removeClass("open");
    });
$(".dropdown").mouseleave(function(){
      $(".dropdown").removeClass("open");
    });
$(".dropdown").mouseover(function(){
      $(".dropdown").addClass("open");
    });
$(".dropdown-menu").mouseover(function(){
      $(".dropdown").addClass("open");
    });
    
   var options = {
        numZoomLevels: 32,
        projection: "EPSG:3857",
        maxExtent: new OpenLayers.Bounds(-200000, -200000, 200000, 200000),
        center: new OpenLayers.LonLat(-12356463.476333, 5621521.4854095)
    };
    //map = new OpenLayers.Map("map", options);
    var map_controls = [ new OpenLayers.Control.OverviewMap(), new OpenLayers.Control.LayerSwitcher() ];
    map = new OpenLayers.Map("map", {
        transitionEffect: null, 
         zoomMethod: null ,
  projection: new OpenLayers.Projection("EPSG:900913")
});


var myBaseLayer = new OpenLayers.Layer.Google("Google Satellite",
              {'sphericalMercator': true,
                  'numZoomLevels': 32,
                  'type': google.maps.MapTypeId.SATELLITE,
               'maxExtent': new OpenLayers.Bounds(-20037508.34,-20037508.34,20037508.34,20037508.34)
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
    document.getElementById('map').oncontextmenu = function(e){
    e = e?e:window.event;
    if (e.preventDefault) e.preventDefault(); // For non-IE browsers.
        else return false; // For IE browsers.
    };
    
    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {                

defaultHandlerOptions: {
'single': true,
'double': true,
'pixelTolerance': 0,
'stopSingle': false,
'stopDouble': false
},
handleRightClicks:true,
initialize: function(options) {
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

// Add an instance of the Click control that listens to various click events:
var oClick = new OpenLayers.Control.Click({eventMethods:{
'rightclick': function(e) {
alert('rightclick at '+e.xy.x+','+e.xy.y);
}
}});
map.addControl(oClick);
oClick.activate();

inside_panel.addControls([zoom_max_inside]);
var myBaseLayer2 = new OpenLayers.Layer.Google("Google Streets",
              {'sphericalMercator': true,
                  'numZoomLevels': 32,
               'maxExtent': new OpenLayers.Bounds(-20037508.34,-20037508.34,20037508.34,20037508.34)
              });
map.addLayer(myBaseLayer);

    map.events.register("mousemove", map, function (e) {            
        mouse.x = e.pageX; 
        mouse.y = e.pageY; 
        //alert(mouse.x+' '+mouse.y);
    });
    map.events.register("mouseover", map, function (e) {
        //alert("mousein");
    });
    map.events.on({ "zoomend": function(){
        //alert("mouseroll");
    }});
    map.zoomToProxy = map.zoomTo;
    map.zoomTo =  function (zoom,xy){
        // if you want zoom to go through call
        //alert(zoom);
        //alert(xy)
;       // 
        if ( transType == MOVE_TRANS ) {
            map.zoomToProxy(zoom,xy); 
            document.getElementById("link_tooltip").style.opacity = 0;
            document.getElementById("link_tooltip").style.display = 'none';
            document.getElementById("transformSelect").style.opacity = 0;
            document.getElementById("transformSelect").style.display = 'none';
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
                    alert(evt);
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
    wkt = new OpenLayers.Format.WKT();
    map.addLayer(new OpenLayers.Layer.OSM());
    //create a style object
    
    // allow testing of specific renderers via "?renderer=Canvas", etc
                var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
                renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;

                vectors = new OpenLayers.Layer.Vector("Vector Layer", {isBaseLayer: false,
                    renderers: renderer
                });

                
                map.addControl(new OpenLayers.Control.LayerSwitcher());
                map.addControl(new OpenLayers.Control.MousePosition());

                /*controls = {
                    point: new OpenLayers.Control.DrawFeature(vectors,
                                OpenLayers.Handler.Point),
                    line: new OpenLayers.Control.DrawFeature(vectors,
                                OpenLayers.Handler.Path),
                    polygon: new OpenLayers.Control.DrawFeature(vectors,
                                OpenLayers.Handler.Polygon),
                    drag: new OpenLayers.Control.DragFeature(vectors)
                };

                for(var key in controls) {
                    //alert(key);
                    //map.addControl(controls[key]);
                }*/
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
    //map.addLayer(vectors);
  //map.setLayerIndex(vectors,1);
  map.setLayerIndex(vectorsA,2);
  map.setLayerIndex(vectorsB,3);
  map.setLayerIndex(vectorsLinks,4);
        var lays = [vectorsA, vectorsB, vectorsLinks];
        selectControl = new OpenLayers.Control.SelectFeature(lays, {highlightOnly: true});
            map.addControl(selectControl);
            
            selectControl.activate();
            //alert(map.events.BROWSER_EVENTS);
            vectorsA.events.on({
                'featureselected': onFeatureSelect,
                'featureunselected': onFeatureUnselect,
                'mousemove': onFeatureOver2,
                scope: vectorsA
           });
           vectorsB.events.on({
                'featureselected': onFeatureSelect,
                'featureunselected': onFeatureUnselect,
                'mousemove': onFeatureOver2,
                scope: vectorsB
           });
           vectorsLinks.events.on({
                'featureselected': onLinkFeatureSelect,
                'featureunselected': onLinkFeatureUnselect,
                scope: vectorsLinks
           });
         /*
        selectControl2 = new OpenLayers.Control.SelectFeature(vectors2);
            map.addControl(selectControl2);
            selectControl2.activate();
            vectors2.events.on({
                'featureselected': onFeatureSelect
           });*/

//alert(map.projection);
    //alert(map.displayProjection);
   
        //var polygonFeature = wkt.read("POLYGON(15.37412 51.32847,15.374159 51.328592,15.374441 51.328552,15.374586 51.328532,15.374659 51.328521,15.37462 51.328399,15.37412 51.32847)");
        var polygonFeature = wkt.read("POLYGON((20 37, 20 39, 22 39, 22 37, 20 37))");
        var polygonFeatureW = wkt.read("POLYGON((24 41, 24 43, 26 43, 26 41, 24 41))");
        //var polygonFeature = wkt.read("POINT(-25.8203125 2.4609375)");
        polygonFeature.geometry.transform(WGS84, map.getProjectionObject());
        vectorsA.addFeatures([polygonFeature]);
        //alert("OL");
        var polygonFeature2 = wkt.read("POLYGON((20.7240456428877 37.9908366236946,20.7241422428877 37.9906675236946,20.7238686428877 37.9905829236946,20.7238364428877 37.9908197236946,20.7240456428877 37.9908366236946))");
        polygonFeatureW.geometry.transform(WGS84, map.getProjectionObject());
        vectorsB.addFeatures([polygonFeatureW]);
        
        var start_point = polygonFeature.geometry.getCentroid(true);
        var end_point = polygonFeatureW.geometry.getCentroid(true);
        var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
        linkFeature.attributes = {'la': polygonFeature,  'a': 'aalala', 'lb': polygonFeatureW};
        var links = [];
        links.push(linkFeature);
        polygonFeature.attributes = {'links': links};
        polygonFeatureW.attributes = {'links': links};
        vectorsLinks.addFeatures([linkFeature]);
        /*window.setInterval(function() {rotateFeature(
                               polygonFeature, 360 / 20, polygonFeature.geometry.getCentroid(true));}, 100);*/
    map.zoomToMaxExtent();
    
    });

var dialogOpened = false;
function testAnim() {
    hideAllPanels();
    $("#connectionPanel").show();

var int=self.setInterval(periodicUpdate, 1);
if ( ! dialogOpened ) {
    $("#map").animate({
       width: '70%'
    }, { duration: 1000, queue: false, complete: function() { map.updateSize(); window.clearInterval(int);return false; }});
    $(".ui-dialog").animate({
       width: '33%'
    }, { duration: 1000, queue: false, complete: function() { return false; }});
    dialogOpened = true;
    } else {
        $("#map").animate({
       width: '100%'
    }, { duration: 1000, queue: false, complete: function() { map.updateSize(); window.clearInterval(int);return false; }});
    $(".ui-dialog").animate({
       width: '0%'
    }, { duration: 1000, queue: false, complete: function() { return false; }});
    dialogOpened = false;
    }
}

function hideAllPanels() {
    $("#connectionPanel").hide();
    $("#datasetPanel").hide();
    //$("#fusionPanel").hide();
    $("#linksPanel").hide();
}

function testAnim2() {
    hideAllPanels();
    $("#datasetPanel").show();
}

function expandLinksPanel() {
    hideAllPanels();
    $(".ui-dialog").animate({
       width: '70%'
    }, { duration: 1000, queue: false, complete: function() { return false; }});
    $("#linksPanel").show();
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
    if ( transType == ROTATE_TRANS ) {
    
    } else if ( transType == SCALE_TRANS ) {
            
    } 
}

var selectedGeom = null;
var selectedGeomA = null;
var selectedGeomB = null;
var lastPiuxel;

function luda2(e) {
    alert('a');
}

var activeFeature =  null;
function luda(event, deltaZ) {
    var angle = 5.0;
    var scale = 2.0;
    document.getElementById("transformSelect").style.opacity = 0.0;
    document.getElementById("transformSelect").style.display = 'none';
    if ( transType == ROTATE_TRANS ) {
        if ( deltaZ < 0 )
            angle = -angle;
        activeFeature.geometry.rotate(angle, activeFeature.geometry.getCentroid(true));
        activeFeature.layer.drawFeature(activeFeature);
        //alert(angle);
    } else if ( transType == SCALE_TRANS ) {
        if ( deltaZ < 0 )
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
        
// Keep track of the selected features 
function addSelected(feature) {
    alert('Select');
    selectedFeatures.push(feature);
}

// Clear the list of selected features 
function clearSelected(feature) {
    alert('Delete');
    selectedFeatures = [];
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
   if ( selectedGeomA == null ) {
       selectedGeomA = feature;
   }
   lastPixel = pixel;
}

// Feature moving 
function doDragA(feature, pixel) {
    //alert('Do drag');
    if ( selectedGeomA != null ) {
        //alert('nick');
        //if (feature != selectedGeomA ) {
        //alert('nick');
            vectorsLinks.destroyFeatures([selectedGeomA.attributes.links[0]]);
            var start_point = selectedGeomA.geometry.getCentroid(true);
            var end_point = selectedGeomA.attributes.links[0].attributes.lb.geometry.getCentroid(true);
        //alert(end_point);
        //alert(start_point);
        //alert(end_point);
            var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
            linkFeature.attributes = {'la': selectedGeomA.attributes.links[0].attributes.la,  'a': selectedGeomA.attributes.links[0].attributes.a, 'lb': selectedGeomA.attributes.links[0].attributes.lb};
            linkFeature.style = {
                strokeColor: "red",
                    cursor: "pointer",
                    fillColor: "red",
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    fillOpacity: 0.5,
                title: selectedGeomA.attributes.links[0].attributes.a };
        selectedGeomA.attributes.links[0] = linkFeature;
            vectorsLinks.addFeatures([linkFeature]);
            //alert('nick');
            var res = map.getResolution();
            selectedGeomA.geometry.move(pixel.x, pixel.y);
            vectorsA.drawFeature(selectedGeomA);
            vectorsLinks.drawFeature(linkFeature);
            //alert('nick');
        //}
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragA(feature, pixel) {
    if ( selectedGeomA != null ) {
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
   if ( selectedGeomB == null ) {
       selectedGeomB = feature;
   }
   lastPixel = pixel;
}

// Feature moving 
function doDragB(feature, pixel) {
    //alert('Do drag');
    if ( selectedGeomB != null ) {
        //if (feature != selectedGeomB ) {
            vectorsLinks.destroyFeatures([selectedGeomB.attributes.links[0]]);
            var start_point = selectedGeomB.geometry.getCentroid(true);
            var end_point = selectedGeomB.attributes.links[0].attributes.la.geometry.getCentroid(true);
            var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
            linkFeature.attributes = {'la': selectedGeomB.attributes.links[0].attributes.la,  'a': selectedGeomB.attributes.links[0].attributes.a, 'lb': selectedGeomB.attributes.links[0].attributes.lb};
            linkFeature.style = {
                strokeColor: "red",
                    cursor: "pointer",
                    fillColor: "red",
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    fillOpacity: 0.5,
                title: selectedGeomB.attributes.links[0].attributes.a };
            selectedGeomB.attributes.links[0] = linkFeature;
            vectorsLinks.addFeatures([linkFeature]);
            var res = map.getResolution();
            //selectedGeomB.geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
            vectorsB.drawFeature(selectedGeomB);
            vectorsLinks.drawFeature(linkFeature);
            //alert('sth fishy');
        //}
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragB(feature, pixel) {
    if ( selectedGeomB != null ) {
        selectedGeomB.geometry.transform(map.getProjectionObject(), WGS84);
        alert('End drag '+wkt.write(selectedGeomB));
        //alert('End drag '+wkt.write(selectedGeomB.linls[0]));
        selectedGeomB.geometry.transform(WGS84, map.getProjectionObject());
        //selectedGeomB.state = OpenLayers.State.UPDATE;
        selectedGeomB = null;
    }
}

function fuseVisible() {
    var bounds = map.calculateBounds();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseVisibleServlet",
        // the data to send (will be converted to a query string)
        data: { "left": bounds.left,
                "bottom": bounds.bottom,
                "right": bounds.right,
                "top": bounds.top },
        // the type of data we expect back
        dataType : "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            //$('#connLabel').text(responseText);
            addMapDataJson(responseJson);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            alert( "Sorry, there was a problem!" );
            console.log( "Error: " + errorThrown );
            console.log( "Status: " + status );
            console.dir( xhr );
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
            //$('#connLabel').text("connected");
        }
    });
}

$("#link_tooltip").bind("transitionend webkitTransitionEnd oTransitionEnd MSTransitionEnd", function() {
    if (!feature_is_selected) {
        document.getElementById("link_tooltip").style.display = 'none';
    }
});

Proj4js.defs["EPSG:3035"] = "+proj=laea +lat_0=52 +lon_0=10 +x_0=4321000 +y_0=3210000 +ellps=GRS80 +units=m +no_defs";

var epsg900913 = new OpenLayers.Projection('EPSG:900913');
var epsg3035   = new OpenLayers.Projection("EPSG:4326");
//var epsg900913 = new OpenLayers.Projection('EPSG:3035');
//var epsg3035   = new OpenLayers.Projection('EPSG:900913');

function onFeatureOver (event) {
    //alert('Over');
}

function onFeatureOver2 (event) {
    alert('Over 2');
}

function onFeatureUnselect (event) {
    document.getElementById("transformSelect").style.opacity = 0.0;
    //alert("Nick");
    document.getElementById("transformSelect").style.display = 'none';
    activeFeature = null;
}

function setTransformation () {
    //alert('Set Transformation');
    //var nameValue = document.getElementById("transForm").value;
    //alert($('input[name=t]:checked', '#transForm').val());
    if ($('input[name=t]:checked', '#transForm').val() == 'tra') {
        transType = MOVE_TRANS;
        dragControlA.activate();
        dragControlB.activate();
    } else if ( $('input[name=t]:checked', '#transForm').val() == 'rot') {
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
    //alert('Select');
    //selectedFeatures.push(feature);}
    document.getElementById("transformSelect").style.opacity = 0.8;
    //alert("Nick");
    document.getElementById("transformSelect").style.display = 'inline';
    document.getElementById("transformSelect").style.left = mouse.x+'px';
    document.getElementById("transformSelect").style.top = mouse.y+'px';
    alert('nick');
    var inside = " <div class=\"checkboxes\">\n"+
            "<label> Apply Transformations </label>"+
            "<form id=\"transForm\" action=\"\">"+
" <label for=\"t1\"><input type=\"radio\" name=\"t\" id=\"t1\" value=\"tra\" />Move</label><br />\n"+
" <label for=\"t2\"><input type=\"radio\" name=\"t\" id=\"t2\" value=\"sca\" />Scale</label><br />\n"+
" <label for=\"t3\"><input type=\"radio\" name=\"t\" id=\"t3\" value=\"rot\" />Rotate</label><br />\n"+
" </form></div>\n";
    //document.getElementById("t1").onclick = setTransformation;
    //document.getElementById("t2").onclick = setTransformation;
    //document.getElementById("t3").onclick = setTransformation;
    document.getElementById("transformSelect").innerHTML = inside;
    $("#t1").click(setTransformation);
    $("#t2").click(setTransformation);
    $("#t3").click(setTransformation);
    
    activeFeature = event.feature;
}
    
function onFeatureUnselectB (event) {
    alert('Unselect B');
}

function onFeatureSelectB(event) {
    alert('Select B');
}

function onFeatureUnselectA (event) {
    alert('Unselect A');
}

function onFeatureSelectA(event) {
    alert('Select A');
}

function onLinkedFeatureUnselect(event) {
    document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;   
}

function onLinkFeatureSelect(event) {
    // fetch the cluster's latlon and set the map center to it and call zoomin function
    // which takes you to a one level zoom in and I hope this solves your purpose :)    
    //map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    //map.zoomToExtent(event.feature.geometry.getBounds(), true);
    //map.zoomIn();
    //alert('tomas');
    //$("testid2").html("your new header");
    //$("testid2").html('your new header');
    //document.getElementById("testid2").innerText = "public offers";
    //document.getElementById("testid2").innerHTML = event.feature.style.title;
    
    if ( event.feature.prev_fused === true ) {
        feature_is_selected = true;  
        event.feature.attributes.la.style.display = 'inline';
        event.feature.attributes.lb.style.display = 'inline';
        event.feature.attributes.la.style.fillOpacity = 0.35
        event.feature.attributes.lb.style.fillOpacity = 0.35;
        event.feature.attributes.la.style.strokeOpacity = 0.35;
        event.feature.attributes.lb.style.strokeOpacity = 0.35;
        
        vectorsA.redraw();
        vectorsB.redraw();
        //vectorsLinks.refresh();
    
        //alert("should have");
    }
    feature_is_selected = true;    
    //alert("Feature "+event.feature.attributes.a);
    //alert("Feature "+event.feature.attributes.la);
    current_feature = event.feature;
    
    var sendData = new Array();
    sendData[sendData.length] = event.feature.attributes.a;
    var list = document.getElementById("matchList");
    var listItem = list.getElementsByTagName("li");
    var inputItem = listItem[0].getElementsByTagName("input");      
    sendData[sendData.length] = inputItem[0].value;
    sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";
    for (var i=1; i < listItem.length; i++) {
        var inputItem = listItem[i].getElementsByTagName("input");      
        //alert('Long name = '+listItem[i].long_name);
        sendData[sendData.length] = inputItem[0].value;
        //alert(selectedProperties['id'+(i-1)]);
        sendData[sendData.length] = listItem[i].long_name;
        //alert(sendData[sendData.length-1]);
    }
    
    list = document.getElementById("linkMatchList");
    listItem = list.getElementsByTagName("li");
    for (var i=0; i < listItem.length; i++) {
        var inputItem = listItem[i].getElementsByTagName("input");      
        //alert('Long name = '+listItem[i].long_name);
        sendData[sendData.length] = inputItem[0].value;
        //alert(selectedProperties['id'+(i-1)]);
        sendData[sendData.length] = listItem[i].long_name;
        //alert(sendData[sendData.length-1]);
    }
    //alert(event.feature.attributes.a);
    //alert("done");
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "LinkSchemasServlet",
        // the data to send (will be converted to a query string)
        data: { "subject" : event.feature.attributes.a },
        // the type of data we expect back
        dataType : "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            //$('#connLabel').text(responseJson);
            linkMatchesJSON = responseJson;
            $('#linkNameA').text(responseJson.p.nodeA);            
            $('#linkNameB').text(responseJson.p.nodeB);            
            document.getElementById("linkMatchList").innerHTML = "";
            var schemaListA = document.getElementById("linkSchemasA");
            schemaListA.innerHTML = "";
            $.each(responseJson.p.propsFullA, function(index, element) {
                if (element.short_rep.indexOf("posSeq") >= 0) {
                    return;
                }
                var opt = document.createElement("li");
                var optlbl = document.createElement("label");
                optlbl.innerHTML = "";
                opt.innerHTML = element.short_rep;
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
            $.each(responseJson.p.propsFullB, function(index, element) {
                if (element.short_rep.indexOf("posSeq") >= 0) {
                    return;
                }
                var opt = document.createElement("li");
                var optlbl = document.createElement("label");
                optlbl.innerHTML = "";
                opt.innerHTML = element.short_rep;
                opt.long_name = element.long_rep;
                opt.onclick = linkPropSelectedB;                
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListB.appendChild(opt);
            });
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            alert( "Sorry, there was a problem with the first AJAX" );
            console.log( "Error: " + errorThrown );
            console.log( "Status: " + status );
            console.dir( xhr );
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
            //$('#connLabel').text("connected");
        }
    });
    
    //alert("done");
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FusionServlet",
        // the data to send (will be converted to a query string)
        data: {props:sendData},
        // the type of data we expect back
        dataType : "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            //$('#connLabel').text(responseJson);
            fusionPanel(event, responseJson);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            alert( "Sorry, there was a problem with the second AJAX" );
            console.log( "Error: " + errorThrown );
            console.log( "Status: " + status );
            console.dir( xhr );
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
            //$('#connLabel').text("connected");
        }
    });
}

/*
 * 
 * @param {type} val
 * @returns {undefined}
 */
        
function fusionPanel( event, val ) {
    //alert("Event "+event);
    //alert("Val "+val);
    //vectorsLinks.destroyFeatures([event.feature]);
    /*
     var prev = 0;
            document.getElementById("linksList").innerHTML = "<li></li>";
            for ( var i = 0; i < responseText.length; i++ )
            {
                if(responseText.charAt(i) == ',') {
                    var link = responseText.substring(prev, i);
                    var node=document.createElement("li");
                    var text = '<label><input type="checkbox">'+link+'<label>';
                    alert(text);
                    node.innerHTML = text;
                    document.getElementById("linksList").appendChild(node);
    
                    prev = i + 1;
                }
            }
     */
    
    //current_tip = mouse;
        
    //alert("Feature : "+vent.feature.attributes.a);
    
    var geom_typeA = val.geomsA[0].substring(0, val.geomsA[0].indexOf("("));
    var geom_typeB = val.geomsB[0].substring(0, val.geomsB[0].indexOf("("));
    var avail_trans = "";
    var avail_meta_trans = "";
    $.each(val.geomTransforms, function(index, element) {
        avail_trans += "<option>"+element+"</option>";
    });
    $.each(val.metaTransforms, function(index, element) {
        avail_meta_trans += "<option>"+element+"</option>";
    });
    
    var s="<p class=\"geoinfo\" id=\"link_name\">Ludacris</p>\n"+
//" <div class=\"checkboxes\">\n"+
//" <label for=\"chk1\"><input type=\"checkbox\" name=\"chk1\" id=\"chk1\" />Flag as misplaced fusion</label><br />\n"+
//" </div>\n"+
//" Description: <textarea name=\"textarea\" style=\"width:99%;height:50px;\" class=\"centered\"></textarea>\n"+
" <table border=1 id=\"fusionTable\" style=\"width: 100%;>\n"+
" <tr>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\"> </td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Value from "+$('#idDatasetA').val()+"</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Predicate</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Value from "+$('#idDatasetB').val()+"</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Action</td>\n"+
//" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
" </tr>\n"+
" <tr>\n"+
" <td title=\""+val.geomsA[0]+"\" style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+geom_typeA+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">asWKT</td>\n"+
" <td title=\""+val.geomsB[0]+"\" style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+geom_typeB+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\"><select id=\"geoTrans\" style=\"width: 100%;\">"+avail_trans+"</select></td>\n"+
//" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Fused Geom</td>\n"+
" </tr>\n"+
" </table>"+
" <table border=0 id=\"shiftPanel\">"+
" <tr>\n"+
" <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Shift (%):</td>\n"+
" <td style=\"width:10px; text-align: center;\" align=\"left\" valign=\"bottom\"><input style=\"width:50px;\" type=\"text\" id=\"shift\" name=\"shift\" value=\"100\"/></td>\n"+
" <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Scale:</td>\n"+
" <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"scale_fac\" name=\"x_scale\" value=\"1.0\"/></td>\n"+
" <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Rotate:</td>\n"+
" <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"rotate_fac\" name=\"x_rotate\" value=\"0.0\"/></td>\n"+
//" <td style=\"width:21px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
" </tr>\n"+
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
" </table>"+
" <input id=\"fuseButton\" type=\"submit\" value=\"Fuse\" style=\"float:right\" onclick=\"return false;\"/>\n";

    document.getElementById("link_tooltip").innerHTML = s;
    var tbl = document.getElementById("fusionTable");
    $('#fuseButton').click(performFusion);
    //alert ("props");
    //alert (val['properties']);
    //alert (val.properties);
    //$.each(val, function(index, element) {
    if (typeof val.properties !== "undefined") {
    // Works
        $.each(val.properties, function(index1, element1) {
        //alert(index1 +' '+ element1);
            //$.each(element1, function(index11, element11) {
            //alert("PROP "+element1["property"]);
           //alert("PROP 2" + element1.property);
                var opt = document.createElement("tr");
                opt.long_name = element1.propertyLong;
                //alert("OPT NAME "+opt.long_name);
                var trunc_pos = element1.property.lastIndexOf("#");
                var trunc = element1.property;
                if (trunc_pos < 0)
                    trunc_pos = element1.property.lastIndexOf("/");
                if (trunc_pos >= 0)
                    trunc = element1.property.substring(trunc_pos+1);
                var entry = " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+element1.valueA+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+trunc+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+element1.valueB+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\"><select style=\"width: 100%;\">"+avail_meta_trans+"</select></td>\n"
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
    document.getElementById("link_tooltip").style.opacity = 0.8;
    //alert("Nick");
    document.getElementById("link_tooltip").style.display = 'inline';
    document.getElementById("link_tooltip").style.left = mouse.x+'px';
    document.getElementById("link_tooltip").style.top = mouse.y+'px';
    document.getElementById("link_name").innerHTML = event.feature.attributes.a;
    
    $('#scale_fac').attr('disabled', 'disabled');
    $('#rotate_fac').attr('disabled', 'disabled');
    $('#shift').attr('disabled', 'disabled');
    
    $('#geoTrans').change(function() {
        //alert( $(this).find("option:selected").text() );      
        var selection = $(this).find("option:selected").text();
        if ( selection === "ShiftAToB" || selection === "ShiftBToA" ) {
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
    /*alert($('#scale_fac').prop('disabled'));
    alert($('#rotate_fac').prop('disabled'));
    alert($('#x_coord').prop('disabled'));
    alert($('#y_coord').prop('disabled'));
    alert('so close 1');*/
         
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
        //current_feature.attributes.la.geometry.transform(map.getProjectionObject(), WGS84);
        //current_feature.attributes.lb.geometry.transform(map.getProjectionObject(), WGS84);
    //alert(wkt.write(current_feature.attributes.la));
        //alert(wkt.write(current_feature.attributes.lb));
        //shiftValuesJSON.geoA = wkt.write(current_feature.attributes.la);
        //shiftValuesJSON.geoB = wkt.write(current_feature.attributes.lb);
        //current_feature.attributes.la.geometry.transform(WGS84, map.getProjectionObject());
        //current_feature.attributes.lb.geometry.transform(WGS84, map.getProjectionObject());
    if ( !$('#scale_fac').prop('disabled') ) {
        /*alert($('#scale_fac').val());
        alert($('#rotate_fac').val());
        alert($('#x_coord').val());
        alert($('#y_coord').val());*/
        //alert(wkt.write(current_feature.attributes.la));
        //alert(wkt.write(current_feature.attributes.lb));
        shiftValuesJSON = new Object();
        shiftValuesJSON.shift = $('#shift').val();
        shiftValuesJSON.scaleFact = $('#scale_fac').val();
        shiftValuesJSON.rotateFact = $('#rotate_fac').val();
    }
    //alert(current_feature == null);
    var geomCells = tblRows[1].getElementsByTagName("td");
    var geomFuse = new Object();
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
        //alert(tmpGeomAction[0].value);
        sendJSON[sendJSON.length] = geomFuse;
    for (var i=2; i < tblRows.length; i++) {
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
            
        for (var j=0; j < cells.length; j++) {            
            //alert(cells[j].getElementsByTagName("select").length);
            //alert(cells[j]);
            var action = cells[j].getElementsByTagName("select");
            //alert(action[0]);
            if (action.length == 1) {
               //alert(action[0].innerHTML);
               //alert(action[0].value);
               var cell = action[0].value;
               sendData[sendData.length] = cell;
            } else if ( j == 1 ) { 
               var cell = cells[j].long_name;
               //alert(cell);
               sendData[sendData.length] = cell;  
            } else {
                var cell = cells[j].innerHTML;
                sendData[sendData.length] = cell;
            };
            //alert(cell);
        }
    }
    
    var sndJSON = JSON.stringify(sendJSON);
    var sndShiftJSON = JSON.stringify(shiftValuesJSON);
    //alert(sndJSON);
    //alert(sndShiftJSON);
    
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseLinkServlet",
        // the data to send (will be converted to a query string)
        data: {props:sendData, propsJSON:sndJSON, factJSON:sndShiftJSON},
        // the type of data we expect back
        dataType : "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            //$('#connLabel').text(responseJson);
            //alert(responseJson);
            previewLinkedGeom( responseJson );
            //fusionPanel(event, responseJson);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            alert( "Sorry, there was a problem!" );
            console.log( "Error: " + errorThrown );
            console.log( "Status: " + status );
            console.dir( xhr );
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
            //$('#connLabel').text("connected");
        }
    });
    
}

function previewLinkedGeom ( resp ) {
    //alert(current_feature);
    //alert(current_feature.attributes.a);
    //alert(current_feature.attributes.la);
    //alert(current_feature.attributes.lb);
    /*current_feature.attributes.la.style.fillOpacity = 0;
    current_feature.attributes.lb.style.fillOpacity = 0;
    current_feature.attributes.la.style.strokeOpacity = 0;
    current_feature.attributes.lb.style.strokeOpacity = 0;*/
    current_feature.attributes.la.style.display = 'none';
    current_feature.attributes.lb.style.display = 'none';
    var linkFeature = wkt.read(resp.geom);
    //alert(resp.geom);
    if( Object.prototype.toString.call( linkFeature ) === '[object Array]' ) {
        //alert('Array');
        for(var i = 0; i < linkFeature.length; i++) {
            linkFeature[i].geometry.transform(WGS84, map.getProjectionObject());
            linkFeature[i].attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb};
            linkFeature[i].style = {
                strokeColor: "red",
                cursor: "pointer",
                fillColor: "red",
                pointRadius: 1,
                strokeOpacity: 0.5,
                strokeWidth: 3,
                fillOpacity: 0.5,
                title: current_feature.attributes.a };
            
            linkFeature[i].prev_fused = true;       
            
            vectorsLinks.addFeatures([linkFeature[i]]);
            //alert('done');
        }
        vectorsLinks.removeFeatures([current_feature]);
    } else {
        
        linkFeature.geometry.transform(WGS84, map.getProjectionObject());
        linkFeature.attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb};
        linkFeature.style = {
            strokeColor: "red",
            cursor: "pointer",
            fillColor: "red",
            pointRadius: 1,
            strokeOpacity: 0.5,
            strokeWidth: 3,
            fillOpacity: 0.5,
            title: current_feature.attributes.a };
            //alert('done feature '+linkFeature);
        linkFeature.prev_fused = true;       
        vectorsLinks.removeFeatures([current_feature]);
        vectorsLinks.addFeatures([linkFeature]);
    }
    
    vectorsA.redraw();
    vectorsB.redraw();
    vectorsLinks.refresh();
    //alert('done');
    current_feature = null;
    feature_is_selected = false;
    document.getElementById("link_tooltip").style.opacity = 0;
    //alert('done');
}

function onLinkFeatureUnselect (event) {
    document.getElementById("link_tooltip").style.opacity = 0;
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
    
    //alert(event.feature.attributes.a);
    document.getElementById("link_tooltip").style.opacity = 0.8;
    document.getElementById("link_tooltip").style.display = 'inline';
    document.getElementById("link_tooltip").style.left = mouse.x+'px';
    document.getElementById("link_tooltip").style.top = mouse.y+'px';
}

function onFeatureUnselectFromLinks (event) {
    document.getElementById("link_tooltip").style.opacity = 0;
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
    document.getElementById("link_tooltip").innerHTML = "Nick";
    //alert("Nick");
    document.getElementById("link_tooltip").style.opacity = 0.8;
    //alert("Nick");
    document.getElementById("link_tooltip").style.opacity = 0.8;
    document.getElementById("link_tooltip").style.display = 'inline';
    document.getElementById("link_tooltip").style.left = mouse.x+'px';
    document.getElementById("link_tooltip").style.top = mouse.y+'px';
}

function addMapDataJson(jsongeoms) {
    $.each(jsongeoms.linked_ents, function(index, element) {
       //alert(element.subA);
        
        var polygonFeatureA = wkt.read(element.geomB);
        polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureA.attributes = {'a': element.subB};
        polygonFeatureA.style = { strokeColor: "blue",
                strokeWidth: 3,
                cursor: "pointer",
                fillColor: "blue",
                fillOpacity: 0.5,
                title: element.subA };
          
        var polygonFeatureB = wkt.read(element.geomA);
        polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
        polygonFeatureB.attributes = {'a': element.subA};
        polygonFeatureB.style = { strokeColor: "black",
                                    pointRadius: 1,
                                    strokeWidth: 5,
                                    cursor: "pointer",
                                    fillColor: "black",
                                        title: element.subB };
         
                //alert(polygonFeatureA.geometry.getCentroid());
                //alert(polygonFeatureB.geometry.getCentroid());
                
        var start_point = polygonFeatureA.geometry.getCentroid(true);
        var end_point = polygonFeatureB.geometry.getCentroid(true);
        var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
        linkFeature.attributes = {'la': polygonFeatureA,  'a': element.subA, 'lb': polygonFeatureB};
        linkFeature.style = { strokeColor: "red",
                                cursor: "pointer",
                                fillColor: "red",
                                strokeOpacity: 0.5,
                                strokeWidth: 3,
                                fillOpacity: 0.5,
                                title: element.subA };
                
        //alert(linkFeature.attributes.la);
        //alert(linkFeature.fid);
        //alert(linkFeature.attributes.la);
        vectorsLinks.addFeatures([linkFeature]);
        vectorsA.addFeatures([polygonFeatureB]);
        vectorsB.addFeatures([polygonFeatureA]);
    });
    //alert('done');
}

function addMapData (geom) {
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
    for ( var i = 0; i < geoms[0].length; i++ )
    {
        if(geoms[0].charAt(i) == ';') {
            if ( step === 0 ) {
                first = geoms[0].substring(prev, i);
                //alert("first "+first);
                prev = i + 1;
                step++;
            } else if ( step === 1) {
                second = geoms[0].substring(prev, i);
                //alert("second "+second);
                polygonFeatureA = wkt.read(second);
                //alert (map.getProjectionObject());
                //alert(map.displayProjection);
                //alert(map.Projection);
                //polygonFeature.geometry.transform(epsg3035, epsg900913);
                polygonFeatureA.geometry.transform(WGS84, map.getProjectionObject());
                //polygonFeatureA.attributes = {'a': first};
                polygonFeatureA.style = {
                strokeColor: "blue",
                strokeWidth: 3,
                cursor: "pointer",
                fillColor: "blue",
                fillOpacity: 0.5,
                title: first };
            
                vectorsB.addFeatures([polygonFeatureA]);
                prev = i + 1;
                step++;
            } else if ( step === 2 ) {
                third = geoms[0].substring(prev, i);
                //alert("third "+third);
                prev = i + 1;
                step++;
            } else {
                fourth = geoms[0].substring(prev, i);
                //alert(second);
                polygonFeatureB = wkt.read(fourth);
                //alert (second);
                //polygonFeature.geometry.transform(epsg3035, epsg900913);
                polygonFeatureB.geometry.transform(WGS84, map.getProjectionObject());
                //polygonFeatureB.attributes = {'a': third};
                polygonFeatureB.style = {
                strokeColor: "black",
                pointRadius: 1,
                strokeWidth: 1,
                cursor: "pointer",
                fillColor: "black",
                title: third };
            
                //alert(polygonFeatureA.geometry.getCentroid());
                //alert(polygonFeatureB.geometry.getCentroid());
                
                var start_point = polygonFeatureA.geometry.getCentroid(true);
                var end_point = polygonFeatureB.geometry.getCentroid(true);
                
                //alert(start_point);
                //alert(end_point);
                
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                var links = [];
                links.push(linkFeature);
                polygonFeatureA.attributes = {'links': links, 'a': first};
                polygonFeatureB.attributes = {'links': links, 'a': third};
                linkFeature.attributes = {'a': third, 'b': first, 'la': polygonFeatureB, 'lb': polygonFeatureA};
                linkFeature.style = {
                strokeColor: "red",
                    cursor: "pointer",
                    fillColor: "red",
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    fillOpacity: 0.5,
                title: third };
                
                vectorsLinks.addFeatures([linkFeature]);
                vectorsA.addFeatures([polygonFeatureB]);
                
                prev = i + 1;
                step = 0;
            }
        }
    }
}