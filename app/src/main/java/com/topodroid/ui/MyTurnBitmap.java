/* @file MyTurnBitmap.java
 *
 * @author marco corvi
 * @date mar 2018
 *
 * @brief TopoDroid turnable bitmap
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * credits:
 *   the idea for the rotation routine is adapted from 
 *   David Eberly, Integer-based rotations of images, Geometric Tools, LLC, created Feb. 9, 2006 - last modified Mar. 2, 2008
 *   http://www.geometrictools.col
 */
package com.topodroid.ui;

// import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDColor;
import com.topodroid.TDX.R;

// import android.content.Context;
import android.content.res.Resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
// import android.graphics.drawable.BitmapDrawable;

public class MyTurnBitmap 
{
  static private MyTurnBitmap mTurnBitmap = null;  // singleton
  private int mBGcolor; // background color
  private int[] mPxlSave = null;
  private int   mSize    = 0;
  private int   mOff     = 0;
  private int[] mPxl;

  // table of cosine and sine as signed char
  private static final int[] cos128 = {
 128, 127, 127, 127, 127, 127, 126, 126, 125, 124, 124, 123, 122, 121, 120, 119, 118, 117, 115, 114, 112, 111, 109, 108, 106, 104, 102, 100, 98, 96, 94, 92, 90, 88, 85, 83, 81, 78, 76, 73, 71, 68, 65, 63, 60, 57, 54, 51, 48, 46, 43, 40, 37, 34, 31, 28, 24, 21, 18, 15, 12, 9, 6, 3, 0, -3, -6, -9, -12, -15, -18, -21, -24, -28, -31, -34, -37, -40, -43, -46, -48, -51, -54, -57, -60, -63, -65, -68, -71, -73, -76, -78, -81, -83, -85, -88, -90, -92, -94, -96, -98, -100, -102, -104, -106, -108, -109, -111, -112, -114, -115, -117, -118, -119, -120, -121, -122, -123, -124, -124, -125, -126, -126, -127, -127, -127, -127, -127  };
  private static final int[] sin128 = {
 0, 3, 6, 9, 12, 15, 18, 21, 24, 28, 31, 34, 37, 40, 43, 46, 48, 51, 54, 57, 60, 63, 65, 68, 71, 73, 76, 78, 81, 83, 85, 88, 90, 92, 94, 96, 98, 100, 102, 104, 106, 108, 109, 111, 112, 114, 115, 117, 118, 119, 120, 121, 122, 123, 124, 124, 125, 126, 126, 127, 127, 127, 127, 127, 128, 127, 127, 127, 127, 127, 126, 126, 125, 124, 124, 123, 122, 121, 120, 119, 118, 117, 115, 114, 112, 111, 109, 108, 106, 104, 102, 100, 98, 96, 94, 92, 90, 88, 85, 83, 81, 78, 76, 73, 71, 68, 65, 63, 60, 57, 54, 51, 48, 46, 43, 40, 37, 34, 31, 28, 24, 21, 18, 15, 12, 9, 6, 3  };


  /** factory method - create the singleton if it does not exist
   * @param res   resources
   * @return class singleton
   */
  public static MyTurnBitmap getTurnBitmap( Resources res )
  {
    if ( mTurnBitmap == null ) {
      Bitmap dial = BitmapFactory.decodeResource( res, R.drawable.iz_dial_transp ); // FIXME_AZIMUTH_DIAL
      mTurnBitmap = new MyTurnBitmap( dial, TDColor.TRANSPARENT );
    }
    return mTurnBitmap;
  }

  // Bitmap bm1 = Bitmap.createBitmap( mPxl, mSize, mSize, Bitmap.Config.ALPHA_8 );
  // Bitmap bm2 = Bitmap.createScaledBitmap( bm1, w, w, true );

