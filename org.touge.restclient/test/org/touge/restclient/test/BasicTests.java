package org.touge.restclient.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.touge.restclient.ReSTClient;
import org.touge.restclient.ReSTClient.Response;

/**
 * Base tests for ReSTClient.
 * @author kgilmer
 *
 */
public class BasicTests extends TestCase {

	/**
	 * Test GET functionality of ReSTClient
	 * @throws IOException
	 */
	public void testGET() throws IOException {
		String testUrl = "http://kgilmer.github.com/touge/";
		ReSTClient client = new ReSTClient();
		
		//Simplest case
		String res = client.callGet(testUrl);
		
		assertNotNull(res);
		
		assertTrue(res.contains(ReSTClient.class.getSimpleName()));
		
		//Slightly more complex, specify String deserializer		
		String res2 = client.callGet(testUrl, ReSTClient.STRING_DESERIALIZER).getContent();
		
		assertNotNull(res2);
		assertTrue(res.equals(res2));
		
		//Handle as an InputStream
		/*InputStream is = client.callGet(testUrl, ReSTClient.INPUTSTREAM_DESERIALIZER).getContent();
		
		assertNotNull(is);
		assertTrue(is.available() > 0);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		byte[] buff = new byte[1024 * 8];
		
		while (is.available() > 0) {
			int len = is.read(buff);
			baos.write(buff, 0, len);
		}
		
		baos.close();
		String res3 = new String(baos.toByteArray(), "UTF-8");
		System.out.println("size: " + res2.length() + " " + res3.length());
		assertTrue(res3.equals(res2));*/
		
		//Test readStream
		InputStream is = client.callGet(testUrl, ReSTClient.INPUTSTREAM_DESERIALIZER).getContent();
		String res4 = new String(ReSTClient.readStream(is), "UTF-8");
		System.out.println("size: " + res2.length() + " " + res4.length());
		assertNotNull(res4);
		assertTrue(res4.equals(res2));
		
		//Test custom deserializer
		Response<List<Object>> response = client.callGet(testUrl, new CustomObjectArrayDeserializer());
		
		assertTrue(response != null);
		List<Object> l = response.getContent();
		assertNotNull(l);
		assertTrue(l.size() > 0);		
	}
	
	public void testErrorHandling() throws IOException {
		String testUrl = "http://kgilmer.github.com/touge/";
		String testBadUrl = "http://shinyama.info/notfound";
		ReSTClient client = new ReSTClient();
		
		client.setErrorHandler(null);
		
		Response<String> resp = client.callGet(testUrl, ReSTClient.STRING_DESERIALIZER);
		assertFalse(resp.isError());
		//Should not throw exception.
		resp = client.callGet(testBadUrl, ReSTClient.STRING_DESERIALIZER);
		assertTrue(resp.isError());
		
		client.setErrorHandler(new ReSTClient.ErrorHandler() {
			
			@Override
			public void handleError(int code) throws IOException {
				throw new IOException("Error: " + code);
			}
		});
		
		boolean exceptionThrown = false;
		//good url, should not throw exception
		try {
			resp = client.callGet(testUrl, ReSTClient.STRING_DESERIALIZER);
		} catch (IOException e) {
			exceptionThrown = true;
		}
		assertFalse(exceptionThrown);
		
		//bad url, should now throw exception
		try {
			resp = client.callGet(testBadUrl, ReSTClient.STRING_DESERIALIZER);
			resp.getContent();
		} catch (IOException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}
	
	private class CustomObjectArrayDeserializer implements ReSTClient.ResponseDeserializer<List<Object>> {

		@Override
		public List<Object> deserialize(InputStream input, int responseCode, Map<String, List<String>> headers) throws IOException {
			String s = new String(ReSTClient.readStream(input), "UTF-8");
			List<Object> tl = new ArrayList<Object>(Arrays.asList(s.split(" ")));
			
			return tl;
		}
		
	}
}
