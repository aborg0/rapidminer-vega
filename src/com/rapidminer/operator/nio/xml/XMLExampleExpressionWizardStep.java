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
package com.rapidminer.operator.nio.xml;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.AbstractListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTextField;
import com.rapidminer.gui.tools.ExtendedJTextField.TextChangeListener;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.io.Encoding;

/**
 * This step allows to enter an XPath expression whose
 * matches will be used as examples.
 * 
 * @author Sebastian Land
 */
public class XMLExampleExpressionWizardStep extends WizardStep {

    private static Properties XML_PROPERTIES = new Properties();
    {
        XML_PROPERTIES.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    private class XMLTreeModel implements TreeModel {

        private LinkedList<TreeModelListener> listeners = new LinkedList<TreeModelListener>();
        private Document document;

        public XMLTreeModel(Document document) {
            this.document = document;
        }

        @Override
        public Object getRoot() {
            return document.getDocumentElement();
        }

        @Override
        public Object getChild(Object parent, int index) {
            Element element = (Element) parent;
            NodeList childNodes = element.getChildNodes();
            int elementIndex = 0;
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element) {
                    if (elementIndex == index)
                        return childNodes.item(i);
                    else
                        elementIndex++;
                }
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            Element element = (Element) parent;
            NodeList childNodes = element.getChildNodes();
            int elementIndex = 0;
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element) {
                    elementIndex++;
                }
            }
            return elementIndex;
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            // firing event to all listener
            for (TreeModelListener listener : listeners) {
                listener.treeNodesChanged(new TreeModelEvent(this, path));
            }
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Element element = (Element) parent;
            NodeList childNodes = element.getChildNodes();
            int elementIndex = 0;
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element) {
                    if (child == childNodes.item(i))
                        return elementIndex;
                    elementIndex++;
                }
            }
            return -1;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }
    }

    private class XPathMatchesListModel extends AbstractListModel {
        private static final long serialVersionUID = 5596412058073512745L;
        private Document document;
        private XPath xpath;
        private NodeList exampleNodes;

        public XPathMatchesListModel(Document document) {
            this.document = document;
            this.xpath = XPathFactory.newInstance().newXPath();
        }

        public void setXPathExpression(String expression) {
            XPathExpression exampleExpression = null;
            try {
                exampleExpression = xpath.compile(expression);
            } catch (XPathExpressionException e1) {
                errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", e1));
                errorLabel.setForeground(Color.RED);
            }
            if (exampleExpression != null) {
                try {
                    int oldSize = getSize();
                    exampleNodes = (NodeList) exampleExpression.evaluate(document, XPathConstants.NODESET);
                    fireContentsChanged(this, 0, Math.min(oldSize, exampleNodes.getLength()));
                    if (oldSize > exampleNodes.getLength()) {
                        fireIntervalRemoved(this, exampleNodes.getLength(), oldSize - 1);
                    } else if (oldSize < exampleNodes.getLength()) {
                        fireIntervalAdded(this, oldSize, exampleNodes.getLength() - 1);
                    }
                    errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.xpath_result", exampleNodes.getLength()));
                    errorLabel.setForeground(Color.BLACK);
                } catch (XPathExpressionException e) {
                    errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", e));
                    errorLabel.setForeground(Color.RED);
                    exampleNodes = null;
                }
            } else {
                exampleNodes = null;
            }
        }

        @Override
        public int getSize() {
            if (exampleNodes == null)
                return 0;
            return exampleNodes.getLength();
        }

        @Override
        public Object getElementAt(int index) {
            return exampleNodes.item(index);
        }

    }

    private class AttributeTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final int NAME_COLUMN = 0;
        private static final int VALUE_COLUMN = 1;

        private NamedNodeMap attributes;

        @Override
        public int getRowCount() {
            if (attributes != null)
                return attributes.getLength();
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case NAME_COLUMN:
                return "attribute";
            case VALUE_COLUMN:
                return "value";
            }
            return "";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case NAME_COLUMN:
                return attributes.item(rowIndex).getNodeName();
            case VALUE_COLUMN:
                return attributes.item(rowIndex).getNodeValue();
            }
            return null;
        }

        public void setElement(Element element) {
            this.attributes = element.getAttributes();
            fireTableDataChanged();
        }

    }

    private XMLResultSetConfiguration configuration;

    private JPanel component = new JPanel(new GridBagLayout());
    private XMLTreeModel xmlTreeModel;
    private JTree xmlTree = new JTree();
    private AttributeTableModel attributeTableModel = new AttributeTableModel();
    private JTable attributeTable = new JTable(attributeTableModel);
    private JList matchesList = new JList();
    private JLabel errorLabel = new JLabel();
    private XPathMatchesListModel matchesListModel;
    private ExtendedJTextField expressionField = new ExtendedJTextField();

    /**
     * There must be a configuration given, but might be empty.
     * 
     * @throws OperatorException
     */
    public XMLExampleExpressionWizardStep(AbstractWizard parent, final XMLResultSetConfiguration configuration) throws OperatorException {
        super("importwizard.xml.example_expression");
        this.configuration = configuration;

        // adding components

        JPanel leftBarPanel = new JPanel(new GridBagLayout());
        {
            GridBagConstraints leftBarConstraints = new GridBagConstraints();
            leftBarConstraints.insets = new Insets(0, 5, 5, 5);
            leftBarConstraints.fill = GridBagConstraints.BOTH;
            leftBarConstraints.weightx = 1;
            leftBarConstraints.weighty = 0.7;
            leftBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
            leftBarPanel.add(new ExtendedJScrollPane(xmlTree), leftBarConstraints);
            leftBarConstraints.weighty = 0.3;
            leftBarConstraints.insets = new Insets(5, 5, 0, 5);
            leftBarPanel.add(new ExtendedJScrollPane(attributeTable), leftBarConstraints);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1d;
        c.weightx = 0.3d;
        component.add(leftBarPanel, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.7d;
        component.add(new ExtendedJScrollPane(matchesList), c);

        c.weightx = 1d;
        c.weighty = 0d;

        errorLabel.setForeground(Color.RED);
        errorLabel.setText(" asds");
        c.weighty = 0;
        component.add(errorLabel, c);
        component.add(new ExtendedJScrollPane(expressionField), c);

        // listener
        expressionField.getModel().addTextChangeListener(new TextChangeListener() {
            @Override
            public void informTextChanged(String newValue) {
                if (matchesListModel != null)
                    matchesListModel.setXPathExpression(newValue);
            }
        });

        xmlTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Element element = (Element) e.getNewLeadSelectionPath().getLastPathComponent();
                if (element != null) {
                    attributeTableModel.setElement(element);
                }
            }
        });

        xmlTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = xmlTree.getClosestPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        StringBuilder builder = new StringBuilder();
                        for (Object pathObject: path.getPath()) {
                            Element element = (Element) pathObject;
                            builder.append("/");

                            // treating namespace if necessary
                            String namespaceURI = element.getNamespaceURI();
                            if (namespaceURI != null) {
                                // if default namespace uri: Don't do anything
                                if (!namespaceURI.equals(configuration.getDefaultNamespaceURI())) {
                                    String namespaceId = configuration.getNamespaceId(namespaceURI);
                                    if (namespaceId == null) {
                                        // set error as namespace hasn't been configured
                                    } else {
                                        builder.append(namespaceId);
                                        builder.append(":");
                                    }
                                }
                            }

                            // appending element name
                            builder.append(element.getTagName());
                        }

                        // now set text to textmodel of editfield
                        expressionField.setText(builder.toString());
                        expressionField.setText(builder.toString());
                    }
                }
            }
        });

        // configure renderer
        matchesList.setCellRenderer(new ListCellRenderer() {
            private static final long serialVersionUID = 1L;
            private JTextArea area = new JTextArea();
            private JPanel wraperPanel = new JPanel(new GridLayout(1, 1));
            private JPanel emptyPanel = new JPanel();
            {
                wraperPanel.add(area);
                wraperPanel.setBorder(new EmptyBorder(new Insets(0, 0, 1, 0)));
                wraperPanel.setBackground(Color.BLACK);
                wraperPanel.setOpaque(true);
            }

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    Charset encoding = Encoding.getEncoding("UTF-8");
                    XMLTools.stream(new DOMSource((Node) value), new StreamResult(new OutputStreamWriter(os, encoding)), encoding, XML_PROPERTIES);
                    area.setText(os.toString("UTF-8"));
                    return wraperPanel;
                } catch (XMLException e) {
                    return emptyPanel;
                } catch (UnsupportedEncodingException e) {
                    return emptyPanel;
                }
            }
        });
        xmlTree.setCellRenderer(new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;
            private JPanel emptyPanel = new JPanel();

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof Element) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<");
                    Element element = (Element) value;
                    builder.append(element.getTagName());
                    builder.append(">");

                    JLabel treeCellRendererComponent = (JLabel) super.getTreeCellRendererComponent(tree, builder.toString(), selected, expanded, leaf, row, hasFocus);
                    treeCellRendererComponent.setIcon(null);

                    return treeCellRendererComponent;
                }
                // this should not happen, as the model only shows elements
                return emptyPanel;
            }
        });
    }

    @Override
    protected boolean performEnteringAction(WizardStepDirection direction) {
        try {
            xmlTreeModel = new XMLTreeModel(configuration.getDocumentObjectModel());
            xmlTree.setModel(xmlTreeModel);
            matchesListModel = new XPathMatchesListModel(configuration.getDocumentObjectModel());
            matchesList.setModel(matchesListModel);
            if (configuration.getExampleXPath() != null)
                expressionField.getModel().setValue(configuration.getExampleXPath());
            else
                expressionField.getModel().setValue("asds");
        } catch (OperatorException e) {
            errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.cannot_load_dom", e));
        }
        return true;
    }

    @Override
    protected boolean performLeavingAction(WizardStepDirection direction) {
        return true;
    }

    @Override
    protected JComponent getComponent() {
        return component;
    }

    @Override
    protected boolean canProceed() {
        return true;
    }

    @Override
    protected boolean canGoBack() {
        return true;
    }
}
