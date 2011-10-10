
package com.javac.filesearcher;

import javax.swing.ListModel;

/**
 * Interface for a basic data store 
 * 
 * @author jonb
 */
public interface DataModelIfc<T> {

    /**
     * Add the given object to the store
     * @param s 
     */
    public void add(T s);

    /**
     * Get the element in the store indexed by i
     * @param i 
     */
    public T get(int i);

    /** 
     * @return the number of elements in the store
     */
    public int size();

    /**
     * Empty the datastore
     */
    public void clear();

    /**
     * For GUI JList purposes, return the store as a ListModel
     * @return the datastore as a ListModel
     */
    public ListModel getAsListModel();
}
