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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.ISubject;

public class DelegatingGeoLocator<SUBJECT extends ISubject<?>> implements IGeoLocator<SUBJECT> {

    private final List<IGeoLocator<SUBJECT>> delegates = new ArrayList<>();
    
    @SafeVarargs
    public DelegatingGeoLocator(@SuppressWarnings("unchecked") IGeoLocator<SUBJECT>... delegates) 
    {
        if ( delegates != null ) {
            this.delegates.addAll( Arrays.asList( delegates ) );
        }
    }
    
    @Override
    public List<GeoLocation<SUBJECT>> locate(Collection<SUBJECT> subjects,de.codesourcery.geoip.locate.IGeoLocator.IProgressListener progressListener) throws Exception 
    {
        Optional<IGeoLocator<SUBJECT>> delegate = getAvailable();
        if ( ! delegate.isPresent() ) {
            throw new Exception("No geo locators available" ); 
        }
        return delegate.get().locate( subjects , progressListener );
    }
    
    private Optional<IGeoLocator<SUBJECT>> getAvailable() {
        return delegates.stream().filter( d -> d.isAvailable() ).findFirst();
    }

    @Override
    public GeoLocation<SUBJECT> locate(SUBJECT subject) throws Exception 
    {
        Optional<IGeoLocator<SUBJECT>> delegate = getAvailable();
        if ( ! delegate.isPresent() ) {
            throw new Exception("No geo locators available" ); 
        }
        return delegate.get().locate( subject );
    }

    @Override
    public void flushCaches() {
        delegates.forEach( d -> { try { d.flushCaches(); } catch(Exception e) { /* nop */ } } );
    }

    @Override
    public void dispose() throws Exception {
        delegates.forEach( d -> { try { d.dispose(); } catch(Exception e) { /* nop */ } } );
    }

    @Override
    public boolean isAvailable() {
        return getAvailable().isPresent();
    }
}
