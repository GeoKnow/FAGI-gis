package gr.athenainnovation.imis.fusion.gis.metatransformations;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Keeps right (nodeB) geometry.
 * @author Thomas Maroulis
 */
public class KeepRightAction extends AbstractFusionAction {

    private static final String ID = "Keep Right Meta";
    
    @Override
    public void fuse(final Connection connection, final String nodeA, final String nodeB) throws SQLException {
        
    }

    @Override
    public double score(final Connection connection, final String nodeA, final String nodeB, Double threshold) throws SQLException {
        return 1.0;
    }

    @Override
    public String getID() {
        return ID;
    }
    
    @Override
    public void fuseAll(Connection connection) throws SQLException {
        
    }
    
}
