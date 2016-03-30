
package br.com.eai.jms.requestreply;

import java.util.Optional;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	@SuppressWarnings("unused")
	public static void logAndThrow(Exception e) {
		logger.error(e.getMessage(), e);
		throw new RuntimeException(e.getMessage(), e);
	}
	
	public static String separator() {
		return StringUtils.repeat("-", 50);
	}
	
	public static void logQueueMessage(Logger l, String title, Message msg) throws JMSException {
		int lineSize = 60;
		Optional<String> correlationId = Optional.ofNullable(msg.getJMSCorrelationID());
		Optional<Destination> replyTo  = Optional.ofNullable(msg.getJMSReplyTo());
		
		l.info("[ /{}\\ ]", StringUtils.center(title,lineSize + 14,"*") );
		l.info("[ | Message ID: {} | ]", StringUtils.rightPad(msg.getJMSMessageID(), lineSize, " ")  );
		l.info("[ | Correl. ID: {} | ]", StringUtils.rightPad(correlationId.orElseGet(() -> "[NULL]") , lineSize, " ")  );
		l.info("[ | Reply   To: {} | ]", StringUtils.rightPad(replyTo.map(Destination::toString).orElse("[NULL]") , lineSize, " ")  );
		if (msg instanceof TextMessage) {
			l.info("[ | Contents  : {} | ]", StringUtils.rightPad(((TextMessage)msg).getText(), lineSize, " "));
		}
		l.info("[ \\\\{}/ ]", StringUtils.rightPad("*", lineSize + 14, "*")  );
	}

}
