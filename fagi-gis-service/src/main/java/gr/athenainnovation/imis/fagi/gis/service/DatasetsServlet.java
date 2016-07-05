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
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.core.FAGIUser;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONDatasetConfigResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.SPARQLUtilities;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
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
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        
        // Per request state
        PrintWriter                 out = null;
        HttpSession                 sess;
        ObjectMapper                mapper = new ObjectMapper();
        GraphConfig                 graphConf;
        DBConfig                    dbConf;
        FAGIUser                    activeUser;
        JSONDatasetConfigResult     ret;
        JSONRequestResult           res;

        try {
            out = response.getWriter();

            sess = request.getSession(false);
            
            ret = new JSONDatasetConfigResult();
            res = new JSONRequestResult();
            ret.setResult(res);
            
            if ( sess == null ) {
                LOG.trace("No active session found");
                LOG.debug("No active session found");
                res.setStatusCode(-1);
                res.setMessage("Invalid session");

                out.print(mapper.writeValueAsString(ret));
                
                return;
            }

            graphConf = new GraphConfig("", "", "", "");
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            activeUser = (FAGIUser)sess.getAttribute("logged_user");
            
            LOG.trace("Is A the dominant dataset? : " + request.getParameter("d_dom"));
            System.out.println("Is A the dominant dataset? : " + request.getParameter("d_dom"));
            if (request.getParameter("d_dom") != null) {
                String param = request.getParameter("d_dom").toString().trim();
                if (param.equalsIgnoreCase("true")) {
                    LOG.info("Dominant dataset is A");
                    graphConf.setDominantA(true);
                } else {
                    LOG.info("Dominant dataset is B");
                }
            } else {
                graphConf.setDominantA(false);
            }
            
            System.out.println(activeUser.getName());
            System.out.println(activeUser.getPass());
            System.out.println(activeUser.getMail());
            
            // Construct the names of all TEMP Graphs that FAGI uses
            final String allLinksGraph = "http://localhost:8890/DAV/all_links_" + dbConf.getDBName()+"_fagi_" + activeUser.getName();
            final String linksGraph = "http://localhost:8890/DAV/links_" + dbConf.getDBName()+"_fagi_" + activeUser.getName() ;
            final String sampleLinksGraph = "http://localhost:8890/DAV/links_sample_" + dbConf.getDBName()+"_fagi_" + activeUser.getName() ;
            final String allClusterGraph = "http://localhost:8890/DAV/all_cluster_" + dbConf.getDBName()+"_fagi_" + activeUser.getName();
            final String clusterGraph = "http://localhost:8890/DAV/cluster_" + dbConf.getDBName()+"_fagi_" + activeUser.getName();
            final String targetGraph = request.getParameter("t_graph") ;
            final String metadataGraphA = targetGraph + "_" + dbConf.getDBName() + "A_fagi_" + activeUser.getName() ;
            final String metadataGraphB = targetGraph + "_" + dbConf.getDBName() + "B_fagi_" + activeUser.getName() ;
            final String typeGraphA = targetGraph + "_" + dbConf.getDBName() + "A_types_fagi_" + activeUser.getName() ;
            final String typeGraphB = targetGraph + "_" + dbConf.getDBName() + "B_types_fagi_" + activeUser.getName() ;
            final String targetTempGraph = targetGraph+"_"+dbConf.getDBName()+"_fagi_" + activeUser.getName() ;
            
            // Set graph configuration
            graphConf.setEndpointA(request.getParameter("da_end"));
            graphConf.setEndpointB(request.getParameter("db_end"));
            graphConf.setGraphA(request.getParameter("da_name"));
            graphConf.setGraphB(request.getParameter("db_name"));
            graphConf.setTypeGraphA(request.getParameter("db_name"));
            graphConf.setTypeGraphB(request.getParameter("db_name"));
            graphConf.setEndpointT(request.getParameter("t_end"));
            graphConf.setGraphL(request.getParameter("l_graph"));
            graphConf.setEndpointL(request.getParameter("l_end"));

            //System.out.println(graphConf.getEndpointA());
            //System.out.println(graphConf.getEndpointB());
            //System.out.println(graphConf.getGraphA());
            //System.out.println(graphConf.getGraphB());
            
            // [FAGI_TODOs] add checks 
            //graphConf.setTargetGraph(targetGraph);
            graphConf.setTargetGraph(targetTempGraph);
            graphConf.setTargetTempGraph(targetTempGraph);
            graphConf.setAllLinksGraph(allLinksGraph);
            graphConf.setLinksGraph(linksGraph);
            graphConf.setSampleLinksGraph(sampleLinksGraph);
            graphConf.setAllClusterGraph(allClusterGraph);
            graphConf.setClusterGraph(clusterGraph);
            graphConf.setMetadataGraphA(metadataGraphA);
            graphConf.setMetadataGraphB(metadataGraphB);
            graphConf.setTypeGraphA(typeGraphA);
            graphConf.setTypeGraphB(typeGraphB);
            
            // Try a dummy connection to Virtuoso
            VirtGraph vSet = null;
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.error("Virtgraph Create Exception", connEx);
                vSet = null;
                    //ret.setMessage("Connection to Virtuoso failed!");
                //ret.setStatusCode(-1);
                //System.out.println(connEx.getMessage());      
                out.println(mapper.writeValueAsString(ret));
                out.close();

                return;
            }

            System.out.println("Creating User FAGI");
            SPARQLUtilities.createSPARQLUser(vSet.getConnection(), activeUser.getName(), activeUser.getPass());
            SPARQLUtilities.createSPARQLUserGraph(graphConf, vSet.getConnection(), activeUser.getName(), activeUser.getPass());

            /*
            int depthA = SPARQLUtilities.getGraphDepth(graphConf.getGraphA(), graphConf.getEndpointA());
            int depthB = SPARQLUtilities.getGraphDepth(graphConf.getGraphB(), graphConf.getEndpointB());
            
            graphConf.setDepthA(depthA);
            graphConf.setDepthB(depthB);
            
            LOG.info("DepthA " + depthA);
            LOG.info("DepthB " + depthB);
            LOG.info("Endpoint " + graphConf.getEndpointL());
            LOG.info("Graph " + graphConf.getGraphL());
            */
            
            sess.setAttribute("gr_conf", graphConf);
            sess.setAttribute("t_graph", targetGraph);
            sess.setAttribute("t_end", request.getParameter("t_end"));
            sess.setAttribute("bulk", request.getParameter("bulk"));
            sess.setAttribute("dom", graphConf.isDominantA());

            // Simply return 1 if links are to be fetched from an endpoint
            if ( graphConf.getGraphL().isEmpty() || graphConf.getEndpointL().isEmpty() )
                ret.setRemoteLinks(false);
            else 
                ret.setRemoteLinks(true);
            System.out.println("False or True " + ret.isRemoteLinks());
            res.setStatusCode(0);
            res.setMessage("done");

            //System.out.print("\n\n\n\n\n"+mapper.writeValueAsString(ret)+"\n\n\n\n\n\n");
            out.print(mapper.writeValueAsString(ret));
            
        } catch ( IOException ioe ) {
            throw new ServletException("Unkonw Servlet Error");
        } finally {
            if (out != null )
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
