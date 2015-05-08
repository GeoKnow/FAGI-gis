/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(document).ready(function () {                        // When the HTML DOM is ready loading, then execute the following function...
    //$.ajaxSetup({
    //    cache: false
    //});
    //alert(OpenLayers.Events.BROWSER_EVENTS);
    init();
});

var form = document.getElementById('file-form');
var fileSelect = document.getElementById('file-select');
var uploadButton = document.getElementById('upload-button');
var States = new Array();
var scoreThreshold = 0.3;

function init() {
    $('#popupBBoxMenu').hide();
    $('#popupTransformMenu').hide();
    $('#popupMenu').hide();

    $(".buttonset").buttonset();
    $('#connButton').click(setConnection);
    $('#dataButton').click(setDatasets);
    $('#loadButton').click(setConnection);
    $('#buttonFilterLinksA').click(filterLinksA);
    $('#buttonFilterLinksB').click(filterLinksB);
    $('#linksButton').click(schemaMatch);
    $('#allLinksButton').click(selectAll);
    $('#finalButton').click(submitLinks);

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
    $('#clusteringTool').click(expandClusteringPanel);   
    $('#fetchTool').click(activateFecthUnlinked);
    $('#visibleSelect').click(activateVisibleSelect);

    $('#clusterButton').click(performClustering);

    $('#transformBBoxButton').click(enableBBoxTransform);
    $('#fetchBBoxContainedButton').click(fetchContained);
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
    $('#createLinkButton').click(function () {
        if ( lastPo == null ) {
            lastPo = activeFeature.geometry.getCentroid(true);
            lastPo.node = activeFeature;
        } else {
            nowPo = activeFeature.geometry.getCentroid(true);
            nowPo.node = activeFeature;
            createNewLink(nowPo.node, lastPo.node);
        }
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

    form.onsubmit = function (event) {
        event.preventDefault();

        // Update button text.
        uploadButton.innerHTML = 'Uploading...';
        alert("tomas");

        var files = fileSelect.files;

        var formData = new FormData();

        for (var i = 0; i < files.length; i++) {
            var file = files[i];


            // Add the file to the request.
            formData.append('links', file, file.name);
            //alert(file.name);

            $.ajax({
                // request type
                type: "POST",
                // the URL for the request
                url: "LinksServlet",
                // the data to send (will be converted to a query string)
                data: formData,
                // the type of data we expect back
                dataType: "html",
                // code to run if the request succeeds;
                // the response is passed to the function
                success: function (responseText) {
                    //alert("TIMMY");
                },
                // code to run if the request fails; the raw request and
                // status codes are passed to the function
                error: function (xhr, status, errorThrown) {
                    alert("Sorry, there was a problem!");
                    alert("xhr: ", xhr, " status: ", status, " error: ", errorThrown);
                },
                // code to run regardless of success or failure
                complete: function (xhr, status) {
                    alert("TIMMY");
                }
            });

            //alert(file.name);

        }
    };

    //alert("luda");
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
    alert('Radius ' + radius);
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


function createNewLink(nodeA, nodeB) {
    var line2 = new OpenLayers.Geometry.LineString([lastPo, nowPo]);
    linkFeature = new OpenLayers.Feature.Vector(line2);
    //alert('Lon Lat');
    linkFeature.attributes = {'a': 'aalala'};
    linkFeature.style = {
        strokeColor: "red",
        cursor: "pointer",
        fillColor: "red",
        strokeOpacity: 0.5,
        strokeWidth: 3,
        fillOpacity: 0.5,
    title: nodeA.attributes.a};
    linkFeature.attributes = {'la': nodeA, 'a': nodeA.attributes.a, 'lb': nodeB};
    
    vectorsLinksTemp.destroyFeatures();
    vectorsLinks.addFeatures([linkFeature]);
    vectorsLinks.drawFeature(linkFeature);

    lastPo = null;
/*
    var sendData = new Object();
    
    $.ajax({
        url: 'CreateLinkServlet', //Server script to process data
        type: 'POST',
        //Ajax events
        // the type of data we expect back
        dataType: "json",
        success: function (responseText) {
            console.log("All good " + responseText);
        },
        error: function (responseText) {
            alert("All bad " + responseText);
            alert("Error");
        },
        data: sendData
                //Options to tell jQuery not to process data or worry about content-type.
    });*/
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
        //alert(element.prev_selected);
        if (element.prev_selected === true)
            strA += element.innerHTML;
    });

    var listB = document.getElementById("linkSchemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        //alert(element.prev_selected);
        if (element.prev_selected === true)
            strB += element.innerHTML;
    });
    //alert(strA);
    //alert(strB);
    var node = document.createElement("li");
    node.onclick = linkMatchedSchemaClicked;
    var text = '<input class="match" type="text" name="lname" value="' + linkLastSelectedFromA.innerHTML + '=>' + linkLastSelectedFromB.innerHTML + '"/>';
    node.long_name = linkLastSelectedFromA.long_name + '=>' + linkLastSelectedFromB.long_name;
    var repA = getText(linkLastSelectedFromA.firstChild);
    var repB = getText(linkLastSelectedFromB.firstChild);
    var text;
    if (linkLastSelectedFromA === null) {
        text = '<input class="match" type="text" name="lname" value="' + repB + '"/>';
        node.long_name = 'dummy' + '=>' + linkLastSelectedFromB.long_name;
    }
    else if (linkLastSelectedFromB === null) {
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        node.long_name = linkLastSelectedFromA.long_name + '=>' + 'dummy';
    }
    else {
        text = '<input class="match" type="text" name="lname" value="' + repA + '=>' + repB + '"/>';
        node.long_name = linkLastSelectedFromA.long_name + '=>' + linkLastSelectedFromB.long_name;
    }
    //selectedProperties['link_id'+next_link_id] = linkLastSelectedFromA.long_name+'=>'+linkLastSelectedFromB.long_name;
    //alert(selectedProperties['id'+next_id]);
    //alert('id '+linkLastSelectedFromA.long_name+'=>'+linkLastSelectedFromB.long_name);
    //alert(selectedProperties['id'+next_id]);
    node.innerHTML = text;

    next_link_id++;
    document.getElementById("linkMatchList").appendChild(node);
    
    updateFusionTable();
});

