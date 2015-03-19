/**
 * @author    Marian Crisan <mcrisan@intacct.com>
 * 
 * Copyright (c) 2014,2015 Intacct OpenSource Initiative
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The Intacct code uses the 3-rd party library org.json which has the following license agreement
 *  
    Copyright (c) 2002 JSON.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 The Software shall be used for Good, not Evil.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.

 * 
 * OVERVIEW
 * The general pattern for using this SDK is to first create an instance of api_session and call either
 * connectCredentials or connectSessionId to start an active session with the Intacct Web Services gateway.
 * You will then pass the api_session as an argument in the api_post class methods.  intacctws-java handles all
 * XML serialization and de-serialization and HTTPS transport.
 * 
 * ---------------------
 * Notes (habrudan) Feb 2015
 * ------
 * this is a SINGLETON; 
 * NOT thread safe due to private variables that store call details and are not synchronized
 * 
 * introduced the IntacctSDKRuntimeException RuntimeException in the services interface; 
 * this needs to be caught by the caller in order to get the exception details and context
 * see sample usage in TestSDKTool.java (contains sample code for all services). 
 * 
 * The refactoring of the return type now provides more details on the outcome of the operation. 
 * 
 * Here is the format of the successful return: 
 * @return JSONObject using the following format 
 *     	{	"OVERALL_STATUS":"Success",
 *     		"ERROR":"NO_ERROR",
 *     		"CORRECT_RECORDS":[	{"RECORDNO":key,"attribute1":key},
 *     							{"RECORDNO":key,"attribute1":key},
 *  	 						{"RECORDNO":key,"attribute1":key} ]
 *      }
 *     
 * For READ operations the format has the following content: 
 * {
   "OVERALL_STATUS": "SUCCESS",
   "ERROR": "NO_ERROR",
   "READ_RESULT": [{key1:value1, key2:value2, key3:value3}]
   }
 * where key* are the names of the columns of the object in the DB schema from the read request 
 * and the value* are the values of the fields from the DB
 * 
 * For any exception the session adapter will raise a IntacctSDKRuntimeException
 * The IntacctSDKRuntimeException contains details on the cause of the exception and additionally for multiple records 
 * which are correct; the service raises exception on the first record that causes it and raises exception; thus all 
 * previous records are correct but due to the semantics of the service - transactional per request - if exception 
 * is encountered all changes are rolled back.  Here are the details of the cause of the exception 
 * 
 *  {
   	  	"OVERALL_STATUS": "Failure",
   		"ERROR": [
      				{"ERROR_MESSAGE": "Another Contact with the given value(s)  already exists  Use a unique value instead."},
      				{"ERROR_RECORD": { "attribute1": key, "attribute2": "name"}}
   				 ],
   		"CORRECT_RECORDS": [{"RECORDNO":key,"attribute1":key},
   		 					{"RECORDNO":key,"attribute1":key}]
		}
 *   
 *  
 */

package com.intacct.ws;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import com.intacct.ws.exception.IntacctSDKRuntimeException;
import com.intacct.ws.util.ConfigConstants;
import com.intacct.ws.util.ConfigLoader;

public class APISession<ReturnType> {
	
	final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "\n<request>"
			+ "\n   <control>"
			+ "\n       <senderid>{4%}</senderid>"
			+ "\n       <password>{5%}</password>"
			+ "\n       <controlid>foobar</controlid>"
			+ "\n       <uniqueid>false</uniqueid>"
			+ "\n       <dtdversion>3.0</dtdversion>"
			+ "\n   </control>"
			+ "\n   <operation>"
			+ "\n     <authentication>";

	final static String XML_FOOTER = "</authentication>"
			+ "\n <content>"
			+ "\n  <function controlid=\"foobar\"><getAPISession></getAPISession></function>"
			+ "\n</content>"
			+ "\n</operation>"
			+ "\n</request>";

	final static String XML_LOGIN = "<login>"
			                        + "\n  <userid>{1%}</userid>"
			                        + "\n  <companyid>{2%}</companyid>"
			                        + "\n  <password>{3%}</password>"
			                        + "\n  {%entityid%}"
			                + "\n</login>";

	final static String XML_SESSIONID = "<sessionid>{1%}</sessionid>";
	final String xml_response_header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n <result>";
	final String xml_response_footer = "</result>"; 
	
	private static boolean dryRun = false;
	private String lastRequest = null;
	private String lastResponse = null;
	
	private String dtdVersion="3.0";
	private String dtdVersion21 = "2.1";
	private static boolean multiFunc=false; 
	
	private enum SESSION_OPER_TYPE  { CREATE, UPDATE, DELETE, READ, DETAIL, INSPECT}; 
	
	final static int DEFAULT_RECORDS = 100;
	
	final static int DEFAULT_PAGESIZE = 1000;
	final static int DEFAULT_MAXRETURN = 100000;
	
	private String senderId, senderPassword, sessionId, endpoint;
	private RETURN_FORMAT returnFormat;
	private enum RETURN_FORMAT {  CSVOBJ("csv"), XMLOBJ("xml"), JSONOBJ("json"); 
		private String formatName; 
		private RETURN_FORMAT(String format) {
			formatName = format; 
		}
		public String getName(){
			return formatName; 
		}
	}; 

	private boolean transaction = false;
	private APISession<ReturnType> session = null;
	
	private static int requestCounter = 0;
	private APITracer tracer = null;
	
	/*
	 * keeps the type of the objects that is being worked on; create and update can have multiple
	 * NOT thread safe  
	 */
	private List<String> objectType = new ArrayList<String>();
	
	// constructor
	private APISession (RETURN_FORMAT returnFormat) {
		this.returnFormat = returnFormat;
	}
	
	public static APISession<JSONObject> getJSONInstance(String companyId, String userId,
			String password, String senderId, String senderPassword) throws IOException {
		return APISession.getJSONInstance(companyId, userId, password, senderId, senderPassword, null, null);
	}
	
	/**
	 * Allows for a transactional context execution 
	 * 
	 * @param companyId
	 * @param userId
	 * @param password
	 * @param senderId
	 * @param senderPassword
	 * @return
	 * @throws IOException
	 */
	public static APISession<JSONObject> getTRXJSONInstance(String companyId, String userId,
			String password, String senderId, String senderPassword) throws IOException {
		APISession<JSONObject> session = APISession.getJSONInstance(companyId, userId, password, senderId, senderPassword, null, null);
		session.transaction = true; 
		return session; 
	}
	
	public static APISession<JSONObject> getJSONInstance(String companyId, String userId,
			String password, String senderId, String senderPassword, String entityType, String entityId) throws IOException {

		APISession<JSONObject> session = new APISession<JSONObject>(APISession.RETURN_FORMAT.JSONOBJ);
		session.connect(companyId, userId, password, senderId, senderPassword, entityType, entityId);
		return session;
	}
	
