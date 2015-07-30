
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import java.util.ArrayList;
import java.util.List;
import net.didion.jwnl.data.IndexWord;

/**
 *
 * @author imis-nkarag
 */

public class Schema {
    public List<String> words;
    public List<String> skippedWords;
    public List<IndexWord> indexes;
    public String predicate;
    public String objectStr;
    public float texDist;
    public float semDist;    
    public float typeDist;    
    
    public Schema() {
        words = new ArrayList<>();
        indexes = new ArrayList<>();
    }

    public void addWord(String w) {
        words.add(w);
    }

    public void addIndex(IndexWord w) {
        indexes.add(w);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("");
        out.append("Schema ---\n");
        out.append("Word Count: ").append(words.size()).append("\n");
        for (String s : words) {
            out.append(s).append(" ");
        }
        out.append("\n");
        for (IndexWord w : indexes) {
            out.append(w.getLemma()).append(":").append(w.getPOS().getLabel()).append(" ");
        }
        out.append("\n");

        return out.toString();
    }
}
