/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.operatortree.actions.CutCopyPasteAction;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.LogService;


/**
 * A tree displaying repository contents.
 *
 * @author Simon Fischer, Tobias Malbrecht
 */
public class RepositoryTree extends JTree {

	private static final long serialVersionUID = -6613576606220873341L;

	/**
	 * Abstract superclass of actions that are executed on subclasses of {@link Entry}.
	 * Automatically enables/disables itself.
	 */
	private abstract class AbstractRepositoryAction<T extends Entry> extends ResourceAction {
		
		private static final long serialVersionUID = 7980472544436850356L;

		private final Class<T> requiredSelectionType;
		private final boolean needsWriteAccess;
		
		private AbstractRepositoryAction(Class<T> requiredSelectionType, boolean needsWriteAccess, String i18nKey) {
			super(true, i18nKey);			
			this.requiredSelectionType = requiredSelectionType;
			this.needsWriteAccess = needsWriteAccess;
			setEnabled(false);
		}

		@Override
		protected void update(boolean[] conditions) {
			// we have our own mechanism to enable/disable actions,
			// so ignore ConditionalAction mechanism
		}
		
		protected void enable() {
			Entry entry = getSelectedEntry();
			setEnabled((entry != null) && requiredSelectionType.isInstance(entry) && (!needsWriteAccess || !entry.isReadOnly()));
		}
		
		public void actionPerformed(ActionEvent e) {
			actionPerformed(requiredSelectionType.cast(getSelectedEntry()));
		}
		
		public abstract void actionPerformed(T cast);
	}

	public final AbstractRepositoryAction<Repository> CONFIGURE_ACTION = new AbstractRepositoryAction<Repository>(Repository.class, false, "configure_repository") {			
		private static final long serialVersionUID = 1L;		
		@Override
		public void actionPerformed(Repository repository) {
			new RepositoryConfigurationDialog(repository).setVisible(true);
		}
	};

