/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
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
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
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
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 *
 * @author nick
 */
@WebServlet(name = "FuseLinkServlet", urlPatterns = {"/FuseLinkServlet"})
public class FuseLinkServlet extends HttpServlet {

    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";
    private static final String LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    private static final String DB_URL = "jdbc:postgresql:";

    private class JSONFusions {
        List<JSONPropertyFusion> fusions;

        public JSONFusions(List<JSONPropertyFusion> fusions) {
            this.fusions = fusions;
        }

                
        public JSONFusions() {
            fusions = new ArrayList<>();
        }

        public List<JSONPropertyFusion> getFusions() {
            return fusions;
        }

        public void setFusions(List<JSONPropertyFusion> fusions) {
            this.fusions = fusions;
        }
        
    }
    
    private static class JSONShiftFactors {
        Float shift;
        Float scaleFact;
        Float rotateFact;

        public JSONShiftFactors() {
        }

        public Float getShift() {
            return shift;
        }

        public void setShift(Float shift) {
            this.shift = shift;
        }

        public Float getScaleFact() {
            return scaleFact;
        }

        public void setScaleFact(Float scaleFact) {
            this.scaleFact = scaleFact;
        }

        public Float getRotateFact() {
            return rotateFact;
        }

        public void setRotateFact(Float rotateFact) {
            this.rotateFact = rotateFact;
        }
        
    }
    
    private static class JSONPropertyFusion {
        String valA;
        String pre;
        String preL;
        String valB;
        String action;

        public JSONPropertyFusion() {
        }

        public String getValA() {
            return valA;
        }

        public void setValA(String valA) {
            this.valA = valA;
        }

        public String getPre() {
            return pre;
        }

        public void setPre(String pre) {
            this.pre = pre;
        }

        public String getPreL() {
            return preL;
        }

        public void setPreL(String preL) {
            this.preL = preL;
        }

        public String getValB() {
            return valB;
        }

        public void setValB(String valB) {
            this.valB = valB;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
        
    }
    
    private class JSONFusionResult {
        String geom;
        String na;
        String nb;

        public JSONFusionResult() {
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

        public String getNa() {
            return na;
        }

        public void setNa(String na) {
            this.na = na;
        }

        public String getNb() {
            return nb;
        }

        public void setNb(String nb) {
            this.nb = nb;
        }
        
    }
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
            throws ServletException, IOException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Per reqiest state
        PrintWriter out = response.getWriter();
        JSONFusionResult        ret = new JSONFusionResult();
        DBConfig                dbConf;
        GraphConfig             grConf;
        VirtGraph               vSet = null;
        PreparedStatement       stmt = null;
        Connection              dbConn = null;
        ResultSet               rs = null;
        List<FusionState>       fs = null;
        String                  tGraph = null;
        String                  nodeA = null;
        String                  nodeB = null;
        String                  dom = null;
        String                  domSub = null;
        HttpSession             sess = null;
        JSONPropertyFusion[]    selectedFusions;

