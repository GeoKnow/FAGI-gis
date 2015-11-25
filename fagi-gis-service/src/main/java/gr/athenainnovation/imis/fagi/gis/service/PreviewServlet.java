/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONPreviewLink;
import gr.athenainnovation.imis.fusion.gis.json.JSONPreviewResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "PreviewServlet", urlPatterns = {"/PreviewServlet"})
public class PreviewServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(PreviewServlet.class);

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
        response.setContentType("text/html;charset=UTF-8");
        
        // Per request data
        PrintWriter                 out = null;
        Connection                  dbConn = null;
        String[]                    selectedLinks;
        HttpSession                 sess;            
        DBConfig                    dbConf;
        HashMap<String, String>     hashLinks;
        JSONRequestResult           res;
        JSONPreviewResult           ret;
        ObjectMapper                mapper = new ObjectMapper();
        boolean                     succeeded;
        
        try {
            try {
                out = response.getWriter();
            } catch (IOException ex) {
                LOG.trace("IOException thrown in servlet Writer");
                LOG.debug("IOException thrown in servlet Writer : \n" + ex.getMessage() );
                
                return;
            }
                    
            ret = new JSONPreviewResult();
            res = new JSONRequestResult();
            ret.setResult(res);
            sess = request.getSession(false);
            
            if (sess == null ) {
                ret.getResult().setMessage("Failed to create session!");
                ret.getResult().setStatusCode(-1);
                
                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }
            
            selectedLinks = request.getParameterValues("links[]");
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            hashLinks = (HashMap<String, String>)sess.getAttribute("links");
            
            List<Link> lst = new ArrayList<Link>();
            for ( String s : selectedLinks) {
                if (hashLinks.containsKey(s)) {
                    Link l = new Link(s, hashLinks.get(s));
                    lst.add(l);
                }
            }
            
            final GeometryFuser geometryFuser = new GeometryFuser();
            
            succeeded = geometryFuser.connect(dbConf);
            if ( !succeeded ) {
                LOG.trace("Connection for link upload failed");
                LOG.debug("Connection for link upload failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem connecting to PostGIS for link upload");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            succeeded = geometryFuser.loadLinks(lst);
            if ( !succeeded ) {
                LOG.trace("Link upload failed");
                LOG.debug("Link upload failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with PostGIS link upload");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            succeeded = geometryFuser.clean();
            if ( !succeeded ) {
                LOG.trace("Cleanup failed");
                LOG.debug("Cleanup failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with link upload cleanup");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            try{
                Class.forName("org.postgresql.Driver");     
            } catch (ClassNotFoundException ex) {
                LOG.trace("Driver Class Not Found Exception");
                LOG.debug("Driver Class Not Found Exception : " + ex);
                ret.getResult().setMessage("Could not load Postgis JDBC Driver!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                out.close();

                return;
            }
    
            try {
                String url = Constants.DB_URL.concat(dbConf.getDBName());
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            } catch(SQLException sqlex) {
                LOG.trace("Postgis Connect Exception");
                LOG.debug("Postgis Connect Exception : " + sqlex);
                LOG.debug("Postgis Connect Exception : " + sqlex);
                ret.getResult().setMessage("Connection to Postgis failed!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                out.close();
            
                return;
            }
            
            StringBuilder geomColl = new StringBuilder();
            
            String queryGeomsA = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(a.geom) as g\n" +
                                "FROM links INNER JOIN dataset_a_geometries AS a\n" +
                                "ON (links.nodea = a.subject)";
            
            String queryGeomsB = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(b.geom) as g\n" +
                                 "FROM links INNER JOIN dataset_b_geometries AS b\n" +
                                 "ON (links.nodeb = b.subject)";
                  
            String selectLinkedGeoms = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(a_g) as ga, ST_asText(b_g) as gb\n" +
                                        "FROM links \n" +
                                        "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n" +
                                        "		   dataset_b_geometries.subject AS b_s,\n" +
                                        "		   dataset_a_geometries.geom AS a_g,\n" +
                                        "		   dataset_b_geometries.geom AS b_g\n" +
                                        "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n" +
                                        "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
            
            HashMap<String, JSONPreviewLink> previews = Maps.newHashMap();
            try (PreparedStatement stmt = dbConn.prepareStatement(selectLinkedGeoms);
                    ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    final String gb = rs.getString("gb");
                    final String lb = rs.getString("lb");
                    final String ga = rs.getString("ga");
                    final String la = rs.getString("la");
                    
                    if ( !previews.containsKey(la) ) {
                        JSONPreviewLink plink = new JSONPreviewLink(ga,la,gb,lb);
                        previews.put(la, plink);
                    } else {
                        JSONPreviewLink plink = previews.get(la);
                    }
                    
                    geomColl.append(lb);
                    geomColl.append(";");
                    geomColl.append(gb);
                    geomColl.append(";");
                    geomColl.append(la);
                    geomColl.append(";");
                    geomColl.append(ga);
                    geomColl.append(";");
                }
                
            } catch (SQLException ex) {
                LOG.trace("Postgis Connect Exception");
                LOG.debug("Postgis Connect Exception : " + ex);
                LOG.debug("Postgis Connect Exception : " + ex);
                ret.getResult().setMessage("Connection to Postgis failed!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                out.close();
            }
            
            out.print(geomColl);
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
        } finally {
            try {
                if ( dbConn != null )
                    dbConn.close();
            } catch (SQLException ex) {
                LOG.trace("Postgis Connection could not be closed");
                LOG.debug("Postgis Connection could not be closed : " + ex.getMessage());
                LOG.debug("Postgis Connection could not be closed : " + ex.getSQLState());
            }
            if ( out != null )
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
