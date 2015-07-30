package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;

/**
 * This worker handles scoring a transformation against a set of links.
 * @author Thomas Maroulis
 */
public class ScoreWorker extends SwingWorker<Map<String, Double>, Void> {
    
    private final AbstractFusionTransformation rule;
    private final List<Link> links;
    private final DBConfig dbConfig;
    private final Double threshold;
    
    /**
     * Construct new score worker with the given parameters.
     * @param rule rule to use for the fusion
     * @param links list of links to be fused
     * @param dbConfig database configuration
     * @param threshold threshold
     */
    public ScoreWorker(final AbstractFusionTransformation rule, final List<Link> links, final DBConfig dbConfig, final Double threshold) {
        super();
        this.rule = rule;
        this.links = links;
        this.dbConfig = dbConfig;
        this.threshold = threshold;
    }
    
    @Override
    protected Map<String, Double> doInBackground() {
        final GeometryFuser geometryFuser = new GeometryFuser();
        try {
            geometryFuser.connect(dbConfig);
            return geometryFuser.score(rule, links, threshold);
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            geometryFuser.clean();
        }
    }    
}