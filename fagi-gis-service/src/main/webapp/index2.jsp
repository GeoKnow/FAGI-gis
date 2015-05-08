<html>
    <head>
        <title>FAGI-gis</title>
        <meta name="description" content="website description" />
        <meta name="keywords" content="website keywords, website keywords" />
        <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
        <link rel="stylesheet" href="//code.jquery.com/ui/1.11.3/themes/smoothness/jquery-ui.css">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap-theme.min.css">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css">
  <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
</head>
    <body>

        <div class="tooltip" id="popupMenu">
            <table style="width:100%; color:white;">
                <tr>
                    <td>Select Radius (km) : </td>
                    <td><input id="radiusSpinner" name="value"></td> 
                </tr>
            </table>
            <input id="popupButton" type="submit" value="Fetch Geometries" style="float:right; color: black;" onclick="return false;"/>
        </div>

        <div class="tooltip" id="popupBBoxMenu">
            <ul id="bboxMenu">
                <li id="transformBBoxButton">Transform BBox</li>
                <li id="fetchBBoxContainedButton">Fetch Contained</li>
            </ul>
        </div>

        <div class="tooltip" id="popupTransformMenu">
            <ul id="transformMenu">
                <li id="moveButton">Move</li>
                <li id="rotateButton">Rotate</li>
                <li id="scaleButton">Scale</li>
                <li id="createLinkButton">Create Link</li>
            </ul>
        </div>

        <div id="fagi">
            <div id="nav3">
                <table style="width:100%; height:5%;">
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
                        <td><form method="get" action="/search" id="search">
                                <input name="q" type="text" size="40" placeholder="Search..." />
                            </form></td>
                    </tr>
                </table> </div>
            <div style="float:right; width:100%; height:95%;" id="map"></div>
        </div>
        <div id="dialog" style="ui-dialog-titlebar" title="Connections">
            <div id="mainPanel">
                <div id="datasetPanel">
                    <form id="dataDiv" name="data_input">
                        <!-- Linux IMIS -->
                      Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/osm"/> 
                        <!-- Windows IMIS 
                        Dataset A: <input type="text" name="da_name" id="idDatasetA" class="centered" value="http://localhost/DAV/osm"/> -->
                        <!-- Mac OS X 
                        Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/DAV/osm"/> -->
                        <datalist id="datalist1"></datalist>
                        SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="http://localhost:8890/sparql"/> 
                        <!-- Linux IMIS -->
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/wik"/>
                        <!-- Windows IMIS 
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost/DAV/wik"/> -->
                        <!-- Mac OS X 
                        Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/DAV/wik"/> -->
                        SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql"/>
                        SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql"/>
                        Target Graph: <input type="text" name="t_graph" class="centered" value="http://localhost:8890/fused_dataset"/>
                        SPARQL Endpoint Target: <input type="text" name="t_end" class="centered" value="http://localhost:8890/sparql"/>
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
                                    <td style="padding-right: 20px;" align="left" valign="bottom">Dominant Dataset:</td>
                                    <td style="text-align: center; padding-right: 20px;"><div class="checkboxes" style="display:inline-block;"><label><input id="domA" name="d_dom" value="true" type="checkbox" checked="true"/>A</label></div></td>
                                    <td style="text-align: center; padding-right: 20px;"><div class="checkboxes" style="display:inline-block;"><label><input id="domB" type="checkbox" />B</label></div></td>                                  
                                </tr>
                            </tbody>
                        </table>

                        <label id="dataLabel" for="male">No dataset selected</label><input id="dataButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>

                    </form>
                </div>
                <div id="connectionPanel">
                    <form id="connDiv" name="conn_input"><p />
                        Virtuoso URL: <input type="text" name="v_url" class="centered" value="localhost:1111"/>
                        Virtuoso Username: <input type="text" name="v_name" class="centered" value="dba"/>
                        Virtuoso Password: <input type="password" name="v_pass" class="centered" value="dba"/>
                        <!-- Linux IMIS -->
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/>
                        <!-- Windows IMIS 
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/> -->
                        <!-- Mac OS X 
                        PostGIS Username: <input type="text" name="p_name" class="centered" value="nickvitsas"/> -->
                        PostGIS Database <input type="text" name="p_data" class="centered" value="postgis1"/>
                        PostGIS Password: <input type="password" name="p_pass" class="centered" value="1111"/>
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
                                    </select></td>
                                <td>
                                </td>
                                <td style="width:100; text-align: center;" align="left" valign="bottom"><input style="width: 100%" id="buttonFilterLinksB" type="button" value="Filter" /></td>
                            </tr></form>
                        <tr>

                        </tr>
                        </tbody></table>  
                    <ul id="linksList" class="righted">
                    </ul>
                    <input id="linksButton" type="submit" value="Submit" style="float:right" onclick="return true;"/>
                    <input id="allLinksButton" type="submit" value="Select All" style="float:right" onclick="return true;"/>
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
                    <div class="table-responsive" id="linkFusionTable">
                    </div>
                </div>
                <div id="matchingPanel">
                    <table class="complex_selector">
                        <p>
                            <label for="spinner">Select scoring threshold:</label>
                            <input id="spinner" name="value">
                        </p>
                        <tbody><tr>
                                <td id="datasetNameA" style="width:216; text-align: center;" align="left" valign="bottom">Dataset A</td>
                                <td id="datasetNameB" style="width:216; text-align: center;" align="left" valign="bottom">Dataset B</td>
                                <td>
                                </td>
                                <td style="width:216; text-align: center;" align="left" valign="bottom">Selected</td>
                            </tr>
                            <tr>
                                <td style="width:30%;">
                                    <div class="horscroll">
                                        <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                        <ul id="schemasA" class="schemaList"></ul>
                                    </div>
                                </td>
                                <td style="width:30%">
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
                                <td style="width:30%">
                                    <ul id="matchList" class="righted">
                                    </ul>
                                </td>
                            </tr>
                        </tbody></table>
                    <form action="#">
                    <fieldset>
                        <label for="speed">Perform batch fusion on : </label>
                        <select name="speed" id="clusterSelector">
                            <option selected="selected">All links</option>
                        </select>
                    </fieldset>
                    </form>
                    <input id="finalButton" type="submit" value="Preview" style="float:right" onclick="return true;"/>
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
            </div>
        </div>
        <script src="//code.jquery.com/jquery-1.10.2.js"></script>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js"></script>
        <script src="//code.jquery.com/ui/1.11.3/jquery-ui.js"></script>
        <script type="text/javascript" src="js/jquery.autocomplete.min.js"></script>
        <script type="text/javascript" src="js/jquery.easing-sooper.js"></script>
        <script type="text/javascript" src="js/jquery.sooperfish.js"></script>
        <script type="text/javascript" src="js/modernizr-1.5.min.js"></script>
        <script type="text/javascript" src="js/json2.js"></script>
        <script type="text/javascript" src="js/proj4js-combined.min.js"></script>
        <script type="text/javascript" src="js/EPSG27563.min.js"></script>
        <script src="http://openlayers.org/api/OpenLayers.js"></script> 
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
