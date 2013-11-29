package gr.athenainnovation.imis.fusion.gis.transformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract class for the definition of fusion transformations.
 * @author Thomas Maroulis
 */
public abstract class AbstractFusionTransformation {
    
    /**
     * Fuse geometries of given nodes.
     * @param connection connection to database
     * @param nodeA first node URI
     * @param nodeB second node URI
     * @throws SQLException
     */
    public abstract void fuse(final Connection connection, final String nodeA, final String nodeB) throws SQLException;
    
    /**
     * Score this transformation on its suitability for fusing the geometries of given nodes.
     * @param connection connection to database
     * @param nodeA first node URI
     * @param nodeB second node URI
     * @return score value in range [0.0...1.0]
     * @throws SQLException
     */
    public abstract double score(final Connection connection, final String nodeA, final String nodeB) throws SQLException;
    
    /**
     * 
     * @return an identifier for this transformation
     */
    public abstract String getID();
    
    /**
     * Insert fused geometry into database.
     * 
     * @param connection connection to database
     * @param nodeA URI of first node
     * @param nodeB URI of second node
     * @param fusedGeometry WKT serialisation of fused geometry
     * @return the return value of the update execution
     * @throws SQLException
     */
    protected int insertFusedGeometry(final Connection connection, final String nodeA, final String nodeB, final String fusedGeometry) throws SQLException {
        final String query = "INSERT INTO fused_geometries (subject_A, subject_B, geom) VALUES (?,?,ST_GeometryFromText(?, 4326))";
        
        try (final PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            statement.setString(3, fusedGeometry);
            int executionValue = statement.executeUpdate();
            connection.commit();
            
            return executionValue;
        }
        catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }
}