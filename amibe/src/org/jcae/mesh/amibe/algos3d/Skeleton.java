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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import gnu.trove.set.hash.TIntHashSet;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.Amibe2VTK;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Compute polylines from non-manifold, boundary, and group boundary edges
 * @author Jerome Robert
 */
public class Skeleton {
	private final Collection<List<AbstractHalfEdge>> polylines;
	private final Set<Integer> groups1 = HashFactory.createSet();
	private final Set<Integer> groups2 = HashFactory.createSet();
	/**
	 * @param mesh
	 * @param angle min polyline angle
	 */
	public Skeleton(Mesh mesh, double angle)
	{
		polylines = computePolylines(getNonManifoldHE(mesh), angle);
	}

	/**
	 * Return half edges witch are border of the give groups
	 * @param groupIds
	 * @return
	 */
	public Collection<AbstractHalfEdge> getByGroups(int ... groupIds)
	{
		ArrayList<AbstractHalfEdge> a = new ArrayList<AbstractHalfEdge>();
		for(List<AbstractHalfEdge> l:getPolylines(groupIds))
			a.addAll(l);
		return a;
	}

	public Collection<AbstractHalfEdge> getByGroups(int groupIds)
	{
		ArrayList<AbstractHalfEdge> a = new ArrayList<AbstractHalfEdge>();
		for(List<AbstractHalfEdge> l:getPolylines(groupIds))
			a.addAll(l);
		return a;
	}

	public Collection<AbstractHalfEdge> getByGroupsAtLeast(int ... groupIds)
	{
		ArrayList<AbstractHalfEdge> a = new ArrayList<AbstractHalfEdge>();
		for(List<AbstractHalfEdge> l:getPolylinesAtLeast(groupIds))
			a.addAll(l);
		return a;
	}

