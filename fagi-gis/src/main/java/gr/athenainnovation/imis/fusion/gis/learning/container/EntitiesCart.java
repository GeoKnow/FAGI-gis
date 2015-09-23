
package gr.athenainnovation.imis.fusion.gis.learning.container;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author imis-nkarag
 */
 
public class EntitiesCart {
    
    private static final int MAX_CAPACITY = 1000;
    private static final int clearAtSize = 1010; //buffer slack
    private boolean isFull;
    private final List<MapPair> entitiesList = new ArrayList<>(); 
    
    public boolean isFull(){
        return isFull;
    }
    
    public boolean isEmpty(){
        if(entitiesList.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
    public void emptyCart(){
        isFull = false;
        if(entitiesList.size() > clearAtSize){
            entitiesList.clear();
        }        
    }
    
    public void addEntity(MapPair mapPair){

        entitiesList.add(mapPair);
        if(entitiesList.size() > MAX_CAPACITY){
            isFull = true;
        }
        
    }
    
    public List<MapPair> getMapEntities(){
        return entitiesList;
    }
    
    public MapPair getLastMapPair(){
        if(entitiesList.isEmpty()){
            throw new RuntimeException("Entities Cart is currently empty.");
        }
        return entitiesList.get(entitiesList.size()-1);
    }
    
    public int getSize(){
        return entitiesList.size();
    }
}
