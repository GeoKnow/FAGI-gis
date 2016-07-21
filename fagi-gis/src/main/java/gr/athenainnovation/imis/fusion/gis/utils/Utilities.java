/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import static gr.athenainnovation.imis.fusion.gis.utils.Constants.NANOS_PER_SECOND;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
public class Utilities {

    private static final Logger LOG = Log.getClassFAGILogger(Utilities.class);    
    
    public static DBConfig setUpDefaultDatabase(String name, String mail) {
        final String DEFAULT_VIRT_USER = "dba";
        final String DEFAULT_VIRT_PASS = "dba";
        final String DEFAULT_VIRT_URL = "localhost:1111";
        final String DEFAULT_POST_DB = "fagi_"+name+"_"+mail.split("@")[0];
        final String DEFAULT_POST_USER = "postgres";
        final String DEFAULT_POST_PASS = "1111";
        
        boolean                         success = true;
        
        VirtGraph vSet = null;
        DBConfig                        dbConf = new DBConfig("", "", "", "", "", "", "");
        Connection                      dbConn = null;
        
        dbConf.setUsername(Credentials.SERVICE_VIRTUOSO_USER);
        dbConf.setPassword(Credentials.SERVICE_VIRTUOSO_PASS);
        dbConf.setDbURL(DEFAULT_VIRT_URL);

            //HttpAuthenticator authenticator = new SimpleAuthenticator(DEFAULT_VIRT_USER, DEFAULT_VIRT_PASS.toCharArray());
        //sess.setAttribute("fg-sparql-auth", authenticator);
        dbConf.setDbName(DEFAULT_POST_DB);
        dbConf.setDbUsername(Credentials.SERVICE_POSTGRES_USER);
        dbConf.setDbPassword(Credentials.SERVICE_POSTGRES_PASS);

        // Try a dummy connection to Virtuoso
        try {
            vSet = new VirtGraph("jdbc:virtuoso://" + DEFAULT_VIRT_URL + "/CHARSET=UTF-8",
                    Credentials.SERVICE_VIRTUOSO_USER,
                    Credentials.SERVICE_VIRTUOSO_PASS);
        } catch (JenaException connEx) {
            LOG.error("Virtgraph Create Exception", connEx);

        }

        // Try loading the postgres sql driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOG.info("Driver Class Not Found Exception");
            LOG.error("Driver Class Not Found Exception ", ex);

        }

        // Try a dummy connection to Postgres to check if a database with the same name exists
        try {
            String url = Constants.DB_URL;
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            //dbConn.setAutoCommit(false);
        } catch (SQLException sqlex) {
            LOG.info("Postgis Connect Exception");
            LOG.error("Postgis Connect Exception ", sqlex);
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
            success = databaseInitialiser.initialise(dbConf);
        } else {
            final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
            success = databaseInitialiser.clearTables(dbConf);
        }

        return dbConf;
    }
    
    
    public static String getPredicateOntology(String pred )
    {
        String onto = StringUtils.substringBefore(pred, "#");
        onto = onto.concat("#");
        if (onto.equals(pred)) {
            onto = StringUtils.substring(pred, 0, StringUtils.lastIndexOf(pred, "/"));
            onto = onto.concat("/");
        }
        
        return onto;
    }
        
    public static String getPredicateName(String pred )
    {
        return null;
    }
    
    public static String convertStreamToString(java.io.InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    // For debug purposes
    static final boolean DEBUG_REMOTE = false;
    public static boolean isLocalInstance(InetAddress addr) {
        if (!DEBUG_REMOTE) {
            // Check if the address is a valid special local or loop back
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
                return true;
            }

            // Check if the address is defined on any interface
            try {
                return NetworkInterface.getByInetAddress(addr) != null;
            } catch (SocketException e) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public static boolean isURLToLocalInstance(String url) {
        boolean isLocal;
        
        // Check if the address is defined on any interface
        try {
            URL endURL = new URL(url);
            isLocal = isLocalInstance(InetAddress.getByName(endURL.getHost())); //"localhost" for localhost
        }catch(UnknownHostException unknownHost) {
            isLocal = false;
        } catch (MalformedURLException ex) {
            isLocal = false;
        }
        
        return isLocal;
    }
    
    public static float nanoToSeconds(long nano) {
        return nano / NANOS_PER_SECOND;
    }
    
    public static List<String> findCommonPrefixedPropertyChains(String pattern, List<String> patterns) {
        List<String> ret = new ArrayList<>();
        final String truePattern = pattern+",";
                
        for ( String s : patterns) {
            // Handle comma separated cases
            if ( s.startsWith(truePattern) ) {
                ret.add(s);
                
                continue;
            }
            
            // Handle single depth cases
            if ( s.startsWith(pattern) ) {
                
                ret.add(s);
            }
            
        }
        
        return ret;
    }
}
