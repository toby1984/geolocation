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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.json.JSONTokener;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.StringSubject;

/**
 * A <code>IGeoLocator</code> that uses the geo-location API at http://freegeoip.net to retrieve data.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class FreeGeoIPLocator extends AbstractGeoLocator<StringSubject> 
{
	private static final int THROTTLE_MILLIS = 300;
	
	private final AtomicLong lastRequestTime = new AtomicLong(0);
	
	@Override
	public synchronized GeoLocation<StringSubject> locate(StringSubject ipAddress) throws Exception 
	{
		final long lastTime = lastRequestTime.get();
		if ( lastTime != 0 ) 
		{
			final long delta = System.currentTimeMillis() - lastTime;
			if ( delta < 300 ) {
				try { Thread.sleep(THROTTLE_MILLIS); } catch(Exception e) {};
			}
		}
		
		lastRequestTime.compareAndSet( lastTime ,  System.currentTimeMillis() );
		
    	final StringBuilder builder = new StringBuilder();
    	System.out.println("Retrieving location data for "+ipAddress+" ...");
    	
    	final URL api = new URL("http://freegeoip.net/json/"+ipAddress);
    	final URLConnection yc = api.openConnection(); 
        try ( BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream())) ) 
        {
        	String inputLine;
        	while ((inputLine = in.readLine()) != null) 
        	{
        		builder.append( inputLine );
        	}
        } 
        // {"ip":"213.191.64.208","country_code":"DE","country_name":"Germany","region_code":"","region_name":"","city":"","zipcode":"","latitude":51,"longitude":9,"metro_code":"","area_code":""}
        
        final Map<String, Object> params = parseJSON( builder.toString() );
        System.out.println("GOT: "+params);
        
        String city = objToString( params.get("city") );
        String country = objToString( params.get("country_name" ) );
        double latitude = Double.parseDouble( params.get("latitude" ).toString() );
        double longitude = Double.parseDouble( params.get("longitude" ).toString() );
        
        if ("Reserved".equals( country ) ) {
        	return new GeoLocation<StringSubject>(ipAddress);
        }
        return new GeoLocation<StringSubject>(ipAddress,latitude,longitude).setParameter( GeoLocation.KEY_CITY , city ).setParameter( GeoLocation.KEY_COUNTRY, country);
	}
	
	private static String objToString(Object obj) {
		return obj == null ? "" : obj.toString();
	}
	
	private static Map<String,Object> parseJSON(String input) 
	{
		Map<String,Object> result = new HashMap<>();
		JSONObject obj = new JSONObject( new JSONTokener( input ) );
		for ( String key :  JSONObject.getNames( obj ) ) {
			Object value = obj.get(key);
			if ( value != null ) {
				result.put( key.toLowerCase() , value );
			}
		}
		return result;
	}

    @Override
    public boolean isAvailable() {
        return true;
    }	
}