/* @file DXF.java
 *
 * @author marco corvi
 * @date mar 2013
 *
 * @brief TopoDroid dxf const and utils
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.io.dxf;

import com.topodroid.utils.TDString;
// import com.topodroid.math.Point2D;
// import com.topodroid.math.BezierCurve;
// import com.topodroid.prefs.TDSetting;
// import com.topodroid.common.PlotType;

// import com.topodroid.utils.TDMath;
// import com.topodroid.utils.TDLog;
// import com.topodroid.utils.TDVersion;
// import com.topodroid.num.TDNum;

import java.util.Locale;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
// import java.io.FileInputStream;
// import java.io.BufferedInputStream;
// import java.io.DataInputStream;
// import java.io.FileNotFoundException;
import java.io.IOException;

// import android.graphics.RectF;

public class DXF
{
  final static int ACAD_9  =  9;
  final static int ACAD_12 = 13;
  final static int ACAD_14 = 16;

  static boolean mVersion9  = false;
  static boolean mVersion13 = false;
  static boolean mVersion14 = false;
  static boolean mVersion13_14 = false;

  final static int BY_BLOCK = 0;
  final static int BY_LAYER = 256;
  final static int LNK_color = 15; // HBX_DXF link layer fix color=brown 15

  public static int inc( int h ) { ++h; if ( h == 0x0105 ) ++h; return h; }

  // static final String ten = "10";
  static final String style_dejavu  = "DejaVu";
  static final String standard      = "Standard";
  static final String lt_continuous = "Continuous";
  static final String lt_byBlock    = "ByBlock";
  static final String lt_byLayer    = "ByLayer";
  static final String lt_center     = "Center";
  static final String lt_ticks      = "Ticks";
  
  static final String AcDbSymbolTR = "AcDbSymbolTableRecord";
  static final String AcDbEntity   = "AcDbEntity";
  static final String AcDbText     = "AcDbText";
  static final String AcDbLine     = "AcDbLine";
  static final String AcDbPolyline = "AcDbPolyline";
  static final String AcDb3dPolyline = "AcDb3dPolyline";
  static final String AcDb2dPolyline = "AcDb2dPolyline";
  static final String AcDbVertex   = "AcDbVertex";
  static final String AcDbCircle   = "AcDbCircle";
  static final String AcDbArc      = "AcDbArc";
  static final String AcDbHatch    = "AcDbHatch";
  static final String AcDbEllipse  = "AcDbEllipse";
  static final String AcDbDictionary = "AcDbDictionary";

  static final String EOL = "\r\n";
  static final String EOL100 = "  100\r\n";
  static final String EOLSPACE = "\r\n  ";
  static final String SPACE = "  ";
 
  static int p_style = 0;

  /** write a comment
   * @param out       output writer
   * @param comment   comment
   */
  static void writeComment( BufferedWriter out, String comment ) throws IOException
  {
    out.write( "  999" + EOL + comment + EOL );
  }

  /** write a handle in hex
   * @param out       output writer
   * @param code      ...
   * @param handle    handle
   */
  static void writeHex( BufferedWriter out, int code, int handle ) throws IOException 
  {
    if ( mVersion13_14 ) {
      StringWriter sw = new StringWriter();
      PrintWriter pw  = new PrintWriter(sw);
      pw.printf("  %d%s%X%s", code, EOL, handle, EOL );
      out.write( sw.getBuffer().toString() );
    }
  }

  /** write a handle in hex
   * @param pw        output printer
   * @param code      ...
   * @param handle    handle
   */
  static void printHex( PrintWriter pw, int code, int handle ) 
  {
    if ( mVersion13_14 ) {
      pw.printf("  %d%s%X%s", code, EOL, handle, EOL );
    }
  }

  /** write one AcDb
   * @param out       output writer
   * @param hex       ...
   * @param acdb1     AcDb string
   */
  static int writeAcDb( BufferedWriter out, int hex, String acdb1 ) throws IOException 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc(hex);
        writeHex( out, 5, hex ); 
      }
      out.write( EOL100 + acdb1 + EOL );
    }
    return hex;
  }

  /** write two AcDb
   * @param out       output writer
   * @param hex       ...
   * @param acdb1     first AcDb string
   * @param acdb2     second AcDb string
   */
  static int writeAcDb( BufferedWriter out, int hex, String acdb1, String acdb2 ) throws IOException 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        writeHex( out, 5, hex );
      }
      out.write( EOL100 + acdb1 + EOL+ EOL100 + acdb2 + EOL );
    }
    return hex;
  }

  /** write two AcDb
   * @param out       output writer
   * @param hex       ...
   * @param ref       ...
   * @param acdb1     first AcDb string
   * @param acdb2     second AcDb string
   */
  static int writeAcDb( BufferedWriter out, int hex, int ref, String acdb1, String acdb2 ) throws IOException 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        writeHex( out, 5, hex );
      }
      if ( ref >= 0 ) writeHex( out, 330, ref );
      out.write( EOL100 + acdb1 + EOL+ EOL100 + acdb2 + EOL );
    }
    return hex;
  }


  /** write one AcDb
   * @param pw        output printer
   * @param hex       ...
   * @param acdb1     AcDb string
   */
  static int printAcDb( PrintWriter pw, int hex, String acdb1 )
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      pw.printf( EOL100 + acdb1 + EOL );
    }
    return hex;
  }

  /** write one AcDb
   * @param pw        output printer
   * @param hex       ...
   * @param ref       ...
   * @param acdb1     AcDb string
   */
  static int printAcDb( PrintWriter pw, int hex, int ref, String acdb1 ) 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      if ( ref >= 0 ) printHex( pw, 330, ref );
      pw.printf( EOL100 + acdb1 + EOL );
    }
    return hex;
  }

  /** write AcDb ModelSpace
   * @param pw        output printer
   * @param hex       ...
   * @param ref       ...
   * @param layer     layer
   * @param acdb1     AcDb string
   */
  static int printAcDbModelSpace( PrintWriter pw, int hex, int ref, String layer, String acdb1 ) 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      if ( ref >= 0 ) printHex( pw, 330, ref );
      pw.printf( "  999\r\n*Model_Space-handle\r\n" );
      pw.printf( EOL100 + AcDbEntity + EOL );
    } 
    pw.printf("  8" + EOL + layer + EOL );
    if ( mVersion13_14 ) {
      pw.printf( EOL100 + acdb1 + EOL );
    }
    return hex;
  }

  /** write two AcDb
   * @param pw        output printer
   * @param hex       ...
   * @param acdb1     first AcDb string
   * @param acdb2     second AcDb string
   */
  static int printAcDb( PrintWriter pw, int hex, String acdb1, String acdb2 ) 
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      pw.printf( EOL100 + acdb1 + EOL + EOL100 + acdb2 + EOL );
    }
    return hex;
  }

  /** write two AcDb
   * @param pw        output printer
   * @param hex       ...
   * @param ref       ...
   * @param acdb1     first AcDb string
   * @param acdb2     second AcDb string
   */
  static int printAcDb( PrintWriter pw, int hex, int ref, String acdb1, String acdb2 )
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      if ( ref >= 0 ) printHex( pw, 330, ref );
      pw.printf( EOL100 + acdb1 + EOL + EOL100 + acdb2 + EOL );
    }
    return hex;
  }

  /** write three AcDb
   * @param pw        output printer
   * @param hex       ...
   * @param ref       ...
   * @param acdb1     first AcDb string
   * @param acdb2     second AcDb string
   * @param acdb3     third AcDb string
   */
  static int printAcDb( PrintWriter pw, int hex, int ref, String acdb1, String acdb2, String acdb3 )
  {
    if ( mVersion13_14 ) {
      if ( hex >= 0 ) {
        hex = inc( hex );
        printHex( pw, 5, hex );
      }
      if ( ref >= 0 ) printHex( pw, 330, ref );
      pw.printf( EOL100 + acdb1 + EOL + EOL100 + acdb2 + EOL + EOL100 + acdb3 + EOL );
    }
    return hex;
  }

  /** write a string
   * @param out      output writer
   * @param code     string code
   * @param text     string text
   */
  static void writeString(  BufferedWriter out, int code, String text ) throws IOException
  {
    out.write( "  " + code + EOL + text + EOL );
  }

  /** write an empty string
   * @param out      output writer
   * @param code     string code
   */
  static void writeStringEmpty(  BufferedWriter out, int code ) throws IOException
  {
    out.write( "  " + code + EOL + TDString.EMPTY + EOL );
  }

  /** write the string "1.0"
   * @param out      output writer
   * @param code     string code
   */
  static void writeStringOne(  BufferedWriter out, int code ) throws IOException
  {
    out.write( "  " + code + EOL + "1.0" + EOL );
  }

  /** write the string "0.0"
   * @param out      output writer
   * @param code     string code
   */
  static void writeStringZero(  BufferedWriter out, int code ) throws IOException
  {
    out.write( "  " + code + EOL + "0.0" + EOL );
  }

  /** write a string
   * @param pw       output printer
   * @param code     string code
   * @param text     string text
   */
  static void printString(  PrintWriter pw, int code, String text )
  {
    pw.printf("  %d%s%s%s", code, EOL, text, EOL );
  }

  /** write a float number
   * @param pw       output printer
   * @param code     string code
   * @param val      float value
   */
  static void printFloat(  PrintWriter pw, int code, float val )
  {
    pw.printf(Locale.US, "  %d%s%.2f%s", code, EOL, val, EOL );
  }

  /** write an integer number
   * @param out      output writer
   * @param code     string code
   * @param val      integer value
   */
  static void writeInt(  BufferedWriter out, int code, int val ) throws IOException
  {
    out.write( SPACE + code + EOL + val + EOL );
  }

  /** write an float number
   * @param out      output writer
   * @param code     string code
   * @param val      integer value
   */
  static void writeFloat(  BufferedWriter out, int code, float val ) throws IOException
  {
    out.write(  code + EOL + val + EOL );
  }

  /** write an integer number
   * @param pw       output printer
   * @param code     string code
   * @param val      integer value
   */
  static void printInt(  PrintWriter pw, int code, int val )
  {
    pw.printf( "  %d%s%d%s", code, EOL, val, EOL );
  }

  /** write a X-Y pair
   * @param out      output writer
   * @param x        X value
   * @param y        Y value
   * @param base     base code
   */
  static void writeXY( BufferedWriter out, int x, int y, int base ) throws IOException
  {
    int b10 = 10 + base;
    int b20 = 20 + base;
    out.write( SPACE + b10 + EOL + x + EOLSPACE + b20 + EOL + y + EOL );
  }

  /** write a XYZ triple
   * @param out      output writer
   * @param x        X value
   * @param y        Y value
   * @param z        Z value
   * @param base     base code
   */
  static void writeXYZ( BufferedWriter out, int x, int y, int z, int base ) throws IOException
  {
    int b10 = 10 + base;
    int b20 = 20 + base;
    int b30 = 30 + base;
    out.write( SPACE + b10 + EOL + x + EOLSPACE + b20 + EOL + y + EOLSPACE + b30 + EOL + z + EOL );
  }

  /** write a X-Y pair
   * @param pw       output printer
   * @param x        X value
   * @param y        Y value
   * @param base     base code
   */
  static void printXY( PrintWriter pw, float x, float y, int base )
  {
    pw.printf(Locale.US, "  %d%s%.2f%s  %d%s%.2f%s", base+10, EOL, x, EOL, base+20, EOL, y, EOL );
  }

  /** write a XYZ triple
   * @param pw       output printer
   * @param x        X value
   * @param y        Y value
   * @param z        Z value
   * @param base     base code
   */
  static void printXYZ( PrintWriter pw, float x, float y, float z, int base )
  {
    pw.printf(Locale.US, "  %d%s%.2f%s  %d%s%.2f%s  %d%s%.2f%s",
       base+10, EOL, x, EOL, base+20, EOL, y, EOL, base+30, EOL, z, EOL );
  }

  // static void printIntXYZ( PrintWriter pw, int x, int y, int z, int base )
  // {
  //   pw.printf(Locale.US, "  %d%s%d%s  %d%s%d%s  %d%s%d%s",
  //      base+10, EOL, x, EOL, base+20, EOL, y, EOL, base+30, EOL, z, EOL );
  // }

  /** print layer, linetype and color
   * @param layer    layer (cannot be null)
   * @param linetype linetype (can be null)
   * @param color    color index
   */
  private static void printLayerLinetypeColor( PrintWriter pw, String layer, String linetype, int color )
  {
    printString( pw, 8, layer );
    if ( linetype != null ) printString( pw, 6, linetype );
    printInt( pw, 62, color );
  }

  // -----------------------------------------

  /** write a SECTION start
   * @param out      output writer
   * @param name     section name
   */
  static void writeSection( BufferedWriter out, String name ) throws IOException
  {
    writeString(out, 0, "SECTION");
    writeString(out, 2, name );
  }

  /** write a SECTION end
   * @param out      output writer
   */
  static void writeEndSection( BufferedWriter out ) throws IOException
  {
    writeString(out, 0, "ENDSEC" );
  }

  /** write a TABLE begin
   * @param out      output writer
   * @param name     table name
   * @param handle   ACAD handle
   * @param num      ...
   */
  static int writeBeginTable(  BufferedWriter out, String name, int handle, int num ) throws IOException
  {
    writeString(out, 0, "TABLE" );
    writeString(out, 2, name );
    handle = writeAcDb( out, handle, "AcDbSymbolTable" );
    if ( num >= 0 ) writeInt(out, 70, num );
    return handle;
  }
  
  /** write a TABLE end
   * @param out      output writer
   */
  static void writeEndTable(  BufferedWriter out ) throws IOException
  {
    writeString( out, 0, "ENDTAB");
  }

  /** write a LAYER
   * @param pw2      output printer
   * @param handle   ACAD handle
   * @param name     layer name
   * @param flag     layer flags
   * @param color    layer color
   * @param linetype layer line type
   */
  static int printLayer( PrintWriter pw2, int handle, String name, int flag, int color, String linetype )
  {
    name = name.replace(":", "-");
    printString( pw2, 0, "LAYER" );
    handle = printAcDb( pw2, handle, AcDbSymbolTR, "AcDbLayerTableRecord");
    printString( pw2, 2, name );  // layer name
    printString( pw2, 6, linetype ); // linetype name
    printInt( pw2, 62, color );   // layer color
    printInt( pw2, 70, flag );    // layer flag
    // if ( mVersion13_14 ) {
    //   printInt( pw2, 330, 2 );       // soft-pointer id/handle to owner dictionary
    //   printInt( pw2, 370, -3 );      // line-weight enum value
    //   printString( pw2, 390, "F" );  // hard-pointer id/handle or plot style-name object
    //   // printInt( pw2, 347, 46 );
    //   // printInt( pw2, 348, 0 );
    // }
    return handle;
  }

  // static void printEndText( PrintWriter pw, String style )
  // {
  //   printString( pw, 7, style );
  //   printString( pw, 100, AcDbText );
  // }

  /** write a line point (VERTEX)
   * @param pw       output printer
   * @param scale    scale factor
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   * @param x        X coordinate
   * @param y        Y coordinate
   * @param z        Z coordinate
   * @param linetype line type
   * @param color    color
   * @param p3D      3d polyline
   */
  static int printLinePoint( PrintWriter pw, float scale, int handle, int ref, String layer, float x, float y, float z, String linetype, int color, boolean p3D )
  {
    if ( mVersion14 ) {
      printXY( pw, x * scale, -y * scale, 0 );
    } else {
      printString( pw, 0, "VERTEX" );
      printLayerLinetypeColor( pw, layer, linetype, color );
      // printString( pw, 8, layer );
      // printString( pw, 6, linetype ); // HBX_DXF
      // printInt( pw, 62, color ); // HBX_DXF
      if ( mVersion13 ) {
        //handle = printAcDb( pw, handle, ref, AcDbEntity, AcDbVertex, "AcDb3dPolylineVertex" );
        handle = printAcDb( pw, handle, ref, AcDbEntity, AcDbVertex, p3D?"AcDb3dPolylineVertex":"AcDb2dVertex" );
      }
      if (p3D) {
        printXYZ(pw, x * scale, -y * scale, z, 0);
        if ( mVersion13 ) printInt( pw, 70, 32 ); // flag 32 = 3D polyline vertex
      } else {
        printXY( pw, x * scale, -y * scale, 0 ); // HBX_DXF
      }
      //if ( mVersion13 ) { //if 3D
      //  printInt( pw, 70, 32 ); // flag 32 = 3D polyline vertex
      //}
    }
    return handle;
  }

  /** write a line segment
   * @param pw       output printer
   * @param scale    scale factor
   * @param handle   ACAD handle
   * @param layer    layer name
   * @param x1       X coordinate of the first point
   * @param y1       Y coordinate of the first point
   * @param x2       X coordinate of the second point
   * @param y2       Y coordinate of the second point
   * @param z        Z coordinate
   */
  static int printLine(PrintWriter pw, float scale, int handle, String layer, float x1, float y1, float x2, float y2, float z )
  {
    printString( pw, 0, "LINE" );
    if ( mVersion13_14 ) {
      handle = printAcDb( pw, handle, AcDbEntity, AcDbLine );
    }
    printString( pw, 8, layer );
    // printInt(  pw, 39, 0 );         // line thickness
    printXYZ( pw, x1*scale, y1*scale, z, 0 );
    printXYZ( pw, x2*scale, y2*scale, z, 1 );
    return handle;
  }

