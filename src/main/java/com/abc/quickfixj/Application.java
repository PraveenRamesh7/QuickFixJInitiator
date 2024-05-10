package com.abc.quickfixj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import quickfix.DataDictionaryProvider;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.GapFillFlag;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.NewSeqNo;

public class Application implements quickfix.Application {
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	private static Integer intiatorSeqNo;
	private static Integer acceptorSeqNo;

	public final List<Message> fromAppMessages = Collections.synchronizedList(new ArrayList<>());
	public final List<Message> toAppMessages = Collections.synchronizedList(new ArrayList<>());
	public final List<Message> fromAdminMessages = Collections.synchronizedList(new ArrayList<>());
	public final List<Message> toAdminMessages = Collections.synchronizedList(new ArrayList<>());
	public final List<SessionID> logonSessions = Collections.synchronizedList(new ArrayList<>());
	public final List<SessionID> logoutSessions = Collections.synchronizedList(new ArrayList<>());
	public final List<SessionID> createSessions = Collections.synchronizedList(new ArrayList<>());
	public int sessionResets = 0;

	FixToJson fix2Json= new FixToJson();
	@Autowired
	FixInitiator fixInitiator;
	@Override
	public void onCreate(SessionID sessionId) {
		createSessions.add(sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		log.info("logon :" +  sessionId);
		logonSessions.add(sessionId);
	}

	@Override
	public void onLogout(SessionID sessionId) {
		log.info("logout :" +  sessionId);
		logoutSessions.add(sessionId);

	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		log.info("toAdmin (Initiator) :" +  message);
		toAdminMessages.add(message);
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		log.info("fromAdmin (Initiator) :" +  message);
		fromAdminMessages.add(message);
	}

	private void resetSequenceNo(Message message, SessionID sessionId) {
		try {
			MsgSeqNum seqNum = new MsgSeqNum();
			acceptorSeqNo = message.getHeader().getInt(seqNum.getField());
			log.info("Acceptor Message Sequence Number: " + acceptorSeqNo);
			if(acceptorSeqNo!=intiatorSeqNo) {
				Message sequenceResetMessage = createSequenceResetMessage();
				log.info("Reset Sequence " + sequenceResetMessage);
				sendMessage(sessionId,sequenceResetMessage);
				log.info("Changing Initiator seq no : " + message +" :: "+ 1);
				try {
					Session.lookupSession(sessionId).setNextSenderMsgSeqNum(1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FieldNotFound e) {
			e.printStackTrace();
		}

	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		log.info("Application Request Sent (Initiator) :" +  message);
		toAppMessages.add(message);

	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		log.info("Application Response Received (Initiator) :" + message);
		fromAppMessages.add(message);
	}

	private static Message createSequenceResetMessage() {
		Message sequenceReset = new Message();
		sequenceReset.getHeader().setString(MsgType.FIELD, "4"); 
		sequenceReset.setString(GapFillFlag.FIELD, "Y");
		sequenceReset.setInt(NewSeqNo.FIELD, 1);
		return sequenceReset;
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
					dataDictionaryProvider.getApplicationDataDictionary(getApplVerID(session, message)).validate(message, true);
				} catch (Exception e) {
					LogUtil.logThrowable(sessionID, "Outgoing message failed validation: " + e.getMessage(), e);
					return;
				}
			}
			session.send(message);
		} catch (SessionNotFound e) {
			log.info(e.getMessage());
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


	public void clear() {
		fromAppMessages.clear();
		toAppMessages.clear();
		fromAdminMessages.clear();
		toAdminMessages.clear();
		logonSessions.clear();
		logoutSessions.clear();
		createSessions.clear();
	}

	public SessionID getCreateSessions() {
		if (createSessions.isEmpty()) {
			return null;
		}
		return createSessions.get(createSessions.size() - 1);
	}

	public Message lastFromAppMessage() {
		if (fromAppMessages.isEmpty()) {
			return null;
		}
		return fromAppMessages.get(fromAppMessages.size() - 1);
	}

	public Message lastFromAdminMessage() {
		if (fromAdminMessages.isEmpty()) {
			return null;
		}
		return fromAdminMessages.get(fromAdminMessages.size() - 1);
	}

	public Message lastToAppMessage() {
		if (toAppMessages.isEmpty()) {
			return null;
		}
		return toAppMessages.get(toAppMessages.size() - 1);
	}

	public Message lastToAdminMessage() {
		if (toAdminMessages.isEmpty()) {
			return null;
		}
		return toAdminMessages.get(toAdminMessages.size() - 1);
	}

	public void onReset(SessionID sessionID) {
		sessionResets++;
	}

}
