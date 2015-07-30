package gr.athenainnovation.imis.fusion.gis.geotransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fuses two point geometries by averaging their x, y values. 
 * @author Thomas Maroulis
 */
public class AvgTwoPointsTransformation extends AbstractFusionTransformation {
    
    private static final String ID = "Average two points";
    private static final int intID = 10;
    
    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        final String queryString = "SELECT ST_X(a.geom), ST_Y(a.geom), ST_X(b.geom), ST_Y(b.geom) FROM dataset_a_geometries a, dataset_b_geometries b "
                + "WHERE a.subject=? AND b.subject=?";
        
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final double geometryA_X = resultSet.getDouble(1);
                final double geometryA_Y = resultSet.getDouble(2);
                final double geometryB_X = resultSet.getDouble(3);
                final double geometryB_Y = resultSet.getDouble(4);
                
                final double avgX = (geometryA_X + geometryB_X) / 2;
                final double avgY = (geometryA_Y + geometryB_Y) / 2;
                
                final String fusedGeometry = "POINT(" + avgX + " " + avgY + ")";
                insertFusedGeometry(connection, nodeA, nodeB, fusedGeometry);
            }
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {
        final String queryString = "SELECT GeometryType(a.geom), GeometryType(b.geom) FROM dataset_a_geometries a, dataset_b_geometries b "
                + "WHERE a.subject=? AND b.subject=?";
        
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometryAType = resultSet.getString(1);
                final String geometryBType = resultSet.getString(2);
                
                if(!"POINT".equals(geometryAType.toUpperCase()) || !"POINT".equals(geometryBType.toUpperCase())) {
                    return 0.0;
                }
            }
            
            return 1.0;
        }
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
    public void fuseAll(Connection connection) {
        /*final String queryString = "INSERT INTO fused_geometries (subject_A, subject_B, geom) "
                + "SELECT links.nodea, links.nodeb, dataset_a_geometries.geom "
                + "FROM links INNER JOIN dataset_a_geometries "
                + "ON (links.nodea = dataset_a_geometries.subject)\n";
        
        try (
            final PreparedStatement statement = connection.(queryString)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final double geometryA_X = resultSet.getDouble(1);
                final double geometryA_Y = resultSet.getDouble(2);
                final double geometryB_X = resultSet.getDouble(3);
                final double geometryB_Y = resultSet.getDouble(4);
                
                final double avgX = (geometryA_X + geometryB_X) / 2;
                final double avgY = (geometryA_Y + geometryB_Y) / 2;
                
                final String fusedGeometry = "POINT(" + avgX + " " + avgY + ")";
                insertFusedGeometry(connection, nodeA, nodeB, fusedGeometry);
            }
        }*/
    }

    @Override
    public void fuseCluster(Connection connection) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
