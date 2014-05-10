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

import org.json.JSONObject;
import org.json.JSONWriter;

/**
 * A coordinate in degrees latitude/longitude.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public final class Coordinate
{
	public static final Coordinate ZERO = new Coordinate(0,0);
	
	public final double longitudeInDeg;
	public final double latitudeInDeg;
	
	public Coordinate(double latitudeInDeg, double longitudeInDeg) {
		this.longitudeInDeg = longitudeInDeg;
		this.latitudeInDeg = latitudeInDeg;
	}
	
	public JSONWriter toJSON(JSONWriter writer) {
		writer.object()
		.key("lat").value( latitudeInDeg )		
		.key("long").value( longitudeInDeg )
		.endObject();
		return writer;
	}
	
	public static Coordinate fromJSON(JSONObject object) {
		return new Coordinate( object.getDouble("lat") , object.getDouble("long") );  
	}	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitudeInDeg);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeInDeg);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coordinate other = (Coordinate) obj;
		if (Double.doubleToLongBits(latitudeInDeg) != Double
				.doubleToLongBits(other.latitudeInDeg))
			return false;
		if (Double.doubleToLongBits(longitudeInDeg) != Double
				.doubleToLongBits(other.longitudeInDeg))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "lat="+latitudeInDeg+" , long="+longitudeInDeg;
	}

	public double longitudeInRad() {
		return Math.toRadians( longitudeInDeg );
	}
	
	public double latitudeInRad() {
		return Math.toRadians( latitudeInDeg );
	}		
}