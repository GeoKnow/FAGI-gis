package gr.athenainnovation.imis.fusion.gis.transformations;

import static com.google.common.base.Preconditions.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Parametric transformation that keeps one of the two geometries and scales it. Both which geometry is kept and the scale factor are configurable.
 * @author Thomas Maroulis
 */
public class ScaleTransformation extends AbstractFusionTransformation {
    
    private static final String ID = "Scale";
    
    private boolean keepGeometryA;
    private double scaleFactor;
    private boolean scaleParamsSet = false;

    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {
        checkArgument(scaleParamsSet);
        
        final String queryString;
        final String node;
        
        if(keepGeometryA) {
            queryString = "SELECT ST_asText(ST_Scale(geom,?,?)) FROM dataset_a_geometries WHERE subject=?";
            node = nodeA;
        }
        else {
            queryString = "SELECT ST_asText(ST_Scale(geom,?,?)) FROM dataset_b_geometries WHERE subject=?";
            node = nodeB;
        }
        
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setDouble(1, scaleFactor);
            statement.setDouble(2, scaleFactor);
            statement.setString(3, node);
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometry = resultSet.getString(1);
                insertFusedGeometry(connection, nodeA, nodeB, geometry);
            }
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB) throws SQLException {
        checkArgument(scaleParamsSet);
        
        final String queryString;
        final String node;
        
        if(keepGeometryA) {
            queryString = "SELECT GeometryType(geom) FROM dataset_a_geometries WHERE subject=?";
            node = nodeA;
        }
        else {
            queryString = "SELECT GeometryType(geom) FROM dataset_b_geometries WHERE subject=?";
            node = nodeB;
        }
        
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, node);
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                final String geometryType = resultSet.getString(1);
                
                if("POINT".equals(geometryType)) {
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
    
    /**
     * Set transformation parameters. This method must be called at least once before {@link ScaleTransformation#fuse(java.sql.Connection, java.lang.String, java.lang.String)} or
     * {@link ScaleTransformation#score(java.sql.Connection, java.lang.String, java.lang.String)}.
     * @param keepGeometryA true if the left geometry (geometry A) is to be kept, false if the right geometry is to be kept
     * @param scaleFactor scale factor
     */
    public void setScaleParams(final boolean keepGeometryA, final double scaleFactor) {
        this.keepGeometryA = keepGeometryA;
        this.scaleFactor = scaleFactor;
        scaleParamsSet = true;
    }
    
    /**
     * 
     * @return true if the transformation parameters have been set, false otherwise
     */
    public boolean areScaleParamsSet() {
        return scaleParamsSet;
    }
}
