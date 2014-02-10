package gr.athenainnovation.imis.fusion.gis.transformations;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.sqrt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

 /**
 * Translates (shifts) the polygon to a new centroid, computed by the average distance of the original centroid and the point geometry
 *  
 */
public class ShiftPolygonToAverageDistance extends AbstractFusionTransformation {
    private static final String ID = "ShiftPolygonToAverageDistance";  
        
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
                
                final double avgX = (geometryA_X - geometryB_X) / 2;
                final double avgY = (geometryA_Y - geometryB_Y) / 2;
                
                insertShiftedGeometry(connection, nodeA, nodeB, avgX, avgY, geometryB);
            }
        }
    }

    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {        
        double score;
        //we transform the srid to 3035 in order to get distance in meters
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
                
                //must be POINT and POLYGON, threshold < distance and threshold !=-1. -1 is the default value if there is no threshold specified from the user.  
                if(!"POINT".equals(geometryAType.toUpperCase()) || !"POLYGON".equals(geometryBType.toUpperCase()) || (threshold < distance && threshold != -1)) {
                    //geometries don' t match, return 0 score
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