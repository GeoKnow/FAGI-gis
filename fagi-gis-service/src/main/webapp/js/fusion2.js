/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

 var test = {
    "geometryA": 
        "LINESTRING(-73.979282 40.752029,-73.979577 40.752154,-73.979528 40.752221,-73.979233 40.752097,-73.979276 40.752038,-73.979282 40.752029)",
    "geometryB": 
        "POLYGON((-73.979611 40.752122,-73.979302 40.751991,-73.979251 40.752058,-73.979563 40.752193,-73.979611 40.752122))",
    "owlClassA": [
        "http://geovocab.org/spatial#Feature",
        "http://linkedgeodata.org/meta/Way",
        "http://linkedgeodata.org/ontology/Hotel",
        "http://linkedgeodata.org/ontology/TourismThing",
        "http://geovocab.org/spatial#Feature",
        "http://linkedgeodata.org/meta/Way",
        "http://linkedgeodata.org/ontology/Hotel",
        "http://linkedgeodata.org/ontology/TourismThing"
    ],
    "owlClassB": [
        "http://geoknow.eu/geodata#hotel",
        "http://geoknow.eu/geodata#office_building",
        "http://geoknow.eu/geodata#hotel",
        "http://geoknow.eu/geodata#office_building"
    ],
    "fusionAction": 
        "Keep right"
    

    };
    
$(document).ready(function () {                        // When the HTML DOM is ready loading, then execute the following function...
    //$.ajaxSetup({
    //    cache: false
    //});
    //alert(OpenLayers.Events.BROWSER_EVENTS);
    /* $.ajax({
        
        // request type
        type: "POST",
        // the URL for the request
        url: "LearningServlet",
        // the data to send (will be converted to a query string)
        
        data: {'actions':JSON.stringify(test)},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseText) {
           alert("return json here"); 
           alert(responseText.tagA);
           alert(responseText.tagB);
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            alert("error: " +  xhr + status + errorThrown);
        },
        // code to run regardless of success or failure
        complete: function (xhr, status) {
            alert("complete function");
        }
    });
    */
    init();
});

var form = document.getElementById('file-form');
var fileSelect = document.getElementById('file-select');
var uploadButton = document.getElementById('upload-button');
var States = new Array();
var scoreThreshold = 0.3;

function init() {
    $( "input" ).tooltip();
    //$( document ).tooltip();
    
    disableSpinner();
    
    $('#popupBBoxMenu').hide();
    $('#popupTransformMenu').hide();
    $('#popupValidateMenu').hide();
    $('#popupFindLinkMenu').hide();

    $(".buttonset").buttonset();
    $('#connButton').click(setConnection);
    $('#dataButton').click(setDatasets);
    $('#loadButton').click(setConnection);
    $('#buttonFilterLinksA').click(filterLinksA);
    $('#buttonFilterLinksB').click(filterLinksB);
    $('#linksButton').click(schemaMatch);
    $('#allLinksButton').click(selectAll);
    $('#finalButton').click(submitLinks);

    $('#previewPanel').data("opened", false);
    $('#connectionMenu').click(expandConnectionPanel);
    $('#connectionPanel').data("opened", false);
    $('#datasetMenu').click(expandDatasetPanel);
    $('#datasetPanel').data("opened", false);
    $('#linksMenu').click(expandLinksPanel);
    $('#linksPanel').data("opened", false);
    $('#matchingMenu').click(expandMatchingPanel);
    $('#matchingPanel').data("opened", false);
    $('#fusionPanel').data("opened", false);
    $('#clusteringPanel').data("opened", false);
    $('#fg-fetch-sparql-panel').data("opened", false);
    $('#clusteringTool').click(expandClusteringPanel);   
    $('#fetchBBoxSPARQLButton').click(expandSPARQLFetchPanel);   
    $('#multipleTool').click(activateMultipleTool);   
    $('#bboxTool').click(activateBBoxTool);   
    $('#fetchTool').click(activateFecthUnlinked);
    $('#visibleSelect').click(activateVisibleSelect);
    $('#fg-links-queries-submit').click(linksSPARQLFilter);
    $('#fg-fetch-queries-submit').click(fetchSPARQLContained);
    
    // Clustering
    $('#clusterButton').click(performClustering);
    $("#clusterSelector").change(function() {
        var selectedCluster = $(this).val();
        if (selectedCluster < 0) {
            $.each(vectorsLinks.features, function (index, element) {
                //var assign = assigns.results[element.attributes.a];
                //element.attributes.cluster = assign.cluster;
                element.style = null;
                element.attributes.la.style = null;
                element.attributes.lb.style = null;
            });
        } else if ( selectedCluster == 9999 ) {
            //alert("Custom Cluster");
            $.each(vectorsLinks.features, function (index, element) {
                element.style = {display: 'none'};
                element.attributes.la.style = {display: 'none'};
                element.attributes.lb.style = {display: 'none'};
            });
            $.each(activeFeatureClusterA, function (index, element) {
                element.style = null;
                element.attributes.la.style = null;
                element.attributes.lb.style = null;
            });
        } else {
            $.each(vectorsLinks.features, function (index, element) {
                //var assign = assigns.results[element.attributes.a];
                //element.attributes.cluster = assign.cluster;
                if (element.attributes.cluster != selectedCluster) {
                    element.style = {display: 'none'};
                    element.attributes.la.style = {display: 'none'};
                    element.attributes.lb.style = {display: 'none'};
                }
            });
        }
        
        vectorsA.refresh();
        vectorsB.refresh();
        vectorsLinks.refresh();
    }); 
    
    $('#fetchBBoxSPARQLButton').click(enableSPARQLFetch);
    $('#transformBBoxButton').click(enableBBoxTransform);
    $('#fetchBBoxContainedButton').click(fetchContained);
    $('#fetchBBoxFindButton').click(fetchContainedAndLink);
    
    $('#moveButton').click(function () {transType = MOVE_TRANS;
        dragControlA.activate();
        dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });
    $('#scaleButton').click(function () {transType = SCALE_TRANS;
        dragControlA.activate();
        dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });
    $('#rotateButton').click(function () {transType = ROTATE_TRANS;
        dragControlA.activate();
        dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });
    
    $('#valAllButton').click(function () {  
        var ds = $('#valAllButton').data("dataset");
        if (ds == "A") {
            $.each(vectorsA.features, function (index, element) {
                var links = element.attributes.links;
                if ( typeof links === "undefined" ) {
                    console.log(element.attributes.a);
                    map.zoomToExtent(element.geometry.getBounds());
                }
                
                if (links.length > 0) {
                    var bestLink = null;
                    var bestScore = -1;
                    for (var i = 0; i < links.length; i++) {
                        if ( links[i].validated ) 
                            continue;
                        var linkScore = links[i].dist + links[i].jIndex;
                        if ( linkScore > bestScore ) {
                            bestScore = linkScore;
                            bestLink = links[i];
                        }
                    }
                    //console.log("Best Score " + bestScore);
                    if ( bestLink != null )
                        validateLink(bestLink, ds);
                }
            });
        } else {
            $.each(vectorsB.features, function (index, element) {
                var links = element.attributes.links;
                if ( typeof links === "undefined" ) {
                    console.log(element.attributes.a);
                    map.zoomToExtent(element.geometry.getBounds());
                }
                if (links.length > 0) {
                    var bestLink = null;
                    var bestScore = -1;
                    for (var i = 0; i < links.length; i++) {
                        if ( links[i].validated ) 
                            continue;
                        var linkScore = links[i].dist + links[i].jIndex;
                        if ( linkScore > bestScore ) {
                            bestScore = linkScore;
                            bestLink = links[i];
                        }
                    }
                    console.log("Best Score " + bestScore);
                    if ( bestLink != null )
                        validateLink(bestLink, ds);
                }
            });
        }
    });
    
    $('#valButton').click( function () {
        var feat = $(this).prop("link");
        //console.log(feat.attributes.la.attributes.a);
        //console.log(feat.attributes.lb.attributes.a);
        document.getElementById("popupValidateMenu").style.opacity = 0;
        document.getElementById("popupValidateMenu").style.display = 'none';
        enableSpinner();
        $.ajax({
            url: 'CreateLinkServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                feat.validated = true;
                
                var i = 0;
                var linksA = feat.attributes.la.attributes.links;
                var linksB = feat.attributes.lb.attributes.links;
                var toDel = [];
                var newLinksA = [];
                var newLinksB = [];
                for ( i = 0; i < linksA.length; i++ ) {
                    if (linksA[i].validated == false )
                        toDel[toDel.length] = linksA[i];
                    else
                        newLinksA[newLinksA.length] = linksA[i];
                }
                for ( i = 0; i < linksB.length; i++ ) {
                    if (linksB[i].validated == false )
                        toDel[toDel.length] = linksB[i];
                    else
                        newLinksB[newLinksB.length] = linksB[i];
                }
                feat.attributes.la.attributes.links = newLinksA;
                feat.attributes.lb.attributes.links = newLinksB;
                //vectorsLinksTemp.destroyFeatures(toDel);
                vectorsLinks.destroyFeatures(toDel);
                feat.validated = true;
                vectorsLinks.drawFeature(feat);
                console.log("All good " + responseText);
                
                disableSpinner();
            },
            error: function (responseText) {
                disableSpinner();
                alert("All bad " + responseText);
                alert("Error");
            },
            data: {'subA': feat.attributes.la.attributes.a, 'subB': feat.attributes.lb.attributes.a}
            //Options to tell jQuery not to process data or worry about content-type.
        });
    });
    
    $('#createLinkButton').click(function () {
        
        if ( $('#createLinkButton').html() === "Cancel Link" ) {
            vectorsLinksTemp.destroyFeatures();
            lastPo = null;
            nowPo = null;
            
            document.getElementById("popupTransformMenu").style.opacity = 0;
            document.getElementById("popupTransformMenu").style.display = 'none';
            
            prevActiveFeature = null;
            activeFeature = null;
                    
            return;
        }
        
        if ( lastPo == null ) {
            document.getElementById("popupTransformMenu").style.opacity = 0;
            document.getElementById("popupTransformMenu").style.display = 'none';
            lastPo = activeFeature.geometry.getCentroid(true);
            lastPo.node = activeFeature;
        } else {
            nowPo = activeFeature.geometry.getCentroid(true);
            nowPo.node = activeFeature;
            createNewLink(nowPo.node, lastPo.node);
        }
    });
    
    $("#popupFindLinkButton").click(function () {
        //alert($("#radiusSpinner").spinner("value"));
        for ( var i = 0; i < activeFeature.attributes.links.length; i++ ) 
            if ( !activeFeature.attributes.links[i].validated )
                return;
        
        activeFeature.geometry.transform(map.getProjectionObject(), WGS84);
        var requestEntity = new Object();
        requestEntity.sub = activeFeature.attributes.a;
        requestEntity.ds = 'A';
        if ( activeFeature.layer == vectorsB )
            requestEntity.ds = 'B';
        
        document.getElementById("popupFindLinkMenu").style.opacity = 0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';
        
        requestEntity.geom = wkt.extractGeometry ( activeFeature.geometry.getCentroid(true) );
        //alert(JSON.stringify(requestEntity));
        activeFeature.geometry.transform(WGS84, map.getProjectionObject());
        enableSpinner();
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FindLinkServlet",
            // the data to send (will be converted to a query string)
            data: {entity: JSON.stringify(requestEntity), radius: $("#radiusSpinner").spinner("value")},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                //alert(JSON.stringify(responseJson));
                createSingleUnvalidatedLinks(activeFeature, responseJson);
                disableSpinner();            },
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
    });
    
    $('#findLinkButton').click(function () {        
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';

        document.getElementById("popupFindLinkMenu").style.opacity = 0.7;
        document.getElementById("popupFindLinkMenu").style.display = 'inline';
        document.getElementById("popupFindLinkMenu").style.top = mouse.y;
        document.getElementById("popupFindLinkMenu").style.left = mouse.x; 
    
        /*
        activeFeature.geometry.transform(map.getProjectionObject(), WGS84);
        var requestEntity = new Object();
        requestEntity.sub = activeFeature.attributes.a;
        requestEntity.ds = 'A';
        if ( activeFeature.layer == vectorsB )
            requestEntity.ds = 'B';
        
        requestEntity.geom = wkt.extractGeometry ( activeFeature.geometry.getCentroid(true) );
        //alert(JSON.stringify(requestEntity));
        activeFeature.geometry.transform(WGS84, map.getProjectionObject());
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FindLinkServlet",
            // the data to send (will be converted to a query string)
            data: {entity: JSON.stringify(requestEntity)},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                alert(responseJson);
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
         */
    });
    
    $('.dropdown').css("z-index", "700000");

    var radSpinner = $("#radiusSpinner").spinner({step: 1,
        numberFormat: "n",
        min: 1,
        max: 1000,
        spin: function (event, ui) {
            $(this).change();
        }});
    radSpinner.spinner("value", 100);

    var spinner = $("#spinner").spinner({step: 0.05,
        numberFormat: "n",
        min: 0.0,
        max: 1.0,
        spin: function (event, ui) {
            $(this).change();
        }});
    spinner.spinner("value", 0.3);
}

