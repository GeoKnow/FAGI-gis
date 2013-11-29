package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation;
import java.sql.SQLException;
import java.util.List;
import javax.swing.SwingWorker;

/**
 * This worker handles application of a transformation against a set of links.
 * @author Thomas Maroulis
 */
public class FuseWorker extends SwingWorker<Void, Void> {
    
    private final AbstractFusionTransformation transformation;
    private final List<Link> links;
    private final DBConfig dbConfig;
    
    /**
     * Construct new fuse worker with the given parameters.
     * @param transformation transformation to use for the fusion
     * @param links list of links to be fused
     * @param dbConfig database configuration
     */
    public FuseWorker(final AbstractFusionTransformation transformation, final List<Link> links, final DBConfig dbConfig) {
        super();
        this.transformation = transformation;
        this.links = links;
        this.dbConfig = dbConfig;
    }
    
    @Override
    protected Void doInBackground() {
        final GeometryFuser geometryFuser = new GeometryFuser();
        try {
            geometryFuser.connect(dbConfig);
            geometryFuser.fuse(transformation, links);
            return null;
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            geometryFuser.clean();
        }
    }    
}