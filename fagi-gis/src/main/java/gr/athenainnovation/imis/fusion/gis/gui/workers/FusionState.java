
package gr.athenainnovation.imis.fusion.gis.gui.workers;

import gr.athenainnovation.imis.fusion.gis.geotransformations.ScaleTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import static gr.athenainnovation.imis.fusion.gis.gui.FuserPanel.registerTransformations;

/**
 *
 * @author nick
 */
public class FusionState {
    
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private DBConfig dbConf;
    private GraphConfig graphConf;
    private String linksFile;
    private String virtDir;
    private String trans;
    private boolean imported;
    private String dstGraph;
    private AbstractFusionTransformation transformation;
    private double threshold;
    private String extraParams;
    
    public FusionState() {
        dbConf = new DBConfig("", "", "", "", "", "", "");
        graphConf = new GraphConfig("", "", "", "", "");
        dstGraph = "";
        virtDir = "";
        linksFile = "";
        imported = false;
        threshold = 0.0;
        transformation = null;
        extraParams = "";
        registerTransformations();
    }
    
    public boolean checkConfiguration() {
        boolean isCorrect = true;
        if (virtDir.equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtuosoAllowedDirs"+ANSI_RESET);
            isCorrect = false;
        }
        if (virtDir.equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtuosoAllowedDirs"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (transformation == null) {
            System.out.println(ANSI_RED+"Invalid parameter for linksFile"+ANSI_RESET);
            isCorrect = false;
        } else {
            isCorrect = checkTransformationParams(transformation.getID());
        }
        
        if (dbConf.getUsername().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtUser"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (dbConf.getPassword().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtPass"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (virtDir.equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtDir"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (dbConf.getDBURL().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for virtUrl"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (dbConf.getDBName().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Database Name"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (dbConf.getDBPassword().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Database Password"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (graphConf.getEndpointA().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Source A endpoint"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (graphConf.getEndpointB().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Source B endpoint"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (graphConf.getGraphA().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Source A graph"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (graphConf.getGraphB().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for Source B graph"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (graphConf.getEndpointT().equals((""))) {
            System.out.println(ANSI_RED+"Invalid parameter for target graph"+ANSI_RESET);
            isCorrect = false;
        }
        
        if (dstGraph.equals("")) {
            dstGraph = graphConf.getGraphA();
            System.out.println(ANSI_YELLOW+"No destination graph specified, usinf Source A"+ANSI_RESET);
        }
        return isCorrect;
    }
    
    private boolean checkTransformationParams(String value) {
        if (value.equals("Scale")) {
            String[] vals = extraParams.split("=");
            //System.out.println(vals[0]+" "+vals[1]);
            ((ScaleTransformation)transformation).setScaleParams(true, Double.parseDouble(vals[1]));
        }
        return true;
    }
    
    public void setFusionParam(String label, String value) {
        switch (label) {
            case "linksFile":
                setLinksFile(value);
                break;
            case "virtuosoAllowedDir":
                setVirtDir(value);
                dbConf.setBulkDir(value);
                break;
            case "pg_DatabaseName":
                dbConf.setDbName(value);
                break;
            case "pg_User":
                dbConf.setDbUsername(value);
                break;
            case "pg_Password":
                dbConf.setDbPassword(value);
                break;
            case "pg_Import":
                setImported(Boolean.parseBoolean(value));
                break;
            case "vi_URL":
                dbConf.setDbURL(value);
                break;
            case "vi_User":
                dbConf.setUsername(value);
                break;
            case "vi_Password":
                dbConf.setPassword(value);
                break;
            case "sa_Graph":
                graphConf.setGraphA(value);
                break;
            case "sa_Endpoint":
                graphConf.setEndpointA(value);
                break;
            case "sb_Graph":
                graphConf.setGraphB(value);
                break;
            case "sb_Endpoint":
                graphConf.setEndpointB(value);
                break;
            case "fuse_Transformation":
                setTransformation(FuserPanel.transformations.get(value));
                break;
            case "outputGraph":
                setDstGraph(value);
                break;
            case "outputEndpoint":
                graphConf.setEndpointT(value);
                break;
            case "fuse_Threshold":
                setThreshold(Double.parseDouble(value));
                break;
            case "loc_endpoint":
                graphConf.setEndpointLoc(value);
                break;
            case "fuse_scale_factor":
                extraParams = extraParams.concat("factor="+Double.parseDouble(value));
                break;
            default:
                break;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder rep = new StringBuilder(128);
        
        rep.append("PostGIS as ").append(dbConf.getDBUsername()).append(" using ").append(dbConf.getDBName()).append(" database").append("\n");
        rep.append("Virtuoso at ").append(dbConf.getDBURL()).append(" as ").append(dbConf.getUsername()).append(" using ").append(virtDir).append(" for bulk inserts").append("\n");
        rep.append("Source A is ").append(graphConf.getGraphA()).append(" at ").append(graphConf.getEndpointA()).append("\n");
        rep.append("Source B is ").append(graphConf.getGraphB()).append(" at ").append(graphConf.getEndpointB()).append("\n");
        rep.append("Targetr is ").append(this.dstGraph).append(" at ").append(graphConf.getEndpointT()).append("\n");
        return ANSI_BLUE+rep.toString()+ANSI_WHITE;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public String getDstGraph() {
        return dstGraph;
    }

    public void setDstGraph(String dstGraph) {
        this.dstGraph = dstGraph;
    }

    private String getTrans() {
        return trans;
    }

    private void setTrans(String trans) {
        this.trans = trans;
    }
    
    public AbstractFusionTransformation getTransformation() {
        return transformation;
    }

    public void setTransformation(AbstractFusionTransformation transformation) {
        this.transformation = transformation;
    }
    
    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getVirtDir() {
        return virtDir;
    }

    public void setVirtDir(String virtDir) {
        this.virtDir = virtDir;
    }

    public DBConfig getDbConf() {
        return dbConf;
    }

    public void setDbConf(DBConfig dbConf) {
        this.dbConf = dbConf;
    }

    public GraphConfig getGraphConf() {
        return graphConf;
    }

    public void setGraphConf(GraphConfig graphConf) {
        this.graphConf = graphConf;
    }

    public String getLinksFile() {
        return linksFile;
    }

    public void setLinksFile(String linksFile) {
        this.linksFile = linksFile;
    }
    
}