	public Collection<List<AbstractHalfEdge>> getPolylines(int groupIds)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		main: for(List<AbstractHalfEdge> l: polylines)
		{
			AbstractHalfEdge e = l.get(0);
			if(e.getTri().getGroupId() == groupIds)
				toReturn.add(l);
		}
		return toReturn;
	}
	/**
	 * Return polylines which are border of, at least, the given groups without
	 * taking the order into account
	 * @param groupIds
	 * @return
	 */
	public Collection<List<AbstractHalfEdge>> getPolylinesAtLeast(int ... groupIds)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		ArrayList<Integer> query = new ArrayList<Integer>();
		for(int i: groupIds)
			query.add(i);
		TreeSet<Integer> current = new TreeSet<Integer>();
		for(List<AbstractHalfEdge> l: polylines)
		{
			AbstractHalfEdge e = l.get(0);
			if(e.getTri().getGroupId() == groupIds[0])
			{
				if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					Iterator<AbstractHalfEdge> it = e.fanIterator();
					while(it.hasNext())
					{
						AbstractHalfEdge ne = it.next();
						current.add(ne.getTri().getGroupId());
					}
				}
				else if(e.hasAttributes(AbstractHalfEdge.BOUNDARY) && groupIds.length == 1)
				{
					current.add(e.getTri().getGroupId());
				}
				else
				{
					current.add(e.getTri().getGroupId());
					current.add(e.sym().getTri().getGroupId());
				}
			}
			if(current.containsAll(query))
				toReturn.add(l);
			current.clear();
		}
		return toReturn;
	}

	/**
	 * Return polylines which are border of the given groups
	 * @param groupIds
	 * @return
	 */
	public Collection<List<AbstractHalfEdge>> getPolylines(int ... groupIds)
	{
		TIntHashSet nonManifoldGroupsQuery = null, nonManifoldGroups = null;
		if(groupIds.length > 2) {
			nonManifoldGroupsQuery = new TIntHashSet(groupIds);
			nonManifoldGroups = new TIntHashSet();
		}
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		main: for(List<AbstractHalfEdge> l: polylines)
		{
			AbstractHalfEdge e = l.get(0);
			if(e.getTri().getGroupId() == groupIds[0])
			{
				if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD) && nonManifoldGroups != null)
				{
					nonManifoldGroups.clear();
					Iterator<AbstractHalfEdge> it = e.fanIterator();
					while(it.hasNext())
						nonManifoldGroups.add(it.next().getTri().getGroupId());
					if(nonManifoldGroups.equals(nonManifoldGroupsQuery))
						toReturn.add(l);
				}
				else if(e.hasAttributes(AbstractHalfEdge.BOUNDARY) && groupIds.length == 1)
				{
					toReturn.add(l);
				}
				else if(groupIds.length == 2 && e.sym().getTri().getGroupId() == groupIds[1])
				{
					toReturn.add(l);
				}
			}
		}
		return toReturn;
	}

	/** Wrap List&ltVertex&gt to ensure polyline unicity */
	private class VertexPolyline
	{
		public final List<Vertex> vertices;

		public VertexPolyline(List<AbstractHalfEdge> edges) {
			vertices = new ArrayList<Vertex>(edges.size() + 1);
			for(AbstractHalfEdge e:edges)
				vertices.add(e.origin());
			vertices.add(edges.get(edges.size()-1).destination());
		}

		@Override
		public boolean equals(Object obj) {
			List<Vertex> o = ((VertexPolyline) obj).vertices;
			int s = vertices.size() - 1;
			int os = o.size() - 1;
			return s == os &&
				((vertices.get(0) == o.get(0) &&
				vertices.get(1) == o.get(1) &&
				vertices.get(s) == o.get(s) &&
				vertices.get(s - 1) == o.get(s - 1)) ||
				(vertices.get(0) == o.get(s) &&
				vertices.get(1) == o.get(s-1) &&
				vertices.get(s) == o.get(0) &&
				vertices.get(1) == o.get(s - 1)));
		}

		@Override
		public int hashCode() {
			return vertices.get(0).hashCode() +
				vertices.get(vertices.size()-1).hashCode();
		}
	}

	/**
	 * Return all polylines as vertices.
	 *
	 */
	public Collection<List<Vertex>> getPolylinesVertices()
	{
		Set<VertexPolyline> hs = HashFactory.createSet(polylines.size());
		for(List<AbstractHalfEdge> l:polylines)
			hs.add(new VertexPolyline(l));
		ArrayList<List<Vertex>> toReturn = new ArrayList<List<Vertex>>(hs.size());
		for(VertexPolyline l:hs)
			toReturn.add(l.vertices);
		return toReturn;
	}

	/**
	 * Return all polylines as half edges.
	 * One polyline is returned by fan (ex 3 polyline for a T junction).
	 */
	public Collection<List<AbstractHalfEdge>> getPolylines()
	{
		return Collections.unmodifiableCollection(polylines);
	}

	private Collection<AbstractHalfEdge> getNonManifoldHE(Mesh mesh)
	{
		Set<AbstractHalfEdge> toReturn = HashFactory.createSet();
		for(Triangle t:mesh.getTriangles())
		{
			AbstractHalfEdge he = t.getAbstractHalfEdge();
			assert he != null;
			for(int i = 0; i < 3; i++)
			{
				if(isNonManifold(he))
					toReturn.add(he);
				he = he.next();
			}
		}
		return toReturn;
	}

	/**
	 * Method use to filter hald edges which will be included in the skeleton
	 * @return true if the half edge must be included
	 */
	protected boolean isNonManifold(AbstractHalfEdge he)
	{
		if(he.hasAttributes(AbstractHalfEdge.OUTER))
			return false;
		return he.hasAttributes(AbstractHalfEdge.NONMANIFOLD) ||
			he.hasAttributes(AbstractHalfEdge.BOUNDARY) ||
			he.getTri().getGroupId() != he.sym().getTri().getGroupId();
	}

	private void getGroups(AbstractHalfEdge v, Collection<Integer> groups)
	{
		if(v.hasAttributes(AbstractHalfEdge.BOUNDARY))
		{
			groups.add(v.getTri().getGroupId());
		}
		else if(v.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			Iterator<AbstractHalfEdge> it = v.fanIterator();
			while(it.hasNext())
				groups.add(it.next().getTri().getGroupId());
		}
		else
		{
			groups.add(v.getTri().getGroupId());
			groups.add(v.sym().getTri().getGroupId());
		}
	}

	/** Return true if edge.destination() is a polyline end */
	private boolean isPolylineEnd(AbstractHalfEdge edge, double angle)
	{
		AbstractHalfEdge next = null;
		if(!edge.destination().isMutable())
		{
			return true;
		}
		else if(edge.destination().isManifold())
		{
			Triangle triangle = (Triangle) edge.destination().getLink();
			AbstractHalfEdge ot = edge.destination().getIncidentAbstractHalfEdge(triangle, null);
			assert ot.origin() == edge.destination();
			Vertex d = ot.destination();
			do
			{
				if(ot.destination() != edge.origin() && isNonManifold(ot))
				{
					if(next == null)
						next = ot;
					else
						return true;
				}
				ot = ot.nextOriginLoop();
			}
			while (ot.destination() != d);
		}
		else
		{
			Iterator<AbstractHalfEdge> it = edge.destination().getNeighbourIteratorAbstractHalfEdge();
			while(it.hasNext())
			{
				AbstractHalfEdge e = it.next();
				assert e != edge;
				assert e.origin() == edge.destination();
				if((next == null || e.destination() != next.destination()) &&
					e.destination() != edge.origin() && isNonManifold(e))
				{
					if(next == null)
						next = e;
					else
						return true;
				}
			}
		}
		if(next == null)
			// Cannot find the next segment so this is a end.
			// this happen when the next whould a have been of type which is
			// filtered by the isNonManifold method
			return true;
		if(edge.hasAttributes(AbstractHalfEdge.IMMUTABLE) != next.hasAttributes(
			AbstractHalfEdge.IMMUTABLE))
			return true;

		if(edge.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
			next.hasAttributes(AbstractHalfEdge.BOUNDARY))
		{
			if(edge.getTri().getGroupId() != next.getTri().getGroupId())
			return true;
		}
		else
		{
			groups1.clear();
			groups2.clear();
			getGroups(edge, groups1);
			getGroups(next, groups2);
			if(!groups1.equals(groups2))
				return true;
		}
		if(angle > 2*Math.PI)
			return true;
		return edge.destination().angle3D(edge.origin(), next.destination()) < angle;
	}

	private List<AbstractHalfEdge> createPolyline(AbstractHalfEdge startEdge,
		Collection<Vertex> possibleEnds)
	{
		ArrayList<AbstractHalfEdge> beams = new ArrayList<AbstractHalfEdge>();
		Vertex cv = startEdge.destination();
		AbstractHalfEdge cb = startEdge;
		beams.add(startEdge);
		while(cv != startEdge.origin() && !possibleEnds.contains(cv))
		{
			cb = cb.next();
			while(!isNonManifold(cb))
				cb = cb.sym().next();
			beams.add(cb);
			cv = cb.destination();
		}
		return beams;
	}

	private Collection<List<AbstractHalfEdge>> computePolylines(
		Collection<AbstractHalfEdge> input, double angle)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		Collection<AbstractHalfEdge> beamSet = HashFactory.createSet(input);
		Set<Vertex> polylineEnds = HashFactory.createSet();
		for(AbstractHalfEdge b:beamSet)
		{
			if(isPolylineEnd(b, angle))
				polylineEnds.add(b.destination());
		}

		//The first iteration is for polyline ends detected by isPolylineEnd.
		//Following iterations are for smooth polylines
		do
		{
			for(Vertex bv:polylineEnds)
			{
				Iterator<AbstractHalfEdge> it = bv.getNeighbourIteratorAbstractHalfEdge();
				while(it.hasNext())
				{
					AbstractHalfEdge startBeam = it.next();
					if(beamSet.contains(startBeam))
					{
						List<AbstractHalfEdge> polylineB = createPolyline(
							startBeam, polylineEnds);
						beamSet.removeAll(polylineB);
						toReturn.add(Collections.unmodifiableList(polylineB));
					}
				}
			}
			polylineEnds.clear();
			// Beams which are in smooth loops are not detected by
			// isPolylineEnd. At this step beamSet should only contains such
			// beams, so any vertex could be concidered as a polyline end.
			if(!beamSet.isEmpty())
				polylineEnds.add(beamSet.iterator().next().origin());
		}
		while(!polylineEnds.isEmpty());
		return toReturn;
	}

	public static void main(final String[] args) {
		try {
			Mesh m = new Mesh(MeshTraitsBuilder.getDefault3D());
			assert m.hasAdjacency();
			MeshReader.readObject3D(m, "/home/robert/ast-a319-neo/demo-anabelle/demo/amibe.dir");
			Skeleton sk = new Skeleton(m, 0);
			System.out.println(sk.getPolylines().size());
			int k = 1000;
			for(List<AbstractHalfEdge> p:sk.getPolylines())
			{
				for(AbstractHalfEdge e:p)
					m.addBeam(e.origin(), e.destination(), k);
				k++;
			}
			m.getTriangles().clear();
			MeshWriter.writeObject3D(m, "/tmp/zob.amibe", null);
			new Amibe2VTK("/tmp/zob.amibe").write("/tmp/zob.vtp");
		} catch (Exception ex) {
			Logger.getLogger(Skeleton.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}
}
