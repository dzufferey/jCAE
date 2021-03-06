/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.util.Iterator;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge.Quality;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.projection.TriangleKdTree;

/**
 * Swap edges around a give vertex to improve triangles quality
 * @author Jerome Robert
 */
public class VertexSwapper {
	private final Mesh mesh;
	private final Quality quality = new Quality();
	private TriangleKdTree kdTree;
	private int group = -1;
	public VertexSwapper(MeshLiaison liaison) {
		this(liaison, null);
	}

	public VertexSwapper(Mesh mesh) {
		this(null, mesh);
	}

	private VertexSwapper(MeshLiaison liaison, Mesh mesh) {
		quality.setLiaison(liaison);
		if(liaison == null)
			this.mesh = mesh;
		else
			this.mesh = liaison.getMesh();
		assert this.mesh != null;
	}

	public void setKdTree(TriangleKdTree kdTree) {
		this.kdTree = kdTree;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	/**
	 * Swap the edges whose v is the origin
	 * This method is much slower than swap and should be avoided when possible
	 */
	public void swapOrigin(Vertex v)
	{
		loop: while(true)
		{
			Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
			while(it.hasNext())
			{
				HalfEdge he = (HalfEdge) it.next();
				if(swapImpl(he) != he)
					continue loop;
			}
			break loop;
		}
	}

	/** Swap the edges whose v is the apex */
	public void swap(Vertex v)
	{
		if(v.isManifold())
		{
			swapManifold(v, (Triangle)v.getLink());
		}
		else
		{
			for(Triangle t:(Triangle[])v.getLink())
				swapManifold(v, t);
		}
	}

	/** Return true if the edge should be swapped */
	protected boolean isQualityImproved(Quality quality)
	{
		return quality.getSwappedAngle() > 0 &&
			quality.getSwappedQuality() > quality.getQuality();
	}

	/**
	 * Allow subclasser to customize when an half edge can be swapped.
	 * By default this call HalfEdge.canSwapTopology
	 */
	protected boolean canSwap(HalfEdge e)
	{
		return e.canSwapTopology();
	}

	private void swapManifold(Vertex v, Triangle triangle)
	{
		HalfEdge current = (HalfEdge) v.getIncidentAbstractHalfEdge(triangle, null);
		current = current.next();
		Vertex o = current.origin();
		assert current.apex() == v;
		boolean redo = true;
		while(redo)
		{
			redo = false;
			while(true)
			{
				HalfEdge next = swapImpl(current);
				boolean swapped = next != current;
				current = next;
				if(swapped)
					redo = true;
				else
				{
					current = current.nextApexLoop();
					if (current.origin() == o)
						break;
				}
			}
		}
	}

	private HalfEdge swapImpl(HalfEdge current)
	{
		if (!current.hasAttributes(AbstractHalfEdge.NONMANIFOLD |
			AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.OUTER | AbstractHalfEdge.IMMUTABLE)
			&& canSwap(current))
		{
			quality.setEdge(current);
			if(isQualityImproved(quality))
			{
				if(kdTree != null)
				{
					kdTree.remove(current.getTri());
					kdTree.remove(current.sym().getTri());
				}
				current = (HalfEdge) mesh.edgeSwap(current);
				HalfEdge swapped = current.next();
				if(kdTree != null)
				{
					kdTree.addTriangle(swapped.getTri());
					kdTree.addTriangle(swapped.sym().getTri());
				}
			}
		}
		return current;
	}
}
