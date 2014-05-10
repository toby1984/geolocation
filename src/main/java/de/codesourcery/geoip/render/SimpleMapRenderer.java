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
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jhlabs.map.proj.MillerCylindrical1Projection;
import com.jhlabs.map.proj.Projection;

import de.codesourcery.geoip.Coordinate;

/**
 * Simple map renderer.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SimpleMapRenderer implements IMapRenderer {
	
	private final Map<IMapElement.Type , List<IMapElement>> coordinates = new HashMap<>();
	
	private BufferedImage image;
	
	// Scaling factors used to map cartesian coordinates returned by the 
	// map projection to actual image coordinates
	private double scaleX=160;
	private double scaleY=225;
	
    // These are where the prime meridian crosses 0° latitude , 0° longitude - all
	// lat/long coordinates are relative to this point
	
	// these values are percentage values relative to the map image size (e.g. 0.5/0.5 would be right in the center of the image)
    private double xPercentage=0.5;
    private double yPercentage=0.5009541984732825;
    
    private IMapElementRendererFactory rendererFactory;
    
	private Projection projection = new MillerCylindrical1Projection();	    
	
	public void removeAllCoordinates() {
		this.coordinates.clear();
	}
	
	public void setMapElementRendererFactory(IMapElementRendererFactory factory) {
		this.rendererFactory = factory;
	}
	
	public void setScale(double scaleX,double scaleY) 
	{
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	public double getScaleX() {
		return scaleX;
	}
	
	public double getScaleY() {
		return scaleY;
	}
	
	public void addCoordinates(Collection<IMapElement> elements) {
		for ( IMapElement e : elements ) {
			putElement(e);
		}
	}	
	
	public void addCoordinates(IMapElement c1,IMapElement... c2) {
		putElement( c1 );
		if ( c2 != null ) 
		{
			for( IMapElement e : c2 ) {
				putElement(e);
			}
		}
	}
	
	private void putElement(IMapElement e) {
		List<IMapElement> list = this.coordinates.get( e.getType() );
		if ( list == null ) {
			list = new ArrayList<>();
			this.coordinates.put(e.getType(),list);
		}
		list.add(e);
	}
	
	public void addCoordinate(IMapElement c) {
		putElement(c);
	}
	
	public boolean removeCoordinate(IMapElement c) 
	{
		final List<IMapElement> list = this.coordinates.get( c.getType() );
		if ( list != null ) {
			return list.remove(c);
		}
		return false;
	}
	
	private int lastWidth  = -1;
	private int lastHeight = -1;

	@Override
	public void renderMap(Graphics g, final int width, final int height) 
	{
		if ( lastWidth != width || lastHeight != height ) 
		{
			for ( IMapElement.Type  t : this.coordinates.keySet() ) 
			{
				for ( IMapElement e : this.coordinates.get( t ) ) {
					e.invalidate();
				}
			}
			lastWidth = width;
			lastHeight = height;
		}
		
		final IImageProjection proj = new IImageProjection() 
		{
			private final int imageWidth=width;
			private final int imageHeight=height;
			private final Point2D.Double projOut = new Point2D.Double();
			
			private final double xPercentage = SimpleMapRenderer.this.xPercentage;
			private final double yPercentage = SimpleMapRenderer.this.yPercentage;
			
			private final double scaleX = SimpleMapRenderer.this.scaleX;
			private final double scaleY = SimpleMapRenderer.this.scaleY;		
			
			private final Projection projection = SimpleMapRenderer.this.projection;
			
			@Override
			public void project(Coordinate c, Point out) 
			{
				projection.project( c.longitudeInRad() , c.latitudeInRad() , projOut );
				toImageCoordinates( projOut , imageWidth , imageHeight , out );				
			}
			
			private void toImageCoordinates(Point2D.Double projection,double imageWidth, double imageHeight,Point imageXY) 
			{
				  final double globeCenterX = xPercentage * imageWidth;
				  final double globeCenterY = yPercentage * imageHeight;
				  
				  double scaleX = this.scaleX*(imageWidth/1000.0);
				  double scaleY = this.scaleY*(imageHeight/1000.0);	
				  
				  double x = globeCenterX + projection.x*scaleX;
				  double y = globeCenterY - projection.y*scaleY;
				  
				  imageXY.x = (int) Math.round(x);
				  imageXY.y = (int) Math.round(y);
			}			
		};
		
		// render map image
		g.drawImage( image , 0 , 0 , width , height , null );
		
		for ( IMapElement.Type type : this.coordinates.keySet() ) 
		{
			final List<IMapElement> list = this.coordinates.get(type );
			final IMapElementRenderer renderer = rendererFactory.createRenderer( type , proj , g );
			for ( IMapElement coordinate : list ) 
			{
				renderer.render( coordinate );
			}
		}
	}
	
	@Override
	public void setMapImage(BufferedImage image) {
		this.image = image;
	}
	
	@Override
	public void setOrigin(double xPercentage, double yPercentage) {
		this.xPercentage = xPercentage;
		this.yPercentage = yPercentage;
	}
	
	@Override
	public void setProjection(Projection projection) {
		this.projection = projection;
	}

	@Override
	public IMapElement getClosestMapElement(int x, int y, double maxDistanceSquared) 
	{
		IMapElement closest = null;
		double closestDistanceSquared = 0;
		for ( IMapElement.Type type : this.coordinates.keySet() ) {
			final List<IMapElement> list = this.coordinates.get(type );
			for ( IMapElement e : list ) 
			{
				IMapElement candidate = e.getClosestMapElement( x ,  y ,  maxDistanceSquared );
				if ( candidate != null ) 
				{
					double distanceSquared = candidate.distanceSquared( x , y );
					if ( distanceSquared <= maxDistanceSquared && ( closest == null || distanceSquared < closestDistanceSquared ) ) {
						closest = e;
						closestDistanceSquared = distanceSquared;
					}
				}
			}
		}
		return closest;
	}
}