
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author nick
 */
public class SchemaMatchState {
    public HashMap<String, HashSet<ScoredMatch>> foundA;
    public HashMap<String, HashSet<ScoredMatch>> foundB;
    public String domOntoA;
    public String domOntoB;
    
    public SchemaMatchState(HashMap<String, HashSet<ScoredMatch>> a, HashMap<String, HashSet<ScoredMatch>> b, String domA, String domB) {
        foundA = a;
        foundB = b;
        domOntoA = domA;
        domOntoB = domB;
    }    
}
