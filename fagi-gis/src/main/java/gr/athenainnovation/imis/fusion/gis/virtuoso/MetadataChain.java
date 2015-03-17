
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author imis-nkarag
 */
    public class MetadataChain {
        String link;
        String predicate;
        String objectStr;
        HashMap< String, MetadataChain > chains;

        public MetadataChain(String l, String p, String o) {
            link = l;
            predicate = p;
            objectStr = o;
            chains = null;
        }
        
        public void addCahin(MetadataChain mc) {
            if (chains == null)
                chains = Maps.newHashMap();
            
            chains.put(mc.predicate, mc);
        }
        
        public boolean containsPre(String pre) {
            if (chains == null)
                chains = Maps.newHashMap();
            
            return chains.containsKey(pre);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            if (chains == null) 
                return sb.append(predicate).append(" empty chain").toString();
            
            Iterator it = chains.entrySet().iterator();
            sb.append("Chain of ").append(predicate).append("\n");
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                sb.append(pairs.getKey()).append(" = ").append((MetadataChain)pairs.getValue()).append("\n");
                it.remove(); // avoids a ConcurrentModificationException
            }
            sb.append("\n");
            
            return sb.toString();
        }
    }
