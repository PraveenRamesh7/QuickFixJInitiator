package com.abc.quickfixj;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.DataDictionaryProvider;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.FixVersions;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.LogUtil;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.MessageUtils;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ApplVerID;
import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.MsgType;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix50.NewOrderSingle;
import quickfix.fixt11.TestRequest;

public class FixInitiator {
	private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
	private static final Logger log = LoggerFactory.getLogger(FixInitiator.class);
	private static FixInitiator fixInitiator;
	private boolean initiatorStarted = false;
	private static Initiator socketInitiator = null;
	private Application application;
	FixToJson fix2Json = new FixToJson();
	public FixInitiator() throws Exception {
		SessionSettings settings = new SessionSettings("./Initiator.cfg");
		boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true"));
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
		MessageFactory messageFactory = new DefaultMessageFactory();
		application = new Application();
		socketInitiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory,messageFactory);
		JmxExporter exporter = new JmxExporter();
		exporter.register(socketInitiator);
	}

	

	public Application getApplication() {
		return application;
	}



	public void setApplication(Application application) {
		this.application = application;
	}



	public synchronized void logon() {
		if (!initiatorStarted) {
			try {
				socketInitiator.start();
				initiatorStarted = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			for (SessionID sessionId : socketInitiator.getSessions()) {
				Session.lookupSession(sessionId).logon();
			}
		}
	}

	public void logout() {
		for (SessionID sessionId : socketInitiator.getSessions()) {
			Session.lookupSession(sessionId).logout("user requested");
		}
	}

	public void stop() {
		shutdownLatch.countDown();
	}


	public static FixInitiator get() {
		return fixInitiator;
	}

	public static void main(String[] args) throws Exception {
		fixInitiator = new FixInitiator();
		socketInitiator.start();
		fixInitiator.logon();
		Thread.sleep(5000);
		fixInitiator.newOrderSingle();
		Thread.sleep(1000);
		fixInitiator.sendTestRequest();
		shutdownLatch.await();
	}


	private void newOrderSingle() {
		NewOrderSingle orderSingle = new NewOrderSingle(
				new ClOrdID("456"),
				new Side(Side.BUY),
				new TransactTime(),
				new OrdType(OrdType.MARKET));
		orderSingle.set(new HandlInst('3'));
		orderSingle.set(new Symbol("AJCB"));
		orderSingle.set(new OrderQty(10));
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, orderSingle);
			log.info("Order details Sent " + orderSingle.toString());
			log.info("Json Formate" + fix2Json.convertToJson(orderSingle));
		}

	}
  
	private void sendMessage(SessionID sessionID, Message message) {
		try {
			Session session = Session.lookupSession(sessionID);
			if (session == null) {
				throw new SessionNotFound(sessionID.toString());
			}
			DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
			if (dataDictionaryProvider != null) {
				try {
					dataDictionaryProvider.getApplicationDataDictionary(getApplVerID(session, message));//.validate(message, true);
				} catch (Exception e) {
					LogUtil.logThrowable(sessionID, "Outgoing message failed validation: " + e.getMessage(), e);
					return; 
				}
			}
			//session.setNextSenderMsgSeqNum(100);
			session.send(message);
		} catch (SessionNotFound e) {
			log.error(e.getMessage(), e);
		}
	}

	private ApplVerID getApplVerID(Session session, Message message) {
		String beginString = session.getSessionID().getBeginString();
		if (FixVersions.BEGINSTRING_FIXT11.equals(beginString)) {
			return new ApplVerID(ApplVerID.FIX50);
		} else {
			return MessageUtils.toApplVerID(beginString);
		}
	}

	private void sendTestRequest() {
		TestRequest testRequest = new TestRequest();
		testRequest.getHeader().setString(MsgType.FIELD, MsgType.TEST_REQUEST);
		testRequest.set(new quickfix.field.TestReqID("TestReqID123")); // Set TestReqID field
		testRequest.getHeader().setUtcTimeStamp(52, LocalDateTime.now(), true);
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, testRequest);
			log.info("Test Request Sent " + testRequest.toString());
			log.info("Json Formate" + fix2Json.convertToJson(testRequest));
		}
	}

	

}
