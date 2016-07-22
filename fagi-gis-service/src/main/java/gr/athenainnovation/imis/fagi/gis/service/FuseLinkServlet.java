/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.Concatenation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftAToB;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftBToA;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepLeftTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepBothTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepRightTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONFusionResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONPropertyFusion;
import gr.athenainnovation.imis.fusion.gis.json.JSONShiftFactors;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.SPARQLUtilities;
import gr.athenainnovation.imis.fusion.gis.utils.Utilities;
import static gr.athenainnovation.imis.fusion.gis.utils.Utilities.isURLToLocalInstance;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "FuseLinkServlet", urlPatterns = {"/FuseLinkServlet"})
public class FuseLinkServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(FuseLinkServlet.class);    
        
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Per reqiest state
        PrintWriter             out = null;
        JSONFusionResult        ret = new JSONFusionResult();
        DBConfig                dbConf;
        GraphConfig             grConf;
        VirtGraph               vSet = null;
        Connection              dbConn = null;
        List<FusionState>       fs = null;
        String                  tGraph = null;
        String                  nodeA = null;
        String                  nodeB = null;
        String                  dom = null;
        String                  domSub = null;
        String                  restAction = "None";
        HttpSession             sess = null;
        JSONPropertyFusion[]    selectedFusions;
        ObjectMapper            mapper = new ObjectMapper();
        
        try {
            out = response.getWriter();
            
            sess = request.getSession(false);
            
            if ( sess == null ) {
                return;
            }

            String classParam = request.getParameter("classes");
            restAction = request.getParameter("rest");
            String[] classes = request.getParameterValues("classes[]");
            
            if ( classes == null && classParam == null ) {
                out.print("{}");
                
                return;
            }
            
            //mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            //SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            //mapper.setDateFormat(outputFormat);
            //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            fs = (List<FusionState>)sess.getAttribute("fstate");
            nodeA = (String)sess.getAttribute("nodeA");
            nodeB = (String)sess.getAttribute("nodeB");
            HashMap<String, String> linksHashed =(HashMap<String, String>)sess.getAttribute("links");
            
            if ( nodeB.equals(" ") || nodeB == null )
                nodeB = linksHashed.get(nodeA);
            
            tGraph = (String)sess.getAttribute("t_graph");
            
            try {    
                vSet = new VirtGraph ("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                                         dbConf.getUsername(), 
                                         dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());      
                out.println("Connection to virtuoso failed");
                out.close();
            
                return;
            }
        
            try{
                Class.forName("org.postgresql.Driver");     
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());      
                out.println("Class of postgis failed");
                out.close();
            
                return;
            }
             
            try {
                String url = Constants.DB_URL.concat(dbConf.getDBName());
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
                dbConn.setAutoCommit(false);
            } catch(SQLException sqlex) {
                System.out.println(sqlex.getMessage());      
                out.println("Connection to postgis failed");
                out.close();
            
                return;
            }
            String[] props = request.getParameterValues("props[]");
            String propsJSON = request.getParameter("propsJSON");
            String shiftJSON = request.getParameter("factJSON");
            
            String domA = (String)sess.getAttribute("domA");
            String domB = (String)sess.getAttribute("domB");
            dom = domB;
            domSub = nodeB;
            if ( grConf.isDominantA() ) {
                domSub = nodeA;
                dom = domA;
            }
            System.out.println("Dom A "+domA+" Dom B "+domB);
            System.out.println("Dom Sub A "+nodeA+" Dom B "+nodeB);
            
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jp = factory.createJsonParser(propsJSON);
            selectedFusions = mapper.readValue(jp, JSONPropertyFusion[].class );

            JsonParser jp2 = factory.createJsonParser(shiftJSON);
            JSONShiftFactors sFactors = mapper.readValue(jp2, JSONShiftFactors.class );
            
            System.out.println(propsJSON);
            System.out.println("Shift JSON "+shiftJSON);
            for (JSONPropertyFusion pf : selectedFusions ) {
                System.out.println(pf.getValA());
                System.out.println(pf.getValB());
            }
            
            System.out.println("Fusing : "+nodeA+" "+nodeB);
            AbstractFusionTransformation trans = null;
            for(;;) {
                if (classParam == null) {
                    String domOnto = "";
                    if (grConf.isDominantA()) {
                        domOnto = (String) sess.getAttribute("domA");
                    } else {
                        domOnto = (String) sess.getAttribute("domB");
                    }
                    System.out.println("Add Classes !!!!!!!!!!!!!!!!");

                    for (String c : classes) {
                        ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                        //queryStr.append("WITH <"+fusedGraph+"> ");
                        queryStr.append("INSERT DATA { ");
                        queryStr.append("GRAPH <" + tGraph + "> { ");

                        queryStr.appendIri(nodeA);
                        queryStr.append(" ");
                        queryStr.appendIri(Constants.TYPE);
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.append(".");
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.appendIri(Constants.TYPE);
                        queryStr.append(" ");
                        queryStr.appendIri(Constants.OWL_CLASS);
                        queryStr.append(".");
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.appendIri(Constants.LABEL);
                        queryStr.append(" ");
                        queryStr.appendLiteral(c);

                        queryStr.append("} }");
                        System.out.println("Add Owl Class " + queryStr.toString());

                        UpdateRequest q = queryStr.asUpdate();
                        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                        UpdateProcessor insertClass = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                        insertClass.execute();
                    }
                }
                
                trans = FuserPanel.transformations.get(selectedFusions[0].getAction());
                if ( trans instanceof ShiftAToB) {
                    ((ShiftAToB)trans).setShift(sFactors.getShift());
                    ((ShiftAToB)trans).setRotate(sFactors.getRotateFact());
                    ((ShiftAToB)trans).setScale(sFactors.getScaleFact());
                }
            
                if ( trans instanceof ShiftBToA) {
                    ((ShiftBToA)trans).setShift(sFactors.getShift());
                    ((ShiftBToA)trans).setRotate(sFactors.getRotateFact());
                    ((ShiftBToA)trans).setScale(sFactors.getScaleFact());
                }
                
                updateGeom(nodeA, nodeB, selectedFusions[0].getValA(), selectedFusions[0].getValB(), dbConn);
                        
                if ( trans instanceof KeepRightTransformation) {
                    String q = SPARQLUtilities.formInsertGeomQuery(tGraph, domSub, selectedFusions[0].getValB());
                    System.out.println("Query Right "+q);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.setGeom( selectedFusions[0].getValB() );
                    
                    break;
                }
                
                if ( trans instanceof KeepLeftTransformation) {
                    String q = SPARQLUtilities.formInsertGeomQuery(tGraph, domSub, selectedFusions[0].getValA());
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.setGeom( selectedFusions[0].getValA() );
                    break;
                }
                
                if ( trans instanceof KeepBothTransformation) {
                    String qA = SPARQLUtilities.formInsertGeomQuery(tGraph, domSub, selectedFusions[0].getValA());
                    String qB = SPARQLUtilities.formInsertGeomQuery(tGraph, domSub, selectedFusions[0].getValB());
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(qA, vSet);
                    vur.exec();
                    vur = VirtuosoUpdateFactory.create(qB, vSet);
                    vur.exec();
                            
                    ret.setGeom( "GEOMETRYCOLLECTION("+selectedFusions[0].getValA()+", "+selectedFusions[0].getValB()+")" );

                    break;
                }
                
                if ( trans instanceof Concatenation) {
                    String qA = SPARQLUtilities.formInsertConcatGeomQuery(tGraph, domSub, selectedFusions[0].getValA(), selectedFusions[0].getValB());
                    System.out.println("Concatenation");
                    System.out.println(qA);
                    System.out.println(selectedFusions[0].getValA());
                    System.out.println(selectedFusions[0].getValB());
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(qA, vSet);
                    vur.exec();
                    
                    ret.setGeom( "GEOMETRYCOLLECTION("+selectedFusions[0].getValA()+", "+selectedFusions[0].getValB()+")" );
                    
                    break;
                }
                
                System.out.println(trans == null);
                
                try {
                    trans.fuse(dbConn, nodeA, nodeB);
                } catch (SQLException ex) {
                    LOG.trace("Final fusion failed");
                    LOG.debug("Final fusion failed");
                }
                
                VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
                virtImp.setTransformationID(trans.getID());
            
                virtImp.importGeometriesToVirtuoso((String)sess.getAttribute("t_graph"));
            
                //virtImp.trh.finish();
                virtImp.finishUpload();
            
                String queryGeoms = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(b.geom) as g\n" +
                                 "FROM links INNER JOIN fused_geometries AS b\n" +
                                 "ON (b.subject_a = ?)";
                //System.out.println("With subject "+nodeA);
                //System.out.println("With subject "+nodeB);
                
                PreparedStatement stmt;
                try {
                    stmt = dbConn.prepareStatement(queryGeoms);

                    stmt.setString(1, nodeA);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        ret.setGeom(rs.getString(3));
                        //System.out.println("Returning geom : "+ret.geom);
                    }
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown on update");
                    LOG.debug("SQLException thrown on update : " + ex.getMessage());
                    LOG.debug("SQLException thrown on update : " + ex.getSQLState());
                }
                //System.out.println(queryGeoms);
            
                break;
            }
            
            
            for(int i = 1; i < selectedFusions.length; i++) {
                handleMetadataFusion(selectedFusions[i].getAction(), i, nodeA, tGraph, sess, grConf, vSet, selectedFusions );
                if ( Constants.LATE_FETCH ) {
                    deleteSelectedProperties(restAction, selectedFusions[i].getAction(), i, nodeA, tGraph, sess, grConf, vSet, selectedFusions );
                }
            }
            
            insertRemaining(nodeA, restAction, grConf, vSet);
            SPARQLUtilities.clearFusedLink(nodeA, grConf, vSet.getConnection());
            
            System.out.println(mapper.writeValueAsString(ret));
            
            // Update destinATION GRAPH
            System.out.println("\n\n\n\n\nPreparing to update remote endpoint\n\n\n");
            SPARQLUtilities.UpdateRemoteEndpoint(grConf, vSet);
            
            SPARQLUtilities.checkpoint(vSet.getConnection());
            //System.out.println("JSON Geometry "+mapper.writeValueAsString(ret));
            out.println(mapper.writeValueAsString(ret));
       } catch (java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
            throw new ServletException("OutOfMemoryError thrown by Tomcat");
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
            throw new ServletException("JsonProcessingException thrown by Tomcat");
        } catch (IOException ex) {
            LOG.trace("IOException thrown");
            LOG.debug("IOException thrown : " + ex.getMessage());
            
            throw new ServletException("IOException opening the servlet writer");
        } finally {
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    LOG.trace("Failed to close connection to PostgreSQL");
                }
            }
            
            if ( vSet != null ) {
                vSet.close();
            }
            
            if ( out != null)
                out.close();
        }
    }
    
    private void insertRemaining(String node, String restAction, GraphConfig grConf, VirtGraph vSet) {
        Connection virt_conn = vSet.getConnection();
        StringBuilder getFromB = new StringBuilder();
        StringBuilder getFromA = new StringBuilder();
        
        if (restAction.equalsIgnoreCase("None")) {
            return;
        }

        getFromA.append("SPARQL INSERT\n");
        getFromA.append("  { GRAPH <").append(grConf.getTargetTempGraph()).append("> {\n");
        getFromA.append(" <" + node + "> ?p ?o1 . \n");
        getFromA.append(" ?o1 ?p4 ?o3 .\n");
        getFromA.append(" ?o3 ?p5 ?o4 .\n");
        getFromA.append(" ?o4 ?p6 ?o5\n");
        getFromA.append("} }\nWHERE\n");
        getFromA.append("{\n");
        getFromA.append(" GRAPH <").append(grConf.getMetadataGraphA()).append("> { {<" + node + "> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
        getFromA.append("\n");
        getFromA.append("}");

        getFromB.append("SPARQL INSERT\n");
        getFromB.append("  { GRAPH <").append(grConf.getTargetTempGraph()).append("> {\n");
        getFromB.append(" <" + node + "> ?p ?o1 . \n");
        getFromB.append(" ?o1 ?p4 ?o3 .\n");
        getFromB.append(" ?o3 ?p5 ?o4 .\n");
        getFromB.append(" ?o4 ?p6 ?o5 \n");
        getFromB.append("} }\nWHERE\n");
        getFromB.append("{\n");
        getFromB.append(" GRAPH <").append(grConf.getMetadataGraphB()).append("> { {<" + node + "> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
        getFromB.append("\n");
        getFromB.append("}");

        //System.out.println("GET FROM REMAINING B \n" + getFromB);
        //System.out.println("GET FROM REMAINING A \n" + getFromA);

        // Populate with data from the Sample Liink set
        
        try (PreparedStatement populateDataA = virt_conn.prepareStatement(getFromA.toString());
                PreparedStatement populateDataB = virt_conn.prepareStatement(getFromB.toString())) {
            //starttime = System.nanoTime();

            if (restAction.equalsIgnoreCase("Keep Both")
                    || restAction.equalsIgnoreCase("Keep A")) {
                populateDataA.executeUpdate();
            }
            if (restAction.equalsIgnoreCase("Keep Both")
                    || restAction.equalsIgnoreCase("Keep B")) {
                populateDataB.executeUpdate();
            }

        } catch (SQLException ex) {

            LOG.trace("SQLException thrown during temp target graph populating");
            LOG.debug("SQLException thrown during temp target graph populating : " + ex.getMessage());
            LOG.debug("SQLException thrown during temp target graph populating : " + ex.getSQLState());

        }
    }
    
    private void deleteSelectedProperties(String action, String activeAction, int idx, String node, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) {
        if ( action.equalsIgnoreCase("None") ) {
            return;
        }
        
        final String clearSelectedPropsAStart =   "SPARQL DELETE WHERE {\n"
                                                    + "\n"
                                                    + "    GRAPH <"+grConf.getMetadataGraphA()+"> {\n";
        final String clearSelectedPropsAEnd  = "} }";
        
        final String clearSelectedPropsBStart =   "SPARQL DELETE WHERE {\n"
                                                    + "\n"
                                                    + "    GRAPH <"+grConf.getMetadataGraphB()+"> {\n";
        final String clearSelectedPropsBEnd = "} }";
        
        long startTime, endTime;
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lstA = (List<String>) sess.getAttribute("link_property_patternsA");
        List<String> lstB = (List<String>) sess.getAttribute("link_property_patternsB");

        if (grConf.isDominantA()) {
            domOnto = (String) sess.getAttribute("domA");
        } else {
            domOnto = (String) sess.getAttribute("domB");
        }
        String name = "";
        try {
            name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.trace("UnsupportedEncodingException thrown during fusion of property " + selectedFusions[idx].getPre());
            LOG.debug("UnsupportedEncodingException thrown during fusion of property " + selectedFusions[idx].getPre() + " : " + ex.getMessage());
            
            return;
        }
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();

        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if (newPredTokes.length == 2) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",", "_");
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        
        //testThreads(links);
        //LOG.info(ANSI_YELLOW + "Thread test lasted " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);
        boolean isEndpointALocal;
        boolean isEndpointBLocal;

        isEndpointALocal = isURLToLocalInstance(grConf.getEndpointA()); //"localhost" for localhost

        isEndpointBLocal = isURLToLocalInstance(grConf.getEndpointB()); //"localhost" for localhost
        
        if ( activeAction.equalsIgnoreCase("Concatenation") || 
             activeAction.equalsIgnoreCase("Keep Concatenated Both") ||
             activeAction.equalsIgnoreCase("Keep Flattened Both") ||
             action.equalsIgnoreCase("Keep Both") ||
             action.equalsIgnoreCase("Keep B")) {
            for (String rightProp : rightPres) {
            //String[] leftPreTokens = leftPre.split(",");
                //String[] rightPreTokens = rightPre.split(",");
                String[] mainPattern = rightProp.split(",");

                List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lstB);

                StringBuilder q = new StringBuilder();
                String prev_s = "";
                System.out.println("Patterns " + patterns);

                for (String pattern : patterns) {
                    q.setLength(0);

                    String[] rightPreTokens = pattern.split(",");
                    q.append(clearSelectedPropsBStart);
                    if (grConf.isDominantA()) {
                        q.append(" <" + node + "> <" + rightPreTokens[0] + "> ?o0 . \n");
                    } else {
                        q.append(" <" + node + "> <" + rightPreTokens[0] + "> ?o0 . \n");
                    }
                    prev_s = "?o0";
                    for (int i = 1; i < rightPreTokens.length; i++) {
                        q.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    q.append(clearSelectedPropsBEnd);
                    System.out.println("Long gone B " + q.toString());
                    try (PreparedStatement clearStmt = virt_conn.prepareStatement(q.toString())) {

                        clearStmt.execute();

                    } catch (SQLException ex) {
                        LOG.trace("Dropping fused links failed");
                        LOG.debug("Dropping fused links failed");
                    }
                }
            }
        } else if ( activeAction.equalsIgnoreCase("Concatenation") || 
             activeAction.equalsIgnoreCase("Keep Concatenated Both") ||
             activeAction.equalsIgnoreCase("Keep Flattened Both") ||
             action.equalsIgnoreCase("Keep Both") ||
             action.equalsIgnoreCase("Keep A")) {
            for (String leftProp : leftPres) {
                //String[] leftPreTokens = leftPre.split(",");
                //String[] rightPreTokens = rightPre.split(",");
                String[] mainPattern = leftProp.split(",");

                List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lstA);

                StringBuilder q = new StringBuilder();
                String prev_s = "";
                System.out.println("Patterns " + patterns);

                for (String pattern : patterns) {
                    q.setLength(0);
                    q.append(clearSelectedPropsAStart);
                    String[] leftPreTokens = pattern.split(",");
                    if (grConf.isDominantA()) {
                        q.append(" <" + node + "> <" + leftPreTokens[0] + "> ?o0 . \n");
                    } else {
                        q.append(" <" + node + "> <" + leftPreTokens[0] + "> ?o0 . \n");
                    }
                    prev_s = "?o0";
                    for (int i = 1; i < leftPreTokens.length; i++) {
                        q.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    q.append(clearSelectedPropsAEnd);
                    System.out.println("Long gone A " + q.toString());
                    try (PreparedStatement clearStmt = virt_conn.prepareStatement(q.toString())) {

                        clearStmt.execute();

                    } catch (SQLException ex) {
                        LOG.trace("Dropping fused links failed");
                        LOG.debug("Dropping fused links failed");
                    }
                }
            }
        } else {
            return;
        }
    }
    
    private void lateFetchLinkData(String nodeA, String nodeB, int idx, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions, int activeCluster) {
        //System.out.println("\n\n\n\n\nLATE FETCHING\n\n\n");
        long startTime, endTime;
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lstA = (List<String>) sess.getAttribute("link_property_patternsA");
        List<String> lstB = (List<String>) sess.getAttribute("link_property_patternsB");

        if (grConf.isDominantA()) {
            domOnto = (String) sess.getAttribute("domA");
        } else {
            domOnto = (String) sess.getAttribute("domB");
        }
        String name = "";
        try {
            name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.trace("UnsupportedEncodingException thrown during fusion of property " + selectedFusions[idx].getPre());
            LOG.debug("UnsupportedEncodingException thrown during fusion of property " + selectedFusions[idx].getPre() + " : " + ex.getMessage());
            
            return;
        }
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();

        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if (newPredTokes.length == 2) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",", "_");
        System.out.println("Long name : " + longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres " + leftPre);
        for (String s : leftPres) {
            System.out.print(s + " ");
        }
        System.out.println();
        System.out.println("Right pres " + rightPre);
        for (String s : rightPres) {
            System.out.print(s + " ");
        }

        StringBuilder getFromB = new StringBuilder();
        StringBuilder getFromA = new StringBuilder();

        //testThreads(links);
        //LOG.info(ANSI_YELLOW + "Thread test lasted " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);
        boolean isEndpointALocal;
        boolean isEndpointBLocal;

        isEndpointALocal = isURLToLocalInstance(grConf.getEndpointA()); //"localhost" for localhost

        isEndpointBLocal = isURLToLocalInstance(grConf.getEndpointB()); //"localhost" for localhost

        for (String rightProp : rightPres) {
            //String[] leftPreTokens = leftPre.split(",");
            //String[] rightPreTokens = rightPre.split(",");
            String[] mainPattern = rightProp.split(",");

            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lstB);

            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);

            for (String pattern : patterns) {
                q.setLength(0);

                String[] rightPreTokens = pattern.split(",");

                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + rightPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                getFromB.append("SPARQL INSERT\n");
                getFromB.append("  { GRAPH <").append(grConf.getMetadataGraphB()).append("> {\n");
                prev_s = "<"+nodeA+">";
                if (grConf.isDominantA()) {
                    getFromB.append(" <"+nodeA+"> <" + rightPreTokens[0] + "> ?o0 . \n");
                } else {
                    getFromB.append(" <"+nodeB+"> <" + rightPreTokens[0] + "> ?o0 . \n");
                }
                prev_s = "?o0";
                for (int i = 1; i < rightPreTokens.length; i++) {
                    getFromB.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                getFromB.append("} }\nWHERE\n");
                getFromB.append("{\n");
                if (isEndpointALocal) {
                    getFromB.append(" GRAPH <").append(grConf.getGraphB()).append("> {\n");
                    getFromB.append(" <"+nodeB+"> <" + rightPreTokens[0] + "> ?o0 . \n");
                    prev_s = "?o0";
                    for (int i = 1; i < rightPreTokens.length; i++) {
                        getFromB.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    getFromB.append(" }\n");
                } else {
                    getFromB.append(" SERVICE <" + grConf.getEndpointB() + "> { GRAPH <").append(grConf.getGraphB()).append("> { \n");
                    getFromB.append(" <"+nodeB+"> <" + rightPreTokens[0] + "> ?o0 . \n");
                    prev_s = "?o0";
                    for (int i = 1; i < rightPreTokens.length; i++) {
                        getFromA.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    getFromB.append(" } }\n");
                }
                getFromB.append("}");

                System.out.println("LATE FETCH " + getFromB.toString());
                
                int tries = 0;
                startTime = System.nanoTime();
                while (tries < Constants.MAX_SPARQL_TRIES) {
                    try (PreparedStatement populateDataB = virt_conn.prepareStatement(getFromB.toString())) {

                        populateDataB.executeUpdate();

                    } catch (SQLException ex) {

                        LOG.trace("SQLException thrown during temp graph populating");
                        LOG.debug("SQLException thrown during temp graph populating Try : " + (tries + 1));
                        LOG.debug("SQLException thrown during temp graph populating : " + ex.getMessage());
                        LOG.debug("SQLException thrown during temp graph populating : " + ex.getSQLState());

                        tries++;
                    }
                }
                endTime = System.nanoTime();
                    
                LOG.info("Uploading B lasted "+Utilities.nanoToSeconds(endTime-startTime));
                    
            }
        }
        
        for (String leftProp : leftPres) {
            //String[] leftPreTokens = leftPre.split(",");
            //String[] rightPreTokens = rightPre.split(",");
            String[] mainPattern = leftProp.split(",");

            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lstA);

            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);

            for (String pattern : patterns) {
                q.setLength(0);

                String[] leftPreTokens = pattern.split(",");

                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + leftPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                getFromA.append("SPARQL INSERT\n");
                getFromA.append("  { GRAPH <").append(grConf.getMetadataGraphA()).append("> {\n");
                if (grConf.isDominantA()) {
                    getFromA.append(" <"+nodeA+"> <" + leftPreTokens[0] + "> ?o0 . \n");
                } else {
                    getFromA.append(" <"+nodeB+"> <" + leftPreTokens[0] + "> ?o0 . \n");
                }
                prev_s = "?o0";
                for (int i = 1; i < leftPreTokens.length; i++) {
                    getFromA.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                getFromA.append("} }\nWHERE\n");
                getFromA.append("{\n");
                if (isEndpointALocal) {
                    getFromA.append(" GRAPH <").append(grConf.getGraphA()).append("> {\n");
                    getFromA.append(" <"+nodeA+"> <" + leftPreTokens[0] + "> ?o0 . \n");
                    prev_s = "?o0";
                    for (int i = 1; i < leftPreTokens.length; i++) {
                        getFromA.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    getFromA.append(" }\n");
                } else {
                    getFromA.append(" SERVICE <" + grConf.getEndpointA() + "> { GRAPH <").append(grConf.getGraphA()).append("> { \n");
                    getFromA.append(" <"+nodeA+"> <" + leftPreTokens[0] + "> ?o0 . \n");
                    prev_s = "?o0";
                    for (int i = 1; i < leftPreTokens.length; i++) {
                        getFromA.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                        prev_s = "?o" + i;
                    }
                    getFromA.append(" } }\n");
                }
                getFromA.append("}");

                System.out.println("LATE FETCH " + getFromA.toString());
                
                // Populate with data from the Sample Liink set
                int tries = 0;
                startTime = System.nanoTime();
                while (tries < Constants.MAX_SPARQL_TRIES) {
                    try (PreparedStatement populateDataA = virt_conn.prepareStatement(getFromA.toString())) {

                        populateDataA.executeUpdate();

                    } catch (SQLException ex) {

                        LOG.trace("SQLException thrown during temp graph populating");
                        LOG.debug("SQLException thrown during temp graph populating Try : " + (tries + 1));
                        LOG.debug("SQLException thrown during temp graph populating : " + ex.getMessage());
                        LOG.debug("SQLException thrown during temp graph populating : " + ex.getSQLState());

                        tries++;
                    }
                }
                endTime = System.nanoTime();
                    
                LOG.info("Uploading A lasted "+Utilities.nanoToSeconds(endTime-startTime));
            }
        }
    }
    
    /**
     * Uppdates the specified remote graph.
     * FAGI uses a faster SPARQL 1.1 operation if the
     * process is happening locally
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @throws SQLException if an SQL error occurs
     */
    void UpdateRemoteEndpoint(GraphConfig grConf, VirtGraph vSet) throws SQLException {
        
        boolean isTargetEndpointLocal = Utilities.isURLToLocalInstance(grConf.getTargetGraph());
        
        if ( isTargetEndpointLocal ) {
            LocalUpdateGraphs(grConf, vSet);
        } else {
            SPARQLUpdateRemoteEndpoint(grConf, vSet);
        }
    }
    
    /**
     * Local update of theremote graph with SPARUL ADD
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @throws SQLException if an SQL error occurs
     */
    void LocalUpdateGraphs(GraphConfig grConf, VirtGraph vSet) throws VirtuosoException {
        String addNewTriples = "SPARQL ADD GRAPH <" + grConf.getTargetTempGraph() + "> TO GRAPH <" + grConf.getTargetGraph()+ ">";
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        VirtuosoPreparedStatement vstmt;
        vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(addNewTriples);
        
        vstmt.executeUpdate();
        
        vstmt.close();
    }
    
    /**
     * Remote update through concatenated SPARQL INSERTs
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @throws SQLException if an SQL error occurs
     * @throws VirtuosoException if an Virtuoso SQL error occurs
     */
    void SPARQLUpdateRemoteEndpoint(GraphConfig grConf, VirtGraph vSet) throws VirtuosoException, SQLException {
        String selectURITriples = "SPARQL SELECT * WHERE { GRAPH <"+grConf.getTargetTempGraph()+"> { ?s ?p ?o FILTER ( isURI ( ?o ) ) } }";
        String selectLiteralTriples = "SPARQL SELECT * WHERE { GRAPH <"+grConf.getTargetTempGraph()+"> { ?s ?p ?o FILTER ( isLiteral ( ?o ) ) } }";
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        VirtuosoPreparedStatement vstmt;
        vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(selectURITriples);
        VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery();
        
        boolean updating = true;
        int addIdx = 0;
        int cSize = 1;
        int sizeUp = 1;
        
        // As long as there is data this loop creates concatenated SPARQL INSERTs
        // to update the remote endpoint
        // Iy uses the SPARQL HTTP protocol for issuing SPARQL commands
        // on relies on HTTP Exceptions to reissue the inserts
        
        // Different loop for URIs to ease creation of query
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+grConf.getTargetGraph()+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <" + grConf.getTargetGraph()+"> {");

                if ( !vrs.next() )
                    break;
                
                for (int i = 0; i < cSize; i++) {
                    final String sub = vrs.getString(1);
                    final String pre = vrs.getString(2);
                    final String obj = vrs.getString(3);

                    queryStr.appendIri(sub);
                    queryStr.append(" ");
                    queryStr.appendIri(pre);
                    queryStr.append(" ");
                    queryStr.appendIri(obj); // !!!!! URI
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                    
                    if (!vrs.next()) {
                        updating = false;
                        break;
                    }
                }
                
                queryStr.append("} }");
                
                System.out.println("The insertion query takes this form "+queryStr.toString());
                
                cSize *= 2;
                
                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                insertRemoteB.execute();
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println(ex.getMessage());
                cSize = 0;
            }

        }
        
        vrs.close();
        vstmt.close();
        
        vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(selectLiteralTriples);
        vrs = (VirtuosoResultSet) vstmt.executeQuery();
        
        updating = true;
        addIdx = 0;
        cSize = 1;
        sizeUp = 1;
        
        // Different loop for Literal to ease creation of query
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+grConf.getTargetGraph()+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <" + grConf.getTargetGraph()+"> {");

                if ( !vrs.next() )
                    break;
                
                for (int i = 0; i < cSize; i++) {
                    final String sub = vrs.getString(1);
                    final String pre = vrs.getString(2);
                    final String obj = vrs.getString(3);

                    queryStr.appendIri(sub);
                    queryStr.append(" ");
                    queryStr.appendIri(pre);
                    queryStr.append(" ");
                    queryStr.appendLiteral(obj); // !!!!!! Literal
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                    
                    if (!vrs.next()) {
                        updating = false;
                        break;
                    }
                }
                
                queryStr.append("} }");
                
                System.out.println("The insertion query takes this form "+queryStr.toString());
                
                cSize *= 2;
                
                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                insertRemoteB.execute();
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println(ex.getMessage());
                cSize = 0;
            }

        }
        
        vrs.close();
        vstmt.close();
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private void handleMetadataFusion(String action, int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) {
        try {
            if (action.equals("Keep A")) {
                metadataKeepLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);

            }
            if (action.equals("Keep B")) {
                metadataKeepRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Both")) {
                //metadataKeepBoth(idx);
                metadataKeepRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
                metadataKeepLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Concatenated B")) {
                metadataKeepConcatRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Concatenated A")) {
                metadataKeepConcatLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Concatenated Both")) {
                metadataKeepConcatLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
                metadataKeepConcatRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Concatenation")) {
                metadataConcatenation(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Flattened B")) {
                metadataKeepFlatRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Flattened A")) {
                metadataKeepFlatLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
            if (action.equals("Keep Flattened Both")) {
                metadataKeepFlatLeft(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
                metadataKeepFlatRight(idx, nodeA, tGraph, sess, grConf, vSet, selectedFusions);
            }
        } catch (UnsupportedEncodingException ex) {
            LOG.trace("UnsupportedEncodingException thrown");
            LOG.debug("UnsupportedEncodingException thrown : " + ex.getMessage());
        } catch (SQLException ex) {
            LOG.trace("SQLException thrown");
            LOG.debug("SQLException thrown : " + ex.getMessage());
        }
    }
    
    private void clearPrevious(String[] l, String[] r, GraphConfig grConf, String nodeA, String tGraph) {
        System.out.println("\n\n\nCLEARING\n\n\n\n");
        //System.out.println("Left " + l);
        //System.out.println("Right " + r);
        
        if ( l == null && r == null ) {
            System.out.println("\n\n\nCLEARING\n\n\n\n");
            return;
        }
        
        ParameterizedSparqlString queryStr = null;
    
        if ( l != null ) {
            for (String s : l) {
                String[] toks = s.split(",");
                String prev_s = "<" + nodeA +">";
                int index = 0;
                
                queryStr = new ParameterizedSparqlString();
                queryStr.append("DELETE { GRAPH <" + tGraph + "> { ");
                for (String tok : toks) {
                    queryStr.append(prev_s);
                    queryStr.append(" ");
                    queryStr.appendIri(tok);
                    queryStr.append(" ");
                    queryStr.append("?o"+index);
                    queryStr.append(" ");
                    queryStr.append(". ");
                    prev_s = "?o"+index;
                    index++;
                }
                queryStr.append("} } WHERE { GRAPH <" + tGraph + "> { ");
                prev_s = "<" + nodeA +">";
                for (String tok : toks) {
                    queryStr.append(prev_s);
                    queryStr.append(" ");
                    queryStr.appendIri(tok);
                    queryStr.append(" ");
                    queryStr.append("?o"+index);
                    queryStr.append(" ");
                    queryStr.append(". ");
                    prev_s = "?o"+index;
                    index++;
                }
                queryStr.append("} }");
            }
        }
         
        if ( queryStr != null ) {
            System.out.println("Deletes A " + queryStr.toString());
            UpdateRequest q = queryStr.asUpdate();
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            UpdateProcessor delPrev = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
            delPrev.execute();
        }
        
        queryStr = null;
        
        if ( r != null ) {
            for (String s : r) {
                String[] toks = s.split(",");
                String prev_s = "<" + nodeA +">";
                int index = 0;
                
                queryStr = new ParameterizedSparqlString();
                queryStr.append("DELETE { GRAPH <" + tGraph + "> { ");
                for (String tok : toks) {
                    queryStr.append(prev_s);
                    queryStr.append(" ");
                    queryStr.appendIri(tok);
                    queryStr.append(" ");
                    queryStr.append("?o"+index);
                    queryStr.append(" ");
                    queryStr.append(". ");
                    prev_s = "?o"+index;
                    index++;
                }
                queryStr.append("} } WHERE { GRAPH <" + tGraph + "> { ");
                prev_s = "<" + nodeA +">";
                for (String tok : toks) {
                    queryStr.append(prev_s);
                    queryStr.append(" ");
                    queryStr.appendIri(tok);
                    queryStr.append(" ");
                    queryStr.append("?o"+index);
                    queryStr.append(" ");
                    queryStr.append(". ");
                    prev_s = "?o"+index;
                    index++;
                }
                queryStr.append("} }");
            }
        }  
        
        if ( queryStr != null ) {
            System.out.println("Deletes B " + queryStr.toString());
            UpdateRequest q = queryStr.asUpdate();
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            UpdateProcessor delPrev = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
            //delPrev.execute();
        }
        
        System.out.println("\n\n\nCLEARING\n\n\n\n");

    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataConcatenation(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lstA = (List<String>)sess.getAttribute("link_property_patternsA");
        List<String> lstB = (List<String>)sess.getAttribute("link_property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[0];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
        
        // Find Least Common Prefix
        int lcpIndexB = 0;
        String lcpPropertyB = "";
        Map<String, Map.Entry<Integer, Integer> > commonPres = new HashMap<>();
        for (String rightProp : rightPres) {
            String[] leftPreTokens = rightProp.split(",");
            String[] rightPreTokens = rightProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < rightPreTokens.length; i++) {
                if (!commonPres.containsKey(rightPreTokens[i])) {
                    commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(rightPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        for (Map.Entry entry : commonPres.entrySet()) {
            String[] rightPreTokens = rightPre.split(",");
            
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == rightPres.length ) {
                lcpIndexB = pos;
                lcpPropertyB = rightPreTokens[pos];
            }
        }
        
        System.out.println("L C P B : "+lcpIndexB + " : " + lcpPropertyB);
        
        int lcpIndexA = 0;
        String lcpPropertyA = "";
        commonPres = new HashMap<>();
        for (String leftProp : leftPres) {
            String[] leftPreTokens = leftProp.split(",");
            String[] rightPreTokens = leftProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < leftPreTokens.length; i++) {
                if (!commonPres.containsKey(leftPreTokens[i])) {
                    commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(leftPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        for (Map.Entry entry : commonPres.entrySet()) {
            String[] leftPreTokens = leftPre.split(",");
            
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == leftPres.length ) {
                lcpIndexA = pos;
                lcpPropertyA = leftPreTokens[pos];
            }
        }
        
        System.out.println("L C P A : "+lcpIndexA + " : " + lcpPropertyA);
        
        int lcpIndex = lcpIndexA;
        String lcpProperty = lcpPropertyA;
        if ( lcpIndexA > lcpIndexB ) {
            lcpIndex = lcpIndexB;
            lcpProperty = lcpPropertyB;
        }
        
        final HashMap<String, StringBuilder> newObjs = Maps.newHashMap();
        for (String rightProp : rightPres) {
            String[] mainPattern = rightProp.split(",");
            
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lstB);
            
            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);
            
            for (String pattern : patterns) {
                q.setLength(0);

                String[] rightPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + rightPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                q.append("SPARQL SELECT ?o" + (rightPreTokens.length - 1));
                prev_s = "<" + nodeA + ">";
                q.append(" WHERE {");
                q.append("\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
                for (int i = 0; i < rightPreTokens.length; i++) {
                    q.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("FILTER isLiteral("+prev_s+")} }");
                
                System.out.println(q.toString());
                PreparedStatement stmt;
                stmt = virt_conn.prepareStatement(q.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString("o" + (rightPreTokens.length - 1));
                    final String s = nodeA;

                    StringBuilder concat_str;
                    
                    concat_str = newObjs.get(s);
                    if (concat_str == null) {
                        concat_str = new StringBuilder();
                        newObjs.put(s, concat_str);
                    }
                    concat_str.append(o + " ");
                    
                    System.out.println("Subject " + s);
                    System.out.println("Object " + o);

                }
                System.out.println("Size " + newObjs.size());
            }
        }
        
        for (String leftProp : leftPres) {
            String[] mainPattern = leftProp.split(",");
            
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lstA);
            
            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);
            for (String pattern : patterns) {
                q.setLength(0);
                String[] leftPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + leftPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                q.append("SPARQL SELECT ?o" + (leftPreTokens.length - 1));
                prev_s = "<" + nodeA + ">";
                q.append(" WHERE {");
                q.append("\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
                for (int i = 0; i < leftPreTokens.length; i++) {
                    q.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("FILTER isLiteral("+prev_s+")} }");
                
                System.out.println(q.toString());
                PreparedStatement stmt;
                stmt = virt_conn.prepareStatement(q.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString("o" + (leftPreTokens.length - 1));
                    final String s = nodeA;
                    
                    //System.out.println("The object is " + o);
                    StringBuilder concat_str;
                    
                    concat_str = newObjs.get(s);
                    if (concat_str == null) {
                        concat_str = new StringBuilder();
                        newObjs.put(s, concat_str);
                    }
                    concat_str.append(o + " ");

                    //System.out.println("Subject " + s);
                    //System.out.println("Object " + o);

                    String simplified = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length - 1], "#");
                    if (simplified.equals("")) {
                        simplified = StringUtils.substring(leftPreTokens[leftPreTokens.length - 1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length - 1], "/") + 1);
                    }

                }
            }
        }
        
        String[] path = leftPres[0].split(",");
        StringBuilder q = new StringBuilder();
        System.out.println("Size " + newObjs.size());
        for (Map.Entry<String, StringBuilder> entry : newObjs.entrySet()) {
            String sub = entry.getKey();
            StringBuilder newObj = entry.getValue();
            newObj.setLength(newObj.length() - 1);
            q.setLength(0);
            q.append("INSERT { GRAPH <" + tGraph + "> { ");
            String prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append(prev_s + " <" + domOnto + newPred + "> \"" + newObj + "\" . ");
            q.append("} } WHERE {\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
            prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append("} }");
            System.out.println("Last query " + q.toString());
            
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepFlatLeft(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[0];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
                
        // Find Least Common Prefix
        int lcpIndex = 0;
        String lcpProperty = "";
        Map<String, Map.Entry<Integer, Integer> > commonPres = new HashMap<>();
        for (String leftProp : leftPres) {
            String[] leftPreTokens = leftProp.split(",");
            //String[] rightPreTokens = rightProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < leftPreTokens.length; i++) {
                if (!commonPres.containsKey(leftPreTokens[i])) {
                    commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(leftPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        String[] leftPreTokensF = leftPre.split(",");
        String[] rightPreTokensF = rightPre.split(",");
        List<String> commonPath = new ArrayList<>();
        for (Map.Entry entry : commonPres.entrySet()) {
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == leftPres.length ) {
                lcpIndex = pos;
                lcpProperty = leftPreTokensF[pos];
            }
        }
        
        System.out.println("L C P : "+lcpIndex + " : " + lcpProperty);
        
        for (String leftProp : leftPres) {
            //String[] leftPreTokens = rightProp.split(",");
            String[] mainPattern = leftProp.split(",");

            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lst);

            StringBuilder q = new StringBuilder();
            String prev_s = "";
            Set<String> set = new HashSet<>();
            Set<String> setFlat = new HashSet<>();

            System.out.println("Patterns " + patterns);

            for (String pattern : patterns) {
                String[] leftPreTokens = pattern.split(",");

                String simplified = "";
                if (leftPreTokens.length > mainPattern.length) {
                    simplified = "_" + StringUtils.substringAfter(leftPreTokens[leftPreTokens.length - 1], "#");
                    if (simplified.equals("")) {
                        simplified = "_" + StringUtils.substring(leftPreTokens[leftPreTokens.length - 1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length - 1], "/") + 1);
                    }
                }

                StringBuilder insq = new StringBuilder();
                insq.append("INSERT { GRAPH <" + grConf.getTargetTempGraph() + "> { ");

                prev_s = "<"+nodeA+">";
                for (int i = 0; i < leftPreTokens.length - 2; i++) {
                    insq.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                insq.append(prev_s + " <" + domOnto + newPred + simplified + "> " + "?o" + (leftPreTokens.length - 1) + "");
                insq.append(" } } WHERE {");
                insq.append("\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < leftPreTokens.length - 1; i++) {
                    insq.append(prev_s + " <" + leftPreTokens[i] + "> " + "?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                insq.append(prev_s + " <" + leftPreTokens[leftPreTokens.length - 1] + "> " + "?o" + (leftPreTokens.length - 1) + " . ");

                insq.append("} }");

                System.out.println(insq);

                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
            }
        }
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepFlatRight(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
        
        // Find Least Common Prefix
        int lcpIndex = 0;
        String lcpProperty = "";
        Map<String, Map.Entry<Integer, Integer> > commonPres = new HashMap<>();
        for (String rightProp : rightPres) {
            //String[] leftPreTokens = leftProp.split(",");
            String[] rightPreTokens = rightProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < rightPreTokens.length; i++) {
                if (!commonPres.containsKey(rightPreTokens[i])) {
                    commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(rightPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        String[] leftPreTokensF = leftPre.split(",");
        String[] rightPreTokensF = rightPre.split(",");
        List<String> commonPath = new ArrayList<>();
        for (Map.Entry entry : commonPres.entrySet()) {
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == rightPres.length ) {
                lcpIndex = pos;
                lcpProperty = rightPreTokensF[pos];
            }
        }
        
        System.out.println("L C P : "+lcpIndex + " : " + lcpProperty);
        
        for (String rightProp : rightPres) {
            String[] leftPreTokens = rightProp.split(",");
            String[] mainPattern = rightProp.split(",");

            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lst);

            StringBuilder q = new StringBuilder();
            String prev_s = "";
            Set<String> set = new HashSet<>();
            Set<String> setFlat = new HashSet<>();

            System.out.println("Patterns " + patterns);

            for (String pattern : patterns) {
                set.clear();
                setFlat.clear();
                String[] rightPreTokens = pattern.split(",");

                String simplified = "";
                if (rightPreTokens.length > mainPattern.length) {
                    simplified = "_" + StringUtils.substringAfter(rightPreTokens[rightPreTokens.length - 1], "#");
                    if (simplified.equals("")) {
                        simplified = "_" + StringUtils.substring(rightPreTokens[rightPreTokens.length - 1], StringUtils.lastIndexOf(rightPreTokens[rightPreTokens.length - 1], "/") + 1);
                    }
                }
                StringBuilder insq = new StringBuilder();
                insq.append("INSERT { GRAPH <" + grConf.getTargetTempGraph() + "> { ");

                prev_s = "<"+nodeA+">";
                for (int i = 0; i < mainPattern.length - 2; i++) {
                    insq.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                insq.append(prev_s + " <" + domOnto + newPred + simplified + "> " + "?o" + (rightPreTokens.length - 1) + "");
                insq.append(" } } WHERE {");
                insq.append("\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
                //insq.append("} } WHERE {\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < rightPreTokens.length - 1; i++) {
                    insq.append(prev_s + " <" + rightPreTokens[i] + "> " + "?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                insq.append(prev_s + " <" + rightPreTokens[rightPreTokens.length - 1] + "> " + "?o" + (rightPreTokens.length - 1) + " . ");

                insq.append("} }");
                System.out.println(insq);

                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
            }
        }
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepConcatLeft(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[0];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
        
        // Find Least Common Prefix
        int lcpIndex = 0;
        String lcpProperty = "";
        Map<String, Map.Entry<Integer, Integer> > commonPres = new HashMap<>();
        for (String leftProp : leftPres) {
            String[] leftPreTokens = leftProp.split(",");
            String[] rightPreTokens = leftProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < leftPreTokens.length; i++) {
                if (!commonPres.containsKey(leftPreTokens[i])) {
                    commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(leftPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(leftPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        for (Map.Entry entry : commonPres.entrySet()) {
            String[] leftPreTokens = leftPre.split(",");
            String[] rightPreTokens = rightPre.split(",");
            
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == leftPres.length ) {
                lcpIndex = pos;
                lcpProperty = leftPreTokens[pos];
            }
        }
        
        System.out.println("L C P : "+lcpIndex + " : " + lcpProperty);
        
        final HashMap<String, StringBuilder> newObjs = Maps.newHashMap();
        for (String leftProp : leftPres) {
            //String[] leftPreTokens = leftPre.split(",");
            //String[] rightPreTokens = rightPre.split(",");
            String[] mainPattern = leftProp.split(",");
            
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lst);
            
            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);
            for (String pattern : patterns) {
                q.setLength(0);
                String[] leftPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + leftPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                q.append("SPARQL SELECT ?o" + (leftPreTokens.length - 1));
                prev_s = "<"+nodeA+">";
                q.append(" WHERE {");
                q.append("\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
                for (int i = 0; i < leftPreTokens.length; i++) {
                    q.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("FILTER isLiteral("+prev_s+") } }");
                
                System.out.println(q.toString());
                PreparedStatement stmt;
                stmt = virt_conn.prepareStatement(q.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString("o" + (leftPreTokens.length - 1));
                    String s = nodeA;
                    
                    System.out.println("The object is " + o);
                    StringBuilder concat_str;
                    
                    concat_str = newObjs.get(s);
                    if (concat_str == null) {
                        concat_str = new StringBuilder();
                        newObjs.put(s, concat_str);
                    }
                    concat_str.append(o + " ");

                    System.out.println("Subject " + s);
                    System.out.println("Object " + o);

                    String simplified = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length - 1], "#");
                    if (simplified.equals("")) {
                        simplified = StringUtils.substring(leftPreTokens[leftPreTokens.length - 1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length - 1], "/") + 1);
                    }

                }
            }
        }
        
        String[] path = leftPres[0].split(",");
        for (Map.Entry<String, StringBuilder> entry : newObjs.entrySet()) {
            String sub = entry.getKey();
            StringBuilder newObj = entry.getValue();
            newObj.setLength(newObj.length() - 1);
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <" + grConf.getTargetTempGraph() + "> { ");
            String prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append(prev_s + " <" + domOnto + newPred + "> \"" + newObj + "\"");
            q.append("} } WHERE {\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
            prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append("} }");
            System.out.println("Last query " + q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
    
    private void metadataKeepConcatBoth(int idx) {
        // This is just a dummy funtion since 
        // Keep Concatenated Both can eaosily be
        // implemented using Left and right Concatenation 
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepConcatRight(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        System.out.println("Short name : " + name + newPred );
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
        
        // Find Least Common Prefix
        int lcpIndex = 0;
        String lcpProperty = "";
        Map<String, Map.Entry<Integer, Integer> > commonPres = new HashMap<>();
        for (String rightProp : rightPres) {
            String[] leftPreTokens = rightProp.split(",");
            String[] rightPreTokens = rightProp.split(",");

            // Find LCP (Least Common Prefix)
            for (int i = 0; i < rightPreTokens.length; i++) {
                if (!commonPres.containsKey(rightPreTokens[i])) {
                    commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, 1));
                } else {
                    Map.Entry retValue = commonPres.get(rightPreTokens[i]);
                    int pos = (Integer) retValue.getKey();
                    int val = (Integer) retValue.getValue();
                    if (pos > i) {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(i, val + 1));
                    } else {
                        commonPres.put(rightPreTokens[i], new AbstractMap.SimpleEntry<>(pos, val + 1));
                    }
                }
            }
        }
        
        for (Map.Entry entry : commonPres.entrySet()) {
            String[] leftPreTokens = leftPre.split(",");
            String[] rightPreTokens = rightPre.split(",");
            
            AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) entry.getValue();
            int pos = (Integer) e.getKey();
            int val = (Integer) e.getValue();
            System.out.println("Val : "+val);
            if ( val == rightPres.length ) {
                lcpIndex = pos;
                lcpProperty = rightPreTokens[pos];
            }
        }
        
        System.out.println("L C P : "+lcpIndex + " : " + lcpProperty);
        
        final HashMap<String, StringBuilder> newObjs = Maps.newHashMap();
        for (String rightProp : rightPres) {
            //String[] leftPreTokens = leftPre.split(",");
            //String[] rightPreTokens = rightPre.split(",");
            String[] mainPattern = rightProp.split(",");
            
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lst);
            
            StringBuilder q = new StringBuilder();
            String prev_s = "";
            System.out.println("Patterns " + patterns);
            
            for (String pattern : patterns) {
                q.setLength(0);

                String[] rightPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + rightPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                q.append("SPARQL SELECT ?o" + (rightPreTokens.length - 1));
                prev_s = "<"+nodeA+">";
                q.append(" WHERE {");
                q.append("\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
                for (int i = 0; i < rightPreTokens.length; i++) {
                    q.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("FILTER isLiteral("+prev_s+")} }");
                
                System.out.println(q.toString());
                PreparedStatement stmt;
                stmt = virt_conn.prepareStatement(q.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString("o" + (rightPreTokens.length - 1));
                    String s = nodeA;

                    StringBuilder concat_str;
                    
                    concat_str = newObjs.get(s);
                    if (concat_str == null) {
                        concat_str = new StringBuilder();
                        newObjs.put(s, concat_str);
                    }
                    concat_str.append(o + " ");
                    
                    System.out.println("Subject " + s);
                    System.out.println("Object " + o);

                }
                System.out.println("Size " + newObjs.size());
            }
        }
        
        String[] path = rightPres[0].split(",");
        for (Map.Entry<String, StringBuilder> entry : newObjs.entrySet()) {
            String sub = entry.getKey();
            StringBuilder newObj = entry.getValue();
            newObj.setLength(newObj.length() - 1);
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <" + tGraph + "> { ");
            String prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append(prev_s + " <" + domOnto + newPred + "> \"" + newObj + "\"");
            q.append("} } WHERE {\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
            prev_s = "<" + sub + ">";
            for (int i = 0; i < lcpIndex; i++) {
                q.append(prev_s + " <" + path[i] + "> ?o" + i + " . ");
                prev_s = "?o" + i;
            }
            q.append("} }");
            System.out.println("Last query " + q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
        
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepLeft(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[0];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        clearPrevious(leftPres, rightPres, grConf, nodeA, tGraph);
        
        for (String leftProp : leftPres) {
            String[] mainPattern = leftProp.split(",");
                
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(leftProp, lst);  
            
            for (String pattern : patterns) {
                String[] leftPreTokens = pattern.split(",");
                StringBuilder q = new StringBuilder();
                q.append("INSERT { GRAPH <" + grConf.getTargetTempGraph() + "> { ");
                String prev_s = "<"+ nodeA +">";
                for (int i = 0; i < mainPattern.length - 1; i++) {
                    q.append(prev_s + " <" + mainPattern[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append(prev_s + " <" + domOnto + newPred + "> ?o" + (mainPattern.length - 1) + " . ");
                
                prev_s = "?o" + (mainPattern.length - 1);
                for (int i = mainPattern.length; i < leftPreTokens.length; i++) {
                    q.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                
                prev_s = "<"+ nodeA +">";
                q.append("} } WHERE {\n GRAPH <" + grConf.getMetadataGraphA() + "> {");
                for (int i = 0; i < leftPreTokens.length; i++) {
                    q.append(prev_s + " <" + leftPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("} }");
                System.out.println(q.toString());
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
                vur.exec();
            }
        }
    }

    private void metadataKeepBoth(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) {
        // Dummy call for meatadata Keep Both
    }
    
    /**
     * Function that handles metadata fusion
     * based on the selected action. A lot could be done
     * to optimize this single link version but
     * since this is relatively fast, the process is
     * implemented similarly to the batch fusion model for cinsistency
     *
     * @param action String represantation of the selected action
     * @param response servlet response
     * @throws SQLException if a servlet-specific error occurs
     * @throws UnsupportedEncodingException if an I/O error occurs
     */
    private void metadataKeepRight(int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        String domOnto = "";
        List<String> lst = (List<String>)sess.getAttribute("link_property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        
        // OSM has some values with URL encoded symbols
        // We could possibly fix these "errors" by decoding
        String name = URLDecoder.decode(selectedFusions[idx].getPre(), "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].getPreL();
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split(Constants.PROPERTY_SEPARATOR);
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split(Constants.PROPERTY_SEPARATOR);
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        leftPre = StringUtils.removeEnd(leftPre, "|");
        rightPre = StringUtils.removeEnd(rightPre, "|");
        String[] leftPres = leftPre.split("\\|");
        String[] rightPres = rightPre.split("\\|");
        System.out.println("Left pres "+leftPre);
        for ( String s : leftPres )
            System.out.print(s + " ");
        System.out.println();
        System.out.println("Right pres "+rightPre);   
        for ( String s : rightPres )
            System.out.print(s + " ");
        
        // Loop over every selected property
        for (String rightProp : rightPres) {
            String[] mainPattern = rightProp.split(",");
                
            List<String> patterns = Utilities.findCommonPrefixedPropertyChains(rightProp, lst);
            // Loop over every property chain that contains
            // a prefix of the selected properties
            for (String pattern : patterns) {
                String[] rightPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + rightPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                StringBuilder q = new StringBuilder();
                q.append("INSERT { GRAPH <" + grConf.getTargetTempGraph() + "> { ");
                String prev_s = "<"+ nodeA +">";
                for (int i = 0; i < mainPattern.length - 1; i++) {
                    q.append(prev_s + " <" + mainPattern[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append(prev_s + " <" + domOnto + newPred + "> ?o" + (mainPattern.length - 1) + " . ");
                
                prev_s = "?o" + (mainPattern.length - 1);
                for (int i = mainPattern.length; i < rightPreTokens.length; i++) {
                    q.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                
                prev_s = "<"+ nodeA +">";
                q.append("} } WHERE {");
                q.append("\n GRAPH <" + grConf.getMetadataGraphB() + "> {");
                for (int i = 0; i < rightPreTokens.length; i++) {
                    q.append(prev_s + " <" + rightPreTokens[i] + "> ?o" + i + " . ");
                    prev_s = "?o" + i;
                }
                q.append("} }");
                System.out.println(q.toString());
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
                vur.exec();
            }
        }
    }
    
    // Update geometries in PostGIS to account for Map transformation
    private void updateGeom( String subA, String subB, String geomA, String geomB , Connection dbConn) {
        final String qA = "UPDATE dataset_a_geometries SET geom = ST_GeomFromText(?, 4326) WHERE subject = ?";
        final String qB = "UPDATE dataset_b_geometries SET geom = ST_GeomFromText(?, 4326) WHERE subject = ?";
        
        PreparedStatement stmt = null;
        try {
            stmt = dbConn.prepareStatement(qA);

            stmt.setString(1, geomA);
            stmt.setString(2, subA);
            stmt.executeUpdate();

            stmt.close();

            stmt = dbConn.prepareStatement(qB);
            stmt.setString(1, geomB);
            stmt.setString(2, subB);
            stmt.executeUpdate();

            stmt.close();

            dbConn.commit();
        } catch (SQLException sqlex) {
            LOG.trace("SQLException thrown during Geometry Update");
            LOG.debug("SQLException thrown during Geometry Update : " + sqlex.getMessage());
            LOG.debug("SQLException thrown during Geometry Update : " + sqlex.getSQLState());
            if (dbConn != null) {
                try {
                    dbConn.rollback();
                } catch (SQLException ex1) {
                    LOG.trace("SQLException thrown during rollback");
                    LOG.debug("SQLException thrown during rollback : " + ex1.getMessage());
                    LOG.debug("SQLException thrown during rollback : " + ex1.getSQLState());
                }
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown");
                    LOG.debug("SQLException thrown : " + ex.getMessage());
                    LOG.debug("SQLException thrown : " + ex.getSQLState());
                }
            }
        }
        
    }
    
    private void executeVirtuosoUpdate(String updateQuery, VirtGraph vSet) {                
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, vSet);
        
        //VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (query, set);
        //vqe.execSelect();
        //update_handler.addUpdate(updateQuery);
        vur.exec();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            processRequest(request, response);
        } finally {
            
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {        
            processRequest(request, response);
        } finally {
            
        }
        
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
