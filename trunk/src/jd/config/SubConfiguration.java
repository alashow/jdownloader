package jd.config;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.utils.JDUtilities;


public class SubConfiguration extends Property implements Serializable {



    private transient Logger        logger;
    private String name;

    /**
     * 
     */
    public SubConfiguration(String name) {
        logger = JDUtilities.getLogger();
        this.name=name;
        File file;
        Object props = JDUtilities.loadObject(null, file=JDUtilities.getResourceFile("config/"+name+".cfg"), false);
       file.getParentFile().mkdirs();
        if(props!=null){
            this.setProperties((HashMap<String, Object>)props);
        }
    }
    public void save(){
        JDUtilities.saveObject(null, this.getProperties(),  JDUtilities.getResourceFile("config/"+name+".cfg"),null, null, false);
    }


}