  /** make the bitmap for a given azimuth
   * @param azimuth azimuth angle [deg]
   * @param w       button size [pxl]
   * @return rotated bitmap
   */
  public Bitmap getBitmap( float azimuth, int w )
  {
    // TDLog.v( "get rotated bitmap Angle " + azimuth + " size " + w );
    rotatedBitmap( azimuth );
    Bitmap bm1 = Bitmap.createBitmap( mSize, mSize, Bitmap.Config.ARGB_8888 );
    for (int j=0; j<mSize; ++j ) for ( int i=0; i<mSize; ++i ) {
      bm1.setPixel( i, j, mPxl[j*mSize+i] );
    }
    if ( w < 32 ) return bm1; // SAFETY CHECK
    Bitmap bm2 = Bitmap.createScaledBitmap( bm1, w, w, true );
    bm1.recycle();
    return bm2;
  }

  /** private cstr
   * @param dial     reference bitmap - pixels are saved in mPxlSave
   * @param color    background color
   */
  private MyTurnBitmap( Bitmap dial, int color )
  {
    mBGcolor = color;
    // if ( mPxlSave == null ) { // always true
      int dial_size = dial.getWidth();
      mOff  = dial_size / 4;
      mSize = 2 * mOff + dial_size;
      mPxlSave = new int[mSize * mSize];
      int j = 0;
      for ( ; j < mOff; ++j ) {
        int j_size = j * mSize;
        for ( int i=0; i < mSize; ++i ) mPxlSave[j_size+i] = mBGcolor;
      }
      for ( ; j < mOff+dial_size; ++j ) {
        int j_size = j * mSize;
        int i = 0;
        for ( ; i < mOff;           ++i ) mPxlSave[j_size+i] = mBGcolor;
        for ( ; i < mOff+dial_size; ++i ) mPxlSave[j_size+i] = dial.getPixel(i-mOff, j-mOff);
        for ( ; i < mSize;          ++i ) mPxlSave[j_size+i] = mBGcolor;
        
      }
      for ( ; j < mSize; ++j ) {
        int j_size = j * mSize;
        for ( int i=0; i < mSize; ++i ) mPxlSave[j_size+i] = mBGcolor;
      }
      mPxl = new int[mSize * mSize];
    // }
    // TDLog.v( "Pxl Size " + dial_size + " " + dial.getHeight() );
  }

  /** rotate the pixmap to the given azimuth
   * @param azimuth degrees
   */
  private void rotatedBitmap( float azimuth )
  {
  /*
    float n11 = (mSize - 1.0f)/2;
    float n21 = (mSize - 1.0f)/2;
    float c = TDMath.cosd( 90-azimuth );
    float s = TDMath.sind( 90-azimuth );
    float n11sn21 = n11 + s * n21 - c * n21;
    float n11cn21 = n11 - c * n21 - s * n21;
  */
    azimuth = 90 - azimuth;
    if ( azimuth < 0 ) { azimuth += 360; } else if ( azimuth > 360 ) { azimuth -= 360; }
    int azi = ((int)(azimuth * 256.0/360.0 )) % 256;
    int c, s;
    if ( azi >= 128 ) {
      c = - cos128[azi-128];
      s = - sin128[azi-128];
    } else {
      c = cos128[azi];
      s = sin128[azi];
    }
    int n11 = (mSize - 1)*128;
    int n21 = (mSize - 1);
    int n11sn21 = n11 + s * n21 - c * n21;
    int n11cn21 = n11 - c * n21 - s * n21;
    c *= 2;
    s *= 2;
  //
    for ( int j=0; j<mSize; ++j ) {
    /*
      float js = n11sn21 - s * j;          // n11 - s * (j - n21);
      float jc = n11cn21 + c * j;          // n11 + c * (j - n21);
    */
      int js = n11sn21 - s * j;       
      int jc = n11cn21 + c * j;      
    //
      for ( int i=0; i<mSize; ++i ) {
	// int ii = (int)( js + c * i );      // js + c * (i - n21);
	int ii = ( js + c * i ) >> 8;  
	if ( ii >= 0 && ii < mSize ) {
	  // int jj = (int)( jc + s * i );    // jc + s * (i - n21);
	  int jj = ( jc + s * i ) >> 8;
	  if ( jj >= 0 && jj < mSize ) {
            mPxl[j*mSize+i] = mPxlSave[ ii + jj * mSize ];
  	  } else {
            mPxl[j*mSize+i] = mBGcolor;
       	  }
        }
      }
    }
  }

}

