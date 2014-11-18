/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fusion;

import com.google.common.collect.Lists;
import java.util.List;

/**
 *
 * @author nick
 */
public class FusionState {
    public List<String> objsA;
    public List<String> predsA;
    public List<String> preds;
    public List<String> predsB;
    public List<String> objsB;
    public List<String> actions;
    
    FusionState() {
        preds = Lists.newArrayList();
        predsA = Lists.newArrayList();
        predsB = Lists.newArrayList();
        objsA = Lists.newArrayList();
        objsB = Lists.newArrayList();
        actions = Lists.newArrayList();
    }
}
