package hello.fast;

import com.alibaba.fastjson.JSONObject;
import hello.fast.util.ErrorController;
import hello.fast.obj.Bucket;
import hello.fast.source.PGConnection;
import org.springframework.util.DigestUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 层级采样线程
 */
public class LayerThread extends Thread {

	// 根据时间戳对数据进行排序 （从TimescaleDB查询的数据结果存在时间的乱序问题）
	private static Comparator<Map<String, Object>> sampleComparator = new Comparator<Map<String, Object>>() {
		@Override
		public int compare(Map<String, Object> sampleDataPoint1, Map<String, Object> sampleDataPoint2) {
			return sampleDataPoint1.get("time").toString().compareTo(sampleDataPoint2.get("time").toString());
		}
	};

	public volatile boolean exit = false;
	private static final String salt = "&%12345***&&%%$$#@1";
	private String url;
	private String username;
	private String password;
	private String database;
	private String timeseries;
	private List<String> columns;
	private String timecolumn;
	private String starttime;
	private String endtime;
	private String TYPE;
	private Integer ratio;
	private String subId;
	private Integer level;
	private String sample;
	private String dbtype;
	private Map<String, Double> valueLimit;
	private final long setuptime;
	private long throughput;
	private long batchlimit;
	private BlockingQueue<Map<String, Object>> dataQueue;
	private BlockingQueue<Map<String, Object>> sampleQueue;
	private Long bucketSum;
	private String columnsStr;

	public LayerThread(String url, String username, String password, String database, String timeseries,
			List<String> columns, String timecolumn, String starttime, String endtime, String TYPE, Integer ratio,
			String subId, Integer level, String sample, String dbtype, Map<String, Double> valueLimit, Long batchlimit,
			BlockingQueue<Map<String, Object>> dataQueue, Long bucketSum) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.database = database;
		this.timeseries = timeseries;
		this.columns = columns;
		this.timecolumn = timecolumn;
		this.starttime = starttime;
		this.endtime = endtime;
		this.TYPE = TYPE;
		this.ratio = ratio;
		this.valueLimit = valueLimit;
		this.subId = subId;
		this.level = level;
		this.sample = sample;
		this.dbtype = dbtype;
		this.batchlimit = batchlimit;
		this.dataQueue = dataQueue;
		this.sampleQueue = new LinkedBlockingQueue<>();
		this.bucketSum = bucketSum;

		this.setuptime = System.currentTimeMillis();
		this.throughput = 0L;
		this.columnsStr = "";
		for (int k = 0; k < columns.size(); k++) {
			this.columnsStr += columns.get(k);
			if (k != columns.size() - 1) {
				this.columnsStr += ",";
			}
		}
	}

	@Override
	public void run() {

		// batchlimit L0层级每次从数据库读取的数据量
		batchlimit = 100000;

		// kickOffSampling 触发新一轮采样的累计数据数目
		long kickOffSampling = 1000L;

		// 触发新一层级样本采样的数据数目
		long kickOffThreshold = kickOffSampling * ratio;

		// 是否已经触发过下一层级采样线程
		boolean hadKickOff = false;

		// 本层级样本在TSDB中的数据表
		String pgDatabase = database.replace('.', '_');
		String tableName = "L" + level + "_M" + subId;

		// 最新桶内的最早时间戳，下一批次数据的查询起始时间
		String latestTime = starttime;

		// 读取config文件获取内置数据库信息
		String config = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader("fast.config"));
			String str = "";
			StringBuilder sb = new StringBuilder();
			while ((str = br.readLine()) != null) {
				str = new String(str.getBytes(), "UTF-8");// 解决中文乱码问题
				sb.append(str);
			}
			config = sb.toString();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		JSONObject jsonObject = JSONObject.parseObject(config);
		String innerUrl = jsonObject.getString("innerURL");
		String innerUserName = jsonObject.getString("innerusername");
		String innerPassword = jsonObject.getString("innerpassword");

		PGConnection pgtool = new PGConnection(innerUrl, innerUserName, innerPassword);
		Connection connection = pgtool.getConn();

		if (connection == null) {
			System.out.println("get connection defeat");
			return;
		}

		if (level == 0) {
			String createDatabaseSql = String.format("create database %s;", pgDatabase);
//			System.out.println(createDatabaseSql);
			pgtool.queryUpdate(connection, createDatabaseSql);
		}
		// create database if not exist
		pgtool = new PGConnection(innerUrl + pgDatabase.toLowerCase(), innerUserName, innerPassword);
		connection = pgtool.getConn();

		if (level == 0) {
			String extentionsql = "CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;";
			pgtool.queryUpdate(connection, extentionsql);
		}

		// create table
		String createTableSql = "CREATE TABLE %s (" + "   time    TIMESTAMPTZ         NOT NULL,"
				+ "   weight  DOUBLE PRECISION    NOT NULL,";
		String[] co = new String[columns.size()];
		columns.toArray(co);
		for (int i = 0; i < co.length; i++) {
			if (i == co.length - 1) {
				createTableSql += "   " + co[i] + "      " + TYPE + "                  NOT NULL,";
				createTableSql += "   " + co[i] + "error" + "      " + "DOUBLE PRECISION"
						+ "                  NOT NULL,";
				createTableSql += "   " + co[i] + "area" + "      " + "DOUBLE PRECISION" + "                  NOT NULL";
			} else {
				createTableSql += "   " + co[i] + "      " + TYPE + "                  NOT NULL,";
				createTableSql += "   " + co[i] + "error" + "      " + "DOUBLE PRECISION"
						+ "                  NOT NULL,";
				createTableSql += "   " + co[i] + "area" + "      " + "DOUBLE PRECISION"
						+ "                  NOT NULL,";
			}
		}
		createTableSql += ");";
		System.out.println("createTableSql:" + createTableSql);

		String createHyperTableSql = "select create_hypertable('%s', 'time');";

		pgtool.queryUpdate(connection, String.format(createTableSql, tableName));
