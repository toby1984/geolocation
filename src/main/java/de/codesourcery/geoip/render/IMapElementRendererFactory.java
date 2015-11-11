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

import java.awt.Graphics;

import com.jhlabs.map.proj.Projection;

/**
 * Factory for {@link  IMapElementRenderer} instances.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IMapElementRendererFactory {

	/**
	 * 
	 * @param type
	 * @param projection image project to be used when mapping cartesian coordinates returned by {@link Projection#project(double, double, java.awt.geom.Point2D.Double)}
	 * to the actual map image.
	 * @param g Graphics object to use for rendering
	 * 
	 * @return
	 */
	public IMapElementRenderer createRenderer(IMapElement.Type type,IImageProjection projection,Graphics g);
}
