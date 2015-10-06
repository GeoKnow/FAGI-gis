/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.clustering;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.json.ClusteringResult;
import gr.athenainnovation.imis.fusion.gis.json.ClusteringResults;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author nick
 */
public class GeoClusterer {
    public static enum GeoAttribute {
        VECOTR_LENGTH, VECTOR_DIRECTION, COVERAGE 
    }
    public static final Map<String, GeoAttribute> geoAttributes;
    static {
        Map<String, GeoAttribute> aMap = new HashMap<>();
        aMap.put("vLen", GeoAttribute.VECOTR_LENGTH);
        aMap.put("vDir", GeoAttribute.VECTOR_DIRECTION);
        aMap.put("cov", GeoAttribute.COVERAGE);
        geoAttributes = Collections.unmodifiableMap(aMap);
    }
    
    private Connection connection;
    
    private static final String DB_URL = "jdbc:postgresql:";
    private final DBConfig dbConf;
    private PreparedStatement stmt;
    private ResultSet rs;
    private HashMap<String, ClusteringVector> attrData;
    
    public GeoClusterer(DBConfig d_c) {
        dbConf = d_c;
        attrData = Maps.newHashMap();
    }
    
    private void rangeChange(List<DoubleWrapper> l, double oldMin, double oldMax, double newMin, double newMax) {
        double oldRange = (oldMax - oldMin);
        double newRange = (newMax - newMin);
        for (DoubleWrapper d : l) {
            if (oldRange == 0) {
                d.val = newMin;
            } else {
                d.val = (((d.val - oldMin) * newRange) / oldRange) + newMin;
            }
        }
    }
    
    /*
    private void normalize(List<DoubleWrapper> l) {
        DoubleWrapper max = Collections.max(l);
        System.out.println("Max "+max.val);
        for (DoubleWrapper d : l) {
            d.val /= max.val;
        }
        System.out.println("END");
    }
    */
    private void normalize(List<DoubleWrapper> l) {
        Double max = Collections.max(l).val;
        System.out.println("Max "+max);
        for (DoubleWrapper d : l) {
            d.val /= max;
        }
        System.out.println("END");
    }
    
