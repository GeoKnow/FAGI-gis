package gr.athenainnovation.imis.fusion.gis.gui;

import static com.google.common.base.Preconditions.*;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.DBConfigListener;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FuseWorker;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ScoreWorker;
import gr.athenainnovation.imis.fusion.gis.transformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.AvgTwoPointsTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.KeepBothTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.KeepLeftTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.KeepMostPointsAndTranslateTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.KeepMostPointsTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.KeepRightTransformation;
import gr.athenainnovation.imis.fusion.gis.transformations.ScaleTransformation;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

/**
 * Handles application of fusion transformations.
 * @author Thomas Maroulis
 */
public class FuserPanel extends javax.swing.JPanel implements DBConfigListener {
    private static final Logger LOG = Logger.getLogger(FuserPanel.class);
    
    private final ErrorListener errorListener;
    private DBConfig dbConfig;
    private final Map<String, AbstractFusionTransformation> transformations;
    private final Map<String, Map<String, Double>> scoresForAllRules;
    private List<Link> links;
    
    private boolean dbConfigSet = false;
    private boolean linksSet = false;
    private boolean busy = false;
    
    /**
     * Creates new form FuserPanel
     * @param errorListener error message listener
     */
    public FuserPanel(final ErrorListener errorListener) {
        super();
        this.errorListener = errorListener;
        initComponents();
        
        transformations = new HashMap<>();
        registerTransformations();
        displayTransformations();
        
        scoresForAllRules = new HashMap<>();
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
    
    private void setBusy(final boolean busy) {
        this.busy = busy;
        setFieldsEnabled();
    }
    
    private void setFieldsEnabled() {
        final boolean enabled = dbConfigSet && linksSet && !busy;
        fuseButton.setEnabled(enabled);
        scoreButton.setEnabled(enabled);
    }
    
    // Registers all implemented transformations
    private void registerTransformations() {
        KeepLeftTransformation keepLeftTransformation = new KeepLeftTransformation();
        transformations.put(keepLeftTransformation.getID(), keepLeftTransformation);
        
        KeepRightTransformation keepRightTransformation = new KeepRightTransformation();
        transformations.put(keepRightTransformation.getID(), keepRightTransformation);
        
        KeepMostPointsTransformation keepMostPointsTransformation = new KeepMostPointsTransformation();
        transformations.put(keepMostPointsTransformation.getID(), keepMostPointsTransformation);
        
        KeepMostPointsAndTranslateTransformation keepMostPointsAndTranslateTransformation = new KeepMostPointsAndTranslateTransformation();
        transformations.put(keepMostPointsAndTranslateTransformation.getID(), keepMostPointsAndTranslateTransformation);
        
        AvgTwoPointsTransformation avgTwoPointsTransformation = new AvgTwoPointsTransformation();
        transformations.put(avgTwoPointsTransformation.getID(), avgTwoPointsTransformation);
        
        KeepBothTransformation keepBothTransformation = new KeepBothTransformation();
        transformations.put(keepBothTransformation.getID(), keepBothTransformation);
        
        ScaleTransformation scaleTransformation = new ScaleTransformation();
        transformations.put(scaleTransformation.getID(), scaleTransformation);
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
        }
        else {
            linkList.setCellRenderer(new CustomLinkListRenderer());
        }
    }
    
    private void displayLinks() {
        setLinkListModel();
        linkList.setCellRenderer(new CustomLinkListRenderer());
    }
    
    private void setLinkListModel() {
        final DefaultListModel<String> model = new DefaultListModel<>();
        for(Link link : links) {
            model.addElement(link.getKey());
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
                .addComponent(linksFileField, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        jScrollPane1.setViewportView(linkList);

        transformationComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                transformationComboBoxItemStateChanged(evt);
            }
        });

        statusField.setText("Idle...");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(transformationComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(statusField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(scoreButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fuseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                    .addComponent(statusField))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
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
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
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
            
            final ScoreWorker scoreWorker = new ScoreWorker(transformation, links, dbConfig) {
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
                    
                    LOG.info("Score worker has terminated.");
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
            
            final FuseWorker fuseWorker = new FuseWorker(transformation, selectedLinks, dbConfig) {
                @Override protected void done() {
                    // Call get despite return type being Void to prevent SwingWorker from swallowing exceptions
                    try {
                        get();
                        statusField.setText("Done (fusing) with transformation: " + transformation.getID());
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
                    
                    LOG.info("Fuse worker has terminated.");
                }
            };
            
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton fuseButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton linkFileChooserButton;
    private javax.swing.JList linkList;
    private javax.swing.JTextField linksFileField;
    private javax.swing.JButton loadLinksButton;
    private javax.swing.JButton scoreButton;
    private javax.swing.JLabel statusField;
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
        
        if(scores != null && scores.get(value.toString()) != null && scores.get(value.toString()) >= 0.5) {
            component.setForeground(Color.MAGENTA);
        }
        else {
            component.setForeground(Color.BLACK);
        }
        
        return component;
    }
    
}