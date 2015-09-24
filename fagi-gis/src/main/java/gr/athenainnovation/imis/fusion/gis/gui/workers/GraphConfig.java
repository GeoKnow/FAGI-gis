package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.checkNotNull;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;

/**
 * Keeps info about graph names and given endpoints.
 */
public class GraphConfig {
    private String graphA, graphB, graphL, endpointA, endpointB, endpointL, endpointLoc, endpointT;
    private boolean dominantA;
    
    private List<String> geoPropertiesA;
    private List<String> geoPropertiesB;
    private List<String> geoTypesA;
    private List<String> geoTypesB;
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = "";
        this.endpointT = "";
        this.dominantA = true;
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB, boolean isADominant){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = "";
        this.endpointT = "";
        this.dominantA = isADominant;
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB, String endpointLoc, String endpointT) {
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = endpointLoc;
        this.endpointT = endpointT;
        this.dominantA = true;
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB, String endLoc){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = endLoc;
        this.dominantA = true;
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }

    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB, String endLoc, String endL, String graphL){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = endLoc;
        this.dominantA = true;
        this.graphL = graphL;
        this.endpointL = endL;
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }
    
    public String getGraphL() {
        return graphL;
    }

    public void setGraphL(String graphL) {
        this.graphL = graphL;
    }

    public String getEndpointL() {
        return endpointL;
    }

    public void setEndpointL(String endpointL) {
        this.endpointL = endpointL;
    }

    public String getEndpointLoc() {
        return endpointLoc;
    }

    public void setEndpointLoc(String endpointLoc) {
        this.endpointLoc = endpointLoc;
    }
    
    public String getGraphA() {
        return graphA;
    }
     
    public String getGraphB() {
        return graphB;
    }
    
    public String getEndpointA() {
        return endpointA;
    }
    
    public String getEndpointB() {
        return endpointB;
    }

    public void setGraphA(String graphA) {
        this.graphA = graphA;
    }

    public void setGraphB(String graphB) {
        this.graphB = graphB;
    }

    public void setEndpointA(String endpointA) {
        this.endpointA = endpointA;
    }

    public void setEndpointB(String endpointB) {
        this.endpointB = endpointB;
    }

    public String getEndpointT() {
        return endpointT;
    }

    public void setEndpointT(String endpointT) {
        this.endpointT = endpointT;
    }

    public boolean isDominantA() {
        return dominantA;
    }

    public void setDominantA(boolean dominantA) {
        this.dominantA = dominantA;
    }

    public List<String> getGeoPropertiesA() {
        return geoPropertiesA;
    }

    public void setGeoPropertiesA(List<String> geoPropertiesA) {
        this.geoPropertiesA = geoPropertiesA;
    }

    public List<String> getGeoPropertiesB() {
        return geoPropertiesB;
    }

    public void setGeoPropertiesB(List<String> geoPropertiesB) {
        this.geoPropertiesB = geoPropertiesB;
    }

    public List<String> getGeoTypesA() {
        return geoTypesA;
    }

    public void setGeoTypesA(List<String> geoTypesA) {
        this.geoTypesA = geoTypesA;
    }

    public List<String> getGeoTypesB() {
        return geoTypesB;
    }

    public void setGeoTypesB(List<String> geoTypesB) {
        this.geoTypesB = geoTypesB;
    }
    
    public void scanGeoProperties() {
        if ( this.geoPropertiesA.isEmpty() ) {
            Set<String> sT = new HashSet<>();
            Set<String> sP = new HashSet<>();
            fillGeoProperties(this.graphA, this.endpointA, sP, sT);    
            //fillGeoProperties(this.graphA, this.endpointA, this.geoPropertiesA, this.geoTypesA);   
            this.geoPropertiesA = new ArrayList<>(sP);
            this.geoTypesA = new ArrayList<>(sT);
        }
        if ( this.geoPropertiesB.isEmpty() ) {
            Set<String> sT = new HashSet<>();
            Set<String> sP = new HashSet<>();
            fillGeoProperties(this.graphB, this.endpointB, sP, sT);    
            //fillGeoProperties(this.graphB, this.endpointB, this.geoPropertiesB, this.geoTypesB);    
            this.geoPropertiesB = new ArrayList<>(sP);
            this.geoTypesB = new ArrayList<>(sT);
        }
        System.out.println("Geo Props A " + geoPropertiesA);
        System.out.println("Geo Types A " + geoTypesA);
        System.out.println("Geo Props B " + geoPropertiesB);
        System.out.println("Geo Types B " + geoTypesB);
    }
    
    private void fillGeoProperties(String g, String s, Set<String> l, Set<String> t) {
        String geoQuery = "SELECT ?p\n" +
                          "WHERE { \n" +
                          "GRAPH <"+g+"> {\n" +
                          "  {\n" +
                          "    ?s ?p ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n" +
                          "  } } }";
        System.out.println("Graph "+geoQuery);
        System.out.println("Graph "+g);
        System.out.println("Graph "+s);
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh = new QueryEngineHTTP(s, QueryFactory.create(geoQuery), authenticator);
        System.out.println("Requesting " + QueryEngineHTTP.supportedSelectContentTypes[3]);
        System.out.println("Requesting " + QueryEngineHTTP.supportedSelectContentTypes[0]);
        System.out.println("Requesting " + QueryEngineHTTP.supportedSelectContentTypes[1]);
        qeh.setSelectContentType(QueryEngineHTTP.supportedSelectContentTypes[3]);
        //qeh.addDefaultGraph(g);
        //QueryExecution queryExecution = qeh;
        final ResultSet resultSet = qeh.execSelect();
        /*
        "WHERE\n" +
        */
        while(resultSet.hasNext()) {
            final QuerySolution querySolution = resultSet.next();
            System.out.println(null == querySolution);
            System.out.println(querySolution.varNames().next());
            for (Iterator<String> flavoursIter = querySolution.varNames(); flavoursIter.hasNext();){
                System.out.println(flavoursIter.next());
            }
            System.out.println(null == querySolution.get("?pre"));
            System.out.println(null == querySolution.get("?p"));
            System.out.println(null == querySolution.get("?geo_t"));
            if ( null == querySolution.get("?p") ) continue;
            final String geo = querySolution.get("?pre").toString();
            final String geo_t = querySolution.get("?geo_t").toString();
            //System.out.println("Geo Type : "+geo_t);
            if ( geo.toLowerCase().contains("geometry") ) {
                l.add(geo);
                t.add(geo_t);
            }
        }
        
        qeh.close();
    }
}
