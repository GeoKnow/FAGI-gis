/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Nick Vitsas
 */
public class JSONLinkMatches {

    HashMap<String, HashSet<ScoredMatch>> foundA;
    HashMap<String, HashSet<ScoredMatch>> foundB;

    public JSONLinkMatches() {
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

}
