/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fusion.gis.cli;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Importer;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import static gr.athenainnovation.imis.fusion.gis.gui.FuserPanel.registerTransformations;
import static gr.athenainnovation.imis.fusion.gis.gui.FuserPanel.scoresForAllRules;
import static gr.athenainnovation.imis.fusion.gis.gui.FuserPanel.transformations;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FuseWorker;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RED;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ScoreWorker;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import gr.athenainnovation.imis.fusion.gis.postgis.ScriptRunner;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author nick
 */
public class FusionGISCLI {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FusionGISCLI.class);
    
    private class FAGILogger implements ErrorListener {

        @Override
        public void notifyError(String message) {
            System.out.println("ERROR:"+message);
        }
    
    }
    
    private static FAGILogger errListen;
    
    public static void main(String args[]) {
        List<String> lines;
        long startTime, endTime;
        String config_file;
        if (args.length != 2) {
            System.out.println(ANSI_YELLOW+"Usage: FAGI -c configFile"+ANSI_RESET);
            return;
        }
        if (args[0].equals("-c")) {
            config_file = args[1];
        } else {
            for(String a : args)
                System.out.println(a);
            System.out.println(ANSI_YELLOW+"Usage: FAGI -c configFile"+ANSI_RESET);
            return;
        }
        try {
            
            final FusionState st = new FusionState();
            
            //lines = Files.readAllLines(Paths.get("/home/nick/Projects/FAGI-gis-master/fusion.conf"), Charset.defaultCharset());
            lines = Files.readAllLines(Paths.get(config_file), Charset.defaultCharset());
            for (String line : lines) {
                if (line.startsWith("#")) {
                } else if (line.equals("")) {
                } else {
                    String [] params = line.split("=");
                    st.setFusionParam(params[0].trim(), params[1].trim());
                }
            }
            
            boolean isValid = st.checkConfiguration();
            if ( isValid ) {
                System.out.println("-- Executing following Configuration");
                LOG.info(st);
            } else {
                return;
            }
            if (st.isImported()) {
                //System.out.println("ssasasasasasa");
                final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
                databaseInitialiser.initialise(st.getDbConf());
            
                //final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConfig, PostGISImporter.DATASET_A, sourceDatasetA, datasetAStatusField, errorListener);
                Dataset sourceADataset = new Dataset(st.getGraphConf().getEndpointA(), st.getGraphConf().getGraphA(), "");
                final ImporterWorker datasetAImportWorker = new ImporterWorker(st.getDbConf(), PostGISImporter.DATASET_A, sourceADataset, null, errListen);
                datasetAImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override public void propertyChange(PropertyChangeEvent evt) {
                        if("progress".equals(evt.getPropertyName())) {
                            //System.out.println("Tom");
                        }
                    }
                });
            
                Dataset sourceBDataset = new Dataset(st.getGraphConf().getEndpointB(), st.getGraphConf().getGraphB(), "");
                final ImporterWorker datasetBImportWorker = new ImporterWorker(st.getDbConf(), PostGISImporter.DATASET_B, sourceBDataset, null, errListen);
            
                datasetBImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override public void propertyChange(PropertyChangeEvent evt) {
                        if("progress".equals(evt.getPropertyName())) {
                            //System.out.println("Tom2");
                        }
                    }
                });
            
                startTime = System.nanoTime();
                datasetAImportWorker.execute();
                datasetBImportWorker.execute();
            
                datasetAImportWorker.get();
                datasetBImportWorker.get();
                endTime = System.nanoTime();
            }
            
            //System.out.println("Time spent importing data to PostGIS "+(endTime-startTime)/1000000000f);
            ArrayList<Link> links = (ArrayList<Link>) GeometryFuser.parseLinksFile(st.getLinksFile()); 
            
            final ScoreWorker scoreWorker = new ScoreWorker(st.getTransformation(), links, st.getDbConf(), st.getThreshold());
            
            scoreWorker.execute();
            scoresForAllRules.put(st.getTransformation().getID(), scoreWorker.get());
            
            boolean createNew = !st.getDstGraph().equals(st.getGraphConf().getGraphA());
            final FuseWorker fuseWorker = new FuseWorker(st.getTransformation(), links, st.getDbConf(), st.getDstGraph(), createNew, st.getGraphConf(), null, null, errListen);

            fuseWorker.execute();
            fuseWorker.get();
        } catch (IOException ex) {
            if(ex instanceof NoSuchFileException) {
                System.out.println(ANSI_RED+args[1]+" does not exist"+ANSI_RESET);
                return;
            }
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
            SQLException exception = ex;
            while(exception.getNextException() != null) {
                Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, exception.getNextException());
                exception = exception.getNextException();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
