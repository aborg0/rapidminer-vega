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
package com.rapidminer.operator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.GenericOperatorFactory;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.documentation.OperatorDocumentation;
import com.rapidminer.tools.plugin.Plugin;



/**
 * Data container for name, class, short name, path and the (very short)
 * description of an operator. If the corresponding operator is not marked 
 * as deprecated the deprecation info string should be null. If the icon 
 * string is null, the group icon will be used.
 * 
 * @author Ingo Mierswa
 */
public class OperatorDescription implements Comparable<OperatorDescription> {

	private final String key;
	private final Class<? extends Operator> clazz;
	private List<String> replacesDeprecatedKeys;
	
	private final OperatorDocumentation documentation;

	private ImageIcon[] icons;
	private final String iconName;

	private final GroupTree groupTree;
	
	/**
	 * @deprecated Only used for Weka
	 */
	@Deprecated
	private final String deprecationInfo = null;

	private final Plugin provider;

	private boolean enabled = true;

	/** Parses an operator in the RM 5.0 standard and assigns it to the given group. 
	 * @param bundle */
	@SuppressWarnings("unchecked")
	public OperatorDescription(GroupTree groupTree, Element element, ClassLoader classLoader, Plugin provider, OperatorDocBundle bundle) throws ClassNotFoundException, XMLException {
		this.provider = provider;
		this.groupTree = groupTree;		
		
		key = XMLTools.getTagContents(element, "key", true);
		
		this.iconName = XMLTools.getTagContents(element, "icon");
		
		Class<?> generatedClass = Class.forName(XMLTools.getTagContents(element, "class", true).trim(), true, classLoader);
		this.clazz = (Class<? extends Operator>) generatedClass;

		this.documentation = (OperatorDocumentation) bundle.getObject("operator."+key);
		if (documentation.getName().equals("")) {
			documentation.setName(key);
			documentation.setDocumentation("Operator's description is missing in referenced OperatorDoc.");
		}
		groupTree.addOperatorDescription(this);		
		loadIcons();

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ((child instanceof Element) && (((Element)child).getTagName().equals("replaces"))) {
				setIsReplacementFor(((Element)child).getTextContent());
			}
		}
	}
	
	/** Constructor for programmatic (non-parsed) creation of OperatorDescriptions, e.g. by a {@link GenericOperatorFactory}. */
	public OperatorDescription(String key, Class<? extends Operator> clazz, GroupTree groupTree, ClassLoader classLoader, String iconName, Plugin provider) {
		this(key, clazz, groupTree, classLoader, iconName, provider, null); 
	}
	
	/** Constructor for programmatic (non-parsed) creation of OperatorDescriptions, e.g. by a {@link GenericOperatorFactory}.
	 * Additionally this allows to specify an operator documentation bundle where the docu is retrieved from.
	 *  */
	public OperatorDescription(String key, Class<? extends Operator> clazz, GroupTree groupTree, ClassLoader classLoader, String iconName, Plugin provider, OperatorDocBundle bundle) {		
		this.key = key;
		this.clazz = clazz;
		this.groupTree = groupTree;
		this.provider = provider;
		this.iconName = iconName;
		if (bundle == null) {
			this.documentation = new OperatorDocumentation(key);
		} else {
			this.documentation = (OperatorDocumentation) bundle.getObject("operator."+key);
			if (documentation.getName().equals("")) {
				documentation.setName(key);
				documentation.setDocumentation("Operator's description is missing in referenced OperatorDoc.");
			}
		}
		groupTree.addOperatorDescription(this);
		loadIcons();
	}
		
	/** Creates a new operator description object. If the corresponding operator is not marked as deprecated the 
	 *  deprecation info string should be null. If the icon string is null, the group icon will be used.
	 * @deprecated No I18N support. */	
	@Deprecated
	public OperatorDescription(ClassLoader classLoader, String key, String name, String className, 
			String group, String iconName, String deprecationInfo, Plugin provider) throws ClassNotFoundException {
		this(classLoader, key, name, className, null, null, group, iconName, deprecationInfo, provider);
	}

	/** Creates an operator description with the given fields.
	 *  @deprecated This constructor cannot provide an internationalization mechanism since description
	 *    is not taken from operator documentation bundle. */	
	@SuppressWarnings("unchecked")
	@Deprecated
	public OperatorDescription(ClassLoader classLoader, String key, String name, String className, 
			String shortDescription, String longDescription, 
			String groupName, String iconName, String deprecationInfo, Plugin provider) throws ClassNotFoundException {	
		this.key = key;
		this.iconName = iconName;
		this.clazz = (Class<? extends Operator>) Class.forName(className, true, classLoader);
		this.documentation = new OperatorDocumentation(name);
		this.documentation.setSynopsis(shortDescription);
		this.documentation.setDocumentation(longDescription);
		this.documentation.setDeprecation(deprecationInfo);
		
		this.groupTree = GroupTree.findGroup(groupName, null);
		groupTree.addOperatorDescription(this);

		this.provider = provider;

		loadIcons();
	}

