package br.com.eai.jms.requestreply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
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
public class JMSRequestReplySampleORIGINAL {

	/**
	 * The RefFSContextFactory used here refers to the JMS IBM MQ Implementation
	 * In order to working and use this Connection Factory you'll have to have
	 * this following JARs from IBM MQ Client (JMS Impl.) in your classpath.
	 * <br>
	 * Here is the Maven style of local jars needed:
	 * 
	 * <pre>
	 *       &lt;dependency&gt;
	 *          &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *          &lt;artifactId&gt;fscontext&lt;/artifactId&gt;
	 *          &lt;version&gt;1.0&lt;/version&gt;
	 *          &lt;scope&gt;system&lt;/scope&gt;
	 *          &lt;systemPath&gt;${project.basedir}/lib/fscontext.jar&lt;/systemPath&gt;
	 *       &lt;/dependency&gt;
	 *       &lt;dependency&gt;
	 *            &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *            &lt;artifactId&gt;providerutil&lt;/artifactId&gt;
	 *            &lt;version&gt;1.0&lt;/version&gt;
	 *            &lt;scope&gt;system&lt;/scope&gt;
	 *            &lt;systemPath&gt;${project.basedir}/lib/providerutil.jar&lt;/systemPath&gt;
	 *        &lt;/dependency&gt;
	 *        &lt;dependency&gt;
	 *            &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *            &lt;artifactId&gt;com.ibm.mqjms&lt;/artifactId&gt;
	 *            &lt;version&gt;1.0&lt;/version&gt;
	 *            &lt;scope&gt;system&lt;/scope&gt;
	 *            &lt;systemPath&gt;${project.basedir}/lib/com.ibm.mqjms.jar&lt;/systemPath&gt;
	 *        &lt;/dependency&gt;
	 *        &lt;dependency&gt;
	 *            &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *            &lt;artifactId&gt;com.ibm.mq.jmqi&lt;/artifactId&gt;
	 *            &lt;version&gt;1.0&lt;/version&gt;
	 *            &lt;scope&gt;system&lt;/scope&gt;
	 *            &lt;systemPath&gt;${project.basedir}/lib/com.ibm.mq.jmqi.jar&lt;/systemPath&gt;
	 *        &lt;/dependency&gt;
	 *        &lt;dependency&gt;
	 *            &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *            &lt;artifactId&gt;dhbcore&lt;/artifactId&gt;
	 *            &lt;version&gt;1.0&lt;/version&gt;
	 *            &lt;scope&gt;system&lt;/scope&gt;
	 *            &lt;systemPath&gt;${project.basedir}/lib/dhbcore.jar&lt;/systemPath&gt;
	 *        &lt;/dependency&gt;
	 *         &lt;dependency&gt;
	 *            &lt;groupId&gt;ibm&lt;/groupId&gt;
	 *            &lt;artifactId&gt;com.ibm.mq.headers&lt;/artifactId&gt;
	 *            &lt;version&gt;1.0&lt;/version&gt;
	 *            &lt;scope&gt;system&lt;/scope&gt;
	 *            &lt;systemPath&gt;${project.basedir}/lib/com.ibm.mq.headers.jar&lt;/systemPath&gt;
	 *        &lt;/dependency&gt;
	 * </pre>
	 */
	private static String	contextFactory			= "com.sun.jndi.fscontext.RefFSContextFactory";
	/**
	 * This URL refers actually to a binding made throughout a File created by
	 * MQ Explorer (IBM MQ Tool)<br>
	 * In order to be able to access the JMS Objects it's necessary have this
	 * file locally <br>
	 * (This is the case you don't have the JMS objects registered in a JNDI
	 * Server Repository)
	 */
	private static String	url						= "file:/C:/Users/talent.uazambuja/Developer/JMS_MQ_Objects";
	private static String	connectionFactoryName	= "UalterLocalConnectionFactory";
	private static Logger	logger					= LoggerFactory.getLogger(JMSRequestReplySampleORIGINAL.class);
	private static String	REQUEST_QUEUE			= "REQUEST";
	private static String	REPLY_QUEUE				= "REPLY";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static class MessagingProviderConnection {

		Context		ctx			= null;
		Connection	connection	= null;
		Session		session		= null;

		public MessagingProviderConnection() {
			// Properties for environment
			Hashtable environment = new Hashtable();
			environment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
			environment.put(Context.PROVIDER_URL, url);
			try {
				ctx = new InitialDirContext(environment);
				logger.debug("JDNI Context Found: {}", ctx.getNameInNamespace());
				// Connection Factory
				ConnectionFactory connFactory = (ConnectionFactory) ctx.lookup(connectionFactoryName);
				// Connection
				connection = connFactory.createConnection();
				connection.start();
				logger.debug("ConnectionFactory started");
				// Session
				boolean transacted = true;
				session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
				logger.debug("Session with Queue Manager created");
			} catch (NamingException | JMSException e) {
				logAndThrow(e);
			}
		}

		public void close() {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					logAndThrow(e);
				}
			}
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
					logAndThrow(e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					logAndThrow(e);
				}
			}
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			this.close();
		}

	}

	public JMSRequestReplySampleORIGINAL() {
	}

	public void registerRequestor(String name, long interval, String replyQueueName, String... messagesText) {
		new Requestor(name, interval, replyQueueName, messagesText).start();
	}

	public void registerReplier(String name) {
		new Replier(name).start();
	}

	/**
	 * 
	 * @author Ualter Azambuja
	 */
	public static class Replier extends Thread implements Runnable {
		public Replier(String name) {
			super(name);
		}
		
		public void run() {
			MessagingProviderConnection mp = new MessagingProviderConnection();
			try {
				logger.info("Configuring the Listener to Messaging Server");
				// Get Queue Request
				Destination requestQueue = (Destination) mp.ctx.lookup(REQUEST_QUEUE);
				logger.debug("Listen on the QUEUE: {}", REQUEST_QUEUE);
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
							logger.info("[ ********** MESSAGE RECEIVED ********** ]");
							logger.info("[  Message ID:" + requestTextMessage.getJMSMessageID() + "  ]");
							logger.info("[  Correl. ID:" + requestTextMessage.getJMSCorrelationID() + "  ]");
							logger.info("[  Reply   To:" + requestTextMessage.getJMSReplyTo() + "  ]");
							logger.info("[  Contents  :" + requestTextMessage.getText() + "  ]");
							logger.info("[ ************************************** ]");

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
							if (requestMessage.getJMSReplyTo() == null) logger.error("Message received does not have the replyTo configured");
							logger.error("[ ***** ERROR MESSAGE RECEIVED ***** ]" + Thread.currentThread().getName());
							logger.error("[  Message ID:" + requestMessage.getJMSMessageID() + "  ]");
							logger.error("[  Correl. ID:" + requestMessage.getJMSCorrelationID() + "  ]");
							logger.error("[  Reply   To:" + requestMessage.getJMSReplyTo() + "  ]");
							logger.error("[ ************************************** ]");
							// Put the this Message to a ERROR queue, for instance.
							// - To do :-)
						}
					} catch (JMSException e) {
						logAndThrow(e);
					}
				});
				logger.info("Listening...");
				while (true)
					Thread.sleep(1000);
			} catch (NamingException | JMSException e) {
				logAndThrow(e);
			} catch (InterruptedException e) {
				logAndThrow(e);
			} finally {
				try {
					mp.session.commit();
				} catch (JMSException e) {
					logAndThrow(e);
				} finally {
					mp.close();
				}
				mp.close();
			}
		}
	}


	public static class Requestor extends Thread implements Runnable {

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

	public static Long processMathEquation(String eq) {
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

	public static void main(String[] args) {

		JMSRequestReplySampleORIGINAL jmsSample = new JMSRequestReplySampleORIGINAL();
		jmsSample.registerRequestor("RequestorApp", 1000, REPLY_QUEUE, "4 + 4 * 2");
		jmsSample.registerRequestor("RequestorApp", 1000, REPLY_QUEUE, "3 + 2");
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logAndThrow(e);
		}
		
		jmsSample.registerReplier("ReplierApp");

	}

	private static void logAndThrow(Exception e) {
		logger.error(e.getMessage(), e);
		throw new RuntimeException(e.getMessage(), e);
	}

}
