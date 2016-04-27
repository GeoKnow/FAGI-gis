// Attempting to be structured for once.....
var FAGI = new Object();



//BBox
var transform;

// Constants in FAGI

FAGI.PropertyConstants = {
    WKT_PROPERTY: '<fagi-gis:wkt>',
    HAS_GEOMETRY_PROPERTY: '<fagi-gis:geometry>',
    SAME_AS_PROPERTY: '<fagi-gis:same_as>',
    TYPE_PROPERTY: '<fagi-gis:type>',
};

FAGI.Constants = {
    // The property separator for FAGI
    // This only has meanig for FAGI core
    PROPERTY_SEPARATOR: '=>',
    MAX_CLUSTERS: 10,
    CLUSTER_COLORS: ['silver', 'gray', 'black', 'navy', 'maroon', 'yellow', 'olive', 'lime', 'aqua', 'teal'],
    MOVE_TRANS: 1,
    ROTATE_TRANS: 2,
    SCALE_TRANS: 3,
    // Projection of geometry for storing in Post-GIS
    WGS84: new OpenLayers.Projection("EPSG:4326"),
    // Panel animation duration 
    DURATION: 200,
    BBOX_QUERY_START_A: 'SELECT ?subject ?geometry WHERE {\n' +
            '   ?subject \n' +
            '   ?predicate\n' +
            '   ?object .\n' +
            '   ?subject \n' +
            '   ' + FAGI.PropertyConstants.HAS_GEOMETRY_PROPERTY + '\n' +
            '   ?geolink .\n' +
            '   ?geolink \n' +
            '   ' + FAGI.PropertyConstants.WKT_PROPERTY + '\n' +
            '   ?geometry \n' +
            '\n' +
            '   FILTER ( <fagi-gis:contained_in> ( ',
    BBOX_QUERY_END_A: '))\n}',
    BBOX_QUERY_START_B: 'SELECT ?subject ?geometry WHERE {\n' +
            '   ?subject \n' +
            '   ?predicate\n' +
            '   ?object .\n' +
            '   ?subject \n' +
            '   ' + FAGI.PropertyConstants.HAS_GEOMETRY_PROPERTY + '\n' +
            '   ?geolink .\n' +
            '   ?geolink \n' +
            '   ' + FAGI.PropertyConstants.WKT_PROPERTY + '\n' +
            '   ?geometry \n' +
            '\n' +
            '   FILTER ( <fagi-gis:contained_in> ( ',
    BBOX_QUERY_END_B: '))\n}',
    DEFAULT_TYPE_QUERY_A: 'SELECT ?subjectA ?subjectB WHERE {\n' +
            '   GRAPH <fagi-gis:links> {\n' +
            '       ?subjectA \n' +
            '       ' + FAGI.PropertyConstants.SAME_AS_PROPERTY + '\n' +
            '   ?subjectB \n' +
            '   } .\n' +
            '   GRAPH <fagi-gis:metadata> {\n' +
            '       ?subjectA \n' +
            '       ' + FAGI.PropertyConstants.TYPE_PROPERTY + '\n' +
            '       ?type\n' +
            '   }\n' +
            '}',
    DEFAULT_TYPE_QUERY_B: 'SELECT ?subjectA ?subjectB WHERE {\n' +
            '   GRAPH <fagi-gis:links> {\n' +
            '       ?subjectA \n' +
            '       ' + FAGI.PropertyConstants.SAME_AS_PROPERTY + '\n' +
            '       ?subjectB \n' +
            '   } .\n' +
            '   GRAPH <fagi-gis:metadata> {\n' +
            '       ?subjectA \n' +
            '       ' + FAGI.PropertyConstants.TYPE_PROPERTY + '\n' +
            '       ?type\n' +
            '   }\n' +
            '}',
    DEFAULT_QUERY: 'SELECT ?subjectA ?subjectB WHERE { \n' +
            'GRAPH <fagi-gis:links> { \n' +
            '   ?subjectA \n' +
            '   ' + FAGI.PropertyConstants.SAME_AS_PROPERTY + '\n' +
            '   ?subjectB \n' +
            '   }\n' +
            '}',
    SERVICE_BUILD: false


};

//Utility classes
FAGI.NavigationUI = {
};

FAGI.MapUI = {
    // WKT
    wkt: new OpenLayers.Format.WKT(),
    // Map
    map: new OpenLayers.Map("map", {
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
    }),
    // Clicking on the map resets all open popups
    resetAllPopups: function (e) {

        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';

        document.getElementById("popupFindLinkMenu").style.opacity = 0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';

        document.getElementById("popupValidateMenu").style.opacity = 0;
        document.getElementById("popupValidateMenu").style.display = 'none';

        document.getElementById("popupBBoxMenu").style.opacity = 0;
        document.getElementById("popupBBoxMenu").style.display = 'none';

        document.getElementById("fg-popup-batch-find-link-menu").style.opacity = 0;
        document.getElementById("fg-popup-batch-find-link-menu").style.display = 'none';

        return true;
    },
    resetMultipleSelect: function () {
        FAGI.ActiveState.mselectActive = false;
    },
    resetMapControl: function () {
        FAGI.MapUI.resetAllPopups();
        FAGI.MapUI.resetMultipleSelect();
    },
    periodicMapUpdate: function () {
        FAGI.MapUI.map.updateSize();
    }

};

FAGI.PanelsUI = {
    hideAllPanels: function ( ) {
        $("#connectionPanel").hide();
        $("#datasetPanel").hide();
        $("#fusionPanel").hide();
        $('#matchingPanel').hide();
        $("#fusionPanel").hide();
        $("#linksPanel").hide();
        $("#clusteringPanel").hide();
        $("#fg-fetch-sparql-panel").hide();
        $("#fg-user-panel").hide();
        $("#fg-user-selection-panel").hide();
        $("#previewPanel").hide();
        $("#mainPanel").hide();
        
        clearSelected();
        
        //$("#mainPanel").width("0%");
        //$("#mainPanel").height("0%");
    },
    closeOpenPanel: function () {
        FAGI.PanelsUI.hideAllPanels();
        //alert("ole");
        if (FAGI.PanelsUI.lastClickedMenu != null) {
            FAGI.PanelsUI.lastClickedMenu.data("opened", false);
            FAGI.PanelsUI.lastClickedMenu = null;

            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");
        }
        FAGI.MapUI.map.updateSize();
    },
    closeAllPanels: function (activePanel) {
        $("#connectionPanel").currentlyOpened = false;
        $("#datasetPanel").currentlyOpened = false;
        $("#fusionPanel").currentlyOpened = false;
        $("#fusionPanel").currentlyOpened = false;
        $("#linksPanel").currentlyOpened = false;
        $("#fg-user-panel").currentlyOpened = false;
        $("#fg-user-selection-panel").currentlyOpened = false;
        
        $(activePanel).currentlyOpened = false;
    },
    lastClickedMenu: null,
    panelWidth: 0

};

FAGI.PanelsUI.Editors = {
    // SPARQL Editors
    sparqlEditorA: null,
    sparqlEditorB: null,
    sparqlFetchEditorA: null,
    sparqlFetchEditorB: null

};

FAGI.MapUI.Contexts = {
    contextA: {
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
                return FAGI.Constants.CLUSTER_COLORS[feature.attributes.cluster];
            }
        }
    },
    contextB: {
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
                return FAGI.Constants.CLUSTER_COLORS[feature.attributes.cluster];
            }
        }
    },
    contextLink: {
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
                    return FAGI.Constants.CLUSTER_COLORS[feature.attributes.cluster];
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
    }

};

FAGI.MapUI.Styles = {
    styleA: new OpenLayers.Style({
        strokeColor: "${getColor}",
        strokeWidth: 3,
        pointRadius: 1,
        cursor: "pointer",
        fillColor: "${getColor}",
        fillOpacity: "${getOpacity}",
        title: "${getTitle}"
    }, {context: FAGI.MapUI.Contexts.contextA}),
    styleB: new OpenLayers.Style({
        strokeColor: "${getColor}",
        strokeWidth: 3,
        pointRadius: 1,
        cursor: "pointer",
        fillColor: "${getColor}",
        fillOpacity: "${getOpacity}",
        title: "${getTitle}"
    }, {context: FAGI.MapUI.Contexts.contextB}),
    styleLinks: new OpenLayers.Style({
        strokeColor: "${getColor}",
        cursor: "pointer",
        fillColor: "${getColor}",
        fillOpacity: "${getOpacity}",
        pointRadius: 1,
        strokeOpacity: "${getOpacity}",
        strokeWidth: 3,
        strokeDashstyle: "${getLineStyle}",
        title: '${getTitle}'
    }, {context: FAGI.MapUI.Contexts.contextLink}),
    styleBBox: {
        strokeColor: "red",
        cursor: "pointer",
        fillColor: "red",
        pointRadius: 1,
        strokeOpacity: 0.3,
        strokeWidth: 3,
        title: 'first',
        fillOpacity: 0.3
    },
    styleFused: new OpenLayers.Style({
        strokeColor: "darkblue",
        strokeWidth: 3,
        pointRadius: 3,
        cursor: "pointer",
        fillColor: "darkblue",
        strokeOpacity: 0.5,
        fillOpacity: 0.5,
        title: "${getSubject}"
    }, {context: FAGI.MapUI.Contexts.contextA})

};

FAGI.MapUI.Layers = {
    // MAp Dynamic Layers
    vectorsA: new OpenLayers.Layer.Vector('Dataset A Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(FAGI.MapUI.Styles.styleA)}),
    vectorsB: new OpenLayers.Layer.Vector('Dataset B Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(FAGI.MapUI.Styles.styleB)}),
    vectorsFused: new OpenLayers.Layer.Vector('Fused Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(FAGI.MapUI.Styles.styleFused)}),
    vectorsLinks: new OpenLayers.Layer.Vector('Links Layer', {isBaseLayer: false, styleMap: new OpenLayers.StyleMap(FAGI.MapUI.Styles.styleLinks)}),
    vectorsLinksTemp: new OpenLayers.Layer.Vector('Links Layer Temp', {isBaseLayer: false, style: FAGI.MapUI.Styles.styleLinks}),
    bboxLayer: new OpenLayers.Layer.Vector('BBox Layer', {isBaseLayer: false, style: FAGI.MapUI.Styles.styleBBox}),
    // Map Base Layers
    streetLayer: null,
    satlliteLayer: null,
    OSMLayer: null

};

FAGI.MapUI.Controls = {
    dragControlA: null,
    dragControlB: null,
    selectControl: null,
    boxControl: null,
    multipleSelector: null,
    transformControl: null,
    drawControls: null
};

FAGI.Utilities = {
    enableSpinner: function () {
        if (!FAGI.ActiveState.spinnerEnabled) {
            $("#fg-screen-dimmer").show();
            $("#fg-loading-spinner").show();
            FAGI.ActiveState.spinnerEnabled = true;
        }
    },
    disableSpinner: function () {
        if (FAGI.ActiveState.spinnerEnabled) {
            $("#fg-screen-dimmer").hide();
            $("#fg-loading-spinner").hide();
            FAGI.ActiveState.spinnerEnabled = false;
        }
    },
    getPropertyName: function (property) {
        var trunc_pos = property.lastIndexOf("#");
        var trunc = property;
        if (trunc_pos < 0)
            trunc_pos = property.lastIndexOf("/");
        if (trunc_pos >= 0)
            trunc = property.substring(trunc_pos + 1);

        return trunc;
    },
    getPropertyOntology: function (property) {
        var trunc_pos = property.lastIndexOf("#");
        var trunc = property;
        if (trunc_pos < 0)
            trunc_pos = property.lastIndexOf("/");
        if (trunc_pos >= 0)
            trunc = property.substring(0, trunc_pos);

        return trunc;
    },
    repeatNTimes: function (fn, times) {
        for (var i = 0; i < times; i++)
            fn();
    },
    
    
    // File Download
    requestDatasetFile: function () {
        alert('DownloadDatasetServlet');
        FAGI.Utilities.enableSpinner();
        FAGI.Utilities.downloadResults();
        /*$.ajax({
            url: 'DownloadDatasetServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                FAGI.Utilities.disableSpinner();
                console.log("All good " + responseText);
            },
            error: function (responseText) {
                FAGI.Utilities.disableSpinner();
                alert("All bad " + responseText);
                alert("Error");
            },
            data: {'subA': null}
            //Options to tell jQuery not to process data or worry about content-type.
        });*/
    },
   
   
    downloadURL: function (url, callback) {
        var d = new Date();
        var count = d.getTime();
        var hiddenIFrameID = 'hiddenDownloader' + count;
        var iframe = document.createElement('iframe');
        iframe.id = hiddenIFrameID;
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
        iframe.src = url;
        callback();
    },
    
    
    downloadResults: function (event) {
        var url = "DownloadDatasetServlet";
        //window.location.href = url;
        //$("#popupLogin").popup("close");
        //$( "#popupClose" ).click();
        
        FAGI.Utilities.downloadURL(url, function() { FAGI.Utilities.disableSpinner(); });
         
    }

};

FAGI.NavigationUI.Callbacks = {
};

FAGI.PanelsUI.Callbacks = {
    onUnfilterButtonPressed: function () {
        //alert($('#typeListA').val());
        //alert($('#typeListA').text());
        //alert(send);
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "FilterServlet",
            // the data to send (will be converted to a query string)
            data: {"filter": null, "dataset": ""},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {
                if (responseJson.result.statusCode == 0) {
                    $("#linksList").html(responseJson.linksHTML);
                } else {
                    alert(responseJson.result.message);
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
    }

};

FAGI.MapUI.Callbacks = {
    onClusterSelectionChange: function () {
        var selectedCluster = $(this).val();
        if (selectedCluster < 0) {
            $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
                //var assign = assigns.results[element.attributes.a];
                //element.attributes.cluster = assign.cluster;
                element.style = null;
                element.attributes.la.style = null;
                element.attributes.lb.style = null;
            });
        } else if (selectedCluster == 9999) {
            //alert("Custom Cluster");
            $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
                element.style = {display: 'none'};
                element.attributes.la.style = {display: 'none'};
                element.attributes.lb.style = {display: 'none'};
            });
            $.each(FAGI.ActiveState.activeFeatureClusterA, function (index, element) {
                element.style = null;
                element.attributes.la.style = null;
                element.attributes.lb.style = null;
            });
        } else {
            $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
                //var assign = assigns.results[element.attributes.a];
                //element.attributes.cluster = assign.cluster;
                element.style = null;
                element.attributes.la.style = null;
                element.attributes.lb.style = null;
                if (element.attributes.cluster != selectedCluster) {
                    element.style = {display: 'none'};
                    element.attributes.la.style = {display: 'none'};
                    element.attributes.lb.style = {display: 'none'};
                }
            });
        }

        FAGI.MapUI.Layers.vectorsA.redraw();
        FAGI.MapUI.Layers.vectorsB.redraw();
        FAGI.MapUI.Layers.vectorsLinks.redraw();
        //FAGI.MapUI.Layers.vectorsA.refresh();
        //FAGI.MapUI.Layers.vectorsB.refresh();
        //FAGI.MapUI.Layers.vectorsLinks.refresh();
    }

};

