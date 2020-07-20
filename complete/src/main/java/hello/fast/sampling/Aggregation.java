package hello.fast.sampling;

import hello.fast.obj.Bucket;

import java.util.*;

/**
* 聚合采样算子，计算每个桶内的平均值返回
*/
public class Aggregation implements SamplingOperator {
    @Override
    public List<Map<String, Object>> sample(List<Bucket> buckets, String timeLabel, String valueLabel) {
        List<Map<String, Object>> res = new ArrayList<>();

        for(Bucket bucket : buckets){
            List<Map<String, Object>> datapoints = bucket.getDataPoints();
            if(datapoints.size() == 0) continue;

            double valueSum = 0;
            for(int i = 0; i < datapoints.size(); i++){
                Map<String, Object> candi = datapoints.get(i);
                Object value = candi.get(valueLabel);
                if(value instanceof Double) valueSum += (double)value;
                else if(value instanceof Integer) valueSum += (double)((Integer)value);
                else if(value instanceof Long) valueSum += (double)((Long)value);
                else valueSum += 0;
            }

            Map<String, Object> obj = new HashMap<>();
            for (String map_key : datapoints.get(datapoints.size()-1).keySet()) {
				if (map_key.equals(valueLabel)) {
		            obj.put(valueLabel, valueSum / datapoints.size());
				}else {
					obj.put(map_key, datapoints.get(datapoints.size()-1).get(map_key));
				}
			}
            res.add(obj);
        }

        return res;
    }
}
