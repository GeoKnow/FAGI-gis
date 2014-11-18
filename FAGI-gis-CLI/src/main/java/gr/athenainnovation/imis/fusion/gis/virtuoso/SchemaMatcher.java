
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author imis-nkarag
 */
public class SchemaMatcher {
    Schema sA;
    Schema sB;
    List<String> matches;

    public SchemaMatcher() {
        this.matches = new ArrayList<>();
    }

    public SchemaMatcher(Schema sA, Schema sB) {
        this.sA = sA;
        this.sB = sB;
        this.matches = new ArrayList<>();
    }
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("");
        out.append("Matcher ---\n");
        out.append(sA.predicate).append(" VS ").append(sB.predicate).append("\n");
        out.append("Word Count: ").append(sA.words.size()).append("\n");
        for (String s : sA.words) {
            out.append(s).append(" ");
        }
        out.append("\n");
        out.append("Word Count: ").append(sB.words.size()).append("\n");
        for (String s : sB.words) {
            out.append(s).append(" ");
        }
        out.append("\n");
        if (matches.isEmpty() ) {
            out.append("empty\n");
        }
        for (String s : matches) {
            out.append(s).append(" ");
        }
        out.append("\n");

        return out.toString();
    }
}
