package gr.athenainnovation.imis.fusion.gis.learning.core;

//import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
//import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import com.vividsolutions.jts.io.WKTReader;
//import org.geotools.geometry.jts.JTS;
//import org.geotools.referencing.CRS;
//import org.opengis.referencing.operation.MathTransform;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//import org.geotools.referencing.crs.DefaultGeocentricCRS;
//import org.geotools.referencing.crs.DefaultGeographicCRS;
//import org.opengis.referencing.FactoryException;
//import org.opengis.referencing.operation.TransformException;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import java.util.List;

/**
 *
 * @author imis-nkarag
 */
public class MapPairJsonDeserializer extends JsonDeserializer<MapPair> {

    private static final WKTReader wktReader = new WKTReader();
    //private static final GeometryFactory geometryFactory = new GeometryFactory();
    //private MathTransform transform;
    //private static final CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
    //private static final CoordinateReferenceSystem targetCRS = DefaultGeocentricCRS.CARTESIAN;

    public MapPairJsonDeserializer() {
        System.out.println("new MapPairJsonDeserializer");
//        try {
//            transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
//        } catch (FactoryException ex) {
//            Logger.getLogger(MapPairJsonDeserializer.class.getName()).log(Level.SEVERE, null, ex);
//        }

    }

    @Override
    public MapPair deserialize(JsonParser jp, DeserializationContext ctxt) {

        MapPair mapPair = new MapPair();

        try {

            JsonNode node = jp.readValueAsTree();

            JsonNode geometryANode = node.get("geometryA");
            JsonNode geometryBNode = node.get("geometryB");
            JsonNode owlClassANode = node.get("owlClassA");
            JsonNode owlClassBNode = node.get("owlClassB");
            JsonNode fusionActionNode = node.get("fusionAction");

            Geometry geometryAwgs84 = null;
            //Geometry geometryA = null;
            String geometryStringWithoutQuotesA = removeQuotes(geometryANode.toString());

            try {
                String geometryStringWithoutQuotestTempA = geometryStringWithoutQuotesA.replace("LINESTRING", "POLYGON");
                geometryAwgs84 = wktReader.read(geometryStringWithoutQuotestTempA);

//                Coordinate[] coordinatesA = geometryAwgs84.getCoordinates();
//                Coordinate[] coordinatesCartesian = new Coordinate[coordinatesA.length];
//
//                for(int i =0; i<coordinatesA.length; i++){
//                    coordinatesCartesian[i] = JTS.transform(coordinatesA[i], null, transform);                    
//                }
//                geometryA = geometryFactory.createPolygon(coordinatesCartesian);
            } catch (ParseException ex) {
                System.out.println("Linestring not closed");
                try {
                    geometryAwgs84 = wktReader.read(geometryStringWithoutQuotesA);
//                    Coordinate[] coordinatesA = geometryAwgs84.getCoordinates();              
//                    Coordinate[] coordinatesCartesian = new Coordinate[coordinatesA.length];
//
//                    for(int i =0; i<coordinatesA.length; i++){
//                        coordinatesCartesian[i] = JTS.transform(coordinatesA[i], null, transform);                    
//                    }
//
//                    geometryA = geometryFactory.createPolygon(coordinatesCartesian);

                } catch (ParseException ex2) { //| TransformException
                    Logger.getLogger(MapPairJsonDeserializer.class.getName()).log(Level.SEVERE, null, ex2);
                }
            }

            Geometry geometryB = null;
            String geometryStringWithoutQuotesB = removeQuotes(geometryBNode.toString());

            try {
                String geometryStringWithoutQuotestTempB = geometryStringWithoutQuotesA.replace("LINESTRING", "POLYGON");

                geometryB = wktReader.read(geometryStringWithoutQuotestTempB);
            } catch (ParseException ex3) {
                System.out.println("Linestring not closed");
                try {
                    geometryB = wktReader.read(geometryStringWithoutQuotesB);
                } catch (ParseException ex1) {
                    Logger.getLogger(MapPairJsonDeserializer.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

            String owlClassA = removeQuotes(owlClassANode.toString());
            String owlClassB = removeQuotes(owlClassBNode.toString());

            String fusionAction = removeQuotes(fusionActionNode.toString());

            mapPair.setGeometryA(geometryAwgs84);
            mapPair.setGeometryB(geometryB);
            mapPair.setOWLClassA(owlClassA);
            mapPair.setOWLClassB(owlClassB);

            mapPair.setFusionAction(fusionAction);

            System.out.println("geometryA: " + mapPair.getGeometryA());
            System.out.println("geometryB: " + mapPair.getGeometryB());

            System.out.println("owlClassA: " + mapPair.getOWLClassA());
            System.out.println("owlClassB: " + mapPair.getOWLClassB());

            System.out.println("fusionAction: " + mapPair.getFusionAction());

        } catch (IOException ex) {
            Logger.getLogger(MapPairJsonDeserializer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return mapPair;
    }

    private static String removeQuotes(String geometryWithQuotes) {
        return geometryWithQuotes.substring(1, geometryWithQuotes.length() - 1);
    }

}
