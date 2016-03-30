package br.com.eai.jms.requestreply;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replier: This is the Application that answer the requests, is listening to
 * Requestor Applications and delivery the replies for them. Requestor: This is
 * the Application that needs some information from the Replier. It send its
 * request to the Replier Application and waits listening for its answers.
 * 
 * @author Ualter Azambuja Junior
 *
 */
public class JMSRequestReplySample {

	private static Logger	logger					= LoggerFactory.getLogger(JMSRequestReplySample.class);
	private String filePropertiesJMSServer;

	public JMSRequestReplySample(String filePropertiesJMSServer) {
		this.filePropertiesJMSServer = filePropertiesJMSServer;
		Configuration.initConfiguration(this.filePropertiesJMSServer);
	}

	/**
	 * Method to register an Application Requestor (a consumer of the service). This is the application that needs some information of the 
	 * Replier (the application responsible to provide the replies of the service). Here the Requestor register itself sending all the messages that
	 * it needs answer from the application replier. 
	 * 
	 * @param name Name of the Application Requestor (consumer of the service).
	 * @param intervalMessages Interval to send the messages, in case were sent more than one.
	 * @param replyQueueName The Queue that the message replies must be send to, where the application requestor will be listening to get its answering.
	 * @param messagesText A list (or only one) of messages to be sent.
	 */
	public void registerRequestor(String name, long intervalMessages, String replyQueueName, String... messagesText) {
		new Requestor(name, intervalMessages, replyQueueName, messagesText).start();
	}

	/**
	 * Method to register the Application Replier - This is the provider of the service, the application the owns the answers needed for the requestor. <BR>
	 * It listen and gets each one of the message received at the Request Queue, and after the computation, send the message replies to the "Queue Reply" specified by the Application Requestor.
	 * 
	 * @param name Name of the Application Replier
	 */
	public void registerReplier(String name) {
		new Replier(name).start();
	}

	public static void main(String[] args) {
		String filePropertiesJMS = "ActiveMQ.jms.properties";
		if ( args.length > 0 ) {
			filePropertiesJMS = args[0];
		}
		JMSRequestReplySample jmsSample = new JMSRequestReplySample(filePropertiesJMS);
		
		logger.info(Utils.separator());
		logger.info("Starting the requestors");
		jmsSample.registerRequestor("RequestorApp", 1000, Configuration.getReplyQueue() , "(4 + 4) * 2");
		jmsSample.registerRequestor("RequestorApp", 1000, Configuration.getReplyQueue(), "4 + 4 * 2");
		logger.info(Utils.separator());
		
		// Waits 3 secs before start sending requests to the replier 
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Utils.logAndThrow(e);
		}
		// Register the Replier first
		logger.info(Utils.separator());
		logger.info("Start the Replier");
		logger.info(Utils.separator());
		jmsSample.registerReplier("ReplierApp");
		
		//quickTestJMSConnectionSendReceiveQueueHELLO();
	}
	
	@SuppressWarnings({"unchecked","rawtypes"})
	public static void quickTestJMSConnectionSendReceiveQueueHELLO() {
		Hashtable environment = new Hashtable();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		environment.put(Context.PROVIDER_URL, "tcp://localhost:61616");
		
		InitialContext ctx = null;
		Connection connection = null;
		Session session = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			ctx = new InitialDirContext(environment);
			logger.debug("JDNI Context Found: {}", ctx.getNameInNamespace());
			// Connection Factory
			ConnectionFactory connFactory = (ConnectionFactory) ctx.lookup("ConnectionFactory");
			// Connection
			connection = connFactory.createConnection();
			connection.start();
			logger.debug("ConnectionFactory started");
			// Session
			boolean transacted = true;
			session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
			logger.debug("Session with Queue Manager created");
			
			// Get the queue HELLO
			logger.debug("Get the Queue HELLO from Messaging Server");
			Destination queueHELLO = (Queue) ctx.lookup("HELLO");
			
			// Sending Message Test to HELLO
			logger.debug("Sending the message \"Hello World\" to the Queue HELLO");
			TextMessage sentMessage = session.createTextMessage("Hello World");
			producer = session.createProducer(queueHELLO);
			producer.send(sentMessage);
			session.commit();
			
			logger.debug("Consuming the same message sent right before");
			consumer = session.createConsumer(queueHELLO);
			Message msg = consumer.receive(5000);
			if ( msg instanceof TextMessage ) {
				TextMessage receivedMessage = (TextMessage)msg;
				logger.debug("Message received = \"{}\"", receivedMessage.getText());
			} else {
				logger.error("Ops! Was expected to find a Text Message at the queue, it was not!");
			}
			
			logger.debug("Finish!");
			session.commit();
			
		} catch (NamingException | JMSException e) {
			e.printStackTrace();
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
				}
			}
			if (producer != null) {
				try {
					producer.close();
				} catch (JMSException e) {
				}
			}
			if (consumer != null) {
				try {
					consumer.close();
				} catch (JMSException e) {
				}
			}
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
				}
			}
		}
	}

}