        try {
            sess = request.getSession(false);
            
            if ( sess == null ) {
                return;
            }

            String classParam = request.getParameter("classes");
            String[] classes = request.getParameterValues("classes[]");
            
            if ( classes == null && classParam == null ) {
                out.print("{}");
                
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
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
                String url = DB_URL.concat(dbConf.getDBName());
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
            
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jp = factory.createJsonParser(propsJSON);
            selectedFusions = mapper.readValue(jp, JSONPropertyFusion[].class );

            JsonParser jp2 = factory.createJsonParser(shiftJSON);
            JSONShiftFactors sFactors = mapper.readValue(jp2, JSONShiftFactors.class );
            
            if ( sFactors != null ) {
                System.out.println(sFactors.shift);
                System.out.println(sFactors.scaleFact);
                System.out.println(sFactors.rotateFact);
            }
            
            System.out.println(propsJSON);
            System.out.println("Shift JSON "+shiftJSON);
            for (JSONPropertyFusion pf : selectedFusions ) {
                System.out.println(pf.valA);
                System.out.println(pf.valB);
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
                        queryStr.appendIri(TYPE);
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.append(".");
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.appendIri(TYPE);
                        queryStr.append(" ");
                        queryStr.appendIri(OWL_CLASS);
                        queryStr.append(".");
                        queryStr.append(" ");
                        queryStr.appendIri(domOnto + c);
                        queryStr.append(" ");
                        queryStr.appendIri(LABEL);
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
                
                trans = FuserPanel.transformations.get(selectedFusions[0].action);
                if ( trans instanceof ShiftAToB) {
                    ((ShiftAToB)trans).setShift(sFactors.shift);
                    ((ShiftAToB)trans).setRotate(sFactors.rotateFact);
                    ((ShiftAToB)trans).setScale(sFactors.scaleFact);
                }
            
                if ( trans instanceof ShiftBToA) {
                    ((ShiftBToA)trans).setShift(sFactors.shift);
                    ((ShiftBToA)trans).setRotate(sFactors.rotateFact);
                    ((ShiftBToA)trans).setScale(sFactors.scaleFact);
                }
                
                updateGeom(nodeA, nodeB, selectedFusions[0].valA, selectedFusions[0].valB, dbConn);
                        
                if ( trans instanceof KeepRightTransformation) {
                    String q = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valB);
                    System.out.println("Query Right "+q);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.geom = selectedFusions[0].valB;
                    break;
                }
                
                if ( trans instanceof KeepLeftTransformation) {
                    String q = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valA);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.geom = selectedFusions[0].valA;
                    break;
                }
                
                if ( trans instanceof KeepBothTransformation) {
                    String qA = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valA);
                    String qB = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valB);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(qA, vSet);
                    vur.exec();
                    vur = VirtuosoUpdateFactory.create(qB, vSet);
                    vur.exec();
                    
                    ret.geom = "GEOMETRYCOLLECTION("+selectedFusions[0].valA+", "+selectedFusions[0].valB+")";

                    break;
                }
                
                if ( trans instanceof Concatenation) {
                    String qA = formInsertConcatGeomQuery(tGraph, domSub, selectedFusions[0].valA, selectedFusions[0].valB);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(qA, vSet);
                    vur.exec();
                    
                    ret.geom = "GEOMETRYCOLLECTION("+selectedFusions[0].valA+", "+selectedFusions[0].valB+")";
                    
                    break;
                }
                
                System.out.println(trans == null);
                
                trans.fuse(dbConn, nodeA, nodeB);
                
                VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
                virtImp.setTransformationID(trans.getID());
            
                virtImp.importGeometriesToVirtuoso((String)sess.getAttribute("t_graph"));
            
                virtImp.trh.finish();
            
                String queryGeoms = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(b.geom) as g\n" +
                                 "FROM links INNER JOIN fused_geometries AS b\n" +
                                 "ON (b.subject_a = ?)";
                //System.out.println("With subject "+nodeA);
                //System.out.println("With subject "+nodeB);
                stmt = dbConn.prepareStatement(queryGeoms);
                stmt.setString(1, nodeA);
                rs = stmt.executeQuery();
            
                if(rs.next()) {
                    ret.geom = rs.getString(3);
                    //System.out.println("Returning geom : "+ret.geom);
                }
                //System.out.println(queryGeoms);
            
                break;
            }
            
            
            for(int i = 1; i < selectedFusions.length; i++) {
                handleMetadataFusion(selectedFusions[i].action, i, nodeA, tGraph, sess, grConf, vSet, selectedFusions );
            }
            
            //System.out.println("JSON Geometry "+mapper.writeValueAsString(ret));
            /* TODO output your page here. You may use following sample code. */
            out.println(mapper.writeValueAsString(ret));
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if ( vSet != null ) {
                vSet.close();
            }
            