function getText(obj) {
    return obj.textContent ? obj.textContent : obj.innerText;
}

$('#addSchema').click(function () {
    if (lastSelectedFromA === null && lastSelectedFromB === null) {
        //alert("tom");
        alert("No matching selected");
        //alert(lastSelectedFromB === null);
        return;
    }

    if (lastSelectedFromA === null && lastSelectedFromB === null) {
        //alert("tom");
        alert("No matching selected");
        //alert(lastSelectedFromB === null);
        return;
    }

    var strA = "";
    var strB = "";
    var listA = document.getElementById("schemasA");
    var listItemsA = listA.getElementsByTagName("li");
    $.each(listItemsA, function (index, element) {
        //alert(element.prev_selected);
        if (element.prev_selected === true)
            strA += element.innerHTML;
    });
    var listB = document.getElementById("schemasB");
    var listItemsB = listB.getElementsByTagName("li");
    $.each(listItemsB, function (index, element) {
        //alert(element.prev_selected);
        if (element.prev_selected === true)
            strB += element.innerHTML;
    });
    //alert(strA);
    //alert(strB);
    var node = document.createElement("li");
    node.onclick = matchedSchemaClicked;
    var text;
    var repA = getText(lastSelectedFromA.firstChild);
    var repB = getText(lastSelectedFromB.firstChild);
    if (lastSelectedFromA === null) {
        text = '<input class="match" type="text" name="lname" value="' + repB + '"/>';
        node.long_name = 'dummy' + '=>' + lastSelectedFromB.long_name;
    }
    else if (lastSelectedFromB === null) {
        text = '<input class="match" type="text" name="lname" value="' + repA + '"/>';
        node.long_name = lastSelectedFromA.long_name + '=>' + 'dummy';
    }
    else {
        text = '<input class="match" type="text" name="lname" value="' + repA + '=>' + repB + '"/>';
        node.long_name = lastSelectedFromA.long_name + '=>' + lastSelectedFromB.long_name;
    }
    //selectedProperties['id'+next_id] = lastSelectedFromA.long_name+'=>'+lastSelectedFromB.long_name;
    //alert(selectedProperties['id'+next_id]);
    node.innerHTML = text;
    for (var name in selectedProperties) {
        //alert(name);
    }
    next_id++;
    document.getElementById("matchList").appendChild(node);
});

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
    alert(assigns.numOfClusters);
    for(var i = 0; i < assigns.numOfClusters; i++) {
        alert($("#clusterSelector").html());
        $("#clusterSelector").append("<option>Cluster "+i+"</option>");
        
    }
    $.each(vectorsLinks.features, function (index, element) {
        var assign = assigns.results[element.attributes.a];
        //console.log(element.attributes.a);
        //console.log(assign.cluster);
        console.log(element.attributes.a + " and " + element.attributes.b + " in Cluster "+assign.cluster+"\n");
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
        $.ajax({
            url: 'ClusteringServlet', //Server script to process data
            type: 'POST',
            //Ajax events
            // the type of data we expect back
            dataType: "json",
            success: function (responseText) {
                console.log("All good "+responseText);
                assignClusters(responseText);
            },
            error: function (responseText) {
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
        },
        error: function (responseText) {
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

function submitLinks() {
    //alert('tom');
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

    //alert(sendData);
    $("#matchingMenu").trigger('click');
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
            if (responseText === "Connection parameters not set")
                $('#dataLabel').text(responseText);
            else
                addMapData(responseText);
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

var mappings;
var schemasA = new Object;
var schemasB = new Object;

function schemaMatch() {
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    var links = new Array();
    for (var i = 0; i < listItem.length; i++) {
        //alert(listItem[i]);
        var listLabel = listItem[i].getElementsByTagName("label");
        for (var j = 0; j < listLabel.length; j++) {
            //alert("Label Last Child : "+listLabel[j].lastChild.data);
            links[links.length] = listLabel[j].lastChild.data;
        }
    }
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

            $.each(responseJson.foundA, function (index, element) {
                var opt = document.createElement("li");
                console.log(opt);
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
                opt.innerHTML = result_str;
                opt.long_name = index;
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

                opt.innerHTML = result_str;
                opt.long_name = index;
                opt.onclick = propSelectedB;
                opt.prev_selected = false;
                opt.backColor = opt.style.backgroundColor;
                opt.match_count = 0;
                opt.appendChild(optlbl);
                optlbl.style.cssFloat = "right";
                schemaListB.appendChild(opt);
            });
            var node = document.createElement("li");
            var text = '<input class="match" type="text" name="lname" value="' + 'http://www.opengis.net/ont/geosparql#asWKT' + '"/>';
            //alert(selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML]);
            node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
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

        this.style.backgroundColor = linkBackColorA;
        linkLastSelectedFromA = null;
        this.prev_selected = false;
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
                            //alert(element1);
                            //alert(element1.innerHTML);
                            element1.match_count--;
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                }
            });
            //alert('edw');
            if (linkLastSelectedFromA.match_count > 0)
                linkLastSelectedFromA.style.backgroundColor = "yellow";
            else
                linkLastSelectedFromA.style.backgroundColor = linkLastSelectedFromA.backColor;
            linkLastSelectedFromA.prev_selected = false;
        }
        //alert("long name "+this.long_name);
        //alert(linkMatchesJSON.m.foundA);
        var elems = linkMatchesJSON.m.foundA[this.long_name];
        //alert(this.long_name);
        //alert(elems);
        //alert(linkMatchesJSON);
        //alert(linkMatchesJSON == null);
        //alert(JSON.stringify(linkMatchesJSON));
        //alert(JSON.stringify(linkMatchesJSON.m));
        var list = document.getElementById("linkSchemasB");
        var listItems = list.getElementsByTagName("li");

        //alert("ID TYPE"+listItems);
        //alert('edw');
        $.each(listItems, function (index1, element1) {
            //alert(element1.backColor);
            if (!element1.prev_selected)
                element1.style.backgroundColor = element1.backColor;
            if (typeof elems !== "undefined") {
                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        //alert(element1.long_name);
                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = element.score;
                        element1.match_count++;
                        if (!element1.prev_selected)
                            element1.style.backgroundColor = "yellow";
                    }
                });
            }
        });
        //alert('edw');
        linkLastSelectedFromA = this;
        this.prev_selected = true;
    }
}

