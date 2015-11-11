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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import de.codesourcery.geoip.GeoLocation;

/**
 * A {@link IMapElementRenderer} that renders a {@link GeoLocation} as a simple dot on the map.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class PointRenderer implements IMapElementRenderer {

	private final Graphics g;
	private final IImageProjection imageProject;
	
	public static class MapPoint implements IMapElement 
	{
		public final GeoLocation<?> location;
		public final Point point = new Point();
		public final String label;
		public boolean isValid = false;
		public final Color color;
		public final Map<String,Object> attributes = new HashMap<>();
		
		public MapPoint(GeoLocation<?> location,Color color) {
			this.location = location;
			this.color = color;
			this.label = null;
		}
		
		public MapPoint(GeoLocation<?> location,String label, Color color) {
			this.location = location;
			this.color = color;
			this.label = label;
		}		
		
		@Override
		public boolean isVisible(IImageProjection projection) 
		{
			if ( ! isValid ) {
				calculateCoordinates( projection );
			}
			final Point p = new Point();
			projection.project( location.coordinate() , p );
			return p.x >= 0 && p.y >= 0 && p.x < projection.getWidthInPixels() && p.y < projection.getHeightInPixels();
		}
		
		@Override
		public double distanceSquared(int x, int y) 
		{
			double dx = x - this.point.x;
			double dy = y - this.point.y;
			return dx*dx+dy*dy;
		}		

		@Override
		public Type getType() {
			return IMapElement.Type.POINT;
		}

		@Override
		public boolean hasType(Type t) {
			return t.equals( IMapElement.Type.POINT );
		}
		
		@Override
		public boolean isValid() {
			return isValid;
		}

		@Override
		public void invalidate() {
			isValid = false;
		}

		@Override
		public void calculateCoordinates(IImageProjection projection) 
		{
			projection.project( location.coordinate() ,  this.point );
			isValid = true;
		}

		@Override
		public IMapElement getClosestMapElement(int x, int y,double maxDistanceSquared) 
		{
			double distanceSquared = distanceSquared( x ,  y );
			return ( distanceSquared <= maxDistanceSquared ) ? this : null; 
		}

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
	}
	
	public static MapPoint createPoint(GeoLocation<?> location,Color color) {
		return new MapPoint(location,color);
	}
	
	public static MapPoint createPoint(GeoLocation<?> location,String label,Color color) {
		return new MapPoint(location,label,color);
	}	

	public PointRenderer(IImageProjection imageProject, Graphics g) {
		this.imageProject = imageProject;
		this.g = g;
	}

	@Override
	public void render(IMapElement element) 
	{
		final MapPoint point = (MapPoint) element;
		if ( ! point.isValid ) {
			point.calculateCoordinates( imageProject );
		}
		
		final int radius = 3;
		
		g.setColor( point.color );
		g.fillArc( point.point.x - radius , point.point.y - radius , radius*2 , radius*2 , 0 , 360 );			
		
		if ( point.label != null && element.getAttributes().containsKey( IMapElement.ATTRIBUTE_RENDER_LABEL ) ) {
			g.drawString( point.label ,  point.point.x + radius + 1,  point.point.y - radius - 1 );
		}
	}	
}