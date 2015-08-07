package cn.imqiba.main;

import java.util.List;

import org.apache.log4j.Logger;

import cn.imqiba.config.RedisConfig;
import cn.imqiba.config.ServerConfig;
import cn.imqiba.config.ServerConfig.NewUserTips;
import cn.imqiba.util.CacheKey;
import cn.imqiba.util.RedisAgent;
import cn.imqiba.util.ServerHelper;

public class LoginPushWorker implements Runnable
{
	private Logger logger = Logger.getLogger(Class.class);
	@Override
	public void run()
	{
		List<NewUserTips> newUserTipsList = ServerConfig.getInstance().getNewUserTips();
		
		RedisAgent redis = new RedisAgent();
		String assistantUinString = redis.hget(CacheKey.SysUser.servername, CacheKey.SysUser.keyname, 0, CacheKey.SysUser.assistant);
		int assistantUin = Integer.parseInt(assistantUinString);
		
		while(true)
		{
//			long curTime = System.currentTimeMillis() / 1000;
			
			int sysNotiCount = 0;
			int[] sysNotiVers = new int[10];
			
			List<String> sysConfigList = redis.hmget(CacheKey.SystemConfig.servername, CacheKey.SystemConfig.keyname, 0,
					CacheKey.SystemConfig.cursysnotiver, CacheKey.SystemConfig.sysnotiset);
			
			int curSysNotiVer = Integer.parseInt(sysConfigList.get(0));
			String[] sysNotiVerStrings = sysConfigList.get(1).split(":", 11);
			
			for(String sysNotiVerString : sysNotiVerStrings)
			{
				sysNotiVers[sysNotiCount++] = Integer.parseInt(sysNotiVerString);
			}
			
			int redisCount = RedisConfig.getInstance().GetSubscribeRedisCount("push:noti");
			for(int i = 0; i < redisCount; ++i)
			{
				List<String> data = redis.blpop("queue", i, 10, "push:noti");
				
				try
				{
					if(data == null)
					{
						continue;
					}
					
					String[] params = data.get(1).split(":", 2);
					if(params == null || params.length == 0)
					{
						continue;
					}
					
					String action = params[0];
					int uin = Integer.parseInt(params[1]);
					if(action.equals("regist"))
					{
						for(NewUserTips newUserTips : newUserTipsList)
						{
							ServerHelper.pushSysNoti(ServerConfig.getInstance().getApnsService(), assistantUin, uin, newUserTips.m_titleString,
									newUserTips.m_contentString);
						}
					}
					else if(action.equals("login"))
					{
						List<String> userBaseInfoList = redis.hmget(CacheKey.UserBaseInfo.servername, CacheKey.UserBaseInfo.keyname + uin, uin,
								CacheKey.UserBaseInfo.sysnotiver, CacheKey.UserBaseInfo.phonetype);
						
						int sysNotiVer = 0;
						if(userBaseInfoList.get(0) != null)
						{
							sysNotiVer = Integer.parseInt(userBaseInfoList.get(0));
						}
						
						int phoneType = Integer.parseInt(userBaseInfoList.get(1));
						
						if(sysNotiVer >= curSysNotiVer)
						{
							continue;
						}
						
						for(int notiIndex = 0; notiIndex < sysNotiCount; ++notiIndex)
						{
							if(sysNotiVers[notiIndex] <= sysNotiVer)
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
				}
				catch (Exception e)
				{
					logger.error("", e);
				}
			}
		}
	}
}
