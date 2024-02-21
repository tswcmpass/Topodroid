/** @file ExportLAS.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Point LAS exporter
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.c3out;


import com.topodroid.TDX.TglParser;

// import com.topodroid.utils.TDLog;

// import java.util.Locale;
// import java.util.List;
// import java.util.ArrayList;
import java.util.Calendar;

// import java.io.FileWriter;
// import java.io.PrintWriter;
// import java.io.PrintStream;
import java.io.DataOutputStream;
// import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;

public class ExportLAS
{
  final static double FACTOR = 100;
  final static double SCALE = 1/FACTOR;

  final static int HDR_SIZE  = 227; // 379;     // bytes
  final static int PTS_OFFS  = 229; // bytes
  final static int FMT0_SIZE =  20; // bytes
  final static int FMT1_SIZE =  28; // bytes
  final static int FMT6_SIZE =  30; // bytes
  final static int VLR_SIZE  =  54;  // VLR header size
  
  public static boolean exportBinary( DataOutputStream dos, TglParser data, boolean do_splays, boolean do_walls, boolean do_station )
  { 
    if ( data == null ) return false;

    double[] v = data.getSplaysEndpoints();
    int nr_pts = data.getSplayNumber();
    // Log.v( "Cave3D-LAS", "Number of points " + nr_pts );
    int fmt = FMT1_SIZE;
    int nr_vlr = 0;

    double min_x, min_y, min_z;
    double max_x, max_y, max_z;
    if ( nr_pts == 0 ) return false;
    min_x = max_x = v[0];
    min_y = max_y = v[1];
    min_z = max_z = v[2];
    for ( int k=1; k<nr_pts; ++k ) {
      int k3 = k*3;
      if ( v[k3] < min_x ) { min_x = v[k3]; } else if ( v[k3] > max_x ) { max_x = v[k3]; }
      ++k3;
      if ( v[k3] < min_y ) { min_y = v[k3]; } else if ( v[k3] > max_y ) { max_y = v[k3]; }
      ++k3;
      if ( v[k3] < min_z ) { min_z = v[k3]; } else if ( v[k3] > max_z ) { max_z = v[k3]; }
    }
    min_x -= 1;
    min_y -= 1;
    min_z -= 1;
    max_x += 1 - min_x;
    max_y += 1 - min_y;
    max_z += 1 - min_z;
    double int_a = (90)/max_x;
    double int_z = (1<<15)/max_z;
    try {
      byte[] header = makeHeader( nr_pts, nr_vlr, fmt, min_x, max_x, min_y, max_y, min_z, max_z );
      dos.write( header );
      dos.write( (byte)0xdd );
      dos.write( (byte)0xcc );

      if ( nr_vlr > 0 ) { // always false
        byte[] record = new byte[1];
        for ( int k = 0; k < nr_vlr; ++ k ) {
          byte[] vlr = makeVLR("LAS_projection", 34735, record );
          dos.write( vlr );
	}
      }

      for ( int k3 = 0; k3 < 3*nr_pts; k3 += 3 ) {
	double z = v[k3+2] - min_z;
	double x = v[k3]   - min_x;
        byte[] point = getPointFormat1( x, v[k3+1]-min_y, z, (short)(z*int_z), (byte)(x*int_a) );
        dos.write( point );
      }
      dos.flush();
      dos.close();
    } catch ( IOException e2 ) {
      // TODO
      return false;
    }
    return true;
  }


  static private void putShort( ByteBuffer bb, short s )
  {
    bb.put( (byte)(s & 0xff) );
    bb.put( (byte)((s>>8) & 0xff) );
  }

  static private void putInt( ByteBuffer bb, int s )
  {
    bb.put( (byte)(s & 0xff) );
    bb.put( (byte)((s>>8) & 0xff) );
    bb.put( (byte)((s>>16) & 0xff) );
    bb.put( (byte)((s>>24) & 0xff) );
  }

  static private void putLong( ByteBuffer bb, long s )
  {
    byte[] b = new byte[8];
    ByteBuffer.wrap(b).putLong(s);
    for ( int k=7; k>=0; --k ) bb.put( b[k] );
  }

  static private void putDouble( ByteBuffer bb, double d )
  {
    byte[] b = new byte[8];
    ByteBuffer.wrap(b).putDouble(d);
    for ( int k=7; k>=0; --k ) bb.put( b[k] );
  }

  static private byte[] getPointFormat0( double x, double y, double z, short intensity, byte angle )
  {
    ByteBuffer ret = ByteBuffer.allocate( FMT0_SIZE );

    putInt( ret, (int)(FACTOR * x) );
    putInt( ret, (int)(FACTOR * y) );
    putInt( ret, (int)(FACTOR * z) );
    putShort( ret, (short)intensity );
    ret.put( (byte)0x24 );    // flag bits      00100100
                              //                      ^ scan dir. flag: 0 neg. 1 pos.
    ret.put( (byte)0 );       // classification 00000000
    ret.put( (byte)angle );       // scan angle: from -90 to +90
    ret.put( (byte)0 );       // user data / file marker
    putShort( ret, (short)0 ); // source ID / user bit field
    return ret.array();
  }

  static private byte[] getPointFormat1( double x, double y, double z, short intensity, byte angle )
  {
    ByteBuffer ret = ByteBuffer.allocate( FMT1_SIZE );
    long now = System.currentTimeMillis();

    putInt( ret, (int)(FACTOR * x) );
    putInt( ret, (int)(FACTOR * y) );
    putInt( ret, (int)(FACTOR * z) );
    putShort( ret, (short)intensity );
    ret.put( (byte)0x09 );    // flag bits      00100100
                              //                      ^ scan dir. flag: 0 neg. 1 pos.
    ret.put( (byte)0x01 );    // classification 00000000
    ret.put( (byte)angle    );    // scan angle: from -90 to +90
    ret.put( (byte)0    );    // user data / file marker
    putShort( ret, (short)0x01 ); // source ID / user bit field
    putLong( ret, (long)now ); // GPS time
    return ret.array();
  }

  static private byte[] getPointFormat6( double x, double y, double z, short intensity, short angle )
  {
    ByteBuffer ret = ByteBuffer.allocate( FMT6_SIZE );
    long now = System.currentTimeMillis();

    putInt( ret, (int)(FACTOR * x) );
    putInt( ret, (int)(FACTOR * y) );
    putInt( ret, (int)(FACTOR * z) );
    putShort( ret, (short)intensity );
    ret.put( (byte)0x24 );    // flag bits      00100100
    ret.put( (byte)0 );       // classification 00000000
    ret.put( (byte)0 );       // user data
    putShort( ret, (short)angle ); // scan angle
    putShort( ret, (short)0 ); // source ID
    putLong( ret, (long)now ); // GPS time
    return ret.array();
  }

  static private byte[] makeVLR( String userId, int recordId, byte[] record )
  {
    int len  = record.length;
    int size = VLR_SIZE + len;
    ByteBuffer ret = ByteBuffer.allocate( size );
    
    int k;
    int k_len = userId.length();
    putShort( ret, (short)0xaabb ); // reserved (directory version)
    for ( k=0; k<k_len; ++k ) ret.put( (byte)userId.charAt(k) );
    for ( ; k<16; ++k ) ret.put( (byte)0 );
    putShort( ret, (short)recordId ); // record id
    putShort( ret, (short)len );        // record length after header
    for ( k=0; k<32; ++k ) ret.put( (byte)0 ); // description
    return ret.array();
  }

  static private byte[] makeHeader( int nr_pts, int nr_vlr, int fmt,
		                 double min_x, double max_x,
                                 double min_y, double max_y,
                                 double min_z, double max_z )
  {
    ByteBuffer ret = ByteBuffer.allocate( HDR_SIZE );

    // short gen_coding = 0; (unused)
    // bit-0 GPS time (0 = GPS week time)
    // bit-1 waveform data packet (deprecated, use 0)
    // bit-2 waveform data packet ext. (if set external, use 0)
    // bit-3 return nr synth. generated (use 0)
    // bit-4 WTK (coord ref system) (use 0)
    // bit-5-15 reserved, must be 0
               // 1        0         0         0  "
    String sys = "OTHER";
    String sw  = "TopoDroid";

    Calendar calendar = Calendar.getInstance();
    short day  = (short)calendar.get(Calendar.DAY_OF_YEAR);
    short year = (short)calendar.get(Calendar.YEAR);
    // int[] rets = new int[5]; // UNUSED
    // rets[0] = rets[1] = rets[2] = rets[3] = rets[4] = 0;
    // long[] rets2 = new long[15];
    // for ( int k=0; k<15; ++k ) rets2[k] = 0;

    short len = (short)fmt;
    // long elen = HDR_SIZE + len * nr_pts;
    int pt_offset = PTS_OFFS;
    int k=0;

    ret.put( (byte)0x4c );
    ret.put( (byte)0x41 );
    ret.put( (byte)0x53 );
    ret.put( (byte)0x46 );
    putShort( ret, (short)0 ); // source ID
    putShort( ret, (short)0 ); // global encoding (8)
    putInt(   ret, (int)0 );   // ID-GUID
    putShort( ret, (short)0 );
    putShort( ret, (short)0 );
    putLong(  ret, (long)0 );  // (24)
    // Log.v( "Cave3D-LAS", "position major/minor " + ret.position() );
    ret.put( (byte)1 );  // major 1
    ret.put( (byte)2 );  // minor 2 (26)
    for ( k=0; k<5; ++k ) ret.put( (byte)sys.charAt(k) ); 
    for (   ; k<32; ++k ) ret.put( (byte)0 );
    for ( k=0; k<9; ++k ) ret.put( (byte)sw.charAt(k) );  
    for (   ; k<32; ++k ) ret.put( (byte)0 ); // (90)
    putShort( ret, (short)day );
    putShort( ret, (short)year );     // (94)
    putShort( ret, (short)HDR_SIZE ); // header size
    putInt(   ret, (int)pt_offset );    // pt data offset (100)
    putInt(   ret, (int)nr_vlr );       // nr VLR 
    ret.put( (byte)1 );              // pt record format
    putShort( ret, (short)len );      // pt record length (107)
    putInt(   ret, (int)nr_pts );       // legacy nr pt records (115)

    putInt(   ret, (int)nr_pts );       // nr pt records by return 
    for ( k=1; k < 5; ++k ) ret.putInt( (int)0 );        // 5 ints (135)
    // Log.v( "Cave3D-LAS", "position scale " + ret.position() );
    putDouble( ret, SCALE );  // X scale - cast to double not needed
    putDouble( ret, SCALE );  // Y scale
    putDouble( ret, SCALE );  // Z scale
    putDouble( ret, 0 );      // X offset
    putDouble( ret, 0 );      // Y offset
    putDouble( ret, 0 );      // Z offset (183)
    // Log.v( "Cave3D-LAS", "position max/min " + ret.position() );
    putDouble( ret, max_x ); // cast to double not needed
    putDouble( ret, min_x );
    putDouble( ret, max_y );
    putDouble( ret, min_y );
    putDouble( ret, max_z );
    putDouble( ret, min_z ); // (231)

    // putLong( ret, (long)0 );        // start of waveform packet record {v. 1.3}
    // putLong( ret, (long)elen );     // start of EVRL (247) {v. 1.4}
    // putInt( ret, (int)0 );          // nr EVRL (251)
    // putLong( ret, (long)nr_pts );   // nr pt records (259)
    // // Log.v( "Cave3D-LAS", "position " + ret.position() );
    // // ret.put( rets2 );        // (379)
    // for ( k=0; k<15; ++k ) putLong( ret, (long)0 );

    return ret.array();
  }
}
