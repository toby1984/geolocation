/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
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
package de.codesourcery.geoip;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

/**
 * The location of a specific {@link ISubject} along with an arbitrary set of associated parameters/metadata.
 * 
 * @author tobias.gierke@code-sourcery.de
 *
 * @param <SUBJECT>
 * @see ISubject
 * @see Coordinate
 */
public class GeoLocation<SUBJECT extends ISubject<?>> 
{
	private final SUBJECT subject;
	private final Coordinate coordinate;
	private final boolean hasValidCoordinates;

	private final Map<String,Object> parameters;
	/**
	 * 
	 * @see GeoLocation#parameter(String)
	 * @see GeoLocation#parameters()
	 */	
	public static final String KEY_COUNTRY = "country";
	/**
	 * 
	 * @see GeoLocation#parameter(String)
	 * @see GeoLocation#parameters()
	 */
	public static final String KEY_CITY = "city";

	/**
	 * Creates a shallow copy of this instance (no deep-copying of parameters).
	 * @return
	 */
	public GeoLocation<SUBJECT> createShallowCopy() 
	{
		return new GeoLocation<>( subject , coordinate , hasValidCoordinates , new HashMap<>( parameters ) );
	}	

	/**
	 * Creates an instance marked as <b>invalid</b>.
	 * 
	 * @param subject
	 * @see #hasValidCoordinates()
	 */
	public GeoLocation(SUBJECT subject) 
	{
		if ( subject == null ) {
			throw new IllegalArgumentException("subject must not be null");
		}
		this.subject = subject;
		this.coordinate = Coordinate.ZERO;
		hasValidCoordinates = false;
		parameters = new HashMap<>();
	}

	/**
	 * Creates a valid instance without any additional metadata.
	 * 
	 * @param subject
	 * @param latitude
	 * @param longitude
	 * @see #hasValidCoordinates()
	 */
	public GeoLocation(SUBJECT subject, double latitude, double longitude) 
	{
		if ( subject == null ) {
			throw new IllegalArgumentException("subject must not be null");
		}		
		this.subject = subject;
		this.coordinate = new Coordinate(latitude, longitude);
		this.hasValidCoordinates = true;
		parameters = new HashMap<>();
	}

	/**
	 * Create instance without any additional metadata.
	 * 	
	 * @param subject
	 * @param latitude
	 * @param longitude
	 * @param isValid whether this location should be considered 'valid'.
	 * @see #hasValidCoordinates()
	 */
	public GeoLocation(SUBJECT subject, double latitude, double longitude,boolean isValid) 
	{
		if ( subject == null ) {
			throw new IllegalArgumentException("subject must not be null");
		}		
		this.subject = subject;
		this.coordinate = new Coordinate(latitude, longitude);
		this.hasValidCoordinates = isValid;
		parameters = new HashMap<>();
	}

	/**
	 * Create an instance with additional metadata.
	 * 
	 * @param subject
	 * @param coordinate
	 * @param isValid
	 * @param parameters
	 * 
	 * @see #hasValidCoordinates()
	 */
	private GeoLocation(SUBJECT subject, Coordinate coordinate,boolean isValid,Map<String,Object> parameters) 
	{
		if ( subject == null ) {
			throw new IllegalArgumentException("subject must not be null");
		}		
		this.subject = subject;
		this.coordinate = coordinate;
		this.hasValidCoordinates = isValid;
		this.parameters = parameters;
	}		

	/**
	 * Serialize this instance as a JSON string.
	 * 
	 * @param writer
	 */
	public void toJSON(JSONWriter writer) 
	{
		writer
		.object()
		.key("subject");
		subject.toJSON( writer );

		writer.key("coords");

		coordinate.toJSON( writer )

		.key("valid").value(hasValidCoordinates)
		.key("parameters");
		toJSON( parameters , writer );

		writer.endObject();
	}

