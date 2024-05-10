package com.abc.quickfixj;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import quickfix.DataDictionary;
import quickfix.FieldMap;
import quickfix.Message;

public class FixToJson {
	
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public String convertToJson(Message fixMessage) {
        Map<String, Object> jsonMap = new HashMap<>();
        try {
            DataDictionary dataDictionary = new DataDictionary("src/main/resources/FIX50SP2.xml");
            addFieldsToJsonMap(fixMessage.getHeader(), jsonMap, dataDictionary);
            addFieldsToJsonMap(fixMessage, jsonMap, dataDictionary);
            addFieldsToJsonMap(fixMessage.getTrailer(), jsonMap, dataDictionary);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson.toJson(jsonMap);
    }

    private void addFieldsToJsonMap(FieldMap fieldMap, Map<String, Object> jsonMap, DataDictionary dataDictionary) {
        fieldMap.iterator().forEachRemaining((field) -> {
            int fieldTag = field.getTag();
            String fieldName = dataDictionary.getFieldName(fieldTag);
            String fieldValue = field.getObject().toString();
            jsonMap.put(fieldName+"_"+fieldTag, fieldValue);
        });
    }
}
