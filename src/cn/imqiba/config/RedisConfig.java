package cn.imqiba.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import cn.imqiba.util.RedisBank;

public class RedisConfig
{
	public class RedisInfo
	{
		public String m_stServerName = null;
		public int m_stServerID = 0;
		public String m_strServerAddress = null;
		public int m_nServerPort = 0;
		public String m_strChannelKey = null;
		
		public RedisInfo(String serverName, int serverID, String serverAddress, int serverPort, String channelKey)
		{
			m_stServerName = serverName;
			m_stServerID = serverID;
			m_strServerAddress = serverAddress;
			m_nServerPort = serverPort;
			m_strChannelKey = channelKey;
		}
	}
	
	private Logger logger = Logger.getLogger(Class.class);
	private Map<String, List<RedisInfo>> m_stSubscribeKeyMap = new HashMap<String, List<RedisInfo>>();
	private Map<String, List<RedisInfo>> m_stRedisConfigMap = new HashMap<String, List<RedisInfo>>();
	private static RedisConfig m_stInstance = new RedisConfig();
	
	public static RedisConfig getInstance()
	{
		return m_stInstance;
	}
	
	public boolean parser(String path) throws Exception
	{
		SAXReader reader = new SAXReader();
		InputStream stream = null;
		try
		{
			stream = new FileInputStream(path);
		} 
		catch (FileNotFoundException e)
		{
			logger.error( e.getClass().getName()+" : "+ e.getMessage() );
			return false;
		}
		
		try
		{
			Document doc = reader.read(stream);
			//获取根结点
			Element server = doc.getRootElement();
			
			List<?> paramList = server.elements("node");
			for(Object obj : paramList)
			{
				Element param = (Element)obj;
				String serverName = param.attributeValue("server_name");
				int serverID = Integer.parseInt(param.attributeValue("server_id"));
				String serverAddress = param.attributeValue("server_address");
				int serverPort = Integer.parseInt(param.attributeValue("server_port"));
				String channelKey = param.attributeValue("channel_key");
				
				RedisBank.getInstance().addRedisInstance(serverName, serverID, serverAddress, serverPort);
				if(channelKey != null)
				{
					List<RedisInfo> subscribeList = m_stSubscribeKeyMap.get(channelKey);
					if(subscribeList == null)
					{
						subscribeList = new ArrayList<RedisInfo>();
						m_stSubscribeKeyMap.put(channelKey, subscribeList);
					}
					subscribeList.add(new RedisInfo(serverName, serverID, serverAddress, serverPort, channelKey));
				}
				else if(serverName != null)
				{
					List<RedisInfo> redisInfoList = m_stRedisConfigMap.get(serverName);
					if(redisInfoList == null)
					{
						redisInfoList = new ArrayList<RedisInfo>();
						m_stRedisConfigMap.put(serverName, redisInfoList);
					}
					redisInfoList.add(new RedisInfo(serverName, serverID, serverAddress, serverPort, channelKey));
				}
			}
		}
		catch(DocumentException e)
		{
			logger.error( e.getClass().getName()+" : "+ e.getMessage() );
			return false;
		}
		
		return true;
	}
	
	public List<RedisInfo> GetRedisConfigInfo(String serverName)
	{
		return m_stRedisConfigMap.get(serverName);
	}
	
	public int GetSubscribeRedisCount(String channelKey)
	{
		List<RedisInfo> redisInfoList = m_stSubscribeKeyMap.get(channelKey);
		if(redisInfoList == null)
		{
			return 0;
		}
		
		return redisInfoList.size();
	}
}
