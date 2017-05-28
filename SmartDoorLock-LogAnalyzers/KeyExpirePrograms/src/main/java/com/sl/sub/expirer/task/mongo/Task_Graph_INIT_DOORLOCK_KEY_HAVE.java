package com.sl.sub.expirer.task.mongo;


import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import com.sl.sub.expirer.context.ExpirerContext;
import com.sl.sub.expirer.log.SystemLogSetter;
import com.sl.sub.expirer.task.ExpirerTask;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Repository
public class Task_Graph_INIT_DOORLOCK_KEY_HAVE implements ExpirerTask{

	private SqlSession oracleSession;
	private MongoTemplate mongoTemplate;
	private	SystemLogSetter logSetter;
	
	final String ORACLE_NS = "mapper.com.sl.sub.expirer.task.graph.";
	final String COLLECTION = "DOORLOCK_KEY_HAVE";
	
	private static Logger logger = LoggerFactory.getLogger(Task_Graph_INIT_DOORLOCK_KEY_HAVE.class);
	
	//logic temp variable
	JSONArray keys = new JSONArray();
	JSONArray doorlockList = new JSONArray();
	JSONObject doorlockListKeys = new JSONObject();
	
	JSONObject doorlockTemp =null;
	JSONObject keyTemp =null;
	JSONObject graphTemp = null;
	String serial_no = "";
	int member = 0;
	int manager = 0;
	int master = 0;
	String dateString="";
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	Date date;
	
	
	//mongoDB update or insert opt
	String query = "";
	String set = "";
	
