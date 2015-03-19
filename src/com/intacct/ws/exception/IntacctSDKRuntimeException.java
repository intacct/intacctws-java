package com.intacct.ws.exception;

import org.json.JSONObject;
/**
 * Copyright (c) 2015, Intacct OpenSource Initiative
 * All rights reserved.
 * 
 * 
 * Root exception class for the Intacct Java SDK providing programatic access to the Intacct Platform
 * Implemented as a RuntimeException in order to provide maximum flexibility for partner extensions
 * 
 * It encapsulates information on the type of operation that caused the exception 
 * Returns a JSONObject detailing the conditions of the exception and data that caused it
 * Upon exception the invocation is considered as failed and no data changed (unless explicitly 
 * documented side effects in the method) 
 * 
 */
public class IntacctSDKRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum OPERATION {
		CREATE, UPDATE, DELETE, UPSERT , READ, READ_QUERY, READ_BY_NAME, DELETE_ALL, DELETE_BY_QUERY, INSPECT
	}
	
	private OPERATION operation;

	private JSONObject jsonDetails = new JSONObject(); 
	
	public IntacctSDKRuntimeException() {
		// TODO Auto-generated constructor stub
	}

	public IntacctSDKRuntimeException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public IntacctSDKRuntimeException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public IntacctSDKRuntimeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public IntacctSDKRuntimeException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

	public IntacctSDKRuntimeException(OPERATION opr, JSONObject jsonErrObj){
		super();
		operation = opr; 
		jsonDetails = jsonErrObj;
	}
			
			
	public JSONObject getJsonDetails() {
		return jsonDetails;
	}

	public void setJsonDetails(JSONObject jsonErrObj) {
		this.jsonDetails = jsonErrObj;
	}

	public OPERATION getOperation() {
		return operation;
	}

}
