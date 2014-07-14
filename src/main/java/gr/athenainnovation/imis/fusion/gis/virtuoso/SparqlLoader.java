/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 *
 * @author nickvitsas
 */
public class SparqlLoader implements TripleHandler{

    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";
    private static final String DUMMY_UPDATE = "with <http://localhost:8890/fused_dataset> insert {<dummy> <dummy> <dummy>} \n" +
                                               "where { <dummy> <dummy> <dummy> }";
    private final String fusedGraph;
    private final VirtGraph set;
    
    private VirtuosoUpdateRequest update_handler;
            
    SparqlLoader(String graph, VirtGraph s) {
        this.fusedGraph = graph;
        this.update_handler = VirtuosoUpdateFactory.create(DUMMY_UPDATE, s);
        this.set = s;
    }
    
    @Override
    public void addTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTriple(Node s, Node p, Node o) {
        String object;
        String subject;
        String predicate;
           
        subject = s.getURI();
        predicate = p.getURI();
        if(o.isURI()) {
            if(o.isBlank()) {
                object = o.getLiteralLexicalForm();  
                //System.out.println(object);
                object =  "<" + object + ">";
            }
            else {
                object = o.getURI();
                //System.out.println(object);
                object = "<" + object + ">";
            }
        }
        else {
            object = o.getLiteralLexicalForm(); 
            //System.out.println(object);
            object = "\"" + object + "\"";
        }
        //System.out.println(object);
        //System.out.println();
        
        String insertMetadata = formInsertQuery(subject, predicate, object);
        executeVirtuosoUpdate(insertMetadata);
    }

    @Override
    public void addTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addGeomTriple(String subject, String fusedGeometry) {
        final String updateQuery = formInsertQuery(subject, fusedGeometry);
        executeVirtuosoUpdate(updateQuery); 
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
    public void deleteAllGeom(String subject) {
        final String deleteQuery = formDeleteGeomQuery(subject); //delete subject_a if it exists from previous transformations
        executeVirtuosoUpdate(deleteQuery);
    }

    @Override
    public void deleteAllWgs84(String subject) {
        final String deleteWgs84 = formDeleteWgs84query(subject);
        executeVirtuosoUpdate(deleteWgs84);
    }
    
    private String formDeleteWgs84query(String subject){
        return "WITH <" + fusedGraph + "> DELETE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 } WHERE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 }";
    }
    
    private String formDeleteGeomQuery(String subject){      
        return "WITH <" + fusedGraph + "> DELETE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 } WHERE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 }";   
    }
    
    //query for inserting a triple set into a virtuoso graph
    private String formInsertQuery(String subject, String fusedGeometry) { 
        return "INSERT INTO <" + fusedGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
    
    private String formInsertQuery(String subject, String predicate, String object){
        return "WITH <" + fusedGraph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
        //keep left, average of two points 
    }
    
    private void executeVirtuosoUpdate(String updateQuery) {                
        //VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, set);
        
        //VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (query, set);
        //vqe.execSelect();
        update_handler.addUpdate(updateQuery);
        //vur.exec();
    }

    @Override
    public void init() {
    }

    @Override
    public void finish() {
        update_handler.exec();
    }
}
