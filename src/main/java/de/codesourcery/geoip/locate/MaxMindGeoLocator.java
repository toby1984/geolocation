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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.StringSubject;

public class MaxMindGeoLocator extends AbstractGeoLocator<StringSubject> {

	private static final String classpath = "/GeoLite2-City.mmdb";
	
	private final Object READER_LOCK = new Object();
	private DatabaseReader reader;
	
	public MaxMindGeoLocator() {
	}
	
	@Override
	public boolean isAvailable() 
	{
		final InputStream stream = MaxMindGeoLocator.class.getResourceAsStream( classpath );
		if ( stream == null ) {
			return false;
		}
		try { stream.close(); } catch (IOException e) { }
		return true;
	}
	
	protected synchronized DatabaseReader getReader() throws IOException 
	{
		synchronized (READER_LOCK) 
		{
			if ( reader != null ) {
				return reader;
			}
			
			final InputStream stream = MaxMindGeoLocator.class.getResourceAsStream( classpath );
			if ( stream == null ) {
				throw new IOException("Failed to open classpath resource "+classpath);
			}
			reader = new DatabaseReader.Builder(stream).build();
			return reader;
		} 
	}

	@Override
	public GeoLocation<StringSubject> locate(StringSubject subjects) throws Exception 
	{
		try {
			final CityResponse response = getReader().city( InetAddress.getByName( subjects.value() ) );
			final GeoLocation<StringSubject> result = new GeoLocation<>(subjects,
						response.getLocation().getLatitude(),
						response.getLocation().getLongitude() );
			
			result.setParameter( GeoLocation.KEY_CITY , response.getCity().getName() );
			result.setParameter( GeoLocation.KEY_COUNTRY , response.getCountry().getName() );
			return result;
					
		}
		catch(GeoIp2Exception e) 
		{
			if ( e instanceof AddressNotFoundException) 
			{
				return new GeoLocation<>(subjects);
			}
			throw e;
		}
		catch(Exception e) {
			throw e;
		}
	}

	@Override
	public void dispose() throws Exception 
	{
		synchronized (READER_LOCK) 
		{
			try 
			{
				if ( reader != null ) {
					reader.close();
				}
			} finally {
				reader = null;
			}
		}
	}
}
