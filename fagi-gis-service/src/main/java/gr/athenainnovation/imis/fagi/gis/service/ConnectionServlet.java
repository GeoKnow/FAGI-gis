
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import java.io.File;
//import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
//import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
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

@WebServlet(name = "ConnectionServlet", urlPatterns = {"/ConnectionServlet"})
public class ConnectionServlet extends HttpServlet {
    
    private JSONConnRequest ret = null;      
    
    private class JSONConnRequest {
        // -1 error 0 success
        int statusCode;
        String message;

        public JSONConnRequest() {
            this.statusCode = -1;
            this.message = "general error";
        }

        public JSONConnRequest(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
        
    }
    private static final String DB_URL = "jdbc:postgresql:";
    
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
    PrintWriter out = response.getWriter();
    VirtGraph vSet = null;
    FusionState st = new FusionState();
    DBConfig dbConf = new DBConfig("", "", "", "", "", "", "");
    Connection dbConn = null;
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    ret = new JSONConnRequest();
    
    try {
        response.setContentType("text/html;charset=UTF-8");
        //System.out.println(Paths.get("").toAbsolutePath().toString());

        //System.out.println(System.getProperty("user.dir"));   
        st.setDbConf(dbConf);
          String relativeWebPath = "/FAGI-gis-WebInterface/lib/stopWords.ser";
String absoluteDiskPath = getServletContext().getRealPath(relativeWebPath);

                System.out.println("Rel Path "+System.getProperty("user.dir"));
                System.out.println("Rel Path "+(new File(".")).getAbsolutePath());
                System.out.println("Rel Path "+System.getenv());
        dbConf.setUsername(request.getParameter("v_name"));
        dbConf.setPassword(request.getParameter("v_pass"));
        dbConf.setDbURL(request.getParameter("v_url"));
        
        dbConf.setDbName(request.getParameter("p_data"));
        dbConf.setDbUsername(request.getParameter("p_name"));
        dbConf.setDbPassword(request.getParameter("p_pass"));
        //System.out.println(dbConf.getDBUsername());
        try {
            vSet = new VirtGraph ("jdbc:virtuoso://" + request.getParameter("v_url") + "/CHARSET=UTF-8",
                                         request.getParameter("v_name"), 
                                         request.getParameter("v_pass"));
        } catch (JenaException connEx) {
            vSet = null;
            ret.setMessage("Connection to Virtuoso failed!");
            ret.setStatusCode(-1);
            //System.out.println(connEx.getMessage());      
            out.println(mapper.writeValueAsString(ret));
            out.close();
            
            return;
        }
        
        // Make a connection to check if a database with the same name exists
        try{
            Class.forName("org.postgresql.Driver");     
        } catch (ClassNotFoundException ex) {
            System.out.println(ex.getMessage());    
            ret.setMessage("Could not load Postgis JDBC Driver!");
            ret.setStatusCode(-1);
            //System.out.println(connEx.getMessage());      
            out.println(mapper.writeValueAsString(ret));
            out.close();
            
            return;
        }
        try {
            //final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
            //databaseInitialiser.initialise(st.getDbConf());
                
            String url = DB_URL;//.concat(dbConf.getDBName());
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            //dbConn.setAutoCommit(false);
        } catch(SQLException sqlex) {
            System.out.println(sqlex.getMessage());      
            ret.setMessage("Connection to Postgis failed!");
            ret.setStatusCode(-1);
            //System.out.println(connEx.getMessage());      
            out.println(mapper.writeValueAsString(ret));
            out.close();
            
            return;
        }
        
        // Check database existance ( PostgreSQL specific )
        PreparedStatement stmt = dbConn.prepareStatement("SELECT 1 from pg_database WHERE datname = ? ");
        stmt.setString(1, dbConf.getDBName());
        ResultSet rs = stmt.executeQuery();
        
        // If it has a row then the database exists
        boolean createDB = true;
        if(rs.next()) {
            System.out.println("Database already exists");
            createDB = false;
        }
        
        // Create if needed
        if (createDB) {
            final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
            databaseInitialiser.initialise(dbConf);
        }
        
        ret.setMessage("Virtuoso and PostGIS connection established!");
        ret.setStatusCode(0);
            
        out.println(mapper.writeValueAsString(ret));
        
        //System.out.println(request.getParameter("v_url")+" "+request.getParameter("v_pass")+" "+request.getParameter("v_name"));
        HttpSession sess = request.getSession(true);
        sess.setAttribute("db_conf",  dbConf);
                
        } finally {
            if(vSet != null)
                vSet.close();
            if(dbConn != null)
                try {
                    //dbConn.commit();
                    dbConn.close();
                    //dbConn.commit();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
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
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(ConnectionServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ConnectionServlet.class.getName()).log(Level.SEVERE, null, ex);
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
