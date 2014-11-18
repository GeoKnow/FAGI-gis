/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(document).ready(function() {                        // When the HTML DOM is ready loading, then execute the following function...
    init();
});

var form = document.getElementById('file-form');
var fileSelect = document.getElementById('file-select');
var uploadButton = document.getElementById('upload-button');

function init() {
    //alert('tomas');
    $('#connButton').click(setConnection);
    $('#dataButton').click(setDatasets);
    $('#loadButton').click(setConnection);
    $('#buttonFilterLinksA').click(filterLinksA);
    $('#buttonFilterLinksB').click(filterLinksB);
    $('#linksButton').click(schemaMatch);
    $('#finalButton').click(submitLinks);
    
    form.onsubmit = function(event) {
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
        dataType : "html",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            alert("TIMMY");
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            alert( "Sorry, there was a problem!" );
            alert("xhr: ", xhr, " status: ",status, " error: ", errorThrown);
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
             alert("TIMMY");
        }
    });
    
    alert(file.name);

}
};

    //alert("luda");
}

$(':file').change(function(){
    var file = this.files[0];
    var name = file.name;
    var size = file.size;
    var type = file.type;
    //Your validation
});

var selectedProperties = new Object();

$('#addSchema').click(function(){
    if ( lastSelectedFromA === null || lastSelectedFromB === null ) {
        //alert("tom");
        //alert(lastSelectedFromA === null);
        //alert(lastSelectedFromB === null);
        return;
    }
    var node=document.createElement("li");
    var text = '<input class="match" type="text" name="lname" value="'+lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML+'"/>';
    selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML] = lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML;
    //alert(selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML]);
    node.innerHTML = text;
    for (var name in selectedProperties) {
        //alert(name);
    }
    document.getElementById("matchList").appendChild(node);
});

$('#removeSchema').click(function(){
    alert("remove");
    alert($('#schemasB').val());
    alert($('#schemasB').text());
});

$('#buttonL').click(function(){
    var formData = new FormData(document.getElementById("linksDiv"));
    //alert("Ludas");
    $.ajax({
        url: 'LinksServlet',  //Server script to process data
        type: 'POST',
        //Ajax events
        success: function( responseText ) {
            var list = document.getElementById("linksList");
            var typesA = document.getElementById("typeListA");
            var typesB = document.getElementById("typeListB");
            var arrays = responseText.split("+>>>+");
            //alert(arrays[1]);
            list.innerHTML = arrays[0];
            typesA.innerHTML = arrays[1];
            typesB.innerHTML = arrays[2];
        },
        error: function( responseText ) {
            
            alert("Error");
        },
        data: formData,
        //Options to tell jQuery not to process data or worry about content-type.
        cache: false,
        contentType: false,
        processData: false
    });
});

