package com.rapidminer.repository.gui.process;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.ProcessStackTrace;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.LogService;

/**
 * 
 * @author Simon Fischer
 *
 */
public class RemoteProcessesTreeModel implements TreeModel {

	private static final long serialVersionUID = 1L;

	private static final long UPDATE_PERIOD = 2500;

	private final class UpdateTask extends TimerTask {
		@Override
		public void run() {
			final List<RemoteRepository> newRepositories = RemoteRepository.getAll();			
			TreeModelEvent event = null;
			if (!newRepositories.equals(repositories)) {
				event = new TreeModelEvent(this, new Object[] { root });	
			}
			final TreeModelEvent topLevelTreeEvent = event;
			 
			final Map<RemoteRepository, List<ProcessResponse>> newProcesses = new HashMap<RemoteRepository, List<ProcessResponse>>();
			final List<TreeModelEvent> repositoryEvents = new LinkedList<TreeModelEvent>(); 
			for (RemoteRepository repos : newRepositories) {
				//if (repos.isConnected()) {
				try {
					List<ProcessResponse> runningProcesses = repos.getProcessService().getRunningProcesses();
					List<ProcessResponse> oldProcesses = processes.get(repos);
					if ((oldProcesses == null) || !oldProcesses.equals(runningProcesses)) {
						repositoryEvents.add(new TreeModelEvent(this, new TreePath(new Object[] {root, repos} )));
					}
					newProcesses.put(repos, runningProcesses);
				} catch (RepositoryException ex) {
					LogService.getRoot().log(Level.WARNING, "Error fetching remote process list: "+ex, ex);					
				}
				//}
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					RemoteProcessesTreeModel.this.repositories = newRepositories;
					RemoteProcessesTreeModel.this.processes    = newProcesses;
					if (topLevelTreeEvent != null) {
						fireStructureChanged(topLevelTreeEvent);
					} else {
						for (TreeModelEvent e : repositoryEvents) {
							fireStructureChanged(e);
						}
					}
				}
			});			
		}
	}

	private Map<RemoteRepository, List<ProcessResponse>> processes = new HashMap<RemoteRepository, List<ProcessResponse>>(); 
	private List<RemoteRepository> repositories = new LinkedList<RemoteRepository>();
	
	private Object root = new Object();
	
	private Timer updateTimer = new Timer("RemoteProcess-Updater", true);

	
	public RemoteProcessesTreeModel() {
		updateTimer.schedule(new UpdateTask(), UPDATE_PERIOD, UPDATE_PERIOD);
	}
	
	private EventListenerList listeners = new EventListenerList();
	
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(TreeModelListener.class, l);		
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent == root) {
			return repositories.get(index);			
		} else if (parent instanceof RemoteRepository) {
			return processes.get(parent).get(index);
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			ProcessStackTrace trace = proResponse.getTrace();
			if ((trace != null) && (trace.getElements() != null)) {
				return trace.getElements().get(index);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent == root) {
			return repositories.size();
		}  else if (parent instanceof RemoteRepository) {
			List<ProcessResponse> list = processes.get((RemoteRepository)parent);
			if (list == null) {
				return 0;
			} else {
				return list.size();
			}
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			ProcessStackTrace trace = proResponse.getTrace();
			if ((trace != null) && (trace.getElements() != null)) {
				return trace.getElements().size();
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent == root) {
			return repositories.indexOf(child);
		} else if (parent instanceof RemoteRepository) {
			return processes.get((RemoteRepository)parent).indexOf(child);
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			ProcessStackTrace trace = proResponse.getTrace();
			if ((trace != null) && (trace.getElements() != null)) {
				return trace.getElements().indexOf(child);
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		return (node != root) && !(node instanceof ProcessResponse) && !(node instanceof RemoteRepository);
	}


	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// not editable		
	}


	private void fireUpdate() {
		TreeModelEvent e = new TreeModelEvent(this, new Object[] { root });
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeStructureChanged(e);
		}
	}					


	private void fireStructureChanged(TreeModelEvent topLevelTreeEvent) {
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeStructureChanged(topLevelTreeEvent);
		}	
	}

}
