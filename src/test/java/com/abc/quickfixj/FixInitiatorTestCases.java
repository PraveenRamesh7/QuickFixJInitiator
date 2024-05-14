package com.abc.quickfixj;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.SessionSettings;
import quickfix.field.MsgType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FixInitiatorTestCases {
	MsgType msgType = new MsgType();
	private static FixInitiator fixInitiator;
	FixToJson fixToJson = new FixToJson();
	private static final Logger log = LoggerFactory.getLogger(FixInitiator.class);
	private static String dash="--------------------";
	private static String hash="###################################################################################################";

	@AfterAll
	static void tearDown() {
		fixInitiator.logout();
	}

	@Test
	@Order(1)
	void testBasicConnectivity() throws Exception{
		System.out.println(hash);
		log.info(dash+" TestCase1: Executing Basic Connectivity Test "+dash);
		fixInitiator = new FixInitiator();
		fixInitiator.logon();
		Thread.sleep(2000); 
		if(fixInitiator.getApplication().getCreateSessions()!=null) {
			log.info("Created Session from Intiator to Acceptor " + fixInitiator.getApplication().getCreateSessions());
		}
		log.info(dash+" TestCase1: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(2)
	void testLogonMessages() throws InterruptedException{
		System.out.println(hash);
		log.info(dash+" TestCase2: Executing Exchange to connect to the FIX Gateway as Initiator "+dash);
		Thread.sleep(2000); 
		log.info("Sending Logon Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+"Successfully Logged on with Acceptor"+dash);
		log.info(dash+" TestCase2: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(3)
	void testSendMessages() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase3: Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		logoutTest();
		log.info(dash+" TestCase3: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(4)
	void testReEstablishConnection() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase4: Exchange to restart FIX Session maintaining the sequence numbers from previous session "+dash);
		Thread.sleep(2000); 
		fixInitiator.logon();
		Thread.sleep(2000); 
		log.info("Sending Logon Message from re-establish the session from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from re-establish the session from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon on previous session successful");
			log.info(dash+"Successfully Logged on with Acceptor after re-establish with previous session"+dash);
			Thread.sleep(30000);
			log.info("Sending HeartBeat from Initiator" );
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
			Thread.sleep(1000);
			log.info("Recevied HeartBeat from Acceptor");
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected Heartbeat messages");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		log.info(dash+" TestCase4: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(5)
	void testHeartBeatConnection() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase5: Exchange to stop message flow to demonstrate a Heartbeat is sent when the Heartbeat Interval is reached "+dash);
		log.info(dash+" Exchange is stopped messaing for 30sec "+dash);
		Thread.sleep(30000); 
		try {
			log.info("Sending HeartBeat from Initiator" );
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied HeartBeat from Acceptor");
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected Heartbeat messages");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase5: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(6)
	void testTestRequest() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase6: The exchange is to respond to a FIX Gateway initiated Test Request "+dash);
		log.info(dash+" Exchange is stopped messaing for 30sec inculding heartbeat "+dash);
		fixInitiator.sendTestRequest();
		try {
			log.info("Sending TestRquest from Initiator" );
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied TestRequest from Acceptor");
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected TestRequest Resposne");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase6: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(7)
	void testResendRequest() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase7: The FIX Gateway is to send a Resend Request because a sequence number gap is detected. The exchange is to respond in Gap Fill Mode. "+dash);
		fixInitiator.sendExecutionReport(true,100);
		log.info("Sending New Order Single from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		Thread.sleep(2000);
		log.info("Recevied Resend Request from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		log.info("Sending Reset Sequence from Initiator");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		//assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.EXECUTION_REPORT), "Expected Execution Report Resposne");
		log.info(dash+" TestCase7: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(8)
	void testSendMessage() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase8: Exchange to send an invalid message to the FIX Gateway. The message is rejected by the FIX Gateway and the exchange resends the corrected message "+dash);
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		executionErrorReportMessage();
		Thread.sleep(2000);
		executionReportMessage();
		Thread.sleep(2000);
		log.info(dash+" TestCase8: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(9)
	void testRejectSendMessage() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase9: Exchange to send an invalid message to the FIX Gateway. The message is rejected by the FIX Gateway and the exchange resends the corrected message "+dash);
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		executionRejectReportMessage();
		Thread.sleep(2000);
		executionReportMessage();
		Thread.sleep(2000);
		log.info(dash+" TestCase9: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(10)
	void testResetSequenceto1Message() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase10: The exchange resets the sequence number to 1 after terminating the session and re-establishing a new session. "+dash);
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		logoutTest();
		Thread.sleep(2000);
		fixInitiator.logon();
		Thread.sleep(2000); 
		log.info("Sending Logon Message with reset the sequence number to 1 from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+"Successfully Logged on with sequence number to 1 from Acceptor"+dash);
		log.info(dash+" Sending 5 Different Messages after resets the sequence "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages after resets the sequence "+dash);
		log.info(dash+" TestCase10: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(11)
	void testResetSequence24HrsMessage() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase11: The exchange resets the sequence number is to 1. (For markets running 24 hour connectivity) "+dash);
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		Thread.sleep(2000);
		fixInitiator.logout();
		Thread.sleep(2000);
		fixInitiator.logon();
		Thread.sleep(2000); 
		log.info("Sending Logon Message with reset the sequence number to 1 from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+"Successfully Logged on with sequence number to 1 from Acceptor"+dash);
		log.info(dash+" Sending 5 Different Messages after resets the sequence "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages after resets the sequence "+dash);
		log.info(dash+" TestCase11: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(12)
	void testSessionRecoveryMessage() throws InterruptedException, FieldNotFound {
		System.out.println(hash);
		log.info(dash+" TestCase12: The exchange performs a session recovery after sending a sequence number that is less than the sequence number expected "+dash);
		fixInitiator.sendExecutionReport(true,1);
		log.info("Sending Execution Report Message with lower seqno from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		Thread.sleep(1000);
		log.info("Recevied Execution Report Error Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		Thread.sleep(2000);
		fixInitiator.logon();
		Thread.sleep(2000); 
		log.info("Sending Logon Message with reset the sequence number to 1 from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+"Successfully Logged on with sequence number to 1 from Acceptor"+dash);
		log.info(dash+" Sending 5 Different Messages after resets the sequence "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages after resets the sequence "+dash);
		log.info(dash+" TestCase12: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(13)
	void testExchangeTestRequest() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase13: The exchange initiates a Test Request "+dash);
		fixInitiator.sendTestRequest();
		Thread.sleep(1000);
		try {
			log.info("Sending TestRquest from Initiator" );
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied HeartBeat from Acceptor");
			log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected TestRequest Resposne");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase13: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	@Test
	@Order(14)
	void testLogout() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase14: The exchange initiates a termination of the session "+dash);
		logoutTest();
		log.info(dash+" TestCase14: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	private void executionErrorReportMessage() throws InterruptedException {
		fixInitiator.sendExecutionErrorReport(false);
		Thread.sleep(2000);
		log.info("Sending Execution Report Error Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Execution Report Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.REJECT), "Expected Execution Report Rejected Message successful");
		} catch (FieldNotFound e) {
		}

	}

	private void executionRejectReportMessage() throws InterruptedException {
		fixInitiator.sendExecutionErrorReport();
		Thread.sleep(2000);
		log.info("Sending Execution Report without tag 54(side) Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Execution Report Rejected Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.REJECT), "Expected Execution Report Rejected Message successful");
		} catch (FieldNotFound e) {
		}

	}

	private void sendMessage() throws InterruptedException {
		executionReportMessage();
		Thread.sleep(2000);
		tradeCaptureReportMessage();
		Thread.sleep(2000);
		securityDefinitionMessage();
		Thread.sleep(2000);
		derivativeSecurityListMessage();
		Thread.sleep(2000);
		securityStatusMessage();
		Thread.sleep(2000);

	}

	private void securityStatusMessage() throws InterruptedException{
		fixInitiator.sendSecurityStatus();
		Thread.sleep(2000);
		log.info("Sending Security Status Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Security Status Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAppMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAppMessage().getHeader().getField(msgType).valueEquals(MsgType.SECURITY_STATUS), "Expected Security Status Ack successful");
		} catch (FieldNotFound e) {
		}

	}

	private void derivativeSecurityListMessage() throws InterruptedException{
		fixInitiator.sendDerivativeSecurityList();
		Thread.sleep(2000);
		log.info("Sending DerivativeSecurityList Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied DerivativeSecurityList Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAppMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAppMessage().getHeader().getField(msgType).valueEquals(MsgType.SECURITY_LIST_UPDATE_REPORT), "Expected DerivativeSecurityList Ack successful");
		} catch (FieldNotFound e) {
		}

	}

	private void securityDefinitionMessage() throws InterruptedException{
		fixInitiator.sendSecurityDefinition();
		Thread.sleep(2000);
		log.info("Sending Security Definition Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Security Definition Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAppMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAppMessage().getHeader().getField(msgType).valueEquals(MsgType.SECURITY_DEFINITION_UPDATE_REPORT), "Expected Security Definition Ack successful");
		} catch (FieldNotFound e) {
		}

	}

	private void executionReportMessage() throws InterruptedException {
		fixInitiator.sendExecutionReport(false,0);
		Thread.sleep(2000);
		log.info("Sending Execution Report Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Execution Report Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAppMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAppMessage().getHeader().getField(msgType).valueEquals(MsgType.EXECUTION_ACKNOWLEDGEMENT), "Expected Execution Report Ack successful");
		} catch (FieldNotFound e) {
		}
	}

	private void tradeCaptureReportMessage() throws InterruptedException {
		fixInitiator.sendTradeCaptureReport();
		Thread.sleep(2000);
		log.info("Sending Trade Capture Report Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAppMessage()));
		log.info("Recevied Trade Capture Report Ack Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAppMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAppMessage().getHeader().getField(msgType).valueEquals(MsgType.TRADE_CAPTURE_REPORT_ACK), "Expected Trade Capture Report Ack successful");
		} catch (FieldNotFound e) {
		}
	}

	private void logoutTest() throws InterruptedException{
		Thread.sleep(2000); 
		fixInitiator.logout();
		Thread.sleep(2000); 
		log.info("Sending Logout Message from Initiator" );
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logout Message from Acceptor");
		log.info(fixToJson.fix2Json2(fixInitiator.getApplication().lastFromAdminMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGOUT), "Expected logout successful");
		} catch (FieldNotFound e) {
		}
	}

}