function validateLink(feat, ds) {
    //console.log(feat.attributes.la.attributes.a);
    console.log("Dataset "+ds);
    document.getElementById("popupValidateMenu").style.opacity = 0;
    document.getElementById("popupValidateMenu").style.display = 'none';
    enableSpinner();
    $.ajax({
        url: 'CreateLinkServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseText) {
            feat.validated = true;

            var i = 0;
            var linksA = feat.attributes.la.attributes.links;
            var linksB = feat.attributes.lb.attributes.links;
            var toDel = [];
            var newLinksA = [];
            var newLinksB = [];
            if (ds == "A") {
                for (i = 0; i < linksA.length; i++) {
                    if (linksA[i].validated == false)
                        toDel[toDel.length] = linksA[i];
                    else
                        newLinksA[newLinksA.length] = linksA[i];
                }
                feat.attributes.la.attributes.links = newLinksA;

            } else {
                for (i = 0; i < linksB.length; i++) {
                    if (linksB[i].validated == false)
                        toDel[toDel.length] = linksB[i];
                    else
                        newLinksB[newLinksB.length] = linksB[i];
                }
                feat.attributes.lb.attributes.links = newLinksB;
            }
            //vectorsLinksTemp.destroyFeatures(toDel);
            vectorsLinks.destroyFeatures(toDel);
            feat.validated = true;
            vectorsLinks.drawFeature(feat);
            //console.log("All good " + responseText);

            disableSpinner();
        },
        error: function (responseText) {
            disableSpinner();
            alert("All bad " + responseText);
            alert("Error");
        },
        data: {'subA': feat.attributes.la.attributes.a, 'subB': feat.attributes.lb.attributes.a}
        //Options to tell jQuery not to process data or worry about content-type.
    });
}

$("#domA").change(function () {
    if ($('#domB').is(":checked")) {
        $('#domB').prop('checked', false);
    } else {
        $('#domA').prop('checked', true);
    }
});

$("#spinner").change(function () {
    scoreThreshold = $(this).spinner("value");
});

$("#radiusSpinner").change(function () {
    radius = $(this).spinner("value");
    //alert('Radius ' + radius);
});

$("#domB").change(function () {
    if ($('#domA').is(":checked")) {
        $('#domA').prop('checked', false);
    } else {
        $('#domB').prop('checked', true);
    }
});

$(':file').change(function () {
    var file = this.files[0];
    var name = file.name;
    var size = file.size;
    var type = file.type;
    //Your validation
});

function enableSPARQLFetch( links ) {
   
}
    
function createSingleUnvalidatedLinks(feat, links) {
    $.each(links, function (index, element) {
        var featB = null;
        var layer = null;
        if (feat.layer.name == "Dataset A Layer") {
            featB = vectorsB.getFeaturesByAttribute("a", element.subB);
            layer = vectorsB;
            //console.log(featB.length);
            console.log(element.subB);
            if (featB.length > 0) {
                var retFeat = createUnvalidatedLink(feat, featB[0]);
                
                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;
            
                feat.attributes.links[feat.attributes.links.length] = retFeat;
                featB[0].attributes.links[featB[0].attributes.links.length] = retFeat;
            } else {
                var polygonFeature = wkt.read(element.geomB);
                polygonFeature.geometry.transform(WGS84, map.getProjectionObject());
                polygonFeature.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeature)};
                var retFeat = createUnvalidatedLink(feat, polygonFeature);
                
                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;
            
                //console.log(retFeat.attributes.la.attributes.a);
                polygonFeature.attributes.links[polygonFeature.attributes.links.length] = retFeat;
                feat.attributes.links[feat.attributes.links.length] = retFeat;
                //polygonFeature.attributes.a = retFeat.attributes.lb.attributes.a;
                layer.addFeatures([polygonFeature]);
            }
                //createUnvalidatedLinkWithGeom(feat, element, layer);
        } else {
            featB = vectorsA.getFeaturesByAttribute("a", element.subB);
            layer = vectorsA;
            //console.log(featB.length);
            console.log(element.subB);
            if (featB.length > 0) {
                var retFeat = createUnvalidatedLink(featB[0], feat);
                
                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;
            
                feat.attributes.links[feat.attributes.links.length] = retFeat;
                featB[0].attributes.links[featB[0].attributes.links.length] = retFeat;
            } else {
                var polygonFeature = wkt.read(element.geomB);
                polygonFeature.geometry.transform(WGS84, map.getProjectionObject());
                polygonFeature.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeature)};
                var retFeat = createUnvalidatedLink(polygonFeature, feat);
                
                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;
            
                console.log(retFeat.attributes.la.attributes.a);
                polygonFeature.attributes.links[polygonFeature.attributes.links.length] = retFeat;
                feat.attributes.links[feat.attributes.links.length] = retFeat;
                //polygonFeature.attributes.a = retFeat.attributes.la.attributes.a;
                layer.addFeatures([polygonFeature]);
            }
                //createUnvalidatedLinkWithGeom(feat, element, layer);
        }
    });
}

