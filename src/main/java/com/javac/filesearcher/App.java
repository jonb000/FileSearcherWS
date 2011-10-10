package com.javac.filesearcher;

import javax.swing.DefaultListModel;

/**
 * Invocation point for the application.  This application will
 * search the file system for a name matching the provided
 * regular expression.  
 * 
 * If directed to do so, the file contents
 * will be searched for the matching regex string INSTEAD of the
 * directories.  
 * 
 * Other options are to not recurse through 
 * subdirectories and to ignore links.
 * 
 * The application can be executed from the command line or has
 * a built-in GUI.
 * 
 * 
 * @author Jon Batcheller jonb@javac.com
 *
 */
public class App {

    private static final String USAGE_MESSAGE = ""
            + "Usage: [options] path regex\n"
            + "-c     (search file contents)\n"
            + "-h     (print this message)\n"
            + "-i     (ignore case)\n"
            + "-r     (recurse all sub-directories\n"
            + "path   (location in filesystem to begin search)\n"
            + "regex  (regular expression to search for)\n\n";

    /**
     * Search the array for a given string (option switch)
     * 
     * @param args - list of command line arguments
     * @param val - switch value to look for
     * @return true if switch is found
     */
    static boolean isInArgs(String[] args, String val) {
        boolean result = false;
        val = val.toLowerCase();

        for (String arg : args) {
            if (arg.equals(val)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Top level main routine.  Either takes command-like arguments, or
     * fires off the UI when no arguments.  This way the jar GUI can be executed
     * from a browser as a double-click on the jar or put on a web server
     * as a web-start app
     * 
     * @param args invoke with -h for full description of command line args
     */
    public static void main(String[] args) {
        try {
            int numArgs = args.length;
            if (numArgs == 0) {
                //use the Swing GUI, 
                java.awt.EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        View view = new View(new SearchRunner(new DataModel()));
                    }
                });

            } else { //command line
                if ((numArgs < 2) || isInArgs(args, "-h")) {
                    System.err.println(USAGE_MESSAGE);
                } else {
                    //always the last two args
                    String regex = args[numArgs - 1];
                    String path = args[numArgs - 2];

                    //instantiate searcher and configure, ignore invalid switches
                    SearchRunner searcher = new SearchRunner(new DataModel());
                    searcher.setStartPath(path);
                    searcher.setRegex(regex);
                    searcher.setSearchFileContents(isInArgs(args, "-c"));
                    searcher.setRecurseSubdirs(isInArgs(args, "-r"));
                    searcher.setIgnoreCase(isInArgs(args, "-i"));

                    //perform the search and output results
                    searcher.start(false);
                    System.out.println(searcher.getMatchCount() + 
                            " Matched, Searched " + searcher.getFileCount() + 
                            " Files & " + searcher.getDirCount() + 
                            " Directories");
                    DataModelIfc data = searcher.getDataModel();
                    for (int i = 0; i < data.size(); i++) {
                        System.out.println(data.get(i));
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("\nCommand Error: " + ex.getLocalizedMessage() +
                    "\n");
            System.err.println(USAGE_MESSAGE);
        }

    }
}
