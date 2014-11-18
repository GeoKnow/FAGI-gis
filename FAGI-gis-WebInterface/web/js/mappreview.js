var map;
var wkt;
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
                    strokeOpacity: 0.5,
                    strokeWidth: 3,
                    title: 'first',
                    fillOpacity: 0.5
                 };
                 
var vectorsA = new OpenLayers.Layer.Vector('Dataset A Layer',{isBaseLayer: false, style: styleA});
var vectorsB = new OpenLayers.Layer.Vector('Dataset B Layer', {isBaseLayer: false, style: styleB});
var vectorsLinks = new OpenLayers.Layer.Vector('Links Layer', {isBaseLayer: false, style: styleLinks});
           
var mouse = {x: 0, y: 0};
var current_tip = {x: 0, y: 0};
var current_feature;
var feature_is_selected = false;
var WGS84 = new OpenLayers.Projection("EPSG:4326");

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
    
   var options = {
        numZoomLevels: 32,
        projection: "EPSG:3857",
        maxExtent: new OpenLayers.Bounds(-200000, -200000, 200000, 200000),
        center: new OpenLayers.LonLat(-12356463.476333, 5621521.4854095)
    };
    //map = new OpenLayers.Map("map", options);

    map = new OpenLayers.Map("map", {
        transitionEffect: null, 
         zoomMethod: null ,
  projection: new OpenLayers.Projection("EPSG:900913")
});
var myBaseLayer = new OpenLayers.Layer.Google("Google Streets",
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
        //alert('ha');
        map.zoomToProxy(zoom,xy); 
        document.getElementById("link_tooltip").style.opacity = 0;
        document.getElementById("link_tooltip").style.display = 'none';
        //else do nothing and map wont zoom
    };

    var wms3 = new OpenLayers.Layer.WMS( "OpenLayers WMS", {isBaseLayer: true},
            "http://vmap0.tiles.osgeo.org/wms/vmap0", {layers: 'basic'} );
    //map.addLayer(wms3);
    //alert('tom');
    wkt = new OpenLayers.Format.WKT();
    map.addLayer(new OpenLayers.Layer.OSM());
    //create a style object
    
    map.addLayer(vectorsA);
    map.addLayer(vectorsB);
    map.addLayer(vectorsLinks);
  map.setLayerIndex(vectorsA,9000);
  map.setLayerIndex(vectorsB,90);
  map.setLayerIndex(vectorsLinks,999);
        var lays = [vectorsA, vectorsB, vectorsLinks];
        var selectControl = new OpenLayers.Control.SelectFeature(lays);
            map.addControl(selectControl);
            selectControl.activate();
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
                'featureselected': onFeatureSelectFromLinks,
                'featureunselected': onFeatureUnselectFromLinks,
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
        var polygonFeature = wkt.read("POLYGON((-25.8203125 2.4609375, -15.8203125 -10.546875, 6.85546875 -11.25, 8.26171875 -3.33984375, -15.8203125 2.4609375))");
        //var polygonFeature = wkt.read("POINT(-25.8203125 2.4609375)");
        polygonFeature.geometry.transform(map.displayProjection, map.getProjectionObject());
        //vectorsA.addFeatures([polygonFeature]);
        
        var polygonFeature2 = wkt.read("POLYGON((23.7240456428877 37.9908366236946,23.7241422428877 37.9906675236946,23.7238686428877 37.9905829236946,23.7238364428877 37.9908197236946,23.7240456428877 37.9908366236946))");
        polygonFeature2.geometry.transform(WGS84, map.getProjectionObject());
        //vectorsB.addFeatures([polygonFeature2]);
        
    map.zoomToMaxExtent();
});

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

function onFeatureUnselectFromA (event) {
    document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;
    //document.getElementById("link_tooltip").style.display = 'none';
    
    //document.getElementById("link_tooltip").fadeOut('slow', 'linear');
}

