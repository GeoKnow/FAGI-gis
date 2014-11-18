package gr.athenainnovation.imis.fusion.gis.geotransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps the geometry with the most points.
 * @author Thomas Maroulis
 */
public class KeepMostPointsTransformation extends AbstractFusionTransformation {
    
    private static final String ID = "Keep most points";

    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        final String queryStringA = "SELECT ST_NPoints(geom), ST_asText(geom) FROM dataset_a_geometries WHERE subject=?";
        final String queryStringB = "SELECT ST_NPoints(geom), ST_asText(geom) FROM dataset_b_geometries WHERE subject=?";
        
        try (final PreparedStatement statementA = connection.prepareStatement(queryStringA);
                final PreparedStatement statementB = connection.prepareStatement(queryStringB)) {
            statementA.setString(1, nodeA);
            statementB.setString(1, nodeB);
            
            final ResultSet resultSetA = statementA.executeQuery();
            final ResultSet resultSetB = statementB.executeQuery();
            
            while(resultSetA.next() && resultSetB.next()) {
                final int pointsA = resultSetA.getInt(1);
                final int pointsB = resultSetB.getInt(1);
                
                if(pointsA >= pointsB) {
                    final String geometry = resultSetA.getString(2);
                    insertFusedGeometry(connection, nodeA, nodeB, geometry);
                }
                else {
                    final String geometry = resultSetB.getString(2);
                    insertFusedGeometry(connection, nodeA, nodeB, geometry);
                }
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
    public void fuseAll(Connection connection) throws SQLException {
        final String insertMostPoints = 
"INSERT INTO fused_geometries (subject_A, subject_B, geom)\n" +
"SELECT links.nodea, links.nodeb, CASE WHEN points_a >= points_b \n" +
"					THEN a_g\n" +
"					ELSE b_g\n" +
"				      END AS geom\n" +
"FROM links \n" +
"INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, \n" +
"		   dataset_b_geometries.subject AS b_s,\n" +
"		   dataset_a_geometries.geom AS a_g, \n" +
"		   dataset_b_geometries.geom AS b_g,\n" +
"		   ST_NPoints(dataset_a_geometries.geom) AS points_a,\n" +
"		   ST_NPoints(dataset_b_geometries.geom) AS points_b\n" +
"		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n" +
"		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
        
        try (final PreparedStatement stmt = connection.prepareStatement(insertMostPoints)) {
            
            stmt.executeUpdate();
        }
    }
}
