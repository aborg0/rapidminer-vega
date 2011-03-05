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
package com.rapidminer.gui.processeditor.association;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.operatortree.OperatorTreeCellEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.Pair;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * View that recommends operators based on the operators used in the current process.
 * 
 * @author Marco Boeck
 *
 */
public class AssociationViewer extends JPanel implements Dockable, Observer {
	
	/** the tree holding the operators */
	private JTree tree;
	
	public static final String ASSOCIATION_VIEWER_DOCK_KEY = "association_viewer";
	
	private static final long serialVersionUID = 7208990793923476542L;
	
	private final DockKey DOCK_KEY = new ResourceDockKey(ASSOCIATION_VIEWER_DOCK_KEY);
	
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}
	
	
	/**
	 * Constructor to create th view. Needs the according AssociationListener.
	 * @param listener the AssociationListener
	 */
	public AssociationViewer(AssociationListener listener) {
		listener.addObserver(this);
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		tree = new JTree(new DefaultMutableTreeNode("Operators"));
		tree.setCellEditor(new OperatorTreeCellEditor(tree));
		tree.setCellRenderer(new OperatorTreeRenderer());
		tree.setRowHeight(20);
		tree.setDragEnabled(true);
		tree.setTransferHandler(new OperatorTreeTransferHandler());
		// don't allow collapse
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			
			@Override
			public void treeWillExpand(TreeExpansionEvent event)
					throws ExpandVetoException { }
			
			@Override
			public void treeWillCollapse(TreeExpansionEvent event)
					throws ExpandVetoException {
				throw new ExpandVetoException(event);
			}
		});
		JScrollPane scrollPane = new ExtendedJScrollPane(tree);
		scrollPane.setBorder(null);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		this.add(scrollPane, gbc);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(Observable o, Object arg) {
		if (arg != null) {
			((DefaultMutableTreeNode)tree.getModel().getRoot()).removeAllChildren();
			try {
			List<Pair<String, Double>> list = (List<Pair<String, Double>>)arg;
			double value;
			String opName;
			OperatorDescription operatorDescription = null;
			for (int i=list.size()-1; i>=0; i--) {
				opName = list.get(i).getFirst();
				operatorDescription = OperatorService.getOperatorDescription(opName);
				value = list.get(i).getSecond()*100;
				((DefaultMutableTreeNode)tree.getModel().getRoot()).add(new OperatorMutableTreeNode(operatorDescription, value));
			}
			tree.expandRow(list.size());
			tree.updateUI();
			} catch (ClassCastException e) {
				return;
			}
		}
	}
	
	
	private class OperatorMutableTreeNode extends DefaultMutableTreeNode {
		
		private ImageIcon icon;
		
		private double confidence;
		
		private OperatorDescription opDesc;
		
		
		private static final long serialVersionUID = 6165602483381902570L;
		
		
		public OperatorMutableTreeNode(Object object, double confidence) {
			super(object);
			this.opDesc = (OperatorDescription) object;
			ImageIcon opIcon = opDesc.getSmallIcon();
			opIcon = (opIcon == null) ? new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)) : opIcon;
			this.confidence = confidence;
			this.icon = opIcon;
		}
		
		public ImageIcon getIcon() {
			return icon;
		}
		
		public double getConfidence() {
			return confidence;
		}
		
		public String getName() {
			return opDesc.getName();
		}
		
		public OperatorDescription getOperatorDescription() {
			return opDesc;
		}
		
	}
	
	
	private class OperatorTreeRenderer extends DefaultTreeCellRenderer {
		
		private static final long serialVersionUID = 2059049018727227041L;

		
		@Override
	    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
	        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	        DecimalFormat formatter = new DecimalFormat("#.#");
	        try {
		        OperatorMutableTreeNode node = (OperatorMutableTreeNode)value;
		        setText(node.getName() + " (" + formatter.format(node.getConfidence()) + "%)");
		        setIcon(node.getIcon());
	        } catch (ClassCastException e) { }
	        setFont(new Font(getFont().getFontName(), Font.PLAIN, 13));
	        return this;
	    }

	}
	
	
	private class OperatorTreeTransferHandler extends OperatorTransferHandler {

		private static final long serialVersionUID = -7964653678882905102L;

		
		@Override
		protected List<Operator> getDraggedOperators() {
			List<Operator> list = new LinkedList<Operator>();
			Operator op;
			if (tree.getSelectionRows() != null) {
				for (int row : tree.getSelectionRows()) {
					OperatorMutableTreeNode node = (OperatorMutableTreeNode)tree.getModel().getChild(tree.getModel().getRoot(), row-1);
					try {
						op = OperatorService.createOperator(node.getOperatorDescription());
						if (op != null) {
							list.add(op);
						}
					} catch (OperatorCreationException e) { 
						LogService.getRoot().warning("could not create operator!");
					}
				}
			}
			return list;
		}
		
	}

}
