package gr.athenainnovation.imis.fusion.gis.transformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps left (nodeA) geometry.
 * @author Thomas Maroulis
 */
public class KeepLeftTransformation extends AbstractFusionTransformation {

    private static final String ID = "Keep left";

    @Override
    public void fuse(final Connection connection, final String nodeA, final String nodeB) throws SQLException {
        final String queryString = "SELECT ST_asText(geom) FROM dataset_a_geometries WHERE subject=?";            
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometry = resultSet.getString(1);

                //System.out.println("Fused geometry: nodeA nodeB geometry =  " + nodeA + nodeB + geometry);
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
    
}
