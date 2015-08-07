package cn.imqiba.main;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.imqiba.config.RedisConfig;
import cn.imqiba.config.ServerConfig;
import cn.imqiba.config.RedisConfig.RedisInfo;
import cn.imqiba.util.CacheKey;

public class PushNoti
{
	static
	{
		PropertyConfigurator.configure(System.getProperty("user.dir") + File.separator + "log4j.properties");
	}
	
	private static Logger logger = Logger.getLogger(Class.class);
	
	public static boolean initConfig()
	{
		boolean result = false;
		try
		{
			result = ServerConfig.getInstance().parser(System.getProperty("user.dir") + File.separator + "server_config.xml");
			result = RedisConfig.getInstance().parser(System.getProperty("user.dir") + File.separator + "redis_config.xml");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return result;
		}
		
		return result;
	}
	
	public static void main(String[] args)
	{
		if(!initConfig())
		{
			return;
		}
		
		List<RedisInfo> redisInfoList = RedisConfig.getInstance().GetRedisConfigInfo("user:info");
		if(redisInfoList != null)
		{
			for(RedisInfo redisInfo : redisInfoList)
			{
				new Thread(new OnlinePushWorker(redisInfo.m_stServerID)).start();
			}
		}
		
		int pushQueueCount = RedisConfig.getInstance().GetSubscribeRedisCount("push:noti");
		ExecutorService threadPool = Executors.newFixedThreadPool(pushQueueCount);
		threadPool.execute(new LoginPushWorker());
		
        return;
	}


}
