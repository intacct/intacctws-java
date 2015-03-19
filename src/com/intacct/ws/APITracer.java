package com.intacct.ws;

public interface APITracer {
	public void traceRequest(int requestId, String requestXML);
	public void traceResponse(int requestId, String responseXML);
}
