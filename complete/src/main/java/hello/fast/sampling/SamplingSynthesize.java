package hello.fast.sampling;

import java.util.List;
import java.util.Map;

import hello.fast.obj.Bucket;

public interface SamplingSynthesize {
	   List<Map<String, Object>> sample(List<Bucket> buckets, String timelabel, String[] labels);
}
