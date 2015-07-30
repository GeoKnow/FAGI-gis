package gr.athenainnovation.imis.fusion.gis.geotransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps left (nodeA) geometry.
 * @author Thomas Maroulis
 */
public class KeepLeftTransformation extends AbstractFusionTransformation {

    private static final String ID = "Keep A";
    private static final int intID = 3;
    
    @Override
    public void fuse(final Connection connection, final String nodeA, final String nodeB) throws SQLException {
        final String queryString = "SELECT ST_asText(geom) FROM dataset_a_geometries WHERE subject=?";            
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometry = resultSet.getString(1);
                //System.out.println("Geo: "+geometry);
                insertFusedGeometry(connection, nodeA, nodeB, geometry);
            }
        }
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
    public int getIntegerID() {
        return intID;
    }   
    
    @Override
    public void fuseAll(Connection connection) throws SQLException {
        final String queryString = "INSERT INTO fused_geometries (subject_A, subject_B, geom) "
                + "SELECT links.nodea, links.nodeb, dataset_a_geometries.geom "
                + "FROM links INNER JOIN dataset_a_geometries "
                + "ON (links.nodea = dataset_a_geometries.subject)";
        
        try (final PreparedStatement stmt = connection.prepareStatement(queryString)) {
            
            stmt.executeUpdate();            
            connection.commit();
        }
    }
    
    @Override
    public void fuseCluster(Connection connection) throws SQLException {
        final String queryString = "INSERT INTO fused_geometries (subject_A, subject_B, geom) "
                + "SELECT cluster.nodea, cluster.nodeb, dataset_a_geometries.geom "
                + "FROM cluster INNER JOIN dataset_a_geometries "
                + "ON (cluster.nodea = dataset_a_geometries.subject)";
        
        try (final PreparedStatement stmt = connection.prepareStatement(queryString)) {
            
            stmt.executeUpdate();            
            connection.commit();
        }
    }
}
