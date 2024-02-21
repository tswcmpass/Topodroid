/* @file WorldMagneticModel.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid World Magnetic Model 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * Implemented after GeomagneticLibrary.c by
 *  National Geophysical Data Center
 *  NOAA EGC/2
 *  325 Broadway
 *  Boulder, CO 80303 USA
 *  Attn: Susan McLean
 *  Phone:  (303) 497-6478
 *  Email:  Susan.McLean@noaa.gov
 */
package com.topodroid.mag;

import com.topodroid.utils.TDLog;

import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import android.content.Context;

public class WorldMagneticModel
{
  // private int nMax;
  // private int numTerms;
  private MagModel mModel;
  private static MagDate  mStartEpoch = null;
  private static float[]  mGeoidHeightBuffer = null;
  private static WMMcoeff[] mWmmCoeff = null;
  private MagEllipsoid mEllipsoid;
  private MagGeoid     mGeoid;

  public WorldMagneticModel( Context context )
  {
    // TDLog.v( "WMM cstr" );
    int n_max = 12;
    int n_terms = MagUtil.CALCULATE_NUMTERMS( n_max );
    loadWMM( context, n_terms );
    loadEGM9615( context );

    mModel = new MagModel( n_terms, n_max, n_max );
    mModel.setEpoch( mStartEpoch );
    // mModel.epoch = 2020.0;
    // mModel.CoefficientFileEndDate = mModel.epoch + 5;
    mModel.setCoeffs( mWmmCoeff );
    mEllipsoid = new MagEllipsoid(); // default values
    mGeoid = new MagGeoid( mGeoidHeightBuffer );
  }

  public MagElement computeMagElement( double latitude, double longitude, double height, int year, int month, int day )
  {
    // TDLog.v( "Mag date " + year + " " + month + " " + day );
    MagDate date = new MagDate( year, month, day );
    return doComputeMagElement( latitude, longitude, height, date );
  }

  public MagElement computeMagElement( double latitude, double longitude, double height, double dec_year )
  {
    // TDLog.v( "Mag date " + dec_year );
    MagDate date = new MagDate( dec_year );
    return doComputeMagElement( latitude, longitude, height, date );
  }

  // height [M]
  public double geoidToEllipsoid( double latitude, double longitude, double height )
  {
    MagGeodetic geodetic = new MagGeodetic();
    geodetic.phi    = latitude;  // dec degree
    geodetic.lambda = longitude; // dec degree
    geodetic.HeightAboveGeoid = height / 1000; // KM
    geodetic.HeightAboveEllipsoid = -9999;
    mGeoid.convertGeoidToEllipsoidHeight( geodetic );
    // TDLog.v( "Geoid To Ellipsoid G " + geodetic.HeightAboveGeoid + " E " + geodetic.HeightAboveEllipsoid );
    return geodetic.HeightAboveEllipsoid * 1000; // M
  }

  // height [M]
  public double ellipsoidToGeoid( double latitude, double longitude, double height )
  {
    MagGeodetic geodetic = new MagGeodetic();
    geodetic.phi    = latitude;  // dec degree
    geodetic.lambda = longitude; // dec degree
    geodetic.HeightAboveGeoid = -9999;
    geodetic.HeightAboveEllipsoid = height / 1000; // KM
    mGeoid.convertEllipsoidToGeoidHeight( geodetic ); 
    // TDLog.v( "Ellipsoid to Geoid G " + geodetic.HeightAboveGeoid + " E " + geodetic.HeightAboveEllipsoid );
    return geodetic.HeightAboveGeoid * 1000; // M
  }

  // ============================================================================
  
  // height = ellipsoid height [M]
  private MagElement doComputeMagElement( double latitude, double longitude, double height, MagDate date )
  {
    MagGeodetic geodetic = new MagGeodetic();
    geodetic.phi    = latitude;  // dec degree
    geodetic.lambda = longitude; // dec degree
    geodetic.HeightAboveEllipsoid = height / 1000; // KM
    geodetic.HeightAboveGeoid = height / 1000;
    // geodetic.HeightAboveGeoid = -9999;
    // mGeoid.convertEllipsoidToGeoidHeight( geodetic ); // FIXME

    MagSpherical spherical = mEllipsoid.geodeticToSpherical( geodetic ); // geodetic to Spherical Eqs. 17-18
    MagModel timedModel    = mModel.getTimelyModifyModel( date );

    // date.debugDate();
    // timedModel.debugModel();

    GeomagLib geomag = new GeomagLib();

    /* Computes the geoMagnetic field elements and their time change*/
    MagElement elements = geomag.MAG_Geomag( mEllipsoid, spherical, geodetic, timedModel );
    geomag.calculateGridVariation( geodetic, elements );
    // MagElement errors = MagUtil.getWMMErrorCalc( elements.H );
    return elements;
  }