FAGI.MapUI.Callbacks.Linking = {
    onCreateLinkButtonPressed: function () {

        if ($('#createLinkButton').html() === "Cancel Link") {
            FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
            FAGI.ActiveState.lastPo = null;
            FAGI.ActiveState.nowPo = null;

            document.getElementById("popupTransformMenu").style.opacity = 0;
            document.getElementById("popupTransformMenu").style.display = 'none';

            FAGI.ActiveState.prevActiveFeature = null;
            FAGI.ActiveState.activeFeature = null;

            return;
        }

        if (FAGI.ActiveState.lastPo == null) {
            document.getElementById("popupTransformMenu").style.opacity = 0;
            document.getElementById("popupTransformMenu").style.display = 'none';
            FAGI.ActiveState.lastPo = FAGI.ActiveState.activeFeature.geometry.getCentroid(true);
            FAGI.ActiveState.lastPo.node = FAGI.ActiveState.activeFeature;
        } else {
            FAGI.ActiveState.nowPo = FAGI.ActiveState.activeFeature.geometry.getCentroid(true);
            FAGI.ActiveState.nowPo.node = FAGI.ActiveState.activeFeature;
            createNewLink(FAGI.ActiveState.nowPo.node, FAGI.ActiveState.lastPo.node);
        }
    },
    onBatchFindLinkButtonPressed: function () {
        document.getElementById("popupBBoxMenu").style.opacity = 0;
        document.getElementById("popupBBoxMenu").style.display = 'none';

        document.getElementById("fg-popup-batch-find-link-menu").style.opacity = 0.7;
        document.getElementById("fg-popup-batch-find-link-menu").style.display = 'inline';
        document.getElementById("fg-popup-batch-find-link-menu").style.top = FAGI.ActiveState.mouse.y;
        document.getElementById("fg-popup-batch-find-link-menu").style.left = FAGI.ActiveState.mouse.x;
    },
    onFindLinkButtonPressed: function () {
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';

        document.getElementById("popupFindLinkMenu").style.opacity = 0.7;
        document.getElementById("popupFindLinkMenu").style.display = 'inline';
        document.getElementById("popupFindLinkMenu").style.top = FAGI.ActiveState.mouse.y;
        document.getElementById("popupFindLinkMenu").style.left = FAGI.ActiveState.mouse.x;
    },
    onBatchFindLinkPopupButtonPressed: function () {
        fetchContainedAndLink();
    },
    onFindLinkPopupButtonPressed: function () {
        for (var i = 0; i < FAGI.ActiveState.activeFeature.attributes.links.length; i++)
            if (!FAGI.ActiveState.activeFeature.attributes.links[i].validated)
                return;

        FAGI.ActiveState.activeFeature.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        var requestEntity = new Object();
        requestEntity.sub = FAGI.ActiveState.activeFeature.attributes.a;
        requestEntity.ds = 'A';
        if (FAGI.ActiveState.activeFeature.layer == FAGI.MapUI.Layers.vectorsB)
            requestEntity.ds = 'B';

        document.getElementById("popupFindLinkMenu").style.opacity = 0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';

        requestEntity.geom = FAGI.MapUI.wkt.extractGeometry(FAGI.ActiveState.activeFeature.geometry.getCentroid(true));
        //alert(JSON.stringify(requestEntity));
        FAGI.ActiveState.activeFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        FAGI.Utilities.enableSpinner();
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
                createSingleUnvalidatedLinks(FAGI.ActiveState.activeFeature, responseJson);
                FAGI.Utilities.disableSpinner();
            },
            // code to run if the request fails; the raw request and
            // status codes are passed to the function
            error: function (xhr, status, errorThrown) {
                FAGI.Utilities.disableSpinner();
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

};

FAGI.ActiveState = {
    // Has the user Previewed the geometries
    linksPreviewed: false,
    // Active transformation action
    transType: FAGI.Constants.MOVE_TRANS,
    // Is multiple selectin active
    multipleEnabled: false,
    // Hollding last and current Mouse Position during
    // link creation
    lastPo: null,
    nowPo: null,
    // Cancel link creation process
    cancelLink: false,
    // Btach Mode
    batchMode: false,
    // Clusters created during multiple selection
    activeFeatureClusterA: {},
    activeFeatureClusterB: {},
    activeFeaturePreview: null,
    // Constantly updated with the mouse position
    mouse: {x: 0, y: 0},
    current_tip: {x: 0, y: 0},
    // Is a feature currently selected
    current_feature: null,
    feature_is_selected: false,
    previewed: false,
    mselectActive: false,
    // Temp OpenLayers features
    polygonFeature: null,
    polygonFeatureW: null,
    // Monitors active selection of geometries
    activeFeature: null,
    prevActiveFeature: null,
    // Updated during dragging for the corresponding dataset
    // Only used during batch fusion actions
    globalOffsetAX: 0.0,
    globalOffsetAY: 0.0,
    globalOffsetBX: 0.0,
    globalOffsetBY: 0.0,
    globalOffsetVecAX: 0.0,
    globalOffsetVecAY: 0.0,
    globalOffsetVecBX: 0.0,
    globalOffsetVecBY: 0.0,
    // Used durng dragging to monitor actively
    // dragged geimetry
    selectedGeomA: null,
    selectedGeomB: null,
    // Score Threshold
    scoreThreshold: 0.3,
    // The loading Spinner that disables any FAGI Input
    spinnerEnabled: true,
    // Active SPARQL Queries
    activeQueryA: FAGI.Constants.DEFAULT_QUERY,
    activeQueryB: FAGI.Constants.DEFAULT_QUERY,
    activeBBoxQueryA: FAGI.Constants.DEFAULT_QUERY,
    activeBBoxQueryB: FAGI.Constants.DEFAULT_QUERY,
    lastMatchedSchemaClicked: null,
    lastLinkMatchedSchemaClicked: null

};

//$.ajaxSetup({cache: false});

$(window).load(function () {
});

function resizedw() {
    // Haven't resized in 100ms!
    //alert("done");
    firstRe = true;
}

var doit;
var firstRe = true;

// On page load
$(document).ready(function () {

    FAGI.Utilities.disableSpinner();

    /* NEW INTERFACE */
    FAGI.PanelsUI.hideAllPanels();

    FAGI.PanelsUI.dialogWidth = $("#dialog").width();

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
    $("#clusterCount").val($("#slider").slider("value"));
    $("#connVecDirCheck").buttonset();
    $("#connVecLenCheck").buttonset();
    $("#connCoverageCheck").buttonset();
    $("#bFusionToggle").buttonset();
    $(".ui-buttonset .ui-button-text").css("line-height", "0.5");

    // Batch Mode Controls and Callbacks
    $("#bFusionToggle").data("batchMode", false);
    $("#batch-on-radio").click(function () {
        FAGI.ActiveState.batchMode = true;
        $("#bFusionToggle").data("batchMode", true);
        $("#bFusionOptions").css("display", "inline");

        return true;
    });

    $("#batch-off-radio").click(function () {
        FAGI.ActiveState.batchMode = false;
        $("#bFusionToggle").data("batchMode", false);
        $("#bFusionOptions").css("display", "none");

        return true;
    });

    // On type select, change the query editor value
    $('#typeListA').change(function () {
        var selection = $(this).find("option:selected").text();
        var query = FAGI.Constants.DEFAULT_TYPE_QUERY_A.replace('?type', '<' + selection + '>');
        FAGI.PanelsUI.Editors.sparqlEditorA.setValue(query);
    });

    $('#typeListB').change(function () {
        var selection = $(this).find("option:selected").text();
        var query = FAGI.Constants.DEFAULT_TYPE_QUERY_B.replace('?type', '<' + selection + '>');
        FAGI.PanelsUI.Editors.sparqlEditorB.setValue(query);
    });

    $("#bboxMenu").menu();
    $("#transformMenu").menu();
    $("#validateMenu").menu();

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

    // Dropdown menu for the Tools tab
    // Setting classes
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

    // Set up close buttons for each of the popups
    $("#close-findlink-menu-btn").click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

        document.getElementById("popupFindLinkMenu").style.opacity = 0.0;
        document.getElementById("popupFindLinkMenu").style.display = 'none';
    });
    $("#fg-close-batch-findlink-menu-btn").click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

        document.getElementById("fg-popup-batch-find-link-menu").style.opacity = 0.0;
        document.getElementById("fg-popup-batch-find-link-menu").style.display = 'none';
    });
    $("#close-bbox-menu-btn").click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

        FAGI.MapUI.Layers.bboxLayer.destroyFeatures();

        document.getElementById("popupBBoxMenu").style.opacity = 0.0;
        document.getElementById("popupBBoxMenu").style.display = 'none';
    });
    $("#close-validate-menu-btn").click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

        document.getElementById("popupValidateMenu").style.opacity = 0.0;
        document.getElementById("popupValidateMenu").style.display = 'none';
    });
    $("#close-transform-menu-btn").click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

        document.getElementById("popupTransformMenu").style.opacity = 0.0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });

    var options = {
        numZoomLevels: 32,
        projection: "EPSG:3857",
        maxExtent: new OpenLayers.Bounds(-200000, -200000, 200000, 200000),
        center: new OpenLayers.LonLat(-12356463.476333, 5621521.4854095)
    };

    var vaseLayerStreets = new OpenLayers.Layer.Google(
            "Google Streets", // the default
            {'sphericalMercator': true,
                'numZoomLevels': 32,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });

    // Set up FAGI base map layers
    FAGI.MapUI.Layers.streetLayer = new OpenLayers.Layer.Google("Google Streets",
            {'sphericalMercator': true,
                'units': 'km',
                'numZoomLevels': 32,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });
    FAGI.MapUI.Layers.satlliteLayer = new OpenLayers.Layer.Google("Google Satellite",
            {'sphericalMercator': true,
                'numZoomLevels': 32,
                'type': google.maps.MapTypeId.SATELLITE,
                'maxExtent': new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34)
            });

    FAGI.MapUI.Layers.OSMLayer = new OpenLayers.Layer.OSM("OSM", "", {isBaseLayer: true, zoomOffset: 0, 'numZoomLevels': 40});

    // Add to map
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.OSMLayer);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.satlliteLayer);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.streetLayer);

    //FAGI.MapUI.map.setBaseLayer(FAGI.MapUI.Layers.satlliteLayer);

    /*
     FAGI.MapUI.Layers.satlliteLayer.redraw();
     FAGI.MapUI.Layers.satlliteLayer.setVisibility(true);
     FAGI.MapUI.map.zoomTo(6);
     FAGI.MapUI.map.addControl(new OpenLayers.Control.LayerSwitcher());
     */

    var inside_panel = new OpenLayers.Control.Panel({
        displayClass: 'insidePanel'
    });
    FAGI.MapUI.map.addControl(inside_panel);
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

    //Proj4js.reportError = function(msg) {alert(msg);};
