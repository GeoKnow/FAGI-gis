
package gr.athenainnovation.imis.fusion.gis.learning.vectors;

import gr.athenainnovation.imis.fusion.gis.learning.container.MapPair;
import java.util.Map;

/**
 *
 * @author imis-nkarag
 */

public class VectorFactory {
    
    public Vector getVector(String featureType, Map<Boolean,String> featureSelection, MapPair mapPair){
        if(featureType.equals("double")){
            return new DoubleVector(featureSelection);
        }
        else {
            return new BooleanVector(featureSelection, mapPair);
        }
    }
}
