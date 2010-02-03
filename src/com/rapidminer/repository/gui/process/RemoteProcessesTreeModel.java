package com.rapidminer.repository.gui.process;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.ProcessStackTrace;
import com.rapid_i.repository.wsimport.ProcessStackTraceElement;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.RemoteProcessState;
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

	private static class PendingEvent {
		private static enum Type { ADD, STRUCTURE_CHANGED, DELETE };
		private TreeModelEvent event;
		private Type type;
		public PendingEvent(TreeModelEvent event, Type type) {		
			this.event = event;
			this.type = type;
		}		
	}

	private class ProcessList {
		private List<Integer> knownIds = new LinkedList<Integer>();
		private Map<Integer,ProcessResponse> processResponses = new HashMap<Integer,ProcessResponse>();
		//		public Collection<ProcessResponse> allProcesses() {
		//			return processResponses.values();
		//		}
		public int add(ProcessResponse pr) {
			int newIndex = -1;
			if (!processResponses.containsKey(pr.getId())) {
				newIndex = knownIds.size();
				knownIds.add(pr.getId());				
			}
			processResponses.put(pr.getId(), pr);
			return newIndex;
		}		
		public ProcessResponse getByIndex(int index) {
			return processResponses.get(knownIds.get(index));
		}
		public ProcessResponse getById(int id) {
			return processResponses.get(id);
		}
		public int size() {
			return knownIds.size();
		}
		public int indexOf(ProcessResponse child) {
			int index = 0;
			for (Integer id : knownIds) {
				ProcessResponse pr = processResponses.get(id);
				if ((pr != null) && (pr.getId() == child.getId())) {
					return index;
				}
				index++;				
			}
			return -1;
		}
		public ProcessList copy() {
			ProcessList copy = new ProcessList();
			for (Integer id : knownIds) {
				copy.add(processResponses.get(id));
			}
			return copy;
		}
		//		public int delete(ProcessResponse process) {
		//			int oldId = knownIds.indexOf(process.getId());
		//			knownIds.remove(oldId);e
		//			processResponses.remove(process.getId());
		//			return oldId;
		//		}
		private void trim(Collection<Integer> processIds, RemoteRepository repos, Collection<PendingEvent> pendingProcessEvents) {
			Iterator<Entry<Integer, ProcessResponse>> i = processResponses.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<Integer,ProcessResponse> entry = i.next();
				ProcessResponse process = entry.getValue();
				if (!processIds.contains(process.getId())) {
					int oldIndex = knownIds.indexOf(process.getId());
					knownIds.remove(oldIndex);
					i.remove();
					pendingProcessEvents.add(new PendingEvent(new TreeModelEvent(this,
							new Object[] { root, repos },
							new int[] { oldIndex },
							new Object[] { process }), PendingEvent.Type.DELETE));
				}
			}
		}
	}

	private final class UpdateTask extends TimerTask {
		@Override
		public void run() {
			final List<RemoteRepository> newRepositories = RemoteRepository.getAll();
			final List<PendingEvent> pendingProcessEvents = new LinkedList<PendingEvent>();			
			if (!newRepositories.equals(repositories)) {
				pendingProcessEvents.add(new PendingEvent(new TreeModelEvent(this, new Object[] { root }), PendingEvent.Type.STRUCTURE_CHANGED));	
			}

			final Map<RemoteRepository, ProcessList> newProcesses = new HashMap<RemoteRepository, ProcessList>();

			for (RemoteRepository repos : newRepositories) {
				if (observedRepositories.contains(repos)) {
					ProcessList oldProcessList = processes.get(repos);
					ProcessList newProcessList = oldProcessList == null ? new ProcessList() : oldProcessList.copy();
					//if (repos.isConnected()) {
						try {
							Collection<Integer> processIds = repos.getProcessService().getRunningProcesses(since);
							// First, delete removed ids
							newProcessList.trim(processIds, repos, pendingProcessEvents);

							for (Integer processId : processIds) {
								ProcessResponse oldProcess = newProcessList.getById(processId);
								// we update if we don't know the id yet or if the process is not complete						
								if (oldProcess == null) {
									ProcessResponse newResponse = repos.getProcessService().getRunningProcessesInfo(processId);
									int newIndex = newProcessList.add(newResponse);
									pendingProcessEvents.add(new PendingEvent(new TreeModelEvent(this, 
											new Object[] {root, repos}, 
											new int[] {newIndex}, 
											new Object[] {newResponse}), PendingEvent.Type.ADD));							
								} else if (!RemoteProcessState.valueOf(oldProcess.getState()).isTerminated()) {
									ProcessResponse updatedResponse = repos.getProcessService().getRunningProcessesInfo(processId);
									newProcessList.add(updatedResponse);
									pendingProcessEvents.add(new PendingEvent(new TreeModelEvent(this, 
											new Object[] {root, repos, updatedResponse}), PendingEvent.Type.STRUCTURE_CHANGED));							

								} else {								
									// If process is terminated, there is not need to update.
									// The process is already in the list since it is copied
								}						
							}
							newProcesses.put(repos, newProcessList);
						} catch (Exception ex) {
							LogService.getRoot().log(Level.WARNING, "Error fetching remote process list: "+ex, ex);
							newProcesses.clear();
							pendingProcessEvents.add(new PendingEvent(new TreeModelEvent(this, new TreePath(new Object[] { root, repos })), PendingEvent.Type.STRUCTURE_CHANGED));
						}
					//}
				}
			}
			// finally, set the new data atomically and fire all events
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					RemoteProcessesTreeModel.this.repositories = newRepositories;
					RemoteProcessesTreeModel.this.processes    = newProcesses;					
					for (PendingEvent e : pendingProcessEvents) {
						switch (e.type) {
						case ADD:
							fireAdd(e.event);
							break;
						case DELETE:
							fireDelete(e.event);
							break;
						case STRUCTURE_CHANGED:
							fireStructureChanged(e.event);
							break;
						default:
							throw new RuntimeException("Unknown event type: "+e.type);	
						}
					}					
				}
			});			
		}
	}

	private Map<RemoteRepository, ProcessList> processes = new HashMap<RemoteRepository, ProcessList>(); 
	private List<RemoteRepository> repositories = new LinkedList<RemoteRepository>();
	private Set<RemoteRepository> observedRepositories = new HashSet<RemoteRepository>();
	
	private Object root = new Object();

	private Timer updateTimer = new Timer("RemoteProcess-Updater", true);

	private XMLGregorianCalendar since;

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
			return processes.get(parent).getByIndex(index);
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			if (proResponse.getException() != null) {
				if (index == 0) {
					return new ExceptionWrapper(proResponse.getException());
				} else {
					return null;
				}
			} else {
				ProcessStackTrace trace = proResponse.getTrace();
				int elementsSize = 0;
				if ((trace != null) && (trace.getElements() != null)) {
					elementsSize = trace.getElements().size();
				}
				if (index < elementsSize) {
					return trace.getElements().get(index);
				} else {
					return new OutputLocation(proResponse.getOutputLocations().get(index - elementsSize));
				}
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
			ProcessList list = processes.get((RemoteRepository)parent);
			if (list == null) {
				return 0;
			} else {
				return list.size();
			}
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			if (proResponse.getException() != null) {
				return 1;
			} else {
				int size = 0;
				ProcessStackTrace trace = proResponse.getTrace();
				if ((trace != null) && (trace.getElements() != null)) {
					size += trace.getElements().size();
				} 
				if (proResponse.getOutputLocations() != null) {
					size += proResponse.getOutputLocations().size();
				} 
				return size;
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
			return processes.get((RemoteRepository)parent).indexOf((ProcessResponse) child);
		} else if (parent instanceof ProcessResponse) {
			ProcessResponse proResponse = (ProcessResponse)parent;
			if (child instanceof ProcessStackTraceElement) {
				ProcessStackTrace trace = proResponse.getTrace();
				if ((trace != null) && (trace.getElements() != null)) {
					return trace.getElements().indexOf(child);
				} else {
					return -1;
				}
			} else if (child instanceof OutputLocation) {
				if (proResponse.getOutputLocations() != null) {
					return proResponse.getOutputLocations().indexOf(((OutputLocation)child).getLocation());
				} else {
					return -1;
				}
			} else if (child instanceof ExceptionWrapper) {
				return 0;
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


	private void fireAdd(TreeModelEvent e) {
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeNodesInserted(e);
		}
	}					

	private void fireDelete(TreeModelEvent event) {
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeNodesRemoved(event);
		}		
	}


	private void fireStructureChanged(TreeModelEvent e) {
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeStructureChanged(e);
		}	
	}

	public void setSince(Date since) {
		if (since == null) {
			this.since = null;
		} else {
			try {
				this.since = XMLTools.getXMLGregorianCalendar(since);
			} catch (DatatypeConfigurationException e) {
				// cannot happen
				throw new RuntimeException(e);	
			}
		}
	}
	
	public void observe(RemoteRepository rep) {
		observedRepositories.add(rep);
	}
	public void ignore(RemoteRepository rep) {
		observedRepositories.remove(rep);
	}

}
