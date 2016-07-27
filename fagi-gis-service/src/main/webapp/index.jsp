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
                                            if (str == null) {
                                                request.setAttribute("dataset-l", "");
                                                //request.setAttribute("dataset-l", "http://localhost:8890/DAV/test_links");
                                            } //request.setAttribute("dataset-l", "http://localhost:8890/DAV/links_postgis1");
                                            //request.setAttribute("dataset-l", "http://generator.geoknow.eu/resource/RdfImport_1441965089653");
                                            else {
                                                request.setAttribute("dataset-l", request.getParameter("dataset-l"));
                                            }

                                            str = request.getParameter("endpoint-l");
                                            if (str == null) {
                                                request.setAttribute("endpoint-l", "");
                                                //request.setAttribute("endpoint-l", "http://localhost:8890/sparql");
                                            } //request.setAttribute("endpoint-l", "http://localhost:8890/sparql");
                                            //request.setAttribute("endpoint-l", "http://178.63.95.211:8080/generator/rest/session/be19b030-63f4-457a-b02c-32b2180aa59c");
                                            //request.setAttribute("endpoint-l", "http://fagi.guests.ipsyp.dom:8891/sparql");
                                            else {
                                                request.setAttribute("endpoint-l", request.getParameter("endpoint-l"));
                                            }

                                            str = request.getParameter("dataset-a");
                                            if (str == null) {
                                                request.setAttribute("dataset-a", "http://localhost:8890/DAV/osm_demo");
                                                //request.setAttribute("dataset-a", "http://localhost:8890/osm_sample");
                                            } //request.setAttribute("dataset-a", "http://generator.geoknow.eu/resource/RdfImport_1443084682160");
                                            else {
                                                request.setAttribute("dataset-a", request.getParameter("dataset-a"));
                                            }

                                            str = request.getParameter("dataset-b");
                                            if (str == null) {
                                                request.setAttribute("dataset-b", "http://localhost:8890/DAV/wik_demo");
                                                //request.setAttribute("dataset-b", "http://localhost:8890/wikimapia_sample");
                                            } //request.setAttribute("dataset-b", "http://generator.geoknow.eu/resource/RdfImport_1443084718375");
                                            else {
                                                request.setAttribute("dataset-b", request.getParameter("dataset-b"));
                                            }

                                            str = request.getParameter("endpoint-a");
                                            if (str == null) {
                                                request.setAttribute("endpoint-a", "http://localhost:8890/sparql");
                                            } //request.setAttribute("endpoint-a", "http://generator.geoknow.eu:8080/generator/rest/session/8d631684-e4cc-4bae-bea7-656c07f78663");
                                            //request.setAttribute("endpoint-a", "http://fagi.guests.ipsyp.dom:8891/sparql");
                                            else {
                                                request.setAttribute("endpoint-a", request.getParameter("endpoint-a"));
                                            }

                                            str = request.getParameter("endpoint-b");
                                            if (str == null) {
                                                request.setAttribute("endpoint-b", "http://localhost:8890/sparql");
                                            } //request.setAttribute("endpoint-b", "http://generator.geoknow.eu:8080/generator/rest/session/8d631684-e4cc-4bae-bea7-656c07f78663");
                                            //request.setAttribute("endpoint-b", "http://fagi.guests.ipsyp.dom:8891/sparql");
                                            else {
                                                request.setAttribute("endpoint-b", request.getParameter("endpoint-b"));
                                            }

                                            str = request.getParameter("postgis-username");
                                            if (str == null) //request.setAttribute("postgis-username", "nickvitsas");
                                            {
                                                request.setAttribute("postgis-username", "postgres");
                                            } //request.setAttribute("postgis-username", "fagi");
                                            else {
                                                request.setAttribute("postgis-username", request.getParameter("postgis-username"));
                                            }

                                            str = request.getParameter("postgis-password");
                                            if (str == null) {
                                                request.setAttribute("postgis-password", "1111");
                                            } //request.setAttribute("postgis-password", "fagi");
                                            else {
                                                request.setAttribute("postgis-password", request.getParameter("postgis-password"));
                                            }

                                            str = request.getParameter("postgis-database");
                                            if (str == null) {
                                                request.setAttribute("postgis-database", "fagi");
                                                //request.setAttribute("postgis-database", "postgis1");
                                            } //request.setAttribute("postgis-database", "fagi");
                                            else {
                                                request.setAttribute("postgis-database", request.getParameter("postgis-database"));
                                            }

                                            str = request.getParameter("postgis-host");
                                            if (str == null) {
                                                request.setAttribute("postgis-host", "localhost");
                                            } else {
                                                request.setAttribute("postgis-host", request.getParameter("postgis-host"));
                                            }

                                            str = request.getParameter("postgis-port");
                                            if (str == null) {
                                                request.setAttribute("postgis-port", "1111");
                                            } else {
                                                request.setAttribute("postgis-port", request.getParameter("postgis-port"));
                                            }

                                            str = request.getParameter("target-endpoint");
                                            if (str == null) {
                                                request.setAttribute("target-endpoint", "http://localhost:8890/sparql");
                                            } else {
                                                request.setAttribute("target-endpoint", request.getParameter("target-endpoint"));
                                            }

                                            str = request.getParameter("target-dataset");
                                            if (str == null) {
                                                request.setAttribute("target-dataset", "http://localhost:8890/fused_dataset");
                                            } else {
                                                request.setAttribute("target-dataset", request.getParameter("target-dataset"));
                                            }

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

                                        <div class="tooltip" id="fg-popup-batch-find-link-menu">
                                            <button id="fg-close-batch-findlink-menu-btn" type="button" class="btn btn-primary">X</button>
                                            <table style="width:100%; color:white;">
                                                <tr>
                                                    <td>Select Radius (m) : </td>
                                                    <td><input id="fg-batch-radius-spinner" name="value"></td> 
                                                </tr>
                                            </table>
                                            <input id="fg-popup-find-link-button" type="submit" value="Fetch Geometries" style="float:right; color: black;" onclick="return false;"/>
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

                                                        <td id="connectionCell"><a id="connectionMenu" href="#connection">connection</a></td>
                                                        <td id="userCell"><a id="userMenu" href="#user">user</a></td>
                                                        <td><a id="datasetMenu" href="#datasets">datasets</a></td>
                                                        <td><a id="linksMenu" href="#links">links</a></td>
                                                        <td><a id="matchingMenu" href="#fusion">matching</a></td>
                                                        <td>
                                                            <div class="dropdown"><a id="dLabel"
                                                                                     href="javascript:;" 
                                                                                     aria-haspopup="true" 
                                                                                     role="button">
                                                                    tools
                                                                    <span class="caret"></span></a>

                                                                <ul class="dropdown-menu" role="menu">
                                                                    <!--<li><a id="visibleSelect" href="#">visible select</a></li>-->
                                                                    <li><a id="multipleTool" href="#">multiple select</a></li>
                                                                    <li><a id="bboxTool" href="#">bounding box select</a></li>
                                                                    <li><a id="fetchTool" href="#">fetch unlinked</a></li>
                                                                    <li><a id="clusteringTool" href="#">perform clustering</a></li>
                                                                    <li class="divider"></li>
                                                                    <li><a id="fg-download-fused-tool" href="javascript:;">download fused dataset</a></li>
                                                                    <!--<li><a href="#">advanced search</a></li>
                                                                    <li><a href="#">sparql</a></li>-->
                                                                    <li class="divider"></li>
                                                                    <li><a href="#">reset fagi</a></li>
                                                                </ul>
                                                            </div>
                                                            </div></td>
                                                        <td>
                                                            <a href="#about">about</a>
                                                        </td>
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
                                                                                    <td>
                                                                                        <form method="get" action="/search" id="search">
                                                                                            <input name="q" type="text" size="40" placeholder="Search..." />
                                                                                        </form>
                                                                                    </td>
                                                                                    </tr>
                                                                                    </table>
                                                                                    </div>
                                                                                    <div id="fagi" class="split split-horizontal" style="width: 100%; height:95%;">
                                                                                        <div class="split content" style="overflow: auto; height:100%; margin: 0px;" id="mainPanel">
                                                                                            <span id='fg-close-panel' onclick='return false;'>x</span>
                                                                                            <br />
                                                                                            <div id="previewPanel">
                                                                                                <table id="previewTable" class="rwd-table" style="width:100%; color:white;">
                                                                                                    <tr>
                                                                                                        <td>Subject</td>
                                                                                                        <td>Predictae></td> 
                                                                                                        <td>Object</td> 
                                                                                                    </tr>
                                                                                                </table>
                                                                                            </div>
                                                                                            <div id="fg-user-selection-panel">
                                                                                                Selected Links:
                                                                                                <ul id="fg-user-selection-list">
                                                                                                </ul>
                                                                                            </div>
                                                                                            <div id="fg-user-panel">
                                                                                                <form id="fg-user-div" name="user_input"><p />
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-user-mail">E-Mail:</label>
                                                                                                        <input id="fg-user-mail" type="text" name="u_mail" value="" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-user-name">Username:</label>
                                                                                                        <input id="fg-user-name" type="text" name="u_name" value="" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-user-pass">Password:</label>
                                                                                                        <input id="fg-user-pass" type="password" name="u_pass" value="" class="form-control">
                                                                                                    </div>
                                                                                                    <label id="fg-user-label" for="male">User not logged</label>
                                                                                                    <input id="fg-user-create-btn" type="submit" value="Create" style="float:right" onclick="return false;"/>
                                                                                                    <input id="fg-user-login-btn" type="submit" value="Login" style="float:right" onclick="return false;"/>
                                                                                                </form>
                                                                                            </div>
                                                                                            <div id="datasetPanel">
                                                                                                <form id="dataDiv" name="data_input">
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-dataset-input-a">Dataset A:</label>
                                                                                                        <input id="fg-dataset-input-a" type="text" name="da_name" value="<% out.println(request.getAttribute("dataset-a"));%>" title="Named Graph for Dataset A" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-endpoint-input-a">SPARQL Endpoint A:</label>
                                                                                                        <input id="fg-endpoint-input-a" type="text" name="da_end" value="<% out.println(request.getAttribute("endpoint-a"));%>" title="Named Graph for Dataset A" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="panel-group">
                                                                                                        <div class="panel panel-default">
                                                                                                            <div class="panel-heading">
                                                                                                                <h1 class="panel-title">
                                                                                                                    <a data-toggle="collapse" href="#fg-auth-dropdown-a" style="color: black; font-size: 10;">Authenticate</a>
                                                                                                                </h1>
                                                                                                            </div>
                                                                                                            <div id="fg-auth-dropdown-a" class="panel-collapse collapse">
                                                                                                                <div class="panel-body">
                                                                                                                    <table style="table-layout: fixed; width: 100%;">
                                                                                                                        <tr>
                                                                                                                            <td >User</td>
                                                                                                                            <td><input id="fg-auth-user-a" type="text" value=""/></td>
                                                                                                                            <td >Pass</td>
                                                                                                                            <td><input id="fg-auth-pass-a" type="text" value=""/></td>
                                                                                                                        </tr>
                                                                                                                    </table>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-dataset-input-b">Dataset B:</label>
                                                                                                        <input id="fg-dataset-input-b" type="text" name="db_name" value="<% out.println(request.getAttribute("dataset-b"));%>" title="Named Graph for Dataset A" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-endpoint-input-b">SPARQL Endpoint A:</label>
                                                                                                        <input id="fg-endpoint-input-b" type="text" name="db_end" value="<% out.println(request.getAttribute("endpoint-b"));%>" title="Named Graph for Dataset A" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="panel-group">
                                                                                                        <div class="panel panel-default">
                                                                                                            <div class="panel-heading">
                                                                                                                <h1 class="panel-title">
                                                                                                                    <a data-toggle="collapse" href="#fg-auth-dropdown-b" style="color: black; font-size: 10;">Authenticate</a>
                                                                                                                </h1>
                                                                                                            </div>
                                                                                                            <div id="fg-auth-dropdown-b" class="panel-collapse collapse">
                                                                                                                <div class="panel-body">
                                                                                                                    <table style="table-layout: fixed; width: 100%;">
                                                                                                                        <tr>
                                                                                                                            <td >User</td>
                                                                                                                            <td><input id="fg-auth-user-b" type="text" value=""/></td>
                                                                                                                            <td >Pass</td>
                                                                                                                            <td><input id="fg-auth-pass-b" type="text" value=""/></td>
                                                                                                                        </tr>
                                                                                                                    </table>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-dataset-input-t">Target Graph:</label>
                                                                                                        <input id="fg-dataset-input-t" type="text" name="t_graph" value="<% out.println(request.getAttribute("target-dataset"));%>" title="Named Graph for the Target Dataset" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-endpoint-input-t">SPARQL Endpoint Target:</label>
                                                                                                        <input id="fg-endpoint-input-t" type="text" name="t_end" value="<% out.println(request.getAttribute("target-endpoint"));%>" title="Endpoint if the target dataset" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="panel-group">
                                                                                                        <div class="panel panel-default">
                                                                                                            <div class="panel-heading">
                                                                                                                <h1 class="panel-title">
                                                                                                                    <a data-toggle="collapse" href="#fg-auth-dropdown-t" style="color: black; font-size: 10;">Authenticate</a>
                                                                                                                </h1>
                                                                                                            </div>
                                                                                                            <div id="fg-auth-dropdown-t" class="panel-collapse collapse">
                                                                                                                <div class="panel-body">
                                                                                                                    <table style="table-layout: fixed; width: 100%;">
                                                                                                                        <tr>
                                                                                                                            <td >User</td>
                                                                                                                            <td><input id="fg-auth-user-t" type="text" value=""/></td>
                                                                                                                            <td >Pass</td>
                                                                                                                            <td><input id="fg-auth-pass-t" type="text" value=""/></td>
                                                                                                                        </tr>
                                                                                                                    </table>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                    <table>
                                                                                                        <tbody>
                                                                                                            <tr>
                                                                                                                <td style="padding-right: 20px;" align="left" valign="bottom">Preview geometries from target dataset:</td>
                                                                                                                <td style="padding-right: 20px;"><div><input id="fg-fetch-fused-check" name="t" value="false" type="checkbox" checked="false"/></div></td>
                                                                                                            </tr>
                                                                                                        </tbody>
                                                                                                    </table>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-dataset-input-l">Links Graph:</label>
                                                                                                        <input id="fg-dataset-input-l" type="text" name="l_graph" value="<% out.println(request.getAttribute("dataset-l"));%>" title="Named Graph for the Target Dataset" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-endpoint-input-l">SPARQL Endpoint Links:</label>
                                                                                                        <input id="fg-endpoint-input-l" type="text" name="l_end" value="<% out.println(request.getAttribute("endpoint-l"));%>" title="Endpoint if the target dataset" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="panel-group">
                                                                                                        <div class="panel panel-default">
                                                                                                            <div class="panel-heading">
                                                                                                                <h1 class="panel-title">
                                                                                                                    <a data-toggle="collapse" href="#fg-auth-dropdown-l" style="color: black; font-size: 10;">Authenticate</a>
                                                                                                                </h1>
                                                                                                            </div>
                                                                                                            <div id="fg-auth-dropdown-l" class="panel-collapse collapse">
                                                                                                                <div class="panel-body">
                                                                                                                    <table style="table-layout: fixed; width: 100%;">
                                                                                                                        <tr>
                                                                                                                            <td >User</td>
                                                                                                                            <td><input id="fg-auth-user-l" type="text" value=""/></td>
                                                                                                                            <td >Pass</td>
                                                                                                                            <td><input id="fg-auth-pass-l" type="text" value=""/></td>
                                                                                                                        </tr>
                                                                                                                    </table>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                    <table>
                                                                                                        <tbody>
                                                                                                            <tr>
                                                                                                                <td style="vertical-align: middle; text-align: center; padding-right: 20px;" align="left" valign="bottom">Dominant Dataset:</td>
                                                                                                                <td style="vertical-align: middle;; text-align: center; padding-right: 20px;"><label><input id="domA" name="d_dom" value="true" type="checkbox" checked="true"/>A</label></td>
                                                                                                                <td style="vertical-align: middle; text-align: center; padding-right: 20px;"><label><input id="domB" type="checkbox" />B</label></td>                                  
                                                                                                            </tr>
                                                                                                        </tbody>
                                                                                                    </table>

                                                                                                    <label id="fg-dataset-label" for="male">No dataset selected</label><input id="dataButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>

                                                                                                </form>
                                                                                            </div>
                                                                                            <div id="connectionPanel">
                                                                                                <form id="connDiv" name="conn_input"><p />
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-virt-url">Virtuoso URL:</label>
                                                                                                        <input id="fg-virt-url" ype="text" name="v_url" value="<% out.println(request.getAttribute("postgis-host") + ":" + request.getAttribute("postgis-port"));%>" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-virt-name">Virtuoso Username:</label>
                                                                                                        <input id="fg-virt-name" type="text" name="v_name" value="dba" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-virt-pass">Virtuoso Password:</label>
                                                                                                        <input id="fg-virt-pass" type="password" name="v_pass" value="dba" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-post-name">PostGIS Username:</label>
                                                                                                        <input id="fg-post-name" ype="text" name="p_name" value="<% out.println(request.getAttribute("postgis-username"));%>" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-post-db">PostGIS Database</label>
                                                                                                        <input id="fg-post-db" type="text" name="p_data" value="<% out.println(request.getAttribute("postgis-database"));%>" class="form-control">
                                                                                                    </div>
                                                                                                    <div class="form-group">
                                                                                                        <label for="fg-post-pass">PostGIS Password:</label>
                                                                                                        <input id="fg-post-pass" type="password" name="p_pass" value="<% out.println(request.getAttribute("postgis-password"));%>" class="form-control" value="<% out.println(request.getAttribute("postgis-username"));%>">
                                                                                                    </div>
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
                                                                                                                <td style="width:216; text-align: center;" align="left" valign="bottom"> <input id="fg-file-input" name="file" type="file" /></td>
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
                                                                                                <input id="fg-links-unfilter-button" type="submit" value="Unfilter" style="float:right" onclick="return true;"/>
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
                                                                                                <table class="complex_selector rwd-table-white">
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
                                                                                                                <ul id="linkMatchList" style="word-break: break-all;word-wrap: break-word;" class="righted schemaList">
                                                                                                                </ul>
                                                                                                            </td>
                                                                                                        </tr>
                                                                                                    </tbody></table>
                                                                                                <!-- <input id="linkSchemaButton" type="submit" value="Add Link Matched Properties" style="float:right" onclick="return true;"/> -->
                                                                                                <table border="1">
                                                                                                    <tbody>
                                                                                                        <tr>
                                                                                                            <td style="width:26; background-color: green; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                                                                                            <td id="legendLinkSetA" style="text-align: center;" align="left" valign="bottom">Dataset A Layer</td>
                                                                                                            <td style="width:26; background-color: blue; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
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
                                                                                                <table class="complex_selector rwd-table-white">
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
                                                                                                                <ul id="matchList" style="word-break: break-all;word-wrap: break-word;" class="righted schemaList">
                                                                                                                        </ul>
                                                                                                                    </td>
                                                                                                                </tr>
                                                                                                            </tbody></table>
                                                                                                        <table border="1">
                                                                                                            <tbody>
                                                                                                                <tr>
                                                                                                                    <td style="width:26; background-color: green; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                                                                                                    <td id="legendSetA" style="text-align: center;" align="left" valign="bottom">Dataset A Layer</td>
                                                                                                                    <td style="width:26; background-color: blue; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                                                                                                    <td id="legendSetB" style="text-align: center;" align="left" valign="bottom">Dataset B Layer</td>
                                                                                                                    <td style="width:26; background-color: red; table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                                                                                                    <td id="legendSetL" style="text-align: center;" align="left" valign="bottom">Link Layer</td>
                                                                                                                </tr></form>
                                                                                                            </tbody>
                                                                                                        </table>
                                                                                                        <input id="finalButton" type="submit" value="Preview" style="display: block; margin-left:auto; margin-right:0;" onclick="return true;"/>

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
                                                                                                        <td>
                                                                                                            <div style="float: right;" id="connVecLenCheck">
                                                                                                                <input type="radio" name="vecLen" id="check1"><label for="check1">YES</label>
                                                                                                                    <input type="radio" name="vecLen" id="check2"><label for="check2">NO</label>
                                                                                                                        </div>
                                                                                                                        </td> 
                                                                                                                        </tr>
                                                                                                                        <tr>
                                                                                                                            <td>Connecting Vector Direction:</td>
                                                                                                                            <td>
                                                                                                                                <div style="float: right;" id="connVecDirCheck">
                                                                                                                                    <input type="radio" name="VecDir" id="check3"><label for="check3">YES</label>
                                                                                                                                        <input type="radio" name="VecDir"  id="check4"><label for="check4">NO</label>
                                                                                                                                            </div>
                                                                                                                                            </td> 
                                                                                                                                            </tr>
                                                                                                                                            <tr>
                                                                                                                                                <td>Polygon Coverage:</td>
                                                                                                                                                <td>
                                                                                                                                                    <div style="float: right;" id="connCoverageCheck">
                                                                                                                                                        <input type="radio" name="Coverage" id="check5"><label for="check5">YES</label>
                                                                                                                                                            <input type="radio" name="Coverage"  id="check6"><label for="check6">NO</label>
                                                                                                                                                                </div>
                                                                                                                                                                </td> 
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
                                                                                                                                                                    <div class="split content" style="height:100%" id="map"></div>
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
                                                                                                                                                                    <script type="text/javascript" src="js/split.min.js"></script>
                                                                                                                                                                    <script type="text/javascript" src="js/map.js"></script>
                                                                                                                                                                    <script src="http://maps.google.com/maps/api/js?v=3.5&sensor=false"></script>
                                                                                                                                                                    <script type="text/javascript" src="js/fusion.js"></script>

                                                                                                                                                                    <script type="text/javascript">
                                                                                                                                                                            $(document).ready(function () {
                                                                                                                                                                                $('ul.sf-menu').sooperfish();
                                                                                                                                                                            });
                                                                                                                                                                    </script>

                                                                                                                                                                    <script type="text/javascript" src="js/droppanel.js"></script>
                                                                                                                                                                    </body>

                                                                                                                                                                    </html>
