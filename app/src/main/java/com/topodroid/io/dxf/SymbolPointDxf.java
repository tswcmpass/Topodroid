/* @file SymbolPointDxf.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief TopoDroid drawing: point symbol dxf encoder
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.io.dxf;

// import com.topodroid.math.BezierCurve;
// import com.topodroid.math.Point2D;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedWriter;


import java.util.Locale;
import java.util.ArrayList;

public class SymbolPointDxf
{
  public void line( String layer, float x0, float y0, float x1, float y1, int col )
  {
    startLine( layer );
    addHandle( DXF.ACAD_12 );
    addAcDbLine( false );
    addLine( x0, y0, x1, y1, col );
  }

  public void polyline( String layer, float[] xx, float[] yy, int col )
  {
    startPolyline( /* layer */ );
    addHandle( DXF.ACAD_12 );
    addColor( DXF.lt_byLayer );
    addAcDbEntity( DXF.ACAD_12, null ); // 2021-A
    addLayer( TOKEN_POLYLINE, DXF.ACAD_9, layer );  // 2021-A    !!2022 ACAD_14 must be write layer!!
    addAcDbPolyline();
    addPolylineColor( col ); // HBX 256 -> col
    addPolylineGroup( 1 );
    addPolylineLineWidth( 0 );
    addPolylineNPoints( xx.length );
    addPolylineClosed( false );
    addPolylinePosition( 0, 0, 0 );
    for ( int k=0; k<xx.length; ++k ) {
      startVertex( null ); // null layer
      addHandlePointer( DXF.ACAD_12 );
      addAcDbEntity( DXF.ACAD_12, null );
      addLayer( TOKEN_VERTEX, DXF.ACAD_9, layer ); 
      addAcDbVertex( DXF.ACAD_12 );
      addVertexData( xx[k], yy[k] );
      addVertexFlag( DXF.ACAD_12, 32 );
    }
    closeSeq();
    addHandlePointer( DXF.ACAD_12 ); // 
    addAcDbEntity( DXF.ACAD_12, layer );
    // addHandle( DXF.ACAD_12 ); already in HandlePointer
  }

  public void circle( String layer, float x, float y, float r, int col )
  {
    startCircle( layer );
    addAcDbCircle( true );
    addCircle( x, y, r, col );
  }


  // DXF.printString( pw, 0, "ARC" );
  // DXF.printString( pw, 8, layer );
  // DXF.printAcDb(pw, -1, DXF.AcDbEntity, DXF.AcDbCircle );
  // DXF.printXYZ( pw, (x0+x1)/2*dxfScale, -(y0+y1)/2*dxfScale, 0.0f, 0 ); // CENTER
  // DXF.printFloat( pw, 40, x1*dxfScale );                                // RADIUS
  // DXF.printString( pw, 100, DXF.AcDbArc );
  // DXF.printFloat( pw, 50, x2 );                                         // ANGLES
  // DXF.printFloat( pw, 51, x2+y2 );

  /** TODO
   * @param layer  layer
   * @param x      center X coord
   * @param y      center Y coord
   * @param r      radius
   * @param a1     start angle (?)
   * @param a2     end angle (?)
   * @param col    color index
   */
  public void arc( String layer, float x, float y, float r, float a1, float a2, int col )
  {
    startArc( layer );
    addAcDbCircle( true ); 
    addCircle( x, y, r, col );
    addAcDbArc( true ); 
    addArcAngles( a1, a2 );
  }

  // DXF.printString( pw, 0, "ELLIPSE" );
  // DXF.printString( pw, 8, layer );
  // DXF.printAcDb(pw, -1, DXF.AcDbEntity, DXF.AcDbEllipse );
  // pw.printf(Locale.US,
  //           "  10\n%.2f\n  20\n%.2f\n  30\n%.2f\n  11\n%.2f\n  21\n%.2f\n  31\n%.2f\n  40\n%.2f\n  41\n%.2f\n  42\n%.2f\n",
  //           (x0+x1)/2*dxfScale, -(y0+y1)/2*dxfScale, 0.0f,                 // CENTER
  //           x1*dxfScale, -(y0+y1)/2*dxfScale, 0.0f,                        // ENDPOINT OF MAJOR AXIS
  //           (y1-y0)/(x1-x0),                                              // RATIO MINOR/MAJOR
  //           x2*TDMath.DEG2RAD, (x2+y2)*TDMath.DEG2RAD );  // START and END PARAMS
  public void ellipse( String layer, float x, float y, float a, float b, float a1, float a2, int col )
  {
    // TODO
  }

  public int writeDxf( BufferedWriter out, int version, int handle, int ref ) throws IOException
  {
    int saved_value = -1;
    for ( DxfToken token : mDxfTokens ) {
      if ( token.version <= version ) {
        if ( token instanceof HandlePointerToken && saved_value > 0 ) ((HandlePointerToken)token).setValue( saved_value );
        handle = token.write( out, version, handle, ref );
        if ( token instanceof HandleToken && handle > 0 ) saved_value = handle;
      }
    }
    return handle;
  }

  // ---------------------------------------------------------
  /** tokens are pieces of the DXF file.
   * The tokens are added to a list and at the end the token write to the output in sequence
   */

  private final static int TOKEN_LINE     =  1;
  private final static int TOKEN_POLYLINE =  2;
  private final static int TOKEN_CIRCLE   =  3;
  private final static int TOKEN_ARC      =  4;
  private final static int TOKEN_ELLIPSE  =  5;
  private final static int TOKEN_VERTEX   =  6;
  private final static int TOKEN_SEQEND   =  7;
  private final static int TOKEN_ENTITY   =  8;
  // private final static int TOKEN_LAYER    =  9;
  private final static int TOKEN_COLOR    = 10;

  private final static int TOKEN_HANDLE   = 11;
  private final static int TOKEN_POINTER  = 12;
  private final static int TOKEN_REF      = 13;
  private final static int TOKEN_ACDB     = 14;
  private final static int TOKEN_FLAG     = 15;
  private final static int TOKEN_DATA     = 16;
  private final static int TOKEN_NPOINTS  = 17;
  private final static int TOKEN_POSITION = 18;

  /** base (abstract) token class
   */
  private abstract class DxfToken
  {
    int type = 0;
    int version;

    DxfToken( int t, int v )
    {
      type    = t;
      version = v;
    }

    abstract int write( BufferedWriter out, int version, int handle, int ref ) throws IOException;
  }

  /** generic (normal) token
   */
  private class NormalToken extends DxfToken
  {
    String string; // string 

    NormalToken( int t, int v, String s )
    {
      super( t, v );
      string = s;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_14 && type == TOKEN_VERTEX ) return handle;
      if ( string != null ) out.write( string );
      return handle;
    }
  }

  /** SEQEND token
   */
  private class SeqendToken extends NormalToken
  {
    SeqendToken( int v, String s )
    {
      super( TOKEN_SEQEND, v, s );
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_14 ) return handle; // skip
      if ( string != null ) out.write( string );
      // if ( version == DXF.ACAD_12 ) {
      //    handle = DXF.inc( handle );
      //    DXF.writeHex( out, 5, handle );
      // }
      return handle;
    }
  }

  /** FLAG token, for poly-lines
   */
  private class FlagToken extends DxfToken
  {
    int is3D;
    int isClosed;

    FlagToken( boolean _is3D, boolean _isClosed )
    {
      super( TOKEN_FLAG, DXF.ACAD_9 );
      is3D     = _is3D ? 8 : 0;
      isClosed = _isClosed? 1 : 0;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if  ( version == DXF.ACAD_14 ) {
        out.write( String.format(Locale.US, "  70%s%d%s", DXF.EOL, isClosed, DXF.EOL ) );
      } else {
        out.write( String.format(Locale.US, "  70%s%d%s", DXF.EOL, (is3D+isClosed), DXF.EOL ) );
      }
      return handle;
    }
  }

  /** NAME token, with LAYER
   */
  private class NameToken extends DxfToken
  {
    String name;
    String layer;

    /** cstr
     * @param v   ACAD version
     * @param n   name
     * @param l   layer
     */
    NameToken( int v, String n, String l )
    {
      super( TOKEN_ARC, v );
      name  = n;
      layer = l;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      out.write( String.format( "  0%s%s%s", DXF.EOL, name, DXF.EOL ) );
      if ( version == DXF.ACAD_12 || version == DXF.ACAD_14 ) {
        handle = DXF.inc(handle);
        out.write( String.format( "  5%s%X%s", DXF.EOL, handle, DXF.EOL ) );
        if ( ref >= 0 ) out.write( String.format( "  330%s%X%s", DXF.EOL, ref, DXF.EOL ) );
        out.write( String.format( "  100%sAcDbEntity%s", DXF.EOL, DXF.EOL ) );
      }
      if ( layer != null ) out.write( String.format( "  8%s%s%s", DXF.EOL, layer, DXF.EOL ) );
      return handle;
    }
  }

  /** POLYLINE token
   */
  private class PolylineToken extends DxfToken
  {
    /** cstr
     * @param v   ACAD version
     */
    PolylineToken( int v )
    {
      super( TOKEN_POLYLINE, v );
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_14 ) {
        out.write( String.format( "  0%sLWPOLYLINE%s", DXF.EOL, DXF.EOL ) );
      } else {
        out.write( String.format( "  0%sPOLYLINE%s", DXF.EOL, DXF.EOL ) );
      }
      return handle;
    }
  }

  /** POLYLINE token, with AcDb
   */
  private class PolylineAcDbToken extends DxfToken
  {
    /** cstr
     * @param v   ACAD version
     */
    PolylineAcDbToken( int v )
    {
      super( TOKEN_POLYLINE, v );
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_12 ) {
        out.write( String.format( "  100%s%s%s", DXF.EOL, DXF.AcDb3dPolyline, DXF.EOL ) );
      } else if ( version == DXF.ACAD_14 ) {
        out.write( String.format( "  100%s%s%s", DXF.EOL, DXF.AcDbEntity, DXF.EOL ) ); // ACAD_14 skips EntityToken
        out.write( String.format( "  100%s%s%s", DXF.EOL, DXF.AcDbPolyline, DXF.EOL ) );
      }
      return handle;
    }
  }
   
  /** DATA (X, Y) token
   */
  private class DataToken extends DxfToken
  {
    float x, y;

    /** cstr
     * @param v   ACAD version
     * @param xx  X value
     * @param yy  Y value
     */
    DataToken( int v, float xx, float yy )
    {
      super( TOKEN_DATA, v );
      x = xx;
      y = yy;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw  = new PrintWriter(sw);
      pw.printf(Locale.US, "  10%s%.2f%s  20%s%.2f%s", DXF.EOL, x, DXF.EOL, DXF.EOL, y, DXF.EOL );
      if ( version != DXF.ACAD_14 ) {
        pw.printf("  30%s0.00%s", DXF.EOL, DXF.EOL );
      }
      out.write( sw.getBuffer().toString() );
      return handle;
    }
  }
   
  /** number of points token
   */
  private class NPointsToken extends DxfToken
  {
    int npt;

    /** cstr
     * @param v   ACAD version
     * @param n   number of points
     */
    NPointsToken( int v, int n )
    {
      super( TOKEN_NPOINTS, v );
      npt = n;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_14 ) {
        // DXF.printInt( pw, 43, 0 ); 
        out.write( String.format(Locale.US, "  90%s%d%s", DXF.EOL, npt, DXF.EOL ) );
      }
      return handle;
    }
  }
   
  /** 3D point token
   */
  private class PositionToken extends DxfToken
  {
    float x, y, z;

    /** cstr
     * @param version   ACAD version
     * @param xx  X coord
     * @param yy  Y coord
     * @param zz  Z coord
     */
    PositionToken( int version, float xx, float yy, float zz )
    {
      super( TOKEN_POSITION, version );
      x = xx;
      y = yy;
      z = zz;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version != DXF.ACAD_14 ) {
        // DXF.printInt( pw, 43, 0 ); 
        out.write( String.format(Locale.US, "  10%s%.2f%s  20%s%.2f%s  30%s%.2f%s", DXF.EOL, x, DXF.EOL, DXF.EOL, y, DXF.EOL, DXF.EOL, z, DXF.EOL ) );
      }
      return handle;
    }
  }

  /** ENTITY token: it does not affect handle - used only for VERTEX
   */
  private class EntityToken extends DxfToken
  {
    String layer; 

    EntityToken( int version, String _layer )
    {
      super( TOKEN_ENTITY, version );
      layer = _layer;
    }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == DXF.ACAD_14 ) return handle;
      if ( version >= this.version ) {
        out.write( String.format("  100%s%s%s", DXF.EOL, DXF.AcDbEntity, DXF.EOL ) );
        if ( layer != null )  out.write( String.format("  8%s%s%s", DXF.EOL, layer, DXF.EOL ) );
      }
      return handle;
    }
  }

  /** HANDLE token
   */
  private class HandleToken extends DxfToken
  {
    HandleToken( int version ) { super( TOKEN_HANDLE, version ); }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version >= this.version && handle >= 0 ) {
        handle = DXF.inc( handle );
        // DXF.writeHex( out, 5, handle );
        StringWriter sw = new StringWriter();
        PrintWriter pw  = new PrintWriter(sw);
        pw.printf("  %d%s%X%s", 5, DXF.EOL, handle, DXF.EOL );
        out.write( sw.getBuffer().toString() );
      }
      return handle;
    }
  }

  /** HANDLE_POINTER token, used only for POLYLINE vertex and SEQEND
   */
  private class HandlePointerToken extends DxfToken
  {
    int value = -1;

    HandlePointerToken( int version ) { super( TOKEN_POINTER, version ); }

    void setValue( int v ) { value = v; }

    int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
    {
      if ( version == this.version && handle >= 0 ) {
        handle = DXF.inc( handle );
        // DXF.writeHex( out, 5, handle );
        StringWriter sw = new StringWriter();
        PrintWriter pw  = new PrintWriter(sw);
        pw.printf("  %d%s%X%s", 5, DXF.EOL, handle, DXF.EOL );
        if ( value > 0 ) pw.printf("  %d%s%X%s", 330, DXF.EOL, value, DXF.EOL );
        out.write( sw.getBuffer().toString() );
      }
      return handle;
    }
  }

  // NOTE this class is not used 
  // private class HandleRefToken extends DxfToken
  // {
  //   HandleRefToken( int version ) { super( TOKEN_REF, version ); }

  //   int write( BufferedWriter out, int version, int handle, int ref ) throws IOException
  //   {
  //     if ( version >= this.version && handle >= 0 ) {
  //       handle = DXF.inc( handle );
  //       // DXF.writeHex( out, 5, handle );
  //       StringWriter sw = new StringWriter();
  //       PrintWriter pw  = new PrintWriter(sw);
  //       pw.printf("  %d%s%X%s", 5, DXF.EOL, handle, DXF.EOL );
  //       if ( ref > 0 ) pw.printf("  %d%s%X%s", 330, DXF.EOL, ref, DXF.EOL );
  //       out.write( sw.getBuffer().toString() );
  //     }
  //     return handle;
  //   }
  // }

 
  // -------------------------------------------
  private ArrayList< DxfToken > mDxfTokens; // PRIVATE

  /** cstr
   */
  public SymbolPointDxf( )
  {
    mDxfTokens = new ArrayList< DxfToken >();
  }

  private void addToken( DxfToken token ) { mDxfTokens.add( token ); }
    
  private void startItem( int type, int version, String name, String layer )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw ); // DXF writer
    DXF.printString( pw, 0, name );
    if ( layer != null) DXF.printString( pw, 8, layer );
    addToken( new NormalToken( type, version, sw.toString() ) );
  }

  private void startLine( String layer )     { startItem( TOKEN_LINE,     DXF.ACAD_9, "LINE",     layer ); }
  private void startVertex( String layer )   { startItem( TOKEN_VERTEX,   DXF.ACAD_9, "VERTEX",   layer ); } // skip on ACAD_14

  private void startEllipse( String layer )  { addToken( new NameToken( DXF.ACAD_9, "ELLIPSE", layer ) ); }
  private void startCircle( String layer )   { addToken( new NameToken( DXF.ACAD_9, "CIRCLE", layer ) ); }
  private void startArc( String layer )      { addToken( new NameToken( DXF.ACAD_9, "ARC", layer ) ); }
    
  private void addAcDb( String acdb_item, boolean acdb_only )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw );
    if ( ! acdb_only ) pw.printf( DXF.EOL100 +  DXF.AcDbEntity + DXF.EOL );
    pw.printf( DXF.EOL100 + acdb_item + DXF.EOL );
    addToken( new NormalToken( TOKEN_ACDB, DXF.ACAD_12, sw.toString() ) );
  }

  private void addAcDbLine( boolean acdb_only )    { addAcDb( DXF.AcDbLine, acdb_only ); }
  private void addAcDbCircle( boolean acdb_only )  { addAcDb( DXF.AcDbCircle, acdb_only ); }
  private void addAcDbArc( boolean acdb_only )     { addAcDb( DXF.AcDbArc, acdb_only ); }
  private void addAcDbEllipse( boolean acdb_only ) { addAcDb( DXF.AcDbEllipse, acdb_only ); }

  private void startPolyline( ) { addToken( new PolylineToken( DXF.ACAD_9 ) ); }
  private void addAcDbPolyline() { addToken( new PolylineAcDbToken( DXF.ACAD_12 ) ); }

  /** add the header token of a vertex 
   * @param version ACAD version
   */
  private void addAcDbVertex( int version ) 
  { 
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw );
    // pw.printf( DXF.EOL100 + DXF.AcDbEntity + DXF.EOL );
    pw.printf( DXF.EOL100 + DXF.AcDbVertex + DXF.EOL );
    pw.printf( DXF.EOL100 + "AcDb3dPolylineVertex" + DXF.EOL );
    // pw.printf( "  70" + DXF.EOL + "32" + DXF.EOL );
    addToken( new NormalToken( TOKEN_VERTEX, version, sw.toString() ) );
  }

  // used only for VERTEX and SEQEND
  private void addAcDbEntity( int version, String layer ) { addToken( new EntityToken( version, layer ) ); }

  /** add a "layer" token
   * @param type    token type
   * @param version ACAD version
   * @param layer   layer name
   * @note used only for VERTEX and POLYLINE
   */
  private void addLayer( int type, int version, String layer ) 
  {
    if ( layer != null ) addToken( new NormalToken( type, version, String.format( "  8%s%s%s", DXF.EOL, layer, DXF.EOL ) ) );
  }

  /** add a "flag" token
   * @param version ACAD version
   * @param flag    flag
   * @note used only for VERTEX
   */
  private void addVertexFlag( int version, int flag ) 
  {
    addToken( new NormalToken( TOKEN_VERTEX, version, String.format(Locale.US, "  70%s%d%s", DXF.EOL, flag, DXF.EOL ) ) );
  }

  /** add a "color" token
   * @param color   color
   */
  private void addColor( String color )
  {
    addToken( new NormalToken( TOKEN_COLOR, DXF.ACAD_9, String.format("  6%s%s%s", DXF.EOL, color, DXF.EOL ) ) ); 
  }

  private void addHandle( int version )        { addToken( new HandleToken( version ) ); }
  private void addHandlePointer( int version ) { addToken( new HandlePointerToken( version ) ); }
  // private void addHandleRef( int version )     { addToken( new HandleRefToken( version ) ); }

  /** add a line segment to the DXF
   * @param x0   first point X coord
   * @param y0   first point Y coord
   * @param x1   second point X coord
   * @param y1   second point Y coord
   */
  private void addLine( float x0, float y0, float x1, float y1, int col )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw );
    DXF.printInt( pw, 62, col ); // DXF.BY_LAYER ); // color 0: by_block, 256: by_layer
    DXF.printXYZ( pw, x0, y0, 0.0f, 0 ); // prev point
    DXF.printXYZ( pw, x1, y1, 0.0f, 1 ); // current point
    addToken( new NormalToken( TOKEN_LINE, DXF.ACAD_9, sw.toString() ) );
  }

  private void addVertexData( float x0, float y0 ) { addToken( new DataToken( DXF.ACAD_9, x0, y0 ) ); }

  // private void headerPolyline( )
  // {
  //   StringWriter sw = new StringWriter();
  //   PrintWriter pw  = new PrintWriter( sw ); // DXF writer
  //   // DXF.printInt(  pw, 39, 1 ); // line thickness
  //   // DXF.printInt(  pw, 40, 1 ); // start width
  //   // DXF.printInt(  pw, 41, 1 ); // end width
  //   DXF.printInt( pw, 66, 1 ); // group 1
  //   DXF.printInt( pw, 62, DXF.BY_LAYER ); // color 0: by_block, 256: by_layer
  //   // DXF.printInt( pw, 75, 0 ); // 6 cubic spline, 5 quad spline, 0 is the default

  private void addPolylineColor( int color )
  {
    addToken( new NormalToken( TOKEN_POLYLINE, DXF.ACAD_9, String.format(Locale.US, "  62%s%d%s", DXF.EOL, color, DXF.EOL ) ) );
  }

  private void addPolylineGroup( int group )
  {
    addToken( new NormalToken( TOKEN_POLYLINE, DXF.ACAD_9, String.format(Locale.US, "  66%s%d%s", DXF.EOL, group, DXF.EOL ) ) );
  }

  private void addPolylinePosition( float x, float y, float z )
  {
    addToken( new PositionToken( DXF.ACAD_9, x, y, z ) );
  }

  private void addPolylineNPoints( int npt )
  {
    addToken( new NPointsToken( DXF.ACAD_14, npt ) );
  }

  private void addPolylineLineWidth( int width )
  {
    addToken( new NormalToken( TOKEN_POLYLINE, DXF.ACAD_14, String.format(Locale.US, "  43%s%d%s", DXF.EOL, width, DXF.EOL ) ) );
  }

  private void addPolylineClosed( boolean closed )
  {
    addToken( new FlagToken( true, closed ) );
  }

  // private void addVertex( String layer, float x, float y )
  // {
  //   StringWriter sw = new StringWriter();
  //   PrintWriter pw  = new PrintWriter( sw ); // DXF writer
  //   DXF.printString( pw, 0, "VERTEX" );
  //   DXF.printString( pw, 8, layer );
  //   DXF.printXYZ( pw, x, y, 0.0f, 0 );
  //   addToken( new NormalToken( TOKEN_VERTEX, DXF.ACAD_9, sw.toString() ) );
  // }

  /** close a sequence: add a SEQEND token
   * @note used only for VERTEX
   */
  private void closeSeq()
  {
    addToken( new SeqendToken( DXF.ACAD_9, String.format("  0%sSEQEND%s", DXF.EOL, DXF.EOL ) ) );
  }

  /** add a circle
   * @param x    X coord of the center
   * @param y    Y coord of the center
   * @param r    radius
   */
  private void addCircle( float x, float y, float r, int col )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw ); // DXF writer
    DXF.printInt( pw, 62, col ); // DXF.BY_LAYER ); // color 0: by_block, 256: by_layer
    DXF.printXYZ( pw, x, y, 0.0f, 0 );
    DXF.printFloat( pw, 40, r );
    addToken( new NormalToken( TOKEN_CIRCLE, DXF.ACAD_9, sw.toString() ) );
  }

  /** add angles of an arc (?)
   * @param a1 angle ...
   * @param a2 angle ...
   */
  private void addArcAngles( float a1, float a2 )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw ); // DXF writer
    DXF.printFloat( pw, 50, -a1-a2 );     // ANGLES DXF negative direction
    DXF.printFloat( pw, 51, -a1 );
    addToken( new NormalToken( TOKEN_ARC, DXF.ACAD_9, sw.toString() ) );
  }

  /** add an ellipse
   * @param x0 left endpoint X coord
   * @param y0 left endpoint Y coord
   * @param x1 right endpoint X coord
   * @param y1 right endpoint Y coord
   * @param r  aspect ratio
   * @param a1 angle ...
   * @param a2 angle ...
   */
  private void addEllipse( float x0, float y0, float x1, float y1, float r, float a1, float a2, int col ) 
  {
    float xc = (x0+x1)/2;
    float yc = (y0+y1)/2;
    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw ); // DXF writer
    DXF.printInt( pw, 62, col ); // DXF.BY_LAYER ); // color 0: by_block, 256: by_layer
    DXF.printXYZ( pw, xc, yc, 0.0f, 0 ); // CENTER
    DXF.printXYZ( pw, x0, yc, 0.0f, 1 ); // LEFT VERTEX
    DXF.printFloat( pw, 40, r  );        // ASPECT_RATIO
    DXF.printFloat( pw, 41, 180+a1 );    // ANGLES
    DXF.printFloat( pw, 42, 180+a1+a2 );
    // pw.printf(Locale.US,
    //           "  10\n%.2f\n  20\n%.2f\n  30\n%.2f\n  11\n%.2f\n  21\n%.2f\n  31\n%.2f\n  40\n%.2f\n  41\n%.2f\n  42\n%.2f\n",
    //           (x0+x1)/2*dxfScale, -(y0+y1)/2*dxfScale, 0.0f,                 // CENTER
    //           x1*dxfScale, -(y0+y1)/2*dxfScale, 0.0f,                        // ENDPOINT OF MAJOR AXIS
    //           (y1-y0)/(x1-x0),                                              // RATIO MINOR/MAJOR
    //           x2*TDMath.DEG2RAD, (x2+y2)*TDMath.DEG2RAD );  // START and END PARAMS
    addToken( new NormalToken( TOKEN_ELLIPSE, DXF.ACAD_9, sw.toString() ) );
  }

}

