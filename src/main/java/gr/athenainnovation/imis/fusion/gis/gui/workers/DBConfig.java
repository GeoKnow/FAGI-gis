package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.*;

/**
 * Stores connection data for a database.
 * @author Thomas Maroulis
 */
public class DBConfig {
    private final String dbName, dbUsername, dbPassword;

    /**
     * Constructs a new instance with given database connetion data.
     * @param dbName database name
     * @param dbUsername database username
     * @param dbPassword database password
     */
    public DBConfig(final String dbName, final String dbUsername, final String dbPassword) {        
        this.dbName = checkNotNull(dbName, "DB name cannot be null.");
        this.dbUsername = checkNotNull(dbUsername, "DB username cannot be null");
        this.dbPassword = checkNotNull(dbPassword, "DB password cannot be null.");
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
}