function createUnvalidatedLinkWithGeom(feat, elem, layer) {
    polygonFeature = wkt.read(elem);
    polygonFeature.geometry.transform(WGS84, map.getProjectionObject());

    var start_point = polygonFeature.geometry.getCentroid(true);
    var end_point = feat.geometry.getCentroid(true);
    
    var line2 = new OpenLayers.Geometry.LineString([lastPo, nowPo]);
    linkFeature = new OpenLayers.Feature.Vector(line2);
    linkFeature.attributes = {'la': nodeA,
        'a': nodeA.attributes.a,
        'lb': nodeB,
        'cluster': nodeB.attributes.cluster,
        'opacity': 0.8};

    var links = [];
    links[0] = linkFeature;
    
    polygonFeature.attributes = {'links': links, 'a': first, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': wkt.write(polygonFeature)};

    linkFeature.prev_fused = false;
    linkFeature.validated = false;
    nodeA.attributes.links = links;
    nodeB.attributes.links = links;

    //vectorsLinksTemp.destroyFeatures();
    vectorsLinks.addFeatures([linkFeature]);
    vectorsLinks.drawFeature(linkFeature);
    layer.addFeatures();
    
    return linkFeature;
}

function createUnvalidatedLink(nodeA, nodeB) {    
    var start_point_wgs = nodeA.geometry.getCentroid(true);
    var end_point_wgs = nodeB.geometry.getCentroid(true);

    //console.log(JSON.stringify(start_point_wgs));
    //console.log(JSON.stringify(end_point_wgs));

    var line2 = new OpenLayers.Geometry.LineString([start_point_wgs, end_point_wgs]);
    linkFeature = new OpenLayers.Feature.Vector(line2);
    linkFeature.validated = false;
    linkFeature.attributes = {'la': nodeA,
        'a': nodeA.attributes.a,
        'lb': nodeB,
        'cluster': nodeB.attributes.cluster,
        'opacity': 0.8};

    var links = [];
    links[0] = linkFeature;
    linkFeature.prev_fused = false;
    linkFeature.validated = false;
    //nodeA.attributes.links = links;
    //nodeB.attributes.links = links;

    //vectorsLinksTemp.destroyFeatures();
    vectorsLinks.addFeatures([linkFeature]);
    vectorsLinks.drawFeature(linkFeature);

    return linkFeature;
}

function createNewLink(nodeA, nodeB) {
    if (nodeA.layer.name !== nodeB.layer.name) {
        // We want nodeA to refer to Layer A
        if (nodeA.layer == vectorsA) {
            // Unless if the dominant set is B
            if (!$('#domA').is(":checked")) {
                var temp = nodeA;
                nodeA = nodeB;
                nodeB = temp;
            }
        } else {
            if ($('#domA').is(":checked")) {
                var temp = nodeA;
                nodeA = nodeB;
                nodeB = temp;
            }
        }
        
        var line2 = new OpenLayers.Geometry.LineString([lastPo, nowPo]);
        linkFeature = new OpenLayers.Feature.Vector(line2);
        linkFeature.attributes = {'la': nodeA,
            'a': nodeA.attributes.a,
            'lb': nodeB,
            'cluster': nodeB.attributes.cluster,
            'opacity': 0.8};
        
        linkFeature.prev_fused = false;
        linkFeature.validated = true;
        nodeA.attributes.links[0] = linkFeature;
        nodeB.attributes.links[0] = linkFeature;

        vectorsLinksTemp.destroyFeatures();
        vectorsLinks.addFeatures([linkFeature]);
        vectorsLinks.drawFeature(linkFeature);

        lastPo = null;

        var sendData = new Object();
        console.log(nodeA.attributes.a);
        console.log(nodeB.attributes.a);
        enableSpinner();
        $.ajax({
            url: 'CreateLinkServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                disableSpinner();
                console.log("All good " + responseText);
            },
            error: function (responseText) {
                disableSpinner();
                alert("All bad " + responseText);
                alert("Error");
            },
            data: {'subA' : nodeA.attributes.a, 'subB' : nodeB.attributes.a}
                    //Options to tell jQuery not to process data or worry about content-type.
        });
    } else {
        lastPo = null;
        alert('You cannot fuse geometries of the same dataset');
    }
}

