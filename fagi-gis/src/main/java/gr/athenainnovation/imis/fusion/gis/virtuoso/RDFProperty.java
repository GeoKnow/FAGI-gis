
package gr.athenainnovation.imis.fusion.gis.virtuoso;

/**
 *
 * @author imis-nkarag
 */
public class RDFProperty {
    String pred;
    String type;

    public RDFProperty(String pred, String type) {
        this.pred = pred;
        this.type = type;
    }

    public String getPred() {
        return pred;
    }

    public void setPred(String pred) {
        this.pred = pred;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
