
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import loci.formats.FormatException;
import loci.plugins.BF;


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
        
        //select multiple files for batch analysis
        FileFilter tcspcFiles  = new FileNameExtensionFilter("TCSPC data files", "sdt"); //don't let muppets chose the wrong file type!
        File[] dataFiles = fileSelector(tcspcFiles, true, "Select TCSPC Image files");
        
        File[] classifierFile = null;
        if (segment==true) {
	    FileFilter wekaClassifier = new FileNameExtensionFilter("WEKA Trainable Segmentation classifier", "model");
            JFileChooser classifierChooser = new JFileChooser();
            classifierChooser.setDialogTitle("Select classifier to use");
            classifierChooser.setFileFilter(wekaClassifier);
            classifierChooser.setMultiSelectionEnabled(false);//can only select a single classifier!
            classifierChooser.setCurrentDirectory(new File("c://"));
            int classifierChooserResult = classifierChooser.showOpenDialog(null);
            if (classifierChooserResult == JFileChooser.APPROVE_OPTION) {
                classifierFile = classifierChooser.getSelectedFiles();
            }
	}
    }
    
    public File [] fileSelector(FileFilter fileTypeFilter, Boolean multiFile, String title) {
        File[] selectedFiles = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setFileFilter(fileTypeFilter);
        fileChooser.setMultiSelectionEnabled(multiFile);//select multiple files to batch analyse if true
        fileChooser.setCurrentDirectory(new File("c://"));
	int fileChooserResult = fileChooser.showOpenDialog(null);
	if (fileChooserResult == JFileChooser.APPROVE_OPTION) {
            selectedFiles = fileChooser.getSelectedFiles();
	}
        return selectedFiles;
    }
    
    public ImagePlus bioformatsFileOpener (String filePath){
        
        ImagePlus[] imp = new ImagePlus[1];
        try {
            imp = BF.openImagePlus(filePath);
        } catch (IOException e) {
            IJ.error("Cannot read file:" + filePath, e.getMessage());
        } catch (FormatException e) {
            IJ.error("Cannot read file:" + filePath, e.getMessage());
        }
        return imp[0];
    }
    
    
    public ImagePlus hyperStackAssembler (File [] fileLocations) {
        int x = 256;//hardwire these numbers until sensible way to check file headers and handle mismatches can be implemented
        int y = 256;
        int t = 64;
        int bitDepth = 32;
        int c = 0;//channels TBC
        ImagePlus hyperStack = IJ.createHyperStack("", x, y, c, fileLocations.length, t, bitDepth); // use z for stacking files
        
        //do something useful here
        return hyperStack;
    }
    
    public static void main (final String... args){
            new phasor_FLIM().run("");
        }
}
