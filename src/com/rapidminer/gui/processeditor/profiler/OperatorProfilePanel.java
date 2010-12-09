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
package com.rapidminer.gui.processeditor.profiler;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.processeditor.profiler.data.ProfilerData;
import com.rapidminer.gui.processeditor.profiler.data.ProfilerDataManager;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;


/**
 * The JPanel which displays the data for one operator.
 * 
 * @author Marco Boeck
 */
public class OperatorProfilePanel extends JPanel {
	
	private JLabel label_operatorIcon;
	private JLabel label_operatorName;
	private JLabel label_operatorRunCountDescription;
	private JLabel label_operatorAverageTimeRunningDescription;
	private JLabel label_operatorLastTimeRunningDescription;
	private JLabel label_operatorAverageCpuTimeRunningDescription;
	private JLabel label_operatorLastCpuTimeRunningDescription;
	private JLabel label_operatorRunCountValue;
	private JLabel label_operatorAverageTimeRunningValue;
	private JLabel label_operatorLastTimeRunningValue;
	private JLabel label_operatorAverageCpuTimeRunningValue;
	private JLabel label_operatorLastCpuTimeRunningValue;
	private JLabel label_operatorCpuResourceConsumptionEstimationDescription;
	private JLabel label_operatorCpuResourceConsumptionEstimationValue;
	private JLabel label_operatorMemoryResourceConsumptionEstimationDescription;
	private JLabel label_operatorMemoryResourceConsumptionEstimationValue;
	
	private static final long serialVersionUID = 8830933821400357100L;
	
	
	/**
	 * Constructor which needs the operator for which the gui component should be constructed.
	 */
	public OperatorProfilePanel(Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}
		