function onFeatureSelect(event) {
    // fetch the cluster's latlon and set the map center to it and call zoomin function
    // which takes you to a one level zoom in and I hope this solves your purpose :)    
    map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    map.zoomToExtent(event.feature.geometry.getBounds(), true);
    map.zoomIn();
    //alert('tomas');
    //$("testid2").html("your new header");
    //$("testid2").html('your new header');
    //document.getElementById("testid2").innerText = "public offers";
    //document.getElementById("testid2").innerHTML = event.feature.style.title;
    feature_is_selected = true;    
    
    var sendData = new Array();
    sendData[sendData.length] = event.feature.attributes.a;
    var list = document.getElementById("matchList");
    var listItem = list.getElementsByTagName("li");
    var inputItem = listItem[0].getElementsByTagName("input");      
    sendData[sendData.length] = inputItem[0].value;
    sendData[sendData.length] = "http://www.opengis.net/ont/geosparql#asWKT";
    for (var i=1; i < listItem.length; i++) {
        var inputItem = listItem[i].getElementsByTagName("input");      
        //alert(inputItem[0].value);
        sendData[sendData.length] = inputItem[0].value;
        //alert(sendData[sendData.length-1]);
        sendData[sendData.length] = selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML];
        //alert(sendData[sendData.length-1]);
    }
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
            $('#connLabel').text(responseJson);
            fusionPanel(event, responseJson);
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

/*
 * 
 * @param {type} val
 * @returns {undefined}
 */
