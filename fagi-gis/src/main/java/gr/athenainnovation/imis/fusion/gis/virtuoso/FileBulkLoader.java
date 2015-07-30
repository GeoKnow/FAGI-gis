
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.Node;
//import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
//import static gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter.getUnsignedInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 *
 * @author nickvitsas
 */
public class FileBulkLoader implements TripleHandler {
    
    //private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";
    
    //private static final Node LAT_NODE = NodeFactory.createURI(LAT);
    //private static final Node LON_NODE = NodeFactory.createURI(LON);
    //private static final Node HAS_GEO_NODE = NodeFactory.createURI(HAS_GEOMETRY);
    //private static final Node WKT_NODE = NodeFactory.createURI(HAS_GEOMETRY);
            
    private String bulk_insert;
    
    private final String fusedGraph;
    private final VirtGraph set;
    private final Connection virt_conn;
    private final PrintWriter out;
    private final StringBuilder delete_wgs;
    private final StringBuilder delete_geoms;
    private final List<String> delWGSList;
    private final List<String> delGeomList;
    private final DBConfig dConf;
    private final GraphConfig gConf;
    private UpdateRequest request;
    private VirtuosoUpdateRequest virt_req;
    private String bulkDir;
    
    FileBulkLoader(String graph, DBConfig dc, GraphConfig gc, VirtGraph s) throws IOException {
        dConf = dc;
        gConf = gc;
        this.fusedGraph = graph;
        this.set = s;
        this.bulkDir = dc.getBulkDir();
        this.virt_conn = s.getConnection();
        File theDir = new File(bulkDir+"bulk_inserts");

  // if the directory does not exist, create it
        if (!theDir.exists()) {
        //System.out.println("creating directory: " + directoryName);
        boolean result = false;

        try{
            theDir.mkdir();
                result = true;
            } catch(SecurityException se){
                //handle it
            }        
            if(result) {    
                //System.out.println("DIR created");  
            }
        }
        File f = new File(bulkDir+"bulk_inserts/load.nt");
        //f.getParentFile().mkdirs();
        out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        //f.deleteOnExit();
        delete_wgs = new StringBuilder(4000);
        delete_geoms = new StringBuilder(4000);
        bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkDir+"bulk_inserts/load.nt'), '', "+"'"+graph+"', 128)";
        request = UpdateFactory.create() ;
        virt_req = new VirtuosoUpdateRequest(set);
        delWGSList = new ArrayList<>();
        delGeomList = new ArrayList<>();
    }
    
    @Override
    public void addTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTriple(Node s, Node p, Node o) {
        //String object;
        String triple;
        if (o.isURI()) {
            triple = "<"+s+"> <"+p+"> <"+o+"> .";
        } else {
            triple = "<"+s+"> <"+p+"> \""+o.getLiteralLexicalForm().replaceAll("\"", "").replaceAll("\n", "").replaceAll("\\\\", "")+"\" .";
        }
        out.println(triple);
    }

    @Override
    public void addTriple(String s, String p, String o) {
        String triple = "<"+s+"> <"+p+"> "+o+" .";
        out.println(triple);
    }

    @Override
    public void addGeomTriple(String subject, String fusedGeometry) {
        String geomTriplets = formGeometryTriplets(subject, fusedGeometry);
        out.println(geomTriplets);
    }

