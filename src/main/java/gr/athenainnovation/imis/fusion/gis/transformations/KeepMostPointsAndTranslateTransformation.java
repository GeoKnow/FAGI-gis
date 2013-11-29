package gr.athenainnovation.imis.fusion.gis.transformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps the geometry with the most points and translates it so that its centroid matches the centroid of the other geometry.
 * @author Thomas Maroulis
 */
public class KeepMostPointsAndTranslateTransformation extends AbstractFusionTransformation {
    
    private static final String ID = "Keep most points and translate";

    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        final String queryStringA = "SELECT ST_NPoints(geom), ST_X(ST_Centroid(geom)), ST_Y(ST_Centroid(geom)) FROM dataset_a_geometries WHERE subject=?";
        final String queryStringB = "SELECT ST_NPoints(geom), ST_X(ST_Centroid(geom)), ST_Y(ST_Centroid(geom)) FROM dataset_b_geometries WHERE subject=?";
        
        final String getGeomAQueryString = "SELECT ST_asText(ST_Translate(geom,?,?)) FROM dataset_a_geometries WHERE subject=?";
        final String getGeomBQueryString = "SELECT ST_asText(ST_Translate(geom,?,?)) FROM dataset_b_geometries WHERE subject=?";
        
        try (final PreparedStatement statementA = connection.prepareStatement(queryStringA);
                final PreparedStatement statementB = connection.prepareStatement(queryStringB)) {
            statementA.setString(1, nodeA);
            statementB.setString(1, nodeB);
            
            final ResultSet resultSetA = statementA.executeQuery();
            final ResultSet resultSetB = statementB.executeQuery();
            
            while(resultSetA.next() && resultSetB.next()) {
                final int pointsA = resultSetA.getInt(1);
                final int pointsB = resultSetB.getInt(1);
                
                final double centroidA_X = resultSetA.getDouble(2);
                final double centroidA_Y = resultSetA.getDouble(3);
                final double centroidB_X = resultSetB.getDouble(2);
                final double centroidB_Y = resultSetB.getDouble(3);
                
                if(pointsA >= pointsB) {
                    final double dx = centroidB_X - centroidA_X;
                    final double dy = centroidB_Y - centroidA_Y;
                    final String geometry = getTranslatedGeometry(connection, getGeomAQueryString, nodeA, dx, dy);
                    insertFusedGeometry(connection, nodeA, nodeB, geometry);
                }
                else {
                    final double dx = centroidA_X - centroidB_X;
                    final double dy = centroidA_Y - centroidB_Y;
                    final String geometry = getTranslatedGeometry(connection, getGeomBQueryString, nodeB, dx, dy);
                    insertFusedGeometry(connection, nodeA, nodeB, geometry);
                }
            }
        }
    }
    
    private String getTranslatedGeometry(final Connection connection, final String query, final String node, final double dx, final double dy) throws SQLException {
        try(final PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, dx);
            statement.setDouble(2, dy);
            statement.setString(3, node);
            final ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getString(1);
            }
            else {
                throw new RuntimeException("Failed to retrieve geometry for node: " + node);
            }
        }
    }

    @Override
    public double score(final Connection connection, final String nodeA, final String nodeB) throws SQLException {
        return 1.0;
    }

    @Override
    public String getID() {
        return ID;
    }
}
