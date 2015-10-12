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
 * @author nickvitsas
 */
public class SPARQLUtilities {
    
    private static final Logger LOG = Log.getClassFAGILogger(SPARQLUtilities.class);
    
    public static boolean createLinksGraph(List<Link> lst, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) {
        final String dropGraph = "SPARQL DROP SILENT GRAPH <"+ grConf.getAllLinksGraph()+  ">";
        final String createGraph = "SPARQL CREATE GRAPH <"+ grConf.getAllLinksGraph()+  ">";
        
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
            LOG.trace("Dropping "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("Dropping "+grConf.getAllLinksGraph()+" failed");
            
            success = false;
            return success;
        }

        try (PreparedStatement createStmt = virt_conn.prepareStatement(createGraph)){
            createStmt.execute();
            createStmt.close();
            
        } catch (SQLException ex) {
            LOG.trace("Creating "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("Creating "+grConf.getAllLinksGraph()+" failed");
            
            success = false;
            return success;
        }
        
        
        //bulkInsertLinks(lst, virt_conn, bulkInsertDir);
        success = SPARQLInsertLink(lst, grConf, virt_conn);
        
        return success;
    }

    private static boolean SPARQLInsertLink(List<Link> l, GraphConfig grConf, Connection virt_conn) {
        boolean success = true;
        StringBuilder sb = new StringBuilder();
        
        VirtuosoConnection conn = (VirtuosoConnection) virt_conn;
        sb.append("SPARQL WITH <" + grConf.getAllLinksGraph() + "> INSERT {");
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
            LOG.trace("VirtuosoException on "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("VirtuosoException on "+grConf.getAllLinksGraph()+" failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on "+grConf.getAllLinksGraph()+" failed : " + ex.getSQLState());
            
            success = false;
            return success;
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed : " + ex.getMessage());
            LOG.debug("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed : " + ex.getSQLState());
            
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
