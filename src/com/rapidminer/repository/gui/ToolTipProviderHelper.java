package com.rapidminer.repository.gui;

import java.awt.Component;
import java.util.logging.Level;

import com.rapidminer.gui.flow.ExampleSetMetaDataTableModel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;

/**
 * 
 * @author Simon Fischer
 *
 */
public class ToolTipProviderHelper {

	public static String getTip(Entry o) {
		if (o instanceof IOObjectEntry) {
			IOObjectEntry e = (IOObjectEntry) o;
			StringBuilder tip = new StringBuilder();
			tip.append("<h3>").append(e.getName()).append("</h3>");
			if (!e.willBlock()) {
				try {
					MetaData metaData = e.retrieveMetaData();
					if (metaData != null) {
						tip.append("<p>");
						if (metaData instanceof ExampleSetMetaData) {
							tip.append(((ExampleSetMetaData)metaData).getShortDescription());
						} else {
							tip.append(metaData.getDescription());
						}
						tip.append("</p>");
					}
				} catch (RepositoryException e1) {
					LogService.getRoot().log(Level.WARNING, "Cannot fetch meta data for tool tip: "+e, e);
					return null;
				}
			} else {
				tip.append("<p>Meta data for this object not loaded yet.<br/><a href=\"loadMetaData?");
				tip.append(e.getLocation().toString());
				tip.append("\">Click to load.</a></p>");
			}
			return tip.toString();
		} else {
			if (o instanceof Entry) {
				StringBuilder tip = new StringBuilder();
				tip.append("<h3>").append(((Entry)o).getName()).append("</h3><p>").append(((Entry)o).getDescription()).append("</p>");
				if (o instanceof BlobEntry) {
					tip.append("<p><strong>Type:</strong> ").append(((BlobEntry)o).getMimeType()).append("</p>");
				}
				return tip.toString();
			} else {
				return null;	
			}
		}
	}

	public static Component getCustomComponent(Entry o) {
		if (o instanceof IOObjectEntry) {
			IOObjectEntry e = (IOObjectEntry) o;
			if (!e.willBlock()) {
				try {
					MetaData metaData = e.retrieveMetaData();
					if ((metaData != null) && (metaData instanceof ExampleSetMetaData)) {
						return ExampleSetMetaDataTableModel.makeTableForToolTip((ExampleSetMetaData) metaData);
					}
				} catch (Exception ex) {
					LogService.getRoot().log(Level.WARNING, "Error retrieving meta data for "+e.getLocation()+": "+ex, ex);							
				}
			}
		}
		return null;				
	}
	
}
