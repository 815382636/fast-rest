package hello.fast.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hello.fast.obj.Bucket;

public class WeightSynthesize implements SamplingSynthesize{

	@Override
	public List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels) {
		List<Map<String, Object>> res = new ArrayList<>();
		System.out.println("桶数量："+buckets.size());
        for(Bucket bucket : buckets){
            List<Map<String, Object>> datapoints = bucket.getDataPoints();
            if(datapoints.size() <= (labels.length+1)*2){
                res.addAll(datapoints);
                continue;
            }
    		List<Map<String, Object>> res1 = new ArrayList<>();
    		for (int i = 0; i < (labels.length+1)*2; i++) {
        		res1.add(datapoints.get(i));
			}
            for(int i = (labels.length+1)*2; i < datapoints.size(); i++){
                Map<String, Object> candi = datapoints.get(i);
                max_value(res1,candi);

            }
            for (int i = 0; i < (labels.length+1)*2; i++) {
            	res.add(res1.get(i));
            }                  
        }
        
		return res;
	}

	private void max_value(List<Map<String, Object>> res1, Map<String, Object> candi) {
		Map<String, Object> min =candi;
		for (int i = 0; i < res1.size(); i++) {
			min =(double)min.get("weight")>(double)res1.get(i).get("weight")?res1.get(i):min;
		}
		if (min !=candi) {
			res1.remove(min);
			res1.add(candi);
		}
	}

}