	/**
	 * Deserialize a <code>GeoLocation</code> from a JSON string.
	 * 
	 * @param obj
	 * @param constructor
	 * @return
	 */
	public static <T extends ISubject<?>> GeoLocation<T> fromJSON(JSONObject obj,Constructor<T> constructor) {

		final T subject;
		try {
			subject = constructor.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new JSONException("Failed to instantiate "+constructor);
		}
		subject.fromJSON( obj.getJSONObject("subject" ) );

		final Coordinate coordinates = Coordinate.fromJSON( obj.getJSONObject( "coords" ) );
		final boolean isValid = obj.getBoolean("valid");
		final Map<String,Object> params = readMap( obj.getJSONObject("parameters" ) );
		return new GeoLocation<T>(subject,coordinates , isValid , params );
	}	

	private static Map<String, Object> readMap(JSONObject jsonObject) 
	{
		Map<String, Object> result = new HashMap<>();
		final String[] names = JSONObject.getNames( jsonObject );
		if ( names == null ) {
			return result;
		}
		for ( String key : names ) {

			final JSONObject pair = jsonObject.getJSONObject( key );
			final String type = pair.getString("t");
			final Object value;
			switch( type ) 
			{
			case "l":
				value = pair.getLong( "v" );
				break;
			case "i":
				value = Integer.valueOf( (int) pair.getLong( "v" ) );
				break;
			case "s":
				value = pair.getString("v");
				break;
			case "d":
				value = pair.getDouble("v");
				break;
			case "f":
				value = Float.valueOf( (float) pair.getDouble("v") );
				break;					
			default:
				throw new JSONException("Unhandled type: >"+type+"<");
			}
			result.put(key, value);
		}
		return result;
	}

	private static void toJSON(Map<String,Object> map,JSONWriter writer) 
	{
		writer.object();
		for ( Map.Entry<String,Object> entry : map.entrySet() ) {
			toJSON( entry.getKey() , entry.getValue() , writer );
		}		
		writer.endObject();
	}

	private static void toJSON(String key, Object object,JSONWriter writer) {

		writer.key( key );
		writer.object();
		if ( object instanceof Long) {
			writer.key("t").value( "l" )
			.key("v").value( (Long) object );
		} else if ( object instanceof Integer) {
			writer.key("t").value( "i" )
			.key("v").value( (Integer) object );			
		} else if ( object instanceof String) {
			writer.key("t").value( "s" )
			.key("v").value( (String) object );				
		} else if ( object instanceof Double) {
			writer.key("t").value( "d" )
			.key("v").value( (Double) object );				
		} else if ( object instanceof Float) {
			writer.key("t").value( "f" )
			.key("v").value( (Float) object );					
		} else {
			throw new JSONException("Don't know how to serialize "+object);
		}
		writer.endObject();
	}

	@Override
	public String toString() {
		return subject+" [ valid="+hasValidCoordinates+" , "+coordinate+" , "+parameters+" ]";
	}

	@Override
	public boolean equals(Object obj) 
	{
		if ( obj instanceof GeoLocation) 
		{
			final GeoLocation<?> other = (GeoLocation<?>) obj;
			return ObjectUtils.equals( this.subject , other.subject );
		}
		return false;
	}

	@Override
	public int hashCode() 
	{
		return subject.hashCode();
	}

	public Map<String, Object> parameters() {
		return parameters;
	}

	public GeoLocation<SUBJECT> setParameter(String key,Object value) {
		parameters.put(key, value);
		return this;
	}

	/**
	 * Returns this location's coordinate.
	 * 
	 * Make sure to assert that the coordinate is in fact {@link #hasValidCoordinates() valid}. 
	 * @return
	 * @see #hasValidCoordinates()
	 */
	public Coordinate coordinate() {
		return coordinate;
	}	

	public double latitude() {
		return coordinate.latitudeInDeg;
	}

	public double longitude() {
		return coordinate.longitudeInDeg;
	}

	/**
	 * Returns whether this location actually has valid coordinates.
	 * 
	 * @return <code>true</code> if {@link #coordinate()} returns valid coordinates, otherwise <code>false</code>.
	 */
	public boolean hasValidCoordinates() {
		return hasValidCoordinates;
	}

	public SUBJECT subject() {
		return subject;
	}

	public boolean hasParameter(String key) {
		return parameters.get(key) != null;
	}

	public Object parameter(String key) {
		return parameters.get(key);
	}	

	public Object parameter(String key,Object defaultValue) {
		Object result = parameters.get(key);
		return result != null ? result : defaultValue;
	}
}