package io.hummer.eclipse.search;

import io.hummer.eclipse.search.JarSearcher.FileSearchMatch;
import io.hummer.eclipse.search.JarSearcher.JarFileEntry;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class MainWindow extends ApplicationWindow {

	private static final String TEXT_CANCEL = "Cancel";
	private static final String TEXT_CANCELING = "Canceling...";
	TableViewer tableViewer;
	Table table;
	Composite parent;
	Text searchText;
	Button searchButton;
	Button includeJRE;
	Set<JarFileEntry> searchResults = new HashSet<JarFileEntry>();

	public MainWindow(Shell parentShell) {
		super(parentShell);
		parent = parentShell;
	}

	@Override
	protected Control createContents(Composite parent) {

		getShell().setSize(600, 400);
		getShell().setText("Search Window");
		Device device = Display.getCurrent ();

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Composite comp1 = new Composite(container, SWT.TRANSPARENT);
		GridLayout grid = new GridLayout(4, false);
		comp1.setLayout(grid);
		Text label = new Text(comp1, SWT.TRANSPARENT);
		label.setText("Search string: ");
		Color grey = device.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		label.setBackground(grey);
		label.setEditable(false);
		searchText = new Text(comp1, SWT.NONE);
		searchText.setText("");
		searchText.setSize(200, searchText.getSize().y);
		searchButton = new Button(comp1, SWT.NONE);
		searchButton.setSize(300, searchButton.getSize().y);
		searchButton.setText("Search");
		searchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(searchButton.getText().equals("Search")) {
					search();
				} else if(searchButton.getText().equals(TEXT_CANCEL)) {
					searchButton.setText(TEXT_CANCELING);
				}
			}
		});
		includeJRE = new Button(comp1, SWT.CHECK);
		includeJRE.setText("Include JRE libs");

		// Create the composite
		Composite comp2 = new Composite(container, SWT.NONE);
		comp2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Add TableColumnLayout
		TableColumnLayout layout = new TableColumnLayout();
		comp2.setLayout(layout);

		tableViewer = new TableViewer(comp2, SWT.BORDER | SWT.FULL_SELECTION);
		// viewer.setSorter(new Sorter());
		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						IStructuredSelection sel = (IStructuredSelection) tableViewer
								.getSelection();
						Object o = sel.getFirstElement();
						if (o != null) {
							System.out.println("Selected : " + o);
						} else {
							System.out.println("Selection cleared!");
						}
					}
				});
		table = tableViewer.getTable();
		table.getHorizontalBar().setVisible(true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableViewerColumn col0 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblCol0 = col0.getColumn();
		// Specify width using weights
		layout.setColumnData(tblCol0, new ColumnWeightData(20,
				ColumnWeightData.MINIMUM_WIDTH, true));
		tblCol0.setText("Project(s)");
		col0.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object o) {
				JarFileEntry e = (JarFileEntry) o;
				List<String> list = new LinkedList<String>(e.projectNames);
				Collections.sort(list);
				String names = list.toString();
				return names.substring(1, names.length() - 1);
			}
		});

		TableViewerColumn col1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblCol1 = col1.getColumn();
		// Specify width using weights
		layout.setColumnData(tblCol1, new ColumnWeightData(30,
				ColumnWeightData.MINIMUM_WIDTH, true));
		tblCol1.setText("File Name");
		col1.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object o) {
				JarFileEntry e = (JarFileEntry) o;
				return e.name;
			}
		});

		TableViewerColumn col2 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblCol2 = col2.getColumn();
		layout.setColumnData(tblCol2, new ColumnWeightData(30,
				ColumnWeightData.MINIMUM_WIDTH, true));
		tblCol2.setText("Jar File");
		col2.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object o) {
				JarFileEntry e = (JarFileEntry) o;
				return "" + e.jarFile;
			}
		});

		TableViewerColumn col3 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblCol3 = col3.getColumn();
		layout.setColumnData(tblCol3, new ColumnWeightData(2,
				ColumnWeightData.MINIMUM_WIDTH, true));
		tblCol3.setText("# Matches");
		col3.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object o) {
				JarFileEntry e = (JarFileEntry) o;
				return "" + e.matches.size();
			}
		});

		TableViewerColumn col4 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblCol4 = col4.getColumn();
		layout.setColumnData(tblCol4, new ColumnWeightData(50,
				ColumnWeightData.MINIMUM_WIDTH, true));
		tblCol4.setText("Matches");
		col4.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object o) {
				JarFileEntry e = (JarFileEntry) o;
				return e.dumpMatches(50);
			}
		});

		return container;
	}

	private void search() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final JarSearcher s = new JarSearcher(workspace);
		try {
			searchResults.clear();
			searchButton.setText(TEXT_CANCEL);
			final Display display = Display.getDefault();
			final SearchResultListener l = new SearchResultListener() {
				public void onResult(final FileSearchMatch match) {
					display.asyncExec(new Runnable() {
						public void run() {
							searchResults.add(match.file);
							tableViewer.setInput(searchResults);
						}
					});
				}
				@Override
				public boolean stillRunning() {
					final LinkedBlockingQueue<Boolean> q = new LinkedBlockingQueue<Boolean>();
					display.asyncExec(new Runnable() {
						public void run() {
							try {
								q.put(searchButton.getText().equals(TEXT_CANCEL));
							} catch (InterruptedException e) { }
						}
					});
					try {
						return q.take();
					} catch (InterruptedException e) {
						return false;
					}
				}
				public void searchFinished() {
					display.asyncExec(new Runnable() {
						public void run() {
							searchButton.setText("Search");
						}
					});
				}
			};
			final String search = searchText.getText();
			final boolean jre = includeJRE.getSelection();
			new Thread() {
				public void run() {
					try {
						s.getJarEntriesContaining(search, jre, l);
					} catch (Exception e) {
						e.printStackTrace(); // TODO logging
					}
				}
			}.start();
			//searchResults.addAll(files);
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openInformation(parent.getShell(), "Error",
					"An error occurred: " + e);
		}

	}

}
