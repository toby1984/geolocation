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

import java.awt.Point;

import com.jhlabs.map.proj.Projection;

import de.codesourcery.geoip.Coordinate;

/**
 * Implementations know how to map cartesian coordinates
 * returned by {@link Projection#project(double, double, java.awt.geom.Point2D.Double)}
 * onto a destination image.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IImageProjection {

	/**
	 * Map cartesian coordinates to image (view) coordinates.
	 * 
	 * @param cartesianCoordinates Coordinates returned by {@link Projection#project(double, double, java.awt.geom.Point2D.Double)}
	 * @param out method result, image coordinates
	 */
	public void project(Coordinate cartesianCoordinates,Point out);
	
	/**
	 * Returns the destination's image width.
	 * @return
	 */
	public int getWidthInPixels();
	
	/**
	 * Returns the destination's image height.
	 * @return
	 */
	public int getHeightInPixels();
}
