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
 * OVERVIEW
 * The general pattern for using this SDK is to first create an instance of api_session and call either
 * connectCredentials or connectSessionId to start an active session with the Intacct Web Services gateway.
 * You will then pass the api_session as an argument in the api_post class methods.  intacctws-java handles all
 * XML serialization and de-serialization and HTTPS transport.
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
 *
 *
 */

package com.intacct.ws;

import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

import com.intacct.ws.exception.IntacctSDKRuntimeException;

public class APIUtil {

	public static String ErrorToString(String errorMsg) {
		JSONException ex = new JSONException(errorMsg);
		//throw ex;
		return errorMsg;
	}

	@SuppressWarnings("unchecked")
	public static String JSONObjErrorToString(Object obj) {
		
		java.util.Iterator<String> keyArray;
		String key, key_err;
		Object value, value_err;
		
		String errorno = "";
		String description = "";
		String description2 = "";
		String correction = "";
		
		if ( obj == null ) {
			JSONException ex = new JSONException("Malformed error");
			throw ex;
		}
		
		keyArray = ((JSONObject) obj).keys();
		
		key_err = keyArray.next().toString();
        value_err = ((JSONObject) obj).opt(key_err);
        
        if ( value_err instanceof JSONArray ) {
        	value_err = ((JSONArray) value_err).getJSONObject(0);
        }
        keyArray = ((JSONObject) value_err).keys();
		while ( keyArray.hasNext() ) 
		{
            key = keyArray.next().toString();
            value = ((JSONObject) value_err).opt(key);
            
            if ( key.equals("errorno") )
            		errorno = (String) value;
            else if ( key.equals("description") )
            	description = (String) value;
            else if ( key.equals("description2") )
            	description2 = (String) value;
            else if ( key.equals("correction") )
            	correction = (String) value;
		}
		
		JSONException ex = new JSONException("\rerrorno: " + errorno + "\rdescription: " + description + "\rdescription2: " + description2 + "\rcorrection: " + correction);
		//throw ex;
		return ex.toString();
	}

	public static String JSONToCsv(JSONObject response) {
		return CDL.rowToString(response.toJSONArray(response.names()));
	}
	
	public static JSONObject csvToJSON(String csv) {
		// Put the header line of the result in JSONArray  
		JSONTokener jTok = new JSONTokener(csv);
	    JSONArray jHeaderArray = CDL.rowToJSONArray(jTok);
	    
	    // Put the values in a JSONArray
	    JSONArray jValuesArray = CDL.toJSONArray(csv);
	    
	    // Put the result into a JSONObject
	    JSONObject jObj = jValuesArray.toJSONObject(jHeaderArray);
	    
	    return jObj;
	}
	
