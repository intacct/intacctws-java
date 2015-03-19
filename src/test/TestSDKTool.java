/**
 * @author    Marian Crisan <mcrisan@intacct.com>
 * 
 * Copyright (c) 2014, Intacct OpenSource Initiative
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
 *
 * *  The Intacct code uses the 3-rd party library org.json which has the following license agreement
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
 *
 *
 * OVERVIEW
 * The general pattern for using this SDK is to first create an instance of api_session and call either
 * connectCredentials or connectSessionId to start an active session with the Intacct Web Services gateway.
 * You will then pass the api_session as an argument in the api_post class methods.  intacctws-java handles all
 * XML serialization and de-serialization and HTTPS transport.
 */

package test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;

import com.intacct.ws.APISession;
import com.intacct.ws.APITracer;
import com.intacct.ws.APIUtil;
import com.intacct.ws.exception.IntacctSDKRuntimeException;
import com.intacct.ws.util.ConfigConstants;
import com.intacct.ws.util.ConfigLoader;

class OutputTracer implements APITracer
{
	public void traceRequest(int requestId, String requestXML)
	{
		System.out.println("Request " + requestId + ": " + requestXML);
	}
	
	public void traceResponse(int requestId, String responseXML)
	{
		System.out.println("Response for request " + requestId + ": " + responseXML);
	}
}

public class TestSDKTool {

	@Test
	/**
	 * The values of the Id/Passwords need to be setup in the environment.
	 *  
	 */
	public void testEnvVariables(){
		
		   Map<String, String> env = System.getenv();
		   System.out.println("Company:  " + env.get(ConfigConstants.COMPANY));
		   System.out.println("User ID:  " + env.get(ConfigConstants.WS_USER_ID));
		   System.out.println("User Passwd:  " + env.get(ConfigConstants.WS_PASSWD));
		   System.out.println("DB ID:  " + env.get(ConfigConstants.DB_ID));
		   System.out.println("DB Passwd:  " + env.get(ConfigConstants.DB_PASSWD));
		   System.out.println("URL End Point:  " + env.get(ConfigConstants.END_POINT_URL));
		   
	        /*
	         for (String envName : env.keySet()) {
	            System.out.format("%s=%s%n",
	                              envName,
	                              env.get(envName));
	        } */
	        
	}
	
