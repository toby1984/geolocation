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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import com.jhlabs.map.proj.Projection;

import de.codesourcery.geoip.Coordinate;
import de.codesourcery.geoip.MapImage;
import de.codesourcery.geoip.ImageRegion;

/**
 * Simple map renderer.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SimpleMapRenderer implements IMapRenderer {

	private final Map<IMapElement.Type , List<IMapElement>> coordinates = new HashMap<>();

	private MapImage image;

	private IMapElementRendererFactory rendererFactory;

	public void removeAllCoordinates() {
		this.coordinates.clear();
	}

	public void setMapElementRendererFactory(IMapElementRendererFactory factory) {
		this.rendererFactory = factory;
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
	private boolean invalidationRequired = true;

	private void invalidateMapElements() {
		for ( IMapElement.Type  t : this.coordinates.keySet() ) 
		{
			for ( IMapElement e : this.coordinates.get( t ) ) {
				e.invalidate();
			}
		}
		invalidationRequired = false;
	}
	
	private ImageRegion lastRegion=null;
	private BufferedImage scaledImage;

	@Override
	public void renderMap(Graphics g, final ImageRegion region, final int width, final int height) 
	{
		final boolean sizeChanged = lastWidth != width || lastHeight != height;
		final boolean regionChanged = lastRegion == null || ! lastRegion.equals(region);

		if ( invalidationRequired || image.hasChanged() || regionChanged || sizeChanged || scaledImage == null) 
		{
			System.out.println("Recalculating projection");
			
			invalidateMapElements();
			image.resetChanged();
			
			lastRegion = region;
			lastWidth = width;
			lastHeight = height;
			
			// create scaled map image
			final Point topLeft = new Point();
			region.getTopLeftCorner( topLeft );

			final Dimension size = new Dimension();
			region.getSize( size );

			final int srcX2 = topLeft.x + size.width;
			final int srcY2 = topLeft.y + size.height;

			scaledImage = new BufferedImage( width , height , image.getImage().getType() );
			Graphics2D graphics = scaledImage.createGraphics();
			graphics.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC );
			graphics.drawImage( image.getImage(), 0 , 0, width , height , topLeft.x, topLeft.y , srcX2, srcY2 , null);
			graphics.dispose();
		}

		// render map image
		g.drawImage( scaledImage , 0 , 0 , null);

		final IImageProjection proj = new IImageProjection() 
		{
			private final Point2D.Double projOut = new Point2D.Double();

			private final double xPercentage = image.getOriginXPercentage();
			private final double yPercentage = image.getOriginYPercentage();

			private final double scaleX = image.getScaleX();
			private final double scaleY = image.getScaleY();

			private final Projection projection = image.getProjection();

			@Override
			public void project(Coordinate c, Point out) 
			{
				// project from lat/long to Proj cartesian coordinates
				projection.project( c.longitudeInRad() , c.latitudeInRad() , projOut );

				// convert Proj cartesian coordinates relative to the globe's coordinate system into
				// X/Y pixel coordinates on our unscaled world-map image
				toImageCoordinates( projOut , image.getImage() , projOut );		

				// now convert X/Y pixel coordinates from unscaled world-image to view coordinates
				region.project( width, height ,  projOut );
				
				out.x = (int) Math.round(projOut.x);
				out.y = (int) Math.round(projOut.y);
			}

			private void toImageCoordinates(Point2D.Double cartesianProjection,BufferedImage image,Point2D.Double out) 
			{
				final double srcWidth=image.getWidth();
				final double srcHeight=image.getHeight();
				
				final double globeCenterX = xPercentage * srcWidth;
				final double globeCenterY = yPercentage * srcHeight;

				double scaleX = this.scaleX * (srcWidth/1000.0);
				double scaleY = this.scaleY * (srcHeight/1000.0);	

				out.x = globeCenterX + cartesianProjection.x * scaleX;
				out.y = globeCenterY - cartesianProjection.y * scaleY;
			}

			@Override
			public int getWidthInPixels() {
				return width;
			}

			@Override
			public int getHeightInPixels() {
				return height;
			}			
		};

		for ( IMapElement.Type type : this.coordinates.keySet() ) 
		{
			final List<IMapElement> list = this.coordinates.get(type );
			final IMapElementRenderer renderer = rendererFactory.createRenderer( type , proj , g );
			for ( IMapElement element : list ) 
			{
				if ( element.isVisible( proj ) ) {
					renderer.render( element );
				}
			}
		}
	}

	@Override
	public void setMapImage(MapImage mapImage) {
		this.image = mapImage;
		invalidationRequired=true;
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

	@Override
	public MapImage getMapImage() {
		return image;
	}

    @Override
    public Collection<IMapElement> getMapElements() {
        return coordinates.values().stream().flatMap( l -> l.stream() ).collect( Collectors.toList() );
    }
}