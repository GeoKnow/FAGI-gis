/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(document).ready(function () {                        // When the HTML DOM is ready loading, then execute the following function...
    init();
});

function init() {
    $("input").tooltip();
    //$( document ).tooltip();
    FAGI.PanelsUI.hideAllPanels();

    FAGI.Utilities.disableSpinner();

    /*
     Split(['#pane', '#map'], {
     gutterSize: 8,
     onDragEnd: function (event, ui) {
     $("#mainPanel").width("0%");
     $("#mainPanel").height("0%");
     //$("#map").removeClass("split content");
     //$("#fagi").removeClass("split split-horizontal");
     //$("#pane").width("0%");
     $('.gutter').remove();
     $("#map").width("100%");
     },
     cursor: 'col-resize'
     });
     */

    $('#popupBBoxMenu').hide();
    $('#popupTransformMenu').hide();
    $('#popupValidateMenu').hide();
    $('#popupFindLinkMenu').hide();
    $('#fg-info-popup').hide();

    $('input').addClass("ui-widget ui-widget-content ui-corner-all");

    $("input[type=submit], button")
            .button()
            .click(function (event) {
                event.preventDefault();
            });

    // Allow authentication for Remote Endpoints
    $('#fg-auth-dropdown-a').accordion({
        collapsible: false,
        heightStyle: "content",
        active: false

    });
    $('#fg-auth-dropdown-b').accordion({
        collapsible: false,
        heightStyle: "content",
        active: false
    });
    $('#fg-auth-dropdown-l').accordion({
        collapsible: false,
        heightStyle: "content",
        active: false
    });
    $('#fg-auth-dropdown-t').accordion({
        collapsible: false,
        heightStyle: "content",
        active: false
    });


    $(".buttonset").buttonset();

    $('#fg-user-create-btn').click(createUser);
    $('#fg-user-login-btn').click(loginUser);
    $('#connButton').click(setConnection);
    $('#dataButton').click(setDatasets);
    $('#loadButton').click(setConnection);
    $('#buttonFilterLinksA').click(filterLinksA);
    $('#buttonFilterLinksB').click(filterLinksB);
    $('#linksButton').click(schemaMatch);
    $('#allLinksButton').click(selectAll);
    $('#finalButton').click(submitLinks);

    $('#fg-close-panel').click(FAGI.PanelsUI.closeOpenPanel);

    $('#previewPanel').data("opened", false);
    $('#datasetMenu').click(expandDatasetPanel);
    $('#datasetPanel').data("opened", false);
    $('#linksMenu').click(expandLinksPanel);
    $('#linksPanel').data("opened", false);
    $('#matchingMenu').click(expandMatchingPanel);
    $('#matchingPanel').data("opened", false);
    $('#fusionPanel').data("opened", false);
    $('#fg-user-panel').data("opened", false);
    $('#fg-user-selection-panel').data("opened", false);
    $('#userMenu').click(expandUserPanel);
    $('#clusteringPanel').data("opened", false);
    $('#fg-fetch-sparql-panel').data("opened", false);
    $('#clusteringTool').click(expandClusteringPanel);
    $('#fetchBBoxSPARQLButton').click(expandSPARQLFetchPanel);
    $('#multipleTool').click(activateMultipleTool);
    $('#bboxTool').click(activateBBoxTool);
    $('#fetchTool').click(activateFecthUnlinked);
    $('#visibleSelect').click(activateVisibleSelect);
    $('#fg-download-fused-tool').click(FAGI.Utilities.requestDatasetFile);
    $('#fg-links-queries-submit').click(linksSPARQLFilter);
    $('#fg-fetch-queries-submit').click(fetchSPARQLContained);

    
    $('#userCell').hide();
    $('#connectionMenu').click(expandConnectionPanel);
    $('#connectionPanel').data("opened", false);

    // Reload all links
    $('#fg-links-unfilter-button').click(FAGI.PanelsUI.Callbacks.onUnfilterButtonPressed);

    // Clustering
    $('#clusterButton').click(performClustering);
    $("#clusterSelector").change(FAGI.MapUI.Callbacks.onClusterSelectionChange);

    $('#fetchBBoxSPARQLButton').click(enableSPARQLFetch);
    $('#transformBBoxButton').click(enableBBoxTransform);
    $('#fetchBBoxContainedButton').click(fetchContained);
    //$('#fetchBBoxFindButton').click(fetchContainedAndLink);
    $('#fetchBBoxFindButton').click(FAGI.MapUI.Callbacks.Linking.onBatchFindLinkButtonPressed);

    $('#moveButton').click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.MOVE_TRANS;
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });
    $('#scaleButton').click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.SCALE_TRANS;
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });
    $('#rotateButton').click(function () {
        FAGI.ActiveState.transType = FAGI.Constants.ROTATE_TRANS;
        FAGI.MapUI.Controls.dragControlA.activate();
        FAGI.MapUI.Controls.dragControlB.activate();
        document.getElementById("popupTransformMenu").style.opacity = 0;
        document.getElementById("popupTransformMenu").style.display = 'none';
    });

    $('#valAllButton').click(function () {
        var ds = $('#valAllButton').data("dataset");
        if (ds == "A") {
            $.each(FAGI.MapUI.Layers.vectorsA.features, function (index, element) {
                var links = element.attributes.links;
                if (typeof links === "undefined") {
                    console.log(element.attributes.a);
                    FAGI.MapUI.map.zoomToExtent(element.geometry.getBounds());
                }
                console.log(element.attributes.a);
                if (links.length > 0) {
                    var bestLink = null;
                    var bestScore = -1;
                    console.log("Links Count " + links.length);
                    for (var i = 0; i < links.length; i++) {
                        if (links[i].validated)
                            continue;
                        var linkScore = links[i].dist + links[i].jIndex;
                        if (linkScore > bestScore) {
                            bestScore = linkScore;
                            bestLink = links[i];
                        }
                    }
                    console.log("Best Score " + bestScore);

                    if (bestLink != null)
                        validateLink(bestLink, ds);
                }
            });
        } else {
            $.each(FAGI.MapUI.Layers.vectorsB.features, function (index, element) {
                var links = element.attributes.links;
                if (typeof links === "undefined") {
                    console.log(element.attributes.a);
                    FAGI.MapUI.map.zoomToExtent(element.geometry.getBounds());
                }
                if (links.length > 0) {
                    var bestLink = null;
                    var bestScore = -1;
                    for (var i = 0; i < links.length; i++) {
                        if (links[i].validated)
                            continue;
                        var linkScore = links[i].dist + links[i].jIndex;
                        if (linkScore > bestScore) {
                            bestScore = linkScore;
                            bestLink = links[i];
                        }
                    }
                    console.log("Best Score " + bestScore);
                    if (bestLink != null)
                        validateLink(bestLink, ds);
                }
            });
        }
    });

    $('#valButton').click(function () {
        var feat = $(this).prop("link");
        document.getElementById("popupValidateMenu").style.opacity = 0;
        document.getElementById("popupValidateMenu").style.display = 'none';
        FAGI.Utilities.enableSpinner();
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
                for (i = 0; i < linksA.length; i++) {
                    if (linksA[i].validated == false)
                        toDel[toDel.length] = linksA[i];
                    else
                        newLinksA[newLinksA.length] = linksA[i];
                }
                for (i = 0; i < linksB.length; i++) {
                    if (linksB[i].validated == false)
                        toDel[toDel.length] = linksB[i];
                    else
                        newLinksB[newLinksB.length] = linksB[i];
                }
                feat.attributes.la.attributes.links = newLinksA;
                feat.attributes.lb.attributes.links = newLinksB;
                //FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures(toDel);
                FAGI.MapUI.Layers.vectorsLinks.destroyFeatures(toDel);
                feat.validated = true;
                FAGI.MapUI.Layers.vectorsLinks.drawFeature(feat);
                //console.log("All good " + responseText);

                FAGI.Utilities.disableSpinner();
            },
            error: function (responseText) {
                FAGI.Utilities.disableSpinner();
                alert("All bad " + responseText);
                alert("Error");
            },
            data: {'subA': feat.attributes.la.attributes.a, 'subB': feat.attributes.lb.attributes.a}
            //Options to tell jQuery not to process data or worry about content-type.
        });
    });

    // Set Callbacks for Link Creation
    $('#createLinkButton').click(FAGI.MapUI.Callbacks.Linking.onCreateLinkButtonPressed);
    $("#popupFindLinkButton").click(FAGI.MapUI.Callbacks.Linking.onFindLinkPopupButtonPressed);
    $("#fg-popup-find-link-button").click(FAGI.MapUI.Callbacks.Linking.onBatchFindLinkPopupButtonPressed);
    $('#findLinkButton').click(FAGI.MapUI.Callbacks.Linking.onFindLinkButtonPressed);

    $('.dropdown').css("z-index", "700000");

    var radSpinner = $("#radiusSpinner").spinner({step: 1,
        numberFormat: "n",
        min: 1,
        max: 1000,
        spin: function (event, ui) {
            $(this).change();
        }});
    radSpinner.spinner("value", 100);

    radSpinner = $("#fg-batch-radius-spinner").spinner({step: 1,
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
    console.log("Dataset " + ds);
    document.getElementById("popupValidateMenu").style.opacity = 0;
    document.getElementById("popupValidateMenu").style.display = 'none';
    FAGI.Utilities.enableSpinner();
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
                //feat.attributes.la.attributes.links = newLinksA;

            } else {
                for (i = 0; i < linksB.length; i++) {
                    if (linksB[i].validated == false)
                        toDel[toDel.length] = linksB[i];
                    else
                        newLinksB[newLinksB.length] = linksB[i];
                }
                //feat.attributes.lb.attributes.links = newLinksB;
            }
            //FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures(toDel);
            //FAGI.MapUI.Layers.vectorsLinks.destroyFeatures(toDel);
            feat.validated = true;
            FAGI.MapUI.Layers.vectorsLinks.drawFeature(feat);
            //console.log("All good " + responseText);

            FAGI.Utilities.disableSpinner();
        },
        error: function (responseText) {
            FAGI.Utilities.disableSpinner();
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
    FAGI.ActiveState.scoreThreshold = $(this).spinner("value");
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

function enableSPARQLFetch(links) {

}

function createSingleUnvalidatedLinks(feat, links) {
    $.each(links, function (index, element) {
        var featB = null;
        var layer = null;
        if (feat.layer.name == "Dataset A Layer") {
            featB = FAGI.MapUI.Layers.vectorsB.getFeaturesByAttribute("a", element.subB);
            layer = FAGI.MapUI.Layers.vectorsB;
            //console.log(featB.length);
            console.log(element.subB);
            if (featB.length > 0) {
                var retFeat = createUnvalidatedLink(feat, featB[0]);

                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;

                feat.attributes.links[feat.attributes.links.length] = retFeat;
                featB[0].attributes.links[featB[0].attributes.links.length] = retFeat;
            } else {
                var polygonFeature = FAGI.MapUI.wkt.read(element.geomB);
                polygonFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                polygonFeature.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeature)};
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
            featB = FAGI.MapUI.Layers.vectorsA.getFeaturesByAttribute("a", element.subB);
            layer = FAGI.MapUI.Layers.vectorsA;
            //console.log(featB.length);
            console.log(element.subB);
            if (featB.length > 0) {
                var retFeat = createUnvalidatedLink(featB[0], feat);

                retFeat.jIndex = element.jIndex;
                retFeat.dist = element.dist;

                feat.attributes.links[feat.attributes.links.length] = retFeat;
                featB[0].attributes.links[featB[0].attributes.links.length] = retFeat;
            } else {
                var polygonFeature = FAGI.MapUI.wkt.read(element.geomB);
                polygonFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                polygonFeature.attributes = {'links': [], 'a': element.subB, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeature)};
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
    polygonFeature = FAGI.MapUI.wkt.read(elem);
    polygonFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());

    var start_point = polygonFeature.geometry.getCentroid(true);
    var end_point = feat.geometry.getCentroid(true);

    var line2 = new OpenLayers.Geometry.LineString([FAGI.ActiveState.lastPo, FAGI.ActiveState.nowPo]);
    linkFeature = new OpenLayers.Feature.Vector(line2);
    linkFeature.attributes = {'la': nodeA,
        'a': nodeA.attributes.a,
        'lb': nodeB,
        'cluster': nodeB.attributes.cluster,
        'opacity': 0.8};

    var links = [];
    links[0] = linkFeature;

    polygonFeature.attributes = {'links': links, 'a': first, 'cluster': 'Unset', 'opacity': 0.3, 'oGeom': FAGI.MapUI.wkt.write(polygonFeature)};

    linkFeature.prev_fused = false;
    linkFeature.validated = false;
    nodeA.attributes.links = links;
    nodeB.attributes.links = links;

    //FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
    FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
    FAGI.MapUI.Layers.vectorsLinks.drawFeature(linkFeature);
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

    //FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
    FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
    FAGI.MapUI.Layers.vectorsLinks.drawFeature(linkFeature);

    return linkFeature;
}

function createNewLink(nodeA, nodeB) {
    if (nodeA.layer.name !== nodeB.layer.name) {
        // We want nodeA to refer to Layer A
        if (nodeA.layer == FAGI.MapUI.Layers.vectorsA) {
            // Unless if the dominant set is B
            /*if (!$('#domA').is(":checked")) {
             var temp = nodeA;
             nodeA = nodeB;
             nodeB = temp;
             }*/
        } else {
            //if ($('#domA').is(":checked")) {
            var temp = nodeA;
            nodeA = nodeB;
            nodeB = temp;
            //}
        }

        var line2 = new OpenLayers.Geometry.LineString([FAGI.ActiveState.lastPo, FAGI.ActiveState.nowPo]);
        linkFeature = new OpenLayers.Feature.Vector(line2);
        linkFeature.attributes = {'la': nodeA,
            'a': nodeA.attributes.a,
            'lb': nodeB,
            'cluster': nodeB.attributes.cluster,
            'opacity': 0.8};

        linkFeature.prev_fused = false;
        linkFeature.validated = true;
        nodeA.attributes.links[nodeA.attributes.links.length] = linkFeature;
        nodeB.attributes.links[nodeB.attributes.links.length] = linkFeature;

        FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
        FAGI.MapUI.Layers.vectorsLinks.addFeatures([linkFeature]);
        FAGI.MapUI.Layers.vectorsLinks.drawFeature(linkFeature);

        FAGI.ActiveState.lastPo = null;

        var sendData = new Object();
        console.log(nodeA.attributes.a);
        console.log(nodeB.attributes.a);
        FAGI.Utilities.enableSpinner();
        $.ajax({
            url: 'CreateLinkServlet', //Server script to process data
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
            data: {'subA': nodeA.attributes.a, 'subB': nodeB.attributes.a}
            //Options to tell jQuery not to process data or worry about content-type.
        });
    } else {
        FAGI.ActiveState.lastPo = null;
        FAGI.MapUI.Layers.vectorsLinksTemp.destroyFeatures();
        alert('You cannot fuse geometries of the same dataset');
    }
}

var selectedProperties = new Object();
var next_id = 0;
var next_link_id = 0;
$('#addLinkSchema').click(function () {
    if (linkLastSelectedFromA === null || linkLastSelectedFromB === null) {
        alert("No properties selected");
        return;
    }

    // Property result
    var strA = "";
    var strB = "";

    // Add properties from A
    var listA = document.getElementById("linkSchemasA");
    var listItemsA = listA.getElementsByTagName("li");
    $.each(listItemsA, function (index, element) {
        element.style.backgroundColor = element.backColor;
        element.match_count = 0;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (linkLastSelectedFromA === null)
            linkLastSelectedFromA = element;

        if (element.prev_selected === true) {
            strA += element.long_name + "|";
            element.prev_selected = false;
        }
    });

    // Add propertiesfromB
    var listB = document.getElementById("linkSchemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        element.style.backgroundColor = element.backColor;
        element.match_count = 0;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (linkLastSelectedFromB === null)
            linkLastSelectedFromB = element;

        if (element.prev_selected === true) {
            strB += element.long_name + "|";
            element.prev_selected = false;
        }
    });

    strA = strA.substring(0, strA.length - 1);
    strB = strB.substring(0, strB.length - 1);
    //alert(strA);
    //alert(strB);
    var node = document.createElement("li");
    node.onclick = linkMatchedSchemaClicked;
    var text = '<input class="match" type="text" name="lname" value="' + linkLastSelectedFromA.innerHTML + FAGI.Constants.PROPERTY_SEPARATOR + linkLastSelectedFromB.innerHTML + '"/>';
    node.long_name = strA + FAGI.Constants.PROPERTY_SEPARATOR + strB;
    var repA;
    var repB;
    var text;
    if (linkLastSelectedFromA === null) {
        repB = getText(linkLastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repB + '"/>';
        //node.long_name = 'dummy' + FAGI.Constants.PROPERTY_SEPARATOR + linkLastSelectedFromB.long_name;
        node.long_name = 'dummy' + FAGI.Constants.PROPERTY_SEPARATOR + strB;
    }
    else if (linkLastSelectedFromB === null) {
        repA = getText(linkLastSelectedFromA.firstChild)
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        //node.long_name = linkLastSelectedFromA.long_name + FAGI.Constants.PROPERTY_SEPARATOR + 'dummy';
        node.long_name = strA + FAGI.Constants.PROPERTY_SEPARATOR + 'dummy';
    }
    else {
        repA = getText(linkLastSelectedFromA.firstChild);
        repB = getText(linkLastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + FAGI.Constants.PROPERTY_SEPARATOR + repB + '"/>';
        //node.long_name = linkLastSelectedFromA.long_name + FAGI.Constants.PROPERTY_SEPARATOR + linkLastSelectedFromB.long_name;
        node.long_name = strA + FAGI.Constants.PROPERTY_SEPARATOR + strB;
    }
    console.log(node.long_name);

    node.innerHTML = text;
    $(node).on('input', function (e) {
        var row = $("#fusionTable tr")[this.rowIndex];
        this.newPred = e.target.value;
        $(row).get(0).newPred = e.target.value;
        $(row).find("td")[1].innerHTML = e.target.value;
    });

    next_link_id++;
    document.getElementById("linkMatchList").appendChild(node);

    //Reset selections
    linkLastSelectedFromA = null;
    linkLastSelectedFromB = null;

    // Update tables
    updateFusionTable(node);
});

function getText(obj) {
    return obj.textContent ? obj.textContent : obj.innerText;
}

$('#addSchema').click(function () {
    if (lastSelectedFromA === null && lastSelectedFromB === null) {
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
        element.match_count = 0;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (element.prev_selected === true) {
            strA += element.long_name + "|";
            element.prev_selected = false;
        }
    });
    var listB = document.getElementById("schemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        //alert(element.prev_selected);
        element.style.backgroundColor = element.backColor;
        element.match_count = 0;
        var scoreLbl = element.getElementsByTagName("div");
        if (typeof scoreLbl[0] !== "undefined") {
            scoreLbl[0].innerHTML = "";
        }
        if (element.prev_selected === true) {
            strB += element.long_name + "|";
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
        //node.long_name = 'dummy' + FAGI.Constants.PROPERTY_SEPARATOR + lastSelectedFromB.long_name;
        node.long_name = 'dummy' + FAGI.Constants.PROPERTY_SEPARATOR + strB;
        node.newPred = repB;
    }
    else if (lastSelectedFromB === null) {
        repA = getText(lastSelectedFromA.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        //node.long_name = lastSelectedFromA.long_name + FAGI.Constants.PROPERTY_SEPARATOR + 'dummy';
        node.long_name = strA + FAGI.Constants.PROPERTY_SEPARATOR + 'dummy';
        node.newPred = repA;
    }
    else {
        repA = getText(lastSelectedFromA.firstChild);
        repB = getText(lastSelectedFromB.firstChild);
        text = '<input class="match" type="text" name="lname" value="' + repA + FAGI.Constants.PROPERTY_SEPARATOR + repB + '"/>';
        //node.long_name = lastSelectedFromA.long_name + FAGI.Constants.PROPERTY_SEPARATOR + lastSelectedFromB.long_name;
        node.long_name = strA + FAGI.Constants.PROPERTY_SEPARATOR + strB;
        node.newPred = repA + FAGI.Constants.PROPERTY_SEPARATOR + repB;
    }
    //selectedProperties['id'+next_id] = lastSelectedFromA.long_name+FAGI.Constants.PROPERTY_SEPARATOR+lastSelectedFromB.long_name;
    //alert(selectedProperties['id'+next_id]);
    node.innerHTML = text;
    $(node).on('input', function (e) {
        var row = $("#bFusionTable tr")[this.rowIndex - 1];
        this.newPred = e.target.value;
        $(row).get(0).newPred = e.target.value;
        $(row).find("td")[1].innerHTML = e.target.value;
    });

    next_id++;
    document.getElementById("matchList").appendChild(node);

    //Reset Selection
    lastSelectedFromA = null;
    lastSelectedFromB = null;

    // Update fusion table
    updateBFusionTable(node);
});

function replaceAt(str, at, withChar) {
    return str.substr(0, at) + withChar + str.substr(at + withChar.length);
}



function matchedSchemaClicked() {
    FAGI.ActiveState.lastMatchedSchemaClicked = this;
    //alert(document.getElementById("matchList"));
    //alert(this);
}

function linkMatchedSchemaClicked() {
    FAGI.ActiveState.lastLinkMatchedSchemaClicked = this;
    //alert(document.getElementById("matchList"));
    //alert(this);
}

function assignClusters(assigns) {
    //alert(assigns.numOfClusters);
    for (var i = 0; i < assigns.numOfClusters; i++) {
        //alert($("#clusterSelector").html());
        $("#clusterSelector").append("<option value=\"" + i + "\" >Cluster " + i + "</option>");

    }

    $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
        var assign = assigns.results[element.attributes.a];
        element.attributes.cluster = assign.cluster;
    });
    //$.each(responseJson.foundB, function (index, element) {}
}

function performClustering() {
    //alert('tom');
    console.log($("#slider").slider("value"));
    var vLen = $("#connVecDirCheck :radio:checked + label").text();
    var vDir = $("#connVecLenCheck :radio:checked + label").text();
    var cov = $("#connCoverageCheck :radio:checked + label").text();

    console.log($("#connVecDirCheck :radio:checked + label").text());
    console.log($("#connVecLenCheck :radio:checked + label").text());
    console.log($("#connCoverageCheck :radio:checked + label").text());

    if (vLen == "" && vDir == "" && cov == "") {
        alert("please select at least one attribute for clustering");
    } else {
        var sendData = new Object();

        if (vLen == 'YES')
            sendData.vLen = 'YES';
        if (vDir == 'YES')
            sendData.vDir = 'YES';
        if (vLen == 'YES')
            sendData.cov = 'YES';
        sendData.clusterCount = $("#slider").slider("value");

        //alert('file', $('input[type=file]')[0].files[0]);
        //alert($('#swapButton').is(":checked"));
        //alert('hey');
        FAGI.Utilities.enableSpinner();
        $.ajax({
            url: 'ClusteringServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                //console.log("All good "+responseText);
                assignClusters(responseText);
                FAGI.Utilities.disableSpinner();
            },
            error: function (responseText) {
                FAGI.Utilities.disableSpinner();
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
 var text = '<input class="match" type="text" name="lname" value="'+lastSelectedFromA.innerHTML+FAGI.Constants.PROPERTY_SEPARATOR+lastSelectedFromB.innerHTML+'"/>';
 selectedProperties['id'+next_id] = lastSelectedFromA.innerHTML+FAGI.Constants.PROPERTY_SEPARATOR+lastSelectedFromB.innerHTML;
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
    var start = $('#matchList li').index(FAGI.ActiveState.lastMatchedSchemaClicked);
    //alert("Start : "+start);
    document.getElementById("matchList").removeChild(FAGI.ActiveState.lastMatchedSchemaClicked);
    //alert('done : ' + FAGI.ActiveState.lastMatchedSchemaClicked.rowIndex);
    $("#bFusionTable tr").eq(FAGI.ActiveState.lastMatchedSchemaClicked.rowIndex - 1).remove();
    var rowCount = $('#matchList li').length;
    //alert(rowCount);
    for (var i = start; i < rowCount; i++) {
        //alert($("#matchList li").eq(i).get(0));
        //alert($("#matchList li").eq(i).get(0).rowIndex);
        $("#matchList li").eq(i).get(0).rowIndex--;
        //alert($("#matchList li").eq(i).get(0).rowIndex);
    }
    //alert($('#schemasB').val());
    //alert($('#schemasB').text());
});

$('#removeLinkSchema').click(function () {
    var start = $('#linkMatchList li').index(FAGI.ActiveState.lastLinkMatchedSchemaClicked);
    //alert("Start : "+start);
    document.getElementById("linkMatchList").removeChild(FAGI.ActiveState.lastLinkMatchedSchemaClicked);
    //alert('done : ' + FAGI.ActiveState.lastLinkMatchedSchemaClicked.rowIndex);
    $("#fusionTable tr").eq(FAGI.ActiveState.lastLinkMatchedSchemaClicked.rowIndex - 1).remove();
    var rowCount = $('#linkMatchList li').length;
    //alert(rowCount);
    for (var i = start; i < rowCount; i++) {
        //alert($("#linkMatchList li").eq(i).get(0));
        //alert($("#linkMatchList li").eq(i).get(0).rowIndex);
        $("#linkMatchList li").eq(i).get(0).rowIndex--;
        //alert($("#linkMatchList li").eq(i).get(0).rowIndex);
    }
    //alert('done');
    //alert($('#schemasB').val());
    //alert($('#schemasB').text());
});

function loadLinkedEntities(formData) {
    FAGI.Utilities.enableSpinner();
    $.ajax({
        url: 'LinksServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseJson) {
            //alert("All good "+responseJson);
            var list = document.getElementById("linksList");
            var typesA = document.getElementById("typeListA");
            var typesB = document.getElementById("typeListB");
            //var arrays = responseText.split("+>>>+");
            //list.innerHTML = arrays[0];
            //typesA.innerHTML = arrays[1];
            //typesB.innerHTML = arrays[2];
            if (responseJson.result.statusCode == 0) {
                list.innerHTML = responseJson.linkListHTML;
                typesA.innerHTML = responseJson.filtersListAHTML;
                typesB.innerHTML = responseJson.filtersListBHTML;
            } else {
                //alert(responseJson.result.message);
                $("#buttonL").prop('disabled', false);
            }
            FAGI.Utilities.disableSpinner();
        },
        error: function (xhr, status, errorThrown) {
            FAGI.Utilities.disableSpinner();
            alert("Sorry, there was a problem!");
            console.log("Error: " + errorThrown);
            console.log("Status: " + status);
            alert("Error");
        },
        data: formData,
        //Options to tell jQuery not to process data or worry about content-type.
        cache: false,
        contentType: false,
        processData: false
    });
}

$('#buttonL').click(function () {
    //var formData = new FormData(document.getElementById("linksDiv"));
    var formData = new FormData();
    formData.append('file', $('input[type=file]')[0].files[0]);

    loadLinkedEntities(formData);
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

    FAGI.Utilities.enableSpinner();
    $("#matchingMenu").trigger('click');
    if (!FAGI.ActiveState.linksPreviewed) {
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
                FAGI.ActiveState.linksPreviewed = true;
                if (responseText === "Connection parameters not set") {
                    $('#fg-dataset-label').text(responseText);
                } else {
                    //alert(responseText);
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
                        var restFuseAction = $("#fg-batch-fuse-rest-selector option:selected").text();

                        $.ajax({
                            // request type
                            type: "POST",
                            // the URL for the request
                            url: "BatchFusionServlet",
                            // the data to send (will be converted to a query string)
                            data: {propsJSON: sndJSON, factJSON: sndShiftJSON, clusterJSON: clusterJSON, cluster: $("#clusterSelector").val(), rest: restFuseAction},
                            // the type of data we expect back
                            dataType: "json",
                            // code to run if the request succeeds;
                            // the response is passed to the function
                            success: function (responseJson) {
                                //$('#connLabel').text(responseJson);
                                FAGI.Utilities.disableSpinner();
                                batchFusionPreview(responseJson);
                                //previewLinkedGeom(responseJson);
                                //fusionPanel(event, responseJson);
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
                    } else {
                        FAGI.Utilities.disableSpinner();
                        addMapData(responseText);
                    }
                    $('#finalButton').prop('disabled', true);
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
            var restFuseAction = $("#fg-batch-fuse-rest-selector option:selected").text();

            $.ajax({
                // request type
                type: "POST",
                // the URL for the request
                url: "BatchFusionServlet",
                // the data to send (will be converted to a query string)
                data: {propsJSON: sndJSON, factJSON: sndShiftJSON, clusterJSON: clusterJSON, cluster: $("#clusterSelector").val(), rest: restFuseAction},
                // the type of data we expect back
                dataType: "json",
                // code to run if the request succeeds;
                // the response is passed to the function
                success: function (responseJson) {
                    //$('#connLabel').text(responseJson);
                    batchFusionPreview(responseJson);
                    //previewLinkedGeom(responseJson);
                    //fusionPanel(event, responseJson);
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
    }
}

function createLinkCluster(cluster) {
    var ret = new Array();
    if (cluster == 9999) {
        /*$.each(FAGI.ActiveState.activeFeatureClusterA, function (index, element) {
            var clusterLink = new Object();
            clusterLink.nodeA = element.attributes.la.attributes.a;
            clusterLink.nodeB = element.attributes.lb.attributes.a;
            ret[ret.length] = clusterLink;
        });*/
        $l_SelectedListElements = $("#fg-user-selection-list li");
        $l_SelectedListElements.each(function (index, element) {
            //alert(index);
            //alert($(element).find("input:checked").length);
            if ( $(element).find("input:checked").length > 0 ) {
                var clusterLink = new Object();
                var l_ElementPair = $(element).find("label").text().split("<-->");
                clusterLink.nodeA = l_ElementPair[0];
                clusterLink.nodeB = l_ElementPair[1];
                ret[ret.length] = clusterLink;
            }
        });
    } else {
        $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
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
    var toDelFeatures = new Array();
    if (cluster < 0) {
        /*$.each(geomsJSON.fusedGeoms, function (index, element) {
         var clusterLink = new Object();
         var geom = element.geom;
         addGeom(index, geom);
         console.log("Got " + element.nb + " with geom " + element.geom);
         });
         */

        $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
            var clusterLink = new Object();
            var geom = geomsJSON.fusedGeoms[element.attributes.a];
            addGeom(element, geom.geom);
            //console.log("Got " + geom.nb + " with geom " + geom.geom);
        });

    } else if (cluster == 9999) {
        $.each(FAGI.ActiveState.activeFeatureClusterA, function (index, element) {
            var geom = geomsJSON.fusedGeoms[element.attributes.a];
            
            if (typeof geom == 'undefined')
                return true;
            
            //toDelFeatures[toDelFeatures.length] = element.attributes.links[0];
            toDelFeatures[toDelFeatures.length] = element;
            addGeom(element, geom.geom);
            //console.log("In Custom cluster Got " + geom.nb + " with geom " + geom.geom);
        });
    } else {
        $.each(FAGI.MapUI.Layers.vectorsLinks.features, function (index, element) {
            if (element.attributes.cluster == cluster) {
                toDelFeatures[toDelFeatures.length] = element;
                var geom = geomsJSON.fusedGeoms[element.attributes.a];
                addGeom(element, geom.geom);
                //console.log("Got " + geom.nb + " with geom " + geom.geom);
            }
        });
    }

    if (toDelFeatures.length)
        FAGI.MapUI.Layers.vectorsLinks.destroyFeatures(toDelFeatures);
    else
        FAGI.MapUI.Layers.vectorsLinks.destroyFeatures();
}

function addGeom(feat, geom) {
    //console.log(feat);
    //console.log(geom);
    toDeleteFeatures = new Array();
    feat.attributes.la.style = {display: 'none'};
    feat.attributes.lb.style = {display: 'none'};

    FAGI.MapUI.Layers.vectorsA.drawFeature(feat.attributes.la);
    FAGI.MapUI.Layers.vectorsB.drawFeature(feat.attributes.lb);

    //console.log("Link feature "+linkFeature);
    //alert(resp.geom);
    $.each(geom.split('|'), function (index, value) {
        //alert(index + ": " + value);
        var linkFeature = FAGI.MapUI.wkt.read(value);

        if (Object.prototype.toString.call(linkFeature) === '[object Array]') {
            //alert('Array');
            for (var i = 0; i < linkFeature.length; i++) {
                linkFeature[i].geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
                linkFeature[i].attributes = {'a': feat.attributes.a, 'la': feat.attributes.la, 'lb': feat.attributes.lb, 'cluster': feat.attributes.cluster};
                linkFeature[i].validated = true;
                linkFeature[i].prev_fused = true;

                FAGI.MapUI.Layers.vectorsFused.addFeatures([linkFeature[i]]);
                //alert('done');
            }
            toDeleteFeatures[toDeleteFeatures.length] = feat;
        } else {
            //alert('reached');
            linkFeature.geometry.transform(FAGI.Constants.WGS84, FAGI.MapUI.map.getProjectionObject());
            linkFeature.attributes = {'a': feat.attributes.a, 'la': feat.attributes.la, 'lb': feat.attributes.lb, 'cluster': feat.attributes.cluster};

            linkFeature.prev_fused = true;
            linkFeature.validated = true;
            FAGI.MapUI.Layers.vectorsFused.addFeatures([linkFeature]);
        }
    });

    //FAGI.MapUI.Layers.vectorsA.redraw();
    //FAGI.MapUI.Layers.vectorsB.redraw();
    //FAGI.MapUI.Layers.vectorsLinks.refresh();
    //FAGI.MapUI.Layers.vectorsFused.refresh();

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

function initBatchFusionTable(val) {
    avail_trans = "";
    avail_meta_trans = "";
    $.each(val.geomTransforms, function (index, element) {
        avail_trans += "<option value=\"" + element + "\">" + element + "</option>";

    });
    $.each(val.metaTransforms, function (index, element) {
        avail_meta_trans += "<option value=\"" + element + "\">" + element + "</option>";
    });

    var s = "<p class=\"geoinfo\" id=\"link_name\">Fusion Table</p>\n" +
            " <table class=\"rwd-table\" border=1 id=\"bFusionTable\">\n" +
            " <tr>\n" +
            " <td>Value from " + $('#fg-dataset-input-a').val() + "</td>\n" +
            " <td>Predicate</td>\n" +
            " <td>Value from " + $('#fg-dataset-input-b').val() + "</td>\n" +
            " <td>Action</td>\n" +
            " </tr>\n" +
            " <tr>\n" +
            " <td title=\"" + "WKT Geometry" + "\">" + "WKT Geometry" + "</td>\n" +
            " <td>asWKT</td>\n" +
            " <td title=\"" + "WKT Geometry" + "\">" + "WKT Geometry" + "</td>\n" +
            " <td><select id=\"bgeoTrans\" style=\"color: black; width: 100%;\">" + avail_trans + "</select></td>\n" +
            " </tr>\n" +
            " </table>" +
            " <fieldset id=\"fg-batch-fuse-rest-fieldset\"> " +
            " <label for=\"fg-batch-fuse-rest-selector\" id=\"fg-batch-fuse-rest-label\">Remaining Metadata Action</label>" +
            " <select name=\"fg-batch-fuse-rest-selector\" id=\"fg-batch-fuse-rest-selector\">" +
            " <option>Keep A</option>" +
            " <option selected=\"selected\">None</option>" +
            " <option>Keep B</option>" +
            " <option>Keep Both</option>" +
            " </select>" +
            " </fieldset> " +
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
    //$("#fg-batch-fuse-rest-selector").selectmenu();
    //$(".ui-menu-item").css("color", "black");
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
    FAGI.Utilities.enableSpinner();
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    var links = new Array();
    $('#linksList input:checked').each(function () {
        //alert(($(this).parent().html()));
        //alert(($(this).text()));
        //alert(getText($(this).get(0)));
        links[links.length] = getText($(this).parent().get(0));
        //alert(getText($(this).get(0)));
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

            //$('#linksList').html("");

            initBatchFusionTable(responseJson);

            // Add properties from dataset A
            $.each(responseJson.foundA, function (index, element) {
                var opt = document.createElement("li");
                //console.log(opt);
                var optlbl = document.createElement("div");
                $(optlbl).addClass("scored");
                optlbl.innerHTML = "";

                var tokens = index.split(",");
                var result_str = "";
                for (var i = 0; i < tokens.length; i++) {

                    var trunc = FAGI.Utilities.getPropertyName(tokens[i]);

                    result_str += trunc;
                    if (i != (tokens.length - 1)) {
                        result_str += ",";
                    }
                }

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
            //alert(selectedProperties[lastSelectedFromA.innerHTML+FAGI.Constants.PROPERTY_SEPARATOR+lastSelectedFromB.innerHTML]);
            node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
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
        if (linkLastSelectedFromA !== null) {
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

            if (window.event.ctrlKey) {
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

        list = document.getElementById("linkSchemasA");
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
        if (linkLastSelectedFromB !== null) {
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

            if (window.event.ctrlKey) {
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

            var list = document.getElementById("schemasA");
            var listItems = list.getElementsByTagName("li");
            if (typeof elems != 'undefined') {
                $.each(listItems, function (index1, element1) {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = this.backColor;

                    element1.prev_selected = false;
                });
            }

            //alert("as");
            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
                this.style.backgroundColor = this.backColor;
        } else {
            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
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
                });
            }

            if (window.event.ctrlKey) {
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
                        if (element.score > FAGI.ActiveState.scoreThreshold) {

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

            var list = document.getElementById("schemasB");
            var listItems = list.getElementsByTagName("li");
            if (typeof elems != 'undefined') {
                $.each(listItems, function (index1, element1) {
                    if (element1.match_count > 0)
                        element1.style.backgroundColor = "yellow";
                    else
                        element1.style.backgroundColor = this.backColor;

                    element1.prev_selected = false;
                });
            }

        } else {
            if (this.match_count > 0)
                this.style.backgroundColor = "yellow";
            else
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
                });
            }
            /*
             if (lastSelectedFromB.match_count > 0)
             lastSelectedFromB.style.backgroundColor = "yellow";
             else
             lastSelectedFromB.style.backgroundColor = lastSelectedFromB.backColor;
             lastSelectedFromB.prev_selected = false;
             */

            if (window.event.ctrlKey) {
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
                        if (element.score > FAGI.ActiveState.scoreThreshold) {
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
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //var list = document.getElementById("linksList");
            if (responseJson.result.statusCode == 0) {
                $("#linksList").html(responseJson.linksHTML);
            } else {
                alert(responseJson.result.message);
            }
            //list.innerHTML = responseText;
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
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {
            //$('#connLabel').text(responseText);
            if (responseJson.result.statusCode == 0) {
                $("#linksList").html(responseJson.linksHTML);
            } else {
                alert(responseJson.result.message);
            }
            alert(1);
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
    FAGI.Utilities.enableSpinner();
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
            FAGI.Utilities.disableSpinner();
            if (responseJson.statusCode == 0) {
                $("#datasetMenu").trigger('click');
            }
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

function createUser()
{
    
}

function loginUser()
{
    
}

function bazi(elem) {
    //alert(elem.checked);
}

function setDatasets()
{
    var values = $('#dataDiv').serialize();
    //alert( values );
    //alert($('#fg-fetch-fused-check').prop('checked'));
    FAGI.Utilities.enableSpinner();
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "DatasetsServlet",
        // the data to send (will be converted to a query string)
        data: values,
        // the type of data we expect back
        dataType: "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function (responseJson) {

            FAGI.Utilities.disableSpinner();
            $('#fg-dataset-label').text("Datasets accepted");
            $('#datasetNameA').html($('#fg-dataset-input-a').val());
            $('#datasetNameB').html($('#fg-dataset-input-b').val());
            $('#legendSetA').html($('#fg-dataset-input-a').val());
            $('#legendSetB').html($('#fg-dataset-input-b').val());
            $('#datasetNameA').html($('#fg-dataset-input-a').val());
            $('#datasetNameB').html($('#fg-dataset-input-b').val());
            $('#legendLinkSetA').html($('#fg-dataset-input-a').val());
            $('#legendLinkSetB').html($('#fg-dataset-input-b').val());

            //Loaqd links through endpoint
            //alert(JSON.stringify(responseJson.remoteLinks));
            //alert(responseJson.remoteLinks)
            ;
            if (responseJson.remoteLinks) {
                //alert("NAI");
                loadLinkedEntities(null);
            }

            //Scan target dataset for any already fused geometry
            if ($('#fg-fetch-fused-check').prop('checked'))
                scanGeometries();

            // Disable file uploading if a SPARQL endpoint for links is provided
            if ($('#fg-dataset-input-l').val() && $('#fg-endpoint-input-l').val())
                $("#buttonL").prop('disabled', true);

            $("#linksMenu").trigger('click');
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

function scanGeometries() {
    FAGI.Utilities.enableSpinner();
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
            $('#fg-dataset-label').text(responseJSON.result.message);
            //alert('tom');
            FAGI.Utilities.disableSpinner();
            if (responseJSON.result.statusCode == 0)
                addFusedMapDataJson(responseJSON);
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