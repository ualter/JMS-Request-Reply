
package br.com.eai.jms.requestreply;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Ualter Azambuja
 */
public class Replier extends Thread implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(Replier.class);

	public Replier(String name) {
		super(name);
	}

	public void run() {
		MessagingProviderConnection mp = new MessagingProviderConnection();
		try {
			logger.info("Configuring the Listener to Messaging Server");
			// Get Queue Request
			Destination requestQueue = (Destination) mp.ctx.lookup(Configuration.getRequestQueue());
			logger.debug("Listen on the QUEUE: {}", Configuration.getRequestQueue());
			// Create replier for listener
			MessageConsumer consumer = mp.session.createConsumer(requestQueue);
			consumer.setMessageListener(requestMessage -> {
				try {
					if ((requestMessage instanceof TextMessage) && requestMessage.getJMSReplyTo() != null) {
						/**
						 * The message is a TEXT and send the ReplyTo QUEUE to
						 * "reply to" ;-)
						 */
						TextMessage requestTextMessage = (TextMessage) requestMessage;
						Utils.logQueueMessage(logger, " MESSAGE RECEIVED ", requestTextMessage);

						/**
						 * Sending the Response
						 */
						// Get Queue for the Reply
						Long result = processMathEquation(requestTextMessage.getText());
						logger.debug("Answer to the queue: {}", requestMessage.getJMSReplyTo());
						MessageProducer producer = mp.session.createProducer(requestMessage.getJMSReplyTo());
						// Composing the Text reply message
						String response = "Ok! Here is the result: \"" + requestTextMessage.getText() + " = " + result + "\"";
						TextMessage replyMessage = mp.session.createTextMessage(response);
						replyMessage.setJMSCorrelationID(requestMessage.getJMSMessageID());
						producer.send(replyMessage);
						logger.info("Reply Message Sent to the queue: {}", requestMessage.getJMSReplyTo());
						mp.session.commit();
					} else {
						/**
						 * Ops... We receive a message to reply that there's no
						 * ReplyTo, so sorry, just put aside in a error QUEUE
						 */
						if (requestMessage.getJMSReplyTo() == null)
							logger.error("Message received does not have the replyTo configured");
						Utils.logQueueMessage(logger, " MESSAGE RECEIVED ", requestMessage);
						// Put the this Message to a ERROR queue, for instance.
						// - To do :-)
					}
				} catch (JMSException e) {
					Utils.logAndThrow(e);
				}
			});
			logger.info("Listening...");
			while (true)
				Thread.sleep(1000);
		} catch (NamingException | JMSException e) {
			Utils.logAndThrow(e);
		} catch (InterruptedException e) {
			Utils.logAndThrow(e);
		} finally {
			try {
				mp.session.commit();
			} catch (JMSException e) {
				Utils.logAndThrow(e);
			} finally {
				mp.close();
			}
			mp.close();
		}
	}

	public Long processMathEquation(String eq) {
		long result = 0;
		Matcher matchNumbers = Pattern.compile("[0-9]").matcher(eq);
		Matcher matchOperators = Pattern.compile("(\\+|-|\\*|/)").matcher(eq);
		List<Integer> numbers = new ArrayList<Integer>();
		List<String> operators = new ArrayList<String>();
		while (matchNumbers.find()) {
			numbers.add(Integer.parseInt(matchNumbers.group()));
		}
		while (matchOperators.find()) {
			operators.add(matchOperators.group());
		}
		int indexOperator = 0;
		boolean startOperation = false;
		for (Integer number : numbers) {
			if (startOperation) {
				String operator = operators.get(indexOperator++);
				switch (operator) {
					case "+":
						result = (result + number);
						break;
					case "-":
						result = (result - number);
						break;
					case "*":
						result = (result * number);
						break;
					case "/":
						result = (result / number);
						break;
					default:
						break;
				}
			} else {
				result = number;
				startOperation = true;
			}
		}
		return result;
	}
}
