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
    
    private class JSONFusedGeometries {
        List<JSONFusedGeometry> geoms;

        public JSONFusedGeometries() {
            geoms = new ArrayList<>();
        }

        public List<JSONFusedGeometry> getGeoms() {
            return geoms;
        }

        public void setGeoms(List<JSONFusedGeometry> geoms) {
            this.geoms = geoms;
        }

    }
    
    private class JSONFusedGeometry {
        String geom;
        String subject;

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
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            JSONFusedGeometries fused = new JSONFusedGeometries();
            
            HttpSession sess = request.getSession(true);
            String tGraph = (String)sess.getAttribute("t_graph");
            String geoQ = "SPARQL SELECT  ?os ?g\n" +
                          "WHERE\n" +
                          "  { GRAPH <"+tGraph+">  { ?os ?p1 ?b . ?b ?p2 ?g\n" +
                          "        \n" +
                          "      }\n" +
                          "  }";
            System.out.println("Geom Q "+geoQ);
            
            DBConfig dbConf = (DBConfig)sess.getAttribute("db_conf");
            VirtGraph vSet = null;
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
            //System.out.println("Get SPARQL prefix "+vSet..getSparqlPrefix());
            /*
           VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (geoQ, vSet);
        
                com.hp.hpl.jena.query.ResultSet results = vqe.execSelect();
                System.out.println("CALLED "+results.hasNext());
    for ( ; results.hasNext() ; )
    {
      QuerySolution soln = results.nextSolution() ;
      RDFNode x = soln.get("count(?os)") ;       // Get a result variable by name.
      System.out.println(x.asNode().toString());
    }
  */
                /*Query query = QueryFactory.create(geoQ);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                QueryExecution queryExecution = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", query, authenticator);
                com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
                long startTime =  System.nanoTime();
                System.out.println("Geometry "+resultSet.hasNext());
                //while(resultSet.hasNext()) {
                //    System.out.println("kati einai kai auto");
                //}
                query = QueryFactory.create(geoQ);
                authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                queryExecution = QueryExecutionFactory.create(query, ModelFactory.createModelForGraph(vSet));
                resultSet = queryExecution.execSelect();
                System.out.println("Geometry "+resultSet.hasNext());*/
            Connection virt_conn = vSet.getConnection();
            PreparedStatement fetchFusedGeoms;
            fetchFusedGeoms = virt_conn.prepareStatement(geoQ.toString());
            ResultSet rs = fetchFusedGeoms.executeQuery();
            //System.out.println("CALLED "+rs.());
            while ( rs.next() ) {
                JSONFusedGeometry geom = new JSONFusedGeometry();
                String g = rs.getString(2);
                String s = rs.getString(1);
                String truncG = StringUtils.substringBefore(g, "))");
                truncG = truncG.concat("))");
                geom.setGeom(truncG);
                geom.setSubject(s);
                
                System.out.println("Geometry "+g);
                fused.geoms.add(geom);
            }
            /* TODO output your page here. You may use following sample code. */
            System.out.println(mapper.writeValueAsString(fused));
            out.println(mapper.writeValueAsString(fused));
        } catch (SQLException ex) {
            Logger.getLogger(ScanGeometriesServlet.class.getName()).log(Level.SEVERE, null, ex);
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
