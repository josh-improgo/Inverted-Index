## How to Compile Program
You must have the zipped version of the project.
To compile, type in the command line or console:

    java -jar SearchEngine.zip -CorpusDir PathOfDir -InvertedIndex NameOfIIndexFile -StopList NameOfStopListFile -Queries QueryFile -Results ResultsFile -Stemming StemmingFile -SnippetNum SnippetNumber -Display DisplayNum

Where:
    PathOfDir is the directory of all the webpages,
    NameOfIIndexFile is the path to the invertedIndex file,
    NameOfStopListFile is the path to the stopList file,
    QueryFile is the path to the queryFile, and
    ResultsFile is the path to the resultsFile.
    StemmingFile is the path to the stemmingFile.
    SnippetNumber is the number of words you will receive within the snippet before and after the query.
    DisplayNum is the integer between 0-2. 0 is for only text file, 1 is for only GUI, and 2 	is for both GUI and text file.


    *Note: the -Stemming flag is optional. If you want stemming, you must add the flag followed by a stemmingFile. If you do not want stemming, simply exclude the -Stemming flag and the stemming file name.


--If you are using an IDE such as IntelliJ or Eclipse and are trying to run it within the IDE--
Go to Configurations and select the Java Application.
Inside the Program Arguments, enter:
	-CorpusDir PathOfDir -InvertedIndex NameOfIIndexFile -StopList NameOfStopListFile -Queries QueryFile -Results ResultsFile -Stemming StemmingFile -SnippetNum SnippetNumber -Display DisplayNum

## What Each Document Contains
PathOfDir should be a corpus of 200 HTML documents.
The inverted index file, once processed, will contain a list of all non-stopwords found in the Corpus. Each word will
    contain values that show the name of the document, the total frequency, and each position found. It will be in this
    format:

    "WORD"
	{"DOCUMENT", "TOTAL FREQUENCY": [POSITION]}

    Where if there are multiple documents and positions, will be shown as the following:

    "WORD"
	{"DOCUMENT1", "TOTAL FREQUENCY": [POSITION1, POSITION2, POSITION3]}{"DOCUMENT2", "TOTAL FREQUENCY": [POSITION1, POSITION2, ...}{...}

## How to Search the Inverted Index (Stemmed index if -Stemming flag is true)
There are two ways to search the inverted index (or stemmed index).
1) Processing through Query File.
2) Processing through the console.


The following are the accepted command arguments:

    a) Query TERM
    b) Frequency TERM
    c) Search WORD
    d) Search DOC
    e) Snippet QUERY

TERM and WORD is any word that the user would like to query, find the frequency of, or search.
DOC is associated with the HTML files within the corpus.

    *Example Uses*

    QUERY TERM
        Query apple
    Return result format:
        {Document, Total Frequency: [Positions]}

    FREQUENCY TERM
        Frequency apple
    Return result format:
        {Document, Frequency}

    SEARCH WORD
        Search apple
    Return result format:
        {Document, Frequency}

    SEARCH DOC
        Search example_file.html
    Return result format:
        {Word, Frequency}

    SNIPPET QUERY
        snippet what fruits are red
    Return result format:
        document
            snippet

--Processing through Query File--
**Query file must be done before compiling.
Inside the query file, the commands must be separated by new lines.

    *Example file format*

        Query apple
        Query banana
        Frequency apple
        Search apple
        Search example_file.html
        Snippet what fruits are red

    The results of these commands will appear in the resultsFile.


--Processing through Console--
After running the program, the user is initially prompted whether to use the persisted inverted index files (if there exists a file with data inside).
    *Note: Persistence is done writing out to a text file and read in when the program is executed.

    It looks like you have an inverted index on file. Would you like to use this file? If No, the corpus will be reprocessed to create a new inverted index. Type Y/N.

Type Y to process the persisted inverted index file or N to process the corpus to create a new inverted index.


If the stemming flag was applied, the user will then be prompted whether to use persisted

    It looks like you have a stemmed index on file. Would you like to use this file? If No, the corpus and inverted index will be reprocessed to create a new stemmed index. Type Y/N.

Type Y to process the persisted stemmed index file or N to process the corpus and inverted index to create a new stemmed index.

The user is then prompted whether to append the console queries to the resultsFile.

    Append queries to ResultsFile? Y\N

Type Y to append or N to not affect the resultsFile.

The user will then be prompted the following:

    Type one of the following:
    a) "Query TERM" to search TERM within the Corpus.
    b) "Frequency TERM" to identify the frequency of TERM in each document.
    c) "Search WORD" to get which documents WORD appears and its frequency.
    d) "Search DOC" to get all words found in DOC and its frequency.
    e) "Snippet QUERY" to get the a snippet of the first occurrence of the query in each document.
    f) "Quit" to quit.

Simply type in the commands (similar to how it is in QueryFile but as inputs in the console) to retrieve results.

## Stop Word List Reference
    Long Stopword List from: https://www.ranks.nl/stopwords

## Porter's Algorithm Reference
    Java Code of Porter's Algorithm from: https://tartarus.org/martin/PorterStemmer/java.txt

