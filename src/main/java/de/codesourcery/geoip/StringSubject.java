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

/**
 * A {@link ISubject} that just wraps a plain Java <code>String</code>.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class StringSubject implements ISubject<String> {

	private String subject;
	
	public StringSubject() {
	}
	
	public StringSubject(String s) 
	{
		if (s == null ) {
			throw new IllegalArgumentException("subject cannot be NULL");
		}
		this.subject = s;
	}
	
	@Override
	public int hashCode() 
	{
		return 31 + ((subject == null) ? 0 : subject.hashCode());
	}

	@Override
	public boolean equals(Object obj) 
	{
		if ( obj instanceof StringSubject) {
			return this.subject.equals( ((StringSubject) obj).subject );
		}
		return false;
	}

	@Override
	public StringSubject createDeepCopy() 
	{
		return this;
	}
	
	@Override
	public String toString() {
		return subject;
	}

	@Override
	public String value() {
		return subject;
	}

	@Override
	public void toJSON(JSONWriter writer) {
		writer.object().key("value").value( subject ).endObject();
	}

	@Override
	public void fromJSON(JSONObject object) {
		this.subject = (String) object.getString("value");
	}
}