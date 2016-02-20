
package br.com.eai.jms.requestreply;

import java.sql.Connection;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessagingProviderConnection {

	Context					ctx			= null;
	Connection				connection	= null;
	Session					session		= null;
	private static Logger	logger		= LoggerFactory.getLogger(MessagingProviderConnection.class);

	public MessagingProviderConnection() {
		// Properties for environment
		Hashtable environment = new Hashtable();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, Configuration.getContextFactory());
		environment.put(Context.PROVIDER_URL, Configuration.getURL());
		try {
			ctx = new InitialDirContext(environment);
			logger.debug("JDNI Context Found: {}", ctx.getNameInNamespace());
			// Connection Factory
			ConnectionFactory connFactory = (ConnectionFactory) ctx.lookup(Configuration.getConnectionFactoryName());
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