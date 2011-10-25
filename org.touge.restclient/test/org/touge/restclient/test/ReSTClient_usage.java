// ### **ReSTClient** wraps java.net.HttpURLConnection and provides facilities for deserialization, explicit and implicit error handling, and request header initialization.
// ReSTClient sources are [on GitHub](https://github.com/kgilmer/touge).
/*
 * This file is in the public domain, furnished "as is", without technical
 * support, and with no warranty, express or implied, as to its usefulness for
 *	any purpose.
 */
package org.touge.restclient.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.touge.restclient.ReSTClient;
import org.touge.restclient.ReSTClient.HttpMethod;
import org.touge.restclient.ReSTClient.Response;
import org.touge.restclient.ReSTClient.ResponseDeserializer;
import org.touge.restclient.ReSTClient.URLBuilder;

// ### Examples of how to use ReSTClient.
public class ReSTClient_usage {
	
	public static void main(String[] args) throws IOException {		
		
		ReSTClient restClient = new ReSTClient();
				
		//The most common simple thing to do is GET and return the body
		//as a String.  ReSTClient:
		String responseBody = 
			restClient.callGet("localhost");		
		
		//Another common thing is to POST a String to a server:
		restClient.callPost("localhost", "my content");
		
		//Print debug info to System.out
		restClient.setDebugWriter(new PrintWriter(System.out));
		
		//Getting more complex, we can specify a deserializer that will turn
		//the server response into an Object our client wants.  Here we use
		//one of the few predefined deserializers.
		//Call GET on localhost using long form, pass in a predefined deserializer, no body (since GET), and no custom headers.
		Response<String> resp = 
			restClient.call(HttpMethod.GET, "localhost", ReSTClient.STRING_DESERIALIZER, null, null);
		pl("Response: " + resp.getContent());
		
		//Call get and provide a custom deserializer into a custom type.
		Response<Double> cresp = 
			restClient.call(HttpMethod.GET, "localhost", new ResponseDeserializer<Double>() {

				@Override
				public Double deserialize(InputStream input, int responseCode, Map<String, List<String>> headers) throws IOException {
					//Here one would check the server response and read the input stream.
					String [] s = "321 asdf".split(" ");  
					return Double.parseDouble(s[1]);
				}
				
			}, null, null);
		
		//Disable debug output
		restClient.setDebugWriter(null);
		
		//API allows for code that specifically checks for errors, 
		// or relies on exception handling.
		// First we explicitly check for errors.
		if (!resp.isError())
			pl(resp.getContent());		
		
		// Or we can use an error handler instead.
		restClient.setErrorHandler(new ReSTClient.ErrorHandler() {
			
			@Override
			public void handleError(int code, String message) throws IOException {
				System.err.println("HTTP Error " + code + " occurred.");
			}
		});
		
		//Use URLBuilder to build a url.
		URLBuilder localhost = restClient.buildURL("localhost");
		
		//Prints http://localhost
		pl(localhost);
		
		//Prints http://localhost/myservlet
		pl(
				localhost.append("//myservlet"));		
		
		// do a POST with the short-form method
		Response<Integer> rc = 
			restClient.callPost(localhost, "My POST content");
		
		// Check the last response for an error.
		if (rc.isError())
			pl("boo!");
					
		// Call GET and pass back the raw response InputStream to the client.
		pl(
				restClient.callGet(localhost, ReSTClient.INPUTSTREAM_DESERIALIZER)
					.getContent().available());
		
		// Create a rest client that will throw exceptions on all HTTP and I/O errors.
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		
		//This GET will deserialize server response as a string and 
		//throw IOException on any error.  
		Response<String> rs = 
			restClient.callGet(localhost, ReSTClient.STRING_DESERIALIZER);
		
		pl(rs.getContent());
		
		// Subsequent calls to this rest client will not throw exceptions on HTTP errors.
		restClient.setErrorHandler(null);
					
		//Since we do not have an error handler, this call will not throw IOException.
		rs = restClient.callGet(localhost.copy("asdf"), ReSTClient.STRING_DESERIALIZER);
		
		if (rs.isError())
			pl("Error: " + rs.getCode());
		
		//Set the error handler to throw all errors.
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		
		//The following line will throw IOException.
		try {
			rs = restClient.callGet(localhost.copy("/asdf"), ReSTClient.STRING_DESERIALIZER);
			//Error is thrown when trying to get content.
			pl(rs.getContent());
		} catch (IOException e) {
			pl("Error: " + e.getMessage());
		}
		
		//following line will throw IOException 
		try {
			//Error is thrown when trying to get content.
			String respstr = restClient.callGet("localhost/asdf");				
		} catch (IOException e) {
			pl("Error: " + e.getMessage());
		}
		
		// Only throw errors relating to server problems.
		restClient.setErrorHandler(ReSTClient.THROW_5XX_ERRORS);
		
		//following line will throw IOException 
		try {
			rs = restClient.callGet("localhost/asdf", ReSTClient.STRING_DESERIALIZER);
			//Error is not thrown when trying to get content because it is not a server error, but rather a 404.
			
			pl("Should be true: " + rs.isError());
		} catch (IOException e) {
			pl("Error: " + e.getMessage());
		}
		
		//following line will throw IOException 
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		try {
			//Error is thrown when trying to get content.
			String respstr = restClient.callGetContent(localhost.copy("/asdf"), ReSTClient.STRING_DESERIALIZER);				
		} catch (IOException e) {
			pl("Error: " + e.getMessage());
		}
		
		//Multipart POST with a file upload.
		Map<String, Object> body = new HashMap<String, Object>();
		
		body.put("tkey", "tval");
		body.put("myfile", new ReSTClient.FormFile("/tmp/boo.txt", "text/plain"));
		
		restClient.callPostMultipart("localhost", body);
		
		//PUT, short form, throw exception on error
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		Response<Integer> mr = restClient.callPut("localhost", new ByteArrayInputStream("boo".getBytes()));
		
		//HTTP DELETE
		Response<Integer> drc = restClient.callDelete(localhost.copy("/deleteurl"));
		pl("should be true: " + drc.isError());
		
		//HTTP HEAD
		Response<Integer> mrc = restClient.callHead(localhost);
		
		pl("should be false: " + mrc.isError());					

		//When programmatically building URLs it can be nice to have something
		//take care of the concatenation, path separators, and scheme.  
		//URLBuilder is a static helper that does this.
		
		//This URLBuilder builds https://citibank.com/secureme/halp	
		pl(
				restClient.buildURL("htTPS://citibank.com/secureme/").append("/halp"));
		
		// Builds http://yahoo.com/a/mystore/toodles?index=5
		pl(restClient.buildURL("yahoo.com")
										.append("a")
										.append("mystore/")
										.append("toodles?index=5"));
				
		// Builds http://me.com/you/andi/like/each/ohter
		pl(
				restClient.buildURL("me.com/")
					.append("/you/")
					.append("/andi/")
					.append("like/each/ohter/"));
		
		// Builds https://myhost.com/first/second/third/forth/fith/mypage.asp?i=1&b=2&c=3
		pl(
				restClient.buildURL(
						"myhost.com", 
						"first/", 
						"//second", 
						"third/forth/fith", 
						"mypage.asp?i=1&b=2&c=3").setHttps(true));
		
		// Create child URLs from base URLs
		URLBuilder origurl = restClient.buildURL("myhost.net/","/homepage");
		URLBuilder newurl = origurl
			.copy()
			.append("asdf/adf/reqotwoetiywer")
			.setHttps(true);
						
		// Original URL: http://myhost.net/homepage
		pl(origurl);
		// Child URL: https://myhost.net/homepage/asdf/adf/reqotwoetiywer
		pl(newurl);
	}
	
	private static void pl(Object message) {
		System.out.println(message);
	}
}