	@Override
	public JSONObject execute(JSONObject data) {
		// TODO Auto-generated method stub
		this.beforeExecute();
		//start of this method
		try{	
			logger.info(data.toString());
			
/*			
			data.put("userList", userList);
			data.put("userListKeys", userListKeys);
			data.put("userListDoorlock", userListDoorlock);
			
			data.put("catList", catList);
			data.put("catListKeys", catListKeys);
			
			data.put("doorlockList", doorlockList);
			data.put("doorlockListKeys", doorlockListKeys);		
*/			
			doorlockList = data.getJSONArray("doorlockList");
			doorlockListKeys = data.getJSONObject("doorlockListKeys");
			date = new Date(System.currentTimeMillis());
			dateString = df.format(date).toString();
//			dateString = "2016-11-04";
			for(int i=0;i<doorlockListKeys.size();i++){
				doorlockTemp=null;
				doorlockTemp=doorlockList.getJSONObject(i);
				serial_no = doorlockTemp.getString("SERIAL_NO");
				keys = doorlockListKeys.getJSONArray(serial_no);
				member=0;
				manager=0;
				master=0;
				for(int j=0;j<keys.size();j++){
					keyTemp = null;
					keyTemp = keys.getJSONObject(j);
					switch(keyTemp.getString("GRADE")){
						case "MASTER":
							master++;
							break;
						case "MANAGER":
							manager++;
							break;
						case "MEMBER":
							member++;
							break;
						default:
							logger.warn("으아니 슈발 이건 무슨 등급이다냐아아아~~!!");
							break;
					}
				}
				graphTemp = null;
				graphTemp = new JSONObject();
				graphTemp.put("MASTER", master);
				graphTemp.put("MANAGER", manager);
				graphTemp.put("MEMBER", member);
				graphTemp.put("DATE", dateString);
				graphTemp.put("SERIAL_NO", serial_no);
				logger.debug(serial_no+":"+graphTemp.toString());
				
				if(doorlock_isExist(graphTemp)){
					if(date_isExist(graphTemp)){
						//날짜가 있을때 업데이트를 칩니다.
						query=	"{SERIAL_NO:'(1)',GRAPH:{$elemMatch:{DATE:'(2)'}}}";
						query = query
								.replace("(1)", graphTemp.getString("SERIAL_NO"))
								.replace("(2)", graphTemp.getString("DATE"));
						set = "{$set:{GRAPH.$.DATE:'(1)',GRAPH.$.MASTER:(2),GRAPH.$.MANAGER:(3),GRAPH.$.MEMBER:(4)}}";
						set = set.replace("(1)",graphTemp.getString("DATE"))
								.replace("(2)", graphTemp.getString("MASTER"))
								.replace("(3)", graphTemp.getString("MANAGER"))
								.replace("(4)", graphTemp.getString("MEMBER"));
						
						WriteResult result = mongoTemplate.getCollection(COLLECTION).update(
																(DBObject)JSON.parse(query),
																(DBObject)JSON.parse(set),
																false,
																true);
						logger.debug(result.toString());
						
						
					}else{
						//날짜가 없을때 새로 생성합니다.
						query=	"{SERIAL_NO:'(1)'}";
						query = query.replace("(1)", graphTemp.getString("SERIAL_NO"));
						set = "{$push:{GRAPH:{DATE:'(1)',MASTER:(2),MANAGER:(3),MEMBER:(4)}}}";
						set = set.replace("(1)",graphTemp.getString("DATE"))
								.replace("(2)", graphTemp.getString("MASTER"))
								.replace("(3)", graphTemp.getString("MANAGER"))
								.replace("(4)", graphTemp.getString("MEMBER"));
						
						WriteResult result = mongoTemplate.getCollection(COLLECTION).update(
																(DBObject)JSON.parse(query),
																(DBObject)JSON.parse(set),
																false,
																true);
						logger.debug(result.toString());
					}
				}else{
					//등록된 사용자가 없을경우 새로 DB를 생성합니다.
					set = "{SERIAL_NO:'(1)',GRAPH:[{DATE:'(2)',MASTER:(3),MANAGER:(4),MEMBER:(5)}]}";
					set = set
							.replace("(1)", graphTemp.getString("SERIAL_NO"))
							.replace("(2)", graphTemp.getString("DATE"))
							.replace("(3)", graphTemp.getString("MASTER"))
							.replace("(4)", graphTemp.getString("MANAGER"))
							.replace("(5)", graphTemp.getString("MEMBER"));
					WriteResult result = mongoTemplate.getCollection(COLLECTION).insert((DBObject)JSON.parse(set));						
					logger.debug(result.toString());
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		this.afterExecute(true);
		return data;
	}
	
	public boolean doorlock_isExist(JSONObject user){
		query="{SERIAL_NO:'"+user.getString("SERIAL_NO")+"'}";
		DBCursor cursor = mongoTemplate.getCollection(COLLECTION).find((DBObject)JSON.parse(query));
		logger.debug(cursor.toString());
		if(cursor.hasNext()){
			logger.debug(" TRUE : doorlock_isExist:["+user.getString("SERIAL_NO")+"]");
			return true;
		}else{
			logger.debug(" FALSE : doorlock_isExist:["+user.getString("SERIAL_NO")+"]");
			return false;
		}	
	}
	public boolean date_isExist(JSONObject user){
		query=	"{"+
					"SERIAL_NO:'"+user.getString("SERIAL_NO")+"',"+ 
					"GRAPH:{"+
						"$elemMatch:{DATE:'"+user.getString("DATE")+"'}"+
					"}"+
				"}";
		DBCursor cursor = mongoTemplate.getCollection(COLLECTION).find((DBObject)JSON.parse(query));
		logger.debug(cursor.toString());
		if(cursor.hasNext()){
			logger.debug(" TRUE : date_isExist:["+user.getString("SERIAL_NO")+"]");
			return true;
		}else{
			logger.debug(" FALSE : date_isExist:["+user.getString("SERIAL_NO")+"]");
			return false;
		}	
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Override
	public void beforeExecute() {
		// TODO Auto-generated method stub
		this.oracleSession = (SqlSessionTemplate) ExpirerContext.context.getBean("sqlSessionTemplate");
		this.mongoTemplate = (MongoTemplate) ExpirerContext.context.getBean("mongoTemplate");
		this.logSetter = (SystemLogSetter) ExpirerContext.context.getBean("logSetter");
		
	}

	@Override
	public void afterExecute(boolean commit) {
		// TODO Auto-generated method stuf
	}
}
