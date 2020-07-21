package hello.fast.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilMethod {
	public static List<Map<String, Object>> change_type(List<Map<String, Object>> dataPoints,List<String> columns) {
		List<Map<String, Object>> returnDataPoints = new ArrayList<>();
		
		for (Map<String, Object> data : dataPoints) {
			for (String key1 : data.keySet()) {
				for (String column : columns) {
					if (key1.toLowerCase().contains(column.toLowerCase())) {
						Map<String, Object> newMap =new HashMap<>();
						newMap.put("key", column);
						newMap.put("value", data.get(key1));
						if (data.containsKey("time")) {
							newMap.put("time", data.get("time"));
						}
						if (newMap.containsKey("timestamp")) {
							newMap.put("timestamp", data.get("timestamp"));
						}
						returnDataPoints.add(newMap);
						break;
					}
				}
			}
		}
			
		return returnDataPoints;
	}
}
