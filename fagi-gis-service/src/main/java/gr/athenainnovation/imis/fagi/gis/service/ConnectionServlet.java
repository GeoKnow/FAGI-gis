
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.mgt.Explain;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.SystemUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */

@WebServlet(name = "ConnectionServlet", urlPatterns = {"/ConnectionServlet"})
public class ConnectionServlet extends HttpServlet {
    
    private static final Logger LOG = Log.getClassFAGILogger(ConnectionServlet.class);    

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

        HttpSession                     sess;
        PrintWriter                     out = null;
        VirtGraph                       vSet = null;
        FusionState                     st = new FusionState();
        DBConfig                        dbConf = new DBConfig("", "", "", "", "", "", "");
        Connection                      dbConn = null;
        ObjectMapper                    mapper = new ObjectMapper();
        JSONRequestResult               ret = null;
        boolean                         succeded = true;
        
        try {
            out = response.getWriter();
            
            //mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            ret = new JSONRequestResult();

            // The only time we need a session if one does not exist
            sess = request.getSession(true);

            // Set logging state
            // Currrently when set to Debug, Jena really litters the output
            ARQ.setExecutionLogging(Explain.InfoLevel.ALL) ;
            Logger logger = Log.getFAGILogger();
            logger.setLevel(Level.TRACE);
            Enumeration e = logger.getAllAppenders();
            System.out.println(e.toString());
            while (e.hasMoreElements()) {
                Appender app = (Appender) e.nextElement();
                if ( app instanceof FileAppender) {
                    FileAppender fapp = (FileAppender)app;
                    if (SystemUtils.IS_OS_MAC_OSX) {
                        fapp.setFile("/Users/nickvitsas/Desktop/log.txt");
                    }
                    else if (SystemUtils.IS_OS_WINDOWS)  {
                        
                    }
                }
                    
            }
            
            /*
             Enumeration e = logger.getAllAppenders();
             System.out.println(e.toString());
             while ( e.hasMoreElements() ) {
             FileAppender app = (FileAppender) e.nextElement();
            
             System.out.println(app.);
             }
             */
            if (sess == null) {
                ret.setMessage("Failed to create session!");
                ret.setStatusCode(-1);
                //System.out.println(connEx.getMessage());      
                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }

            LOG.info("First Try");
            LOG.trace("First Try");
            response.setContentType("text/html;charset=UTF-8");
            
            st.setDbConf(dbConf);
            String relativeWebPath = "/FAGI-gis-WebInterface/lib/stopWords.ser";
            String absoluteDiskPath = getServletContext().getRealPath(relativeWebPath);

        //System.out.println("Rel Path "+System.getProperty("user.dir"));
            //System.out.println("Rel Path "+(new File(".")).getAbsolutePath());
            //System.out.println("Rel Path "+System.getenv());
            // Initialize Database configuration with provided values
            dbConf.setUsername(request.getParameter("v_name"));
            dbConf.setPassword(request.getParameter("v_pass"));
            dbConf.setDbURL(request.getParameter("v_url"));

            HttpAuthenticator authenticator = new SimpleAuthenticator(dbConf.getUsername(), dbConf.getPassword().toCharArray());
            sess.setAttribute("fg-sparql-auth", authenticator);
            dbConf.setDbName(request.getParameter("p_data"));
            dbConf.setDbUsername(request.getParameter("p_name"));
            dbConf.setDbPassword(request.getParameter("p_pass"));

            // Try a dummy connection to Virtuoso
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + request.getParameter("v_url") + "/CHARSET=UTF-8",
                        request.getParameter("v_name"),
                        request.getParameter("v_pass"));
            } catch (JenaException connEx) {
                LOG.error("Virtgraph Create Exception", connEx);
                vSet = null;
                ret.setMessage("Connection to Virtuoso failed!");
                ret.setStatusCode(-1);
                //System.out.println(connEx.getMessage());      
                out.println(mapper.writeValueAsString(ret));
                out.close();

                return;
            }

            // Try loading the postgres sql driver
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException ex) {
                LOG.error("Driver Class Not Found Exception", ex);
                ret.setMessage("Could not load Postgis JDBC Driver!");
                ret.setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                out.close();

                return;
            }

            // Try a dummy connection to Postgres to check if a database with the same name exists
            try {
                String url = Constants.DB_URL;
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
                //dbConn.setAutoCommit(false);
            } catch (SQLException sqlex) {
                LOG.error("Postgis Connect Exception", sqlex);
                ret.setMessage("Connection to Postgis failed!");
                ret.setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                out.close();

                return;
            }

            // Check database existance ( PostgreSQL specific )
            PreparedStatement stmt;
            // If it has a row then the database exists
            boolean createDB = true;
            try {
                stmt = dbConn.prepareStatement("SELECT 1 from pg_database WHERE datname = ? ");

                stmt.setString(1, dbConf.getDBName());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    System.out.println("Database already exists");
                    createDB = false;
                }
            } catch (SQLException ex) {
                LOG.trace("SQLException thrown table probing");
                LOG.debug("SQLException thrown table probing : " + ex.getMessage());
                LOG.debug("SQLException thrown table probing : " + ex.getSQLState());
            }
            
            // Create if needed
            if (createDB) {
                final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
                succeded = databaseInitialiser.initialise(dbConf);
            } else {
                final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
                succeded = databaseInitialiser.clearTables(dbConf);
            }

            if ( !succeded ) {
                LOG.error("Postgis database could not be set up");
                ret.setMessage("Postgis database could not be set up!");
                ret.setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));
                
                out.close();
                
                return;
            }
            
            ret.setMessage("Virtuoso and PostGIS connection established!");
            ret.setStatusCode(0);

            out.println(mapper.writeValueAsString(ret));

            //System.out.println(request.getParameter("v_url")+" "+request.getParameter("v_pass")+" "+request.getParameter("v_name"));
            sess.setAttribute("db_conf", dbConf);
        } catch (java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
            throw new ServletException("OutOfMemoryError thrown by Tomcat");
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
            throw new ServletException("JsonProcessingException thrown by Tomcat");
        } catch (IOException ex) {
            LOG.trace("IOException thrown");
            LOG.debug("IOException thrown : " + ex.getMessage());
            
            throw new ServletException("IOException opening the servlet writer");
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