var selectedProperties = new Object();
var next_id = 0;
var next_link_id = 0;
$('#addLinkSchema').click(function () {
    if (linkLastSelectedFromA === null || linkLastSelectedFromB === null) {
        alert("No properties selected");
        //alert(lastSelectedFromA === null);
        //alert(lastSelectedFromB === null);
        return;
    }

    var strA = "";
    var strB = "";
    //alert(strB);
    var listA = document.getElementById("linkSchemasA");
    //alert('pls');
    var listItemsA = listA.getElementsByTagName("li");
    $.each(listItemsA, function (index, element) {
        element.style.backgroundColor = element.backColor;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if ( linkLastSelectedFromA === null )
            linkLastSelectedFromA = element;
        if (element.prev_selected === true) {
            strA += element.long_name+"|";
            element.prev_selected = false;
        }
    });

    var listB = document.getElementById("linkSchemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        element.style.backgroundColor = element.backColor;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if ( linkLastSelectedFromB === null )
            linkLastSelectedFromB = element;
        if (element.prev_selected === true) {
            strB +=  element.long_name+"|";
            element.prev_selected = false;
        }
    });
    strA = strA.substring(0, strA.length - 1);
    strB = strB.substring(0, strB.length - 1);
    //alert(strA);
    //alert(strB);
    var node = document.createElement("li");
    node.onclick = linkMatchedSchemaClicked;
    var text = '<input class="match" type="text" name="lname" value="' + linkLastSelectedFromA.innerHTML + '=>' + linkLastSelectedFromB.innerHTML + '"/>';
    node.long_name = strA + '=>' + strB;
    var repA;
    var repB;
    var text;
    if (linkLastSelectedFromA === null) {
        repB = getText(linkLastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repB + '"/>';
        //node.long_name = 'dummy' + '=>' + linkLastSelectedFromB.long_name;
        node.long_name = 'dummy' + '=>' + strB;
    }
    else if (linkLastSelectedFromB === null) {
        repA = getText(linkLastSelectedFromA.firstChild)
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        //node.long_name = linkLastSelectedFromA.long_name + '=>' + 'dummy';
        node.long_name = strA + '=>' + 'dummy';
    }
    else {
        repA = getText(linkLastSelectedFromA.firstChild);
        repB = getText(linkLastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + '=>' + repB + '"/>';
        //node.long_name = linkLastSelectedFromA.long_name + '=>' + linkLastSelectedFromB.long_name;
        node.long_name = strA + '=>' + strB;
    }
    console.log(node.long_name);
    
    node.innerHTML = text;
    $( node ).on('input', function (e) {
        var row = $("#fusionTable tr")[this.rowIndex];
        this.newPred = e.target.value;
        $(row).get(0).newPred = e.target.value;
        $(row).find("td")[1].innerHTML = e.target.value;
    });
    
    next_link_id++;
    document.getElementById("linkMatchList").appendChild(node);
    
    updateFusionTable(node);
});

function getText(obj) {
    return obj.textContent ? obj.textContent : obj.innerText;
}

$('#addSchema').click(function () {
    if (lastSelectedFromA === null && lastSelectedFromB === null) {
        //alert("tom");
        alert("No matching selected");
        return;
    }

    var strA = "";
    var strB = "";
    var listA = document.getElementById("schemasA");
    var listItemsA = listA.getElementsByTagName("li");
    $.each(listItemsA, function (index, element) {
        //alert(element.prev_selected);
        element.style.backgroundColor = element.backColor;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (element.prev_selected === true) {
            strA += element.long_name+"|";
            element.prev_selected = false;
        }
    });
    var listB = document.getElementById("schemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        //alert(element.prev_selected);
        element.style.backgroundColor = element.backColor;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (element.prev_selected === true) {
            strB +=  element.long_name+"|";
            element.prev_selected = false;
        }
    });
    strA = strA.substring(0, strA.length - 1);
    strB = strB.substring(0, strB.length - 1);
    //alert(strA);
    //alert(strB);
    var node = document.createElement("li");
    node.onclick = matchedSchemaClicked;
    var text;
    var repA;
    var repB;
    var propText;
    if (lastSelectedFromA === null) {
        repB = getText(lastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repB + '"/>';
        //node.long_name = 'dummy' + '=>' + lastSelectedFromB.long_name;
        node.long_name = 'dummy' + '=>' + strB;
        node.newPred = repB;
    }
    else if (lastSelectedFromB === null) {
        repA = getText(lastSelectedFromA.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        //node.long_name = lastSelectedFromA.long_name + '=>' + 'dummy';
        node.long_name = strA + '=>' + 'dummy';
        node.newPred = repA;
    }
    else {
        repA = getText(lastSelectedFromA.firstChild);
        repB = getText(lastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + '=>' + repB + '"/>';
        //node.long_name = lastSelectedFromA.long_name + '=>' + lastSelectedFromB.long_name;
        node.long_name = strA + '=>' + strB;
        node.newPred = repA + '=>' + repB;
    }
    //selectedProperties['id'+next_id] = lastSelectedFromA.long_name+'=>'+lastSelectedFromB.long_name;
    //alert(selectedProperties['id'+next_id]);
    node.innerHTML = text;
    $( node ).on('input', function (e) {
        var row = $("#bFusionTable tr")[this.rowIndex - 1];
        this.newPred = e.target.value;
        $(row).get(0).newPred = e.target.value;
        $(row).find("td")[1].innerHTML = e.target.value;
    });    
    
    next_id++;
    document.getElementById("matchList").appendChild(node);
    
    lastSelectedFromA = null;
    lastSelectedFromB = null;
    
    updateBFusionTable(node);
});

function replaceAt(str, at, withChar) {
    return str.substr(0, at) + withChar + str.substr(at+withChar.length);
}

var lastMatchedSchemaClicked = null;
var lastLinkMatchedSchemaClicked = null;

function matchedSchemaClicked() {
    lastMatchedSchemaClicked = this;
    //alert(document.getElementById("matchList"));
    //alert(this);
}

function linkMatchedSchemaClicked() {
    lastLinkMatchedSchemaClicked = this;
    //alert(document.getElementById("matchList"));
    //alert(this);
}

function assignClusters(assigns) {
    //alert(assigns.numOfClusters);
    for(var i = 0; i < assigns.numOfClusters; i++) {
        //alert($("#clusterSelector").html());
        $("#clusterSelector").append("<option value=\""+i+"\" >Cluster "+i+"</option>");
        
    }
    
    $.each(vectorsLinks.features, function (index, element) {
        var assign = assigns.results[element.attributes.a];
        element.attributes.cluster = assign.cluster;
    });
    //$.each(responseJson.foundB, function (index, element) {}
}

function performClustering () {
    //alert('tom');
    console.log($( "#slider" ).slider( "value" ));
    var vLen = $("#connVecDirCheck :radio:checked + label").text();
    var vDir = $("#connVecLenCheck :radio:checked + label").text();
    var cov = $("#connCoverageCheck :radio:checked + label").text();
    
    console.log($("#connVecDirCheck :radio:checked + label").text());
    console.log($("#connVecLenCheck :radio:checked + label").text());
    console.log($("#connCoverageCheck :radio:checked + label").text());
    
    if ( vLen == "" && vDir == "" && cov == "" ) {
        alert("please select at least one attribute for clustering");
    } else {
        var sendData = new Object();
        
        if (vLen == 'YES') sendData.vLen = 'YES';
        if (vDir == 'YES') sendData.vDir = 'YES';
        if (vLen == 'YES') sendData.cov = 'YES';
        sendData.clusterCount = $( "#slider" ).slider( "value" );
        
        //alert('file', $('input[type=file]')[0].files[0]);
        //alert($('#swapButton').is(":checked"));
        //alert('hey');
        enableSpinner();
        $.ajax({
            url: 'ClusteringServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                //console.log("All good "+responseText);
                assignClusters(responseText);
                disableSpinner();
            },
            error: function (responseText) {
                disableSpinner();
                alert("All bad " + responseText);
                alert("Error");
            },
            data: sendData
            //Options to tell jQuery not to process data or worry about content-type.
        });
    }
}

/*
 $('#addSchema').click(function(){
 if ( lastSelectedFromA === null || lastSelectedFromB === null ) {
 //alert("tom");
 //alert(lastSelectedFromA === null);
 //alert(lastSelectedFromB === null);
 return;
 }
 
 var node=document.createElement("li");
 var text = '<input class="match" type="text" name="lname" value="'+lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML+'"/>';
 selectedProperties['id'+next_id] = lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML;
 //alert(selectedProperties['id'+next_id]);
 node.innerHTML = text;
 for (var name in selectedProperties) {
 //alert(name);
 }
 next_id++;
 document.getElementById("matchList").appendChild(node);
 });
 */
$('#removeSchema').click(function () {
    document.getElementById("matchList").removeChild(lastMatchedSchemaClicked);
    //alert('done');
    //alert($('#schemasB').val());
    //alert($('#schemasB').text());
});

$('#removeLinkSchema').click(function () {
    document.getElementById("matchList").removeChild(lastLinkMatchedSchemaClicked);
    //alert('done');
    //alert($('#schemasB').val());
    //alert($('#schemasB').text());
});

$('#buttonL').click(function () {
    //var formData = new FormData(document.getElementById("linksDiv"));
    var formData = new FormData();
    formData.append('file', $('input[type=file]')[0].files[0]);
    //alert('file', $('input[type=file]')[0].files[0]);
    //alert($('#swapButton').is(":checked"));
    //alert('hey');
    enableSpinner();
    $.ajax({
        url: 'LinksServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "text",
        success: function (responseText) {
            //alert("All good "+responseText);
            var list = document.getElementById("linksList");
            var typesA = document.getElementById("typeListA");
            var typesB = document.getElementById("typeListB");
            var arrays = responseText.split("+>>>+");
            //alert(arrays[1]);
            list.innerHTML = arrays[0];
            typesA.innerHTML = arrays[1];
            typesB.innerHTML = arrays[2];
            disableSpinner();
        },
        error: function (responseText) {
            disableSpinner();
            alert("All bad " + responseText);
            alert("Error");
        },
        data: formData,
        //Options to tell jQuery not to process data or worry about content-type.
        cache: false,
        contentType: false,
        processData: false
    });
});

function submitLinks(batchFusion) {
    var sendJSONData = new Array();
    var sendData = new Array();
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    for (var i = 0; i < listItem.length; i++) {
        var labelItem = listItem[i].getElementsByTagName("label");
        if (labelItem[0].firstChild.checked) {
            var linksA = labelItem[0].lastChild.data.split("<-->");
            sendData[sendData.length] = linksA[0];
            //alert(linksA[0]);
        }
    }

    enableSpinner();
    $("#matchingMenu").trigger('click');
    if (!linksPreviewed) {
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "PreviewServlet",
            // the data to send (will be converted to a query string)
            data: {links: sendData},
            // the type of data we expect back
            dataType: "text",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseText) {
                //$('#connLabel').text(responseText);
                linksPreviewed = true;
                if (responseText === "Connection parameters not set") {
                    $('#dataLabel').text(responseText);
                } else {
                    //alert('add');
                    //addMapData(responseText);
                    if (batchFusion === true) {
                        addMapData(responseText);
                        var tbl = document.getElementById("bFusionTable");
                        //alert('so close 2');
                        var tblBody = document.getElementById("bFusionTable");
                        //alert(tblBody);
                        var tblRows = tblBody.getElementsByTagName("tr");
                        var sendJSON = new Array();
                        var clusterJSON = null;
                        var shiftValuesJSON = new Object();
                        if (!$('#bscale_fac').prop('disabled')) {
                            shiftValuesJSON.shift = $('#bshift').val();
                            shiftValuesJSON.scaleFact = $('#bscale_fac').val();
                            shiftValuesJSON.rotateFact = $('#brotate_fac').val();
                        }
                        if (!$('#offset-x-a').prop('disabled')) {
                            shiftValuesJSON.gOffsetAX = $('#offset-x-a').val();
                            shiftValuesJSON.gOffsetAY = $('#offset-y-a').val();
                            shiftValuesJSON.gOffsetBX = $('#offset-x-b').val();
                            shiftValuesJSON.gOffsetBY = $('#offset-y-b').val();
                        }
                        //alert($( "#clusterSelector" ).val());
                        if ($("#clusterSelector").val() > -1) {
                            //alert("Cluster chosen");
                            clusterJSON = createLinkCluster($("#clusterSelector").val());
                        }

                        //alert(current_feature == null);
                        var geomCells = tblRows[1].getElementsByTagName("td");
                        var geomFuse = new Object();

                        geomFuse.pre = geomCells[1].innerHTML;
                        geomFuse.preL = "http://www.opengis.net/ont/geosparql#asWKT";
                        var tmpGeomAction = geomCells[3].getElementsByTagName("select");
                        //alert('after valB '+tmpGeomAction.length+' '+geomCells.length);
                        if (tmpGeomAction.length == 1) {
                            geomFuse.action = tmpGeomAction[0].value;
                        }

                        sendJSON[sendJSON.length] = geomFuse;
                        for (var i = 2; i < tblRows.length; i++) {
                            var cells = tblRows[i].getElementsByTagName("td");
                            var propFuse = new Object();

                            propFuse.pre = tblRows[i].newPred;
                            propFuse.preL = tblRows[i].long_name;
                            var tmpAction = cells[3].getElementsByTagName("select");
                            if (tmpAction.length == 1) {
                                propFuse.action = tmpAction[0].value;
                            }

                            sendJSON[sendJSON.length] = propFuse;
                        }

                        var sndJSON = JSON.stringify(sendJSON);
                        var sndShiftJSON = JSON.stringify(shiftValuesJSON);
                        $.ajax({
                            // request type
                            type: "POST",
                            // the URL for the request
                            url: "BatchFusionServlet",
                            // the data to send (will be converted to a query string)
                            data: {propsJSON: sndJSON, factJSON: sndShiftJSON, clusterJSON: clusterJSON, cluster: $("#clusterSelector").val()},
                            // the type of data we expect back
                            dataType: "json",
                            // code to run if the request succeeds;
                            // the response is passed to the function
                            success: function (responseJson) {
                                //$('#connLabel').text(responseJson);
                                batchFusionPreview(responseJson);
                                //previewLinkedGeom(responseJson);
                                //fusionPanel(event, responseJson);
                                disableSpinner();
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
                    } else {
                        disableSpinner();
                        addMapData(responseText);
                    }
                }
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
    } else {
        if (batchFusion === true) {
            var tbl = document.getElementById("bFusionTable");
            //alert('so close 2');
            var tblBody = document.getElementById("bFusionTable");
            //alert(tblBody);
            var tblRows = tblBody.getElementsByTagName("tr");
            var sendJSON = new Array();
            var clusterJSON = null;
            var shiftValuesJSON = new Object();
            if (!$('#bscale_fac').prop('disabled')) {
                shiftValuesJSON.shift = $('#bshift').val();
                shiftValuesJSON.scaleFact = $('#bscale_fac').val();
                shiftValuesJSON.rotateFact = $('#brotate_fac').val();
            }
            if (!$('#offset-x-a').prop('disabled')) {
                shiftValuesJSON.gOffsetAX = $('#offset-x-a').val();
                shiftValuesJSON.gOffsetAY = $('#offset-y-a').val();
                shiftValuesJSON.gOffsetBX = $('#offset-x-b').val();
                shiftValuesJSON.gOffsetBY = $('#offset-y-b').val();
            }
            //alert($( "#clusterSelector" ).val());
            if ($("#clusterSelector").val() > -1) {
                //alert("Cluster chosen");
                clusterJSON = createLinkCluster($("#clusterSelector").val());
            }

            //alert(current_feature == null);
            var geomCells = tblRows[1].getElementsByTagName("td");
            var geomFuse = new Object();

            geomFuse.pre = geomCells[1].innerHTML;
            geomFuse.preL = "http://www.opengis.net/ont/geosparql#asWKT";
            var tmpGeomAction = geomCells[3].getElementsByTagName("select");
            //alert('after valB '+tmpGeomAction.length+' '+geomCells.length);
            if (tmpGeomAction.length == 1) {
                geomFuse.action = tmpGeomAction[0].value;
            }

            sendJSON[sendJSON.length] = geomFuse;
            for (var i = 2; i < tblRows.length; i++) {
                var cells = tblRows[i].getElementsByTagName("td");
                var propFuse = new Object();

                propFuse.pre = tblRows[i].newPred;
                propFuse.preL = tblRows[i].long_name;
                var tmpAction = cells[3].getElementsByTagName("select");
                if (tmpAction.length == 1) {
                    propFuse.action = tmpAction[0].value;
                }

                sendJSON[sendJSON.length] = propFuse;
            }

            var sndJSON = JSON.stringify(sendJSON);
            var sndShiftJSON = JSON.stringify(shiftValuesJSON);
            $.ajax({
                // request type
                type: "POST",
                // the URL for the request
                url: "BatchFusionServlet",
                // the data to send (will be converted to a query string)
                data: {propsJSON: sndJSON, factJSON: sndShiftJSON, clusterJSON: clusterJSON, cluster: $("#clusterSelector").val()},
                // the type of data we expect back
                dataType: "json",
                // code to run if the request succeeds;
                // the response is passed to the function
                success: function (responseJson) {
                    //$('#connLabel').text(responseJson);
                    batchFusionPreview(responseJson);
                    //previewLinkedGeom(responseJson);
                    //fusionPanel(event, responseJson);
                    disableSpinner();
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
    }
}

function createLinkCluster(cluster) {
    var ret = new Array();
    if (cluster == 9999) {
        $.each(activeFeatureClusterA, function (index, element) {
            var clusterLink = new Object();
            clusterLink.nodeA = element.attributes.la.attributes.a;
            clusterLink.nodeB = element.attributes.lb.attributes.a;
            ret[ret.length] = clusterLink;
        });
    } else {
        $.each(vectorsLinks.features, function (index, element) {
            if (element.attributes.cluster == cluster) {
                var clusterLink = new Object();
                clusterLink.nodeA = element.attributes.la.attributes.a;
                clusterLink.nodeB = element.attributes.lb.attributes.a;
                ret[ret.length] = clusterLink;
            }
        });
    }
    console.log();
    return JSON.stringify(ret);
}

function batchFusionPreview(geomsJSON) {
    var cluster = geomsJSON.cluster;
    var toDelFeatures =  new Array();
    if (cluster < 0) {
        $.each(vectorsLinks.features, function (index, element) {
            var clusterLink = new Object();
            var geom = geomsJSON.fusedGeoms[element.attributes.a];
            addGeom(element, geom.geom);
            console.log("Got " + geom.nb + " with geom " + geom.geom);
        });
    } else if ( cluster == 9999 ) {
        $.each(activeFeatureClusterA, function (index, element) {
            toDelFeatures[toDelFeatures.length] = element;
            var geom = geomsJSON.fusedGeoms[element.attributes.a];
            addGeom(element, geom.geom);
            console.log("In Custom cluster Got " + geom.nb + " with geom " + geom.geom);
        });
    } else {
        $.each(vectorsLinks.features, function (index, element) {
            if (element.attributes.cluster == cluster) {
                toDelFeatures[toDelFeatures.length] = element;
                var geom = geomsJSON.fusedGeoms[element.attributes.a];
                addGeom(element, geom.geom);
                console.log("Got " + geom.nb + " with geom " + geom.geom);
            } 
        });
    }
    
    if ( toDelFeatures.length )
       vectorsLinks.destroyFeatures(toDelFeatures);
    else
       vectorsLinks.destroyFeatures();
}

function addGeom(feat, geom) {
    //console.log(feat);
    //console.log(geom);
    toDeleteFeatures = new Array();
    feat.attributes.la.style = { display : 'none' };
    feat.attributes.lb.style = { display : 'none' };
     
    var linkFeature = wkt.read(geom);
    //console.log("Link feature "+linkFeature);
    //alert(resp.geom);
    if (Object.prototype.toString.call(linkFeature) === '[object Array]') {
        //alert('Array');
        for (var i = 0; i < linkFeature.length; i++) {
            linkFeature[i].geometry.transform(WGS84, map.getProjectionObject());
            linkFeature[i].attributes = {'a': feat.attributes.a, 'la': feat.attributes.la, 'lb': feat.attributes.lb, 'cluster': feat.attributes.cluster};
            linkFeature[i].validated = true;
            linkFeature[i].prev_fused = true;
            
            vectorsFused.addFeatures([linkFeature[i]]);
            //alert('done');
        }
        toDeleteFeatures[toDeleteFeatures.length] = feat;
    } else {
        //alert('reached');
        linkFeature.geometry.transform(WGS84, map.getProjectionObject());
        linkFeature.attributes = {'a': feat.attributes.a, 'la': feat.attributes.la, 'lb': feat.attributes.lb, 'cluster': feat.attributes.cluster};
        
        //alert('done feature '+linkFeature);
        linkFeature.prev_fused = true;
        linkFeature.validated = true;
        //alert('reached 2');
        //vectorsLinks.removeFeatures([feat]);
        //toDeleteFeatures[toDeleteFeatures.length] = feat;
        vectorsFused.addFeatures([linkFeature]);
    }

    vectorsA.redraw();
    vectorsB.redraw();
    vectorsLinks.refresh();
    vectorsFused.refresh();
    
    //return toDeleteFeatures;
}

function loadLinks()
{
    alert("luda");
    var list = document.getElementById("linksList");
    alert('listadasdasdasda');
    var listItem = list.getElementsByTagName("li");
    alert(listItem);
    for (var i = 0; i < listItem.length; i++) {
        //alert(listItem[i].innerHTML);
        alert(i);
    }
    alert("LUDAS");
}

function selectAll() {
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    for (var i = 0; i < listItem.length; i++) {
        //alert(listItem[i]);
        var listInput = listItem[i].getElementsByTagName("input");
        for (var j = 0; j < listInput.length; j++) {
            listInput[j].checked = true;
        }
    }
}

function performBatchFusion() {    
    submitLinks(true);
}

var mappings;
var schemasA = new Object;
var schemasB = new Object;
    
function initBatchFusionTable (val) {
    avail_trans = "";
    avail_meta_trans = "";
    $.each(val.geomTransforms, function (index, element) {
        avail_trans += "<option value=\""+element+"\">" + element + "</option>";
        
    });
    $.each(val.metaTransforms, function (index, element) {
        avail_meta_trans += "<option value=\""+element+"\">" + element + "</option>";
    });
    
    var s = "<p class=\"geoinfo\" id=\"link_name\">Fusion Table</p>\n" +
//" <div class=\"checkboxes\">\n"+
//" <label for=\"chk1\"><input type=\"checkbox\" name=\"chk1\" id=\"chk1\" />Flag as misplaced fusion</label><br />\n"+
//" </div>\n"+
//" Description: <textarea name=\"textarea\" style=\"width:99%;height:50px;\" class=\"centered\"></textarea>\n"+
            " <table class=\"rwd-table\" border=1 id=\"bFusionTable\">\n" +
            " <tr>\n" +
            " <td>Value from " + $('#idDatasetA').val() + "</td>\n" +
            " <td>Predicate</td>\n" +
            " <td>Value from " + $('#idDatasetB').val() + "</td>\n" +
            " <td>Action</td>\n" +
//" <td style=\"width:20%; text-align: center;\" align=\"left\" valign=\"bottom\">Result</td>\n"+
            " </tr>\n" +
            " <tr>\n" +
            " <td title=\"" + "WKT Geometry" + "\">" + "WKT Geometry" + "</td>\n" +
            " <td>asWKT</td>\n" +
            " <td title=\"" + "WKT Geometry" + "\">" + "WKT Geometry" + "</td>\n" +
            " <td><select id=\"bgeoTrans\" style=\"color: black; width: 100%;\">" + avail_trans + "</select></td>\n" +
//" <td style=\"width:216; text-align: center;\" align=\"left\" valign=\"bottom\">Fused Geom</td>\n"+
            " </tr>\n" +
            " </table>" +
            " <table border=0 id=\"bshiftPanel\">" +
            " <tr>\n" +
            " <td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Shift (%):</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input style=\"width:100px;\" type=\"text\" id=\"bshift\" name=\"bshift\" value=\"100\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Scale:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:100px;\" id=\"bscale_fac\" name=\"bx_scale\" value=\"1.0\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Rotate:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:100px;\" id=\"brotate_fac\" name=\"bx_rotate\" value=\"0.0\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Global A Offset X:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input style=\"width:100px;\" type=\"text\" id=\"offset-x-a\" name=\"offset-x-a\" value=\"0.0\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Global A Offset Y:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:100px;\" id=\"offset-y-a\" name=\"offset-y-a\" value=\"0.0\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Global B Offset Y:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:100px;\" id=\"offset-x-b\" name=\"offset-x-b\" value=\"0.0\"/></td>\n" +
            " </tr><tr><td style=\"white-space: nowrap; width:100px; text-align: center;\" align=\"left\" valign=\"center\">Global B Offset Y:</td>\n" +
            " <td style=\"width:150px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:100px;\" id=\"offset-y-b\" name=\"offset-y-b\" value=\"0.0\"/></td>\n" +
            " </tr>\n" +
            " </table>" +
            " <input id=\"bfuseButton\" type=\"submit\" value=\"Fuse\" style=\"float:right\" onclick=\"return false;\"/>\n";

    document.getElementById("batchFusionTable").innerHTML = s;
    
    $('#bfuseButton').click(performBatchFusion);
    $('#bgeoTrans option[value="ShiftAToB"]').attr('selected', 'selected');
    var preSelected = $('#bgeoTrans').find("option:selected").text();
    if (preSelected !== "ShiftAToB" && preSelected !== "ShiftBToA") {
        $('#bscale_fac').attr('disabled', 'disabled');
        $('#brotate_fac').attr('disabled', 'disabled');
        $('#bshift').attr('disabled', 'disabled');
        
        $('#offset-x-a').attr('disabled', 'disabled');
        $('#offset-y-a').attr('disabled', 'disabled');
        $('#offset-x-b').attr('disabled', 'disabled');
        $('#offset-y-b').attr('disabled', 'disabled');
    }

    $('#bgeoTrans').change(function () {
        //alert( $(this).find("option:selected").text() );      
        var selection = $(this).find("option:selected").text();
        if (selection === "ShiftAToB" || selection === "ShiftBToA") {
            $('#bscale_fac').removeAttr('disabled');
            $('#brotate_fac').removeAttr('disabled');
            $('#bshift').removeAttr('disabled');
            
            $('#offset-x-a').attr('disabled', 'disabled');
            $('#offset-y-a').attr('disabled', 'disabled');
            $('#offset-x-b').attr('disabled', 'disabled');
            $('#offset-y-b').attr('disabled', 'disabled');
            
        } else if (selection === "Keep A" || selection === "Keep B" || selection === "Keep both") {
            $('#offset-x-a').removeAttr('disabled');
            $('#offset-y-a').removeAttr('disabled');
            $('#offset-x-b').removeAttr('disabled');
            $('#offset-y-b').removeAttr('disabled');
            
            $('#bscale_fac').attr('disabled', 'disabled');
            $('#brotate_fac').attr('disabled', 'disabled');
            $('#bshift').attr('disabled', 'disabled');
            
        } else {
            $('#bscale_fac').attr('disabled', 'disabled');
            $('#brotate_fac').attr('disabled', 'disabled');
            $('#bshift').attr('disabled', 'disabled');
            
            $('#offset-x-a').attr('disabled', 'disabled');
            $('#offset-y-a').attr('disabled', 'disabled');
            $('#offset-x-b').attr('disabled', 'disabled');
            $('#offset-y-b').attr('disabled', 'disabled');
        }
    });
}

function schemaMatch() {
    enableSpinner();
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    var links = new Array();
    $('#linksList input:checked').each(function() {
        //alert(($(this).parent().html()));
        //alert(($(this).text()));
        //alert(getText($(this).get(0)));
        links[links.length] = getText( $(this).parent().get(0) );
    });
    /*
    for (var i = 0; i < listItem.length; i++) {
        //alert(listItem[i]);
        var listLabel = listItem[i].getElementsByTagName("label");
        for (var j = 0; j < listLabel.length; j++) {
            //alert("Label Last Child : "+listLabel[j].lastChild.data);
            links[links.length] = listLabel[j].lastChild.data;
        }
    }
    */
    $("#batch-toggle-table").css("display", "inline");
    $("#matchingMenu").trigger('click');
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "SchemaMatchServlet",
        // the data to send (will be converted to a query string)
        data: {links: links},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            mappins = responseJson;
            var modA = 1;
            var modB = 1;
            var schemaListA = document.getElementById("schemasA");
            var linkMatchList = document.getElementById("linkMatchList");
            //linkMatchList.innerHTML = "";
            schemaListA.innerHTML = "";
            
            document.getElementById("bFusionOptions").style.display = 'none';
            
            initBatchFusionTable(responseJson);
            
            $.each(responseJson.foundA, function (index, element) {
                var opt = document.createElement("li");
                //console.log(opt);
                var optlbl = document.createElement("div");
                $(optlbl).addClass("scored");
                optlbl.innerHTML = "";
                //alert(index);
                /*var tokens = index.split(",");
                 for (var i = 0; i < tokens.length; i++) {
                 
                 }*/
                var tokens = index.split(",");
                var result_str = "";
                //alert(tokens);
                for (var i = 0; i < tokens.length; i++) {
                    var trunc_pos = tokens[i].lastIndexOf("#");
                    var trunc = tokens[i];
                    if (trunc_pos < 0)
                        trunc_pos = tokens[i].lastIndexOf("/");
                    if (trunc_pos >= 0)
                        trunc = tokens[i].substring(trunc_pos + 1);

                    result_str += trunc;
                    if (i != (tokens.length - 1)) {
                        result_str += ","
                    }
                }
                //alert(result_str);
                
                opt.innerHTML = decodeURIComponent(result_str);
                //alert(index);
                opt.long_name = index;
                //alert(opt.long_name);
                opt.onclick = propSelectedA;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListA.appendChild(opt);
            });
            var schemaListB = document.getElementById("schemasB");
            schemaListB.innerHTML = "";
            $.each(responseJson.foundB, function (index, element) {
                var opt = document.createElement("li");
                var optlbl = document.createElement("div");
                $(optlbl).addClass("scored");
                optlbl.innerHTML = "";
                var tokens = index.split(",");
                var result_str = "";
                for (var i = 0; i < tokens.length; i++) {
                    var trunc_pos = tokens[i].lastIndexOf("#");
                    var trunc = tokens[i];
                    if (trunc_pos < 0)
                        trunc_pos = tokens[i].lastIndexOf("/");
                    if (trunc_pos >= 0)
                        trunc = tokens[i].substring(trunc_pos + 1);

                    result_str += trunc;
                    if (i != (tokens.length - 1)) {
                        result_str += ","
                    }
                }
                
                opt.innerHTML = decodeURIComponent(result_str);
                //alert(index);
                opt.long_name = index;
                //alert(opt.long_name);
                opt.onclick = propSelectedB;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListB.appendChild(opt);
            });
            var optDummyA = document.createElement("li");
            var optDummyB = document.createElement("li");
            $(optDummyA).addClass("underline");
            $(optDummyB).addClass("underline");
            schemaListA.appendChild(optDummyA);
            schemaListB.appendChild(optDummyB);
            $.each(responseJson.otherPropertiesA, function (index, element) {
                var opt = document.createElement("li");
                var tokens = element.split(",");
                var result_str = "";
                for (var i = 0; i < tokens.length; i++) {
                    var trunc_pos = tokens[i].lastIndexOf("#");
                    var trunc = tokens[i];
                    if (trunc_pos < 0)
                        trunc_pos = tokens[i].lastIndexOf("/");
                    if (trunc_pos >= 0)
                        trunc = tokens[i].substring(trunc_pos + 1);

                    result_str += trunc;
                    if (i != (tokens.length - 1)) {
                        result_str += ","
                    }
                }

                opt.innerHTML = decodeURIComponent(result_str);
                opt.long_name = element;
                opt.onclick = propSelectedA;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                schemaListA.appendChild(opt);
            });
            $.each(responseJson.otherPropertiesB, function (index, element) {
                var opt = document.createElement("li");
                var tokens = element.split(",");
                var result_str = "";
                for (var i = 0; i < tokens.length; i++) {
                    var trunc_pos = tokens[i].lastIndexOf("#");
                    var trunc = tokens[i];
                    if (trunc_pos < 0)
                        trunc_pos = tokens[i].lastIndexOf("/");
                    if (trunc_pos >= 0)
                        trunc = tokens[i].substring(trunc_pos + 1);

                    result_str += trunc;
                    if (i != (tokens.length - 1)) {
                        result_str += ","
                    }
                }

                opt.innerHTML = decodeURIComponent(result_str);
                opt.long_name = element;
                opt.onclick = propSelectedB;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                schemaListB.appendChild(opt);
            });
            var node = document.createElement("li");
            var text = '<input class="match" type="text" name="lname" value="' + 'http://www.opengis.net/ont/geosparql#asWKT' + '"/>';
            //alert(selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML]);
            node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
            disableSpinner();
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

var lastSelectedFromA = null;
var lastSelectedFromB = null;
var linkLastSelectedFromA = null;
var linkLastSelectedFromB = null;
var linkBackColorA = null;
var linkBackColorB = null;
var backColorA = null;
var backColorB = null;
var clickCountA = 0;
var clickCountB = 0;

function linkPropSelectedA() {
    //alert(linkMatchesJSON);
    if (this.prev_selected === true) {      
        
        if (!window.event.ctrlKey) {
            var elems = linkMatchesJSON.m.foundA[this.long_name];

            var list = document.getElementById("linkSchemasB");
            var listItems = list.getElementsByTagName("li");

            if (typeof elems != 'undefined') {
                $.each(elems, function (index, element) {
                    //alert(element);
                    $.each(listItems, function (index1, element1) {
                        //alert("alert");
                        if (element1.long_name == element.rep) {

                            if (element1.match_count > 0)
                                element1.match_count--;

                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }

            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
                this.style.backgroundColor = this.backColor;
        } else {
            this.style.backgroundColor = this.backColor;
        }
        linkLastSelectedFromA = null;
        this.prev_selected = false;
        
        return;
    } else {        
        this.style.backgroundColor = "blueviolet";
        
        /* to be removed for m to n */
        if(linkLastSelectedFromA !== null) {
            //alert(linkLastSelectedFromA.long_name);
            var elems = linkMatchesJSON.m.foundA[linkLastSelectedFromA.long_name];
            
            var list = document.getElementById("linkSchemasB");
            var listItems = list.getElementsByTagName("li");
        
            $.each(listItems, function (index1, element1) {
                //alert(element1.backColor);
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                if (typeof elems !== "undefined") {
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            
                            if (element1.match_count > 0)
                                element1.match_count--;
                            
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;
                            else if (element1.prev_selected)
                                element1.style.backgroundColor = "blueviolet";
                            
                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                }
            });
            
            if ( window.event.ctrlKey ) {
                linkLastSelectedFromA = this;
                this.prev_selected = true;
                
                return;
            } else {
                linkLastSelectedFromA.prev_selected = false;
                linkLastSelectedFromA.style.backgroundColor = linkLastSelectedFromA.backColor;
                linkLastSelectedFromA = this;
                this.prev_selected = true;
            }
        }
        var elems = linkMatchesJSON.m.foundA[this.long_name];
       
        var list = document.getElementById("linkSchemasB");
        var listItems = list.getElementsByTagName("li");
        
        if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                //alert(element1.backColor);
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                if (typeof elems !== "undefined") {
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            //alert(element1.long_name);
                            
                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = element.score;
                            element1.match_count++;
                            if (!element1.prev_selected)
                                element1.style.backgroundColor = "yellow";
                            
                        }
                    });
                }
            });
        }
        
        list = document.getElementById("linkSchemasB");
        listItems = list.getElementsByTagName("li");
        endListLoop = false;
        //if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (typeof element1.match_count != 'undefined') {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                } else {
                    element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                }
                
            });
        //}
        
        linkLastSelectedFromA = this;
        this.style.backgroundColor = "blueviolet";
        this.prev_selected = true;
    }
}

function linkPropSelectedB() {
    if (this.prev_selected === true) {      
        if (!window.event.ctrlKey) {
            var elems = linkMatchesJSON.m.foundB[this.long_name];

            var list = document.getElementById("linkSchemasA");
            var listItems = list.getElementsByTagName("li");

            if (typeof elems != 'undefined') {
                $.each(elems, function (index, element) {
                    //alert(element);
                    $.each(listItems, function (index1, element1) {
                        //alert("alert");
                        if (element1.long_name == element.rep) {

                            if (element1.match_count > 0)
                                element1.match_count--;

                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }

            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
                this.style.backgroundColor = this.backColor;
        } else {
            this.style.backgroundColor = this.backColor;
        }
        linkLastSelectedFromB = null;
        this.prev_selected = false;
        
        return;
    } else {
        this.style.backgroundColor = "blueviolet";
        
        /* to be removed for m to n */
        if(linkLastSelectedFromB !== null) {
            var elems = linkMatchesJSON.m.foundB[linkLastSelectedFromB.long_name];
            
            var list = document.getElementById("linkSchemasA");
            var listItems = list.getElementsByTagName("li");
        
            if (typeof elems != 'undefined') {
                $.each(listItems, function (index1, element1) {
                    //alert(element1.backColor);
                    if (!element1.prev_selected)
                        element1.style.backgroundColor = element1.backColor;
                    if (typeof elems !== "undefined") {
                        $.each(elems, function (index, element) {
                            if (element1.long_name == element.rep) {
                                
                                if (element1.match_count > 0)
                                    element1.match_count--;
                            
                                if (element1.match_count == 0 && !element1.prev_selected)
                                    element1.style.backgroundColor = element1.backColor;

                                var scoreLbl = element1.getElementsByTagName("div");
                                if (typeof scoreLbl[0] === "undefined") {
                                    return false;
                                }
                                scoreLbl[0].innerHTML = "";
                            }
                        });
                    }
                });
            }
            /*
            if (linkLastSelectedFromB.match_count > 0) 
                linkLastSelectedFromB.style.backgroundColor = "yellow";
            else
                linkLastSelectedFromB.style.backgroundColor = linkLastSelectedFromB.backColor;
            */
            
            if ( window.event.ctrlKey ) {
                linkLastSelectedFromB = this;
                this.prev_selected = true;
                
                return;
            } else {
                linkLastSelectedFromB.prev_selected = false;
                linkLastSelectedFromB.style.backgroundColor = linkLastSelectedFromB.backColor;
                linkLastSelectedFromB = this;
                this.prev_selected = true;
            }
        }
        var elems = linkMatchesJSON.m.foundB[this.long_name];
        
        var list = document.getElementById("linkSchemasA");
        var listItems = list.getElementsByTagName("li");
        
        if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                //alert(element1.backColor);
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                if (typeof elems !== "undefined") {
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            //alert(element1.long_name);
                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            element1.match_count++;
                            if (!element1.prev_selected)
                                element1.style.backgroundColor = "yellow";
                        }
                    });
                }
            });
        }

        list = document.getElementById("linkSchemasB");
        listItems = list.getElementsByTagName("li");
        endListLoop = false;
        //if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (typeof element1.match_count != 'undefined') {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                } else {
                    element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                }
                
            });
        //}
        
        linkLastSelectedFromB = this;
        this.style.backgroundColor = "blueviolet";
        this.prev_selected = true;
    }
}

/*
 Multiple property selection to be done with Ctrl + click
*/

function propSelectedA() {
    //alert(this.prev_selected == true);
    //alert($(this).prop("prev_selected") == true);

    if (this.prev_selected === true) {
        if (!window.event.ctrlKey) {
            
            var elems = mappins.foundA[this.long_name];
            //alert(elems);
            var list = document.getElementById("schemasB");
            var listItems = list.getElementsByTagName("li");
            if (typeof elems != 'undefined') {
                $.each(elems, function (index, element) {
                    $.each(listItems, function (index1, element1) {
                        //alert("enter");
                        if (element1.long_name == element.rep) {

                            if (element1.match_count > 0)
                                element1.match_count--;

                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }
            //alert("as");
            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
                this.style.backgroundColor = this.backColor;
        } else {
            this.style.backgroundColor = this.backColor;
        }
        lastSelectedFromA = null;
        this.prev_selected = false;
    } else {
        //this.backColor = this.style.backgroundColor;
        this.style.backgroundColor = "blueviolet";

        /* to be removed for m to n */
        if (lastSelectedFromA !== null) {
            var elems = mappins.foundA[lastSelectedFromA.long_name];
            var list = document.getElementById("schemasB");
            var listItems = list.getElementsByTagName("li");
            if (typeof elems != 'undefined') {
                $.each(listItems, function (index1, element1) {
                    //alert(element1.backColor);
                    if (!element1.prev_selected)
                        element1.style.backgroundColor = element1.backColor;
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            if ( element1.match_count > 0 )
                                element1.match_count--;
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;
                            else if (element1.prev_selected) 
                                element1.style.backgroundColor = "blueviolet";

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }
            /*
            if (lastSelectedFromA.match_count > 0)
                lastSelectedFromA.style.backgroundColor = "yellow";
            else
                lastSelectedFromA.style.backgroundColor = lastSelectedFromA.backColor;
            lastSelectedFromA.prev_selected = false;
            */
            
            if ( window.event.ctrlKey ) {
                lastSelectedFromA = this;
                this.prev_selected = true;
                
                return;
            } else {
                lastSelectedFromA.prev_selected = false;
                lastSelectedFromA.style.backgroundColor = lastSelectedFromA.backColor;
                lastSelectedFromA = this;
                this.prev_selected = true;
            }
        }
        var elems = mappins.foundA[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasB");
        var listItems = list.getElementsByTagName("li");

        var endListLoop = false;
        if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;

                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        if (element.score > scoreThreshold) {

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                endListLoop = true;
                                return false;
                            }
                            scoreLbl[0].innerHTML = element.score;
                            element1.match_count++;
                            if (!element1.prev_selected)
                                element1.style.backgroundColor = "yellow";
                        }
                    }
                });
            });
        }    
        
        list = document.getElementById("schemasA");
        listItems = list.getElementsByTagName("li");
        endListLoop = false;
        //if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (typeof element1.match_count != 'undefined') {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                } else {
                    element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                }
                
            });
        //}
        
        lastSelectedFromA = this;
        this.prev_selected = true;
        this.style.backgroundColor = "blueviolet";
    }
}

