/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2007,2008,2009, by EADS France
 
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

package org.jcae.mesh.bora.algo;

import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Vertex0d implements AlgoInterface
{
	private static final Logger LOGGER=Logger.getLogger(Vertex0d.class.getName());
	
	public boolean isAvailable()
	{
		return true;
	}

	public int getOrientation(int o)
	{
		return o;
	}

	public boolean compute(BDiscretization d)
	{
		BCADGraphCell cell = d.getGraphCell();
		d.setMesh(cell.getShape());
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, ""+this+"  shape: "+d.getMesh());
		return true;
	}
	
	@Override
	public final String toString()
	{
		return "Algo: "+getClass().getName();
	}
}
