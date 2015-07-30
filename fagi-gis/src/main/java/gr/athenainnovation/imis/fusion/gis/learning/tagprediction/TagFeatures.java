
package gr.athenainnovation.imis.fusion.gis.learning.tagprediction;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.bwaldvogel.liblinear.FeatureNode;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.util.ArrayList;
import java.util.List;
//import org.openstreetmap.josm.plugins.container.OSMWay;

/**
 * Constructs the geometry feature nodes for liblinear.
 * 
 * @author imis-nkarag
 */

public class TagFeatures {
    
    private int id = 1; //= 1422; //pass this as a param from main
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private static final int NUMBER_OF_AREA_FEATURES = 25;
    private static final int NUMBER_OF_POINTS = 13;
    private static final int NUMBER_OF_MEAN = 23; //for boolean intervals
    private static final int NUMBER_OF_VARIANCE = 37; //for boolean intervals
    //private final Geometry geometry;
    
    public TagFeatures(){
        System.out.println("TagFeatures creating..");
        //this.geometry = geometry;
        //this.id = 1;
    }
    
    public void createGeometryFeaturesA(MapPair wayNode){
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////  geometry Features ///////////////////            
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // geometry type feature //
        String  geometryType= wayNode.getGeometryA().getGeometryType();
        switch (geometryType) {
            //the IDs are unique for each geometry type
            case "LineString":
                wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1));
                id += 4;
                break;
            case "Polygon":               
                wayNode.getFeatureNodeListA().add(new FeatureNode(id+1, 1));
                id += 4;
                break;
            case "LinearRing":
                wayNode.getFeatureNodeListA().add(new FeatureNode(id+2, 1));
                id += 4;
                break;
            case "Point":
                wayNode.getFeatureNodeListA().add(new FeatureNode(id+3, 1));
                id += 4;                                        
                break;
        }
        //LOG.info("after type " + id + " and further increase " + (id+1));
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // rectangle geometry shape feature //
        //id 1426
        if (wayNode.getGeometryA().isRectangle()){                 
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // number of points of geometry feature //
        id++; //1427    
        //System.out.println("should be 1427 -> " + id);
        int numberOfPoints = wayNode.getGeometryA().getNumPoints();
        numberOfPointsFeature(numberOfPoints, wayNode);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // area of geometry feature //
        //id 1440
        double area = wayNode.getGeometryA().getArea();

        if(geometryType.equals("Polygon")){ 

            areaFeature(area,wayNode);
            //the id increases by 25 in the areaFeature method
        }
        else{
            id += 25;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
        // resembles to a circle feature //  
        //id 1465
        //id++;
        if(geometryResemblesCircle(wayNode)){ //this method checks if the shape of the geometry resembles to a circle
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
        }
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // mean edge feature // 
        
        id++;
        //TOGGLE COMMENT !! commenting out mean and variance to run the best case
        
        //System.out.println("mean start" + id);
        Coordinate[] nodeGeometries = wayNode.getGeometryA().getCoordinates();
        List<Double> edgeLengths = new ArrayList<>();
        
        if(!wayNode.getGeometryA().getGeometryType().toUpperCase().equals("POINT")){
            for (int i = 0; i < nodeGeometries.length-1; i++) {
                Coordinate[] nodePair = new Coordinate[2];
                nodePair[0] = nodeGeometries[i];
                nodePair[1] = nodeGeometries[i+1];
                LineString tempGeom = geometryFactory.createLineString(nodePair);
                edgeLengths.add(tempGeom.getLength()); 
            }
        }
        else{          
            edgeLengths.add(0.0);
        }
        double edgeSum = 0;
        for(Double edge : edgeLengths){
            edgeSum = edgeSum + edge;
        }
        double mean = edgeSum/edgeLengths.size();
        //double normalizedMean = sqrt(mean);
        //geometriesPortion = geometriesPortion + id + ":" + normalizedMean + " "; 
        //wayNode.getIndexVector().put(id, normalizedMean);

//intervals with boolean values for mean feature    
        
        if(mean<2){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<4){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<6){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<8){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<10){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<12){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<14){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<16){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<18){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<20){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<25){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<30){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<35){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<40){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+13, 1.0)); 
            id += NUMBER_OF_MEAN;
        }
        else if(mean<45){        
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+14, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<50){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+15, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<60){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+16, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<70){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+17, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<80){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+18, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<90){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+19, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<100){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+20, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<200){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+21, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else {
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+22, 1.0));
            id += NUMBER_OF_MEAN;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // variance feature// 
        //id++; //this should be removed if using boolean features with intervals
        //System.out.println("must be 1467" + id);
        double sum = 0;
        for(Double edge : edgeLengths){
            sum += (edge-mean)*(edge-mean);
        }
        
        //double variance = sum/edgeLengths.size();  
        double normalizedVariance = (sum/edgeLengths.size())/(mean*mean); //normalized with square of mean value
        //geometriesPortion = geometriesPortion + id + ":" + normalizedVariance + " ";
        //wayNode.getIndexVector().put(id, normalizedVariance);
 //intervals with boolean values for variance feature  
        
        if(normalizedVariance == 0){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_VARIANCE;
        }                           
        else if(normalizedVariance < 0.005){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_VARIANCE;
        }             
        else if(normalizedVariance < 0.01){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.02){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.03){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.04){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.05){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.06){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.07){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_VARIANCE;
        }       
        else if(normalizedVariance < 0.08){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.09){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.1){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.12){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_VARIANCE;
        }      
        else if(normalizedVariance < 0.14){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+13, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.16){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+14, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.18){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+15, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.20){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+16, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.22){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+17, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.24){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+18, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.26){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+19, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.28){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+20, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.30){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+21, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.32){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+22, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.34){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+23, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.36){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+24, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.38){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+25, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.40){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+26, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.42){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+27, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.44){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+28, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.46){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+29, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.48){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+30, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.5){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+31, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.6){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+32, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.7){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+33, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.8){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+34, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.9){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+35, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 1){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+36, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else {
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+37, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        //System.out.println("mean end from instanceVectors " + id);
        //System.out.println("geom: " + wayNode.getFeatureNodeListA());
        //System.out.println("last geometry id: " + id);
        setLastID(id);
        
        
    }
    
    public void createGeometryFeaturesB(MapPair wayNode){
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////  geometry Features ///////////////////            
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // geometry type feature //
        String  geometryType= wayNode.getGeometryA().getGeometryType();
        switch (geometryType) {
            //the IDs are unique for each geometry type
            case "LineString":
                wayNode.getFeatureNodeListB().add(new FeatureNode(id, 1));
                id += 4;
                break;
            case "Polygon":               
                wayNode.getFeatureNodeListB().add(new FeatureNode(id+1, 1));
                id += 4;
                break;
            case "LinearRing":
                wayNode.getFeatureNodeListB().add(new FeatureNode(id+2, 1));
                id += 4;
                break;
            case "Point":
                wayNode.getFeatureNodeListB().add(new FeatureNode(id+3, 1));
                id += 4;                                        
                break;
        }
        //LOG.info("after type " + id + " and further increase " + (id+1));
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // rectangle geometry shape feature //
        //id 1426
        if (wayNode.getGeometryA().isRectangle()){                 
            wayNode.getFeatureNodeListB().add(new FeatureNode(id, 1.0));
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // number of points of geometry feature //
        id++; //1427    
        //System.out.println("should be 1427 -> " + id);
        int numberOfPoints = wayNode.getGeometryA().getNumPoints();
        numberOfPointsFeature(numberOfPoints, wayNode);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // area of geometry feature //
        //id 1440
        double area = wayNode.getGeometryA().getArea();

        if(geometryType.equals("Polygon")){ 

            areaFeature(area,wayNode);
            //the id increases by 25 in the areaFeature method
        }
        else{
            id += 25;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
        // resembles to a circle feature //  
        //id 1465
        //id++;
        if(geometryResemblesCircle(wayNode)){ //this method checks if the shape of the geometry resembles to a circle
            wayNode.getFeatureNodeListB().add(new FeatureNode(id, 1.0));
        }
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // mean edge feature // 
        
        id++;
        //TOGGLE COMMENT !! commenting out mean and variance to run the best case
        
        //System.out.println("mean start" + id);
        Coordinate[] nodeGeometries = wayNode.getGeometryA().getCoordinates();
        List<Double> edgeLengths = new ArrayList<>();
        
        if(!wayNode.getGeometryA().getGeometryType().toUpperCase().equals("POINT")){
            for (int i = 0; i < nodeGeometries.length-1; i++) {
                Coordinate[] nodePair = new Coordinate[2];
                nodePair[0] = nodeGeometries[i];
                nodePair[1] = nodeGeometries[i+1];
                LineString tempGeom = geometryFactory.createLineString(nodePair);
                edgeLengths.add(tempGeom.getLength()); 
            }
        }
        else{          
            edgeLengths.add(0.0);
        }
        double edgeSum = 0;
        for(Double edge : edgeLengths){
            edgeSum += edge;
        }
        double mean = edgeSum/edgeLengths.size();
        //double normalizedMean = sqrt(mean);
        //geometriesPortion = geometriesPortion + id + ":" + normalizedMean + " "; 
        //wayNode.getIndexVector().put(id, normalizedMean);

//intervals with boolean values for mean feature    
        
        if(mean<2){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<4){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<6){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<8){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<10){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<12){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<14){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<16){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<18){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<20){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<25){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<30){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<35){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<40){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+13, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<45){        
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+14, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<50){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+15, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<60){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+16, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<70){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+17, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<80){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+18, 1.0));
            id += NUMBER_OF_MEAN;
        }        
        else if(mean<90){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+19, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<100){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+20, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else if(mean<200){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+21, 1.0));
            id += NUMBER_OF_MEAN;
        }
        else {
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+22, 1.0));
            id += NUMBER_OF_MEAN;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // variance feature// 
        //id++; //this should be removed if using boolean features with intervals
        //System.out.println("must be 1467" + id);
        double sum = 0;
        for(Double edge : edgeLengths){
            sum += (edge-mean)*(edge-mean);
        }
        
        //double variance = sum/edgeLengths.size();  
        double normalizedVariance = (sum/edgeLengths.size())/(mean*mean); //normalized with square of mean value
        //geometriesPortion = geometriesPortion + id + ":" + normalizedVariance + " ";
        //wayNode.getIndexVector().put(id, normalizedVariance);
 //intervals with boolean values for variance feature  
        
        if(normalizedVariance == 0){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_VARIANCE;
        }                           
        else if(normalizedVariance < 0.005){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_VARIANCE;
        }             
        else if(normalizedVariance < 0.01){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.02){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.03){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.04){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.05){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.06){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.07){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_VARIANCE;
        }       
        else if(normalizedVariance < 0.08){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.09){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.1){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.12){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_VARIANCE;
        }      
        else if(normalizedVariance < 0.14){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+13, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.16){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+14, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.18){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+15, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.20){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+16, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.22){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+17, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.24){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+18, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.26){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+19, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.28){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+20, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.30){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+21, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.32){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+22, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.34){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+23, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.36){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+24, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.38){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+25, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.40){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+26, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.42){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+27, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.44){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+28, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.46){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+29, 1.0));
            id += NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.48){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+30, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.5){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+31, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.6){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+32, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.7){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+33, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.8){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+34, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.9){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+35, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 1){
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+36, 1.0));
            id += NUMBER_OF_VARIANCE;
        }
        else {
            wayNode.getFeatureNodeListB().add(new FeatureNode(id+37, 1.0));
            id += NUMBER_OF_VARIANCE;
        } 
        //System.out.println("mean end from instanceVectors " + id);
        //System.out.println("geom: " + wayNode.getFeatureNodeListB());
        //System.out.println("last geometry id: " + id);
        setLastID(id);
        
        
    }
   
    
    private void numberOfPointsFeature(int numberOfPoints, MapPair wayNode) {           
        //int NUMBER_OF_POINTS = 13; //increase the id after the feature is found for the next portion of the vector.

        if(numberOfPoints<10){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<20){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<30){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<40){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<50){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<75){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<100){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<150){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<200){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<300){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<500){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<1000){ 
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_POINTS;
        }
        else{
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_POINTS;
        }
    }
    
    private void areaFeature(double area, MapPair wayNode) {        
        
        if(area<50){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<100){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+1, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<150){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+2, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<200){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+3, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<250){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+4, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<300){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+5, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<350){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+6, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<400){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+7, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<450){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+8, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<500){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+9, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<750){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+10, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+11, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1250){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+12, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1500){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+13, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1750){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+14, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+15, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2250){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+16, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2500){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+17, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2750){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+18, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<3000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+19, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<3500){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+20, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<4000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+21, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<5000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+22, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<10000){
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+23, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }
        else{
            wayNode.getFeatureNodeListA().add(new FeatureNode(id+24, 1.0));
            id += NUMBER_OF_AREA_FEATURES;
        }       
    }     
    
    
    private boolean geometryResemblesCircle(MapPair way){
        Geometry wayGeometry = way.getGeometryA();
        boolean isCircle = false;
        /*
        if(wayGeometry.getGeometryType().equals("Polygon") && wayGeometry.getNumPoints()>=16){ 
             
            
//            Coordinate[] points = way.getGeometryA().getCoordinates();
//            for(Coordinate co : points){
//                //co.
//            }
            
            List<Geometry> points = way.getNodeGeometries();
            //way.
            Geometry firstPoint = points.get(0);            
            double radius = firstPoint.distance(wayGeometry.getCentroid());
            
            // buffer around the distance of the first point to centroid
            double radiusBufferSmaller = radius*0.6; 
            //the rest of the point-to-centroid distances will be compared with these 
            double radiusBufferGreater = radius*1.4; 
            isCircle = true;
            
            for (Geometry point : points){                
                double tempRadius = point.distance(wayGeometry.getCentroid());
                boolean tempIsCircle = (radiusBufferSmaller <= tempRadius) && (tempRadius <= radiusBufferGreater);
                isCircle = isCircle && tempIsCircle; //if any of the points give a false, the method will return false
                //if (!isCircle){break;}
            }     
            
            double ratio = wayGeometry.getLength() / wayGeometry.getArea();            
            boolean tempIsCircle = ratio < 0.06; //arbitary value based on statistic measure of osm instances. 
                                                 //The smaller this value, the closer this polygon resembles to a circle            
            isCircle = isCircle && tempIsCircle;
        }
        */         
        return isCircle;
         
    }     
    
    private void setLastID(int lastID){
        this.id = lastID;
    }
    
    public int getLastID(){
        return id + 1;
    }
}

