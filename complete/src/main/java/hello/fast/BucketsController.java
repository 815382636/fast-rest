package hello.fast;

import hello.fast.util.OutlierDetection;
import org.apache.iotdb.rpc.IoTDBRPCException;
import org.apache.iotdb.session.IoTDBSessionException;
import org.apache.thrift.TException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import hello.fast.obj.Bucket;

/**
 * 分桶控制器，将原始数据按照权重分桶
 */
@RestController
public class BucketsController {
	@RequestMapping("/bucket")
	public List<Bucket> buckets(@RequestParam(value = "url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
			@RequestParam(value = "username", defaultValue = "root") String username,
			@RequestParam(value = "password", defaultValue = "root") String password,
			@RequestParam(value = "database") String database, @RequestParam(value = "timeseries") String timeseries,
			@RequestParam(value = "columns") String columns,
			@RequestParam(value = "timeColumn", defaultValue = "time") String timecolumn,
			@RequestParam(value = "startTime", required = false) String starttime,
			@RequestParam(value = "endTime", required = false) String endtime,
			@RequestParam(value = "conditions", required = false) String conditions,
			@RequestParam(value = "query", required = false) String query,
			@RequestParam(value = "format", defaultValue = "map") String format,
			@RequestParam(value = "ip", required = false) String ip,
			@RequestParam(value = "port", required = false) String port,
			@RequestParam(value = "amount", defaultValue = "2000") Integer amount,
			@RequestParam(value = "dbtype", defaultValue = "iotdb") String dbtype,
			@RequestParam(value = "timeLimit", required = false) Double timeLimit,
			@RequestParam(value = "valueLimit", required = false) Double valueLimit)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {

		url = url.replace("\"", "");
		username = username.replace("\"", "");
		password = password.replace("\"", "");
		database = database.replace("\"", "");
		timeseries = timeseries.replace("\"", "");
		columns = columns.replace("\"", "");
		timecolumn = timecolumn.replace("\"", "");
		starttime = starttime == null ? null : starttime.replace("\"", "");
		endtime = endtime == null ? null : endtime.replace("\"", "");
		conditions = conditions == null ? null : conditions.replace("\"", "");
		format = format.replace("\"", "");
		dbtype = dbtype.replace("\"", "");
		ip = ip == null ? null : ip.replace("\"", "");
		port = port == null ? null : port.replace("\"", "");
		query = query == null ? null : query.replace("\"", "");

		return _buckets(url, username, password, database, timeseries, columns, timecolumn, starttime, endtime,
				conditions, query, format, ip, port, amount, dbtype, timeLimit, valueLimit);
	}

	static List<Bucket> _buckets(String url, String username, String password, String database, String timeseries,
			String columns, String timecolumn, String starttime, String endtime, String conditions, String query,
			String format, String ip, String port, Integer amount, String dbtype, Double timeLimit, Double valueLimit)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
//    	拿到所有数据
		List<Map<String, Object>> dataPoints = DataController._dataPoints(url, username, password, database, timeseries,
				columns, timecolumn, starttime, endtime, conditions, query, "map", ip, port, dbtype);

		if (dataPoints.size() < 2)
			return null;

		String iotdbLabel = database + "." + timeseries + "." + columns;
		String label = dbtype.equals("iotdb") ? iotdbLabel : columns;
		String timelabel = "time";

		return _buckets(dataPoints, timelabel, label, amount, timeLimit, valueLimit);
	}

	static List<Bucket> correlation_buckets(String url, String username, String password, String database,
			String timeseries, String columnsStr, String timecolumn, String starttime, String endtime,
			String conditions, String query, String format, String ip, String port, Integer amount, String dbtype,
			Map<String, Double> valueLimit, String[] labels)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
//    	拿到所有数据
		List<Map<String, Object>> dataPoints = DataController._dataPoints(url, username, password, database, timeseries,
				columnsStr, timecolumn, starttime, endtime, conditions, query, "map", ip, port, dbtype);

		if (dataPoints.size() < 2)
			return null;

		String timelabel = "time";

		return corralation_buckets(dataPoints, timelabel, labels, amount, valueLimit);
	}

	static List<Bucket> new_buckets(String column, String url, String username, String password, String database,
			String timeseries, String columns, String timecolumn, String starttime, String endtime, String conditions,
			String query, String format, String ip, String port, Integer amount, String dbtype, Double timeLimit,
			Double valueLimit) throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
