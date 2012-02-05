package org.touge.restclient.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.touge.restclient.RestClient;
import org.touge.restclient.RestClient.HttpGETCacheEntry;

/**
 * Base tests for ReSTClient.
 * @author kgilmer
 *
 */
public class CacheTestCase extends TestCase {

	/**
	 * Test GET functionality of ReSTClient
	 * @throws IOException
	 */
	public void testCacheMethodCoverage() throws IOException {
		String testUrl = "http://kgilmer.github.com/touge/";
		RestClient client = new RestClient();
		client.setDebugWriter(new PrintWriter(System.err));
		EmptyCache testCache = new EmptyCache();
		client.setCache(testCache);
		
		assertNotNull(client.getCache());
		
		String res = client.callGet(testUrl);
		
		assertTrue(testCache.addCalled);
		assertTrue(testCache.getCalled);			
		assertNotNull(res);
		
		res = client.callGet(testUrl);
		
		assertTrue(testCache.addCalled);
		assertTrue(testCache.getCalled);		
		assertNotNull(res);
		
		MapCache mapCache = new MapCache();
		client.setCache(mapCache);
		
		res = client.callGet(testUrl);
		
	 	assertTrue(mapCache.addCalled);
	 	assertTrue(mapCache.getCalled);	
	 	assertFalse(mapCache.cacheHit);
	 	assertNotNull(res);
		
		res = client.callGet(testUrl);
		
	 	assertTrue(mapCache.addCalled);
	 	assertTrue(mapCache.getCalled);
	 	assertTrue(mapCache.cacheHit);
		assertNotNull(res);
		
		mapCache.clear();
		res = client.callGet(testUrl);
		
	 	assertTrue(mapCache.addCalled);
	 	assertTrue(mapCache.getCalled);	
	 	assertFalse(mapCache.cacheHit);
	 	assertNotNull(res);
		
	}
	
	private class EmptyCache implements RestClient.HttpGETCache {

		
		private boolean getCalled = false;
		private boolean addCalled = false;

		

	

		@Override
		public HttpGETCacheEntry get(String key) {
			getCalled = true;
			return null;
		}





		@Override
		public void put(String key, HttpGETCacheEntry entry) {
			addCalled = true;
		}
		
	}
	
	private class MapCache implements RestClient.HttpGETCache {

		private boolean getCalled = false;
		private boolean addCalled = false;
		private boolean cacheHit = false;
		private Object[] data;
		private final int CONTENT_INDEX = 0;
		private final int HEADERS_INDEX = 1;
		private final int CODE_INDEX = 2;
		private final Map<String, Object[]> cache = new HashMap<String, Object[]>();

		@Override
		public HttpGETCacheEntry get(final String key) {
			getCalled = true;
			
			if (cache.containsKey(key)) {
				cacheHit = true;
				return new RestClient.HttpGETCacheEntry() {
					
					@Override
					public int getResponseCode() {
						return ((Integer) cache.get(key)[CODE_INDEX]).intValue();
					}
					
					@Override
					public byte[] getContent() {						
						return (byte[]) cache.get(key)[CONTENT_INDEX];
					}
					
					@Override
					public Map<String, List<String>> getHeaders() {						
						return (Map<String, List<String>>) cache.get(key)[HEADERS_INDEX];
					}
				};
			}
			
			cacheHit = false;
				
			return null;
		}
		
		public void clear() {
			cache.clear();
		}

		@Override
		public void put(String key, HttpGETCacheEntry entry) {
			addCalled = true;
			Object [] ov = new Object[3];
			ov[CONTENT_INDEX] = entry.getContent();
			ov[HEADERS_INDEX] = entry.getHeaders();
			ov[CODE_INDEX] = entry.getResponseCode();
			
			cache.put(key, ov);
		}
	}
}