// HBX_DXF
  /** write a polyline header
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   * @param closed   whether the polyline is closed
   * @param npt      number of points
   */
  static int printPolylineHeader( PrintWriter pw, int handle, int ref, String layer, boolean closed, int npt )
  {
    String linetype = lt_byLayer;
    int color = BY_LAYER;
    return printPolylineHeader( pw, handle, ref, layer, closed, npt, linetype, color, 0, false );
  }

  /** write a polyline header
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   * @param closed   whether the polyline is closed
   * @param npt      number of points
   * @param linetype line type
   * @param color    color
   * @param z        DXF z height
   * @param p3D      3d polyline
   */
  static int printPolylineHeader( PrintWriter pw, int handle, int ref, String layer, boolean closed,
                                  int npt, String linetype, int color, float z, boolean p3D )
  {
    if ( mVersion14 ) {
      printString( pw, 0, "LWPOLYLINE" );
      handle = printAcDb( pw, handle, AcDbEntity, AcDbPolyline );
      printLayerLinetypeColor( pw, layer, linetype, color );
      // printString( pw, 8, layer );
      // printString( pw, 6, linetype ); // lt_byLayer );
      // printInt( pw, 62, color ); // HBX_DXF
      printInt( pw, 43, 0 ); // width 0: constant
      printFloat( pw, 38, z ); // elevation
      // printInt( pw, 62, BY_LAYER );
      printInt( pw, 90, npt );
      printInt( pw, 70, (closed? 1:0) + 128 ); // polyline flag 8 = 3D polyline, 1 = closed  // inlined close in 5.1.20 // HBX_DXF 128= linetype generated
    } else {
      printString( pw, 0, "POLYLINE" );
      if ( mVersion13 ) {
        handle = printAcDb( pw, handle, AcDbEntity );
        printLayerLinetypeColor( pw, layer, linetype, color );
        // printString( pw, 8, layer );
        // printString( pw, 6, linetype ); // HBX_DXF
        // printInt( pw, 62, color ); // HBX_DXF
        printInt( pw, 43, 0 ); // width 0: constant
        printString( pw, 100, ( p3D? AcDb3dPolyline : AcDb2dPolyline) ); // HBX_DXF
        // printFloat( pw, 38, z ); // elevation // HBX_DXF
        // printInt( pw, 62, BY_LAYER );
        printInt( pw, 70, (closed? 1:0) + 128 + (p3D? 8:0) ); // polyline flag 8 = 3D polyline, 1 = closed  // inlined close in 5.1.20 // HBX_DXF 128 linetype generated
        printInt( pw, 66, 1 ); // group 1
        printXYZ(pw,0f, 0f, z, 0);
      } else { // mVersion9
        printLayerLinetypeColor( pw, layer, linetype, color );
        // printString( pw, 8, layer );
        // printString( pw, 6, linetype ); // HBX_DXF
        // printInt( pw, 62, color ); // HBX_DXF
        // printInt(  pw, 39, 1 ); // line thickness
        // printInt(  pw, 40, 1 ); // start width
        // printInt(  pw, 41, 1 ); // end width
        printInt( pw, 66, 1 ); // group 1
        printXYZ(pw,0f, 0f, z, 0);
        printInt( pw, 70, (closed? 1:0) + 128 + (p3D? 8:0) ); // polyline flag 8 = 3D polyline, 1 = closed  // inlined close in 5.1.20 // HBX_DXF linetype gen enable
        // printInt( pw, 75, 0 ); // 6 cubic spline, 5 quad spline, 0 (optional, default 0) // commented in 5.1.20
      }
    }
    return handle;
  }

  /** write a polyline footer
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   */
  static int printPolylineFooter( PrintWriter pw, int handle, int ref, String layer )
  {
    if ( mVersion14 ) { //HBX_DXF 14 -> 13_14?
      // nothing 
    } else {
      pw.printf("  0%sSEQEND%s", EOL, EOL );
      if ( mVersion13 ) {
        handle = inc(handle);
        printHex( pw, 5, handle );
        printHex( pw, 330, ref );
        printString( pw, 100, AcDbEntity );
        printString( pw, 8, layer );
      }
    }
    return handle;
  }

  // static int printLWPolyline( PrintWriter pw, DrawingPointLinePath line, float scale, int handle, String layer, boolean closed,
  //                             float xoff, float yoff )
  // {
  //   int close = (closed ? 1 : 0 );
  //   printString( pw, 0, "LWPOLYLINE" );
  //   handle = printAcDb( pw, handle, AcDbEntity, AcDbPolyline );
  //   printString( pw, 8, layer );
  //   printInt( pw, 38, 0 ); // elevation
  //   printInt( pw, 39, 1 ); // thickness
  //   printInt( pw, 43, 1 ); // start width
  //   printInt( pw, 70, close ); // not closed
  //   printInt( pw, 90, line.size() ); // nr. of points
  //   for (LinePoint p = line.mFirst; p != null; p = p.mNext ) { 
  //     printXY( pw, (p.x+xoff) * scale, -(p.y+yoff) * scale, 0 );
  //   }
  //   return handle;
  // }

