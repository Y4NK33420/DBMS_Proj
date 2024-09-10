package edu.upenn.cis.db.graphtrans.experiment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ExpConfig {
    private static Map<String, Object> conf = new LinkedHashMap<String, Object>();
    public static Map<String, ExpDataset> datasets = new LinkedHashMap<String, ExpDataset>();
    
    public static void setConf(String key, Object value) {
    	ExpConfig.conf.put(key, value);
    }
    
    public static Object get(String key) {
    	return ExpConfig.conf.get(key);
    }
    
    public static Map<String, ExpDataset> getDataSets() {
    	return datasets;
    }    
    
	public static String getString() {
		StringBuilder str = new StringBuilder("Runner: {\n");
		
		for (Entry<String, Object> key : ExpConfig.conf.entrySet()) {
			str.append(key.getKey()).append(": ").append(key.getValue()).append(",\n");
		}
		str.append("dataset: ").append(datasets);
		
		return str.toString();
	}
    
}