	public static APISession<JSONObject> getJSONInstance(String sessionId, String senderId, String senderPassword) throws IOException {

		APISession<JSONObject> session = new APISession<JSONObject>(APISession.RETURN_FORMAT.JSONOBJ);
		session.connect(sessionId, senderId, senderPassword);
		return session;
	}
/*	
	public static APISession<String> getXMLInstance(String companyId, String userId,
			String password, String senderId, String senderPassword) throws IOException {
		return APISession.getXMLInstance(companyId, userId, password, senderId, senderPassword, null, null);
	}
	
	public static APISession<String> getXMLInstance(String companyId, String userId,
			String password, String senderId, String senderPassword, String entityType, String entityId) throws IOException {

		APISession<String> session = new APISession<String>(APISession.RETURN_FORMAT.XMLOBJ);
		session.connect(companyId, userId, password, senderId, senderPassword, entityType, entityId);
		return session;
	}
	
	public static APISession<String> getXMLInstance(String sessionId, String senderId, String senderPassword) throws IOException {

		APISession<String> session = new APISession<String>(APISession.RETURN_FORMAT.XMLOBJ);
		session.connect(sessionId, senderId, senderPassword);
		return session;
	}

	
	public static APISession<String> getCSVInstance(String companyId, String userId,
			String password, String senderId, String senderPassword) throws IOException {
		return APISession.getCSVInstance(companyId, userId, password, senderId, senderPassword, null, null);
	}
	
	public static APISession<String> getCSVInstance(String companyId, String userId,
			String password, String senderId, String senderPassword, String entityType, String entityId) throws IOException {

		APISession<String> session = new APISession<String>(APISession.RETURN_FORMAT.CSVOBJ);
		session.connect(companyId, userId, password, senderId, senderPassword, entityType, entityId);
		return session;
	}
	
	public static APISession<String> getCSVInstance(String sessionId, String senderId, String senderPassword) throws IOException {

		APISession<String> session = new APISession<String>(APISession.RETURN_FORMAT.XMLOBJ);
		session.connect(sessionId, senderId, senderPassword);
		return session;
	}
	
	*/
	
	private void connect(String companyId, String userId,
			String password, String senderId, String senderPassword, String entityType, String entityId) throws IOException {
		
		String xml = buildHeaderXML(companyId, userId, password, senderId, senderPassword, entityType, entityId);
		
		// connection  
		String response = execute(xml, ConfigLoader.getProperty(ConfigConstants.END_POINT_URL));
		
		// validation of the connection's response
		JSONObject joXML = XML.toJSONObject(response);
		validateConnection(joXML);	
		
		// save the set of user credentials
        this.senderId = senderId;
        this.senderPassword = senderPassword;
	}
	
	private void connect(String sessionId, String senderId, String senderPassword) throws IOException {
		
		String xml = buildSessionHeaderXML(sessionId, senderId, senderPassword);
		
		// connection  
		String response = execute(xml,  ConfigLoader.getProperty(ConfigConstants.END_POINT_URL));
					
		// validation of the connection's response
		JSONObject joXML = XML.toJSONObject(response);
		validateConnection(joXML);	
					
		// save the set of user credentials
		this.senderId = senderId;
        this.senderPassword = senderPassword;
	}
	
	/**
     * Create one or more records.  Object types can be mixed and can be either standard or custom.
     * Check the developer documentation to see which standard objects are supported in this method
     * 
     * @param objects list of names of the object types that will be created
     * @param JSONArray       records is an array of records to create.  Follow the pattern
     * records = array(array('myobjecttype' => array('field1' => 'value',
     *                                                'field2' => 'value')),
     *                  array('myotherobjecttype' => array('field1' => 'value',
     *                                                     'field2' => 'value')));
     * @return ReturnType 
     * 
     * For JSONObject the result has the following format: 
     * 	{	"OVERALL_STATUS":"Success",
     * 		"ERROR":"NO_ERROR",
     * 		"CORRECT_RECORDS":[	{"RECORDNO":key,"attribute1":key},
     * 							{"RECORDNO":key,"attribute1":key},
   	 * 							{"RECORDNO":key,"attribute1":key} ]
     *  }
     * 
	 * @throws IntacctSDKRuntimeException, Exception
	 * The IntacctSDKRuntimeException contains details on the cause of the exception and additionally for multiple records 
	 * which are correct; the service raises exception on the first record that causes it and raises exception; thus all 
	 * previous records are correct but due to the semantics of the service - transactional per request - if exception 
	 * is encountered all changes are rolled back.  Here are the details of the cause of the exception 
	 * 
	 *  {
   	  	"OVERALL_STATUS":"Failure",
   		"ERROR": [
      				{"ERROR_MESSAGE": "Another Contact with the given value(s)  already exists  Use a unique value instead."},
      				{"ERROR_RECORD": { "attribute1": key, "attribute2": "name"}}
   				 ],
   		"CORRECT_RECORDS": [{"RECORDNO":key,"attribute1":key},
   		 					{"RECORDNO":key,"attribute1":key}]
		}
	 *   
     */
    @SuppressWarnings("unchecked")
	public ReturnType create(List<String> objects, JSONArray records) throws IntacctSDKRuntimeException, Exception
    {
    	String createXml;
    	// set private member 
    	objectType = objects; 
    	int nbRecords = APIUtil.getNbRecords(records);
    	
    	if ( nbRecords == 0 )
    		return null;
    		
    	if ( nbRecords > DEFAULT_RECORDS)
    		throw new IntacctSDKRuntimeException("Attempting to create more than" +  DEFAULT_RECORDS + "records. (" + nbRecords + ") ");

        // Convert the record into a xml structure
    	StringBuffer b = new StringBuffer(); 
    	for ( int ix = 0; ix < nbRecords; ix++ ) {
    		JSONObject r = records.getJSONObject(ix);
    		b.append(XML.toString(r));
    	}
        createXml = "<create>"+ b.toString() + "</create>";
        
    	JSONObject jObj = (JSONObject) post(createXml, multiFunc, SESSION_OPER_TYPE.CREATE); 

        return ((ReturnType) jObj);

    }
    
    /**
     * Update one or more records.  Object types can be mixed and can be either standard or custom.
     * Check the developer documentation to see which standard objects are supported in this method
     *
     * @param objects list of names of the object types that will be updated
     * @param JSONArray       records is an array of records to update.  Follow the pattern
     * records = array(array('myobjecttype' => array('field1' => 'value',
     *                                                'field2' => 'value')),
     *                  array('myotherobjecttype' => array('field1' => 'value',
     *                                                     'field2' => 'value')));
     *
     * @return ReturnType  - for JSON format @see APISession::create
	 * @throws Exception 
     */
	public ReturnType update(List<String> objects, JSONArray records) throws IntacctSDKRuntimeException, Exception  {
    	String updateXml;
    	int nbRecords = APIUtil.getNbRecords(records);
    	// set the private member 
    	objectType = objects;
    	
    	if ( nbRecords == 0 )
    		return null;
    	
    	if ( nbRecords > DEFAULT_RECORDS)
    		throw new IntacctSDKRuntimeException(
    				"Attempting to create more than" +  DEFAULT_RECORDS + "records. (" + nbRecords + ") ");

        // Convert the record into a xml structure
    	StringBuffer b = new StringBuffer(); 
    	for ( int ix = 0; ix < nbRecords; ix++ ) {
    		JSONObject r = records.getJSONObject(ix);
    		b.append(XML.toString(r));
    	}
    	updateXml = "<update>"+ b.toString() + "</update>";

    	JSONObject jObj = (JSONObject) post(updateXml, multiFunc, SESSION_OPER_TYPE.UPDATE);
        return ((ReturnType) jObj);

    }
    
