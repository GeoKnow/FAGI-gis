package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.*;

/**
 * Stores connection data for a database.
 */
public class DBConfig {
    private final String dbName, dbUsername, dbPassword, dbURL, username, password;

    /**
     * Constructs a new instance with given database connection data.
     * @param dbName database name
     * @param dbUsername database username
     * @param dbPassword database password
     * @param dbURL virtuoso database url
     * @param username virtuoso username
     * @param password virtuoso password
     */
    public DBConfig(final String dbName, final String dbUsername, final String dbPassword, final String dbURL, final String username, final String password) {        
        this.dbName = checkNotNull(dbName, "DB name cannot be null.");
        this.dbUsername = checkNotNull(dbUsername, "DB username cannot be null");
        this.dbPassword = checkNotNull(dbPassword, "DB password cannot be null.");
        //for virtuoso 
        this.dbURL = checkNotNull(dbURL, "Virtuoso URL cannot be null");
        this.username = checkNotNull(username, "Virtuoso username cannot be null");
        this.password = checkNotNull(password, "Virtuoso password cannot be null.");
    }

    /**
     *
     * @return database name
     */
    public String getDBName() {
        return dbName;
    }

    /**
     *
     * @return database username
     */
    public String getDBUsername() {
        return dbUsername;
    }

    /**
     *
     * @return database password
     */
    public String getDBPassword() {
        return dbPassword;
    }
    
     /**
     * @return virtuoso database URL
     */
    public String getDBURL() {
        return dbURL;
    }
    
     /**
     * @return virtuoso username
     */
    public String getUsername() {
        return username;
    }
    
     /**
     * @return virtuoso password
     */
    public String getPassword() {
        return password;
    }
    
}
