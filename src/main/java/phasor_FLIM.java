
import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;


public class phasor_FLIM implements PlugIn{
    
    public void run (String arg){
        
        //User input: analysis parameters and experiment name
        GenericDialog setUp = new GenericDialog("Phasor FLIM set-up");
        setUp.addNumericField("Laser repetition rate (MHz)", 40, 2);
        setUp.addNumericField("Segmentation probability threshold", 0, 2);
	setUp.addStringField("Input Experiment name: ", "");
        setUp.showDialog();
	if (setUp.wasCanceled()) return;
        
        double repRate = setUp.getNextNumber();
        double segProb = setUp.getNextNumber();
        String exptName = setUp.getNextString();
        
        //decide whether WEKA segmentation is required
        boolean segment = false;
        if (segProb > 0) {
            segment = true;
        }
        //TODO: make file choosing a seperate class to avoid repeated code
        //select multiple files for batch analysis
        File[] selectedFiles = null;
        FileFilter tcspcFiles  = new FileNameExtensionFilter("TCSPC data files", "sdt"); //don't let muppets chose the wrong file type!
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select TCSPC Image files");
        fileChooser.setFileFilter(tcspcFiles);
	fileChooser.setMultiSelectionEnabled(true);//can select multiple files to batch analyse
	fileChooser.setCurrentDirectory(new File("c://"));
	int fileChooserResult = fileChooser.showOpenDialog(null);
	if (fileChooserResult == JFileChooser.APPROVE_OPTION) {
            selectedFiles = fileChooser.getSelectedFiles();
	}
        
        File classifierFile = null;
        if (segment==true) {
	    FileFilter wekaClassifier = new FileNameExtensionFilter("WEKA Trainable Segmentation classifier", "model");
            JFileChooser classifierChooser = new JFileChooser();
            classifierChooser.setDialogTitle("Select classifier to use");
            classifierChooser.setFileFilter(wekaClassifier);
            classifierChooser.setMultiSelectionEnabled(false);//can only select a single classifier!
            classifierChooser.setCurrentDirectory(new File("c://"));
            int classifierChooserResult = classifierChooser.showOpenDialog(null);
            if (classifierChooserResult == JFileChooser.APPROVE_OPTION) {
                selectedFiles = classifierChooser.getSelectedFiles();
            }
	}
    }
        
    public static void main (final String... args){
            new phasor_FLIM().run("");
        }
}
