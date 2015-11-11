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
package de.codesourcery.geoip.render;

import java.util.Map;

import de.codesourcery.geoip.GeoLocation;

/**
 * Implements some arbitrary map element that may be rendered by a {@link IMapElementRenderer}.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public interface IMapElement 
{
    /**
     * 
     * @see #get
     */
    public static final String ATTRIBUTE_RENDER_LABEL = "render_label";
    
	/**
	 * Supported map element types.
	 *  
	 * @author tobias.gierke@code-sourcery.de
	 */
	public static enum Type 
	{
		/**
		 * Renders a point at a specific {@link GeoLocation}.
		 */
		POINT,
		/**
		 * Renders a line between two {@link GeoLocation}s that each will be rendered using a {@link #POINT}.
		 */
		LINE,
		CURVED_LINE;
	}

	/**
	 * Mark any cached image coordinates as being invalid , forcing re-calculation on
	 * the next rendering.
	 * 
	 * @see #calculateCoordinates(IImageProjection)
	 * @see #isValid()
	 */
	public void invalidate();
	
	/**
	 * Check whether any cached image coordinates are still valid.
	 * 
	 * @return
	 * @see #invalidate()
	 */
	public boolean isValid();

	/**
	 * Recalculate any internally cached image coordinates using a given {@link IImageProjection}.
	 * 
	 * @param projection
	 * @see #invalidate()
	 */
	public void calculateCoordinates(IImageProjection projection);
	
	/**
	 * Check whether this map element is (at least partially) visible for a given image projection.
	 * 
	 * @param projection
	 * @return
	 */
	public boolean isVisible(IImageProjection projection);
	
	public Type getType();
	
	public boolean hasType(Type t);
	
	/**
	 * Returns the smallest squared distance between this map element and a point (in pixels/view coordinates).
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distanceSquared(int x,int y);
	
	/**
	 * Returns the map element that is closer than <code>maxDistanceSquared</code> and
	 * closest to a specific point (in view coordinates).
	 *  
	 * @param x
	 * @param y
	 * @param maxDistanceSquared squared max distance around (x,y) that should be considered when looking for 
	 * the closest map element. 
	 * @return
	 */	
	public IMapElement getClosestMapElement(int x, int y, double maxDistanceSquared);	
	
	/**
	 * Returns the attributes of this map element.
	 * @return
	 */
	public Map<String,Object> getAttributes();
}
