package cn.imqiba.main;

import java.util.List;

import org.apache.log4j.Logger;

import cn.imqiba.config.ServerConfig;
import cn.imqiba.util.CacheKey;
import cn.imqiba.util.ServerHelper;
import cn.imqiba.util.CacheKey.UserBaseInfo;
import cn.imqiba.util.RedisAgent;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public class OnlinePushWorker implements Runnable
{
	private Logger logger = Logger.getLogger(Class.class);
	private int m_serverID = 0;
	
	public OnlinePushWorker(int serverID)
	{
		m_serverID = serverID;
	}
	
	@Override
	public void run()
	{
		ScanParams scanParams = new ScanParams();
		scanParams.match(CacheKey.UserBaseInfo.keyname + "*");
		scanParams.count(1000);
		
		while(true)
		{
			RedisAgent redis = new RedisAgent();
			String assistantUinString = redis.hget(CacheKey.SysUser.servername, CacheKey.SysUser.keyname, 0, CacheKey.SysUser.assistant);
			int assistantUin = Integer.parseInt(assistantUinString);
			
			List<String> sysConfigList = redis.hmget(CacheKey.SystemConfig.servername, CacheKey.SystemConfig.keyname, 0,
					CacheKey.SystemConfig.cursysnotiver, CacheKey.SystemConfig.pushsysnotiver, CacheKey.SystemConfig.sysnotiset);
			int curSysNotiVer = Integer.parseInt(sysConfigList.get(0));
			int pushSysNotiVer = Integer.parseInt(sysConfigList.get(1));
			
			if(pushSysNotiVer >= curSysNotiVer)
			{
				try
				{
					Thread.sleep(10000);
				}
				catch (Exception e)
				{
					logger.error("", e);
				}
				
				continue;
			}
			
			String[] sysNotiVerStrings = sysConfigList.get(2).split(":", 11);
			
			int sysNotiCount = 0;
			int[] sysNotiVers = new int[10];
			for(String sysNotiVerString : sysNotiVerStrings)
			{
				sysNotiVers[sysNotiCount++] = Integer.parseInt(sysNotiVerString);
			}
			
			String cursorString = "0";
			do
			{
				long curTime = System.currentTimeMillis() / 1000;
				
				ScanResult<String> scanResult = redis.scan(CacheKey.UserBaseInfo.servername, 0, cursorString, scanParams);
				List<String> userKeyList = scanResult.getResult();
				
				for(String userKey : userKeyList)
				{
					int index = userKey.lastIndexOf(":");
					String uinString = userKey.substring(index + 1);
					
					int uin = Integer.parseInt(uinString);
					if(uin == assistantUin)
					{
						continue;
					}
					
					List<String> userBaseInfoList = redis.hmget(CacheKey.UserBaseInfo.servername, CacheKey.UserBaseInfo.keyname + uinString,
							uin, CacheKey.UserBaseInfo.sysnotiver, CacheKey.UserBaseInfo.lastlogintime);
					
					int userSysVer = 0;
					if(userBaseInfoList.get(0) != null)
					{
						userSysVer = Integer.parseInt(userBaseInfoList.get(0));
					}
					
					if(userSysVer >= curSysNotiVer)
					{
						continue;
					}
					
					long lastLoginTime = Long.parseLong(userBaseInfoList.get(1));
					if(lastLoginTime + 30 * 24 * 60 * 60 < curTime)
					{
						continue;
					}
					
					for(int notiIndex = 0; notiIndex < sysNotiCount; ++notiIndex)
					{
						if(sysNotiVers[notiIndex] <= userSysVer)
						{
							continue;
						}
						
						List<String> sysNotiInfo = redis.hmget(CacheKey.SysNoti.servername, CacheKey.SysNoti.keyname + sysNotiVers[notiIndex],
								sysNotiVers[notiIndex], CacheKey.SysNoti.title, CacheKey.SysNoti.type, CacheKey.SysNoti.fullcontent);
						
						ServerHelper.pushSysNoti(ServerConfig.getInstance().getApnsService(), assistantUin, uin, sysNotiInfo.get(0),
								sysNotiInfo.get(2));
					}
					
					redis.hset(CacheKey.UserBaseInfo.servername, CacheKey.UserBaseInfo.keyname + uin, uin,
							CacheKey.UserBaseInfo.sysnotiver, curSysNotiVer + "");
				}
				
				cursorString = scanResult.getCursor();
			}while (!cursorString.equals("0"));
			
			redis.hset(CacheKey.SystemConfig.servername, CacheKey.SystemConfig.keyname, 0, CacheKey.SystemConfig.pushsysnotiver, curSysNotiVer + "");
		}
	}
}