// Add an instance of the Click control that listens to various click events:
    var oClick = new OpenLayers.Control.Click({eventMethods: {
            'rightclick': function (e) {
                if (FAGI.ActiveState.lastPo != null) {
                    FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
                    FAGI.ActiveState.lastPo = null;
                    FAGI.ActiveState.nowPo = null;

                    document.getElementById("popupTransformMenu").style.opacity = 0;
                    document.getElementById("popupTransformMenu").style.display = 'none';

                    FAGI.ActiveState.prevActiveFeature = null;
                    FAGI.ActiveState.activeFeature = null;

                    //return;
                }
            }
        }});
    FAGI.MapUI.map.addControl(oClick);
    oClick.activate();

    inside_panel.addControls([zoom_max_inside]);

    FAGI.MapUI.Controls.boxControl = new OpenLayers.Control.DrawFeature(FAGI.MapUI.Layers.bboxLayer, OpenLayers.Handler.RegularPolygon, {
        handlerOptions: {
            sides: 4,
            snapAngle: 90,
            irregular: true,
            persist: true
        }
    });

    FAGI.MapUI.Controls.boxControl.handler.callbacks.done = endDragBox;
    FAGI.MapUI.map.addControl(FAGI.MapUI.Controls.boxControl);
    //FAGI.MapUI.Controls.boxControl.activate();
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.bboxLayer);

    transformControl = new OpenLayers.Control.TransformFeature(FAGI.MapUI.Layers.vectorsB, {
        rotate: true,
        irregular: true
    });
    transformControl.events.register("transformcomplete", transformControl, transDone);
    FAGI.MapUI.map.addControl(transformControl);

    transform = new OpenLayers.Control.TransformFeature(FAGI.MapUI.Layers.bboxLayer, {
        rotate: true,
        irregular: true
    });
    transform.events.register("transformcomplete", transform, boxResize);
    FAGI.MapUI.map.addControl(transform);

    FAGI.MapUI.map.events.register("mousemove", FAGI.MapUI.map, function (e) {
        FAGI.ActiveState.mouse.x = e.pageX;
        FAGI.ActiveState.mouse.y = e.pageY;
        if (FAGI.ActiveState.lastPo != null) {
            var curPo = new OpenLayers.Geometry.Point();
            e.xy.y += 3;
            e.xy.x += 3;
            curPo.x = FAGI.MapUI.map.getLonLatFromPixel(e.xy).lon;
            curPo.y = FAGI.MapUI.map.getLonLatFromPixel(e.xy).lat;

            var line2 = new OpenLayers.Geometry.LineString([FAGI.ActiveState.lastPo, curPo]);
            //alert('Length ' + line2.getLength());
            //alert('Length Geo ' + line2.getGeodesicLength(FAGI.MapUI.map.getProjectionObject()));
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
            FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
            FAGI.MapUI.Layers.vectorsLinksTemp.addFeatures([linkFeature]);
            FAGI.MapUI.Layers.vectorsLinksTemp.drawFeature(linkFeature);
        }
        //alert(FAGI.ActiveState.mouse.x+' '+FAGI.ActiveState.mouse.y);
    });
    FAGI.MapUI.map.events.register("mouseover", FAGI.MapUI.map, function (e) {
        //alert("mousein");
    });
    FAGI.MapUI.map.events.on({"zoomend": function () {
            //alert("mouseroll");
        }});
    FAGI.MapUI.map.zoomToProxy = FAGI.MapUI.map.zoomTo;
    FAGI.MapUI.map.zoomTo = function (zoom, xy) {
        // if you want zoom to go through call
        //alert(zoom);
        //alert(xy)
        ;       // 
        if (FAGI.ActiveState.transType == FAGI.Constants.MOVE_TRANS) {
            FAGI.MapUI.map.zoomToProxy(zoom, xy);
        }
        //else do nothing and map wont zoom
    };
    // On map click, close any open popup
    FAGI.MapUI.map.events.register("click", FAGI.MapUI.map, FAGI.MapUI.resetMapControl);

    OpenLayers.Control.MouseWheel = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'cumulative': true
        },
        initialize: function (options) {
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
        onPause: function (evt) {
            //alert(evt);
        }
    });

    var nav = new OpenLayers.Control.Navigation({'zoomWheelEnabled': true});
    nav.wheelChange = luda;
    FAGI.MapUI.map.addControl(nav);

    var controlers = {
        "cumulative": new OpenLayers.Control.MouseWheel({
            handlerOptions: {
                "cumulative": true
            }
        })
    };

    var control;
    for (var key in controlers) {
        control = controlers[key];
        // only to route output here
        control.key = key;
        //alert(key);
        FAGI.MapUI.map.addControl(control);
    }
    control.activate();
    var wms3 = new OpenLayers.Layer.WMS("OpenLayers WMS", {isBaseLayer: true},
    "http://vmap0.tiles.osgeo.org/wms/vmap0", {layers: 'basic'});


    // allow testing of specific renderers via "?renderer=Canvas", etc
    var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
    renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;

    vectors = new OpenLayers.Layer.Vector("Vector Layer", {isBaseLayer: false,
        renderers: renderer
    });


    FAGI.MapUI.map.addControl(new OpenLayers.Control.LayerSwitcher());
    FAGI.MapUI.map.addControl(new OpenLayers.Control.MousePosition());

    FAGI.MapUI.Controls.dragControlA = new OpenLayers.Control.DragFeature(FAGI.MapUI.Layers.vectorsA, {
        onEnter: onFeatureOver,
        onStart: startDragA,
        onDrag: doDragA,
        onComplete: endDragA
    });
    FAGI.MapUI.Controls.dragControlB = new OpenLayers.Control.DragFeature(FAGI.MapUI.Layers.vectorsB, {
        onEnter: onFeatureOver,
        onStart: startDragB,
        onDrag: doDragB,
        onComplete: endDragB
    });
    //FAGI.MapUI.Controls.dragControlA.moveFeature = onFeatureOver2;
    FAGI.MapUI.map.addControls([FAGI.MapUI.Controls.dragControlA, FAGI.MapUI.Controls.dragControlB]);

    FAGI.MapUI.Controls.dragControlA.activate();
    FAGI.MapUI.Controls.dragControlB.activate();
    FAGI.MapUI.map.setCenter(new OpenLayers.LonLat(0, 0), 3);
    var select = new OpenLayers.Control.SelectFeature(vectors, {
        onSelect: addSelected,
        onUnselect: clearSelected
    });

    // aDD lAYERS TO mAP
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.vectorsA);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.vectorsB);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.vectorsLinks);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.vectorsLinksTemp);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.bboxLayer);
    FAGI.MapUI.map.addLayer(FAGI.MapUI.Layers.vectorsFused);

    // Create a mutliple select layer and associate it
    // with the links layer
    var lays = [FAGI.MapUI.Layers.vectorsLinks];
    FAGI.MapUI.Controls.multipleSelector = new OpenLayers.Control.SelectFeature(lays, {
        box: true,
        multiple: true
    });
    FAGI.MapUI.map.addControl(FAGI.MapUI.Controls.multipleSelector);

    drawControls = {
        line: new OpenLayers.Control.DrawFeature(FAGI.MapUI.Layers.vectorsLinks,
                OpenLayers.Handler.Path)
    };

    for (var key in drawControls) {
        FAGI.MapUI.map.addControl(drawControls[key]);
    }

    // Add laeyrs with appropriate depth indexes
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.vectorsA, 3);
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.vectorsB, 4);
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.vectorsLinks, 5);
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.vectorsLinksTemp, 6);
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.vectorsFused, 2);
    FAGI.MapUI.map.setLayerIndex(FAGI.MapUI.Layers.bboxLayer, 7);

    // Create a select control for all Layers and
    // add it to the map
    var laysAll = [FAGI.MapUI.Layers.vectorsA, FAGI.MapUI.Layers.vectorsB, FAGI.MapUI.Layers.vectorsFused, FAGI.MapUI.Layers.vectorsLinks, FAGI.MapUI.Layers.bboxLayer];
    FAGI.MapUI.Controls.selectControl = new OpenLayers.Control.SelectFeature(laysAll, {
        highlightOnly: true,
        toggle: true,
        hover: false});
    FAGI.MapUI.map.addControl(FAGI.MapUI.Controls.selectControl);
    FAGI.MapUI.Controls.selectControl.activate();

    // Set layer specific on select callbacks
    FAGI.MapUI.Layers.vectorsA.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        'featureclick': function (e) {
            alert('thee');
        },
        scope: FAGI.MapUI.Layers.vectorsA
    });
    FAGI.MapUI.Layers.vectorsB.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        'featureclick': function (e) {
            alert('thee');
        },
        scope: FAGI.MapUI.Layers.vectorsB
    });
    FAGI.MapUI.Layers.vectorsLinks.events.on({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });
    FAGI.MapUI.Layers.vectorsFused.events.on({
        'featureselected': onFusedSelect,
        'featureunselected': onFusedUnselect,
        scope: FAGI.MapUI.Layers.vectorsFused
    });
    FAGI.MapUI.Layers.bboxLayer.events.on({
        'featureselected': onBBoxSelect,
        'featureunselected': onBBoxUnselect,
        scope: FAGI.MapUI.Layers.bboxLayer
    });

    // Debug code for testing rendering on mao
    /*
     //var polygonFeature = FAGI.MapUI.wkt.read("POLYGON(15.37412 51.32847,15.374159 51.328592,15.374441 51.328552,15.374586 51.328532,15.374659 51.328521,15.37462 51.328399,15.37412 51.32847)");
     polygonFeature = FAGI.MapUI.wkt.read("POLYGON((20 37, 20 39, 22 39, 22 37, 20 37))");
     //polygonFeatureT1 = FAGI.MapUI.wkt.read("POLYGON((-74.0085826803402 40.7421376304449,-74.0084536803402 40.7420846304449,-74.0086146803402 40.7418666304449,-74.0086036803402 40.7418596304449,-74.0086136803402 40.7418486304449,-74.0086386803402 40.7418596304449,-74.0085826803402 40.7421376304449))");
     polygonFeatureT1 = FAGI.MapUI.wkt.read("POLYGON((-74.008619 40.7421519999998,-74.00849 40.7420989999998,-74.008651 40.7418809999998,-74.00864 40.7418739999998,-74.00865 40.7418629999998,-74.008675 40.7418739999998,-74.008619 40.7421519999998))");
     polygonFeatureT = FAGI.MapUI.wkt.read("LINESTRING(-74.008617 40.741867,-74.008629 40.741872,-74.008581 40.742146,-74.008452 40.742091,-74.008601 40.741889,-74.008617 40.741867)");
     //polygonFeatureT = FAGI.MapUI.wkt.read("LINESTRING(-74.0087622786393 40.7419244782195,-74.0087742786393 40.7419294782196,-74.0087262786393 40.7422034782196,-74.0085972786393 40.7421484782195,-74.0087462786393 40.7419464782195,-74.0087622786393 40.7419244782195)");
     
     polygonFeature.attributes.a = "asasa";
     //polygonFeature.style = FAGI.MapUI.Styles.styleA;
     polygonFeatureW = FAGI.MapUI.wkt.read("POLYGON((24 41, 24 43, 26 43, 26 41, 24 41))");
     //polygonFeatureW = FAGI.MapUI.wkt.read("POLYGON((20.000001 37.000001, 20.000001 39.000001, 22.000001 39.000001, 22.000001 37.000001, 20.000001 37.000001))");
     //var polygonFeature = FAGI.MapUI.wkt.read("POINT(-25.8203125 2.4609375)");
     polygonFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
     polygonFeatureT.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
     polygonFeatureT1.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
     //FAGI.MapUI.Layers.vectorsFused.addFeatures([polygonFeature]);
     //alert("OL");
     var polygonFeature2 = FAGI.MapUI.wkt.read("POLYGON((20.7240456428877 37.9908366236946,20.7241422428877 37.9906675236946,20.7238686428877 37.9905829236946,20.7238364428877 37.9908197236946,20.7240456428877 37.9908366236946))");
     polygonFeatureW.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
     
     var start_point = polygonFeature.geometry.getCentroid(true);
     var end_point = polygonFeatureW.geometry.getCentroid(true);
     var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
     linkFeature.attributes = {'la': polygonFeature, 'a': 'ludacris', 'lb': polygonFeatureW, 'cluster': 'Unset', 'opacity': 0.8};
     var links = [];
     //links.push(linkFeature);
     polygonFeature.attributes = {'a': 'tomaras', 'links': links, 'cluster': 'Unset', 'opacity': 0.3};
     polygonFeatureW.attributes = {'a': 'tomaras2', 'links': links, 'cluster': 'Unset', 'opacity': 0.3};
     //FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
     //window.setInterval(function() {rotateFeature(polygonFeature, 360 / 20, polygonFeature.geometry.getCentroid(true));}, 100);
     //FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureW]);
     //FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeature, polygonFeatureT, polygonFeatureT1]);
     */

    FAGI.MapUI.map.zoomToMaxExtent();
    FAGI.MapUI.map.updateSize();
    FAGI.MapUI.map.render('map');
});

