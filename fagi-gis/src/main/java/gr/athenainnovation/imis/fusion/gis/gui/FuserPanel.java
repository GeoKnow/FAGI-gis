package gr.athenainnovation.imis.fusion.gis.gui;

import static com.google.common.base.Preconditions.*;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.DBConfigListener;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FuseWorker;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ScoreWorker;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AvgTwoPointsTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.Concatenation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepBothTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepLeftTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepMostPointsAndTranslateTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepMostPointsTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepRightTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ScaleTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftAToB;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftBToA;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftPolygonToAverageDistance;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftPolygonToPoint;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import org.apache.log4j.Logger;

 
/**
 * Handles application of fusion transformations.
 */
public class FuserPanel extends javax.swing.JPanel implements DBConfigListener {
    private static final Logger LOG = Logger.getLogger(FuserPanel.class);
    
    private final ErrorListener errorListener;
    private DBConfig dbConfig;
    private GraphConfig graphConfig;
    public static Map<String, AbstractFusionTransformation> transformations;
    static {
        registerTransformations();
    }
    
    public static Map<String, String> meta_transformations;
    public static Map<String, Map<String, Double>> scoresForAllRules;
    private List<Link> links;
    
    private boolean dbConfigSet = false;
    private boolean linksSet = false;
    private boolean busy = false;
    private Double threshold; 
    private String fusedGraph;


    
    /**
     * Creates new form FuserPanel
     * @param errorListener error message listener
     */
    public FuserPanel(final ErrorListener errorListener) {
        super();
        this.errorListener = errorListener;
        initComponents();        
        transformations = new HashMap<>();
        //registerTransformations();
        displayTransformations();    
        scoresForAllRules = new HashMap<>();
    }
    
    @Override
    public void notifyNewGraphConfiguration(final GraphConfig graphConfig) {
        this.graphConfig = graphConfig;

    }
    
    @Override
    public void notifyNewDBConfiguration(final DBConfig dbConfig) {
        this.dbConfig = dbConfig;
        dbConfigIsSet(true);
    }
    
    @Override
    public void resetDBConfiguration() {
        dbConfigIsSet(false);
    }
    
    private void dbConfigIsSet(final boolean dbConfigSet) {
        this.dbConfigSet = dbConfigSet;
        setFieldsEnabled();
    }
    
    private void linksAreSet(final boolean linksSet) {
        this.linksSet = linksSet;
        setFieldsEnabled();
    }
    
    public void setBusy(final boolean busy) {
        this.busy = busy;
        setFieldsEnabled();
    }
    
    private void setFieldsEnabled() {
        final boolean enabled = dbConfigSet && linksSet && !busy;
        fuseButton.setEnabled(enabled);
        scoreButton.setEnabled(enabled);
        selectAllLinks.setEnabled(enabled);
        thresholdField.setEnabled(enabled);
    }
    