    public String cluster( List<GeoAttribute> selectedAttributes, int numClusters ) throws SQLException {
        try {
            if ( selectedAttributes == null ) {
                selectedAttributes = new ArrayList<>();
                for (Map.Entry<String, GeoAttribute> entry : geoAttributes.entrySet()) {
                    selectedAttributes.add(entry.getValue());
                }
            }
            FastVector atts;
            FastVector attsRel;
            FastVector attVals;
            FastVector attValsRel;
            Instances data;
            Instances dataRel;
            double[] vals;
            double[] valsRel;
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            //SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            //mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            Random rnd = new Random();
            rnd.setSeed((new Date()).getTime());

            
            final String url = DB_URL.concat(dbConf.getDBName());
            connection = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            connection.setAutoCommit(false);
                       
            List<DoubleWrapper> lDist = new ArrayList<>();
            List<Integer> lCoverage = new ArrayList<>();
            List<Vector2D> lCoords = new ArrayList<>();
            
            for ( GeoAttribute ga : selectedAttributes ) {
                if ( ga.equals(GeoAttribute.VECOTR_LENGTH) ) {
                    this.calculateDistance(lDist);
                } else if (ga.equals(GeoAttribute.VECTOR_DIRECTION)) {
                    this.calculateDirection(lCoords);
                } else if (ga.equals(GeoAttribute.COVERAGE)) {
                    this.calculateCoverage(lCoverage);
                } else {
                    
                }
            }
            int counter = 0;
            for (Map.Entry<String, ClusteringVector> entry : attrData.entrySet()) {
                System.out.println((++counter)+":"+entry.getKey() + "/" + entry.getValue().nodeA);
                System.out.println(entry.getValue().v.x+" "+entry.getValue().v.y);
                System.out.println(entry.getValue().dist.val);
            }
            
            // Attributes
            atts = new FastVector();
            for ( GeoAttribute ga : selectedAttributes ) {
                if ( ga.equals(GeoAttribute.VECOTR_LENGTH) ) {
                    atts.addElement(new Attribute("dist"));
                } else if (ga.equals(GeoAttribute.VECTOR_DIRECTION)) {
                    atts.addElement(new Attribute("vector X"));
                    atts.addElement(new Attribute("vector Y"));
                } else if (ga.equals(GeoAttribute.COVERAGE)) {
                    atts.addElement(new Attribute("coverage"));
                } else {
                    
                }
            }
            
            // Data
            int distIdx = 0, dirXIdx = 0, dirYIdx = 0, covIdx = 0;
            int nextIdx = 0;
            for ( GeoAttribute ga : selectedAttributes ) {
                if ( ga.equals(GeoAttribute.VECOTR_LENGTH) ) {
                    distIdx = nextIdx;
                    nextIdx++;
                } else if (ga.equals(GeoAttribute.VECTOR_DIRECTION)) {
                    dirXIdx = nextIdx;
                    nextIdx++;
                    dirYIdx = nextIdx;
                    nextIdx++;
                } else if (ga.equals(GeoAttribute.COVERAGE)) {
                    covIdx = nextIdx;
                    nextIdx++;
                } else {
                    
                }
            }
            
            data = new Instances("Geo Relation", atts, 0);
            List<ClusteringVector> orderedAttrList = new ArrayList<>();
            for (Map.Entry<String, ClusteringVector> entry : attrData.entrySet()) {
                // Temp values
                vals = new double[data.numAttributes()];
                // - numeric

                for (GeoAttribute ga : selectedAttributes) {
                    if (ga.equals(GeoAttribute.VECOTR_LENGTH)) {
                        vals[distIdx] = (double) entry.getValue().dist.val;
                    } else if (ga.equals(GeoAttribute.VECTOR_DIRECTION)) {
                        vals[dirXIdx] = (double) entry.getValue().v.x;
                        vals[dirYIdx] = (double) entry.getValue().v.y;
                    } else if (ga.equals(GeoAttribute.COVERAGE)) {
                        vals[covIdx] = (double) entry.getValue().intersects;
                    } else {

                    }
                }
                orderedAttrList.add(entry.getValue());
                data.add(new DenseInstance(1.0, vals));
            }

            // 4. output data
            //System.out.println(data);
            String[] options;
            if (numClusters > 0) {
                options = new String[5];
                options[0] = "-I";                 // max. iterations
                options[1] = "100";
                options[2] = "-N";                 // number of Clusters
                options[3] = Integer.toString(numClusters); 
                options[4] = "-O";                 // Preserve attribute order
            } else {
                options = new String[3];
                options[0] = "-I";                 // max. iterations
                options[1] = "100";
                options[2] = "-O";
            }
            
            //SimpleKMeans clusterer = new SimpleKMeans();   // new instance of clusterer
            EM clusterer = new EM();   // new instance of clusterer
            clusterer.setOptions(options);     // set the options
            clusterer.buildClusterer(data);    // build the clusterer
            System.out.println("Clustering.....");
            System.out.println(clusterer.toString());
            ClusteringResults ret = new ClusteringResults();
            for (int i = 0; i < data.size(); i++) {
                int ass_cluster = clusterer.clusterInstance(data.get(i));
                System.out.println("Assignement "+orderedAttrList.get(i).nodeA+" : "+ass_cluster);
                //System.out.println("Assignement "+orderedAttrList.get(i).nodeA+" : "+clusterer.getAssignments()[i]);
                ClusteringResult res = new ClusteringResult(ass_cluster);
                ret.getResults().put(orderedAttrList.get(i).nodeA, res);
            }
            ret.setNumOfClusters(clusterer.getNumClusters());
            System.out.println("Clustering done.....");
            System.out.println(mapper.writeValueAsString(ret));
            
            return mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            Logger.getLogger(GeoClusterer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            connection.close();
        }
        
        return "{}";
    }
    
    private void calculateDistance(List<DoubleWrapper> l) throws SQLException {
        String queryDistances = "SELECT links.nodea AS sa, links.nodeb AS sb, \n"
                    + "ST_Distance(ST_Centroid(ST_Transform(a_g,2163)), ST_Centroid(ST_Transform(b_g,2163))) AS dist\n"
                    + "FROM links \n"
                    + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, dataset_b_geometries.subject AS b_s,\n"
                    + "		dataset_a_geometries.geom AS a_g, dataset_b_geometries.geom AS b_g,\n"
                    + "		ST_X(dataset_a_geometries.geom) AS a_x, ST_Y(dataset_a_geometries.geom) AS a_y,\n"
                    + "		ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x, ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n"
                    + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
        System.out.println(queryDistances);
          
        stmt = connection.prepareStatement(queryDistances);
        rs = stmt.executeQuery();

        while (rs.next()) {
            ClusteringVector vec = attrData.get(rs.getString("sa"));
            double dist = rs.getDouble("dist");
            DoubleWrapper tmpDist = new DoubleWrapper(dist);
            if (vec != null) {
                vec.dist = tmpDist;
            } else {
                vec = new ClusteringVector(rs.getString("sa"), rs.getString("sb"));
                vec.dist = tmpDist;
                attrData.put(rs.getString("sa"), vec);
            }
            l.add(tmpDist);
        }
        
        rs.close();
        stmt.close();
            
        normalize(l);
    }
    
    private void calculateDirection(List<Vector2D>  l) throws SQLException {
        String queryCoords = "SELECT sa, sb, ((b_x-a_x) / length) AS v_x, ((b_y-a_y) / length) AS v_y FROM ( \n"
                    + "SELECT links.nodea AS sa, links.nodeb AS sb , a_y, b_y, a_x, b_x, |/ ((b_x-a_x)*(b_x-a_x) + (b_y-a_y)*(b_y-a_y)) AS length\n"
                    + "FROM links \n"
                    + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, dataset_b_geometries.subject AS b_s,\n"
                    + "		dataset_a_geometries.geom AS a_g, dataset_b_geometries.geom AS b_g,\n"
                    + "		ST_X(ST_Centroid(dataset_a_geometries.geom)) AS a_x, ST_Y(ST_Centroid(dataset_a_geometries.geom)) AS a_y,\n"
                    + "		ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x, ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n"
                    + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)) AS tbl";
        
