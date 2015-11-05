package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.checkNotNull;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;

/**
 * Keeps info about graph names and given endpoints.
 */
public class GraphConfig {
    
    private static final Logger LOG = Log.getClassFAGILogger(GraphConfig.class);    
    
    private String graphA, graphB, graphL, typeGraphA, typeGraphB, endpointA, endpointB, endpointL, endpointLoc, endpointT;
    private String metadataGraphA, metadataGraphB, targetGraph, targetTempGraph;
    private String allLinksGraph, allClusterGraph, clusterGraph, linksGraph, sampleLinksGraph;
    
    private int depthA;
    private int depthB;
    
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
        this.typeGraphA = "";
        this.typeGraphB = "";

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
        this.typeGraphA = "";
        this.typeGraphB = "";

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
        this.typeGraphA = "";
        this.typeGraphB = "";

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
        this.typeGraphA = "";
        this.typeGraphB = "";

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
        this.typeGraphA = "";
        this.typeGraphB = "";
        
        geoPropertiesA = new ArrayList<>();
        geoPropertiesB = new ArrayList<>();
        geoTypesA = new ArrayList<>();
        geoTypesB = new ArrayList<>();
    }

    public int getDepthA() {
        return depthA;
    }

    public void setDepthA(int depthA) {
        this.depthA = depthA;
    }

    public int getDepthB() {
        return depthB;
    }

    public void setDepthB(int depthB) {
        this.depthB = depthB;
    }

    public String getTypeGraphA() {
        return typeGraphA;
    }

    public void setTypeGraphA(String typeGraphA) {
        this.typeGraphA = typeGraphA;
    }

    public String getTypeGraphB() {
        return typeGraphB;
    }

    public void setTypeGraphB(String typeGraphB) {
        this.typeGraphB = typeGraphB;
    }

    public String getSampleLinksGraph() {
        return sampleLinksGraph;
    }

    public void setSampleLinksGraph(String sampleLinksGraph) {
        this.sampleLinksGraph = sampleLinksGraph;
    }

    public String getAllLinksGraph() {
        return allLinksGraph;
    }

    public void setAllLinksGraph(String allLinksGraph) {
        this.allLinksGraph = allLinksGraph;
    }

    public String getAllClusterGraph() {
        return allClusterGraph;
    }

    public void setAllClusterGraph(String allClusterGraph) {
        this.allClusterGraph = allClusterGraph;
    }

    public String getClusterGraph() {
        return clusterGraph;
    }

    public void setClusterGraph(String clusterGraph) {
        this.clusterGraph = clusterGraph;
    }

    public String getLinksGraph() {
        return linksGraph;
    }

    public void setLinksGraph(String linksGraph) {
        this.linksGraph = linksGraph;
    }

    public String getMetadataGraphA() {
        return metadataGraphA;
    }

    public void setMetadataGraphA(String metadataGraphA) {
        this.metadataGraphA = metadataGraphA;
    }

    public String getMetadataGraphB() {
        return metadataGraphB;
    }

    public void setMetadataGraphB(String metadataGraphB) {
        this.metadataGraphB = metadataGraphB;
    }

    public String getTargetGraph() {
        return targetGraph;
    }

    public void setTargetGraph(String targetGraph) {
        this.targetGraph = targetGraph;
    }

    public String getTargetTempGraph() {
        return targetTempGraph;
    }

    public void setTargetTempGraph(String targetTempGraph) {
        this.targetTempGraph = targetTempGraph;
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
    
    /*
    SELECT ?geo
WHERE {
?s <http://geovocab.org/geometry#geometry> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .
} limit 1
    SELECT distinct(?p) 
WHERE {
?s ?p ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .
}
    */
    private void fillGeoProperties(String g, String s, Set<String> l, Set<String> t) {
        final String geoQuery = "SELECT distinct ?pre ?geo_t \n" +
                          "WHERE { \n" +
                          "GRAPH <"+g+"> {\n" +
                          "  {\n" +
                          "    ?s ?pre ?o . ?o <"+Constants.AS_WKT_REGEX+"> ?geo_t .\n" +
                          "  } } }";
        
        final String wgsQuery = "ASK { ?s <"+Constants.LAT_REGEX+"> ?o1 . ?s <"+Constants.LONG_REGEX+"> ?o2 }";
        
        boolean hasWGS = false;
        
        System.out.println(geoQuery);
        String xmlType = "";
        for (String type : QueryEngineHTTP.supportedSelectContentTypes) {
            if (type.contains("xml")) {
                xmlType = type;
            }
        }
        int tries = 0;
        QueryEngineHTTP qeh = null;
        while (tries < Constants.MAX_SPARQL_TRIES) {
            try {
                final Query query = QueryFactory.create(wgsQuery);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                //queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);

                qeh = QueryExecutionFactory.createServiceRequest(s, query, authenticator);
                qeh.addDefaultGraph(g);
                qeh.setSelectContentType(xmlType);
                hasWGS = qeh.execAsk();

                System.out.println("Has WGS ------- " + hasWGS);

                break;
            } catch (HttpException ex) {
                LOG.trace("HttpException during geometry fetch");
                LOG.debug("HttpException during geometry fetch : " + ex.getMessage());

                tries++;
            } catch (JenaException ex) {
                LOG.trace("JenaException during geometry fetch");
                LOG.debug("JenaException during geometry fetch : " + ex.getMessage());

                tries++;
            } finally {
                if (qeh != null) {
                    qeh.close();
                }
            }
        }

        if ( hasWGS )
            t.add("WGS");
        
        qeh = null;
        
        tries = 0;
        while (tries < Constants.MAX_SPARQL_TRIES) {
            try {
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                qeh = new QueryEngineHTTP(s, geoQuery, authenticator);
                qeh.setSelectContentType(xmlType);
                final ResultSet resultSet = qeh.execSelect();
                while (resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    final String geo = querySolution.get("?pre").toString();
                    String geo_t = querySolution.get("?geo_t").toString();
                    System.out.println("Geo Type : "+geo_t);
                    geo_t = geo_t.substring(0, geo_t.indexOf("("));
                    
                    System.out.println("Geo Type : "+geo_t);
                    if (geo.toLowerCase().contains("geometry")) {
                        l.add(geo);
                        t.add(geo_t);
                    }
                }
                
                break;
            } catch (HttpException ex) {
                LOG.trace("HttpException during geometry fetch");
                LOG.debug("HttpException during geometry fetch : " + ex.getMessage());

                tries++;
            } catch (JenaException ex) {
                LOG.trace("JenaException during geometry fetch");
                LOG.debug("JenaException during geometry fetch : " + ex.getMessage());

                tries++;
            } finally {
                if (qeh != null) {
                    qeh.close();
                }
            }
        }
        qeh.close();
    }
}
