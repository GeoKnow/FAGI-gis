<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<!DOCTYPE HTML>
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>FAGI-gis</title>
  <meta name="description" content="website description" />
  <meta name="keywords" content="website keywords, website keywords" />
  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
  <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
  <link rel="stylesheet" href="//code.jquery.com/ui/1.11.3/themes/smoothness/jquery-ui.css">
  <!-- modernizr enables HTML5 elements and feature detects -->
  
</head>

<body>
    <div class="tooltip" id="link_tooltip">
        <progress></progress>
    </div>
    <div class="tooltip" id="transformSelect">
        <progress></progress>
    </div>
    <%
        //out.println("Your IP address is "+request.getParameter("id"));
    %>
    <div class="tooltip" id="poly_tooltip">polytooltip
        Dataset A: <input list="datalist1" type="text" id="idr" name="da_name" class="centered" value="http://localhost:8890/points3"/>
    </div>
    
  <div id="main">
    <header>
      <div id="logo">
        <div id="logo_text">
          <!-- class="logo_colour", allows you to change the colour of the text -->
          <h1><a href="index.html">GEOKNOW<span class="logo_colour">FAGI-gis</span></a></h1>
          <h2>Fusing geo data since 2014...</h2>
        </div>
      </div>
      <nav>
        <ul class="sf-menu" id="nav">
          <li><a href="index.html">Home</a></li>
          <li><a href="examples.html">Examples</a></li>
          <li class="selected"><a href="page.html">A Page</a></li>
          <li><a href="another_page.html">Another Page</a></li>
          <li><a href="#">Example Drop Down</a>
            <ul>
              <li><a href="#">Drop Down One</a></li>
              <li><a href="#">Drop Down Two</a>
                <ul>
                  <li><a href="#">Sub Drop Down One</a></li>
                  <li><a href="#">Sub Drop Down Two</a></li>
                  <li><a href="#">Sub Drop Down Three</a></li>
                  <li><a href="#">Sub Drop Down Four</a></li>
                  <li><a href="#">Sub Drop Down Five</a></li>
                </ul>
              </li>
              <li><a href="#">Drop Down Three</a></li>
              <li><a href="#">Drop Down Four</a></li>
              <li><a href="#">Drop Down Five</a></li>
            </ul>
          </li>
          <li><a href="contact.html">Contact Us</a></li>
        </ul>
      </nav>
    </header>
    <div id="site_content">
      <div class="gallery">
        <ul class="images">
          <li class="show"><img width="950" height="300" src="images/1.jpg" alt="photo_one" /></li>
          <li><img width="950" height="300" src="images/2.jpg" alt="photo_two" /></li>
          <li><img width="950" height="300" src="images/3.jpg" alt="photo_three" /></li>
        </ul>
      </div>
        
        
     <div class="panel">
        
        <%
        out.println("<h2>Connection</h2>");
        %>
        <div class="panelcontent">
           
            <!--
        <form id="connDiv" name="conn_input"><p />
            Virtuoso URL: <input type="text" name="v_url" class="centered" value="localhost:1111"/>
            Virtuoso Username: <input type="text" name="v_name" class="centered" value="dba"/>
            Virtuoso Password: <input type="password" name="v_pass" class="centered" value="dba"/>
            PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/>
            PostGIS Database <input type="text" name="p_data" class="centered" value="postgis1"/>
            PostGIS Password: <input type="password" name="p_pass" class="centered" value="postgres"/>
            <label id="connLabel" for="male">Connection not established</label><input id="connButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
        </form>-->
      
      <!--
      <form id="connDiv" name="conn_input"><p />
            Virtuoso URL: <input type="text" name="v_url" class="centered" value="localhost:1111"/>
            Virtuoso Username: <input type="text" name="v_name" class="centered" value="dba"/>
            Virtuoso Password: <input type="password" name="v_pass" class="centered" value="dba"/>
            PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/>
            PostGIS Database <input type="text" name="p_data" class="centered" value="postgis1"/>
            PostGIS Password: <input type="password" name="p_pass" class="centered" value="postgres"/>
            <label id="connLabel" for="male">Connection not established</label><input id="connButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
        </form>
      -->
      <form id="connDiv" name="conn_input"><p />
            Virtuoso URL: <input type="text" name="v_url" class="centered" value="localhost:1111"/>
            Virtuoso Username: <input type="text" name="v_name" class="centered" value="dba"/>
            Virtuoso Password: <input type="password" name="v_pass" class="centered" value="dba"/>
            <!-- Linux IMIS 
            PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/> -->
            <!-- Windows IMIS -->
            PostGIS Username: <input type="text" name="p_name" class="centered" value="postgres"/>
            <!-- Mac OS X 
            PostGIS Username: <input type="text" name="p_name" class="centered" value="nickvitsas"/> -->
            PostGIS Database <input type="text" name="p_data" class="centered" value="postgis1"/>
            PostGIS Password: <input type="password" name="p_pass" class="centered" value="1111"/>
            <label id="connLabel" for="male">Connection not established</label><input id="connButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
        </form>
        
        </div>
    </div>

