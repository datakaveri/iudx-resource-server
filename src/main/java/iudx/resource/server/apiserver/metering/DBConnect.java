package iudx.resource.server.apiserver.metering;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnect {

	
	private static final Logger LOGGER = LogManager.getLogger(DBConnect.class);
	
    public  void addData(String email, String resource_ID, String api, long current_Time_Stamp) {
       	String DB_URL = "jdbc:postgresql://139.59.69.190:32508/metering?sslmode=allow&preferQueryMode=simple";
        String USER = "immudb";
        String PASS = "immudb";
        
//        CREATE TABLE immudbdemo1(id INTEGER, time INTEGER, email VARCHAR, api VARCHAR, resource VARCHAR, PRIMARY KEY id);
         
	try {
		
	Connection conn=DriverManager.getConnection(DB_URL, USER, PASS);
	LOGGER.info("Connected!");
//	CREATE TABLE immudbtest(time INTEGER, email VARCHAR, api VARCHAR, resource VARCHAR, PRIMARY KEY email);

	String sql="UPSERT INTO immudbtest(time,email,api,resource) VALUES (?,?,?,?);";
	PreparedStatement state=conn.prepareStatement(sql);
	
	state.setLong(1, current_Time_Stamp);
	state.setString(2, email);
	state.setString(3, api);
	state.setString(4,resource_ID);
	int row=state.executeUpdate();
	if(row>0) {
		LOGGER.info("SuccessFul");
	}
	
	}
	catch(SQLException e){
		e.printStackTrace();
		LOGGER.info("Error!");
	}

    			
    	}
    
    public void getData() {
    	
      	String DB_URL = "jdbc:postgresql://139.59.69.190:32508/defaultdb?sslmode=allow&preferQueryMode=simple";
        String USER = "immudb";
        String PASS = "immudb";
        
         
	try {
		
	Connection conn=DriverManager.getConnection(DB_URL, USER, PASS);
	LOGGER.info("Connected!");
	Statement state=conn.createStatement();
	
	ResultSet rs = state.executeQuery("SELECT * FROM immudbdemo1");
	while(rs.next()) {
	LOGGER.info("TIME: "+getISO(rs.getLong("(defaultdb.immudbdemo1.time)")));
	LOGGER.info("Email: "+rs.getString("(defaultdb.immudbdemo1.email)"));
	LOGGER.info("Api: " + rs.getString("(defaultdb.immudbdemo1.api)"));
	LOGGER.info("Resource: " + rs.getString("(defaultdb.immudbdemo1.resource)"));
	}
	
	}
	catch(SQLException e){
		e.printStackTrace();
		LOGGER.info("Error!");
	}
    	
    }
    
    public String getISO(Long time) {
    	Date d = new Date(time);
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    	String dateStr = sdf.format(d);
    	return dateStr;
    }
    
    }