package org.eclipse.help.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.*;

import org.eclipse.help.*;
import org.eclipse.help.internal.HelpSystem;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
/**
 * Navigation Viewer.  Contains combo for InfoSet selection and Workbook for display
 * of views.
 */
public class NavigationViewer implements ISelectionProvider, IMenuListener {
	private Collection selectionChangedListeners = new ArrayList();
	private Composite contents;
	private EmbeddedHelpView helpView;
	private Combo toc_Combo;
	private TreeViewer viewer;
	private IToc[] tocs;

	/**
	 * NavigationViewer constructor.
	 */
	public NavigationViewer(Composite parent, EmbeddedHelpView helpView) {
		super();
		this.helpView = helpView;
		createControl(parent);
	}
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		viewer.addSelectionChangedListener(listener);
		selectionChangedListeners.add(listener);
	}
	protected Control createControl(Composite parent) {
		// Create a list of available Topics_
		tocs = HelpSystem.getTocManager().getTocs();
		contents = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridData gd = new GridData();
		gd.horizontalAlignment = gd.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = gd.BEGINNING;
		gd.grabExcessVerticalSpace = false;
		// Create combo for selection of Info Sets
		toc_Combo = new Combo(contents, SWT.DROP_DOWN | SWT.READ_ONLY /*| SWT.FLAT*/
		);
		toc_Combo.setLayoutData(gd);

		for (int i = 0; i < tocs.length; i++) {
			toc_Combo.add(tocs[i].getLabel());
		}
		toc_Combo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int index = ((Combo) e.widget).getSelectionIndex();
				final String href = tocs[index].getHref();
				// Switching to another infoset may be time consuming
				// so display the busy cursor
				BusyIndicator.showWhile(null, new Runnable() {
					public void run() {
						try {
							IToc selectedToc = HelpSystem.getTocManager().getToc(href);
							setInput(selectedToc);
						} catch (Exception e) {
						}
					}
				});
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		viewer = new TreeViewer(contents, SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(TreeContentProvider.getDefault());
		viewer.setLabelProvider(ElementLabelProvider.getDefault());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
		for (Iterator it = selectionChangedListeners.iterator(); it.hasNext();) {
			viewer.addSelectionChangedListener((ISelectionChangedListener) it.next());
		}
		WorkbenchHelp.setHelp(
			viewer.getControl(),
			new String[] {
				IHelpUIConstants.TOPICS_VIEWER,
				IHelpUIConstants.NAVIGATION_VIEWER,
				IHelpUIConstants.EMBEDDED_HELP_VIEW });
		createPopUpMenus();
		WorkbenchHelp.setHelp(
			contents,
			new String[] {
				IHelpUIConstants.NAVIGATION_VIEWER,
				IHelpUIConstants.EMBEDDED_HELP_VIEW });
		return contents;
	}
	private void createPopUpMenus() {
		// For now, do this only for win32. 
		if (!System.getProperty("os.name").startsWith("Win"))
			return;
		// create the Menu Manager for tree viewer,and do
		// proper initialization
		MenuManager mgr = new MenuManager();
		Menu shellMenu = mgr.createContextMenu(viewer.getControl());
		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(this);
		viewer.getControl().setMenu(shellMenu);
	}
	public void dispose() {
		((Tree) viewer.getControl()).removeAll();
	}
	public Control getControl() {
		return contents;
	}
	public Object getInput() {
		return viewer.getInput();
	}
	public ISelection getSelection() {
		return viewer.getSelection();
	}
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		viewer.removeSelectionChangedListener(listener);
		selectionChangedListeners.remove(listener);
	}
	/**
	 * Handles double clicks in viewer.
	 * Opens editor if file double-clicked.
	 */
	void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection s = (IStructuredSelection) event.getSelection();
		Object element = s.getFirstElement();
		// Double-clicking in navigator should expand/collapse containers
		if (viewer != null && viewer.isExpandable(element)) {
			viewer.setExpandedState(element, !viewer.getExpandedState(element));
		}
	}
	public void menuAboutToShow(IMenuManager mgr) {
		if (!(getSelection() instanceof IStructuredSelection))
			return;
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		if (((IStructuredSelection) selection).size() == 1) {
			// Menu items for single selection
			if (!(selection.getFirstElement() instanceof ITopic))
				return;
			ITopic topic = (ITopic) selection.getFirstElement();
			if (topic.getSubtopics().length > 0) {
				// add print tree action
				mgr.add(new PrintTopicTreeAction((IStructuredSelection) selection));
				mgr.add(new Separator());
				mgr.update(true);
			}
		}
	}
	/**
	 * New TOC to show
	 */
	public void setInput(Object input) {
		if (input instanceof Toc) {
			// do nothing if asked to display the same TOC
			if (input == getInput())
				return;
			for (int i=0; i<tocs.length; i++)
				if (tocs[i] == input)
				{
					toc_Combo.select(i);
					break;
				}
			// remove selection, so it is gray not blue;
			toc_Combo.clearSelection();
			viewer.setInput(input);
			// update htmlViewer
			setSelection(new StructuredSelection(input));
		}
	}
	/**
	 * Selects to topic given in selection
	 */
	public void setSelection(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return;
		Object o = ((IStructuredSelection) selection).getFirstElement();
		if (o == null)
			return;
		for (Iterator it = selectionChangedListeners.iterator(); it.hasNext();) {
			((ISelectionChangedListener) it.next()).selectionChanged(
				new SelectionChangedEvent(this, selection));
		}
		ITopic t = null;
		if (o instanceof ITopic) {
			t = ((ITopic) o);
		} else if (o instanceof String) { // Synchronization to given url
			String url = (String) o;
			//		ITopic topic =
			//			HelpSystem.getTopicsNavigationManager().getNavigationModel(currentTopics.getHref()).getTopic(url);
			//		if (topic == null)
			//			return;
		}
		if (t != null) {
			// Expand all nodes between root of model and the given element, exclusive
			viewer.expandToLevel(t, 0);
			// Select given element
			viewer.setSelection(new StructuredSelection(t), true);
		}
	}
}