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
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
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
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "FetchUnlinkedServlet", urlPatterns = {"/FetchUnlinkedServlet"})
public class FetchUnlinkedServlet extends HttpServlet {
    //Text
    private static final String strPolygonPattern = "POLYGON\\(\\((.*?)\\)\\)";
    private static final String strTriplePattern = "\\{([^F]*)";
    private static final Pattern patternPolygon = Pattern.compile( strPolygonPattern );
    private static final Pattern patternTriples = Pattern.compile( strTriplePattern );
    private static final Pattern patternInt = Pattern.compile( "^(\\d+)$" );
    
    private class JSONUnlinkedEntity {
        String geom;
        String sub;

        public JSONUnlinkedEntity(String geom, String subs) {
            this.geom = geom;
            this.sub = subs;
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }
        
    }
    
    private class JSONUnlinkedEntities {
        List<JSONUnlinkedEntity> entitiesA;
        List<JSONUnlinkedEntity> entitiesB;

        public JSONUnlinkedEntities() {
            this.entitiesA = new ArrayList<>();
            this.entitiesB = new ArrayList<>();
        }

        public JSONUnlinkedEntities(List<JSONUnlinkedEntity> entitiesA, List<JSONUnlinkedEntity> entitiesB) {
            this.entitiesA = entitiesA;
            this.entitiesB = entitiesB;
        }

        public List<JSONUnlinkedEntity> getEntitiesA() {
            return entitiesA;
        }

        public void setEntitiesA(List<JSONUnlinkedEntity> entitiesA) {
            this.entitiesA = entitiesA;
        }

        public List<JSONUnlinkedEntity> getEntitiesB() {
            return entitiesB;
        }

        public void setEntitiesB(List<JSONUnlinkedEntity> entitiesB) {
            this.entitiesB = entitiesB;
        }
        
    }
    
    private static class JSONBArea {
        String barea;
        double left, right, bottom, top;
        
        public String getBarea() {
            return barea;
        }

        public void setBarea(String barea) {
            this.barea = barea;
        }

        public double getLeft() {
            return left;
        }

        public void setLeft(double left) {
            this.left = left;
        }

        public double getRight() {
            return right;
        }

        public void setRight(double right) {
            this.right = right;
        }

        public double getBottom() {
            return bottom;
        }

        public void setBottom(double bottom) {
            this.bottom = bottom;
        }

        public double getTop() {
            return top;
        }

        public void setTop(double top) {
            this.top = top;
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
            throws ServletException, IOException {
        long startTime, endtime;
        HttpSession sess;
        GraphConfig grConf;
        DBConfig dbConf;
        JSONUnlinkedEntities ret;    
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            
            // Per request state
            sess = request.getSession(false);
            
            if (sess == null ) {
                out.print("{}");
                
                return;
            }
            
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            ret = new JSONUnlinkedEntities();
            JSONBArea BBox;
            
            /* TODO output your page here. You may use following sample code. */
            ObjectMapper mapper = new ObjectMapper();
            String bboxJSON = request.getParameter("bboxJSON");
            String queryA = request.getParameter("queryA");
            String queryB = request.getParameter("queryB");
            
            
            if ( bboxJSON == null || queryA == null || queryB == null ) {
                out.println("{\"error\":\"Invalid parameters for BBox Fetch\"}");
                
                return;
            }
            
            if ( !queryA.isEmpty() ) {
                queryA = normalizeQuery(queryA);
                customFetchGeoms(queryA, ret.entitiesA, grConf.getGraphA(), grConf.getEndpointA(), sess );
            }
            if ( !queryB.isEmpty() ) {
                queryB = normalizeQuery(queryB);
                customFetchGeoms(queryB, ret.entitiesB, grConf.getGraphB(), grConf.getEndpointB(), sess );
                
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

    private void fetchGeoms(List<JSONUnlinkedEntity> l, String graph, String service, String p, String t, JSONBArea BBox, HttpSession sess) {
        StringBuilder geoQuery = new StringBuilder();
        final float X_MAX = 180f;
        final float Y_MAX = 85.05f;
        //System.out.println(BBox.getRight());
        //System.out.println(BBox.getLeft());
        //System.out.println(BBox.getTop());
        //System.out.println(BBox.getBottom());
        if ( t.equalsIgnoreCase("POLYGONaa") ) {
            geoQuery.append("SELECT ?s ?geo WHERE {\n"
                    + "?s <"+p+"> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
            if (BBox.getLeft() < 0) {
                geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                        + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
            } else {
                geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                        + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
            }
            if (BBox.getBottom() < 0) {
                geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getTop() + Y_MAX) + ")\n"
                        + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getBottom() + Y_MAX) + ")\n");
            } else {
                geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getTop() + Y_MAX) + ")\n"
                        + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getBottom() + Y_MAX) + ")\n");
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
        final ResultSet resultSet = qeh.execSelect();
        
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
                    
            final String geo = querySolution.getLiteral("?geo").getString();
            final String sub = querySolution.getResource("?s").getURI();
            
            if ( fetchedGeomsA.contains(sub) )
                continue;
            
            fetchedGeomsA.add(sub);
            newGeom++;
            
            l.add(new JSONUnlinkedEntity(geo, sub));
            //System.out.println("Fetched "+geo);
        }
        
        qeh.close();
    }
    
    private String normalizeQuery(String q) {
        StringBuilder ret = new StringBuilder();
        ret.append( "SELECT ?subject ?geometry WHERE {" );
        Matcher m = patternPolygon.matcher(q);
        String bbox = "empty";
        String triplePattern = "empty";
        if ( m.find() ) {
            bbox = "POLYGON((" + m.group(1) + "))";
        }
        m = patternTriples.matcher(q);
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
            throws ServletException, IOException {
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
            throws ServletException, IOException {
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