<div class="panel">
  <h2>Datasets</h2>
  <div class="panelcontent">
          
          <!--
          <form id="dataDiv" name="data_input"><p />
              Dataset A: <input list="datalist1" type="text" id="ider" name="da_name" class="centered" value="http://localhost:8890/points3"/>
<datalist id="datalist1"></datalist>
              SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="http://localhost:8890/sparql"/>
Dataset B: <input type="text" name="db_name" class="centered" value="http://localhost:8890/polygons3"/>
SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql"/>
Target Graph: <input type="text" id="ider" name="t_graph" class="centered" value="http://localhost:8890/fused_dataset"/>
Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/imis-nkarag/software/FAGI-gis-WebInterface_svn/FAGI-gis-WebInterface"/>
<label id="dataLabel" for="male">No dataset selected</label><input id="dataButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
</form>-->
          <!--
       <form id="dataDiv" name="data_input"><p />
              Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/osm"/>
<datalist id="datalist1"></datalist>
              SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="http://localhost:8890/sparql-auth"/>
            Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/wik"/>
            SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql-auth"/>
            Target Graph: <input type="text" id="ider" name="t_graph" class="centered" value="http://localhost:8890/fused_dataset"/>
            SPARQL Endpoint Target: <input type="text" id="ider" name="t_end" class="centered" value="http://localhost:8890/sparql-auth"/>
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/demo/Documents/FAGI/"/>
            <label id="dataLabel" for="male">No dataset selected</label><input id="dataButton" type="submit" value="Submit" style="float:right" onclick="return false;"/>
</form>
          -->
          
          
          <form id="dataDiv" name="data_input"><p />
              <!-- Linux IMIS
              Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/osm"/>-->
              <!-- Windows IMIS -->
              Dataset A: <input type="text" name="da_name" id="idDatasetA" class="centered" value="http://localhost/DAV/osm"/> -->
              <!-- Mac OS X 
              Dataset A: <input list="datalist1" type="text" id="idDatasetA" name="da_name" class="centered" value="http://localhost:8890/DAV/osm"/> -->
<datalist id="datalist1"></datalist>
              SPARQL Endpoint A: <input type="text" name="da_end" class="centered" value="http://localhost:8890/sparql"/>
              <!-- Linux IMIS
              Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/wik"/> -->
              <!-- Windows IMIS -->
              Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost/DAV/wik"/> -->
              <!-- Mac OS X 
            Dataset B: <input type="text" name="db_name" id="idDatasetB" class="centered" value="http://localhost:8890/DAV/wik"/> -->
            SPARQL Endpoint B: <input type="text" name="db_end" class="centered" value="http://localhost:8890/sparql"/>
            Target Graph: <input type="text" id="ider" name="t_graph" class="centered" value="http://localhost:8890/fused_dataset"/>
            SPARQL Endpoint Target: <input type="text" id="ider" name="t_end" class="centered" value="http://localhost:8890/sparql"/>
            <!--Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/fagi/Desktop/"/>-->
            <!-- Linux IMIS -->
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/home/nick/Projects/FAGI-gis-master/"/>
            <!-- Windows IMIS 
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="C:\Users\nick\Downloads\virtuoso-opensource\database"/>-->
            <!-- Mac OS X 
            Bulk Insert Dir: <input type="text" id="ider" name="bulk" class="centered" value="/Users/nickvitsas/Downloads"/> -->
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
</div>
        
