package com.rapidminer.operator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;

/** Instances of this class can be used to annotate {@link IOObject}s, {@link Attribute}s, etc. 
 * 
 * @author Simon Fischer
 *
 */
public class Annotations implements Serializable, Map<String,String> {

	private static final long serialVersionUID = 1L;

	// for IOObjects

	/** Source, e.g. URI, or SQL query of data. */
	public static final String KEY_SOURCE    = "annotation.source";
	
	/** User defined comment. */
	public static final String KEY_COMMENT   = "annotation.comment";

	// for Attribtues
	
	/** Physical unit of attributes. */
	public static final String KEY_UNIT      = "annotation.unit";

	/** Colors for attribute values. */
	public static final String KEY_COLOR_MAP = "annotation.colors";

	// Dublin Core
	
	public static final String KEY_DC_AUTHOR               = "dc.author";
	public static final String KEY_DC_TITLE                = "dc.title";
	public static final String KEY_DC_SUBJECT              = "dc.subject";
	public static final String KEY_DC_COVERAGE             = "dc.coverage";
	public static final String KEY_DC_DESCRIPTION          = "dc.description";
	public static final String KEY_DC_CREATOR              = "dc.creator";
	public static final String KEY_DC_PUBLISHER            = "dc.publisher";
	public static final String KEY_DC_CONTRIBUTOR          = "dc.contributor";
	public static final String KEY_DC_RIGHTS_HOLDER        = "dc.rightsHolder";
	public static final String KEY_DC_RIGHTS               = "dc.rights";
	public static final String KEY_DC_PROVENANCE           = "dc.provenance";
	public static final String KEY_DC_SOURCE               = "dc.source";
	public static final String KEY_DC_RELATION             = "dc.relation";
	public static final String KEY_DC_AUDIENCE             = "dc.audience";
	public static final String KEY_DC_INSTRUCTIONAL_METHOD = "dc.description";
	
	public static final String[] KEYS_RAPIDMINER_IOOBJECT = {
		KEY_SOURCE,
		KEY_COMMENT
	};
	
	public static final String[] KEYS_DUBLIN_CORE = {
		KEY_DC_AUTHOR,
		KEY_DC_TITLE,
		KEY_DC_SUBJECT,
		KEY_DC_COVERAGE,
		KEY_DC_DESCRIPTION,
		KEY_DC_CREATOR,
		KEY_DC_PUBLISHER,
		KEY_DC_CONTRIBUTOR,
		KEY_DC_RIGHTS_HOLDER,
		KEY_DC_RIGHTS,
		KEY_DC_PROVENANCE,
		KEY_DC_SOURCE,
		KEY_DC_RELATION,
		KEY_DC_AUDIENCE,
		KEY_DC_INSTRUCTIONAL_METHOD
	};
	
	public static final String[] ALL_KEYS_IOOBJECT = {
		KEY_SOURCE,
		KEY_COMMENT,
		
		KEY_DC_AUTHOR,
		KEY_DC_TITLE,
		KEY_DC_SUBJECT,
		KEY_DC_COVERAGE,
		KEY_DC_DESCRIPTION,
		KEY_DC_CREATOR,
		KEY_DC_PUBLISHER,
		KEY_DC_CONTRIBUTOR,
		KEY_DC_RIGHTS_HOLDER,
		KEY_DC_RIGHTS,
		KEY_DC_PROVENANCE,
		KEY_DC_SOURCE,
		KEY_DC_RELATION,
		KEY_DC_AUDIENCE,
		KEY_DC_INSTRUCTIONAL_METHOD
	};
	
	private LinkedHashMap<String,String> keyValueMap = new LinkedHashMap<String,String>();
	
	public Annotations() {		
	}
	
	/** Clone constructor.
	 */
	public Annotations(Annotations annotations) {
		this.keyValueMap = new LinkedHashMap<String, String>(annotations.keyValueMap);
	}


	public void setAnnotation(String key, String value) {
		keyValueMap.put(key, value);
	}
	
	public String getAnnotation(String key) {
		return keyValueMap.get(key);
	}

	public List<String> getKeys() {
		return new ArrayList<String>(keyValueMap.keySet());
	}

	public void removeAnnotation(String key) {
		keyValueMap.remove(key);		
	}

	public int size() {		
		return keyValueMap.size();
	}

	@Override
	public void clear() {
		keyValueMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {	
		return keyValueMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return keyValueMap.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return keyValueMap.entrySet();
	}

	@Override
	public String get(Object key) {
		if (key instanceof String) {
			return getAnnotation((String)key);
		} else {
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		return keyValueMap.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return keyValueMap.keySet();
	}

	@Override
	public String put(String key, String value) {
		setAnnotation(key, value);
		return value;
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> m) {
		keyValueMap.putAll(m);		
	}

	@Override
	public String remove(Object key) {
		return keyValueMap.remove(key);
	}

	@Override
	public Collection<String> values() {
		return keyValueMap.values();
	}
	
	@Override
	public String toString() {
		return keyValueMap.toString();
	}
}
