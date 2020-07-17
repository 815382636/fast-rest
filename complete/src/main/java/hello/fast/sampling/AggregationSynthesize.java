package hello.fast.sampling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hello.fast.obj.Bucket;

public class AggregationSynthesize implements SamplingSynthesize {
	@Override
	public List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels) {
		List<Map<String, Object>> res = new ArrayList<>();

		for (Bucket bucket : buckets) {
			List<Map<String, Object>> datapoints = bucket.getDataPoints();
			if (datapoints.size() == 0)
				continue;
			
			Map<String, Object> obj = new HashMap<>();
			for (int j = 0; j < labels.length; j++) {
				double valueSum = 0;
				for (int i = 0; i < datapoints.size(); i++) {
					Map<String, Object> candi = datapoints.get(i);
					Object value = candi.get(labels[j]);
					if (value instanceof Double)
						valueSum += (double) value;
					else if (value instanceof Integer)
						valueSum += (double) ((Integer) value);
					else if (value instanceof Long)
						valueSum += (double) ((Long) value);
					else
						valueSum += 0;
				}

				obj.put(labels[j], valueSum / datapoints.size());
			}
			obj.put(timelabel, datapoints.get(datapoints.size() - 1).get(timelabel));
			res.add(obj);
			
		}

		return res;

	}

}
