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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.ISubject;

public abstract class AbstractGeoLocator<SUBJECT extends ISubject<?>> implements IGeoLocator<SUBJECT> {

    @Override
    public final List<GeoLocation<SUBJECT>> locate(Collection<SUBJECT> addresses,de.codesourcery.geoip.locate.IGeoLocator.IProgressListener progressListener) throws Exception 
    {
        final List<GeoLocation<SUBJECT>> result = new ArrayList<>();
        progressListener.progress( 0 , addresses.size() );
        int itemCount = 1;
        
        for (Iterator<SUBJECT> it = addresses.iterator(); it.hasNext();) 
        {
            SUBJECT s = it.next();
            result.add( locate( s ) );
            if ( it.hasNext() ) 
            {
                if ( ! progressListener.progress( itemCount++ , addresses.size() ) ) 
                {
                    System.err.println("*** Operation cancelled by user ***");
                    result.clear();
                    break;
                }
            }
        }
        progressListener.progress( addresses.size() , addresses.size() );
        return result;        
    }

    @Override
    public void flushCaches() {
    }

    @Override
    public void dispose() throws Exception {
    }
}
