<%@page language="java"%>
<%@page import="java.lang.*"%>
<%@page import="java.util.*"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<html>
    <head>
        <title>FAGI-gis</title>
        <meta name="description" content="website description" />
        <meta name="keywords" content="website keywords, website keywords" />
        <link href='http://fonts.googleapis.com/css?family=Bree+Serif' rel='stylesheet' type='text/css'>
        <!--
        <link href='http://fonts.googleapis.com/css?family=Indie+Flower' rel='stylesheet' type='text/css'>
        -->
        <link rel="stylesheet" href="js/codemirror/theme/lesser-dark.css">
        <link rel="stylesheet" href="js/codemirror/theme/base16-light.css">
        <link rel="stylesheet" href="js/codemirror/lib/codemirror.css">
        <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
        <link rel="stylesheet" href="//code.jquery.com/ui/1.11.3/themes/smoothness/jquery-ui.css">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap-theme.min.css">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css">
  <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
</head>
    
    <body>
        <%
            String str = request.getParameter("dataset-l");
            if ( str == null ) 
                request.setAttribute("dataset-l", "http://localhost:8890/DAV/links_postgis1");
                //request.setAttribute("dataset-l", "http://generator.geoknow.eu/resource/RdfImport_1441965089653");
            else
                request.setAttribute("dataset-l", request.getParameter("dataset-l"));
            
            str = request.getParameter("endpoint-l");
            if ( str == null ) 
                request.setAttribute("endpoint-l", "http://localhost:8890/sparql");
                //request.setAttribute("endpoint-l", "http://178.63.95.211:8080/generator/rest/session/be19b030-63f4-457a-b02c-32b2180aa59c");
                //request.setAttribute("endpoint-l", "http://fagi.guests.ipsyp.dom:8891/sparql");
            else
                request.setAttribute("endpoint-l", request.getParameter("endpoint-l"));
            
            
            str = request.getParameter("dataset-a");
            if ( str == null ) 
                //request.setAttribute("dataset-a", "http://localhost:8890/DAV/osm_demo");
                request.setAttribute("dataset-a", "http://generator.geoknow.eu/resource/RdfImport_1443084682160");
            else
                request.setAttribute("dataset-a", request.getParameter("dataset-a"));
            
            str = request.getParameter("dataset-b");
            if ( str == null ) 
                //request.setAttribute("dataset-b", "http://localhost:8890/DAV/wik_demo");
                request.setAttribute("dataset-b", "http://generator.geoknow.eu/resource/RdfImport_1443084718375");
            else
                request.setAttribute("dataset-b", request.getParameter("dataset-b"));
            
            str = request.getParameter("endpoint-a");
            if ( str == null ) 
                //request.setAttribute("endpoint-a", "http://localhost:8890/sparql");
                request.setAttribute("endpoint-a", "http://generator.geoknow.eu:8080/generator/rest/session/8d631684-e4cc-4bae-bea7-656c07f78663");
                //request.setAttribute("endpoint-a", "http://fagi.guests.ipsyp.dom:8891/sparql");
            else
                request.setAttribute("endpoint-a", request.getParameter("endpoint-a"));
            
            str = request.getParameter("endpoint-b");
            if ( str == null ) 
                //request.setAttribute("endpoint-b", "http://localhost:8890/sparql");
                request.setAttribute("endpoint-b", "http://generator.geoknow.eu:8080/generator/rest/session/8d631684-e4cc-4bae-bea7-656c07f78663");
                //request.setAttribute("endpoint-b", "http://fagi.guests.ipsyp.dom:8891/sparql");
            else
                request.setAttribute("endpoint-b", request.getParameter("endpoint-b"));
            
            str = request.getParameter("postgis-username");
            if ( str == null ) 
                request.setAttribute("postgis-username", "postgres");
            else
                request.setAttribute("postgis-username", request.getParameter("postgis-username"));
            
            str = request.getParameter("postgis-password");
            if ( str == null ) 
                request.setAttribute("postgis-password", "1111");
            else
                request.setAttribute("postgis-password", request.getParameter("postgis-password"));
            
            str = request.getParameter("postgis-database");
            if ( str == null ) 
                request.setAttribute("postgis-database", "postgis1");
            else
                request.setAttribute("postgis-database", request.getParameter("postgis-database"));
            
            str = request.getParameter("postgis-host");
            if ( str == null ) 
                request.setAttribute("postgis-host", "localhost");
            else
                request.setAttribute("postgis-host", request.getParameter("postgis-host"));
            
            str = request.getParameter("postgis-port");
            if ( str == null ) 
                request.setAttribute("postgis-port", "1111");
            else
                request.setAttribute("postgis-port", request.getParameter("postgis-port"));
            
            str = request.getParameter("target-endpoint");
            if ( str == null ) 
                request.setAttribute("target-endpoint", "http://localhost:8890/sparql");
            else
                request.setAttribute("target-endpoint", request.getParameter("target-endpoint"));
            
            str = request.getParameter("target-dataset");
            if ( str == null ) 
                request.setAttribute("target-dataset", "http://localhost:8890/fused_dataset");
            else
                request.setAttribute("target-dataset", request.getParameter("target-dataset"));
            
            /*
             $scope.service.serviceUrl +
					'?endpoint-a=' + encodeURIComponent($scope.fagi.endpointA == $scope.endpoint? authEndpoint : $scope.fagi.endpointA ) +
					'&endpoint-b=' + encodeURIComponent($scope.fagi.endpointB == $scope.endpoint? authEndpoint : $scope.fagi.endpointB ) +
					'&dataset-a='  + encodeURIComponent($scope.fagi.datasetA!=""? $scope.fagi.datasetA.replace(':',ConfigurationService.getUriBase()):"") +
					'&dataset-b='  + encodeURIComponent($scope.fagi.datasetB!=""? $scope.fagi.datasetB.replace(':',ConfigurationService.getUriBase()):"") +
					'&postgis-username='+ encodeURIComponent($scope.fagi.database.dbUser) +
					'&postgis-password='+ encodeURIComponent($scope.fagi.database.dbPassword) +
					'&postgis-database='+ encodeURIComponent($scope.fagi.database.dbName) +
					'&postgis-host='+ encodeURIComponent($scope.fagi.database.dbHost) +
					'&postgis-port='+ encodeURIComponent($scope.fagi.database.dbPort) +
					'&target-endpoint='+ encodeURIComponent(authEndpoint) +
					'&target-dataset='+ encodeURIComponent($scope.fagi.targetGraph.replace(':',ConfigurationService.getUriBase())) ;
            */
        %>
        
        <div id="fg-screen-dimmer"></div>
        <div id="fg-loading-spinner" class="loader">
            <svg xmlns=http://www.w3.org/2000/svg  viewBox="0 0 100 100" id=circle-middle>
                <circle fill=#EDEDED cx=50 cy=50 r="6" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
            <svg xmlns=http://www.w3.org/2000/svg viewBox="0 0 100 100">
                <circle fill=#26A6D1 cx=50 cy=50 r="4.5" />
            </svg>
        </div>
        
        <!--
        <div class="tooltip"id="fg-info-popup">
            <button id="close-info-menu-btn" type="button" class="btn btn-primary">X</button>
            <label id="fg-info-label" style="font-size: 22px;">Debug Output</label>
        </div>
        -->
        
        <div class="tooltip" id="popupFindLinkMenu">
            <button id="close-findlink-menu-btn" type="button" class="btn btn-primary">X</button>
            <table style="width:100%; color:white;">
                <tr>
                    <td>Select Radius (m) : </td>
                    <td><input id="radiusSpinner" name="value"></td> 
                </tr>
            </table>
            <input id="popupFindLinkButton" type="submit" value="Fetch Geometries" style="float:right; color: black;" onclick="return false;"/>
        </div>
               
        <div class="tooltip" id="popupBBoxMenu">
            <button id="close-bbox-menu-btn" type="button" class="btn btn-primary">X</button>
            <ul id="bboxMenu">
                <li id="transformBBoxButton">Transform BBox</li>
                <li id="fetchBBoxContainedButton">Fetch Contained</li>
                <li id="fetchBBoxSPARQLButton">Fetch With SPARQL</li>
                <li id="fetchBBoxFindButton">Fetch And Find Links</li>
            </ul>
        </div>

        <div class="tooltip" id="popupTransformMenu">
            <button id="close-transform-menu-btn" type="button" class="btn btn-primary">X</button>
            <ul id="transformMenu">
                <li id="moveButton">Move</li>
                <li id="rotateButton">Rotate</li>
                <li id="scaleButton">Scale</li>
                <li id="infoButton">Info</li>
                <li id="findLinkButton">Find Links</li>
                <li id="createLinkButton">Create Link</li>
            </ul>
        </div>

        <div class="tooltip" id="popupValidateMenu">
        <button id="close-validate-menu-btn" type="button" class="btn btn-primary">X</button>
            <ul id="validateMenu">
                <li id="valButton">Validate Link</li>
                <li id="valAllButton">Validate ALL Links</li>
            </ul>
        </div>
        
        <div id="fagi">
            <div id="nav3">
                <table style="width:100%; height:1%;">
                    <tr>

                        <td><a id="connectionMenu" href="#connection">connection</a></td>
                        <td><a id="datasetMenu" href="#datasets">datasets</a></td>
                        <td><a id="linksMenu" href="#links">links</a></td>
                        <td><a id="matchingMenu" href="#fusion">matching</a></td>
                        <td>
                            <div class="dropdown"><a id="dLabel"
                                                     data-target="#" 
                                                     href="http://example.com" 
                                                     data-toggle="dropdown" 
                                                     aria-haspopup="true" 
                                                     role="button">
                                    tools
                                    <span class="caret"></span></a>

                                <ul class="dropdown-menu" role="menu">
                                    <li><a id="visibleSelect" href="#">visible select</a></li>
                                    <li><a id="multipleTool" href="#">multiple select</a></li>
                                    <li><a id="bboxTool" href="#">bounding box select</a></li>
                                    <li><a id="fetchTool" href="#">fetch unlinked</a></li>
                                    <li><a id="clusteringTool" href="#">perform clustering</a></li>
                                    <li><a href="#">find similar</a></li>
                                    <li class="divider"></li>
                                    <li><a href="#">advanced search</a></li>
                                    <li><a href="#">sparql</a></li>
                                    <li class="divider"></li>
                                    <li><a href="#">reset fagi</a></li>
                                </ul>
                            </div>
                            </div></td>
                        <td><a href="#about">about</a></td>
                        <td>
                            <table style="vertical-align: middle; padding: 0; margin: 0;">
                                <tr>
                                    <td style="color: blue;">Batch</td>
                                    <td>
                                        <div style="top: -100px; vertical-align: middle;" id="bFusionToggle">
                                            <input id="batchOn" style="top: 2px; vertical-align:middle;" type="radio" name="batchOn" value="batchOn"><label style="top: 2px; vertical-align:middle;" id="batch-on-radio" for="batchOn">ON</label>
                                            <input id="batchOff" style="top: 2px; vertical-align:middle;" type="radio" name="batchOff" value="batchOff"><label style="top: 2px; vertical-align:middle;" id="batch-off-radio" for="batchOff">OFF</label>
                                        </div>
                                    </td> 
                                </tr>
                            </table>
                        </td>
                        <td><form method="get" action="/search" id="search">
                                <input name="q" type="text" size="40" placeholder="Search..." />
                            </form></td>
                    </tr>
                </table> </div>
            <div style="float:right; width:100%; height:96%;" id="map"></div>
        </div>

        <!--
        <div id="batch-offset-dialog" style="ui-dialog-titlebar" title="Create new user">
            <p>All form fields are required.</p>
            <form>
                <fieldset>
                    <label for="name">Name</label>
                    <input type="text" name="name" id="name" value="Jane Smith" class="text ui-widget-content ui-corner-all">
                    <label for="email">Email</label>
                    <input type="text" name="email" id="email" value="jane@smith.com" class="text ui-widget-content ui-corner-all">
                    <label for="password">Password</label>
                    <input type="password" name="password" id="password" value="xxxxxxx" class="text ui-widget-content ui-corner-all">

                    <input type="submit" tabindex="-1" style="position:absolute; top:-1000px">
                </fieldset>
            </form>
        </div>
        -->
        <div id="dialog" style="ui-dialog-titlebar" title="Connections">
            <div id="mainPanel">
                <div id="datasetPanel">
                    <form id="dataDiv" name="data_input">
                    <!-- Linux IMIS -->
                        Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="<% out.println(request.getAttribute("dataset-a"));%>" title="Named Graph for Dataset A"/>
                        <!-- Windows IMIS 
                        Dataset A: <input type="text" name="da_name" id="idDatasetA" class="centered" value="http://localhost:8890/DAV/osm_berlin'"/> -->
                        <!-- Mac OS X 
                        Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/DAV/osm" title="Named Graph for Dataset A"/> -->
                        <datalist id="datalist1"></datalist>
                        SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="<% out.println(request.getAttribute("endpoint-a"));%>" title="SPARQL Endpoint for Dataset A."/> 
                        <!-- Linux IMIS -->
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="<% out.println(request.getAttribute("dataset-b"));%>" title="Named Graph for Dataset B"/>
                        <!-- Windows IMIS 
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost/DAV/wik"/ title="We ask for your age only for statistical purposes."> -->
                        <!-- Mac OS X 
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/DAV/wik"/> -->
                        SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="<% out.println(request.getAttribute("endpoint-b"));%>" title="SPARQL Endpoint for Dataset A."/>
                        Target Graph: <input type="text" name="t_graph" class="centered" value="<% out.println(request.getAttribute("target-dataset"));%>" title="Name of the target Dataset"/>
                        <table>
                            <tbody>
                                <tr>
                                    <td style="padding-right: 20px;" align="left" valign="bottom">Preview geometries from target dataset:</td>
                                    <td style="padding-right: 20px;"><div><input id="fg-fetch-fused-check" name="t" value="true" type="checkbox" checked="false"/></div></td>
                                </tr>
                            </tbody>
                        </table>
                        SPARQL Endpoint Target: <input type="text" name="t_end" class="centered" value="<% out.println(request.getAttribute("target-endpoint"));%>" title="SPARQL Endpoint of the target dataset."/>
                        Links Graph: <input id="fg-links-graph" type="text" name="l_graph" class="centered" value="<% out.println(request.getAttribute("dataset-l"));%>" title="Name of the links Datase.t(Leave empty if provided through file)"/>
                        SPARQL Endpoint Links <input id="fg-links-endpoint" type="text" name="l_end" class="centered" value="<% out.println(request.getAttribute("endpoint-l"));%>" title="SPARQL Endpoint of the links dataset.(Leave empty if provided through file)"/>
                        <!--Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/fagi/Desktop/"/>-->
                        <!-- Linux IMIS 
                        Bulk Insert Dir: <input type="text" name="bulk" class="centered" value="/home/nick/Projects/FAGI-gis-master/"/> -->
                        <!-- Mac OS X 
                        Bulk Insert Dir: <input type="text" name="bulk" class="centered" value="/Users/nickvitsas/Downloads"/> -->
                        <!-- Windows IMIS 
                        Bulk Insert Dir: <input type="text" name="bulk" class="centered" value="C:\Users\nick\Downloads\virtuoso-opensource\database"/> -->
                        <table>
                            <tbody>
                                <tr>
                                    <td style="vertical-align: middle; text-align: center; padding-right: 20px;" align="left" valign="bottom">Dominant Dataset:</td>
                                    <td style="vertical-align: middle;; text-align: center; padding-right: 20px;"><label><input id="domA" name="d_dom" value="true" type="checkbox" checked="true"/>A</label></td>
                                    <td style="vertical-align: middle; text-align: center; padding-right: 20px;"><label><input id="domB" type="checkbox" />B</label></td>                                  
                                </tr>
                            </tbody>
                        </table>

                        <label id="dataLabel" for="male">No dataset selected</label><input id="dataButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>

                    </form>
                </div>
                <div id="previewPanel">
                    <table id="previewTable" class="rwd-table" style="width:100%; color:white;">
                        <tr>
                            <td>Subject</td>
                            <td>Predictae></td> 
                            <td>Object</td> 
                        </tr>
                    </table>
                </div>
                <div id="connectionPanel">
                    <form id="connDiv" name="conn_input"><p />
                        Virtuoso URL: <input type="text" name="v_url" class="centered" value="<% out.println(request.getAttribute("postgis-host")+":"+request.getAttribute("postgis-port"));%>"/>
                        Virtuoso Username: <input type="text" name="v_name" class="centered" value="dba"/>
                        Virtuoso Password: <input type="password" name="v_pass" class="centered" value="dba"/>
                        <!-- Linux IMIS 
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/> -->
                        <!-- Windows IMIS 
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/> -->
                        <!-- Mac OS X -->
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="<% out.println(request.getAttribute("postgis-username"));%>"/> 
                        PostGIS Database <input type="text" name="p_data" class="centered" value="<% out.println(request.getAttribute("postgis-database"));%>"/>
                        PostGIS Password: <input type="password" name="p_pass" class="centered" value="<% out.println(request.getAttribute("postgis-password"));%>" title="Password for PostGIS instance"/>
                        <label id="connLabel" for="male">Connection not established</label><input id="connButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
                    </form>
                </div>
                <div id="linksPanel">
                    <table id="filterTable">
                        <tbody>
                            <!-- <form action="/LinksServlet" method="post" enctype="multipart/form-data"> -->
                        <form id="linksDiv" enctype="multipart/form-data">
                            <tr>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">Links file:</td>
                                <td style="width:216; text-align: center;" align="left" valign="bottom"> <input name="file" type="file" /></td>
                                <td>
                                </td>
                                <td style="width:216;  text-align: center;" align="left" valign="bottom"><input id="buttonL" type="button" value="Upload" /></td>
                            </tr></form>
                        <form id="filterLinksADiv" style="float:right" >
                            <tr>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">Filtering Options A:</td>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">
                                    <select multiple="multiple" id="typeListA" style="width: 100%">
                                    </select></td>
                                <td>
                                </td>
                                <td style="width:100; text-align: center;" align="left" valign="bottom"><input style="width: 100%" id="buttonFilterLinksA" type="button" value="Filter" /></td>
                            </tr></form>
                        <form id="filterLinksBDiv" style="float:right" >
                            <tr>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">Filtering Options B:</td>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">
                                    <select multiple="multiple" id="typeListB" style="width: 100%">
                                    </select>
                                </td>
                                <td>
                                </td>
                                <td style="width:100; text-align: center;" align="left" valign="bottom"><input style="width: 100%" id="buttonFilterLinksB" type="button" value="Filter" /></td>
                            </tr>
                        </form>
                        <tr>

                        </tr>
                        </tbody>
                    </table>  
                    <ul id="linksList" class="righted">
                    </ul>
                    <input id="linksButton" type="submit" value="Submit" style="float:right" onclick="return true;"/>
                    <input id="allLinksButton" type="submit" value="Select All" style="float:right" onclick="return true;"/>
                    <table style="width: 100%;">
                        <tr style="width: 100%;">
                            <td style="width:50%;">SPARQL Query for Dataset A</td>
                            <td style="width:50%;">SPARQL Query for Dataset B</td>
                        </tr>
                        <tr style="width: 100%;">
                            <td style="width:50%;">
                                <div id="fg-links-sparql-editor-a" style="height:300px; margin: auto; overflow-x: scroll;">
                                </div>
                            </td>
                            <td style="width:50%;">
                                <div id="fg-links-sparql-editor-b" style="height:300px; margin: auto; overflow-x: scroll;">
                                </div>
                            </td>
                        </tr>
                    </table>
                    <input id="fg-links-queries-submit" type="submit" value="Submit Query" style="float:right" onclick="return true;"/>
                </div>
                <div id="fusionPanel">
                    <table class="complex_selector">
                        <tbody><tr>
                                <td id="linkNameA" style="width:30%; text-align: center;" align="left" valign="bottom"><p style="word-break: keep-all;"> Dataset A</p></td>
                                <td id="linkNameB" style="width:30%; text-align: center;" align="left" valign="bottom">Dataset B</td>
                                <td>
                                </td>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">Selected</td>
                            </tr>
                            <tr>
                                <td style="width:30%;">
                                    <div class="horscroll">
                                        <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                        <ul id="linkSchemasA" class="schemaList"></ul>
                                    </div>
                                </td>
                                <td style="width:30%">
                                    <div class="horscroll">
                                        <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                        <ul id="linkSchemasB" class="schemaList"></ul>
                                    </div>                                    </td>
                                <td>
                                    <table align="center" border="0" width="100%"> 
                                        <tbody><tr>
                                                <td class="complex_selector_buttons">
                                                    <input id="addLinkSchema" name="b_add_role" value=">>" tabindex="13" type="submit"></input>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td align="center">
                                                    <input id="removeLinkSchema" name="b_rem_role" value="&lt;&lt;" tabindex="14" type="submit"></input>
                                                </td>
                                            </tr>
                                        </tbody></table> 
                                </td>
                                <td style="width:30%">
                                    <ul id="linkMatchList" class="righted">
                                    </ul>
                                </td>
                            </tr>
                        </tbody></table>
                    <!-- <input id="linkSchemaButton" type="submit" value="Add Link Matched Properties" style="float:right" onclick="return true;"/> -->
                    <table border="1">
                                  <tbody>
                                  <tr>
                                    <td style="width:26; background-color: blue; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendLinkSetA" style="text-align: center;" align="left" valign="bottom">Dataset A Layer</td>
                                    <td style="width:26; background-color: green; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendLinkSetB" style="text-align: center;" align="left" valign="bottom">Dataset B Layer</td>
                                    <td style="width:26; background-color: red; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetL" style="text-align: center;" align="left" valign="bottom">Link Layer</td>
                                  </tr></form>
                                  </tbody>
        </table>
                    <div class="table-responsive" id="linkFusionTable">
                    </div>
                    Class Recommendations<br />
                    <select multiple="multiple" id="classRecommendation" style="width: 50%">
                    </select>
                </div>
                <div id="matchingPanel">
                    <table class="complex_selector table-responsive">
                        <p>
                            <label for="spinner">Select scoring threshold:</label>
                            <input id="spinner" name="value">
                        </p>
                        
                        <tbody><tr>
                                <td id="datasetNameA" align="left" valign="bottom">Dataset A</td>
                                <td id="datasetNameB" align="left" valign="bottom">Dataset B</td>
                                <td>
                                </td>
                                <td align="left" valign="bottom">Selected</td>
                            </tr>
                            <tr>
                                <td>
                                    <div class="horscroll">
                                        <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                        <ul id="schemasA" class="schemaList"></ul>
                                    </div>
                                </td>
                                <td>
                                    <div class="horscroll">
                                        <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                        <ul id="schemasB" class="schemaList"></ul>
                                    </div>                                    </td>
                                <td>
                                    <table align="center" border="0" width="100%"> 
                                        <tbody><tr>
                                                <td class="complex_selector_buttons">
                                                    <input id="addSchema" name="b_add_role" value=">>" tabindex="13" type="submit"></input>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td align="center">
                                                    <input id="removeSchema" name="b_rem_role" value="&lt;&lt;" tabindex="14" type="submit"></input>
                                                </td>
                                            </tr>
                                        </tbody></table> 
                                </td>
                                <td>
                                    <ul id="matchList" class="righted">
                                    </ul>
                                </td>
                            </tr>
                        </tbody></table>
                    <table border="1">
                                  <tbody>
                                  <tr>
                                    <td style="width:26; background-color: blue; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetA" style="text-align: center;" align="left" valign="bottom">Dataset A Layer</td>
                                    <td style="width:26; background-color: green; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetB" style="text-align: center;" align="left" valign="bottom">Dataset B Layer</td>
                                    <td style="width:26; background-color: red; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetL" style="text-align: center;" align="left" valign="bottom">Link Layer</td>
                                  </tr></form>
                                  </tbody>
        </table>
                    <input id="finalButton" type="submit" value="Preview" style="float:right" onclick="return true;"/>
                    
                    <div id="bFusionOptions">
                        <div class="table-responsive" id="batchFusionTable">
                        </div>
                        <form action="#">
                            <fieldset>
                                <label for="speed">Perform batch fusion on : </label>
                                <select name="speed" id="clusterSelector">
                                    <option value="-1" selected="selected">All links</option>
                                </select>
                            </fieldset>
                        </form>
                        <!--
                        <div id="global-offsets">
                            <form action="#">
                                <fieldset>
                                    <label for="x-offset">X</label>
                                    <input type="text" name="x-offset" id="x-offset" value="0.0" class="text ui-widget-content ui-corner-all">
                                    <label for="y-offset">Y</label>
                                    <input type="text" name="y-offset" id="y-offset" value="0.0" class="text ui-widget-content ui-corner-all">
                                </fieldset>
                            </form>
                        </div>
                        -->
                    </div>
                </div>
                <div id="clusteringPanel">
                    <table style="width:100%">
                        <tr>
                            <td>Connecting Vector Length: </td>
                            <td><div id="connVecLenCheck">
                                    <input type="radio" name="vecLen" id="check1"><label for="check1">YES</label>
                                    <input type="radio" name="vecLen" id="check2"><label for="check2">NO</label>
                                </div></td> 
                        </tr>
                        <tr>
                            <td>Connecting Vector Direction:</td>
                            <td><div style="float: right;" id="connVecDirCheck">
                                    <input type="radio" name="VecDir" id="check3"><label for="check3">YES</label>
                                    <input type="radio" name="VecDir"  id="check4"><label for="check4">NO</label>
                                </div></td> 
                        </tr>
                        <tr>
                            <td>Polygon Coverage:</td>
                            <td><div style="float: right;" id="connCoverageCheck">
                                    <input type="radio" name="Coverage" id="check5"><label for="check5">YES</label>
                                    <input type="radio" name="Coverage"  id="check6"><label for="check6">NO</label>
                                </div></td> 
                        </tr>
                    </table>
                    <label for="clusterCount">Cluster count : (0 means calculate best)</label>
                    <input type="text" id="clusterCount" readonly style="border:0; color:#f6931f; font-weight:bold;">
