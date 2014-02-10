package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.Importer;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import java.sql.SQLException;
import javax.swing.SwingWorker;

/**
 * Exports triples from a dataset using its SPARQL endpoint and then imports them into a PostGIS database.
 * @author Thomas Maroulis
 */
public class ImporterWorker extends SwingWorker<Void, Void> {    
    private final int datasetIdent;
    private final Dataset sourceDataset;
    private final DBConfig dbConfig;
    
    private int metadataProgress = 0;
    private int geometryProgress = 0;
    
    /**
     * Constructs a new instance of {@link ImporterWorker} that will export triples from a sourceDataset and import them in the indicated DB.
     * Triples will be loaded in the tables of the database that are indicated by the parameter datasetIdent.
     * @param dbConfig database configuration
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
    public ImporterWorker(final DBConfig dbConfig, final int datasetIdent, final Dataset sourceDataset) {
        super();
        
        this.dbConfig = dbConfig;
        this.datasetIdent = datasetIdent;
        this.sourceDataset = sourceDataset;
    }
    
    @Override
    protected Void doInBackground() {
        Importer importer = null;
        try {
            importer = new Importer(dbConfig, this);
            //importer.importMetadata(datasetIdent, sourceDataset); //we decided not to import the metadata in the DB. 
                                                                    //metadata will get imported straight from the virtuoso graph.
            importer.importGeometries(datasetIdent, sourceDataset);
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            if(importer != null) {
                importer.clean();
            }
        }
        
        return null;
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
}
