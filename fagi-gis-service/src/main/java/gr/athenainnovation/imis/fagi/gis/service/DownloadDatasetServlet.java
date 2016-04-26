/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.core.FAGIUser;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(name = "DownloadDatasetServlet", urlPatterns = {"/DownloadDatasetServlet"})
public class DownloadDatasetServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(DownloadDatasetServlet.class);

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

        HttpSession sess;
        GraphConfig graphConf;
        DBConfig dbConf;
        FAGIUser activeUser;
        VirtGraph vSet = null;

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */

            sess = request.getSession(false);

            if (sess == null) {
                LOG.trace("No active session found");
                LOG.debug("No active session found");

                return;
            }

            //graphConf = new GraphConfig("", "", "", "");
            graphConf = (GraphConfig)sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            activeUser = (FAGIUser) sess.getAttribute("logged_user");

            //response.setContentType(fileType);
            response.setHeader("Access-Control-Allow-Origin", "*");
            //response.setHeader("Cache-control", "no-cache, no-store");
            //response.setHeader("Pragma", "no-cache");
            //response.setHeader("Expires", "-1");
            //response.setHeader("Date", Long.toString(time));
            //response.setHeader("Last-Modified", Long.toString(time));
            response.setHeader("Content-disposition", "attachment; filename=results.nt");

            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.error("Virtgraph Create Exception", connEx);
                vSet = null;

                out.close();
            }

            StreamDataset(out, vSet, graphConf);
        }
    }

    private void StreamDataset(PrintWriter out, VirtGraph vSet, GraphConfig grConf) {
        //final String SELECT_ALL_URLS = "SPARQL SELECT * WHERE { GRAPH <" + "http://localhost:8890/DAV/wik_demo" + "> { ?s ?p ?o FILTER isURI(?o) } }";
        //final String SELECT_ALL_LITERALS = "SPARQL SELECT * WHERE { GRAPH <" + "http://localhost:8890/DAV/wik_demo" + "> { ?s ?p ?o FILTER isLiteral(?o) } }";
        final String SELECT_ALL_URLS = "SPARQL SELECT * WHERE { GRAPH <" + grConf.getTargetGraph() + "> { ?s ?p ?o FILTER isURI(?o) } }";
        final String SELECT_ALL_LITERALS = "SPARQL SELECT * WHERE { GRAPH <" + grConf.getTargetGraph() + "> { ?s ?p ?o FILTER isLiteral(?o) } }";
        final VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        final StringBuilder sb = new StringBuilder();
        VirtuosoResultSet rs;

        try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(SELECT_ALL_URLS);
                VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery()) {

            while (vrs.next()) {
                String s = vrs.getString(1);
                String p = vrs.getString(2);
                String o = vrs.getString(3);
                /*try {
                    s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
                    p = URLDecoder.decode(p, StandardCharsets.UTF_8.name());
                    o = URLDecoder.decode(o, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException ex) {
                    continue;
                }*/
                
                sb.append("<").append(s).append("> ");
                sb.append("<").append(p).append("> ");
                sb.append("<").append(o).append("> .\n");
            }

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on failed");
            LOG.debug("VirtuosoException on failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on failed : " + ex.getSQLState());
        }

        try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(SELECT_ALL_LITERALS);
                VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery()) {

            while (vrs.next()) {
                String s = vrs.getString(1);
                String p = vrs.getString(2);
                String o = vrs.getString(3);
                /*try {
                    s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
                    p = URLDecoder.decode(p, StandardCharsets.UTF_8.name());
                    o = URLDecoder.decode(o, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException ex) {
                    continue;
                }*/
                
                sb.append("<").append(s).append("> ");
                sb.append("<").append(p).append("> ");
                sb.append("\"").append(o).append("\" .\n");
            }

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on failed");
            LOG.debug("VirtuosoException on failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on failed : " + ex.getSQLState());
        }

        sb.setLength(sb.length());

        out.print(sb.toString());
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
