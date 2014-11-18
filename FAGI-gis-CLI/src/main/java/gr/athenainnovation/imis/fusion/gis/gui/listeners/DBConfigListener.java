package gr.athenainnovation.imis.fusion.gis.gui.listeners;

import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;

/**
 * Listener for changes to the database configuration.
 * @author Thomas Maroulis
 */
public interface DBConfigListener {
    /**
     * Notify listener of new database configuration.
     * @param dbConfig database configuration
     */
    void notifyNewDBConfiguration(final DBConfig dbConfig);
    
    /**
     * Notify listener database configuration has been reset.
     * Upon receipt listener should not make use of any local copies of a previously received database configuration, 
     * but rather wait to receive a new configuration via {@link DBConfigListener#notifyNewDBConfiguration(gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig) }.
     */
    void resetDBConfiguration();
    
    void notifyNewGraphConfiguration(final GraphConfig graphConfig);
    
}
