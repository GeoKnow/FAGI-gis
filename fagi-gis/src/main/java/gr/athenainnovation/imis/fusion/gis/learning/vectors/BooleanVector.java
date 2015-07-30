
package gr.athenainnovation.imis.fusion.gis.learning.vectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import de.bwaldvogel.liblinear.FeatureNode;
import gr.athenainnovation.imis.fusion.gis.clustering.Vector2D;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author imis-nkarag
 */

public class BooleanVector implements Vector {
    //private final Map<Boolean, String> featureSelection;
    private int id;
    private final MapPair mapPair;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final double PI = Math.PI;
    private static final int SUM_OF_POINTS = 11;
    private static final int SUM_OF_AREA_FEATURES = 10; 
    private static final int AREA_PERCENTAGE = 19; //ok
    private static final int POINTS_PERCENTAGE = 19; //ok
    private static final int MEAN_PERCENTAGE = 12; //ok
    private static final int VARIANCE_PERCENTAGE = 13; //ok
    private static final int MEANS_AVERAGE = 32; //ok
    private static final int DISTANCE = 5; //ok
    private static final int ANGLE = 4; //ok
    private static final int OWL_CLASS = 20; //ok
    private double meansABAverage;
    private double varianceA;
    private double varianceB;
    private static int lastID;
    
    

    public BooleanVector(Map<Boolean,String> featureSelection, MapPair mapPair){
        //this.featureSelection = featureSelection;
        this.mapPair = mapPair;
    } 
    
    public BooleanVector(MapPair mapPair){
        //featureSelection = null;
        this.mapPair = mapPair;
    }    
 
    
    @Override
    public void createGeometryFeatures() {
        id = 1;
        //BooleanGeometryFeatures geometryFeatures = new BooleanGeometryFeatures(featureSelection, id);
        Geometry geometryA = mapPair.getGeometryA();
        Geometry geometryB = mapPair.getGeometryB();
            
        geometryTypeFeature(geometryA.getGeometryType(), geometryB.getGeometryType());
        
        percentageOfAreaDifference(geometryA.getArea(),geometryB.getArea());        
        
        percentageOfPointsDifference(geometryA.getNumPoints(),geometryB.getNumPoints());
        
        percentageOfMeanEdgeDifference(geometryA, geometryB);
        
        percentageOfMeanEdgeAverage();
        
        percentageOfVariance();
        
        /*        
            we use cross instead of overlap, because: The geometries do more than touch, they actually overlap edges!
            instead the overlap method does this: 
            The geometries have some points in common; but not all points in common 
            (so if one geometry is inside the other overlaps would be false). 
            The overlapping section must be the same kind of shape as the two geometries; 
            so two polygons that touch on a point are not considered to be overlapping.       
            more at: http://docs.geotools.org/stable/userguide/library/jts/relate.html
        */   
        
        geometriesCross(geometryA, geometryB);
        
        geometriesTouch(geometryA, geometryB);
        
        geometriesIntersect(geometryA, geometryB);
        
        geometryAWithinB(geometryA, geometryB);
        
        geometryBWithinA(geometryA, geometryB);
        
        geometryAContainsB(geometryA, geometryB);
        
        geometryBContainsA(geometryA, geometryB);
        
        sumOfNumberOfPointsFeature(geometryA.getNumPoints() + geometryB.getNumPoints());
        
        linkLength(geometryA.getCentroid(), geometryB.getCentroid());
               
        unitVectorAngle(geometryA.getCentroid(), geometryB.getCentroid());
        
        sumOfArea(geometryA.getArea() + geometryB.getArea());       
        
    }
    
