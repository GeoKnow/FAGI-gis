
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author nick
 */
public class SchemaMatchState {
    public HashMap<String, HashSet<String>> foundA;
    public HashMap<String, HashSet<String>> foundB;

    public SchemaMatchState(HashMap<String, HashSet<String>> a, HashMap<String, HashSet<String>> b) {
        foundA = a;
        foundB = b;
    }    
}
