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
package de.codesourcery.geoip.locate;

import java.util.Collection;
import java.util.List;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.ISubject;

/**
 * Implementations provide location information (plus additional metadata, if supported/available) for a specific {@link ISubject}.
 * 
 * @author tobias.gierke@code-sourcery.de
 *
 * @param <SUBJECT>
 */
public interface IGeoLocator<SUBJECT extends ISubject<?>>
{
	/**
	 * Retrieve location information associated with a specific collection of subjects.
	 * 
	 * @param subjects
	 * @return
	 * @throws Exception
	 */
	public List<GeoLocation<SUBJECT>> locate(Collection<SUBJECT> subjects) throws Exception;
	
	/**
	 * Retrieve location information associated with a specific subject.
	 * 
	 * @param subjects
	 * @return
	 * @throws Exception
	 */
	public GeoLocation<SUBJECT> locate(SUBJECT subjects) throws Exception;
	
	/**
	 * Discard any internal caches this implementation may use.
	 */
	public void flushCaches();
	
	/**
	 * Called before this instance is disposed.
	 * 
	 * An instance will no longer be used after this method has been invoked.
	 * 
	 * @throws Exception
	 */
	public void dispose() throws Exception;
}