function newPolygonAdded(a, b) {
    //alert("clicked");
}

function enableBBoxTransform() {
    //alert('transform');
    transform.activate();
}

var activeBBox;
function fetchContainedAndLink() {
    var feature = new OpenLayers.Feature.Vector(activeBBox.toGeometry());
    feature.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);

    var bboxJSON = new Object();
    bboxJSON.barea = FAGI.MapUI.wkt.write(feature);
    bboxJSON.left = feature.geometry.getBounds().left;
    bboxJSON.bottom = feature.geometry.getBounds().bottom;
    bboxJSON.right = feature.geometry.getBounds().right;
    bboxJSON.top = feature.geometry.getBounds().top;

    feature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

    document.getElementById("fg-popup-batch-find-link-menu").style.opacity = 0;
    document.getElementById("fg-popup-batch-find-link-menu").style.display = 'none';

    FAGI.Utilities.enableSpinner();
    //alert('Radius ' + $("#fg-batch-radius-spinner").spinner("value"));
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "BatchFindLinkServlet",
        // the data to send (will be converted to a query string)
        data: {bboxJSON: JSON.stringify(bboxJSON), radius: $("#fg-batch-radius-spinner").spinner("value")},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseJson);
            //alert(responseJson);
            //FAGI.MapUI.Layers.bboxLayer.destroyFeatures();
            //addUnlinkedMapDataJson(responseJson);
            FAGI.MapUI.Layers.bboxLayer.destroyFeatures();
            addUnlinkedMapDataJsonWithLinks(responseJson);
            FAGI.Utilities.disableSpinner();
            //$('#popupBBoxMenu').hide();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            FAGI.Utilities.disableSpinner();
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
    feature.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
    //alert(feature.geometry.getBounds());
    var bboxJSON = new Object();
    bboxJSON.barea = FAGI.MapUI.wkt.write(feature);
    bboxJSON.left = feature.geometry.getBounds().left;
    bboxJSON.bottom = feature.geometry.getBounds().bottom;
    bboxJSON.right = feature.geometry.getBounds().right;
    bboxJSON.top = feature.geometry.getBounds().top;
    //alert('Bounding Area Poly '+bboxJSON.barea);
    //JSON.stringify(bboxJSON);
    feature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

    var qASend = "";
    if (FAGI.PanelsUI.Editors.sparqlFetchEditorA != null)
        qASend = FAGI.PanelsUI.Editors.sparqlFetchEditorA.getValue();
    var qBSend = "";
    if (FAGI.PanelsUI.Editors.sparqlFetchEditorB != null)
        qBSend = FAGI.PanelsUI.Editors.sparqlFetchEditorB.getValue();

    FAGI.Utilities.enableSpinner();
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
            FAGI.MapUI.Layers.bboxLayer.destroyFeatures();
            addUnlinkedMapDataJson(responseJson);

            document.getElementById("popupBBoxMenu").style.opacity = 0;
            document.getElementById("popupBBoxMenu").style.display = 'none';

            FAGI.Utilities.disableSpinner();
            //$('#popupBBoxMenu').hide();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            FAGI.Utilities.disableSpinner();
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
    FAGI.MapUI.Controls.boxControl.deactivate();
}

function boxResize(event) {
    //alert(event.feature.geometry.bounds);
    transform.deactivate();
}

function onBBoxSelect(e) {
    //alert('bbox select');
    document.getElementById("popupBBoxMenu").style.opacity = 0.7;
    //feature_is_selected = false;
    document.getElementById("popupBBoxMenu").style.display = 'inline';
    document.getElementById("popupBBoxMenu").style.top = FAGI.ActiveState.mouse.y;
    document.getElementById("popupBBoxMenu").style.left = FAGI.ActiveState.mouse.x;
}

function onBBoxUnselect(event) {
    //alert('bbox unselect');
    FAGI.MapUI.Layers.bboxLayer.destroyFeatures();
}

function transDone() {
    alert(event.feature.geometry.bounds);
}

function drawBox(bounds) {
    var feature = new OpenLayers.Feature.Vector(bounds.toGeometry());

    FAGI.MapUI.Layers.bboxLayer.addFeatures(feature);
    //transform.setFeature(feature);
}

function clearSelected(event) {
    if (FAGI.ActiveState.multipleEnabled) {
        //alert('multiple end');
        //setSingleMapControls();
        FAGI.ActiveState.multipleEnabled = false;
        FAGI.MapUI.Controls.multipleSelector.unselectAll();

        FAGI.MapUI.Controls.multipleSelector.deactivate();
        FAGI.MapUI.Controls.selectControl.activate();

        // register multiple feature callbacks
        FAGI.MapUI.Layers.vectorsLinks.events.un({
            'featureselected': addSelected,
            'featureunselected': clearSelected,
            scope: FAGI.MapUI.Layers.vectorsLinks
        });

        FAGI.MapUI.Layers.vectorsLinks.events.on({
            'featureselected': onLinkFeatureSelect,
            'featureunselected': onLinkFeatureUnselect,
            scope: FAGI.MapUI.Layers.vectorsLinks
        });

        //alert(JSON.stringify(FAGI.ActiveState.activeFeatureClusterA));
        //if ( activeFeatureCluster.length === 0 ) {

        //}

        //activeFeatureCluster = new Array();
    }
}

function addSelected(event) {
    if (typeof FAGI.ActiveState.activeFeatureClusterA[event.feature.attributes.la.attributes.a] != "undefined")
        return;

    if (typeof FAGI.ActiveState.activeFeatureClusterB[event.feature.attributes.lb.attributes.a] != "undefined")
        return;

    console.log($('#fg-info-popup').width());
    console.log($('#map').width());

    $("#fg-info-label").html($("#fg-info-label").html() + '  ' + event.feature.attributes.la.attributes.a);
    document.getElementById("fg-info-popup").style.top = $('#map').height() - $('#map').height() * 0.95;
    document.getElementById("fg-info-popup").style.left = $('#map').width() - ($('#fg-info-popup').width() + 10);
    //$("#fg-info-popup").width($('#map').width() - ($('#fg-info-popup').width()));

    console.log($('#fg-info-popup').width());
    console.log($('#map').width());
    console.log(event.feature.attributes.la.attributes.a);
    FAGI.ActiveState.activeFeatureClusterA[event.feature.attributes.la.attributes.a] = event.feature;
    FAGI.ActiveState.activeFeatureClusterB[event.feature.attributes.lb.attributes.a] = event.feature;
}

function setMultipleMapControls() {
    // unregister single feature callbacks
    FAGI.MapUI.Layers.vectorsA.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsA
    });
    FAGI.MapUI.Layers.vectorsB.events.un({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsB
    });
    FAGI.MapUI.Layers.vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });

    // register multiple feature callbacks
    FAGI.MapUI.Layers.vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });
}

function setSingleMapControls() {
    // unregister multiple feature callbacks
    FAGI.MapUI.Layers.vectorsLinks.events.un({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });

    // register multiple feature callbacks
    FAGI.MapUI.Layers.vectorsA.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsA
    });
    FAGI.MapUI.Layers.vectorsB.events.on({
        'featureselected': onFeatureSelect,
        'featureunselected': onFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsB
    });
    FAGI.MapUI.Layers.vectorsLinks.events.on({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });
}

function activateMultipleTool() {
    //alert("multiple");
    //activeFeatureCluster = new Array();
    //alert($('#clusterSelector option[value="9999"]').length);

    expandUserSelectionPanel();

    if (!$('#clusterSelector option[value="9999"]').length)
        $("#clusterSelector").append("<option value=\"" + 9999 + "\" >Custom Cluster </option>");

    FAGI.ActiveState.multipleEnabled = true;
    FAGI.MapUI.Controls.multipleSelector.activate();
    FAGI.MapUI.Controls.selectControl.deactivate();

    FAGI.MapUI.Layers.vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });

    // register multiple feature callbacks
    FAGI.MapUI.Layers.vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });
}

function activateBBoxTool() {
    //alert("multiple");
    //activeFeatureCluster = new Array();
    //alert($('#clusterSelector option[value="9999"]').length);

    if (!$('#clusterSelector option[value="9999"]').length)
        $("#clusterSelector").append("<option value=\"" + 9999 + "\" >Custom Cluster </option>");

    FAGI.ActiveState.multipleEnabled = true;
    FAGI.MapUI.Controls.multipleSelector.activate();
    FAGI.MapUI.Controls.selectControl.deactivate();

    FAGI.MapUI.Layers.vectorsLinks.events.un({
        'featureselected': onLinkFeatureSelect,
        'featureunselected': onLinkFeatureUnselect,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });

    /*
     FAGI.MapUI.Layers.vectorsA.events.un({
     'featureselected': onFeatureSelect,
     'featureunselected': onFeatureUnselect,
     scope: FAGI.MapUI.Layers.vectorsA
     });
     FAGI.MapUI.Layers.vectorsB.events.un({
     'featureselected': onFeatureSelect,
     'featureunselected': onFeatureUnselect,
     scope: FAGI.MapUI.Layers.vectorsB
     });
     */

    // register multiple feature callbacks
    FAGI.MapUI.Layers.vectorsLinks.events.on({
        'featureselected': addSelected,
        'featureunselected': clearSelected,
        scope: FAGI.MapUI.Layers.vectorsLinks
    });

    /*
     FAGI.MapUI.Layers.vectorsA.events.on({
     'featureselected': addSelectedFromA,
     'featureunselected': clearSelectedFromA,
     scope: FAGI.MapUI.Layers.vectorsA
     });
     FAGI.MapUI.Layers.vectorsB.events.on({
     'featureselected': addSelectedFromB,
     'featureunselected': clearSelectedFromB,
     scope: FAGI.MapUI.Layers.vectorsB
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
     data: {'queryA': FAGI.PanelsUI.Editors.sparqlEditorA.getValue(), 'queryB': FAGI.PanelsUI.Editors.sparqlEditorB.getValue()}
     //Options to tell jQuery not to process data or worry about content-type.
     });
     */
}

function linksSPARQLFilter() {
    //console.log(FAGI.PanelsUI.Editors.sparqlEditorA.getValue());
    //console.log(FAGI.PanelsUI.Editors.sparqlEditorB.getValue());
    FAGI.Utilities.enableSpinner();
    $.ajax({
        url: 'SPARQLFilterServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseText) {
            FAGI.Utilities.disableSpinner();
            console.log(JSON.stringify(responseText));
            $("#linksList").html(responseText.links);
        },
        error: function (responseText) {
            FAGI.Utilities.disableSpinner();
            alert("All bad " + responseText);
            alert("Error");
        },
        data: {'queryA': FAGI.PanelsUI.Editors.sparqlEditorA.getValue(), 'queryB': FAGI.PanelsUI.Editors.sparqlEditorB.getValue()}
        //Options to tell jQuery not to process data or worry about content-type.
    });

}

function activateFecthUnlinked() {
    FAGI.MapUI.Controls.boxControl.activate();
}

function activateMultipleSelect() {
    //FAGI.MapUI.Controls.multipleSelector.activate();
    //setMultipleMapControls();
    FAGI.MapUI.Controls.boxControl.activate();
    //transform.activate();
}

