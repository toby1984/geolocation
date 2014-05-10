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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;

import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;
import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.StringSubject;

public class CachingLocatorTest extends TestCase {

	public void testCaching() throws Exception 
	{
		final StringSubject subject1 = new StringSubject("test1");
		final StringSubject subject2 = new StringSubject("test2");
		
		final GeoLocation<StringSubject> l1 = new GeoLocation<StringSubject>( subject1 ,  10 ,  20 ,  true );
		l1.setParameter( "key1", Integer.valueOf(10) )
		  .setParameter("key2" , "test")
		  .setParameter("key3" , Float.valueOf( 1.23f ) )
		  .setParameter("key4" , Double.valueOf( 4.56 ) );
		
		final GeoLocation<StringSubject> l2 = new GeoLocation<StringSubject>( subject2 ,  30 ,  40 ,  false );
		
		final IGeoLocator<StringSubject> dummyDelegate = EasyMock.createMock(IGeoLocator.class);
		
		EasyMock.expect(  dummyDelegate.locate( subject1 ) ).andReturn(l1);
		EasyMock.expect(  dummyDelegate.locate( subject2 ) ).andReturn(l2);

		dummyDelegate.flushCaches();
		EasyMock.expectLastCall().anyTimes();
		
		EasyMock.replay( dummyDelegate );
		
		final ByteArrayOutputStream[] out = { null };
		
		CachingGeoLocator<StringSubject> loc = new CachingGeoLocator<StringSubject>( dummyDelegate, StringSubject.class ) {
			
			@Override
			protected InputStream createReader() throws IOException 
			{
				return new ByteArrayInputStream( out[0].toByteArray() ); 
			}
			
			@Override
			protected boolean cacheFileExists() {
				return out[0] != null;
			}
			
			@Override
			protected Writer createWriter() throws IOException 
			{
				out[0] = new ByteArrayOutputStream();
				return new BufferedWriter( new OutputStreamWriter( out[0] ) );
			}
		};
		
		assertNotNull( loc.locate( subject1 ) );
		assertNotNull( loc.locate( subject2 ) );
		loc.dispose();
		
		assertNotNull( "Nothing written?" , out[0] );
		
		System.out.println("Written: "+new String( out[0].toByteArray() ) );
		loc.flushCaches();
		
		EasyMock.verify( dummyDelegate );
		
		EasyMock.resetToStrict( dummyDelegate );
		
		dummyDelegate.flushCaches();
		EasyMock.expectLastCall().anyTimes();
		
		EasyMock.replay( dummyDelegate );
		
		loc.flushCaches();		
		
		GeoLocation<StringSubject> read = loc.locate( subject1 );
		assertNotNull( read );
		System.out.println("Got: "+read);
		read = loc.locate( subject2 );
		assertNotNull( read );
		System.out.println("Got: "+read);
		
		EasyMock.verify( dummyDelegate );		
	}
}