    /**
     * Delete one or more records
     *
     * @param String      object  integration code of object type to delete
     * @param String      ids     String a comma separated list of keys.  use 'id' values for custom
     * objects and 'recordno' values for standard objects
     *
     * @return boolean indicating success or failure
     */
    public boolean delete(String object, String keysList) throws IntacctSDKRuntimeException, Exception
    {
    	String deleteXml;
       	// set private member
    	objectType.add(object); 
    	
    	if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("DELETE - Object is null");
    	
    	if ( keysList == null || keysList.length() == 0 )
    		return false;
    	
    	deleteXml = "<delete><object>" + object + "</object><keys>" + keysList + "</keys>" + "</delete>";
 
    	
    	JSONObject jRetObj = (JSONObject) post(deleteXml, multiFunc, SESSION_OPER_TYPE.DELETE);
    	//System.out.println("Return Json Object for delete: " + jRetObj.toString(2));
        return jRetObj.getString("OVERALL_STATUS").equals("Success");
    }
    
    /**
     * WARNING: This method will attempt to delete all records of a given object type
     * Deletes first 10000 by default
     *
     * @param String      object  object type
     * @param key		  the pk for the object (RECORDNO or ID)
     * @param Integer     max     [optional] Maximum number of records to delete.  Default is 10000
     *
     * @return boolean for success; on error exception is raised. 
	 * @throws Exception 
     */
	@SuppressWarnings("unchecked")
	public boolean deleteAll( String object, int max, String key) throws IntacctSDKRuntimeException, Exception
    {
    	JSONArray jArr = null;
    	JSONObject jObj = null;
    	String query = null, keysResult = null;
    	int nbRecords; 
    	
		java.util.Iterator<String> keyArray_object;
		String key_object;
		Object value_object;
		
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("DeleteAll - Object is null. Please specify the object.");
		
		if ( key == null || key.isEmpty() )
    		throw new IntacctSDKRuntimeException("DeleteAll - Key is null. Please specify a key.");

		else if ( !key.equalsIgnoreCase("RECORDNO") ) 
			if ( !key.equalsIgnoreCase("id") )
	    		throw new IntacctSDKRuntimeException("DeleteAll - Key must be one of the field RECORDNO or id.");

		query = key + " > 0";
    	
        // read all the record id for the given object
        jArr  = ((JSONObject) readByQuery(object, query, key, max)).getJSONArray("READ_RESULT");
        
        nbRecords = APIUtil.getNbRecords(jArr);
        if (  nbRecords == 0 )
    		throw new IntacctSDKRuntimeException("DeleteAll - No records found to delete");
    		
        
        // set the keys list to delete
    	for ( int ix = 0; ix < nbRecords; ix++ ) {
    		jObj = jArr.getJSONObject(ix);
    		
        	keyArray_object = jObj.keys();
    		while ( keyArray_object.hasNext() ) 
    		{
    			key_object = keyArray_object.next().toString();
                value_object = jObj.opt(key_object);
                if ( key_object.equals(key) ) {
                	if ( keysResult == null || keysResult.length() == 0 )
                		keysResult = (String) value_object;
                	else
                		keysResult += "," + (String) value_object;
                }
    		}
    	}
        
		return delete(object, keysResult);
    }
	
    /**
     * WARNING: This method will attempt to delete all records of a given object type given a query
     * Deletes first 10000 by default
     *
     * @param String      object  object type
     * @param key		  the pk for the object (RECORDNO or ID)
     * @param Integer     max     [optional] Maximum number of records to delete.  Default is 10000
     *
     * @return Integer count of records deleted
	 * @throws Exception 
     */
	@SuppressWarnings("unchecked")
	public boolean deleteByQuery( String object, String query, int max, String key) 
			throws IntacctSDKRuntimeException, Exception
    {
    	JSONArray jArr = null;
    	JSONObject jObj = null;
    	String keysResult = null, queryExt = null;
    	int nbRecords; 
    	
		java.util.Iterator<String> keyArray_object;
		String key_object;
		Object value_object;
		
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("DeleteByQuery -  Object is null");
		
		if ( key == null || key.isEmpty() )
    		throw new IntacctSDKRuntimeException("DeleteByQuery - Key is null. Please specify a key.");
		else if ( !key.equalsIgnoreCase("RECORDNO") ) 
			if ( !key.equalsIgnoreCase("id") )
	    		throw new IntacctSDKRuntimeException("DeleteByQuery - Key must be one of the field RECORDNO or id.");
		
		queryExt = key + " > 0";
		if ( query != null && query.length() > 0 ) 
			queryExt += " and" + APIUtil.htmlspecialchars(query);
    	
        // read all the record id for the given object
        jArr  = ((JSONObject) readByQuery(object, queryExt, key, max)).getJSONArray("READ_RESULT"); 
        
        nbRecords = APIUtil.getNbRecords(jArr);
        if (  nbRecords == 0 )
    		throw new IntacctSDKRuntimeException("DeleteByQuery - No records found to delete");
        
        // set the keys list to delete
    	for ( int ix = 0; ix < nbRecords; ix++ ) {
    		jObj = jArr.getJSONObject(ix);
    		
        	keyArray_object = jObj.keys();
    		while ( keyArray_object.hasNext() ) 
    		{
    			key_object = keyArray_object.next().toString();
                value_object = jObj.opt(key_object);
                if ( key_object.equals(key) ) {
                	if ( keysResult == null || keysResult.length() == 0 )
                		keysResult = (String) value_object;
                	else
                		keysResult += "," + (String) value_object;
                }
    		}
    	}
        
		return delete(object, keysResult);
    }
    
