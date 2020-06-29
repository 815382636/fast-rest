package hello.fast;

import java.util.List;
import java.util.Map;

import hello.fast.obj.Bucket;

public class SampleThread extends Thread{

//	url, username, password,
//	database, timeseries, columns, timecolumn, latestTime, endtime,
//	conditions, query, format, ip, port, amount, dbtype, timeLimit, valueLimit
	private String url;
    private String username;
    private String password;
    private String database;
    private String timeseries;
    private String columns;
    private String timecolumn;
    private String starttime;
    private String endtime;
    private String conditions;
    private String query;
    private String format;
    private String sample;
    private String ip;
    private String port;
    private Integer amount;
    private String dbtype;
    private Double timeLimit;
    private Double valueLimit;
	public SampleThread(String url, String username, String password, String database, String timeseries,
			String columns, String timecolumn, String starttime, String endtime, String conditions, String query,
			String format, String sample, String ip, String port, Integer amount, String dbtype, Double timeLimit,
			Double valueLimit) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.database = database;
		this.timeseries = timeseries;
		this.columns = columns;
		this.timecolumn = timecolumn;
		this.starttime = starttime;
		this.endtime = endtime;
		this.conditions = conditions;
		this.query = query;
		this.format = format;
		this.sample = sample;
		this.ip = ip;
		this.port = port;
		this.amount = amount;
		this.dbtype = dbtype;
		this.timeLimit = timeLimit;
		this.valueLimit = valueLimit;
	}
	
	@Override
    public void run() {
		String latestTime = starttime;

//        while (true){
//            // 先根据采样算子分桶，"simpleXXX"为等间隔分桶，否则为自适应分桶
//            List<Bucket> buckets =
//                sample.contains("simple") ?
//                BucketsController._intervals(url, username, password, database, timeseries, columns, timecolumn, latestTime, endtime, conditions, query, format, ip, port, amount, dbtype) :
//                BucketsController._buckets(url, username, password, database, timeseries, columns, timecolumn, latestTime, endtime, conditions, query, format, ip, port, amount, dbtype, timeLimit, valueLimit);
//
//            // 无新数据，数据已经消费完成
//            if(buckets == null) break;
//            // 最新数据点时间没有改变，数据已经消费完成
//            List<Map<String, Object>> lastBucket = buckets.get(buckets.size()-1).getDataPoints();
//            String newestTime = lastBucket.get(lastBucket.size()-1).get("time").toString().substring(0, 23);
//            if(latestTime.equals(newestTime)) break;
//            else latestTime = newestTime;
//
//            long t1 = System.currentTimeMillis();
//
//            res.addAll(samplingOperator.sample(buckets, timelabel, label));
//
//            System.out.println("SampleController: " + (System.currentTimeMillis() - t1) + "ms");
//
//            // 特殊判断：一次就可以获取所有数据
//            if(dataPointCount < batchLimit) break;
//        }
		
		
		
	}
	
    
    
    
}
