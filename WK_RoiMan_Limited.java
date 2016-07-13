import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.Macro_Runner;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Frame;

/*
 * The MIT License
 *
 * Copyright 2016 WAKU_TAKE_A.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * limit ROI
 * @author WAKU_TAKE_A
 * @version 0.9.0.0
 */
public class WK_RoiMan_Limited implements ExtendedPlugInFilter
{
    // const var.
    private static final int FLAGS = DOES_ALL;
    
    // static var.
    private static String type = "Area";
    private static boolean enMin;
    private static double min;
    private static boolean enMax;
    private static double max;

    // var.
    private ImagePlus impSrc = null;    
	private RoiManager roiManager = null;
    private int num_roi = 0;
    private ResultsTable rt = null;	
	private final Macro_Runner mr = new Macro_Runner();

    @Override
    public int showDialog(ImagePlus ip, String cmd, PlugInFilterRunner pifr)
    {
		String[] feats = rt.getHeadings();

        GenericDialog gd = new GenericDialog(cmd + "...");

        gd.addChoice("type", feats, type);
        gd.addCheckbox("enable_min_limit", enMin);
        gd.addNumericField("min_limit", min, 4);
        gd.addCheckbox("enable_max_limit", enMax);
        gd.addNumericField("max_limit", max, 4);

        gd.showDialog();

        if (gd.wasCanceled())
        {
            return DONE;
        }
        else
        {
            type = (String)feats[(int)gd.getNextChoiceIndex()];
            enMin = (boolean)gd.getNextBoolean();
            min = (double)gd.getNextNumber();
            enMax = (boolean)gd.getNextBoolean();
            max = (double)gd.getNextNumber();

            return FLAGS;
        }
    }

    @Override
    public void setNPasses(int i)
    {
        // do nothing
    }

    @Override
    public int setup(String string, ImagePlus imp)
    {
        if(!OCV__LoadLibrary.isLoad)
        {
            IJ.error("Library is not loaded.");
            return DONE;
        }
		
		if (imp == null)
        {
            IJ.noImage();
            return DONE;
        }
        else
        {
            // get the image
            impSrc = imp;
			
            // get the RoiManager
			Frame frame = WindowManager.getFrame("ROI Manager");        

			if (frame==null)
			{
				IJ.run("ROI Manager...");
			}

			frame = WindowManager.getFrame("ROI Manager");
			roiManager = (RoiManager)frame;

			num_roi = roiManager.getCount();

			if(num_roi == 0)
			{
				IJ.error("ERR : ROI is vacant.");
				return DONE;		
			}
           
            roiManager.runCommand("show none");
            
            // get the ResultsTable
            rt = ResultsTable.getResultsTable();

            if(rt == null || rt.getCounter() == 0)
            {
                rt = new ResultsTable();
            }

            rt.reset();

            // Mesure
			rt.show("Results");
            roiManager.deselect();
            mr.runMacro("roiManager(\"Measure\");", "");
            
            return FLAGS;
        }
    }

    @Override
    public void run(ImageProcessor ip)
    {      
        // limit  
        mr.runMacro("setBatchMode(true);", "");	
		
        int col = rt.getColumnIndex(type);
        double val;
        boolean chk_min;
        boolean chk_max;

		for(int i = num_roi - 1; 0 <= i; i--)
		{             
			val = (double)rt.getValueAsDouble(col, i);
			chk_min = enMin ? min <= val : true;
			chk_max = enMax ? val <= max : true; 

			if(!chk_min || !chk_max)
			{
				roiManager.select(i);
				roiManager.runCommand("delete");               
                rt.deleteRow(i);
			}
		}
		
		mr.runMacro("setBatchMode(false);", "");
        rt.show("Results");
    }
}