// HBX_DXF
  /** write a hatch header
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   * @param npt      number of points
   */
  static int printHatchHeader( PrintWriter pw, int handle, int ref, String layer, int npt )
  {
    String linetype = lt_byLayer;
    int color = BY_LAYER;
    return printHatchHeader( pw, handle, ref, layer, npt, linetype, color );
  }

  /** write a hatch header
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param layer    layer name
   * @param npt      number of points
   * @param linetype line type
   * @param color    color
   */
  static int printHatchHeader( PrintWriter pw, int handle, int ref, String layer, int npt, String linetype, int color )
  {
    if ( mVersion13_14 ) {
      printString( pw, 0, "HATCH" );    // entity type HATCH
      handle = printAcDb( pw, handle, AcDbEntity );
      // printString( pw5, 8, "AREA" );  // layer (color BYLAYER)
      printLayerLinetypeColor( pw, layer, linetype, color );
      // printString( pw, 8, layer );      // layer (color BYLAYER)
      // printString( pw, 6, linetype ); // lt_byLayer ); // line color BYLAYER
      // printInt( pw, 62, color ); // BY_LAYER );
      printAcDb( pw, -1, AcDbHatch );

      printXYZ( pw, 0f, 0f, 0f, 0 );
      printXYZ( pw, 0f, 0f, 1f, 200 );  // extrusion direction, default 0,0,1
      printString( pw, 2, "_USER" );    // hatch pattern name

      printInt( pw, 70, 1 );            // 1:solid fill, 0:pattern-fill
      printInt( pw, 71, 0 );            // 1:associative 0:non-associative
      printInt( pw, 91, 1 );            // nr. boundary paths (loops): 1
      // boundary data
        printInt( pw, 92, 7 );          // flag. 1:external 2:polyline 4:derived 8:text 16:outer
        printInt( pw, 72, 0 );          // not-polyline edge type (0: default) 1:line 2:arc 3:ellipse-area 4:spline
                                        // polyline: has-bulge
        printInt( pw, 73, 1 );          // is-closed flag
        printInt( pw, 93, npt );        // nr. of edges (only if not polyline) - nr. vertices (polyline) - maybe this is not necessary
    }
    return handle;
  }

  /** write a hatch footer
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   */
  static int printHatchFooter( PrintWriter pw, int handle, int ref )
  {
    if ( mVersion13_14 ) {
      // printXY( pw, area.mFirst.x * scale, -area.mFirst.y * scale, 0 );
        printInt( pw, 97, 0 );            // nr. source boundary objects
      printInt( pw, 75, 0 );            // hatch style: 0:normal, 1:outer, 2:ignore
      printInt( pw, 76, 1 );            // hatch pattern type: 0:user, 1:predefined, 2:custom

      // printFloat( pw, 52, 0f );         // hatch pattern angle (only pattern fill)
      // printFloat( pw, 41, 1f );         // hatch pattern scale (only pattern fill)
      // printInt( pw, 77, 0 );            // hatch pattern double flag, 0: not double, 1: double (pattern fill only)
      // printInt( pw, 78, 1 );            // nr. pattern definition lines
      /* here goes pattern data
        printFloat( pw, 53, 45f );        // pattern line angle
        printFloat( pw, 43, 0f );         // pattern base point
        printFloat( pw, 44, 0f );
        printFloat( pw, 45, -3.6f );      // pattern line offset
        printFloat( pw, 46, 3.6f );         
        printInt( pw, 79, 0 );            // nr. dash length items
      // // printFloat( pw, 49, 3f );         // dash length (repeated nr. times)
      */
       printFloat( pw, 47, 0.02f );         // pixel size
       printInt( pw, 98, 0 );            // nr. seed points
      // printXYZ( pw, 0f, 0f, 0f, 0 );

      // 450 451 452 453 460 461 462 and 470 all present or none
      // printInt( pw, 450, 0 ); // 0:solid, 1:gradient
      // printInt( pw, 451, 0 ); // reserved
      // printInt( pw, 452, 1 ); // 1:single color  2:two-color
      // printInt( pw, 453, 0 ); // 0:solid, 2:gradient
      // printFloat( pw, 460, 0f ); // rotation angle [rad]
      // printFloat( pw, 461, 0f ); // gradient definition
      // printFloat( pw, 462, 0.5f );  // color tint
      // printFloat( pw, 463, 0f ); // reserved
      // printString( pw, 470, "LINEAR" );  // default

      // printAcDb( pw, -1, AcDbHatch ); // not necessary
    }
    return handle;
  }


  /** write a text
   * @param pw       output printer
   * @param handle   ACAD handle
   * @param ref      ...
   * @param label    text
   * @param x        X coordinate
   * @param y        Y coordinate
   * @param scale    scale factor
   * @param layer    layer name
   * @param style    style
   * @param xoff     X offset
   * @param yoff     Y offset
   * @param z        Z level
   * @param s        flag, (unused)
   * @param color    color
   */
  static int printText( PrintWriter pw, int handle, int ref, String label, float x, float y, float angle, float scale,
                        String layer, String style, float xoff, float yoff, float z, int s, int color )
  {
    // if ( false && mVersion13_14 ) { // FIXME TEXT in AC1012
    //   // int idx = 1 + point.mPointType;
    //   printString( pw, 0, "INSERT" );
    //   handle = printAcDb( pw, handle, "AcDbBlockReference" );
    //   printString( pw, 8, "POINT" );
    //   printString( pw, 2, "P_label" ); // block_name );
    //   printFloat( pw, 41, POINT_SCALE );
    //   printFloat( pw, 42, POINT_SCALE );
    //   printFloat( pw, 50, 360-angle );
    //   printXYZ( pw, x, y, 0, 0 );
    // } else {
      printString( pw, 0, "TEXT" );
      // printString( pw, 2, block );
      handle = printAcDb( pw, handle, ref, AcDbEntity );
      // printLayerLinetypeColor( pw, layer, null, color ); // TODO
      printString( pw, 8, layer );
      // printString( pw, 6, linetype ); // lt_byLayer ); // line color BYLAYER
      printInt( pw, 62, color ); // BY_LAYER );
      printAcDb( pw, -1, AcDbText );
      
      // printString( pw, 7, style_dejavu ); // style (optional)
      // pw.printf("%s%s  0%s", "\"10\"", EOL, EOL );
      printXYZ( pw, x, y, z, 0 );
      // printXYZ( pw, 0, 0, 1, 1 );   // second alignment (optional)
      // printXYZ( pw, 0, 0, 1, 200 ); // extrusion (optional 0 0 1)
      // printFloat( pw, 39, 0 );      // thickness (optional 0) 
      printFloat( pw, 40, scale );     // height
      // printFloat( pw, 41, 1 );      // scale X (optional 1)
      printFloat( pw, 50, angle );     // rotation [deg]
      printFloat( pw, 51, 0 );         // oblique angle
      // printInt( pw, 71, 0 );        // text generation flag (optional 0)
      // printFloat( pw, 72, 0 );      // H-align (optional 0)
      // printFloat( pw, 73, 0 );      // V-align
      printString( pw, 1, label );    
      // printString( pw, 7, style );  // style, optional (default STANDARD)

      printAcDb( pw, -1, AcDbText );
    // }
    return handle;
  }

  /** AutoCAD default blocks
   * @param out    output writer
   * @param name   block record name
   * @param handle handle
   */
  static int writeSpaceBlockRecord( BufferedWriter out, String name, int handle ) throws IOException
  {
     writeString( out, 0, "BLOCK_RECORD" );
     handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbBlockTableRecord" );
     writeString( out, 2, name );
     writeInt( out, 70, 0 );
     writeInt( out, 280, 1 );
     writeInt( out, 281, 0 );
     if ( mVersion13_14 ) {
       writeInt( out, 330, 1 );
     }
     return handle;
  }

  /** AutoCAD default blocks
   * @param out    output writer
   * @param name   block name
   * @param handle handle
   */
  static int writeSpaceBlock( BufferedWriter out, String name, int handle ) throws IOException
  {
    writeString( out, 0, "BLOCK" );
    if ( mVersion13_14 ) {
      handle = writeAcDb( out, handle, AcDbEntity, "AcDbBlockBegin" );
      // writeInt( out, 330, handle );
    }
    writeString( out, 8, "0" );
    writeString( out, 2, name );
    writeInt( out, 70, 0 );       // flag 0=none, 1=anonymous, 2=non-const attr, 4=xref, 8=xref overlay,
    writeInt( out, 10, 0 ); 
    writeInt( out, 20, 0 ); 
    writeInt( out, 30, 0 ); 
    writeString( out, 3, name );
    writeString( out, 1, "" );
    writeString( out, 0, "ENDBLK" );
    if ( mVersion13_14 ) {
      handle = writeAcDb( out, handle, AcDbEntity, "AcDbBlockEnd");
      writeString( out, 8, "0");
    }
    return handle;
  }

  /** TODO Standard DXF header
   * @param out    output writer
   * @param handle handle
   * @param xmin   ...
   * @param ymin   ...
   * @param zmin   ...
   * @param xmax   ...
   * @param ymax   ...
   * @param zmax   ...
   */
  static int writeHeaderSection( BufferedWriter out, int handle,
             float xmin, float ymin, float zmin, float xmax, float ymax, float zmax ) throws IOException
  {
    writeSection( out, "HEADER" );
    // ACAD versions: 1006 (R10) 1009 (R11 R12) 1012 (R13) 1014 (R14)
    //                1015 (2000) 1018 (2004) 1021 (2007) 1024 (2010)  
    writeString( out, 9, "$ACADVER" );
    writeString( out, 1, ( mVersion14? "AC1014" : mVersion13? "AC1012" : "AC1009" ) );
    // writeString( out, 9, "$ACADMAINTVER" ); writeInt( out, 70, 105 ); // ignored
    if ( mVersion13_14 ) {
      writeString( out, 9, "$HANDSEED" );    writeHex( out, 5, 0xffff );
    //  writeString( out, 9, "$DWGCODEPAGE" ); writeString( out, 3, "ANSI_1251" ); //Cyrill
      writeString( out, 9, "$DWGCODEPAGE" ); writeString( out, 3, "ANSI_1250" ); //Central and Eastern European
    }
    // writeString( out, 9, "$REQUIREDVERSIONS" ); writeInt( out, 160, 0 );

    writeString( out, 9, "$INSBASE" );
    {
      StringWriter sw1 = new StringWriter();
      PrintWriter pw1  = new PrintWriter(sw1);
      printXYZ( pw1, 0.0f, 0.0f, 0.0f, 0 ); // FIXME (0,0,0)
      printString( pw1, 9, "$EXTMIN" ); printXYZ( pw1, xmin, ymin, zmin, 0 );
      printString( pw1, 9, "$EXTMAX" ); printXYZ( pw1, xmax, ymax, zmax, 0 );
      if ( mVersion13_14 ) {
        printString( pw1, 9, "$LIMMIN" ); printXY( pw1, 0.0f, 0.0f, 0 );
        printString( pw1, 9, "$LIMMAX" ); printXY( pw1, 420.0f, 297.0f, 0 );
      }
      out.write( sw1.getBuffer().toString() );
    }
    if ( mVersion13_14 ) {
      writeString( out, 9, "$DIMSCALE" );    writeString( out, 40, "1.0" ); // 
      writeString( out, 9, "$DIMTXT" );      writeString( out, 40, "2.5" ); // 
      writeString( out, 9, "$LTSCALE" );     writeFloat( out, 40, 0.2f ); //
      writeString( out, 9, "$LIMCHECK" );    writeInt( out, 70, 0 ); // 
      writeString( out, 9, "$ORTHOMODE" );   writeInt( out, 70, 0 ); // 
      writeString( out, 9, "$FILLMODE" );    writeInt( out, 70, 1 ); // 
      writeString( out, 9, "$QTEXTMODE" );   writeInt( out, 70, 0 ); // 
      writeString( out, 9, "$REGENMODE" );   writeInt( out, 70, 1 ); // 
      //writeString( out, 9, "$MIRRMODE" );    writeInt( out, 70, 0 ); // not handled by DraftSight, not handled by AutoCAD
      writeString( out, 9, "$UNITMODE" );    writeInt( out, 70, 0 ); // 

      writeString( out, 9, "$TEXTSIZE" );    writeInt( out, 40, 5 ); // default text size
      writeString( out, 9, "$TEXTSTYLE" );   writeString( out, 7, standard );
      writeString( out, 9, "$CELTYPE" );     writeString( out, 6, "BYLAYER" ); // 
      writeString( out, 9, "$CELTSCALE" );   writeInt( out, 40, 1 ); // 
      writeString( out, 9, "$CECOLOR" );     writeInt( out, 62, BY_LAYER ); // 

      writeString( out, 9, "$MEASUREMENT" ); writeInt( out, 70, 1 ); // drawing units 1=metric
      writeString( out, 9, "$INSUNITS" );    writeInt( out, 70, 4 ); // default drawing units 0=unit-less 4=mm
      writeString( out, 9, "$DIMASSOC" );    writeInt( out, 280, 0 ); // 0=no association
    }
    writeEndSection( out );
    return handle;
  }

  /** write the CLASSES section (empty section)
   * @param out    output writer
   * @param handle handle
   */
  static int writeClassesSection( BufferedWriter out, int handle ) throws IOException
  {
    if ( mVersion13_14 ) {
      writeSection( out, "CLASSES" );
      writeEndSection( out );
    }
    return handle;
  }

  /** write the VPORT table
   * @param out    output writer
   * @param handle handle
   * @param xmin   minimum X
   * @param ymin   ...
   * @param xmax   maximum X
   * @param ymax   ...
   */
  static int writeVportTable( BufferedWriter out, int handle,
             float xmin, float ymin, float xmax, float ymax ) throws IOException
  {
    if ( mVersion13_14 ) {
      handle = writeBeginTable( out, "VPORT", handle, 1 ); // 1 VPORT
      {
        writeString( out, 0, "VPORT" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbViewportTableRecord" );
        writeString( out, 2, "*Active" ); // name
        writeInt( out, 70, 0 );  // flags:
        writeXY( out, (int)xmin, (int)ymin, 0 ); // lower-left corner
        writeXY( out, (int)xmax, (int)ymax, 1 ); // upper-right corner
        writeXY( out, (int)(xmin+xmax)/2, (int)(ymin+ymax)/2, 2 ); // center point
        writeXY( out, 0, 0, 3 );   // snap base-point
        writeXY( out, 1, 1, 4 );   // snap-spacing
        writeXY( out, 1, 1, 5 );   // grid-spacing
        writeXYZ( out, 0, 0, 1, 6 ); // view direction
        writeXYZ( out, 0, 0, 0, 7 ); // view tangent
        
        writeInt( out, 40, 297 ); // Float.toString( (xmin+xmax)/2 ) );
        writeInt( out, 41, 2 );   // Float.toString( (ymin+ymax)/2 ) );
        writeInt( out, 42, 50 );  // lens length
        writeInt( out, 43, 0 );   // front clipping plane
        writeInt( out, 44, 0 );   // back clipping plane
        writeInt( out, 45, 0 );   // view height
        writeInt( out, 50, 0 );   // snap rotation angle
        writeInt( out, 51, 0 );   // view twist angle
        writeInt( out, 71, 0 );   // view mode:
        writeInt( out, 72, 100 ); // circle sides
        writeInt( out, 73, 1 );   
        writeInt( out, 74, 3 );   // UCSICON setting
        writeInt( out, 75, 0 );
        writeInt( out, 76, 0 );
        writeInt( out, 77, 0 );
        writeInt( out, 78, 0 );

        writeInt( out, 281, 0 );  // render mode: 0=2D optimized
        writeInt( out, 65, 1 );
        writeXYZ( out, 0, 0, 0, 100 );  // UCS origin
        writeXYZ( out, 1, 0, 0, 101 );  // UCS X-axis
        writeXYZ( out, 0, 1, 0, 102 );  // UCS Y-axis
        writeInt( out, 79, 0 );
        writeInt( out, 146, 0 );
        //writeString( out, 348, "2F" );
        //writeInt( out, 60, 3 );
        //writeInt( out, 61, 5 );
        //writeInt( out, 292, 1 );
        //writeInt( out, 282, 1 );
        //writeStringZero( out, 141 );
        //writeStringZero( out, 142 );
        //writeInt( out, 63, 250 );
        //writeString( out, 361, "6D" );
      }
      writeEndTable( out );
    }
    return handle;
  }

  /** write the STYLES section
   * @param out    output writer
   * @param handle handle
   */
  static int writeStylesTable( BufferedWriter out, int handle ) throws IOException
  {
    if ( mVersion13_14 ) {
      int nr_styles = 2;
      handle = writeBeginTable( out, "STYLE", handle, nr_styles );  // 2 styles
      {
        writeString( out, 0, "STYLE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbTextStyleTableRecord" );
        writeString( out, 2, standard );  // name
        writeInt( out, 70, 0 );           // flag (1: shape, 4:vert text, ... )
        writeStringZero( out, 40 );     // text-height: not fixed
        writeStringOne(  out, 41 );
        writeStringZero( out, 50 );
        writeInt( out, 71, 0 );
        writeString( out, 42, "2.5"  );
        writeString( out, 3, "txt" );  // fonts
        writeStringEmpty( out, 4 );

        writeString( out, 0, "STYLE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbTextStyleTableRecord" );
        p_style = handle;
        writeString( out, 2, style_dejavu );  // name
        writeInt( out, 70, 0 );               // flag
        writeStringZero( out, 40 );
        writeStringOne(  out, 41 );
        writeStringZero( out, 50 );
        writeInt( out, 71, 0 );
        writeString( out, 42, "2.5"  );
        writeString( out, 3, "Sans Serif.ttf" );  // fonts
        writeStringEmpty( out, 4 );
        writeString( out, 1001, "ACAD" );
        writeString( out, 1000, "DejaVu Sans" );
        writeInt( out, 1071, 0 );
      }
      writeEndTable( out );
    }
    return handle;
  }
/*
  /** write the TYPES table
   * @param out    output writer
   * @param handle handle
   */
  static int writeLTypesTable( BufferedWriter out, int handle ) throws IOException
  {
    if ( mVersion9 ) { handle = 5; } // necessary ???
    int l_type_nr    = mVersion13_14 ? 5 : 1; // linetype number
    handle = writeBeginTable( out, "LTYPE", handle, l_type_nr );
    int l_type_owner = handle;
    // FIXME this line might be a problem with AutoCAD
    // writeInt( out, 330, 0 ); // table has no owner
    {
      // int flag = 64;
      if ( mVersion13_14 ) {
        writeString( out, 0, "LTYPE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
        writeString( out, 2, lt_byBlock );
        writeInt( out, 330, l_type_owner );
        writeInt( out, 70, 0 );
        writeString( out, 3, "Std by block" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );

        writeString( out, 0, "LTYPE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
        writeString( out, 2, lt_byLayer );
        writeInt( out, 330, l_type_owner );
        writeInt( out, 70, 0 );
        writeString( out, 3, "Std by layer" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );

        writeString( out, 0, "LTYPE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
        writeString( out, 2, lt_continuous );
        writeInt( out, 330, l_type_owner );
        writeInt( out, 70, 0 );
        writeString( out, 3, "Solid line ------" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );

        if ( mVersion13 ) {
          writeString( out, 0, "LTYPE" );
          handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
          writeString( out, 2, lt_center );
          writeInt( out, 330, l_type_owner );
          writeInt( out, 70, 0 );
          writeString( out, 3, "Center ____ _ ____ _ ____ _ ____" ); // description
          writeInt( out, 72, 65 );
          writeInt( out, 73, 4 );         // number of elements
          writeString( out, 40, "2.0" );  // pattern length
          writeString( out, 49, "1.25" );  writeInt( out, 74, 0 ); // segment
          writeString( out, 49, "-0.25" ); writeInt( out, 74, 0 ); // gap
          writeString( out, 49, "0.25" );  writeInt( out, 74, 0 );
          writeString( out, 49, "-0.25" ); writeInt( out, 74, 0 );

          writeString( out, 0, "LTYPE" );
          handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
          writeString( out, 2, lt_ticks );
          writeInt( out, 330, l_type_owner );
          writeInt( out, 70, 0 );
          writeString( out, 3, "Ticks ____|____|____|____" ); // description
          writeInt( out, 72, 65 );
          writeInt( out, 73, 3 );        // number of elements
          writeStringOne( out, 40 ); // pattern length
          writeString( out, 49, "0.5" );  writeInt( out, 74, 0 ); // segment
          writeString( out, 49, "-0.2" ); writeInt( out, 74, 2 ); // embedded text
            writeInt( out, 75, 0 );   // SHAPE number must be 0
            writeInt( out, 340, p_style );  // STYLE pointer FIXME
            writeString( out, 46, "0.1" );  // scale
            writeStringZero( out, 50 );   // rotation
            writeString( out, 44, "-0.1" ); // X offset
            writeString( out, 45, "-0.1" ); // Y offset
            writeString( out, 9, "|" ); // text
          writeString( out, 49, "-0.25" ); writeInt( out, 74, 0 ); // gap
        }

        // writeString( out, 0, "LTYPE" );
        // handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
        // writeString( out, 2, lt_tick );
        // writeInt( out, 330, l_type_owner );
        // writeInt( out, 70, 0 );
        // writeString( out, 3, "Ticks ____|____|____|____" ); // description
        // writeInt( out, 72, 65 );
        // writeInt( out, 73, 4 );
        // writeString( out, 40, "1.45" ); // pattern length
        // writeString( out, 49, "0.25" ); writeInt( out, 74, 0 ); // segment
        // writeString( out, 49, "-0.1" ); writeInt( out, 74, 4 ); // embedded shape
        //   writeInt( out, 75, 1 );   // SHAPE number
        //   writeInt( out, 340, 1 );  // STYLE pointer
        //   writeString( out, 46, "0.1" );  // scale
        //   writeStringZero( out, 50 );   // rotation
        //   writeString( out, 44, "-0.1" ); // X offset
        //   writeStringZero( out, 45 );   // Y offset
        // writeString( out, 49, "-0.1" ); writeInt( out, 74, 0 );
        // writeString( out, 49, "1.0" );  writeInt( out, 74, 0 );

      } else { // dxf9
        writeString( out, 0, "LTYPE" );
        /* handle = */ writeAcDb( out, 14, AcDbSymbolTR, "AcDbLinetypeTableRecord" ); // unnecessary
        writeString( out, 2, lt_continuous );
        writeInt( out, 70, 64 );
        writeString( out, 3, "Solid line" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );
      }
    }
    writeEndTable( out );
    return handle;
  }

// HBX_DXF  header and standard line-types
  /** Separated linetype table header and content
   * @param out    output writer
   * @param handle handle
   * @param ltnr   linetype number (its value is not important)
   * @param p1_style  linetype character style "pointer" 
   */
  static int writeLTypesTableheader( BufferedWriter out, int handle, int ltnr, int p1_style ) throws IOException
  { 
    // if ( mVersion9 ) { handle = 5; } // necessary ???
    // int l_type_nr    = mVersion13_14 ? 1 : 1; // linetype number  // HBX_DXF !
    handle = writeBeginTable( out, "LTYPE", handle, ltnr+1 );
    int l_type_owner = handle;
    {
      if (mVersion14) { // R14
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_byBlock);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Std by block");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);

          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_byLayer);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Std by layer");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);

          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_continuous);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Solid line _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // AutoCAD standard linetype
        /*{
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_center);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Center ____ _ ____ _ ____ _ ____"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 4);         // number of elements
          writeString(out, 40, "2.0");  // pattern length
          writeString(out, 49, "1.25");//1
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.25");//2
          writeInt(out, 74, 0); // gap
          writeString(out, 49, "0.25");//3
          writeInt(out, 74, 0);
          writeString(out, 49, "-0.25");//4
          writeInt(out, 74, 0);
        }*/
        /*{
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_ticks);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Ticks ____|____|____|____"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 3);        // number of elements
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.5");
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.2");
          writeInt(out, 74, 2); // embedded text
          writeInt(out, 75, 0);   // SHAPE number must be 0
          writeInt(out, 340, p1_style);  // STYLE pointer
          writeString(out, 46, "0.1");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "-0.1"); // X offset
          writeString(out, 45, "-0.1"); // Y offset
          writeString(out, 9, "|"); // text
          writeString(out, 49, "-0.25");
          writeInt(out, 74, 0); // gap
        }*/
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_LINK");//b
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "LINK _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // link
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_CHIMNEY");//3
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "CHIMNEY _|_ _|_ _|_"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 3);      // number of elements
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.8"); //1
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.35"); // dash 2
          writeInt(out, 74, 2); // embedded text
          writeInt(out, 75, 0);   // SHAPE number must be 0
          writeInt(out, 340, p1_style);  // STYLE pointer
          writeString(out, 46, "0.4");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "-0.8"); // X offset
          writeString(out, 45, "-0.42"); // Y offset
          writeString(out, 9, "|"); // text
          writeString(out, 49, "-0.25"); // dash 3
          writeInt(out, 74, 0); // gap
        } // chimney 3
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_PIT");//4
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "PIT __|__|__|__"); // description
          writeInt(out, 72, 65);
          // writeInt(out, 73, 1);      // number of elements AutoCAD
          writeInt(out, 73, 2);      // number of elements dwg fast-view ?
          writeString(out, 40, "1.0"); // pattern length
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "0.5"); // dash 1
          writeInt(out, 74, 2); // embedded text
          writeInt(out, 75, 0);   // SHAPE number must be 0
          writeInt(out, 340, p1_style);  // STYLE pointer
          writeString(out, 46, "0.65");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "-1.0"); // X offset
          writeString(out, 45, "0.2"); // Y offset
          writeString(out, 9, "|"); // text
          writeString(out, 49, "0.5"); // dash 2 dwg fast-view ?
          writeInt(out, 74, 0); // gap
        } // pit 4
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_ARROW");//1
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "ARROW ->-->-->"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);      // number of elements
          writeString(out, 40, "1.5"); // pattern length
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "1.0"); // dash 1
          writeInt(out, 74, 2); // embedded text
          writeInt(out, 75, 0);   // SHAPE number must be 0
          writeInt(out, 340, p1_style);  // STYLE pointer
          writeString(out, 46, "0.65");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "0.0"); // X offset
          writeString(out, 45, "-0.31"); // Y offset
          writeString(out, 9, ">"); // text
          writeString(out, 49, "0.5"); // dash 2 dwg fast-view
          writeInt(out, 74, 0); // gap
        } // arrow 1
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_SLOPE");//7
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "SLOPE |i|i|i"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);         // number of elements
          writeString(out, 40, "1.0");    // pattern length
          writeInt(out, 74, 0);         // segment
          writeString(out, 49, "-0.5");   // space 1
          writeInt(out, 74, 2);        // embedded text
          writeInt(out, 75, 0);        // SHAPE number must be 0
          writeInt(out, 340, p1_style);    // STYLE pointer
          writeString(out, 46, "0.65");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "0.0");   // X offset
          writeString(out, 45, "-0.32"); // Y offset
          writeString(out, 9, "|");      // text
          writeString(out, 49, "-0.5");  // space 2
          writeInt(out, 74, 2);       // embedded text
          writeInt(out, 75, 0);       // SHAPE number must be 0
          writeInt(out, 340, p1_style);   // STYLE pointer
          writeString(out, 46, "0.35");  // scale
          writeString(out, 50, "0.0");   // rotation
          writeString(out, 44, "0.1");   // X offset
          writeString(out, 45, "-0.4");  // Y offset
          writeString(out, 9, "|");      // text
          writeInt(out, 74, 0);        // gap
        } // slope 7
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_USER");//a
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "USER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // user a
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL");//b
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // wall b
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_SECTION");//c
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Section _  _  _  _  _");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.3"); //1
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.7"); //2
          writeInt(out, 74, 0); // segment
        } // section c
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_BORDER");//2
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // border 2
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_ROCK-BORDER");//6
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "ROCK-BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // rock-border 6
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL-PRESUMED");//5
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL-PRESUMED __ __ __ __ __");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.7"); //1
          writeInt(out, 74, 0); // segment}
          writeString(out, 49, "-0.3"); //2
          writeInt(out, 74, 0); // segment
        } // wall-presumed 5
        /*{
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_ticks + "2");
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Ticks2 ____|____|____|____"); // description
          writeInt(out, 72, 65);
          writeInt(out, 73, 4);
          writeString(out, 40, "1.45"); // pattern length
          writeString(out, 49, "0.25");
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.1");
          writeInt(out, 74, 4); // embedded shape
          writeInt(out, 75, 1);   // SHAPE number
          writeInt(out, 340, p1_style);  // STYLE pointer
          writeString(out, 46, "0.1");  // scale
          writeStringZero(out, 50);   // rotation
          writeString(out, 44, "-0.1"); // X offset
          writeStringZero(out, 45);   // Y offset
          writeString(out, 49, "-0.1");
          writeInt(out, 74, 0);
          writeString(out, 49, "1.0");
          writeInt(out, 74, 0);
        }*/
      } else if (mVersion9) { // dxf9 (R12)
        {
          writeString(out, 0, "LTYPE");
          /* handle = */
          handle = writeAcDb(out, 14, AcDbSymbolTR, "AcDbLinetypeTableRecord"); // unnecessary
          writeString(out, 2, lt_continuous);
          writeInt(out, 70, 64);
          writeString(out, 3, "Solid line");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        }
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_LINK");//b
          writeInt(out, 70, 0);
          writeString(out, 3, "LINK _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // link
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_USER");//a
          writeInt(out, 70, 0);
          writeString(out, 3, "USER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // user a
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL");//b
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // wall b
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_SECTION");//c
          writeInt(out, 70, 0);
          writeString(out, 3, "Section _  _  _  _  _");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.3"); //1
          //if ( mVersion14 ) {writeInt(out, 74, 0);} // segment
          writeString(out, 49, "-0.7"); //2
          //if ( mVersion14 ) {writeInt(out, 74, 0);} // segment
          //writeStringZero(out, 40);
        } // section c
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_BORDER");//2
          writeInt(out, 70, 0);
          writeString(out, 3, "BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // border 2
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_ROCK-BORDER");//6
          writeInt(out, 70, 0);
          writeString(out, 3, "ROCK-BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // rock-border 6
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL-PRESUMED");//5
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL-PRESUMED __ __ __ __ ");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.7"); //1
          //if (false) {writeInt(out, 74, 0);} // segment}
          writeString(out, 49, "-0.3"); //2
          //if (false) {writeInt(out, 74, 0);} // segment
          //writeStringZero(out, 40);
        } // wall-presumed 5
      } else {// dxf12 (R13)
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_byBlock);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Std by block");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);

          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_byLayer);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Std by layer");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);

          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, lt_continuous);
          writeInt(out, 330, l_type_owner);
          writeInt(out, 70, 0);
          writeString(out, 3, "Solid line _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // AutoCAD standard linetype
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_LINK");//b
          writeInt(out, 70, 0);
          writeString(out, 3, "LINK _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // link
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_USER");//a
          writeInt(out, 70, 0);
          writeString(out, 3, "USER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // user a
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL");//b
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // wall b
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_SECTION");//c
          writeInt(out, 70, 0);
          writeString(out, 3, "Section _  _  _  _  _");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.3"); //1
          writeInt(out, 74, 0); // segment
          writeString(out, 49, "-0.7"); //2
          writeInt(out, 74, 0); // segment
          //writeStringZero(out, 40);
        } // section c
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_BORDER");//2
          writeInt(out, 70, 0);
          writeString(out, 3, "BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // border 2
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_ROCK-BORDER");//6
          writeInt(out, 70, 0);
          writeString(out, 3, "ROCK-BORDER _________");
          writeInt(out, 72, 65);
          writeInt(out, 73, 0);
          writeStringZero(out, 40);
        } // rock-border 6
        {
          writeString(out, 0, "LTYPE");
          handle = writeAcDb(out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord");
          writeString(out, 2, "L_WALL-PRESUMED");//5
          writeInt(out, 70, 0);
          writeString(out, 3, "WALL-PRESUMED __ __ __ __ ");
          writeInt(out, 72, 65);
          writeInt(out, 73, 2);
          writeString(out, 40, "1.0"); // pattern length
          writeString(out, 49, "0.7"); //1
          writeInt(out, 74, 0); // segment}
          writeString(out, 49, "-0.3"); //2
          writeInt(out, 74, 0); // segment
          //writeStringZero(out, 40);
        } // wall-presumed 5
      }
    }
    //writeEndTable( out );
    return handle;
  }

  /** write a LTYPE
   * @param out      output printer
   * @param handle   ACAD handle
   * @param linetype layer line type
   */
  static int printLtype( BufferedWriter out, int handle, String linetype ) throws IOException
  {
    int l_type_owner = handle;
    linetype = linetype.replace(":", "-");
      if ( mVersion13_14 ) {
        writeString( out, 0, "LTYPE" );
        handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbLinetypeTableRecord" );
        writeString( out, 2, linetype );
        writeInt( out, 330, l_type_owner );
        writeInt( out, 70, 0 );
        writeString( out, 3, linetype+" ------" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );
      } else { // dxf9
        writeString( out, 0, "LTYPE" );
        // /* handle = */ writeAcDb( out, 14, AcDbSymbolTR, "AcDbLinetypeTableRecord" ); // unnecessary
        writeString( out, 2, linetype );
        writeInt( out, 70, 0 );
        writeString( out, 3, linetype+" ------" );
        writeInt( out, 72, 65 );
        writeInt( out, 73, 0 );
        writeStringZero( out, 40 );
      }
/*
    name = name.replace(":", "-");
    printString( pw2, 0, "LAYER" );
    handle = printAcDb( pw2, handle, AcDbSymbolTR, "AcDbLayerTableRecord");
    printString( pw2, 2, name );  // layer name
    printInt( pw2, 70, flag );    // layer flag
    printInt( pw2, 62, color );   // layer color
    printString( pw2, 6, linetype ); // linetype name

 */
    // if ( mVersion13_14 ) {
    //   printInt( pw2, 330, 2 );       // soft-pointer id/handle to owner dictionary
    //   printInt( pw2, 370, -3 );      // line-weight enum value
    //   printString( pw2, 390, "F" );  // hard-pointer id/handle or plot style-name object
    //   // printInt( pw2, 347, 46 );
    //   // printInt( pw2, 348, 0 );
    // }
    return handle;
  }
