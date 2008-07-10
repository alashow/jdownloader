package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;

import jd.wizardry7.view.DefaultWizardPage;


public class CheckAGB extends DefaultWizardPage {
	
	private static final CheckAGB INSTANCE = new CheckAGB();
	
	public static CheckAGB getInstance() {
		return INSTANCE;
	}
	
	private CheckAGB() {
		super();
		setPreferredSize(new Dimension(500,600));
	}
	
	JCheckBox checkbox;
	private RTFView rtfView;

	
	protected void initComponents() {
		checkbox = new JCheckBox();
		checkbox.setText("I read and agree to the JDownloader AGBs");
		rtfView = new RTFView(new File("res/agbs.rtf"));
	}
	
	protected Component createBody() {
		initComponents();

		int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n ,n));
        panel.setBorder(new EmptyBorder(n,n,n,n));
        panel.add(rtfView);
        panel.add(checkbox, BorderLayout.SOUTH);
        
		return panel;
	}
	
	
	// Validation ##########################################################

	public String forwardValidation() {
		if (checkbox.isSelected()) {
			checkbox.setSelected(false);
			return "";
		}
		else return "Before you are allowed to continue you have to read and agree to our AGBs";
	}
	
	
	public void enterWizardPageAfterForward() {
		this.checkbox.setSelected(false);
	}
	
	
	
	private static class RTFView extends JScrollPane {

	    private static final long serialVersionUID = 1L;
	    
	    JEditorPane rtf;

	    public RTFView(File file) {
	        try {
	            this.rtf = new JEditorPane(file.toURL());
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        this.rtf.setEditable(false);

	        JViewport vp = this.getViewport();
	        vp.add(rtf);
	    }
	    
	    public Dimension getPreferredSize() {
	        if (this.getParent()!=null) {
	            Dimension newSize = new Dimension(-1, this.getParent().getSize().height-70);
	            System.out.println("newSize: " + newSize);
	            this.setPreferredSize(newSize);
	        }
	        return super.getPreferredSize();
	    }
	}

}





