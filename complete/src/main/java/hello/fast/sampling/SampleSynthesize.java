package hello.fast.sampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import hello.fast.obj.Bucket;

public class SampleSynthesize implements SamplingSynthesize {
	@Override
	public List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels) {
		List<Map<String, Object>> res = new ArrayList<>();
        System.out.println("labels.length"+labels.length);

        for(Bucket bucket : buckets){
            List<Map<String, Object>> datapoints = bucket.getDataPoints();
            if(datapoints.size() <= 2*(labels.length+1)){
                res.addAll(datapoints);
                continue;
            }
            Collections.shuffle(datapoints);
            res.addAll(datapoints.subList(0, 2*(labels.length+1)));
        }

        return res;
	
	
	}
}
