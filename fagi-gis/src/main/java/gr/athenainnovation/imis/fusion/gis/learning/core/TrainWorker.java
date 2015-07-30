
package gr.athenainnovation.imis.fusion.gis.learning.core;

//import de.bwaldvogel.liblinear.Feature;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import gr.athenainnovation.imis.fusion.gis.learning.container.EntitiesCart;
import gr.athenainnovation.imis.fusion.gis.learning.vectors.BooleanVector;
import java.io.File;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author imis-nkarag
 */

//public class TrainWorker implements Runnable {
public class TrainWorker extends SwingWorker<Void, Void> {

    private static final double C = 0.1;    //cost of constraints violation
    private static final double EPS = 0.01; //stopping criteria
    //private final int trainsetSize; 
    private final int numberOfFeatures = BooleanVector.getLastID(); //143 this number includes linkLength and angle features
    //private final List<MapPair> entityList = new ArrayList<>();
    private final EntitiesCart entitiesCart;
    private final String modelTobBeSavedPath;
    private Model model;
    
    public TrainWorker(EntitiesCart entitiesCart, String modelToBeSavedPath){
        this.entitiesCart = entitiesCart;
        this.modelTobBeSavedPath = modelToBeSavedPath;
        //trainsetSize = entitiesCart.getSize();
               
    }
    
    @Override
    public Void doInBackground() throws Exception {
        System.out.println("Thread " + Thread.currentThread().getName() + " of trainWorker executing in backround..");
        int trainSetCapacity = 0;
        for(MapPair mapPair : entitiesCart.getMapEntities()){
            if(!mapPair.isUnclassified()){
                trainSetCapacity++;
            }
        }
        double[] GROUPS_ARRAY = new double[trainSetCapacity];
        FeatureNode[][] trainingSetWithUnknown = new FeatureNode[trainSetCapacity][numberOfFeatures];
        int k = 0;

        for(MapPair mapPair : entitiesCart.getMapEntities()){  
            if(!mapPair.isUnclassified()){
                
                System.out.println("deb trainlist iter .. " + k);
                List<FeatureNode> featureNodeList = mapPair.getFeatureNodeList();
                System.out.println("feature node list size: " + featureNodeList.size()+ ", k: " + k);
                FeatureNode[] featureNodeArray = new FeatureNode[featureNodeList.size()];

                for(int i = 0; i < featureNodeList.size(); i++ ){  
                    //System.out.println("deb feature node list iter .. " + i);
                    featureNodeArray[i] = featureNodeList.get(i);
                } 

                GROUPS_ARRAY[k] = mapPair.getClassID();
                System.out.println("after groups array.. (added class:" + mapPair.getClassID() + ")");
                trainingSetWithUnknown[k] = featureNodeArray;  
                System.out.println(k + " k featureNodeArray:\n" + Arrays.toString(featureNodeArray));
                //System.out.println("after training set with unknown .. ");
                k++;
            }
        }

        System.out.println("initializing Liblinear Problem .. ");
        Problem problem = new Problem();
        problem.l = trainSetCapacity;
        problem.n = numberOfFeatures;
        problem.x = trainingSetWithUnknown; // feature nodes
        problem.y = GROUPS_ARRAY; // target values

        //SolverType solver = SolverType.L2R_LR; // -s 0
        SolverType solver = SolverType.getById(2); //2 -- L2-regularized L2-loss support vector classification (primal)
        
        Parameter parameter = new Parameter(solver, C, EPS);
        
        System.out.println("starting training... ");
        model = Linear.train(problem, parameter);
        System.out.println("model trained... ");
        System.out.println("model will be saved at: " + modelTobBeSavedPath);
        File modelFile = new File(modelTobBeSavedPath);
        model.save(modelFile);      
        //model = Model.load(modelFile);
        System.out.println("trainWorker return");
        return null;
    }
    
    @Override
    public void done() {
        System.out.println("Thread done method.");
       //this.firePropertyChange("done", this, this);
        //calling get to prevent swallowing possible exceptions 
        try {          
            get();
            System.out.println("Thread done. Training complete.");
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TrainWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