    /**
     * Checks to see if a record exists.  If so, it updates, else it creates
     *
     * @param String      object       The type of object to perform upsert on
     * @param JSONArray  records      Array of records to upsert.  Should be passed in the same format as used with
     *                                  create and update
     * @param String       nameField    the field name used for lookup of existence
     * @param String       keyField     the field name used for the internal key (needed for update)
     * @param boolean      readOnlyName Optional.  You shouldn't normally set this to true unless the value in the
     *                                  name field is actually set by the platform and you're passing a formulated value
     *                                  that should not be passed in the create or update
     *
     * @throws IntacctSDKRuntimeException, Exception
     * @return ReturnType	in the format requested		
     */
    @SuppressWarnings("unchecked")
	public ReturnType upsert(String object, JSONArray records, String nameField, 
			String keyField, boolean readOnlyName) throws IntacctSDKRuntimeException, Exception
    {
    	JSONArray existingRecords = null, jArrayRecords = null, toUpdate = null, toCreate = null, 
    			created = new JSONArray(), updated = new JSONArray();
    	String query = null, inClause = null;
    	List<String> objectType = new ArrayList<String>();
    	objectType.add(object); 
		int nbRecords, nbExistRecords; 
		java.util.Iterator<String> keyArray_object;
		String key_object;
		Object value_object;
		JSONObject jObj, jObjResult, resultJsonObj = new JSONObject();
		String[] keys = {""};
    	
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("UPSERT - Object is null");
		
    	nbRecords = APIUtil.getNbRecords(records);
    	
    	if ( nbRecords > DEFAULT_RECORDS)
    		throw new IntacctSDKRuntimeException("You can only upsert up to" +  DEFAULT_RECORDS + 
    				"records at a time.  You passed " + nbRecords + " object records.");
   	
    	jArrayRecords = APIUtil.htmlspecialchars(records, object, nameField);
    	
    	inClause = APIUtil.inClauseConstructor(object, nameField, ",", jArrayRecords);

    	if ( inClause != null && inClause.length() >= 0 )
    		query = nameField + " in (" + inClause + ")";
    	else 
    		query = null;
        
        if ( keyField == null  || keyField.length() == 0 )
        	existingRecords = (JSONArray) ((JSONObject) 
        			readByQuery(object, query, nameField, DEFAULT_MAXRETURN)).get("READ_RESULT");
        else
        	existingRecords = (JSONArray) ((JSONObject) 
        			readByQuery(object, query, nameField + "," + keyField, DEFAULT_MAXRETURN)).get("READ_RESULT");
        
        nbExistRecords = APIUtil.getNbRecords(existingRecords);
        if ( nbExistRecords == 0 )  {
        	// only create records no update
            if (readOnlyName == true)  {
            	jArrayRecords = APIUtil.unsetNameField(jArrayRecords, object, nameField);
            }
            created = (JSONArray) ((JSONObject) create(objectType, jArrayRecords)).get("CORRECT_RECORDS");

        } else {
        	// update and possibly create objects 
        	toCreate = new JSONArray();
        	toUpdate = new JSONArray();
        	
            // find what we need to create and what we need to update from the entry records
        	for ( int ix = 0; ix < nbRecords; ix++ ) {
        		jObj = jArrayRecords.getJSONObject(ix);
        		
	        	keyArray_object = jObj.keys();
	    		while ( keyArray_object.hasNext() ) 
	    		{
	                // the object name 
	    			key_object = keyArray_object.next().toString();
	    			
	                keys[0] = key_object;
	                jObjResult = new JSONObject(jObj, keys);
	                
	                if ( key_object.equals(object) ) {
		    			// the object fields: (nameField : valueField)
		                value_object = jObj.opt(key_object);

		                if ( ((JSONObject) value_object).has(nameField) ) 
		                	toUpdate.put(jObj);
	                    else
	                    	toCreate.put(jObj);
	                
	                	}
	                else 
	                		toCreate.put(jObjResult);
	    		}
        	}
        	if (toCreate.length() > 0) created = (JSONArray) ((JSONObject) create(objectType, toCreate)).get("CORRECT_RECORDS");
			if (toUpdate.length() > 0) updated = (JSONArray) ((JSONObject) update(objectType, toUpdate)).get("CORRECT_RECORDS");
        }

        JSONArray result = new JSONArray()
  	     .put(new JSONObject().put("CREATED", created.getJSONArray(0)))
   	     .put(new JSONObject().put("UPDATED", updated.getJSONArray(0)));
        
        resultJsonObj.put("OVERALL_STATUS", "Success");
        resultJsonObj.put("ERROR", "NO_ERROR");
        resultJsonObj.put("CORRECT_RECORDS", result);

        // TODO: need to handle XML and CSV formats too. 
        return (ReturnType) resultJsonObj; 

    }
    
    /**
     * Read one or more records by their key.  For platform objects, the key is the 'id' field.
     * For standard objects, the key is the 'recordno' field.  Results are returned as a php structured array
     *
     * @param String              object  the integration name for the object
     * @param String              keys    a comma separated list of keys for each record you wish to read
     * @param String              fields  a comma separated list of fields to return
     *
     * @return Array of records
     */
	public ReturnType read(String object, String keys, String fields) throws IntacctSDKRuntimeException, Exception {
		
		ReturnType result;
		String readXml;
		// set the private member 
    	objectType.add(object); 
		
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("READ - Object is null");
			
		readXml = "<read><object>" + object + "</object>";
		
		if ( keys != null && ! keys.isEmpty() )
			readXml += "<keys>" + keys + "</keys>";
		else 
			readXml += "<keys/>";
		
		readXml += "<fields>" + fields + "</fields><returnFormat>" + this.getReturnFormatString() + "</returnFormat></read>";
		
		System.out.println("readXML payload:  "  + readXml);
		
		result = post(readXml, multiFunc, SESSION_OPER_TYPE.READ);
        
		return result;
	}
    
    /**
     * Read records using a query.  Specify the object you want to query and something like a "where" clause
     *
     * @param String      object       the object upon which to run the query
     * @param String      query        the query string to execute.  Use SQL operators
     * @param String      fields       A comma separated list of fields to return
     * @param int         maxRecords   number of records to return.  Defaults to 100000
     * @param string      returnFormat Pass one of the valid constants from api_returnFormat class
     *
     * @return JSONArray 	 result 			Array of objects
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
	public ReturnType readByQuery(String object, String query, String fields, int maxRecords) 
			throws IntacctSDKRuntimeException, Exception
    {
    	int pagesize, nbRecords, thiscount;
    	String xml;
    	boolean moreResults = false; 
    	JSONArray jArrResponse, jArrTemp = null;
    	JSONObject jObj = null, jsonObjResp = new JSONObject();

    	// set the private member 
    	objectType.add(object); 
    	
    	if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("ReadByQuery - Object is null");
    	
        if ( maxRecords < DEFAULT_PAGESIZE )
        	pagesize = maxRecords;
        else 
        	pagesize = DEFAULT_PAGESIZE;
        
        query = APIUtil.htmlspecialchars(query);

        xml = "<readByQuery><object>" + object + "</object><query>" + query + "</query><fields>" + fields.toUpperCase() + 
        		"</fields><returnFormat>" + this.getReturnFormatString() + "</returnFormat>";
        xml += "<pagesize>" + pagesize + "</pagesize>";
        xml += "</readByQuery>";

        jsonObjResp = (JSONObject) post(xml, multiFunc, SESSION_OPER_TYPE.READ);
		
        jArrResponse = jsonObjResp.getJSONArray("READ_RESULT");
        nbRecords = APIUtil.getNbRecords(jArrResponse);
        
        if ( this.returnFormat == RETURN_FORMAT.CSVOBJ && nbRecords == 0 ) {
            // csv with no records will have no response, so avoid the error from validate and just return
            return null;
        }
        
        thiscount = nbRecords;
        
        // we have no idea if there are more if CSV is returned, so just check
        // if the last count returned was  pageSize
        while (thiscount == pagesize && nbRecords < maxRecords) {
        	moreResults = true; 
            xml = "<readMore><object>" + object + "</object></readMore>";
            try {
            	jArrTemp = (JSONArray)  post(xml, multiFunc, SESSION_OPER_TYPE.READ);
                thiscount = APIUtil.getNbRecords(jArrResponse);
                nbRecords += thiscount; 
                
                // add the new found object ro the response 
                for ( int jx = 0; jx < thiscount; jx++ ) {
                	jObj = jArrTemp.getJSONObject(jx);
                	jArrResponse.put(jObj);
                }
            }
            catch (Exception ex) {
        		throw new IntacctSDKRuntimeException(ex.getMessage());
            }
        }
        if (moreResults)
        	jsonObjResp.put("CORRECT_RECORDS", jArrResponse);

        // TODO: need to handle XML and CSV formats too. 
        return (ReturnType) jsonObjResp;
    }
 
    /**
     * Read an object by its name field (vid for standard objects)
     *
     * @param String              object  the integration name for the object
     * @param String              keys    a comma separated list of keys for each record you wish to read
     * @param String              fields  a comma separated list of fields to return
     *
     * @return ReturnType with a collection of results
     */
	public ReturnType readByName(String object, String keys, String fields) throws IntacctSDKRuntimeException, Exception {
		
		ReturnType result;
		String readXml;

		// set the private member
    	objectType.add(object); 
		
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("ReadByName - Object is null");
			
		readXml = "<readByName><object>" + object + "</object>";
		
		if ( keys != null && ! keys.isEmpty() )
			readXml += "<keys>" + keys + "</keys>";
		else 
			readXml += "<keys/>";
		
		readXml += "<fields>" + fields + "</fields><returnFormat>" + this.getReturnFormatString() + "</returnFormat></readByName>";
		
		result = post(readXml, multiFunc, SESSION_OPER_TYPE.READ);
        
		return result;
	}

