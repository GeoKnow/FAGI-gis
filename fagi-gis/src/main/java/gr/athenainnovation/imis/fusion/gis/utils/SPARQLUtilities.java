/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import gr.athenainnovation.imis.fusion.gis.core.Link;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;

/**
 *
 * @author Nick Vitsasvitsas
 */
public class SPARQLUtilities {
    
    private static final Logger LOG = Log.getClassFAGILogger(SPARQLUtilities.class);
    
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
    
    public static boolean updateLastAccess(String graph, Connection virt_conn) {
    
        return true;
    }
    /*
    public static boolean clearFusedLink(String sub, GraphConfig grConf, Connection virt_conn) {
        final String clearLinkFromAll =   "SPARQL DELETE WHERE {\n"
                                              + "\n"
                                              + "    GRAPH <"+grConf.getAllLinksGraph()+"> {\n"
                                              + "    <"+sub+"> <http://www.w3.org/2002/07/owl#sameAs> ?o }\n"
                                              + "    GRAPH <"+grConf.getAllClusterGraph()+"> {\n"
                                              + "    <"+sub+"> <http://www.w3.org/2002/07/owl#sameAs> ?o } \n"
                                              + "    GRAPH <"+grConf.getAllClusterGraph()+"> {\n"
                                              + "    <"+sub+"> <http://www.w3.org/2002/07/owl#sameAs> ?o } \n"
                                              + "}";
        
        System.out.println("DELETE ALL " + clearLinkFromAll);
        
        
        try (PreparedStatement dropAllClusterStmt = virt_conn.prepareStatement(dropAllCluster);
             PreparedStatement dropClusterStmt = virt_conn.prepareStatement(dropCluster);
             PreparedStatement dropLinkStmt = virt_conn.prepareStatement(dropLinks);
             PreparedStatement clearAllClusterAllLinkJoinStmt = virt_conn.prepareStatement(clearAllClusterAllLinkJoin)) {
            
            clearAllClusterAllLinkJoinStmt.execute();
            dropLinkStmt.execute();
            dropClusterStmt.execute();
            dropAllClusterStmt.execute();
            
        } catch (SQLException ex) {
            LOG.trace("Dropping fused links failed");
            LOG.debug("Dropping fused links failed");
        }
        return true;
    }
    */
    
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
}
