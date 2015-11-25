/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.util.HashSet;

/**
 *
 * @author Nick Vitsas
 */
public class JSONLinkProperties {

    String nodeA;
    String nodeB;
    HashSet<String> propsA;
    HashSet<String> propsB;
    HashSet<String> propsLongA;
    HashSet<String> propsLongB;
    HashSet<JSONProperty> propsFullB;
    HashSet<JSONProperty> propsFullA;

    public JSONLinkProperties() {
        propsA = new HashSet<>();
        propsB = new HashSet<>();
        propsLongA = new HashSet<>();
        propsLongB = new HashSet<>();
        propsFullA = new HashSet<>();
        propsFullB = new HashSet<>();
    }

    public String getNodeA() {
        return nodeA;
    }

    public void setNodeA(String nodeA) {
        this.nodeA = nodeA;
    }

    public String getNodeB() {
        return nodeB;
    }

    public void setNodeB(String nodeB) {
        this.nodeB = nodeB;
    }

    public HashSet<String> getPropsA() {
        return propsA;
    }

    public void setPropsA(HashSet<String> propsA) {
        this.propsA = propsA;
    }

    public HashSet<String> getPropsB() {
        return propsB;
    }

    public void setPropsB(HashSet<String> propsB) {
        this.propsB = propsB;
    }

    public HashSet<String> getPropsLongA() {
        return propsLongA;
    }

    public void setPropsLongA(HashSet<String> propsLongA) {
        this.propsLongA = propsLongA;
    }

    public HashSet<String> getPropsLongB() {
        return propsLongB;
    }

    public void setPropsLongB(HashSet<String> propsLongB) {
        this.propsLongB = propsLongB;
    }

    public HashSet<JSONProperty> getPropsFullB() {
        return propsFullB;
    }

    public void setPropsFullB(HashSet<JSONProperty> propsFullB) {
        this.propsFullB = propsFullB;
    }

    public HashSet<JSONProperty> getPropsFullA() {
        return propsFullA;
    }

    public void setPropsFullA(HashSet<JSONProperty> propsFullA) {
        this.propsFullA = propsFullA;
    }

}
