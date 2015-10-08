/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * Property Matching State
 * @author nick
 */
public class JSONMatches {

    HashMap<String, HashSet<ScoredMatch>>       foundA;
    HashMap<String, HashSet<ScoredMatch>>       foundB;
    HashSet<String>                             otherPropertiesA;
    HashSet<String>                             otherPropertiesB;
    List<String>                                geomTransforms;
    List<String>                                metaTransforms;
    JSONRequestResult                           result;
    
    public JSONMatches() {
    }

    public HashMap<String, HashSet<ScoredMatch>> getFoundA() {
        return foundA;
    }

    public void setFoundA(HashMap<String, HashSet<ScoredMatch>> foundA) {
        this.foundA = foundA;
    }

    public HashMap<String, HashSet<ScoredMatch>> getFoundB() {
        return foundB;
    }

    public void setFoundB(HashMap<String, HashSet<ScoredMatch>> foundB) {
        this.foundB = foundB;
    }

    public HashSet<String> getOtherPropertiesA() {
        return otherPropertiesA;
    }

    public void setOtherPropertiesA(HashSet<String> otherPropertiesA) {
        this.otherPropertiesA = otherPropertiesA;
    }

    public HashSet<String> getOtherPropertiesB() {
        return otherPropertiesB;
    }

    public void setOtherPropertiesB(HashSet<String> otherPropertiesB) {
        this.otherPropertiesB = otherPropertiesB;
    }

    public List<String> getGeomTransforms() {
        return geomTransforms;
    }

    public void setGeomTransforms(List<String> geomTransforms) {
        this.geomTransforms = geomTransforms;
    }

    public List<String> getMetaTransforms() {
        return metaTransforms;
    }

    public void setMetaTransforms(List<String> metaTransforms) {
        this.metaTransforms = metaTransforms;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }

}
