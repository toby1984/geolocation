package de.codesourcery.geoip.locate;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.StringSubject;

public class MaxMindGeoLocator implements IGeoLocator<StringSubject> {

	private static final String classpath = "/GeoLite2-City.mmdb";
	
	private final Object READER_LOCK = new Object();
	private DatabaseReader reader;
	
	public MaxMindGeoLocator() {
	}
	
	public static boolean isAvailable() 
	{
		final InputStream stream = MaxMindGeoLocator.class.getResourceAsStream( classpath );
		if ( stream == null ) {
			return false;
		}
		try { stream.close(); } catch (IOException e) { }
		return true;
	}
	
	@Override
	public List<GeoLocation<StringSubject>> locate(Collection<StringSubject> subjects) throws Exception {
		
		List<GeoLocation<StringSubject>>  result = new ArrayList<>();
		for ( StringSubject s : subjects ) {
			result.add(locate(s));
		}
		return result;
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
						response.getLocation().getLongitude() , true );
			
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
	public void flushCaches() {
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
