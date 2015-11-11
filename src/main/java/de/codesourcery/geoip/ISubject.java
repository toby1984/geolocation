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
package de.codesourcery.geoip;

import org.json.JSONObject;
import org.json.JSONWriter;

import de.codesourcery.geoip.locate.IGeoLocator;

/**
 * A subject for which geo-information may be retrieved using a {@link IGeoLocator}.
 * 
 * <p>Instances of this class need to be IMMUTABLE and provide proper <code>equals()</code> and <code>hashCode()</code> implementations (used as map keys).</p>
 * @author tobias.gierke@code-sourcery.de
 *
 * @param <T>
 */
public interface ISubject<T> 
{
	public T value();

	/**
	 * Serialize this instance to a JSON string.
	 * 
	 * @param writer
	 */
	public void toJSON(JSONWriter writer);
	
	@Override
	public boolean equals(Object obj);
	
	@Override
	public int hashCode();
	
	/**
	 * De-serialize this instance from a JSON string.
	 * @param object
	 */
	public void fromJSON(JSONObject object);
	
	/**
	 * Create a deep copy of this instance.
	 * 
	 * @return
	 */
	public ISubject<T> createDeepCopy();
}