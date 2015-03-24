/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fagi.gis.service;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author nick
 */
@WebServlet(name = "DatasetsServlet", urlPatterns = {"/DatasetsServlet"})
public class DatasetsServlet extends HttpServlet {

    private static final String HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    
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
        PrintWriter out = response.getWriter();
        QueryExecution askWKT = null;
        QueryExecution askWGSlat = null;
        QueryExecution askWGSlong = null;
        QueryExecution selectGeom = null;
        
        boolean hasWKTA = false,  hasWKTB = false;
        boolean hasWGSlatA = false,  hasWGSlatB = false;
        boolean hasWGSlongA = false,  hasWGSlongB = false;
    try {
        GraphConfig graphConf = new GraphConfig("", "", "", "");
        
        System.out.println("Dominant A "+request.getParameter("d_dom"));
        
        if ( request.getParameter("d_dom") != null ) {
            if ( request.getParameter("d_dom").equalsIgnoreCase("true") ) {
                graphConf.setDominantA(true);
            }
        } else {
            graphConf.setDominantA(false);
        }
        graphConf.setEndpointA(request.getParameter("da_end"));
        graphConf.setEndpointB(request.getParameter("db_end"));
        graphConf.setGraphA(request.getParameter("da_name"));
        graphConf.setGraphB(request.getParameter("db_name"));
        graphConf.setEndpointT(request.getParameter("t_end"));
        
        HttpSession sess = request.getSession(true);
        sess.setAttribute("gr_conf",  graphConf);
        sess.setAttribute("t_graph", request.getParameter("t_graph"));
        sess.setAttribute("t_end", request.getParameter("t_end"));
        sess.setAttribute("bulk", request.getParameter("bulk"));
        sess.setAttribute("dom", graphConf.isDominantA());
        /*DBConfig dbConf = (DBConfig)request.getSession(true).getAttribute("db_conf");
        if (dbConf == null) {
            out.print("Connection parameters not set");
            
            out.close();
            
            return;
        }
        
        response.setContentType("text/html;charset=UTF-8");
        GraphConfig graphConf = new GraphConfig("", "", "", "");
        
        graphConf.setEndpointA(request.getParameter("da_end"));
        graphConf.setEndpointB(request.getParameter("db_end"));
        graphConf.setGraphA(request.getParameter("da_name"));
        graphConf.setGraphB(request.getParameter("db_name"));
        
        String asWKTString = "ASK { ?s <http://www.opengis.net/ont/geosparql#asWKT> ?o }";
        String asWGSSlongtring = "ASK { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o }";
        String asWGSSlattring = "ASK { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o }";
        
        askWKT = QueryExecutionFactory.sparqlService(graphConf.getEndpointA(), asWKTString, graphConf.getGraphA());  
        askWGSlong = QueryExecutionFactory.sparqlService(graphConf.getEndpointA(), asWGSSlongtring, graphConf.getGraphA());  
        askWGSlat = QueryExecutionFactory.sparqlService(graphConf.getEndpointA(), asWGSSlattring, graphConf.getGraphA());
        
        hasWKTA = askWKT.execAsk();
        hasWGSlongA = askWGSlong.execAsk();
        hasWGSlatA = askWGSlat.execAsk();
        
        askWKT = QueryExecutionFactory.sparqlService(graphConf.getEndpointB(), asWKTString, graphConf.getGraphB());  
        askWGSlong = QueryExecutionFactory.sparqlService(graphConf.getEndpointB(), asWGSSlongtring, graphConf.getGraphB());  
        askWGSlat = QueryExecutionFactory.sparqlService(graphConf.getEndpointB(), asWGSSlattring, graphConf.getGraphB());
        
        hasWKTB = askWKT.execAsk();
        hasWGSlongB = askWGSlong.execAsk();
        hasWGSlatB = askWGSlat.execAsk();
          
        System.out.println(hasWKTA+" "+hasWGSlongA+" "+hasWGSlatA);
        System.out.println(hasWKTB+" "+hasWGSlongB+" "+hasWGSlatB);
        
        
        final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();*/
        //databaseInitialiser.initialise(st.getDbConf());
        //String url = DB_URL.concat(dbConf.getDBName());
        
        /*
        final String restriction = "?s ?p1 _:a . _:a ?p2 ?g FILTER(regex(?s, \"" + "" + "\", \"i\")) " + "FILTER(regex(?p1, \"" + HAS_GEOMETRY_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + AS_WKT_REGEX + "\", \"i\"))";
        final String queryString = "SELECT ?s ?g WHERE { " + restriction + " }";

        selectGeom = QueryExecutionFactory.sparqlService(graphConf.getEndpointB(), queryString, graphConf.getGraphB());  
        
        final com.hp.hpl.jena.query.ResultSet resultSetFromB = selectGeom.execSelect();
        StringBuilder geomColl = new StringBuilder(10000);
        //geomColl.append("GEOMETRYCOLLECTION(");
        while(resultSetFromB.hasNext()) {
            final QuerySolution querySolution = resultSetFromB.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p1, g, p2;
            s = querySolution.get("?s");
            g = querySolution.get("?g");
            
            if (g != null && s != null) {
                //System.out.print(g.asNode().getLiteralLexicalForm()+" "+s.asNode());
                geomColl.append(s.asNode());
                geomColl.append(";");
                geomColl.append(g.asNode().getLiteralLexicalForm());
                geomColl.append(";");
            }
            //System.out.println();
        }
        
        geomColl.append("::");
        
        final String restrictionForWgs = "?s ?p1 ?o1 . ?s ?p2 ?o2 FILTER(regex(?s, \"" + "" + "\", \"i\")) " + "FILTER(regex(?p1, \"" + LAT_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + LONG_REGEX + "\", \"i\"))";
        
        final String queryString1 = "SELECT ?s ?o1 ?o2 WHERE { " + restrictionForWgs + " }";
        
        selectGeom = QueryExecutionFactory.sparqlService(graphConf.getEndpointA(), queryString1, graphConf.getGraphA());  
        
        final com.hp.hpl.jena.query.ResultSet resultSetFromA = selectGeom.execSelect();
        while(resultSetFromA.hasNext()) {
            final QuerySolution querySolution = resultSetFromA.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p1, g, p2;
            s = querySolution.get("?s");
            g = querySolution.get("?g");
            
            final RDFNode objectNode1 = querySolution.get("?o1"); //lat
            final RDFNode objectNode2 = querySolution.get("?o2"); //long
            String geometry = "";
            if(objectNode1.isLiteral() && objectNode2.isLiteral()) {
                final double latitude = objectNode1.asLiteral().getDouble();
                final double longitude = objectNode2.asLiteral().getDouble();
                        
                //construct wkt serialization
                geometry = "POINT ("+ longitude + " " + latitude +")";
            }
                    
            
            if (s != null) {
                //System.out.print(g.asNode().getLiteralLexicalForm()+" "+s.asNode());
                geomColl.append(s.asNode());
                geomColl.append(";");
                geomColl.append(geometry);
                geomColl.append(";");
            }
            //System.out.println();
        }
             
        geomColl.append("::");
        
        selectGeom = QueryExecutionFactory.sparqlService(graphConf.getEndpointB(), queryString, "http://localhost:8890/fused_dataset");  
        
        com.hp.hpl.jena.query.ResultSet resultSetFromF = selectGeom.execSelect();
        //geomColl.append("GEOMETRYCOLLECTION(");
        while(resultSetFromF.hasNext()) {
            final QuerySolution querySolution = resultSetFromF.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p1, g, p2;
            s = querySolution.get("?s");
            g = querySolution.get("?g");
            
            if (g != null && s != null) {
                //System.out.print(g.asNode().getLiteralLexicalForm()+" "+s.asNode());
                geomColl.append(s.asNode());
                geomColl.append(";");
                geomColl.append(g.asNode().getLiteralLexicalForm());
                geomColl.append(";");
            }
            //System.out.println();
        }
        
        int len = geomColl.length();
        //geomColl.setLength(len-1);
            out.print(geomColl.toString());
        */
        } finally {
            out.close();
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
