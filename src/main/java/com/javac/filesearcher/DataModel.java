
package com.javac.filesearcher;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import javax.swing.DefaultListModel;

/**
 * ArrayList String implementation of the DataModelIfc interface
 * 
 * @author jonb
 */
public class DataModel implements DataModelIfc<String> {

    ArrayList<String> list = new ArrayList<String>();

    @Override
    public void add(String s) {
        list.add(s);
    }

    @Override
    public String get(int i) {
        return list.get(i);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    public DefaultListModel getAsListModel() {
        DefaultListModel model = new DefaultListModel();
        for (Object s : list) {
            model.addElement(s);
        }
        return model;
    }
}
