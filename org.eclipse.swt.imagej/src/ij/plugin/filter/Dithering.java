package ij.plugin.filter;

import java.util.Random;
import java.util.Vector;

import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;

/**
 * Dithering_
 *
 * ImageJ plugin for converting images to binary using various dithering methods:
 * - Floyd-Steinberg error diffusion
 * - Bayer ordered dithering (2x2, 4x4)
 * - Random noise dithering
 * - Halftone rendering (square or circular dots with adjustable size and orientation)
 *
 * Features:
 * - Live preview
 * - Macro recording support (all parameters recordable)
 * - Dynamic UI (halftone parameters shown only when needed)
 *
 * Usage:
 * Plugins > Dithering_
 *
 * Example macro:
 * run("Blobs (25K)");
 * run("Dither...", "algorithm=Halftone shape=Circle size=5 orientation=115");
 *
 * Authors:
 * - ChatGPT (assisted implementation)
 * - Jerome Mutterer
 *
 * Year: 2026
 */
public class Dithering implements ExtendedPlugInFilter, DialogListener {

	private static final String[] ALGORITHMS = {"Floyd-Steinberg", "Bayer 2x2", "Bayer 4x4", "Random noise", "Halftone"};
	private static final String[] HALFTONE_SHAPES = {"Square", "Circle"};
	private ImagePlus imp;
	private String algorithm = ALGORITHMS[0];
	private String halftoneShape = HALFTONE_SHAPES[0];
	private int halftoneSize = 3;
	private double halftoneAngle = 75.0;
	private Combo shapeChoice;
	private Text sizeField;
	private Text angleField;
	private Control shapeLabel;
	private Control sizeLabel;
	private Control angleLabel;

	public int setup(String arg, ImagePlus imp) {

		this.imp = imp;
		return DOES_ALL | SUPPORTS_MASKING | FINAL_PROCESSING;
	}

	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {

		GenericDialog gd = new GenericDialog("Dithering");
		gd.setSmartRecording(true);
		gd.addChoice("Algorithm:", ALGORITHMS, algorithm);
		gd.addChoice("Shape:", HALFTONE_SHAPES, halftoneShape);
		shapeLabel = gd.getLabel(); // get label reference immediately after addChoice
		gd.addNumericField("Size:", halftoneSize, 0);
		sizeLabel = gd.getLabel();
		gd.addNumericField("Orientation:", halftoneAngle, 1);
		angleLabel = gd.getLabel();
		Vector choices = gd.getChoices();
		Vector numericFields = gd.getNumericFields();
		Display.getDefault().syncExec(() -> {
			shapeChoice = (Combo)choices.get(1);
			sizeField = (Text)numericFields.get(0);
			angleField = (Text)numericFields.get(1);
			// Hide halftone fields initially without repacking
			toggleHalftoneFields(gd, false);
		});
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();
		if(gd.wasCanceled())
			return DONE;
		readDialog(gd);
		if(Recorder.record) {
			Recorder.setCommand("Dithering ");
			Recorder.recordOption("algorithm", algorithm);
			if(algorithm.equals("Halftone")) {
				Recorder.recordOption("shape", halftoneShape);
				Recorder.recordOption("size", "" + halftoneSize);
				Recorder.recordOption("orientation", "" + halftoneAngle);
			}
		}
		return IJ.setupDialog(imp, DOES_ALL);
	}

	public boolean dialogItemChanged(GenericDialog gd, TypedEvent e) {

		readDialog(gd);
		toggleHalftoneFields(gd, true);
		return true;
	}

	private void readDialog(GenericDialog gd) {

		algorithm = gd.getNextChoice();
		halftoneShape = gd.getNextChoice();
		halftoneSize = (int)gd.getNextNumber();
		halftoneAngle = gd.getNextNumber();
		if(halftoneSize < 2)
			halftoneSize = 2;
	}

	private void toggleHalftoneFields(GenericDialog gd, boolean repack) {

		boolean show = algorithm.equals("Halftone");
		setVisibleExclude(shapeLabel, show);
		setVisibleExclude(shapeChoice, show);
		setVisibleExclude(sizeLabel, show);
		setVisibleExclude(sizeField, show);
		setVisibleExclude(angleLabel, show);
		setVisibleExclude(angleField, show);
		if(repack) {
			gd.getShell().pack();
			gd.getShell().layout(true, true);
		}
	}

