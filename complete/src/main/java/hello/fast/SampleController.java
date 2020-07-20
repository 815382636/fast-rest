package hello.fast;

import hello.fast.obj.Bucket;
import hello.fast.sampling.*;
import hello.fast.util.UtilMethod;

import org.apache.iotdb.rpc.IoTDBRPCException;
import org.apache.iotdb.session.IoTDBSessionException;
import org.apache.thrift.TException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.*;

/**
 * 采样控制器，将分桶后的数据进行采样
 */
@RestController
public class SampleController {
	@RequestMapping("/sample")
	public List<Map<String, Object>> dataPoints(
			@RequestParam(value = "url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
			@RequestParam(value = "username", defaultValue = "root") String username,
			@RequestParam(value = "password", defaultValue = "root") String password,
			@RequestParam(value = "database") String database, @RequestParam(value = "timeseries") String timeseries,
			@RequestParam(value = "columns") List<String> columns,
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
			@RequestParam(value = "sample", defaultValue = "m4") String sample,
			@RequestParam(value = "returnType", defaultValue = "Integration") String returnType,
			@RequestParam(value = "bucketMethod", defaultValue = "adaption") String bucketMethod,
			@RequestParam(value = "correlation", defaultValue = "True") Boolean correlation,
			@RequestParam(value = "valueLimit", required = false) Map<String, Double> valueLimit) throws Exception {

		url = url.replace("\"", "");
		username = username.replace("\"", "");
		password = password.replace("\"", "");
		database = database.replace("\"", "");
		timeseries = timeseries.replace("\"", "");
		timecolumn = timecolumn.replace("\"", "");
        returnType =returnType.replace("\"", "");
		starttime = starttime == null ? "1971-01-01 00:00:00" : starttime.replace("\"", "");
		endtime = endtime == null ? "2099-01-01 00:00:00" : endtime.replace("\"", "");
		conditions = conditions == null ? "" : conditions.replace("\"", "");
		format = format.replace("\"", "");
		dbtype = dbtype.replace("\"", "");
		sample = sample.replace("\"", "");
		bucketMethod =bucketMethod.replace("\"", "");
		ip = ip == null ? null : ip.replace("\"", "");
		port = port == null ? null : port.replace("\"", "");
		query = query == null ? null : query.replace("\"", "");

		List<Map<String, Object>> dataPoints =_samplePoints(url, username, password, database, timeseries, columns, timecolumn, starttime, endtime,
				conditions, query, format, sample, ip, port, amount, dbtype, valueLimit,bucketMethod,correlation);
		if (returnType.contains("Integration")) {
        	return UtilMethod.change_type(dataPoints, columns);
		}else {
			return dataPoints;
		}
	}

	static List<Map<String, Object>> _samplePoints(String url, String username, String password, String database,
			String timeseries, List<String> columns, String timecolumn, String starttime, String endtime,
			String conditions, String query, String format, String sample, String ip, String port, Integer amount,
			String dbtype, Map<String, Double> valueLimit,String bucketMethod,Boolean correlation)
			throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {

		List<Map<String, Object>> res = new ArrayList<>();
		if (!correlation) {
			System.out.println("方法一");
			SamplingOperator samplingOperator;
			// 桶内算子
			if (sample.contains("aggregation"))
				samplingOperator = new Aggregation();
			else if (sample.contains("random"))
				samplingOperator = new Sample();
			else if (sample.contains("outlier"))
				samplingOperator = new Outlier();
			else
				samplingOperator = new M4();

			String typeTime = starttime;

//          对多个column进行处理
			String[] co = new String[columns.size()];
			columns.toArray(co);
			String columnsStr ="";
	        for (int k = 0; k < columns.size(); k++) {
	        	columnsStr +=columns.get(k);
				if(k !=columns.size()-1) {
					columnsStr +=",";
				}
			}
	        
			for (int i = 0; i < co.length; i++) {
				long dataPointCount = DataController._dataPointsCount(url, username, password, database, timeseries,
						co[i], timecolumn, starttime, endtime, conditions, query, format, ip, port, dbtype);
				long freememery = Runtime.getRuntime().freeMemory();
//                每一批次最多查询的数据    batchlimit:21088
				long batchLimit = freememery / 10000L;
				if (!conditions.contains("limit"))
					conditions = conditions + " limit " + batchLimit;
				if (dbtype.equals("postgresql") || dbtype.equals("timescaledb"))
					conditions = " order by time " + conditions;
//                每一批次所要拿到的采样的数目
				amount = (int) (amount * batchLimit / dataPointCount);
				if (amount ==0) {
					amount =1;
				}
//            	System.out.println("batchlimit:"+batchLimit);

				String iotdbLabel = database + "." + timeseries + "." + co[i];
				String label = dbtype.equals("iotdb") ? iotdbLabel : co[i];
				String timelabel = "time";
				String latestTime = typeTime;
				
				while (true) {
					// 先根据采样算子分桶，"simpleXXX"为等间隔分桶，否则为自适应分桶
					List<Bucket> buckets = bucketMethod.contains("uniform")
							? BucketsController.new_intervals(co[i], url, username, password, database, timeseries,
									columnsStr, timecolumn, latestTime, endtime, conditions, query, format, ip, port,
									amount, dbtype)
							: BucketsController.new_buckets(co[i], url, username, password, database, timeseries,
									columnsStr, timecolumn, latestTime, endtime, conditions, query, format, ip, port,
									amount, dbtype, valueLimit==null?null:valueLimit.get("time"), valueLimit==null?null:valueLimit.get(co[i]));

					// 无新数据，数据已经消费完成
					if (buckets == null)
						break;
					// 最新数据点时间没有改变，数据已经消费完成
					List<Map<String, Object>> lastBucket = buckets.get(buckets.size() - 1).getDataPoints();
					
					String newestTime;
					if (lastBucket.get(lastBucket.size() - 1).get("time").toString().length()>23) {
						newestTime =lastBucket.get(lastBucket.size() - 1).get("time").toString().substring(0, 23);
					}else {
						newestTime =lastBucket.get(lastBucket.size() - 1).get("time").toString();
					}
					
					if (latestTime.equals(newestTime))
						break;
					else
						latestTime = newestTime;

					long t1 = System.currentTimeMillis();

					res.addAll(samplingOperator.sample(buckets, timelabel, label));

					System.out.println("SampleController: " + (System.currentTimeMillis() - t1) + "ms");

					// 特殊判断：一次就可以获取所有数据
					if (dataPointCount < batchLimit)
						break;
				}
			}
		} else {
			System.out.println("方法二");
			String timelabel = "time";
			String[] co = new String[columns.size()];
			columns.toArray(co);
			String[] iotdbCo = new String[co.length];
			for (int j = 0; j < iotdbCo.length; j++) {
				iotdbCo[j] = database + "." + timeseries + "." + co[j];
			}
			String[] labels = dbtype.equals("iotdb") ? iotdbCo : co;
			String columnsStr ="";
	        for (int k = 0; k < columns.size(); k++) {
	        	columnsStr +=columns.get(k);
				if(k !=columns.size()-1) {
					columnsStr +=",";
				}
			}
			
			SamplingSynthesize samplingsynthesize;
			// 桶内算子
			if (sample.contains("aggregation"))
				samplingsynthesize = new AggregationSynthesize();
			else if (sample.contains("random"))
				samplingsynthesize = new SampleSynthesize();
			else if (sample.contains("outlier"))
				samplingsynthesize = new OutlierSynthesize();
			else
				samplingsynthesize = new M4Synthesize();
			long dataPointCount = DataController._dataPointsCount(url, username, password, database, timeseries,
					columns.get(0), timecolumn, starttime, endtime, conditions, query, format, ip, port, dbtype);

			long freememery = Runtime.getRuntime().freeMemory();
			long batchLimit = freememery / 10000L;
			if (!conditions.contains("limit"))
				conditions = conditions + " limit " + batchLimit;
			if (dbtype.equals("postgresql") || dbtype.equals("timescaledb"))
				conditions = " order by time " + conditions;
			amount = (int) (amount * batchLimit / dataPointCount);
			if (amount ==0) {
				amount =1;
			}
//			System.out.println("总长度："+dataPointCount);
			String latestTime = starttime;
//			System.out.println("开始时间"+latestTime);
			while (true) {

				List<Bucket> buckets = BucketsController.correlation_buckets(url, username, password, database, timeseries, columnsStr, timecolumn, latestTime, endtime, conditions, query, format, ip, port, amount, dbtype, valueLimit, labels);
				// 无新数据，数据已经消费完成
				if (buckets == null)
					break;
				// 最新数据点时间没有改变，数据已经消费完成
				List<Map<String, Object>> lastBucket = buckets.get(buckets.size() - 1).getDataPoints();

				String newestTime;
				if (lastBucket.get(lastBucket.size() - 1).get("time").toString().length() > 23) {
					newestTime = lastBucket.get(lastBucket.size() - 1).get("time").toString().substring(0, 23);
				} else {
					newestTime = lastBucket.get(lastBucket.size() - 1).get("time").toString();
				}
//				System.out.println("中间时间+"+newestTime);
				if (latestTime.equals(newestTime)) 
					break;
				else
					latestTime = newestTime;

				long t1 = System.currentTimeMillis();

				res.addAll(samplingsynthesize.sample(buckets, timelabel, labels));

				System.out.println("SampleController: " + (System.currentTimeMillis() - t1) + "ms");

				// 特殊判断：一次就可以获取所有数据
				if (dataPointCount < batchLimit) {
					break;}
			}
		}

		System.out.println("----------------");
		System.out.println(res.size());
		System.out.println("---------------------");

		if (format.equals("map"))
			return res;
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> map : res) {
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String mapKey = entry.getKey();
				if (mapKey.equals("time"))
					continue;
				Map<String, Object> m = new HashMap<>();
				m.put("time", map.get("time"));
				m.put("label", mapKey);
				m.put("value", entry.getValue());
				result.add(m);
			}
		}
		return result;
	}

	static List<Map<String, Object>> _samplePoints(List<Bucket> buckets, String timelabel, String label,
			String sample) {
		SamplingOperator samplingOperator;

		if (sample.contains("aggregation"))
			samplingOperator = new Aggregation();
		else if (sample.contains("random"))
			samplingOperator = new Sample();
		else if (sample.contains("outlier"))
			samplingOperator = new Outlier();
		else
			samplingOperator = new M4();

		return samplingOperator.sample(buckets, timelabel, label);
	}
	
	static List<Map<String, Object>> correlation_samplePoints(List<Bucket> buckets, String timelabel, String[] labels,
			String sample) {
		SamplingSynthesize samplingSynthesize;

		if (sample.contains("aggregation"))
			samplingSynthesize = new AggregationSynthesize();
		else if (sample.contains("random"))
			samplingSynthesize = new SampleSynthesize();
		else if (sample.contains("outlier"))
			samplingSynthesize = new OutlierSynthesize();
		else
			samplingSynthesize = new M4Synthesize();

		return samplingSynthesize.sample(buckets, timelabel, labels);
	}
}