//	private void reloadIcon(int index, int size, String iconName) {
//		if (this.iconPaths[index] == null) {
//			if ((iconName != null) && (iconName.length() > 0)) {
//				this.icons[index] = SwingTools.createIcon("operators/"+size+"/" + iconName + ".png");
//				this.iconPaths[index] = "operators/"+size+"/" + iconName + ".png";
//			} else {
//				// try group from most special to most general group
//				String group = getGroup();
//				String groupIconName = group.toLowerCase();
//				this.icons[index] = SwingTools.createIcon("groups/"+size+"/" + groupIconName + ".png");
//				while ((this.icons[index] == null) && (groupIconName.length() > 0)) {
//					if (groupIconName.indexOf(".") >= 0) {
//						groupIconName = groupIconName.substring(0, groupIconName.lastIndexOf(".")).toLowerCase();
//						this.icons[index] = SwingTools.createIcon("groups/"+size+"/" + groupIconName + ".png");
//					} else {
//						groupIconName = "";
//					}
//				}
//				this.iconPaths[index] = "groups/"+size+"/" + groupIconName + ".png";
//			}
//		} else {
//			this.icons[index] = SwingTools.createIcon(this.iconPaths[index]);
//		}		
//	}

//	/** TODO: Remove after operator renaming 
//	 * @deprecated Remove after operator renaming. */
//	@Deprecated
//	public void setKey(String key) {
//		this.key = key;
//		getOperatorDocumentation().setKey(key);
//	}
	
	public String getName() {
		return getOperatorDocumentation().getName();
	}
	
	public String getShortName() {
		return getOperatorDocumentation().getShortName();
	}

	public Class<? extends Operator> getOperatorClass() {
		return clazz;
	}

	public String getShortDescription() {
		return getOperatorDocumentation().getSynopsis();
	}

	public String getLongDescriptionHTML() {
		return getOperatorDocumentation().getDocumentation();
	}

	public OperatorDocumentation getOperatorDocumentation() {
		return documentation;
	}

	public String getGroup() {
		return groupTree.getQName();
	}
	
	public String getGroupName() {
		return groupTree.getGroupName();
	}

	public ImageIcon getIcon() {
		return getIcons()[1];
	}

	public ImageIcon getSmallIcon() {
		ImageIcon[] icons2 = this.getIcons();
		if (icons2[0] != null) {
			return icons2[0];
		} else {
			return icons2[1];
		}
	}

	public ImageIcon getLargeIcon() {
		ImageIcon[] icons2 = this.getIcons();
		if (icons2[2] != null) {
			return icons2[2];
		} else {
			return icons2[1];
		}
	}

	public String getAbbreviatedClassName() {
		return getOperatorClass().getName().replace("com.rapidminer.operator.", "c.r.o.");
	}

	public String getDeprecationInfo() {
		if (deprecationInfo != null) {
			return deprecationInfo;
		} else {
			return getOperatorDocumentation().getDeprecation();
		}
	}

	public boolean isDeprecated() {
		return (deprecationInfo != null);
	}

	public String getProviderName() {
		return provider != null ? provider.getName() : OperatorService.RAPID_MINER_CORE_PREFIX;
	}
	
	/**
	 * This defines the namespace of the provider. If is core, OperatorService.RAPID_MINER_CORE_NAMESPACE is returned.
	 * Otherwise the namespace of the extension is returned as defined by the manifest.xml
	 * @return
	 */
	public String getProviderNamespace() {
		return provider != null ? provider.getExtensionId() : OperatorService.RAPID_MINER_CORE_NAMESPACE;
	}

	public String getKey() {
		if (provider != null) {
			return provider.getPrefix() + ":"  + this.key;
		} else {
			return this.key;
		}
	}

	public void disable() {
		this.enabled  = false;
	}

	/** Some operators may be disabled, e.g. because they cannot be applied inside an application server (file access etc.) */
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String toString() {
		return "key='" + key + "'; name='"+getName()+"'; "+(replacesDeprecatedKeys!=null?"replaces: "+replacesDeprecatedKeys:"")+"; implemented by " + clazz.getName() + "; group: " + groupTree.getQName() + "; icon: " + iconName;
	}

	public int compareTo(OperatorDescription d) {
		String myName = this.getName();		
		String otherName = d.getName();
		return myName.compareTo(otherName);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OperatorDescription)) {
			return false;
		} else {
			OperatorDescription other = (OperatorDescription)o;
			return this.getKey().equals(other.getKey());
		}
	}

	@Override
	public int hashCode() {
		return this.getKey().hashCode();
	}

	/** Creates a new operator based on the description. */
	public Operator createOperatorInstance() throws OperatorCreationException {
		if (!isEnabled()) {
			throw new OperatorCreationException(OperatorCreationException.OPERATOR_DISABLED_ERROR, key + "(" + clazz.getName() + ")", null);
		}
		Operator operator = null;		
		try {
			java.lang.reflect.Constructor<? extends Operator> constructor = clazz.getConstructor(new Class[] { OperatorDescription.class });
			operator = constructor.newInstance(new Object[] { this });
			// necessary in order to allow parameter usage for Weka operators (dynamically created parameters)
			if (this.getName().startsWith("W-")) {
				operator.getParameterTypes();
			}
		} catch (InstantiationException e) {
			throw new OperatorCreationException(OperatorCreationException.INSTANTIATION_ERROR, key + "(" + clazz.getName() + ")", e);
		} catch (IllegalAccessException e) {
			throw new OperatorCreationException(OperatorCreationException.ILLEGAL_ACCESS_ERROR, key + "(" + clazz.getName() + ")", e);
		} catch (NoSuchMethodException e) {
			throw new OperatorCreationException(OperatorCreationException.NO_CONSTRUCTOR_ERROR, key + "(" + clazz.getName() + ")", e);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw new OperatorCreationException(OperatorCreationException.CONSTRUCTION_ERROR, key + "(" + clazz.getName() + ")", e);
		}
		OperatorService.invokeCreationHooks(operator);
		return operator;
	}

	public void setIsReplacementFor(String opName) {
		if (replacesDeprecatedKeys == null) {
			replacesDeprecatedKeys = new LinkedList<String>();
		}
		replacesDeprecatedKeys.add(opName);
	}
	
	/** Returns keys of deprecated operators replaced by this operator. */
	public List<String> getReplacedKeys() {
		if (replacesDeprecatedKeys != null) {
			return replacesDeprecatedKeys;
		} else {
			return Collections.emptyList();
		}
	}

	public String getIconName() {
		if (iconName != null) {
			return iconName;
		} else {
			return groupTree.getIconName();
		}
	}
	
	private void loadIcons() {
		if (iconName != null) {
			icons = new ImageIcon[3];
			icons[0] = SwingTools.createIcon("16/"+iconName);
			icons[1] = SwingTools.createIcon("24/"+iconName);
			icons[2] = SwingTools.createIcon("48/"+iconName);
		} else {
			icons = null;
		}
	}
	
	private ImageIcon[] getIcons() {
		if (icons != null) {
			return icons;
		} else {
			return groupTree.getIcons();
		}
	}
	
	public Plugin getProvider() {
		return provider;
	}
	
	public GroupTree getGroupTree() {
		return groupTree;
	}
}
