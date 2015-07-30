
package gr.athenainnovation.imis.fusion.gis.learning.tagprediction;

//import com.vividsolutions.jts.geom.Geometry;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author imis-nkarag
 */

public class TagPredictor {
    
    private final Map<String, Integer> mapperWithIDs;
    private final MapPair mapPair;
    private int modelSVMLabelSize;
    private int[] modelSVMLabels;
    private final Map<String, String> mappings;
    private static final int RECOMMENDATIONS_SIZE = 12;
    private final Map<Integer, String> idsWithMappings;
    
    public TagPredictor(MapPair mapPair, Map<String, String> mappings, Map<String, Integer> mapperWithIDs, Map<Integer, String> idsWithMappings) {
               
        this.mappings = mappings;
        this.mapperWithIDs = mapperWithIDs;
        this.idsWithMappings = idsWithMappings;
        this.mapPair = mapPair;
        
    }    
    
    public String[] predictedTagsForA(String completeModelPath) {
        Model modelSVM = null;
        //create vector for A
        
        //Geometry geometryA = mapPair.getGeometryA();
        TagFeatures tagFeatures = new TagFeatures();
        tagFeatures.createGeometryFeaturesA(mapPair);
        //System.out.println("getting OWL class prediction..");
        System.out.println("recommending for A, loading OSMRec tag model, path: " + completeModelPath);
        File modelFile = new File(completeModelPath);
        
        //System.out.println("A modelFile can read? " + modelFile.canRead());
        //System.out.println("A modelFile can write? " + modelFile.canWrite());    
        try {
            modelSVM = Model.load(modelFile);
//            System.out.println(completeModelPath + ", (A) loaded model info: \n"
//                    + "nr classes: " + modelSVM.getNrClass()
//                    + "\n feature weights: " + Arrays.toString(modelSVM.getFeatureWeights())
//                    + "\n nr of features: " + modelSVM.getNrFeature()
//                    + "\n bias: " + modelSVM.getBias());
            
        } catch (IOException ex) {           
            Logger.getLogger(TagPredictor.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println("default tag prediction: " + 850);
        }
        
        
        //testing multiple class
        Feature[] testInstance1 = mapPair.getFinalFeaturesA();
        
        modelSVMLabelSize = modelSVM.getLabels().length;
        modelSVMLabels = modelSVM.getLabels(); 
                
//        Map<Integer, Integer> mapLabelsToIDs = new LinkedHashMap<>(modelSVMLabelSize);
//        for(int h =0; h < modelSVMLabelSize; h++){
//
//            mapLabelsToIDs.put(modelSVMLabels[h], h);
//            //System.out.println("h " + h + "   <->  score at " + modelSVMLabels[h]);
//            //System.out.println(h + "   <->    " + modelSVMLabels[h]);
//
//        } 
        double[] scores = new double[modelSVMLabelSize];
        Linear.predictValues(modelSVM, testInstance1, scores);

        Map<Double, Integer> scoresValues = new HashMap<>(scores.length); // TODO consider linked hash map here
        
        for(int h = 0; h < scores.length; h++){

            scoresValues.put(scores[h], h);
            //System.out.println("h " + h + "   <-> score at [h]" + scores[h]);

        }
        
        //System.out.println("scores unsorted: \n" + Arrays.toString(scores));
        Arrays.sort(scores);
        //System.out.println("scores sorted: \n" + Arrays.toString(scores));
           
        
        int[] preds = new int[RECOMMENDATIONS_SIZE];
        for(int p=0; p < RECOMMENDATIONS_SIZE; p++){
            preds[p] = modelSVMLabels[scoresValues.get(scores[scores.length-(p+1)])];
        }
             
        String[] predictedTags = new String[RECOMMENDATIONS_SIZE];
        
        //loop replacing iteration
        for(int p=0; p<RECOMMENDATIONS_SIZE; p++){
            if(idsWithMappings.containsKey(preds[p])){
                predictedTags[p] = idsWithMappings.get(preds[p]);
            }
            
        }
        
        
//        for( Map.Entry<String, Integer> entry : mapperWithIDs.entrySet()){
//            
//            for(int p=0; p<RECOMMENDATIONS_SIZE; p++){
//                
//                if(entry.getValue().equals(preds[p])){
//                    //System.out.println(p + "th predicted class(A): " + entry.getKey());   
//                                       
//                    predictedTags[p] = entry.getKey();
//                    break;
//                }
//            }                    
//        }        
        
//        String[] rankedPredictedTags = new String[RECOMMENDATIONS_SIZE];
//        for(Map.Entry<String, String> tag : mappings.entrySet()){
//
//            for(int k=0; k < RECOMMENDATIONS_SIZE; k++){
//                if(tag.getValue().equals(predictedTags[k])){
//                    rankedPredictedTags[k] = tag.getKey();
//                    //model.addElement(tag.getKey());
//                }
//            }
//        }
        
        System.out.println("OSMRec model path: " + completeModelPath);
        //System.out.println("tagsA, predicted classes: " + Arrays.toString(rankedPredictedTags));    
        System.out.println("tagsA, predicted classes: " + Arrays.toString(predictedTags));
        
        return predictedTags;
        //return rankedPredictedTags;
    }

    public String[] predictedTagsForB(String completeModelPath) {
        Model modelSVM = null;
        TagFeatures tagFeatures = new TagFeatures();
        tagFeatures.createGeometryFeaturesB(mapPair);
        //System.out.println("getting OWL class prediction..");
        //System.out.println("loading OSMRec tag model, path: " + completeModelPath);
        System.out.println("recommending for B, loading OSMRec tag model, path: " + completeModelPath);
        File modelFile = new File(completeModelPath);
        
        //System.out.println("B modelFile can read? " + modelFile.canRead());
        //System.out.println("B modelFile can write? " + modelFile.canWrite());
        try {
            modelSVM = Model.load(modelFile);
//            System.out.println(completeModelPath + ", (B) loaded model info: \n"
//                    + "nr classes: " + modelSVM.getNrClass()
//                    + "\n feature weights: " + Arrays.toString(modelSVM.getFeatureWeights())
//                    + "\n nr of features: " + modelSVM.getNrFeature()
//                    + "\n bias: " + modelSVM.getBias());            
            
        } catch (IOException ex) {           
            Logger.getLogger(TagPredictor.class.getName()).log(Level.SEVERE, null, ex);
            //return 850.0;
        }
        
         //testing multiple class
        Feature[] testInstance2 = mapPair.getFinalFeaturesB();
        //mapPair.
        
        modelSVMLabelSize = modelSVM.getLabels().length;
        modelSVMLabels = modelSVM.getLabels(); 
        
        
//        Map<Integer, Integer> mapLabelsToIDs = new LinkedHashMap<>(modelSVMLabelSize);
//        for(int h = 0; h < modelSVMLabelSize; h++){
//            
//            //System.out.println("h" + h + "   <->  score at " + modelSVMLabels[h]);
//            mapLabelsToIDs.put(modelSVMLabels[h], h);           
//
//        }
        
        double[] scores = new double[modelSVMLabelSize];
        Linear.predictValues(modelSVM, testInstance2, scores);

        Map<Double, Integer> scoresValues = new HashMap<>(scores.length);
        for(int h = 0; h < scores.length; h++){
            
            //System.out.println("h" + h + "   <-> score at [h]" + scores[h]);
            scoresValues.put(scores[h], h);
            
        }
        //System.out.println("scores unsorted: \n" + Arrays.toString(scores));
        Arrays.sort(scores);
        
        //System.out.println("scores sorted: \n" + Arrays.toString(scores));              
        
        int[] preds = new int[RECOMMENDATIONS_SIZE];
        for(int p=0; p < RECOMMENDATIONS_SIZE; p++){
            preds[p] = modelSVMLabels[scoresValues.get(scores[scores.length-(p+1)])];
        }
        
        String[] predictedTags = new String[RECOMMENDATIONS_SIZE];
        //loop replacing iteration
        for(int p=0; p < RECOMMENDATIONS_SIZE; p++){
            //if(idsWithMappings.containsKey(preds[p])){
                predictedTags[p] = idsWithMappings.get(preds[p]);
            //}           
        }       
        
        
        
//        for( Map.Entry<String, Integer> entry : mapperWithIDs.entrySet()){
//
//            for(int p=0; p<RECOMMENDATIONS_SIZE; p++){
//                if(entry.getValue().equals(preds[p])){
//                    //System.out.println(p + "th predicted class (B): " + entry.getKey());                     
//                    predictedTags[p] = entry.getKey();
//                    break;
//                }
//            }                                   
//        }        
        
//        String[] rankedPredictedTags = new String[RECOMMENDATIONS_SIZE];
//        for(Map.Entry<String, String> tag : mappings.entrySet()){
//
//            for(int k = 0; k < RECOMMENDATIONS_SIZE; k++){
//                if(tag.getValue().equals(predictedTags[k])){
//                    rankedPredictedTags[k] = tag.getKey();
//                    //model.addElement(tag.getKey());
//                }
//            }
//        }
        System.out.println("OSMRec model path: " + completeModelPath);
        //System.out.println("tagsB, test multi, predicted classes: " + Arrays.toString(rankedPredictedTags));    
        System.out.println("tagsB, test multi, predicted classes: " + Arrays.toString(predictedTags));    
        return predictedTags;
        //return rankedPredictedTags;       
    }
    
}
