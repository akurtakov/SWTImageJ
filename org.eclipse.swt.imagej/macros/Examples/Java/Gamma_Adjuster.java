import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

import org.eclipse.swt.events.TypedEvent;

import ij.plugin.*;

public class Gamma_Adjuster implements PlugIn {

	public void run(String arg) {
		final ImagePlus img = IJ.openImage("https://imagej.net/ij/images/clown.jpg");
		final ImageProcessor ip = img.getProcessor();
		ip.snapshot();
		DialogListener listener = new DialogListener() {
			public boolean dialogItemChanged(GenericDialog gd, TypedEvent event) {
				double gamma = gd.getNextNumber();
				ip.reset();
				ip.gamma(gamma);
				img.setProcessor(ip);
				gd.repaint();
				return true;
			}
		};

		GenericDialog gd = new GenericDialog("Gamma Adjuster");
		gd.addImage(img);
		gd.addSlider("Gamma:", 0.05, 5.0, 1);
		gd.addDialogListener(listener);
		gd.showDialog();
	}

}