        stmt = connection.prepareStatement(queryCoords);
        rs = stmt.executeQuery();

        while (rs.next()) {
            double v_x = rs.getDouble("v_x");
            double v_y = rs.getDouble("v_y");
            Vector2D tmpVec = new Vector2D(v_x, v_y);
            ClusteringVector vec = attrData.get(rs.getString("sa"));
            if (vec != null) {
                vec.v = tmpVec;
            } else {
                vec = new ClusteringVector(rs.getString("sa"), rs.getString("sb"));
                vec.v = tmpVec;
                attrData.put(rs.getString("sa"), vec);
            }
            l.add(tmpVec);
        }
        
        for (Vector2D v : l) {
            v.x /= 2;
            v.y /= 2;
        }
        
        rs.close();
        stmt.close();
    }
    
    private void calculateCoverage(List<Integer> l) throws SQLException {
         String queryCoverage = "SELECT links.nodea AS sa, links.nodeb AS sb, \n"
                + " CAST( ST_Intersects(a_g, b_g) AS integer ) AS intersects\n"
                + "FROM links \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, dataset_b_geometries.subject AS b_s,\n"
                + "		dataset_a_geometries.geom AS a_g, dataset_b_geometries.geom AS b_g,\n"
                + "		ST_X(dataset_a_geometries.geom) AS a_x, ST_Y(dataset_a_geometries.geom) AS a_y,\n"
                + "		ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x, ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";

        stmt = connection.prepareStatement(queryCoverage);
        rs = stmt.executeQuery();

        while (rs.next()) {
            int inter = rs.getInt("intersects");
            ClusteringVector vec = attrData.get(rs.getString("sa"));
            if (vec != null) {
                vec.intersects = inter;
            } else {
                vec = new ClusteringVector(rs.getString("sa"), rs.getString("sb"));
                vec.intersects = inter;
                attrData.put(rs.getString("sa"), vec);
            }
            l.add(inter);
        }
        
        rs.close();
        stmt.close();
    }
}
