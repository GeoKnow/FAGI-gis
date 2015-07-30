
package gr.athenainnovation.imis.fusion.gis.learning.container;

import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import java.util.Map;

/**
 *
 * @author imis-nkarag
 */

public class FusionActions {

    private final Map<String, AbstractFusionTransformation> transformations;
    //private final double[] GROUPS_ARRAY;
    
    public FusionActions(Map<String, AbstractFusionTransformation> transformations) {    
        System.out.println("init fusion actions..");
        this.transformations = transformations;
        //GROUPS_ARRAY = new double[transformations.size()];
    }   
    
    
    public void registerActions() {
        for(Map.Entry<String, AbstractFusionTransformation> entry : transformations.entrySet()){
            entry.getValue().getIntegerID();
        }        
    }   

    
    public int getFusionActionsSize(){
        return transformations.size();
    }
    
    public Map<String, AbstractFusionTransformation> getActions(){
        return transformations;
    }
    
//    public double[] getGroupsArray(){
//        return GROUPS_ARRAY;
//    }

}
