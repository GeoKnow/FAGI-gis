

package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.logging.Level;
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
import org.apache.log4j.Logger;
import virtuoso.jena.driver.VirtGraph;

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
    private static final String links_graph = "http://localhost:8890/DAV/links";
    private static final String del_wgs_graph = "http://localhost:8890/DAV/del_wgs";
    private static final String del_geom_graph = "http://localhost:8890/DAV/del_geom";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";    
    private static final String PATH_TO_WORDNET = "/usr/share/wordnet";
    private static final int BATCH_SIZE = 10000;
    private static final int SAMPLE_SIZE = 10;   
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
    private final String targetGraph;   
    public final VirtGraph set;
    public VirtGraph set2;
    //private final VirtGraph setA;     
    //private final VirtGraph setB;     
    public DBConfig db_c;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;

           
    
    public VirtuosoImporter(final DBConfig dbConfig, String transformationID, final String fusedGraph, final boolean checkboxIsSelected, final GraphConfig graphConfig) throws SQLException, IOException, JWNLException {
        
        db_c = dbConfig;
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
        //long startTime = System.nanoTime();
        set = getVirtuosoSet(fusedGraph, dbConfig.getDBURL(), dbConfig.getUsername(), dbConfig.getPassword());
        //long endTime = System.nanoTime();
        //System.out.println("Time to connect : "+(endTime-startTime)/1000000000f);       
        virt_conn = set.getConnection();        
        bulkInsertDir = dbConfig.getBulkDir();       
        trh = new FileBulkLoader(fusedGraph, dbConfig, graphConfig, set);
        trh.init();
        
    }
    
    public String getTransformationID() {
        return transformationID;
    }

    public void setTransformationID(String transformationID) {
        this.transformationID = transformationID;
    }
 
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso(final String fusedGraph) {   //imports the geometries in the fused graph                     
        
        Statement stmt = null;     
        Connection connection = null;
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
                
                //List<Triple> lst = new ArrayList<>();
                int p = 0;
                while(rs.next()) {
                    subject = rs.getString("subject_a");
                    fusedGeometry = rs.getString("ST_AsText");
                    if (!(transformationID.equals("Keep both"))){
                        //if transformation is NOT "keep both" -> delete previous geometry
                        
                        trh.deleteAllWgs84(subject);
                        trh.deleteAllGeom(subject);
                        trh.addGeomTriple(subject, fusedGeometry);
                        //System.out.println(subject + " " + fusedGeometry);
                    }
                    else {
                        if (rs.isFirst()){
                            trh.deleteAllGeom(subject);                           
                            trh.addGeomTriple(subject, fusedGeometry);
                        }
                        else {
                            //insert second geometry
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
        
    public void insertLinksMetadataChains(List<Link> links, final String fusedGraph) throws SQLException, IOException, InterruptedException, JWNLException{ //metadata go in the new fusedGraph. the method is called from FuserWorker 
    //keep metadata subjects according to the transformation                    //selects from graph B, inserts in fused  graph
        long starttime, endtime;
        createLinksGraph(links);
        createDelWGSGraph(((FileBulkLoader)trh).getDelWGSList());
        createDelGeomGraph(((FileBulkLoader)trh).getDelGeomList());
        StringBuilder getFromB = new StringBuilder();
        StringBuilder getFromA = new StringBuilder();
        final String dropMetaAGraph = "sparql DROP SILENT GRAPH <"+targetGraph+"metadataA"+">";
        final String dropMetaBGraph = "sparql DROP SILENT GRAPH <"+targetGraph+"metadataB"+">";
        final String createMetaAGraph = "sparql CREATE GRAPH <"+targetGraph+"metadataA"+">";
        final String createMetaBGraph = "sparql CREATE GRAPH <"+targetGraph+"metadataB"+">";
        
        PreparedStatement tempStmt;
        tempStmt = virt_conn.prepareStatement(dropMetaAGraph);
        tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(dropMetaBGraph);
        tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(createMetaAGraph);
        tempStmt.execute();
        tempStmt = virt_conn.prepareStatement(createMetaBGraph);
        tempStmt.execute();
        
        starttime =  System.nanoTime();
        //testThreads(links);
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Thread test lasted "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
        
        starttime =  System.nanoTime();
        String endpointLoc2 = endpointA;
        
        if (endpointLoc2.equals(endpointA)) {
            getFromA.append("INSERT\n");
            getFromA.append("  { GRAPH <").append(targetGraph).append("metadataA"+"> {\n");
            getFromA.append(" ?s ?p ?o1 . \n");
            getFromA.append(" ?o1 ?p4 ?o3 .\n");
            getFromA.append(" ?o3 ?p5 ?o4 .\n");
            getFromA.append(" ?o4 ?p6 ?o5\n");
            getFromA.append("} }\nWHERE\n");
            getFromA.append("{\n");
            getFromA.append(" GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> _:a } .\n");
            getFromA.append(" GRAPH <").append(graphA).append("> { {?s ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} OPTIONAL { ?o4 ?p6 ?o5 .} }\n");
            getFromA.append("\n");
            getFromA.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromA.append("}");
            //System.out.println(getFromA);
        }
        
        if (endpointLoc2.equals(endpointB)) {
            getFromB.append("INSERT\n");
            getFromB.append("  { GRAPH <").append(targetGraph).append("metadataB"+"> {\n");
            getFromB.append(" ?s ?p ?o1 . \n");
            getFromB.append(" ?o1 ?p4 ?o3 .\n");
            getFromB.append(" ?o3 ?p5 ?o4 .\n");
            getFromB.append(" ?o4 ?p6 ?o5\n");
            getFromB.append("} }\nWHERE\n");
            getFromB.append("{\n");
            getFromB.append(" GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n");
            getFromB.append(" GRAPH <").append(graphB).append("> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} OPTIONAL { ?o4 ?p6 ?o5 .} }\n");
            getFromB.append("\n");
            getFromB.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromB.append("}");

        }        
        //System.out.println("GET FROM B \n"+getFromB);
        
        int count = 0;
        int i = 0;
        
        while (i < links.size()) {
            
            createLinksGraphBatch(links, i);
            
            starttime =  System.nanoTime();
            
            UpdateRequest insertFromA = UpdateFactory.create(getFromA.toString());
            UpdateProcessor insertRemoteA = UpdateExecutionFactory.createRemoteForm(insertFromA, endpointA);
            insertRemoteA.execute();
        
            UpdateRequest insertFromB = UpdateFactory.create(getFromB.toString());
            UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(insertFromB, endpointB);
            insertRemoteB.execute();
            
            scanProperties(1);

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
    static HashMap<String, HashSet<String>> foundA = Maps.newHashMap();
    static HashMap<String, HashSet<String>> foundB = Maps.newHashMap();
    
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
    public int compareTypes(String l, String r) {
        if (patternInt.matcher(l).find() && patternInt.matcher(r).find()) return 1;
        if (patternDate.matcher(l).find() && patternDate.matcher(r).find()) return 1;
        if (patternText.matcher(l).find() && patternText.matcher(r).find()) return 1;
        if (patternDecimal.matcher(l).find() && patternDecimal.matcher(r).find()) return 1;
        if (patternWord.matcher(l).find() && patternWord.matcher(r).find()) return 1;

        return 0;
    }
    
    private void scanMatches() throws JWNLException, FileNotFoundException, IOException {
        /*Iterator it = propertiesA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            sb.append(pairs.getKey() + " = " + (MetadataChain)pairs.getValue()+"\n");
            it.remove(); // avoids a ConcurrentModificationException
        }
        */
        
        //System.out.println("Schema Match "+tom++);
        HashMap<String, Schema> schemasA = Maps.newHashMap();
        expandChain(schemasA, propertiesA, "");  
        //System.out.println();
        HashMap<String, Schema> schemasB = Maps.newHashMap();
        expandChain(schemasB, propertiesB, "");    
        for (Map.Entry pairs : schemasA.entrySet()) {
            String chain = (String)pairs.getKey();
            Schema sche = (Schema)pairs.getValue();
            //System.out.println("A "+chain+" "+sche.objectStr);
        }
        for (Map.Entry pairs : schemasB.entrySet()) {
            String chain = (String)pairs.getKey();
            Schema sche = (Schema)pairs.getValue();
            //System.out.println("B "+chain+" "+sche.objectStr);
        }
        
        List<SchemaMatcher> matchers = new ArrayList<>();
        int countA;
        int countB;
        float score;
        float sim;
        
        for (Map.Entry pairsA : schemasA.entrySet()) {
            //String chainA = (String)pairsA.getKey();
            Schema scheA = (Schema)pairsA.getValue();
            for (Map.Entry pairsB : schemasB.entrySet()) {
                //String chainB = (String)pairsB.getKey();
                Schema scheB = (Schema)pairsB.getValue();
                score = 0;
                sim = 0;
                
                if (scheA.indexes.isEmpty())
                    continue;
                if (scheB.indexes.isEmpty())
                    continue;
                
                SchemaMatcher m =  new SchemaMatcher(scheA, scheB);
                countA = 0;
                //System.out.println("Scanning ");
                for ( IndexWord iwA : scheA.indexes) {
                    //System.out.print(iwA.getLemma()+" ");
                }
                //System.out.print(" with ");
                for ( IndexWord iwB : scheB.indexes) {
                    //System.out.print(iwB.getLemma()+" ");
                }
                
                float jaro_dist = 0;
                float jaro_dist_norm;
                int jaroCount = 0;
                for ( IndexWord iwA : scheA.indexes) {
                    countA++;
                    countB = 0;
                    for ( IndexWord iwB : scheB.indexes) {
                        countB++;
                        float tmpScore = calculateAsymmetricRelationshipOperation(iwA, iwB, m);
                        //System.out.println("Score "+tmpScore);
                        score += tmpScore;
                        jaroCount++;
                        //calculateSymmetricRelationshipOperation(iwA, iwB);
                        //System.out.println("Score : " + score);
                        jaro_dist += (float) StringUtils.getJaroWinklerDistance(iwA.getLemma(), iwB.getLemma());
                    }
                }
                
                jaro_dist_norm = jaro_dist / (jaroCount);
                //float sim_score = (score+jaro_dist) / 2;
                /*
                if (sim_score > 0.5) {
                    int same_type = compareTypes(scheA.objectStr, scheB.objectStr);
                    System.out.println("Sim Score : "+sim_score+" "+same_type);
                    System.out.println("Type Score : "+scheA.objectStr+" and "+scheB.objectStr+" "+sim_score+" "+same_type);
                    System.out.println("Jaro distance of "+jaro_dist_norm);
                    System.out.println("Mul : "+scheA.indexes.size()+" x "+scheB.indexes.size() + " = "+(scheA.indexes.size()*scheB.indexes.size()));
                    System.out.println("Final : " + (score+jaro_dist_norm)/2);

                }*/
                if(!m.matches.isEmpty()){
                    matchers.add(m);
                }    
            }
        }
        
        //System.out.println("Matches "+matchers.size());
        //for (SchemaMatcher ma : matchers) {
            //System.out.println(ma);
        //}
        //System.out.println("Total Matches "+foundA.size());
        for (SchemaMatcher ma : matchers) {
            if ( ! ma.matches.isEmpty() ) {
                //if (!foundA.containsKey(ma.sA.predicate)) {
                    HashSet<String> matchesA = foundA.get(ma.sA.predicate);
                    HashSet<String> matchesB = foundB.get(ma.sB.predicate);
                    if (matchesA == null) {
                        matchesA = Sets.newHashSet();
                        foundA.put(ma.sA.predicate, matchesA);
                    }
                    if (matchesB == null) {
                        matchesB = Sets.newHashSet();
                        foundB.put(ma.sB.predicate, matchesB);
                    }
                    matchesA.add(ma.sB.predicate);
                    matchesB.add(ma.sA.predicate);
                //}
                //System.out.println("Matched "+ma.sA.predicate+" with "+ ma.sB.predicate);
            }
        }
    }
    
    private void expandChain(HashMap<String, Schema> lst, HashMap< String, MetadataChain > chains, String chainStr) throws JWNLException, FileNotFoundException, IOException {

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
            String[] arrl = StringUtils.splitByCharacterTypeCamelCase(chain.link);
            Schema m = new Schema();
            m.predicate = pred;
            m.objectStr= chain.objectStr;
            //System.out.println("breaking "+chain.link);
            for (String a : arrl) {
                if(a.equals("_")) 
                    continue;
                if (a.equalsIgnoreCase("has"))
                    continue;
            
                //System.out.print("Value "+a+" ");
                m.addWord(a);
                IndexWordSet wordSet = dictionary.lookupAllIndexWords(a);
                if (wordSet == null)
                    continue;
                IndexWord[] indices = wordSet.getIndexWordArray();
                IndexWord best = null;
                int bestInt = 0;
                for (IndexWord idx : indices) { 
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
                
                if (best == null)
                    continue;
            
                m.addIndex(best);
            }
            //System.out.println(best.getPOS().getLabel());
            //System.out.println();

            //System.out.println("Inserting predicate: "+ pred);
            lst.put(pred, m);
        }
    }
    
    private void scanChain(HashMap< String, MetadataChain > cont, List <String> chain, List <String> objectChain) {
        //System.out.print("Chain: ");
        if (chain.isEmpty())
            return;
        
        MetadataChain root = null;
        String link = chain.get(0);
        if ( cont.containsKey(link) ) {
            root = cont.get(link);
        } else {
            
            if (link == null) {
                //System.out.println("null");
                return;
            }
            
            String main = StringUtils.substringAfter(link, "#");
            if (main.equals("") ) {
                main = StringUtils.substring(link, StringUtils.lastIndexOf(link, "/")+1);
            }
            //System.out.println(main);
            
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
            
            MetadataChain newChain = new MetadataChain(main, link, objectChain.get(0));
            root.addCahin(newChain);
            root = newChain;
            //System.out.print(link+" ");
        }
        
        //System.out.println(root);
    }
    
    public SchemaMatchState scanProperties(int optDepth) throws SQLException, JWNLException, FileNotFoundException, IOException {
        //optDepth = 2;
        JWNL.initialize(new ByteArrayInputStream(getJWNL(PATH_TO_WORDNET).getBytes(StandardCharsets.UTF_8)));
        
        final String dropSamplesGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/links_sample>";
        final String createSamplesGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/links_sample>";
        final String createLinksSample ="SPARQL INSERT\n"+
                                  " { GRAPH <http://localhost:8890/DAV/links_sample> {\n"+
                                  " ?s ?p ?o\n"+
                                  "} }\n"+
                                  "WHERE\n"+
                                  "{\n"+
                                  "{\n"+
                                  "SELECT ?s ?p ?o WHERE {\n"+
                                  " GRAPH <http://localhost:8890/DAV/links> { ?s ?p ?o }\n"+
                                  "} limit "+SAMPLE_SIZE+"\n"+
                                  "}}";
        
        PreparedStatement dropSamplesStmt;
        dropSamplesStmt = virt_conn.prepareStatement(dropSamplesGraph);
        dropSamplesStmt.execute();
        
        PreparedStatement createSamplesStmt;
        createSamplesStmt = virt_conn.prepareStatement(createSamplesGraph);
        createSamplesStmt.execute();
        
        PreparedStatement insertLinksSample;
        insertLinksSample = virt_conn.prepareStatement(createLinksSample);
        insertLinksSample.execute();
            
        //for (int i = optDepth; i >= 0; i--) {
        for (int i = 0; i < optDepth+1; i++) {
            //System.out.println("DEPTH: "+i);
            StringBuilder query = new StringBuilder();
            query.append("sparql SELECT distinct(?s) ?pa1 ?oa1 ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                int prev = ind - 1;
                query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
            }
            query.append("?pb1 ?ob1 ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
            }
            query.append(" WHERE \n {\n");
            query.append("SELECT ?s ?pa1 ?oa1 ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                int prev = ind - 1;
                query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
            }
            query.append("?pb1 ?ob1 ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                int prev = ind - 1;
                query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
            }
            query.append("\nWHERE\n{\n  GRAPH <http://localhost:8890/DAV/links_sample> { ?s ?p ?o }\n" 
                    + " { GRAPH <").append(targetGraph).append("metadataA> { {?s ?pa1 ?oa1} ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                int prev = ind - 1;
                query.append("OPTIONAL {?oa").append(prev).append(" ?pa").append(ind).append(" ?oa").append(ind).append(" } ");
            }
            query.append("}\n } UNION { \n" + "   GRAPH <").append(targetGraph).append("metadataB> { {?s ?pb1 ?ob1} ");
            for(int j = 0; j < i; j++) {
                int ind = j+2;
                int prev = ind - 1;
                query.append("OPTIONAL {?ob").append(prev).append(" ?pb").append(ind).append(" ?ob").append(ind).append(" } ");
            }
            query.append("} }\n"
                    + "}\n"
                    + "} ORDER BY (?s)");
            
            //System.out.println(query.toString());
            
            PreparedStatement fetchProperties;
            fetchProperties = virt_conn.prepareStatement(query.toString());
            ResultSet propertiesRS = fetchProperties.executeQuery();
            
            String prevSubject = "";
            while(propertiesRS.next()) {
                final String subject = propertiesRS.getString(1);
                //propertiesRS.
                if (!prevSubject.equals(subject) && !prevSubject.equals("")) {
                    //if (i == optDepth) {
                        System.out.println(subject);
                        scanMatches();
                        propertiesA.clear();
                        propertiesB.clear();
                    //}
                }               
                //System.out.println(subject);
                List <String> chainA = new ArrayList<>();
                List <String> chainB = new ArrayList<>();
                List <String> objectChainA = new ArrayList<>();
                List <String> objectChainB = new ArrayList<>();
                for(int j = 0; j <= i; j++) {
                    //int ind = j+2;
                    //int prev = ind - 1;
                    int step_over = 2*(i+1);
                    
                    String predicateA = propertiesRS.getString(2*(j+1));
                    String objectA = propertiesRS.getString(2*(j+1)+1);
                    String predicateB = propertiesRS.getString(2*(j+1)+step_over);
                    String objectB = propertiesRS.getString(2*(j+1)+1+step_over);
                    /*if (objectA != null) {
                        System.out.println("Object A "+objectA+" "+patternInt.asPredicate().test(objectA));
                    }
                    if (objectB != null) {
                        System.out.println("Object B "+objectB+" "+patternInt.asPredicate().test(objectB));
                    }*/
                    chainA.add(predicateA);
                    objectChainA.add(objectA);
                    chainB.add(predicateB);
                    objectChainB.add(objectB);
                    
                    /*                   
                    System.out.println(pattern.pattern());
                    System.out.println(pattern.asPredicate().test("asasds dsafdasd"));
                    System.out.println(pattern.asPredicate().test("sadas 123123"));
                    System.out.println(pattern.asPredicate().test("1321 cdgd 4554"));
                    System.out.println(pattern.asPredicate().test("132.12"));
                    System.out.println(pattern.asPredicate().test("132,4"));*/
                    /*SchemaMatcher matcher = 
                    pattern.matcher("01/01/1884");

                    boolean foundA = false;
                    while (matcher.find()) {
                        System.out.printf("I foundA the text" +
                            " \"%s\" starting at " +
                            "index %d and ending at index %d.%n",
                            matcher.group(),
                            matcher.start(),
                         matcher.end());
                        foundA = true;
                    }
                    if(!foundA){
                        System.out.println("No match foundA.%n");
                    }*/
            
                    //System.out.println(" "+predicateA+" "+objectA);
                    //System.out.println(" "+predicateB+" "+objectB);
                }
                //System.out.println("ChainA\n");
                //for (String s1 : chainA) 
                //    System.out.println(s1);
                //System.out.println("ChainB\n");
                //for (String s1 : chainB) 
                //    System.out.println(s1);
                scanChain(propertiesA, chainA, objectChainA);
                scanChain(propertiesB, chainB, objectChainB);
                /*fullPropertyListA.add(predicate);
                String main = StringUtils.substringAfter(predicate, "#");
                if (main.equals("") ) {
                    main = StringUtils.substring(predicate, StringUtils.lastIndexOf(predicate, "/")+1);
                }
                System.out.println(main);
                propertyListA.add(StringUtils.splitByCharacterTypeCamelCase(main));*/
                                
                prevSubject = subject;
            }
            
            scanMatches();
            propertiesA.clear();
            propertiesB.clear();
                        
            //System.out.println(query2.toString());
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
        return new SchemaMatchState(foundA, foundB);
    }
    
    private int scanSense(Synset i, Synset j) throws JWNLException {
        //RelationshipList list = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM);
        //RelationshipList listLvl1 = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM, 1);
        RelationshipList listLvl2 = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM, 5);
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
        
        if (min > 2) {
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
        
        //System.out.println("END WORD "+min);
        if (min > 3) {
            return (float) 0;
        } else if (min == 0) {
            return (float) 0;   
        } else {
            //System.out.println("WOOOOOODY "+min);
            return (float) (1.0/min);
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
            System.out.println("It is not");
        }
        System.out.println(isMyDesiredIp);
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
                System.out.println("Ohh noes, it's a relative URI!");
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
                    + " GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n"
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
                + " SERVICE <"+endpointLoc+"> { GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } }\n"
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
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/links>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/links>";
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
        
        set2 = getVirtuosoSet("http://localhost:8890/DAV/links", db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
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
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/selected_links.nt'), '', "+"'"+links_graph+"')";
        
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
    
    public void createLinksGraph(List<Link> lst) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/all_links>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/all_links>";
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
        
        set2 = getVirtuosoSet("http://localhost:8890/DAV/all_links", db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
        BulkUpdateHandler buh2 = set2.getBulkUpdateHandler();
        LOG.info(ANSI_YELLOW+"Loaded "+lst.size()+" links"+ANSI_RESET);
        
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/selected_links.nt'), '', "
                +"'"+"http://localhost:8890/DAV/all_links"+"')";
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
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/deleted_wgs.nt'), '', "+"'"+del_wgs_graph+"')";

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
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/deleted_geom.nt'), '', "+"'"+del_geom_graph+"')";

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