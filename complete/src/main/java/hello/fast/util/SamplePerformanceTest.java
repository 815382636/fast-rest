package hello.fast.util;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import hello.fast.source.PGConnection;

public class SamplePerformanceTest {
	public static void main(String[] args) {
		String innerUrl = "jdbc:postgresql://192.168.10.172:5432/";
        String innerUserName = "postgres";
        String innerPassword = "1111aaaa";

        PGConnection pgtool = new PGConnection(
                innerUrl,
                innerUserName,
                innerPassword
        );
        Connection connection = pgtool.getConn();
        if (connection == null) {
            System.out.println("get connection defeat");
            return;
        }
        String createDatabaseSql = String.format("create database %s;", "sampletest1");
        System.out.println(createDatabaseSql);
        pgtool.queryUpdate(connection, createDatabaseSql);
        
        pgtool = new PGConnection(
                innerUrl + "sampletest1",
                innerUserName,
                innerPassword
        );
        connection = pgtool.getConn();
        String extentionsql = "CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;";
        pgtool.queryUpdate(connection, extentionsql);
        String createTableSql =
                "CREATE TABLE %s (" +
                "   time    TIMESTAMP         NOT NULL," +
                "   weight  DOUBLE PRECISION    NOT NULL);";
        pgtool.queryUpdate(connection,String.format(createTableSql, "sample10"));
        List<String> sqls = new ArrayList<>();
        StringBuilder sb;
        String bigSql;

        String batchInsertFormat = "insert into %s (time, weight) values ('%s', %s);";
        Random  r =new Random();
        for (int i = 0; i < 100000; i++) {
        	Timestamp ts=new Timestamp(new Date().getTime());
        	double weight =r.nextInt(10);
        	sqls.add(String.format(batchInsertFormat,"sample10",ts,weight));
		}
        sb = new StringBuilder();
        for(String sql : sqls) {
            sb.append(sql);
        }
        bigSql = sb.toString();
        pgtool.queryUpdate(connection, bigSql);
        
        System.out.println("100000 finish");
        
        
        createTableSql =
                "CREATE TABLE %s (" +
                "   time    TIMESTAMP         NOT NULL," +
                "   weight  DOUBLE PRECISION    NOT NULL);";
        pgtool.queryUpdate(connection,String.format(createTableSql, "sample100"));
        
        for(int j =0;j<10;j++) {
        	System.out.println("百万级"+j);
        	List<String> sqls_1 = new ArrayList<>();
            StringBuilder sb_1;
            String bigSql_1;

            String batchInsertFormat_1 = "insert into %s (time, weight) values ('%s', %s);";
            Random  r_1 =new Random();
	        for (int i = 0; i < 100000; i++) {
	        	Timestamp ts=new Timestamp(new Date().getTime());
	        	double weight =r_1.nextInt(10);
	        	sqls_1.add(String.format(batchInsertFormat_1,"sample100",ts,weight));
			}
	        sb_1 = new StringBuilder();
	        for(String sql : sqls_1) {
	            sb_1.append(sql);
	        }
	        bigSql_1 = sb_1.toString();
	        pgtool.queryUpdate(connection, bigSql_1);
        }
        System.out.println("1000000 finish");
        
        createTableSql =
                "CREATE TABLE %s (" +
                "   time    TIMESTAMP         NOT NULL," +
                "   weight  DOUBLE PRECISION    NOT NULL);";
        pgtool.queryUpdate(connection,String.format(createTableSql, "sample1000"));
        for(int j =0;j<100;j++) {
        	System.out.println("千万级"+j);
        	List<String> sqls_1 = new ArrayList<>();
            StringBuilder sb_1;
            String bigSql_1;

            String batchInsertFormat_1 = "insert into %s (time, weight) values ('%s', %s);";
            Random  r_1 =new Random();
	        for (int i = 0; i < 100000; i++) {
	        	Timestamp ts=new Timestamp(new Date().getTime());
	        	double weight =r_1.nextInt(10);
	        	sqls_1.add(String.format(batchInsertFormat_1,"sample1000",ts,weight));
			}
	        sb_1 = new StringBuilder();
	        for(String sql : sqls_1) {
	            sb_1.append(sql);
	        }
	        bigSql_1 = sb_1.toString();
	        pgtool.queryUpdate(connection, bigSql_1);
        }
        System.out.println("10000000 finish");
        
        createTableSql =
                "CREATE TABLE %s (" +
                "   time    TIMESTAMP         NOT NULL," +
                "   weight  DOUBLE PRECISION    NOT NULL);";
        pgtool.queryUpdate(connection,String.format(createTableSql, "sample10000"));
        for(int j =0;j<1000;j++) {
        	System.out.println("亿级"+j);
        	List<String> sqls_1 = new ArrayList<>();
            StringBuilder sb_1;
            String bigSql_1;

            String batchInsertFormat_1 = "insert into %s (time, weight) values ('%s', %s);";
            Random  r_1 =new Random();
	        for (int i = 0; i < 100000; i++) {
	        	Timestamp ts=new Timestamp(new Date().getTime());
	        	double weight =r_1.nextInt(10);
	        	sqls_1.add(String.format(batchInsertFormat_1,"sample10000",ts,weight));
			}
	        sb_1 = new StringBuilder();
	        for(String sql : sqls_1) {
	            sb_1.append(sql);
	        }
	        bigSql_1 = sb_1.toString();
	        pgtool.queryUpdate(connection, bigSql_1);
        }
        System.out.println("100000000 finish");

	}
}
