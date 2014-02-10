package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.sql.SQLException;
import java.util.List;
import javax.swing.SwingWorker;

/**
 * This worker handles application of a transformation against a set of links and initiates the import in virtuoso.
 */

public class FuseWorker extends SwingWorker<Void, Void> {
    
    private final AbstractFusionTransformation transformation;
    private final List<Link> links;
    private final DBConfig dbConfig;    
    private final boolean checkboxIsSelected;
    private final String fusedGraph;
    private final GraphConfig graphConfig;
    /**
     * Construct new fuse worker with the given parameters.
     * @param transformation transformation to use for the fusion
     * @param links list of links to be fused
     * @param dbConfig database configuration
     * @param fusedGraph
     * @param checkboxIsSelected
     * @param graphConfig
     */
    public FuseWorker(final AbstractFusionTransformation transformation, final List<Link> links, final DBConfig dbConfig, final String fusedGraph, final boolean checkboxIsSelected, final GraphConfig graphConfig) {
        super();
        this.transformation = transformation;       
        this.links = links;
        this.dbConfig = dbConfig;
        this.checkboxIsSelected = checkboxIsSelected;
        this.fusedGraph = fusedGraph;
        this.graphConfig = graphConfig;
    }
    
    @Override
    protected Void doInBackground() throws SQLException {
        final GeometryFuser geometryFuser = new GeometryFuser();
        try {
            geometryFuser.connect(dbConfig);
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
            virtImp.importGeometriesToVirtuoso(fusedGraph); 
            virtImp.insertLinksMetadata(links, fusedGraph);    //this method is used here to insert metadata the polygons graph to the new graph
            virtImp.insertMetadataToFusedGraph(links, fusedGraph); //insert from graphA (unister) to the new graph
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
            virtImp.importGeometriesToVirtuoso();
            virtImp.insertLinksMetadata(links); //insert metadata from links to graphA
            
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
    
}