  // public static void main( String[] argv )
  // {
  //   WorldMagneticModel WMM = new WorldMagneticModel();
  //   // System.out.println("Ready");
  //   try {
  //     FileReader fr = new FileReader( "sample_coords.txt" );
  //     BufferedReader br = new BufferedReader( fr );
  //     String line;
  //     while( true ) {
  //       line = br.readLine();
  //       if ( line == null ) break;
  //       // line = line.trim();
  //       // System.out.println("Line " + line );
  //       String[] vals = line.split(" ");
  //       double date = Double.parseDouble( vals[0] );
  //       // System.out.println("Date " + date );
  //       // vals[1] coord system
  //       // System.out.println("Coords " + vals[1] );
  //       // System.out.println("Alt. " + vals[2] );
  //       char unit = vals[2].charAt(0);
  //       double f = 1.0;
  //       if ( unit == 'M' ) { f= 0.0001; }
  //       if ( unit == 'F' ) { f= 0.0003048; }
  //       double alt = f * Double.parseDouble( vals[2].substring(1) );
  //       double lat = Double.parseDouble( vals[3] );
  //       double lng = Double.parseDouble( vals[4] );
  //       // System.out.println("Compute " + date + " " + lat + " " + lng + " " + alt );
  //       MagElement elements = WMM.computeMagElement( lat, lng, alt, date );
  //       elements.dump();
  //     }
  //     fr.close();
  //   } catch ( IOException e ) { }
  // }

  // --------------------------------------------------

  // private static int byteToInt( byte[] b_val )
  // {
  //   int i0 = (int)(b_val[0]); if ( i0 < 0 ) i0 = 256 + i0;
  //   int i1 = (int)(b_val[1]); if ( i1 < 0 ) i1 = 256 + i1;
  //   int i2 = (int)(b_val[2]); if ( i2 < 0 ) i2 = 256 + i2;
  //   int i3 = (int)(b_val[3]); if ( i3 < 0 ) i3 = 256 + i3;
  //   // System.out.println( "Bytes " + b_val[0] + " " + b_val[1] + " " + b_val[2] + " " + b_val[3] );
  //   // System.out.println( "Ints " + i0 + " " + i1 + " " + i2 + " " + i3 );
  //   // return (i0 | (i1<<8) | (i2<<16) | (i3<<24));
  //   return (((i3*256 + i2)*256 + i1)*256 + i0);
  // }

  final static private int N = 1038961;
  final static private int ND = 7002;

  // this is correct
  static private int byteToInt( byte[] b )
  {
    int i3 = (int)b[3]; 
    int i2 = (int)b[2]; if ( (b[2] & 0x80) == 0x80 ) i2 = 256+i2;
    int i1 = (int)b[1]; if ( (b[1] & 0x80) == 0x80 ) i1 = 256+i1;
    int i0 = (int)b[0]; if ( (b[0] & 0x80) == 0x80 ) i0 = 256+i0;
    return ( (i3 << 24) | (i2 << 16) | (i1 << 8) | (i0) );
  }
  
  static private int byteToFirst( byte[] b )
  {
    return (((int)b[0]) << 4) | (((int)b[1] & 0xF0)>>4);
  }
  
  static private int byteToSecond( byte[] b )
  {
    return (int)( (((int)b[2]) << 4) | ((int)b[1] & 0x0F) );
  }
  

