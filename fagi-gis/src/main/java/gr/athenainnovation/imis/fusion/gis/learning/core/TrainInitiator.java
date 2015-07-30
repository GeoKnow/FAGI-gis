
package gr.athenainnovation.imis.fusion.gis.learning.core;

import gr.athenainnovation.imis.fusion.gis.learning.container.EntitiesCart;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPairFactory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author imis-nkarag
 */

public class TrainInitiator {
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isLinux = false;
    private static boolean modelExists = false;
    
    
    public static void main(String[] args) throws IOException{
//        defineOS();
//
//        String json = "{\"geometryA\":\"point(1 1)\", "
//                + "\"geometryB\":\"linestring(1 1, 2 2, 3 3, 4 4, 1 1)\","
//                + "\"owlClassA\":\"hotel\","
//                + "\"owlClassB\":\"amenity\","
//                + "\"fusionAction\":\"ShiftAToB\""
//                + "}";
//
//        EntitiesCart entitiesCart = new EntitiesCart();
//        
//        //produce some test entities
//        MapPairFactory mapPairFactory = new MapPairFactory(entitiesCart);
//        mapPairFactory.produceSomeTestEntities();
//        
//        //if(!entitiesCart.isFull()){
//        if(false){    //TEMPORARY TO TEST TRAINING, TOGGLE COMMENT
//            mapPairFactory.addMapPair(json);
//            
//        }
//        else{
//            mapPairFactory.addMapPair(json); //remove this also
//            TrainWorker trainWorker = new TrainWorker(entitiesCart);
//            try {
//                
//                trainWorker.doInBackground();
//                
//                if(trainWorker.isDone()){
//                    modelExists = true;
//                }
//            } catch (Exception ex) {
//                Logger.getLogger(TrainInitiator.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }

    }
    
    
    
    private static void defineOS() {
       if(OS.contains("nux")){
           isLinux = true;
       }
       else if(OS.contains("win")){
           isLinux = false;
       }
       else{
           System.out.println("Your operating system is not supported yet :/");
           System.exit(0);
       }
    }
}
