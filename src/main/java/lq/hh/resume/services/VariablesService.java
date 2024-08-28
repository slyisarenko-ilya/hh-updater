package lq.hh.resume.services;

public class VariablesService {

	public String getString(String name) {
		return getString(name, null);
	}

	public String getString(String name, String def){
		String candidate = null;
		if(candidate == null){
			candidate = System.getProperty(name);
		}
		if(candidate == null) {
			candidate = System.getenv(name);
		}
		if(candidate == null && def != null){
			candidate = def;
		}
		return candidate;
	}


	public Boolean getBoolean(String name, Boolean def){
		return Boolean.valueOf(getString(name, def.toString()));
	}

	public Integer getInt(String name, Integer def){
		return Integer.valueOf(getString(name, Integer.valueOf(def).toString()));
	}

}
