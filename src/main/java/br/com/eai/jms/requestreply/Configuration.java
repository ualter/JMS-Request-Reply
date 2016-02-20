
package br.com.eai.jms.requestreply;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private org.apache.commons.configuration.Configuration	config;
	private static Configuration							me;
	private static Logger									logger					= LoggerFactory.getLogger(Configuration.class);

	public static String									URL						= "url";
	public static String									CONNECTION_FACTORY_NAME	= "connectionFactoryName";
	public static String									REQUEST_QUEUE			= "REQUEST_QUEUE";
	public static String									REPLY_QUEUE				= "REPLY_QUEUE";
	public static String									CONTEXT_FACTORY			= "contextFactory";

	static {
		me = new Configuration();
	}

	private Configuration() {
		try {
			this.config = new PropertiesConfiguration("jms.properties");
		} catch (ConfigurationException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Configuration getInstance() {
		return me;
	}

	public String getString(String key) {
		return this.config.getString(key);
	}

	public static String getURL() {
		return me.getString(URL);
	}

	public static String getConnectionFactoryName() {
		return me.getString(CONNECTION_FACTORY_NAME);
	}

	public static String getRequestQueue() {
		return me.getString(REQUEST_QUEUE);
	}

	public static String getReplyQueue() {
		return me.getString(REPLY_QUEUE);
	}

	public static String getContextFactory() {
		return me.getString(CONTEXT_FACTORY);
	}

}
