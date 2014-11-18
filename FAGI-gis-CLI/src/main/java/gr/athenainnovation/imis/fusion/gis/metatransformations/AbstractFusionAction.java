/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fusion.gis.metatransformations;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author nick
 */
/**
 * Abstract class for the definition of fusion transformations on metadata.
 */
public abstract class AbstractFusionAction {
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
}