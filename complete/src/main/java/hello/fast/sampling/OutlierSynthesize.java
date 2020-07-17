package hello.fast.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import hello.fast.obj.Bucket;

public class OutlierSynthesize implements SamplingSynthesize {

	@Override
	public List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels) {
		SamplingOperator samplingOperator = new Outlier();
		List<Map<String, Object>> res = new ArrayList<>();

		for (int i = 0; i < labels.length; i++) {
			res.addAll(samplingOperator.sample(buckets, timelabel, labels[i]));
		}

		return res;
	}
}
