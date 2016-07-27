/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.core.FAGIUser;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Credentials;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.Utilities;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
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
 * @author nick
 */
@WebServlet(name = "UserLoginServlet", urlPatterns = {"/UserLoginServlet"})
public class UserLoginServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(UserLoginServlet.class);

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

        /*
        String querys = "SELECT * where { GRAPH <http://localhost:8890/fused_dataset> { <http://linkedgeodata.org/triplify/way113430594> ?p ?o . OPTIONAL { ?o ?p1 ?o1 . OPTIONAL { ?o1 ?p2 ?o2 . OPTIONAL { ?o2 ?p3 ?o3 . } } } } }";
        
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", Credentials.SERVICE_VIRTUOSO_PASS.toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            //QueryEngineHTTP qeh = new QueryEngineHTTP("http://pluto.imis.athena-innovation.gr:10381", querys, authenticator);
            QueryEngineHTTP qeh = new QueryEngineHTTP("http://localhost:8890/sparql", querys, authenticator);
            //qeh.addDefaultGraph((String) sess.getAttribute("t_graph"));
            QueryExecution queryExecution = qeh;
            
            System.out.println("Query for fused data " + querys);

            
            final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();

            System.out.println("Query for fused data " + querys);
            
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

                //ret.getTriples().add(new FetchLinkDataServlet.JSONTriple(subject, p.toString(), o.toString()));
                
                if ( o1 != null )
                    System.out.println(o1.toString());
                
                if ( o2 != null )
                    System.out.println(o2.toString());
                
                if ( o3 != null )
                    System.out.println(o3.toString());
                
            }
                       
        */                
        HttpSession sess;
        VirtGraph vSet = null;
        DBConfig dbConf = null;
        Connection dbConn = null;
        ObjectMapper mapper = new ObjectMapper();
        JSONRequestResult ret = null;
        boolean succeded = true;
        boolean foundUser = false;

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
                dbConf = Utilities.setUpDefaultDatabase(name, mail);
                sess.setAttribute("db_conf", dbConf);
            }

            sess.setAttribute("db_conf", dbConf);

            dbConn = createConnection("//localhost/fagi_users", dbConf.getDBUsername(), dbConf.getDBPassword());

            if (dbConn == null) {
                ret.setMessage("User not found");
                ret.setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));

                return;
            }

            foundUser = createUserDB(dbConn, dbConf);

            boolean exists = checkUser(dbConf, name, pass, mail);
            
            if (exists) {
                ret.setMessage("User found");
                ret.setStatusCode(0);

                FAGIUser user = new FAGIUser(name, pass, mail);

                sess.setAttribute("logged_user", user);

                HashSet<String> fetchedGeomsA = (HashSet<String>) sess.getAttribute("fetchedGeomsA");
                HashSet<String> fetchedGeomsB = (HashSet<String>) sess.getAttribute("fetchedGeomsB");

                if (fetchedGeomsA == null) {
                    fetchedGeomsA = new HashSet<>();
                    sess.setAttribute("fetchedGeomsA", fetchedGeomsA);
                }

                if (fetchedGeomsB == null) {
                    fetchedGeomsB = new HashSet<>();
                    sess.setAttribute("fetchedGeomsB", fetchedGeomsB);
                }

                fetchedGeomsA.clear();
                fetchedGeomsB.clear();
                
                // Save both containers of links
                sess.setAttribute("links", null);
                sess.setAttribute("links_list", null);
            
                final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
                succeded = databaseInitialiser.clearTables(dbConf);
                
            } else {
                ret.setMessage("User not found");
                ret.setStatusCode(-1);
            }

            out.println(mapper.writeValueAsString(ret));

        } finally {
            if (vSet != null) {
                vSet.close();
            }
            if (dbConn != null) {
                try {
                    //dbConn.commit();
                    dbConn.close();
                    //dbConn.commit();
                } catch (SQLException ex) {
                    LOG.error("Virtgraph Close Exception", ex);
                }
            }
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

    boolean createUserDB(Connection conn, DBConfig dbConf) {
        boolean success = true;
        Connection conn2;
        try (PreparedStatement stmtCheck = conn.prepareStatement(USER_DB_CHECK);
                ResultSet rs = stmtCheck.executeQuery()) {
            System.out.println("In here too");
            if (rs.next()) {
                System.out.println("User Exist");
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
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                LOG.info("Exception during user database close");
                LOG.error("Exception during user database close", ex);
            }
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
                System.out.println(rs.getString(1));
                System.out.println(rs.getString(2));
                System.out.println(rs.getString(3));
                System.out.println(rs.getString(4));
                System.out.println("Name found");
                found = true;
            } else {
                System.out.println("Name not found");
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
