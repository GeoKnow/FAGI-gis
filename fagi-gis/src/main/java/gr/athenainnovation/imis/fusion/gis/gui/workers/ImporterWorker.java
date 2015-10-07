package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.Importer;
import gr.athenainnovation.imis.fusion.gis.gui.ImporterPanel;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.apache.log4j.Logger;

/**
 * Exports triples from a dataset using its SPARQL endpoint and then imports them into a PostGIS database.
 * @author Thomas Maroulis
 */
public class ImporterWorker extends SwingWorker<Void, Void> {    
    private static final Logger LOG = Log.getClassFAGILogger(ImporterPanel.class);
    
    private final ErrorListener errorListener;
    
    private final int datasetIdent;
    private final Dataset sourceDataset;
    private final DBConfig dbConfig;
    private final GraphConfig grConf;
    private final javax.swing.JLabel statusText;
    
    private int metadataProgress = 0;
    private int geometryProgress = 0;
    
    private float elapsedTime = 0f; 
    private int importedTripletsCount = 0;
    
    public int getImportedTripletsCount() {
        return importedTripletsCount;
    }

    public void setImportedTripletsCount(int impTriplets) {
        this.importedTripletsCount = impTriplets;
    }
    
    public float getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(float elapTime) {
        this.elapsedTime = elapTime;
    }

    /**
     * Constructs a new instance of {@link ImporterWorker} that will export triples from a sourceDataset and import them in the indicated DB.
     * Triples will be loaded in the tables of the database that are indicated by the parameter datasetIdent.
     * @param dbConfig database configuration
     * @param grConf graph configuration
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @param textField (DEPRECATED) used to refer to the TexArea for output in the old FAGI version
     * @param errListener
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
    public ImporterWorker(final DBConfig dbConfig, final GraphConfig grConf, final int datasetIdent, final Dataset sourceDataset, javax.swing.JLabel textField, final ErrorListener errListener) {
        super();
        
        this.dbConfig = dbConfig;
        this.grConf = grConf;
        this.datasetIdent = datasetIdent;
        this.sourceDataset = sourceDataset;
        
        this.statusText = textField;
        this.errorListener = errListener;
    }
    
    @Override
    protected Void doInBackground() {
        Importer importer;
        importer = new Importer(dbConfig, this, grConf);
        
        if ( !importer.isInitialized() ) 
            return null;
        
        //importer.importMetadata(datasetIdent, sourceDataset); //we decided not to import the metadata in the DB. 
        //metadata will get imported straight from the virtuoso graph.
        importer.importGeometries(datasetIdent, sourceDataset);
        //System.out.println("importGeometries done");
        setElapsedTime(importer.getElapsedTime());
        setImportedTripletsCount(importer.getImportedTripletsCount());
            
        importer.clean();

        return null;
    }
    
    @Override
    protected void done() {
    // Call get despite return type being Void to prevent SwingWorker from swallowing exceptions
        try {
            get();
            if(statusText != null)
                statusText.setText("Imported "+getImportedTripletsCount()+" triplets in "+getElapsedTime()+" secs!");
            else
                LOG.info(ANSI_YELLOW+"Imported "+getImportedTripletsCount()+" triplets in "+getElapsedTime()+" secs!"+ANSI_RESET);
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
            statusText.setText("Worker terminated abnormally.");
        }
        catch (InterruptedException ex) {
            LOG.warn(ex.getMessage());
            errorListener.notifyError(ex.getMessage());
            statusText.setText("Worker terminated abnormally.");
        }
        catch (ExecutionException ex) {
            LOG.warn(ex.getCause().getMessage());
            errorListener.notifyError(ex.getCause().getMessage());
            statusText.setText("Worker terminated abnormally.");
        }
        finally {
            LOG.info(ANSI_YELLOW+"Dataset A import worker has terminated."+ANSI_RESET);
        }
    }

    /**
     * Notify worker of progress of metadata triples import.
     * @param progress progress of metadata triples import. Value must be in range [0..100]
     */
    public void publishMetadataProgress(final int progress) {
        metadataProgress = progress;
        publishProgress();
    }
    
    /**
     * Notify worker of progress of geometry triples import.
     * @param progress progress of geometry triples import. Value must be in range [0...100]
     */
    public void publishGeometryProgress(final int progress) {
        geometryProgress = progress;
        publishProgress();
    }
    
    private void publishProgress() {
        int globalProgress = (int) ((metadataProgress + geometryProgress) / 1); //divide with 2, when metadata are to be imported in postgis

        // We manually restrict values to a [0,100] range to prevent an exception being thrown in case of erroneous progress value.
        if(globalProgress < 0) {
            globalProgress = 0;
        }
        if(globalProgress > 100) {
            globalProgress = 100;
        }
        
        setProgress(globalProgress);
    }

    public DBConfig getDbConfig() {
        return dbConfig;
    }
    
}
