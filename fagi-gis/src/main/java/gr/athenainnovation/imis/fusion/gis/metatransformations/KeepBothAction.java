

package gr.athenainnovation.imis.fusion.gis.metatransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author nick
 */
public class KeepBothAction extends AbstractFusionAction {
    
    private static final String ID = "Keep Both Meta";
        
    private String graphA;
    private String graphB;
    private String graphTarget;

    public KeepBothAction(String graphA, String graphB, String graphTarget) {
        this.graphA = graphA;
        this.graphB = graphB;
        this.graphTarget = graphTarget;
    }
    
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
        String queryA = "SPARQL MOVE GRAPH <"+graphA+"> TO GRAPH <"+graphTarget+">";
        String queryB = "SPARQL MOVE GRAPH <"+graphB+"> TO GRAPH <"+graphTarget+">";
        long starttime, endtime;
                
        starttime =  System.nanoTime();
                
        PreparedStatement moveStmt;
        moveStmt = connection.prepareStatement(queryA);
        moveStmt.execute();
        moveStmt.close();
        
       // connection.commit();
        
        PreparedStatement moveStmt2 = connection.prepareStatement(queryB);
        //moveStmt2.execute();
        //moveStmt2.close();
        
        endtime =  System.nanoTime();
        System.out.println("Graphs Moved in "+(endtime-starttime)/1000000000f);
    }
    
}