    // Registers all implemented transformations
    public static void registerTransformations() {
        if (transformations == null ) 
            transformations = new HashMap<>();
        if (meta_transformations == null ) 
            meta_transformations = new HashMap<>();
        if (scoresForAllRules == null)
            scoresForAllRules = new HashMap<>();
        
        KeepLeftTransformation keepLeftTransformation = new KeepLeftTransformation();
        transformations.put(keepLeftTransformation.getID(), keepLeftTransformation);
        
        KeepRightTransformation keepRightTransformation = new KeepRightTransformation();
        transformations.put(keepRightTransformation.getID(), keepRightTransformation);
        
        KeepMostPointsTransformation keepMostPointsTransformation = new KeepMostPointsTransformation();
        transformations.put(keepMostPointsTransformation.getID(), keepMostPointsTransformation);
        
        KeepMostPointsAndTranslateTransformation keepMostPointsAndTranslateTransformation = new KeepMostPointsAndTranslateTransformation();
        //transformations.put(keepMostPointsAndTranslateTransformation.getID(), keepMostPointsAndTranslateTransformation);
        
        AvgTwoPointsTransformation avgTwoPointsTransformation = new AvgTwoPointsTransformation();
        //transformations.put(avgTwoPointsTransformation.getID(), avgTwoPointsTransformation);
        
        KeepBothTransformation keepBothTransformation = new KeepBothTransformation();
        transformations.put(keepBothTransformation.getID(), keepBothTransformation);
               
        //polygon test
        ShiftPolygonToAverageDistance AverageOfPointAndPolygon = new ShiftPolygonToAverageDistance();
        //transformations.put(AverageOfPointAndPolygon.getID(), AverageOfPointAndPolygon);
        
        ShiftPolygonToPoint ShiftPolygonToPoint = new ShiftPolygonToPoint();
        //transformations.put(ShiftPolygonToPoint.getID(), ShiftPolygonToPoint);
                
        ScaleTransformation scaleTransformation = new ScaleTransformation();
        //transformations.put(scaleTransformation.getID(), scaleTransformation);
        
        ShiftBToA ShiftBToA = new ShiftBToA();
        transformations.put(ShiftBToA.getID(), ShiftBToA);
        
        ShiftAToB ShiftAToB = new ShiftAToB();
        transformations.put(ShiftAToB.getID(), ShiftAToB);
             
        Concatenation concatenation = new Concatenation();
        transformations.put(concatenation.getID(), concatenation);
        
        meta_transformations.put("None", "None");
        meta_transformations.put("Keep A","Keep A");
        meta_transformations.put("Keep B","Keep B");
        meta_transformations.put("Keep Both","Keep Both");
        meta_transformations.put("Keep Concatenated A","Keep Concatenated A");
        meta_transformations.put("Keep Concatenated B","Keep Concatenated B");
        meta_transformations.put("Keep Concatenated Both","Keep Concatenated Both");
        meta_transformations.put("Keep Flattened A","Keep Flattened A");
        meta_transformations.put("Keep Flattened B","Keep Flattened B");
        meta_transformations.put("Keep Flattened Both","Keep Flattened Both");
        meta_transformations.put("Concatenation","Concatenation");
        
    }
    
    private void displayTransformations() {
        final String[] transformationKeys = transformations.keySet().toArray(new String[transformations.keySet().size()]);
        final ComboBoxModel<String> model = new DefaultComboBoxModel<>(transformationKeys);
        transformationComboBox.setModel(model);
    }
    
    private void displayLinks(final String transformation) {
        setLinkListModel();  
        if(transformation != null && scoresForAllRules.get(transformation) != null) {
            linkList.setCellRenderer(new CustomLinkListRenderer(scoresForAllRules.get(transformation)));
            
            //if scoresForAllRules got values, set the scores of transformation in the list
            setScoreLinkListModel(transformation);

        }
        else {
            linkList.setCellRenderer(new CustomLinkListRenderer());
            setScoreLinkListModel(transformation);
        }
    }
    
    private void displayLinks() {        
        setLinkListModel();
        linkList.setCellRenderer(new CustomLinkListRenderer());
    }
    
    //adds the score of each link to the score list model for displaying
    private void setScoreLinkListModel(final String transformation){
        
        final DefaultListModel<String> model = new DefaultListModel<>();
            if (scoresForAllRules.get(transformation) != null){

                Collection allScores = scoresForAllRules.get(transformation).values();

                    for (Object singleScore : allScores ){                       
                        //before add, check and sync the elements
                        model.addElement("Score:  " + singleScore.toString());                       
                        
                    }                      
                scoreLinkList.setModel(model);
            }
            else {
                //if transformation has not been scored yet, clear the model
                model.clear();
                scoreLinkList.setModel(model);
            }               
        
    }   
    