function linkPropSelectedB() {
    if (this.prev_selected === true) {

        this.style.backgroundColor = linkBackColorB;
        linkLastSelectedFromB = null;
        this.prev_selected = false;
    } else {
        this.style.backgroundColor = "blueviolet";

        /* to be removed for m to n */
        if (linkLastSelectedFromB !== null) {
            //alert(linkLastSelectedFromA.long_name);
            var elems = linkMatchesJSON.m.foundB[linkLastSelectedFromB.long_name];

            var list = document.getElementById("linkSchemasA");
            var listItems = list.getElementsByTagName("li");

            $.each(listItems, function (index1, element1) {
                //alert(element1.backColor);
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                if (typeof elems !== "undefined") {
                    $.each(elems, function (index, element) {
                        if (element1.long_name == element.rep) {
                            //alert(element1);
                            //alert(element1.innerHTML);
                            element1.match_count--;
                            if (element1.match_count == 0 && !element1.prev_selected)
                                element1.style.backgroundColor = element1.backColor;

                            var scoreLbl = element1.getElementsByTagName("div");
                            scoreLbl[0].innerHTML = "";
                        }
                    });
                }
            });
            //alert('edw');
            if (linkLastSelectedFromB.match_count > 0)
                linkLastSelectedFromB.style.backgroundColor = "yellow";
            else
                linkLastSelectedFromB.style.backgroundColor = linkLastSelectedFromB.backColor;
            linkLastSelectedFromB.prev_selected = false;
        }
        //alert("long name "+this.long_name);
        //alert(linkMatchesJSON.m.foundA);
        var elems = linkMatchesJSON.m.foundB[this.long_name];
        //alert(elems);
        //alert(linkMatchesJSON);
        //alert(linkMatchesJSON == null);
        //alert(JSON.stringify(linkMatchesJSON));
        //alert(JSON.stringify(linkMatchesJSON.m));
        var list = document.getElementById("linkSchemasA");
        var listItems = list.getElementsByTagName("li");

        //alert("ID TYPE"+listItems);
        //alert('edw');
        $.each(listItems, function (index1, element1) {
            //alert(element1.backColor);
            if (!element1.prev_selected)
                element1.style.backgroundColor = element1.backColor;
            if (typeof elems !== "undefined") {
                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        //alert(element1.long_name);
                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = element.score;
                        element1.match_count++;
                        if (!element1.prev_selected)
                            element1.style.backgroundColor = "yellow";
                    }
                });
            }
        });
        //alert('edw');
        linkLastSelectedFromB = this;
        this.prev_selected = true;
    }
}

