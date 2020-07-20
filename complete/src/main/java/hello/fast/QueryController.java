package hello.fast;

import com.alibaba.fastjson.JSONObject;
import hello.fast.meta.TimeSeries;
import hello.fast.meta.TimeSeriesController;
import hello.fast.util.UtilMethod;

import org.apache.iotdb.rpc.IoTDBRPCException;
import org.apache.iotdb.session.IoTDBSessionException;
import org.apache.thrift.TException;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
* 查询控制器，实现数据采样订阅的层级查询引擎
*/
@RestController
public class QueryController {

    private static final String salt = "&%12345***&&%%$$#@1";

    @RequestMapping("/query")
    public List<Map<String, Object>> publish(
            @RequestParam(value="url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
            @RequestParam(value="username", defaultValue = "root") String username,
            @RequestParam(value="password", defaultValue = "root") String password,
            @RequestParam(value="database") String database,
            @RequestParam(value="timeseries") String timeseries,
            @RequestParam(value="columns") List<String> columns,
            @RequestParam(value="timecolumn", defaultValue = "time") String timecolumn,
            @RequestParam(value="starttime", required = false) String starttime,
            @RequestParam(value="endtime", required = false) String endtime,
            @RequestParam(value="amount", defaultValue = "2000") Long amount,
            @RequestParam(value="ip", required = false) String ip,
            @RequestParam(value="port", required = false) String port,
			@RequestParam(value = "returnType", defaultValue = "Integration") String returnType,
            @RequestParam(value="dbtype", defaultValue = "iotdb") String dbtype,
            @RequestParam(value="format", defaultValue = "map") String format
    ) throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
        url = url.replace("\"", "");
        username = username.replace("\"", "");
        password = password.replace("\"", "");
        database = database.replace("\"", "");
        returnType =returnType.replace("\"", "");
        timeseries = timeseries.replace("\"", "");
        timecolumn = timecolumn.replace("\"", "");
        starttime = starttime == null ? null : starttime.replace("\"", "");
        endtime = endtime == null ? null : endtime.replace("\"", "");
        format = format.replace("\"", "");
        ip = ip == null ? null : ip.replace("\"", "");
        port = port == null ? null : port.replace("\"", "");
        dbtype = dbtype.replace("\"", "");

        if(dbtype.toLowerCase().equals("iotdb")) {
            if (ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);
        }
        else if(dbtype.toLowerCase().equals("timescaledb") || dbtype.toLowerCase().equals("postgresql")) {
            if (ip != null && port != null) url = String.format("jdbc:postgresql://%s:%s/", ip, port);
        }
        else if(dbtype.toLowerCase().equals("influxdb")) {
            if (ip != null && port != null) url = String.format("http://%s:%s/", ip, port);
        }
        else{
            if (ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);
        }

        long t1 = System.currentTimeMillis();

        String config = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("fast.config"));
            String str = "";
            StringBuilder sb = new StringBuilder();
            while ((str = br.readLine()) != null) {
                str=new String(str.getBytes(), StandardCharsets.UTF_8);//解决中文乱码问题
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

        String columnsStr ="";
        for (int k = 0; k < columns.size(); k++) {
        	columnsStr +=columns.get(k);
			if(k !=columns.size()-1) {
				columnsStr +=",";
			}
		}
        
        // iotdb is . tsdb is _
        String[] tables = subTables(url, innerUrl, innerUserName, innerPassword, database, timeseries, columnsStr);
        // 是否已经找到对应层级
        boolean hit = false;

        List<Map<String, Object>> res = null;
        for (String tableName : tables) {
            System.out.println(tableName);
            if(!hit){
                res = new ArrayList<>(DataController._dataPoints(
                    innerUrl, innerUserName, innerPassword, database.replace(".", "_"), tableName, columnsStr + ", weight, error, area", "time", starttime, endtime, null, null, "map", null, null, "postgresql"));
            }
            else{
                res.addAll(DataController._dataPoints(
                    innerUrl, innerUserName, innerPassword, database.replace(".", "_"), tableName, columnsStr, "time", starttime, endtime, null, null, "map", null, null, "postgresql"));
            }
            System.out.println("res.size()" + res.size());
            if (res.size() >= amount) {
                hit = true;
                // 找到对应层级后，以最新数据点为起始时间继续向下查询
                starttime = res.get(res.size() - 1).get("time").toString().substring(0,23);
            }
        }

        if(!hit) res = DataController._dataPoints(url, username, password, database, timeseries, columnsStr, timecolumn, starttime, endtime, " limit 10000", null, format, ip, port, dbtype);
        
        System.out.println("-------------");
        System.out.println(res.size());
        System.out.println("-------------");
        if (returnType.contains("Integration")) 
        	res =UtilMethod.change_type(res, columns);
		
        if(format.equals("map")) return res;
        List<Map<String, Object>> result = new ArrayList<>();
        for(Map<String, Object> map : res){
            for(Map.Entry<String, Object> entry : map.entrySet()){
                String mapKey = entry.getKey();
                if(mapKey.equals("Time")) continue;
                Map<String, Object> m = new HashMap<>();
                m.put("time", map.get("Time"));
                m.put("label", mapKey);
                m.put("value", entry.getValue());
                result.add(m);
            }
        }

        System.out.println("QueryController: " + (System.currentTimeMillis() - t1) + "ms");

        return result;
    }

