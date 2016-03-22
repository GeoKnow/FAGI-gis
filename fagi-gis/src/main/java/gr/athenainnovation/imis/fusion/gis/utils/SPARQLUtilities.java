/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONClusterLink;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsasvitsas
 */
public class SPARQLUtilities {
    
    private static final Logger LOG = Log.getClassFAGILogger(SPARQLUtilities.class);
    
    // Various Geometry Query Factories
    public static String formInsertQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + Constants.HAS_GEOMETRY + "> _:a . _:a <" + Constants.WKT + "> \"" + fusedGeometry + "\"^^<"+Constants.WKT_LITERAL_REGEX+"> }";
    }
    
    public static String formInsertGeomQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + Constants.HAS_GEOMETRY + "> <" + subject + "_geom> . <" + subject +"_geom> <" + Constants.WKT + "> \"" + fusedGeometry + "\"^^<"+Constants.GEOMETRY_TYPE_REGEX+"> }";
    }
    
    public static String formInsertConcatGeomQuery(String tGraph, String subject, String geomA, String geomB) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + Constants.HAS_GEOMETRY + "> <" + subject + "_geom> . <" + subject +"_geom> <" + Constants.WKT + "> \"" + "GEOMETRYCOLLECTION("+geomA+", "+geomB+")" + "\"^^<"+Constants.GEOMETRY_TYPE_REGEX+"> }";
    }
    
    public static String formInsertQuery(String tGraph, String subject, String predicate, String object){
        return "WITH <" + tGraph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
    }
    
    public static boolean createSPARQLUser(Connection virt_conn, String name, String pass) {
        boolean success = true;
        final String queryCreateUser = "DB.DBA.USER_CREATE ('"+name+"', '"+pass+"')";
        //final String queryCreateUser = "DB.DBA.USER_CREATE ('FAGI', 'FAGI')";
        final String queryGrantSparUL = "GRANT SPARQL_UPDATE TO \""+name+"\"";
        //final String queryGrantSparUL = "GRANT SPARQL_UPDATE TO \"FAGI\"";

        System.out.println("Excecuting " + queryCreateUser );
        System.out.println("Excecuting " + queryGrantSparUL );
        try (PreparedStatement createUserStmt = virt_conn.prepareStatement(queryCreateUser);
             PreparedStatement grantSparULStmt = virt_conn.prepareStatement(queryGrantSparUL)) {

            virt_conn.setAutoCommit(false);
            System.out.println("Excecuting");
            createUserStmt.execute();
            grantSparULStmt.execute();
            virt_conn.commit();
            virt_conn.setAutoCommit(true);
            
        } catch (SQLException ex) {
            LOG.trace("Creating user failed");
            LOG.debug("Creating user failed : " + ex.getMessage());
            
            success = false;
        }

        if ( !success ) {
            try {
                virt_conn.rollback();
            } catch (SQLException ex) {
                LOG.trace("Rollback user creation failed");
                LOG.debug("Rollback user failed : " + ex.getMessage());
            }
        }
        
        return success;
    }

    public static boolean createSPARQLUserNamedGraph(Connection virt_conn, String name, String graphName) {
        boolean success = true;
        
        final String querySetUserPerms = "DB.DBA.RDF_DEFAULT_USER_PERMS_SET (?, 0)";
        final String querySetUserGraphPerms = "DB.DBA.RDF_GRAPH_USER_PERMS_SET (?, ?, 3)";

        System.out.println("Excecuting " + querySetUserPerms );
        System.out.println("Excecuting " + querySetUserGraphPerms );
        try (PreparedStatement setUserPermsStmt = virt_conn.prepareStatement(querySetUserPerms);
                PreparedStatement setUserGraphPermsStmt = virt_conn.prepareStatement(querySetUserGraphPerms)) {

            System.out.println("Excecuting inside" );
            virt_conn.setAutoCommit(false);
            
            setUserGraphPermsStmt.setString(1, graphName);
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            
            virt_conn.commit();
            virt_conn.setAutoCommit(true);
            
        } catch (SQLException ex) {
            LOG.trace("Creating user graph failed");
            LOG.debug("Creating user graph failed");
            
            success = false;
        }

        if ( !success ) {
            try {
                virt_conn.rollback();
            } catch (SQLException ex) {
                LOG.trace("Rollback user graph creation failed");
                LOG.debug("Rollback user graph creation failed");
            }
        }
        
        return success;
    }
    
    public static boolean createSPARQLUserGraph(GraphConfig grConf, Connection virt_conn, String name, String graphName) {
        boolean success = true;
        
        final String querySetUserPerms = "DB.DBA.RDF_DEFAULT_USER_PERMS_SET (?, 0)";
        final String querySetUserGraphPerms = "DB.DBA.RDF_GRAPH_USER_PERMS_SET (?, ?, 3)";

        System.out.println("Excecuting " + querySetUserPerms );
        System.out.println("Excecuting " + querySetUserGraphPerms );
        try (PreparedStatement setUserPermsStmt = virt_conn.prepareStatement(querySetUserPerms);
                PreparedStatement setUserGraphPermsStmt = virt_conn.prepareStatement(querySetUserGraphPerms)) {

            System.out.println("Excecuting inside" );
            virt_conn.setAutoCommit(false);
            //setUserPermsStmt.execute();
            //setUserPermsStmt.setString(1, name);
            setUserGraphPermsStmt.setString(1, grConf.getAllClusterGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getLinksGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getAllLinksGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getMetadataGraphA());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getMetadataGraphB());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getSampleLinksGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getTargetTempGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            setUserGraphPermsStmt.setString(1, grConf.getTargetGraph());
            setUserGraphPermsStmt.setString(2, name);
            setUserGraphPermsStmt.execute();
            
            virt_conn.commit();
            virt_conn.setAutoCommit(true);
            
        } catch (SQLException ex) {
            LOG.trace("Creating user graph failed");
            LOG.debug("Creating user graph failed");
            
            success = false;
        }

        if ( !success ) {
            try {
                virt_conn.rollback();
            } catch (SQLException ex) {
                LOG.trace("Rollback user graph creation failed");
                LOG.debug("Rollback user graph creation failed");
            }
        }
        
        return success;
    }
    
    public static boolean clearFusedLinks(GraphConfig grConf, int activeCluster, Connection virt_conn) {
        final String dropCluster = "SPARQL DROP SILENT GRAPH <"+ grConf.getClusterGraph()+  ">";
        final String dropAllCluster = "SPARQL DROP SILENT GRAPH <"+ grConf.getAllClusterGraph()+  ">";
        final String dropLinks = "SPARQL DROP SILENT GRAPH <"+ grConf.getLinksGraph()+  ">";
        final String dropAllLinks = "SPARQL DROP SILENT GRAPH <"+ grConf.getAllLinksGraph()+  ">";
        final String clearAllClusterAllLinkJoin =   "SPARQL DELETE WHERE {\n"
                                                    + "\n"
                                                    + "    GRAPH <"+grConf.getAllLinksGraph()+"> {\n"
                                                    + "    ?s <http://www.w3.org/2002/07/owl#sameAs> ?o }\n"
                                                    + "    GRAPH <"+grConf.getAllClusterGraph()+"> {\n"
                                                    + "    ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } \n"
                                                    + "}";
        
        System.out.println("DELETE ALL " + clearAllClusterAllLinkJoin);
        
        if (activeCluster > 0) {
            try (PreparedStatement dropAllClusterStmt = virt_conn.prepareStatement(dropAllCluster);
                 PreparedStatement dropClusterStmt = virt_conn.prepareStatement(dropCluster);
                 PreparedStatement clearAllClusterAllLinkJoinStmt = virt_conn.prepareStatement(clearAllClusterAllLinkJoin)) {

                clearAllClusterAllLinkJoinStmt.execute();
                dropClusterStmt.execute();
                dropAllClusterStmt.execute();

            } catch (SQLException ex) {
                LOG.trace("Dropping fused links failed");
                LOG.debug("Dropping fused links failed");
            }
        } else {
            try (PreparedStatement dropLinkStmt = virt_conn.prepareStatement(dropLinks);
                 PreparedStatement dropAllLinkStmt = virt_conn.prepareStatement(dropAllLinks)) {

                dropLinkStmt.execute();
                dropAllLinkStmt.execute();

            } catch (SQLException ex) {
                LOG.trace("Dropping fused links failed");
                LOG.debug("Dropping fused links failed");
            }
        }
        
        return true;
    }
    
    public static boolean clearFusedLink(String link, GraphConfig grConf, Connection virt_conn) {
        final String clearLink =   "SPARQL DELETE WHERE {\n"
                                                    + "\n"
                                                    + "    GRAPH <"+grConf.getAllLinksGraph()+"> {\n"
                                                    + "    <"+link+"> <http://www.w3.org/2002/07/owl#sameAs> ?o }\n"
                                                    + "    GRAPH <"+grConf.getAllClusterGraph()+"> {\n"
                                                    + "    <"+link+"> <http://www.w3.org/2002/07/owl#sameAs> ?o } \n"
                                                    + "}";
        
        System.out.println("DELETE ALL OF LINK " + clearLink);
        
        try (PreparedStatement clearLinkStmt = virt_conn.prepareStatement(clearLink)) {

            clearLinkStmt.execute();

        } catch (SQLException ex) {
            LOG.trace("Dropping fused links failed");
            LOG.debug("Dropping fused links failed");
        }

        
        return true;
    }
    
    public static boolean updateLastAccess(String graph, Connection virt_conn) {
    
        return true;
    }
    
    public static boolean createLinksGraph(List<Link> lst, String linkGraph, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) {
        final String dropGraph = "SPARQL DROP SILENT GRAPH <"+ linkGraph+  ">";
        final String createGraph = "SPARQL CREATE GRAPH <"+ linkGraph+  ">";
        
        boolean success = true;

        //final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";

        //PreparedStatement endStmt;
        //endStmt = virt_conn.prepareStatement(endDesc);
        //endStmt.execute();
        
        long starttime, endtime;
        try (PreparedStatement dropStmt = virt_conn.prepareStatement(dropGraph) ) {
            dropStmt.execute();
            dropStmt.close();
            
        } catch (SQLException ex) {
            LOG.trace("Dropping "+linkGraph+" failed");
            LOG.debug("Dropping "+linkGraph+" failed");
            
            success = false;
            return success;
        }

        try (PreparedStatement createStmt = virt_conn.prepareStatement(createGraph)){
            createStmt.execute();
            createStmt.close();
            
        } catch (SQLException ex) {
            LOG.trace("Creating "+linkGraph+" failed");
            LOG.debug("Creating "+linkGraph+" failed");
            
            success = false;
            return success;
        }
        
        
        //bulkInsertLinks(lst, virt_conn, bulkInsertDir);
        success = SPARQLInsertLink(lst, linkGraph, grConf, virt_conn);
        
        return success;
    }

    private static boolean SPARQLInsertLink(List<Link> l, String linkGraph, GraphConfig grConf, Connection virt_conn) {
        boolean success = true;
        StringBuilder sb = new StringBuilder();
        
        VirtuosoConnection conn = (VirtuosoConnection) virt_conn;
        sb.append("SPARQL WITH <" +linkGraph + "> INSERT {");
        sb.append("`iri(??)` <" + Constants.SAME_AS + "> `iri(??)` . } ");
        try ( VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(sb.toString());) {
            System.out.println("Statement " + sb.toString());
            int start = 0;
            int end = l.size();

            for (int i = start; i < end; ++i) {
                Link link = l.get(i);
                vstmt.setString(1, link.getNodeA());
                vstmt.setString(2, link.getNodeB());

                vstmt.addBatch();
            }

            vstmt.executeBatch();
        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on "+linkGraph+" failed");
            LOG.debug("VirtuosoException on "+linkGraph+" failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on "+linkGraph+" failed : " + ex.getSQLState());
            
            success = false;
            return success;
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException on "+linkGraph+" failed");
            LOG.debug("BatchUpdateException on "+linkGraph+" failed : " + ex.getMessage());
            LOG.debug("BatchUpdateException on "+linkGraph+" failed : " + ex.getSQLState());
            
            success = false;
            return success;
        }
        
        return success;
    }

    private static boolean bulkInsertLinks(List<Link> lst, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) {
        long starttime, endtime;
        /*
         set2 = getVirtuosoSet("+ grConf.getAllLinksGraph()+ , db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
         BulkUpdateHandler buh2 = set2.getBulkUpdateHandler();
        LOG.info(ANSI_YELLOW + "Loaded " + lst.size() + " links" + ANSI_RESET);

        starttime = System.nanoTime();
        System.out.println("FILE " + bulkInsertDir + "bulk_inserts" + File.separator + "selected_links.nt");
        //File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        //PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(bulkInsertDir+"bulk_inserts/selected_links.nt")));
        String dir = bulkInsertDir.replace("\\", "/");
        System.out.println("DIR " + dir);
        //dir = "/"+dir;
        //dir = dir.replace(":","");
        PrintWriter out = new PrintWriter(bulkInsertDir + "bulk_inserts/selected_links.nt");
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('" + dir + "bulk_inserts/selected_links.nt'), '', "
                + "'" + grConf.getAllLinksGraph()+ "')";
        //int stop = 0;
        if (lst.size() > 0) {

            for (Link link : lst) {
                //if (stop++ > 1000) break;
                String triple = "<" + link.getNodeA() + "> <" + Constants.SAME_AS + "> <" + link.getNodeB() + "> .";

                out.println(triple);
            }
            out.close();

            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);
            uploadBulkFileStmt.executeUpdate();
        }

        endtime = System.nanoTime();
        LOG.info(ANSI_YELLOW + "Links Graph created in " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);

        starttime = System.nanoTime();

        virt_conn.commit();
        //endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW + "Links Graph created in " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);
                */
        
        return true;
    }
    
    /**
     * Uppdates the specified remote graph.
     * FAGI uses a faster SPARQL 1.1 operation if the
     * process is happening locally
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @return success
     */
    public static boolean UpdateRemoteEndpoint(GraphConfig grConf, VirtGraph vSet) {
       boolean isTargetEndpointLocal = Utilities.isURLToLocalInstance(grConf.getTargetGraph());

        if ( isTargetEndpointLocal ) {
            System.out.println("\n\n\n\nSPARQL");
            LocalUpdateGraphs(grConf, vSet);
        } else {
            System.out.println("\n\n\n\nSPARQL");
            SPARQLUpdateRemoteEndpoint(grConf, vSet);
        }
        
        clearGraph(grConf.getTargetTempGraph(), vSet);
        
        return true;
    }
    
    /**
     * Local update of theremote graph with SPARUL ADD
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @return success
     */
    public static boolean LocalUpdateGraphs(GraphConfig grConf, VirtGraph vSet) {
        String addNewTriples = "SPARQL ADD GRAPH <" + grConf.getTargetTempGraph() + "> TO GRAPH <" + grConf.getTargetGraph()+ ">";
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        boolean success = true;
        
        try ( VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(addNewTriples) ) {
            vstmt.executeUpdate();
        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on remote failed");
            LOG.debug("VirtuosoException on remote failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on remote failed : " + ex.getSQLState());
            
            success = false;
        }
        
        return success;
    }
    
    /**
     * Clear a local graph
     * @param g Named Graph
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @return success=
     */
    public static boolean clearGraph(String g, VirtGraph vSet) {
        boolean success = true;
        final String dropGraph = "SPARQL DROP SILENT GRAPH <"+g+ ">";
        final String createGraph = "SPARQL CREATE GRAPH <"+g+ ">";
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();

        long starttime, endtime;
        try (VirtuosoPreparedStatement dropStmt = (VirtuosoPreparedStatement) conn.prepareStatement(dropGraph);
             VirtuosoPreparedStatement createStmt = (VirtuosoPreparedStatement) conn.prepareStatement(createGraph)) {

            dropStmt.execute();
            createStmt.execute();

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on temp target delete");
            LOG.debug("VirtuosoException on temp target delete : " + ex.getMessage());
        }

        return success;
    }
   
    /**
     * Remote update through concatenated SPARQL INSERTs
     * @param grConf Graph Configuration fro the request
     * @param vSet JENA VirtGraph connection to local Virtuoso instance
     * @return success=
     */
    public static boolean SPARQLUpdateRemoteEndpoint(GraphConfig grConf, VirtGraph vSet) {
        String selectURITriples = "SPARQL SELECT * WHERE { GRAPH <"+grConf.getTargetTempGraph()+"> { ?s ?p ?o FILTER ( isURI ( ?o ) ) } }";
        String selectLiteralTriples = "SPARQL SELECT * WHERE { GRAPH <"+grConf.getTargetTempGraph()+"> { ?s ?p ?o FILTER ( isLiteral ( ?o ) ) } }";
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        
        boolean success = true;
        boolean updating = true;
        int addIdx = 0;
        int cSize = 1;
        int sizeUp = 1;
        int tries = 0;
        
        // As long as there is data this loop creates concatenated SPARQL INSERTs
        // to update the remote endpoint
        // Iy uses the SPARQL HTTP protocol for issuing SPARQL commands
        // on relies on HTTP Exceptions to reissue the inserts
        
        try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(selectURITriples);
                 VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery()) {
            System.out.println("PUMPA\n\n\n\n\n\n");
            while (vrs.next()) {
                
                final String sub = vrs.getString(1);
                final String pre = vrs.getString(2);
                final String obj = vrs.getString(3);
                
                System.out.println(sub+" "+pre+" "+obj+" ");
                
            }
            System.out.println("PUMPA\n\n\n\n\n\n");
                        
        } catch (VirtuosoException ex) {
        }
        
        while (tries < Constants.MAX_SPARQL_TRIES) {
            try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(selectURITriples);
                 VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery()) {
                // Different loop for URIs to ease creation of query
                while (updating) {
                    try {
                        ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                        //queryStr.append("WITH <"+grConf.getTargetGraph()+"> ");
                        queryStr.append("INSERT DATA { ");
                        queryStr.append("GRAPH <" + grConf.getTargetGraph() + "> {");

                        if (!vrs.next()) {
                            break;
                        }

                        String sub = vrs.getString(1);
                        String pre = vrs.getString(2);
                        String obj = vrs.getString(3);

                        queryStr.appendIri(sub);
                        queryStr.append(" ");
                        queryStr.appendIri(pre);
                        queryStr.append(" ");
                        queryStr.appendIri(obj); // !!!!! URI
                        queryStr.append(" ");
                        queryStr.append(".");
                        queryStr.append(" ");
                            
                        for (int i = 0; i < cSize; i++) {
                            if (!vrs.next()) {
                                updating = false;
                                break;
                            }
                            
                            sub = vrs.getString(1);
                            pre = vrs.getString(2);
                            obj = vrs.getString(3);

                            queryStr.appendIri(sub);
                            queryStr.append(" ");
                            queryStr.appendIri(pre);
                            queryStr.append(" ");
                            queryStr.appendIri(obj); // !!!!! URI
                            queryStr.append(" ");
                            queryStr.append(".");
                            queryStr.append(" ");
                        }

                        queryStr.append("} }");

                        System.out.println("The insertion query takes this form " + queryStr.toString());

                        cSize *= 2;

                        UpdateRequest q = queryStr.asUpdate();
                        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                        UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                        insertRemoteB.execute();
                    } catch (org.apache.jena.atlas.web.HttpException ex) {
                        System.out.println(ex.getMessage());
                        cSize = 0;
                    }
                    System.out.println("ALLOU");
                }
            } catch (VirtuosoException ex) {
                System.out.println(ex.getMessage());
                tries++;
            }
            
            break;
        }
        
        if ( tries == Constants.MAX_SPARQL_TRIES ) {
            success = false;
            return success;
        }
        
        updating = true;
        addIdx = 0;
        cSize = 1;
        sizeUp = 1;
        tries = 0;
        
        while (tries < Constants.MAX_SPARQL_TRIES) {
            try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(selectLiteralTriples);
                 VirtuosoResultSet vrs = (VirtuosoResultSet) vstmt.executeQuery()) {
                // Different loop for Literal to ease creation of query
                while (updating) {
                    try {
                        ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                        //queryStr.append("WITH <"+grConf.getTargetGraph()+"> ");
                        queryStr.append("INSERT DATA { ");
                        queryStr.append("GRAPH <" + grConf.getTargetGraph() + "> {");

                        if (!vrs.next()) {
                            break;
                        }

                        String sub = vrs.getString(1);
                        String pre = vrs.getString(2);
                        String obj = vrs.getString(3);

                        queryStr.appendIri(sub);
                        queryStr.append(" ");
                        queryStr.appendIri(pre);
                        queryStr.append(" ");
                        queryStr.appendLiteral(obj); // !!!!!! Literal
                        queryStr.append(" ");
                        queryStr.append(".");
                        queryStr.append(" ");

                        for (int i = 0; i < cSize; i++) {
                            if (!vrs.next()) {
                                updating = false;
                                break;
                            }

                            sub = vrs.getString(1);
                            pre = vrs.getString(2);
                            obj = vrs.getString(3);

                            queryStr.appendIri(sub);
                            queryStr.append(" ");
                            queryStr.appendIri(pre);
                            queryStr.append(" ");
                            queryStr.appendLiteral(obj); // !!!!!! Literal
                            queryStr.append(" ");
                            queryStr.append(".");
                            queryStr.append(" ");
                        }

                        queryStr.append("} }");

                        System.out.println("The insertion query takes this form " + queryStr.toString());

                        cSize *= 2;

                        UpdateRequest q = queryStr.asUpdate();
                        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                        UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                        insertRemoteB.execute();
                    } catch (org.apache.jena.atlas.web.HttpException ex) {
                        System.out.println(ex.getMessage());
                        cSize = 0;
                    }
                    System.out.println("ALLOU 2");
                }
                
                break;
            } catch (VirtuosoException ex) {
                System.out.println(ex.getMessage());
                tries++;
            }
            
                break;
        }
        
        if ( tries == Constants.MAX_SPARQL_TRIES ) {
            success = false;
            return success;
        }
        
        return success;
    }
    
    public static boolean validateDataset(String endpoint, String graph) {
        boolean valid = false;
        
        String check = "SELECT * WHERE { GRAPH <" + graph + "> {?s ?p ?o } } LIMIT 1";
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        
        try (QueryEngineHTTP qeh = QueryExecutionFactory.createServiceRequest(endpoint, QueryFactory.create(check), authenticator)) {

            com.hp.hpl.jena.query.ResultSet resultSet = qeh.execSelect();

            if (resultSet.hasNext()) {
                valid = true;
            }
        } catch (QueryException qex) {
            LOG.trace("QueryException thrown during dataset" + "(" + endpoint + "," + graph + ")" + " validation");
            LOG.debug("QueryException thrown during dataset" + "(" + endpoint + "," + graph + ")" + " validation : \n" + qex.getMessage());

            valid = false;
        }
        
        System.out.println("Is valid " + valid);
        
        return valid;
    }
    
    public static boolean isInputTarget(GraphConfig grConf) {
        if ( grConf.getEndpointT().equalsIgnoreCase(grConf.getEndpointA()) && grConf.getGraphA().equalsIgnoreCase(grConf.getTargetGraph()) )
            return true;
        
        if ( grConf.getEndpointT().equalsIgnoreCase(grConf.getEndpointB()) && grConf.getGraphB().equalsIgnoreCase(grConf.getTargetGraph()) )
            return true;
        
        return false;
    }
    
    public static boolean clearMetadataGraphs(VirtGraph vSet, GraphConfig gr_c) {
        final VirtuosoConnection virt_conn = (VirtuosoConnection) vSet.getConnection();
        final String dropMetaAGraph = "SPARQL DROP SILENT GRAPH <" + gr_c.getMetadataGraphA() + ">";
        final String dropMetaBGraph = "SPARQL DROP SILENT GRAPH <" + gr_c.getMetadataGraphB() + ">";
        final String createMetaAGraph = "SPARQL CREATE GRAPH <" + gr_c.getMetadataGraphA() + ">";
        final String createMetaBGraph = "SPARQL CREATE GRAPH <" + gr_c.getMetadataGraphB() + ">";

        try (PreparedStatement dropMetaAStmt = virt_conn.prepareStatement(dropMetaAGraph);
                PreparedStatement dropMetaBStmt = virt_conn.prepareStatement(dropMetaBGraph);
                PreparedStatement createMetaAStmt = virt_conn.prepareStatement(createMetaAGraph);
                PreparedStatement createMetaBStmt = virt_conn.prepareStatement(createMetaBGraph)) {

            System.out.println("INSIDE META CREATION");
            dropMetaAStmt.execute();
            System.out.println("INSIDE META CREATION 1");
            dropMetaBStmt.execute();
            System.out.println("INSIDE META CREATION 2");
            createMetaAStmt.execute();
            System.out.println("INSIDE META CREATION 3");
            createMetaBStmt.execute();
            System.out.println("OUTSIDE META CREATION");

        } catch (SQLException ex) {

            LOG.trace("SQLException thrown during temp graph creation");
            LOG.debug("SQLException thrown during temp graph creation : " + ex.getMessage());
            LOG.debug("SQLException thrown during temp graph creation : " + ex.getSQLState());

        }
        
        return true;
    }
    
    public static int createLinksGraphBatch(List<Link> lst, int nextIndex, GraphConfig grConf, VirtGraph vSet) {
        final String dropGraph = "SPARQL DROP SILENT GRAPH <"+grConf.getLinksGraph()+ ">";
        final String createGraph = "SPARQL CREATE GRAPH <"+grConf.getLinksGraph()+ ">";
        final VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        long starttime, endtime;

        try (VirtuosoPreparedStatement dropStmt = (VirtuosoPreparedStatement) conn.prepareStatement(dropGraph); 
             VirtuosoPreparedStatement createStmt = (VirtuosoPreparedStatement) conn.prepareStatement(createGraph)) {

            dropStmt.execute();            
            createStmt.execute();

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException during link graph creation");
            LOG.debug("VirtuosoException during link graph creation : " + ex.getMessage());
        }
        //BulkInsertLinksBatch(lst, nextIndex);
        return SPARQLInsertLinksBatch(lst, nextIndex, grConf, vSet);
    }
    
    /**
     * Bulk Insert a batch of Links thrugh SPARQL
     * @param l
     * @param lst  A List of Link objects.
     * @param grConf
     * @param nextIndex Offset in the list
     * @return 
     * @throws virtuoso.jdbc4.VirtuosoException 
     */
    public static int SPARQLInsertLinksBatch(List<Link> l, int nextIndex, GraphConfig grConf, VirtGraph vSet) {
        final VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        StringBuilder sb = new StringBuilder();
        sb.append("SPARQL WITH <"+grConf.getLinksGraph()+ "> INSERT {");
        sb.append("`iri(??)` <"+Constants.SAME_AS+"> `iri(??)` . } ");
        System.out.println("Statement " + sb.toString());
        
        int start = nextIndex;
        int end = nextIndex + Constants.BATCH_SIZE;
        if ( end > l.size() ) {
            end = l.size();
        }
        
        try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(sb.toString())) {

            for (int i = start; i < end; ++i) {
                Link link = l.get(i);
                vstmt.setString(1, link.getNodeA());
                vstmt.setString(2, link.getNodeB());

                vstmt.addBatch();
            }

            vstmt.executeBatch();

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException during link graph creation");
            LOG.debug("VirtuosoException during link graph creation : " + ex.getMessage());
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException during link graph creation");
            LOG.debug("BatchUpdateException during link graph creation : " + ex.getMessage());
        }
        
        if ( end == l.size() )
            return 0;
        else 
            return end;
            
    }

    public static int createClusterGraph(JSONClusterLink[] cluster, int startIndex, GraphConfig grConf, VirtGraph vSet) {
        StringBuilder sb = new StringBuilder();
        final String dropGraph = "SPARQL DROP SILENT GRAPH <"+ grConf.getClusterGraph()+  ">";
        final String createGraph = "SPARQL CREATE GRAPH <"+ grConf.getClusterGraph()+ ">";
        
        sb.append("SPARQL WITH <" + grConf.getClusterGraph() + "> INSERT {");
        sb.append("`iri(??)` <" + Constants.SAME_AS + "> `iri(??)` . } ");
        System.out.println("Statement " + sb.toString());
            
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        int start = startIndex;
        int end = startIndex + Constants.BATCH_SIZE;
        if ( end > cluster.length ) {
            end = cluster.length;
        }
        
        try (VirtuosoPreparedStatement dropStmt = (VirtuosoPreparedStatement) conn.prepareStatement(dropGraph);
                VirtuosoPreparedStatement createStmt = (VirtuosoPreparedStatement) conn.prepareStatement(createGraph);
                VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(sb.toString());) {

            dropStmt.execute();
            createStmt.execute();

            for (int i = start; i < end; ++i) {
                JSONClusterLink link = cluster[i];
                vstmt.setString(1, link.getNodeA());
                vstmt.setString(2, link.getNodeB());

                vstmt.addBatch();
            }

            vstmt.executeBatch();

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException during link graph creation");
            LOG.debug("VirtuosoException during link graph creation : " + ex.getMessage());
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException during link graph creation");
            LOG.debug("BatchUpdateException during link graph creation : " + ex.getMessage());
        }
        
        if ( end == cluster.length )
            return 0;
        else 
            return end;

    }
    
    public static void createClusterGraph(JSONClusterLink[] cluster, DBConfig dbConf, VirtGraph vSet) {
        StringBuilder sb = new StringBuilder();
        sb.append("SPARQL WITH <http://localhost:8890/DAV/all_cluster_" + dbConf.getDBName()+"> INSERT {");
        sb.append("`iri(??)` <"+Constants.SAME_AS+"> `iri(??)` . } ");
        System.out.println("Statement " + sb.toString());
        VirtuosoConnection conn = (VirtuosoConnection) vSet.getConnection();
        
        try (VirtuosoPreparedStatement vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(sb.toString());) {

            int start = 0;
            int end = cluster.length;

            for (int i = start; i < end; ++i) {
                JSONClusterLink link = cluster[i];
                vstmt.setString(1, link.getNodeA());
                vstmt.setString(2, link.getNodeB());

                vstmt.addBatch();
            }

            vstmt.executeBatch();
        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException during link graph creation");
            LOG.debug("VirtuosoException during link graph creation : " + ex.getMessage());
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException during link graph creation");
            LOG.debug("BatchUpdateException during link graph creation : " + ex.getMessage());
        }
    }
    
    public static int getGraphDepth(String g, String e) {
        int depth = 0;
        boolean notEmpty = true;
        
        while (depth < Constants.MAX_METADATA_DEPTH && notEmpty) {
            StringBuilder queryString = new StringBuilder();
            queryString.append("ASK WHERE { ");
            String prev_s = "?s";
            for ( int i = 0; i < ( ( Constants.MAX_METADATA_DEPTH - depth ) - 1 ); i++ ) {
                queryString.append(prev_s + " ?p"+i+" ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            queryString.append(prev_s + " " + "?p"+Constants.MAX_METADATA_DEPTH + " _:a");
            queryString.append(" }");

            System.out.println(queryString);
            //final String queryString = "SELECT ?os WHERE { ?os ?p1 _:a . _:a <http://www.opengis.net/ont/geosparql#asWKT> ?g } LIMIT 1";
            QueryEngineHTTP qeh = null;
            try {
                final Query query = QueryFactory.create(queryString.toString());
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                //queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
                System.out.println("source endpoint: " + e + " query: " + query + "sourceGraph: " + g);

                qeh = QueryExecutionFactory.createServiceRequest(e, query, authenticator);
                qeh.addDefaultGraph(g);
                //QueryExecution queryExecution = qeh;
                qeh.setSelectContentType(QueryEngineHTTP.supportedAskContentTypes[3]);
                boolean rs = qeh.execAsk();

                if ( rs )
                    return Constants.MAX_METADATA_DEPTH - depth;
                
                depth++;
                System.out.println("WKT depth ------- " + depth + " has WKT " + rs);
            } catch (HttpException ex) {
                LOG.trace("HttpException during geometry fetch");
                LOG.debug("HttpException during geometry fetch : " + ex.getMessage());

                depth++;
            } catch (JenaException ex) {
                LOG.trace("JenaException during geometry fetch");
                LOG.debug("JenaException during geometry fetch : " + ex.getMessage());

                depth++;
           } finally {
                if (qeh != null) {
                    qeh.close();
                }
            }
        }
        
        return depth;
    }
}