</p>
                    Number of CLusters: <div id="slider"></div>
                    <input id="clusterButton" type="submit" value="Perform Clustering" style="float:right" onclick="return false;"/>
                </div>
                <div id="fg-fetch-sparql-panel">
                    <table style="width: 100%;">
                        <tr style="width: 100%;">
                            <td style="width:50%;">SPARQL Query for Dataset A</td>
                            <td style="width:50%;">SPARQL Query for Dataset B</td>
                        </tr>
                        <tr style="width: 100%;">
                            <td style="width:50%;">
                                <div id="fg-fetch-sparql-editor-a" style="height:300px; margin: auto; overflow-x: scroll;">
                                </div>
                            </td>
                            <td style="width:50%;">
                                <div id="fg-fetch-sparql-editor-b" style="height:300px; margin: auto; overflow-x: scroll;">
                                </div>
                            </td>
                        </tr>
                    </table>
                    <input id="fg-fetch-queries-submit" type="submit" value="Submit Query" style="float:right" onclick="return true;"/>
                </div>
            </div>
        </div>
        <!--sript src="//code.jquery.com/jquery-1.10.2.js"></script>-->
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js"></script>
        <script src="https://code.jquery.com/ui/1.11.3/jquery-ui.js"></script>
        <script type="text/javascript" src="js/jquery.autocomplete.min.js"></script>
        <script type="text/javascript" src="js/jquery.easing-sooper.js"></script>
        <script type="text/javascript" src="js/jquery.sooperfish.js"></script>
        <script type="text/javascript" src="js/modernizr-1.5.min.js"></script>
        <script type="text/javascript" src="js/json2.js"></script>
        <script type="text/javascript" src="js/proj4js-combined.min.js"></script>
        <script type="text/javascript" src="js/EPSG27563.min.js"></script>
        <script src="http://openlayers.org/api/OpenLayers.js"></script> 
        <script type="text/javascript" src="js/codemirror/lib/codemirror.js"></script>
        <script type="text/javascript" src="js/codemirror/mode/sparql/sparql.js" type="text/javascript" charset="utf-8"></script>
        <script type="text/javascript" src="js/codemirror/addon/edit/matchbrackets.js"></script>
        <script type="text/javascript" src="js/mappreview2.js"></script>
        <script src="http://maps.google.com/maps/api/js?v=3&amp;sensor=false"></script>
        <script type="text/javascript" src="js/fusion2.js"></script>
        <script type="text/javascript" src="js/autocomplete.js"></script>

        <script type="text/javascript">
                    $(document).ready(function () {
                        $('ul.sf-menu').sooperfish();
                    });
        </script>

        <script type="text/javascript" src="js/droppanel.js"></script>
    </body>

</html>
