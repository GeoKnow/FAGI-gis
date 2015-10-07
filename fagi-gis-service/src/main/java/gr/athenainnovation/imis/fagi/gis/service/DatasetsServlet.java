/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

/**
 *
 * @author nick
 */
@WebServlet(name = "DatasetsServlet", urlPatterns = {"/DatasetsServlet"})
public class DatasetsServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(DatasetsServlet.class);    

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
        
        // Per request state
        PrintWriter                 out = response.getWriter();
        HttpSession                 sess;
        ObjectMapper                mapper = new ObjectMapper();
        GraphConfig                 graphConf;
        DBConfig                    dbConf;
                    
        try {
            sess = request.getSession(false);
            
            if ( sess == null ) {
                out.print("{}");
                
                out.close();
                
                return;
            }

            graphConf = new GraphConfig("", "", "", "");
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            
            //System.out.println("Dominant A " + request.getParameter("d_dom"));

            LOG.trace("Is A the dominant dataset? : " + request.getParameter("d_dom"));
            if (request.getParameter("d_dom") != null) {
                String param = request.getParameter("d_dom").toString().trim();
                if (param.equalsIgnoreCase("true")) {
                    LOG.info("Dominant dataset is A");
                    graphConf.setDominantA(true);
                } else {
                    LOG.info("Dominant dataset is B");
                }
            } else {
                graphConf.setDominantA(true);
            }
            
            // Construct the names of all TEMP Graphs that FAGI uses
            final String allLinksGraph = "http://localhost:8890/DAV/all_links_" + dbConf.getDBName()+"_fagi" ;
            final String linksGraph = "http://localhost:8890/DAV/links_" + dbConf.getDBName()+"_fagi" ;
            final String sampleLinksGraph = "http://localhost:8890/DAV/links_sample_" + dbConf.getDBName()+"_fagi" ;
            final String allClusterGraph = "http://localhost:8890/DAV/all_cluster_" + dbConf.getDBName()+"_fagi" ;
            final String clusterGraph = "http://localhost:8890/DAV/cluster_" + dbConf.getDBName()+"_fagi" ;
            final String targetGraph = request.getParameter("t_graph") ;
            final String metadataGraphA = targetGraph + "_" + dbConf.getDBName() + "A_fagi" ;
            final String metadataGraphB = targetGraph + "_" + dbConf.getDBName() + "B_fagi" ;
            final String targetTempGraph = targetGraph+"_"+dbConf.getDBName()+"_fagi" ;
            
            // Set graph configuration
            graphConf.setEndpointA(request.getParameter("da_end"));
            graphConf.setEndpointB(request.getParameter("db_end"));
            graphConf.setGraphA(request.getParameter("da_name"));
            graphConf.setGraphB(request.getParameter("db_name"));
            graphConf.setEndpointT(request.getParameter("t_end"));
            graphConf.setGraphL(request.getParameter("l_graph"));
            graphConf.setEndpointL(request.getParameter("l_end"));
            // [FAGI_TODOs] add checks 
            graphConf.setTargetGraph(targetGraph);
            graphConf.setTargetTempGraph(targetTempGraph);
            graphConf.setAllLinksGraph(allLinksGraph);
            graphConf.setLinksGraph(linksGraph);
            graphConf.setSampleLinksGraph(sampleLinksGraph);
            graphConf.setAllClusterGraph(allClusterGraph);
            graphConf.setClusterGraph(clusterGraph);
            graphConf.setMetadataGraphA(metadataGraphA);
            graphConf.setMetadataGraphB(metadataGraphB);
            
            LOG.info("Endpoint " + graphConf.getEndpointL());
            LOG.info("Graph " + graphConf.getGraphL());

            sess.setAttribute("gr_conf", graphConf);
            sess.setAttribute("t_graph", targetGraph);
            sess.setAttribute("t_end", request.getParameter("t_end"));
            sess.setAttribute("bulk", request.getParameter("bulk"));
            sess.setAttribute("dom", graphConf.isDominantA());

            // Simply return 1 if links are to be fetched from an endpoint
            if ( graphConf.getGraphL().isEmpty() || graphConf.getEndpointL().isEmpty() )
                out.print(0);
            else 
                out.print(1);
            
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
