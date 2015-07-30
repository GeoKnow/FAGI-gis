

package gr.athenainnovation.imis.fusion.gis.metatransformations;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author nick
 */
public class KeepLeftAction extends AbstractFusionAction {
    
    private static final String ID = "Keep Left Meta";
    
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
