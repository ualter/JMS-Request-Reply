package br.com.eai.jms.requestreply;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Requestor extends Thread implements Runnable {

		private String						replyQueueName;
		private long						interval;
		private String[]					messagesText;
		// Save all messages and its ID sent
		private final Map<String, String>	mySentMessages	= new HashMap<String, String>();
		private Destination					replyQueue;
		private MessagingProviderConnection	mp;
		private static Logger logger = LoggerFactory.getLogger(Requestor.class);

		public Requestor(String name, long interval, String replyQueueName, String... messagesText) {
			super(name);
			this.replyQueueName = replyQueueName;
			this.interval = interval;
			this.messagesText = messagesText;

			mp = new MessagingProviderConnection();
			// Get Queue Reply
			try {
				replyQueue = (Destination) mp.ctx.lookup(replyQueueName);
				logger.debug("Get the queue {} to LISTEN the Replies ", replyQueueName);
			} catch (NamingException e) {
				Utils.logAndThrow(e);
			}
		}

		@Override
		public void run() {
			try {
				logger.info("Connection to Messaging for Send a Request");
				// Get Queue Request
				Destination requestQueue = (Destination) mp.ctx.lookup(Configuration.getRequestQueue());
				logger.debug("Get the queue {} to SEND Requests", Configuration.getRequestQueue());

				// Sending the Messages to the Replier
				int index = 0;
				// Create Message
				TextMessage requestMessage;
				for (String msg : messagesText) {
					requestMessage = mp.session.createTextMessage(msg);
					requestMessage.setJMSReplyTo(replyQueue);
					// Sending Message
					MessageProducer producer = mp.session.createProducer(requestQueue);
					producer.send(requestMessage);
					mp.session.commit();
					logger.info("Sent {} of {} Message \"{}\" to QUEUE {}, waiting response at the QUEUE {}...", ++index, messagesText.length, messagesText,
							Configuration.getRequestQueue(), replyQueueName);
					Utils.logQueueMessage(logger, " MESSAGE REQUEST ", requestMessage);

					mySentMessages.put(requestMessage.getJMSMessageID(), requestMessage.getText());

					Thread.sleep(interval);
				}
				logger.info("All messages sent.");

				// Register our thread listener to receive the replies
				listenerThread.start();

			} catch (NamingException | JMSException | InterruptedException e) {
				Utils.logAndThrow(e);
			} finally {
				mp.close();
			}
		}

		// That's the Thread that will Listening the replies related to our sent
		// requests
		Thread listenerThread = new Thread(() -> {
			final MessagingProviderConnection mpThreadListener = new MessagingProviderConnection();
			logger.info("Start listening to replies...");
			try {
				// Creating Filter to Select only my replies
				StringBuilder filter = new StringBuilder("");
				mySentMessages.keySet().stream().forEach(key -> filter.append("JMSCorrelationID = '").append(key).append("' OR "));
				filter.delete(filter.length() - 4, filter.length());
				filter.append("");

				MessageConsumer messageConsumer = mpThreadListener.session.createConsumer(replyQueue, filter.toString());
				messageConsumer.setMessageListener(message -> {
					try {
						if (message instanceof TextMessage) {
							TextMessage textMessage = (TextMessage) message;
							logger.info("Reply Message Received from Replier");
							Utils.logQueueMessage(logger, " REPLY RECEIVED ", textMessage);
							mpThreadListener.session.commit();
						}
					} catch (JMSException e) {
						Utils.logAndThrow(e);
					}
				});
				logger.info("Listening...");
				while (true)
					Thread.sleep(1000);
			} catch (JMSException | InterruptedException e) {
				Utils.logAndThrow(e);
			} finally {
				mpThreadListener.close();
			}
		} , "RequestorListener");

	}
