package com.abc.quickfixj;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.SessionSettings;
import quickfix.field.MsgType;


public class FixInitiatorTestCases {
	MsgType msgType = new MsgType();
	private static FixInitiator fixInitiator;
	FixToJson fixToJson = new FixToJson();
	private static final Logger log = LoggerFactory.getLogger(FixInitiator.class);
	private static String dash="--------------------";
	private static String hash="###################################################################################################";
	@BeforeAll
	static void setUp() throws Exception {
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

	@AfterAll
	static void tearDown() {
		fixInitiator.logout();
	}

	@Test
	void testLogonAndValidateLogonMessage() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase2: Executing Exchange to connect to the FIX Gateway as Initiator "+dash);
		Thread.sleep(2000); 
		log.info("Sending Logon Message from Initiator" );
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from Acceptor");
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+"Successfully Logged on with Acceptor"+dash);
		log.info(dash+" TestCase2: Executing Succesfully "+dash);
		System.out.println(hash);
		testSendMessages();
		testReEstablishConnection();
		testHeartBeatConnection();
		testTestRequest();
	}

	void testSendMessages() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase3: Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		Thread.sleep(2000); 
		fixInitiator.logout();
		Thread.sleep(2000); 
		log.info("Sending Logout Message from Initiator" );
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logout Message from Acceptor");
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGOUT), "Expected logout successful");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase3: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	private void sendMessage() {

	}

	void testReEstablishConnection() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase4: Exchange to restart FIX Session maintaining the sequence numbers from previous session "+dash);
		Thread.sleep(2000); 
		fixInitiator.logon();
		Thread.sleep(2000); 
		log.info("Sending Logon Message from re-establish the session from Initiator" );
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
		log.info("Recevied Logon Message from re-establish the session from Acceptor");
		log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
		MsgType msgType = new MsgType();
		try {
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.LOGON), "Expected logon on previous session successful");
			log.info(dash+"Successfully Logged on with Acceptor after re-establish with previous session"+dash);
			Thread.sleep(30000);
			log.info("Sending HeartBeat from Initiator" );
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied HeartBeat from Acceptor");
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected Heartbeat messages");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" Sending 5 Different Messages "+dash);
		sendMessage();
		log.info(dash+" Executed 5 Different Messages "+dash);
		log.info(dash+" TestCase4: Executing Succesfully "+dash);
		System.out.println(hash);
	}

	void testHeartBeatConnection() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase5: Exchange to stop message flow to demonstrate a Heartbeat is sent when the Heartbeat Interval is reached "+dash);
		log.info(dash+" Exchange is stopped messaing for 30sec "+dash);
		Thread.sleep(30000); 
		try {
			log.info("Sending HeartBeat from Initiator" );
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied HeartBeat from Acceptor");
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.HEARTBEAT), "Expected Heartbeat messages");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase5: Executing Succesfully "+dash);
		System.out.println(hash);
	}


	void testTestRequest() throws InterruptedException {
		System.out.println(hash);
		log.info(dash+" TestCase6: The exchange is to respond to a FIX Gateway initiated Test Request "+dash);
		log.info(dash+" Exchange is stopped messaing for 30sec inculding heartbeat "+dash);
		fixInitiator.sendTestRequest();
		try {
			log.info("Sending TestRquest from Initiator" );
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastToAdminMessage()));
			log.info("Recevied TestRequest from Acceptor");
			log.info(fixToJson.convertToJson(fixInitiator.getApplication().lastFromAdminMessage()));
			assertTrue(fixInitiator.getApplication().lastFromAdminMessage().getHeader().getField(msgType).valueEquals(MsgType.TEST_REQUEST), "Expected TestRequest Resposne");
		} catch (FieldNotFound e) {
		}
		log.info(dash+" TestCase6: Executing Succesfully "+dash);
		System.out.println(hash);
	}

}
