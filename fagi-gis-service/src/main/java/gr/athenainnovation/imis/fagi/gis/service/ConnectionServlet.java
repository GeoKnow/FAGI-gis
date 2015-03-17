
package gr.athenainnovation.imis.fagi.gis.service;

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
import java.sql.SQLException;
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
            //System.out.println(connEx.getMessage());      
            out.println("Connection to virtuoso failed");
            out.close();
            
            return;
        }
        
        System.out.println();
        final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
        databaseInitialiser.initialise(dbConf);
        
         try{
            Class.forName("org.postgresql.Driver");     
        } catch (ClassNotFoundException ex) {
            System.out.println(ex.getMessage());      
            out.println("Class of postgis failed");
            out.close();
            
            return;
        }
        try {
            //final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
            //databaseInitialiser.initialise(st.getDbConf());
                
            String url = DB_URL.concat(dbConf.getDBName());
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            //dbConn.setAutoCommit(false);
        } catch(SQLException sqlex) {
            System.out.println(sqlex.getMessage());      
            out.println("Connection to postgis failed");
            out.close();
            
            return;
        }
        
        out.println("Virtuoso and PostGIS connection established");
        
        //System.out.println(request.getParameter("v_url")+" "+request.getParameter("v_pass")+" "+request.getParameter("v_name"));
        HttpSession sess = request.getSession(true);
        sess.setAttribute("db_conf",  dbConf);
                
        //while ( sess.getAttributeNames().hasMoreElements()) {
            //System.out.println(sess.getAttributeNames().nextElement());
            //sess.getAttributeNames().
        //}
        //sess.invalidate();
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
