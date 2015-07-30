
package gr.athenainnovation.imis.fusion.gis.learning.container;

import com.vividsolutions.jts.geom.Geometry;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import gr.athenainnovation.imis.fusion.gis.learning.core.MapPairJsonDeserializer;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * Class containing information about the map objects.
 * 
 * @author imis-nkarag
 */

@JsonDeserialize(using = MapPairJsonDeserializer.class)
public class MapPair {
    
    private final ArrayList<FeatureNode> featureNodeList = new ArrayList<>();
    private final List<FeatureNode> featureNodeListA = new ArrayList<>();
    private final List<FeatureNode> featureNodeListB = new ArrayList<>();
    //private Feature[] instance = { new FeatureNode(1, 4), new FeatureNode(2, 2) };
    
    //private Set<Integer> classIDs;
    private Geometry geometryA;
    private Geometry geometryB;
    private int classID; //fusion action
    private String owlClassA;
    private String owlClassB;    
    private String fusionAction;
    private boolean isUnclassified;
    

    public void addFeature(FeatureNode featureNode){
        this.featureNodeList.add(featureNode);
    }
    
    public void setClassID(int id){
        this.classID = id;
    }
   
    public int getClassID(){
        return classID;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<FeatureNode> getFeatureNodeListA(){
        return featureNodeListA;
    }    
    
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<FeatureNode> getFeatureNodeListB(){
        return featureNodeListB;
    }     
    
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<FeatureNode> getFeatureNodeList(){
        return featureNodeList;
    }    
    
    public void setGeometryA(Geometry geometryA){
        this.geometryA = geometryA;
    }
    
    public void setGeometryB(Geometry geometryB){
        this.geometryB = geometryB;
    }
    
    public Geometry getGeometryA(){
        return geometryA;
    } 
    
    public Geometry getGeometryB(){
        return geometryB;
    }           
       
    public Feature[] getFinalFeatures(){
        Feature[] instance = new Feature[featureNodeList.size()];
        for(int i=0; i < featureNodeList.size(); i++){
            instance[i] = featureNodeList.get(i);
        }
        return instance;
    }

    public Feature[] getFinalFeaturesA(){
        Feature[] instance = new Feature[featureNodeListA.size()];
        for(int i=0; i < featureNodeListA.size(); i++){
            instance[i] = featureNodeListA.get(i);
        }
        return instance;
    }   

    public Feature[] getFinalFeaturesB(){
        Feature[] instance = new Feature[featureNodeListB.size()];
        for(int i=0; i < featureNodeListB.size(); i++){
            instance[i] = featureNodeListB.get(i);
        }
        return instance;
    }
    
    @JsonValue
    public void setOWLClassA(String owlClassA) {
        this.owlClassA = owlClassA;
    }
    
    @JsonValue
    public String getOWLClassA() {
        return owlClassA;
    }
    
    @JsonValue
    public void setOWLClassB(String owlClassB) {
        this.owlClassB = owlClassB;
    }
    
    @JsonValue
    public String getOWLClassB() {
        return owlClassB;
    } 
    
    @JsonValue
    public void setFusionAction(String fusionAction) {
        this.fusionAction = fusionAction;
    }
    
    @JsonValue
    public String getFusionAction() {
        return fusionAction;
    }
    
    protected void setUnclassified(boolean isUnclassified){
        this.isUnclassified = isUnclassified;
    }
    
    public boolean isUnclassified(){
        return isUnclassified;
    }
}