function propSelectedA() {
    //alert(this);
    //alert(this.prev_selected);

    //this.prev_selected = true;
    //alert(this.prev_selected);
    if (this.prev_selected === true) {
        var elems = mappins.foundA[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasB");
        var listItems = list.getElementsByTagName("li");
        $.each(elems, function (index, element) {
            $.each(listItems, function (index1, element1) {
                //alert("enter");
                if (element1.long_name == element.rep) {

                    element1.match_count--;
                    if (element1.match_count == 0 && !element1.prev_selected)
                        element1.style.backgroundColor = element1.backColor;

                    var scoreLbl = element1.getElementsByTagName("div");
                    scoreLbl[0].innerHTML = "";
                }
            });
        });
        //alert("as");
        if (this.match_count > 0)
            this.style.backgroundColor = "yellow";
        else
            this.style.backgroundColor = this.backColor;
        //alert("as");
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

            $.each(listItems, function (index1, element1) {
                //alert(element1.backColor);
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        //alert(element1);
                        //alert(element1.innerHTML);
                        element1.match_count--;
                        if (element1.match_count == 0 && !element1.prev_selected)
                            element1.style.backgroundColor = element1.backColor;

                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = "";
                    }
                });
            });
            if (lastSelectedFromA.match_count > 0)
                lastSelectedFromA.style.backgroundColor = "yellow";
            else
                lastSelectedFromA.style.backgroundColor = lastSelectedFromA.backColor;
            lastSelectedFromA.prev_selected = false;
        }
        var elems = mappins.foundA[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasB");
        var listItems = list.getElementsByTagName("li");

        //alert("ID TYPE"+listItems);
        $.each(listItems, function (index1, element1) {
            //alert(element1.backColor);
            if (!element1.prev_selected)
                element1.style.backgroundColor = element1.backColor;
            $.each(elems, function (index, element) {
                if (element1.long_name == element.rep) {
                    if (element.score > scoreThreshold) {
                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = element.score;
                        element1.match_count++;
                        if (!element1.prev_selected)
                            element1.style.backgroundColor = "yellow";
                    }
                }
            });
        });

        lastSelectedFromA = this;
        this.prev_selected = true;
    }
}

