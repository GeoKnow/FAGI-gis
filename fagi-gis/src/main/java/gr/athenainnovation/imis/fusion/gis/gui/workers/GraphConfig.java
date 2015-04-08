package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.checkNotNull;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;

/**
 * Keeps info about graph names and given endpoints.
 */
public class GraphConfig {
    private String graphA, graphB, endpointA, endpointB, endpointLoc, endpointT;
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
            fillGeoProperties(this.graphA, this.endpointA, this.geoPropertiesA, this.geoTypesA);    
        }
        if ( this.geoPropertiesB.isEmpty() ) {
            fillGeoProperties(this.graphB, this.endpointB, this.geoPropertiesB, this.geoTypesB);    
        }
    }
    
    private void fillGeoProperties(String g, String s, List<String> l, List<String> t) {
        String geoQuery = "SELECT distinct ?p bif:GeometryType(?geo) AS ?geo_t\n" +
                          "WHERE\n" +
                          "  {\n" +
                          "    ?s ?p ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n" +
                          "  }";
        System.out.println("Graph "+geoQuery);
        System.out.println("Graph "+g);
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh = new QueryEngineHTTP(s, geoQuery, authenticator);
        qeh.addDefaultGraph(g);
        QueryExecution queryExecution = qeh;
        final ResultSet resultSet = queryExecution.execSelect();
        
        while(resultSet.hasNext()) {
            final QuerySolution querySolution = resultSet.next();
                    
            final String geo = querySolution.getResource("?p").getURI();
            final String geo_t = querySolution.getLiteral("?geo_t").getString();
            System.out.println("Geo Type : "+geo_t);
            if ( geo.toLowerCase().contains("geometry") ) {
                l.add(geo);
                t.add(geo_t);
            }
        }
    }
}
