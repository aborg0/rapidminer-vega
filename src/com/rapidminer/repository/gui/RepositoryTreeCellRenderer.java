/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
import java.text.DateFormat;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.tools.Tools;
/**
 * @author Simon Fischer
 */
public class RepositoryTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private final Icon ICON_SERVER        = SwingTools.createIcon("16/server.png");
	private final Icon ICON_FOLDER_OPEN   = SwingTools.createIcon("16/folder.png");
	private final Icon ICON_FOLDER_CLOSED = SwingTools.createIcon("16/folder_closed.png");
	private final Icon ICON_FOLDER_LOCKED = SwingTools.createIcon("16/folder_lock.png");
	private final Icon ICON_PROCESS       = SwingTools.createIcon("16/gear.png");
	private final Icon ICON_DATA          = SwingTools.createIcon("16/data.png");
	private final Icon ICON_BLOB          = SwingTools.createIcon("16/data.png");
	private final Icon ICON_TEXT          = SwingTools.createIcon("16/text.png");
	private final Icon ICON_IMAGE         = SwingTools.createIcon("16/painting.png");

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (value instanceof Entry) {
			Entry entry = (Entry) value;
			String state = "";
			if (entry instanceof Repository) {
				String reposState = ((Repository)entry).getState();
				if (reposState != null) {
					state += reposState + " &ndash; ";
				}
			}
			state += entry.getOwner();
			if (entry instanceof DataEntry) {
				state += "  &ndash; v"+((DataEntry)entry).getRevision();
				long date = ((DataEntry)entry).getDate();
				if (date >= 0) {
					state += ", " + DATE_FORMAT.format(new Date(date));
				}
				long size = ((DataEntry) entry).getSize();
				if (size >= 0) {
					state += " &ndash; " + Tools.formatBytes(size);
				}
			} 
			
			 
			label.setText("<html>" + entry.getName() + " <small style=\"color:gray\">(" + state + ")</small></html>");
			if (entry instanceof Repository) {
				label.setIcon(ICON_SERVER);
			} else if (entry.getType().equals(Folder.TYPE_NAME)) {
				if (entry.isReadOnly()) {
					label.setIcon(ICON_FOLDER_LOCKED);
				} else if (expanded) {
					label.setIcon(ICON_FOLDER_OPEN);
				} else {
					label.setIcon(ICON_FOLDER_CLOSED);
				}
			} else if (entry.getType().equals(IOObjectEntry.TYPE_NAME)) {
				label.setIcon(ICON_DATA);
			} else if (entry.getType().equals(ProcessEntry.TYPE_NAME)) {
				label.setIcon(ICON_PROCESS);
			} else if (entry.getType().equals(BlobEntry.TYPE_NAME)) {
				String mimeType = ((BlobEntry)entry).getMimeType();
				if (mimeType != null) {
					if (mimeType.startsWith("text/") || "application/pdf".equals(mimeType)) {
						label.setIcon(ICON_TEXT);	
					} else if (mimeType.startsWith("image/")) {
						label.setIcon(ICON_IMAGE);	
					} else {
						label.setIcon(ICON_BLOB);
					}
				} else {
					label.setIcon(ICON_BLOB);
				}
			} else {
				label.setIcon(null);
			}
		}
		return label;
	}
}
