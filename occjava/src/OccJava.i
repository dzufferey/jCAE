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
 * (C) Copyright 2008, 2009, by EADS France
 */

%module OccJava

%{
#ifdef HAVE_CONFIG_H
//config.h generated by autotools from config.h.in (see an example in Opencascade).
#include "config.h"
#endif
#include <Adaptor3d_Curve.hxx>
#include <TopExp.hxx>
#include <Poly_Triangulation.hxx>
%}

// Handle enums with Java enums
%javaconst(1);
%include "enums.swg"

// Handle C arrays as Java arrays
%include "arrays_java.i";
%apply double[] {double *};
%apply double[] {double &};

// load the native library
%pragma(java) jniclasscode=%{
	static
	{
		System.loadLibrary("OccJava");
		if(!"0".equals(System.getenv("MMGT_OPT")))
			throw new RuntimeException("The MMGT_OPT environement variable must be set to 0 before using occjava.");
	}
%}

%include "Standard.i"
%include "gp.i"
%include "TCol.i"
%include "TopAbs.i"
%include "TopoDS.i"
%include "GeomAbs.i"
%include "TopTools.i"
%include "Geom.i"
%include "BRep.i"
%include "GeomLProp_SLProps.i"
%include "BRepTools.i"
%include "BRepBuilderAPI.i"
%include "BRepOffsetAPI.i"
%include "BRepPrimAPI.i"
%include "BRepAlgoAPI.i"
%include "Poly.i"
%include "BRepLib.i"
%include "BRepFilletAPI.i"
%include "BRepCheck.i"
%include "ShapeBuild.i"
%include "XSControl.i"
%include "ShapeFix.i"
%include "APIHeaderSection_MakeHeader.i"
// This one require Opencascade 6.2
%include "ShapeUpgrade.i"
//Jens Schmidt, req. f. Thesis
%include "GeomAPI.i"
%include "GC.i"


%typemap(javacode) TopExp
%{
	public static TopoDS_Vertex[] vertices(TopoDS_Edge edge)
	{
		TopoDS_Vertex first=new TopoDS_Vertex();
		TopoDS_Vertex second=new TopoDS_Vertex();
		vertices(edge, first, second);
		return new TopoDS_Vertex[]{first, second};
	}
%}

class TopLoc_Location
{
	%rename(isIdentity) IsIdentity;
	%rename(transformation) Transformation;
	public:
	Standard_Boolean IsIdentity();
	const gp_Trsf& Transformation();
};

class TopExp
{
	public:
	%rename(vertices) Vertices;
	static void Vertices(const TopoDS_Edge& E,TopoDS_Vertex& Vfirst,TopoDS_Vertex& Vlast,const Standard_Boolean CumOri = Standard_False) ;
};

/**
 * TopExp_Explorer
 */