function propSelectedB() {
    if (this.prev_selected === true) {
        //alert("ho");
        if (!window.event.ctrlKey) {
            //alert("ho");vvvv
            var elems = mappins.foundB[this.long_name];
            //alert(elems);
            var list = document.getElementById("schemasA");
            var listItems = list.getElementsByTagName("li");

            if (typeof elems != 'undefined') {
                $.each(elems, function (index, element) {
                    //alert(element);
                    $.each(listItems, function (index1, element1) {
                        //alert("alert");
                        if (element1.long_name == element.rep) {

                            if (element1.match_count > 0)
                                element1.match_count--;
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }
            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
                this.style.backgroundColor = this.backColor;
        } else {
            this.style.backgroundColor = this.backColor;
        }
        lastSelectedFromB = null;
        this.prev_selected = false;
    } else {
        this.style.backgroundColor = "blueviolet";

        /* to be removed for m to n */

        if (lastSelectedFromB !== null) {
            var elems = mappins.foundB[lastSelectedFromB.long_name];
            
            var list = document.getElementById("schemasA");
            var listItems = list.getElementsByTagName("li");
            if (typeof elems != 'undefined') {
                $.each(listItems, function (index1, element1) {
                    if (!element1.prev_selected)
                        element1.style.backgroundColor = element1.backColor;
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            if ( element1.match_count > 0 )
                                element1.match_count--;
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;
                            else if (element1.prev_selected) 
                                element1.style.backgroundColor = "blueviolet";

                            var scoreLbl = element1.getElementsByTagName("div");
                            if (typeof scoreLbl[0] === "undefined") {
                                return false;
                            }
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                });
            }
            /*
            if (lastSelectedFromB.match_count > 0)
                lastSelectedFromB.style.backgroundColor = "yellow";
            else
                lastSelectedFromB.style.backgroundColor = lastSelectedFromB.backColor;
            lastSelectedFromB.prev_selected = false;
            */
           
            if ( window.event.ctrlKey ) {
                lastSelectedFromB = this;
                this.prev_selected = true;
                
                return;
            } else {
                lastSelectedFromB.prev_selected = false;
                lastSelectedFromB.style.backgroundColor = lastSelectedFromB.backColor;
                lastSelectedFromB = this;
                this.prev_selected = true;
            }
        }
        var elems = mappins.foundB[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasA");
        var listItems = list.getElementsByTagName("li");
        
        var endListLoop = false;
        if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        if (element.score > scoreThreshold) {
                            var scoreLbl = element1.getElementsByTagName("div");
                            //alert(typeof scoreLbl);
                            if (typeof scoreLbl[0] === "undefined") {
                                //alert("undefined");
                                endListLoop = true;
                                return false;
                            }
                            scoreLbl[0].innerHTML = element.score;

                            element1.match_count++;
                            if (!element1.prev_selected)
                                element1.style.backgroundColor = "yellow";
                        }
                    }
                });
            });
        }
        
        list = document.getElementById("schemasB");
        listItems = list.getElementsByTagName("li");
        endListLoop = false;
        //if (typeof elems != 'undefined') {
            $.each(listItems, function (index1, element1) {
                if (endListLoop) {
                    return false;
                }
                if (typeof element1.match_count != 'undefined') {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                } else {
                    element1.style.backgroundColor = element1.backColor;
                    element1.prev_selected = false;
                }
                
            });
        //}
        
        lastSelectedFromB = this;
        this.prev_selected = true;
        this.style.backgroundColor = "blueviolet";
    }
}

