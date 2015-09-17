

package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.data.relationship.AsymmetricRelationship;
import net.didion.jwnl.data.relationship.Relationship;
import net.didion.jwnl.data.relationship.RelationshipFinder;
import net.didion.jwnl.data.relationship.RelationshipList;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 * Forms appropriate triples from the PostGIS database and inserts them in the virtuoso specified graph.
 * 
 */


public final class VirtuosoImporter {
    
    private static final Logger LOG = Logger.getLogger(VirtuosoImporter.class);
    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";    
    private String links_graph = "http://localhost:8890/DAV/links";
    private static final String del_wgs_graph = "http://localhost:8890/DAV/del_wgs";
    private static final String del_geom_graph = "http://localhost:8890/DAV/del_geom";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";    
    private static final String PATH_TO_WORDNET_LINUX = "/usr/share/wordnet";
    private static final String PATH_TO_WORDNET_OS_X = "/usr/local/WordNet-3.0/dict";
    private static final String PATH_TO_WORDNET_WINDOWS = "C:\\Program Files (x86)\\WordNet\\2.1\\dict";
    private static final int BATCH_SIZE = 10000;
    private static final int SAMPLE_SIZE = 5;   
    private final String graphB; 
    private final String graphA;
    private String bulkInsertDir;    
    public final TripleHandler trh;   
    //private final Connection connection; // Connection to PostGIS
    private final Connection virt_conn; // Connection to Virtuoso   
    private HashMap< String, MetadataChain > propertiesA;
    private HashMap< String, MetadataChain > propertiesB;  
    private String transformationID;
    private final String endpointA;
    private final String endpointB;
    private final String endpointLoc;
    private final String endpointT;
    private final String targetGraph;   
    public final VirtGraph set;
    public VirtGraph set2;
    //private final VirtGraph setA;     
    //private final VirtGraph setB;     
    public DBConfig db_c;
    public GraphConfig gr_c;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private int wordnetDepth;
    private int maxParentDepth;
    private double raiseToPower;
    private double wordWeight;
    private double textWeight;
    private double typeWeight;
    private double simThreshold;
           
    public VirtuosoImporter(final DBConfig dbConfig, String transformationID, final String fusedGraph, final boolean checkboxIsSelected, final GraphConfig graphConfig) throws SQLException, IOException, JWNLException {
        db_c = dbConfig;
        gr_c = graphConfig;
        dbName = dbConfig.getDBName();
        dbUsername = dbConfig.getDBUsername();
        dbPassword = dbConfig.getDBPassword();
        this.transformationID = transformationID;       
        propertiesA = Maps.newHashMap();
        propertiesB = Maps.newHashMap();      
        graphA = graphConfig.getGraphA();
        graphB = graphConfig.getGraphB();
        targetGraph = fusedGraph;
        endpointA = graphConfig.getEndpointA();
        endpointB = graphConfig.getEndpointB();
        endpointLoc = graphConfig.getEndpointLoc(); 
        endpointT = graphConfig.getEndpointT();
        //long startTime = System.nanoTime();
        set = getVirtuosoSet(fusedGraph, dbConfig.getDBURL(), dbConfig.getUsername(), dbConfig.getPassword());
        //long endTime = System.nanoTime();
        //System.out.println("Time to connect : "+(endTime-startTime)/1000000000f);       
        virt_conn = set.getConnection();        
        bulkInsertDir = dbConfig.getBulkDir();   
        /*
        int lastSlash = bulkInsertDir.lastIndexOf(File.separator);
        if (lastSlash != (bulkInsertDir.length() - 1 ) ) {
            System.out.println("Isxuei");
            bulkInsertDir = bulkInsertDir.concat(File.separator);
        }
        File file = new File(bulkInsertDir+"bulk_inserts");
        if (!file.exists()) {
    System.out.println("creating directory: " + bulkInsertDir+"bulk_inserts");
    boolean result = false;

    try{
        file.mkdir();
        result = true;
     } catch(SecurityException se){
        //handle it
     }        
     if(result) {    
       System.out.println("DIR created");  
     }
  }
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);
*/
        //trh = new FileBulkLoader(fusedGraph, dbConfig, graphConfig, set);
        
        try {
            Scanner scn = null;
            if ( SystemUtils.IS_OS_MAC_OSX ) {
                scn = new Scanner(new File("/home/nick/Projects/fagi-gis.ini"));
            }
            if ( SystemUtils.IS_OS_LINUX ) {
                scn = new Scanner(new File("/home/nick/Projects/fagi-gis.ini"));
            }
            if ( SystemUtils.IS_OS_WINDOWS ) {
                scn = new Scanner(new File("/home/nick/Projects/fagi-gis.ini"));
            }
            while( scn.hasNext() ) {
                String prop = scn.next();
                String[] vals = prop.split(":");
                vals[1].trim();
                vals[0].trim();
                System.out.println(vals[0] + " "+ vals[1]);
                if ( vals[0].equals("wordnet-depth") ) {
                    wordnetDepth = Integer.parseInt(vals[1]);
                } else if ( vals[0].equals("max-parent") ) {
                    maxParentDepth = Integer.parseInt(vals[1]);
                } else if ( vals[0].equals("raise-to-power") ) {
                    raiseToPower = Integer.parseInt(vals[1]);
                } else if ( vals[0].equals("text-match-weight") ) {
                    textWeight = Double.parseDouble(vals[1]);
                } else if ( vals[0].equals("word-match-weight") ) {
                    wordWeight = Double.parseDouble(vals[1]);
                } else if ( vals[0].equals("type-match-weight") ) {
                    typeWeight = Double.parseDouble(vals[1]);
                } else if ( vals[0].equals("sim-threshold") ) {
                    simThreshold = Double.parseDouble(vals[1]);
                }
            }
        } catch (IOException ioe) {
            //System.out.println("File not found");
            wordnetDepth = 5;
            maxParentDepth = 4;
            raiseToPower = 1.0;
            wordWeight = 1.0f;
            textWeight = 1.0f;
            typeWeight = 1.0f;
            simThreshold = 1.0f;
        }
        trh = new BulkLoader(fusedGraph,dbConfig.getDBName(), set, graphConfig.getEndpointT());
        trh.init();
    }
    
    public String getTransformationID() {
        return transformationID;
    }

    public void setTransformationID(String transformationID) {
        this.transformationID = transformationID;
    }
 