    @RequestMapping("/errorquery")
    public List<Map<String, Object>> errorpublish(
            @RequestParam(value="url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
            @RequestParam(value="username", defaultValue = "root") String username,
            @RequestParam(value="password", defaultValue = "root") String password,
            @RequestParam(value="database") String database,
            @RequestParam(value="timeseries") String timeseries,
            @RequestParam(value="columns") List<String> columns,
            @RequestParam(value="timeColumn", defaultValue = "time") String timecolumn,
            @RequestParam(value="startTime", required = false) String starttime,
            @RequestParam(value="endTime", required = false) String endtime,
            @RequestParam(value="error", required = false) Double errorPercent,
            @RequestParam(value="ip", required = false) String ip,
            @RequestParam(value="port", required = false) String port,
			@RequestParam(value = "returnType", defaultValue = "Integration") String returnType,
            @RequestParam(value="dbtype", defaultValue = "iotdb") String dbtype,
            @RequestParam(value="format", defaultValue = "map") String format
    ) throws SQLException, TException, IoTDBRPCException, IoTDBSessionException {
        url = url.replace("\"", "");
        username = username.replace("\"", "");
        password = password.replace("\"", "");
        database = database.replace("\"", "");
        timeseries = timeseries.replace("\"", "");
        timecolumn = timecolumn.replace("\"", "");
        returnType =returnType.replace("\"", "");
        starttime = starttime == null ? null : starttime.replace("\"", "");
        endtime = endtime == null ? null : endtime.replace("\"", "");
        format = format.replace("\"", "");
        ip = ip == null ? null : ip.replace("\"", "");
        port = port == null ? null : port.replace("\"", "");
        dbtype = dbtype.replace("\"", "");

        if(dbtype.toLowerCase().equals("iotdb")) {
            if (ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);
        }
        else if(dbtype.toLowerCase().equals("timescaledb") || dbtype.toLowerCase().equals("postgresql")) {
            if (ip != null && port != null) url = String.format("jdbc:postgresql://%s:%s/", ip, port);
        }
        else if(dbtype.toLowerCase().equals("influxdb")) {
            if (ip != null && port != null) url = String.format("http://%s:%s/", ip, port);
        }
        else{
            if (ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);
        }

        long t1 = System.currentTimeMillis();

        String config = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("fast.config"));
            String str = "";
            StringBuilder sb = new StringBuilder();
            while ((str = br.readLine()) != null) {
                str=new String(str.getBytes(), StandardCharsets.UTF_8);//解决中文乱码问题
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

        String columnsStr ="";
        for (int k = 0; k < columns.size(); k++) {
        	columnsStr +=columns.get(k);
			if(k !=columns.size()-1) {
				columnsStr +=",";
			}
		}
        
        // iotdb is . tsdb is _
        String[] tables = subTables(url, innerUrl, innerUserName, innerPassword, database, timeseries, columnsStr);
        boolean hit = false;

        List<Map<String, Object>> res = null;
        for(String tableName : tables){
            System.out.println(tableName);
            if(!hit){
                res = new ArrayList<>(DataController._dataPoints(
                    innerUrl, innerUserName, innerPassword, database.replace(".", "_"), tableName, columnsStr + ", weight, error, area", "time", starttime, endtime, null, null, "map", null, null, "postgresql"));
            }
            else {
                res.addAll(DataController._dataPoints(
                    innerUrl, innerUserName, innerPassword, database.replace(".", "_"), tableName, columnsStr + ", weight, error, area", "time", starttime, endtime, null, null, "map", null, null, "postgresql"));
            }
            System.out.println("res.size()" + res.size());

            double error= 0.0;
            double area = 0.0;

            for(int i = 0; i < res.size(); i++){
                error += (double)res.get(i).get("error");
                area += (double)res.get(i).get("area");
            }

            System.out.println(error / area);
            if((error / area) <= errorPercent) {
                hit = true;
            }

            if(hit) starttime = res.get(res.size() - 1).get("time").toString().substring(0,23);
        }

        // 找不到合适的样本，查询原始数据
        if(!hit) res = DataController._dataPoints(url, username, password, database, timeseries, columnsStr, timecolumn, starttime, endtime, " limit 10000", null, format, ip, port, dbtype);

        System.out.println("ErrorQueryController: " + (System.currentTimeMillis() - t1) + "ms");

        if (returnType.contains("Integration")) 
        	res =UtilMethod.change_type(res, columns);
        return res;
    }

    // 数据源对应的订阅数据表
    static String[] subTables(String url, String innerurl, String username, String password, String database, String timeseries, String columns) throws SQLException {
        try{
            List<TimeSeries> timeSeries = new TimeSeriesController().timeSeries(innerurl, username, password, database.replace(".", "_"), null, null, "postgresql");
            int n = timeSeries.size();
            String[] res = new String[n];
            String tableName = timeseries;
            for(int i = 0; i < n; i++){
                String Identifier = String.format("%s,%s,%s,%s,%s", url, database, tableName, columns, salt);
                String newSubId = DigestUtils.md5DigestAsHex(Identifier.getBytes()).substring(0,8);
                tableName = "L" + i + "_M" + newSubId;
                res[n-1-i] = tableName;
            }
            return res;
        } catch (Exception e){
            System.out.println(e.getMessage());
            return new String[0];
        }
    }
}
