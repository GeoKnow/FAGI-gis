package gr.athenainnovation.imis.fusion.gis.core;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation;
import static gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation.WGS84_SRID;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;


/**
 * Provides methods for obtaining RDF links, scoring and then applying fusion transformations against them.
 * @author Thomas Maroulis
 */
public class GeometryFuser {
    private static final Logger LOG = Logger.getLogger(GeometryFuser.class);
    
    private static final String DB_URL = "jdbc:postgresql:";
    
    private Connection connection;
    
    public void loadLinks(final List<Link> links) throws SQLException {
        
        //delete old geometries from fused_geometries table. 
        try{
           String deleteLinksTable = "DELETE FROM links";
           PreparedStatement statement = connection.prepareStatement(deleteLinksTable); 
           statement.executeUpdate(); //jan 
           
           connection.commit();
           
        }
        catch (SQLException ex)
        {
          connection.rollback();  
          LOG.warn(ex.getMessage(), ex);
        }
        String insertLinkQuery = "INSERT INTO links (nodea, nodeb) VALUES (?,?)"; 
        final PreparedStatement insertLinkStmt = connection.prepareStatement(insertLinkQuery);
        
        for(Link link : links) {         
            insertLinkStmt.setString(1, link.getNodeA());
            insertLinkStmt.setString(2, link.getNodeB());
            
            insertLinkStmt.addBatch();
        }    
        insertLinkStmt.executeBatch();
        connection.commit();
    }
    
    public void fuseAll(final AbstractFusionTransformation transformation) throws SQLException {
            
    }
    
    /**
     * Apply given fusion transformation on list of links.
     * @param transformation fusion transformation
     * @param links list of links
     * @throws SQLException
     */
    public void fuse(final AbstractFusionTransformation transformation, final List<Link> links) throws SQLException {
        
        //delete old geometries from fused_geometries table. 
        try{           
            //delete all data in fused_geometries table. keep table for the new geometries
           String deleteFusedGeometriesTable = "DELETE FROM fused_geometries";
           PreparedStatement statement = connection.prepareStatement(deleteFusedGeometriesTable); 
           statement.executeUpdate(); //jan 
           //connection.commit();
           
        }
        catch (SQLException ex)
        {
          connection.rollback();  
          LOG.warn(ex.getMessage(), ex);
        }
        
        
        for(Link link : links) {
            //transformation.fuse(connection, link.getNodeA(), link.getNodeB());
        }    
        transformation.fuseAll(connection);
        connection.commit();
    }
    
    /**
     * Score given fusion transformation for each link in list.
     * @param transformation fusion transformation
     * @param links list of links
     * @param threshold threshold
     * @return map with score results (value) for each link (key)
     * @throws SQLException
     */
    public Map<String, Double> score(final AbstractFusionTransformation transformation, final List<Link> links, Double threshold) throws SQLException {
        Map<String, Double> scores = new HashMap<>();
        //System.out.println("asasasa");
        for(Link link : links) {
            scores.put(link.getKey(), transformation.score(connection, link.getNodeA(), link.getNodeB(),threshold));
        }
        //System.out.println("asasasa");
        return scores;
    }
    
    /**
     * Parses given RDF link file.
     * @param linksFile link file
     * @return list of links
     * @throws ParseException if link file contains invalid links
     */
    public static List<Link> parseLinksFile(final String linksFile) throws ParseException {
        List<Link> output = new ArrayList<>();
        
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, linksFile);
        final StmtIterator iter = model.listStatements();
        
        while(iter.hasNext()) {
            final Statement statement = iter.nextStatement();
            final String nodeA = statement.getSubject().getURI();
            //System.out.println(nodeA);
            //System.in.r;
            final String nodeB;
            final RDFNode object = statement.getObject();           
            if(object.isResource()) {
                nodeB = object.asResource().getURI();
                //System.out.println(nodeB);
            }
            else {
                throw new ParseException("Failed to parse link (object not a resource): " + statement.toString(), 0);
            }
            Link l = new Link(nodeA, nodeB);
            output.add(l);
        }
        return output;       
    }
    
    /**
     * Connect to the database
     * @param dbConfig database configuration
     * @throws SQLException 
     */
    public void connect(final DBConfig dbConfig) throws SQLException {
        final String url = DB_URL.concat(dbConfig.getDBName());
        connection = DriverManager.getConnection(url, dbConfig.getDBUsername(), dbConfig.getDBPassword());
        connection.setAutoCommit(false);
        LOG.info(ANSI_YELLOW+"Connection to db established."+ANSI_RESET);
    }
    
    /**
     * Clean-up. Close held resources.
     */
    public void clean() {
        try {
            if(connection != null) {
                connection.close();
            }
            
            LOG.info(ANSI_YELLOW+"Database connection closed."+ANSI_RESET);
        }
        catch (SQLException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
    }
}