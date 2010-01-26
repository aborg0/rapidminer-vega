package com.rapidminer.repository.gui.process;

import java.awt.Component;
import java.text.DateFormat;
import java.util.Calendar;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.ProcessStackTraceElement;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.Tools;

/**
 * 
 * @author Simon Fischer
 *
 */
public class RemoteProcessTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private Icon SERVER_ICON = SwingTools.createIcon("16/application_server_run.png");
	private Icon PROCESS_ICON = SwingTools.createIcon("16/gear.png");
	private Icon OPERATOR_ICON = SwingTools.createIcon("16/element_selection");
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (value instanceof RemoteRepository) {
			label.setText(((RemoteRepository) value).getName());
			label.setIcon(SERVER_ICON);
		} else if (value instanceof ProcessResponse) {
			ProcessResponse processResponse = (ProcessResponse) value;
			Calendar startTime = processResponse.getStartTime().toGregorianCalendar();			
			label.setText(processResponse.getProcessLocation() +" ("+processResponse.getState()+"; started "+DateFormat.getDateTimeInstance().format(startTime.getTime())+")");
			label.setIcon(PROCESS_ICON);
		} else if (value instanceof ProcessStackTraceElement) {
			ProcessStackTraceElement element = (ProcessStackTraceElement) value;
			label.setText(element.getOperatorName() + " ["+element.getApplyCount()+", "+Tools.formatDuration(element.getExecutionTime())+"]");
			label.setIcon(OPERATOR_ICON);
		} else {
			label.setText("???");			
		}
		return label;
	}
}
