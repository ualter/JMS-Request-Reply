package br.com.eai.jms.requestreply;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.print.attribute.standard.Destination;

public class Requestor extends Thread implements Runnable {

		private String						replyQueueName;
		private long						interval;
		private String[]					messagesText;
		// Save all messages and its ID sent
		private final Map<String, String>	mySentMessages	= new HashMap<String, String>();
		private Destination					replyQueue;
		private MessagingProviderConnection	mp;

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
				logAndThrow(e);
			}
		}

		@Override
		public void run() {
			try {
				logger.info("Connection to Messaging for Send a Request");
				// Get Queue Request
				Destination requestQueue = (Destination) mp.ctx.lookup(REQUEST_QUEUE);
				logger.debug("Get the queue {} to SEND Requests", REQUEST_QUEUE);

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
							REQUEST_QUEUE, replyQueueName);
					logger.info("[ ********** MESSAGE REQUEST ********** ]");
					logger.info("[  Message ID:" + requestMessage.getJMSMessageID() + "  ]");
					logger.info("[  Correl. ID:" + requestMessage.getJMSCorrelationID() + "  ]");
					logger.info("[  Reply   To:" + requestMessage.getJMSReplyTo() + "  ]");
					logger.info("[  Contents  :" + requestMessage.getText() + "  ]");
					logger.info("[ ************************************** ]");

					mySentMessages.put(requestMessage.getJMSMessageID(), requestMessage.getText());

					Thread.sleep(interval);
				}
				logger.info("All messages sent.");

				// Register our thread listener to receive the replies
				listenerThread.start();

			} catch (NamingException | JMSException | InterruptedException e) {
				logAndThrow(e);
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
							logger.info("Response Message received");
							logger.info("[ ********** REPLY RECEIVED ********** ]");
							logger.info("[  Message ID:" + textMessage.getJMSMessageID() + "  ]");
							logger.info("[  Correl. ID:" + textMessage.getJMSCorrelationID() + "  ]");
							logger.info("[  Reply   To:" + textMessage.getJMSReplyTo() + "  ]");
							logger.info("[  Contents  :" + textMessage.getText() + "  ]");
							logger.info("[ ************************************** ]");
							mpThreadListener.session.commit();
						}
					} catch (JMSException e) {
						logAndThrow(e);
					}
				});
				logger.info("Listening...");
				while (true)
					Thread.sleep(1000);
			} catch (JMSException | InterruptedException e) {
				logAndThrow(e);
			}
		} , "RequestorListener");

	}