function filterLinksA( )
{
    //alert($('#typeListA').val());
    //alert($('#typeListA').text());
    var send = $('#typeListA').val();
    //alert(send);
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FilterServlet",
        // the data to send (will be converted to a query string)
        data: {"filter": send, "dataset": "A"},
        // the type of data we expect back
        dataType: "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseText) {
            var list = document.getElementById("linksList");
            list.innerHTML = responseText;
            /*//$('#connLabel').text(responseText);
             var prev = 0;
             document.getElementById("linksList").innerHTML = "<li></li>";
             for ( var i = 0; i < responseText.length; i++ )
             {
             if(responseText.charAt(i) == ',') {
             var link = responseText.substring(prev, i);
             var node=document.createElement("li");
             var text = '<div class=\"checkboxes\"><label><input type="checkbox">'+link+'<label>';
             //alert(text);
             node.innerHTML = text;
             document.getElementById("linksList").appendChild(node);
             
             prev = i + 1;
             }
             }*/
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

function filterLinksB( )
{
    //alert($('#typeListB').val());
    //alert($('#typeListB').text());

    var send = $('#typeListB').val();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FilterServlet",
        // the data to send (will be converted to a query string)
        data: {"filter": send, "dataset": "B"},
        // the type of data we expect back
        dataType: "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseText) {
            //$('#connLabel').text(responseText);
            var list = document.getElementById("linksList");
            list.innerHTML = responseText;
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

function setConnection()
{
    var values = $('#connDiv').serialize();
    enableSpinner();
    //alert( values );
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "ConnectionServlet",
        // the data to send (will be converted to a query string)
        data: values,
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            $('#connLabel').text(responseJson.message);
            disableSpinner();
            if (responseJson.statusCode == 0) {
                $("#datasetMenu").trigger('click');
            }
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

function bazi(elem) {
    //alert(elem.checked);
}

function setDatasets()
{
    var values = $('#dataDiv').serialize();
    //alert( values );
    enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "DatasetsServlet",
        // the data to send (will be converted to a query string)
        data: values,
        // the type of data we expect back
        dataType: "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseText) {
            //$('#dataLabel').text(responseText);
            disableSpinner();
            $('#dataLabel').text("Datasets accepted");
            $('#datasetNameA').html($('#idDatasetA').val());
            $('#datasetNameB').html($('#idDatasetB').val());
            $('#legendSetA').html($('#idDatasetA').val());
            $('#legendSetB').html($('#idDatasetB').val());
            $('#datasetNameA').html($('#idDatasetA').val());
            $('#datasetNameB').html($('#idDatasetB').val());
            $('#legendLinkSetA').html($('#idDatasetA').val());
            $('#legendLinkSetB').html($('#idDatasetB').val());
            scanGeometries();
            $("#linksMenu").trigger('click');
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

function scanGeometries() {
    enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "ScanGeometriesServlet",
        // the data to send (will be converted to a query string)
        //data: values,
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJSON) {
            $('#dataLabel').text(responseJSON.message);
            //alert('tom');
            disableSpinner();
            if (responseJSON.statusCode == 0)
                addFusedMapDataJson(responseJSON);
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