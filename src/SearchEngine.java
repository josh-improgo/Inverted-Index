import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {
    static HashMap<String, Hashtable<String, LinkedList<Integer>>> invertedIndexMap = new HashMap<>();
    static HashMap<String, Hashtable<String, LinkedList<Integer>>> stemmedIndexMap = new HashMap<>();

    static HashSet<String> stopwordSet = new HashSet<>();
    final static int WIDTH = 1400;
    final static int HEIGHT = 800;
    static boolean appendToResultsFile = false;
    static String gui_doc, gui_word, gui_snippet, gui_pos, gui_freq;
    static SearchEngineGUI gui;

    static boolean finishedProcessing = false;
    /*
    java SearchEngine -CorpusDir PathOfDir -InvertedIndex NameOfIIndexFile -StopList NameOfStopListFile -Queries QueryFile -Results ResultsFile
     */

    /**
     * Flags            Argument
     * -CorpusDir       PathOfDir
     * -InvertedIndex   NameOfIIndexFile
     * -StopList        NameOfStopListFile
     * -Queries         QueryFile
     * -Results         ResultsFile
     * -Stemming        NameOfStemmingFile
     * -SnippetNum      snippetNumber
     * -Display         displayNum
     **/

    public static void main(String[] args) {
        int i = 0, j;
        String arg;
        char flag;
        boolean vflag = false;
        boolean stemmingFlag = false;
        String pathOfDir = "", nameOfInvertedIndexFile = "", nameOfStopListFile = "", nameOfQueryFile = "", nameOfResultsFile = "", nameOfStemmingFile = "";
        int snippetNum = 0;
        int display = 0;

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            if (arg.equals("-verbose")) {

                System.out.println("verbose mode on");
                vflag = true;

            } else if (arg.toLowerCase().equals("-corpusdir")) {

                if (i < args.length)
                    pathOfDir = args[i++];
                else
                    System.err.println("-corpusdir requires pathOfDir");

                if (vflag)
                    System.out.println("path of dir = " + pathOfDir);

            } else if (arg.toLowerCase().equals("-invertedindex")) {

                if (i < args.length)
                    nameOfInvertedIndexFile = args[i++];
                else
                    System.err.println("-invertedindex requires nameOfInvertedIndexFile");

                if (vflag)
                    System.out.println("name of index file = " + nameOfInvertedIndexFile);

            } else if (arg.toLowerCase().equals("-stoplist")) {

                if (i < args.length)
                    nameOfStopListFile = args[i++];
                else
                    System.err.println("-stoplist requires nameOfStopListFile");

                if (vflag)
                    System.out.println("name of stop list file = " + nameOfStopListFile);

            } else if (arg.toLowerCase().equals("-queries")) {

                if (i < args.length)
                    nameOfQueryFile = args[i++];
                else
                    System.err.println("-queries requires queryFile");

                if (vflag)
                    System.out.println("query file = " + nameOfQueryFile);

            } else if (arg.toLowerCase().equals("-results")) {

                if (i < args.length)
                    nameOfResultsFile = args[i++];
                else
                    System.err.println("-results requires resultsFile");

                if (vflag)
                    System.out.println("results file = " + nameOfResultsFile);
            } else if (arg.toLowerCase().equals("-stemming")) {

                System.out.println("stemming mode on");

                if (i < args.length) {
                    nameOfStemmingFile = args[i++];
                } else
                    System.err.println("-stemming requires stemmingFile");

                if (vflag)
                    System.out.println("stemming file = " + nameOfStemmingFile);

                stemmingFlag = true;
            } else if (arg.toLowerCase().equals("-snippetnum")) {

                if (i < args.length)
                    snippetNum = Integer.parseInt(args[i++]);
                else
                    System.err.println("-snippetNum requires an integer");

            } else if (arg.toLowerCase().equals("-display")) {
                if (i < args.length)
                    display = Integer.parseInt(args[i++]);
                else
                    System.err.println("-display requires an integer between 0-2");

            }
        }

        if (i == args.length) {
            System.out.println(String.format("Num of arguments: %d\tArgs Length: %d", i, args.length));
            if (!stemmingFlag) {
                System.out.println(String.format("pathOfDir = %s, nameOfIndexFile = %s, nameOfStopListFile = %s, queryFile = %s, resultsFile = %s, snippetNum = %d, display = %d", pathOfDir, nameOfInvertedIndexFile, nameOfStopListFile, nameOfQueryFile, nameOfResultsFile, snippetNum, display));
                System.out.println("Success!");
            } else {
                System.out.println(String.format("pathOfDir = %s, nameOfIndexFile = %s, nameOfStopListFile = %s, queryFile = %s, resultsFile = %s, stemmingFile = %s, snippetNum = %d, display = %d", pathOfDir, nameOfInvertedIndexFile, nameOfStopListFile, nameOfQueryFile, nameOfResultsFile, nameOfStemmingFile, snippetNum, display));
                System.out.println("Success!");
            }
        } else {
            System.err.println("Usage: ParseCmdLine [-CorpusDir PathOfDir] [-InvertedIndex NameOfIIndexFile] [-StopList NameOfStopListFile] [-Queries QueryFile] [-Results ResultsFile]");
            System.exit(0);
        }

        File stopListFile = new File(nameOfStopListFile);
        File invertedIndexFile = new File(nameOfInvertedIndexFile);
        File queryFile = new File(nameOfQueryFile);
        File resultsFile = new File(nameOfResultsFile);
        File stemmingFile = new File(nameOfStemmingFile);
        File[] files = new File(pathOfDir).listFiles();

        Stemmer stemmer = new Stemmer();




        System.out.println("Display = " + display);
        if (display == 0) { // run on file only
        } else if (display == 1){ // run on gui only
            runGUI(nameOfStopListFile, nameOfInvertedIndexFile, nameOfQueryFile, nameOfResultsFile, nameOfStemmingFile, pathOfDir, snippetNum, resultsFile, files, stemmingFlag);
        } else if (display == 2) { // run on both
            runGUI(nameOfStopListFile, nameOfInvertedIndexFile, nameOfQueryFile, nameOfResultsFile, nameOfStemmingFile, pathOfDir, snippetNum, resultsFile, files, stemmingFlag);
        } else {
            System.err.println("-display flag must be an integer between 0-2");
        }
        start(stopListFile, invertedIndexFile, queryFile, resultsFile, files, stemmingFlag, stemmingFile, snippetNum);


    }

    /**
     * Processes and compiles all files
     *
     * @param stopListFile      File type containing a list of stop words
     * @param invertedIndexFile File type which the inverted index will be saved into
     * @param queryFile         File type which contains the user's queries
     * @param resultsFile       File type that the results from queries will be saved into       File type that the results from queries will be saved into
     * @param files             File[] type which contains the directory of all HTML documents            File[] type which contains the directory of all HTML documents
     */
    public static void start(File stopListFile, File invertedIndexFile, File queryFile, File resultsFile, File[] files, boolean stemmingFlag, File stemmingFile, int snippetNum) {
        clearResultsFile(resultsFile);
        System.out.println(">>Processing stop list file...");
        processStopList(stopListFile);
        System.out.println(">>Stop list file has been processed.");
        int answer = askUserForInvertedIndexPersistence(invertedIndexFile);

        if (answer == 1) {
            System.out.println(">>The inverted index on file will be used. Please wait while we use and process the inverted index file.");
            parseInvertedIndex(invertedIndexFile);
            System.out.println(">>Inverted index on file has been processed into the inverted index map.");
        } else {
            if (answer == -1) {
                System.out.println(">>Inverted index file is empty. The corpus will be processed.");
            } else { // answer == 0
                System.out.println(">>The corpus will be reprocessed.");
            }
            System.out.println(">>Please wait until the Corpus has been processed...");
            processCorpus(files, stemmingFlag);
            System.out.println(">>The Corpus has been processed.");
            System.out.println(">>Processing inverted index to InvertedIndexFile...");
            processInvertedIndex(invertedIndexFile);
            System.out.println(">>Inverted index has been processed (found in InvertedIndexFile).");
        }

        if (stemmingFlag) {
            int persistedStemming = askUserForStemmedIndexPersistence(stemmingFile);
//            readThroughStemIndex();
            if (persistedStemming == 1) {
                System.out.println(">>The stemmed index on file will be used. Please wait while we use and process the stemmed index file.");
                parseStemmedIndex(stemmingFile);
            } else {
                if (persistedStemming == -1) {
                    System.out.println(">>Stemmed index file is empty. The corpus and inverted index will be processed.");
                } else { // answer == 0
                    System.out.println(">>The corpus and inverted index will be reprocessed.");
                }
                System.out.println(">>Please wait until the Corpus has been processed...");
                processCorpus(files, stemmingFlag);
                System.out.println(">>The Corpus has been processed.");
                System.out.println(">>Processing inverted index to InvertedIndexFile...");
                processInvertedIndex(invertedIndexFile);
                System.out.println(">>Inverted index has been processed (found in InvertedIndexFile).");
                System.out.println(">>Processing stemmed index to StemmingFile...");
                processStemmedIndex(stemmingFile);
                System.out.println(">>Stemmed index has been processed (found in StemmingFile).");
            }
        }

        System.out.println(">>Processing query file...");
        processQueryFile(queryFile, resultsFile, files, stemmingFlag, snippetNum);
        System.out.println(">>Query file has been processed (results are in ResultsFile).\n");
        finishedProcessing = true;
        String result = retrieveStatisticsForAllQueries(stemmingFlag);
        appendToResultsFile(result, resultsFile);
        takeUserInput(resultsFile, files, stemmingFlag, snippetNum);
    }

    /**
     * Processes the stop list and adding it to a hashset. Identifies which are stop words
     * based on the stopListFile given.
     *
     * @param stopListFile File type containing a list of stop words
     */
    public static void processStopList(File stopListFile) {
        Scanner scanner;
        scanner = null;

        try {
            scanner = new Scanner(new FileReader(stopListFile));

            while (scanner.hasNext()) {
                String stopword = scanner.next().toLowerCase();
                stopwordSet.add(stopword);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }

    }

    /**
     * Processes the corpus to create an inverted index
     *
     * @param files             File[] type which contains the directory of all HTML documentsFile[] type which contains the directory of all HTML documents
     * @param stemmingFlag a boolean of whether stemming is performed
     */
    public static void processCorpus(File[] files, boolean stemmingFlag) {
        for (File file : files) {
            if (file.isDirectory()) {
                /*processCorpus(file.listFiles());*/ // recursive
                System.err.println("Path of directory contains nested directories.");
                break;
            } else {

                String p_str = "(?<=>)([^<]*)(?=<)";
                String string = null;

                try {
                    // process file and put into string (to be matched with pattern)
                    string = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Pattern pattern = Pattern.compile(p_str);
                Matcher matcher = pattern.matcher(string);


                ArrayList<String> groups = new ArrayList<>();

                while (matcher.find()) {
                    String group = matcher.group();
                    groups.add(group);
                }

                int position = 0;
                for (String g : groups) {
                    for (String punct : g.split("\\p{Punct}")) {
                        for (String word : punct.split(" ")) {
                            word = word.trim().replaceAll("\\s", ""); // remove whitespace and new lines
                            word = word.replaceAll("[^a-zA-Z]", "");
                            word = word.toLowerCase();
                            if (word.equals(" ") || word.equals("") || word.equals("\t") || word.equals("\n"))
                                continue;

                            if (stopwordSet.contains(word)) { // Process the word
                                // skip stopword
                                position++;
                            } else { // is not a stopword:
                                String name = file.getName();
                                if (invertedIndexMap.containsKey(word)) { // word exists
                                    // Check if document exists
                                    if (invertedIndexMap.get(word).containsKey(name)) { // document already exists
                                        // get document
                                        LinkedList<Integer> document = invertedIndexMap.get(word).get(name);

                                        // get the frequency value
                                        int currentFrequency = document.get(0);

                                        // increment frequency by 1
                                        currentFrequency += 1;
                                        document.set(0, currentFrequency);

                                        // add position
                                        document.add(position);
                                    } else { // document does not exist
                                        // create a new document
                                        LinkedList<Integer> document = new LinkedList<>();
                                        invertedIndexMap.get(word).put(name, document);

                                        // add frequency
                                        document.add(1);

                                        // add position
                                        document.add(position);
                                    }
                                } else { // word does not exist
                                    // create a new HashTable
                                    Hashtable<String, LinkedList<Integer>> hashtable = new Hashtable<>();
                                    invertedIndexMap.put(word, hashtable);

                                    // create a new document
                                    LinkedList<Integer> document = new LinkedList<>();
                                    invertedIndexMap.get(word).put(name, document);

                                    // add frequency
                                    document.add(1);

                                    // add position
                                    document.add(position);
                                }

                                if (stemmingFlag) {
                                    String stemmedWord = stemword(word); // get stemmed word
                                    if (stemmedIndexMap.containsKey(stemmedWord)) { // word exists

                                        if (stemmedIndexMap.get(stemmedWord).containsKey(name)) { // document already exists
                                            // get document
                                            LinkedList<Integer> document = stemmedIndexMap.get(stemmedWord).get(name);

                                            // get frequency value
                                            int currentFrequency = document.get(0);

                                            // increment frequency by 1
                                            currentFrequency += 1;
                                            document.set(0, currentFrequency);

                                            // add position
                                            document.add(position);
                                        } else { // document does not exist
                                            // create a new document
                                            LinkedList<Integer> document = new LinkedList<>();
                                            stemmedIndexMap.get(stemmedWord).put(name, document);

                                            // add frequency
                                            document.add(1);

                                            // add position
                                            document.add(position);
                                        }
                                    } else { // word does not exist
                                        // create a new HashTable
                                        Hashtable<String, LinkedList<Integer>> hashtable = new Hashtable<>();
                                        stemmedIndexMap.put(stemmedWord, hashtable);

                                        // create a new document
                                        LinkedList<Integer> document = new LinkedList<>();
                                        stemmedIndexMap.get(stemmedWord).put(name, document);

                                        // add frequency
                                        document.add(1);

                                        // add position
                                        document.add(position);
                                    }

                                }

                                position++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes white spaces and condensing the string word
     * @param word String type that the user wants to query
     */
    /*private static void removeWhiteSpace(String word) {
        while (word.indexOf("\t") >= 1)
            word = word.replace("\t", " ");
        while (word.indexOf("\n") >= 1)
            word = word.replace("\n", " ");
        while (word.indexOf(" ") >= 1)
            word = word.replace(" ", " ");
        while (word.indexOf("\r") >= 1)
            word = word.replace("\r", " ");
    }*/

    /**
     * Enable user input to perform queries
     *
     * @param resultsFile       File type that the results from queries will be saved into File type that the results from queries will be saved into
     * @param files             File[] type which contains the directory of all HTML documents      File[] type which contains the directory of all HTML documents
     * @param snippetNum        an integer of the numbers representing the number of words before and after the query position
     */
    public static void takeUserInput(File resultsFile, File[] files, boolean stemmingFlag, int snippetNum) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Append queries to ResultsFile? Y\\N");
        /*boolean*/ appendToResultsFile = false;
        while (true) {
            String input = scanner.next().toLowerCase();
            if (input.equals("y")) { // append queries to result file
                appendToResultsFile = true;
                System.out.println("Query results will append to resultFile.");
                break;
            } else if (input.equals("n")) { // do not append queries
                appendToResultsFile = false;
                System.out.println("Query results will not append to resultFile.");
                break;
            } else {
                System.out.println("Invalid argument.");
            }
        }

        do {
            System.out.println("Type one of the following:" +
                    "\na) \"Query TERM\" to search TERM within the Corpus." +
                    "\nb) \"Frequency TERM\" to identify the frequency of TERM in each document." +
                    "\nc) \"Search WORD\" to get which documents WORD appears and its frequency." +
                    "\nd) \"Search DOC\" to get all words found in DOC and its frequency." +
                    "\ne) \"Snippet QUERY\" to get the a snippet of the first occurrence of the query in each document." +
                    "\nf) \"Quit\" to quit.");
            String input = scanner.nextLine().toLowerCase();
            String[] commands = input.split(" ");

            if (commands.length == 2) { // commands[0] is the action, commands[1] is the argument
                commands[0] = commands[0].toLowerCase();
                commands[1] = commands[1].toLowerCase();
                if (commands[0].equals("query")) {
//                    queryTerm(commands[1], resultsFile, appendToResultsFile, stemmingFlag);
                    String result = queryTerm(commands[1], stemmingFlag);
                    if (appendToResultsFile) appendToResultsFile(result, resultsFile);
                } else if (commands[0].equals("frequency")) {
//                    frequencyTerm(commands[1], resultsFile, appendToResultsFile, stemmingFlag);
                    String result = frequencyTerm(commands[1], stemmingFlag);
                    if (appendToResultsFile) appendToResultsFile(result, resultsFile);
                } else if (commands[0].equals("search")) {
                    ArrayList<String> fileNames = getFileNames(files);
                    boolean isFile = false;
                    for (String fileName : fileNames) {
                        if (commands[1].equals(fileName)) {
                            isFile = true;
                            break;
                        }
                    }
                    if (isFile) {
//                        searchDoc(commands[1], resultsFile, appendToResultsFile, stemmingFlag);
                        String result = searchDoc(commands[1], stemmingFlag);
                        if (appendToResultsFile) appendToResultsFile(result, resultsFile);
                    } else {
//                        searchWord(commands[1], resultsFile, appendToResultsFile, stemmingFlag);
                        String result = searchWord(commands[1], stemmingFlag);
                        if (appendToResultsFile) appendToResultsFile(result, resultsFile);
                    }
                } else if (commands[0].equals("snippet")) {

                    String query = input.substring("snippet".length() + 1);
                    String result = getSnippet(files, query, snippetNum, stemmingFlag);
//                    getSnippet(files, resultsFile, query, snippetNum, true, stemmingFlag);
                    if (appendToResultsFile) appendToResultsFile(result, resultsFile);

                } else
                    System.out.println("Invalid command.");

            } else if (commands.length == 1) {
                commands[0] = commands[0].toLowerCase();
                if (commands[0].equals("quit")) {
                    System.out.println("Exiting program...");
                    System.exit(0);
                    break;
                } else
                    System.out.println("Invalid command.");
            } else if (commands.length >= 2) {
                if (commands[0].equals("snippet")) {
                    String query = input.substring("snippet".length() + 1);

                    String result = getSnippet(files, query, snippetNum, stemmingFlag);
//                    getSnippet(files, resultsFile, query, snippetNum, true, stemmingFlag);
                    if (appendToResultsFile) appendToResultsFile(result, resultsFile);
                } else {
                    System.out.println("Invalid command.");
                }
            } else {
                System.out.println("Invalid command.");
            }
        } while (true);
    }

    public static void takeGUIInput(File resultsFile, File[] files, boolean stemmingFlag, int snippetNum, boolean appendToResultsFile) {
        gui.runCommands(resultsFile, files, stemmingFlag, snippetNum, appendToResultsFile);
    }


    /**
     * Retrieve all file names
     *
     * @param files             File[] type which contains the directory of all HTML documentsFile[] type which contains the directory of all HTML documents
     * @return ArrayList<String> type containing the names of all the HTML documents
     */
    public static ArrayList<String> getFileNames(File[] files) {
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : files)
            fileNames.add(file.getName().toLowerCase());

        return fileNames;
    }

    /**
     * Processes the query file from accepted command arguments
     *
     * @param queryFile   File type which contains the user's queries
     * @param resultsFile       File type that the results from queries will be saved into File type that the results from queries will be saved into
     * @param files             File[] type which contains the directory of all HTML documents      File[] type which contains the directory of all HTML documents\
     * @param snippetNum 
     */
    public static void processQueryFile(File queryFile, File resultsFile, File[] files, boolean stemmingFlag, int snippetNum) {
        Scanner scanner;
        scanner = null;

        try {
            scanner = new Scanner(new FileReader(queryFile));

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().toLowerCase();
                String[] commands = input.split(" ");
                if (commands.length == 2) {
                    commands[0] = commands[0].toLowerCase();
                    commands[1] = commands[1].toLowerCase();
                    if (commands[0].equals("query")) {
//                        queryTerm(commands[1], resultsFile, true, stemmingFlag);
                        String result = queryTerm(commands[1], stemmingFlag);
                        appendToResultsFile(result, resultsFile);
                    } else if (commands[0].equals("frequency")) {
//                        frequencyTerm(commands[1], resultsFile, true, stemmingFlag);
                        String result = frequencyTerm(commands[1], stemmingFlag);
                        appendToResultsFile(result, resultsFile);
                    } else if (commands[0].equals("search")) {
                        ArrayList<String> fileNames = getFileNames(files);
                        boolean isFile = false;
                        for (String fileName : fileNames) {
                            if (commands[1].equals(fileName)) {
                                isFile = true;
                                break;
                            }
                        }
                        if (isFile) {
//                            searchDoc(commands[1], resultsFile, true, stemmingFlag);
                            String result = searchDoc(commands[1], stemmingFlag);
                            appendToResultsFile(result, resultsFile);
                        } else {
//                            searchWord(commands[1], resultsFile, true, stemmingFlag);
                            String result = searchWord(commands[1], stemmingFlag);
                            appendToResultsFile(result, resultsFile);
                        }
                    } else if (commands[0].equals("snippet")) {

                        String query = input.substring("snippet".length() + 1);
                        String result = getSnippet(files, query, snippetNum, stemmingFlag);

//                        getSnippet(files, resultsFile, query, snippetNum, true, stemmingFlag);
                        appendToResultsFile(result, resultsFile);

                    } else {
                        System.out.println("Invalid command.");
                    }
                } else if (commands.length >= 2) {
                    if (commands[0].equals("snippet")) {
                        String query = input.substring("snippet".length() + 1);
                        String result = getSnippet(files, query, snippetNum, stemmingFlag);
//                        getSnippet(files, resultsFile, query, snippetNum, true, stemmingFlag);
                        appendToResultsFile(result, resultsFile);

                    } else {
                        System.out.println("Invalid command.");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Read through the entire inverted index
     */
    public static void readThroughResults() {
        for (String word : invertedIndexMap.keySet()) { // iterate through all words
            System.out.println("Word: " + word);
            for (String document : invertedIndexMap.get(word).keySet()) { // iterate through all documents containing word
                System.out.println("\tDocument: " + document);
                int index = 0;
                for (Integer value : invertedIndexMap.get(word).get(document)) { // iterate through all nodes in linked list
                    if (index != 0) {
                        System.out.println("\t\tPosition " + index + ": " + value);
                    } else {
                        System.out.println("\t\tFrequency: " + value);
                    }
                    index++;
                }
            }
        }
    }

    /**
     * Read through the entire stemmed index
     */
    public static void readThroughStemIndex() {
        System.out.println("Reading through");
        for (String word : stemmedIndexMap.keySet()) { // iterate through all words
            System.out.println("Word: " + word);
            for (String document : stemmedIndexMap.get(word).keySet()) { // iterate through all documents containing word
                System.out.println("\tDocument: " + document);
                int index = 0;
                for (Integer value : stemmedIndexMap.get(word).get(document)) { // iterate through all nodes in linked list
                    if (index != 0) {
                        System.out.println("\t\tPosition " + index + ": " + value);
                    } else {
                        System.out.println("\t\tFrequency: " + value);
                    }
                    index++;
                }
            }
        }
    }

    /**
     * Read all words that are in the inverted index
     */
    public static void readWords() {
        for (String word : invertedIndexMap.keySet())
            System.out.println("Word: " + word);
    }

    /**
     * Performs a query on a term and returns document, total frequency, and positions
     *
     * @param term                String type that the user wants to query
     * @param resultsFile       File type that the results from queries will be saved into         File type that the results from queries will be saved into
     * @param appendToResultsFile boolean type that checks whether the user wants to append results to the resultsFile
     * @param stemmingFlag
     */
    public static void queryTerm(String term, File resultsFile, boolean appendToResultsFile, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Query of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if hashmap contains term
            stringBuilder.append("\t\t{Document, Frequency: [Positions]}\n");
            StringBuilder doc_string = new StringBuilder();
            for (String document : map.get(term).keySet()) { // iterate through each document that contains the word
                doc_string.append("\t\t{" + document + ", ");

                int index = 0; // initialize pointer for linked list

                for (Integer value : map.get(term).get(document)) { // iterate through each linked list node
                    if (index != 0) {

                        if (index != 1) // for printing purposes, first position does not contain comma
                            doc_string.append(", " + value);
                        else
                            doc_string.append(value);

                    } else // first term is the frequency of the word in document
                        doc_string.append(value + ": [");

                    index++;
                }
                doc_string.append("]}\n");
            }
            stringBuilder.append(String.format("%s\n", doc_string));
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }

        if (appendToResultsFile) {
            System.out.println("\tOutputting results to ResultsFile...");
            BufferedWriter bufferedWriter;
            bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
                bufferedWriter.append(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(stringBuilder);
    }

    public static String queryTerm(String term, boolean stemmingFlag) {
        gui_doc = gui_word = gui_snippet = gui_pos = gui_freq = "";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Query of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if hashmap contains term
            stringBuilder.append("\t\t{Document, Frequency: [Positions]}\n");
            StringBuilder doc_string = new StringBuilder();
            for (String document : map.get(term).keySet()) { // iterate through each document that contains the word
                doc_string.append("\t\t{" + document + ", ");
                gui_doc += document + "\n";
                int index = 0; // initialize pointer for linked list

                for (Integer value : map.get(term).get(document)) { // iterate through each linked list node
                    if (index != 0) {

                        if (index != 1) { // for printing purposes, first position does not contain comma
                            doc_string.append(", " + value);
                            gui_pos += value + " ";
                        } else {
                            doc_string.append(value);
                            gui_pos += "\n";
                            gui_freq += value + " ";
                        }

                    } else // first term is the frequency of the word in document
                        doc_string.append(value + ": [");

                    index++;
                }
                doc_string.append("]}\n");
            }
            stringBuilder.append(String.format("%s\n", doc_string));
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }
        gui_word = term;
        gui_snippet = "N/A";
//        gui.modifyStatistics(gui_doc, gui_word, gui_snippet, gui_pos, gui_freq);
//        gui_doc, gui_word, gui_snippet, gui_pos, gui_freq;
        return stringBuilder.toString();
    }
    /**
     * Performs a query on the frequency of the term and returns the document and associated frequency
     *
     * @param term                String type that the user wants to query
     * @param resultsFile       File type that the results from queries will be saved into         File type that the results from queries will be saved into
     * @param appendToResultsFile boolean type that checks whether the user wants to append results to the resultsFile
     * @param stemmingFlag
     */
    public static void frequencyTerm(String term, File resultsFile, boolean appendToResultsFile, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Frequency of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if term is in index
            stringBuilder.append("\t\t{Document, Frequency}\n");
            int totalFrequency = 0;
            for (String document : map.get(term).keySet()) { // iterate through documents that term is found in
                int frequency = map.get(term).get(document).getFirst(); // get first node in linked list (which contains frequency)
                totalFrequency += frequency;

                stringBuilder.append(String.format("\t\t{%s, %s}\n", document, frequency));
            }
            stringBuilder.append("\t\tTotal Frequency: " + totalFrequency + "\n");
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }

        if (appendToResultsFile) {
            System.out.println("\tOutputting results to ResultsFile...");
            BufferedWriter bufferedWriter;
            bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
                bufferedWriter.append(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(stringBuilder);
    }

    public static String frequencyTerm(String term, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Frequency of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if term is in index
            stringBuilder.append("\t\t{Document, Frequency}\n");
            int totalFrequency = 0;
            for (String document : map.get(term).keySet()) { // iterate through documents that term is found in
                int frequency = map.get(term).get(document).getFirst(); // get first node in linked list (which contains frequency)
                totalFrequency += frequency;

                stringBuilder.append(String.format("\t\t{%s, %s}\n", document, frequency));
            }
            stringBuilder.append("\t\tTotal Frequency: " + totalFrequency + "\n");
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Prints the inverted index to invertedIndexFile
     *
     * @param invertedIndexFile File type which the inverted index will be saved into
     */
    public static void processInvertedIndex(File invertedIndexFile) {
        BufferedWriter bufferedWriter;
        bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(invertedIndexFile, false));
            bufferedWriter.append("\"Word\"\n\t{Document, Frequency: [Location]}\n");

            for (String word : invertedIndexMap.keySet()) { // iterate through each word in hashmap
                StringBuilder doc_string = new StringBuilder();
                for (String document : invertedIndexMap.get(word).keySet()) { // iterate through each document containing the word
                    doc_string.append("\t{" + document + ", ");
                    int index = 0; // pointer for each node in linked list
                    for (Integer value : invertedIndexMap.get(word).get(document)) { // iterate through each node in linked list
                        if (index != 0) { // not the first node
                            if (index != 1) // (for printing purposes, first value does not contain comma)
                                doc_string.append(", " + value);
                            else
                                doc_string.append(value);

                        } else { // the first node (contains the frequency)
                            doc_string.append(value + ": [");
                        }
                        index++;
                    }
                    doc_string.append("]}\n");
                }
                bufferedWriter.append(String.format("\"%s\"\n%s\n", word, doc_string));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.append("\n");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the stemmed index to stemmedIndexFile
     *
     * @param stemmedIndexFile File type which the inverted index will be saved into
     */
    public static void processStemmedIndex(File stemmedIndexFile) {
        BufferedWriter bufferedWriter;
        bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(stemmedIndexFile, false));
            bufferedWriter.append("\"Word\"\n\t{Document, Frequency: [Location]}\n");

            for (String word : stemmedIndexMap.keySet()) { // iterate through each word in hashmap
                StringBuilder doc_string = new StringBuilder();
                for (String document : stemmedIndexMap.get(word).keySet()) { // iterate through each document containing the word
                    doc_string.append("\n\t{" + document + ", ");
                    int index = 0; // pointer for each node in linked list
                    for (Integer value : stemmedIndexMap.get(word).get(document)) { // iterate through each node in linked list
                        if (index != 0) { // not the first node
                            if (index != 1) // (for printing purposes, first value does not contain comma)
                                doc_string.append(", " + value);
                            else
                                doc_string.append(value);

                        } else { // the first node (contains the frequency)
                            doc_string.append(value + ": [");
                        }
                        index++;
                    }
                    doc_string.append("]}");
                }
                bufferedWriter.append(String.format("\"%s\"\n\t%s\n", word, doc_string));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.append("\n");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Search the word in the corpus and returns the document and its associated frequency
     *
     * @param term                String type that the user wants to query
     * @param resultsFile       File type that the results from queries will be saved into         File type that the results from queries will be saved into
     * @param appendToResultsFile boolean type that checks whether the user wants to append results to the resultsFile
     * @param stemmingFlag a boolean of whether stemming is performed
     */
    public static void searchWord(String term, File resultsFile, boolean appendToResultsFile, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Word Search of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if term is in hashmap
            stringBuilder.append("\t\t{Document, Frequency}\n");
            StringBuilder doc_string = new StringBuilder();
            for (String document : map.get(term).keySet()) { // iterate through documents that term is found in
                doc_string.append("\t\t{" + document + ", " + map.get(term).get(document).getFirst()); // get first node in linked list (contains frequency of term)
                doc_string.append("}\n");
            }
            stringBuilder.append(String.format("%s\n", doc_string));
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }

        if (appendToResultsFile) {
            System.out.println("\tOutputting results to ResultsFile...");
            BufferedWriter bufferedWriter;
            bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
                bufferedWriter.append(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(stringBuilder);

    }

    public static String searchWord(String term, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Word Search of ").append(term);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");

        stringBuilder.append("\n");

        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
            term = stemword(term);
            stringBuilder.append("\tStem word: ").append(term).append("\n");
        } else map = invertedIndexMap;

        if (map.containsKey(term)) { // check if term is in hashmap
            stringBuilder.append("\t\t{Document, Frequency}\n");
            StringBuilder doc_string = new StringBuilder();
            for (String document : map.get(term).keySet()) { // iterate through documents that term is found in
                doc_string.append("\t\t{" + document + ", " + map.get(term).get(document).getFirst()); // get first node in linked list (contains frequency of term)
                doc_string.append("}\n");
            }
            stringBuilder.append(String.format("%s\n", doc_string));
        } else {
            stringBuilder.append("\t\tNo results.\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Searches the document and retrieves all words and its associated frequency within that document
     *
     * @param doc                 String type for the file name of the document
     * @param resultsFile       File type that the results from queries will be saved into         File type that the results from queries will be saved into
     * @param appendToResultsFile boolean type that checks whether the user wants to append results to the resultsFile
     */
    public static void searchDoc(String doc, File resultsFile, boolean appendToResultsFile, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Document Search of ").append(doc);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");
        stringBuilder.append("\n");

        stringBuilder.append("\t\t{Word, Frequency}\n");


        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
        } else map = invertedIndexMap;

        boolean hasResults = false;
        for (String word : map.keySet()) { // iterate through hashmap for each word
            for (String document : map.get(word).keySet()) { // iterate through each document that word is found in
                if (document.toLowerCase().equals(doc.toLowerCase())) { // if document matches the doc argument
                    int frequency = map.get(word).get(document).getFirst(); // get first node in linked list (contains frequency of word)
                    stringBuilder.append("\t\t{" + word + ", " + frequency + "}\n");
                    hasResults = true;
                }
            }
        }
        if (!hasResults)
            stringBuilder.append("\t\tNo results.\n");

        if (appendToResultsFile) {
            System.out.println("\tOutputting results to ResultsFile...");
            BufferedWriter bufferedWriter;
            bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
                bufferedWriter.append(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(stringBuilder);
    }

    public static String searchDoc(String doc, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving Document Search of ").append(doc);

        if (stemmingFlag) stringBuilder.append(" with stemming on.");
        stringBuilder.append("\n");

        stringBuilder.append("\t\t{Word, Frequency}\n");


        HashMap<String, Hashtable<String, LinkedList<Integer>>> map;
        if (stemmingFlag) {
            map = stemmedIndexMap;
        } else map = invertedIndexMap;

        boolean hasResults = false;
        for (String word : map.keySet()) { // iterate through hashmap for each word
            for (String document : map.get(word).keySet()) { // iterate through each document that word is found in
                if (document.toLowerCase().equals(doc.toLowerCase())) { // if document matches the doc argument
                    int frequency = map.get(word).get(document).getFirst(); // get first node in linked list (contains frequency of word)
                    stringBuilder.append("\t\t{" + word + ", " + frequency + "}\n");
                    hasResults = true;
                }
            }
        }
        if (!hasResults)
            stringBuilder.append("\t\tNo results.\n");

        return stringBuilder.toString();
    }

    /**
     * Gets the snippets of the documents
     * @param files             File[] type which contains the directory of all HTML documents
     * @param resultsFile       File type that the results from queries will be saved into
     * @param query             the query that is searched for the snippet
     * @param snippetNum        an integer of the numbers representing the number of words before and after the query position
     * @param appendToResultsFile boolean type that checks whether the user wants to append results to the resultsFile
     * @param stemmingFlag a boolean of whether stemming is performed
     */
    public static void getSnippet(File[] files, File resultsFile, String query, int snippetNum, boolean appendToResultsFile, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving snippet of \"").append(query).append("\"");
        if (stemmingFlag) stringBuilder.append(" with stemming on.");
        stringBuilder.append("\n");

        stringBuilder.append(retrieveSnippet(files, query, snippetNum, stemmingFlag));

        if (appendToResultsFile) {
//            System.out.println("\tOutputting results to ResultsFile...");
            BufferedWriter bufferedWriter;
            bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
                bufferedWriter.append(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedWriter.append("\n");
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(stringBuilder);
    }

    public static String getSnippet(File[] files, String query, int snippetNum, boolean stemmingFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Retrieving snippet of \"").append(query).append("\"");
        if (stemmingFlag) stringBuilder.append(" with stemming on.");
        stringBuilder.append("\n");

        stringBuilder.append(retrieveSnippet(files, query, snippetNum, stemmingFlag));

        return stringBuilder.toString();
    }

    public static void appendToResultsFile(String string, File resultsFile) {
        BufferedWriter bufferedWriter;
        bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, true));
            bufferedWriter.append(string);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.append("\n");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Porter's algorithm stemming that will be applied on the word
     * @param word a string that Porter's Algorithm will be performed on
     * @return a string of the stemmed word
     */
    public static String stemword(String word) {
        Stemmer s = new Stemmer();

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            ch = Character.toLowerCase(ch);
            s.add(ch);
        }
        s.stem();
        String u;
        u = s.toString();
        return u;
    }

    /**
     * Retrieves the snippet based on the query
     * @param files an array of files from the path of the directory
     * @param query the query that is searched for the snippet
     * @param snippetNum an integer of the numbers representing the number of words before and after the query position
     * @param stemmingFlag a boolean of whether stemming is performed
     * @return a string containing the snippet of the first occurrence in each document
     */
    public static String retrieveSnippet(File[] files, String query, int snippetNum, boolean stemmingFlag) {


        ArrayList<String> nonStopwordsInQuery = new ArrayList<>();
        ArrayList<String> documentListContainingQuery = new ArrayList<>();

        String queryWithoutPunctuation = query.replaceAll("[^a-zA-Z ]", "");
        String[] words = queryWithoutPunctuation.toLowerCase().split("\\s");

        StringBuilder snippets = new StringBuilder();
        // only look through query that are non-stopwords
        for (String word : words) {
            if (!stopwordSet.contains(word)) {
                nonStopwordsInQuery.add(word);
            }
        }

        if (!stemmingFlag) {
            // **INVERTED INDEX ITERATION**
            for (String word : invertedIndexMap.keySet()) {
                if (nonStopwordsInQuery.size() == 0) return null;

                String firstWord = nonStopwordsInQuery.get(0);
                if (firstWord.equals(word)) { // if the word in the nonstopwords query exists
                    documentListContainingQuery.addAll(invertedIndexMap.get(word).keySet());
                }
            }

            // iterate through the other words
            for (String queryWord : nonStopwordsInQuery) {
                for (String word : invertedIndexMap.keySet()) {
                    if (queryWord.equals(word)) {
                        ArrayList<String> removeDocuments = new ArrayList<>();
                        for (String queryDocument : documentListContainingQuery) {
                            boolean containsThisDocument = false;
                            for (String document : invertedIndexMap.get(word).keySet()) {
                                if (queryDocument.equals(document)) containsThisDocument = true;

                            }
                            // if it does not contain this document, then remove this document
                            if (!containsThisDocument) removeDocuments.add(queryDocument);
                        }

                        for (String document : removeDocuments)
                            documentListContainingQuery.remove(document);
                    }
                }
            }

            boolean foundCurrentWord = false;
            for (String queryWord : nonStopwordsInQuery) {
                for (String word : invertedIndexMap.keySet()) {
                    if (foundCurrentWord) break;
                    if (queryWord.equals(word)) {
                        for (String queryDocument : documentListContainingQuery) {
                            for (String document : invertedIndexMap.get(word).keySet()) {
                                if (foundCurrentWord) break;
                                if (queryDocument.equals(document)) {
                                    int index = 0;
                                    for (Integer value : invertedIndexMap.get(word).get(document)) {
                                        if (index != 0) {
                                            // the position
                                            int queryPosition = value;
                                            String snippet = iterateThroughDocument(files, document, snippetNum, queryPosition);
                                            snippets.append("\t").append(document).append("\t").append(word).append("\n\t\t").append(snippet).append("\n");
                                            foundCurrentWord = true;
//                                            return snippets.toString();
//                                            break;
                                        } else {
                                            // the frequency
                                        }
                                        index++;
                                    }
                                }
                            }
                        }
                    }
                }
                foundCurrentWord = false;
            }
        } else {
            // **STEMMED INDEX ITERATION**
            ArrayList<String> stemmedWordsInQuery = new ArrayList<>();

            for (String queryWord : nonStopwordsInQuery) {
                String stemmedQueryWord = stemword(queryWord);
                stemmedWordsInQuery.add(stemmedQueryWord);
            }

            for (String word : stemmedIndexMap.keySet()) {
                if (stemmedWordsInQuery.size() == 0) return null;

                String firstWord = stemmedWordsInQuery.get(0);
                if (firstWord.equals(word)) { // if the word in the nonstopwords query exists
                    for (String document : stemmedIndexMap.get(word).keySet()) {
                        documentListContainingQuery.add(document);
                    }
                }
            }

            // iterate through the other words
            for (String stemmedQueryWord : stemmedWordsInQuery) {
                for (String word : stemmedIndexMap.keySet()) {
                    if (stemmedQueryWord.equals(word)) {
                        ArrayList<String> removeDocuments = new ArrayList<>();
                        for (String queryDocument : documentListContainingQuery) {
                            boolean containsThisDocument = false;
                            for (String document : stemmedIndexMap.get(word).keySet()) {
                                if (queryDocument.equals(document)) containsThisDocument = true;

                            }
                            // if it does not contain this document, then remove this document
                            if (!containsThisDocument) removeDocuments.add(queryDocument);
                        }

                        for (String document : removeDocuments)
                            documentListContainingQuery.remove(document);
                    }
                }
            }

            boolean foundCurrentWord = false;
            for (String stemmedQueryWord : stemmedWordsInQuery) {
                for (String word : stemmedIndexMap.keySet()) {
                    if (foundCurrentWord) break;
                    if (stemmedQueryWord.equals(word)) {
                        for (String queryDocument : documentListContainingQuery) {
                            for (String document : stemmedIndexMap.get(word).keySet()) {
                                if (foundCurrentWord) break;
                                if (queryDocument.equals(document)) {
                                    int index = 0;
                                    for (Integer value : stemmedIndexMap.get(word).get(document)) {
                                        if (index != 0) {
                                            // the position
                                            int queryPosition = value;
                                            String snippet = iterateThroughDocument(files, document, snippetNum, queryPosition);
                                            snippets.append("\t").append(document).append("\t").append(word).append("\n\t\t").append(snippet).append("\n");
                                            foundCurrentWord = true;
//                                            return snippets.toString();

                                            break;

                                        } else {

                                        }
                                        index++;
                                    }
                                }
                            }
                        }
                    }
                }
                foundCurrentWord = false;
            }
        }

        return snippets.toString();
    }


    /**
     * Iterate through the documents
     * @param files an array of files from the path of the directory
     * @param document a string containing the document
     * @param snippetNum an integer of the numbers representing the number of words before and after the query position
     * @param queryPosition an integer of the position of the query word in the document
     * @return a string containing all the snippets of the documents
     */
    public static String iterateThroughDocument(File[] files, String document, int snippetNum, int queryPosition) {
        StringBuilder snippet = new StringBuilder();
        for (File file : files) {
            if (file.isDirectory()) {
                System.err.println("Path of directory contains nested directories.");
                break;
            } else {
                if (file.getName().equals(document)) {
                    String p_str = "(?<=>)([^<]*)(?=<)";
                    String string = null;

                    try {
                        string = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Pattern pattern = Pattern.compile(p_str);
                    Matcher matcher = pattern.matcher(string);


                    ArrayList<String> groups = new ArrayList<>();

                    while (matcher.find()) {
                        String group = matcher.group();
                        groups.add(group);
                    }

                    int position = 0;
                    for (String g : groups) {
                        for (String punct : g.split("\\p{Punct}")) {
                            for (String word : punct.split(" ")) {
                                word = word.trim().replaceAll("\\s", ""); // remove whitespace and new lines
                                word = word.replaceAll("[^a-zA-Z]", "");
                                word = word.toLowerCase();
                                if (word.equals(" ") || word.equals("") || word.equals("\t") || word.equals("\n"))
                                    continue;

                                if (queryPosition - snippetNum <= position && position <= queryPosition + snippetNum)
                                    snippet.append(word).append(" ");

                                position++;
                                if (position > queryPosition + snippetNum) break; // ?
                            }
                        }
                    }
                }
            }
        }

//        System.out.println(snippet);
        return snippet.toString();
    }


    /**
     * Parses the inverted index file
     * @param invertedIndexFile File type which the inverted index will be saved into
     */
    public static void parseInvertedIndex(File invertedIndexFile) {
        Scanner scanner = null;

        try {
            scanner = new Scanner(new FileReader(invertedIndexFile));
            String line;
            String currentWord = null;

            // skip initial headers
            scanner.nextLine();
            scanner.nextLine();

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                // if it is wrapped in quotes, it is a word
                if (line.trim().startsWith("\"") && line.trim().endsWith("\"")) {
                    // extract the word
                    Pattern pattern = Pattern.compile("\"([^\"]*)\"");
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        currentWord = matcher.group(1);
                    }
                } else {
                    if (currentWord != null) {

                        //if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
//                        System.out.println("This is an occurrence: " + line);

                        Pattern pattern = Pattern.compile("\\{([^{}]*)\\}");
                        Matcher matcher = pattern.matcher(line);

                        while (matcher.find()) {
                            // document, frequency: positions

                            String occurrenceString = matcher.group(1);
                            String commaDelimeter = ",";
                            String colonDelimeter = ":";

                            int commaIndex = occurrenceString.indexOf(commaDelimeter);
                            String documentString = occurrenceString.substring(0, commaIndex);

                            int colonIndex = occurrenceString.indexOf(colonDelimeter);
                            String frequencyString = occurrenceString.substring(commaIndex + 1, colonIndex).trim();

                            Pattern positionsPattern = Pattern.compile("\\[([^\\[\\]]*)\\]");
                            Matcher positionsMatcher = positionsPattern.matcher(line);

                            Hashtable<String, LinkedList<Integer>> hashtable = new Hashtable<>();

                            if (!invertedIndexMap.containsKey(currentWord)) { // word exists
                                invertedIndexMap.put(currentWord, hashtable);
                            }

                            LinkedList<Integer> document = new LinkedList<>();
                            invertedIndexMap.get(currentWord).put(documentString, document);

                            document.add(Integer.parseInt(frequencyString));

                            while (positionsMatcher.find()) {
                                String[] positions = positionsMatcher.group(1).split(", ");
                                for (String position : positions) {
                                    document.add(Integer.parseInt(position));
                                }
                            }
                        }

                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Parses the stemmed index from the file
     * @param stemmingFile File type containing the stemming file
     */
    public static void parseStemmedIndex(File stemmingFile) {
        Scanner scanner = null;

        try {
            scanner = new Scanner(new FileReader(stemmingFile));
            String line;
            String currentWord = null;

            // skip initial headers
            scanner.nextLine();
            scanner.nextLine();

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                // if it is wrapped in quotes, it is a word
                if (line.trim().startsWith("\"") && line.trim().endsWith("\"")) {
                    // extract the word
                    Pattern pattern = Pattern.compile("\"([^\"]*)\"");
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        currentWord = matcher.group(1);
                    }
                } else {
                    if (currentWord != null) {

                        //if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
//                        System.out.println("This is an occurrence: " + line);

                        Pattern pattern = Pattern.compile("\\{([^{}]*)\\}");
                        Matcher matcher = pattern.matcher(line);

                        while (matcher.find()) {
                            // document, frequency: positions

                            String occurrenceString = matcher.group(1);
                            String commaDelimeter = ",";
                            String colonDelimeter = ":";

                            int commaIndex = occurrenceString.indexOf(commaDelimeter);
                            String documentString = occurrenceString.substring(0, commaIndex);

                            int colonIndex = occurrenceString.indexOf(colonDelimeter);
                            String frequencyString = occurrenceString.substring(commaIndex + 1, colonIndex).trim();

                            Pattern positionsPattern = Pattern.compile("\\[([^\\[\\]]*)\\]");
                            Matcher positionsMatcher = positionsPattern.matcher(line);

                            Hashtable<String, LinkedList<Integer>> hashtable = new Hashtable<>();

                            if (!stemmedIndexMap.containsKey(currentWord)) { // word exists
                                stemmedIndexMap.put(currentWord, hashtable);
                            }
                            LinkedList<Integer> document = new LinkedList<>();
                            stemmedIndexMap.get(currentWord).put(documentString, document);

                            document.add(Integer.parseInt(frequencyString));

                            while (positionsMatcher.find()) {
                                String[] positions = positionsMatcher.group(1).split(", ");
                                for (String position : positions) {
                                    document.add(Integer.parseInt(position));
                                }
                            }
                        }

                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Asks the user for inverted index persistence
     * @param invertedIndexFile File type which the inverted index will be saved into
     * @return an integer that indicates what answer or option the user receives
     */
    public static int askUserForInvertedIndexPersistence(File invertedIndexFile) {
        String string = null;

        try {
            string = new String(Files.readAllBytes(Paths.get(invertedIndexFile.getPath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (string.trim().isEmpty()) {
            // continue to parse the inverted index
//            System.out.println(">>Inverted index file is empty. The corpus will be processed.");
            return -1;
        } else {
            System.out.println(">>It looks like you have an inverted index on file. Would you like to use this file? If No, the corpus will be reprocessed to create a new inverted index. Type Y/N.");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.next().toLowerCase();
                if (input.equals("y")) {
                    return 1;
                } else if (input.equals("n")) {
                    return 0;
                } else {
                    System.out.println("Invalid argument.");
                }
            }
        }
    }

    /**
     * Asks the user for stemmed index persistence
     * @param stemmingFile File type containing the stemming file
     * @return an integer that indicates what answer or option the user receives
     */
    public static int askUserForStemmedIndexPersistence(File stemmingFile) {
        String string = null;

        try {
            string = new String(Files.readAllBytes(Paths.get(stemmingFile.getPath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (string.trim().isEmpty()) {
            // continue to parse the stemmed index
            return -1;
        } else {
            System.out.println(">>It looks like you have a stemmed index on file. Would you like to use this file? If No, the corpus and inverted index will be reprocessed to create a new stemmed index. Type Y/N.");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.next().toLowerCase();
                if (input.equals("y")) {
                    return 1;
                } else if (input.equals("n")) {
                    return 0;
                } else {
                    System.out.println("Invalid argument.");
                }
            }
        }
    }

    public static void runGUI(String nameOfStopListFile, String nameOfInvertedIndexFile, String nameOfQueryFile, String nameOfResultsFile, String nameOfStemmingFile, String pathOfDir, int snippetNum, File resultsFile, File[] files, boolean stemmingFlag) {
        gui = new SearchEngineGUI("Search Engine", nameOfStopListFile, nameOfInvertedIndexFile, nameOfQueryFile, nameOfResultsFile, nameOfStemmingFile, pathOfDir, snippetNum);
        gui.setSize(WIDTH, HEIGHT);
        gui.setVisible(true);


        gui.getBtn_submit().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                takeGUIInput(resultsFile, files, stemmingFlag, snippetNum, appendToResultsFile);
                if (finishedProcessing) {
//                    gui.editRecallAndPrecision();
                } else
                    System.out.println("Please wait until finished processing");
            }
        });
    }


    // Recall and Precision
    // Recall: RelevantQ/RelevantC
    // Precision: RelevantQ/ResponsesQ

    public static double calculateRecall(double relevantQ) {
        // relevantQ: # of documents that are returned and relevant to query
        // relevantC: the # of corpus documents that are relevant to your specific Query (20 docs relevant to specific query)
        double relevantC = 20;
        return relevantQ / relevantC;
    }

    public static double calculatePrecision(double relevantQ, double responsesQ) {
        // relevantQ/responsesQ
        // relevantQ: # of documents that are returned and relevant to query
        // responsesQ: # of documents that the search engine returned

        return relevantQ / responsesQ;

    }

    public static void clearResultsFile(File resultsFile) {
        BufferedWriter bufferedWriter;
        bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(resultsFile, false));
            bufferedWriter.append("");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.append("\n");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static double[] calculateRecallAndPrecisionForQuery(String query, boolean stemmingFlag) {


        ArrayList<String> nonStopwordsInQuery = new ArrayList<>();
        ArrayList<String> documentListContainingQuery = new ArrayList<>();

        String queryWithoutPunctuation = query.replaceAll("[^a-zA-Z ]", "");
        String[] words = queryWithoutPunctuation.toLowerCase().split("\\s");

        // only look through query that are non-stopwords
        for (String word : words) {
            if (!stopwordSet.contains(word)) {
                nonStopwordsInQuery.add(word);
            }
        }
        // relevantQ = # of documents that are returned and relevant to query
        double relevantQ = 0;
        double responsesQ = 0;
        if (!stemmingFlag) {
            // **INVERTED INDEX ITERATION**
            for (String word : invertedIndexMap.keySet()) {
                if (nonStopwordsInQuery.size() == 0) return null;

                String firstWord = nonStopwordsInQuery.get(0);
                if (firstWord.equals(word)) { // if the word in the nonstopwords query exists
                    documentListContainingQuery.addAll(invertedIndexMap.get(word).keySet());
                }
            }

            // iterate through the other words
            for (String queryWord : nonStopwordsInQuery) {
                for (String word : invertedIndexMap.keySet()) {
                    if (queryWord.equals(word)) {
                        ArrayList<String> removeDocuments = new ArrayList<>();
                        for (String queryDocument : documentListContainingQuery) {
                            boolean containsThisDocument = false;
                            for (String document : invertedIndexMap.get(word).keySet()) {
                                if (queryDocument.equals(document)) containsThisDocument = true;

                            }
                            // if it does not contain this document, then remove this document
                            if (!containsThisDocument) removeDocuments.add(queryDocument);
                        }

                        for (String document : removeDocuments)
                            documentListContainingQuery.remove(document);
                    }
                }
            }

            for (String queryWord : nonStopwordsInQuery) { // iterate through the words in the query
                for (String word : invertedIndexMap.keySet()) { // iterate through all words in map
                    if (queryWord.equals(word)) { // if the word is matched with the word in the map
                        for (String queryDocument : documentListContainingQuery) { // go through the documents the word contains
                            for (String document : invertedIndexMap.get(word).keySet()) { // if the document
                                if (queryDocument.equals(document)) {
                                    relevantQ++;
                                }
                                responsesQ++;
                            }
                        }
                    }
                }
            }
        } else {
            // **STEMMED INDEX ITERATION**
            ArrayList<String> stemmedWordsInQuery = new ArrayList<>();

            for (String queryWord : nonStopwordsInQuery) {
                String stemmedQueryWord = stemword(queryWord);
                stemmedWordsInQuery.add(stemmedQueryWord);
            }

            for (String word : stemmedIndexMap.keySet()) {
                if (stemmedWordsInQuery.size() == 0) return null;

                String firstWord = stemmedWordsInQuery.get(0);
                if (firstWord.equals(word)) { // if the word in the nonstopwords query exists
                    for (String document : stemmedIndexMap.get(word).keySet()) {
                        documentListContainingQuery.add(document);
                    }
                }
            }

            // iterate through the other words
            for (String stemmedQueryWord : stemmedWordsInQuery) {
                for (String word : stemmedIndexMap.keySet()) {
                    if (stemmedQueryWord.equals(word)) {
                        ArrayList<String> removeDocuments = new ArrayList<>();
                        for (String queryDocument : documentListContainingQuery) {
                            boolean containsThisDocument = false;
                            for (String document : stemmedIndexMap.get(word).keySet()) {
                                if (queryDocument.equals(document)) containsThisDocument = true;

                            }
                            // if it does not contain this document, then remove this document
                            if (!containsThisDocument) removeDocuments.add(queryDocument);
                        }

                        for (String document : removeDocuments)
                            documentListContainingQuery.remove(document);
                    }
                }
            }

            for (String stemmedQueryWord : stemmedWordsInQuery) {
                for (String word : stemmedIndexMap.keySet()) {
                    if (stemmedQueryWord.equals(word)) {
                        for (String queryDocument : documentListContainingQuery) {
                            for (String document : stemmedIndexMap.get(word).keySet()) {
                                if (queryDocument.equals(document)) {
                                    relevantQ++;
                                }
                                responsesQ++;
                            }
                        }
                    }
                }
            }
        }

        return new double[] {calculateRecall(relevantQ), calculatePrecision(relevantQ, responsesQ)};
    }

    public static String retrieveStatisticsForAllQueries(boolean stemmingFlag) {
        String[] queries = {
        "What is the most densely populated city?",
        "What is the largest building in the world?",
        "What is the net worth of the richest person?",
        "What are the five great lakes called?",
        "How many countries are there?",
        "Who was the first person on the moon?",
        "What countries won World War 1?",
        "What is the largest organ in our body?",
        "What is the largest desert?",
        "How many bones are there in the human body?"};

        StringBuilder stringBuilder = new StringBuilder();
        for (String query : queries) {
            double[] recallAndPrecision = calculateRecallAndPrecisionForQuery(query, stemmingFlag);
            double recall = recallAndPrecision[0];
            double precision = recallAndPrecision[1];
            stringBuilder.append(query).append("\tRecall: " + recall).append("\tPrecision: " + precision).append("\n");
        }

        return stringBuilder.toString();
    }
}
