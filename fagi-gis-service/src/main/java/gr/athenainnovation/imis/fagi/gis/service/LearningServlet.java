
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.learning.container.EntitiesCart;
import gr.athenainnovation.imis.fusion.gis.learning.container.FusionActions;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPairFactory;
import gr.athenainnovation.imis.fusion.gis.learning.core.Predictor;
//import gr.athenainnovation.imis.fusion.gis.learning.core.TrainInitiator;
import gr.athenainnovation.imis.fusion.gis.learning.core.TrainWorker;
import gr.athenainnovation.imis.fusion.gis.learning.tagprediction.OWLClassesParser;
import gr.athenainnovation.imis.fusion.gis.learning.tagprediction.TagPredictor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.sql.Connection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import org.apache.commons.io.FileUtils;
//import javax.servlet.http.HttpSession;

/**
 *
 * @author imis-nkarag
 * @author nick
 */

@WebServlet(name = "LearningServlet", urlPatterns = {"/LearningServlet"})
public class LearningServlet extends HttpServlet {
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String PATH = System.getProperty("user.home");   //when apache starts with sudo, this fails. returns /root 
    //private static final String PATH_WIN = System.getenv("ProgramFiles");  //  C:\Program Files  
    private static final String PATH_WIN = System.getenv("ProgramFiles(X86)");  //  C:\Program Files x86
    
    private static final String PATH_DOC = System.getProperty("user.home") + File.separatorChar + "My Documents";
    
    //private static final String PATH_WIN = "C:";
    
    /* //old paths.
        private static final String LINUX_TAG_MODEL = PATH + "/FAGI_models/best_model";   
        private static final String WINDOWS_TAG_MODEL = PATH_WIN + "\\FAGI_models\\best_model";   
        private static final String LINUX_FUSIONS_MODEL = PATH + "/FAGI_models/model";
        private static final String WINDOWS_FUSIONS_MODEL = PATH_WIN + "\\FAGI_models\\model";               
        private static final String LINUX_DEFAULT_FUSIONS_MODEL = PATH + "/FAGI_models/default_model";
        private static final String WINDOWS_DEFAULT_FUSIONS_MODEL = PATH_WIN + "\\FAGI_models\\default_model";
        private static final String LINUX_MAP_FILE = PATH + "/FAGI_models/Map";
        private static final String WINDOWS_MAP_FILE = PATH_WIN + "\\FAGI_models\\Map";

        private static final String LINUX_TRAIN_FILE = PATH + "/FAGI_models/trainFile.ser";
        private static final String WINDOWS_TRAIN_FILE = PATH_WIN + "\\FAGI_models\\trainFile.ser";   
    */
       
    private static final String LINUX_TAG_MODEL = PATH + "/FAGI-gis models/best_model";
    
    private static final String MODELS_USR = "/usr/share/fagi-gis-service/models";
    
    //private static final String LINUX_HOME = PATH + "/FAGI_models";
    
    private static final String WINDOWS_TAG_MODEL = PATH_DOC + "\\FAGI-gis\\SVM_models\\best_model";   
    private static final String LINUX_FUSIONS_MODEL = PATH + "/FAGI-gis models/model";
    private static final String WINDOWS_FUSIONS_MODEL = PATH_DOC + "\\FAGI-gis\\SVM_models\\model";               
    private static final String LINUX_DEFAULT_FUSIONS_MODEL = PATH + "/FAGI-gis models/default_model";
    private static final String WINDOWS_DEFAULT_FUSIONS_MODEL = PATH_DOC + "\\FAGI-gis\\SVM_models\\default_model";
    private static final String LINUX_MAP_FILE = PATH + "/FAGI-gis models/Map";
    private static final String WINDOWS_MAP_FILE = PATH_WIN + "\\FAGI-gis\\files\\ontology\\Map";
    
    private static final String LINUX_TRAIN_FILE = PATH + "/FAGI-gis models/train/trainData.ser";
    private static final String WINDOWS_TRAIN_FILE = PATH_DOC + "\\FAGI-gis\\SVM_models\\train_data\\trainFile.ser";      
    
