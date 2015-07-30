
package gr.athenainnovation.imis.fusion.gis.learning.core;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.learning.container.EntitiesCart;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author imis-nkarag
 */
public class BatchLearning {
    
    private static final String DB_URL = "jdbc:postgresql:";
    private PreparedStatement stmt = null;
    private Connection dbConn = null;
    private ResultSet rs = null;
    
    private static final WKTReader wktReader = new WKTReader();
    private final EntitiesCart entitiesCart;

    public BatchLearning(DBConfig dbConf, EntitiesCart entitiesCart) {
        
        this.entitiesCart = entitiesCart;
        try {
            String url = DB_URL.concat(dbConf.getDBName());
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            dbConn.setAutoCommit(false);
            
            
        } catch(SQLException sqlex) {
            System.out.println(sqlex.getMessage());      
            System.out.println("Batch connection to postgis failed");
        }  
    }


    public void startBatch(int clusterID) {

        if(clusterID == -1){
            batchAllLinks();
        }
        else{
            batchCluster(clusterID);
        }
            
    }

    private void batchAllLinks() {
        
        
        String queryAllLinks = "SELECT links.nodea, links.nodeb, a_g AS geomA, b_g AS geomB "
                    + "FROM links "
                    + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,"
                    + "dataset_b_geometries.subject AS b_s,"
                    + "ST_AsText(dataset_a_geometries.geom) AS a_g,"
                    + "ST_AsText(dataset_b_geometries.geom) AS b_g "
                    + "FROM dataset_a_geometries, dataset_b_geometries) AS geoms "
                    + "ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
        try {   
            
            stmt = dbConn.prepareStatement(queryAllLinks);
            rs = stmt.executeQuery();
            
            while(rs.next()) {
                
                MapPair mapPair = new MapPair();
                
                String geometryStringA = rs.getString(3);
                mapPair.setGeometryA(wktReader.read(geometryStringA));
                
                String geometryStringB = rs.getString(4);
                mapPair.setGeometryB(wktReader.read(geometryStringB));
                
                mapPair.setFusionAction("KeepA"); //get this from BatchServlet
                entitiesCart.addEntity(mapPair);        
            }
        } catch (SQLException | ParseException ex) {
            Logger.getLogger(BatchLearning.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    private void batchCluster(int clusterID) {
        
        String batch = "SELECT cluster.nodea, cluster.nodeb, a_g AS geomA, b_g AS geomB"
        + "FROM cluster"
        + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,"
        + "dataset_b_geometries.subject AS b_s,"
        + "ST_AsText(dataset_a_geometries.geom) AS a_g,"
        + "ST_AsText(dataset_b_geometries.geom) AS b_g"
        + "FROM dataset_a_geometries, dataset_b_geometries) AS geoms"
        + "ON(cluster.nodea = geoms.a_s AND cluster.nodeb = geoms.b_s)";
       
    }
}