	/**
     * Reads all the records related to a source record through a named relationship.
     *
     * @param String      object   the integration name of the object
     * @param String      keys     a comma separated list of 'id' values of the source records from which you want to read related records
     * @param String      relation the name of the relationship.  This will determine the type of object you are reading
     * @param String      fields   a comma separated list of fields to return
     *
     * @return ReturnType containing the records. 
     */
	public ReturnType readRelated(String object, String keys, String relation, String fields) 
			throws IntacctSDKRuntimeException, Exception {
		
		ReturnType result;
		String readXml;
    	// set the private member
    	objectType.add(object); 
		
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("ReadRelated - Object is null");
		
		// if the relation is null or empty we will use the read function
		if ( relation == null || relation.isEmpty() )
			return read(object, keys, fields);
			
		readXml = "<readRelated><object>" + object + "</object>";
		
		if ( keys != null && ! keys.isEmpty() )
			readXml += "<keys>" + keys + "</keys>";
		else 
			readXml += "<keys/>";
		
		readXml += "<relation>" + relation + "</relation><fields>" + fields + "</fields><returnFormat>" + this.getReturnFormatString() + "</returnFormat></readRelated>";
		
		result = post(readXml, multiFunc, SESSION_OPER_TYPE.READ);
        
		return result;
	}
    
    /**
     * Inspect an object to get a list of its fields
     *
     * @param String      object  The integration name of the object.  Pass '*' to get a complete list of objects
     * @param bool 		  detail  Whether or not to return data type information for the fields.
     *
     * @return ReturnType with metadata of the object
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
	public ReturnType inspect(String object, boolean detail) throws IntacctSDKRuntimeException, Exception
    {
        String inspectXML = null;
        JSONObject jResObj = null;
        SESSION_OPER_TYPE insp_value;
        
    	// set the private member
    	objectType.add(object); 
    	
		if ( object == null || object.isEmpty() )
    		throw new IntacctSDKRuntimeException("Inspect - Object is null");
        
		inspectXML = "<inspect";
        if ( detail ) { 
        	inspectXML += " detail='" + detail + "'>";
        	insp_value = SESSION_OPER_TYPE.DETAIL;
        }
        else { 
        	inspectXML += ">";
        	insp_value = SESSION_OPER_TYPE.INSPECT;
        	
    	}
        inspectXML += "<object>" + object + "</object></inspect>";

        jResObj = (JSONObject) post(inspectXML, multiFunc, insp_value);
        
        // TODO: need to handle XML and CSV formats too. 
        return (ReturnType) jResObj;
    }
        
	/**
     * Internal method for posting the invocation to the Intacct XML Gateway
	 * @param typeOp 
     *
     * @param String      xml        the XML request document
     * @param boolean     multiFunc  whether or not this invocation calls multiple methods.  Default is false
     * @param String      typeOp     the type of operation, read, create, update, delete, ...
     * @param List<String> objects   the name of the objects that are in part of the payload (create and update can have many types)  
     *
     * @throws IntacctSDKRuntimeException, Exception
     * @return ReturnType response document in the format requested via the get<>Instance() call
     * 
     *  relies on the side effect of initialization of the objectType with the list of object types that will be used (see create and update)
     *   
     */ 
	@SuppressWarnings("unchecked")
	private ReturnType post(String xml, boolean multiFunc, SESSION_OPER_TYPE typeOp) 
						throws IntacctSDKRuntimeException, IOException, Exception {
		
		ReturnType resObj = null;
		boolean retry = true;
		JSONObject data = null;
		JSONObject jsonErrRes = new JSONObject(); 
		JSONObject jsonRequest = XML.toJSONObject(xml);
		setLastRequest(jsonRequest.toString(1));

		System.out.println("JSON Request object: " + jsonRequest.toString(2));
		
		String res = "";
	    
	    xml = buildPostXml(xml, dtdVersion, multiFunc);
	    
	    APISession.requestCounter++;
	    this.setLastRequest(xml);
	    if ( this.tracer != null ) {
	    	this.tracer.traceRequest(APISession.requestCounter, xml);
	    }
	    // retry five times on too many operations   
	    res = "";
	    while (retry) {
	        // If we didn't get a response, we had a poorly constructed XML request.
	    	
	        try {
	        	res = execute(xml, this.endpoint);
	        	//System.out.println("Raw xml result: " + res);
	        	this.setLastResponse(res);
	        	if ( this.tracer != null ) {
	    	    	this.tracer.traceResponse(APISession.requestCounter, res);
	    	    }
	        	// if "res" is empty, generate an error message
	        	if ( res.equals("\r") )
	        		throw new IntacctSDKRuntimeException("Result NULL: verify your input data");

	        	if ( (typeOp == SESSION_OPER_TYPE.CREATE) || (typeOp == SESSION_OPER_TYPE.UPDATE) 
	        			|| (typeOp == SESSION_OPER_TYPE.DELETE) || (typeOp == SESSION_OPER_TYPE.DETAIL) || (typeOp == SESSION_OPER_TYPE.INSPECT)) {
	        		// need to process the xml return result 
	        		data = processResultString(res, typeOp);

	        		switch ( this.returnFormat ) {
		        		case JSONOBJ:
		        			resObj = (ReturnType) data;
		        			/* *** need to further check if this separation is needed 
		        			if ( typeOp == DELETE || (typeOp == DETAIL) ) 
		    					resObj = (ReturnType) data.toJSONArray(data.names());
		    				else
		    					resObj = (ReturnType) data;
		    					*/  
		        			break;
		        		case XMLOBJ:
		        			String rXML = xml_response_header + XML.toString(data) + xml_response_footer;
		        			resObj = (ReturnType) rXML; 
		        			break;
		        		case CSVOBJ:
		        			String rCSV = APIUtil.JSONToCsv(data);
		        			resObj = (ReturnType) rCSV;
		        			break;
		        		default:
		        			resObj = null;
	        		}
	        	} else {
	        		// in case of Read* res is already a JSONArray, JSONObject for success case
	        		// otherwise is an xml string containing the error codes 
	        		if (res.contains("<?xml")) {
	        			//System.out.println("Result is in xml format: \n" + res );
	        			data = processResultString(res, typeOp);
	        			resObj = (ReturnType) (xml_response_header + XML.toString(data) + xml_response_footer); 
	        		} else {
	        			resObj = proceedReturnResult(res, typeOp);
	        		}
	        	}
	  // this might not be needed here since we are now looping over retry --->>>          break;
	        	retry = false; 
	        } catch (IntacctSDKRuntimeException sdkExc){
	        	// need to rearrange the result before throwing the exception to the caller
	        	// extract the successfully processed records
	        	jsonErrRes.put("OVERALL_STATUS", sdkExc.getJsonDetails().get("OVERALL_STATUS"));
	        	jsonErrRes.put("CORRECT_RECORDS", sdkExc.getJsonDetails().get("CORRECT_RECORDS"));
	        	//System.out.println("--- CORRECT_RECORDS:  " + sdkExc.getJsonDetails().get("CORRECT_RECORDS").toString());
	        	JSONObject jsonErrMsg= new JSONObject() , jsonErrRec = new JSONObject();
	        	JSONArray jsonErrArray = new JSONArray();
	        	jsonErrMsg.put("ERROR_MESSAGE", sdkExc.getJsonDetails().get("ERROR_MSG"));
	        	/*
	        	 * This can be currently determined only for the operations that have a single object type 
	        	 * in the request; for update and create since there can be multiple object types it is 
	        	 * difficult to determine the record that has caused the exception 
	        	 * 
	        	 */
	        	if (!(typeOp == SESSION_OPER_TYPE.CREATE) && !(typeOp == SESSION_OPER_TYPE.UPDATE) && !(typeOp == SESSION_OPER_TYPE.DELETE)) {
	        		// find the record that caused the error
	        		int errIdx = (Integer) sdkExc.getJsonDetails().get("countSuccessfulRecrds");
	        		JSONObject records = jsonRequest.optJSONObject((String)jsonRequest.names().get(0));
	        		Object reqObj = records.get(records.names().getString(0));
	        		if ( reqObj instanceof JSONObject) {
	        			jsonErrRec.put("ERROR_RECORD",reqObj);
	        		} else if (reqObj instanceof JSONArray) {
	        			jsonErrRec.put("ERROR_RECORD",((JSONArray)reqObj).getJSONObject(errIdx));    //<<<<<<<<<<<<<<<<<<<
	        		} else {
	        			jsonErrRec.put("ERROR_RECORD",reqObj);
	        		}
	        	} else {
	        		jsonErrRec.put("ERROR_RECORD",""); 
	        	}
	        	jsonErrArray.put(0,jsonErrMsg);
	        	jsonErrArray.put(1,jsonErrRec);
	        	jsonErrRes.put("ERROR", jsonErrArray);
	        	sdkExc.setJsonDetails(jsonErrRes);
	        	throw sdkExc;
	        
	    	}  catch (IOException ex) {
	        	if ( ex.toString().contains("NullPointerException") ) 
	        		throw new IntacctSDKRuntimeException("Result NULL: verify your input data");

				throw ex;
	        } 
	    }
	    
	    // return the Object with the results
		return resObj;		
	}

