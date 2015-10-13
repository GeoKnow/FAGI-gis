/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.geotransformations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Nick Vitsas
 */
public class Concatenation extends AbstractFusionTransformation {
    private static final String ID = "Concatenation";  
    private static final int intID = 1;
    
    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        final String queryStringA = "SELECT ST_asText(geom) FROM dataset_a_geometries WHERE subject=?";            
        final String queryStringB = "SELECT ST_asText(geom) FROM dataset_b_geometries WHERE subject=?";            
        try (final PreparedStatement statementA = connection.prepareStatement(queryStringA);
             final PreparedStatement statementB = connection.prepareStatement(queryStringB)) {
            statementA.setString(1, nodeA);
            statementB.setString(1, nodeB);
            final ResultSet resultSetA = statementA.executeQuery();
            final ResultSet resultSetB = statementB.executeQuery();
            
            String geomA = "";
            String geomB = "";
            while(resultSetA.next()) {
                geomA = resultSetA.getString(1);
                System.out.println("Geo: "+geomA);
                //insertFusedGeometryCollection(connection, nodeA, nodeB, geomA, geomB);
            }
            while(resultSetB.next()) {
                geomB = resultSetB.getString(1);
                System.out.println("Geo: "+geomB);
                //insertFusedGeometryCollection(connection, nodeA, nodeB, geomA, geomB);
            }
            
            insertFusedGeometryCollection(connection, nodeA, nodeB, geomA, geomB);
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {        
        
        return 0.0;
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
        final String concatrGeoms = "INSERT INTO fused_geometries (subject_A, subject_B, geom)\n"
                + "SELECT links.nodea, links.nodeb, ST_GeometryFromText ( ('GEOMETRYCOLLECTION(' || a_g || ', ' || b_g || ')'), " + WGS84_SRID + ") AS geom\n"
                + "FROM links \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n"
                + "		   dataset_b_geometries.subject AS b_s,\n"
                + "		   ST_AsText(dataset_a_geometries.geom) AS a_g,\n"
                + "		   ST_AsText(dataset_b_geometries.geom) AS b_g\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n"
                + "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";

        try (final PreparedStatement statementA = connection.prepareStatement(concatrGeoms);) {
            statementA.execute();
            connection.commit();
        }
    }

    @Override
    public void fuseCluster(Connection connection) throws SQLException {
        final String concatrGeoms = "INSERT INTO fused_geometries (subject_A, subject_B, geom)\n"
                + "SELECT cluster.nodea, cluster.nodeb, ST_GeometryFromText ( ('GEOMETRYCOLLECTION(' || a_g || ', ' || b_g || ')'), " + WGS84_SRID + ") AS geom\n"
                + "FROM cluster \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n"
                + "		   dataset_b_geometries.subject AS b_s,\n"
                + "		   ST_AsText(dataset_a_geometries.geom) AS a_g,\n"
                + "		   ST_AsText(dataset_b_geometries.geom) AS b_g\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n"
                + "		ON(cluster.nodea = geoms.a_s AND cluster.nodeb = geoms.b_s)";

        try (final PreparedStatement stmt = connection.prepareStatement(concatrGeoms);) {
            stmt.executeUpdate();
            connection.commit();
        }
    }
    
//  public static void main(String[] args) {
//      final String insertShiftToPoint = 
//        "INSERT INTO fused_geometries (subject_A, subject_B, geom) \n" +
//        "SELECT links.nodea, links.nodeb, ST_Translate(b_g, a_x-b_x,a_y-b_y)\n" +
//        "FROM links \n" +
//        "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n" +
//        "		   dataset_b_geometries.subject AS b_s,\n" +
//        "		   dataset_a_geometries.geom AS a_g,\n" +
//        "		   dataset_b_geometries.geom AS b_g,\n" +
//        "		   ST_X(ST_Centroid(dataset_a_geometries.geom)) AS a_x,\n" +
//        "		   ST_Y(ST_Centroid(dataset_a_geometries.geom)) AS a_y,\n" +
//        "		   ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x,\n" +
//        "		   ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n" +
//        "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n" +
//        "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
//        
//        System.out.println(insertShiftToPoint);
//  }
}