		this.label_operatorName = new JLabel();
		this.label_operatorIcon = new JLabel();
		this.label_operatorRunCountDescription = new JLabel();
		this.label_operatorLastTimeRunningDescription = new JLabel();
		this.label_operatorAverageTimeRunningDescription = new JLabel();
		this.label_operatorAverageCpuTimeRunningDescription = new JLabel();
		this.label_operatorLastCpuTimeRunningDescription = new JLabel();
		this.label_operatorRunCountValue = new JLabel();
		this.label_operatorLastTimeRunningValue = new JLabel();
		this.label_operatorAverageTimeRunningValue = new JLabel();
		this.label_operatorAverageCpuTimeRunningValue = new JLabel();
		this.label_operatorLastCpuTimeRunningValue = new JLabel();
		this.label_operatorCpuResourceConsumptionEstimationDescription = new JLabel();
		this.label_operatorCpuResourceConsumptionEstimationValue = new JLabel();
		this.label_operatorMemoryResourceConsumptionEstimationDescription = new JLabel();
		this.label_operatorMemoryResourceConsumptionEstimationValue = new JLabel();
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		// make sure data exists
		ProfilerData data;
		if (!ProfilerDataManager.getInstance().isOperatorDataAvailable(operator.getName())) {
			data = ProfilerDataManager.getInstance().addOperatorData(
					operator.getProcess(), operator.getName(), new ProfilerData());
		} else {
			data = ProfilerDataManager.getInstance().getOperatorData(operator.getName());
		}
		// retrieve data and display it
		DecimalFormat formatter = new DecimalFormat("#.##");
		this.label_operatorName.setText("<html><b>" + operator.getName() + "</b></html>");
		this.label_operatorIcon.setIcon(operator.getOperatorDescription().getIcon());
		this.label_operatorRunCountDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.run_count.text"));
		String runValue = String.valueOf(data.getRunCount());
		this.label_operatorRunCountValue.setText(runValue);
		// real time labels
		this.label_operatorLastTimeRunningDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.last_exec_real.text"));
		String lastTimeRunningValue;
		if (data.hasRun()) {
			if (data.getLastRunTimeReal() < 1000) {
				lastTimeRunningValue = data.getLastRunTimeReal() == 0 ? "<1ms" : String.valueOf(formatter.format(data.getLastRunTimeReal()) + "ms");
			} else
			if (data.getLastRunTimeReal() < 600000) {
				lastTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeReal()/1000) + "s");
			} else {
				lastTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeReal()/60000) + "min");
			}
		} else {
			lastTimeRunningValue = "-";
		}
		this.label_operatorLastTimeRunningValue.setText(lastTimeRunningValue);
		this.label_operatorAverageTimeRunningDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.average_exec_real.text"));
		String averageTimeRunningValue;
		if (data.hasRun()) {
			if (data.getAverageRunTimeReal() < 1000) {
				averageTimeRunningValue = data.getAverageRunTimeReal() == 0 ? "<1ms" : String.valueOf(formatter.format(data.getAverageRunTimeReal()) + "ms");
			} else
			if (data.getAverageRunTimeReal() < 600000) {
				averageTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeReal()/1000) + "s");
			} else {
				averageTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeReal()/60000) + "min");
			}
		} else {
			averageTimeRunningValue = "-";
		}
		this.label_operatorAverageTimeRunningValue.setText(averageTimeRunningValue);
		// cpu time labels
		this.label_operatorLastCpuTimeRunningDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.last_exec_cpu.text"));
		String lastCpuTimeRunningValue;
		if (data.hasRun()) {
			if (data.getLastRunTimeCpu() < 1000) {
				lastCpuTimeRunningValue = data.getLastRunTimeCpu() == 0 ? "<1ns" : String.valueOf(formatter.format(data.getLastRunTimeCpu()) + "ns");
			} else
			if (data.getLastRunTimeCpu() < 1000000) {
				lastCpuTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeCpu()/1000) + "µs");
			} else
			if (data.getLastRunTimeCpu() < 1000000000) {
				lastCpuTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeCpu()/1000000) + "ms");
			} else
			if (data.getLastRunTimeCpu() < Long.parseLong("600000000000")) {
				lastCpuTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeCpu()/1000000000) + "s");
			} else {
				lastCpuTimeRunningValue = String.valueOf(formatter.format(data.getLastRunTimeCpu()/Double.parseDouble("60000000000")) + "min");
			}
		} else {
			lastCpuTimeRunningValue = "-";
		}
		this.label_operatorLastCpuTimeRunningValue.setText(lastCpuTimeRunningValue);
		this.label_operatorAverageCpuTimeRunningDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.average_exec_cpu.text"));
		String averageCpuTimeRunningValue;
		if (data.hasRun()) {
			if (data.getAverageRunTimeCpu() < 1000) {
				averageCpuTimeRunningValue = data.getAverageRunTimeCpu() == 0 ? "<1ns" : String.valueOf(formatter.format(data.getAverageRunTimeCpu()) + "ns");
			} else
			if (data.getAverageRunTimeCpu() < 1000000) {
				averageCpuTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeCpu()/1000) + "µs");
			} else
			if (data.getAverageRunTimeCpu() < 1000000000) {
				averageCpuTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeCpu()/1000000) + "ms");
			} else
			if (data.getAverageRunTimeCpu() < Double.parseDouble("600000000000")) {
				averageCpuTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeCpu()/1000000000) + "s");
			} else {
				averageCpuTimeRunningValue = String.valueOf(formatter.format(data.getAverageRunTimeCpu()/Double.parseDouble("60000000000")) + "min");
			}
		} else {
			averageCpuTimeRunningValue = "-";
		}
		this.label_operatorAverageCpuTimeRunningValue.setText(averageCpuTimeRunningValue);
		
		//TODO: Implement
		// CPU
		this.label_operatorCpuResourceConsumptionEstimationDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.resource_consumption_cpu.text"));
		if (operator.getResourceConsumptionEstimator() == null) {
			this.label_operatorCpuResourceConsumptionEstimationValue.setText(
					I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.resource_consumption.text_null"));
		} else {
			this.label_operatorCpuResourceConsumptionEstimationValue.setText("" + operator.getResourceConsumptionEstimator().getCpuFunction());
		}
		// Memory
		this.label_operatorMemoryResourceConsumptionEstimationDescription.setText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.resource_consumption_memory.text"));
		if (operator.getResourceConsumptionEstimator() == null) {
			this.label_operatorMemoryResourceConsumptionEstimationValue.setText(
					I18N.getMessage(I18N.getGUIBundle(), "gui.label.profiler_operator_panel.resource_consumption.text_null"));
		} else {
			this.label_operatorMemoryResourceConsumptionEstimationValue.setText("" + operator.getResourceConsumptionEstimator().getMemoryFunction());
		}
		
		// format GridBagLayout
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.0f;
		gbc.weighty = 0.0f;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		label_operatorName.setFont(label_operatorName.getFont().deriveFont(label_operatorName.getFont().getStyle() ^ Font.BOLD));

		add(label_operatorName, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		add(label_operatorIcon, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0f;
		gbc.gridwidth = 2;
		gbc.ipadx = 2;
		gbc.ipady = 1;
		gbc.weightx = 0.0f;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 5, 0, 5);
		add(label_operatorCpuResourceConsumptionEstimationDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		add(label_operatorMemoryResourceConsumptionEstimationDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		add(label_operatorRunCountDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 4;
		add(label_operatorLastTimeRunningDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 5;
		add(label_operatorLastCpuTimeRunningDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 6;
		add(label_operatorAverageTimeRunningDescription, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 7;
		add(label_operatorAverageCpuTimeRunningDescription, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 1.0f;
		gbc.gridwidth = 2;
		add(label_operatorCpuResourceConsumptionEstimationValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 2;
		add(label_operatorMemoryResourceConsumptionEstimationValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 3;
		add(label_operatorRunCountValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 4;
		add(label_operatorLastTimeRunningValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 5;
		add(label_operatorLastCpuTimeRunningValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 6;
		add(label_operatorAverageTimeRunningValue, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 7;
		add(label_operatorAverageCpuTimeRunningValue, gbc);
		
		setBorder(BorderFactory.createLoweredBevelBorder());
	}

}
