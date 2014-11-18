
package gr.athenainnovation.imis.fusion.gis.cli;

import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FuseWorker;
import gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RED;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.relationship.AsymmetricRelationship;
import net.didion.jwnl.data.relationship.Relationship;
import net.didion.jwnl.data.relationship.RelationshipFinder;
import net.didion.jwnl.data.relationship.RelationshipList;
import net.didion.jwnl.dictionary.Dictionary;
import net.didion.jwnl.dictionary.MorphologicalProcessor;
//import org.apache.commons.lang3.StringUtils;

/**
 * Entry of the program
 * 
 * @author nicks
 */

public class FusionGISCLI {
    private static final String PATH_TO_WORDNET = "/usr/share/wordnet";
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FusionGISCLI.class);          
    private class FAGILogger implements ErrorListener {

        @Override
        public void notifyError(String message) {
            System.out.println("ERROR:"+message);
        }
    
    }
    
    private static FAGILogger errListen;
      
    private static void scanSense(Synset i, Synset j) throws JWNLException {
        RelationshipList list = RelationshipFinder.getInstance().findRelationships(i, j, PointerType.HYPERNYM);
	//System.out.println("Hypernym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
	for (Iterator itr = list.iterator(); itr.hasNext();) {
            ((Relationship) itr.next()).getNodeList();
	}
        for(Object o : list) {
            Relationship rel = (Relationship) o;
            int commonParentIndex = ((AsymmetricRelationship) rel).getCommonParentIndex();
            //System.out.println("Common Parent Index: " + tom);
            //System.out.println("Depth: " + rel.getDepth());
            if (commonParentIndex < 4) {
                rel.getNodeList().print();
                //System.out.println();
            }
        }
    }
    
    private static void demonstrateAsymmetricRelationshipOperation(IndexWord start, IndexWord end) throws JWNLException {
		// Try to find a relationship between the first sense of <var>start</var> and the first sense of <var>end</var>
		//System.out.println(end.getSenseCount()); 
                //System.out.println(start.getSenseCount()); 
                for(Synset i : start.getSenses()) {
                    for (Synset j : end.getSenses()) {
                        scanSense(i, j);
                    }
                }
	}

    private static void demonstrateSymmetricRelationshipOperation(IndexWord start, IndexWord end) throws JWNLException {
            // find all synonyms that <var>start</var> and <var>end</var> have in common
            RelationshipList list = RelationshipFinder.getInstance().findRelationships(start.getSense(1), 
                    end.getSense(1), PointerType.SIMILAR_TO);
            //System.out.println("Synonym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
            for (Iterator itr = list.iterator(); itr.hasNext();) {
                    //((Relationship) itr.next()).getNodeList().print();
            }
            if( list.size() > 0){
                //System.out.println("Depth: " + ((Relationship) list.get(0)).getDepth());
            }    
    }
        
    public static void main(String args[]) {
        List<String> lines;
        long startTime, endTime;
        String config_file;
        MorphologicalProcessor morph;
        
        try {

            JWNL.initialize(new ByteArrayInputStream(getJWNL(PATH_TO_WORDNET).getBytes(StandardCharsets.UTF_8)));
            Dictionary dictionary = Dictionary.getInstance();
            morph = dictionary.getMorphologicalProcessor();
            IndexWord word = dictionary.lookupIndexWord(POS.NOUN, "label");
            IndexWord word2 = dictionary.lookupIndexWord(POS.VERB, "address");
            
            //demonstrateSymmetricRelationshipOperation(word, word2);
            //System.out.println("Finish SYM");
            demonstrateAsymmetricRelationshipOperation(word, word2);
            //System.out.println(word.getLemma());
            //System.out.println(word2.getSenses());
            
            /* //debugging output
            if (word != null) {
                for(Synset i : word.getSenses()) {
                    System.out.println(i);    
                }
            }
            String[] tom = StringUtils.splitByCharacterTypeCamelCase("sdasdFFFGF///FGGFF$$@%$#!@%^GFEfkhsdiuygegljkasdf5");
            for(String t : tom) {
                System.out.println(t);
            }
            if (word2 != null) {
                for(Synset i : word2.getSenses()) {
                    System.out.println(i);    
                }
            }
            /*
            /*IndexWord w;
            String sample = "hunting";
            System.out.println("Senses of the word 'wing':");
            Synset[] senses = word.getSenses();
            for (int i=0; i<senses.length; i++) {
                Synset sense = senses[i];
		System.out.println((i+1) + ". " + sense.getGloss());
		Pointer[] holo = sense.getPointers(PointerType.PART_HOLONYM);
		for (int j=0; j<holo.length; j++) {
		    Synset synset = (Synset) (holo[j].getTarget());
		    Word synsetWord = synset.getWord(0);
		    System.out.print("  -part-of-> " + synsetWord.getLemma());
		    System.out.println(" = " + synset.getGloss());
		}
            }
            
            
            Synset[] senses2 = word.getSenses();
            for (Synset syn : senses2) {
                final String gloss = syn.getGloss();
                System.out.println("HERE: "+gloss.contains(word2.getLemma()));
            }
            
            senses2 = word2.getSenses();
            for (Synset syn : senses2) {
                final String gloss = syn.getGloss();
                System.out.println("HERE 2: "+gloss.contains(word.getLemma()));
            }
            PointerTargetNodeList hypernyms = PointerUtils.getInstance().getDirectHypernyms(word.getSense(1));
		System.out.println("Direct hypernyms of \"" + word.getLemma() + "\":");
		hypernyms.print();
            
            hypernyms = PointerUtils.getInstance().getDirectHypernyms(word2.getSense(1));
		System.out.println("Direct hypernyms of \"" + word2.getLemma() + "\":");
		hypernyms.print();
                
            for (Object hypernym : hypernyms) {
                PointerTargetNode n = (PointerTargetNode) hypernym;
                for (Word wo : n.getSynset().getWords()) {
                    System.out.println(wo.getLemma());
                } 
                System.out.println();
            } 
           
            PointerTargetTree hyponyms = PointerUtils.getInstance().getHyponymTree(word.getSense(1));
		//System.out.println("Hyponyms of \"" + word.getLemma() + "\":");
		//hyponyms.print();
                
            hyponyms = PointerUtils.getInstance().getHyponymTree(word2.getSense(1));
		//System.out.println("Hyponyms of \"" + word2.getLemma() + "\":");
		//hyponyms.print();
                
            String input = "http://localhost:8890/DAV/uni2";
            String[] parts = input.split("[\\W]");
            for (String s : parts) {
                System.out.println(s);
            }
            w = morph.lookupBaseForm( POS.VERB, sample );
			if ( w != null )
				System.out.println(w.getLemma().toString());
			w = morph.lookupBaseForm( POS.NOUN, sample );
			if ( w != null )
				System.out.println(w.getLemma().toString());
			w = morph.lookupBaseForm( POS.ADJECTIVE, sample );
			if ( w != null )
				System.out.println(w.getLemma().toString());
			w = morph.lookupBaseForm( POS.ADVERB, sample );
			if ( w != null )
                            System.out.println(w.getLemma().toString());
                 */       
            /*String wordForm = "make";
  Synset[] synsets = dictionary.getSynsets(wordForm,SynsetType.VERB);
  if (synsets.length > 0) {
       for (int i = 0; i < synsets.length; i++) {
    String[] wordForms = synsets[i].getWordForms();
    for (int j = 0; j < wordForms.length; j++) {
           if(!synonyms.contains(wordForms[j])){
        synonyms.add(wordForms[j]); }
                }
           }
     }*/
        } catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
	}
        
        if (args.length != 2) {
            System.out.println(args.length);
            for(String a : args)
                System.out.println(a);
            System.out.println(ANSI_YELLOW+"Usage: FAGI -c configFile"+ANSI_RESET);
            return;
        }
        if (args[0].equals("-c")) {
            config_file = args[1];
        } else {
            for(String a : args)
                System.out.println(a);
            System.out.println(ANSI_YELLOW+"Usage: FAGI -c configFile"+ANSI_RESET);
            return;
        }
        
        try {
            
            final FusionState st = new FusionState();
            
            //lines = Files.readAllLines(Paths.get("/home/nick/Projects/FAGI-gis-master/fusion.conf"), Charset.defaultCharset());
            lines = Files.readAllLines(Paths.get(config_file), Charset.defaultCharset());
            for (String line : lines) {
                if (line.startsWith("#")) {
                } else if (line.equals("")) {
                } else {
                    String [] params = line.split("=");
                    st.setFusionParam(params[0].trim(), params[1].trim());
                }
            }
            
            boolean isValid = st.checkConfiguration();
            if ( isValid ) {
                System.out.println("-- Executing following Configuration");
                LOG.info(st);
            } else {
                return;
            }
            if (st.isImported()) {
                //System.out.println("ssasasasasasa");
                final DatabaseInitialiser databaseInitialiser = new DatabaseInitialiser();
                databaseInitialiser.initialise(st.getDbConf());
            
                //final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConfig, PostGISImporter.DATASET_A, sourceDatasetA, datasetAStatusField, errorListener);
                Dataset sourceADataset = new Dataset(st.getGraphConf().getEndpointA(), st.getGraphConf().getGraphA(), "");
                final ImporterWorker datasetAImportWorker = new ImporterWorker(st.getDbConf(), 
                        PostGISImporter.DATASET_A, sourceADataset, null, errListen);
                datasetAImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override public void propertyChange(PropertyChangeEvent evt) {
                        if("progress".equals(evt.getPropertyName())) {
                            //System.out.println("prog");
                        }
                    }
                });
            
                Dataset sourceBDataset = new Dataset(st.getGraphConf().getEndpointB(), st.getGraphConf().getGraphB(), "");
                final ImporterWorker datasetBImportWorker = new ImporterWorker(st.getDbConf(), 
                        PostGISImporter.DATASET_B, sourceBDataset, null, errListen);
            
                datasetBImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override public void propertyChange(PropertyChangeEvent evt) {
                        if("progress".equals(evt.getPropertyName())) {
                            //System.out.println("prog2");
                        }
                    }
                });
            
                startTime = System.nanoTime();
                datasetAImportWorker.execute();
                datasetBImportWorker.execute();
            
                datasetAImportWorker.get();
                datasetBImportWorker.get();
                endTime = System.nanoTime();
            }
            
            //System.out.println("Time spent importing data to PostGIS "+(endTime-startTime)/1000000000f);
            ArrayList<Link> links = (ArrayList<Link>) GeometryFuser.parseLinksFile(st.getLinksFile()); 
            
            //final ScoreWorker scoreWorker = new ScoreWorker(st.getTransformation(), links, st.getDbConf(), st.getThreshold());
            
            //scoreWorker.execute();
            //scoresForAllRules.put(st.getTransformation().getID(), scoreWorker.get());
            
            boolean createNew = !st.getDstGraph().equals(st.getGraphConf().getGraphA());
            final FuseWorker fuseWorker = new FuseWorker(st.getTransformation(), links, st.getDbConf(),
                    st.getDstGraph(), createNew, st.getGraphConf(), null, null, errListen);

            fuseWorker.execute();
            fuseWorker.get();
        } catch (IOException ex) {
            if(ex instanceof NoSuchFileException) {
                System.out.println(ANSI_RED+args[1]+" does not exist"+ANSI_RESET);
                return;
            }
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            //Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
            SQLException exception = ex;
            while(exception != null) {
                Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, exception);
                exception = exception.getNextException();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Done.");
    }
    
        private static String getJWNL(String pathToWordnet){
        
        String jwnlXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<jwnl_properties language=\"en\">\n" +
        "	<version publisher=\"Princeton\" number=\"3.0\" language=\"en\"/>\n" +
        "	<dictionary class=\"net.didion.jwnl.dictionary.FileBackedDictionary\">\n" +
        "		<param name=\"morphological_processor\" value=\"net.didion.jwnl.dictionary.morph.DefaultMorphologicalProcessor\">\n" +
        "			<param name=\"operations\">\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.DetachSuffixesOperation\">\n" +
        "					<param name=\"noun\" value=\"|s=|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y|\"/>\n" +
        "					<param name=\"verb\" value=\"|s=|ies=y|es=e|es=|ed=e|ed=|ing=e|ing=|\"/>\n" +
        "					<param name=\"adjective\" value=\"|er=|est=|er=e|est=e|\"/>\n" +
        "                    <param name=\"operations\">\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "                    </param>\n" +
        "				</param>\n" +
        "				<param value=\"net.didion.jwnl.dictionary.morph.TokenizerOperation\">\n" +
        "					<param name=\"delimiters\">\n" +
        "						<param value=\" \"/>\n" +
        "						<param value=\"-\"/>\n" +
        "					</param>\n" +
        "					<param name=\"token_operations\">\n" +
        "                        <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "						<param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "						<param value=\"net.didion.jwnl.dictionary.morph.DetachSuffixesOperation\">\n" +
        "							<param name=\"noun\" value=\"|s=|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y|\"/>\n" +
        "							<param name=\"verb\" value=\"|s=|ies=y|es=e|es=|ed=e|ed=|ing=e|ing=|\"/>\n" +
        "							<param name=\"adjective\" value=\"|er=|est=|er=e|est=e|\"/>\n" +
        "                            <param name=\"operations\">\n" +
        "                                <param value=\"net.didion.jwnl.dictionary.morph.LookupIndexWordOperation\"/>\n" +
        "                                <param value=\"net.didion.jwnl.dictionary.morph.LookupExceptionsOperation\"/>\n" +
        "                            </param>\n" +
        "						</param>\n" +
        "					</param>\n" +
        "				</param>\n" +
        "			</param>\n" +
        "		</param>\n" +
        "		<param name=\"dictionary_element_factory\" value=\"net.didion.jwnl.princeton.data.PrincetonWN17FileDictionaryElementFactory\"/>\n" +
        "		<param name=\"file_manager\" value=\"net.didion.jwnl.dictionary.file_manager.FileManagerImpl\">\n" +
        "			<param name=\"file_type\" value=\"net.didion.jwnl.princeton.file.PrincetonRandomAccessDictionaryFile\"/>\n" +
        "			<param name=\"dictionary_path\" value=\""+ pathToWordnet +"\"/>\n" +
        "		</param>\n" +
        "	</dictionary>\n" +
        "	<resource class=\"PrincetonResource\"/>\n" +
        "</jwnl_properties>";
        
        return jwnlXML;
    }
}
