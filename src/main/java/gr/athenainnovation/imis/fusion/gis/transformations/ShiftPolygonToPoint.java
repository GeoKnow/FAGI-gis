package gr.athenainnovation.imis.fusion.gis.transformations;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.sqrt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

 /**
 * Translates (shifts) the polygon to a new centroid. The new centroid is the coordinates of the POINT geometry
 */
public class ShiftPolygonToPoint extends AbstractFusionTransformation {
    private static final String ID = "ShiftPolygonToPoint";  
        
    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {

        final String queryString = "SELECT ST_X(a.geom), ST_Y(a.geom), ST_X(ST_Centroid(b.geom)), ST_Y(ST_Centroid(b.geom)), ST_AsText(b.geom) FROM dataset_a_geometries a, dataset_b_geometries b "
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
                final String geometryB = resultSet.getString(5);
                
                //calculating the shift of the new centroid from the point and centroid coordinates
                final double deltaX = geometryA_X - geometryB_X;
                final double deltaY = geometryA_Y - geometryB_Y;
                
                insertShiftedGeometry(connection, nodeA, nodeB, deltaX, deltaY, geometryB);
            }
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {        
        double score;
        //transform from 4326 to other srid (3035) 
        final String queryString = "SELECT GeometryType(a.geom), GeometryType(b.geom), ST_Distance(ST_Transform(a.geom, 3035), ST_Centroid(ST_Transform(b.geom,3035))) FROM dataset_a_geometries a, dataset_b_geometries b "
                + "WHERE a.subject=? AND b.subject=?";
                
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, nodeA);
            statement.setString(2, nodeB);

            final ResultSet resultSet = statement.executeQuery();            
            while(resultSet.next()) {
                //get geometries
                final String geometryAType = resultSet.getString(1);
                final String geometryBType = resultSet.getString(2);
                
                //get distance between geometries
                final double distance = resultSet.getDouble(3);
                
                
                //must be POINT and POLYGON, threshold < distance and threshold !=-1. -1 is default value for no threshold from the user.  
                if(!"POINT".equals(geometryAType.toUpperCase()) || !"POLYGON".equals(geometryBType.toUpperCase()) || (threshold < distance && threshold != -1)) {
                    //geometries don' t match or threshold < distance, return 0 score
                    return 0.0;
                }
                //check if user provided threshold
                if (threshold == -1.0){
                    return 1.0;
                }
                else
                {
                  //score computing formula  
                  score = sqrt((abs(threshold) - distance)/threshold);
                  return score;
                }                                    
                
            }           
            return 0.0;
        }
    }

    @Override
    public String getID() {
        return ID;
    }
}    