    @Override
    public void deleteTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTriple(Node s, Node p, Node o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init() {
        try {
            clearBulkLoadHistory();
        } catch (SQLException ex) {
            Logger.getLogger(FileBulkLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void finish() {
        try {
            out.close();
            //System.out.println("Excuting "+request.getOperations().size());
            long starttime = System.nanoTime();
            //executeDeletes();
            long endtime =  System.nanoTime();
            //System.out.println("Deletes lasted "+(endtime-starttime)/1000000000f);
            
            starttime = System.nanoTime();
            uploadFile();
            
            endtime =  System.nanoTime();
            
            //System.out.println("Bulk Insert lasted "+(endtime-starttime)/1000000000f);
            
        } catch (SQLException ex) {
            Logger.getLogger(FileBulkLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void deleteAllGeom(String subject) {
        delGeomList.add(subject);
    }

    @Override
    public void deleteAllWgs84(String subject) {
        delWGSList.add(subject);
    }

    public List<String> getDelWGSList() {
        return delWGSList;
    }

    public List<String> getDelGeomList() {
        return delGeomList;
    }
    
    private void executeDeletes() throws SQLException {
        String delGeomQuery;
        String delWgsQuery;
        if (gConf.getEndpointLoc().equals(gConf.getEndpointA())) {
            delGeomQuery = "DELETE FROM <"+fusedGraph+"> { \n"
                + "?s <http://www.opengis.net/ont/geosparql#hasGeometry> ?a . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?o2} \n"
                + "WHERE { \n"
                + "GRAPH <http://localhost:8890/DAV/del_geom> { ?s <del> ?o } .\n"
                + "?s <http://www.opengis.net/ont/geosparql#hasGeometry> ?a . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?o2 }";
            delWgsQuery = "DELETE FROM <"+fusedGraph+"> { \n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 .\n?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 } \n"
                + "WHERE { \n"
                + "GRAPH <http://localhost:8890/DAV/del_wgs> { ?s <del> ?o } .\n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . \n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }";
            //System.out.println(delGeomQuery);
        } else {
            delGeomQuery = "DELETE FROM <"+fusedGraph+"> { \n"
                + "?s <http://www.opengis.net/ont/geosparql#hasGeometry> ?a . ?a <http://www.opengis.net/ont/geosparql#asWKT> ?o2 } \n"
                + "WHERE { \n"
                + "SERVICE <"+gConf.getEndpointLoc()+"> { GRAPH <http://localhost:8890/DAV/del_geom> { ?s <del> ?o } } .\n"
                + "?s <http://www.opengis.net/ont/geosparql#hasGeometry> ?a . ?a <http://www.opengis.net/ont/geosparql#asWKT> ?o2 }";
            delWgsQuery = "DELETE FROM <"+fusedGraph+"> { \n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 .\n?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 } \n"
                + "WHERE { \n"
                + "SERVICE <"+gConf.getEndpointLoc()+"> { GRAPH <http://localhost:8890/DAV/del_wgs> { ?s <del> ?s } } .\n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . \n"
                + "?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }";
        }
        
        VirtuosoUpdateRequest vurWgs = VirtuosoUpdateFactory.create(delWgsQuery, set);
        long starttime, endtime;
        starttime = System.nanoTime();
        vurWgs.exec();
        endtime =  System.nanoTime();
        //System.out.println(ANSI_YELLOW+"wgs parsed in "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
        VirtuosoUpdateRequest vurGeom = VirtuosoUpdateFactory.create(delGeomQuery, set);
        starttime = System.nanoTime();
        vurGeom.exec();
        endtime =  System.nanoTime();
        //System.out.println(ANSI_YELLOW+"geom parsed in "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
    }
    
    private void clearBulkLoadHistory() throws SQLException {
        PreparedStatement clearBulkLoadTblStmt;
        clearBulkLoadTblStmt = virt_conn.prepareStatement(clearBulkLoadTbl);                        
        clearBulkLoadTblStmt.executeUpdate();
    }
    
    private void uploadFile () throws SQLException {
        PreparedStatement uploadBulkFileStmt;
        uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
        uploadBulkFileStmt.executeUpdate();
    }
    
    private String formGeometryTriplets(String subject, String fusedGeometry) {
        /*Node s1 = NodeFactory.createAnon();
        return "<"+subject+">"+" <"+HAS_GEOMETRY+"> <"+s1.getBlankNodeLabel()+"> .\n"
                  + "<" + s1.getBlankNodeLabel()+"> <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> .";      */
        return "<"+subject+">"+" <"+HAS_GEOMETRY+"> <"+subject+"_geom"+"> .\n"
                  + "<" + subject+"_geom"+"> <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> ."; 
    }
    
    private String formDeleteWgs84query(String subject){
        return "WITH <" + fusedGraph + "> DELETE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 } WHERE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 }";
    }
    
    private String formDeleteGeomQuery(String subject){      
        return "WITH <" + fusedGraph + "> DELETE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 } WHERE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 }";   
    }
    
}
