
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Nick Vitsas
 */
public class SchemaMatchState {
    public HashMap<String, HashSet<ScoredMatch>> foundA;
    public HashMap<String, HashSet<ScoredMatch>> foundB;
    public String domOntoA;
    public String domOntoB;
    public HashSet<String> otherPropertiesA;
    public HashSet<String> otherPropertiesB;
    public List<String> geomTransforms;
    public List<String> metaTransforms;
    
    public SchemaMatchState(HashMap<String,
                            HashSet<ScoredMatch>> a,
                            HashMap<String, HashSet<ScoredMatch>> b,
                            String domA, String domB,
                            HashSet<String> opsA, HashSet<String> opsB) {
        foundA = a;
        foundB = b;
        domOntoA = domA;
        domOntoB = domB;
        otherPropertiesA = opsA;
        otherPropertiesB = opsB;
        geomTransforms = new ArrayList<>();
        metaTransforms = new ArrayList<>();
        
        for (Map.Entry<String, AbstractFusionTransformation> entry : FuserPanel.transformations .entrySet())
            {
                System.out.println("Transformation "+entry.getKey());
                geomTransforms.add(entry.getKey());
            }
            
            for (Map.Entry<String, String> entry : FuserPanel.meta_transformations .entrySet())
            {
                System.out.println("Transformation "+entry.getKey());
                metaTransforms.add(entry.getKey());
            }
    }   
    
    public List<String> getPropertyList(String d) {
        List<String> lst = new ArrayList<>();
        
        if ( d.compareTo("A") == 0 ) {
            for ( String s : otherPropertiesA ) {
                lst.add(s);
            }
            for ( String s : foundA.keySet() ) {
                lst.add(s);
            }
        } else {
        
            for (String s : otherPropertiesB) {
                lst.add(s);
            }

            for (String s : foundB.keySet()) {
                lst.add(s);
            }

        }
        
        return lst;
    }
    
}
