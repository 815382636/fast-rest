package hello.fast.sampling;

import hello.fast.obj.Bucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* M4采样算子，提取每个桶内的四个极值点
*/
public class M4Synthesize implements SamplingSynthesize {
    @Override
    public List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels) {
        List<Map<String, Object>> res = new ArrayList<>();

        for(Bucket bucket : buckets){
            List<Map<String, Object>> datapoints = bucket.getDataPoints();
            if(datapoints.size() <= 2*labels.length){
                res.addAll(datapoints);
                continue;
            }

            res.add(datapoints.get(0));
            Map<String, Object> maxi;
            Map<String, Object> mini;
            for (int j = 0; j < labels.length; j++) {
            	maxi = datapoints.get(0);
            	mini = datapoints.get(0);
            	for(int i = 1; i < datapoints.size()-1; i++){
                    Map<String, Object> candi = datapoints.get(i);
                    Object value = candi.get(labels[j]);
                    if(value instanceof Double) maxi = (Double) value >= (Double)maxi.get(labels[j]) ? candi : maxi;
                    else if(value instanceof Integer) maxi = (Integer) value >= (Integer)maxi.get(labels[j]) ? candi : maxi;
                    else if(value instanceof Long) maxi = (Long) value >= (Long)maxi.get(labels[j]) ? candi : maxi;
                    else maxi = (Double) value >= (Double)maxi.get(labels[j]) ? candi : maxi;

                    if(value instanceof Double) mini = (Double) value <= (Double)mini.get(labels[j]) ? candi : mini;
                    else if(value instanceof Integer) mini = (Integer) value <= (Integer)mini.get(labels[j]) ? candi : mini;
                    else if(value instanceof Long) mini = (Long) value <= (Long)mini.get(labels[j]) ? candi : mini;
                    else mini = (Double) value <= (Double)mini.get(labels[j]) ? candi : mini;
                }
                res.add(maxi);
                res.add(mini);
			}
            res.add(datapoints.get(datapoints.size()-1));
        }

        return res;
    }
}
