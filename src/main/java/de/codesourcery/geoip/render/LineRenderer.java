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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Line2D;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.render.PointRenderer.MapPoint;

/**
 * A {@link IMapElementRenderer} that links to {@link MapPoint} with a simple line.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public final class LineRenderer implements IMapElementRenderer {

	private final Graphics g;
	private final IImageProjection imageProject;
	private final PointRenderer pointRenderer;
	
	public static class MapLine implements IMapElement 
	{
		public final MapPoint start;
		public final MapPoint end;
		
		public final Color color;
		
		public MapLine(MapPoint start,MapPoint end,Color color) {
			this.start = start;
			this.end = end;
			this.color = color;
		}
		
		@Override
		public boolean isVisible(IImageProjection projection) 
		{
			if ( ! isValid() ) {
				calculateCoordinates( projection );
			}
			
			final int minX = Math.min( start.point.x , end.point.x );
			final int maxX = Math.max( start.point.x , end.point.x );
			
			final int minY = Math.min( start.point.y , end.point.y );
			final int maxY = Math.max( start.point.y , end.point.y );

			final Line2D.Float l = new Line2D.Float(minX,minY,maxX,maxY);
			final Rectangle rect = new Rectangle(0 , 0 , projection.getWidthInPixels() , projection.getHeightInPixels() );
			return rect.intersectsLine( l );
		}
		
		@Override
		public double distanceSquared(int x, int y) 
		{
			double d1 = start.distanceSquared(x,y);
			double d2 = start.distanceSquared(x,y);
			return d1 < d2 ? d1 : d2;
		}
		
		@Override
		public boolean isValid() {
			return start.isValid() && end.isValid();
		}

		@Override
		public Type getType() {
			return IMapElement.Type.LINE;
		}

		@Override
		public boolean hasType(Type t) {
			return t.equals( IMapElement.Type.LINE );
		}

		@Override
		public void invalidate() {
			start.invalidate();
			end.invalidate();
		}

		@Override
		public void calculateCoordinates(IImageProjection projection) 
		{
			start.calculateCoordinates( projection );
			end.calculateCoordinates( projection );
		}
		
		@Override
		public IMapElement getClosestMapElement(int x, int y,double maxDistanceSquared) 
		{
			double distanceSquared1 = start.distanceSquared( x ,  y );
			double distanceSquared2 = end.distanceSquared( x ,  y );
			
			if ( distanceSquared1 < distanceSquared2 ) {
				if ( distanceSquared1 <= maxDistanceSquared ) {
					return start;
				}
			} 
			if ( distanceSquared2 <= maxDistanceSquared ) {
				return end;
			}
			return null;
		}		
	}
	
	public static MapLine createLine(GeoLocation<?> start,GeoLocation<?> end , Color color) 
	{
		return new MapLine( PointRenderer.createPoint( start , color) ,
							 PointRenderer.createPoint( end , color) ,
							 color);
	}
	
	public static MapLine createLine(MapPoint start,MapPoint end , Color color) 
	{
		return new MapLine( start ,
							 end ,
							 color);
	}	

	public LineRenderer(IImageProjection imageProject, Graphics g) {
		this.imageProject = imageProject;
		this.g = g;
		this.pointRenderer = new PointRenderer( imageProject, g);
	}

	@Override
	public void render(IMapElement element) 
	{
		final MapLine line = (MapLine) element;
		if ( ! line.isValid() ) {
			line.calculateCoordinates( imageProject );
		}
		
		g.setColor( line.color );
		g.drawLine( line.start.point.x , line.start.point.y , line.end.point.x , line.end.point.y );
		
		pointRenderer.render( line.start );
		pointRenderer.render( line.end );		
	}	
}