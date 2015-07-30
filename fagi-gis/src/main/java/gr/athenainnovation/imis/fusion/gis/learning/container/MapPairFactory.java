
package gr.athenainnovation.imis.fusion.gis.learning.container;

//import org.opengis.referencing.operation.MathTransform;
//import org.geotools.referencing.CRS;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.learning.vectors.BooleanVector;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.map.ObjectMapper;
//import org.geotools.referencing.crs.DefaultGeocentricCRS;
//import org.geotools.referencing.crs.DefaultGeographicCRS;
//import org.opengis.referencing.FactoryException;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Takes the data from the interface and constructs the MapPair objects for every instance.
 * The created objects are put into the cart.
 * 
 * @author imis-nkarag
 */

public class MapPairFactory {
       
    private final EntitiesCart entitiesCart;
    private final FusionActions fusionActions;
    private MapPair currentMapPair;
        
    public MapPairFactory(EntitiesCart entitiesCart, FusionActions fusionActions) {
        
        this.entitiesCart = entitiesCart;
        this.fusionActions = fusionActions;
        
    }

    public void addMapPair(String json) {
        
        MapPair mapPair = new MapPair();
        try {
            mapPair = new ObjectMapper().readValue(json, MapPair.class);
        } catch (IOException ex) {
            System.out.println("something wrong with the json!");
            Logger.getLogger(MapPairFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("map Pair constructed from Factory: \n"  
                + mapPair.getOWLClassA() + "\n"
                + mapPair.getOWLClassB() + "\n"
                + mapPair.getGeometryA() + "\n"
                + mapPair.getGeometryB() + "\n"
                + mapPair.getFusionAction() + "\n"
        ); 
        
        BooleanVector booleanVector = new BooleanVector(mapPair);
        booleanVector.createGeometryFeatures();
        booleanVector.createOWLFeatures();
        
        //booleanVector.showFeatures();

        for(Map.Entry<String, AbstractFusionTransformation> fusionAction : fusionActions.getActions().entrySet()){
            if(fusionAction.getValue().getID().equals(mapPair.getFusionAction())){
//                System.out.println("FUSION ACTION FOUND EQUAL: " + fusionAction.getValue().getID() + "\n" 
//                        + mapPair.getFusionAction());
                mapPair.setClassID(fusionAction.getValue().getIntegerID());
                mapPair.setUnclassified(false);
                
                break;
            }
            else{
                mapPair.setUnclassified(true);                            
                //System.out.println("NOT EQUAL: " + fusionAction.getValue().getID() + "\n" 
                //        + mapPair.getFusionAction());
            }
            
        }
        entitiesCart.addEntity(mapPair);
//        if(!mapPair.isUnclassified()){ 
//            entitiesCart.addEntity(mapPair);
//            System.out.println("map pair added to train list"); 
//        }
//        else{
//            if(entitiesCart.isEmpty()){
//                entitiesCart.addEntity(mapPair); //init entitiesCart
//            }
//        }
        setCurrentMapPair(mapPair);
        
    }
    
    private void setCurrentMapPair(MapPair currentMapPair){
        this.currentMapPair = currentMapPair;
    }
    
    public MapPair getCurrentMapPair(){
        return currentMapPair;
    }
    
    //debug method
    /*
    private void defineGeometry(MapPair mapPair, String geomType) {
        double latitude1 = 0.50;
        double longitude1 = 0.84;
        Coordinate coordinate1 = new Coordinate(longitude1, latitude1);
        
        double latitude2 = 0.51;
        double longitude2 = 0.85;
        Coordinate coordinate2 = new Coordinate(longitude2, latitude2);
        
        double latitude3 = 0.52;
        double longitude3 = 0.86; 
        Coordinate coordinate3 = new Coordinate(longitude3, latitude3);
        
        double latitude4 = 0.50;
        double longitude4 = 0.84; 
        Coordinate coordinate4 = new Coordinate(longitude4, latitude4);       
        
        if(geomType.equalsIgnoreCase("point")){
            Coordinate targetGeometry = null;
            Coordinate sourceCoordinate = new Coordinate(longitude1, latitude1);
            try {    
                targetGeometry = JTS.transform(sourceCoordinate, null, transform);
            } catch (MismatchedDimensionException | TransformException ex) {
                Logger.getLogger(MapPairFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
            Geometry geom = geometryFactory.createPoint(coordinate1);

            mapPair.setGeometryA(geom);
        }
        else if(geomType.equalsIgnoreCase("polygon")){
            Coordinate[] poly = {coordinate1, coordinate2, coordinate3, coordinate4};
            Geometry geom = geometryFactory.createPolygon(poly);
            mapPair.setGeometryA(geom);
        }
        else if(geomType.equalsIgnoreCase("linestring")){
            Coordinate[] line = {coordinate1, coordinate2, coordinate3, coordinate4};
            Geometry geom =  geometryFactory.createLineString(line);
            mapPair.setGeometryA(geom);
            
        }
    } 
    */
    
    /*
    //debug/test method
    private void produceSomeTestEntities(){
    
        //Geometry geom1 = new Geometry();
        String geo1 = "Point";
        MapPair m1 = new MapPair();
        defineGeometry(m1, geo1);         
        
        m1.addFeature(new FeatureNode(1, 4));
        m1.addFeature(new FeatureNode(2, 2));
        classifyEntity(m1);
        entitiesCart.addEntity(m1);
        
        String geo2 = "Polygon";
        MapPair m2 = new MapPair();
        defineGeometry(m2, geo2);
        m2.addFeature(new FeatureNode(1, 3));
        m2.addFeature(new FeatureNode(2, 5));
        classifyEntity(m2);
        entitiesCart.addEntity(m2);
        
        String geo3 = "Linestring";
        MapPair m3 = new MapPair();
        defineGeometry(m3, geo3);
        m3.addFeature(new FeatureNode(1, 4));
        m3.addFeature(new FeatureNode(2, 5)); 
        classifyEntity(m3);
        entitiesCart.addEntity(m3);
    }   
    */
    
    
    /*
    //debug method
    private void classifyEntity(MapPair mapPair){
        String fusionAction = mapPair.getFusionAction();
        if(mapPair.getFusionAction() != null){
            if(fusionAction.equals("ShiftBToA")){
                mapPair.setClassID(0);
            }
            else if(fusionAction.equals("ShiftAToB")){
                mapPair.setClassID(2);
            }
            else {
                mapPair.setClassID(1);
            }
        }
        else{
            mapPair.setClassID(1);
        }       
        
    } 
    */
      
}
