package ij.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ij.WindowManager;

public class HTMLDialog extends Dialog implements ShellListener, KeyListener {

	private String message;
	private boolean modal;
	private String title;
	private boolean escapePressed;
	private Browser browserDialog;

	public HTMLDialog(String title, String message) {

		super(ij.IJ.getInstance().getShell());
		this.message = message;
		this.title = title;
		// if (!modal) {
		WindowManager.addWindow(this);
		modal = false;
		openSyncExec();
	}

	public HTMLDialog(Shell parentShell, String title, String message) {

		super(parentShell);
		this.message = message;
		// if (!modal) {
		WindowManager.addWindow(this);
		modal = false;
		openSyncExec();
	}

	public HTMLDialog(Shell parentShell, String title, String message, boolean modal) {

		super(parentShell);
		this.message = message;
		modal = true;
		openSyncExec();
	}

	public void openSyncExec() {

		Display.getDefault().syncExec(() -> {
			open();
			GUI.centerOnImageJScreen(HTMLDialog.this.getShell());
		});
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite composite = (Composite)super.createDialogArea(parent);
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 600;
		data.heightHint = 300;
		composite.setLayoutData(data);
		browserDialog = new Browser(composite, SWT.NONE);
		browserDialog.addKeyListener(this);
		browserDialog.setText(message);
		browserDialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {

	}

	protected boolean isResizable() {

		return true;
	};

	@Override
	protected void configureShell(Shell shell) {

		super.configureShell(shell);
		if(title != null)
			shell.setText(title);
	}

	@Override
	public void okPressed() {

		close();
	}

	@Override
	public void shellActivated(ShellEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shellClosed(ShellEvent e) {

		e.doit = false;
		if(!modal)
			WindowManager.removeWindow(this);
		e.doit = true;
	}

	@Override
	public void shellDeactivated(ShellEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shellDeiconified(ShellEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shellIconified(ShellEvent e) {
		// TODO Auto-generated method stub

	}

	public void keyPressed(org.eclipse.swt.events.KeyEvent e) {

		int keyCode = e.keyCode;
		int character = e.character;
		ij.IJ.setKeyDown(keyCode);
		escapePressed = keyCode == SWT.ESC;
		if(character == 'c') {
			browserDialog.getText();
			Clipboard cb = new Clipboard(Display.getDefault());
			String textData = browserDialog.getText();
			TextTransfer textTransfer = TextTransfer.getInstance();
			cb.setContents(new Object[]{textData}, new Transfer[]{textTransfer});
		} else if(keyCode == SWT.CR || keyCode == SWT.LF || character == 'w' || escapePressed)
			close();
	}

	public boolean escapePressed() {

		return escapePressed;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}
}