	public final AbstractRepositoryAction<Entry> COPY_LOCATION_ACTION = new AbstractRepositoryAction<Entry>(Entry.class, false, "repository_copy_location") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(Entry e) {
			String value = e.getLocation().toString();
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(value), new ClipboardOwner() {				
				@Override
				public void lostOwnership(Clipboard clipboard, Transferable contents) { }
			});
		}			
	};
	
	public final AbstractRepositoryAction<DataEntry> OPEN_ACTION = new AbstractRepositoryAction<DataEntry>(DataEntry.class, false, "open_repository_entry") {			
		private static final long serialVersionUID = 1L;
		
		@Override
		public void actionPerformed(DataEntry data) {
			if (data instanceof IOObjectEntry) {
				OpenAction.showAsResult((IOObjectEntry) data);
			} else if (data instanceof ProcessEntry) {
				openProcess((ProcessEntry) data);
			}
		}									
	};
	
	public final AbstractRepositoryAction<Folder> REFRESH_ACTION = new AbstractRepositoryAction<Folder>(Folder.class, false, "repository_refresh_folder") {			
		private static final long serialVersionUID = 1L;				
		@Override
		public void actionPerformed(final Folder folder) {					
			ProgressThread openProgressThread = new ProgressThread("refreshing") {
				public void run() {
					try {
						folder.refresh();
					} catch (Exception e) {
						SwingTools.showSimpleErrorMessage("cannot_refresh_folder", e);
					}
				}
			};
			openProgressThread.start();														
		}					
	};
	
	public final AbstractRepositoryAction<Folder> CREATE_FOLDER_ACTION = new AbstractRepositoryAction<Folder>(Folder.class, true, "repository_create_folder") {			
		private static final long serialVersionUID = 1L;				
		@Override
		public void actionPerformed(final Folder folder) {				
			ProgressThread openProgressThread = new ProgressThread("create_folder") {
				public void run() {			
					String name = SwingTools.showInputDialog("repository.new_folder", "");
					if (name != null) {
						try {
							folder.createFolder(name);
						} catch (Exception e) {
							SwingTools.showSimpleErrorMessage("cannot_create_folder", e, name);
						}
					}
				}
			};
			openProgressThread.start();														
		}					
	};
	
	public final AbstractRepositoryAction<Entry> STORE_PROCESS_ACTION = new AbstractRepositoryAction<Entry>(Entry.class, true, "repository_store_process") {
		private static final long serialVersionUID = 1252342138665768477L;

		@Override
		public void actionPerformed(Entry entry) {
			if (entry instanceof Folder) {
				storeInFolder(Folder.class.cast(entry));
			}
			if (entry instanceof ProcessEntry) {
				overwriteProcess(ProcessEntry.class.cast(entry));
			}
		}
		
		private void storeInFolder(final Folder folder) {
			final String name = SwingTools.showInputDialog("store_process", "");
			if (name != null) {
				if (name.isEmpty()) {
					SwingTools.showVerySimpleErrorMessage("please_enter_non_empty_name");
					return;
				}
				try {
					if (folder.containsEntry(name)) {
						SwingTools.showVerySimpleErrorMessage("repository_entry_already_exists", name);
						return;
					}
				} catch (RepositoryException e1) {
					SwingTools.showSimpleErrorMessage("cannot_store_process_in_repository", e1, name);
					return;
				}

				ProgressThread storeProgressThread = new ProgressThread("store_process") {
					public void run() {
						getProgressListener().setTotal(100);
						try {
							getProgressListener().setCompleted(10);
							Process process = RapidMinerGUI.getMainFrame().getProcess();
							process.setProcessLocation(new RepositoryProcessLocation(new RepositoryLocation(folder.getLocation(), name)));
							folder.createProcessEntry(name, process.getRootOperator().getXML(false));
							expandPath(getSelectionPath());
						} catch (Exception e) {
							SwingTools.showSimpleErrorMessage("cannot_store_process_in_repository", e, name);
						} finally {
							getProgressListener().setCompleted(10);
							getProgressListener().complete();
						}
					}
				};					
				storeProgressThread.start();
			}
		}
		
		private void overwriteProcess(final ProcessEntry processEntry) {
			if (SwingTools.showConfirmDialog("overwrite", ConfirmDialog.YES_NO_OPTION, processEntry.getLocation()) == ConfirmDialog.YES_OPTION) {
				ProgressThread storeProgressThread = new ProgressThread("store_process") {
					@Override
					public void run() {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(10);
						try {								
							Process process = RapidMinerGUI.getMainFrame().getProcess();
							process.setProcessLocation(new RepositoryProcessLocation(processEntry.getLocation()));
							processEntry.storeXML(process.getRootOperator().getXML(false));
						} catch (Exception e) {
							SwingTools.showSimpleErrorMessage("cannot_store_process_in_repository", e, processEntry.getName());								
						} finally {
							getProgressListener().setCompleted(100);
							getProgressListener().complete();
						}
					}						
				};
				storeProgressThread.start();
			}
		}
		
		@Override
		protected void enable() {
			Entry entry = getSelectedEntry();
			setEnabled((Folder.class.isInstance(entry) || ProcessEntry.class.isInstance(entry)));
		}
	};
	
	private final AbstractRepositoryAction<Entry> RENAME_ACTION = new AbstractRepositoryAction<Entry>(Entry.class, true, "repository_rename_entry") {
		private static final long serialVersionUID = 9154545892241244065L;

		@Override
		public void actionPerformed(Entry entry) {
			String name = SwingTools.showInputDialog("file_chooser.rename", entry.getName(), entry.getName());
			if (name != null) {
				boolean success = false;
				try {
					success = entry.rename(name);
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("cannot_rename_entry", e, entry.getName(), name);
				}
				if (!success) {
					SwingTools.showVerySimpleErrorMessage("cannot_rename_entry", entry.getName(), name);
				}
			}
		}
		
	};

	public final AbstractRepositoryAction<Entry> DELETE_ACTION = new AbstractRepositoryAction<Entry>(Entry.class, true, "repository_delete_entry") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(Entry entry) {
			if (SwingTools.showConfirmDialog("file_chooser.delete", ConfirmDialog.YES_NO_OPTION, entry.getName()) == ConfirmDialog.YES_OPTION) {
				try {
					entry.delete();
				} catch (Exception e1) {
					SwingTools.showSimpleErrorMessage("cannot_delete_entry", e1);
				}
			}
		}								
	};
	
	
	private final Collection<AbstractRepositoryAction<?>> allActions = new LinkedList<AbstractRepositoryAction<?>>();

	private EventListenerList listenerList = new EventListenerList();
	
	public RepositoryTree() {
		this(null);
	}
	
	public RepositoryTree(Dialog owner) {
		super(new RepositoryTreeModel(RepositoryManager.getInstance(null)));

		allActions.add(OPEN_ACTION);
		allActions.add(STORE_PROCESS_ACTION);
		allActions.add(RENAME_ACTION);
		allActions.add(DELETE_ACTION);
		allActions.add(CREATE_FOLDER_ACTION);
		allActions.add(REFRESH_ACTION);
		allActions.add(COPY_LOCATION_ACTION);
		allActions.add(CONFIGURE_ACTION);
		RENAME_ACTION.addToActionMap(this, WHEN_FOCUSED);
		DELETE_ACTION.addToActionMap(this, WHEN_FOCUSED);
		REFRESH_ACTION.addToActionMap(this, WHEN_FOCUSED);
		
		setRootVisible(false);
		setShowsRootHandles(true);
		setCellRenderer(new RepositoryTreeCellRenderer());
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton()== MouseEvent.BUTTON3){
			    	int row = getRowForLocation(e.getX(),e.getY());
			        setSelectionInterval(row, row);
					if (e.isPopupTrigger()) {
						showPopup(e);
					}
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton()== MouseEvent.BUTTON3){
			    	int row = getRowForLocation(e.getX(),e.getY());
			        setSelectionInterval(row, row);
					if (e.isPopupTrigger()) {
						showPopup(e);
					}				
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton()== MouseEvent.BUTTON3){
			    	int row = getRowForLocation(e.getX(),e.getY());
			        setSelectionInterval(row, row);
					if (e.isPopupTrigger()) {
						showPopup(e);
					}
				}
			}			
		});			

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					TreePath path = getSelectionPath();
					if (path == null) {
						return;
					}
					fireLocationSelected((Entry) path.getLastPathComponent());
				}
			}			
		});
		
		setDragEnabled(true);
        setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				List<DataFlavor> flavors = Arrays.asList(transferFlavors);
				boolean contains = flavors.contains(DataFlavor.javaFileListFlavor);
				contains |= flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
				return contains;				
			}
				
			/** Imports data files using a Wizard. */
			@Override
			public boolean importData(final TransferSupport ts) {
				// determine where to insert
				final Entry droppedOn;
				if (ts.isDrop()) {
					Point dropPoint = ts.getDropLocation().getDropPoint();
					TreePath path = getPathForLocation((int)dropPoint.getX(), (int)dropPoint.getY());
					if (path == null) {
						return false;
					}
					droppedOn = (Entry) path.getLastPathComponent();
				} else {
					droppedOn = getSelectedEntry();
				}
				if (droppedOn == null) {
					return false;
				}
				
				try {
					List<DataFlavor> flavors = Arrays.asList(ts.getDataFlavors());
					if (flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
						final RepositoryLocation loc = (RepositoryLocation) ts.getTransferable().getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
						if (droppedOn instanceof Folder) {
							new ProgressThread("copy_repository_entry", true) {
								public void run() {
									try {
										if (ts.isDrop() && (ts.getDropAction() == MOVE)) {
											RepositoryManager.getInstance(null).move(loc, (Folder)droppedOn, getProgressListener());
										} else {
											RepositoryManager.getInstance(null).copy(loc, (Folder)droppedOn, getProgressListener());
										}
									} catch (RepositoryException e) {
										SwingTools.showSimpleErrorMessage("error_in_copy_repository_entry", e, loc.toString(), e.getMessage());
									}
								}
							}.start();
							return true;	
						} else {
							return false;
						}
					} else if (flavors.contains(DataFlavor.javaFileListFlavor)) {
						List files = (List)ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						File file = (File) files.get(0);						
						DataImportWizard.importData(file, droppedOn.getLocation());
						return true;
					} else {
						return false;
					}
				} catch (UnsupportedFlavorException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot accept drop flavor: "+e, e);
					return false;
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Error during drop: "+e, e);
					return false;
				}
			}
			
			@Override
			public int getSourceActions(JComponent c) {
			    return COPY_OR_MOVE;
			}
			@Override
			protected Transferable createTransferable(JComponent c) {
				TreePath path = getSelectionPath();
				if (path != null) {
					Entry e = (Entry)path.getLastPathComponent();
					final RepositoryLocation location = e.getLocation();
					return new Transferable() {
						@Override
						public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
							if (flavor.equals(DataFlavor.stringFlavor)) {
								return location.getAbsoluteLocation();
							} else if (TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR.equals(flavor)) {
								return location;
							} else {
								throw new IllegalArgumentException("Flavor not supported: "+flavor);
							}
						}
						@Override
						public DataFlavor[] getTransferDataFlavors() {
							return new DataFlavor[] { 
									TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR,
									DataFlavor.stringFlavor
							};
						}
						@Override
						public boolean isDataFlavorSupported(DataFlavor flavor) {
							return TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR.equals(flavor) ||
							DataFlavor.stringFlavor.equals(flavor);
						}
					};
				} else {
					return null;
				}
			}        
        });

