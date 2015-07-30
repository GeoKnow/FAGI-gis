/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
var a = $('#ider').autocomplete({
	serviceUrl:'SuggestServlet',
	minChars:2,
	maxHeight:400,
	width:300,
	zIndex: 9999,
	deferRequestBy: 0, //miliseconds
	params: { country:'Yes' }, //aditional parameters
	noCache: false, //default is false, set to true to disable caching
	// callback function:
	onSelect: function(value, data){ alert('You selected: ' + value + ', ' + data); }
});*/

var pendingSuggestions = false;
$('#ider').on('input', function(){
   //alert($(this).val());
   //document.getElementById("datalist1").innerHtml = '<option value="Chinasdfdsfdssdf"></option>';
   if(pendingSuggestions)
       return;
   pendingSuggestions = true;
   
   /*var values = $('#dataDiv').serialize();
   $.ajax({
        // request type
        type: "POST",
        // the URL for the request
        url: "SuggestServlet",
        // the data to send (will be converted to a query string)
        data: { query : $(this).val() },
        // the type of data we expect back
        dataType : "text",
        // code to run if the request succeeds;
        // the response is passed to the function
        success: function( responseText ) {
            //$('#dataLabel').text(responseText);
            //addMapData(responseText);
            //alert(responseText);
            var dataList = $("#datalist1");
            dataList.empty();
            prev = 0;
            for ( var i = 0; i < responseText.length; i++ )
    {
        if(responseText.charAt(i) == ';') {
                first = responseText.substring(prev, i);
                prev = i + 1;
                var opt = $("<option></option>").attr("value", first);
                dataList.append(opt);
        }
    }
           
            //if(res.DATA.length) {
            //for(var i=0, len=res.DATA.length; i<len; i++) {
            
            //}
            pendingSuggestions = false;
//}
        },
        // code to run if the request fails; the raw request and
        // status codes are passed to the function
        error: function( xhr, status, errorThrown ) {
            //alert( "Sorry, there was a problem!" );
            console.log( "Error: " + errorThrown );
            console.log( "Status: " + status );
            console.dir( xhr );
        },
        // code to run regardless of success or failure
        complete: function( xhr, status ) {
            //$('#connLabel').text("connected");
        }
    });*/
});