    public void tempFunc() throws SQLException {
        final String askWik = "sparql with <http://localhost:8890/wik> select distinct(?p) where { ?s ?p ?o } ";
        final String askUni = "sparql with <http://localhost:8890/uni> select distinct(?p) where { ?s ?p ?o } ";
        final String askOsm = "sparql with <http://localhost:8890/osm> select distinct(?p) where { ?s ?p ?o } ";
        
        PrintWriter wOsm = null;
        PrintWriter wUni = null;
        PrintWriter wWik = null;
        HashMap<String, List<String>> hs = new HashMap<>();
        try {
            //create a temporary file
            File oOsm = new File("osm_predicates.txt");
            File oWik = new File("wik_predicates.txt");
            File oUni = new File("uni_predicates.txt");

            // This will output the full path where the file will be written to...
            //System.out.println(oOsm.getCanonicalPath());

            wOsm = new PrintWriter(new FileWriter(oOsm));
            wUni = new PrintWriter(new FileWriter(oUni));
            wWik = new PrintWriter(new FileWriter(oWik));
            
            PreparedStatement tempStmt;
            ResultSet rs;
        
            tempStmt = virt_conn.prepareStatement(askWik);
            rs = tempStmt.executeQuery();
            PrintWriter wRef = wWik;
            
            while ( rs.next() ) {
                String s = rs.getString(1);
                //System.out.println(s);
                s = URLDecoder.decode(s, "UTF-8");
                //System.out.println(s);
                
                String main = StringUtils.substringAfter(s, "#");
                if (main.equals("") ) {
                    main = StringUtils.substring(s, StringUtils.lastIndexOf(s, "/")+1);
                }
            
                wRef.print(s+" -> "+main);
                
                Matcher mat = patternWordbreaker.matcher(main);
                List<String> toks = new ArrayList<String>();
                while (mat.find()) {
                    toks.add(mat.group());
                }
                
                if (toks.isEmpty()) {
                    wRef.println();
                    continue;
                }
                
                wRef.print(" : [ ");
                for (int i = 0; i < toks.size() - 1; i++) {
                    if ( !hs.containsKey(toks.get(i)) ) {
                        List<String> lst = new ArrayList<>();
                        lst.add(toks.get(i));
                        
                        hs.put(toks.get(i), lst);
                    } else {
                        boolean found = false;
                        for (String ret : hs.get(toks.get(i)) ) {
                            if ( ret.equals(toks.get(i)) ) {
                                found = true;
                                break;
                            }
                        }
                    
                        if ( !found )
                            hs.get(toks.get(i)).add(toks.get(i));
                    }
                    wRef.print(toks.get(i)+", ");
                }
                wRef.println(toks.get(toks.size()-1)+" ]");
                
                if (toks.get(toks.size()-1).equals("SIREN"))
                    //System.out.println(toks.get(toks.size()-1));
                
                if ( !hs.containsKey(toks.get(toks.size()-1)) ) {
                    List<String> lst = new ArrayList<>();
                    lst.add(toks.get(toks.size()-1));
                    
                    hs.put(toks.get(toks.size()-1), lst);
                } else {
                    boolean found = false;
                    for (String ret : hs.get(toks.get(toks.size()-1)) ) {
                        if ( ret.equals(toks.get(toks.size()-1))) {
                            found = true;
                            break;
                        }
                    }
                    
                    if ( !found )
                        hs.get(toks.get(toks.size()-1)).add(toks.get(toks.size()-1));
                }
            }
            
            tempStmt.close();
            rs.close();
            
            tempStmt = virt_conn.prepareStatement(askOsm);
            rs = tempStmt.executeQuery();
            wRef = wOsm;
            
            while ( rs.next() ) {
                String s = rs.getString(1);
                //System.out.println(s);
                s = URLDecoder.decode(s, "UTF-8");
                //System.out.println(s);
                
                String main = StringUtils.substringAfter(s, "#");
                if (main.equals("") ) {
                    main = StringUtils.substring(s, StringUtils.lastIndexOf(s, "/")+1);
                }
            
                wRef.print(s+" -> "+main);
                
                Matcher mat = patternWordbreaker.matcher(main);
                List<String> toks = new ArrayList<String>();
                while (mat.find()) {
                    toks.add(mat.group());
                }
                
                if (toks.isEmpty()) {
                    wRef.println();
                    continue;
                }
                
                wRef.print(" : [ ");
                for (int i = 0; i < toks.size() - 1; i++) {
                    if ( !hs.containsKey(toks.get(i)) ) {
                        List<String> lst = new ArrayList<>();
                        lst.add(toks.get(i));
                        
                        hs.put(toks.get(i), lst);
                    } else {
                        boolean found = false;
                        for (String ret : hs.get(toks.get(i)) ) {
                            if ( ret.equals(toks.get(i)) ) {
                                found = true;
                                break;
                            }
                        }
                    
                        if ( !found )
                            hs.get(toks.get(i)).add(toks.get(i));
                    }
                    wRef.print(toks.get(i)+", ");
                }
                wRef.println(toks.get(toks.size()-1)+" ]");
                
                if ( !hs.containsKey(toks.get(toks.size()-1)) ) {
                    List<String> lst = new ArrayList<>();
                    lst.add(toks.get(toks.size()-1));
                    
                    hs.put(toks.get(toks.size()-1), lst);
                } else {
                    boolean found = false;
                    for (String ret : hs.get(toks.get(toks.size()-1)) ) {
                        if ( ret.equals(toks.get(toks.size()-1))) {
                            found = true;
                            break;
                        }
                    }
                    
                    if ( !found )
                        hs.get(toks.get(toks.size()-1)).add(toks.get(toks.size()-1));
                }
            }
            
            tempStmt.close();
            rs.close();
        
            tempStmt = virt_conn.prepareStatement(askUni);
            rs = tempStmt.executeQuery();
            wRef = wUni;
            
            while ( rs.next() ) {
                String s = rs.getString(1);
                //System.out.println(s);
                s = URLDecoder.decode(s, "UTF-8");
                //System.out.println(s);
                
                String main = StringUtils.substringAfter(s, "#");
                if (main.equals("") ) {
                    main = StringUtils.substring(s, StringUtils.lastIndexOf(s, "/")+1);
                }
            
                wRef.print(s+" -> "+main);
                
                Matcher mat = patternWordbreaker.matcher(main);
                List<String> toks = new ArrayList<String>();
                while (mat.find()) {
                    toks.add(mat.group());
                }
                
                if (toks.isEmpty()) {
                    wRef.println();
                    continue;
                }
                
                wRef.print(" : [ ");
                for (int i = 0; i < toks.size() - 1; i++) {
                    if ( !hs.containsKey(toks.get(i)) ) {
                        List<String> lst = new ArrayList<>();
                        lst.add(toks.get(i));
                        
                        hs.put(toks.get(i), lst);
                    } else {
                        boolean found = false;
                        for (String ret : hs.get(toks.get(i)) ) {
                            if ( ret.equals(toks.get(i)) ) {
                                found = true;
                                break;
                            }
                        }
                    
                        if ( !found )
                            hs.get(toks.get(i)).add(toks.get(i));
                    }
                    wRef.print(toks.get(i)+", ");
                }
                wRef.println(toks.get(toks.size()-1)+" ]");
                
                if ( !hs.containsKey(toks.get(toks.size()-1)) ) {
                    List<String> lst = new ArrayList<>();
                    lst.add(toks.get(toks.size()-1));
                    
                    hs.put(toks.get(toks.size()-1), lst);
                } else {
                    boolean found = false;
                    for (String ret : hs.get(toks.get(toks.size()-1)) ) {
                        if ( ret.equals(toks.get(toks.size()-1))) {
                            found = true;
                            break;
                        }
                    }
                    
                    if ( !found )
                        hs.get(toks.get(toks.size()-1)).add(toks.get(toks.size()-1));
                }
            }
            
            tempStmt.close();
            rs.close();
        
            File oAll = new File("all_words.txt");

            PrintWriter wAll = new PrintWriter(new FileWriter(oAll));
            for (Map.Entry pairs : hs.entrySet()) {
                List<String> lst = (List<String>)pairs.getValue();
            }
            
            wAll.close();
            wOsm.close();
            wWik.close();
            wUni.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                wOsm.close();
                wUni.close();
                wWik.close();
            } catch (Exception e) {
            }
        }
    }

    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso(final String fusedGraph) {   //imports the geometries in the fused graph                     
        
        Statement stmt = null;     
        Connection connection = null;
        System.out.println("Upload of geometries about to commence");
        try{      
            connection = DriverManager.getConnection(DB_URL + dbName, dbUsername, dbPassword);
            //String deleteQuery;
            String subject;
            String fusedGeometry;
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
                   ResultSet.CONCUR_READ_ONLY);                        
            
            //select from source dataset in postgis. The transformations will take place from A to B dataset.
            String selectFromPostGIS = "SELECT DISTINCT subject_a, subject_b, ST_AsText(geom) FROM fused_geometries";    
            try (ResultSet rs = stmt.executeQuery(selectFromPostGIS)) {
                clearBulkLoadHistory();
                System.out.println("Query executed");

                //List<Triple> lst = new ArrayList<>();
                int p = 0;
                //System.out.println("Happens");
                while(rs.next()) {
                    subject = rs.getString("subject_a");
                    fusedGeometry = rs.getString("ST_AsText");
                        //System.out.println("Inserting "+subject + " " + fusedGeometry);
                    if (!(transformationID.equals("Keep both"))){
                        //if transformation is NOT "keep both" -> delete previous geometry
                        
                        trh.deleteAllWgs84(subject);
                        trh.deleteAllGeom(subject);
                        trh.addGeomTriple(subject, fusedGeometry);
                        
                    }
                    else {
                        if (rs.isFirst()){
                            trh.deleteAllGeom(subject);                           
                            trh.addGeomTriple(subject, fusedGeometry);
                            //System.out.println("Geom added for "+subject);
                        }
                        else {
                            //insert second geometry
                            //System.out.println("Geom added for "+subject);
                            trh.addGeomTriple(subject, fusedGeometry);
                        }
                    }
                }
            }
        }
        catch(SQLException ex ){
            //out.close();
            LOG.warn(ex.getMessage(), ex);
        }
        finally{
            if(stmt != null){
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(VirtuosoImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }            
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(VirtuosoImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }        
        }         
    }
    
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso() { //imports the geometries in the graph A                       
        importGeometriesToVirtuoso(null);
    }
        
    public static boolean isLocalEndpoint(String url) throws UnknownHostException {
        // Check if the address is a valid special local or loop back
        int start = url.lastIndexOf("//");
        int last = url.substring(start).indexOf(":");
        //System.out.println(start+" "+last);
        String address = url.substring(start+2, last+start);
        //System.out.println("Address "+address);
        InetAddress addr = InetAddress.getByName(address);
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    
    public void insertLinksMetadataChains(List<Link> links, final String fusedGraph, boolean scanProperties) throws SQLException, IOException, InterruptedException, JWNLException, Exception{ //metadata go in the new fusedGraph. the method is called from FuserWorker 
    //keep metadata subjects according to the transformation                    //selects from graph B, inserts in fused  graph
        //tempFunc();
        
        long starttime, endtime;
        createLinksGraph(links);
        //createDelWGSGraph(((FileBulkLoader)trh).getDelWGSList());
        //createDelGeomGraph(((FileBulkLoader)trh).getDelGeomList());
        StringBuilder getFromB = new StringBuilder();
        StringBuilder getFromA = new StringBuilder();
        final String dropMetaAGraph = "sparql CLEAR GRAPH <"+targetGraph+"_"+db_c.getDBName()+"A"+">";
        final String dropMetaBGraph = "sparql CLEAR GRAPH <"+targetGraph+"_"+db_c.getDBName()+"B"+">";
        final String createMetaAGraph = "sparql CREATE GRAPH <"+targetGraph+"_"+db_c.getDBName()+"A"+">";
        final String createMetaBGraph = "sparql CREATE GRAPH <"+targetGraph+"_"+db_c.getDBName()+"B"+">";
        
        PreparedStatement tempStmt;
        tempStmt = virt_conn.prepareStatement(dropMetaAGraph);
        tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(dropMetaBGraph);
        tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(createMetaAGraph);
        //tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(createMetaBGraph);
        //tempStmt.execute();
        
        starttime =  System.nanoTime();
        //testThreads(links);
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Thread test lasted "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
        
        URL endAURL = new URL(endpointA);
        URL endBURL = new URL(endpointB);
        System.out.println("Host A " + endAURL.getHost() + " : " + endAURL.getPort());
        System.out.println("Host B " + endBURL.getHost() + " : " + endBURL.getPort());
        boolean isEndpointALocal = false;
        boolean isEndpointBLocal = false;
        try
        {
            isEndpointALocal = isThisMyIpAddress(InetAddress.getByName(endAURL.getHost())); //"localhost" for localhost
        }
        catch(UnknownHostException unknownHost)
        {
            System.out.println("It is not");
        }
        try
        {
            isEndpointBLocal = isThisMyIpAddress(InetAddress.getByName(endBURL.getHost())); //"localhost" for localhost
        }
        catch(UnknownHostException unknownHost)
        {
            System.out.println("It is not");
        }
        System.out.println(isEndpointALocal);
        System.out.println(isEndpointBLocal);
        
        starttime =  System.nanoTime();
        String endpointLoc2 = endpointA;
        //System.out.println("is local "+isLocalEndpoint(endpointA));
        //if (endpointLoc2.equals(endpointA)) {
            getFromA.append("sparql INSERT\n");
            if (scanProperties)
                getFromA.append("  { GRAPH <").append(targetGraph).append("_"+db_c.getDBName()+"A"+"> {\n");
            else 
                getFromA.append("  { GRAPH <").append(targetGraph).append(""+"> {\n");
            if (gr_c.isDominantA())
                getFromA.append(" ?s ?p ?o1 . \n");
            else
                getFromA.append(" ?o ?p ?o1 . \n");
            getFromA.append(" ?o1 ?p4 ?o3 .\n");
            getFromA.append(" ?o3 ?p5 ?o4 .\n");
            getFromA.append(" ?o4 ?p6 ?o5\n");
            getFromA.append("} }\nWHERE\n");
            getFromA.append("{\n");
            getFromA.append(" GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n");
            if ( isEndpointALocal ) 
                getFromA.append(" GRAPH <").append(graphA).append("> { {?s ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
            else    
                getFromA.append(" SERVICE <"+endpointA+"> { GRAPH <").append(graphA).append("> { {?s ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } } }\n");
            getFromA.append("\n");
            getFromA.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p4,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromA.append("}");
            //System.out.println(getFromA);
        //}
        
        //if (endpointLoc2.equals(endpointB)) {
            getFromB.append("sparql INSERT\n");
            if (scanProperties)
                getFromB.append("  { GRAPH <").append(targetGraph).append("_"+db_c.getDBName()+"B"+"> {\n");
            else
                getFromB.append("  { GRAPH <").append(targetGraph).append(""+"> {\n");
            if (gr_c.isDominantA())
                getFromB.append(" ?s ?p ?o1 . \n");
            else
                getFromB.append(" ?o ?p ?o1 . \n");
            getFromB.append(" ?o1 ?p4 ?o3 .\n");
            getFromB.append(" ?o3 ?p5 ?o4 .\n");
            getFromB.append(" ?o4 ?p6 ?o5\n");
            getFromB.append("} }\nWHERE\n");
            getFromB.append("{\n");
            getFromB.append(" GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n");
            if ( isEndpointBLocal ) 
                getFromB.append(" GRAPH <").append(graphB).append("> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
            else 
                getFromB.append(" SERVICE <"+endpointB+"> { GRAPH <").append(graphB).append("> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } } }\n");
            getFromB.append("\n");
            getFromB.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p4,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromB.append("}");

        //}        
        System.out.println("GET FROM B \n"+getFromB);
        System.out.println("GET FROM B \n"+getFromA);
        
        int count = 0;
        int i = 0;
        
        while (i < links.size()) {
            
            createLinksGraphBatch(links, i);
            
            starttime =  System.nanoTime();
            /*
            UpdateRequest insertFromA = UpdateFactory.create(getFromA.toString());
            UpdateProcessor insertRemoteA = UpdateExecutionFactory.createRemoteForm(insertFromA, endpointA);
            insertRemoteA.execute();
        
            UpdateRequest insertFromB = UpdateFactory.create(getFromB.toString());
            UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(insertFromB, endpointB);
            insertRemoteB.execute();
            */
            
            tempStmt = virt_conn.prepareStatement(getFromA.toString());
            tempStmt.executeUpdate();
        
            tempStmt.close();
                        
            tempStmt = virt_conn.prepareStatement(getFromB.toString());
            tempStmt.executeUpdate();
            
            //if (scanProperties)
            //    scanProperties(2, null);
            
            tempStmt.close();
            
            endtime =  System.nanoTime();
            LOG.info("Metadata main parsed in "+(endtime-starttime)/1000000000f);
            i += BATCH_SIZE;
            count++;
        }
        //System.out.println(count);
        
        //System.out.println("First List");

        endtime =  System.nanoTime();
        LOG.info("Metadata parsed in "+(endtime-starttime)/1000000000f);       
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Metadata parsed in "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
    }
    
    static int tom = 0;
    HashMap<String, HashSet<ScoredMatch>> foundA = new HashMap<>();
    HashMap<String, HashSet<ScoredMatch>> foundB = new HashMap<>();
    HashSet<String> recoveredWords = null;
    HashSet<String> uniquePropertiesA = new HashSet<>();
    HashSet<String> uniquePropertiesB = new HashSet<>();
    HashSet<String> nonMatchedPropertiesA = new HashSet<>();
    HashSet<String> nonMatchedPropertiesB = new HashSet<>();
    
    //Integer
    final Pattern patternInt = Pattern.compile( "^(\\d+)$" );
    //Date
    final Pattern patternDate = Pattern.compile( "^(\\d{2}(/\\d{2}/\\d{4}|-\\d{2}-\\d{4}))$" );
    //Word
    final Pattern patternWord = Pattern.compile( "^(\\w)$" );
    //Text
    final Pattern patternText = Pattern.compile( "\\w(\\s+\\w)+" );
    //Decimal
    final Pattern patternDecimal = Pattern.compile( "^(\\d+(.|,)\\d+)$" );
    // Word breaker
    //final Pattern patternWordbreaker = Pattern.compile( "(([a-z]|[A-Z])[a-z]*)" );
    final Pattern patternWordbreaker = Pattern.compile( "(([a-z]|[A-Z])[a-z]+)|(([a-z]|[A-Z])[A-Z]+)" );
    
    public int compareTypes(String l, String r) {
        if (patternInt.matcher(l).find() && patternInt.matcher(r).find()) return 1;
        if (patternDate.matcher(l).find() && patternDate.matcher(r).find()) return 1;
        if (patternText.matcher(l).find() && patternText.matcher(r).find()) return 1;
        if (patternDecimal.matcher(l).find() && patternDecimal.matcher(r).find()) return 1;
        if (patternWord.matcher(l).find() && patternWord.matcher(r).find()) return 1;

        return 0;
    }
    
    private class SchemaNormalizer{
        Schema sRef;
        float textDist;
        float semDist;
        float typeDist;
        
        public SchemaNormalizer(Schema sRef, float textDist, float semDist, float typeDist) {
            this.sRef = sRef;
            this.textDist = textDist;
            this.semDist = semDist;
            this.typeDist = typeDist;
        }
        
    }
    
    HashMap<String, Schema> schemasA = Maps.newHashMap();
    HashMap<String, Schema> schemasB = Maps.newHashMap();
        
    private void scanMatches() throws JWNLException, FileNotFoundException, IOException, ParseException {
        /*Iterator it = propertiesA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            System.out.println("Debug kapote "+(pairs.getKey() + " = " + (MetadataChain)pairs.getValue()+"\n"));
            it.remove(); // avoids a ConcurrentModificationException
        }
        */
        //System.out.println("Schema Match "+tom++);
        expandChain(schemasA, propertiesA, "");  
        //System.out.println();
        expandChain(schemasB, propertiesB, "");    
        for (Map.Entry pairs : schemasA.entrySet()) {
            String chain = (String)pairs.getKey();
            Schema sche = (Schema)pairs.getValue();
            System.out.println("A "+chain+" "+sche.predicate + " Size " + sche.indexes.size());
        }
        for (Map.Entry pairs : schemasB.entrySet()) {
            String chain = (String)pairs.getKey();
            Schema sche = (Schema)pairs.getValue();
            System.out.println("B "+chain+" "+sche.predicate + " Size " + sche.indexes.size());
        }
        
        List<SchemaMatcher> matchers = new ArrayList<>();
        int countA;
        int countB;
        float score;
        float sim;
        
        Map<String, List<SchemaNormalizer> > scorer = Maps.newHashMap();
        for (Map.Entry pairsA : schemasA.entrySet()) {
            //String chainA = (String)pairsA.getKey();
            Schema scheA = (Schema)pairsA.getValue();
            
            nonMatchedPropertiesA.add(scheA.predicate);
            
            if (scheA.indexes.isEmpty()) {
                //System.out.println("Empty Index A");
                continue;
            }
            
            float maxDist = -1.0f;
            float maxScore = -1.0f;
            for (Map.Entry pairsB : schemasB.entrySet()) {
                //String chainB = (String)pairsB.getKey();
                Schema scheB = (Schema)pairsB.getValue();
                //System.out.println("Would Match "+scheA.predicate + scheB.predicate);
                nonMatchedPropertiesB.add(scheB.predicate);
                score = 0;
                sim = 0;
                
                if (scheB.indexes.isEmpty()) {
                    //System.out.println("Empty Index A");
                    continue;
                }
                
                SchemaMatcher m =  new SchemaMatcher(scheA, scheB);
                countA = 0;
                                            
                //System.out.println("Matching "+scheA.predicate + scheB.predicate);
                //System.out.println("Main "+main);
                for ( IndexWord iwA : scheA.indexes) {
                    countA++;
                    countB = 0;
                    for ( IndexWord iwB : scheB.indexes) {
                        System.out.println("Scoring : "+iwA.getLemma()+ " and "+ iwB.getLemma());   
                        countB++;
                        float tmpScore = calculateAsymmetricRelationshipOperation(iwA, iwB, m);
                        score += tmpScore;
                        //System.out.println("Score : "+tmpScore);                       
                    }
                }
                System.out.println();
                float jaro_dist = 0;
                float jaro_dist_norm;
                int jaroCount = 0;
                for ( String iwA : scheA.words) {
                    countA++;
                    countB = 0;
                    for ( String iwB : scheB.words) {
                        jaroCount++;
                        //System.out.println("Jaroing : "+iwA+ " and "+ iwB);   
                        jaro_dist += (float) StringUtils.getJaroWinklerDistance(iwA, iwB);
                    }
                }
                
                jaro_dist_norm = jaro_dist / (jaroCount);
                //System.out.println("Jaro : " + jaro_dist_norm);
                //scheB.texDist = jaro_dist_norm;
                if (jaro_dist_norm > maxDist ) {
                    maxDist = jaro_dist_norm;
                }
                score = score / (scheA.indexes.size()*scheB.indexes.size());
                //scheB.semDist = score;
                if (score > maxScore ) {
                    maxScore = score;
                }
                    
                //scheB.typeDist = 0.0f;
                float sim_score = (score+jaro_dist)/2;
                //if (sim_score > 0.5) {
                    int same_type = compareTypes(scheA.objectStr, scheB.objectStr);
                    //System.out.print("Sim Score : "+sim_score+" "+same_type+" ");
                    //System.out.print("Type Score : "+scheA.objectStr+" and "+scheB.objectStr+" "+sim_score+" "+same_type+" ");
                    //System.out.print("Jaro distance of "+jaro_dist_norm+" ");
                    //System.out.print("Mul : "+scheA.indexes.size()+" x "+scheB.indexes.size() + " = "+(scheA.indexes.size()*scheB.indexes.size())+" ");
                    //System.out.println("Final : " + (score + jaro_dist_norm + same_type)/3);
                    //scheB.typeDist = same_type;
                    sim_score = (score + jaro_dist_norm + 0.5f * same_type)/3;
                //}
                    //System.out.println("Probs ::::::: "+score);
                SchemaNormalizer snorm = new SchemaNormalizer(scheB, jaro_dist_norm, score, same_type );
                if ( !scorer.containsKey(scheA.predicate+scheB.predicate)) {
                    List<SchemaNormalizer> scheLst = new ArrayList<SchemaNormalizer>();
                    scheLst.add(snorm);
                    scorer.put(scheA.predicate+scheB.predicate, scheLst);
                }
                scorer.get(scheA.predicate+scheB.predicate).add(snorm);
                //m.score = sim_score;
                if(!m.matches.isEmpty()){
                    //System.out.println(m.matches.get(0));
                    matchers.add(m);
                }    
            }
            
            scheA.texDist = maxDist;
            scheA.semDist = maxScore;
        }
        
        //System.out.println("Matches "+matchers.size());
        //for (SchemaMatcher ma : matchers) {
            //System.out.println(ma);
        //}
        //System.out.println("Total Matches "+foundA.size());
        HashMap<String, Schema> schemaPtrsA = Maps.newHashMap();
        HashMap<String, Schema> schemaPtrsB = Maps.newHashMap();
        //System.out.println("------------------------------------------");
        //System.out.println("            NON MATCHED ENTITIES          ");
        //System.out.println("------------------------------------------");
        for (SchemaMatcher ma : matchers) {
            if (!ma.matches.isEmpty()) {
                //if (!foundA.containsKey(ma.sA.predicate)) {
                
                //System.out.println(ma.sA.predicate);
                //System.out.println(ma.sB.predicate);
                //System.out.println(nonMatchedPropertiesA);
                //System.out.println(nonMatchedPropertiesB);
                nonMatchedPropertiesA.remove(ma.sA.predicate);
                nonMatchedPropertiesB.remove(ma.sB.predicate);
                
                HashSet<ScoredMatch> matchesA = foundA.get(ma.sA.predicate);
                HashSet<ScoredMatch> matchesB = foundB.get(ma.sB.predicate);

                SchemaNormalizer selectedSche = null;
                List<SchemaNormalizer> snormLst = scorer.get(ma.sA.predicate + ma.sB.predicate);
                for (SchemaNormalizer snorm : snormLst) {
                    if (ma.sB.predicate.equals(snorm.sRef.predicate)) {
                        selectedSche = snorm;
                        break;
                    }
                }
                    //System.out.println("Scoring "+selectedSche.semDist+" "+selectedSche.textDist+" "+selectedSche.typeDist);
                //System.out.println("Scoring "+ma.sA.semDist+" "+ma.sA.texDist);
                float sim_score = 0;
                if (!ma.sA.predicate.equals(selectedSche.sRef.predicate)) {
                    if (ma.sA.semDist < 0.00000001f) {
                        ma.sA.semDist = 1.0f;
                    }
                    if (ma.sA.texDist < 0.00000001f) {
                        ma.sA.texDist = 1.0f;
                    }

                    sim_score = (float) (wordWeight * ((selectedSche.semDist / ma.sA.semDist))
                            + (textWeight * (selectedSche.textDist / ma.sA.texDist))
                            + (typeWeight * (selectedSche.typeDist))) / 3.0f;

                    //System.out.println("Scoring "+ma.sA.predicate+" "+selectedSche.sRef.predicate+" = "+ sim_score);
                } else {
                    sim_score = 1.0f;
                }
                ma.score = sim_score;

                schemaPtrsA.put(ma.sA.predicate, ma.sA);
                schemaPtrsA.put(ma.sB.predicate, ma.sB);

                if (matchesA == null) {
                    matchesA = Sets.newHashSet();
                    foundA.put(ma.sA.predicate, matchesA);
                }
                if (matchesB == null) {
                    matchesB = Sets.newHashSet();
                    foundB.put(ma.sB.predicate, matchesB);
                }

                ScoredMatch scoredA = new ScoredMatch(ma.sB.predicate, ma.score);
                ScoredMatch scoredB = new ScoredMatch(ma.sA.predicate, ma.score);

                if (!matchesA.contains(scoredA)) {
                    matchesA.add(scoredA);
                }
                if (!matchesB.contains(scoredA)) {
                    matchesB.add(scoredB);
                }

                //}
                //System.out.println("Matched "+ma.sA.predicate+" with "+ ma.sB.predicate + " = "+ma.score);
            }
        }
    }
    
    private void expandChain(HashMap<String, Schema> lst, HashMap< String, MetadataChain > chains, String chainStr) throws JWNLException, FileNotFoundException, IOException, ParseException {

        //JWNL.initialize(new FileInputStream("/home/nick/NetBeansProjects/test/test/FAGI-gis-WebInterface/lib/jwnl14-rc2/config/file_properties.xml"));
        //File currentDirectory = new File(new File(".").getAbsolutePath());
        //JWNL.initialize(new FileInputStream(currentDirectory.getCanonicalPath()+"/src/main/resources/file_properties.xml"));

        Dictionary dictionary = Dictionary.getInstance();
        //System.out.println("Chain REP : "+chainStr);
        for (Map.Entry pairs : chains.entrySet()) {
            MetadataChain chain = (MetadataChain)pairs.getValue();
            //System.out.println("Chain REP 2 : "+chainStr+chain.predicate+","); 
            //System.out.println(System.getenv());
            String pad;
            if (chain.chains != null) {
                pad = ",";
                expandChain(lst, chain.chains, chainStr+chain.predicate+pad);
            }
            
            if (lst.containsKey(chainStr)){
                continue;
            }
    
            String pred = chainStr.concat(chain.predicate);
            
            //String que = "";
            List<String> arrl = new ArrayList<>();
            //System.out.println("Chain Link : " + chain.link);
            
            //URL normalizer can possibly be truned on here
            String normalizedLink = URLDecoder.decode(chain.link, "UTF-8");
            //Matcher mat = patternWordbreaker.matcher(chain.link);
            Matcher mat = patternWordbreaker.matcher(normalizedLink);
            while (mat.find()) {
                arrl.add(mat.group());
            }
            //System.out.print("Found:");
            for(String s : arrl)
                System.out.print(" "+s);
            System.out.println();
            //List<String> arrl = StringUtils.splitByCharacterTypeCamelCase(chain.link);
            Schema m = new Schema(); 
            m.predicate = pred;
            m.objectStr= chain.objectStr;
            //System.out.println("breaking "+chain.link);
            Analyzer englishAnalyzer =  new EnglishAnalyzer(Version.LUCENE_36);
            QueryParser englishParser = new QueryParser(Version.LUCENE_36, "", englishAnalyzer);
            for (String a : arrl) {
                m.addWord(a);
                if (recoveredWords.contains(a.toLowerCase())) {
                    System.out.println("Cancelling "+a);
                    continue;
                }
            
                //System.out.print("Value "+a+" ");
                
                System.out.println("Value : "+a+" stemmed : "+englishParser.parse(a).toString());
                IndexWordSet wordSet = dictionary.lookupAllIndexWords(englishParser.parse(a).toString());
                //IndexWordSet wordSet = dictionary.lookupAllIndexWords(a);
                if (wordSet == null)
                    continue;
                IndexWord[] indices = wordSet.getIndexWordArray();
                IndexWord best = null;
                int bestInt = 0;
                for (IndexWord idx : indices) { 
                    System.out.println("POS label " + idx.getPOS().getLabel());
                    if ( idx.getPOS().getLabel().equals("noun") ) {
                        best = idx;
                        bestInt = 3;
                    } else if ( idx.getPOS().getLabel().equals("adjective") && bestInt < 3 ) {
                        best = idx;
                        bestInt = 2;
                    } else if ( idx.getPOS().getLabel().equals("verb") && bestInt < 2 ) {
                        best = idx;
                        bestInt = 1;
                    }
                }
                
                if (best == null) {
                    System.out.println("Null Best for " + englishParser.parse(a).toString());
                    continue;
                }
            
                m.addIndex(best);
            }
            //System.out.println(best.getPOS().getLabel());
            //System.out.println();

            //System.out.println("Inserting predicate: "+ pred);
            lst.put(pred, m);
        }
        //System.out.println();
    }
    
    private void scanChain(HashMap< String, MetadataChain > cont, List <String> chain, List <String> objectChain) {
        //System.out.print("Chain: ");
        if (chain.isEmpty())
            return;
        
        String link = chain.get(0);
        if (link == null) {
            return;
        }
        
        MetadataChain root = null;
        if ( cont.containsKey(link) ) {
            root = cont.get(link);
        } else {
            String main = StringUtils.substringAfter(link, "#");
            if (main.equals("") ) {
                main = StringUtils.substring(link, StringUtils.lastIndexOf(link, "/")+1);
            }
            
            //Analyzer englishAnalyzer =  new EnglishAnalyzer(Version.LUCENE_36);
            //QueryParser englishParser = new QueryParser(Version.LUCENE_36, "", englishAnalyzer);
            
            //System.out.println("Parsed "+englishParser.parse(main));
            //System.out.println("Main "+main);
            
            root = new MetadataChain(main, link, objectChain.get(0));
            cont.put(root.predicate, root);
            //System.out.println(main+" "+root.predicate+" "+link);
        }
                
        for (int i = 1; i < chain.size(); i++ ) {
            //System.out.println("Oxi file");
            link = chain.get(i);
            if (link == null) {
                //System.out.println("null");
                return;
            }
            
            if ( root.chains != null ) {
                if ( root.chains.containsKey(link) ) {
                    root = root.chains.get(link);
                    continue;
                }
            }
            
            String main = StringUtils.substringAfter(link, "#");
            if (main.equals("") ) {
                main = StringUtils.substring(link, StringUtils.lastIndexOf(link, "/")+1);
            }
            //System.out.println(main);
            
            MetadataChain newChain = new MetadataChain(main, link, objectChain.get(i));
            root.addCahin(newChain);
            root = newChain;
            //System.out.print(link+" ");
        }
        
        //System.out.println(root);
    }
    
    public SchemaMatchState scanProperties(int optDepth, String link) throws SQLException, JWNLException, FileNotFoundException, IOException, ParseException {
        optDepth = 3;
        if ( SystemUtils.IS_OS_MAC_OSX ) {
            JWNL.initialize(new ByteArrayInputStream(getJWNL(PATH_TO_WORDNET_OS_X).getBytes(StandardCharsets.UTF_8)));
        }
        if ( SystemUtils.IS_OS_LINUX ) {
            JWNL.initialize(new ByteArrayInputStream(getJWNL(PATH_TO_WORDNET_LINUX).getBytes(StandardCharsets.UTF_8)));
        }
        if ( SystemUtils.IS_OS_WINDOWS ) {
            JWNL.initialize(new ByteArrayInputStream(getJWNL(PATH_TO_WORDNET_WINDOWS).getBytes(StandardCharsets.UTF_8)));
        }
        try(
          //InputStream file = new FileInputStream("/home/nick/NetBeansProjects/test/test/FAGI-gis-CLI/src/main/resources/stopWords.ser");
          //InputStream file = new FileInputStream("/var/lib/tomcat7/webapps/FAGI-WebInterface/images/stopWords.ser");
          InputStream file = this.getClass().getResourceAsStream("/stopWords.ser");
          InputStream buffer = new BufferedInputStream(file);
          ObjectInput input = new ObjectInputStream (buffer);
        ){
          //deserialize the List
          recoveredWords = (HashSet)input.readObject();

          //for(String word: recoveredWords){
           // System.out.println("Recovered Quark: " + word);
          //}
        }
        catch(ClassNotFoundException ex){
          System.out.println("No class");
        }
        catch(IOException ex){
          System.out.println("No input");
        }
        
        foundA.clear();
        foundB.clear();
        if (link != null) {
            for (int i = 0; i < optDepth + 1; i++) {
                //System.out.println("DEPTH: "+i);
                StringBuilder query = new StringBuilder();
                query.append("sparql SELECT ?pa1 ?oa1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
                }
                query.append("?pb1 ?ob1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
                }
                query.append("\nWHERE\n{\n" + " { GRAPH <").append(targetGraph).append("_" + db_c.getDBName() + "A> { {<" + link + "> ?pa1 ?oa1} ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . OPTIONAL {?oa").append(prev).append(" ?pa").append(ind).append(" ?oa").append(ind).append(" ");
                }
                for (int j = 0; j < i; j++) {
                    query.append(" } ");
                }
                query.append("}\n } UNION { \n" + "   GRAPH <").append(targetGraph).append("_" + db_c.getDBName() + "B> { {<" + link + "> ?pb1 ?ob1} ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . OPTIONAL {?ob").append(prev).append(" ?pb").append(ind).append(" ?ob").append(ind).append(" ");
                }
                for (int j = 0; j < i; j++) {
                    query.append(" } ");
                }
                query.append("} }\n"
                        + "}");

            //System.out.println("SINGLE LINK QUERY "+query.toString());
                PreparedStatement fetchProperties;
                fetchProperties = virt_conn.prepareStatement(query.toString());
                ResultSet propertiesRS = fetchProperties.executeQuery();

                String prevSubject = link;
                while (propertiesRS.next()) {
                    List<String> chainA = new ArrayList<>();
                    List<String> chainB = new ArrayList<>();
                    List<String> objectChainA = new ArrayList<>();
                    List<String> objectChainB = new ArrayList<>();
                    for (int j = 0; j <= i; j++) {
                        int step_over = 2 * (i + 1);

                        String predicateA = propertiesRS.getString(2 * (j + 1) - 1);
                        String objectA = propertiesRS.getString(2 * (j + 1));
                        String predicateB = propertiesRS.getString(2 * (j + 1) + step_over - 1);
                        String objectB = propertiesRS.getString(2 * (j + 1) + step_over);

                        if (predicateA != null) {
                            //predicateA = URLDecoder.decode(predicateA, "UTF-8");
                        }
                        if (predicateB != null) {
                            //predicateB = URLDecoder.decode(predicateB, "UTF-8");
                        }

                        if (predicateA != null) {
                            if (!uniquePropertiesA.contains(predicateA)) {
                                //uniquePropertiesA.add(predicateA);
                            }
                            if (predicateA.contains("posSeq")) {
                                continue;
                            }
                            if (predicateA.contains("asWKT")) {
                                continue;
                            }
                            if (predicateA.contains("geometry")) {
                                continue;
                            }
                        }
                        if (predicateB != null) {
                            if (!uniquePropertiesB.contains(predicateB)) {
                                //uniquePropertiesB.add(predicateB);
                            }
                            if (predicateB.contains("posSeq")) {
                                continue;
                            }
                            if (predicateB.contains("asWKT")) {
                                continue;
                            }
                            if (predicateB.contains("geometry")) {
                                continue;
                            }
                        }

                        chainA.add(predicateA);
                        objectChainA.add(objectA);
                        chainB.add(predicateB);
                        objectChainB.add(objectB);

                        if (objectA != null) {
                            //System.out.println("Object A "+objectA+" "+predicateA);
                        }
                        if (objectB != null) {
                            //System.out.println("Object B "+objectB+" "+predicateB);
                        }
                    }
                    scanChain(propertiesA, chainA, objectChainA);
                    scanChain(propertiesB, chainB, objectChainB);
                }

                scanMatches();
                propertiesA.clear();
                propertiesB.clear();
                
                propertiesRS.close();
                fetchProperties.close();
            }
        } else {
            final String dropSamplesGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/links_sample_" + db_c.getDBName() + ">";
            final String createSamplesGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/links_sample_" + db_c.getDBName() + ">";
            final String createLinksSample = "SPARQL INSERT\n"
                    + " { GRAPH <http://localhost:8890/DAV/links_sample_" + db_c.getDBName() + "> {\n"
                    + " ?s ?p ?o\n"
                    + "} }\n"
                    + "WHERE\n"
                    + "{\n"
                    + "{\n"
                    + "SELECT ?s ?p ?o WHERE {\n"
                    + " GRAPH <http://localhost:8890/DAV/links_" + db_c.getDBName() + "> { ?s ?p ?o }\n"
                    + "} limit " + SAMPLE_SIZE + "\n"
                    + "}}";

            PreparedStatement dropSamplesStmt;
            dropSamplesStmt = virt_conn.prepareStatement(dropSamplesGraph);
            dropSamplesStmt.execute();

            PreparedStatement createSamplesStmt;
            createSamplesStmt = virt_conn.prepareStatement(createSamplesGraph);
            createSamplesStmt.execute();

            PreparedStatement insertLinksSample;
            insertLinksSample = virt_conn.prepareStatement(createLinksSample);
            insertLinksSample.execute();

            dropSamplesStmt.close();
            createSamplesStmt.close();
            insertLinksSample.close();
            
            foundA.clear();
            foundB.clear();
            //for (int i = optDepth; i >= 0; i--) {
            for (int i = 0; i < optDepth + 1; i++) {
                //System.out.println("DEPTH: "+i);
                StringBuilder query = new StringBuilder();
                query.append("sparql SELECT distinct(?s) ?pa1 ?oa1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
                }
                query.append("?pb1 ?ob1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
                }
                query.append(" WHERE \n {\n");
                query.append("SELECT ?s ?pa1 ?oa1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
                }
                query.append("?pb1 ?ob1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
                }
                if (gr_c.isDominantA()) {
                    query.append("\nWHERE\n{\n  GRAPH <http://localhost:8890/DAV/links_sample_" + db_c.getDBName() + "> { ?s ?p ?o }\n");
                } else {
                    query.append("\nWHERE\n{\n  GRAPH <http://localhost:8890/DAV/links_sample_" + db_c.getDBName() + "> { ?o ?p ?s }\n");
                }
                query.append(" { GRAPH <").append(targetGraph).append("_" + db_c.getDBName() + "A> { {?s ?pa1 ?oa1} ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . ?oa").append(prev).append(" ?pa").append(ind).append(" ?oa").append(ind).append("  ");
                }
                for (int j = 0; j < i; j++) {
                    query.append(" ");
                }
                query.append("}\n } UNION { \n" + "   GRAPH <").append(targetGraph).append("_" + db_c.getDBName() + "B> { {?s ?pb1 ?ob1} ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . ?ob").append(prev).append(" ?pb").append(ind).append(" ?ob").append(ind).append("  ");
                }
                for (int j = 0; j < i; j++) {
                    query.append(" ");
                }
                query.append("} }\n"
                        + "}\n"
                        + "} ORDER BY (?s)");

                System.out.println("Properties Query : "+query.toString());
                PreparedStatement fetchProperties;
                fetchProperties = virt_conn.prepareStatement(query.toString());
                ResultSet propertiesRS = fetchProperties.executeQuery();

                String prevSubject = "";
                while (propertiesRS.next()) {
                    final String subject = propertiesRS.getString(1);
                    //propertiesRS.
                    if (!prevSubject.equals(subject) && !prevSubject.equals("")) {
                        //if (i == optDepth) {
                        //System.out.println(subject);
                        scanMatches();
                        propertiesA.clear();
                        propertiesB.clear();
                        //}
                    }
                    //System.out.println(subject);
                    List<String> chainA = new ArrayList<>();
                    List<String> chainB = new ArrayList<>();
                    List<String> objectChainA = new ArrayList<>();
                    List<String> objectChainB = new ArrayList<>();
                    for (int j = 0; j <= i; j++) {
                    //int ind = j+2;
                        //int prev = ind - 1;
                        int step_over = 2 * (i + 1);

                        String predicateA = propertiesRS.getString(2 * (j + 1));
                        String objectA = propertiesRS.getString(2 * (j + 1) + 1);
                        String predicateB = propertiesRS.getString(2 * (j + 1) + step_over);
                        String objectB = propertiesRS.getString(2 * (j + 1) + 1 + step_over);
                        /*if (objectA != null) {
                         System.out.println("Object A "+objectA+" "+patternInt.asPredicate().test(objectA));
                         }
                         if (objectB != null) {
                         System.out.println("Object B "+objectB+" "+patternInt.asPredicate().test(objectB));
                         }*/

                        if (predicateA != null) {
                            //predicateA = URLDecoder.decode(predicateA, "UTF-8");
                        }
                        if (predicateB != null) {
                            //predicateB = URLDecoder.decode(predicateB, "UTF-8");
                        }

                        if (predicateA != null) {
                            if (!nonMatchedPropertiesA.contains(predicateA)) {
                                //nonMatchedPropertiesA.add(predicateA);
                            }
                            if (!uniquePropertiesA.contains(predicateA)) {
                                uniquePropertiesA.add(predicateA);
                            }
                            if (predicateA.contains("posSeq")) {
                                //continue;
                            }
                            if (predicateA.contains("asWKT")) {
                                //continue;
                            }
                            if (predicateA.contains("geometry")) {
                                //continue;
                            }
                        }
                        if (predicateB != null) {
                            if (!nonMatchedPropertiesB.contains(predicateB)) {
                                //nonMatchedPropertiesB.add(predicateB);
                            }
                            if (!uniquePropertiesB.contains(predicateB)) {
                                uniquePropertiesB.add(predicateB);
                            }
                            if (predicateB.contains("posSeq")) {
                                //continue;
                            }
                            if (predicateB.contains("asWKT")) {
                                //continue;
                            }
                            if (predicateB.contains("geometry")) {
                                //continue;
                            }
                        }

                        chainA.add(predicateA);
                        objectChainA.add(objectA);
                        chainB.add(predicateB);
                        objectChainB.add(objectB);

                        //System.out.println(" "+predicateA+" "+objectA);
                        //System.out.println(" "+predicateB+" "+objectB);
                    }
                    //System.out.println("Chain A "+chainA);
                    //System.out.println("Chain B "+chainB);
                    scanChain(propertiesA, chainA, objectChainA);
                    scanChain(propertiesB, chainB, objectChainB);

                    prevSubject = subject;
                }

                scanMatches();
                propertiesA.clear();
                propertiesB.clear();

                propertiesRS.close();
                fetchProperties.close();
                //System.out.println(query2.toString());
            }
        }
        StringBuilder sb = new StringBuilder();
        Iterator it = propertiesA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            sb.append(pairs.getKey()).append(" = ").append((MetadataChain)pairs.getValue()).append("\n");
            it.remove(); // avoids a ConcurrentModificationException
        }
        if(JWNL.isInitialized()){
                JWNL.shutdown();
        }
        
        HashMap<String, Integer> freqMap = Maps.newHashMap();
        for (String key : uniquePropertiesA) {
            //System.out.println(key);
            String onto = StringUtils.substringBefore(key, "#");
            onto = onto.concat("#");
            if (onto.equals(key) ) {
                onto = StringUtils.substring(key, 0,StringUtils.lastIndexOf(key, "/"));
                onto = onto.concat("/");
            }
            //System.out.println("Onto "+onto+" "+StringUtils.lastIndexOf(key, "/"));
            if ( freqMap.containsKey(onto) ) {
                freqMap.put(onto, freqMap.get(onto) + 1);
            } else {
                freqMap.put(onto, 1);
            }
        }
        int max = -1;
        String domOntologyA = "";
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            String key = entry.getKey();
            Integer value = (Integer) entry.getValue();
            if ( value > max ) {
                if ( key.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns") ) 
                    continue;
                max = value;
                domOntologyA = key;
            }
            //System.out.println("Entry "+key+" : "+value);
        }
        freqMap.clear();
        for (String key : uniquePropertiesB) {
            //System.out.println(key);
            String onto = StringUtils.substringBefore(key, "#");
            onto = onto.concat("#");
            if (onto.equals(key) ) {
                onto = StringUtils.substring(key, 0,StringUtils.lastIndexOf(key, "/"));
                onto = onto.concat("/");
            }
            //System.out.println("Onto "+onto+" "+StringUtils.lastIndexOf(key, "/"));
            if ( freqMap.containsKey(onto) ) {
                freqMap.put(onto, freqMap.get(onto) + 1);
            } else {
                freqMap.put(onto, 1);
            }
        }
        max = -1;
        String domOntologyB = "";
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            String key = entry.getKey();
            Integer value = (Integer) entry.getValue();
            if ( value > max ) {
                if ( key.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns") ) 
                    continue;
                max = value;
                domOntologyB = key;
            }
            //System.out.println("Entry "+key+" : "+value);
        }
        
        //System.out.println("Dominant from A "+domOntologyA+", Dominant from B "+domOntologyB);
        //System.out.println();
        //for (String key : uniquePropertiesB) {
        //    System.out.println(key);
        //}
        
        //System.out.println("Found A");
        Iterator iter = foundA.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry pairs = (Map.Entry)iter.next();
                HashSet<ScoredMatch> set = (HashSet<ScoredMatch>)pairs.getValue();
                //System.out.println("KEY: "+pairs.getKey());
                for(ScoredMatch s : set) {
                    //System.out.println(s.getRep());
                }
            }
        //System.out.println("Found B");
            iter = foundB.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry pairs = (Map.Entry)iter.next();
                HashSet<ScoredMatch> set = (HashSet<ScoredMatch>)pairs.getValue();
                //System.out.println("KEY: "+pairs.getKey());
                //for(ScoredMatch s : set) {
                    //System.out.println(s.getRep());
                //}
            }
        
        return new SchemaMatchState(foundA, foundB, domOntologyA, domOntologyB, nonMatchedPropertiesA, nonMatchedPropertiesB);
    }
    
    private class Namespace {
        String ontology;
        int freq = 0;

        public Namespace(String ontology) {
            this.ontology = ontology;
            freq = 1;
        }
        
        @Override
        public boolean equals(Object object) {
            return ontology.equals(object);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.ontology);
            return hash;
        }
    }
    
    //int maxParentDepth = 3;
    private int scanSense(Synset i, Synset j) throws JWNLException {
        //RelationshipList list = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM);
        //RelationshipList listLvl1 = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM, 1);
        RelationshipList listLvl2 = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM, wordnetDepth);
	//System.out.println("Hypernym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
	//int ret = -1;
        int tom;
        //int count = 0;
        int min = 999999;
    
        for(Object o : listLvl2) {
            //System.out.println("List Size " + list.size());
            //count++;
            Relationship rel = (Relationship) o;
            tom = ((AsymmetricRelationship) rel).getCommonParentIndex();
            if (tom < min) 
                min = tom;
            //System.out.println("Common Parent Index: " + tom);
            //System.out.println("Depth 2: " + rel.getDepth());
            //rel.getNodeList().print();
        }
        
        if (min > maxParentDepth) {
            return -1;
        } else {
            return min;
        }
    }
    
    private float calculateAsymmetricRelationshipOperation(IndexWord start, IndexWord end, SchemaMatcher m) throws JWNLException {
		// Try to find a relationship between the first sense of <var>start</var> and the first sense of <var>end</var>
	//System.out.println("Asymetric relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");	
        if (start == null || end == null) {
            return (float) 0.0;
        }
        //System.out.println("Immediate Relationship "+RelationshipFinder.getInstance().getImmediateRelationship(start, end));
        //System.out.println("Immediate Relationship "+RelationshipFinder.getInstance().getImmediateRelationship(end, start));
        if (start.getLemma().equals(end.getLemma())) {
            m.matches.add(end.getLemma());
            return (float) 1.0;
        }
        //System.out.println("NEW WORD");
        //System.out.print(start.getLemma()+" VS "); 
        //System.out.println(end.getLemma()); 
        Synset[] setA = start.getSenses();
        Synset[] setB = end.getSenses();
        if (setA == null || setB == null) {
            return (float) 0.0;
        } 
        int min = 99999;
        
        int total = 0;
        int count = 0;
        for(Synset i : setA) {
            //System.out.println("YOLO 2 "+i);
            //System.out.println("START SYM");
            Word[] cruise = i.getWords();
            for( Word al : cruise ) {
                //System.out.print(al.getLemma()+" ");
            }
            //System.out.println();
            //System.out.println("END SYM");
            
            count++;
            for (Synset j : setB) {
                //System.out.println("Header "+i.getLexFileName()+" VS "+j.getGloss());
                //System.out.println("YOLO 3 "+j);
                int ret = scanSense(i, j);
                if (ret < 0) {
                    continue;
                }
                //System.out.println("RETURN "+ret);
                if (ret <= min) {
                    //if(ret < min)
                    //System.out.println();
                    min = ret;
                        //System.out.println("MIN "+ret);
                        //System.out.println("sim("+start.getLemma() + "," + end.getLemma()+") = "+1.0/ret);
                        //System.out.println("MIN\n"+i+"\n"+j);
                    
                    //System.out.println();
                    for (Word relevant : j.getWords())
                        m.matches.add(relevant.getLemma());
                }
                //total = ret;
            }
        }
        
        //System.out.println("END WORD "+maxParentDepth);
        if (min > maxParentDepth) {
            return (float) 0;
        } else if (min == 0) {
            return (float) 0;   
        } else {
            //return (float) (1.0f - (min/(float)maxParentDepth) );
            //System.out.println("MIN : "+min);
            //ystem.out.println("POW : "+Math.pow((float) (1.0f - (min/(float)maxParentDepth) ), raiseToPower));
            //System.out.println("THE POW : "+raiseToPower);
            return (float) Math.pow((float) (1.0f - (min/(float)(maxParentDepth+1)) ), raiseToPower);
        }
    }  
    
    public void printChains() {
        Iterator it = propertiesA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            //System.out.println(pairs.getKey() + " = " + (MetadataChain)pairs.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }
    
    public static boolean isThisMyIpAddress(InetAddress addr) {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }
    
    private String getJWNL(String pathToWordnet){
        
        String jwnlXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<jwnl_properties language=\"en\">\n" +
        "	<version publisher=\"Princeton\" number=\"3.0\" language=\"en\"/>\n" +
        "	<dictionary class=\"net.didion.jwnl.dictionary.FileBackedDictionary\">\n" +
        "		<param name=\"morphological_processor\" value=\"net.didion.jwnl.dictionary.morph.DefaultMorphologicalProcessor\">\n" +
        "			<param name=\"operations\">\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.DetachSuffixesOperation\">\n" +
        "					<param name=\"noun\" value=\"|s=|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y|\"/>\n" +
        "					<param name=\"verb\" value=\"|s=|ies=y|es=e|es=|ed=e|ed=|ing=e|ing=|\"/>\n" +
        "					<param name=\"adjective\" value=\"|er=|est=|er=e|est=e|\"/>\n" +
        "                    <param name=\"operations\">\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "                    </param>\n" +
        "				</param>\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.TokenizerOperation\">\n" +
        "					<param name=\"delimiters\">\n" +
        "						<param value=\" \"/>\n" +
        "						<param value=\"-\"/>\n" +
        "					</param>\n" +
        "					<param name=\"token_operations\">\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "						<param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "						<param value=\"net.didion.jwnl.dictionary.morph.DetachSuffixesOperation\">\n" +
        "							<param name=\"noun\" value=\"|s=|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y|\"/>\n" +
        "							<param name=\"verb\" value=\"|s=|ies=y|es=e|es=|ed=e|ed=|ing=e|ing=|\"/>\n" +
        "							<param name=\"adjective\" value=\"|er=|est=|er=e|est=e|\"/>\n" +
        "                            <param name=\"operations\">\n" +
        "                                <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "                                <param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "                            </param>\n" +
        "						</param>\n" +
        "					</param>\n" +
        "				</param>\n" +
        "			</param>\n" +
        "		</param>\n" +
        "		<param name=\"dictionary_element_factory\" value=\"net.didion.jwnl.princeton.data.PrincetonWN17FileDictionaryElementFactory\"/>\n" +
        "		<param name=\"file_manager\" value=\"net.didion.jwnl.dictionary.file_manager.FileManagerImpl\">\n" +
        "			<param name=\"file_type\" value=\"net.didion.jwnl.princeton.file.PrincetonRandomAccessDictionaryFile\"/>\n" +
        "			<param name=\"dictionary_path\" value=\""+ pathToWordnet +"\"/>\n" +
        "		</param>\n" +
        "	</dictionary>\n" +
        "	<resource class=\"PrincetonResource\"/>\n" +
        "</jwnl_properties>";
        
        return jwnlXML;
    }
    
    public void insertLinksMetadata(List<Link> links) throws SQLException, IOException{ //metadata go in the graphA, not the new one. the method is called without the fusedGraph from FuserWorker
    //keep metadata subjects according to the transformation    
        long starttime, endtime;
        createLinksGraph(links);
        String getFromB;
        createDelWGSGraph(((FileBulkLoader)trh).getDelWGSList());
        createDelGeomGraph(((FileBulkLoader)trh).getDelGeomList());
        starttime = System.nanoTime();
        
        //System.out.println(System.getProperty("user.name")); 
        //java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
        //System.out.println("Hostname of local machine: " + localMachine.getHostName());
        
        boolean isMyDesiredIp = false;
        try
        {
            isMyDesiredIp = isThisMyIpAddress(InetAddress.getByName("nicks")); //"localhost" for localhost
        }
        catch(UnknownHostException unknownHost)
        {
            //System.out.println("It is not");
        }
        //System.out.println(isMyDesiredIp);
        //String remoteDesc = "LOAD SERVICE <"+endpointB+"> DATA";
        //UpdateRequest getDesc = UpdateFactory.create(remoteDesc);
        //UpdateProcessor loadDesc = UpdateExecutionFactory.createRemoteForm(getDesc, endpointB);
        //loadDesc.execute();
        final URI u;
        try {
            u = new URI("https://help.github.com/articles/error-permission-denied-publickey");
            // URI u = new URI("/works/with/me/too");
            // URI u = new URI("/can/../do/./more/../sophis?ticated=stuff+too");
            if(u.isAbsolute())
            {
                //System.out.println("Yes, i am absolute!");
            }
            else
            {
                //System.out.println("Ohh noes, it's a relative URI!");
            }
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                LOG.info(" IP Addr: " + localhost.getHostAddress());
                // Just in case this host has multiple IP addresses....
                InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
                if (allMyIps != null && allMyIps.length > 1) {
                    LOG.info(" Full list of IP addresses:");
                    for (InetAddress allMyIp : allMyIps) {
                        LOG.info("    " + allMyIp);
                    }
                }
            } catch (UnknownHostException e) {
                LOG.info(" (error retrieving server host name)");
            }

            try {
                LOG.info("Full list of Network Interfaces:");
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    LOG.info("    " + intf.getName() + " " + intf.getDisplayName());
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        LOG.info("        " + enumIpAddr.nextElement().toString());
                    }
                }
            } catch (SocketException e) {
                LOG.info(" (error retrieving network interface list)");
            }
        } catch (URISyntaxException ex) {
                java.util.logging.Logger.getLogger(VirtuosoImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (endpointLoc.equals(endpointB)) {
            getFromB = "INSERT\n"
                    + "  { GRAPH <"+targetGraph+"> {\n"
                    + " ?s ?p ?o1 . \n"
                    + " ?o1 ?p4 ?o3 .\n"
                    + " ?o3 ?p5 ?o4\n"
                    + "} }\nWHERE\n"
                    + "{\n"
                    + " GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n"
                    + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                    + "\n"
                    + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                    + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                    + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                    + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                    + "}";
        } else {
            getFromB = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " SERVICE <"+endpointLoc+"> { GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } }\n"
                + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        }
        
        //int count = 0;
        int i = 0;
        while (i < links.size()) {
            
            createLinksGraphBatch(links, i);            
            starttime =  System.nanoTime();           
            UpdateRequest insertFromB = UpdateFactory.create(getFromB);
            UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(insertFromB, endpointB);
            insertRemoteB.execute();
        
            endtime =  System.nanoTime();
            LOG.info("Metadata parsed in "+(endtime-starttime)/1000000000f);
            i += BATCH_SIZE;
            //count++;
        }
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Metadata parsed in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }    
    
    
    private VirtGraph getVirtuosoSet (String graph, String url, String username, String password) throws SQLException {
        //Class.forName("virtuoso.jdbc4.Driver");
        VirtGraph vSet = new VirtGraph (graph, "jdbc:virtuoso://" + url + "/CHARSET=UTF-8", username, password);
        LOG.info(ANSI_YELLOW+"Virtuoso connection established."+ANSI_RESET);
        return vSet;
    }
    
    public void clean() {
        set.close();        
        LOG.info(ANSI_YELLOW+"Virtuoso import is done."+ANSI_RESET);

    }          

    private void clearBulkLoadHistory() throws SQLException {
        PreparedStatement clearBulkLoadTblStmt;
        clearBulkLoadTblStmt = virt_conn.prepareStatement(clearBulkLoadTbl);                        
        clearBulkLoadTblStmt.executeUpdate();
    }
       
    
    private void createLinksGraphBatch(List<Link> lst, int nextIndex) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+">";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/links_"+db_c.getDBName()+">";
        final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";
        
        PreparedStatement endStmt;
        endStmt = virt_conn.prepareStatement(endDesc);
        //endStmt.execute();
        
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        
        createStmt.close();
        
        //BulkInsertLinksBatch(lst, nextIndex);
        SPARQLInsertLinksBatch(lst, nextIndex);
    }
    
    public void createLinksGraph(List<Link> lst) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/all_links_"+db_c.getDBName()+">";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/all_links_"+db_c.getDBName()+">";
        final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";
        
        PreparedStatement endStmt;
        endStmt = virt_conn.prepareStatement(endDesc);
        //endStmt.execute();
        
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        
        //BulkInsertLinks(lst);
        SPARQLInsertLinks(lst);
    }
    
    private void BulkInsertLinksBatch(List<Link> lst, int nextIndex) throws SQLException, IOException {  
        long starttime, endtime;
        set2 = getVirtuosoSet("http://localhost:8890/DAV/links_"+db_c.getDBName()+"", db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
        BulkUpdateHandler buh2 = set2.getBulkUpdateHandler();
        LOG.info(ANSI_YELLOW+"Loaded "+lst.size()+" links"+ANSI_RESET);
        
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        if (f.exists()){
             f.delete();
        }  
        
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        links_graph = "http://localhost:8890/DAV/links_"+db_c.getDBName();
        String dir = bulkInsertDir.replace("\\", "/");
        //System.out.println("DIRECTORY "+dir);
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+dir+"bulk_inserts/selected_links.nt'), '', "+"'"+links_graph+"')";
        
        int i = nextIndex;
        //System.out.println(i);
        if ( lst.size() > 0 ) {
            while (i < lst.size() && i < nextIndex + BATCH_SIZE ) {
                Link link = lst.get(i);
                String triple = "<"+link.getNodeA()+"> <"+SAME_AS+"> <"+link.getNodeB()+"> .";
        
                out.println(triple);
                i++;
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        //System.out.println(i);
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Links Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
        
        starttime = System.nanoTime();
        virt_conn.commit();
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Links Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
        
    private void SPARQLInsertLinksBatch(List<Link> l, int nextIndex) {
        boolean updating = true;
        int addIdx = nextIndex;
        int cSize = 1;
        int sizeUp = 1;
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+fusedGraph+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <http://localhost:8890/DAV/links_" + db_c.getDBName() + "> { ");
                int top = 0;
                if (cSize >= l.size()) {
                    top = l.size();
                } else {
                    top = cSize;
                }
                for (int i = addIdx; i < top; i++) {
                    final String subject = l.get(i).getNodeA();
                    final String subjectB = l.get(i).getNodeB();
                    queryStr.appendIri(subject);
                    queryStr.append(" ");
                    queryStr.appendIri(SAME_AS);
                    queryStr.append(" ");
                    queryStr.appendIri(subjectB);
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                }
                queryStr.append("} }");
                //System.out.println("Print "+queryStr.toString());

                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, endpointT, authenticator);
                insertRemoteB.execute();
                
                //System.out.println("Add at "+addIdx+" Size "+cSize);
                addIdx += (cSize - addIdx);
                sizeUp *= 2;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                if (cSize == addIdx) {
                    updating = false;
                }
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println("Failed at " + addIdx + " Size " + cSize);
                System.out.println("Crazy Stuff");
                System.out.println(ex.getLocalizedMessage());
                ex.printStackTrace();
                ex.printStackTrace(System.out);
                sizeUp = 1;
                cSize = addIdx;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);

                break;
                //System.out.println("Going back at "+addIdx+" Size "+cSize);
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
                break;
            }
        }
    }
    
    private void SPARQLInsertLinks(List<Link> l) {
        boolean updating = true;
        int addIdx = 0;
        int cSize = 1;
        int sizeUp = 1;
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+fusedGraph+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <http://localhost:8890/DAV/all_links_" + db_c.getDBName() + "> { ");
                int top = 0;
                if (cSize >= l.size()) {
                    top = l.size();
                } else {
                    top = cSize;
                }
                for (int i = addIdx; i < top; i++) {
                    final String subject = l.get(i).getNodeA();
                    final String subjectB = l.get(i).getNodeB();
                    queryStr.appendIri(subject);
                    queryStr.append(" ");
                    queryStr.appendIri(SAME_AS);
                    queryStr.append(" ");
                    queryStr.appendIri(subjectB);
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                }
                queryStr.append("} }");
                    //System.out.println("Print "+queryStr.toString());

                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, endpointT, authenticator);
                //insertRemoteB.execute();
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(queryStr.toString(), set);
        
                //update_handler.addUpdate(updateQuery);
                vur.exec();
        
                //System.out.println("Add at "+addIdx+" Size "+cSize);
                addIdx += (cSize - addIdx);
                sizeUp *= 2;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                if (cSize == addIdx) {
                    updating = false;
                }
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println("Failed at " + addIdx + " Size " + cSize);
                System.out.println("Crazy Stuff");
                System.out.println(ex.getLocalizedMessage());
                ex.printStackTrace();
                ex.printStackTrace(System.out);
                sizeUp = 1;
                cSize = addIdx;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);

                break;
                //System.out.println("Going back at "+addIdx+" Size "+cSize);
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
                break;
            }
        }
    }
    
    private void BulkInsertLinks(List<Link> lst) throws SQLException, IOException {  
        set2 = getVirtuosoSet("http://localhost:8890/DAV/all_links_"+db_c.getDBName(), db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
        BulkUpdateHandler buh2 = set2.getBulkUpdateHandler();
        LOG.info(ANSI_YELLOW+"Loaded "+lst.size()+" links"+ANSI_RESET);
        long starttime, endtime;
        
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        String dir = bulkInsertDir.replace("\\", "/");
        //System.out.println("DIRECTORY "+dir);
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+dir+"bulk_inserts/selected_links.nt'), '', "
                +"'"+"http://localhost:8890/DAV/all_links_"+db_c.getDBName()+"')";
        //int stop = 0;
        if ( lst.size() > 0 ) {
            
            for(Link link : lst) {
                //if (stop++ > 1000) break;
                String triple = "<"+link.getNodeA()+"> <"+SAME_AS+"> <"+link.getNodeB()+"> .";
        
                out.println(triple);
            }
            
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Links Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
        
        starttime = System.nanoTime();

        virt_conn.commit();
        //endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Links Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    
    }
    
    private void createDelWGSGraph(List<String> lst) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/del_wgs>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/del_wgs>";
    
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/deleted_wgs.nt");
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        String dir = bulkInsertDir.replace("\\", "/");
        //System.out.println("DIRECTORY "+dir);
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+dir+"bulk_inserts/deleted_wgs.nt'), '', "+"'"+del_wgs_graph+"')";

        if ( lst.size() > 0 ) {

            for(String sub : lst) {
                String triple = "<"+sub+"> <del> <a> .";
        
                out.println(triple);
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Delete WGS Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
    
    private void createDelGeomGraph(List<String> lst) throws IOException, SQLException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/del_geom>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/del_geom>";
    
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/deleted_geom.nt");
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        String dir = bulkInsertDir.replace("\\", "/");
        //System.out.println("DIRECTORY "+dir);
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+dir+"bulk_inserts/deleted_geom.nt'), '', "+"'"+del_geom_graph+"')";

        if ( lst.size() > 0 ) {

            for(String sub : lst) {
                String triple = "<"+sub+"> <del> <"+sub+"_geom> .";
        
                out.println(triple);
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Delete Geom Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }                   
}    
