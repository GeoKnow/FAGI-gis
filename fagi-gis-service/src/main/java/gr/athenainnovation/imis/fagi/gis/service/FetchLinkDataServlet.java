/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.clustering.GeoClusterer;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "FetchLinkDataServlet", urlPatterns = {"/FetchLinkDataServlet"})
public class FetchLinkDataServlet extends HttpServlet {

    private class JSONTriple {
        String s, p, o;

        public JSONTriple(String s, String p, String o) {
            this.s = s;
            this.p = p;
            this.o = o;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getO() {
            return o;
        }

        public void setO(String o) {
            this.o = o;
        }
        
    }
    
     private class JSONTriples {
        List<JSONTriple> triples;

        public JSONTriples() {
            triples = new ArrayList<>();
        }

        public List<JSONTriple> getTriples() {
            return triples;
        }

        public void setTriples(List<JSONTriple> triples) {
            this.triples = triples;
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
        
        HttpSession sess;
        GraphConfig grConf;
        DBConfig dbConf;
        JSONTriples ret;
        String subject;
        VirtGraph vSet = null;
        
        try (PrintWriter out = response.getWriter()) {
            
            sess = request.getSession(false);
            
            if ( sess == null ) {
                out.println("{}");
                
                return;
            }
            
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            ret = new JSONTriples();
            subject = request.getParameter("subject");
        
            /* TODO output your page here. You may use following sample code. */
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());
                out.println("Connection to virtuoso failed");
                out.close();

                return;
            }

            final String selectAll = "SELECT * where "
                    + "{ GRAPH <" + grConf.getTargetGraph() + "> { "
                    + "<" + subject + "> ?p ?o ."
                    + " OPTIONAL { ?o ?p1 ?o1 ."
                    + " OPTIONAL { ?o1 ?p2 ?o2 ."
                    + " OPTIONAL { ?o2 ?p3 ?o3 . } } } } }";
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            QueryEngineHTTP qeh = new QueryEngineHTTP(grConf.getEndpointT(), selectAll, authenticator);
            qeh.addDefaultGraph((String) sess.getAttribute("t_graph"));
            QueryExecution queryExecution = qeh;
            final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();

            //geomColl.append("GEOMETRYCOLLECTION(");
            while (resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                //final String predicate = querySolution.getResource("?p").getURI();
                RDFNode p, o, p1, o1, p2, o2, p3, o3;
                o = querySolution.get("?o");
                p = querySolution.get("?p");
                o1 = querySolution.get("?o1");
                o2 = querySolution.get("?o2");
                o3 = querySolution.get("?o3");
                p3 = querySolution.get("?p3");
                p2 = querySolution.get("?p2");
                p1 = querySolution.get("?p1");

                ret.getTriples().add(new JSONTriple(subject, p.toString(), o.toString()));
                
                if ( o1 != null )
                    ret.getTriples().add(new JSONTriple(o.toString(), p1.toString(), o1.toString()));
                
                if ( o2 != null )
                    ret.getTriples().add(new JSONTriple(o1.toString(), p2.toString(), o2.toString()));
                
                if ( o3 != null )
                    ret.getTriples().add(new JSONTriple(o2.toString(), p3.toString(), o3.toString()));
                
            }
            
            
            System.out.println(mapper.writeValueAsString(ret));
            out.println(mapper.writeValueAsString(ret));
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
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
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(FetchLinkDataServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(FetchLinkDataServlet.class.getName()).log(Level.SEVERE, null, ex);
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
