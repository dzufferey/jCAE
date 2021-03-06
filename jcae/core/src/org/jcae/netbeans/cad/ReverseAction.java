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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.cad;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ReverseAction extends CookieAction
{	
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{NbShape.class};
	}

	protected void performAction(Node[] nodes)
	{
		for(Node node: nodes)
		{
			GeomUtils.getShape(node).reverse();
			GeomUtils.getParentBrep(node).getDataObject().setModified(true);
		}
	}
	
	public String getName()
	{
		return "Reverse";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	@Override
	protected boolean asynchronous()
	{
		return false;
	}
	
	@Override
	protected String iconResource()
	{
		return "org/jcae/netbeans/cad/reverse.gif";
	}
}