    //adds the links in the list model for display       
    private void setLinkListModel() {
        final DefaultListModel<String> model = new DefaultListModel<>(); 
        
            //clear the model to ensure the list cleared if the user changed the linkfile           
            model.clear();
            
            for(Link link : links) {                               
                model.addElement(link.getKey()); 
            }        
        
             linkList.setModel(model);
 
            //iteration to retrieve the right sequence of the links from scoresForAllRules, for displaying.             
            for (Entry<String, Map<String,Double>> entry: scoresForAllRules.entrySet()){

                model.clear();
                for(Entry<String, Double> entry1 : entry.getValue().entrySet()){
                    model.addElement(entry1.getKey());
                }
       
            }
        linkList.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        linksFileField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        linkFileChooserButton = new javax.swing.JButton();
        loadLinksButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        fuseButton = new javax.swing.JButton();
        scoreButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        linkList = new javax.swing.JList();
        transformationComboBox = new javax.swing.JComboBox();
        statusField = new javax.swing.JLabel();
        thresholdField = new javax.swing.JTextField();
        thresholdLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        scoreLinkList = new javax.swing.JList();
        selectAllLinks = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        fusedGraphField = new javax.swing.JTextField();
        fusedGraphCheckbox = new javax.swing.JCheckBox();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("RDF links"));

        jLabel4.setText("Links file:");