<div class="panel">
  <h2>Linked Entities</h2>
  
  <div class="panelcontent">
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
         <ul id="linksList" class="righted">
  </ul>
      <input id="linksButton" type="submit" value="Submit" style="float:right" onclick="return true;"/>
      <input id="allLinksButton" type="submit" value="Select All" style="float:right" onclick="return true;"/>
  </div>
</div>
        
<div class="panel">
  <h2>Fusion</h2>
   
  <div class="panelcontent">

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
                                          <div class="#horscroll">
                                      <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                      <ul id="schemasA" class="schemaList"></ul>
                                          </div>
                                      </td>
                                      <td style="width:30%">
                                        <div class="#horscroll">
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
      
               <input id="finalButton" type="submit" value="Preview" style="float:right" onclick="return true;"/>

      <table class="complex_selector">
                                  <tbody><tr>
                                    <td id="linkNameA" style="width:216; text-align: center;" align="left" valign="bottom">Dataset A</td>
                                    <td id="linkNameB" style="width:216; text-align: center;" align="left" valign="bottom">Dataset B</td>
                                    <td>
                                    </td>
                                    <td style="width:216; text-align: center;" align="left" valign="bottom">Selected</td>
                                  </tr>
                                  <tr>
                                      <td style="width:30%;">
                                          <div class="#horscroll">
                                      <!--<select id="schemasA" name="sl_roles_available" size="5" multiple="TRUE" tabindex="12"></select>-->
                                      <ul id="linkSchemasA" class="schemaList"></ul>
                                          </div>
                                      </td>
                                      <td style="width:30%">
                                        <div class="#horscroll">
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
         <input id="linkSchemaButton" type="submit" value="Add Link Matched Properties" style="float:right" onclick="return true;"/>
  </div>
</div>       
      <div class="content">
        <h1>Preview</h1>
        <table border="1">
                                  <tbody>
                                  <tr>
                                    <td style="width:26; background-color: rgb(0,0,0); table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetA" style="text-align: center;" align="left" valign="bottom">Dataset A Layer</td>
                                    <td style="width:26; background-color: rgb(0,0,255); table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetB" style="text-align: center;" align="left" valign="bottom">Dataset B Layer</td>
                                    <td style="width:26; background-color: rgb(255,0,0); table-layout:fixed; width:20px; overflow:hidden; word-wrap:break-word;text-align: center;" align="left" valign="bottom"></td>
                                    <td id="legendSetL" style="text-align: center;" align="left" valign="bottom">Link Layer</td>
                                  </tr></form>
                                  </tbody>
        </table>
        
        <div id="map" style="height:400px; width:100%; position:relative; "></div>
        <input id="fuseByZoom" type="submit" value="Fuse visible" style="float:right" onclick="return true;"/>
      </div>
    </div>
    <footer>
      <p>Copyright copy; GEOKNOW TEAM | <a href="http://geoknow.eu/Welcome.html">GeoKnow</a></p>
    </footer>
  </div>
  <script type='text/javascript' > 
    var linkMatchesJSON = null;
  </script>
  <!-- javascript at the bottom for fast page loading -->
  <script src="//code.jquery.com/jquery-1.10.2.js"></script>
  <script src="//code.jquery.com/ui/1.11.3/jquery-ui.js"></script>
  <script type="text/javascript" src="js/jquery.autocomplete.min.js"></script>
  <script type="text/javascript" src="js/jquery.easing-sooper.js"></script>
  <script type="text/javascript" src="js/jquery.sooperfish.js"></script>
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
