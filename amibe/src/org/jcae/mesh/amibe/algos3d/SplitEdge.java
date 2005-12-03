/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.NotOrientedEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.PAVLSortedTree2;
import org.jcae.mesh.amibe.util.PAVLNodeOTriangle;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Split long edges.  Edges are sorted and splitted in turn, the longest edge
 * being processed first.
 * TODO: edges should be swapped too to improve triangle quality.
 */
public class SplitEdge
{
	private static Logger logger=Logger.getLogger(SplitEdge.class);
	private Mesh mesh;
	private double minlen2;
	
	/**
	 * Creates a <code>SplitEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param l  target size
	 */
	public SplitEdge(Mesh m, double l)
	{
		mesh = m;
		minlen2 = 2.0 * l * l;
	}
	
	/**
	 * Split all edges which are longer than minlen2*sqrt(2).
	 * Edges are sorted and the longest edge is always splitted.
	 */
	public void compute()
	{
		logger.debug("Running SplitEdge");
		// There were problems with PAVLSortedTree,
		// try PAVLSortedTree2 instead.
		PAVLSortedTree2 tree = computeTree();
		splitAllEdges(tree);
	}
	
	private PAVLSortedTree2 computeTree()
	{
		PAVLSortedTree2 tree = new PAVLSortedTree2();
		NotOrientedEdge noe = new NotOrientedEdge();
		NotOrientedEdge sym = new NotOrientedEdge();
		unmarkEdges();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			noe.bind(f);
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				if (noe.hasAttributes(OTriangle.MARKED))
					continue;
				noe.setAttributes(OTriangle.MARKED);
				if (noe.getAdj() != null)
				{
					OTriangle.symOTri(noe, sym);
					if (sym.hasAttributes(OTriangle.MARKED))
						continue;
					sym.setAttributes(OTriangle.MARKED);
				}
				addToTree(noe, tree);
			}
		}
		unmarkEdges();
		return tree;
	}
	
	private static double cost(NotOrientedEdge noe)
	{
		double [] p0 = noe.origin().getUV();
		double [] p1 = noe.destination().getUV();
		return (p1[0] - p0[0]) * (p1[0] - p0[0]) +
		       (p1[1] - p0[1]) * (p1[1] - p0[1]) +
		       (p1[2] - p0[2]) * (p1[2] - p0[2]);
	}
	
	private void addToTree(NotOrientedEdge noe, PAVLSortedTree2 tree)
	{
		if (noe.hasAttributes(OTriangle.OUTER))
			return;
		double l2 = cost(noe);
		if (l2 > minlen2)
			tree.insert(new PAVLNodeOTriangle(noe, l2));
	}
	
	private void unmarkEdges()
	{
		NotOrientedEdge noe = new NotOrientedEdge();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			f.unsetMarked();
		}
	}
	
	private boolean splitAllEdges(PAVLSortedTree2 tree)
	{
		int splitted = 0;
		NotOrientedEdge edge = new NotOrientedEdge();
		NotOrientedEdge sym = new NotOrientedEdge();
		OTriangle temp = new OTriangle();
		double [] newXYZ = new double[3];
		double sinMin = Math.sin(Math.PI / 36.0);
		while (tree.size() > 0)
		{
			PAVLNodeOTriangle current = (PAVLNodeOTriangle) tree.last();
			Vertex v = null;
			do
			{
				edge.bind(current.getTriangle(), current.getLocalNumber());
				double key = current.getKey();
				if (cost(edge) != key)
				{
					// This edge has been modified
					PAVLNodeOTriangle next = (PAVLNodeOTriangle) tree.prev();
					tree.remove(current);
					addToTree(edge, tree);
					current = next;
					continue;
				}
				// New point
				double [] p0 = edge.origin().getUV();
				double [] p1 = edge.destination().getUV();
				for (int i = 0; i < 3; i++)
					newXYZ[i] = 0.5*(p0[i]+p1[i]);
				v = new Vertex(newXYZ[0], newXYZ[1], newXYZ[2]);
				if (edge.hasAttributes(OTriangle.BOUNDARY))
				{
					// FIXME: Check deflection
					mesh.setRefVertexOnboundary(v);
					break;
				}
				// Discrete differential operators, and thus
				// discreteProject, does not work on boundary
				// nodes.
				Vertex vm = edge.origin();
				if (vm.getRef() != 0)
					vm = edge.destination();
				if (vm.getRef() == 0 && !vm.discreteProject(v))
				{
					PAVLNodeOTriangle next = (PAVLNodeOTriangle) tree.prev();
					tree.remove(current);
					current = next;
					continue;
				}
				double dapex = v.distance3D(edge.apex());
				if (!edge.hasAttributes(OTriangle.BOUNDARY))
				{
					OTriangle.symOTri(edge, sym);
					dapex = Math.min(dapex, v.distance3D(sym.apex()));
				}
				if (dapex * dapex > minlen2 / 16.0)
					break;
				//  A new point would be near an existing
				//  one.  It is likely that this is still
				//  true when this edge is modified, so
				//  remove it from the tree to speed up
				//  processing.
				PAVLNodeOTriangle next = (PAVLNodeOTriangle) tree.prev();
				tree.remove(current);
				current = next;
			} while (current != null);
			if (current == null)
				break;
			tree.remove(current);
			if (logger.isDebugEnabled())
				logger.debug("Split edge: "+edge);
			edge.split(v);
			assert edge.destination() == v : v+" "+edge;
			splitted++;
			// Update edge length
			for (int i = 0; i < 3; i++)
			{
				addToTree(edge, tree);
				edge.prevOTri();
			}
			edge.prevOTriDest();
			assert edge.destination() == v : v+" "+edge;
			for (int i = 0; i < 2; i++)
			{
				edge.prevOTri();
				addToTree(edge, tree);
			}
			if (edge.getAdj() != null)
			{
				edge.symOTri();
				assert edge.destination() == v : v+" "+edge;
				for (int i = 0; i < 2; i++)
				{
					edge.prevOTri();
					addToTree(edge, tree);
				}
				edge.symOTri();
				edge.prevOTri();
				addToTree(edge, tree);
			}
		}
		assert mesh.isValid();
		logger.info("Number of splitted edges: "+splitted);
		return splitted > 0;
	}
	
}
