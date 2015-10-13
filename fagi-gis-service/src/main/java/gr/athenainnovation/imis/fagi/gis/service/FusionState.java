
package gr.athenainnovation.imis.fagi.gis.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Vitsas
 */
public class FusionState implements Serializable {
    public List<String> objsA;
    public List<String> predsA;
    public List<String> preds;
    public List<String> predsB;
    public List<String> objsB;
    public List<String> actions;
    
    FusionState() {
        //FusionState implements Serializable and
        //changed google.commons newArrayList to java ArrayLists, for tomcat to be able to serialize class/attributes 
        preds = new ArrayList<>();
        predsA = new ArrayList<>();
        predsB = new ArrayList<>();
        objsA = new ArrayList<>();
        objsB = new ArrayList<>();
        actions = new ArrayList<>();
    }
}
