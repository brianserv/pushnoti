package cn.imqiba.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;


public class ServerConfig
{
	private Logger logger = Logger.getLogger(Class.class);
	private List<NewUserTips> m_newUserTipsList = new ArrayList<NewUserTips>();
	private ApnsService apnsService = null;
	private String m_secretFilePath = null;
	private String m_secretToken = null;
	
	private static ServerConfig m_serverConfig = new ServerConfig();
	
	private ServerConfig()
	{
	}
	
	public class NewUserTips
	{
		public String m_titleString = null;
		public String m_contentString = null;
	}
	
	public static ServerConfig getInstance()
	{
		return m_serverConfig;
	}
	
	public ApnsService getApnsService()
	{
		if (apnsService == null)
		{
			synchronized (ServerConfig.class)
			{
				if(apnsService == null)
				{
			        apnsService = APNS.newService().withCert(m_secretFilePath, m_secretToken).withSandboxDestination().build();
				}
			}
		}
		
		return apnsService;
    }
	
	public List<NewUserTips> getNewUserTips()
	{
		return m_newUserTipsList;
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
			
			Element newUserTipsElements = server.element("newuser_tips");
			List<Element> tipsList = newUserTipsElements.elements();
			for(Element tipsElement : tipsList)
			{
				NewUserTips newUserTips = new NewUserTips();
				newUserTips.m_titleString = tipsElement.attributeValue("title");
				newUserTips.m_contentString = tipsElement.attributeValue("content");
				m_newUserTipsList.add(newUserTips);
			}
			
			Element apns = server.element("apns");
			m_secretFilePath = apns.attributeValue("file");
			m_secretToken = apns.attributeValue("token");
		}
		catch(DocumentException e)
		{
			logger.error("", e);
			return false;
		}
		
		return true;
	}
}
