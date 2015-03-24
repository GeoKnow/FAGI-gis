package gr.athenainnovation.imis.fusion.gis.postgis;

//import com.google.common.base.Optional;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.log4j.Logger;



/**
 * The class provides methods for importing RDF triples to the PostGIS DB.
 * @author Thomas Maroulis
 */
public class PostGISImporter {
    private static final Logger LOG = Logger.getLogger(PostGISImporter.class);
    private final String dbName;
    private final Connection connection;
    
    private static final String DB_URL = "jdbc:postgresql:";
    
    /**
     * Represents dataset A in the db schema ('left' dataset).
     */
    public static final int DATASET_A = 0;
    /**
     * Represents dataset B in the db schema ('right' dataset).
     */
    public static final int DATASET_B = 1;
    
    //private PreparedStatement insertMetadataA;
    //private PreparedStatement insertMetadataB;
    private PreparedStatement insertGeometryA;
    private PreparedStatement insertGeometryB;
    
    /**
     * Constructs a new instance of PostGISImporter and immediately establishes a connection to the specified database.
     * @param dbConfig database configuration object
     * @throws SQLException  
     */
    public PostGISImporter(final DBConfig dbConfig) throws SQLException {        
        dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();
        
        this.connection = connect(dbName, dbUsername, dbPassword);
        prepareStatements();
    }
    
    /**
     * Loads endpoint/graph to the appropriate dataset info table.
     * @param dataset {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param endpoint dataset endpoint URL
     * @param graph dataset graph URI
     * @throws SQLException  
     */
    public void loadInfo(final int dataset, final String endpoint, final String graph) throws SQLException {        
        PreparedStatement insertInfo = null;
        
        try {
            if(dataset == DATASET_A) {
                insertInfo = connection.prepareStatement("INSERT INTO dataset_a_info (endpoint, graph) VALUES (?, ?)");
            }
            else {
                insertInfo = connection.prepareStatement("INSERT INTO dataset_b_info (endpoint, graph) VALUES (?, ?)");
            }
            
            insertInfo.setString(1, endpoint);
            insertInfo.setString(2, graph);
            
            insertInfo.executeUpdate();
            connection.commit();
        }
        catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
        finally {
            if(insertInfo != null) {
                insertInfo.close();
            }
        }
    }
    
    /**
     * Loads an RDF statement to the appropriate dataset metadata table.
     * @param dataset {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param subject statement subject
     * @param predicate statement predicate
     * @param object statement object
     * @param objectLang optional of statement object language or {@link Optional#absent()} if no language tag exists
     * @param objectDatatype optional of statement object datatype or {@link Optional#absent()} if no datatype exists
     * @throws SQLException  
     */
    /*
    public void loadMetadata(final int dataset, final String subject, final String predicate, final String object, final Optional<String> objectLang, final Optional<String> objectDatatype) 
            throws SQLException {
        PreparedStatement insertMetadata;
        
        try {
            if(dataset == DATASET_A) {
                insertMetadata = insertMetadataA;
            }
            else {
                insertMetadata = insertMetadataB;
            }
            
            insertMetadata.setString(1, subject);
            insertMetadata.setString(2, predicate);
            insertMetadata.setString(3, object);
            if(objectLang.isPresent()) {
                
                insertMetadata.setString(4, objectLang.get());
            }
            else {
                insertMetadata.setString(4, "");
            }
            if(objectDatatype.isPresent()) {
                insertMetadata.setString(5, objectDatatype.get());
            }
            else {
                insertMetadata.setString(5, "");
            }
            insertMetadata.executeUpdate();
            connection.commit();
        }
        catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }*/
    
    /**
     * Loads geometric information to the appropriate dataset geometries table.
     * @param dataset {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param subject subject with which the provided geometry is associated
     * @param geometry geometry in WKT serialisation format
     * @throws SQLException  
     */
    public void loadGeometry(final int dataset, final String subject, final String geometry) throws SQLException {       
        PreparedStatement insertGeometry;
        
        try {
            if(dataset == DATASET_A) {
                insertGeometry = insertGeometryA;
            }
            else {
                insertGeometry = insertGeometryB;
            }
            System.out.println(subject+ " with geom "+geometry);
            insertGeometry.setString(1, subject);
            insertGeometry.setString(2, geometry);
            
            //insertGeometry.executeUpdate();
            insertGeometry.addBatch();
            //connection.commit();
        }
        catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }
    
    public void finishUpdates() throws SQLException {
        insertGeometryA.executeBatch();
        insertGeometryB.executeBatch();
        connection.commit();
        System.out.println("geom commited 1");
    }
    /**
     * Releases all database resources and terminates connection to it.
     * @throws SQLException 
     */
    public void clean() throws SQLException {
//        if(insertMetadataA != null) {
//            insertMetadataA.close();
//        }
//        if(insertMetadataB != null) {
//            insertMetadataB.close();
//        }
        if(insertGeometryA != null) {
            insertGeometryA.close();
        }
        if(insertGeometryB != null) {
            insertGeometryB.close();
        }

        if(connection != null) {
            connection.close();
        }
        System.out.println("POSTGISImporter, statements closed");
        LOG.info(ANSI_YELLOW+"Database connection closed."+ANSI_RESET);
    }
    
    // Establish connection to database
    private Connection connect(final String dbName, final String dbUsername, final String dbPassword) throws SQLException {
        final String url = DB_URL.concat(dbName);
        final Connection dbConn = DriverManager.getConnection(url, dbUsername, dbPassword);
        dbConn.setAutoCommit(false);
        LOG.info(ANSI_YELLOW+"Connection to db established."+ANSI_RESET);
        return dbConn;
    }
    
    private void prepareStatements() throws SQLException {
        //final String insertMetadataAString = "INSERT INTO dataset_a_metadata (subject, predicate, object, object_lang, object_datatype) VALUES (?, ?, ?, ?, ?)";
        //final String insertMetadataBString = "INSERT INTO dataset_b_metadata (subject, predicate, object, object_lang, object_datatype) VALUES (?, ?, ?, ?, ?)";
        final String insertGeometryAString = "INSERT INTO dataset_a_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
        final String insertGeometryBString = "INSERT INTO dataset_b_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
        
        //insertMetadataA = connection.prepareStatement(insertMetadataAString);
        //insertMetadataB = connection.prepareStatement(insertMetadataBString);
        insertGeometryA = connection.prepareStatement(insertGeometryAString);
        insertGeometryB = connection.prepareStatement(insertGeometryBString);
    }

    public String getDbName() {
        return dbName;
    }
    
}
