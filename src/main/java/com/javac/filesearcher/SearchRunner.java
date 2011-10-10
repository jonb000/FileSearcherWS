package com.javac.filesearcher;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.Boolean;
import java.lang.Integer;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the basic implementation of a threaded file searching
 * routine.  Results are placed in the constructor injected data
 * model.  For testing and non GUI execution, threading can be
 * bypassed.
 * 
 * For each successful file path added to the datastore, a property
 * change event is fired.   In addition, an event is fired when the
 * search completes.
 * 
 * @author jonb
 */
public class SearchRunner implements Runnable {

    /**
     * Change event string on each path added to the datastore
     */
    public static final String UI_UPDATE = "UI_UPDATE";
    /**
     * Change event string passed when the searching routine stops
     */
    public static final String RUNNER_STOPPED = "STOP";
    //
    /** Private instance variables, see the appropriate 
     * setter/getter for functionality descriptions */
    private boolean stopRequest = false;
    private boolean isRunning = false;
    private boolean searchFileContents = false;
    private boolean recurseSubdirs = false;
    private boolean searchIsRegex = false;
    private boolean ignoreCase = false;
    private String startPath = "";
    private String searchString;
    private int fileCount = 0;
    private int dirCount = 0;
    private int maxContentFileSize = 0xFFFFF; 
    //
    /**
     * With the possibility of circular linkages, we maintain a visited
     * list to avoid infinite recursion
     */
    HashMap<String,Boolean> visitedList = new HashMap<String,Boolean>();
    
    /** Datastore injected from the constructor
     * 
     */
    private DataModelIfc dataModel;
    /** 
     * For controller to view event passing
     * 
     */
    private PropertyChangeSupport propChangeSupport =
            new PropertyChangeSupport(this);

    /**
     * Construct a search runner, placing matched data in the model
     * @param dataModel 
     */
    public SearchRunner(DataModelIfc dataModel) {
        this.dataModel = dataModel;
    }

    /**
     * @return true, the search will continue through subdirectories

     */
    public boolean isRecurseSubdirs() {
        return recurseSubdirs;
    }

    /**
     * Set to true to recurse through child subdirectories.  As false, the
     * search will terminate at the given "startPath".
     * @param recurseSubdirs 
     */
    public void setRecurseSubdirs(boolean recurseSubdirs) {
        this.recurseSubdirs = recurseSubdirs;
    }

    /**
     * If true, the contents of the file are searched for the "regex", not
     * the  filesystem pathnames
     * 
     * @return true if file contents are to be examined
     */
    public boolean isSearchFileContents() {
        return searchFileContents;
    }

    /**
     * Set true to search file contents, not file pathnames
     * 
     * @param searchFileContents 
     */
    public void setSearchFileContents(boolean searchFileContents) {
        this.searchFileContents = searchFileContents;
    }

    /**
     * Set the regular expression string to be searched for.  Must follow
     * syntax of the java.util.regex.Pattern class or an exception is thrown
     * @param regex 
     */
    public void setRegex(String regex) throws Exception {
        setSearchString(regex, true);
    }

    /**
     * Set the search string. if boolean is true, 
     * then treat it as a regular expression
     * @param val
     * @param isRegex 
     */
    public void setSearchString(String val, boolean isRegex) throws Exception {
        this.searchIsRegex = isRegex;
        searchString = val;
        if (isRegex) {

            //compile it just to be sure valid regex
            Pattern.compile(searchString);
        }

    }

    /**
     * Set to true if search should ignore case sensitivity
     * @param ignoreCase 
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     *  
     * @return the currently set filesystem path where searching begins
     */
    public String getStartPath() {
        return startPath;
    }

    /**
     * Set the starting filesystem location for the search
     * @param startPath 
     */
    public void setStartPath(String startPath) throws Exception {

        this.startPath = startPath;
        File f = new File(startPath);
        if (!f.exists()) {
            throw new Exception(startPath + " does not exist.");
        }
        setStartPath(f);

    }

    /**
     * Set the starting filesystem location for the search
     * @param f
     * @throws Exception 
     */
    public void setStartPath(File f) throws Exception {
        this.startPath = f.getCanonicalPath();
        if (!f.canRead()) {
            throw new Exception(startPath + " exists but cannot be read.");
        }
    }

    /**
     * 
     * @return the current data model object
     */
    public DataModelIfc getDataModel() {
        return dataModel;
    }

    /**
     * Stop the running search
     */
    public void stop() {
        stopRequest = true;
    }

    /**
     * 
     * @return Current number of directories searched
     */
    public int getDirCount() {
        return dirCount;
    }

    /**
     * 
     * @return The maximum number of bytes a file can be to be content searched
     */
    public int getMaxContentFileSize() {
        return maxContentFileSize;
    }

    /**
     * Set, in bytes, the maximum size for a file to be content searched.
     * 
     * @param maxContentFileSize 
     */
    public void setMaxContentFileSize(int maxContentFileSize) {
        this.maxContentFileSize = maxContentFileSize;
    }

    /**
     *
     * @return current number of files examined for a match
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * 
     * @return current number of matches found
     */
    public int getMatchCount() {
        return dataModel.size();
    }
    
    

