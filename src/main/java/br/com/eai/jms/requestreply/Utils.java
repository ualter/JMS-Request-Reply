
package br.com.eai.jms.requestreply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static Logger logger = LoggerFactory.getLogger(JMSRequestReplySample.class);

	@SuppressWarnings("unused")
	public static void logAndThrow(Exception e) {
		logger.error(e.getMessage(), e);
		throw new RuntimeException(e.getMessage(), e);
	}

}
