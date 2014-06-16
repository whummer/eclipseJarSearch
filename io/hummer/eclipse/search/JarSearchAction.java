package io.hummer.eclipse.search;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class JarSearchAction implements IWorkbenchWindowActionDelegate {

	IWorkbenchWindow activeWindow = null;
	ApplicationWindow dialog;

	public void run(IAction proxyAction) {
		dialog.open();
	}

	// IActionDelegate method
	public void selectionChanged(IAction proxyAction, ISelection selection) {
		// do nothing, action is not dependent on the selection
	}

	// IWorkbenchWindowActionDelegate method
	public void init(IWorkbenchWindow window) {
		activeWindow = window;
		dialog = new MainWindow(window.getShell());
		dialog.setStatus("Search ...");
		// System.out.println(dialog.get);
	}

	// IWorkbenchWindowActionDelegate method
	public void dispose() {
		// nothing to do
	}
}
