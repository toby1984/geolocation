/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.geoip.locate;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.ISubject;

/**
 * A caching wrapper from {@link IGeoLocator} instances.
 * 
 * <p>This class maintains a map with all results returned by the 
 * wrapped <code>IGeoLocator</code> and will persist this data as a JSON string to a file
 * on the local filesystem when {@link #dispose()} is called.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 *
 * @param <SUBJECT>
 */
public class CachingGeoLocator<SUBJECT extends ISubject<?>> extends AbstractGeoLocator<SUBJECT> {

	private final IGeoLocator<SUBJECT> delegate;
	
	private final File CACHE_FILE = new File("geolocation.cache");

	private final Object CACHE_LOCK = new Object();
	
	//@GuardedBy( CACHE_LOCK )	
	private Map<SUBJECT,GeoLocation<SUBJECT>> cache;

	//@GuardedBy( CACHE_LOCK )	
	private boolean cacheLoaded = false;
	
	private final Constructor<SUBJECT> constructor;
	
	public CachingGeoLocator(IGeoLocator<SUBJECT> delegate,Class<SUBJECT> clazz) {
		this.delegate = delegate;
		try {
			this.constructor = clazz.getConstructor();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private GeoLocation<SUBJECT> cacheLookup(SUBJECT key) 
	{
		synchronized (CACHE_LOCK) 
		{
			if ( cacheLoaded ) {
				GeoLocation<SUBJECT> result = cache.get( key );
				return result == null ? null : result.createShallowCopy();
			}
		}
	
		long time = -System.currentTimeMillis();
		Map<SUBJECT, GeoLocation<SUBJECT>> loaded = null;
		try {
			loaded = loadCache();
		} catch (Exception e) {
			e.printStackTrace();
			loaded = new HashMap<>();
		}
		time += System.currentTimeMillis();
		System.out.println("Loaded "+loaded.size()+" cache entries from disk ("+time+" ms)");
		synchronized ( CACHE_LOCK ) 
		{
			if ( ! cacheLoaded ) 
			{
				cache = loaded;
				cacheLoaded = true;
			}
		}
		return cacheLookup(key);
	}
	
	private Map<SUBJECT, GeoLocation<SUBJECT>> loadCache() throws FileNotFoundException, IOException 
	{
		final Map<SUBJECT, GeoLocation<SUBJECT>> result = new HashMap<>();
		if ( ! cacheFileExists() ) {
			return result;
		}
		
		try ( InputStream in = createReader() ) 
		{
			JSONArray jsonArray = new JSONArray( new JSONTokener( in ) );
			int length = jsonArray.length();
			for ( int i = 0 ; i < length ; i++) 
			{
				final JSONObject obj = jsonArray.getJSONObject( i );
				final GeoLocation<SUBJECT> location = GeoLocation.fromJSON( obj , constructor );
				result.put( location.subject() , location );
			}
		}
		return result;
	}
	
	protected boolean cacheFileExists() {
		return CACHE_FILE.exists();
	}
	
	@Override
	public GeoLocation<SUBJECT> locate(SUBJECT address) throws Exception {
		
		GeoLocation<SUBJECT> result = cacheLookup( address );
		if ( result == null ) {
		    System.out.println("CACHE-MISS: >"+address+"<");
			result = delegate.locate( address );
			synchronized( CACHE_FILE ) 
			{
				GeoLocation<SUBJECT> existing = cache.get( address );
				if ( existing == null ) {
					cache.put( address , result.createShallowCopy() );
					System.out.println("CACHE-UPDATE: "+existing+" ( cache size: "+cache.size()+")" );
				} else {
					result = existing;
				}
			}
		}
		return result;
	}
	
	protected Writer createWriter() throws IOException {
		return new BufferedWriter( new FileWriter( CACHE_FILE )  );
	}
	
	protected InputStream createReader() throws IOException 
	{
		return new BufferedInputStream( new FileInputStream( CACHE_FILE ) );
	}	

	@Override
	public void dispose() throws Exception 
	{
		synchronized (CACHE_LOCK) 
		{
			if ( ! cacheLoaded ) {
				return;
			}
			
			long time = -System.currentTimeMillis();
			try (Writer writer = createWriter() ) 
			{
				final JSONWriter jsonWriter = new JSONWriter(writer);
				
				// start
				jsonWriter.array();
				
				for ( Entry<SUBJECT, GeoLocation<SUBJECT>> entry : cache.entrySet() ) 
				{
					entry.getValue().toJSON( jsonWriter );
				}

				// end
				jsonWriter.endArray();
			}
			time += System.currentTimeMillis();
			System.out.println("Persisted "+cache.size()+" cache entries ("+time+" ms)");			
		}
	}

	@Override
	public void flushCaches() {

		try {
			synchronized (CACHE_LOCK) 
			{
				if ( cacheLoaded ) {
					cache = null;
					cacheLoaded = false;
				}
			}
		} finally {
			delegate.flushCaches();
		}
	}
	
	@Override
	public boolean isAvailable() 
	{
	    return delegate.isAvailable();
	}
}