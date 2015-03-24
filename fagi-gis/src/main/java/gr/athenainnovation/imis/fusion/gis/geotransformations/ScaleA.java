/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.geotransformations;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.sqrt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author nick
 */
public class ScaleA extends AbstractFusionTransformation {
    private static final String ID = "ScaleA";  
        
    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {

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