// END HBX_DXF

  /** write the additional tables (...)
   * @param out    output writer
   * @param handle handle
   */
  static int writeExtraTables( BufferedWriter out, int handle ) throws IOException
  {
    handle = writeBeginTable( out, "VIEW", handle, 0 ); // no VIEW
    writeEndTable( out );

    handle = writeBeginTable( out, "UCS", handle, 0 ); // no UCS
    writeEndTable( out );
    
    handle = writeBeginTable( out, "APPID", handle, 1 );
    {
      writeString( out, 0, "APPID" );
      if ( mVersion9 ) { handle = 11; } // incremented before writing
      handle = writeAcDb( out, handle, AcDbSymbolTR, "AcDbRegAppTableRecord" );
      writeString( out, 2, "ACAD" ); // application name
      writeInt( out, 70, 0 );        // flag
    }
    writeEndTable( out );
    return handle;
  }

  /** write the DIMSTYLE table
   * @param out    output writer
   * @param handle handle
   */
  static int writeDimstyleTable( BufferedWriter out, int handle ) throws IOException
  {
    if ( mVersion13_14 ) {
      handle = writeBeginTable( out, "DIMSTYLE", handle, 1 );
      // writeString( out, 100, "AcDbDimStyleTable" );
      // writeInt( out, 71, 0 ); // DIMTOL
      {
        writeString( out, 0, "DIMSTYLE" );
        handle = inc(handle);
        writeHex( out, 105, handle ); 
        writeAcDb( out, -1, AcDbSymbolTR, "AcDbDimStyleTableRecord" ); // do i need handle ?
        writeString( out, 2, standard );
        writeStringEmpty( out, 3 );
        writeStringEmpty( out, 4 );
        writeStringEmpty( out, 5 );
        writeStringEmpty( out, 6 );
        writeStringEmpty( out, 7 );
        writeStringOne( out, 40 );
        writeString( out, 41, "2.5" );
        writeString( out, 42, "0.625" );
        writeString( out, 43, "3.75" );
        writeString( out, 44, "1.25" );
        writeStringZero( out, 45 );
        writeStringZero( out, 46 );
        writeStringZero( out, 47 );
        writeStringZero( out, 48 );
        writeInt( out, 70, 0 );
        writeInt( out, 71, 0 );
        writeInt( out, 72, 0 );

        writeInt( out, 73, 0 );
        writeInt( out, 74, 0 );
        writeInt( out, 75, 0 );
        writeInt( out, 76, 0 );
        writeInt( out, 77, 1 );
        writeInt( out, 78, 8 );
        writeString( out, 140, "2.5" );
        writeString( out, 141, "2.5" );
        writeStringZero( out, 142 );
        writeString( out, 143, "0.04" );
        writeStringOne( out, 144 );
        writeStringZero( out, 145 );
        writeStringOne( out, 146 );
        writeString( out, 147, "0.625" );
        writeInt( out, 170, 0 );
        writeInt( out, 171, 3 );
        writeInt( out, 172, 1 );
        writeInt( out, 173, 0 );
        writeInt( out, 174, 0 );
        writeInt( out, 175, 0 );
        writeInt( out, 176, 0 );
        writeInt( out, 177, 0 );
        writeInt( out, 178, 0 );
        writeInt( out, 271, 2 );
        writeInt( out, 272, 2 );
        writeInt( out, 274, 3 );
        writeInt( out, 278, 44 );
        writeInt( out, 283, 0 );
        writeInt( out, 284, 8 );
        writeInt( out, 340, 0x11 );
      }
      writeEndTable( out );
    }
    return handle;
  }