%{#include "TopExp_Explorer.hxx"%}
class TopExp_Explorer
{
	public:
	TopExp_Explorer();
	TopExp_Explorer(const TopoDS_Shape& S,const TopAbs_ShapeEnum ToFind,
		const TopAbs_ShapeEnum ToAvoid = TopAbs_SHAPE);
	%rename(init) Init;
	%rename(more) More;
	%rename(next) Next;
	%rename(current) Current;
	void Init(const TopoDS_Shape& S, const TopAbs_ShapeEnum ToFind, 
		const TopAbs_ShapeEnum ToAvoid = TopAbs_SHAPE) ;
	Standard_Boolean More() const;
	void Next() ;
	const TopoDS_Shape & Current();
};

/**
 * Bnd_Box
 */
%{#include "Bnd_Box.hxx"%}
%typemap(javacode) Bnd_Box
%{
    /**
     * Return the array { Xmin, Ymin, Zmin, Xmax, Ymax, Zmax }
     */ 
	public double[] get()
	{
		double[] toReturn=new double[6];
		get(toReturn);
		return toReturn;
	}
%}

class Bnd_Box
{
	%rename(isVoid) IsVoid;
	public:
	Bnd_Box();
	Standard_Boolean IsVoid() const;
};

%extend Bnd_Box
{
	void get(double box[6])
	{
		if(!self->IsVoid())
			self->Get(box[0], box[1], box[2], box[3], box[4], box[5]);
	}
};

/**
 * BRepBndLib
 */
%{#include "BRepBndLib.hxx"%}
class BRepBndLib
{
	public:
	%rename(add) Add;
	static void Add(const TopoDS_Shape& shape,Bnd_Box& bndBox);
};

/**
 * Adaptor2d_Curve2d
 */
%{#include "Adaptor2d_Curve2d.hxx"%}

class Adaptor2d_Curve2d
{		
	Adaptor2d_Curve2d()=0;
	public:
	%rename(value) Value;
	virtual gp_Pnt2d Value(const Standard_Real U) const;
};

/**
 * Geom2dAdaptor_Curve
 */
%{#include "Geom2dAdaptor_Curve.hxx"%}
class Geom2dAdaptor_Curve: public Adaptor2d_Curve2d
{
	%rename(load) Load;
	public:
	Geom2dAdaptor_Curve();
	Geom2dAdaptor_Curve(const Handle_Geom2d_Curve & C);
	Geom2dAdaptor_Curve(const Handle_Geom2d_Curve & C,const Standard_Real UFirst,const Standard_Real ULast);
	void Load(const Handle_Geom2d_Curve & C) ;
	void Load(const Handle_Geom2d_Curve & C,const Standard_Real UFirst,const Standard_Real ULast) ;
};

/**
 * Adaptor3d_Curve
 */
%{#include "Adaptor3d_Curve.hxx"%}

class Adaptor3d_Curve
{		
	Adaptor3d_Curve()=0;
	public:
	%rename(firstParameter) FirstParameter;
	Standard_Real FirstParameter() const;
	%rename(lastParameter) LastParameter;
	Standard_Real LastParameter() const;
	%rename(continuity) Continuity;
	GeomAbs_Shape Continuity() const;
	%rename(value) Value;
	const gp_Pnt Value(const Standard_Real U) const;
	%rename(getType) GetType;
	GeomAbs_CurveType GetType() const;
};

//extends the Adaptor3d_Curve class to reduce the JNI overhead when
//calling a lot of Adaptor3d_Curve.Value
%extend Adaptor3d_Curve
{
	public:
	void arrayValues(int size, double u[])
	{
		for (int i = 0; i < size; i++)
		{
			gp_Pnt gp=self->Value(u[3*i]);
			u[3*i]   = gp.X();
			u[3*i+1] = gp.Y();
			u[3*i+2] = gp.Z();
		}
	}

	void d0(double u, double p[3])
    {
		gp_Pnt pp;
		self->D0(u, pp);
		p[0] = pp.X();
		p[1] = pp.Y();
		p[2] = pp.Z();
    }

	void d1(double u, double p[3], double v[3])
    {
		gp_Pnt pp;
		gp_Vec vv;
		self->D1(u, pp, vv);
		p[0] = pp.X();
		p[1] = pp.Y();
		p[2] = pp.Z();
		v[0] = vv.X();
		v[1] = vv.Y();
		v[2] = vv.Z();
    }

	void d2(double u, double p[3], double v1[3], double v2[3])
    {
		gp_Pnt pp;
		gp_Vec vv1, vv2;
		self->D2(u, pp, vv1, vv2);
		p[0] = pp.X();
		p[1] = pp.Y();
		p[2] = pp.Z();
		v1[0] = vv1.X();
		v1[1] = vv1.Y();
		v1[2] = vv1.Z();
		v2[0] = vv2.X();
		v2[1] = vv2.Y();
		v2[2] = vv2.Z();
    }
};

/**
 * GeomAdaptor_Curve
 */
%{#include "GeomAdaptor_Curve.hxx"%}

class GeomAdaptor_Curve: public Adaptor3d_Curve
{
	%rename(load) Load;
	public:
	GeomAdaptor_Curve();
	GeomAdaptor_Curve(const Handle_Geom_Curve & C);
	GeomAdaptor_Curve(const Handle_Geom_Curve & C,
		const Standard_Real UFirst,const Standard_Real ULast);
	void Load(const Handle_Geom_Curve & C) ;
	void Load(const Handle_Geom_Curve & C,
		const Standard_Real UFirst,const Standard_Real ULast) ;

};

/**
 * BRepAdaptor_Curve
 */
%{#include "BRepAdaptor_Curve.hxx"%}

class BRepAdaptor_Curve: public Adaptor3d_Curve
{
	%rename(initialize) Initialize;
	public:
	BRepAdaptor_Curve();
	BRepAdaptor_Curve(const TopoDS_Edge &E);
	BRepAdaptor_Curve(const TopoDS_Edge &E, const TopoDS_Face &F);
	void Initialize(const TopoDS_Edge &E);
	void Initialize(const TopoDS_Edge &E, const TopoDS_Face &F);
};

/**
 * Adaptor3d_Surface
 */
%{#include "Adaptor3d_Surface.hxx"%}

class Adaptor3d_Surface
{
	Adaptor3d_Surface()=0;
	public:
	%rename(firstUParameter) FirstUParameter;
	Standard_Real FirstUParameter() const;
	%rename(lastUParameter) LastUParameter;
	Standard_Real LastUParameter() const;
	%rename(firstVParameter) FirstVParameter;
	Standard_Real FirstVParameter() const;
	%rename(lastVParameter) LastVParameter;
	Standard_Real LastVParameter() const;
	%rename(uContinuity) UContinuity;
	GeomAbs_Shape UContinuity() const;
	%rename(vContinuity) VContinuity;
	GeomAbs_Shape VContinuity() const;
	%rename(value) Value;
	const gp_Pnt Value(const Standard_Real U, const Standard_Real V) const;
	%rename(getType) GetType;
	GeomAbs_SurfaceType GetType() const;
};

%extend Adaptor3d_Surface
{
	public:
	void d0(double u, double v, double p[3])
    {
		gp_Pnt pp;
		self->D0(u, v, pp);
		p[0] = pp.X();
		p[1] = pp.Y();
		p[2] = pp.Z();
    }

	void d1(double u, double v, double p[3], double d1u[3], double d1v[3])
    {
		gp_Pnt pp;
		gp_Vec dd1u, dd1v;
		self->D1(u, v, pp, dd1u, dd1v);
		p[0] = pp.X();
		p[1] = pp.Y();
		p[2] = pp.Z();
		d1u[0] = dd1u.X();
		d1u[1] = dd1u.Y();
		d1u[2] = dd1u.Z();
		d1v[0] = dd1v.X();
		d1v[1] = dd1v.Y();
		d1v[2] = dd1v.Z();
    }
};

/**
 * BRepAdaptor_Surface
 */
%{#include "BRepAdaptor_Surface.hxx"%}

class BRepAdaptor_Surface: public Adaptor3d_Surface
{
	%rename(initialize) Initialize;
	public:
	BRepAdaptor_Surface();
	BRepAdaptor_Surface(const TopoDS_Face &F, const Standard_Boolean R = Standard_True);
	void Initialize(const TopoDS_Face &F, const Standard_Boolean R = Standard_True);
};

/**
 * GProp_GProps
 */
 %{#include "GProp_GProps.hxx"%}
 class GProp_GProps
 {
	 public:
	 %rename(mass) Mass;
	 GProp_GProps();
	 Standard_Real Mass() const;
 };
 
/**
 * BRepGProp
 */
%{#include "BRepGProp.hxx"%}
class BRepGProp
{
	public:
	%rename(linearProperties) LinearProperties;
	%rename(surfaceProperties) SurfaceProperties;
	%rename(volumeProperties) VolumeProperties;
	static void LinearProperties(const TopoDS_Shape& shape, GProp_GProps& properties);
        static void VolumeProperties(const TopoDS_Shape& shape, GProp_GProps& properties, const Standard_Boolean onlyClosed = Standard_False) ;
        static Standard_Real VolumeProperties(const TopoDS_Shape& shape, GProp_GProps& properties, const Standard_Real Eps, const Standard_Boolean onlyClosed = Standard_False) ;
        static void SurfaceProperties(const TopoDS_Shape& shape, GProp_GProps& properties) ;
        static Standard_Real SurfaceProperties(const TopoDS_Shape& shape, GProp_GProps& properties, const Standard_Real Eps) ;
};

/**
 * BRepLProp
 */
%{#include "BRepLProp.hxx"%}
class BRepLProp
{
	public:
	%rename(continuity) Continuity;
	static GeomAbs_Shape Continuity(const BRepAdaptor_Curve & C1, const BRepAdaptor_Curve & C2, const Standard_Real u1, const Standard_Real u2);
};

/**
 *
 */
%rename(VOID) IFSelect_RetVoid;
%rename(DONE) IFSelect_RetDone;
%rename(ERROR) IFSelect_RetError;
%rename(FAIL) IFSelect_RetFail;
%rename(STOP) IFSelect_RetStop;
enum IFSelect_ReturnStatus {
 IFSelect_RetVoid,
 IFSelect_RetDone,
 IFSelect_RetError,
 IFSelect_RetFail,
 IFSelect_RetStop
};
 
%{#include <ShapeAnalysis_FreeBounds.hxx>%}
class ShapeAnalysis_FreeBounds
{
	%rename(getClosedWires) GetClosedWires;
	%rename(getOpenWires) GetOpenWires;
	public:
	ShapeAnalysis_FreeBounds(const TopoDS_Shape& shape,
		const Standard_Boolean splitclosed = Standard_False,
		const Standard_Boolean splitopen = Standard_True);
	const TopoDS_Compound& GetClosedWires() const;
	const TopoDS_Compound& GetOpenWires() const;
};

%{#include <GCPnts_UniformDeflection.hxx>%}
class GCPnts_UniformDeflection
{
	%rename(initialize) Initialize;
	%rename(nbPoints) NbPoints;
	%rename(parameter) Parameter;
	public:
	GCPnts_UniformDeflection();
	void Initialize(Adaptor3d_Curve& C,const Standard_Real Deflection,
		const Standard_Real U1,const Standard_Real U2,
		const Standard_Boolean WithControl = Standard_True) ;
	Standard_Integer NbPoints() const;
	Standard_Real Parameter(const Standard_Integer Index) const;
};

%{#include <BRepMesh_DiscretRoot.hxx>%}
class BRepMesh_DiscretRoot
{
	%rename(setDeflection) SetDeflection;
	%rename(setAngle) SetAngle;
	%rename(deflection) Deflection;
	%rename(angle) Angle;
	%rename(perform) Perform;
	
	protected:
	BRepMesh_DiscretRoot();
	public:
	void SetDeflection(const Standard_Real D) ;
	void SetAngle(const Standard_Real Ang) ;
	Standard_Real Deflection() const;
	Standard_Real Angle() const;
	virtual void Perform();
};

%{#include <BRepMesh_IncrementalMesh.hxx>%}
class BRepMesh_IncrementalMesh : public BRepMesh_DiscretRoot
{
	%rename(perform) Perform;
	%rename(isModified) IsModified;
	
	public:
	BRepMesh_IncrementalMesh();
	BRepMesh_IncrementalMesh(const TopoDS_Shape& S,const Standard_Real D,
		const Standard_Boolean Relatif = Standard_False,
		const Standard_Real Ang = 0.5);
		
	void Perform();
	Standard_Boolean IsModified() const;
};

%{#include <GeomAPI_ProjectPointOnSurf.hxx>%}

%typemap(javacode) GeomAPI_ProjectPointOnSurf
%{
	public void lowerDistanceParameters(double[] uv)
	{
		double[] d2=new double[1];
		lowerDistanceParameters(uv, d2);
		uv[1]=d2[0];
	}
%}

class GeomAPI_ProjectPointOnSurf
{
	%rename(init) Init;
	%rename(nbPoints) NbPoints;
	%rename(lowerDistanceParameters) LowerDistanceParameters;
	%rename(lowerDistance) LowerDistance;
	%rename(point) Point;
	%rename(parameters) Parameters;
	%rename(nearestPoint) NearestPoint;
	public:
	GeomAPI_ProjectPointOnSurf(const gp_Pnt& P,
		const Handle_Geom_Surface & Surface);
	void Init(const gp_Pnt& P,const Handle_Geom_Surface & surface);
	Standard_Integer NbPoints() const;	
	Quantity_Length LowerDistance() const;
	const gp_Pnt Point(const Standard_Integer Index) const;
	void LowerDistanceParameters(Quantity_Parameter& U,Quantity_Parameter& V) const;
	void Parameters(const Standard_Integer Index,Quantity_Parameter& U,Quantity_Parameter& V) const;
	gp_Pnt NearestPoint() const;
};

/**
 * BRepAlgo
 */
%{#include <BRepAlgo.hxx>%}
class BRepAlgo
{
	%rename(isValid) IsValid;
	%rename(isTopologicallyValid) IsTopologicallyValid;
	public:	
	static Standard_Boolean IsValid(const TopoDS_Shape& S);
	static Standard_Boolean IsTopologicallyValid(const TopoDS_Shape& S);
};

/**
 * BRepClass_FaceClassifier
 */
%{#include <BRepClass_FaceClassifier.hxx>%}
class BRepClass_FaceClassifier
{
	%rename(perform) Perform;
	%rename(state) State;
	public:
	BRepClass_FaceClassifier();
    void Perform(const TopoDS_Face & F, const gp_Pnt & P, const Standard_Real Tol);
	TopAbs_State State();
};