    /**
     * Begin a threaded search
     */
    public void start() {
        start(true);
    }

    /**
     * Begin a search, in a separate thread if the argument is true
     * 
     * @param threaded 
     */
    public void start(boolean threaded) {
        dataModel.clear();
        if (threaded) {
            Thread t = new Thread(this);
            t.start();
        } else {
            run();
        }
    }

    /**
     * Clear the dataModel and tell the UI
     */
    public void clear() {
        dataModel.clear();
        visitedList.clear();
        fileCount = 0;
        dirCount = 0;
        propChangeSupport.firePropertyChange(UI_UPDATE, " ", "");
    }

    /**
     * 
     * @return true if the searcher thread is executing
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Wraps the getCanonicalPath exception
     * @param f - file to get the path of
     * @return the canonical path if available
     */
    
    public String getPath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (Exception ex) {
            return f.getAbsolutePath();
        }
    }

    /**
     * Converts files, smaller than the maxContentFileSize into a String for
     * matching purposes.
     * @param file
     * @return a String matching the file contents
     * @throws Exception 
     */
    public String convertToString(File file) throws Exception {
        //for space and time, we don't search content of files bigger than this
        if (file.length() > maxContentFileSize ) return "";
        
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            char[] buf = new char[8192]; //read buffer
            int r = 0;

            while ((r = reader.read(buf)) != -1) {
                result.append(buf, 0, r);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return result.toString();
    }

    

    /**
     * Examine the string looking for a match with the configured
     * searchString.
     * 
     * @param val
     * @return true if  match found
     */
    private boolean checkForMatch(String val) {

        //don't bother if nothing there
        if (val == null || val.length() == 0) {
            return false;
        }

        boolean result = false;

        if (searchIsRegex) {
            int flags = (ignoreCase) ? Pattern.CASE_INSENSITIVE : 0;
            Pattern p2 = Pattern.compile(searchString, flags);
            Matcher m2 = p2.matcher(val);
            result = m2.matches();

        } else {  //straight string compare
            if (ignoreCase) {
                val = val.toLowerCase();
                searchString = searchString.toLowerCase();
            }
            result = val.contains(searchString);
        }
        return result;
    }

    /**
     * Search the directory "path" for files, and examine those filesnames
     * for a match with the "searchString".  Keep counts of number of files
     * examined and directories examined.  If the "searchFileContents" flag
     * is set, the contents, NOT the file names are examined for a match.
     * 
     * Change events are fired when a match is found and when a directory is
     * traversed.  
     * 
     * @param path 
     */
    private void traverseAndMatch(String path) {

        File dir = new File(path);

        //if not a directory, we are done
        if (addNewDirectory(dir)) {

            //iterate through this directory's files
            File[] files = dir.listFiles();
            for (File childFile : files) {
                //break out of the loop if a stop has been requested
                if (stopRequest) {
                    break;
                }

                if (childFile.isDirectory()) {
                    //if we are not searching contents, see if name is a match
                    if (!searchFileContents && checkForMatch(childFile.getName())) {
                        addMatch(getPath(childFile));
                    }
                    //if we are recursing, traverse this directory
                    if (recurseSubdirs) {
                        traverseAndMatch(getPath(childFile));
                    }
                } else {
                    fileCount++;
                    if (searchFileContents) {
                        //load the file into a String for matching.
                        //Should this fail, put the failure in the output
                        try {
                            if (checkForMatch(convertToString(childFile))) {
                                addMatch(childFile.getCanonicalPath());
                            }
                        } catch (Exception ex) {
                            addMatch(getPath(childFile));
                        }
                    } else { //just check the filename
                        if (checkForMatch(childFile.getName())) {
                            addMatch(getPath(childFile));
                        }
                    }

                }
            }
        }
    }

    /**
     * Internal routine to add a path to the datastore and fire an event
     * @param path 
     */
    private void addMatch(String path) {
        dataModel.add(path);
        fireChangeEvent(UI_UPDATE);
    }
    
    /**
     * Track directories to avoid loops.  Fire an event on each new directory
     * @param dir
     * @return true if a new directory
     */
    private boolean addNewDirectory(File dir) {
        if (!dir.isDirectory()) return false;
        
        //see if we have been here before, due to a circular linkage.
        if (visitedList.containsKey(getPath(dir))) {
            return false;
        }
        visitedList.put(getPath(dir), true);
        dirCount++;
        fireChangeEvent(UI_UPDATE);
        return true;
    }

    /**
     * Fire off a property change event
     */
    private void fireChangeEvent(String eventType) {
        propChangeSupport.firePropertyChange(eventType, 0, fileCount);
    }
    


    /**
     * Run the SearchRunner based on configured parameters, placing data in
     * the datamodel
     */
    public void run() {
        try {
            clear();
                isRunning = true;
                traverseAndMatch(startPath);
        } finally {
            isRunning = false;
            stopRequest = false;
            propChangeSupport.firePropertyChange(RUNNER_STOPPED, "", "1");
        }
    }

    /**
     * Add a property listener to get change events
     * @param listener 
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propChangeSupport.addPropertyChangeListener(listener);
    }

    /** 
     * Remove a property listener to get change events
     * @param listener 
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propChangeSupport.removePropertyChangeListener(listener);

    }

   
}