function propSelectedB() {
    //alert(this);
    //alert(this.prev_selected);

    //this.prev_selected = true;
    //alert(this.prev_selected);
    if (this.prev_selected === true) {
        //alert("ho");

        //alert("ho");vvvv
        var elems = mappins.foundB[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasA");
        var listItems = list.getElementsByTagName("li");

        $.each(elems, function (index, element) {
            //alert(element);
            $.each(listItems, function (index1, element1) {
                //alert("alert");
                if (element1.long_name == element.rep) {

                    element1.match_count = 0;
                    if (element1.match_count == 0 && !element1.prev_selected)
                        element1.style.backgroundColor = element1.backColor;

                    var scoreLbl = element1.getElementsByTagName("div");
                    scoreLbl[0].innerHTML = "";
                }
            });
        });

        if (this.match_count > 0)
            this.style.backgroundColor = "yellow";
        else
            this.style.backgroundColor = this.backColor;

        lastSelectedFromB = null;
        this.prev_selected = false;
    } else {
        //this.backColor = this.style.backgroundColor;
        this.style.backgroundColor = "blueviolet";

        /* to be removed for m to n */

        if (lastSelectedFromB !== null) {
            //alert(lastSelectedFromB.match_count);
            var elems = mappins.foundB[lastSelectedFromB.long_name];
            //alert(elems);
            var list = document.getElementById("schemasA");
            var listItems = list.getElementsByTagName("li");

            $.each(listItems, function (index1, element1) {
                if (!element1.prev_selected)
                    element1.style.backgroundColor = element1.backColor;
                $.each(elems, function (index, element) {
                    if (element1.long_name == element.rep) {
                        element1.match_count--;
                        if (element1.match_count == 0 && !element1.prev_selected)
                            element1.style.backgroundColor = element1.backColor;

                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = "";
                    }
                });
            });
            if (lastSelectedFromB.match_count > 0)
                lastSelectedFromB.style.backgroundColor = "yellow";
            else
                lastSelectedFromB.style.backgroundColor = lastSelectedFromB.backColor;
            lastSelectedFromB.prev_selected = false;
        }
        var elems = mappins.foundB[this.long_name];
        //alert(elems);
        var list = document.getElementById("schemasA");
        var listItems = list.getElementsByTagName("li");

        $.each(listItems, function (index1, element1) {
            if (!element1.prev_selected)
                element1.style.backgroundColor = element1.backColor;
            $.each(elems, function (index, element) {
                if (element1.long_name == element.rep) {
                    if (element.score > scoreThreshold) {
                        var scoreLbl = element1.getElementsByTagName("div");
                        scoreLbl[0].innerHTML = element.score;

                        element1.match_count++;
                        if (!element1.prev_selected)
                            element1.style.backgroundColor = "yellow";
                    }
                }
            });
        });

        lastSelectedFromB = this;
        this.prev_selected = true;
    }
}

/*
 * 
 function propSelectedA() {
 //alert(this);
 //alert(this.prev_selected);
 
 //this.prev_selected = true;
 //alert(this.prev_selected);
 if(lastSelectedFromA !== null) 
 lastSelectedFromA.style.backgroundColor = this.style.backgroundColor;
 else 
 backColor = this.style.backgroundColor;
 
 clickCountA++;
 if (clickCountA === 2)
 clickCountA = 0;
 
 if(lastSelectedFromA === this && (clickCountA % 2 === 1)) {
 var list = document.getElementById("schemasB");
 var listItems = list.getElementsByTagName("li");
 $.each(listItems, function(index, element) {
 element.style.backgroundColor = backColor;
 });
 this.style.backgroundColor = backColor;
 lastSelectedFromA = null;
 return;
 } else {
 clickCountA = 0;
 this.style.backgroundColor = "blueviolet";
 }
 //alert(this.innerHTML);
 var elems = mappins[this.innerHTML];
 //alert(elems);
 var list = document.getElementById("schemasB");
 var listItems = list.getElementsByTagName("li");
 $.each(listItems, function(index, element) {
 element.style.backgroundColor = "white";
 });
 //alert("ID TYPE"+listItems);
 $.each(elems, function(index, element) {
 listItems[index].style.backgroundColor = "red";
 });
 
 $.each(responseJson, function(index, element) {
 $.each(element, function(index, element) {
 if (!schemasB[element] !== 1) {
 schemasB[element] = modB++;
 }
 });
 });
 *
 
 lastSelectedFromA = this;
 }
 */
/*
 function propSelectedB() {
 
 if(lastSelectedFromB !== null) 
 lastSelectedFromB.style.backgroundColor = this.style.backgroundColor;
 else 
 backColorB = this.style.backgroundColor;
 
 clickCountB++;
 if (clickCountB === 2)
 clickCountB = 0;
 
 if(lastSelectedFromB === this && (clickCountB % 2 === 1)) {
 var list = document.getElementById("schemasA");
 var listItems = list.getElementsByTagName("li");
 $.each(listItems, function(index, element) {
 element.style.backgroundColor = backColorB;
 });
 this.style.backgroundColor = backColorB;
 lastSelectedFromB = null;
 return;
 } else {
 clickCountB = 0;
 this.style.backgroundColor = "blueviolet";
 }
 //alert(this.innerHTML);
 var elems = mappins[this.innerHTML];
 //alert(elems);
 var list = document.getElementById("schemasA");
 var listItems = list.getElementsByTagName("li");
 $.each(listItems, function(index, element) {
 //element.style.backgroundColor = "white";
 });
 //alert("ID TYPE"+listItems);
 $.each(elems, function(index, element) {
 //listItems[index].style.backgroundColor = "red";
 });
 
 //alert("Selected from B");
 lastSelectedFromB = this;
 }*/

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
            if (responseJson.statusCode == 0) {
                $("#datasetMenu").trigger('click');
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

function bazi(elem) {
    //alert(elem.checked);
}

function setDatasets()
{
    var values = $('#dataDiv').serialize();
    //alert( values );
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
            $('#dataLabel').text("Datasets accepted");
            $('#datasetNameA').html($('#idDatasetA').val());
            $('#datasetNameB').html($('#idDatasetB').val());
            $('#legendSetA').html($('#idDatasetA').val());
            $('#legendSetB').html($('#idDatasetB').val());
            scanGeometries();
            $("#linksMenu").trigger('click');
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

function scanGeometries() {
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
            if (responseJSON.statusCode == 0)
                addFusedMapDataJson(responseJSON);
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