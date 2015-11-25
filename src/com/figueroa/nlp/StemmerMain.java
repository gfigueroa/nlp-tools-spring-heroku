package com.figueroa.nlp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 *
 * @author gfigueroa
 */
public class StemmerMain {

    // POSTagger and Stemmer
    private final static String POS_TAGGER_MODEL_PATH =
    		"WEB-INF" + File.separator + "resources" + File.separator + 
            "pos_models" + File.separator + "english-left3words-distsim.tagger";
    
    private final static String POS_TAGGER_CONFIG_PATH =
    		"WEB-INF" + File.separator + "resources" + File.separator + 
            "pos_models" +  File.separator + "english-left3words-distsim.tagger.props";
    
    private final static String TAG_SEPARATOR = "_";
    private final static String WN_HOME = "WEB-INF" + File.separator + 
            "resources" +  File.separator + "WordNet-3.0";
    
    private POSTagger posTagger;
    private Stemmer stemmer;

    public StemmerMain(String path) {
        // Check if path ends with separator
    	if (!path.endsWith(File.separator)) {
    		path += File.separator;
    	}
    	
    	// Load POSTagger
        posTagger = new POSTagger(path + POS_TAGGER_MODEL_PATH, path + 
                POS_TAGGER_CONFIG_PATH, TAG_SEPARATOR);
        // Load Stemmer
        stemmer = new Stemmer(path + WN_HOME, posTagger);
    }
    
    private void stemCSVFiles(String dataDirectory, String tokenSeparator) {
        File directory = new File(dataDirectory);
        File fileList[] = directory.listFiles();

        System.out.println("***************************************************");
        System.out.println("Stemming text files for directory: " + dataDirectory +
                "...");
        System.out.println("***************************************************");
        System.out.println();

        for (int i = 0; i < fileList.length; i++) {

            File currFile = fileList[i];
            System.out.println("Processing file: " +
                    currFile.getName() +
                    " (" + (i + 1) + "/" + fileList.length + ")...");

            File stemmedFile = new File(currFile.getPath() + ".stemmed");
            try {
                // File to read
                FileInputStream fis = new FileInputStream(currFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                // File to write
                if (!stemmedFile.exists()) {
                    stemmedFile.createNewFile();
                }
                FileWriter fw = new FileWriter(stemmedFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);

                // If abstract is divided into two or more paragraphs, these are combined.
                while (br.ready()) {
                    String line = br.readLine();
                    String[] tokens = line.split(tokenSeparator);
                    String stemmedLine = tokens[0] + tokenSeparator +
                            tokens[1] + tokenSeparator;
                    
                    for (int j = 2; j < tokens.length; j++) { // First two tokens are IDs
                        String token = tokens[j];
                        String stemmedToken = stemmer.stemText(token, false);
                        stemmedLine += stemmedToken + "\t";
                    }
                    bw.write(stemmedLine + "\n");
                }

                fis.close();
                bis.close();
                br.close();
                bw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println();
        }
        System.out.println("***************************************************");
        System.out.println("Text stemmed successfully for files in directory: " + dataDirectory);
    }

    private void stemFiles(String dataDirectory) {
        File directory = new File(dataDirectory);
        File fileList[] = directory.listFiles();

        System.out.println("***************************************************");
        System.out.println("Stemming text files for directory: " + dataDirectory +
                "...");
        System.out.println("***************************************************");
        System.out.println();

        for (int i = 0; i < fileList.length; i++) {

            File currFile = fileList[i];
            System.out.println("Processing file: " +
                    currFile.getName() +
                    " (" + (i + 1) + "/" + fileList.length + ")...");

            File stemmedFile = new File(currFile.getPath() + ".stemmed");
            try {
                // File to read
                FileInputStream fis = new FileInputStream(currFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                // File to write
                if (!stemmedFile.exists()) {
                    stemmedFile.createNewFile();
                }
                FileWriter fw = new FileWriter(stemmedFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);

                // If abstract is divided into two or more paragraphs, these are combined.
                while (br.ready()) {
                    String line = br.readLine();

                    String stemmedLine = stemmer.stemText(line, false);
                    bw.write(stemmedLine + "\n");
                }

                fis.close();
                bis.close();
                br.close();
                bw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println();
        }
        System.out.println("***************************************************");
        System.out.println("Text stemmed successfully for files in directory: " + dataDirectory);
    }

    public String stemText(String text) throws Exception {
        String stemmedText = stemmer.stemText(text, false);
        return stemmedText;
    }
    
    public String tagText(String text) throws Exception {
        String taggedText = posTagger.tagText(text);
        return taggedText;
    }
}