  static public void loadEGM9615( Context context )
  {
    // TDLog.v( "load EGM9615");
    {
      if ( mGeoidHeightBuffer != null ) return;
      mGeoidHeightBuffer = new float[ N ];
      try {
        // byte[] b_val = new byte[4];
        // DataInputStream fis = new DataInputStream( context.getAssets().open( "wmm/egm9615" ) );
        // for ( int k=0; k < N; ++k ) {
        //   fis.read( b_val );
        //   int i_val = byteToInt( b_val );
        //   double val = i_val / 1000.0f;
        //   mGeoidHeightBuffer[k] = val;
        // }
        // fis.close();
  
        byte[] b4 = new byte[4];
        byte[] b3 = new byte[3];
        byte[] b2 = new byte[2];
  
        int[] res   = new int[ N ];
        int[] delta = new int[ ND ];
        int d_val1, d_val2;

        DataInputStream fis = new DataInputStream( context.getAssets().open( "wmm/egm9615.1024" ) );
        fis.readFully( b4 );
        int k_old = 0;
        res[k_old] = byteToInt( b4 );
        for ( int nk=0; nk<ND; ++nk ) {
          fis.readFully( b4 );
          int old_val = byteToInt( b4 );
          int d_val = old_val >> 18;
          int dk   = old_val & 0x03ffff; // if ( d_val < 0 ) dk ^= 0x03ffff;
          k_old += dk + 1;
          if ( k_old < N ) {
            res[k_old] = d_val; // d_val is res[k_old] - res[k_old-1];
          }
          delta[nk] = dk;
        }
        k_old = 0;
        for ( int nk=0; nk<ND; ++nk ) {
          int nj = delta[nk];
          k_old ++; // skip one res
          for ( int j=1; j<nj; j+=2 ) {
            fis.readFully( b3 );
            d_val1 = byteToFirst( b3 );
            if ( d_val1 >= 2048 ) d_val1 -= 4096;
            res[k_old++] = d_val1;
            d_val2 = byteToSecond( b3 );
            if ( d_val2 >= 2048 ) d_val2 -= 4096;
            res[k_old++] = d_val2;
          }
          if ( (nj%2) == 1 ) {
            fis.readFully( b2 );
            d_val1 = byteToFirst( b2 );
            if ( d_val1 >= 2048 ) d_val1 -= 4096;
            res[k_old++] = d_val1;
          }
        }
        fis.close();
      
        mGeoidHeightBuffer[0] = res[0] / 1000.0f;
        for ( int k=1; k<N; ++k ) {
          res[k] += res[k-1];
          mGeoidHeightBuffer[k] = res[k] / 1000.0f;
        }
      } catch ( IOException e ) {
        TDLog.Error("Input error " + e.getMessage() );
      }
      // System.out.println("loaded EGM9615");
    }
    // TDLog.v( "load EGM9615 done");
  }

  static private void loadWMM( Context context, int num_terms )
  {
    // TDLog.v( "WMM load WMM coeff " + num_terms );
    {
      if ( mWmmCoeff != null ) return;
      mWmmCoeff = new WMMcoeff[ num_terms ];
      for ( int k=0; k<num_terms; ++k ) mWmmCoeff[k] = null;
      
      try {
        InputStreamReader fr = new InputStreamReader( context.getAssets().open( "wmm/wmm.cof" ) );
        BufferedReader br = new BufferedReader( fr );
        String line = br.readLine().trim();
        String[] vals = line.split(" ");
        double start = Double.parseDouble( vals[0] );
        // System.out.println("Start Epoch " + start );
        mStartEpoch = new MagDate( start );
        for ( ; ; ) {
          line = br.readLine().trim();
          if ( line.startsWith("99999") ) break;
          vals = line.split(" ");
          int j = 0; while ( vals[j].length() == 0 ) ++j;
          int n = Integer.parseInt( vals[j] );
          ++j; while ( vals[j].length() == 0 ) ++j;
          int m = Integer.parseInt( vals[j] );
          ++j; while ( vals[j].length() == 0 ) ++j;
          double v0 = Double.parseDouble( vals[j] );
          ++j; while ( vals[j].length() == 0 ) ++j;
          double v1 = Double.parseDouble( vals[j] );
          ++j; while ( vals[j].length() == 0 ) ++j;
          double v2 = Double.parseDouble( vals[j] );
          ++j; while ( vals[j].length() == 0 ) ++j;
          double v3 = Double.parseDouble( vals[j] );
          int index = WMMcoeff.index( n, m );
          mWmmCoeff[index] = new WMMcoeff( n, m, v0, v1, v2, v3 );
          // TDLog.v( "WMM N,M " + n + " " + m + " " + v0 + " " + v1 + " " + v2 + " " + v3 );
        }
        fr.close();
      } catch( IOException e ) {
        // TODO 
      }
      // System.out.println("loaded WMM");
    }
  }
}
