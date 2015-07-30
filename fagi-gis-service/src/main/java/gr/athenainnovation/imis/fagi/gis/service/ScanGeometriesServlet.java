/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import static com.hp.hpl.jena.enhanced.BuiltinPersonalities.model;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

/**
 *
 * @author nick
 */
@WebServlet(name = "ScanGeometriesServlet", urlPatterns = {"/ScanGeometriesServlet"})
public class ScanGeometriesServlet extends HttpServlet {

    private static final String HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    
    JSONFusedGeometries ret = null;
    GraphConfig grConf = null;
    
    private class JSONFusedGeometries {
        List<JSONFusedGeometry> geoms;
        String message;
        int statusCode;
        
        public JSONFusedGeometries() {
            geoms = new ArrayList<>();
            statusCode = -1;
            message = "";
        }

        public List<JSONFusedGeometry> getGeoms() {
            return geoms;
        }

        public void setGeoms(List<JSONFusedGeometry> geoms) {
            this.geoms = geoms;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

    }
    
    private class JSONFusedGeometry {
        String geom;
        String subject;

        public JSONFusedGeometry(String geom, String subject) {
            this.geom = geom;
            this.subject = subject;
        }

        public JSONFusedGeometry() {
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
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
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            
            HttpSession sess = request.getSession(true);
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            
            ret = new JSONFusedGeometries();
            
            final String restriction = "?s ?p1 _:a . _:a <"+AS_WKT_REGEX+"> ?g";
        final String geoQuery = "SELECT ?s ?g WHERE { " + restriction + " }";

        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh = new QueryEngineHTTP(grConf.getEndpointT(), geoQuery, authenticator);
        qeh.addDefaultGraph((String)sess.getAttribute("t_graph"));
        QueryExecution queryExecution = qeh;
        final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
        
        //geomColl.append("GEOMETRYCOLLECTION(");
        while(resultSet.hasNext()) {
            final QuerySolution querySolution = resultSet.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p1, g, p2;
            s = querySolution.get("?s");
            g = querySolution.get("?g");
            
            String geo = g.asLiteral().getString();
            int ind = geo.indexOf("^^");
            if ( ind > 0 ) {
                geo = geo.substring(0, ind);
            }
            String sub = s.toString();
            ret.geoms.add(new JSONFusedGeometry(geo, sub));
        }
        
            if (ret.geoms.size() > 0) {
                ret.setMessage("Datasets accepted!(Found fused geometries)");
                ret.setStatusCode(0);
            } else {
                ret.setMessage("Datasets accepted!(Found NO fused geometries)");
                ret.setStatusCode(1);
            }
        
            System.out.println(mapper.writeValueAsString(ret));
            out.println(mapper.writeValueAsString(ret));
        }
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
