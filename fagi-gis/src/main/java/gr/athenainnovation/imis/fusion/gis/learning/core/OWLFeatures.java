
package gr.athenainnovation.imis.fusion.gis.learning.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.bwaldvogel.liblinear.FeatureNode;
import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import gr.athenainnovation.imis.fusion.gis.learning.vectors.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author imis-nkarag
 */

public class OWLFeatures implements Vector {
    private final Map<Boolean, String> featureSelection;
    private int id;
    private final MapPair mapPair;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final int SUM_OF_POINTS = 11;
    private static final int SUM_OF_AREA_FEATURES = 10; 
    private static final int AREA_PERCENTAGE = 19; //to test
    private static final int POINTS_PERCENTAGE = 19; //to test
    private static final int MEAN_PERCENTAGE = 19;
    private static final int VARIANCE_PERCENTAGE = 19;
    private static final int MEANS_AVERAGE = 32; //ok
    private double meansABAverage;
    private double varianceA;
    private double varianceB;


    public OWLFeatures(Map<Boolean,String> featureSelection, MapPair mapPair){
        this.featureSelection = featureSelection;
        this.mapPair = mapPair;
    } 
    
    public OWLFeatures(MapPair mapPair){
        featureSelection = null;
        this.mapPair = mapPair;
    }    
 
    
    @Override
    public void createOWLFeatures() {
        id = 1;
    
    }   

    @Override
    public void createGeometryFeatures() {
        //do nothing
    }


}