        linkFileChooserButton.setText("...");
        linkFileChooserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linkFileChooserButtonActionPerformed(evt);
            }
        });

        loadLinksButton.setText("Load links");
        loadLinksButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadLinksButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(linksFileField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(linkFileChooserButton, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(loadLinksButton)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(linksFileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(linkFileChooserButton)
                    .addComponent(jLabel4)
                    .addComponent(loadLinksButton))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Fuser"));

        jLabel1.setText("Select transformation:");

        fuseButton.setText("Fuse");
        fuseButton.setEnabled(false);
        fuseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fuseButtonActionPerformed(evt);
            }
        });

        scoreButton.setText("Score");
        scoreButton.setEnabled(false);
        scoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoreButtonActionPerformed(evt);
            }
        });

        JScrollBar sBar1 = jScrollPane1.getVerticalScrollBar();

        linkList.setFixedCellHeight(20);
        jScrollPane1.setViewportView(linkList);

        transformationComboBox.setToolTipText("");
        transformationComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                transformationComboBoxItemStateChanged(evt);
            }
        });
        transformationComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transformationComboBoxActionPerformed(evt);
            }
        });

        statusField.setText("Idle...");

        thresholdField.setText("50");

        thresholdLabel.setText("Threshold (meters):");

        JScrollBar sBar2 = jScrollPane2.getVerticalScrollBar();
        sBar2.setModel(sBar1.getModel());

        scoreLinkList.setFixedCellHeight(20);
        jScrollPane2.setViewportView(scoreLinkList);

        selectAllLinks.setText("Select All Corresponding Links");
        selectAllLinks.setEnabled(false);
        selectAllLinks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllLinksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(transformationComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(statusField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(thresholdLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(thresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(selectAllLinks, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(scoreButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fuseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(transformationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fuseButton)
                    .addComponent(scoreButton)
                    .addComponent(statusField)
                    .addComponent(thresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(thresholdLabel))
                .addGap(24, 24, 24)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectAllLinks)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Fused dataset graph"));

        fusedGraphField.setText("http://localhost:8890/fused_dataset");

        fusedGraphCheckbox.setText("Use this graph for fusion:");
        fusedGraphCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fusedGraphCheckboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fusedGraphCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fusedGraphField, javax.swing.GroupLayout.PREFERRED_SIZE, 504, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fusedGraphField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fusedGraphCheckbox))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.getAccessibleContext().setAccessibleName("fused graph");
    }// </editor-fold>//GEN-END:initComponents

    private void linkFileChooserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linkFileChooserButtonActionPerformed
        try {
            final File file = new File(linksFileField.getText());
            final JFileChooser fileChooser = new JFileChooser(file);

            final int returnVal = fileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                linksFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
            errorListener.notifyError(ex.getMessage());
        }
    }//GEN-LAST:event_linkFileChooserButtonActionPerformed

    private void loadLinksButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadLinksButtonActionPerformed
        try { 
            if (links != null){
                links.clear();
            }
                
            links = GeometryFuser.parseLinksFile(linksFileField.getText());            
            
            displayLinks();
            linksAreSet(true);
        }
        catch (ParseException | RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
            errorListener.notifyError(ex.getMessage());
        }
    }//GEN-LAST:event_loadLinksButtonActionPerformed

    private void scoreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoreButtonActionPerformed
        try {
            final AbstractFusionTransformation transformation = transformations.get((String) transformationComboBox.getSelectedItem());
            
            // Handle ScaleTransformation separately to display popup for setting transformation parameters
            if(transformation instanceof ScaleTransformation) {
                ScaleParamsPanel scaleParamsPanel = new ScaleParamsPanel();
                final int result = JOptionPane.showConfirmDialog(this, scaleParamsPanel, "Scale transformation parameters", JOptionPane.OK_CANCEL_OPTION);
                if(result == JOptionPane.OK_OPTION) {
                    final ScaleTransformation tempTransformation = (ScaleTransformation) transformation;
                    tempTransformation.setScaleParams(scaleParamsPanel.isKeepLeftDatasetSet(), scaleParamsPanel.getScaleFactor());
                }
            }
                                 
            //new ScoreWorker, threshold passed as parameter
            final ScoreWorker scoreWorker = new ScoreWorker(transformation, links, dbConfig, getThreshold()) {
                @Override protected void done() {
                    try {
                        scoresForAllRules.put(transformation.getID(), get());
                        displayLinks(transformation.getID());
                        statusField.setText("Done (scoring) with transformation: " + transformation.getID());
                    }
                    catch (RuntimeException ex) {
                        if(ex.getCause() == null) {
                            LOG.warn(ex.getMessage(), ex);
                            errorListener.notifyError(ex.getMessage());
                        }
                        else {
                            LOG.warn(ex.getCause().getMessage(), ex.getCause());
                            errorListener.notifyError(ex.getCause().getMessage());
                        }
                        statusField.setText("Worker terminated abnormally.");
                    }
                    catch (InterruptedException ex) {
                        LOG.warn(ex.getMessage(), ex);
                        errorListener.notifyError(ex.getMessage());
                        statusField.setText("Worker terminated abnormally.");
                    }
                    catch (ExecutionException ex) {
                        LOG.warn(ex.getCause().getMessage(), ex.getCause());
                        errorListener.notifyError(ex.getMessage());
                        statusField.setText("Worker terminated abnormally.");
                    }
                    finally {
                        setBusy(false);
                    }
                    
                    LOG.info(ANSI_YELLOW+"Score worker has terminated."+ANSI_RESET);
                }
            };
                        
            
            statusField.setText("Working...");
            setBusy(true);
            scoreWorker.execute();
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
            errorListener.notifyError(ex.getMessage());
        }
    }//GEN-LAST:event_scoreButtonActionPerformed
    
    
    private Double getThreshold(){
        //we set a default value of "-1" if the user didn' t choose a threshold, for scoring purposes.
            if(!thresholdField.getText().isEmpty()){
                try{
                threshold = Double.parseDouble(thresholdField.getText());               
                thresholdLabel.setText("Threshold set!");
                
                }
                catch (NumberFormatException ex) {                 
                    errorListener.notifyError("Threshold must be number!");
                }
            }
            else{
                threshold = -1.0; // value for no threshold!
                thresholdLabel.setText("Threshold (meters):");
            }            
         return threshold;    
    }
    
    private void fuseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fuseButtonActionPerformed
        try {
            final AbstractFusionTransformation transformation = transformations.get((String) transformationComboBox.getSelectedItem());
            
            // Handle ScaleTransformation separately to display popup for setting transformation parameters
            if(transformation instanceof ScaleTransformation) {
                ScaleParamsPanel scaleParamsPanel = new ScaleParamsPanel();
                final int result = JOptionPane.showConfirmDialog(this, scaleParamsPanel, "Scale transformation parameters", JOptionPane.OK_CANCEL_OPTION);
                if(result == JOptionPane.OK_OPTION) {
                    final ScaleTransformation tempTransformation = (ScaleTransformation) transformation;
                    tempTransformation.setScaleParams(scaleParamsPanel.isKeepLeftDatasetSet(), scaleParamsPanel.getScaleFactor());
                }
            }
            
            final List<String> selectedLinkKeys = (List<String>) linkList.getSelectedValuesList();
            final List<Link> selectedLinks = new ArrayList<>();
            for(Link link : links) {
                if(selectedLinkKeys.contains(link.getKey())) {
                    selectedLinks.add(link);
                }
            }
            
            if(selectedLinks.isEmpty()) {
                errorListener.notifyError("No links selected.");
                return;
            }
            
            final FuseWorker fuseWorker = new FuseWorker(transformation, selectedLinks, dbConfig, fusedGraph, fusedGraphCheckbox.isSelected(), graphConfig, this, statusField, errorListener);
            
            statusField.setText("Working...");
            setBusy(true);
            fuseWorker.execute();
        }
        catch (RuntimeException ex) {
            errorListener.notifyError(ex.getMessage());
        }                        
        
    }//GEN-LAST:event_fuseButtonActionPerformed

    private void transformationComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_transformationComboBoxItemStateChanged
        final String transformation = (String) transformationComboBox.getSelectedItem();
        displayLinks(transformation);
    }//GEN-LAST:event_transformationComboBoxItemStateChanged

    private void transformationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transformationComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_transformationComboBoxActionPerformed

    private void selectAllLinksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllLinksActionPerformed
        // select the links that can be fused from linkList 
        int linkListSize = linkList.getModel().getSize();
        int[] indexArray = new int[linkListSize];       

        for (int i = 0; i < linkListSize; i++) {            
            Object item = linkList.getModel().getElementAt(i);            
            //get color of element at i
            Color color = linkList.getCellRenderer().getListCellRendererComponent(linkList, item, i, busy, busy).getForeground();
            if (color.equals(Color.MAGENTA)){
                indexArray [i] = i;
            } 
        }
        //set selected elements. index array contains the indices of the colored elements 
        linkList.setSelectedIndices(indexArray);
        
        //find elements to select by score. prefered comparison only in linkList so this stays out for now
        /*
        for (int i = 0; i < linkList.getModel().getSize(); i++) {
            Object item = scoreLinkList.getModel().getElementAt(i);
            System.out.println(" Score Item    = " + item);
        }
        */
    }//GEN-LAST:event_selectAllLinksActionPerformed

    private void fusedGraphCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fusedGraphCheckboxActionPerformed
        if(fusedGraphCheckbox.isSelected()) {

            fusedGraph = fusedGraphField.getText();
            fusedGraphField.setEnabled(false);
        }       
        else
        {
            fusedGraph = fusedGraphField.getText();
            fusedGraphField.setEnabled(true);
        }
    }//GEN-LAST:event_fusedGraphCheckboxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton fuseButton;
    private javax.swing.JCheckBox fusedGraphCheckbox;
    private javax.swing.JTextField fusedGraphField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton linkFileChooserButton;
    private javax.swing.JList linkList;
    private javax.swing.JTextField linksFileField;
    private javax.swing.JButton loadLinksButton;
    private javax.swing.JButton scoreButton;
    private javax.swing.JList scoreLinkList;
    private javax.swing.JButton selectAllLinks;
    private javax.swing.JLabel statusField;
    private javax.swing.JTextField thresholdField;
    private javax.swing.JLabel thresholdLabel;
    private javax.swing.JComboBox transformationComboBox;
    // End of variables declaration//GEN-END:variables
}

class CustomLinkListRenderer extends DefaultListCellRenderer {

    private final Map<String, Double> scores;
    
    public CustomLinkListRenderer(final Map<String, Double> scores) {
        super();
        this.scores = checkNotNull(scores);
    }
    
    public CustomLinkListRenderer() {
        super();
        scores = null;
    }
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if(scores != null && scores.get(value.toString()) != null && scores.get(value.toString()) > 0.0) { 
           
            component.setForeground(Color.MAGENTA);                                                                 
            
        }
        else {
            component.setForeground(Color.BLACK);
        }
        
        return component;
    }    
    
}