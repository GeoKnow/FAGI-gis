package gr.athenainnovation.imis.fusion.gis.core;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import static gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter.DATASET_A;
import static gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter.DATASET_B;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 * Provides the infrastructure for the export of metadata and geometric triples from a dataset with a given SPARQL endpoint and 
 * their loading into a PostGIS db using an instance of {@link PostGISImporter}.
 * @author Thomas Maroulis
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
    
    /**
     * This method fetches all triples with a subject matching parameter subjectRegex and a predicate not matching {@link Importer#HAS_GEOMETRY_REGEX}.
     * Those triples are then imported into a PostGIS database using an instance of {@link PostGISImporter}.
     * 
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @throws SQLException 
     * @throws RuntimeException in case of an unrecoverable error. The cause of the error will be encapsulated by the thrown RuntimeException
     */
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
            
            //System.out.println("\nqueryString from Importer\n" + queryString);
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
    }
    
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
        checkArgument(datasetIdent == DATASET_A || datasetIdent == DATASET_B, "Illegal dataset code: " + datasetIdent);
        final String sourceEndpoint = sourceDataset.getEndpoint();
        final String sourceGraph = sourceDataset.getGraph();
        final String subjectRegex = sourceDataset.getSubjectRegex();
        checkIfValidRegexArgument(subjectRegex);
                         
        //check the serialisation of the dataset with a count query. If it doesn t find WKT the serialisation is wgs84        
        
        //check which is the dataset to define triples format

         //test wgs84
        final String restrictionForWgs = "?s ?p1 ?o1 . ?s ?p2 ?o2 FILTER(regex(?s, \"" + subjectRegex + "\", \"i\")) " + "FILTER(regex(?p1, \"" + LAT_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + LONG_REGEX + "\", \"i\"))";
        
        final String queryString1 = "SELECT ?s ?o1 ?o2 WHERE { " + restrictionForWgs + " }";
        int countWgs = checkForSerialisation(sourceEndpoint, sourceGraph, restrictionForWgs);
        //System.out.println("countWgs SUCCESS   " + countWgs);
        //execute wgs84
        //double wgs = objectNode.asLiteral().getDouble();
        //test wgs84  //construct POINT long lat
        
         
        final String restriction = "?s ?p1 _:a . _:a ?p2 ?g FILTER(regex(?s, \"" + subjectRegex + "\", \"i\")) " + "FILTER(regex(?p1, \"" + HAS_GEOMETRY_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + AS_WKT_REGEX + "\", \"i\"))";
        int countWKT = checkForSerialisation(sourceEndpoint, sourceGraph, restriction);
        final String queryString = "SELECT ?s ?g WHERE { " + restriction + " }";

        
        //final int totalCount = queryExpectedResultSetSize(sourceEndpoint, sourceGraph, restriction);
        int currentCount = 1;
        
        QueryExecution queryExecution = null;
        
        if (!(countWKT > 0)){ //if geosparql geometry doesn' t exist        
            
            try {
                final Query query = QueryFactory.create(queryString1);
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);

                final ResultSet resultSet = queryExecution.execSelect();

                while(resultSet.hasNext()) {
                    
                    
                    final QuerySolution querySolution = resultSet.next();

                    final String subject = querySolution.getResource("?s").getURI();
                    final RDFNode objectNode1 = querySolution.get("?o1"); //lat
                    final RDFNode objectNode2 = querySolution.get("?o2"); //long
                    
                    if(objectNode1.isLiteral() && objectNode2.isLiteral()) {
                        final double latitude = objectNode1.asLiteral().getDouble();
                        final double longitude = objectNode2.asLiteral().getDouble();
                        
                        //construct wkt serialization
                        String geometry = "POINT ("+ longitude + " " + latitude +")";
                        //System.out.println("wgs84 from importer  " + geometry);
                        postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    }
                    else {
                        LOG.warn("Resource found where geometry serialisation literal expected.");
                    }

                    callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWgs)));
                    
                    
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
        }
                       
        
        
        else{ //if geosparql geometry exists
            try {
                final Query query = QueryFactory.create(queryString);
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
                
                final ResultSet resultSet = queryExecution.execSelect();
                
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    final String subject = querySolution.getResource("?s").getURI();                    
                    final RDFNode objectNode = querySolution.get("?g");
                    if(objectNode.isLiteral()) {
                        final String geometry = objectNode.asLiteral().getLexicalForm();
                        //System.out.println("geometry objectNode.asLiteral().getLexicalForm():   "+ geometry);
                        
                        postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    }
                    else {
                        LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    
                    callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWKT)));
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
        }
    

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
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
            
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
    
        private int checkForSerialisation(final String sourceEndpoint, final String sourceGraph, final String restriction) {
        int count = -1;
        
        final String queryString = "SELECT (COUNT (?s) as ?count) WHERE { " + restriction + " } LIMIT 10";
        QueryExecution queryExecution = null;
        
        try {
            final Query query = QueryFactory.create(queryString);
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph);
            
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