// SECTION OBJECTS
  /** minimal object section
   * @param out    output writer
   * @param handle    handle
   */
  static int writeSectionObjects( BufferedWriter out, int handle ) throws IOException
  {
    writeSection( out, "OBJECTS" );

    StringWriter swx = new StringWriter();
    PrintWriter pwx  = new PrintWriter(swx);

    printString( pwx, 0, "DICTIONARY" );
    handle = printAcDb( pwx, handle, AcDbDictionary );
    int saved = handle;
    // printInt( pwx, 280, 0 );
    printInt( pwx, 281, 1 );
    printString( pwx, 3, "ACAD_GROUP" );
    if ( mVersion13_14 ) {
      handle = inc(handle);
      printHex( pwx, 350, handle );
    }

    printString( pwx, 0, "DICTIONARY" );
    // handle = printAcDb( pwx, handle, AcDbDictionary );
    if ( mVersion13_14 ) {
      printHex( pwx, 5, handle );
      if ( mVersion13_14 ) {
        printHex( pwx, 330, saved );
      }
      pwx.printf( EOL100 + AcDbDictionary + EOL );
    }

    // printInt( pwx, 280, 0 );
    printInt( pwx, 281, 1 );

    out.write( swx.getBuffer().toString() );
    out.flush();

    writeEndSection( out );
    return handle;
  }

}