//    	拿到所有数据
		List<Map<String, Object>> dataPoints = DataController._dataPoints(url, username, password, database, timeseries,
				columns, timecolumn, starttime, endtime, conditions, query, "map", ip, port, dbtype);

		if (dataPoints.size() < 2)
			return null;

		String iotdbLabel = database + "." + timeseries + "." + column;
		String label = dbtype.equals("iotdb") ? iotdbLabel : column;
		String timelabel = "time";

		return _buckets(dataPoints, timelabel, label, amount, timeLimit, valueLimit);
	}

	static List<Bucket> _intervals(String url, String username, String password, String database, String timeseries,
			String columns, String timecolumn, String starttime, String endtime, String conditions, String query,
			String format, String ip, String port, Integer amount, String dbtype)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
		List<Map<String, Object>> linkedDataPoints = DataController._dataPoints(url, username, password, database,
				timeseries, timecolumn, columns, starttime, endtime, conditions, query, "map", ip, port, dbtype);
		if (linkedDataPoints.size() < 2)
			return null;

		List<Map<String, Object>> dataPoints = new ArrayList<>(linkedDataPoints);

		Long firstTimestamp = (long) dataPoints.get(0).get("timestamp");
		Long lastTimestamp = (long) dataPoints.get(dataPoints.size() - 1).get("timestamp");
		Long timestampRange = lastTimestamp - firstTimestamp;
		Long timeinteval = timestampRange / amount * 4;

		String iotdbLabel = database + "." + timeseries + "." + columns;
		String label = dbtype.equals("iotdb") ? iotdbLabel : columns;
		String timelabel = "time";

		for (Map<String, Object> dataPoint : dataPoints)
			dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));
		List<Bucket> buckets = new ArrayList<>();

		int p = 0, q = 0;
		int n = dataPoints.size();
		lastTimestamp = firstTimestamp + timeinteval;
		while (p < n) {
			Long dataTimestamp = (long) dataPoints.get(p).get("timestamp");
			if (dataTimestamp > lastTimestamp) {
				buckets.add(new Bucket(dataPoints.subList(q, p)));
				q = p;
				lastTimestamp += timeinteval;
			}
			p++;
		}
		buckets.add(new Bucket(dataPoints.subList(q, p)));

		return buckets;
	}

	static List<Bucket> new_intervals(String column, String url, String username, String password, String database,
			String timeseries, String columns, String timecolumn, String starttime, String endtime, String conditions,
			String query, String format, String ip, String port, Integer amount, String dbtype)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
		List<Map<String, Object>> linkedDataPoints = DataController._dataPoints(url, username, password, database,
				timeseries, timecolumn, columns, starttime, endtime, conditions, query, "map", ip, port, dbtype);
		if (linkedDataPoints.size() < 2)
			return null;

		List<Map<String, Object>> dataPoints = new ArrayList<>(linkedDataPoints);

		Long firstTimestamp = (long) dataPoints.get(0).get("timestamp");
		Long lastTimestamp = (long) dataPoints.get(dataPoints.size() - 1).get("timestamp");
		Long timestampRange = lastTimestamp - firstTimestamp;
		Long timeinteval = timestampRange / amount * 4;

		String iotdbLabel = database + "." + timeseries + "." + column;
		String label = dbtype.equals("iotdb") ? iotdbLabel : column;
		String timelabel = "time";

		for (Map<String, Object> dataPoint : dataPoints)
			dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));
		List<Bucket> buckets = new ArrayList<>();

		int p = 0, q = 0;
		int n = dataPoints.size();
		lastTimestamp = firstTimestamp + timeinteval;
		while (p < n) {
			Long dataTimestamp = (long) dataPoints.get(p).get("timestamp");
			if (dataTimestamp > lastTimestamp) {
				buckets.add(new Bucket(dataPoints.subList(q, p)));
				q = p;
				lastTimestamp += timeinteval;
			}
			p++;
		}
		buckets.add(new Bucket(dataPoints.subList(q, p)));

		return buckets;
	}

	static long _bucketSum(List<Map<String, Object>> dataPoints, String timelabel, String label, Integer amount,
			Double timeLimit, Double valueLimit) {
		for (Map<String, Object> dataPoint : dataPoints)
			dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));
		WeightController._weights(dataPoints, timelabel, label, amount, timeLimit, valueLimit);
		List<Double> weights = new ArrayList<>();
		for (Map<String, Object> dataPoint : dataPoints)
			weights.add((Double) dataPoint.get("weight"));

		// 二分查找
		int n = amount == null ? 1000 : amount / 4;
		long lo = 0, hi = weights.size() * 100;
		while (lo < hi) {
			long mid = lo + (hi - lo >> 1);
			int count = 0;
			double sum = 0;
			for (double weight : weights) {
				if (weight < 0) {
					if (sum > 0)
						count++;
					sum = 0;
				} else if (sum + weight > mid) {
					sum = weight;
					if (++count > n)
						break;
				} else
					sum += weight;
			}
			count++;
			if (count >= n)
				lo = mid + 1;
			else
				hi = mid;
		}
		long bucketSum = lo;
		System.out.println("bucketSum" + bucketSum);
		return bucketSum;
	}

	static long corralation_bucketSum(List<Map<String, Object>> dataPoints, String[] labels, Integer amount) {
		List<Double> weights = new ArrayList<>();
		for (Map<String, Object> dataPoint : dataPoints)
			weights.add((Double) dataPoint.get("weight"));

		// 二分查找
		int n = amount == null ? 1000 : amount / (labels.length + 1) * 2;
		long lo = 0, hi = weights.size() * 100;
		while (lo < hi) {
			long mid = lo + (hi - lo >> 1);
			int count = 0;
			double sum = 0;
			for (double weight : weights) {
				if (weight < 0) {
					if (sum > 0)
						count++;
					sum = 0;
				} else if (sum + weight > mid) {
					sum = weight;
					if (++count > n)
						break;
				} else
					sum += weight;
			}
			count++;
			if (count >= n)
				lo = mid + 1;
			else
				hi = mid;
		}
		long bucketSum = lo;
		System.out.println("bucketSum" + bucketSum);
		return bucketSum;
	}

	static List<Bucket> _buckets(List<Map<String, Object>> dataPoints, String timelabel, String label, Integer amount,
			Double timeLimit, Double valueLimit) {
		List<Bucket> res = new ArrayList<>();

		for (Map<String, Object> dataPoint : dataPoints)
			dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));
		WeightController._weights(dataPoints, timelabel, label, amount, timeLimit, valueLimit);

		long t1 = System.currentTimeMillis();

		List<Double> weights = new ArrayList<>();
		for (Map<String, Object> dataPoint : dataPoints)
			weights.add((Double) dataPoint.get("weight"));

		long bucketSum = _bucketSum(dataPoints, timelabel, label, amount, timeLimit, valueLimit);

		double sum = 0;
		int lastIndex = 0;

		for (int i = 0; i < weights.size(); i++) {
			double weight = weights.get(i);
			if (weight < 0) {
				if (sum > 0) {
					res.add(new Bucket(dataPoints.subList(lastIndex, i)));
					res.add(new Bucket(dataPoints.subList(i, i + 1)));
					lastIndex = i + 1;
					sum = 0;
				} else {
					res.add(new Bucket(dataPoints.subList(i, i + 1)));
					lastIndex = i + 1;
					sum = 0;
				}
			}
			if (sum + weight > bucketSum) {
				res.add(new Bucket(dataPoints.subList(lastIndex, i)));
				lastIndex = i;
				sum = weight;
			} else
				sum += weight;
		}
		res.add(new Bucket(dataPoints.subList(lastIndex, dataPoints.size())));

		for (int i = 0; i < res.size(); i++) {
			for (Map<String, Object> p : res.get(i).getDataPoints()) {
				if ((Double) p.get("weight") < 0) {
					p.put("bucket", -1);
				} else
					p.put("bucket", i);
			}
		}

		System.out.println("BucketController: " + (System.currentTimeMillis() - t1) + "ms");

		return res;
	}

	static List<Bucket> corralation_buckets(List<Map<String, Object>> dataPoints, String timelabel, String[] labels,
			Integer amount, Map<String, Double> valueLimit) {
		List<Bucket> res = new ArrayList<>();

		for (Map<String, Object> dataPoint : dataPoints)
			dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));
		WeightController.corralation_weights(dataPoints, timelabel, labels, amount, valueLimit);

		long t1 = System.currentTimeMillis();

		List<Double> weights = new ArrayList<>();
		for (Map<String, Object> dataPoint : dataPoints)
			weights.add((Double) dataPoint.get("weight"));

		long bucketSum = corralation_bucketSum(dataPoints, labels, amount);

		double sum = 0;
		int lastIndex = 0;

		for (int i = 0; i < weights.size(); i++) {
			double weight = weights.get(i);
			if (weight < 0) {
				if (sum > 0) {
					res.add(new Bucket(dataPoints.subList(lastIndex, i)));
					res.add(new Bucket(dataPoints.subList(i, i + 1)));
					lastIndex = i + 1;
					sum = 0;
				} else {
					res.add(new Bucket(dataPoints.subList(i, i + 1)));
					lastIndex = i + 1;
					sum = 0;
				}
			}
			if (sum + weight > bucketSum) {
				res.add(new Bucket(dataPoints.subList(lastIndex, i)));
				lastIndex = i;
				sum = weight;
			} else
				sum += weight;
		}
		res.add(new Bucket(dataPoints.subList(lastIndex, dataPoints.size())));

		for (int i = 0; i < res.size(); i++) {
			for (Map<String, Object> p : res.get(i).getDataPoints()) {
				if ((Double) p.get("weight") < 0) {
					p.put("bucket", -1);
				} else
					p.put("bucket", i);
			}
		}

		System.out.println("BucketController: " + (System.currentTimeMillis() - t1) + "ms");

		return res;
	}
}
