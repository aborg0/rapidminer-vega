package com.rapidminer.operator.nio.xml;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;

/**
 * This wizard steps lets the user specify the mapping of namespace URIs to namespace ids.
 * 
 * @author Marius Helf
 *
 * TODO handle xmlns 
 */
public class XMLNamespaceMapWizardStep extends WizardStep {

	private static final String NO_DEFAULT_NAMESPACE = "<none>";
	private XMLResultSetConfiguration configuration;
	private JPanel component = new JPanel(new GridBagLayout());
	private JComboBox defaultNamespaceComboBox = new JComboBox();
	private NamespaceMapTableModel namespaceMapModel;

	public XMLNamespaceMapWizardStep(AbstractWizard parent,  XMLResultSetConfiguration configuration) {
		super("importwizard.xml.namespace_mapping");
		this.configuration = configuration;
		
		// init model for namespace map and add action listener
		namespaceMapModel = new NamespaceMapTableModel(null, null);
		// fire state changed whenever the namespace map table changes
		namespaceMapModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				fireStateChanged();
			}
		});
		
		// add components
		ExtendedJTable namespaceMapTable = new ExtendedJTable();
		namespaceMapTable.setModel(namespaceMapModel);
		GridBagConstraints gridConstraint = new GridBagConstraints();
		gridConstraint.insets = new Insets(0, 5, 5, 5);
		gridConstraint.fill = GridBagConstraints.BOTH;
		gridConstraint.weightx = 1;
		gridConstraint.weighty = 1;
		gridConstraint.gridwidth = GridBagConstraints.REMAINDER;
		component.add(new ExtendedJScrollPane(namespaceMapTable), gridConstraint);
		
		// init default namespace controls
		gridConstraint.gridwidth = 1;
		gridConstraint.weightx = 0;
		gridConstraint.weighty = 0;

		component.add(new JLabel(I18N.getGUILabel("importwizard.xml.namespace_mapping.default_namespace")), gridConstraint);
		gridConstraint.weightx = 1;
		gridConstraint.gridwidth = GridBagConstraints.REMAINDER;
		component.add(defaultNamespaceComboBox, gridConstraint);
	}

	@Override
	protected JComponent getComponent() {
		return component;
	}

	@Override
	protected boolean canProceed() {
		return namespaceMapModel.getIdNamespaceMap().size() == namespaceMapModel.getRowCount() && defaultNamespaceComboBox.getSelectedItem() != null;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}
	
	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (direction != WizardStepDirection.BACKWARD) {
			configuration.setNamespaceAware(true);
			// get all namespaces
			Element rootElement = null;
			try {
				rootElement = configuration.getDocumentObjectModel().getDocumentElement();
			} catch (OperatorException e) {
				// TODO
				e.printStackTrace();
			}
			String[] namespaces = new String[0];
			namespaces = getNamespaces(rootElement).toArray(namespaces);			
			
			namespaceMapModel.initializeData(configuration.getNamespacesMap(), namespaces);
			
			// init default namespace combobox
			defaultNamespaceComboBox.removeAllItems();
			defaultNamespaceComboBox.addItem(NO_DEFAULT_NAMESPACE);
			for (String namespace : namespaces) {
				defaultNamespaceComboBox.addItem(namespace);
			}
			if (configuration.getDefaultNamespaceURI() != null) {
				defaultNamespaceComboBox.setSelectedItem(configuration.getDefaultNamespaceURI());
			} else {
				defaultNamespaceComboBox.setSelectedItem(NO_DEFAULT_NAMESPACE);
			}
			fireStateChanged();
		}
		return true;
	}
	
	/**
	 * Returns a set containing all namespaces defined in element and (recursively) its child-elements.
	 * 
	 */
	protected Set<String> getNamespaces(Node node) {
		Set<String> namespaces = new HashSet<String>();
		if (node == null) {
			return namespaces;
		}
		String namespace = node.getNamespaceURI();
		if (namespace != null) {
			namespaces.add(namespace);
		}
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node child = children.item(i);
			namespaces.addAll(getNamespaces(child));
		}
		return namespaces;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setNamespacesMap(namespaceMapModel.getIdNamespaceMap());
		String selectedNamespace = (String)defaultNamespaceComboBox.getSelectedItem();
		if (selectedNamespace != NO_DEFAULT_NAMESPACE) {
			configuration.setDefaultNamespaceURI(selectedNamespace);
		} else {
			configuration.setDefaultNamespaceURI(null);
		}
		return true;
	}

}
