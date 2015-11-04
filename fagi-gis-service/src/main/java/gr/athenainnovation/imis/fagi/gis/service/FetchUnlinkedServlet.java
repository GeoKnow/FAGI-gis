/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONBArea;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONUnlinkedEntities;
import gr.athenainnovation.imis.fusion.gis.json.JSONUnlinkedEntity;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.Patterns;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "FetchUnlinkedServlet", urlPatterns = {"/FetchUnlinkedServlet"})
public class FetchUnlinkedServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(FetchUnlinkedServlet.class);    
        
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
        
        // [er sesssion state
        HttpSession                 sess;
        GraphConfig                 grConf;
        DBConfig                    dbConf;
        JSONRequestResult           res;
        JSONUnlinkedEntities        ret;    
        JSONBArea                   BBox;
        ObjectMapper                mapper = new ObjectMapper();
        PrintWriter                 out = null;
                
        response.setContentType("text/html;charset=UTF-8");
        
        try {
            out = response.getWriter();
            
            ret = new JSONUnlinkedEntities();
            res = new JSONRequestResult();
            ret.setResult(res);
            
            sess = request.getSession(false);
            
            if (sess == null ) {
                LOG.trace("No active session found");
                LOG.debug("No active session found");
                res.setStatusCode(-1);
                res.setMessage("Invalid session");

                out.print(mapper.writeValueAsString(ret));
                
                return;
            }
            
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            
            /* TODO output your page here. You may use following sample code. */
            String bboxJSON = request.getParameter("bboxJSON");
            String queryA = request.getParameter("queryA");
            String queryB = request.getParameter("queryB");
            
            if ( bboxJSON == null || queryA == null || queryB == null ) {
                LOG.trace("Invalid Session parameters");
                LOG.debug("Invalid Session parameters");
                res.setStatusCode(-1);
                res.setMessage("Invalid session");

                out.print(mapper.writeValueAsString(ret));
                
                return;
            }
            
            if ( !queryA.isEmpty() ) {
                queryA = normalizeQuery(queryA);
                customFetchGeoms(queryA, ret.getEntitiesA(), grConf.getGraphA(), grConf.getEndpointA(), sess );
            }
            if ( !queryB.isEmpty() ) {
                queryB = normalizeQuery(queryB);
                customFetchGeoms(queryB, ret.getEntitiesB(), grConf.getGraphB(), grConf.getEndpointB(), sess );
                
                out.print(mapper.writeValueAsString(ret));
                
                return;
            }
            
            System.out.println("Properties JSON "+bboxJSON);
            System.out.println("Qyery A "+queryA);
            System.out.println("Query B "+queryA);
            
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jsParser = factory.createJsonParser(bboxJSON);
            BBox = mapper.readValue(jsParser, JSONBArea.class );
            
            grConf.scanGeoProperties();
            
            List<String> geoPropsA = grConf.getGeoPropertiesA();
            List<String> geoPropsB = grConf.getGeoPropertiesB();
            List<String> geoTypesA = grConf.getGeoTypesA();
            List<String> geoTypesB = grConf.getGeoTypesB();
                        
            for (int i = 0; i < geoPropsA.size(); i++ ) {
                String p = geoPropsA.get(i);
                String t = geoTypesA.get(i);
                fetchGeoms(ret.getEntitiesA(), grConf.getGraphA(), grConf.getEndpointA(), p, t, BBox, sess);
            }
            
            for (int i = 0; i < geoPropsB.size(); i++ ) {
                String p = geoPropsB.get(i);
                String t = geoTypesB.get(i);
                fetchGeoms(ret.getEntitiesB(), grConf.getGraphB(), grConf.getEndpointB(), p, t, BBox, sess);
            }
            
            //System.out.println("Ret JSON "+mapper.writeValueAsString(ret));
            out.print(mapper.writeValueAsString(ret));
        } catch ( IOException ex) {
            
        }
    }

    private void customFetchGeoms(String q, List<JSONUnlinkedEntity> l, String graph, String service, HttpSession sess) {
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh = new QueryEngineHTTP(service, q, authenticator);
        qeh.addDefaultGraph(graph);
        QueryExecution queryExecution = qeh;
        final ResultSet resultSet = queryExecution.execSelect();
        
        HashSet<String> fetchedGeomsA = (HashSet<String>) sess.getAttribute("fetchedGeomsA");
        HashSet<String> fetchedGeomsB = (HashSet<String>) sess.getAttribute("fetchedGeomsB");

        if (fetchedGeomsA == null) {
            fetchedGeomsA = new HashSet<>();
            sess.setAttribute("fetchedGeomsA", fetchedGeomsA);
        }

        if (fetchedGeomsB == null) {
            fetchedGeomsB = new HashSet<>();
            sess.setAttribute("fetchedGeomsB", fetchedGeomsB);
        }
    
        System.out.println("Fetched from A : " + fetchedGeomsA.size());
        System.out.println("Fetched from B : " + fetchedGeomsB.size());
        
        int newGeom = 0;
        while ( resultSet.hasNext() ) {
            final QuerySolution querySolution = resultSet.next();
                    
            final String geo = querySolution.getLiteral("?geometry").getString();
            final String sub = querySolution.getResource("?subject").getURI();
            
            if ( fetchedGeomsA.contains(sub) )
                continue;
            
            fetchedGeomsA.add(sub);
            newGeom++;
            
            l.add(new JSONUnlinkedEntity(geo, sub));
            //System.out.println("Fetched "+geo);
        }
        
        queryExecution.close();
    }

    private boolean fetchGeoms(List<JSONUnlinkedEntity> l, String graph, String service, String p, String t, JSONBArea BBox, HttpSession sess) {
        boolean success = true;
        StringBuilder geoQuery = new StringBuilder();
        
        //System.out.println(BBox.getRight());
        //System.out.println(BBox.getLeft());
        //System.out.println(BBox.getTop());
        //System.out.println(BBox.getBottom());
        if ( t.equalsIgnoreCase("POLYGONaa") ) {
            geoQuery.append("SELECT ?s ?geo WHERE {\n"
                    + "?s <"+p+"> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
            if (BBox.getLeft() < 0) {
                geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + Constants.MERC_X_MAX + " )  < " + (BBox.getRight() + Constants.MERC_X_MAX) + ")\n"
                        + "FILTER ( ( bif:st_xmax(?geo) + " + Constants.MERC_X_MAX + " ) > " + (BBox.getLeft() + Constants.MERC_X_MAX) + ")\n");
            } else {
                geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + Constants.MERC_X_MAX + " )  < " + (BBox.getRight() + Constants.MERC_X_MAX) + ")\n"
                        + "FILTER ( ( bif:st_xmax(?geo) + " + Constants.MERC_X_MAX + " ) > " + (BBox.getLeft() + Constants.MERC_X_MAX) + ")\n");
            }
            if (BBox.getBottom() < 0) {
                geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Constants.MERC_Y_MAX + " ) > " + (BBox.getTop() + Constants.MERC_Y_MAX) + ")\n"
                        + "FILTER ( ( bif:st_ymax(?geo) + " + Constants.MERC_Y_MAX + " ) < " + (BBox.getBottom() + Constants.MERC_Y_MAX) + ")\n");
            } else {
                geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Constants.MERC_Y_MAX + " ) < " + (BBox.getTop() + Constants.MERC_Y_MAX) + ")\n"
                        + "FILTER ( ( bif:st_ymax(?geo) + " + Constants.MERC_Y_MAX + " ) > " + (BBox.getBottom() + Constants.MERC_Y_MAX) + ")\n");
            }
            geoQuery.append("}");
        } else {
            geoQuery.append("SELECT ?s ?geo\n" +
                               "WHERE {\n" +
                               "?s <"+p+"> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n" +
                               "FILTER (bif:st_intersects (?geo, bif:st_geomfromtext(\""+BBox.getBarea()+"\"), 0))\n" +
                                "}");
        }
        System.out.println("Geom query "+geoQuery);
        //final Query query = QueryFactory.create(geoQuery);
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
        qeh.addDefaultGraph(graph);
        String xmlType = "";
        for (String type : QueryEngineHTTP.supportedSelectContentTypes) {
            if (type.contains("xml")) {
                xmlType = type;
            }
        }
        qeh.setSelectContentType(xmlType);
        //QueryExecution queryExecution = qeh;
        HashSet<String> fetchedGeomsA = (HashSet<String>) sess.getAttribute("fetchedGeomsA");
        HashSet<String> fetchedGeomsB = (HashSet<String>) sess.getAttribute("fetchedGeomsB");

        if (fetchedGeomsA == null) {
            fetchedGeomsA = new HashSet<>();
            sess.setAttribute("fetchedGeomsA", fetchedGeomsA);
        }

        if (fetchedGeomsB == null) {
            fetchedGeomsB = new HashSet<>();
            sess.setAttribute("fetchedGeomsB", fetchedGeomsB);
        }

        System.out.println("Fetched from A : " + fetchedGeomsA.size());
        System.out.println("Fetched from B : " + fetchedGeomsB.size());
        
        int tries = 0;
        
        while ( tries < Constants.MAX_SPARQL_TRIES ) {
            try {
                final ResultSet resultSet = qeh.execSelect();

                int newGeom = 0;
                while (resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();

                    final String geo = querySolution.getLiteral("?geo").getString();
                    final String sub = querySolution.getResource("?s").getURI();

                    if (fetchedGeomsA.contains(sub)) {
                        continue;
                    }

                    fetchedGeomsA.add(sub);
                    newGeom++;

                    l.add(new JSONUnlinkedEntity(geo, sub));
                    //System.out.println("Fetched "+geo);
                }

                qeh.close();
            } catch (HttpException ex) {
                LOG.trace("HttpException during geometry fetch");
                LOG.debug("HttpException during geometry fetch : " + ex.getMessage());
                
                tries++;
            } catch (JenaException ex) {
                LOG.trace("JenaException during geometry fetch");
                LOG.debug("JenaException during geometry fetch : " + ex.getMessage());
                
                tries++;
            }
        }
        
        if ( tries == Constants.MAX_SPARQL_TRIES )
            success = false;
        
        return success;
    }
    
    private String normalizeQuery(String q) {
        StringBuilder ret = new StringBuilder();
        ret.append( "SELECT ?subject ?geometry WHERE {" );
        Matcher m = Patterns.PATTERN_POLYGON.matcher(q);
        String bbox = "empty";
        String triplePattern = "empty";
        if ( m.find() ) {
            bbox = "POLYGON((" + m.group(1) + "))";
        }
        m = Patterns.PATTERN_TRIPLE.matcher(q);
        if ( m.find() ) {
            triplePattern = m.group(1);
        }
        
        System.out.println("BBOX : " + bbox);
        System.out.println("Triple Pattern "+ triplePattern);
        
        ret.append(triplePattern);
        
        ret.append( "FILTER ");
        ret.append( " ( bif:st_intersects (?geometry, bif:st_geomfromtext(\""+bbox+"\"), 0)) }");
        
        System.out.println(ret.toString());
        
        return ret.toString();
    }
    
    private String extractOuterFilter(String q) {
        return "";
    } 
    
    private String extractInnerFilter(String q) {
        return "";
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
        processRequest(request, response);
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
            throws ServletException {
        processRequest(request, response);
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