function fusionPanel( event, val ) {
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
    
    //current_feature = event.feature;
    
    //alert(event.feature.attributes.a);
    var s="<p class=\"geoinfo\" id=\"link_name\">Ludacris</p>\n"+
//" <div class=\"checkboxes\">\n"+
//" <label for=\"chk1\"><input type=\"checkbox\" name=\"chk1\" id=\"chk1\" />Flag as misplaced fusion</label><br />\n"+
//" </div>\n"+
//" Description: <textarea name=\"textarea\" style=\"width:99%;height:50px;\" class=\"centered\"></textarea>\n"+
" <table border=1 id=\"fusionTable\" style=\"width: 100%;>\n"+
" <tr>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\"> </td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Value A</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Predicate</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Value B</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Action</td>\n"+
" <td style=\"width:216px; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
" </tr>\n"+
" <tr>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Geom A</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">asWKT</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Geom B</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\"><select style=\"width: 100%;\"><option>Average two points</option><option>Keep both</option><option>Keep left</option><option>Keep right</option><option>Shift Polygon To Point</option><option>Shift Polygon to average distance</option><option>Scale</option></select></td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Fused Geom</td>\n"+
" </tr>\n"+
" </table>"+
" <input id=\"fuseButton\" type=\"submit\" value=\"Fuse\" style=\"float:right\" onclick=\"return false;\"/>\n";

    document.getElementById("link_tooltip").innerHTML = s;
    var tbl = document.getElementById("fusionTable");
    $('#fuseButton').click(performFusion);
    
    //$.each(val, function(index, element) {
        $.each(val['fusions'], function(index1, element1) {
        //alert(index1 +' '+ element1);
            //$.each(element1, function(index11, element11) {
            //alert("PROP "+element1["property"]);
            //alert("PROP 2"+element1.property);
            //alert(index11 +' '+ element11);
                var opt = document.createElement("tr");
                //alert("HURRAY");
                var retL = element1.left.lastIndexOf("#");
                if ( retL < 0 )
                    retL = element1.left.lastIndexOf("/");
                var retR = element1.left.lastIndexOf("#");
                if ( retR < 0 )
                    retR = element1.left.lastIndexOf("/");
                var entry = " <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+element1.left+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+element1.property+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">"+element1.right+"</td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\"><select style=\"width: 100%;\"><option>Keep Left</option><option>Keep Right</option><option>Keep Both</option><option>Keep Concatenated Left</option><option>Keep Concatenated Right</option><option>Keep Concatenated Both</option><option>Keep Flattened Left</option><option>Keep Flattened Right</option><option>Keep Flattened Both</option></select></td>\n"+
" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n";

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
    
    //alert("Nick");
    document.getElementById("link_tooltip").style.opacity = 0.8;
    //alert("Nick");
    document.getElementById("link_tooltip").style.display = 'inline';
    document.getElementById("link_tooltip").style.left = mouse.x+'px';
    document.getElementById("link_tooltip").style.top = mouse.y+'px';
    document.getElementById("link_name").innerHTML = event.feature.attributes.a;
    
    //alert("Nick");
}

function performFusion() {
    //alert('so close 1');
    var tbl = document.getElementById("fusionTable");
    //alert('so close 2');
    var tblBody = document.getElementById("fusionTable");
    //alert(tblBody);
    var tblRows = tblBody.getElementsByTagName("tr");
    //alert(tblRows);
    //alert(tblRows[0]);
    var sendData = new Array();    
    for (var i=1; i < tblRows.length; i++) {
        var cells = tblRows[i].getElementsByTagName("td");
        for (var j=0; j < cells.length; j++) {
            //alert(cells[j].getElementsByTagName("select").length);
            var action = cells[j].getElementsByTagName("select");
            //alert(action[0]);
            if (action.length == 1) {
               //alert(action[0].innerHTML);
               //alert(action[0].value);
               var cell = action[0].value;
                sendData[sendData.length] = cell;
            } else {
                var cell = cells[j].innerHTML;
                sendData[sendData.length] = cell;
            };
            //alert(cell);
        }
    }
    
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseLinkServlet",
        // the data to send (will be converted to a query string)
        data: {props:sendData},
        // the type of data we expect back
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            //$('#connLabel').text(responseJson);
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

function onFeatureUnselect (event) {
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
    
    alert(event.feature.attributes.a);
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
                polygonFeatureA.attributes = {'a': first};
                polygonFeatureA.style = {
                strokeColor: "blue",
                strokeWidth: 3,
                cursor: "pointer",
                fillColor: "blue",
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
                polygonFeatureB.attributes = {'a': third};
                polygonFeatureB.style = {
                strokeColor: "black",
                pointRadius: 1,
                strokeWidth: 5,
                cursor: "pointer",
                fillColor: "black",
                title: third };
            
                //alert(polygonFeatureA.geometry.getCentroid());
                //alert(polygonFeatureB.geometry.getCentroid());
                
                var start_point = polygonFeatureA.geometry.getCentroid();
                var end_point = polygonFeatureB.geometry.getCentroid();
                
                //alert(start_point);
                //alert(end_point);
                
                vectorsLinks.addFeatures([new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]))]);
                vectorsA.addFeatures([polygonFeatureB]);
                
                prev = i + 1;
                step = 0;
            }
        }
    }
    /*
    var step = 0;
    var prev = 0;
    //alert(geoms[1]);
    for ( var i = 0; i < geoms[1].length; i++ )
    {
        if(geoms[1].charAt(i) == ';') {
            if ( step === 0 ) {
                first = geoms[1].substring(prev, i);
                prev = i + 1;
                step++;
            } else {
                second = geoms[1].substring(prev, i);
                //alert(second);
                var polygonFeature = wkt.read(second);
                //alert (second);
                //polygonFeature.geometry.transform(epsg3035, epsg900913);
                polygonFeature.geometry.transform(WGS84, map.getProjectionObject());
                polygonFeature.attributes = {'a': first};
                polygonFeature.style = {
                strokeColor: "black",
                pointRadius: 1,
                strokeWidth: 5,
                cursor: "pointer",
                fillColor: "black",
                title: first };
            
                vectorsA.addFeatures([polygonFeature]);
                prev = i + 1;
                step = 0;
            }
        }
    }
    for ( var i = 0; i < geoms[2].length; i++ )
    {
        if(geoms[2].charAt(i) == ';') {
            if ( step === 0 ) {
                first = geoms[2].substring(prev, i);
                prev = i + 1;
                step++;
            } else {
                second = geoms[2].substring(prev, i);
                //alert(second);
                var polygonFeature = wkt.read(second);
                //alert (second);
                //polygonFeature.geometry.transform(epsg3035, epsg900913);
                polygonFeature.geometry.transform(WGS84, map.getProjectionObject());3
                polygonFeature.style = styleLinks;
            
                vectorsLinks.addFeatures([polygonFeature]);
                prev = i + 1;
                step = 0;
            }
        }
    }
    alert(vectorsA.features.length);
    /*
    var polygonFeatures = wkt.read(geom);
    var index;
    for (index = 0; index < polygonFeatures.length; index++) {
        polygonFeatures[index].geometry.transform(map.displayProjection, map.getProjectionObject());
        vectors.addFeatures([polygonFeatures[index]]);
    } 
    map.setCenter(polygonFeatures[0].geometry.getBounds().getCenterLonLat());
    map.zoomToExtent(event.feature.geometry.getBounds(), true);
    map.zoomIn();
    //alert ("geom 2");
    //map.zoomToMaxExtent();
    */
}