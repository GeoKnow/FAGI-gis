
package gr.athenainnovation.imis.fusion.gis.learning.core;

import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
//import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author imis-nkarag
 */

public class Predictor {
    //private static final String PATH = System.getProperty("user.home");
    
    private Predictor(){
        
    }
    
    public static double getPrediction(MapPair mapPair, String modelPath) throws IOException{
        
//        File currentDirectory = new File(new File(".").getAbsolutePath());
//        System.out.println("canonical path " + currentDirectory.getCanonicalPath());
//        System.out.println("absolute path" + currentDirectory.getAbsolutePath());
        
        //System.out.println("getting prediction..");
        Model modelSVM = null;
        System.out.println("recommending for fusion, loading fusion model, path: " + modelPath);
        File modelFile = new File(modelPath);
        
//        System.out.println("fusion modelFile can read? " + modelFile.canRead());
//        System.out.println("fusion modelFile can write? " + modelFile.canWrite());
        
        try {
            modelSVM = Model.load(modelFile);
           
            System.out.println(modelPath + ", (B) loaded model info: \n"
                    + "nr classes: " + modelSVM.getNrClass()
                    + "\n feature weights: " + Arrays.toString(modelSVM.getFeatureWeights())
                    + "\n nr of features: " + modelSVM.getNrFeature()
                    + "\n bias: " + modelSVM.getBias()); 
            
        } catch (IOException ex) {           
            Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
            //return 2.0;
        }
        //Model model = ;
        double prediction = Linear.predict(modelSVM, mapPair.getFinalFeatures());
        System.out.println("fusion model path: " + modelPath);
        System.out.println("fusion predicted class: " + prediction); 
        return prediction;
    }
     
    
    public static double getPredictionByDefaultModel(MapPair mapPair, String completeDefaultModelPath) throws IOException{
        Model modelSVM = null;
//        File currentDirectory = new File(new File(".").getAbsolutePath());
//        System.out.println("canonical path " + currentDirectory.getCanonicalPath());
//        System.out.println("absolute path" + currentDirectory.getAbsolutePath());
        
        System.out.println("default recommending for fusion, loading default fusion model, path: " + completeDefaultModelPath);
        File modelFile = new File(completeDefaultModelPath);

//        System.out.println("default fusion modelFile can read? " + modelFile.canRead());
//        System.out.println("default fusion modelFile can write? " + modelFile.canWrite());  
        
        try {
            modelSVM = Model.load(modelFile);

            
            System.out.println(completeDefaultModelPath + ", (B) loaded model info: \n"
                    + "nr classes: " + modelSVM.getNrClass()
                    + "\n feature weights: " + Arrays.toString(modelSVM.getFeatureWeights())
                    + "\n nr of features: " + modelSVM.getNrFeature()
                    + "\n bias: " + modelSVM.getBias());             
            
            
        } catch (IOException ex) {           
            Logger.getLogger(Predictor.class.getName()).log(Level.SEVERE, null, ex);
            //return 2.0;
        }
        //Model model = ;
        double prediction = Linear.predict(modelSVM, mapPair.getFinalFeatures());
        System.out.println("fusion model path: " + completeDefaultModelPath);
        System.out.println("fusion predicted class: " + prediction); 
        return prediction;
    }

}
