package com.abc.quickfixj;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
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
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.Currency;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.HandlInst;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.ListID;
import quickfix.field.MarketID;
import quickfix.field.MarketSegmentID;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.SecondaryClOrdID;
import quickfix.field.SecondaryOrderID;
import quickfix.field.SecurityID;
import quickfix.field.SecurityReqID;
import quickfix.field.SecurityRequestResult;
import quickfix.field.SecurityResponseID;
import quickfix.field.SecurityResponseType;
import quickfix.field.SecurityStatusReqID;
import quickfix.field.SecurityType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TradeDate;
import quickfix.field.TradeReportID;
import quickfix.field.TradeReportTransType;
import quickfix.field.TradeReportType;
import quickfix.field.TransactTime;
import quickfix.field.TrdRptStatus;
import quickfix.field.TrdType;
import quickfix.fix50.NewOrderSingle;
import quickfix.fix50sp2.DerivativeSecurityList;
import quickfix.fix50sp2.ExecutionReport;
import quickfix.fix50sp2.SecurityDefinition;
import quickfix.fix50sp2.SecurityStatus;
import quickfix.fix50sp2.TradeCaptureReport;
import quickfix.fix50sp2.component.Instrument;
import quickfix.fix50sp2.component.MarketSegmentGrp;
import quickfix.fix50sp2.component.MarketSegmentGrp.NoMarketSegments;
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
		fixInitiator.sendTradeCaptureReport();
		shutdownLatch.await();
	}


	public void newOrderSingle() {
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
			log.info("Json Formate" + fix2Json.fix2Json2(orderSingle));
		}

	}


	public void generateSingleOrder() {

		NewOrderSingle newOrderSingle = new NewOrderSingle();
		try {
			newOrderSingle.set(new ClOrdID(UUID.randomUUID().toString()));
			newOrderSingle.set(new Symbol("BTCEUR"));
			newOrderSingle.set(new Side(Side.BUY));
			newOrderSingle.set(new TransactTime(LocalDateTime.now(ZoneOffset.UTC)));
			newOrderSingle.set(new OrderQty(1));
			newOrderSingle.set(new OrdType(OrdType.LIMIT));
			newOrderSingle.set(new Price(2000));
			newOrderSingle.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
			newOrderSingle.set(new HandlInst('3'));
			for (SessionID sessionId : socketInitiator.getSessions()) {
				Session.lookupSession(sessionId).setNextSenderMsgSeqNum(100);
				Session.sendToTarget(newOrderSingle, sessionId);
			}
		}catch(Exception ex){

		}
	}


	public void sendExecutionReport(Boolean sequencChange,int sequenceNo) {
		try {
			ExecutionReport executionReport = new ExecutionReport();
			executionReport.set(new OrderID("123456789")); 
			executionReport.set(new ExecID("987654321")); 
			executionReport.set(new ExecType(ExecType.FILL)); 
			executionReport.set(new OrdStatus(OrdStatus.FILLED)); 
			executionReport.set(new Symbol("AAPL")); 
			executionReport.set(new Side(Side.BUY)); 
			executionReport.set(new TransactTime(LocalDateTime.now())); 
			executionReport.set(new LastQty(100)); 
			executionReport.set(new LastPx(150.00)); 
			executionReport.set(new LeavesQty(100)); 
			executionReport.set(new CumQty(100)); 
			executionReport.set(new AvgPx(150.00)); 
			executionReport.set(new ClOrdID("12354"));
			for (SessionID sessionId : socketInitiator.getSessions()) {
				if(sequencChange)
					Session.lookupSession(sessionId).setNextSenderMsgSeqNum(sequenceNo);
				sendMessage(sessionId,executionReport);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendExecutionErrorReport(Boolean sequencChange) {
		try {
			ExecutionReport executionReport = new ExecutionReport();
			executionReport.set(new OrderID("123456789")); 
			executionReport.set(new ExecID("987654321")); 
			executionReport.set(new ExecType(ExecType.FILL)); 
			executionReport.setString(OrdStatus.FIELD, "10");
			executionReport.set(new Symbol("AAPL")); 
			executionReport.set(new Side(Side.BUY)); 
			executionReport.set(new TransactTime(LocalDateTime.now())); 
			executionReport.set(new LastQty(100)); 
			executionReport.set(new LastPx(150.00)); 
			executionReport.set(new LeavesQty(100)); 
			executionReport.set(new CumQty(100)); 
			executionReport.set(new AvgPx(150.00)); 
			executionReport.set(new ClOrdID("12354"));
			for (SessionID sessionId : socketInitiator.getSessions()) {
				if(sequencChange)
					Session.lookupSession(sessionId).setNextSenderMsgSeqNum(100);
				sendTestMessage(sessionId,executionReport);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void sendExecutionErrorReport() {
		try {
			ExecutionReport executionReport = new ExecutionReport();
			executionReport.set(new OrderID("123456789")); 
			executionReport.set(new ExecID("987654321")); 
			executionReport.set(new ExecType(ExecType.FILL)); 
			executionReport.set(new OrdStatus(OrdStatus.FILLED)); 
			executionReport.set(new Symbol("AAPL")); 
			//		executionReport.set(new Side(Side.BUY)); 
			executionReport.set(new TransactTime(LocalDateTime.now())); 
			executionReport.set(new LastQty(100)); 
			executionReport.set(new LastPx(150.00)); 
			executionReport.set(new LeavesQty(100)); 
			executionReport.set(new CumQty(100)); 
			executionReport.set(new AvgPx(150.00)); 
			executionReport.set(new ClOrdID("12354"));
			for (SessionID sessionId : socketInitiator.getSessions()) {
				sendTestMessage(sessionId,executionReport);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendTradeCaptureReport() {
		TradeCaptureReport tradeCaptureReport = new TradeCaptureReport();
		tradeCaptureReport.set(new TradeReportID("554564"));
		TradeCaptureReport.NoSides noSidesGroup = new TradeCaptureReport.NoSides();
		tradeCaptureReport.set(new TradeReportTransType(TradeReportTransType.NEW)); // Transaction Type: New
		tradeCaptureReport.set(new TradeReportType(TradeReportType.SUBMIT)); 
		tradeCaptureReport.set(new TrdType(TrdType.REGULAR_TRADE)); 
		tradeCaptureReport.set(new Symbol("AAPL")); 
		tradeCaptureReport.set(new LastQty(100)); 
		tradeCaptureReport.set(new LastPx(150.00)); 
		tradeCaptureReport.set(new TradeDate(LocalDateTime.now().toString())); 
		tradeCaptureReport.set(new TransactTime(LocalDateTime.now())); 
		tradeCaptureReport.set(new ExecID("987654321")); 
		tradeCaptureReport.set(new ExecType(ExecType.FILL)); 
		tradeCaptureReport.set(new SecurityID("123456"));
		tradeCaptureReport.set(new quickfix.field.SecurityStatus("1"));
		tradeCaptureReport.set(new TrdRptStatus(TrdRptStatus.ACCEPTED));
		Instrument instrument = new Instrument();
		instrument.set(new Symbol("BTC"));
		tradeCaptureReport.set(instrument);
		noSidesGroup.set(new Side(Side.BUY));
		noSidesGroup.set(new OrderID("656443"));
		noSidesGroup.set(new SecondaryOrderID("656465"));
		noSidesGroup.set(new ClOrdID("766544"));
		noSidesGroup.set(new SecondaryClOrdID("4354654"));
		noSidesGroup.set(new ListID("74564"));
		tradeCaptureReport.addGroup(noSidesGroup);
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, tradeCaptureReport);
		}
	}

	public void sendSecurityDefinition() {
		SecurityDefinition securityDefinition = new SecurityDefinition();
		securityDefinition.set(new SecurityID("SEC123")); 
		securityDefinition.set(new SecurityReqID("123")); 
		securityDefinition.set(new SecurityResponseID("456")); 
		securityDefinition.set(new SecurityResponseType(SecurityResponseType.REJECT_SECURITY_PROPOSAL)); 
		securityDefinition.set(new quickfix.fix50sp2.component.Instrument()); 
		securityDefinition.set(new Symbol("AAPL")); 
		securityDefinition.set(new SecurityType(SecurityType.COMMON_STOCK)); 
		securityDefinition.set(new Currency("Euro")); 
		securityDefinition.set(new quickfix.field.SecurityDesc("Apple Inc.")); 
		Instrument instrument = new Instrument();
		instrument.set(new Symbol("BTC"));
		instrument.set(new SecurityType(SecurityType.BUY_SELLBACK));
		securityDefinition.set(instrument);
		//		MarketSegmentGrp grp = new MarketSegmentGrp();
		//		NoMarketSegments noMarketSegments = new NoMarketSegments();
		//		noMarketSegments.set(new MarketID("MRK1234"));
		//		noMarketSegments.set(new MarketSegmentID("MRKS1234"));
		//		grp.addGroup(noMarketSegments);
		//		securityDefinition.addGroup(noMarketSegments);
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, securityDefinition);
		}
	}

	public void sendDerivativeSecurityList() {
		DerivativeSecurityList derivativeSecurityList = new DerivativeSecurityList();
		derivativeSecurityList.set(new SecurityReqID("123")); 
		derivativeSecurityList.set(new SecurityResponseID("456")); 
		derivativeSecurityList.set(new SecurityRequestResult(0)); 
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, derivativeSecurityList);
		}
	}

	public void sendSecurityStatus() {
		SecurityStatus securityStatus = new SecurityStatus();
		securityStatus.set(new SecurityStatusReqID("123")); 
		securityStatus.set(new quickfix.field.SecurityStatus("1")); 
		securityStatus.set(new TransactTime(LocalDateTime.now())); 
		securityStatus.set(new Symbol("AAPL")); 
		securityStatus.set(new SecurityID("123456")); 
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendMessage(sessionId, securityStatus);
		}
	}

	public void sendTestRequest() {
		TestRequest testRequest = new TestRequest();
		testRequest.getHeader().setString(MsgType.FIELD, MsgType.TEST_REQUEST);
		testRequest.set(new quickfix.field.TestReqID("TestReqID123")); // Set TestReqID field
		testRequest.getHeader().setUtcTimeStamp(52, LocalDateTime.now(), true);
		for (SessionID sessionId : socketInitiator.getSessions()) {
			sendTestMessage(sessionId, testRequest);
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
					dataDictionaryProvider.getApplicationDataDictionary(
							getApplVerID(session, message)).validate(message, true);
				} catch (Exception e) {
					LogUtil.logThrowable(sessionID, "Outgoing message failed validation: "
							+ e.getMessage(), e);
					return;
				}
			}

			session.send(message);
		} catch (SessionNotFound e) {
			log.error(e.getMessage(), e);
		}
	}
	private void sendTestMessage(SessionID sessionID, Message message) {
		try {
			Session session = Session.lookupSession(sessionID);
			if (session == null) {
				throw new SessionNotFound(sessionID.toString());
			}

			DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
			if (dataDictionaryProvider != null) {
				try {
					dataDictionaryProvider.getApplicationDataDictionary(
							getApplVerID(session, message));//.validate(message, true);
				} catch (Exception e) {
					LogUtil.logThrowable(sessionID, "Outgoing message failed validation: "
							+ e.getMessage(), e);
					return;
				}
			}

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



}