            out.close();
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
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private void handleMetadataFusion(String action, int idx, String nodeA, String tGraph, HttpSession sess, GraphConfig grConf, VirtGraph vSet, JSONPropertyFusion[] selectedFusions) throws SQLException, UnsupportedEncodingException {
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
        List<String> lstA = (List<String>)sess.getAttribute("property_patternsA");
        List<String> lstB = (List<String>)sess.getAttribute("property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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
            
            List<String> patterns = findChains(rightProp, lstB);
            
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
                    String o = rs.getString(2);
                    final String s = nodeA;
;
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
            
            List<String> patterns = findChains(leftProp, lstA);
            
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
                
                q.append("} }");
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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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

            List<String> patterns = findChains(leftProp, lst);

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
                insq.append("INSERT { GRAPH <" + tGraph + "> { ");

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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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

            List<String> patterns = findChains(rightProp, lst);

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
                insq.append("INSERT { GRAPH <" + tGraph + "> { ");

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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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
            
            List<String> patterns = findChains(leftProp, lst);
            
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
                q.append("FILTER isLiteral("+prev_s+")");
                
                q.append("} }");
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
            q.append("INSERT { GRAPH <" + tGraph + "> { ");
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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        System.out.println("Short name : " + name + newPred );
        String[] predicates = longName.split("=>");
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
            
            List<String> patterns = findChains(rightProp, lst);
            
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
                    String o = rs.getString(2);
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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsA");
        
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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
                
            List<String> patterns = findChains(leftProp, lst);  
            
            for (String pattern : patterns) {
                String[] leftPreTokens = pattern.split(",");
                StringBuilder q = new StringBuilder();
                q.append("INSERT { GRAPH <" + tGraph + "> { ");
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
        List<String> lst = (List<String>)sess.getAttribute("property_patternsB");

        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        
        // OSM has some values with URL encoded symbols
        // We could possibly fix these "errors" by decoding
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        //String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        String longName = selectedFusions[idx].preL;
        
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        newPred = newPred.replaceAll(",","_");
        System.out.println("Long name : "+longName);
        String[] predicates = longName.split("=>");
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
                
            List<String> patterns = findChains(rightProp, lst);
            // Loop over every property chain that contains
            // a prefix of the selected properties
            for (String pattern : patterns) {
                String[] rightPreTokens = pattern.split(",");
                
                System.out.println("Pattern : " + pattern);
                System.out.println("Right Tokens : " + rightPreTokens.length);
                System.out.println("Main Pattern : " + mainPattern.length);
                
                StringBuilder q = new StringBuilder();
                q.append("INSERT { GRAPH <" + tGraph + "> { ");
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
    
    //private class RDF
    private void updateGeom( String subA, String subB, String geomA, String geomB , Connection dbConn) throws SQLException {
        final String qA = "UPDATE dataset_a_geometries SET geom = ST_GeomFromText(?, 4326) WHERE subject = ?";
        final String qB = "UPDATE dataset_b_geometries SET geom = ST_GeomFromText(?, 4326) WHERE subject = ?";
        
        PreparedStatement stmt = dbConn.prepareStatement(qA);
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
        
    }
    
    private List<String> findChains(String p, List<String> lst) {
        List<String> ret = new ArrayList<>();
        
        for ( String s : lst) {
            //if ( ( s.compareTo(p) != 0 ) && s.startsWith(p) ) 
            if ( s.startsWith(p) ) 
                ret.add(s);
        }
        
        return ret;
    }
    
    private String formInsertQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
    
    private String formInsertGeomQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> <" + subject + "_geom> . <" + subject +"_geom> <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry> }";
    }
    
    private String formInsertConcatGeomQuery(String tGraph, String subject, String geomA, String geomB) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> <" + subject + "_geom> . <" + subject +"_geom> <" + WKT + "> \"" + "GEOMETRYCOLLECTION("+geomA+", "+geomB+")" + "\"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry> }";
    }
    
    private String formInsertQuery(String tGraph, String subject, String predicate, String object){
        return "WITH <" + tGraph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
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
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (SQLException ex) {
            Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
