package gr.athenainnovation.imis.fusion.gis.core;

//import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import gr.athenainnovation.imis.fusion.gis.cli.FusionGISCLI;
//import static gr.athenainnovation.imis.fusion.gis.cli.FusionGISCLI.getFAGIState;
//import static gr.athenainnovation.imis.fusion.gis.cli.FusionGISCLI.st;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import static gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter.DATASET_A;
import static gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter.DATASET_B;
import java.sql.SQLException;
import java.util.logging.Level;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;

/**
 * Provides the infrastructure for the export of metadata and geometric triples from a dataset with a given SPARQL endpoint and 
 * their loading into a PostGIS db using an instance of {@link PostGISImporter}.
 */

public class Importer {
    private static final Logger LOG = Logger.getLogger(Importer.class);
    
    private final ImporterWorker callback;
    private final PostGISImporter postGISImporter;
    
    // Regexes to match the predicates of the expected triple path from subject node to its attached WKT geometry serialisation.
    private static final String HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    
    private float elapsedTime = 0f;
    private int importedTripletsCount = 0;
    
    public float getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(float elapTime) {
        this.elapsedTime = elapTime;
    }

    public int getImportedTripletsCount() {
        return importedTripletsCount;
    }

    public void setImportedTripletsCount(int impTriplets) {
        this.importedTripletsCount = impTriplets;
    }
    
    public void addImportedTriplets(int addTriplets) {
        this.importedTripletsCount += addTriplets;
    }
    
    /**
     * Constructs a new instance of the importer with the given {@link PostGISImporter}.
     * @param dBConfig database configuration
     * @param callback callback
     * @throws SQLException 
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
    public Importer(final DBConfig dBConfig, final ImporterWorker callback) throws SQLException {
        this.callback = callback;
        this.postGISImporter = new PostGISImporter(dBConfig);
    }
    
    public Importer(final DBConfig dBConfig) throws SQLException {
        this.callback = null;
        this.postGISImporter = new PostGISImporter(dBConfig);
    }
    
    /**
     * This method fetches all triples with a subject matching parameter subjectRegex and a predicate not matching {@link Importer#HAS_GEOMETRY_REGEX}.
     * Those triples are then imported into a PostGIS database using an instance of {@link PostGISImporter}.
     * 
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @throws SQLException 
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
    /*
    public void importMetadata(final int datasetIdent, final Dataset sourceDataset) throws SQLException {
        checkArgument(datasetIdent == DATASET_A || datasetIdent == DATASET_B, "Illegal dataset code: " + datasetIdent);
        final String sourceEndpoint = sourceDataset.getEndpoint();
        final String sourceGraph = sourceDataset.getGraph();
        final String subjectRegex = sourceDataset.getSubjectRegex();
        checkIfValidRegexArgument(subjectRegex);
        
        final String restriction = "?s ?p ?o FILTER (regex(?s, \"" + subjectRegex + "\", \"i\")) FILTER (!regex(?p, \"" + HAS_GEOMETRY_REGEX +"\", \"i\"))"; //put wkt filter jan16
        final String queryString = "SELECT ?s ?p ?o WHERE { " + restriction + " }";
        
        final int totalCount = queryExpectedResultSetSize(sourceEndpoint, sourceGraph, restriction);
        int currentCount = 1;
        
        QueryExecution queryExecution = null;
        
        try {
            postGISImporter.loadInfo(datasetIdent, sourceEndpoint, sourceGraph);
            
            final Query query = QueryFactory.create(queryString);
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
            
            final ResultSet resultSet = queryExecution.execSelect();
            
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                
                final String subject = querySolution.getResource("?s").toString();
                final String predicate = querySolution.getResource("?p").getURI();
                final String object;
                final Optional<String>  objectLang;
                final Optional<String> objectDatatype;
                final RDFNode objectNode = querySolution.get("?o");
                if(objectNode.isResource()) {
                    if(objectNode.isAnon()) {
                        //Ignore this triple
                    }
                    else {
                        object = objectNode.asResource().getURI();
                        objectLang = Optional.absent();
                        objectDatatype = Optional.absent();
                        postGISImporter.loadMetadata(datasetIdent, subject, predicate, object, objectLang, objectDatatype);
                    }
                }
                else {
                    object = objectNode.asLiteral().getLexicalForm();
                    objectLang = Optional.fromNullable(objectNode.asLiteral().getLanguage());
                    objectDatatype = Optional.fromNullable(objectNode.asLiteral().getDatatypeURI());
                    postGISImporter.loadMetadata(datasetIdent, subject, predicate, object, objectLang, objectDatatype);
                }
                
                callback.publishMetadataProgress((int) (0.5 + (100 * (double) currentCount++ / (double) totalCount)));
            }
        }
        catch (SQLException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
    }*/
    