//        setTransferHandler(new OperatorTransferHandler() {
//			private static final long serialVersionUID = 1L;
//			@Override
//			protected List<Operator> getDraggedOperators() {
//				TreePath path = getSelectionPath();
//				if (path != null) {
//					Entry e = (Entry)path.getLastPathComponent();
//					if (e instanceof IOObjectEntry) {
//						RepositorySource source;
//						try {
//							source = OperatorService.createOperator(RepositorySource.class);
//							source.setParameter(RepositorySource.PARAMETER_REPOSITORY_ENTRY, e.getLocation().getAbsoluteLocation());
//							return Collections.<Operator>singletonList(source);
//						} catch (OperatorCreationException e1) {
//							LogService.getRoot().log(Level.WARNING, "Cannot create RepositorySource: "+e, e);
//							return null;
//						}						
//					} else {
//						return null;
//					}
//				} else {
//					return null;
//				}
//			}        	
//        });
     
        getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				enableActions();			
			}			
		});
        
        addTreeExpansionListener(new TreeExpansionListener() {
			
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				// select the last expanded/collapsed path 
				selectionModel.setSelectionPath(event.getPath());
			}
			
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				// select the last expanded/collapsed path 
 				treeExpanded(event);
			}
		});
        
        enableActions();
        
        new ToolTipWindow(owner, new TipProvider() {			
			@Override
			public String getTip(Object o) {		
				if (o instanceof Entry) {
					return ToolTipProviderHelper.getTip((Entry) o);
				} else {
					return null;
				}
			}
			
			@Override
			public Object getIdUnder(Point point) {
				TreePath path = getPathForLocation((int)point.getX(), (int)point.getY());
				if (path != null) {
					return path.getLastPathComponent();
				} else {
					return null;
				}
			}

			@Override
			public Component getCustomComponent(Object o) {
				if (o instanceof Entry) {
					return ToolTipProviderHelper.getCustomComponent((Entry) o);
				} else {
					return null;
				}
			}
		}, this);
	}
	
	public void addRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.add(RepositorySelectionListener.class, listener);
	}

	public void removeRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.remove(RepositorySelectionListener.class, listener);
	}
	
	private void fireLocationSelected(Entry entry) {
		RepositorySelectionEvent event = null;
		for (RepositorySelectionListener l : listenerList.getListeners(RepositorySelectionListener.class)) {
			if (event == null) {
				event = new RepositorySelectionEvent(entry);
			}
			l.repositoryLocationSelected(event);
		}
	}

	/** Selects as much as possible of the selected path to the given location.
	 *  Returns true if the given location references a folder. */
	boolean expandIfExists(RepositoryLocation relativeTo, String location) {
		RepositoryLocation loc; 
		boolean full = true;
		try {
			loc = new RepositoryLocation(relativeTo, location);
		} catch (Exception e) {
			// do nothing
			return false;
		}
		Entry entry = null;
		while (true) {
			try {
				entry = loc.locateEntry();
				if (entry != null) {
					break;
				}
			} catch (RepositoryException e) {
				return false;				
			}			
			loc = loc.parent();
			if (loc == null) {
				return false;
			}
			full = false;
		}
		if (entry != null) {
			RepositoryTreeModel model = (RepositoryTreeModel) getModel();		
			TreePath pathTo = model.getPathTo(entry);
			expandPath(pathTo);
			setSelectionPath(pathTo);
			if (entry instanceof Folder) {
				return full;
			}
		}
		return false;		
		//loc = loc.parent();
	}
	
	public void enableActions() {
		for (AbstractRepositoryAction<?> action : allActions) {
			action.enable();
		}
	}

	private void showPopup(MouseEvent e) {
		TreePath path = getSelectionPath();
		if (path == null) {
			return;
		}
		Object selected = path.getLastPathComponent();
		JPopupMenu menu = new JPopupMenu();
		if (selected instanceof Repository) {
			if (((Repository) selected).isConfigurable()) {
				menu.add(CONFIGURE_ACTION);
			}
		}
		if (selected instanceof DataEntry) {
			menu.add(OPEN_ACTION);
		}
		if (selected instanceof ProcessEntry ||
			selected instanceof Folder) {
			menu.add(STORE_PROCESS_ACTION);
		}
		if (selected instanceof Entry) {
			menu.add(RENAME_ACTION);			
		}
		if (selected instanceof Folder) {
			menu.add(CREATE_FOLDER_ACTION);			
		}
		//menu.add(CutCopyPasteAction.CUT_ACTION);
		
		menu.addSeparator();
		menu.add(CutCopyPasteAction.COPY_ACTION);
		menu.add(CutCopyPasteAction.PASTE_ACTION);		
		menu.add(COPY_LOCATION_ACTION);
		if (selected instanceof Entry) {
			menu.add(DELETE_ACTION);
		}

		if (selected instanceof Folder) {
			menu.addSeparator();
			menu.add(REFRESH_ACTION);
		}
		
		if (selected instanceof Entry) {
			Collection<Action> customActions = ((Entry) selected).getCustomActions();
			if ((customActions != null) && !customActions.isEmpty()) {
				menu.addSeparator();
				for (Action a : customActions) {
					menu.add(a);
				}
			}
		}
		
		menu.show(this, e.getX(), e.getY());
	}

	/** Opens the process held by the given entry (in the background) and opens it. */
	public static void openProcess(final ProcessEntry processEntry) {
		ProgressThread openProgressThread = new ProgressThread("open_process") {
			public void run() {							
				RepositoryProcessLocation processLocation = new RepositoryProcessLocation(processEntry.getLocation());
				if (RapidMinerGUI.getMainFrame().close()){
					OpenAction.open(processLocation, false);
				}
				/* PRE FIX OF BUG 308: When opening process with double click all changes are discarded
				 * 
				 * try {
					RepositoryProcessLocation processLocation = new RepositoryProcessLocation(processEntry.getLocation());
					String xml = processEntry.retrieveXML();
					try {
						final Process process = new Process(xml);						
						process.setProcessLocation(processLocation);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								RapidMinerGUI.getMainFrame().setOpenedProcess(process, true, processEntry.getLocation().toString());
							}
						});
					} catch (Exception e) {
						RapidMinerGUI.getMainFrame().handleBrokenProxessXML(processLocation, xml, e);
					}								
				} catch (Exception e1) {
					SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);								
				}*/
				
			}
		};
		openProgressThread.start();
	}
	
	private Entry getSelectedEntry() {
		TreePath path = getSelectionPath();
		if (path == null) {
			return null;
		}
		Object selected = path.getLastPathComponent();
		if (selected instanceof Entry) {
			return (Entry) selected;
		} else {
			return null;
		}
	}
	
	public Collection<AbstractRepositoryAction<?>> getAllActions() {
		return allActions;
	}
}
