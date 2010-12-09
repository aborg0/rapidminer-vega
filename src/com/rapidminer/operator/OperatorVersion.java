package com.rapidminer.operator;

import java.util.Comparator;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.plugin.Plugin;

/** Operators can change their behaviour from one version to another. 
 *  Hence, their version is stored in the process XML file. If the behaviour
 *  of an operator changes from, say, version 5.0.003 to 5.0.004, we can notify
 *  the user in version 5.0.004. To that end, the method {@link Operator#getCompatibilityLevel()}
 *  will return 5.0.003, whereas {@link Operator#getIncompatibleVersionChanges()} will return
 *  [5.0.004] (or a superset thereof).
 *  
 *  
 *  <strong>Note:</strong> The version numbers always refer to the plugin the operator is loaded from.
 *  If it is not loaded from a plugin, it refers to the RapidMiner version.
 * 
 * @author Simon Fischer
 *
 */
public class OperatorVersion implements Comparable<OperatorVersion> {

	private final int major;
	private final int minor;
	private final int buildNumber;

	/** Sorts in ascending order. */
	public static final Comparator<OperatorVersion> COMPARATOR = new Comparator<OperatorVersion>() {
		@Override
		public int compare(OperatorVersion o1, OperatorVersion o2) {
			return o1.ordinal() - o2.ordinal();
		}
	};
	
	/** Parses a version string of the form x.xx.xxx
	 * 
	 * @throws IllegalArgumentException for malformed strings.
	 */
	public OperatorVersion(String versionString) {
		if (versionString.isEmpty()) {
			throw new IllegalArgumentException("Unparseable version string: "+versionString);
		}
		// remove characters at end		
		while (Character.isLetter(versionString.charAt(versionString.length()-1))) {
			versionString = versionString.substring(0, versionString.length()-2);
		}
		String[] split = versionString.split("\\.");
		if (split.length != 3) {
			throw new IllegalArgumentException("Unparseable version string: "+versionString);
		}
		try {
			this.major = Integer.parseInt(split[0]);
			this.minor = Integer.parseInt(split[1]);
			this.buildNumber = Integer.parseInt(split[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unparseable version string: "+versionString);
		}
	}

	public OperatorVersion(int major, int minor, int buildNumber) {
		super();
		this.major = major;
		this.minor = minor;
		this.buildNumber = buildNumber;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getBuildNumber() {
		return buildNumber;
	}
	
	public int ordinal() {
		return major * 1000 * 1000 + minor * 1000 + buildNumber;
	}

	/** Sorts in ascending order. */
	@Override
	public int compareTo(OperatorVersion o) {
		return ordinal() - o.ordinal();
	}
	
	@Override
	public String toString() {
		return major + "."+minor+"."+buildNumber;
	}
	
	public static OperatorVersion getLatestVersion(OperatorDescription desc) {		
		try {
			Plugin plugin = desc.getProvider();
			if (plugin == null) {
				return new OperatorVersion(RapidMiner.getLongVersion());
			} else {
				return new OperatorVersion(plugin.getVersion());
			}
		} catch (IllegalArgumentException e) {
			// returning default version of 5.0.000
			return new OperatorVersion("5.0.000");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + buildNumber;
		result = prime * result + major;
		result = prime * result + minor;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OperatorVersion other = (OperatorVersion) obj;
		if (buildNumber != other.buildNumber)
			return false;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		return true;
	}
	
	public boolean isAtLeast(int major, int minor, int buildNumber) {
		return this.compareTo(new OperatorVersion(major, minor, buildNumber)) >= 0;
	}
	
	public boolean isAtMost(int major, int minor, int buildNumber) {
		return this.compareTo(new OperatorVersion(major, minor, buildNumber)) <= 0;
	}
}
