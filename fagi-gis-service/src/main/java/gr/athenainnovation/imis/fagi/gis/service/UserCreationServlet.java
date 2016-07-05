/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.athenainnovation.imis.fusion.gis.core.FAGIUser;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.Utilities;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(name = "UserCreationServlet", urlPatterns = {"/UserCreationServlet"})
public class UserCreationServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(UserCreationServlet.class);

    private static final String DEFAULT_DB_NAME = "fagi_users";
    private static final String DEFAULT_TABLE_NAME = "fagi_users";
    private static final String USER_DB_CHECK = "SELECT datname FROM pg_catalog.pg_database WHERE datname = '" + DEFAULT_DB_NAME + "'";
    private static final String USER_DB_CREATE = "CREATE DATABASE " + DEFAULT_DB_NAME + "";
    private static final String USER_DB_CHECK_USER = "SELECT * FROM " + DEFAULT_TABLE_NAME + " WHERE MAIL = ?";
    private static final String USER_DB_CREATE_USERS = "CREATE TABLE IF NOT EXISTS " + DEFAULT_TABLE_NAME + ""
            + "(\n"
            + "   ID SERIAL PRIMARY KEY      NOT NULL,\n"
            + "   NAME           CHAR(50) NOT NULL,\n"
            + "   PASS           CHAR(50) NOT NULL,\n"
            + "   MAIL           CHAR(50) NOT NULL"
            + ")";
    private static final String USER_DB_INSERT_USER = "INSERT INTO " + DEFAULT_TABLE_NAME + " ( NAME, PASS, MAIL ) VALUES ( ?, ?, ?);";
    
    private static final String USER_VIRTUOSO_CREATE = "INSERT INTO " + DEFAULT_TABLE_NAME + " ( NAME, PASS, MAIL ) VALUES ( ?, ?, ?);";

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
        VirtGraph vSet = null;
        DBConfig dbConf = null;
        Connection dbConn = null;
        ObjectMapper mapper = new ObjectMapper();
        JSONRequestResult ret = null;
        boolean succeded = true;

        System.out.println("In Here");

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */

            //mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            ret = new JSONRequestResult();

            sess = request.getSession(false);
            if (sess != null) {
                //sess.invalidate();
            } else {
                sess = request.getSession(true);
            }
            
            // The only time we need a session if one does not exist
            //sess = request.getSession(true);

            String name = request.getParameter("u_name");
            String pass = request.getParameter("u_pass");
            String mail = request.getParameter("u_mail");

            dbConf = (DBConfig) sess.getAttribute("db_conf");

            System.out.println(name);
            System.out.println(pass);
            System.out.println(mail);
            System.out.println(dbConf);

            if (dbConf == null) {
                dbConf = Utilities.setUpDefaultDatabase(name);
                sess.setAttribute("db_conf", dbConf);
            }
            
            dbConn = createConnection("", dbConf.getDBUsername(), dbConf.getDBPassword());
            createUserDB(dbConn, dbConf);
            
            //createUserVirtuoso(dbConn, dbConf);
            
            boolean exists = checkUser(dbConf, name, pass, mail);

            if ( !exists ) {
                createUser(dbConf, name, pass, mail);
                
                ret.setMessage("User created");
                ret.setStatusCode(0);
                
                FAGIUser user = new FAGIUser(name, pass, mail);
                
                sess.setAttribute("logged_user", user);
            } else {
                ret.setMessage("User already exists");
                ret.setStatusCode(-1);
            }
            
            out.printf(mapper.writeValueAsString(ret));
        }
    }

    Connection createConnection(String db, String user, String pass) {
        Connection conn = null;

        System.out.println("User creation ");
        // Try loading the postgres sql driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOG.info("Driver Class Not Found Exception");
            LOG.error("Driver Class Not Found Exception", ex);

            return null;
        }

        // Try a dummy connection to Postgres to check if a database with the same name exists
        try {
            String url = Constants.DB_URL + db;
            conn = DriverManager.getConnection(url, user, pass);
            //dbConn.setAutoCommit(false);
        } catch (SQLException sqlex) {
            LOG.info("Postgis Connect Exception");
            LOG.error("Postgis Connect Exception", sqlex);

            return null;
        }

        return conn;
    }

    boolean createUserVirtuoso( DBConfig dbConf, String name, String pass ) {
        boolean success = true;
        
        
        
        return success;
    }
    
    boolean createUserDB(Connection conn, DBConfig dbConf) {
        boolean success = true;
        Connection conn2;
        try (PreparedStatement stmtCheck = conn.prepareStatement(USER_DB_CHECK);
                ResultSet rs = stmtCheck.executeQuery()) {
            System.out.println("In here too");
            if (rs.next()) {
                return true;
            } else {
                System.out.println("creation");
                try (PreparedStatement stmtCreate = conn.prepareStatement(USER_DB_CREATE)) {
                    stmtCreate.executeUpdate();
                }
                conn2 = createConnection("//localhost/fagi_users", dbConf.getDBUsername(), dbConf.getDBPassword());
                try (PreparedStatement stmtCreateTable = conn2.prepareStatement(USER_DB_CREATE_USERS)) {
                    stmtCreateTable.executeUpdate();
                }
                conn2.close();
            }
            conn.close();

        } catch (SQLException ex) {
            LOG.info("Exception during user database creation");
            LOG.error("Exception during user database creation", ex);
        }

        return success;
    }

    boolean createUser(DBConfig dbConf, String name, String pass, String mail) {
        boolean success = true;
        Connection conn = createConnection("//localhost/fagi_users", dbConf.getDBUsername(), dbConf.getDBPassword());
        try (PreparedStatement stmtCreate = conn.prepareStatement(USER_DB_INSERT_USER)) {

            //stmtCreate.setInt(1, 0);
            stmtCreate.setString(1, name);
            stmtCreate.setString(2, pass);
            stmtCreate.setString(3, mail);

            stmtCreate.executeUpdate();

        } catch (SQLException ex) {
            LOG.info("Exception during user database creation");
            LOG.error("Exception during user database creation", ex);
        }

        return success;
    }

    boolean checkUser(DBConfig dbConf, String name, String pass, String mail) {
        boolean found = true;
        Connection conn = createConnection("//localhost/fagi_users", dbConf.getDBUsername(), dbConf.getDBPassword());
        try (PreparedStatement stmtCheck = conn.prepareStatement(USER_DB_CHECK_USER)) {

            //stmtCreate.setInt(1, 0);
            stmtCheck.setString(1, mail);

            ResultSet rs = stmtCheck.executeQuery();

            if (rs.next()) {
                //System.out.println(rs.getString(1));
                //System.out.println(rs.getString(2));
                //System.out.println(rs.getString(3));
                //System.out.println(rs.getString(4));
                //System.out.println("Name found");
                found = true;
            } else {
                //System.out.println("Name not found");
                found = false;
            }

        } catch (SQLException ex) {
            LOG.info("Exception during user database creation");
            LOG.error("Exception during user database creation", ex);

            found = false;
        }

        return found;
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
