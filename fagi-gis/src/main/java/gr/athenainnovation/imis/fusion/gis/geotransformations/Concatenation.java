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
 * @author nick
 */
public class Concatenation extends AbstractFusionTransformation {
    private static final String ID = "Concatenation";  
        
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
    public void fuseAll(Connection connection) throws SQLException {
       
    }
}
