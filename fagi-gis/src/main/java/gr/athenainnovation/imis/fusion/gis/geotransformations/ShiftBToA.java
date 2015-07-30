
package gr.athenainnovation.imis.fusion.gis.geotransformations;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.sqrt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


 /**
 * Translates (shifts) geometry B to A. Uses Centroids if needed.
 * 
 * @author imis-nkarag
 */

public class ShiftBToA extends AbstractFusionTransformation {
    private static final String ID = "ShiftBToA";  
    private static final int intID = 7;
    private float shift;
    private float scale;
    private float rotate;
    
    @Override
    public void fuse(Connection connection, String nodeA, String nodeB) throws SQLException {

        final String queryString = "SELECT ST_X(ST_Centroid(a.geom)), ST_Y(ST_Centroid(a.geom)), ST_X(ST_Centroid(b.geom)), ST_Y(ST_Centroid(b.geom)), ST_AsText(b.geom) FROM dataset_a_geometries a, dataset_b_geometries b "
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
                final String geometryA = resultSet.getString(5);
                
                //calculating the shift of the new centroid from the point and centroid coordinates
                final double deltaX = geometryA_X - geometryB_X;
                final double deltaY = geometryA_Y - geometryB_Y;
                
                String shiftedGeom = "";
                System.out.println(geometryA);
                shiftedGeom = applyRotate(connection, geometryA);
                System.out.println(shiftedGeom);
                shiftedGeom = applyScale(connection, shiftedGeom);
                System.out.println(shiftedGeom);
                shiftedGeom = applyTransformation(connection, shiftedGeom, deltaX, deltaY);
                System.out.println(shiftedGeom);

                //insertShiftedGeometry(connection, nodeA, nodeB, deltaX, deltaY, geometryA);
                insertFusedGeometry(connection, nodeA, nodeB, shiftedGeom);
            }
        }
    }

    private String applyTransformation (Connection connection, String geom, double deltaX, double deltaY) throws SQLException {
        final String queryString = "SELECT ST_AsText ( ST_Translate( ST_GeometryFromText(?,"+WGS84_SRID+"),?,?) )";
        String geometryA = "";
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, geom);
            statement.setFloat(2, (float) ( ( this.shift / 100 ) * deltaX));
            statement.setFloat(3, (float) ( ( this.shift / 100 ) * deltaY));
            
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                geometryA = resultSet.getString(1);
            } 
        }
        
        return geometryA;
    }
    
    private String applyScale(Connection connection, String geom) throws SQLException {
        final String queryString = "SELECT ST_AsText ( ST_Scale( ST_GeometryFromText(?,"+WGS84_SRID+"),?,? ) )";
        String geometryA = "";
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, geom);
            statement.setFloat(2, (float) this.scale);
            statement.setFloat(3, (float) this.scale);
            System.out.println("Scale : "+this.scale);
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                geometryA = resultSet.getString(1);
            } 
            
            
        }
        
        return geometryA;
    }
    
    private String applyRotate(Connection connection, String geom) throws SQLException {
        //final String queryString = "SELECT ST_AsText ( ST_Rotate( ST_Transform(ST_GeometryFromText(?,"+WGS84_SRID+"), 3786),?, ST_Centroid(ST_Transform(ST_GeometryFromText(?,"+WGS84_SRID+"), 3786) ) ) )";
        final String queryString = "SELECT ST_AsText ( ST_Rotate( ST_GeometryFromText(?,"+WGS84_SRID+"),?, ST_Centroid( ST_GeometryFromText(?,"+WGS84_SRID+") ) ) )";
        final String queryStringD = "SELECT ST_AsText(ST_Transform(ST_GeometryFromText(?, 3786),"+WGS84_SRID+")) ";
        //final String queryStringD = "SELECT ST_AsText(ST_GeometryFromText(?, 3786)) ";
        //final String queryStringD = "SELECT ST_AsText(ST_Transform(ST_GeometryFromText(?, 3786),"+WGS84_SRID+")) ";
        float angle_in_rads = (float) (this.rotate * (Math.PI / 180));
        String geometryA = "";
        String deb = "";
        try (final PreparedStatement statement = connection.prepareStatement(queryString)) {
            
            
            statement.setString(1, geom);
            statement.setFloat(2, (float) angle_in_rads);
            statement.setString(3, geom);
            System.out.println("Rot : "+this.rotate+" "+angle_in_rads);
            final ResultSet resultSet = statement.executeQuery();
            
            while(resultSet.next()) {
                geometryA = resultSet.getString(1);
            } 
            
            final PreparedStatement statementD = connection.prepareStatement(queryStringD);
            statementD.setString(1, geometryA);
            final ResultSet resultSetD = statementD.executeQuery();
            
            while(resultSetD.next()) {
                deb = resultSetD.getString(1);
                System.out.print("Debug print "+deb);
            } 
        }
        
        //return deb;
        return geometryA;
    }
    
    @Override
    public double score(Connection connection, String nodeA, String nodeB, Double threshold) throws SQLException {        
        double score;
        //transform from 4326 to other srid (3035) 
        final String queryString = "SELECT GeometryType(a.geom), GeometryType(b.geom), ST_Distance(ST_Transform(a.geom, 900913), ST_Centroid(ST_Transform(b.geom,900913))) FROM dataset_a_geometries a, dataset_b_geometries b "
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

    @Override
    public int getIntegerID() {
        return intID;
    }    
    
    @Override
    public void fuseAll(Connection connection) throws SQLException {
        final String createScaleFunc = "CREATE OR REPLACE FUNCTION GeomResize(GEOMETRY, FLOAT) RETURNS GEOMETRY AS '\n"
                + "SELECT ST_Translate(ST_Scale($1, $2, $2), ST_X(ST_Centroid($1))*(1 - \n"
                + "$2), ST_Y(ST_Centroid($1))*(1 - $2)) AS resized_geometry;\n"
                + "' LANGUAGE 'sql';";
        final String createRotateFunc = "CREATE OR REPLACE FUNCTION GeomRotate(GEOMETRY, FLOAT) RETURNS GEOMETRY AS '\n"
                + "SELECT ST_Translate(ST_Rotate(ST_Translate(ST_Transform($1, 2249), -ST_X(ST_Centroid(ST_Transform($1, 2249))), -ST_Y(ST_Centroid(ST_Transform($1, 2249)))), radians($2)), ST_X(ST_Centroid(ST_Transform($1, 2249))), ST_Y(ST_Centroid(ST_Transform($1, 2249)))) AS rotated_geometry;\n"
                + "' LANGUAGE 'sql'";
        
        final String insertShiftToPoint =
                "INSERT INTO fused_geometries (subject_A, subject_B, geom) \n"
                + "SELECT links.nodea, links.nodeb, ST_Transform(GeomRotate(GeomResize(ST_Translate(b_g, (a_x-b_x) * (?), (a_y-b_y) * (?) ), ?), ?), 4326) AS geom\n"
                + "FROM links \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n"
                + "		   dataset_b_geometries.subject AS b_s,\n"
                + "		   dataset_a_geometries.geom AS a_g,\n"
                + "		   dataset_b_geometries.geom AS b_g,\n"
                + "		   ST_X(ST_Centroid(dataset_a_geometries.geom)) AS a_x,\n"
                + "		   ST_Y(ST_Centroid(dataset_a_geometries.geom)) AS a_y,\n"
                + "		   ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x,\n"
                + "		   ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n"
                + "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
        try (final PreparedStatement stmt = connection.prepareStatement(insertShiftToPoint);
             final PreparedStatement stmtCreateScaleFunc = connection.prepareStatement(createScaleFunc);
             final PreparedStatement stmtCreateRotateFunc = connection.prepareStatement(createRotateFunc)) {
            
            stmt.setFloat(1, (float) ( this.shift / 100 ));
            stmt.setFloat(2, (float) ( this.shift / 100 ));
            stmt.setFloat(3, (float) ( this.scale ));
            stmt.setFloat(4, (float) ( this.rotate ));
            
            //stmtCreateScaleFunc.executeUpdate();
            //stmtCreateRotateFunc.executeUpdate();
            stmt.executeUpdate();            
            connection.commit();
        }
    }

    @Override
    public void fuseCluster(Connection connection) throws SQLException {
        final String createScaleFunc = "CREATE OR REPLACE FUNCTION GeomResize(GEOMETRY, FLOAT) RETURNS GEOMETRY AS '\n"
                + "SELECT ST_Translate(ST_Scale($1, $2, $2), ST_X(ST_Centroid($1))*(1 - \n"
                + "$2), ST_Y(ST_Centroid($1))*(1 - $2)) AS resized_geometry;\n"
                + "' LANGUAGE 'sql';";
        final String createRotateFunc = "CREATE OR REPLACE FUNCTION GeomRotate(GEOMETRY, FLOAT) RETURNS GEOMETRY AS '\n"
                + "SELECT ST_Translate(ST_Rotate(ST_Translate(ST_Transform($1, 2249), -ST_X(ST_Centroid(ST_Transform($1, 2249))), -ST_Y(ST_Centroid(ST_Transform($1, 2249)))), radians($2)), ST_X(ST_Centroid(ST_Transform($1, 2249))), ST_Y(ST_Centroid(ST_Transform($1, 2249)))) AS rotated_geometry;\n"
                + "' LANGUAGE 'sql'";
        
        final String insertShiftToPoint =
                "INSERT INTO fused_geometries (subject_A, subject_B, geom) \n"
                + "SELECT cluster.nodea, cluster.nodeb, ST_Transform(GeomRotate(GeomResize(ST_Translate(b_g, (a_x-b_x) * (?), (a_y-b_y) * (?) ), ?), ?), 4326) AS geom\n"
                + "FROM cluster \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n"
                + "		   dataset_b_geometries.subject AS b_s,\n"
                + "		   dataset_a_geometries.geom AS a_g,\n"
                + "		   dataset_b_geometries.geom AS b_g,\n"
                + "		   ST_X(ST_Centroid(dataset_a_geometries.geom)) AS a_x,\n"
                + "		   ST_Y(ST_Centroid(dataset_a_geometries.geom)) AS a_y,\n"
                + "		   ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x,\n"
                + "		   ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n"
                + "		ON(cluster.nodea = geoms.a_s AND cluster.nodeb = geoms.b_s)";
        try (final PreparedStatement stmt = connection.prepareStatement(insertShiftToPoint);
             final PreparedStatement stmtCreateScaleFunc = connection.prepareStatement(createScaleFunc);
             final PreparedStatement stmtCreateRotateFunc = connection.prepareStatement(createRotateFunc)) {
            
            stmt.setFloat(1, (float) ( this.shift / 100 ));
            stmt.setFloat(2, (float) ( this.shift / 100 ));
            stmt.setFloat(3, (float) ( this.scale ));
            stmt.setFloat(4, (float) ( this.rotate ));
            
            //stmtCreateScaleFunc.executeUpdate();
            //stmtCreateRotateFunc.executeUpdate();
            stmt.executeUpdate();            
            connection.commit();
        }
    }
    
    public float getShift() {
        return shift;
    }

    public void setShift(float shift) {
        this.shift = shift;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getRotate() {
        return rotate;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }   
    
}    