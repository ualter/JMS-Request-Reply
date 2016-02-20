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
public class JMSRequestReplySample {

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
	private static Logger	logger					= LoggerFactory.getLogger(JMSRequestReplySample.class);
	private static String	REQUEST_QUEUE			= "REQUEST";
	private static String	REPLY_QUEUE				= "REPLY";

	
	public JMSRequestReplySample() {
	}

	public void registerRequestor(String name, long interval, String replyQueueName, String... messagesText) {
		new Requestor(name, interval, replyQueueName, messagesText).start();
	}

	public void registerReplier(String name) {
		new Replier(name).start();
	}

	public static void main(String[] args) {

		JMSRequestReplySample jmsSample = new JMSRequestReplySample();
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
