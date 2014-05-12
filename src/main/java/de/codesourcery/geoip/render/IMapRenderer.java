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
package de.codesourcery.geoip.render;

import java.awt.Graphics;

import de.codesourcery.geoip.MapImage;
import de.codesourcery.geoip.MapImageRegion;

/**
 * Implementations render a map image with {@link IMapElement}s on top.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public interface IMapRenderer
{
	/**
	 * Sets the map image to be used.
	 * 
	 * Make sure to also set a {@link Projection) matching the image.
	 * 
	 * @param image
	 * @see #getMapImage()
	 */
	public void setMapImage(MapImage image);
	
	/**
	 * Returns the map image currently being used.
	 * 
	 * @return
	 */
	public MapImage getMapImage();
	
	/**
	 * Renders the map image along with any associated {@link IMapElement}s to a specific
	 * {@link Graphics} context.
	 * 
	 * @param g context to use for rendering
	 * @param region Region of map image that should be rendered
	 * @param width desired width in pixels
	 * @param height desired height in pixels
	 */
	public void renderMap(Graphics g , MapImageRegion region, int width, int height);
	
	/**
	 * Sets the renderer factory to be used.
	 * 
	 * @param factory
	 */
	public void setMapElementRendererFactory(IMapElementRendererFactory factory);
	
	/**
	 * Returns the map element that is closer than <code>minDistance</code> and
	 * closest to a specific point (in view coordinates).
	 *  
	 * @param x
	 * @param y
	 * @param minDistance max distance around (x,y) that should be considered when looking for 
	 * the closest map element. 
	 * @return
	 */
	public IMapElement getClosestMapElement(int x,int y,double maxDistance);
}