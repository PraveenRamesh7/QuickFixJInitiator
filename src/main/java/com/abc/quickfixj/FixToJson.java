package com.abc.quickfixj;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.Field;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.FieldType;
import quickfix.Group;
import quickfix.Message;
import quickfix.MessageFactory;

public class FixToJson {
	MessageFactory messageFactory;
	DataDictionary dataDictionary;
	public FixToJson() 
	{
		
		try {
			messageFactory = new quickfix.fixt11.MessageFactory();
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			dataDictionary = new DataDictionary(classloader.getResourceAsStream("FIX50SP2.xml"));
		} catch (ConfigError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public String fix2Json(Message message) 
	{
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		ObjectNode rootNode = mapper.createObjectNode();
		ObjectNode headerNode = rootNode.putObject("header");
		convertFieldMapToJSON(dataDictionary, message.getHeader(), headerNode);
		convertFieldMapToJSON(dataDictionary, message, rootNode);
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return	"";
	}
	public String fix2Json2(Message message) 
	{
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		ObjectNode rootNode = mapper.createObjectNode();
		ObjectNode headerNode = rootNode.putObject("header");
		convertFieldMapToJSON2(dataDictionary, message.getHeader(), headerNode);
		convertFieldMapToJSON2(dataDictionary, message, rootNode);
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return	"";
	}
	private void convertFieldMapToJSON(DataDictionary dataDictionary,
			FieldMap fieldmap, ObjectNode node) throws FieldNotFound {
				Iterator<Field<?>> fieldIterator = fieldmap.iterator();
				while (fieldIterator.hasNext()) {
					Field field = (Field) fieldIterator.next();
					String value = fieldmap.getString(field.getTag());
					if (!isGroupCountField(dataDictionary, field)) {
						node.put(String.valueOf(field.getTag()), value);
						/*String fieldName = dataDictionary.getFieldName(field.getTag());
						if (fieldName == null){ fieldName = "UDF"+ field.getTag();}
						node.put(fieldName, value);*/
					}
				}

				Iterator groupsKeys = fieldmap.groupKeyIterator();
				while (groupsKeys.hasNext()) {
					int groupCountTag = ((Integer) groupsKeys.next()).intValue();
					// System.out.println(groupCountTag + ": count = "
					// + fieldMap.getInt(groupCountTag));
					Group group = new Group(groupCountTag, 0);
					ArrayNode repeatingGroup = node.putArray(String
							.valueOf(groupCountTag));
					/*String fieldName = dataDictionary.getFieldName(groupCountTag);
					if (fieldName == null){ fieldName = "UDF"+ groupCountTag;}
					ArrayNode repeatingGroup = node.putArray(fieldName);
		*/
					int i = 1;
					while (fieldmap.hasGroup(i, groupCountTag)) {
						fieldmap.getGroup(i, group);
						ObjectNode groupNode = repeatingGroup.addObject();
						convertFieldMapToJSON(dataDictionary, group, groupNode);
						i++;
					}
				}
			}
	private void convertFieldMapToJSON2(DataDictionary dataDictionary,
			FieldMap fieldmap, ObjectNode node) throws FieldNotFound {
				Iterator<Field<?>> fieldIterator = fieldmap.iterator();
				while (fieldIterator.hasNext()) {
					Field field = (Field) fieldIterator.next();
					String value = fieldmap.getString(field.getTag());
					if (!isGroupCountField(dataDictionary, field)) {
						node.put(dataDictionary.getFieldName(field.getTag()), value);
						/*String fieldName = dataDictionary.getFieldName(field.getTag());
						if (fieldName == null){ fieldName = "UDF"+ field.getTag();}
						node.put(fieldName, value);*/
					}
				}

				Iterator groupsKeys = fieldmap.groupKeyIterator();
				while (groupsKeys.hasNext()) {
					int groupCountTag = ((Integer) groupsKeys.next()).intValue();
					// System.out.println(groupCountTag + ": count = "
					// + fieldMap.getInt(groupCountTag));
					Group group = new Group(groupCountTag, 0);
					ArrayNode repeatingGroup = node.putArray(String
							.valueOf(groupCountTag));
					/*String fieldName = dataDictionary.getFieldName(groupCountTag);
					if (fieldName == null){ fieldName = "UDF"+ groupCountTag;}
					ArrayNode repeatingGroup = node.putArray(fieldName);
		*/
					int i = 1;
					while (fieldmap.hasGroup(i, groupCountTag)) {
						fieldmap.getGroup(i, group);
						ObjectNode groupNode = repeatingGroup.addObject();
						convertFieldMapToJSON(dataDictionary, group, groupNode);
						i++;
					}
				}
			}
	private boolean isGroupCountField(DataDictionary dd, Field field) {
		return dd.getFieldType(field.getTag()) == FieldType.NUMINGROUP;

	}


	
	
	
}
