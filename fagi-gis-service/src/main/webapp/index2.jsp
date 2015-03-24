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
    </head>
    <body>
        <div class="tooltip" id="transformSelect">
            <progress></progress>
        </div>
        <div id="fagi">
            <div id="nav3">
            <table style="width:100%; height:5%;">
  <tr>
    <td><a href="#connection" onclick="testAnim();return false;">connection</a></td>
    
                    <td><a href="#links" onclick="testAnim2();return false;">datasets</a></td>
                    <td><a href="#links" onclick="expandLinksPanel();return false;">links</a></td>
                    <td><a href="#fusion">fusion</a></td>
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
    <li><a href="#">fuse visible</a></li>
    <li><a href="#">multiple select</a></li>
    <li><a href="#">find similar</a></li>
    <li class="divider"></li>
    <li><a href="#">advanced search</a></li>
    <li><a href="#">sparql</a></li>
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
            <div id="datasetPanel">
            <form id="dataDiv" style="color: blue;" name="data_input">
                Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/osm"/>
              SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="http://localhost:8890/sparql"/>
            Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/wik"/>
            SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql"/>
            Target Graph: <input type="text" id="ider" name="t_graph" class="centered" value="http://localhost:8890/fused_dataset"/>
            SPARQL Endpoint Target: <input type="text" id="ider" name="t_end" class="centered" value="http://localhost:8890/sparql"/>-->
            <!--Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/fagi/Desktop/"/>-->
            <!-- Linux IMIS 
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/nick/Projects/FAGI-gis-master/"/>-->
            <!-- Mac OS X -->
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/Users/nickvitsas/Downloads"/>
            <table>
                <tbody>
                    <tr>
                        <td style="text-align: center; padding-right: 20px;" align="left" valign="bottom">Dominant Dataset:</td>
                        <td style="text-align: center; padding-right: 20px;" align="left" valign="bottom"><div class="checkboxes" style="display:inline-block;"><label><input id="domA" name="d_dom" value="true" type="checkbox" checked="true"/>A</label></div></td>
                        <td style="text-align: center; padding-right: 20px;" align="left" valign="bottom"><div class="checkboxes" style="display:inline-block;"><label><input id="domB" type="checkbox" />B</label></div></td>                                  
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
            <!-- Linux IMIS 
            PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/> -->
            <!-- Mac OS X -->
            PostGIS Username: <input type="text" name="p_name" class="centered" value="nickvitsas"/>
            PostGIS Database <input type="text" name="p_data" class="centered" value="postgis1"/>
            PostGIS Password: <input type="password" name="p_pass" class="centered" value="1111"/>
            <label id="connLabel" for="male">Connection not established</label><input id="connButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
        </form>
            </div>
            <div id="linksPanel">
        <table>
                                  <tbody>
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
         <ul id="linksList" style="background: white;" class="righted">
  </ul>
      <input id="linksButton" type="submit" value="Submit" style="float:right" onclick="return true;"/>
      <input id="allLinksButton" type="submit" value="Select All" style="float:right" onclick="return true;"/>
  </div>
  <script src="//code.jquery.com/jquery-1.10.2.js"></script>
  <script src="//code.jquery.com/ui/1.11.3/jquery-ui.js"></script>
  <script type="text/javascript" src="js/jquery.autocomplete.min.js"></script>
  <script type="text/javascript" src="js/jquery.easing-sooper.js"></script>
  <script type="text/javascript" src="js/jquery.sooperfish.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js"></script>
  <script type="text/javascript" src="js/image_fade.js"></script>
  <script type="text/javascript" src="js/modernizr-1.5.min.js"></script>
  <script type="text/javascript" src="js/proj4.js"></script>
  <script type="text/javascript" src="js/json2.js"></script>
  <script src="http://openlayers.org/api/OpenLayers.js"></script> 
  <script type="text/javascript" src="js/mappreview.js"></script>
  <script src="http://maps.google.com/maps/api/js?v=3&amp;sensor=false"></script>
  <script type="text/javascript" src="js/fusion.js"></script>
  <script type="text/javascript" src="js/autocomplete.js"></script>
  
  <script type="text/javascript">
    $(document).ready(function() {
      $('ul.sf-menu').sooperfish();
    });
  </script>
  
  <script type="text/javascript" src="js/droppanel.js">  </script>
    </body>
 
</html>