    /**
     * This method fetches all triples with a subject matching parameter subjectRegex and a two triple chain with a predicate matching {@link Importer#HAS_GEOMETRY_REGEX} in the first triple and
     * {@link Importer#AS_WKT_REGEX} in the second triple.
     * Those triples are then imported into a PostGIS database using an instance of {@link PostGISImporter}.
     * 
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @throws SQLException 
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
    public void importGeometries(final int datasetIdent, final Dataset sourceDataset) throws SQLException {
        //System.out.println("start import geometries");
        checkArgument(datasetIdent == DATASET_A || datasetIdent == DATASET_B, "Illegal dataset code: " + datasetIdent);
        final String sourceEndpoint = sourceDataset.getEndpoint();
        final String sourceGraph = sourceDataset.getGraph();
        final String subjectRegex = sourceDataset.getSubjectRegex();
        checkIfValidRegexArgument(subjectRegex);                  
        //check the serialisation of the dataset with a count query. If it doesn t find WKT the serialisation, the serialisation is wgs84        
        
        //check which is the dataset to define triples format
        //wgs84
        final String restrictionForWgs = "?s ?p1 ?o1 . ?s ?p2 ?o2 FILTER(regex(?s, \"" + subjectRegex + "\", \"i\")) " + "FILTER(regex(?p1, \"" + LAT_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + LONG_REGEX + "\", \"i\"))";
        
        final String queryString1 = "SELECT ?s ?o1 ?o2 WHERE { GRAPH <http://localhost:8890/DAV/all_links_"+postGISImporter.getDbName()+"> {?s ?lp ?os} . GRAPH <"+sourceGraph+"> {" + restrictionForWgs + "} }";
        boolean countWgs = checkForWGS(sourceEndpoint, sourceGraph, restrictionForWgs, "?s");       
        final String restriction = "?os ?p1 _:a . _:a <"+AS_WKT_REGEX+"> ?g ";
        /*final String restriction = "?os ?p1 _:a . _:a ?p2 ?g FILTER(regex(?os, \"" + subjectRegex + "\", \"i\")) " + "" +
                "FILTER(regex(?p2, \"" + AS_WKT_REGEX + "\", \"i\"))";*/
        
        
        boolean countWKT = checkForWKT(sourceEndpoint, sourceGraph, restriction, "?os");
        //int countWKT = 1;
        //int countWgs = 0;
        
        final String queryString;
        if (datasetIdent == DATASET_A)
            queryString = "SELECT ?os ?g WHERE { GRAPH <http://localhost:8890/DAV/all_links_"+postGISImporter.getDbName()+"> {?os ?lp ?s} . GRAPH <"+sourceGraph+"> {" + restriction + " } }";
        else 
            queryString = "SELECT ?os ?g WHERE { GRAPH <http://localhost:8890/DAV/all_links_"+postGISImporter.getDbName()+"> {?s ?lp ?os} . GRAPH <"+sourceGraph+"> {" + restriction + " } }";
        
//System.out.println("Query String "+queryString);
        //System.out.println("Count WKT " + countWKT+" Count WGS "+countWgs);
        int currentCount = 1;
        
