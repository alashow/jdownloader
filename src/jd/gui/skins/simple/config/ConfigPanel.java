package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.gui.UIInterface;
import jd.plugins.Plugin;
import jd.utils.JDUtilities;

public abstract class ConfigPanel extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;

   private Vector<GUIConfigEntry> entries= new  Vector<GUIConfigEntry>();
    protected UIInterface uiinterface;
    protected JPanel panel;    
    protected Logger            logger           = Plugin.getLogger();
   
    protected  Insets insets = new Insets(1,5,1,5);
    ConfigPanel(UIInterface uiinterface){
        
        this.setLayout(new BorderLayout());
        panel = new JPanel(new GridBagLayout());
        this.uiinterface=uiinterface;      
       
    }

    public void addGUIConfigEntry(GUIConfigEntry entry){
     
        JDUtilities.addToGridBag(panel, entry, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        entries.add(entry);
      
    }
    public void saveConfigEntries(){
       Iterator<GUIConfigEntry> it = entries.iterator();
       
       while(it.hasNext()){
           GUIConfigEntry akt=it.next();
           logger.info("entries: "+entries.size()+" : "+akt.getConfigEntry().getPropertyInstance());
           if(akt.getConfigEntry().getPropertyInstance()!=null&&akt.getConfigEntry().getPropertyName()!=null)   
           akt.getConfigEntry().getPropertyInstance().setProperty(akt.getConfigEntry().getPropertyName(),akt.getText());
           
           
       }
    }
    public void loadConfigEntries(){
        Iterator<GUIConfigEntry> it = entries.iterator();
         
        while(it.hasNext()){
            GUIConfigEntry akt=it.next();
       
           if(akt.getConfigEntry().getPropertyInstance()!=null&&akt.getConfigEntry().getPropertyName()!=null)        
            akt.setData( akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName()));
            
            
        }
     }
  
    
    public abstract void initPanel();
    public abstract void save();
    public abstract void load();
    public abstract String getName();
}
