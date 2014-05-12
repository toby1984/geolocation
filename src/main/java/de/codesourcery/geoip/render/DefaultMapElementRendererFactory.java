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
