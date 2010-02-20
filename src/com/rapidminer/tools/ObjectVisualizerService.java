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
package com.rapidminer.tools;

import java.util.Map;
import java.util.WeakHashMap;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.DummyObjectVisualizer;
import com.rapidminer.gui.ExampleVisualizer;


/** 
 * This class provides the management of {@link ObjectVisualizer}s. 
 * 
 * @author Ingo Mierswa
 */
public class ObjectVisualizerService {

	private static final DummyObjectVisualizer DUMMY_VISUALIZER = new DummyObjectVisualizer();
	
	private static final Map<Object, ObjectVisualizer> visualizerMap = new WeakHashMap<Object, ObjectVisualizer>();
	
	/**
	 * This method adds the given visualizer for the target object. Please not that only one 
	 * visualizer per object is allowed. The subsequent added visualizer will overwrite the first.
	 * 
	 * The targets will be remembered using a weak reference, so that they don't pose a memory
	 * leak: If the object isn't referenced anywhere else, it will be deleted.
	 */
	public static void addObjectVisualizer(Object target, ObjectVisualizer visualizer) {
		visualizerMap.put(target, visualizer);
	}

	/**
	 * Returns the object visualizer registered for this targetObject. If the targetObject is of type
	 * ExampleSet and there's no special visualizer registered, it will return an new ExampleVisualizer.
	 */
	public static ObjectVisualizer getVisualizerForObject(Object targetObject) {
		ObjectVisualizer capableVisualizer = visualizerMap.get(targetObject);
		if (capableVisualizer == null) {
			if (targetObject instanceof ExampleSet) {
				ObjectVisualizer visualizer = new ExampleVisualizer((ExampleSet)targetObject);
				addObjectVisualizer(targetObject, visualizer);
				return visualizer;
			}
			return DUMMY_VISUALIZER;
		}
		return capableVisualizer;
	}
}
