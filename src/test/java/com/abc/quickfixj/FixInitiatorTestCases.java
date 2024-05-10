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
	private static FixInitiator fixInitiator;
	FixToJson fixToJson = new FixToJson();
	private static final Logger log = LoggerFactory.getLogger(FixInitiator.class);
	private static String dash="--------------------";
	@BeforeAll
	static void setUp() throws Exception {
		log.info(dash+" TestCase1: Executing Basic Connectivity Test "+dash);
		fixInitiator = new FixInitiator();
		fixInitiator.logon();
		Thread.sleep(2000); 
		if(fixInitiator.getApplication().getCreateSessions()!=null) {
			log.info("Created Session from Intiator to Acceptor " + fixInitiator.getApplication().getCreateSessions());
		}
		log.info(dash+" TestCase1: Executing Succesfully "+dash);
	}

	@AfterAll
	static void tearDown() {
		fixInitiator.logout();
		try {
			fixInitiator.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	void testLogonAndValidateLogonMessage() throws InterruptedException {
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
	}



}