	@Test
	public void testCreateJSON() {
		
		JSONObject jObj = null;
		
		try {
			System.out.println("Test Create JSON");
						
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
		
/*			APISession<JSONArray> session = APISession.getTRXJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD));
*/					 
			OutputTracer tracer = new OutputTracer();
			
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			//String data = "[{location: { locationid: 'cmc1', name: 'cmc1'} }, {location: { locationid: 'cmc2', name: 'cmc2'} }, {location: { locationid: 'cmc3', name: 'cmc3'} }, {location: { locationid: 'cmc4', name: 'cmc4'}}, "
			//		+ "{location: { locationid: 'cmc5', name: 'cmc5'} }, {location: { locationid: 'cmc6', name: 'cmc6'} }, {location: { locationid: 'cmc7', name: 'cmc7'} }, {location: { locationid: 'cmc8', name: 'cmc8'}  }]";
			//String data = "[{location: {locationid:'ha49', name:'hTest49'} }, {location: {locationid:'ha33', name:'hTest33'} }]";
			//		+ "{location: {locationid:'ha46', name:'hTest46'} }, 
			//String data = "[{location: {locationid:'ha33', name:'hTest33'} }]";
//			String data = "[{customer: {customerid:'ha02', name:'hTest02'} }]";
//			String data = "[{customer: {customerid:'ha06', name:'hTest06'} } ,{customer: {customerid:'ha07', name:'hTest07'} } ,"  
//					   + "{customer: {customerid:'ha02', name:'hTest02'}} , {customer: {customerid:'ha08', name:'hTest08'} } ]";
			String data = "[{customer: {customerid:'ha08', name:'hTest08'} } ,{customer: {customerid:'ha09', name:'hTest09'} }, "
					+ " {department: {departmentid:'HA07', title:'hTest07'} } ,{department: {departmentid:'HA06', title:'hTest06'} } , "
					+ " {customer: {customerid:'ha10', name:'hTest10'} }, {customer: {customerid:'ha11', name:'hTest11'} }]";	
			List<String> objects = new ArrayList<String>();  
			objects.add("department");
			objects.add("customer");

			JSONArray records = new JSONArray(data);
			jObj = session.create(objects, records);
			
			assertNotNull("Create - get null object", jObj);
			System.out.println("Result JSON create object status:  " );
			APIUtil.printJSONObject(jObj);
			System.out.println("FIN Test Create JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
			
		}  catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testUpdateJSON() {
		
		JSONObject jObj = null;
		
		try {
			System.out.println("Test Update JSON");
			
			APISession<JSONObject> session =  APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
//			String data = "[{DDSJOB: {recordno:'84', OBJECT:'htest'} }]";
//			String data = "[{location: {locationid:'ha12', name:'hTest12-3'} }, {location: {locationid:'ha13', name:'hTest13-3'} }, {location: {locationid:'ha01', name:'hTest13-3'} } ]";
//			String data = "[{location: {locationid:'ha14', name:'hTest14-1'} }]";
	
//			String data = "[{customer: {customerid:'ha06', name:'hTest06'} } ,{customer: {customerid:'ha07', name:'hTest07'} } ,"  
//					   + "{customer: {customerid:'ha02', name:'hTest02'}} ]"; 
//			String data = "[{customer: {customerid:'ha02', name:'hTest02'} } ,{customer: {customerid:'ha03', name:'hTest03'} } ,"
//					+ "{customer: {customerid:'ha06', name:'hTest06'} }]";
//			String data = "[{customer: {customerid:'ha02', name:'hTest02'} } ,{customer: {customerid:'ha03', name:'hTest03'} } ]";
			String data = "[{customer: {customerid:'ha01', name:'hTest01-upd'} }, {customer: {customerid:'ha02', name:'hTest02-upd'} } "
						+ ",	{department: {departmentid:'HA02', title:'hTest02-upd'} }]";
			List<String> objects = new ArrayList<String>(); 
			objects.add("customer"); objects.add("department");
			
			JSONArray records = new JSONArray(data);
			jObj = session.update(objects, records); 
			
			assertNotNull("Update - get null object", jObj);
			
			APIUtil.printJSONObject(jObj);
			
			System.out.println("FIN Test Update JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		}	catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Verifies if input data exists; if so does update otherwise it does insert. 
	 *   NOTE: need to provide the recordno for the entries that need to be updated; otherwise the read will not return those 
	 *		 and as a result it will try to insert those records resulting in unique constraint violation 
	 */
	@Test
	public void testUpsertJSON() {
		JSONObject jObj = null;
		
		try {
			System.out.println("Test Upsert JSON");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			//String data = "[{location: {recordno: 15586, locationid: 'ha11', name: 'cmc5.555555'}}, {location: {locationid: 'ha12', name: 'ha12'}}]";
			//String data = "[{location: {locationid: 'ha11', name: 'cmc5.555555'}}]";  //, {location: {locationid: 'ha12', name: 'ha12'}}]";
			//String data = "[{location: {recordno: 96, locationid:'ha14', name:'hTest14-upsert'} }, "
			//		+ "{location: {locationid: 'ha38', name: 'ha38'}}, "
			//		+ "{location: {locationid: 'ha39', name: 'ha39'}}]";
			/*
			 * NOTE: need to provide the record no for the entries that need to be updated; otherwise the read will not return those 
			 * and as a result it will try to insert those records resulting in unique constraint violation 
			 */
			//String data = "[{customer: {recordno: 166, customerid:'ha06', name:'hTest06'}} ]"; //, {customer: {customerid:'ha08', name:'hTest08'} } ]";  //,{customer: {customerid:'ha07', name:'hTest07'} },"
				//	+ "{customer: {customerid:'ha05', name:'hTest05-upsert'} } ]";
			String data = "[{customer: {recordno: 218, customerid:'ha21', name:'hTest21-upd'} }, {customer: {customerid:'ha15', name:'hTest15'} }, "
					+ "{customer: {recordno: 92, customerid:'ha04', name:'hTest04-upd+'} }, " //{customer: {recordno: 91, customerid:'ha05', name:'hTest05-upd'} }, "
					+ "{customer: {customerid:'ha14', name:'hTest14'} }, {customer: {recordno: 182, customerid:'ha06', name:'hTest06-updt'} } ]";
			
			JSONArray records = new JSONArray(data);
			
			jObj = session.upsert("customer", records, "recordno", "customerid,name", false);
			
			assertNotNull("Upsert - get null object", jObj);
			
			APIUtil.printJSONObject(jObj);
			System.out.println("FIN Test Upsert JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);			
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test	
	public void testReadJSONObject() {
		
		String object;
		String fields;
		String keys;
		
		JSONObject jObj = null;
		
		try {
			System.out.println("Test Read JSON");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD));
			
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			// APITracer has 2 methods:
			// traceRequest(requestId, requestXML);
			// traceResponse(requestId, responseXML);
			// APISession calls these methods when read, create, update, delete, readByQuery upsert, ... are called 
			
			//object = "ddsjob";fields = "*";	keys = "82, 84";
			object ="customer"; fields="CNY#, RECORD#, CUSTOMERID, NAME "; keys="71,92,140,141";  // 161 does not exist
			
			jObj = session.read(object, keys, fields);
			
			assertNotNull("ReadJSON - get null object", jObj);
			System.out.println("JSON Return object: " + jObj.toString(1));
			System.out.println("FIN Test Read JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	@Test	
	public void testReadXML() {
		
		String object;
		String fields;
		String keys;
		
		String xmlRes = null;
		
		try {
			System.out.println("Test Read XML");
			
			APISession<String> session = APISession.getXMLInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY), 
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD),
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD));
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			object ="customer"; fields="CNY#, RECORD#, CUSTOMERID, NAME "; keys="71,92";  
			
			//xmlRes = XML.toString(session.read(object, keys, fields));
			xmlRes = session.read(object, keys, fields);
			
			assertNotNull("ReadXML - get null object", xmlRes);
			
			System.out.println("FIn Test Read XML");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test	
	public void testReadByNameXML() {
		
		String object;
		String fields;
		String keys; 
		
		String jObj = null;
		
		try {
			System.out.println("Test ReadByName XML");
			
			APISession<String> session = APISession.getXMLInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD),
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD));
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			object = "ddsjob";
			fields = "RECORDNO,OBJECT,JOBTYPE,STATUS,ERROR,FILELIST"; 			//fields = "*";
			keys = "35,36,37";  // RECORDNO
			
			jObj = session.readByName(object, keys, fields);
			
			assertNotNull("ReadByName XML - get null object", jObj);
			
			System.out.println("FIn Test ReadByName XML");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
*/
	@Test	
	public void testReadByNameJSON() {
		
		String object;
		String fields;
		String keys; 
		
		JSONObject jObj = null;
		
		try {
			System.out.println("Test ReadByName JSON");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			// NOTE: fields are case sensitive 
			object = "ddsjob";	fields = "RECORDNO,OBJECT,JOBTYPE,STATUS,ERROR,FILELIST";	keys = "35,36";  // RECORDNO
		//	object = "customer";	fields="*"; keys="166";  // 161 does not exist
			
			jObj = session.readByName(object, keys, fields);
			
			assertNotNull("ReadByName JSON - get null object", jObj);
			
			APIUtil.printJSONObject(jObj);
			System.out.println("FIn Test ReadByName JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test	
	public void testReadRelatedJSON() {
		
		String object, fields, keys, relation; 
		
		JSONObject jObj = null;
		
		try {
			System.out.println("Test ReadRelated JSON");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer
			
			object = "ddsjob";
			fields = "RECORDNO,OBJECT_W,JOBTYPE,STATUS,ERROR,FILELIST";
			keys = "35";  // RECORDNO
			relation = "";
			
			jObj = session.readRelated(object, keys, relation, fields);
			
			assertNotNull("ReadByName JSON - get null object", jObj);
			
			APIUtil.printJSONObject(jObj);
			System.out.println("FIn Test ReadRelated JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);		
		}	catch (IOException ex)	{
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testReadByQueryJSON() {
		
		JSONObject jObj = null;
		String query, fields, object;
		int maxRecords = 1000;
		
		try {
			System.out.println("Test ReadByQuery JSON");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer

			//object = "location";  query = "RECORDNO in (56, 62)";  fields = "RECORDNO,LOCATIONID,NAME";
			object = "customer";
			//query = "CUSTOMERID in (\"ha03\", \"ha02\")"; 
			query = "CUSTOMERID in (\"ha23\", \"ha19\")";
			fields = "RECORDNO,CUSTOMERID,NAME";
			
			jObj = session.readByQuery(object, query, fields, maxRecords);
			
			assertNotNull("ReadByQuery - get null object", jObj);
			System.out.println("  ReadByQuery JSON Result:  ");
			APIUtil.printJSONObject(jObj);
			System.out.println("FIN Test ReadByQuery JSON");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		}catch (IOException ex)		{
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDelete() {
		
		boolean result;
		String object, keys;
		
		try {
			System.out.println("Test Delete");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer

//			object = "location";
//			keys = "76,77"; // RECORDNO  
			object = "customer";  keys="244,245,246,247"; //"217,218,219,216, 215, 214,223, 224,220"; // RECORD#

			//object="department"; keys="54";
			
			result = session.delete(object, keys);
			if ( result )
				System.out.println("Delete - Okay");
			else 
				System.out.println("Delete - failes");
			
    		// assertNotNull("Delete - get null object", result);
    		    		
			System.out.println("FIN Test Delete");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDeleteAll() {
		
		boolean result;
		String object, keys;
		int max;
		
		try {
			System.out.println("Test Delete ALL");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer

			//object = "ddsjob";	max = 2;  keys = "RECORDNO"; // RECORDNO or id  
			object = "customer";	max = 2;  keys = "RECORDNO"; // RECORDNO or id
			
			result = session.deleteAll(object, max, keys); 
			if ( result )
				System.out.println("Delete ALL - Okay");
			else 
				System.out.println("Delete ALL - failes");
			
			System.out.println("FIN Test Delete ALL");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);		
		}	catch (IOException ex)
		{
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDeleteByQuery() {
		
		boolean result;
		String object, keys, query = null;
		int max;
		
		try {
			System.out.println("Test DeleteByQuery");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer

			//object = "ddsjob"; keys = "RECORDNO"; // RECORDNO or id
			object = "customer"; keys = "RECORDNO";
			max = 2;
			query = new String(" RECORDNOIN (166, 167)");

			result = session.deleteByQuery(object, query, max, keys); 
			if ( result )
				System.out.println("DeleteByQuery - Okay");
			else 
				System.out.println("DeleteByQuery - failes");
			
			System.out.println("FIN Test DeleteByQuery");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);		
		}  catch (IOException ex)   {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testInspect() {
		
		boolean detail;
		String object;
		JSONObject jObj = null;
		
		try {
			System.out.println("Test Inspect");
			
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			//OutputTracer tracer = new OutputTracer();
			//session.setTracer(tracer); // tracer is instance of interface APITracer

			//object = "ddsjob";
			object = "customer";

			//jObj = session.inspect(object, false); 
			jObj = session.inspect(object, true);
			
			assertNotNull("Inspect - get null object", jObj);
			
			APIUtil.printJSONObject(jObj);
			
			System.out.println("FIN Test Inspect");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);		
		}  catch (IOException ex) {
			ex.printStackTrace();
			System.exit(-1); 
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
	@Test
	public void invokeService() throws IOException {
		String xmlReadPayload = "<read><object>customer</object><keys>71,92,140,141</keys><fields>CNY#, RECORD#, CUSTOMERID, NAME </fields><returnFormat>json</returnFormat></read>";
		JSONObject jsonReturn; 
		System.out.println("invokeService");
		
		try {
			APISession<JSONObject> session = APISession.getJSONInstance(ConfigLoader.getProperty(ConfigConstants.COMPANY),
					ConfigLoader.getProperty(ConfigConstants.WS_USER_ID), ConfigLoader.getProperty(ConfigConstants.WS_PASSWD), 
					ConfigLoader.getProperty(ConfigConstants.DB_ID), ConfigLoader.getProperty(ConfigConstants.DB_PASSWD)); 
			OutputTracer tracer = new OutputTracer();
			session.setTracer(tracer); // tracer is instance of interface APITracer

			jsonReturn = session.invokeService(xmlReadPayload, false, null);
			
			APIUtil.printJSONObject(jsonReturn);

			System.out.println("FIN Test invokeService");
		} catch (IntacctSDKRuntimeException  sdkExc){
			System.out.println("------------ Caught IntacctSDKRuntimeException. Request status:  \n " + sdkExc.getJsonDetails().toString(3));
			System.out.println("------------------------------------------------------------------------");
			sdkExc.printStackTrace();
			System.exit(-1);		
		}  catch (IOException ex) {
			ex.printStackTrace();
			System.exit(-1); 
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	
	}

}