function animatePanel(strech, opened) {
    var int = self.setInterval(FAGI.MapUI.periodicMapUpdate, 1);
    //alert(strech);
    //FAGI.PanelsUI.lastClickedMenu.width($("#dialog").width());

    if (!opened) {
        $(".ui-dialog-content").css("width", "100%");
        $("#map").animate({
            width: (100 - strech) + '%'
        }, {duration: FAGI.Constants.DURATION, queue: false, complete: function () {
                FAGI.MapUI.map.updateSize();
                window.clearInterval(int);
                return false;
            }});
        $(".ui-dialog").animate({
            width: strech + '%'
        }, {duration: FAGI.Constants.DURATION, queue: false, complete: function () {
                $("#fg-links-sparql-editor-a").html("");
                $("#fg-links-sparql-editor-b").html("");
                if ($(FAGI.PanelsUI.lastClickedMenu).is($("#linksPanel"))) {
                    FAGI.PanelsUI.Editors.sparqlEditorA = CodeMirror($("#fg-links-sparql-editor-a").get(0), {
                        value: FAGI.ActiveState.activeQueryA,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    FAGI.PanelsUI.Editors.sparqlEditorB = CodeMirror($("#fg-links-sparql-editor-b").get(0), {
                        value: FAGI.ActiveState.activeQueryB,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    $(".CodeMirror-vscrollbar").css("overflow-y", "hidden");
                }
                if ($(FAGI.PanelsUI.lastClickedMenu).is($("#fg-fetch-sparql-panel"))) {
                    var feature = new OpenLayers.Feature.Vector(activeBBox.toGeometry());
                    feature.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
                    var wktRep = FAGI.MapUI.wkt.write(feature);
                    $("#fg-fetch-queries-submit").prop("bbox", wktRep);
                    FAGI.PanelsUI.Editors.sparqlFetchEditorA = CodeMirror($("#fg-fetch-sparql-editor-a").get(0), {
                        value: FAGI.Constants.BBOX_QUERY_START_A + wktRep + FAGI.Constants.BBOX_QUERY_END_A,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    FAGI.PanelsUI.Editors.sparqlFetchEditorB = CodeMirror($("#fg-fetch-sparql-editor-b").get(0), {
                        value: FAGI.Constants.BBOX_QUERY_START_B + wktRep + FAGI.Constants.BBOX_QUERY_END_B,
                        styleActiveLine: true,
                        lineNumbers: true,
                        lineWrapping: true,
                        mode: "sparql",
                        matchBrackets: true
                    });
                    $(".CodeMirror-vscrollbar").css("overflow-y", "hidden");
                }

                return false;
            }});
    } else {
        $("#map").animate({
            width: '100%'
        }, {duration: FAGI.Constants.DURATION, queue: false, complete: function () {
                FAGI.MapUI.map.updateSize();
                window.clearInterval(int);
                return false;
            }});
        $(".ui-dialog").animate({
            width: '0%'
        }, {duration: FAGI.Constants.DURATION, queue: false, complete: function () {
                FAGI.PanelsUI.Editors.sparqlFetchEditorA = null;
                FAGI.PanelsUI.Editors.sparqlFetchEditorB = null;

                return false;
            }});
    }
}

function expandConnectionPanel() {
    FAGI.PanelsUI.hideAllPanels();

    //alert(FAGI.PanelsUI.lastClickedMenu != null);
    //alert($(FAGI.PanelsUI.lastClickedMenu).is($("#connectionPanel")));
    // alert($("#connectionPanel").data("opened"));

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#connectionPanel")))) {

        $("#mainPanel").show();
        $("#connectionPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#connectionPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#connectionPanel");

    } else {
        if ($("#connectionPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#connectionPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#connectionPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [30, 70],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#connectionPanel");
            $("#connectionPanel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
}

function expandUserPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#fg-user-panel")))) {

        $("#mainPanel").show();
        $("#fg-user-panel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#fg-user-panel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#fg-user-panel");

    } else {
        if ($("#fg-user-panel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#fg-user-panel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#fg-user-panel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [30, 70],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#fg-user-panel");
            $("#fg-user-panel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
}

function expandUserSelectionPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#fg-user-selection-panel")))) {

        $("#mainPanel").show();
        $("#fg-user-selection-panel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#fg-user-selection-panel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#fg-user-selection-panel");

    } else {
        if ($("#fg-user-selection-panel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#fg-user-selection-panel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#fg-user-selection-panel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [30, 70],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#fg-use-selection-panel");
            $("#fg-user-selection-panel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
}

function expandMatchingPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#matchingPanel")))) {

        $("#mainPanel").show();
        $("#matchingPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#matchingPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#matchingPanel");

    } else {
        if ($("#matchingPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#matchingPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#matchingPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [70, 30],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#matchingPanel");
            $("#matchingPanel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
    /*
     /FAGI.PanelsUI.hideAllPanels();
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#matchingPanel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#matchingPanel");
     $('#dialog').dialog('option', 'title', 'Property Matching');
     $("#matchingPanel").show();
     if ($("#matchingPanel").data("opened")) {
     animatePanel(0, $("#matchingPanel").data("opened"));
     $("#matchingPanel").data("opened", false);
     } else {
     animatePanel(70, $("#matchingPanel").data("opened"));
     $("#matchingPanel").data("opened", true);
     }
     */
}

function expandPreviewPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#previewPanel")))) {

        $("#mainPanel").show();
        $("#previewPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#previewPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#previewPanel");

    } else {
        if ($("#previewPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#previewPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#previewPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [70, 30],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#previewPanel");
            $("#previewPanel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
    /*
     FAGI.PanelsUI.hideAllPanels();
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#previewPanel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#previewPanel");
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
     */
}

function expandSPARQLFetchPanel() {
    FAGI.PanelsUI.hideAllPanels();

    document.getElementById("popupBBoxMenu").style.opacity = 0.0;
    document.getElementById("popupBBoxMenu").style.display = 'none';

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#fg-fetch-sparql-panel")))) {

        $("#mainPanel").show();
        $("#fg-fetch-sparql-panel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#fg-fetch-sparql-panel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#fg-fetch-sparql-panel");

    } else {
        if ($("#fg-fetch-sparql-panel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#fg-fetch-sparql-panel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#fg-fetch-sparql-panel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [70, 30],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#fg-fetch-sparql-panel");
            $("#fg-fetch-sparql-panel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
    /*
     document.getElementById("popupBBoxMenu").style.opacity = 0.0;
     document.getElementById("popupBBoxMenu").style.display = 'none'; 
     
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#fg-fetch-sparql-panel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#fg-fetch-sparql-panel");
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
     */
}

function expandClusteringPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#clusteringPanel")))) {

        $("#mainPanel").show();
        $("#clusteringPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#clusteringPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#clusteringPanel");

    } else {
        if ($("#clusteringPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#clusteringPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#clusteringPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [30, 70],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#clusteringPanel");
            $("#clusteringPanel").data("opened", true);
        }
    }


    FAGI.MapUI.map.updateSize();
    /*
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#clusteringPanel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#clusteringPanel");
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
     */
}

function expandDatasetPanel() {
    FAGI.PanelsUI.hideAllPanels();

    //alert(FAGI.PanelsUI.lastClickedMenu != null);
    //alert($(FAGI.PanelsUI.lastClickedMenu).is($("#datasetPanel")));
    //alert($("#datasetPanel").data("opened"));

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#datasetPanel")))) {

        $("#mainPanel").show();
        $("#datasetPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#datasetPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#datasetPanel");

    } else {
        if ($("#datasetPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");

            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#datasetPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#datasetPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [30, 70],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#datasetPanel");
            $("#datasetPanel").data("opened", true);
        }

        FAGI.MapUI.map.updateSize();
    }

    /*
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#datasetPanel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#datasetPanel");
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
     */
}

function expandLinksPanel() {
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#linksPanel")))) {

        $("#mainPanel").show();
        $("#linksPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#linksPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#linksPanel");

    } else {
        if ($("#linksPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#linksPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#linksPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [70, 30],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#linksPanel");
            $("#linksPanel").data("opened", true);
        }
    }

    FAGI.MapUI.map.updateSize();
    /*
     
     if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#linksPanel")))) {
     //alert($(FAGI.PanelsUI.lastClickedMenu));
     $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
     }
     FAGI.PanelsUI.lastClickedMenu = $("#linksPanel");
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
     */
}

function beforeClosePanel() {
    animatePanel(0, true);
    FAGI.PanelsUI.lastClickedMenu.data("opened", false);

    if (FAGI.ActiveState.activeFeaturePreview != null) {
        FAGI.MapUI.Controls.selectControl.deactivate();
        FAGI.MapUI.Controls.dragControlB.activate();
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.selectControl.activate();

        FAGI.ActiveState.activeFeaturePreview.attributes.la.style = {display: 'none'};
        FAGI.ActiveState.activeFeaturePreview.attributes.lb.style = {display: 'none'};

        FAGI.MapUI.Layers.vectorsA.drawFeature(FAGI.ActiveState.activeFeaturePreview.attributes.la);
        FAGI.MapUI.Layers.vectorsA.drawFeature(FAGI.ActiveState.activeFeaturePreview.attributes.lb);
    }

    return false;
}

function luda(event, deltaZ) {
    var angle = 5.0;
    var scale = 2.0;
    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none';
    if (FAGI.ActiveState.transType == FAGI.Constants.ROTATE_TRANS) {
        if (deltaZ < 0)
            angle = -angle;
        FAGI.ActiveState.activeFeature.geometry.rotate(angle, FAGI.ActiveState.activeFeature.geometry.getCentroid(true));
        FAGI.ActiveState.activeFeature.layer.drawFeature(FAGI.ActiveState.activeFeature);
        //alert(angle);
    } else if (FAGI.ActiveState.transType == FAGI.Constants.SCALE_TRANS) {
        if (deltaZ < 0)
            scale = 0.9;
        else
            scale = 1.1;
        FAGI.ActiveState.activeFeature.geometry.resize(scale, FAGI.ActiveState.activeFeature.geometry.getCentroid(true));
        FAGI.ActiveState.activeFeature.layer.drawFeature(FAGI.ActiveState.activeFeature);
    }
}

function rotateFeature(feature, angle, origin) {
    //alert("tha me deis");
    feature.geometry.rotate(angle, origin);
    feature.layer.drawFeature(feature);
}

function startDragA(feature, pixel) {
    //console.log( "drag A" );

    FAGI.ActiveState.selectedGeomA = feature;

    // When both a select and a drag control are active,
    // Openlayers can misjudge a click for a drag and bice versa
    // This is an attempt to counter this
    if (FAGI.ActiveState.mselectActive) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        FAGI.ActiveState.FAGI.ActiveState.activeFeatureClusterA[feature.attributes.links[0].attributes.la.attributes.a] = feature.attributes.links[0];
        FAGI.ActiveState.activeFeatureClusterB[feature.attributes.links[0].attributes.lb.attributes.a] = feature.attributes.links[0];

        return;
    }

    if (FAGI.ActiveState.lastPo != null) {
        if (FAGI.ActiveState.prevActiveFeature != null) {
            if (FAGI.ActiveState.prevActiveFeature == feature) {
                $('#createLinkButton').html("Cancel Link");
            }
        }
    }

    //console.log(feature.attributes.links.length);    
    if (feature.attributes.links.length) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }

    FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

    $('#createLinkButton').css("color", "red");

    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = FAGI.ActiveState.mouse.y;
    document.getElementById("popupTransformMenu").style.left = FAGI.ActiveState.mouse.x;

    FAGI.ActiveState.prevActiveFeature = FAGI.ActiveState.activeFeature;
    FAGI.ActiveState.activeFeature = feature;

    lastPixel = pixel;
}

// Feature moving 
function doDragA(feature, pixel) {
    //alert('Do drag');
    FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none';

    if (FAGI.ActiveState.selectedGeomA != null) {
        //alert('nick');
        //if (feature != FAGI.ActiveState.selectedGeomA ) {
        //alert('nick');
        if (FAGI.ActiveState.selectedGeomA.attributes.links.length > 0) {
            // Redraw all links
            var i;
            for (i = 0; i < FAGI.ActiveState.selectedGeomA.attributes.links.length; i++) {
                var validated = FAGI.ActiveState.selectedGeomA.attributes.links[i].validated;
                var otherEnd = FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb;
                var otherEndLinkIdx;
                for (var j = 0; j < otherEnd.attributes.links.length; j++) {
                    if (otherEnd.attributes.links[j] == FAGI.ActiveState.selectedGeomA.attributes.links[i]) {
                        //console.log('other end link' + otherEndLinkIdx);
                        //console.log('other end link' + (otherEnd.attributes.links[j] == FAGI.ActiveState.selectedGeomA.attributes.links[i]));
                        //console.log('other end link' + (otherEnd.attributes.links[j].attributes.a == FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.a));
                        otherEndLinkIdx = j;

                        break;
                    }
                }
                FAGI.MapUI.Layers.vectorsLinks.destroyFeatures([FAGI.ActiveState.selectedGeomA.attributes.links[i]]);
                var original = FAGI.MapUI.wkt.read(FAGI.ActiveState.selectedGeomA.attributes.oGeom);
                var start_point = FAGI.ActiveState.selectedGeomA.geometry.getCentroid(true);
                var end_point = FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb.geometry.getCentroid(true);

                FAGI.ActiveState.selectedGeomA.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
                FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
                original.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);

                var start_point_wgs = FAGI.ActiveState.selectedGeomA.geometry.getCentroid(true);
                var end_point_wgs = original.geometry.getCentroid(true);
                //var end_point_wgs = FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb.geometry.getCentroid(true);

                globalOffsetAX = end_point_wgs.x - start_point_wgs.x;
                globalOffsetAY = end_point_wgs.y - start_point_wgs.y;

                FAGI.ActiveState.selectedGeomA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

                $('#offset-x-a').val(globalOffsetAX);
                $('#offset-y-a').val(globalOffsetAY);
                var vecLen = Math.sqrt(globalOffsetAX * globalOffsetAX + globalOffsetAY * globalOffsetAY);
                globalOffsetVecBX = globalOffsetAX / vecLen;
                globalOffsetVecBY = globalOffsetAY / vecLen;
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                linkFeature.attributes = {'la': FAGI.ActiveState.selectedGeomA.attributes.links[0].attributes.la,
                    'a': FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.a,
                    'lb': FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.lb,
                    'cluster': FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.cluster,
                    'opacity': FAGI.ActiveState.selectedGeomA.attributes.links[i].attributes.opacity};
                linkFeature.prev_fused = false;
                linkFeature.validated = validated;
                if (!validated) {
                    linkFeature.jIndex = FAGI.ActiveState.selectedGeomA.attributes.links[i].jIndex;
                    linkFeature.dist = FAGI.ActiveState.selectedGeomA.attributes.links[i].dist;
                }
                FAGI.ActiveState.selectedGeomA.attributes.links[i] = linkFeature;
                otherEnd.attributes.links[otherEndLinkIdx] = linkFeature;
                FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
                //alert('nick');
                var res = FAGI.MapUI.map.getResolution();
                //FAGI.ActiveState.selectedGeomA.geometry.move(pixel.x, pixel.y);
                FAGI.MapUI.Layers.vectorsLinks.drawFeature(linkFeature);
                //alert('nick');
                //}
            }
            FAGI.MapUI.Layers.vectorsA.drawFeature(FAGI.ActiveState.selectedGeomA);
        }
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragA(feature, pixel) {
    if (FAGI.ActiveState.selectedGeomA != null) {
        //alert('End drag '+FAGI.ActiveState.selectedGeomA);
        //alert('End drag '+FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        //alert('End drag '+FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA.linls[0]));

        FAGI.ActiveState.selectedGeomA.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        
        if (typeof FAGI.ActiveState.activeFeatureClusterA[FAGI.ActiveState.selectedGeomA.attributes.links[0].attributes.a] != "undefined")
            FAGI.ActiveState.activeFeatureClusterA[FAGI.ActiveState.selectedGeomA.attributes.links[0].attributes.a] = FAGI.ActiveState.selectedGeomA.attributes.links[0];
    
        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        //alert(FAGI.ActiveState.selectedGeomA.attributes.a);
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "UpdatePositionServlet",
            // the data to send (will be converted to a query string)
            data: {pSubject: FAGI.ActiveState.selectedGeomA.attributes.a, pGeometry: FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA), pGraph: 'A'},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {

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

        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        //FAGI.ActiveState.selectedGeomA.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        FAGI.ActiveState.selectedGeomA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        //FAGI.ActiveState.selectedGeomA.state = OpenLayers.State.UPDATE;
        FAGI.ActiveState.selectedGeomA = null;
    }
}

function startDragB(feature, pixel) {
    //if (FAGI.ActiveState.selectedGeomB == null) {
    FAGI.ActiveState.selectedGeomB = feature;
    //}
    FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

    if (FAGI.ActiveState.mselectActive) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        FAGI.ActiveState.activeFeatureClusterA[feature.attributes.links[0].attributes.la.attributes.a] = feature.attributes.links[0];
        FAGI.ActiveState.activeFeatureClusterB[feature.attributes.links[0].attributes.lb.attributes.a] = feature.attributes.links[0];

        return;
    }

    if (FAGI.ActiveState.lastPo != null) {
        if (FAGI.ActiveState.prevActiveFeature != null) {
            if (FAGI.ActiveState.prevActiveFeature == feature) {
                $('#createLinkButton').html("Cancel Link");

                //console.log("Prev with current " + FAGI.ActiveState.prevActiveFeature == feature);
                //console.log("Active with current " + FAGI.ActiveState.activeFeature.fid === feature.fid);
            }
        }
    }

    //console.log(feature.attributes.links.length);    
    if (feature.attributes.links.length) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }

    $('#createLinkButton').css("color", "red");

    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = FAGI.ActiveState.mouse.y;
    document.getElementById("popupTransformMenu").style.left = FAGI.ActiveState.mouse.x;

    FAGI.ActiveState.prevActiveFeature = FAGI.ActiveState.activeFeature;
    FAGI.ActiveState.activeFeature = feature;

    lastPixel = pixel;
}

// Feature moving 
function doDragB(feature, pixel) {
    FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none';
    if (FAGI.ActiveState.selectedGeomB != null) {
        //if (feature != FAGI.ActiveState.selectedGeomB ) {
        if (FAGI.ActiveState.selectedGeomB.attributes.links.length > 0) {
            // Redraw all links
            //console.log(FAGI.ActiveState.selectedGeomB.attributes.links.length);
            var i;
            for (i = 0; i < FAGI.ActiveState.selectedGeomB.attributes.links.length; i++) {
                var validated = FAGI.ActiveState.selectedGeomB.attributes.links[i].validated;
                var otherEnd = FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la;
                var otherEndLinkIdx = 0;


                for (var j = 0; j < otherEnd.attributes.links.length; j++) {
                    if (otherEnd.attributes.links[j] == FAGI.ActiveState.selectedGeomB.attributes.links[i]) {
                        otherEndLinkIdx = j;

                        break;
                    }
                }
                FAGI.MapUI.Layers.vectorsLinks.destroyFeatures([FAGI.ActiveState.selectedGeomB.attributes.links[i]]);
                var original = FAGI.MapUI.wkt.read(FAGI.ActiveState.selectedGeomB.attributes.oGeom);
                var start_point = FAGI.ActiveState.selectedGeomB.geometry.getCentroid(true);
                var end_point = FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la.geometry.getCentroid(true);

                FAGI.ActiveState.selectedGeomB.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
                FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
                original.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);

                var start_point_wgs = FAGI.ActiveState.selectedGeomB.geometry.getCentroid(true);
                var end_point_wgs = original.geometry.getCentroid(true);
                //var end_point_wgs = FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la.geometry.getCentroid(true);

                //console.log(globalOffsetBX);
                //console.log(end_point_wgs.x + " " + end_point_wgs.x);

                globalOffsetBX = end_point_wgs.x - start_point_wgs.x;
                globalOffsetBY = end_point_wgs.y - start_point_wgs.y;

                //console.log(globalOffsetBX);
                //console.log(globalOffsetBY);

                FAGI.ActiveState.selectedGeomB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

                $('#offset-x-b').val(globalOffsetBX);
                $('#offset-y-b').val(globalOffsetBY);

                var vecLen = Math.sqrt(globalOffsetBX * globalOffsetBX + globalOffsetBY * globalOffsetBY);
                globalOffsetVecBX = globalOffsetBX / vecLen;
                globalOffsetVecBY = globalOffsetBY / vecLen;
                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                linkFeature.attributes = {'la': FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.la,
                    'a': FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.a,
                    'lb': FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.lb,
                    'cluster': FAGI.ActiveState.selectedGeomB.attributes.links[i].attributes.cluster};
                linkFeature.prev_fused = false;
                linkFeature.validated = validated;
                if (!validated) {
                    linkFeature.jIndex = FAGI.ActiveState.selectedGeomB.attributes.links[i].jIndex;
                    linkFeature.dist = FAGI.ActiveState.selectedGeomB.attributes.links[i].dist;
                }
                FAGI.ActiveState.selectedGeomB.attributes.links[i] = linkFeature;
                otherEnd.attributes.links[otherEndLinkIdx] = linkFeature;
                FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
                var res = FAGI.MapUI.map.getResolution();
                //FAGI.ActiveState.selectedGeomB.geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
                FAGI.MapUI.Layers.vectorsLinks.drawFeature(linkFeature);
                //alert('sth fishy');
                //}
            }
            FAGI.MapUI.Layers.vectorsB.drawFeature(FAGI.ActiveState.selectedGeomB);
        }
    }
    lastPixel = pixel;
}

// Featrue stopped moving 
function endDragB(feature, pixel) {
    if (FAGI.ActiveState.selectedGeomB != null) {
        FAGI.ActiveState.selectedGeomB.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomB));
        //alert(FAGI.ActiveState.selectedGeomB.attributes.a);
        //alert("luda");
        if (typeof FAGI.ActiveState.activeFeatureClusterA[FAGI.ActiveState.selectedGeomB.attributes.links[0].attributes.a] != "undefined")
            FAGI.ActiveState.activeFeatureClusterA[FAGI.ActiveState.selectedGeomB.attributes.links[0].attributes.a] = FAGI.ActiveState.selectedGeomB.attributes.links[0];
        
        $.ajax({
            // request type
            type: "POST",
            // the URL for the request
            url: "UpdatePositionServlet",
            // the data to send (will be converted to a query string)
            data: {pSubject: FAGI.ActiveState.selectedGeomB.attributes.a, pGeometry: FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomB), pGraph: 'B'},
            // the type of data we expect back
            dataType: "json",
            // code to run if the request succeeds;
            // the response is passed to the function
            success: function (responseJson) {

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

        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        //FAGI.ActiveState.selectedGeomB.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        //alert(FAGI.MapUI.wkt.write(FAGI.ActiveState.selectedGeomA));
        FAGI.ActiveState.selectedGeomB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        FAGI.ActiveState.selectedGeomB = null;
    }
}

function activateVisibleSelect() {
    //console.log("Fusing");
    var bounds = FAGI.MapUI.map.calculateBounds();
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

//proj4.defs('CART', "+title= (long/lat) +proj=longlat +ellps=FAGI.Constants.WGS84 +datum=FAGI.Constants.WGS84 +units=degrees");

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
    FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;

    console.log("Unselect");

    document.getElementById("popupTransformMenu").style.opacity = 0.0;
    document.getElementById("popupTransformMenu").style.display = 'none';

    //FAGI.ActiveState.prevActiveFeature = event.feature;
    //FAGI.ActiveState.activeFeature = null;

    //document.getElementById("transformSelect").style.display = 'none';
    //FAGI.ActiveState.activeFeature = null;
}

function setTransformation() {
    //alert('Set Transformation');
    //var nameValue = document.getElementById("transForm").value;
    //alert($('input[name=t]:checked', '#transForm').val());
    if ($('input[name=t]:checked', '#transForm').val() == 'tra') {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.dragControlB.activate();
    } else if ($('input[name=t]:checked', '#transForm').val() == 'rot') {
        FAGI.ActiveState.transType = FAGI.Constants.ROTATE_TRANS;
        FAGI.MapUI.Controls.dragControlA.deactivate();
        FAGI.MapUI.Controls.dragControlB.deactivate();
        //alert('tom');
    } else {
        FAGI.ActiveState.transType = FAGI.Constants.SCALE_TRANS;
        FAGI.MapUI.Controls.dragControlA.deactivate();
        FAGI.MapUI.Controls.dragControlB.deactivate();
    }
}

function onFeatureSelect(event) {
    //alert(FAGI.ActiveState.mselectActive);
    if (FAGI.ActiveState.mselectActive) {
        //var clusterLink = new Object();
        //clusterLink.nodeA = event.feature.attributes.links[0].attributes.la.attributes.a;
        //clusterLink.nodeB = event.feature.attributes.links[0].attributes.lb.attributes.a;
        //activeFeatureCluster[activeFeatureCluster.length] = clusterLink;
        FAGI.ActiveState.activeFeatureClusterA[event.feature.attributes.links[0].attributes.la.attributes.a] = event.feature.attributes.links[0];
        FAGI.ActiveState.activeFeatureClusterB[event.feature.attributes.links[0].attributes.lb.attributes.a] = event.feature.attributes.links[0];

        return;
    }
    //console.log(event.feature.attributes.links.length);    
    if (event.feature.attributes.links.length) {
        $('#findLinkButton').hide();
    } else {
        $('#findLinkButton').show();
    }

    $('#createLinkButton').css("color", "red");
    $('#createLinkButton').html("Create Link");

    //console.log("Select");

    if (FAGI.ActiveState.lastPo != null) {
        if (FAGI.ActiveState.prevActiveFeature != null) {
            if (FAGI.ActiveState.prevActiveFeature == event.feature) {
                $('#createLinkButton').html("Cancel Link");

                console.log("Prev with current " + FAGI.ActiveState.prevActiveFeature == event.feature);
                //console.log("Active with current " + FAGI.ActiveState.FAGI.ActiveState.activeFeature.fid === event.feature.fid);
            }
        }
    }
    document.getElementById("popupTransformMenu").style.opacity = 0.7;
    document.getElementById("popupTransformMenu").style.display = 'inline';
    document.getElementById("popupTransformMenu").style.top = FAGI.ActiveState.mouse.y;
    document.getElementById("popupTransformMenu").style.left = FAGI.ActiveState.mouse.x;

    FAGI.ActiveState.prevActiveFeature = FAGI.ActiveState.activeFeature;
    FAGI.ActiveState.activeFeature = event.feature;
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

        if (isUrlValid(sub))
            sub = "<a style=\"text-decoration: none; font-size: 80%;\" href=\"" + sub + "\" target=\"_blank\">" + sub + "</a>";

        if (isUrlValid(pre))
            pre = "<a style=\"text-decoration: none; font-size: 80%;\" href=\"" + pre + "\" target=\"_blank\">" + pre + "</a>";

        if (isUrlValid(obj))
            obj = "<a style=\"text-decoration: none; font-size: 80%;\"  href=\"" + obj + "\" target=\"_blank\">" + obj + "</a>";


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
            //FAGI.MapUI.map.zoomToExtent(event.feature.geometry.getBounds());
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
        var originalA = FAGI.MapUI.wkt.read(event.feature.attributes.la.attributes.oGeom);
        var originalB = FAGI.MapUI.wkt.read(event.feature.attributes.lb.attributes.oGeom);

        originalA.attributes = {'a': event.feature.attributes.la.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': event.feature.attributes.la.attributes.oGeom};
        FAGI.MapUI.Layers.vectorsA.addFeatures([originalA]);

        originalB.attributes = {'a': event.feature.attributes.lb.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': event.feature.attributes.lb.attributes.oGeom};
        FAGI.MapUI.Layers.vectorsB.addFeatures([originalB]);

        FAGI.MapUI.Layers.vectorsA.removeFeatures([event.feature.attributes.la]);
        FAGI.MapUI.Layers.vectorsB.removeFeatures([event.feature.attributes.lb]);

        event.feature.attributes.la = originalA;
        event.feature.attributes.lb = originalB;

        event.feature.attributes.la.style = null;
        event.feature.attributes.lb.style = null;
    }

    //FAGI.MapUI.Controls.selectControl.deactivate();
    FAGI.MapUI.Controls.dragControlA.deactivate();
    FAGI.MapUI.Controls.dragControlB.deactivate();

    FAGI.MapUI.Layers.vectorsA.redraw();
    FAGI.MapUI.Layers.vectorsB.redraw();

    FAGI.ActiveState.activeFeaturePreview = event.feature;

    expandPreviewPanel();
}

function onFusedUnselect(event) {
    expandPreviewPanel();

    FAGI.ActiveState.activeFeaturePreview = null;

    FAGI.MapUI.Controls.selectControl.deactivate();
    FAGI.MapUI.Controls.dragControlB.activate();
    FAGI.MapUI.Controls.dragControlA.activate();
    FAGI.MapUI.Controls.selectControl.activate();

    event.feature.attributes.la.style = {display: 'none'};
    event.feature.attributes.lb.style = {display: 'none'};

    FAGI.MapUI.Layers.vectorsA.drawFeature(event.feature.attributes.la);
    FAGI.MapUI.Layers.vectorsA.drawFeature(event.feature.attributes.lb);

    //FAGI.MapUI.Layers.vectorsA.redraw();
    //FAGI.MapUI.Layers.vectorsB.redraw();
}

function onLinkFeatureSelect(event) {
    //alert(event.feature.prev_fused);
    if (FAGI.ActiveState.multipleEnabled === true)
        return;

    if (!event.feature.validated) {

        document.getElementById("popupValidateMenu").style.opacity = 0.7;
        document.getElementById("popupValidateMenu").style.display = 'inline';
        document.getElementById("popupValidateMenu").style.top = FAGI.ActiveState.mouse.y;
        document.getElementById("popupValidateMenu").style.left = FAGI.ActiveState.mouse.x;

        $('#valButton').prop("link", event.feature);

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

        alert('hu');

        var originalA = FAGI.MapUI.wkt.read(event.feature.attributes.la.attributes.oGeom);
        var originalB = FAGI.MapUI.wkt.read(event.feature.attributes.lb.attributes.oGeom);

        originalA.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
        originalB.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);

        //event.feature.attributes.la.geometry = originalA.geometry;
        //event.feature.attributes.lb.geometry = originalB.geometry;

        event.feature.attributes.la.move(originalA.geometry.getCentroid(true));
        event.feature.attributes.lb.move(originalB.geometry.getCentroid(true));

        event.feature.attributes.la.style = null;
        event.feature.attributes.lb.style = null;

        //FAGI.MapUI.Controls.selectControl.deactivate();
        FAGI.MapUI.Controls.dragControlA.deactivate();
        FAGI.MapUI.Controls.dragControlB.deactivate();

        FAGI.MapUI.Layers.vectorsA.redraw();
        FAGI.MapUI.Layers.vectorsB.redraw();
        //FAGI.MapUI.Layers.vectorsLinks.refresh();

        return;
    }

    feature_is_selected = true;
    //alert("Feature "+event.feature.attributes.a);
    //alert("Feature "+event.feature.attributes.la);
    current_feature = event.feature;

    //alert('tom');
    FAGI.PanelsUI.hideAllPanels();

    if ((FAGI.PanelsUI.lastClickedMenu != null) && (!$(FAGI.PanelsUI.lastClickedMenu).is($("#fusionPanel")))) {

        $("#mainPanel").show();
        $("#fusionPanel").show();
        FAGI.PanelsUI.lastClickedMenu.data("opened", false);
        $("#fusionPanel").data("opened", true);
        FAGI.PanelsUI.lastClickedMenu = $("#fusionPanel");

    } else {
        if ($("#fusionPanel").data("opened")) {
            $(FAGI.PanelsUI.lastClickedMenu).data("opened", false);
            $("#mainPanel").width("0%");
            $("#mainPanel").height("0%");
            //$("#map").removeClass("split content");
            //$("#fagi").removeClass("split split-horizontal");
            //$("#pane").width("0%");
            $('.gutter').remove();
            $("#map").width("100%");

            FAGI.PanelsUI.lastClickedMenu = null;
            $("#fusionPanel").data("opened", false);
        } else {
            $("#mainPanel").show();
            $("#mainPanel").width("100%");
            $("#mainPanel").height("100%");

            $("#fusionPanel").show();

            Split(['#mainPanel', '#map'], {
                gutterSize: 8,
                sizes: [70, 30],
                onDragEnd: function (event, ui) {
                    FAGI.MapUI.map.updateSize();
                },
                cursor: 'col-resize'
            });

            FAGI.PanelsUI.lastClickedMenu = $("#fusionPanel");
            $("#fusionPanel").data("opened", true);
        }
    }

    FAGI.MapUI.map.updateSize();
    /*$("#fusionPanel").show();
     //alert($("#fusionPanel").data("opened"));
     if ($("#fusionPanel").data("opened")) {
     animatePanel(0, $("#fusionPanel").data("opened"));
     $("#fusionPanel").data("opened", false);
     } else {
     animatePanel(70, $("#fusionPanel").data("opened"));
     $("#fusionPanel").data("fusionPanel", true);
     }*/

    var sendData = new Array();
    sendData[sendData.length] = event.feature.attributes.a;
    var list = document.getElementById("matchList");
    var linkList = document.getElementById("linkMatchList");
    var listItem = list.getElementsByTagName("li");

    if (typeof listItem !== "undefined" && (listItem.length > 0)) {
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

    FAGI.Utilities.enableSpinner();
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
                //console.log(node.innerHTML);
                //console.log(node.long_name);
                //console.log(node.newPred);
                //console.log(node.rowIndex);
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
            FAGI.Utilities.disableSpinner();
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function (xhr, status, errorThrown) {
            FAGI.Utilities.disableSpinner();
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
                //FAGI.MapUI.map.zoomToExtent(event.feature.geometry.getBounds());
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
    node.rowIndex = rowCount + 1;

    var trunc = FAGI.Utilities.getPropertyName(property);

    var entry = " <td>" + "Metadata from A" + "</td>\n" +
            " <td>" + trunc + "</td>\n" +
            " <td>" + "Metadata from B" + "</td>\n" +
            " <td><select style=\"color: black; width: 100%;\">" + avail_meta_trans + "</select></td>\n"

    opt.innerHTML = entry;

    $tbl.append($(opt));
}

function updateFusionTable(node) {
    //console.log(current_feature == null);
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
            console.log("Long name : " + listItem[i].long_name);
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
        data: {'actions': JSON.stringify(recommendation)},
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //alert(JSON.stringify(responseJson));
            $('#classRecommendation').html("");
            $.each(responseJson.tagList, function (index, element) {
                $('#classRecommendation').append($("<option></option>")
                        .attr("value", element)
                        .text(element));
            });
            $('#geoTrans option[value="' + responseJson.predictedFusionAction + '"]').attr('selected', 'selected');
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
        avail_trans += "<option value=\"" + element + "\">" + element + "</option>";
    });
    $.each(val.metaTransforms, function (index, element) {
        avail_meta_trans += "<option value=\"" + element + "\">" + element + "</option>";
    });

    var s = " <p class=\"geoinfo\" id=\"link_name\">Ludacris</p>\n" +
            " <table class=\"rwd-table\" border=1 id=\"fusionTable\">\n" +
            " <tr>\n" +
            " <td>Value from " + $('#fg-dataset-input-a').val() + "</td>\n" +
            " <td>Predicate</td>\n" +
            " <td>Value from " + $('#fg-dataset-input-b').val() + "</td>\n" +
            " <td>Action</td>\n" +
            " </tr>\n" +
            " <tr>\n" +
            " <td title=\"" + val.geomsA[0] + "\">" + geom_typeA + "</td>\n" +
            " <td>asWKT</td>\n" +
            " <td title=\"" + val.geomsB[0] + "\">" + geom_typeB + "</td>\n" +
            " <td><select id=\"geoTrans\" style=\"color: black; width: 100%;\">" + avail_trans + "</select></td>\n" +
            " </tr>\n" +
            " </table>" +
            " <fieldset>" +
            " <label for=\"fg-fuse-rest-selector\">Remaining Metadata Action</label>" +
            " <select name=\"fg-fuse-rest-selector\" id=\"fg-fuse-rest-selector\">" +
            " <option>Keep A</option>" +
            " <option selected=\"selected\">None</option>" +
            " <option>Keep B</option>" +
            " <option>Keep Both</option>" +
            " </select>" +
            " </fieldset> " +
            " <table border=0 id=\"shiftPanel\">" +
            " <tr>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Shift (%):</td>\n" +
            " <td style=\"width:10px; text-align: center;\" align=\"left\" valign=\"bottom\"><input style=\"width:50px;\" type=\"text\" id=\"shift\" name=\"shift\" value=\"100\"/></td>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Scale:</td>\n" +
            " <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"scale_fac\" name=\"x_scale\" value=\"1.0\"/></td>\n" +
            " <td style=\"white-space: nowrap; width:30px; text-align: center;\" align=\"left\" valign=\"center\">Rotate:</td>\n" +
            " <td style=\"width:20px; text-align: center;\" align=\"left\" valign=\"bottom\"><input type=\"text\" style=\"width:50px;\" id=\"rotate_fac\" name=\"x_rotate\" value=\"0.0\"/></td>\n" +
            " </tr>\n" +
            " </table>" +
            " <input id=\"fuseButton\" type=\"submit\" value=\"Fuse\" style=\"float:right\" onclick=\"return false;\"/>\n";

    document.getElementById("linkFusionTable").innerHTML = s;
    //$("#fg-fuse-rest-selector").selectmenu();
    //$("#fg-fuse-rest-selector").css("width", "200px");
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
            if (opt.long_name == node.long_name) {
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

    if (send == null)
        send = "";

    //alert(send);
    //alert(current_feature == null);
    var geomCells = tblRows[1].getElementsByTagName("td");
    var geomFuse = new Object();
    current_feature.attributes.la.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);
    current_feature.attributes.lb.geometry.transform(FAGI.MapUI.map.getProjectionObject(), FAGI.Constants.WGS84);

    geomFuse.valA = FAGI.MapUI.wkt.write(current_feature.attributes.la);
    geomFuse.pre = geomCells[1].innerHTML;
    //alert('after pre');
    geomFuse.preL = tblRows[0].long_name;
    //lert('after prel');
    if (typeof geomFuse.preL === "undefined") {
        geomFuse.preL = "dummy";
    }

    geomFuse.valB = FAGI.MapUI.wkt.write(current_feature.attributes.lb);
    var tmpGeomAction = geomCells[3].getElementsByTagName("select");
    //alert('after valB '+tmpGeomAction.length+' '+geomCells.length);
    if (tmpGeomAction.length == 1) {
        geomFuse.action = tmpGeomAction[0].value;
    }


    var teach = $('#fuseButton').prop("recommendation");
    teach.fusionAction = geomFuse.action;
    //alert(JSON.stringify(teach));

    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "LearningServlet",
        // the data to send (will be converted to a query string)
        data: {'actions': JSON.stringify(teach)},
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

    current_feature.attributes.la.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
    current_feature.attributes.lb.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

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
    var restFuseAction = $("#fg-fuse-rest-selector option:selected").text();

    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FuseLinkServlet",
        // the data to send (will be converted to a query string)
        data: {props: sendData, propsJSON: sndJSON, factJSON: sndShiftJSON, classes: send, rest: restFuseAction},
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
        var linkFeature = FAGI.MapUI.wkt.read(element.geom);
        if (linkFeature instanceof Array) {
            for (var i = 0; i < linkFeature.length; i++) {
                linkFeature[i].geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                linkFeature[i].attributes = {'a': element.subject};
            }
            FAGI.MapUI.Layers.vectorsFused.addFeatures(linkFeature);
        } else {
            //alert(linkFeature.geometry);
            linkFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            linkFeature.attributes = {'a': element.subject};
            linkFeature.attributes.a = element.subject;
            FAGI.MapUI.Layers.vectorsFused.addFeatures([linkFeature]);
        }
    });
    FAGI.MapUI.Layers.vectorsFused.redraw();
}

function previewLinkedGeom(resp) {
    var tempGeomA = null;
    var tempGeomB = null;
    if (current_feature.attributes.la.style == null) {
        tempGeomA = FAGI.MapUI.wkt.read(current_feature.attributes.la.attributes.oGeom);
        tempGeomA.attributes = {'a': current_feature.attributes.la.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(tempGeomA)};
        tempGeomA.style = {display: 'none'};
        FAGI.MapUI.Layers.vectorsA.removeFeatures([current_feature.attributes.la]);
        FAGI.MapUI.Layers.vectorsA.addFeatures([tempGeomA]);
    }

    if (current_feature.attributes.lb.style == null) {
        tempGeomB = FAGI.MapUI.wkt.read(current_feature.attributes.lb.attributes.oGeom);
        tempGeomB.attributes = {'a': current_feature.attributes.lb.attributes.a, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(tempGeomB)};
        tempGeomB.style = {display: 'none'};
        FAGI.MapUI.Layers.vectorsB.removeFeatures([current_feature.attributes.lb]);
        FAGI.MapUI.Layers.vectorsB.addFeatures([tempGeomB]);
    }

    current_feature.attributes.la = tempGeomA;
    current_feature.attributes.lb = tempGeomB;

    var linkFeature = FAGI.MapUI.wkt.read(resp.geom);
    //alert(resp.geom);
    if (Object.prototype.toString.call(linkFeature) === '[object Array]') {
        //alert('Array');
        for (var i = 0; i < linkFeature.length; i++) {
            linkFeature[i].geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            linkFeature[i].attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb, 'cluster': current_feature.attributes.cluster};

            linkFeature[i].prev_fused = true;
            linkFeature[i].validated = true;
            //FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature[i]]);
            FAGI.MapUI.Layers.vectorsFused.addFeatures([linkFeature[i]]);
            //alert('done');
        }
        FAGI.MapUI.Layers.vectorsLinks.removeFeatures([current_feature]);
    } else {
        //alert('reached');
        linkFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        linkFeature.attributes = {'a': current_feature.attributes.a, 'la': current_feature.attributes.la, 'lb': current_feature.attributes.lb, 'cluster': current_feature.attributes.cluster};

        //alert('done feature '+linkFeature);
        linkFeature.prev_fused = true;
        linkFeature.validated = true;
        //alert('reached 2');
        FAGI.MapUI.Layers.vectorsLinks.removeFeatures([current_feature]);
        //FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
        FAGI.MapUI.Layers.vectorsFused.addFeatures([linkFeature]);
    }

    FAGI.MapUI.Layers.vectorsA.redraw();
    FAGI.MapUI.Layers.vectorsB.redraw();
    FAGI.MapUI.Layers.vectorsLinks.refresh();
    FAGI.MapUI.Layers.vectorsFused.refresh();
    //alert('done');
    current_feature = null;
    feature_is_selected = false;
}

function onLinkFeatureUnselect(event) {
    if (FAGI.ActiveState.multipleEnabled === true)
        return;
    //document.getElementById("link_tooltip").style.opacity = 0;
    feature_is_selected = false;
    //alert('a');
    if (event.feature.prev_fused === true) {
        event.feature.attributes.la.style = {display: 'none'};
        event.feature.attributes.lb.style = {display: 'none'};

        //FAGI.MapUI.Controls.selectControl.activate();
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.dragControlB.activate();

        FAGI.MapUI.Layers.vectorsA.redraw();
        FAGI.MapUI.Layers.vectorsB.redraw();
    }
    //document.getElementById("link_tooltip").style.display = 'none';
    //document.getElementById("link_tooltip").fadeOut('slow', 'linear');
}

function onFeatureSelectFromLinks(event) {
    // fetch the cluster's latlon and set the map center to it and call zoomin function
    // which takes you to a one level zoom in and I hope this solves your purpose :)    
    FAGI.MapUI.map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    FAGI.MapUI.map.zoomToExtent(event.feature.geometry.getBounds(), true);
    FAGI.MapUI.map.zoomIn();
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
    FAGI.MapUI.map.setCenter(event.feature.geometry.getBounds().getCenterLonLat());
    FAGI.MapUI.map.zoomToExtent(event.feature.geometry.getBounds(), true);
    FAGI.MapUI.map.zoomIn();
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
    //alert(jsonentitiess.dataset);
    $('#valAllButton').data("dataset", jsonentitiess.dataset);
    //alert($('#valAllButton').data("dataset"));
    if (jsonentitiess.dataset == "A") {
        $.each(jsonentitiess.entitiesA, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureA = FAGI.MapUI.wkt.read(element);
            var links = [];
            polygonFeatureA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            polygonFeatureA.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureA)};

            FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeatureA]);
        });
        $.each(jsonentitiess.entitiesB, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureB = FAGI.MapUI.wkt.read(element);
            var links = [];
            polygonFeatureB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            polygonFeatureB.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureB)};

            FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureB]);
        });
        $.each(jsonentitiess.links, function (index, element) {
            //console.log("Sub A "+element.subA);
            //console.log("Sub B "+element.subB);
            //console.log("Geo A "+element.geomA);
            //console.log("Geo B "+element.geomB);

            var featA = null;
            if (!element.geomA) {
                featA = FAGI.MapUI.Layers.vectorsA.getFeaturesByAttribute("a", element.subA)[0];
            } else {
                featA = FAGI.MapUI.wkt.read(element.geomA);
                featA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                featA.attributes = {'links': [], 'a': element.subA, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(featA)};
            }
            var featB = null;
            if (!element.geomB) {
                featB = FAGI.MapUI.Layers.vectorsB.getFeaturesByAttribute("a", element.subB)[0];
            } else {
                featB = FAGI.MapUI.wkt.read(element.geomB);
                featB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                featB.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(featB)};
            }


            var retFeat = createUnvalidatedLink(featA, featB);

            retFeat.jIndex = element.jIndex;
            retFeat.dist = element.dist;

            //console.log(featA.attributes.links.length);

            featA.attributes.links[featA.attributes.links.length] = retFeat;
            featA.attributes.a = retFeat.attributes.la.attributes.a;

            featB.attributes.links[featB.attributes.links.length] = retFeat;
            featB.attributes.a = retFeat.attributes.lb.attributes.a;

            FAGI.MapUI.Layers.vectorsA.addFeatures([featA]);
            FAGI.MapUI.Layers.vectorsB.addFeatures([featB]);

            //createUnvalidatedLink(featA, featB);
        });
    } else {
        $.each(jsonentitiess.entitiesB, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureA = FAGI.MapUI.wkt.read(element);
            var links = [];
            polygonFeatureA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            polygonFeatureA.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureA)};

            FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeatureA]);
        });
        $.each(jsonentitiess.entitiesA, function (index, element) {
            if (element == "")
                return true;

            var polygonFeatureB = FAGI.MapUI.wkt.read(element);
            var links = [];
            polygonFeatureB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            polygonFeatureB.attributes = {'links': links, 'a': index, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureB)};

            FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureB]);
        });
        $.each(jsonentitiess.links, function (index, element) {
            var featA = null;
            if (!element.geomB) {
                featA = FAGI.MapUI.Layers.vectorsA.getFeaturesByAttribute("a", element.subB)[0];
            } else {
                featA = FAGI.MapUI.wkt.read(element.geomB);
                featA.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(featA)};
                featA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            }
            var featB = null;
            if (!element.geomA) {
                featB = FAGI.MapUI.Layers.vectorsB.getFeaturesByAttribute("a", element.subA)[0];
            } else {
                featB = FAGI.MapUI.wkt.read(element.geomA);
                featB.attributes = {'links': [], 'a': element.subA, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(featB)};
                featB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            }

            var retFeat = createUnvalidatedLink(featA, featB);

            retFeat.jIndex = element.jIndex;
            retFeat.dist = element.dist;

            //console.log(retFeat.attributes.la.attributes.a);

            featA.attributes.links[featA.attributes.links.length] = retFeat;
            featA.attributes.a = retFeat.attributes.la.attributes.a;

            featB.attributes.links[featB.attributes.links.length] = retFeat;
            featB.attributes.a = retFeat.attributes.lb.attributes.a;

            FAGI.MapUI.Layers.vectorsA.addFeatures([featA]);
            FAGI.MapUI.Layers.vectorsB.addFeatures([featB]);

            //createUnvalidatedLink(featA, featB);
        });
    }
}