	/**
     * You won't normally use this function, but if you just want to pass a fully constructed XML document
     * to Intacct, then use this function.
     *
     * @param String body     a Valid XML string
     * @param String endPoint URL to post the XML to
     *
     * @throws exception
     * @return String the raw XML returned by Intacct
     */
	public static String execute(String body, String endpoint) throws IOException {
		StringBuffer response = null;
		HttpURLConnection connection = null;
		
		// Create connection
		URL url = new URL(endpoint);
		connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
		connection.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));
		connection.setRequestProperty("Content-Language", "en-US");  
			
		connection.setUseCaches (false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
      
		//Send request
		DataOutputStream wr = new DataOutputStream (
		              connection.getOutputStream ());
		/*
		 wr.writeBytes ("fName=" + URLEncoder.encode("???", "UTF-8") + body +
		 			        "&lName=" + URLEncoder.encode("???", "UTF-8"));
		*/
		
		wr.writeBytes("xmlrequest=" + URLEncoder.encode(body, "UTF-8"));
		wr.flush ();
		wr.close ();
		
		//Get Response	
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		
		String line;
		response = new StringBuffer(); 
		while((line = rd.readLine()) != null) {
			response.append(line);
		    response.append('\r');
		}
		rd.close();
		
	    if(connection != null) {
	    	connection.disconnect(); 
	    }
		
		return response.toString();
	}

	/**
     * Run any Intacct API method not directly implemented in this class.  You must pass
     * a valid XML for the method you wish to invoke.  Uses 3.0 API definitions if dtdVersion is null
     *
     * @param String    xml        			valid XML for the method you wish to invoke or collection of functions to invoke
     * @param string    dtdVersion Either 	"2.1" or "3.0" defaults to "3.0" if parameter is null
     * @param boolean 	multiInvocation 	true if the xml contains multiple functions; false otherwise 
	 * @return 
     *
     * @return String the XML response from Intacct 
	 * @throws Exception 
     */
    public ReturnType invokeService(String xml, boolean multiInvocation, String dtdVersion) throws IntacctSDKRuntimeException, Exception  {
    	
    	if ( xml == null || xml.length() == 0)
    		throw new IntacctSDKRuntimeException("invokeService - XML string is null. Please call this method with a valid XML.");

    	// TODO: additional validation for the XML 

       	if ( dtdVersion == null || dtdVersion.length() == 0 )
    		dtdVersion = "2.1";
    	
        return post(xml, multiInvocation, SESSION_OPER_TYPE.READ);
    }
	
    
	@SuppressWarnings("unchecked")
	private void validateConnection(JSONObject response) {

		java.util.Iterator<String> keyArray, keyArray_ctrl, keyArray_op, keyArray_status, keyArray_api, keyArray_user;
		String key, key_ctrl, key_op, key_status, key_res, key_api, key_user;
		Object value, value_ctrl, value_op, value_status, value_res, value_api, value_user;
		
		String error_message = "";
		
		if ( response == null )
    		throw new IntacctSDKRuntimeException("Connection: Invalid XML response");
		
		keyArray = response.keys();
		
		if ( !keyArray.hasNext() ) 
			return;
		
		key_res = keyArray.next().toString();
        value_res = response.opt(key_res);
        
        keyArray = ((JSONObject) value_res).keys();
        while ( keyArray.hasNext() ) 
		{
            
            key = keyArray.next().toString();
            value = ((JSONObject) value_res).opt(key);
            
            if ( key.equals("control") )
            {
            	keyArray_ctrl = ((JSONObject) value).keys();
            	
            	while ( keyArray_ctrl.hasNext() )
            	{
	            	key_ctrl = keyArray_ctrl.next().toString();
	                value_ctrl = ((JSONObject) value).opt(key_ctrl);
	                
	                // verify if the control->status == sucess
	                if ( value_ctrl instanceof String && ((String) value_ctrl).equals("failure")  )
	            		throw new IntacctSDKRuntimeException("XML response - control failure");
            	}
            }
            
            if ( key.equals("operation") )
            {
            	keyArray_op = ((JSONObject) value).keys();
            	
            	while ( keyArray_op.hasNext() )
            	{
            		key_op = keyArray_op.next().toString();
            		value_op = ((JSONObject) value).opt(key_op);
            		
            		keyArray_status = ((JSONObject) value_op).keys();

                	while ( keyArray_status.hasNext() )
                	{
                		key_status = keyArray_status.next().toString();
                		value_status = ((JSONObject) value_op).opt(key_status);
	                
                		// verify if the operation->...->status == sucess
                		if ( value_status instanceof String && ((String) value_status).equals("failure") )
                		{
                			if ( key_op.equals("authentification") )
                				error_message = "XML reponse - authentfication failure";
                			else 
                				if ( key_op.equals("result") )
                					error_message = "XML response - result failure";
                			throw new IntacctSDKRuntimeException(error_message);
                		}
                		
                		// save the set of user credentials : companyId, userId, sessionId & endpoint
                		if ( key_status.equals("companyid") ){
                			value_status.toString();
                		}
                		else if ( key_status.equals("userid") ){
                			value_status.toString();
                		}
                		else if ( key_status.equals("data") ){
                			keyArray_api = ((JSONObject) value_status).keys();
                			
                			while ( keyArray_api.hasNext() )
                        	{
                				key_api = keyArray_api.next().toString();
                				value_api = ((JSONObject) value_status).opt(key_api);
                				
                				keyArray_user = ((JSONObject) value_api).keys();
                				
                				while ( keyArray_user.hasNext() )
	                        	{
                					key_user = keyArray_user.next().toString();
	                				value_user = ((JSONObject) value_api).opt(key_user);
	                				
	                				if ( value_user instanceof String && key_user.equals("sessionid") )
	                					this.sessionId = value_user.toString();	
	                				else if ( value_user instanceof String && key_user.equals("endpoint") )
	                					this.endpoint= value_user.toString();
	                        	}
                        	}
                		}
                	}
            	}
            }
		}
		
		return;
	}
	
	private String buildHeaderXML(String companyId, String userId,
			String password, String senderId, String senderPassword,
			String entityType, String entityId) {
		
		String xml = XML_HEADER + XML_LOGIN + XML_FOOTER;
		
		xml = xml.replace("{1%}", userId);
		xml = xml.replace("{2%}", companyId);
		xml = xml.replace("{3%}", password);
		xml = xml.replace("{4%}", senderId);
		xml = xml.replace("{5%}", senderPassword);

        if (entityType == "location") {
        	xml = xml.replace("{%entityid%}", "<locationid>" + entityId + "</locationid>");
        } else if (entityType == "client") {
        	xml = xml.replace("{%entityid%}", "<clientid>" + entityId + "</clientid>");
        } else {
        	xml = xml.replace("{%entityid%}", "");
        }
		
		return xml;
	}

	private String buildSessionHeaderXML(String sessionId, String senderId,
			String senderPassword) {
		String xml = XML_HEADER + XML_SESSIONID + XML_FOOTER;
		
		xml = xml.replace("{1%}", sessionId);
		xml = xml.replace("{4%}", senderId);
		xml = xml.replace("{5%}", senderPassword);
		
		return xml;	
	}

	private String buildPostXml(String readXml, String dtdVersion2, boolean multiFunc2) {
		
		String templateHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "\r<request>"
			    + "\r <control>"
			    + "\r  <senderid>" + this.senderId + "</senderid>"
			    + "\r  <password>" + this.senderPassword + "</password>"
			    + "\r  <controlid>foobar</controlid>"
			    + "\r  <uniqueid>false</uniqueid>"
			    + "\r  <dtdversion>" + dtdVersion + "</dtdversion>"
			    + "\r </control>"
			    + "\r <operation transaction=\'" + this.transaction + "\'>"
			    + "\r <authentication>"
			    + "\r   <sessionid>" + this.sessionId + "</sessionid>"
			    + "\r </authentication>";
	    
	    String contentHead =
	            "\r<content>"
	                + "\r <function controlid=\"foobar\">";
	
	    String contentFoot = 
	                "\r </function>" 
	            + "\r</content>";
	
	    String templateFoot =
	        "\r </operation>"
	    + "\r</request>";
	    
	    if (multiFunc) {
	    	readXml = templateHead + readXml + templateFoot;
	    } else {
	    	readXml = templateHead + contentHead + readXml + contentFoot + templateFoot;
	    }
	
	    if (dryRun == true) {
	        setLastRequest(readXml);
	        
	        return null;
	    }
	    
		return readXml;
	}
	
	
	/**
	 * @return the session
	 */
	public APISession<ReturnType> getSession() {
		return session;
	}

	/**
	 * @param session the session to set
	 */
	public void setSession(APISession<ReturnType> session) {
		this.session = session;
	}	
	
	@SuppressWarnings("unchecked")
	private ReturnType proceedReturnResult(String result, SESSION_OPER_TYPE typeOp) throws Exception {
		Object res = null;
		JSONObject jObj = new JSONObject();
		
		switch ( this.returnFormat ) {
			case JSONOBJ:
				if ( typeOp == SESSION_OPER_TYPE.DELETE || typeOp == SESSION_OPER_TYPE.DETAIL
					|| typeOp == SESSION_OPER_TYPE.INSPECT)    {
					jObj = XML.toJSONObject(result);
					//res = jObj.toJSONArray(jObj.names());
					res = jObj;
				}
				else {
					if (typeOp == SESSION_OPER_TYPE.READ) {
						// need to construct the response JSON object with all the details 
						if (result.contains("[")) {
							// the response is a JSON Array containing the query result 
							jObj.put("OVERALL_STATUS", "SUCCESS");
							jObj.put("READ_RESULT", new JSONArray(result));
							jObj.put("ERROR", "NO_ERROR");
							res = jObj; 						
						} else{
						// the result is a JSON Object 
						jObj.put("OVERALL_STATUS", "SUCCESS");
						jObj.put("READ_RESULT", new JSONObject(result));
						jObj.put("ERROR", "NO_ERROR");
						res = jObj; 	
						}
					}
				}
				break;

			case XMLOBJ:
			case CSVOBJ:
				res = result;
				break;
				
			default: 
				throw new Exception("Unknown return format " + this.returnFormat);
		}
		return (ReturnType) res;
	}

	
	/**
     * Valid responses from post methods
	 * @param typeOp 
     *
     * @param JSONOBject response  The XML response object
     *
     * @throws Exception
     * @return null
     */
	//@SuppressWarnings("unchecked")
	private JSONObject processResultString(String res, SESSION_OPER_TYPE typeOp) throws IntacctSDKRuntimeException {
		boolean hasdata = false; 
		JSONObject resObj = null;
		Object jObj; 
	
		resObj = XML.toJSONObject(res);
		
		if ( resObj == null )
    		throw new IntacctSDKRuntimeException(typeOp + ": Invalid XML response");
			
		//System.out.println(this.getClass().getName()+ "::processResultString ----- Resulting raw JSON Object:  \n" + resObj.toString(1));
		
		//java.util.Iterator<String> keyArray_xml, keyArray_op, keyArray_res;
		//String key, key_xml, key_op, key_res;
		//Object value, value_xml, value_op, value_res;
		int countSuccessRecords = 0; 
		JSONObject dataObject = null, jsonRes = new JSONObject(), returnJSONObj, jsonExcObj;
		JSONArray dataResArray = new JSONArray(), dataSuccResArray = new JSONArray();
		String dataObjectName = "";
		//String bRes = "{a: true}"; 
		Map<String, Object> map = new HashMap<String, Object>();
		
		jsonRes = resObj.getJSONObject("response").getJSONObject("operation").getJSONObject("result");
		//System.out.println(" -------  JSON raw result is: \n" + jsonRes.toString(2));
		
		if (jsonRes.has("data")){
			hasdata = true; 
			dataObject = jsonRes.getJSONObject("data");
			countSuccessRecords = dataObject.getInt("count");
			if (countSuccessRecords > 0) {
				//dataObjectName =  (String) jsonRes.getJSONObject("data").names().get(1);
				// assumes the objectType list contains at least one object since there are results
				dataObjectName =  objectType.get(0);

				if ((typeOp == SESSION_OPER_TYPE.CREATE) || (typeOp == SESSION_OPER_TYPE.DELETE) || 
					(typeOp == SESSION_OPER_TYPE.READ) || (typeOp == SESSION_OPER_TYPE.UPDATE) ) {
					// check to see if there are results for this object; in case of failure for multiple objects 
					// the error ones are missing
					if (dataObject.has(dataObjectName) ){
						// due to the XML to JSON translation need to handle the case when there is only 
						// one result for this object type and wrap result in an array for consistency
						jObj = dataObject.get(dataObjectName);
						if (jObj instanceof JSONObject) {
							dataResArray.put(new JSONArray().put(jObj));
						} else dataResArray.put(jObj);
					}

					// check in case there are multiple types of objects in the result 
					int size = objectType.size() , count = 1;
					while(count < size){
						dataObjectName = objectType.get(count);
						if (dataObject.has(dataObjectName) ) {
							// due to the XML to JSON translation need to handle the case when there is only 
							// one result for this object type and wrap result in an array for consistency
							jObj = dataObject.get(dataObjectName);
							if (jObj instanceof JSONObject) {
								dataResArray.put(new JSONArray().put(jObj));
							} else dataResArray.put(jObj);
						}
						count++; 
					}
				}// end CRUUD operations
				else {
					// DETAIL or INSPECT
					map.put("OBJECT_TYPE", dataObjectName);
					map.put("FIELDS", ((JSONObject)((JSONObject)dataObject.get("Type")).get("Fields")).get("Field"));
				}
			}
		} // end has data 

		// check status of the request response 
		if (jsonRes.getString("status").equals("failure")){
			// the failed test case
			JSONObject errMsgObj = null; 
			JSONArray errArr = null;
			String errMsg = "";
			IntacctSDKRuntimeException sdkExc = new IntacctSDKRuntimeException("Request has failed"); 
			map.put("OVERALL_STATUS", "Failure");
			map.put("countSuccessfulRecrds", countSuccessRecords);
			if (countSuccessRecords > 0) {			
				map.put("CORRECT_RECORDS",dataResArray);
			} else map.put("CORRECT_RECORDS", ""); 
			errMsgObj = jsonRes.getJSONObject("errormessage");
			Object errObj = errMsgObj.get("error");
			if (errObj instanceof JSONObject){
				errMsg = ((JSONObject)errObj).getString("errorno") + "   " + ((JSONObject)errObj).getString("description") +
						"   " + ((JSONObject)errObj).getString("correction") + "   " +
						((JSONObject)errObj).getString("description2");
			} else	if (errObj instanceof JSONArray) {
				errArr = errMsgObj.getJSONArray("error");
				// assume the first one is the most descriptive; for an exhaustive handling include all the array elements
				errMsg = errArr.getJSONObject(0).getString("errorno") + "   " + 
						errArr.getJSONObject(0).getString("description") + "   " +
						errArr.getJSONObject(0).getString("description2") + "    " +
						errArr.getJSONObject(0).getString("correction");
			} else throw new IntacctSDKRuntimeException(
		    			"Misformed error message from platform for operation:  " + 
		    			typeOp + " for request\n " + getLastRequest());

			map.put("ERROR_MSG", errMsg);
			jsonExcObj = new JSONObject(map);
			sdkExc.setJsonDetails(jsonExcObj);
			throw sdkExc; 
		}
		else { 
			// this is the success case 
			map.put("OVERALL_STATUS", "Success");
			// ok this is a little hack for the xml format; to handle this properly requires more code 
			// refactoring... sorry, next time around 
			if (typeOp == SESSION_OPER_TYPE.READ) 
				map.put("READ_RESULT", dataResArray);
			else if (!(typeOp == SESSION_OPER_TYPE.INSPECT) && !(typeOp == SESSION_OPER_TYPE.DETAIL)) 
				map.put("CORRECT_RECORDS", dataResArray);
			map.put("ERROR", "NO_ERROR");
			returnJSONObj = new JSONObject(map);
			//System.out.println(this.getClass().getName() + "::processResultString  return JSON Return Object: " + returnJSONObj);			
		}
		return returnJSONObj;
	}
	
	public String getReturnFormatString()
	{
		String rf = null;
		switch ( this.returnFormat ) {
			case JSONOBJ:
				rf = RETURN_FORMAT.JSONOBJ.getName();
				//rf = "json";
				break;
			case XMLOBJ:
				rf = RETURN_FORMAT.XMLOBJ.getName();
				//rf = "xml";
				break;
			case CSVOBJ:
				rf = RETURN_FORMAT.CSVOBJ.getName();
				//rf = "csv";
				break;
			default:
				rf = "unknown";
		}
		
		return rf;
	}
	
	public void setTracer(APITracer tracer)
	{
		this.tracer = tracer;
	}

	/**
	 * @return the lastRequest
	 */
	public String getLastRequest() {
		return lastRequest;
	}

	/**
	 * @param lastRequest the lastRequest to set
	 */
	private void setLastRequest(String lastRequest) {
		this.lastRequest = lastRequest;
	}

	/**
	 * @return the lastResponse
	 */
	public String getLastResponse() {
		return lastResponse;
	}

	/**
	 * @param lastResponse the lastResponse to set
	 */
	private void setLastResponse(String lastResponse) {
		this.lastResponse = lastResponse;
	}
}

