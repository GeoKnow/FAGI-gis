
package fusion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nick
 */
public class FusionState implements Serializable{
    public List<String> objsA;
    public List<String> predsA;
    public List<String> preds;
    public List<String> predsB;
    public List<String> objsB;
    public List<String> actions;
    
    FusionState() {
        preds = new ArrayList<>();
        predsA = new ArrayList<>();
        predsB = new ArrayList<>();
        objsA = new ArrayList<>();
        objsB = new ArrayList<>();
        actions = new ArrayList<>();
    }
}
