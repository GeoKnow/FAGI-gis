
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.learning.container.EntitiesCart;
import gr.athenainnovation.imis.fusion.gis.learning.container.FusionActions;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPairFactory;
//import gr.athenainnovation.imis.fusion.gis.learning.core.BatchLearning;
//import gr.athenainnovation.imis.fusion.gis.learning.core.TrainWorker;
import gr.athenainnovation.imis.fusion.gis.learning.tagprediction.OWLClassesParser;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

/**
 *
 * @author imis-nkarag
 */

@WebServlet(name = "BatchLearningServlet", urlPatterns = {"/BatchLearningServlet"})
public class BatchLearningServlet extends HttpServlet {
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String PATH = System.getProperty("user.home");
    private static boolean modelExists = false;
    private static boolean isLinux = false;

    
    private FusionActions fusionActions;
    private MapPairFactory mapPairFactory;
    private EntitiesCart entitiesCart;
    private Map<String, String> mappings;
    private Map<String, Integer> mapperWithIDs;

    private Connection dbConn = null;
    private HttpSession sess = null;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("got in train servlet");

        System.out.println(FuserPanel.transformations);
        defineOS();
        parseTagsMappedToClasses();
        
        fusionActions = new FusionActions(FuserPanel.transformations);
        //fusionActions.registerActions();  
        //entitiesCart = new EntitiesCart();
        mapPairFactory = new MapPairFactory(LearningServlet.getCart(), fusionActions);
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
        throws ServletException, IOException, SQLException {
        
        try (PrintWriter out = response.getWriter()) {
            /*response.setContentType("text/html;charset=UTF-8");
            System.out.println("batch process request");
            System.out.println("batch Servlet Thread: " + Thread.currentThread().getName());

            DBConfig dbConf = (DBConfig)sess.getAttribute("db_conf");
            //grConf = (GraphConfig)sess.getAttribute("gr_conf");

            BatchLearning batchLearning = new BatchLearning(dbConf, LearningServlet.getCart());
       

            String JSONdata = request.getParameter("cluster");
            System.out.println("JSONdata: " + JSONdata);        

            TrainWorker trainWorker = new TrainWorker(entitiesCart);

            PropertyChangeListener propChangeListn = new PropertyChangeListener() {              
                @Override
                public void propertyChange(PropertyChangeEvent pce) {
                    if("DONE".equals(pce.getNewValue().toString())){
                        System.out.println("training is DONE:" + pce.getNewValue());
                        modelExists = true;
                        //entitiesCart.emptyCart();
                    }
                }
            };

            trainWorker.addPropertyChangeListener(propChangeListn);
            try {   
                System.out.println("batch training process started!");
                trainWorker.execute();

            } catch (Exception ex) {
                System.out.println("batch train worker thread error:\n");
                Logger.getLogger(LearningServlet.class.getName()).log(Level.SEVERE, null, ex);
            }


            //ObjectMapper mapper = new ObjectMapper();
            //out.println(mapper.writeValueAsString(recommendations));
            System.out.println("batch process finished.");
            //out.println("Your JSON");
            */
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
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(BatchLearningServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(BatchLearningServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            System.out.println("trying to load OWL classes, path (linux): " + PATH + "/Map");
            initialFile = new File(PATH + "/Map"); 
            
        } 
        else{
            System.out.println("trying to load OWL classes, path (windows): " + PATH + "\\Map");
            initialFile = new File(PATH + "\\Map");
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
         System.out.println("mappings " + mappings);
         System.out.println("mapperWithIDs " + mapperWithIDs);
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

