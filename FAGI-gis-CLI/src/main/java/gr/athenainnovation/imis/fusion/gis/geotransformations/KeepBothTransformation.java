package gr.athenainnovation.imis.fusion.gis.geotransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps both given geometries.
 * @author Thomas Maroulis
 */
public class KeepBothTransformation extends AbstractFusionTransformation {
    
    private static final String ID = "Keep both";

    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        final String queryString = "SELECT ST_asText(a.geom), ST_asText(b.geom) FROM dataset_a_geometries a, dataset_b_geometries b "
                + "WHERE a.subject=? AND b.subject=?";
        
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometryA = resultSet.getString(1);
                final String geometryB = resultSet.getString(2);
                
                insertFusedGeometry(connection, nodeA, nodeB, geometryA);
                insertFusedGeometry(connection, nodeA, nodeB, geometryB);
            }
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {
        return 1.0;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public void fuseAll(Connection connection) throws SQLException {
        final String insertGeomsFromA = "INSERT INTO fused_geometries (subject_A, subject_B, geom) \n" +
                                        "SELECT links.nodea, links.nodeb, a.geom \n" +
                                        "FROM links INNER JOIN dataset_a_geometries AS a\n" +
                                        "ON (links.nodea = a.subject)";
        
        final String insertGeomsFromB = "INSERT INTO fused_geometries (subject_A, subject_B, geom) \n" +
                                        "SELECT links.nodea, links.nodeb, b.geom \n" +
                                        "FROM links INNER JOIN dataset_b_geometries AS b\n" +
                                        "ON (links.nodeb = b.subject)";
        
        try (final PreparedStatement stmtInsertFromA = connection.prepareStatement(insertGeomsFromA)) {
            stmtInsertFromA.executeUpdate();
        }  
        
        try (final PreparedStatement stmtInsertFromB = connection.prepareStatement(insertGeomsFromB)) {
            stmtInsertFromB.executeUpdate();
        }
    }
}
