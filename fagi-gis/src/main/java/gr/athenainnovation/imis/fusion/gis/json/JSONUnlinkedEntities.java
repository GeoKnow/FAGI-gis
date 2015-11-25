/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Vitsas
 */
public class JSONUnlinkedEntities {

    List<JSONUnlinkedEntity>        entitiesA;
    List<JSONUnlinkedEntity>        entitiesB;

    JSONRequestResult               result;
    
    public JSONUnlinkedEntities() {
        this.entitiesA = new ArrayList<>();
        this.entitiesB = new ArrayList<>();
    }

    public JSONUnlinkedEntities(List<JSONUnlinkedEntity> entitiesA, List<JSONUnlinkedEntity> entitiesB) {
        this.entitiesA = entitiesA;
        this.entitiesB = entitiesB;
    }

    public List<JSONUnlinkedEntity> getEntitiesA() {
        return entitiesA;
    }

    public void setEntitiesA(List<JSONUnlinkedEntity> entitiesA) {
        this.entitiesA = entitiesA;
    }

    public List<JSONUnlinkedEntity> getEntitiesB() {
        return entitiesB;
    }

    public void setEntitiesB(List<JSONUnlinkedEntity> entitiesB) {
        this.entitiesB = entitiesB;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
    
}
