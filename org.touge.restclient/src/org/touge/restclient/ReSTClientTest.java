/*
 * This file is in the public domain, furnished "as is", without technical
 * support, and with no warranty, express or implied, as to its usefulness for
 *	any purpose.
 */
package org.touge.restclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.touge.restclient.ReSTClient.HttpMethod;
import org.touge.restclient.ReSTClient.Response;
import org.touge.restclient.ReSTClient.ResponseDeserializer;
import org.touge.restclient.ReSTClient.URLBuilder;


public class ReSTClientTest {
	
	public static void main(String[] args) throws IOException {		
		
		ReSTClient restClient = new ReSTClient();
				
		//Simplest GET, with the short-form method.  Call get, and deserialize the response
		//into a String.
		System.out.println(
				restClient.callGet("localhost"));		
		
		//Call GET on localhost using long form, pass in a predefined deserializer, no body (since GET), and no custom headers.
		Response<String> resp = 
			restClient.call(HttpMethod.GET, "localhost", ReSTClient.STRING_DESERIALIZER, null, null);
		System.out.println("Response: " + resp.getContent());
		
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
		
		
		//API allows for code that specifically checks for errors, 
		// or relies on exception handling.
		// Explicitly check for errors.
		if (!resp.isError())
			System.out.println(resp.getContent());		
		
		// Use an error handler.
		restClient.setErrorHandler(new ReSTClient.ErrorHandler() {
			
			@Override
			public void handleError(int code) throws IOException {
				System.err.println("HTTP Error " + code + " occurred.");
			}
		});
		
		//Use URLBuilder to build a url.
		URLBuilder localhost = restClient.buildURL("localhost");
		
		// do a POST with the short-form method
		Response<Integer> rc = 
			restClient.callPost(localhost, "My POST content");
		
		// Check the last response for an error.
		if (rc.isError())
			System.out.println("boo!");
					
		// Call GET and pass back the raw response InputStream to the client.
		System.out.println(
				restClient.callGet(localhost, ReSTClient.INPUTSTREAM_DESERIALIZER)
					.getContent().available());
		
		// Create a rest client that will throw exceptions on all HTTP and I/O errors.
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		
		//This GET will deserialize server response as a string and 
		//throw IOException on any error.
		Response<String> rs = 
			restClient.callGet(localhost, ReSTClient.STRING_DESERIALIZER);
		
		// Print the response.
		System.out.println(rs.getContent());
		
		// Subsequent calls to this rest client will not throw exceptions on HTTP errors.
		restClient.setErrorHandler(null);
					
		//following line will never throw IOException 
		rs = restClient.callGet(localhost.copy("asdf"), ReSTClient.STRING_DESERIALIZER);
		
		if (rs.isError())
			System.out.println("Error: " + rs.getCode());
		
		//Set the error handler to throw all errors.
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		
		//following line will throw IOException 
		try {
			rs = restClient.callGet(localhost.copy("/asdf"), ReSTClient.STRING_DESERIALIZER);
			//Error is thrown when trying to get content.
			System.out.println(rs.getContent());
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		
		//following line will throw IOException 
		try {
			//Error is thrown when trying to get content.
			String respstr = restClient.callGet("localhost/asdf");				
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		
		// Only throw errors relating to server problems.
		restClient.setErrorHandler(ReSTClient.THROW_5XX_ERRORS);
		
		//following line will throw IOException 
		try {
			rs = restClient.callGet("localhost/asdf", ReSTClient.STRING_DESERIALIZER);
			//Error is not thrown when trying to get content because it is not a server error, but rather a 404.
			
			System.out.println("Should be true: " + rs.isError());
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		
		//following line will throw IOException 
		restClient.setErrorHandler(ReSTClient.THROW_ALL_ERRORS);
		try {
			//Error is thrown when trying to get content.
			String respstr = restClient.callGetContent(localhost.copy("/asdf"), ReSTClient.STRING_DESERIALIZER);				
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
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
		System.out.println("should be true: " + drc.isError());
		
		//HTTP HEAD
		Response<Integer> mrc = restClient.callHead(localhost);
		
		System.out.println("should be false: " + mrc.isError());					

		
		//This URLBuilder builds https://citibank.com/secureme/halp	
		System.out.println(
				restClient.buildURL("htTPS://citibank.com/secureme/").append("/halp"));
		
		// Builds http://yahoo.com/a/mystore/toodles?index=5
		System.out.println(restClient.buildURL("yahoo.com")
										.append("a")
										.append("mystore/")
										.append("toodles?index=5"));
				
		// Builds http://me.com/you/andi/like/each/ohter
		System.out.println(
				restClient.buildURL("me.com/")
					.append("/you/")
					.append("/andi/")
					.append("like/each/ohter/"));
		
		// Builds https://myhost.com/first/second/third/forth/fith/mypage.asp?i=1&b=2&c=3
		System.out.println(
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
		System.out.println(origurl);
		// Child URL: https://myhost.net/homepage/asdf/adf/reqotwoetiywer
		System.out.println(newurl);
	}
}
