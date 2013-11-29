package gr.athenainnovation.imis.fusion.gis.gui;

import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;

/**
 * Application entry point.
 * @author Thomas Maroulis
 */
public class FusionGISGUI extends JFrame implements ErrorListener {
    
    private DatabasePanel databasePanel;
    private ImporterPanel importerPanel;
    private FuserPanel fuserPanel;
    
    private static final int SCROLL_INCREMENT = 20;
    
    public FusionGISGUI() {
        initComponents();
    }
    
    private JTabbedPane JTabbedPane;
    
    private void initComponents() {
        JTabbedPane = new javax.swing.JTabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(JTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 757, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(JTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                .addContainerGap())
        );
        
        databasePanel = new DatabasePanel(this);
        JScrollPane datasetPanelScrollPane = new JScrollPane(databasePanel);
        datasetPanelScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        datasetPanelScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_INCREMENT);
        JTabbedPane.addTab("Database", datasetPanelScrollPane);
        
        importerPanel = new ImporterPanel(this);
        JScrollPane importerPanelScrollPane = new JScrollPane(importerPanel);
        importerPanelScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        importerPanelScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_INCREMENT);
        JTabbedPane.addTab("Importer", importerPanelScrollPane);
        
        fuserPanel = new FuserPanel(this);
        JScrollPane fuserPanelScrollPane = new JScrollPane(fuserPanel);
        fuserPanelScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        fuserPanelScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_INCREMENT);
        JTabbedPane.addTab("Fuser", fuserPanelScrollPane);
        
        databasePanel.registerListener(importerPanel);
        databasePanel.registerListener(fuserPanel);
        
        setPreferredSize(new Dimension(1024, 960));
        pack();
    }
    
    @Override
    public void notifyError(final String message) {
        JOptionPane.showMessageDialog(this, message,"Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            Logger log = Logger.getLogger(FusionGISGUI.class);
            log.fatal(ex.getMessage(), ex);
        }
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FusionGISGUI().setVisible(true);
            }
        });
    }           
}
