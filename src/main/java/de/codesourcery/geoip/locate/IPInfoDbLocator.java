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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.json.JSONTokener;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.StringSubject;

public class IPInfoDbLocator extends AbstractGeoLocator<StringSubject>
{
    private static final File API_KEY_FILE = new File("ipinfo.apikey");
    
    private static final int THROTTLE_MILLIS = 300;
    
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    
    @Override
    public boolean isAvailable() 
    {
        try {
            return getAPIKey().isPresent();
        } catch(Exception e) {
            return false;
        }
    }
    
    public static void main(String[] args) throws Exception 
    {
        final IPInfoDbLocator loc = new IPInfoDbLocator();
        final GeoLocation<StringSubject> location = loc.locate( new StringSubject( "74.125.45.100" ) );
        
        System.out.println("GOT: "+location);
    }
    
    private Optional<String> getAPIKey() throws FileNotFoundException, IOException 
    {
        return Optional.of("459deb54c0221b6a3d051755e28d8283f28a02f3a2079f253205a4fda4db4c34");
//        if ( API_KEY_FILE.exists() && API_KEY_FILE.canRead() && API_KEY_FILE.isFile() ) {
//            try ( BufferedReader r = new BufferedReader(new FileReader( API_KEY_FILE ) ) ) 
//            {
//                final String line = r.readLine();
//                return line != null && line.length() > 0 ? Optional.of( line ) : Optional.empty();
//            }
//        }
//        return Optional.empty();
    }
    
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
        
        final Optional<String> apiKey = getAPIKey();
        if ( ! apiKey.isPresent() ) {
            throw new RuntimeException("Found no API key in file "+API_KEY_FILE.getAbsolutePath());
        }
        
        final URL api = new URL("http://api.ipinfodb.com/v3/ip-city/?format=json&key="+apiKey.get()+"&ip="+ipAddress);
        
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
        
        System.out.println("Server returned: "+builder);
        final Map<String, Object> params = parseJSON( builder.toString() );
        System.out.println("GOT: "+params);
        
        String city = objToString( params.get("cityname") );
        String country = objToString( params.get("countryname" ) );
        double latitude = Double.parseDouble( params.get("latitude" ).toString() );
        double longitude = Double.parseDouble( params.get("longitude" ).toString() );
        
        if ("Reserved".equals( country ) ) {
            return new GeoLocation<StringSubject>(ipAddress);
        }
        return new GeoLocation<StringSubject>(ipAddress,latitude,longitude)
                .setParameter( GeoLocation.KEY_CITY , city )
                .setParameter( GeoLocation.KEY_COUNTRY, country);
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
}