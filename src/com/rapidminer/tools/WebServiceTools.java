package com.rapidminer.tools;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import com.sun.xml.internal.ws.client.BindingProviderProperties;

/**
 * 
 * @author Simon Fischer
 *
 */
public class WebServiceTools {

	private static final int TIMEOUT = 4000;

	public static void setTimeout(BindingProvider port) {
		setTimeout(port, TIMEOUT);
	}
	
	/** Sets the timeout for this web service client. Every port created
	 *  by a JAX-WS can be cast to BindingProvider. */
	public static void setTimeout(BindingProvider port, int timeout) {
		Map<String, Object> ctxt = ((BindingProvider) port).getRequestContext();
//		ctxt.put("com.sun.xml.ws.developer.JAXWSProperties.CONNECT_TIMEOUT", timeout);
//		ctxt.put("com.sun.xml.ws.connect.timeout", timeout);
//		ctxt.put("com.sun.xml.ws.request.timeout", timeout); 
//		ctxt.put("com.sun.xml.internal.ws.request.timeout", timeout);
		ctxt.put(BindingProviderProperties.REQUEST_TIMEOUT, timeout);
		ctxt.put(BindingProviderProperties.CONNECT_TIMEOUT, timeout);
	}
}
