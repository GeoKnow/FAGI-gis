package gr.athenainnovation.imis.fusion.gis.postgis;

import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

/**
 * Handles construction and initialisation of a new PostGIS database.
 * @author Thomas Maroulis
 */
public class DatabaseInitialiser {
    private static final Logger LOG = Logger.getLogger(DatabaseInitialiser.class);
    
    private static final String DB_URL = "jdbc:postgresql:";
    
    private static final String DB_EXTENSIONS = "/extensions.sql";
    private static final String DB_SCHEMA = "/schema.sql";
    
    /**
     * Execute database initialization logic.
     * @param dbConfig database configuration
     * @throws SQLException 
     */
    public void initialise(final DBConfig dbConfig) throws SQLException{
        createDB(dbConfig);
        try (final Connection connection = connect(dbConfig.getDBName(), dbConfig.getDBUsername(), dbConfig.getDBPassword())) {
            executeIntialisationScripts(connection);
        }
    }
    
    
    // Create a new database
    private void createDB(final DBConfig dbConfig) throws SQLException {
        final String dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();
        
        Connection db = null;
        Statement stmt = null;
        String sql;
        
        try {
                db = DriverManager.getConnection(DB_URL, dbUsername, dbPassword);
                stmt = db.createStatement();
                
                sql = "DROP DATABASE IF EXISTS " + dbName;
                stmt.executeUpdate(sql);
                
                sql = "CREATE DATABASE " + dbName;
                stmt.executeUpdate(sql);
                
                LOG.info("Database creation complete");
        }
        finally {
            if(stmt != null) {
                stmt.close();
            }
            
            if(db != null) {
                db.close();
            }
        }
    }
    
    // Establish connection to database
    private Connection connect(final String dbName, final String dbUsername, final String dbPassword) throws SQLException {
        final String url = DB_URL.concat(dbName);
        final Connection dbConn = DriverManager.getConnection(url, dbUsername, dbPassword);
        dbConn.setAutoCommit(false);
        LOG.info("Connection to db established.");
        return dbConn;
    }
    
    // Execute database initialisation scripts.
    private void executeIntialisationScripts(final Connection db) throws SQLException {
        final ScriptRunner scriptRunner = new ScriptRunner(db);
        scriptRunner.setSendFullScript(true);
        final InputStream dbExtensionsStream = this.getClass().getResourceAsStream(DB_EXTENSIONS);
        if(dbExtensionsStream == null) {
            throw new NullPointerException("Failed to open extensions script.");
        }
        else {
            scriptRunner.runScript(new InputStreamReader(dbExtensionsStream));
        }
        final InputStream dbSchemaStream = this.getClass().getResourceAsStream(DB_SCHEMA);
        if(dbSchemaStream == null) {
            throw new NullPointerException("Failed to open schema script.");
        }
        else {
            scriptRunner.runScript(new InputStreamReader(dbSchemaStream));
        }
        
        LOG.info("Database initialised");
    }
}