	private void setVisibleExclude(Control control, boolean visible) {

		if(control == null)
			return;
		control.setVisible(visible);
		Object layoutData = control.getLayoutData();
		if(layoutData instanceof GridData) {
			((GridData)layoutData).exclude = !visible;
		} else {
			// Labels from makeLabel() have no GridData assigned — create one
			GridData gd = new GridData();
			gd.exclude = !visible;
			control.setLayoutData(gd);
		}
	}

	public void run(ImageProcessor ip) {

		ImageProcessor bp = ip.convertToByte(true);
		int[][] pixel = bp.getIntArray();
		int width = bp.getWidth();
		int height = bp.getHeight();
		if(algorithm.equals("Floyd-Steinberg"))
			floydSteinberg(pixel, width, height);
		else if(algorithm.equals("Bayer 2x2"))
			orderedDither(pixel, width, height, BAYER_2X2);
		else if(algorithm.equals("Bayer 4x4"))
			orderedDither(pixel, width, height, BAYER_4X4);
		else if(algorithm.equals("Random noise"))
			randomDither(pixel, width, height);
		else if(algorithm.equals("Halftone"))
			halftone(pixel, width, height, halftoneShape, halftoneSize, halftoneAngle);
		bp.setIntArray(pixel);
		ip.insert(bp, 0, 0);
		if(ip.getBitDepth() == 16 || ip.getBitDepth() == 32)
			ip.resetMinAndMax();
	}

	public void setNPasses(int nPasses) {

	}

	private void floydSteinberg(int[][] pixel, int width, int height) {

		int oldpixel, newpixel, error;
		for(int y = 0; y < height; y++) {
			boolean nbottom = y < height - 1;
			for(int x = 0; x < width; x++) {
				boolean nleft = x > 0;
				boolean nright = x < width - 1;
				oldpixel = clamp(pixel[x][y]);
				newpixel = oldpixel < 128 ? 0 : 255;
				pixel[x][y] = newpixel;
				error = oldpixel - newpixel;
				if(nright)
					pixel[x + 1][y] += 7 * error / 16;
				if(nleft && nbottom)
					pixel[x - 1][y + 1] += 3 * error / 16;
				if(nbottom)
					pixel[x][y + 1] += 5 * error / 16;
				if(nright && nbottom)
					pixel[x + 1][y + 1] += error / 16;
			}
		}
	}

	private void orderedDither(int[][] pixel, int width, int height, int[][] matrix) {

		int n = matrix.length;
		int levels = n * n;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int value = clamp(pixel[x][y]);
				int threshold = (int)(((matrix[y % n][x % n] + 0.5) * 255.0) / levels);
				pixel[x][y] = value < threshold ? 0 : 255;
			}
		}
	}

	private void randomDither(int[][] pixel, int width, int height) {

		Random random = new Random(12345L);
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int value = clamp(pixel[x][y]);
				int threshold = random.nextInt(256);
				pixel[x][y] = value < threshold ? 0 : 255;
			}
		}
	}

	private void halftone(int[][] pixel, int width, int height, String shape, int size, double angleDegrees) {

		double angle = Math.toRadians(angleDegrees);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double half = size / 2.0;
		boolean circle = shape.equals("Circle");
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int value = clamp(pixel[x][y]);
				double darkness = 1.0 - value / 255.0;
				double xr = x * cos + y * sin;
				double yr = -x * sin + y * cos;
				double cx = xr - Math.floor(xr / size) * size - half;
				double cy = yr - Math.floor(yr / size) * size - half;
				double threshold;
				if(circle) {
					double dist = Math.sqrt(cx * cx + cy * cy);
					threshold = 1.0 - dist / (half * Math.sqrt(2.0));
				} else {
					double dist = Math.max(Math.abs(cx), Math.abs(cy));
					threshold = 1.0 - dist / half;
				}
				pixel[x][y] = darkness > threshold ? 0 : 255;
			}
		}
	}

	private static int clamp(int v) {

		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	private static final int[][] BAYER_2X2 = {{0, 2}, {3, 1}};
	private static final int[][] BAYER_4X4 = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
}