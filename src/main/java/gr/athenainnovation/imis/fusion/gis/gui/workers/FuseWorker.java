package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.apache.log4j.Logger;

/**
 * This worker handles application of a transformation against a set of links and initiates the import in virtuoso.
 */

public class FuseWorker extends SwingWorker<Void, Void> {
    private static final Logger LOG = Logger.getLogger(FuserPanel.class);
    
    private final AbstractFusionTransformation transformation;
    private final List<Link> links;
    private final DBConfig dbConfig;    
    private final boolean checkboxIsSelected;
    private final String fusedGraph;
    private final GraphConfig graphConfig;
    
    private final JLabel statusField;
    private final FuserPanel parentPanel;
    private final ErrorListener errorListener;
            
    private float elapsedTime = 0f; 
    private int importedTripletsCount = 0;

    
    public FuseWorker(AbstractFusionTransformation transformation, List<Link> selectedLinks, DBConfig dbConfig, String fusedGraph, boolean selected, GraphConfig graphConfig, FuserPanel pan, JLabel stField, ErrorListener errListener) {
        super();
        this.transformation = transformation;       
        this.links = selectedLinks;
        this.dbConfig = dbConfig;
        this.checkboxIsSelected = selected;
        this.fusedGraph = fusedGraph;
        this.graphConfig = graphConfig;  
        statusField = stField;
        parentPanel = pan;
        errorListener = errListener;
    }

    public void setElapsedTime(float elapTime) {
        this.elapsedTime = elapTime;
    }

    public float getElapsedTime() {
        return elapsedTime;
    }  
    
    @Override
    protected Void doInBackground() throws SQLException, IOException {
        final GeometryFuser geometryFuser = new GeometryFuser();
        long starttime, endtime;
        try {
            geometryFuser.connect(dbConfig);
            geometryFuser.loadLinks(links);
            geometryFuser.fuse(transformation, links);

        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            geometryFuser.clean();
        } 
        //virtuoso import        
        VirtuosoImporter virtImp = new VirtuosoImporter(dbConfig, transformation.getID(),fusedGraph, checkboxIsSelected, graphConfig);
        if (checkboxIsSelected) {
            try{
                starttime = System.nanoTime();
                virtImp.importGeometriesToVirtuoso(fusedGraph); 
                virtImp.insertLinksMetadataChains(links, fusedGraph);
                //virtImp.insertLinksMetadata(links, fusedGraph);    //this method is used here to insert metadata the polygons graph to the new graph
                //virtImp.insertMetadataToFusedGraph(links, fusedGraph); //insert from graphA (unister) to the new graph
                
                virtImp.trh.finish();
                //LOG.info("Bulk Insert lasted "+(endtime-starttime)/1000000000f);
             
                //virtImp.set.getConnection().commit();
                
                endtime =  System.nanoTime();
                setElapsedTime((endtime-starttime)/1000000000f);
                LOG.info(ANSI_YELLOW+"Time spent fusing "+getElapsedTime()+""+ANSI_RESET);
                
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                virtImp.clean();
            }
        }
        else {
            try{
                starttime = System.nanoTime();
                virtImp.importGeometriesToVirtuoso();
                virtImp.insertLinksMetadata(links); //insert metadata from links to graphA
                
                virtImp.trh.finish();
                //LOG.info("Bulk Insert lasted "+(endtime-starttime)/1000000000f);
             
                //virtImp.set.getConnection().commit();
                
                endtime =  System.nanoTime();
                setElapsedTime((endtime-starttime)/1000000000f);
                LOG.info(ANSI_YELLOW+"Time spent fusing "+getElapsedTime()+ANSI_RESET);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                virtImp.clean();
            }          
            
        }        
        return null;
    }  
    
    @Override protected void done() {
        // Call get despite return type being Void to prevent SwingWorker from swallowing exceptions
        try {
            get();
            if(statusField!=null)
                statusField.setText("Done (fusing) with transformation: " + transformation.getID()+ " in "+this.getElapsedTime()+" secs!!");
        }
        catch (RuntimeException ex) {
            if(ex.getCause() == null) {
                LOG.warn(ex.getMessage(), ex);
                errorListener.notifyError(ex.getMessage());
            }
            else {
                LOG.warn(ex.getCause().getMessage(), ex.getCause());
                errorListener.notifyError(ex.getCause().getMessage());
            }
            if(statusField!=null)
                statusField.setText("Worker terminated abnormally.");
        }
        catch (InterruptedException ex) {
            LOG.warn(ex.getMessage(), ex);
            errorListener.notifyError(ex.getMessage());
            if(statusField!=null)
                statusField.setText("Worker terminated abnormally.");
        }
        catch (ExecutionException ex) {
            LOG.warn(ex.getCause().getMessage(), ex.getCause());
            errorListener.notifyError(ex.getMessage());
            if(statusField!=null)
                statusField.setText("Worker terminated abnormally.");
        }
        finally {
            if(parentPanel != null)
                parentPanel.setBusy(false);
        }
                    
        LOG.info(ANSI_YELLOW+"Fuse worker has terminated."+ANSI_RESET);
    }
 
    
}