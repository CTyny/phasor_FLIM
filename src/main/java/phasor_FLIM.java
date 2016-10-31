
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
//import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import loci.formats.FormatException;
import loci.plugins.BF;
import trainableSegmentation.WekaSegmentation;


public class phasor_FLIM implements PlugIn{
    
    public void run (String arg){
        
        //User input: analysis parameters and experiment name
        GenericDialog userInput = new GenericDialog("Phasor FLIM set-up");
        userInput.addNumericField("Segmentation probability threshold", 0, 2);
	userInput.addStringField("Input Experiment name: ", "");
        userInput.showDialog();
	if (userInput.wasCanceled()) return;
        
        double segProb = userInput.getNextNumber();
        String exptName = userInput.getNextString();        
        
        //select multiple files for batch analysis
        FileFilter tcspcFiles  = new FileNameExtensionFilter("TCSPC data files", "sdt"); //don't let muppets chose the wrong file type!
        File[] dataFiles = fileSelector(tcspcFiles, true, "Select TCSPC Image files (binned data only)");
        
        File[] classifierFile = null;
        if (segProb > 0) {
	    FileFilter wekaClassifier = new FileNameExtensionFilter("WEKA Trainable Segmentation classifier", "model");
            classifierFile = fileSelector(wekaClassifier, false, "Select classifier to use");
	}
        double[][][][] results = new double[dataFiles.length][][][];
        for (int i=0; i<dataFiles.length; i++) {
            results[i] = phasorCalculator(bioformatsFileOpener(dataFiles[i].getAbsolutePath()));//test phasor calculator need to interate over multiple files
            IJ.log("File " + (i+1) + " processed...");
        }
        phasor2DHistogram(results);
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
    
    public double[][][] phasorCalculator (ImagePlus input){
        
        IJ.log("Calculating Phasor");
        int x = input.getWidth();
        int y = input.getHeight();
        int c = input.getNChannels();
        int t = input.getNFrames();
        int z = input.getNSlices();
        
        //determine start of decay
        int startFrame = 1;
        int frameSumMax = 0;
        for (int i=1; i<=t; i++){
            int frameSum =0;
            input.setT(i);
            ImageProcessor ip = input.getProcessor();
            int[][] values = ip.getIntArray();
            for (int j=0; j<values[0].length; j++){
                for (int k=0; k<values[0].length; k++){
                    frameSum += values[k][j];
                }
            }
            if (frameSum > frameSumMax){
                startFrame = i;
                frameSumMax = frameSum;
            }
        }
        
        double [][][] output = new double [3][x][y];
        
        for (int i=startFrame; i<=t; i++){
            input.setT(i);
            double gFactor = Math.cos(2*Math.PI*(i+1-startFrame)/t);
            double sFactor = Math.sin(2*Math.PI*(i+1-startFrame)/t);
            
            ImageProcessor ip = input.getProcessor();
            
            int[][] values = ip.getIntArray();
            for (int j=0; j<y; j++){
                for (int k=0; k<x; k++){
                    if (values[k][j] > 0){ //save time if the value is zero!
                        output[0][k][j] += values[k][j];//calculate total
                        output[1][k][j] += values[k][j]*gFactor;//modify to calculate g integral
                        output[2][k][j] += values[k][j]*sFactor;//modify to calculate s integral
                    }
                }    
            }
        }
        for (int j=0; j<y; j++){
            for (int k=0; k<x; k++){
                if (output[0][k][j] > 0){ //don't divide by zero!
                    output[1][k][j] /= output[0][k][j];
                    output[2][k][j] /= output[0][k][j];
                }
            }
        }
        
        return output;
    }
    
    public ImageProcessor applyWekaClassifier (ImagePlus toBeSegmented, String classifierFilePath){
        
        WekaSegmentation segmentator = new WekaSegmentation(toBeSegmented);
	segmentator.loadClassifier(classifierFilePath);
	ImagePlus classResult = segmentator.applyClassifier(toBeSegmented, 0, true);
        classResult.show();//check output
	classResult.setSlice(1);
        ImageProcessor probIP = classResult.getProcessor();
        return probIP;                               
    }
    
    public void phasor2DHistogram(double[][][][] input){
        //photon count weighted for now :-)
        int binNumber = 500; //for ease of playing with plot resolution!
        double limitHigh = 0;
        double twoDimFreq[][] = new double [binNumber][binNumber];
        
        for (int i=0; i<input.length; i++){
            for (int j=0; j<input[0][0][0].length; j++){
                for (int k=0; k<input[0][0].length; k++){
                    if (input[i][0][k][j]>50 && input[i][1][k][j]>0 && input[i][1][k][j]<1 && input[i][2][k][j]>0 && input[i][2][k][j]<1){
                        double gElement = Math.floor(input[i][1][k][j]*binNumber);
                        double sElement = Math.floor(input[i][2][k][j]*binNumber);
                        twoDimFreq[(int)gElement][(int)sElement] += input[i][0][k][j];
                    }
                }
            }
        }
        ImagePlus phasorPlot = NewImage.createFloatImage("Phasor plot", binNumber, binNumber, 1, NewImage.FILL_BLACK);
        ImageProcessor ip = phasorPlot.getProcessor();
        
        for (int i=0; i<binNumber; i++){
            for (int j=0; j<binNumber; j++){
                ip.putPixelValue(j, binNumber-i, twoDimFreq[j][i]);
                if (twoDimFreq[j][i] > limitHigh){
                    limitHigh = twoDimFreq[j][i];
                }
            }
        }
        phasorPlot.show();
        phasorPlot.setLut(fire());
        phasorPlot.setDisplayRange(0, limitHigh);
        Overlay universalCircle = new Overlay();
        OvalRoi roi = new OvalRoi(0, 250, 500, 500);
        universalCircle.add(roi);
        phasorPlot.setOverlay(universalCircle);
        
        
        /*FileSaver saved = new FileSaver(phasorPlot);
        saved.saveAsTiff("c:\\EXPERIMENTAL DATA\\Phasor test\\test.tiff");*/
    }
    
    public LUT fire(){
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        int [] r = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,7,10,13,16,19,22,25,28,31,34,37,40,43,46,49,52,55,58,61,64,67,70,73,76,79,82,85,88,91,94,98,101,104,107,110,113,116,119,122,125,128,131,134,137,140,143,146,148,150,152,154,156,158,160,162,163,164,166,167,168,170,171,173,174,175,177,178,179,181,182,184,185,186,188,189,190,192,193,195,196,198,199,201,202,204,205,207,208,209,210,212,213,214,215,217,218,220,221,223,224,226,227,229,230,231,233,234,235,237,238,240,241,243,244,246,247,249,250,252,252,252,253,253,253,254,254,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255};
        int [] g = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,3,5,7,8,10,12,14,16,19,21,24,27,29,32,35,37,40,43,46,48,51,54,57,59,62,65,68,70,73,76,79,81,84,87,90,92,95,98,101,103,105,107,109,111,113,115,117,119,121,123,125,127,129,131,133,134,136,138,140,141,143,145,147,148,150,152,154,155,157,159,161,162,164,166,168,169,171,173,175,176,178,180,182,184,186,188,190,191,193,195,197,199,201,203,205,206,208,210,212,213,215,217,219,220,222,224,226,228,230,232,234,235,237,239,241,242,244,246,248,248,249,250,251,252,253,254,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255};
        int [] b = {0,7,15,22,30,38,45,53,61,65,69,74,78,82,87,91,96,100,104,108,113,117,121,125,130,134,138,143,147,151,156,160,165,168,171,175,178,181,185,188,192,195,199,202,206,209,213,216,220,220,221,222,223,224,225,226,227,224,222,220,218,216,214,212,210,206,202,199,195,191,188,184,181,177,173,169,166,162,158,154,151,147,143,140,136,132,129,125,122,118,114,111,107,103,100,96,93,89,85,82,78,74,71,67,64,60,56,53,49,45,42,38,35,31,27,23,20,16,12,8,5,4,3,3,2,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,8,13,17,21,26,30,35,42,50,58,66,74,82,90,98,105,113,121,129,136,144,152,160,167,175,183,191,199,207,215,223,227,231,235,239,243,247,251,255,255,255,255,255,255,255,255};
        for (int i=0; i<256; i++){
            reds[i] = (byte)r[i];
            greens[i] = (byte)g[i];
            blues[i] = (byte)b[i];
        }
        
        LUT output = new LUT(reds, greens, blues);
        
        return output;
    }
    
    public ImagePlus hyperstackAssembler (double[][][] input){
        ImagePlus hyperStack = IJ.createHyperStack("phasorCalculator Output", 256, 256, 3, 1, 1, 32);
        
        for (int i=0; i<3; i++){
            hyperStack.setPosition(i+1,1,1);
            ImageProcessor ip = hyperStack.getProcessor();
        
            for (int j=0; j<256; j++){
                for (int k=0; k<256; k++){
                ip.putPixelValue(k, j, input[i][k][j]);
                }
            }
            
            hyperStack.setProcessor(ip);
        }
        return hyperStack;
    }
    
    public static void main (final String... args){
            new phasor_FLIM().run("");
        }
}
