/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
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
package com.rapidminer.gui.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.meta.ProcessEmbeddingOperator;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;

/** Transfer handler that supports dragging and dropping operators.
 *  TODO: Implement RepositoryLocation so this handles creation of the operator, rather than, e.g. the 
 *    NewOperatorTree, which cannot resolve relative locations.
 * @author Simon Fischer
 *
 */
public abstract class ReceivingOperatorTransferHandler extends OperatorTransferHandler {

	private static final long serialVersionUID = 5355397064093668659L;

	private final List<DataFlavor> acceptableFlavors = new LinkedList<DataFlavor>();
	
	public ReceivingOperatorTransferHandler() {
		acceptableFlavors.add(TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR);
		//acceptableFlavors.add(TransferableOperator.XML_SERIALIZED_TRANSFERRED_OPERATORS_FLAVOR);
		acceptableFlavors.add(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
		acceptableFlavors.add(DataFlavor.javaFileListFlavor);
		acceptableFlavors.add(DataFlavor.stringFlavor);
	}
		
	/** Drops the operator at the given location. The location may be null,
	 *  indicating that this is a paste.
	 *  */
	protected abstract boolean dropNow(List<Operator> newOperators, Point loc);	
	protected abstract void markDropOver(Point dropPoint);
	protected abstract boolean isDropLocationOk(List<Operator> operator, Point loc);
	protected abstract void dropEnds();
	protected abstract Process getProcess();

	// Drop Support
	
	@Override
	public boolean canImport(TransferSupport ts) {
		for (DataFlavor flavor : acceptableFlavors) {			
			if (ts.isDataFlavorSupported(flavor)) {
				if (ts.isDrop()) {
					markDropOver(ts.getDropLocation().getDropPoint());
				}
				return true;
			}	
		}		
		return false;
	}
	
	@Override
	public boolean importData(TransferSupport ts) {
		if (!canImport(ts)) return false;
		DataFlavor acceptedFlavor = null;
		for (DataFlavor flavor : acceptableFlavors) {
			if (ts.isDataFlavorSupported(flavor)) {
				acceptedFlavor = flavor;
				break;
			}				
		}
		if (acceptedFlavor == null) {
			dropEnds();
			return false; // cannot happen
		}

		Object transferData;				
		try {
			transferData = ts.getTransferable().getTransferData(acceptedFlavor);
		} catch (Exception e1) {						
			LogService.getRoot().log(Level.SEVERE, "While accepting drop: ", e1);
			dropEnds();
			return false;
		}
		List<Operator> newOperators;
		if (acceptedFlavor.equals(DataFlavor.javaFileListFlavor)) {							
			File file = (File)((List)transferData).get(0);
			if (file.getName().toLowerCase().endsWith("."+RapidMiner.PROCESS_FILE_EXTENSION)) {
				// This is a process file
				try {
					ProcessEmbeddingOperator processEmbedder = OperatorService.createOperator(ProcessEmbeddingOperator.class);
					processEmbedder.setParameter(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE, file.getAbsolutePath());
					newOperators = Collections.<Operator>singletonList(processEmbedder);
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("cannot_create_process_embedder", e);
					dropEnds();
					return false;
				} 
			} else {
				// This is probably a data file
				try {
					newOperators = Collections.<Operator>singletonList(AbstractReader.createReader(file.toURI().toURL()));
				} catch (OperatorCreationException e1) {
					LogService.getRoot().log(Level.SEVERE, "While accepting drop: ", e1);		
					SwingTools.showSimpleErrorMessage("cannot_create_reader_for_file", e1,file.getName());
					return false;
				} catch (MalformedURLException e) {
					LogService.getRoot().log(Level.SEVERE, "While accepting drop: ", e);		
					SwingTools.showSimpleErrorMessage("cannot_create_reader_for_file", e, file.getName());
					return false;
				}
				if (newOperators == null) {							
					JOptionPane.showMessageDialog(RapidMinerGUI.getMainFrame(), "No reader operator available for file "+file.getName());
					dropEnds();
					return false;
				}
			}
		} else if (acceptedFlavor.equals(TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR)){
			// This is an operator
			if (transferData instanceof Operator[]) {							
				newOperators = Arrays.asList((Operator[])transferData);
			} else {
				LogService.getRoot().warning("Expected Operator[] for data flavor "+acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(DataFlavor.stringFlavor)) {
			if (transferData instanceof String) {				
				try {
					Process process = new Process((String)transferData);
					newOperators = process.getRootOperator().getSubprocess(0).getOperators();
				} catch (Exception e) {					
					try {
						Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader((String)transferData)));
						NodeList opElements = document.getDocumentElement().getChildNodes();
						Operator newOp = null;
						for (int i = 0; i < opElements.getLength(); i++) {
							Node child = opElements.item(i);
							if (child instanceof Element) {
								Element elem = (Element) child;
								if ("operator".equals(elem.getTagName())) {
									newOp = new XMLImporter(null).parseOperator(elem, new LinkedList<UnknownParameterInformation>());									
									break;
								}
							}
						}
						if (newOp == null) {
							LogService.getRoot().log(Level.WARNING, "Cannot parse operator from clipboard. No <operator> tag found. String is: "+transferData);	
							dropEnds();
							return false;
						}
						newOperators = Collections.singletonList(newOp);
					} catch (Exception e1) {
						LogService.getRoot().log(Level.WARNING, "Cannot parse operator from clipboard ("+e1+"). String is: "+transferData, e1);	
						dropEnds();
						return false;
					}					
				}
			} else {
				LogService.getRoot().warning("Expected String for data flavor "+acceptedFlavor);
				dropEnds();
				return false;
			}
		} else if (acceptedFlavor.equals(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
			if (transferData instanceof RepositoryLocation) {
				RepositoryLocation repositoryLocation = (RepositoryLocation)transferData;
				// true if ioobject is selected, false if process entry
				boolean createSource;
				try {
					Entry entry = repositoryLocation.locateEntry();
					if (entry instanceof ProcessEntry) {
						createSource = false;
					} else {
						createSource = true;
					}
				} catch (Exception e) {
					createSource = true;
				}
				String resolvedLocation;					
				if (getProcess().getRepositoryLocation() != null) {
					resolvedLocation = repositoryLocation.makeRelative(getProcess().getRepositoryLocation().parent());
				} else {
					resolvedLocation = repositoryLocation.getAbsoluteLocation();
				}
				if (createSource) {					
					try {						
						RepositorySource source = OperatorService.createOperator(RepositorySource.class);						
						source.setParameter(RepositorySource.PARAMETER_REPOSITORY_ENTRY, resolvedLocation);
						newOperators = Collections.<Operator>singletonList(source);
					} catch (OperatorCreationException e1) {
						LogService.getRoot().log(Level.WARNING, "Cannot create RepositorySource: "+e1, e1);
						return false;
					}					
				} else {
					try {
						ProcessEmbeddingOperator embedder = OperatorService.createOperator(ProcessEmbeddingOperator.class);
						embedder.setParameter(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE, resolvedLocation);
						newOperators = Collections.<Operator>singletonList(embedder);
					} catch (OperatorCreationException e1) {
						LogService.getRoot().log(Level.WARNING, "Cannot create RepositorySource: "+e1, e1);
						return false;
					}
				}
			} else {
				LogService.getRoot().warning("Expected RepositoryLocation for data flavor "+acceptedFlavor);
				dropEnds();
				return false;
			}
		} else {
			// cannot happen
			dropEnds();
			return false;
		}

		if (ts.isDrop()) {
			// drop
			Point loc = ts.getDropLocation().getDropPoint();
			boolean dropLocationOk = !ts.isDrop() || isDropLocationOk(newOperators, loc);
			if (!dropLocationOk) {
				dropEnds();
				return false;
			} else {
				if (ts.getDropAction() == MOVE) {
					for (Operator operator : newOperators) {
						operator.remove();
					}					
				}
				newOperators = cloneAll(newOperators);
				
//				switch (ts.getDropAction()) {
//				case MOVE:
////					// TODO: We should be doing this in the drop complete method of the source
////					// but this must happen before the operator is inserted. Idea: Always clone?
////					for (Operator op : newOperators) {
////						op.remove();
////					}
//					// We don't have to do anything, but we should clone the operators here.
//					
//					break;
//				case COPY:
//					newOperators = cloneAll(newOperators);					
//					break;
//				}
				boolean result;
				try {
					result = dropNow(newOperators, ts.isDrop() ? loc : null);
				} catch (RuntimeException e) {
					LogService.getRoot().log(Level.WARNING, "Error in drop: "+e, e);
					SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());					
					dropEnds();
					return false;
				}
				dropEnds();
				return result;
			}
		} else {
			// paste
			newOperators = cloneAll(newOperators);
			boolean result; 
			try {
				result = dropNow(newOperators, null);
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "Error in paste: "+e, e);
				SwingTools.showVerySimpleErrorMessage("error_in_paste", e.getMessage(), e.getMessage());
				dropEnds();
				return false;
			}
			dropEnds();
			return result;
		}
	}
	
	private List<Operator> cloneAll(List<Operator> operators) {
		List<Operator> result = new LinkedList<Operator>();
		for (Operator op : operators) {
			result.add(op.cloneOperator(op.getName(), false));
		}
		return result;
	}
}