	public static String StringToXML(String key, String values) throws IntacctSDKRuntimeException{
		String xml = "";
		JSONArray jArrayValue = null;

		if ( values == null || values.length() == 0 )
				throw new IntacctSDKRuntimeException("StringToXML - No values specified");
		
		jArrayValue = new JSONArray(values);
		
		int lenghtArrayVal = jArrayValue.length();
		
		if ( jArrayValue == null || jArrayValue.length() == 0 )
			throw new IntacctSDKRuntimeException("StringToXML - No values specified");
		else if ( lenghtArrayVal == 1 )
			return "<" + key + ">" +  jArrayValue.getString(0) + "</" + key + ">";

		try {
			jArrayValue.getInt(0);
		}
		catch ( Exception e) {
			xml = "<" + key + ">";
		}

		for ( int ix = 0; ix < lenghtArrayVal ; ix++ ) {
			JSONObject jObj = jArrayValue.getJSONObject(ix);
			xml += XML.toString(jObj);	
		}
	    
	    try {
			jArrayValue.getInt(0);
		}
		catch ( Exception e) {
			xml += "</" + key + ">";
		}
	    
	    return xml;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONArray htmlspecialchars(JSONArray records, String object, String nameField) {

		JSONObject jObj = null;
		JSONArray jArray = null;
		int idx = 0;
		java.util.Iterator<String> keyArray_object;
		String key_object, data;
		Object value_object;
		
		jArray = records;
		
		for ( idx = 0; idx < jArray.length(); idx++ ) {
    		jObj = jArray.getJSONObject(idx);
    		
    		keyArray_object = jObj.keys();
    		while ( keyArray_object.hasNext() ) 
    		{
                // the object name 
    			key_object = keyArray_object.next().toString();

    			if ( key_object.equals(object) ) {
        			// the object fields: (nameField : valueField)
                    value_object = jObj.opt(key_object);

                    if ( ((JSONObject) value_object).has(nameField) ) {
                		data = ((JSONObject) value_object).get(nameField).toString();

                		data = data.replaceAll( "&amp;", "//\\&(?!#[0-9]*;)//");
                		data = data.replace("&quot;", "\"");
                		data = data.replaceAll("&apos;", "'");
                		data = data.replaceAll("&lt;", "<");
                		data = data.replaceAll("&gt;", ">");
                		
                		((JSONObject) value_object).put(key_object, data);
                	}
                }
            }
		}
		
		return records;
	}

	public static String htmlspecialchars(String value) {

		String data;

		data = value;
		data = data.replaceAll( "&amp;", "//\\&(?!#[0-9]*;)//");
		data = data.replace("&quot;", "\"");
		data = data.replaceAll("&apos;", "'");
		data = data.replaceAll("&lt;", "<");
		data = data.replaceAll("&gt;", ">");
		
		return data;
	}

	
	/**
     * Get the number of records of the JSONArray
     *
     * @param JSONArray jArray
     * 
     * @return int count 
     */
	public static int getNbRecords(JSONArray jArray) {
        return jArray.length();
    }

	@SuppressWarnings("unchecked")
	public static String inClauseConstructor(String object, String nameField, String delimiter, JSONArray jArray) {
		
		String result = "";
		JSONObject jObj = null;
		java.util.Iterator<String> keyArray_object;
		String key_object;
		Object value_object;
		
		for ( int ix = 0; ix < jArray.length(); ix++ ) {
    		jObj = jArray.getJSONObject(ix);
    		
    		keyArray_object = jObj.keys();
    		while ( keyArray_object.hasNext() ) 
    		{
                // the object name 
    			key_object = keyArray_object.next().toString();

    			if ( key_object.equals(object) ) {
        			// the object fields: (nameField : valueField)
                    value_object = jObj.opt(key_object);

                	if ( ((JSONObject) value_object).has(nameField) ) {
                		if ( result.length() == 0 )
                			result = ((JSONObject) value_object).get(nameField).toString();
                		else 
                			result += result = "," + ((JSONObject) value_object).get(nameField).toString();
                	}
                }
            }
		}
		
		return result;
	}
	
	/**
     * Remove a key from JSONObject
     * @param nameField 
     * @param JSONArray  jArray     	result from post to Intacct Web Services
     *
     * @return JSONArray response		returned object 
     */
    public static JSONArray unsetNameField(JSONArray jArray, String object, String nameField) {
		// TODO remove the pair key-> value based on nameField of the "object"    
    	JSONObject jObj = null;
    	JSONArray response = jArray;
    	@SuppressWarnings("rawtypes")
		java.util.Iterator keyArray_object, keyArray_field;
		String key_object, key_field;
		Object value_object;
    	
    	// this line return the value of the object removed or null
    	//response = (JSONObject) jObj.remove(nameField);

		for ( int ix = 0; ix < jArray.length(); ix++ ) {
    		jObj = jArray.getJSONObject(ix);
    		
	        keyArray_object = jObj.keys();
			while ( keyArray_object.hasNext() ) 
			{
	            key_object = keyArray_object.next().toString();
	            
	            if ( key_object.equals(object) ) {
		            value_object = jObj.opt(key_object);
	            	keyArray_field = ((JSONObject) value_object).keys();
	            	while ( keyArray_field.hasNext() ) 
	        		{
	                    key_field = keyArray_field.next().toString();
	                    
	                    if ( key_field.equals(nameField) ) {
	                    	// remove the nameField
	                    	((JSONObject) value_object).remove(nameField);
	                    	break;
	                    }
	        		}
	            }
			}
			
			// jArray.remove(ix);
			response.put(ix, jObj);
		}
		
		return response;
	}

	/**
     * All this functions from here are helpful for debugging purposes.  
     *
     */
	public static void printJSONObject(JSONObject jObj) {
		
		if ( jObj == null )
			System.out.println(" Object null");
		else 
			//System.out.println(jObj.toString(10));
			System.out.println(jObj.toString(3));
		
		return;
	}

	public static void printJSONArray(JSONArray names) {

		System.out.println(names.toString());
		
		return;
	}
	
	public static void printResultLength(int length) {
		System.out.println(length);
		
		return;
		
	}
	
	public static void printString(String texte) {
		System.out.println(texte);
		
		return;
	}
}
	