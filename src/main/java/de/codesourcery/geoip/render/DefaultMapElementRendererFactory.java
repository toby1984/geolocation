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

import de.codesourcery.geoip.render.IMapElement.Type;

/**
 * Default renderer factory.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class DefaultMapElementRendererFactory implements IMapElementRendererFactory {
		@Override
		public IMapElementRenderer createRenderer(Type type,IImageProjection projection, Graphics g) 
		{
			switch( type ) 
			{
			case LINE:
				return new LineRenderer(projection, g);
			case CURVED_LINE:
				return new CurvedLineRenderer(projection, g);
			case POINT:
				return new PointRenderer(projection, g);
			default:
				throw new RuntimeException("Unhandled type "+type);
			}
		}
}