        QueryExecution queryExecution = null;
        if (!countWKT){ //if geosparql geometry doesn' t exist        
            try {
                //System.out.println("Query String "+queryString);
                final Query query = QueryFactory.create(queryString1);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, authenticator);
                final ResultSet resultSet = queryExecution.execSelect();
                long startTime =  System.nanoTime();
                while(resultSet.hasNext()) {
                    
                    
                    final QuerySolution querySolution = resultSet.next();
                    
                    final String subject = querySolution.getResource("?s").getURI();
                    //System.out.println("Subject "+subject);
                    final RDFNode objectNode1 = querySolution.get("?o1"); //lat
                    final RDFNode objectNode2 = querySolution.get("?o2"); //long
                    
                    if(objectNode1.isLiteral() && objectNode2.isLiteral()) {
                        final double latitude = objectNode1.asLiteral().getDouble();
                        final double longitude = objectNode2.asLiteral().getDouble();
                        
                        //construct wkt serialization
                        String geometry = "POINT ("+ longitude + " " + latitude +")";
                        postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    }
                    else {
                        LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    
                    //if (callback != null )
                        //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWgs)));                                       
                }
                postGISImporter.finishUpdates();
                //System.out.println("PostGISImporter finishedUpdates");
                long endTime =  System.nanoTime();
                setElapsedTime((endTime-startTime)/1000000000f);
            }
            catch (SQLException | RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
            finally {
                if(queryExecution != null) {
                    queryExecution.close();
                }
            }
        }                                   
        else{ //if geosparql geometry exists
            //System.out.println("geosparql");
            try {
                //System.out.println("Geosparql Query String "+queryString);
                final Query query = QueryFactory.create(queryString);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, authenticator);
                //System.out.println("query: \n" + query + "\nendpoint: " + sourceEndpoint + " sourceGraph: " + sourceGraph);
                final ResultSet resultSet = queryExecution.execSelect();
                long startTime =  System.nanoTime();
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    final String subject = querySolution.getResource("?os").getURI();                 
                    //System.out.println(subject);
                    final RDFNode objectNode = querySolution.get("?g");
                    if(objectNode.isLiteral()) {
                        final String geometry = objectNode.asLiteral().getLexicalForm();
                        //System.out.println("Virtuoso geometry objectNode.asLiteral().getLexicalForm():   "+ geometry);
                        
                        postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    }
                    else {
                        LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    //if (callback != null )
                        //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWKT)));
                }
                postGISImporter.finishUpdates();
                //System.out.println("Count : "+currentCount);
                long endTime =  System.nanoTime();
                setElapsedTime((endTime-startTime)/1000000000f);
            }
            catch (SQLException | RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                java.util.logging.Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
            
                SQLException exception = (SQLException) ex;
            while(exception != null) {
                java.util.logging.Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, exception);
                exception = exception.getNextException();
            }
                //System.out.println("throwing runtime ex");
                throw new RuntimeException(ex);
            }
            finally {
                if(queryExecution != null) {
                    queryExecution.close();
                }
            }
        }    
        addImportedTriplets(1 + 1);
    }
    
    /**
     * Clean-up. Close held resources.
     */
    public void clean() {
        if(postGISImporter != null) {
            try {
                postGISImporter.clean();
            }
            catch (SQLException ex) {
                LOG.warn(ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Execute a SELECT COUNT to get the expected size of the result set for the purposes of calculating a progress percentage.
     * @param sourceEndpoint endpoint URL
     * @param sourceGraph graph URI
     * @param restriction SPARQL restriction to use in the SELECT COUNT query
     * @return number of times SPARQL restriction has been matched in the graph
     */
    private int queryExpectedResultSetSize(final String sourceEndpoint, final String sourceGraph, final String restriction) {
        int count = -1;
        
        final String queryString = "SELECT (COUNT (?s) as ?count) WHERE { " + restriction + " }";
        QueryExecution queryExecution = null;
        
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            
            final ResultSet resultSet = queryExecution.execSelect();
            
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                
                count = querySolution.getLiteral("?count").getInt();
            }
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        
        return count;
    }
    
    private boolean checkForWGS(final String sourceEndpoint, final String sourceGraph, final String restriction, final String sub) {
        boolean result = false;
        /*
        final String queryString = "SELECT (COUNT ("+sub+") as ?count) WHERE { " + restriction + " }";
        System.out.println("Query "+queryString);
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            final ResultSet resultSet = queryExecution.execSelect();
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();                
                count = querySolution.getLiteral("?count").getInt();
            }
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }*/
    //ASK WHERE { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . ?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }
        final String queryString = "ASK WHERE { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . ?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            result = queryExecution.execAsk();
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        return result;
    }
    
    private boolean checkForWKT(final String sourceEndpoint, final String sourceGraph, final String restriction, final String sub) {
        boolean result = false;
        /*
        final String queryString = "SELECT (COUNT ("+sub+") as ?count) WHERE { " + restriction + " }";
        System.out.println("Query "+queryString);
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            final ResultSet resultSet = queryExecution.execSelect();
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();                
                count = querySolution.getLiteral("?count").getInt();
            }
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }*/
        
        final String queryString = "ASK WHERE { ?os ?p1 _:a . _:a <http://www.opengis.net/ont/geosparql#asWKT> ?g }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            result = queryExecution.execAsk();
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        return result;
    }
    
    private int checkForSerialisation(final String sourceEndpoint, final String sourceGraph, final String restriction) {
        int count = -1;
        
        final String queryString = "SELECT (COUNT (?os) as ?count) WHERE { " + restriction + " }";
        //System.out.println("Query "+queryString);
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            final ResultSet resultSet = queryExecution.execSelect();
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();                
                count = querySolution.getLiteral("?count").getInt();
            }
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        
        return count;
    }
    
    private void checkIfValidRegexArgument(final String regex) {
        String localRegex = regex;
        
        while(!localRegex.isEmpty() && localRegex.indexOf('"') != -1) {
            if(localRegex.indexOf('"') == 0 || localRegex.charAt(localRegex.indexOf('"') - 1) != '\'') {
                throw new IllegalArgumentException("Regex cannot contain unescaped double quotes.");
            }
            else {
                localRegex = localRegex.substring(localRegex.indexOf('"'));
            }
        }
    }
}