function addUnlinkedMapDataJson(jsonentitiess) {
    $.each(jsonentitiess.entitiesA, function (index, element) {
        var polygonFeatureA = FAGI.MapUI.wkt.read(element.geom);
        var links = [];
        polygonFeatureA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        polygonFeatureA.attributes = {'links': links, 'a': element.sub, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureA)};

        FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeatureA]);
    });
    $.each(jsonentitiess.entitiesB, function (index, element) {
        var polygonFeatureB = FAGI.MapUI.wkt.read(element.geom);
        var links = [];
        polygonFeatureB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        polygonFeatureB.attributes = {'links': links, 'a': element.sub, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureB)};

        FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureB]);
    });
}

function addMapDataJson(jsongeoms) {
    $.each(jsongeoms.linked_ents, function (index, element) {
        //alert(element.subA);

        var polygonFeatureA = FAGI.MapUI.wkt.read(element.geomB);
        polygonFeatureA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        polygonFeatureA.attributes = {'a': element.subB, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureA)};

        var polygonFeatureB = FAGI.MapUI.wkt.read(element.geomA);
        polygonFeatureB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
        polygonFeatureB.attributes = {'a': element.subA, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureB)};

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
        FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
        FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeatureB]);
        FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureA]);
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
                polygonFeatureA = FAGI.MapUI.wkt.read(second);
                polygonFeatureA.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                polygonFeatureA.attributes = {'a': first, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': second};

                FAGI.MapUI.Layers.vectorsB.addFeatures([polygonFeatureA]);
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
                polygonFeatureB = FAGI.MapUI.wkt.read(fourth);
                polygonFeatureB.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                polygonFeatureB.attributes = {'a': third, 'cluster': 'Unset', 'opacity': 0.8, 'oGeom': fourth};

                var start_point = polygonFeatureA.geometry.getCentroid(true);
                var end_point = polygonFeatureB.geometry.getCentroid(true);

                var linkFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([start_point, end_point]));
                var links = [];
                links.push(linkFeature);
                polygonFeatureA.attributes = {'links': links, 'a': first, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureA)};
                polygonFeatureB.attributes = {'links': links, 'a': third, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeatureB)};
                linkFeature.attributes = {'a': third, 'b': first, 'la': polygonFeatureB, 'lb': polygonFeatureA, 'cluster': 'Unset', 'opacity': 0.8};
                linkFeature.prev_fused = false;
                linkFeature.validated = true;
                //console.log(linkFeature.attributes.la + " " + linkFeature.attributes.lb);
                FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
                FAGI.MapUI.Layers.vectorsA.addFeatures([polygonFeatureB]);

                prev = i + 1;
                step = 0;
            }
        }
    }
}