function submitLinks () {
    //alert('tom');
    var sendData = new Array();
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    for (var i=0; i < listItem.length; i++) {
        var labelItem = listItem[i].getElementsByTagName("label");      
        if (labelItem[0].firstChild.checked) {
            var linksA = labelItem[0].lastChild.data.split("<-->");
            sendData[sendData.length] = linksA[0];
        }
    }
    
    //alert(sendData);
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "PreviewServlet",
        // the data to send (will be converted to a query string)
        data: {links:sendData},
        // the type of data we expect back
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            $('#connLabel').text(responseText);
            if(responseText === "Connection parameters not set")
                $('#dataLabel').text(responseText);
            else
                addMapData(responseText);
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

function loadLinks () 
{
    alert("luda");
    var list = document.getElementById("linksList");
    alert('listadasdasdasda');
    var listItem = list.getElementsByTagName("li");
    alert(listItem);
    for (var i=0; i < listItem.length; i++) {
        //alert(listItem[i].innerHTML);
        alert(i);
    }
    alert("LUDAS");
}

var mappings;
var schemasA = new Object;
var schemasB = new Object;

function schemaMatch () {
    //alert("Clicked");
    var list = document.getElementById("linksList");
    var listItem = list.getElementsByTagName("li");
    //var links = new Array(); 
    var links = ["adasdasd", "sadasdassd"];
    /*for (var i=0; i < listItem.length; i++) {
        var label = listItem[i].firstChild.getElementsByTagName("label");
        for (var j=0; j < label.length; j++) {
            var txt = label[j].innerHTML;
            var start = txt.indexOf(">");
            alert(txt.substring(start+1, txt.length));
            links[links.length] = txt.substring(start+1, txt.length);
        }
    }*/
    //alert("Clicked");
    //alert(JSON.stringify(links));
    //var send_links = $.serializeArray(links);
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "SchemaMatchServlet",
        // the data to send (will be converted to a query string)
        data: {"links":JSON.stringify(links)},
        // the type of data we expect back
        dataType : "json",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseJson ) {
            $('#connLabel').text(responseJson);
            mappins = responseJson;
            
            var modA = 1;
            var modB = 1;
            $.each(responseJson, function(index, element) {
                schemasA[index] = modA++;
                $.each(element, function(index, element) {
                    if (!schemasB[element]) {
                        schemasB[element] = modB++;
                        //alert(schemasB[element]);
                    }
                    //alert("Tom");
                    //alert(index+' '+element);
                });
            });
            //alert(schemasB);
            $.each(schemasA, function(index, element) {
                //alert("Tom 3");
                //alert(index+' '+element);
            });
            $.each(schemasB, function(index, element) {
                //alert("Tom 3");
                //alert(index+' '+element);
            });
            var schemaListA = document.getElementById("schemasA");
            $.each(schemasA, function(index, element) {
                var opt = document.createElement("li");
                opt.innerHTML = index;
                opt.onclick = propSelectedA;
                schemaListA.appendChild(opt);
            });
            var schemaListB = document.getElementById("schemasB");
            $.each(schemasB, function(index, element) {
                var opt = document.createElement("li");
                opt.innerHTML = index;
                opt.onclick = propSelectedB;
                schemaListB.appendChild(opt);
            });
            var node=document.createElement("li");
            var text = '<input class="match" type="text" name="lname" value="'+'http://www.opengis.net/ont/geosparql#asWKT'+'"/>';
            //alert(selectedProperties[lastSelectedFromA.innerHTML+'=>'+lastSelectedFromB.innerHTML]);
            node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
            /*var prev = 0;
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
            }*/
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

var lastSelectedFromA = null;
var lastSelectedFromB = null;
var backColor = null;
var backColorB = null;
var clickCountA = 0;
var clickCountB = 0;

function propSelectedA() {
        
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
    /*
    $.each(responseJson, function(index, element) {
        $.each(element, function(index, element) {
            if (!schemasB[element] !== 1) {
                schemasB[element] = modB++;
            }
        });
    });
           */ 
            
    lastSelectedFromA = this;
}

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
    /*var elems = mappins[this.innerHTML];
    //alert(elems);
    var list = document.getElementById("schemasA");
    var listItems = list.getElementsByTagName("li");
    $.each(listItems, function(index, element) {
        //element.style.backgroundColor = "white";
    });
    //alert("ID TYPE"+listItems);
    $.each(elems, function(index, element) {
        //listItems[index].style.backgroundColor = "red";
    });*/
            
            //alert("Selected from B");
    lastSelectedFromB = this;
}

function filterLinksA ( ) 
{               
    alert($('#typeListA').val());
    alert($('#typeListA').text());
    
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FilterServlet",
        // the data to send (will be converted to a query string)
        data: {"filter": $('#typeListA').val(), "dataset": "A"},
        // the type of data we expect back
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            $('#connLabel').text(responseText);
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

function filterLinksB ( ) 
{               
    alert($('#typeListB').val());
    alert($('#typeListB').text());
    
    $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "FilterServlet",
        // the data to send (will be converted to a query string)
        data: {"filter": $('#typeListB').val(), "dataset": "B"},
        // the type of data we expect back
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            $('#connLabel').text(responseText);
            var prev = 0;
            
            node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
            for ( var i = 0; i < responseText.length; i++ )
            {
                if(responseText.charAt(i) == ';') {
                    link = responseText.substring(prev, i);
                    var opt = document.createElement("option");
                    node.innerHTML = text;
            document.getElementById("matchList").appendChild(node);
                    prev = i + 1;
                }
            }
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

function setConnection () 
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
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            $('#connLabel').text(responseText);
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

function bazi(elem) {
    alert(elem.checked);
}

function setDatasets () 
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
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            //$('#dataLabel').text(responseText);
            $('#dataLabel').text("Datasets accepted");
            //if(responseText === "Connection parameters not set")
            //    $('#dataLabel').text(responseText);
            //else
            //    addMapData(responseText);
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
function loadXMLDoc()
{
    var xmlhttp;
    if (window.XMLHttpRequest)
    {// code for IE7+, Firefox, Chrome, Opera, Safari
        xmlhttp=new XMLHttpRequest();
    }
    else
    {// code for IE6, IE5
        xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
    
    xmlhttp.onreadystatechange=function()
  {
  if (xmlhttp.readyState==4 && xmlhttp.status==200)
    {
    document.getElementById("myDiv").innerHTML=xmlhttp.responseText;
    }
  }; 
  
    xmlhttp.open("GET","ConnectionServlet",true);
    xmlhttp.send();
}*/