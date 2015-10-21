package gr.athenainnovation.imis.fusion.gis.postgis;

import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 * Handles construction and initialization of a new PostGIS database.
 * @author Thomas Maroulis
 */
public class DatabaseInitialiser {
    
    private static final Logger LOG = Log.getClassFAGILogger(DatabaseInitialiser.class);    
    
    private static final String DB_EXTENSIONS = "/extensions.sql";
    private static final String DB_SCHEMA = "/schema.sql";
    
    /**
     * Execute database initialization logic.
     * @param dbConfig database configuration
     */
    public boolean initialise(final DBConfig dbConfig) {
        boolean success = true;
        
        success = createDB(dbConfig);
        
        try (final Connection connection = connect(dbConfig.getDBName(), dbConfig.getDBUsername(), dbConfig.getDBPassword())) {
            executeIntialisationScripts(connection);
            connection.close();
        } catch (SQLException ex) {
            LOG.trace("SQLException thrown during scripts execution");
            LOG.debug("SQLException thrown during scripts execution : "+ ex.getMessage());
            LOG.debug("SQLException thrown during scripts execution : "+ ex.getSQLState());
            
            success = false;
        }
        
        return success;
    }    
    
    // Create a new database
    private boolean createDB(final DBConfig dbConfig) {
        boolean success = true;
        final String dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();

        Connection db = null;
        Statement stmt = null;
        String sql;
        try {
            Class.forName("org.postgresql.Driver");
            db = DriverManager.getConnection(Constants.DB_URL, dbUsername, dbPassword);
            db.setAutoCommit(false);
            stmt = db.createStatement();

            sql = "DROP DATABASE IF EXISTS " + dbName;
            stmt.executeUpdate(sql);

            sql = "CREATE DATABASE " + dbName;
            stmt.executeUpdate(sql);

            db.commit();
            
            LOG.info(ANSI_YELLOW + "Database creation complete" + ANSI_RESET);
        } catch (ClassNotFoundException ex) {
            LOG.trace("Postgis Connect Exception", ex);
            LOG.debug("Postgis Connect Exception", ex);
                            
            success = false;
        } catch (SQLException ex) {
            LOG.trace("SQLException thrown during creation of Database and Extentions");
            LOG.debug("SQLException thrown during creation of Database and Extentions : "+ ex.getMessage());
            LOG.debug("SQLException thrown during creation of Database and Extentions : "+ ex.getSQLState());
                            
            success = false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during close of statement for creation of Database and Extentions");
                    LOG.debug("SQLException thrown during close of statement for creation of Database and Extentions : "+ ex.getMessage());
                    LOG.debug("SQLException thrown during close of statement for creation of Database and Extentions : "+ ex.getSQLState());
                    
                }
            }

            if (db != null) {
                try {
                    if ( !success )
                        db.rollback();
                    db.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during close of connection for creation of Database and Extentions");
                    LOG.debug("SQLException thrown during close of connection for creation of Database and Extentions : "+ ex.getMessage());
                    LOG.debug("SQLException thrown during close of connection for creation of Database and Extentions : "+ ex.getSQLState());
                    
                }
            }
        }
        
        return success;
    }
    
    // Establish connection to database
    private Connection connect(final String dbName, final String dbUsername, final String dbPassword) throws SQLException {
        final String url = Constants.DB_URL.concat(dbName);
        final Connection dbConn = DriverManager.getConnection(url, dbUsername, dbPassword);
        // Only time we use autocommit ON because
        // you cannot create a DB in transactional mode
        dbConn.setAutoCommit(true);
        LOG.info(ANSI_YELLOW+"Connection to db established."+ANSI_RESET);
        
        return dbConn;
    }
    
    // Execute database initialisation scripts.
    private boolean executeIntialisationScripts(final Connection db) {
        boolean success = true;
        
        final ScriptRunner scriptRunner = new ScriptRunner(db);
        scriptRunner.setSendFullScript(true);
        final InputStream dbExtensionsStream = this.getClass().getResourceAsStream(DB_EXTENSIONS);
        if(dbExtensionsStream == null) {
            LOG.trace("Failure in creation of Extentions. Could not find resource with name " + DB_EXTENSIONS);
            LOG.debug("Failure in creation of Extentions. Could not find resource with name " + DB_EXTENSIONS);
        }
        else {
            scriptRunner.runScript(new InputStreamReader(dbExtensionsStream));
            LOG.trace("Extentions loaded");
        }
        final InputStream dbSchemaStream = this.getClass().getResourceAsStream(DB_SCHEMA);
        if(dbSchemaStream == null) {
            LOG.trace("Failure in creation of Database schema. Could not find resource with name " + DB_SCHEMA);
            LOG.debug("Failure in creation of Database schema. Could not find resource with name " + DB_SCHEMA);
        }
        else {
            scriptRunner.runScript(new InputStreamReader(dbSchemaStream));
            LOG.trace("Schema loaded");
        }
        
        LOG.info(ANSI_YELLOW+"Database initialised"+ANSI_RESET);
        
        return success;
    }
    
    public boolean clearTables(final DBConfig dbConfig) {
        boolean success = true;
        final String dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();
        
        Connection db = null;
        PreparedStatement stmt = null;
        String sql;
        try {
            Class.forName("org.postgresql.Driver");
            final String url = Constants.DB_URL.concat(dbConfig.getDBName());
            db = DriverManager.getConnection(url, dbUsername, dbPassword);
            db.setAutoCommit(false);
            
            String deleteATable = "DELETE FROM dataset_b_geometries";
            stmt = db.prepareStatement(deleteATable);
            stmt.executeUpdate();

            stmt.close();

            String deleteBTable = "DELETE FROM dataset_a_geometries";
            stmt = db.prepareStatement(deleteBTable);
            stmt.executeUpdate();

            stmt.close();
            
            String deleteFTable = "DELETE FROM fused_geometries";
            stmt = db.prepareStatement(deleteFTable);
            stmt.executeUpdate();

            stmt.close();
            
            db.commit();
            
            success = true;
        }
        catch (ClassNotFoundException ex) {
            LOG.trace("ClassNotFoundException thrown during tables deletion");
            LOG.debug("ClassNotFoundException thrown during tables deletion : " + ex.getMessage());
                
            success = false;
        } catch (SQLException ex) {
            LOG.trace("SQLException thrown during tables deletion");
            LOG.debug("SQLException thrown during tables deletion : " + ex.getMessage());
            LOG.debug("SQLException thrown during tables deletion : " + ex.getSQLState());
                
            success = false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during tables deletion statement close");
                    LOG.debug("SQLException thrown during tables deletion statement close : " + ex.getMessage());
                    LOG.debug("SQLException thrown during tables deletion statement close : " + ex.getSQLState());
                }
            }

            if (db != null) {
                try {
                    if ( !success )
                        db.rollback();
                    db.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during tables deletion connection close");
                    LOG.debug("SQLException thrown during tables deletion connection close : " + ex.getMessage());
                    LOG.debug("SQLException thrown during tables deletion connection close : " + ex.getSQLState());
                }
            }
        }
        
        return success;
    }
    
}
