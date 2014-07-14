package gr.athenainnovation.imis.fusion.gis.transformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract class for the definition of fusion transformations.
 */
public abstract class AbstractFusionTransformation {
    public static final int WGS84_SRID = 4326;
    
    public abstract void fuseAll(final Connection connection) throws SQLException;
    
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
     * @param threshold threshold
     * @return score value in range [0.0...1.0]
     * @throws SQLException
     */
    public abstract double score(final Connection connection, final String nodeA, final String nodeB, final Double threshold) throws SQLException;
    
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
     * @param fusedGeometry WKT serialization of fused geometry
     * @return the return value of the update execution
     * @throws SQLException
     */
    
    protected int insertFusedGeometry(final Connection connection, final String nodeA, final String nodeB, final String fusedGeometry) throws SQLException {
         
        //4326 srid
        final String query = "INSERT INTO fused_geometries (subject_A, subject_B, geom) VALUES (?,?,ST_GeometryFromText(?,"+WGS84_SRID+"))"; 

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
    
    protected int insertShiftedGeometry(final Connection connection, final String nodeA, final String nodeB, double deltaX, double deltaY, String geometryB) throws SQLException {
        
        //translation computes with srid 4326
        final String query = "INSERT INTO fused_geometries (subject_A, subject_B, geom) VALUES (?,?,ST_Translate(   ST_GeometryFromText(?,"+WGS84_SRID+"),?,? ))"; 
        //System.out.println(geometryB);
        try (final PreparedStatement statement = connection.prepareStatement(query)) {            
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            statement.setString(3, geometryB);
            statement.setDouble(4, deltaX);
            statement.setDouble(5, deltaY);
            
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