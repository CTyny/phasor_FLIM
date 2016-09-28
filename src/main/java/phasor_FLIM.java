
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;


public class phasor_FLIM implements PlugIn{
    
    public void run (String arg){
        
        GenericDialog setUp = new GenericDialog("Phasor FLIM set-up");
        setUp.addNumericField("Laser repetition rate (MHz)", 40, 2);
        setUp.addNumericField("Segmentation probability threshold", 0.5, 2);
	setUp.addStringField("Input Experiment name: ", "");
        setUp.showDialog();
	if (setUp.wasCanceled()) return;
        
        double repRate = setUp.getNextNumber();
        double segProb = setUp.getNextNumber();
        String exptName = setUp.getNextString();
    }
    
    public static void main (final String... args){
            new phasor_FLIM().run("");
        }
}