    private static final int TAG_REC_SIZE = 6;
            
    
    private static boolean modelExists = true;
    private static boolean isLinux = false;
    private static EntitiesCart batchEntitiesCart;

    
    private FusionActions fusionActions;
    private MapPairFactory mapPairFactory;
    private EntitiesCart entitiesCart;
    private Map<String, String> mappings;
    private Map<String, Integer> mapperWithIDs;
    private Map<Integer, String> idsWithMappings;
    private static final boolean NETBEANS = true;
    //private final Connection dbConn = null;
    //private final HttpSession sess = null;
    
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);       
//        System.out.println("base PATH linux = " + PATH);
//        System.out.println("base PATH windows = " + PATH_WIN);
        System.out.println(FuserPanel.transformations);
                      
        File fagiModels = new File(MODELS_USR);
        File pathToPaste = new File(PATH + "/FAGI-gis models");
        System.out.println("fagiModels path: \n" + MODELS_USR);
        System.out.println("user home path: \n" + PATH);
        
        try {
            FileUtils.copyDirectory(fagiModels, pathToPaste);
        } catch (IOException ex) {
            Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        
        isLinux = !OS.contains("win");
        
        parseTagsMappedToClasses();
        
        fusionActions = new FusionActions(FuserPanel.transformations);
        //fusionActions.registerActions();  
        entitiesCart = new EntitiesCart();
        mapPairFactory = new MapPairFactory(entitiesCart, fusionActions);
        //mapPairFactory.produceSomeTestEntities();
        
        
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
                    /* temporary testing batch code */  

//                    System.out.println("starting batch");
//                    HttpSession sess = request.getSession(true);
//                    DBConfig dbConf = (DBConfig)sess.getAttribute("db_conf");
//                    BatchLearning b = new BatchLearning(dbConf, entitiesCart);
//                    b.startBatch(-1);
//                    System.out.println("ending batch");

                    /* end of temporary testing batch code */
        

        try (PrintWriter out = response.getWriter()) {
            
            String JSONdata = request.getParameter("actions");
            //System.out.println(JSONdata);
            System.out.println("JSONdata: " + JSONdata);
            
            mapPairFactory.addMapPair(JSONdata);           
            TagPredictor tagPredictor = new TagPredictor(mapPairFactory.getCurrentMapPair(), mappings, mapperWithIDs, idsWithMappings);
                        
            LinkedHashSet<String> tagsSet = new LinkedHashSet<>();
                                  
            String[] tagsA;
            if(isLinux){
                tagsA = tagPredictor.predictedTagsForA(LINUX_TAG_MODEL);
                //System.out.println("lala " + Arrays.toString(tagsA));
            }
            else{
                tagsA = tagPredictor.predictedTagsForA(WINDOWS_TAG_MODEL);
            }
                                 
            for(int i =0; i < TAG_REC_SIZE; i++){
                tagsSet.add(tagsA[i]);
//                if(!(mappings.get(tagsA[i]) == null)){               
//                    tagsSet.add(mappings.get(tagsA[i])); //adding owl class, not the tag 
//                }
            }
            
            String[] tagsB;
            if(isLinux){
                tagsB = tagPredictor.predictedTagsForB(LINUX_TAG_MODEL);
                //System.out.println("lala " + Arrays.toString(tagsB));
            }
            else{
                tagsB = tagPredictor.predictedTagsForB(WINDOWS_TAG_MODEL);
            }
                      
            for(int i = 0; i < TAG_REC_SIZE; i++){
                tagsSet.add(tagsB[i]);
//                if(!(mappings.get(tagsB[i]) == null)){
//                    tagsSet.add(mappings.get(tagsB[i])); //adding owl class, not the tag
//                }
                //System.out.println("lala " + Arrays.toString(tagsA));
            }            

            JSONPredictions recommendations = new JSONPredictions();
            System.out.println("final prediction set: " + tagsSet.toString());
            recommendations.setTagList(tagsSet);
            
            if (mapPairFactory.getCurrentMapPair().isUnclassified()) {
                Double prediction;
                if (modelExists) {
                    if(isLinux){
                        prediction = Predictor.getPrediction(entitiesCart.getLastMapPair(), LINUX_FUSIONS_MODEL);
                        //System.out.println("prediction from model at " + LINUX_FUSIONS_MODEL + " successful: " + prediction);
                    }
                    else{
                        prediction = Predictor.getPrediction(entitiesCart.getLastMapPair(), WINDOWS_FUSIONS_MODEL);
                        //System.out.println("prediction from model at " + WINDOWS_FUSIONS_MODEL + " successful: " + prediction);
                    }

                } else {
                    if(isLinux){
                        prediction = Predictor.getPredictionByDefaultModel(entitiesCart.getLastMapPair(), LINUX_DEFAULT_FUSIONS_MODEL);
                        //System.out.println("prediction from default model at " + LINUX_DEFAULT_FUSIONS_MODEL + " successful: " + prediction);
                    }
                    else{
                        prediction = Predictor.getPredictionByDefaultModel(entitiesCart.getLastMapPair(), WINDOWS_DEFAULT_FUSIONS_MODEL);
                        //System.out.println("prediction from default model at " + WINDOWS_DEFAULT_FUSIONS_MODEL +" successful: " + prediction);    
                    }
                }

                Map<String, AbstractFusionTransformation> actions = fusionActions.getActions();
                for(Map.Entry<String, AbstractFusionTransformation> action : actions.entrySet()){
                    if(prediction.intValue() == action.getValue().getIntegerID()){
                        //System.out.println("fusion recommendation: " + action.getKey());
                        recommendations.setPredictedFusionAction(action.getKey());
                        break;
                    }
                    else{ 
                        //System.out.println("fusion not recommended: " + action.getKey());
                    }
                }
            }
            else{
                //System.out.println("has been fused with: " + mapPairFactory.getCurrentMapPair().getFusionAction());
                recommendations.setPredictedFusionAction(mapPairFactory.getCurrentMapPair().getFusionAction());  
            }
             
            
            if (entitiesCart.isFull()) {
                //writeTrainFileToDisk(entitiesCart);
                initiateTraining();
                
            }
            else{
                System.out.println("CART NOT FULL");
            }

            //System.out.println("tag A predicted: " + tagA);
            //System.out.println("tag B predicted: " + tagB);
            //System.out.println("fusionAction predicted: " + recommendations.getPredictedFusionAction()); 
            
            ObjectMapper mapper = new ObjectMapper();
            out.println(mapper.writeValueAsString(recommendations));
            
            //setBatchEntitiesCart(entitiesCart);
            //ServletContext context = getServletContext();
            //File appHome = new File(getServletContext().getInitParameter("MyAppHome"));
            
//            if(NETBEANS){                
//                //get models as is, from user.home
//            }
//            else{
                
//                File f = new File(new File(".").getCanonicalPath());
//
//                String path = f.getParent();
//                String modelPath = path + "/webapps/fagi-gis-service/WEB-INF/classes/models/model";
//                String defaultModelPath = path + "/webapps/fagi-gis-service/WEB-INF/classes/models/default_model";
//                String mapPath = path + "/webapps/fagi-gis-service/WEB-INF/classes/models/Map";
//                
//                if(new File(modelPath).exists()){
//                    System.out.println("model file exists: " + modelPath);
//                    System.out.println("default_model file exists: " + defaultModelPath);
//                    System.out.println("Map file exists: " + mapPath);
//                }
//                else{
//                    System.out.println("could not locate file: " + modelPath);
//                    System.out.println("could not locate file default_model: " + defaultModelPath);
//                    System.out.println("could not locate file Map: " + mapPath);
//                }
//            }

            System.out.println("process finished.");
            //out.println("Your JSON");
            
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    
        private void parseTagsMappedToClasses() {

        //InputStream tagsToClassesMapping = TrainWorker.class.getResourceAsStream("/resources/files/Map");
        
        File initialFile;
        if(isLinux){
            System.out.println("trying to load OWL classes, path (linux): " + LINUX_MAP_FILE);
            initialFile = new File(LINUX_MAP_FILE); 
            
        } 
        else{
            System.out.println("trying to load OWL classes, path (windows): " + WINDOWS_MAP_FILE);
            initialFile = new File(WINDOWS_MAP_FILE);
        }
        
        
        InputStream tagsToClassesMapping;
        try {
            tagsToClassesMapping = new FileInputStream(initialFile);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not load default OWL classes from file!");
            Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

         OWLClassesParser mapper = new OWLClassesParser();
         try {   
             mapper.parseFile(tagsToClassesMapping);

         } catch (FileNotFoundException ex) {
             Logger.getLogger(OWLClassesParser.class.getName()).log(Level.SEVERE, null, ex);
         }
         mappings = mapper.getMappings();
         mapperWithIDs = mapper.getMappingsWithIDs(); 
         idsWithMappings = mapper.getIDsWithMappings();
         System.out.println("mappings " + mappings);
         System.out.println("mapperWithIDs " + mapperWithIDs);
         System.out.println("idsWithMappings " + idsWithMappings);
     }
          
    public EntitiesCart getEntitiesCart(){
        return entitiesCart;
    }

    private void initiateTraining() {
        
        System.out.println("CART IS FULL, learning process should start");  
                              
        //1. Deserialize EntitiesCart from disk
        //2. Add this cart to the deserialized list
        //3. Serialize enriched entitiesCart from scratch. (RE-WRITE PREVIOUS FILE)
        //
        TrainWorker trainWorker;
        if(isLinux){
            trainWorker = new TrainWorker(entitiesCart, LINUX_FUSIONS_MODEL);
        }
        else{
            trainWorker = new TrainWorker(entitiesCart, WINDOWS_FUSIONS_MODEL);
        }
        

        PropertyChangeListener propChangeListn = new PropertyChangeListener() {              
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if("DONE".equals(pce.getNewValue().toString())){
                    System.out.println("training is DONE:" + pce.getNewValue());
                    modelExists = true;
                    entitiesCart.emptyCart();
                }
            }

        };

        trainWorker.addPropertyChangeListener(propChangeListn);
        //modelExists = false;
        try {   
            System.out.println("training process started!");
            trainWorker.execute();

        } catch (Exception ex) {
            System.out.println("train worker thread error:\n");
            Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void setBatchEntitiesCart(EntitiesCart entitiesCart){
        batchEntitiesCart = entitiesCart;
    }    
    
    public static EntitiesCart getCart(){
        return batchEntitiesCart;
    }

    private void writeTrainFileToDisk(EntitiesCart entitiesCart) {
        if(isLinux){
            try (FileOutputStream fileOut = new FileOutputStream(LINUX_TRAIN_FILE); 
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(entitiesCart);
            }
            catch(IOException e){
                System.out.println("serialize error:\n " + e);
            }                    
        }
        else{
            try (FileOutputStream fileOut = new FileOutputStream(WINDOWS_TRAIN_FILE); 
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(entitiesCart);
            }
            catch(IOException e){
                System.out.println("serialize error:\n " + e);
            }             
        }
    }
    
    private EntitiesCart obtainTrainFileFromDisk(){
        EntitiesCart entitiesFromFile = null;
        FileInputStream fileIn = null; 
        try {
            if(isLinux){
                fileIn = new FileInputStream(LINUX_TRAIN_FILE);
            }
            else{
                fileIn = new FileInputStream(WINDOWS_TRAIN_FILE);
            }

            ObjectInputStream in = new ObjectInputStream(fileIn) ;
            entitiesFromFile = (EntitiesCart) in.readObject();

            System.out.println("entitiesCart obtained from file, isFull?: " + entitiesFromFile.isFull());
            System.out.println("entitiesCart obtained from file, getMapEntitiesSize: " + entitiesFromFile.getMapEntities().size());
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                fileIn.close();
            } catch (IOException ex) {
                Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
                
            }
        }  
        return entitiesFromFile;
    }
    
    private static class JSONPredictions {
        
        private LinkedHashSet<String> tagList;
        private String predictedFusionAction;

        public JSONPredictions() {
        }

        public void setTagList(LinkedHashSet<String> tagList) {
            //this.tagList.clear();
            this.tagList = new LinkedHashSet<>(tagList); 
            //this.tagList = tagList;
        }        

        public HashSet<String> getTagList(){
            return new HashSet<>(tagList);
        }       

        public void setPredictedFusionAction(String predictedFusionAction){
            this.predictedFusionAction = predictedFusionAction;
        }
        
        public String getPredictedFusionAction() {
            return predictedFusionAction;
        }
    }      
}
