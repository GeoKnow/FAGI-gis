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
public class JSONFusion {

    // Classes on geometry types
    List<String>                geomsA;
    List<String>                geomsB;
    List<String>                classesA;
    List<String>                classesB;
    
    // List of JSON properties that where matched
    List<JSONPropertyMatch>     properties;
    
    // List of available FAGI-gis trandformation for Geometries and Metadata
    List<String>                geomTransforms;
    List<String>                metaTransforms;

    JSONRequestResult           result;
    
    public JSONFusion() {
        this.geomsA = new ArrayList<>();
        this.geomsB = new ArrayList<>();
        this.classesA = new ArrayList<>();
        this.classesB = new ArrayList<>();
        this.properties = new ArrayList<>();
        this.geomTransforms = new ArrayList<>();
        this.metaTransforms = new ArrayList<>();
    }

    public List<JSONPropertyMatch> getProperties() {
        return properties;
    }

    public void setProperties(List<JSONPropertyMatch> properties) {
        this.properties = properties;
    }

    public List<String> getGeomsA() {
        return geomsA;
    }

    public void setGeomsA(List<String> geomsA) {
        this.geomsA = geomsA;
    }

    public List<String> getGeomsB() {
        return geomsB;
    }

    public void setGeomsB(List<String> geomsB) {
        this.geomsB = geomsB;
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

    public List<String> getClassesA() {
        return classesA;
    }

    public void setClassesA(List<String> classesA) {
        this.classesA = classesA;
    }

    public List<String> getClassesB() {
        return classesB;
    }

    public void setClassesB(List<String> classesB) {
        this.classesB = classesB;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }

}
