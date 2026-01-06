package ij.swt;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

/**
 * Convert SWT mouse event state to the legacy AWT modifier bitmask (deprecated *_MASK constants).
 */
public final class SwtToAwtLegacy {

	private SwtToAwtLegacy() {

	}

	/**
	 * Convert an SWT stateMask (for example from Event.stateMask) and optional numeric button
	 * to AWT legacy modifier mask.
	 *
	 * @param stateMask
	 *            SWT stateMask (SWT.SHIFT, SWT.CTRL, SWT.ALT, SWT.COMMAND, SWT.BUTTON1/2/3)
	 * @param swtButton
	 *            numeric SWT button (1,2,3) or 0 if unknown
	 * @return legacy AWT modifier mask using InputEvent.*_MASK and MouseEvent.BUTTON*_MASK
	 */
	public static int fromSwtStateMaskAndButton(int stateMask, int swtButton) {

		int legacy = 0;
		// Modifier keys
		if((stateMask & SWT.SHIFT) != 0)
			legacy |= InputEvent.SHIFT_MASK;
		if((stateMask & SWT.CTRL) != 0)
			legacy |= InputEvent.CTRL_MASK;
		if((stateMask & SWT.ALT) != 0)
			legacy |= InputEvent.ALT_MASK;
		if((stateMask & SWT.COMMAND) != 0)
			legacy |= InputEvent.META_MASK; // Command -> Meta in AWT (macOS)
		// Button bits can appear either as numeric button or as SWT.BUTTONx bits in stateMask.
		// Check numeric button first if provided:
		if(swtButton == 1)
			legacy |= MouseEvent.BUTTON1_MASK;
		else if(swtButton == 2)
			legacy |= MouseEvent.BUTTON2_MASK;
		else if(swtButton == 3)
			legacy |= MouseEvent.BUTTON3_MASK;
		// Also check SWT button mask bits (some code sets these in stateMask)
		if((stateMask & SWT.BUTTON1) != 0)
			legacy |= MouseEvent.BUTTON1_MASK;
		if((stateMask & SWT.BUTTON2) != 0)
			legacy |= MouseEvent.BUTTON2_MASK;
		if((stateMask & SWT.BUTTON3) != 0)
			legacy |= MouseEvent.BUTTON3_MASK;
		return legacy;
	}

	/**
	 * Convert org.eclipse.swt.widgets.Event to legacy AWT modifier mask.
	 * Uses Event.stateMask and Event.button (if present).
	 */
	public static int fromSwtEvent(Event evt) {

		int btn = 0;
		try {
			btn = evt.button;
		} catch(Throwable t) {
			/* some Event instances may not have button; safe fallback */ }
		return fromSwtStateMaskAndButton(evt.stateMask, btn);
	}

	/**
	 * Convert org.eclipse.swt.events.MouseEvent to legacy AWT modifier mask.
	 */
	public static int fromSwtMouseEvent(org.eclipse.swt.events.MouseEvent me) {

		return fromSwtStateMaskAndButton(me.stateMask, me.button);
	}
}