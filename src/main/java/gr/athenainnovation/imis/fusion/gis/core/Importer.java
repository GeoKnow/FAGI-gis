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
        
        final String restriction = "?s ?p ?o FILTER (regex(?s, \"" + subjectRegex + "\", \"i\")) FILTER (!regex(?p, \"" + HAS_GEOMETRY_REGEX +"\", \"i\"))";
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
                
                final String subject = querySolution.getResource("?s").getURI();
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
        
        final String restriction = "?s ?p1 _:a . _:a ?p2 ?g FILTER(regex(?s, \"" + subjectRegex + "\", \"i\")) " + "FILTER(regex(?p1, \"" + HAS_GEOMETRY_REGEX + "\", \"i\"))" +
                "FILTER(regex(?p2, \"" + AS_WKT_REGEX + "\", \"i\"))";
        final String queryString = "SELECT ?s ?g WHERE { " + restriction + " }";
        
        final int totalCount = queryExpectedResultSetSize(sourceEndpoint, sourceGraph, restriction);
        int currentCount = 1;
        
        QueryExecution queryExecution = null;
        
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
                    postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                }
                else {
                    LOG.warn("Resource found where geometry serialisation literal expected.");
                }
                
                callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) totalCount)));
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