    private void geometryTypeFeature(String geometryTypeA, String geometryTypeB) {
        if(geometryTypeA.equalsIgnoreCase("LineString") || geometryTypeB.equalsIgnoreCase("LineString")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("Polygon")|| geometryTypeB.equalsIgnoreCase("Polygon")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("Linearring")|| geometryTypeB.equalsIgnoreCase("CircularString")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("Point")|| geometryTypeB.equalsIgnoreCase("Point")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("MultiLineString")|| geometryTypeB.equalsIgnoreCase("MultiLineString")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("MultiPolygon")|| geometryTypeB.equalsIgnoreCase("MultiPolygon")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("MultiPoint")|| geometryTypeB.equalsIgnoreCase("MultiPoint")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++;
        if(geometryTypeA.equalsIgnoreCase("GeometryCollection")|| geometryTypeB.equalsIgnoreCase("GeometryCollection")){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1));
            //id += 8;
        }
        id++; //id = 9
        
    }
    
    private void sumOfNumberOfPointsFeature(int sumOfPoints) {
        
        if(sumOfPoints < 10){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<20){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<30){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+2, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<40){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+3, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<50){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+4, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<75){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+5, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<100){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+6, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<150){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+7, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<200){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+8, 1.0));
            id += SUM_OF_POINTS;
        }
        else if(sumOfPoints<300){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+9, 1.0));
            id += SUM_OF_POINTS;
        }
        else {
            mapPair.getFeatureNodeList().add(new FeatureNode(id+10, 1.0));
            id += SUM_OF_POINTS;
        }       
    }
    
    private void sumOfArea(double sumOfArea) {        
        
        if(sumOfArea<50){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<100){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<150){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+2, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<200){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+3, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<250){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+4, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<300){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+5, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<350){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+6, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<400){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+7, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<450){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+8, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<500){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+9, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<750){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+10, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<1000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+11, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<1250){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+12, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<1500){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+13, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<1750){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+14, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<2000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+15, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<2250){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+16, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<2500){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+17, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<2750){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+18, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<3000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+19, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<3500){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+20, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<4000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+21, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<5000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+22, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else if(sumOfArea<10000){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+23, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }
        else{
            mapPair.getFeatureNodeList().add(new FeatureNode(id+24, 1.0));
            id += SUM_OF_AREA_FEATURES;
        }       
        //setLastID(id);        
    }     

    private void percentageOfAreaDifference(double areaA, double areaB) {
        //System.out.println("\ntesting percentage of area, starting id: " + id);
        double maxArea = areaA; 
        if(areaA < areaB){
            maxArea = areaB; 
        }        
        double percentage;
        
        if(areaA == 0 && areaB == 0 ){
            percentage = 0;
        }
        else{
            percentage = (areaA - areaB)/maxArea;
        }
        
        int tempId = 0;
        for(double percentageStep = -0.9; percentageStep < 1; percentageStep += 0.1){
            //System.out.println("percentage: " + percentage + ", step: " + percentageStep);
            if(percentage < percentageStep){
                
                mapPair.getFeatureNodeList().add(new FeatureNode(id + tempId, 1.0));
                //System.out.println("--percentage: " + percentage + ", step: " + percentageStep + "feature added: " + (id+tempId));
                break;
            }
            tempId++;
        }
        id += AREA_PERCENTAGE;
        //System.out.println("\ntesting percentage of area, ending id: " + id + ", temp id:" + tempId);
    }

    private void percentageOfPointsDifference(int pointsA, int pointsB){
        //System.out.println("\ntesting percentage of points, starting id: " + id);
        int maxPoints = pointsA; 
        if(pointsA < pointsB){
            maxPoints = pointsB; 
        }
        
        double percentage;
        
        if(pointsA == 0 && pointsB == 0 ){
            percentage = 0;
        }
        else{
            percentage = (pointsA - pointsB)/maxPoints;
        }
        
        int tempId = 0;
        for(double percentageStep = -1; percentageStep < 1; percentageStep += 0.1){
            
            if(percentage < percentageStep){
                
                mapPair.getFeatureNodeList().add(new FeatureNode(id + tempId, 1.0)); 
                //System.out.println("percentage: " + percentage + ", step: " + percentageStep + "feature added: " + (id+ tempId));
                break;
            }
            tempId++;
        }
        id += POINTS_PERCENTAGE;
        //System.out.println("testing percentage of points, ending id: " + id + "\n temp id:" + tempId + "\n");
    }
    
    private void percentageOfMeanEdgeDifference(Geometry geometryA, Geometry geometryB){
        //System.out.println("testing percentageOfMeanEdgeDifference, starting id=" + id);
        Coordinate[] nodeGeometriesA = geometryA.getCoordinates();
        Coordinate[] nodeGeometriesB = geometryB.getCoordinates();
        
        List<Double> edgeLengthsA = new ArrayList<>();
        List<Double> edgeLengthsB = new ArrayList<>();
        
        if(!geometryA.getGeometryType().toUpperCase().equals("POINT")){
            for (int i = 0; i < nodeGeometriesA.length-1; i++) {
                Coordinate[] nodePair = new Coordinate[2];
                nodePair[0] = nodeGeometriesA[i];
                nodePair[1] = nodeGeometriesA[i+1];
                
                
                LineString tempGeom = geometryFactory.createLineString(nodePair);
                edgeLengthsA.add(tempGeom.getLength()); 
            }
        }
        else{          
            edgeLengthsA.add(0.0);
        }
        
        double edgeSumA = 0;
        
        for(Double edgeA : edgeLengthsA){
            edgeSumA += edgeA;
        }
        double meanA = edgeSumA/edgeLengthsA.size();
        
        double sumA = 0;
        for(Double edgeA : edgeLengthsA){
            sumA += (edgeA-meanA)*(edgeA-meanA);
        }
        varianceA = sumA/edgeLengthsA.size();
        
        if(!geometryB.getGeometryType().toUpperCase().equals("POINT")){
            for (int i = 0; i < nodeGeometriesB.length-1; i++) {
                Coordinate[] nodePair = new Coordinate[2];
                nodePair[0] = nodeGeometriesB[i];
                nodePair[1] = nodeGeometriesB[i+1];
                
                
                LineString tempGeom = geometryFactory.createLineString(nodePair);
                edgeLengthsB.add(tempGeom.getLength()); 
            }
        }
        else{          
            edgeLengthsB.add(0.0);
        }
        
        double edgeSumB = 0;
        
        for(Double edgeB : edgeLengthsB){
            edgeSumB += edgeB;
        }
        double meanB = edgeSumB/edgeLengthsB.size();
        
        double maxMean = meanA; 
        if(meanA < meanB){
            maxMean = meanB; 
        } 
        
        double sumB = 0;
        for(Double edgeB : edgeLengthsB){
            sumB += (edgeB-meanB)*(edgeB-meanB);
        }
        varianceB = sumB/edgeLengthsB.size();
        
        
        double meanDifference = (meanA - meanB)/maxMean;
        
        int tempId = 0;
        for(double percentageStep = -1; percentageStep < 1; percentageStep += 0.1 ){
            //System.out.println("meanDifference: " + meanDifference + ", step: " + percentageStep);
            if(meanDifference < percentageStep){
                
                mapPair.getFeatureNodeList().add(new FeatureNode(id + tempId, 1.0));
                //id += MEAN_PERCENTAGE - tempId;
                id += MEAN_PERCENTAGE;
                break;
            }
            tempId++;
        }
        meansABAverage = (meanA + meanB) / 2;
        //System.out.println("testing percentageOfMeanEdgeDifference, ending id=" + id);
    }
    
    private void percentageOfVariance(){
        //System.out.println("testing percentageOfVariance, starting id=" + id);
        double maxVariance = varianceA;
        if(varianceA < varianceB){
            maxVariance = varianceB;
        }
        double varianceDifference = (varianceA - varianceB)/maxVariance;

        int tempId = 0;
        for(double percentageStep = -1; percentageStep < 1; percentageStep += 0.1 ){
            //System.out.println("varianceDifference: " + varianceDifference + ", step: " + percentageStep + ", tempID=" + tempId);
            if(varianceDifference < percentageStep){
                
                mapPair.getFeatureNodeList().add(new FeatureNode(id + tempId, 1.0));
                //id += VARIANCE_PERCENTAGE - tempId;
                id += VARIANCE_PERCENTAGE;
                break;
            }
            tempId++;
        }
        //System.out.println("testing percentageOfVariance, ending id=" + id);
    }

    private void geometriesCross(Geometry geometryA, Geometry geometryB) {
        
        if(geometryA.crosses(geometryB)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometriesTouch(Geometry geometryA, Geometry geometryB) {
        
        if(geometryA.touches(geometryB)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometriesIntersect(Geometry geometryA, Geometry geometryB) {
        if(geometryA.intersects(geometryB)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometryAWithinB(Geometry geometryA, Geometry geometryB) {
        if(geometryA.within(geometryB)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometryBWithinA(Geometry geometryA, Geometry geometryB) {
        if(geometryB.within(geometryA)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometryAContainsB(Geometry geometryA, Geometry geometryB) {
        if(geometryA.contains(geometryB)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }

    private void geometryBContainsA(Geometry geometryA, Geometry geometryB) {
        if(geometryB.contains(geometryA)){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
        }
        id++;
    }
    
    private void percentageOfMeanEdgeAverage(){
        
        if(meansABAverage < 0.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 1){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 1.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+2, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 2){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+3, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 2.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+4, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 3){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+5, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 3.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+6, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 4){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+7, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 4.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+8, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+9, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 5.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+10, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 6){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+11, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 6.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+12, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 7){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+13, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 7.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+14, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 8){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+15, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 8.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+16, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 9){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+17, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 9.5){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+18, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 10){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+19, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 11){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+20, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 12){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+21, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 15){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+22, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 20){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+23, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 25){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+24, 1.0));
            id += MEANS_AVERAGE;
        }
        else if(meansABAverage < 30){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+25, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 35){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+26, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 40){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+27, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 50){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+28, 1.0));
            id += MEANS_AVERAGE;
        }        
        else if(meansABAverage < 100){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+29, 1.0));
            id += MEANS_AVERAGE;
        }      
        else if(meansABAverage < 200){
            mapPair.getFeatureNodeList().add(new FeatureNode(id+30, 1.0));
            id += MEANS_AVERAGE;
        } 
        else {
            mapPair.getFeatureNodeList().add(new FeatureNode(id+31, 1.0));
            id += MEANS_AVERAGE;
        }                 
    }    

    private void linkLength(Point centroidA, Point centroidB) {
        //increase number of features in liblinear
//        Point centroidA = geometryA.getCentroid();
//        Point centroidB = geometryB.getCentroid();

        double distance = centroidA.distance(centroidB)*1000000; //returns aprox values from 1-15.  
        //111195
        System.out.println("distance*1M of centroids: " + distance);
        //if(distance < 10){
//        if(distance < 2){    
//            //System.out.println("distance<10");
//            mapPair.getFeatureNodeList().add(new FeatureNode(id, 1.0));
//            id += DISTANCE;
//        }
        //else if(distance < 50){
        
        
        if(distance < 4){
            //System.out.println("distance<50");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += DISTANCE;
        }
        //else if(distance < 100){
        else if(distance < 6){    
            //System.out.println("distance<100");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += DISTANCE;
        }
        //else if(distance < 200){
        else if(distance < 8){    
            //System.out.println("distance<200");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += DISTANCE;
        }
        //else if(distance < 500){
        else if(distance < 15){
            //System.out.println("distance<500");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += DISTANCE;
        }        
        //else if(distance < 1000){
//        else if(distance < 15){    
//            //System.out.println("distance<1000");
//            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
//            id += DISTANCE;
//        } 
        else {
            //System.out.println("distance>1000");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += DISTANCE;
        }         
    }

    private void unitVectorAngle(Point centroidA, Point centroidB) {
        
        
        double dx = centroidB.getX() - centroidA.getX();
        double dy = centroidB.getY() - centroidA.getY();    
        
        double magnitude = Math.sqrt(dx*dx + dy*dy);
        
        double unitVectorX = dx/magnitude;
        double unitVectorY = dy/magnitude;        
                
        double slope = unitVectorY/unitVectorX;    
        //double angle = Math.atan(slope/(1 - slope));
        double angle = Math.atan(slope);
        
        System.out.println("centroidA x: " + centroidA.getX());
        System.out.println("centroidA y: " + centroidA.getY());
        
        System.out.println("\ndx: " + dx 
                + "\ndy: " + dy 
                + "\nlength: " + magnitude 
                + "\nunitX: " + unitVectorX
                + "\nunitY: " + unitVectorY                
                + "\nslope (unY / unX: " + slope
                + "angle: " + angle);
        

        //limit values for arctan are (-pi/2, pi/2) ~ (-1.5707, 1.5707)

        if(angle < -PI/4){
            //System.out.println("distance<50");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += ANGLE;
        } 
        else if(angle < 0){
            //System.out.println("distance<500");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += ANGLE;
        } 
        else if(angle < PI/4){
            //System.out.println("distance<500");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += ANGLE;
        }
        else{
            //System.out.println("distance<500");
            mapPair.getFeatureNodeList().add(new FeatureNode(id+1, 1.0));
            id += ANGLE;
        }        
    }
    
    @Override
    public void createOWLFeatures(){
        System.out.println("starting id OWL: " + id);
        
        String classA = mapPair.getOWLClassA();
        String classB = mapPair.getOWLClassB();
        
        String[] classesA = classA.split(",");
        String[] classesB = classB.split(",");
        int sizeA = classesA.length;
        int sizeB = classesB.length;
        
        double maxArea = sizeA; 
        if(sizeA < sizeB){
            maxArea = sizeB; 
        }        
        double percentage;
        
        if(sizeA == 0 && sizeB == 0 ){
            percentage = 0;
        }
        else{
            percentage = (sizeA - sizeB)/maxArea;
        }        
        
        int tempId = 0;
        for(double percentageStep = -0.9; percentageStep < 1; percentageStep += 0.1){
            //System.out.println("percentage: " + percentage + ", step: " + percentageStep);
            if(percentage < percentageStep){
                
                mapPair.getFeatureNodeList().add(new FeatureNode(id + tempId, 1.0));
                //System.out.println("--percentage: " + percentage + ", step: " + percentageStep + "feature added: " + (id+tempId));
                break;
            }
            tempId++;
        }
        id += OWL_CLASS;        
        //System.out.println("ending id OWL: " + id);
        
        setLastID(id);
        System.out.println("last id: " + id);        
    }
    
    public void showFeatures(){
        System.out.println(mapPair.getFeatureNodeList());
    }

    private void setLastID(int lastID) {
        BooleanVector.lastID = lastID;
    }
    
    public static int getLastID(){
        return lastID;
    }           
}