//		System.out.println(String.format(createHyperTableSql, tableName));
		pgtool.queryUpdate(connection, String.format(createHyperTableSql, tableName));

		String timelabel = "time";
		String[] iotdbCo = new String[co.length];
		for (int j = 0; j < iotdbCo.length; j++) {
			iotdbCo[j] = database + "." + timeseries + "." + co[j];
		}
		String weightLabel = dbtype.equals("iotdb") ? database + "." + timeseries + ".weight" : "weight";

		String[] labels = dbtype.equals("iotdb") ? iotdbCo : co;
		if (dbtype.equals("influxdb") || dbtype.equals("postgresql")) {
			String[] nlabels = new String[labels.length];
			for (int i = 0; i < nlabels.length; i++) {
				nlabels[i] = labels[i].toLowerCase();
			}
			labels = nlabels;
		}

		// 获取第一批数据，目标为计算BucketSum与时间数值的离群值阈值
		List<Map<String, Object>> dataPoints = null;
		try {
			if (level > 0) {
				dataPoints = new ArrayList<>();
				long k = Math.min(dataQueue.size(), batchlimit);
				System.out.println("k = " + k);
				for (int i = 0; i < k; i++) {
					dataPoints.add(dataQueue.poll());
				}
			} else {// level == 0
				dataPoints = DataController._dataPoints(url, username, password, database, timeseries, columnsStr,
						timecolumn, latestTime, null, String.format(" limit %s", batchlimit), null, "map", null, null,
						dbtype);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// time format change for iotdb
		if (dbtype.equals("iotdb"))
			for (Map<String, Object> dataPoint : dataPoints)
				dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));

		// first weights calc
		List<Double> weights = new ArrayList<>();
		if (level == 0) {
			WeightController.corralation_weights(dataPoints, timelabel, labels, dataPoints.size() / ratio, valueLimit);
			for (Map<String, Object> dataPoint : dataPoints) {
				weights.add((Double) dataPoint.get("weight"));
			}
		} else {
			for (Map<String, Object> dataPoint : dataPoints) {
				weights.add((double) dataPoint.get(weightLabel));
				dataPoint.put("weight", dataPoint.get(weightLabel));
			}
		}

		if (bucketSum == null)
			bucketSum = BucketsController.corralation_bucketSum(dataPoints, labels, dataPoints.size() / ratio);
		System.out.println("level " + level + " bucketSum:" + bucketSum);

		List<Bucket> buckets = BucketsController.corralation_buckets(dataPoints, timelabel, labels,
				dataPoints.size() / ratio, valueLimit);

		// 进行桶内采样
		List<Map<String, Object>> sampleDataPoints = new ArrayList<>();
		sampleDataPoints = SampleController.correlation_samplePoints(buckets, timelabel, labels, sample);

		// 删除上次的最后一桶
		String deletesql = String.format("delete from %s where time>='%s'", tableName, latestTime);
		System.out.println(deletesql);
		pgtool.queryUpdate(connection, deletesql);

		// 写入新一批样本

		List<String> sqls;
		StringBuilder sb;
		String bigSql;
		sampleDataPoints.sort(sampleComparator);
		ErrorController.correlation_lineError(dataPoints, sampleDataPoints, labels, level == 0);
		sqls = new ArrayList<>();
		String batchInsertFormat = "insert into %s (time, weight";
		for (int i = 0; i < columns.size(); i++) {
			batchInsertFormat += ", " + columns.get(i);
			batchInsertFormat += ", " + columns.get(i) + "error";
			batchInsertFormat += ", " + columns.get(i) + "area";
		}
		batchInsertFormat += ") values (";
		for (Map<String, Object> map : sampleDataPoints) {
			String newbatchInsertFormat = batchInsertFormat;
			newbatchInsertFormat += "'" + map.get("time").toString() + "', " + map.get("weight");
			for (int i = 0; i < labels.length; i++) {
				newbatchInsertFormat += ", " + map.get(labels[i]);
				newbatchInsertFormat += ", " + map.get(labels[i] + "error");
				newbatchInsertFormat += ", " + map.get(labels[i] + "area");
			}
			newbatchInsertFormat += ");";
			sqls.add(String.format(newbatchInsertFormat, tableName));
		}
		sb = new StringBuilder();
		for (String sql : sqls) {
			if (sql.toLowerCase().contains("nan"))
				System.out.println(sql);
			else if (sql.toLowerCase().contains("infinity")) {
				System.out.println("-------------");
				System.out.println(sql);
			} else
				sb.append(sql);
		}
		bigSql = sb.toString();
		pgtool.queryUpdate(connection, bigSql);

		// 更新sampleQueue给下一层级
		for (Map<String, Object> s : sampleDataPoints) {
			for (int i = 0; i < labels.length; i++) {
				s.put(columns.get(i).toLowerCase(), s.get(labels[i]));
				s.put(columns.get(i).toLowerCase() + "error", s.get(labels[i] + "error"));
				s.put(columns.get(i).toLowerCase() + "area", s.get(labels[i] + "area"));

			}
		}
		sampleQueue.addAll(sampleDataPoints);

		if (buckets.get(buckets.size() - 1).getDataPoints().get(0).get("time").toString().length()>23) {
			latestTime = buckets.get(buckets.size() - 1).getDataPoints().get(0).get("time").toString().substring(0, 23);
		}else {
			latestTime = buckets.get(buckets.size() - 1).getDataPoints().get(0).get("time").toString();
		}

		// keep sampling data
		while (!exit) {
			long roundStartTime = System.currentTimeMillis();

			if (level > 0) {
				while (true) {
					double queueWeight = 0.0;
					dataPoints = new ArrayList<>(dataQueue);
					for (Map<String, Object> p : dataPoints)
						queueWeight += (Double) p.get("weight");
					if (queueWeight >= bucketSum)
						break;
					else {
						try {
							Thread.sleep(100 * level);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				// level = 0
				if (dataPoints.size() < batchlimit) {
					System.out.println("L0 catched up <<<<<<<<<<<<<<<!!!!!!!!!!!!!!");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				if (level > 0) {
					dataPoints = new ArrayList<>();
					long k = Math.min(dataQueue.size(), batchlimit);
					System.out.println("k = " + k);
					for (int i = 0; i < k; i++) {
						dataPoints.add(dataQueue.poll());
					}
				} else {
					// level = 0
					dataPoints = DataController._dataPoints(url, username, password, database, timeseries, columnsStr,
							timecolumn, latestTime, null, String.format(" limit %s", batchlimit), null, "map", null,
							null, dbtype);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dataPoints = new ArrayList<>(dataPoints);

			for (Map<String, Object> dataPoint : dataPoints)
				dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));

			dataPoints.sort(sampleComparator);

			if (dataPoints == null || dataPoints.size() == 0) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			// time format change for iotdb
			if (dbtype.equals("iotdb"))
				for (Map<String, Object> dataPoint : dataPoints)
					dataPoint.put(timelabel, dataPoint.get(timelabel).toString().replace("T", " "));

			// weights calc
			weights = new ArrayList<>();
			if (level == 0) {
				WeightController.corralation_weights(dataPoints, timelabel, labels, dataPoints.size() / ratio,
						valueLimit);
				for (Map<String, Object> dataPoint : dataPoints)
					weights.add((Double) dataPoint.get("weight"));
			} else {
				for (Map<String, Object> dataPoint : dataPoints) {
					weights.add((double) dataPoint.get(weightLabel));
					dataPoint.put("weight", dataPoint.get(weightLabel));
				}
			}

			if (bucketSum == null)
				bucketSum = BucketsController.corralation_bucketSum(dataPoints, labels, dataPoints.size() / ratio);
			System.out.println("level " + level + " bucketSum:" + bucketSum);

			buckets = BucketsController.corralation_buckets(dataPoints, timelabel, labels, dataPoints.size() / ratio,
					valueLimit);

			// 进行桶内采样
			sampleDataPoints = SampleController.correlation_samplePoints(buckets, timelabel, labels, sample);

			// 删除上次的最后一桶
			deletesql = String.format("delete from %s where time>='%s'", tableName, latestTime);
			System.out.println(deletesql);
			pgtool.queryUpdate(connection, deletesql);

			// 写入新一批样本
			sampleDataPoints.sort(sampleComparator);
			ErrorController.correlation_lineError(dataPoints, sampleDataPoints, labels, level == 0);
			sqls = new ArrayList<>();

			for (Map<String, Object> map : sampleDataPoints) {
				String newbatchInsertFormat = batchInsertFormat;
				newbatchInsertFormat += "'" + map.get("time").toString() + "', " + map.get("weight");
				for (int i = 0; i < labels.length; i++) {
					newbatchInsertFormat += ", " + map.get(labels[i]);
					newbatchInsertFormat += ", " + map.get(labels[i] + "error");
					newbatchInsertFormat += ", " + map.get(labels[i] + "area");
				}
				newbatchInsertFormat += ");";
				sqls.add(String.format(newbatchInsertFormat, tableName));
			}
			sb = new StringBuilder();
			for (String sql : sqls) {
				if (sql.toLowerCase().contains("nan"))
					System.out.println(sql);
				else
					sb.append(sql);
			}
			bigSql = sb.toString();
			pgtool.queryUpdate(connection, bigSql);

			// 更新sampleQueue给下一层级
			for (Map<String, Object> s : sampleDataPoints) {
				for (int i = 0; i < labels.length; i++) {
					s.put(columns.get(i).toLowerCase(), s.get(labels[i]));
					s.put(columns.get(i).toLowerCase() + "error", s.get(labels[i] + "error"));
					s.put(columns.get(i).toLowerCase() + "area", s.get(labels[i] + "area"));
				}
			}
			sampleQueue.addAll(sampleDataPoints);

			// 统计throughput
			throughput += dataPoints.size();
			long usedtime = System.currentTimeMillis() - setuptime;
			long roundtime = System.currentTimeMillis() - roundStartTime + 1L;
			System.out.println(String.format("throughput:%d, used time: %d, average:%d", throughput, usedtime,
					dataPoints.size() * 1000 / roundtime));

			// 根据throughput触发下一层级
//			System.out.println("hadKickOff,throughput,kickOffThreshold");
			if (!hadKickOff && throughput > kickOffThreshold) {
				hadKickOff = true;
				String Identifier = String.format("%s,%s,%s,%s,%s", url, database, tableName, columnsStr, salt);
				String newSubId = DigestUtils.md5DigestAsHex(Identifier.getBytes()).substring(0, 8);
				System.out.println("kick off the level " + (level + 1) + "<<<<<<!!!!!!!");
				if (level ==6) {
					System.out.println("level6:sampleQueue.size:"+sampleQueue.size());
				}
				LayerThread pgsubscribeThread = new LayerThread(url, username, password, database, tableName, columns,
						timecolumn, starttime, endtime, TYPE, ratio, newSubId, level + 1, sample, "postgresql",
						valueLimit, batchlimit, sampleQueue, bucketSum * ratio / 2);
				pgsubscribeThread.start();
			}

			if (exit) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			// 单独处理最新桶，获得latestTime
			List<Map<String, Object>> newcomingData = buckets.get(buckets.size() - 1).getDataPoints();
//			latestTime = newcomingData.get(0).get("time").toString().substring(0, 23);
			if (newcomingData.get(0).get("time").toString().length()>23) {
				latestTime = newcomingData.get(0).get("time").toString().substring(0, 23);
			}else {
				latestTime = newcomingData.get(0).get("time").toString();
			}
			// 采样终止条件：latestTime大于截止日期
			if (endtime != null && latestTime.compareTo(endtime) >= 0) {
				exit = true;
				System.out.println("L" + level + " catched up <<<<<<<<<<<<<<<!!!!!!!!!!!!!!");
			}

			System.out.println("buckets.size():" + buckets.size());
			System.out.println(String.format("L%d data size: %s, sample size: %s, ratio: %s", level, dataPoints.size(),
					sampleDataPoints.size(), dataPoints.size() / sampleDataPoints.size()));

			// 生命在于留白
			try {
				long interval = 300L;
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
