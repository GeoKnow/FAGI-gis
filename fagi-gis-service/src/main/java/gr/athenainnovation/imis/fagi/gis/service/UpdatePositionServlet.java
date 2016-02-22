/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "UpdatePositionServlet", urlPatterns = {"/UpdatePositionServlet"})
public class UpdatePositionServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(UpdatePositionServlet.class);       
        
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
        
        HttpSession                     sess;
        DBConfig                        dbConf;
        ObjectMapper                    mapper = new ObjectMapper();
        JSONRequestResult               ret = null;
        
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            
            sess = request.getSession(false);
            
            ret = new JSONRequestResult();

            if (sess == null) {
                ret.setMessage("Failed to create session!");
                ret.setStatusCode(-1);
                //System.out.println(connEx.getMessage());      
                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            String lGeometry = request.getParameter("pGeometry");
            String lSubject = request.getParameter("pSubject");
            String lGraph = request.getParameter("pGraph");
            
            System.out.println(lSubject);
            System.out.println(lGeometry);
            System.out.println(lGraph);
            
            if ( lGraph.equalsIgnoreCase("A") ) {
                updateGeometryPosition(lSubject, lGeometry, "dataset_a_geometries", dbConf);
            } else {
                updateGeometryPosition(lSubject, lGeometry, "dataset_b_geometries", dbConf);
            }
            
            ret.setMessage("Geometry Updated!");
            ret.setStatusCode(0);

            out.println(mapper.writeValueAsString(ret));
        }
    }

    private boolean updateGeometryPosition(String s, String g, String t, DBConfig dbConf) {
        boolean rSuccess = true;
        
        Connection dbConn = connect(dbConf);

        final String lQuery = "UPDATE "+t+" SET geom = ST_GeomFromText(?, 4326) WHERE subject = ?";
        
        System.out.println(lQuery);
        
        try (PreparedStatement stmt = dbConn.prepareStatement(lQuery) ) {
            stmt.setString(1, g);
            stmt.setString(2, s);
            stmt.executeUpdate();

            dbConn.commit();
        } catch (SQLException sqlex) {
            LOG.trace("SQLException thrown during Geometry Update");
            LOG.debug("SQLException thrown during Geometry Update : " + sqlex.getMessage());
            LOG.debug("SQLException thrown during Geometry Update : " + sqlex.getSQLState());
            if (dbConn != null) {
                try {
                    dbConn.rollback();
                } catch (SQLException ex1) {
                    LOG.trace("SQLException thrown during rollback");
                    LOG.debug("SQLException thrown during rollback : " + ex1.getMessage());
                    LOG.debug("SQLException thrown during rollback : " + ex1.getSQLState());
                }
            }
        }
        
        try {
            dbConn.close();
        } catch (SQLException ex1) {
            LOG.trace("SQLException thrown during rollback");
            LOG.debug("SQLException thrown during rollback : " + ex1.getMessage());
            LOG.debug("SQLException thrown during rollback : " + ex1.getSQLState());
        }
        
        return rSuccess;
    }
    
    private Connection connect(DBConfig dbConf) {
        Connection dbConn = null;
        
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            
            dbConn = null;
        }

        try {
            String url = Constants.DB_URL.concat(dbConf.getDBName());
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            dbConn.setAutoCommit(false);
        } catch (SQLException sqlex) {

            dbConn = null;
        }
        
        return dbConn;
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
