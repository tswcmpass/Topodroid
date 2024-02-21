/** @file ParserDEM.java
 *
 * @author marco corvi
 * @date apr 2020
 *
 * @brief DEM parser
 *
 * Usage:
 *    ParserDEM DEM = new ParserDEM( filename );
 *    if ( DEM.valid() ) DEM.readData( west, east, south, north );
 *    if ( DEM.valid() ) { // use data
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;

import com.topodroid.utils.TDLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class ParserDEM extends DEMsurface
{
  protected boolean mValid;    // header is valid
  protected int   mMaxSize;    // max DEM size (in each direction)
  // double mDim1; protected double   xcell;     // cell size
  // double mDim2; protected double   ycell;     // cell size
  // float[] mZ; protected float[] mData;     // DEM data
  // int mNr1; protected int     mX;    // grid dimension
  // int mNr2; protected int     mY;    // grid dimension

  // double mEast1 mEast2 mNorth1, mNorth2
  // protected double   mWest, mEast, mSouth, mNorth; // bounds
  protected double nodata;    // nodata value
  String mFilename; // DEM filename
  protected double xunit; // X factor, either 1 [m] or R*PI/180 [dec.degree]
  protected double yunit;

  protected InputStreamReader mIsr = null;
  protected BufferedReader mBr = null;

  /** cstr
   * @param isr        input reader
   * @param filename   file fullpath
   * @param size       max DEM size
   */
  ParserDEM( InputStreamReader isr, String filename, int size ) // FIXME DEM_URI 
  {
    mIsr      = isr;
    mBr       = new BufferedReader( mIsr );
    mFilename = filename;
    mMaxSize  = size;
    xunit = 1.0f;
    yunit = 1.0f;
    mValid = readHeader( mFilename );
  }

  /** cstr
   * @param filename   file fullpath
   * @param size       max DEM size
   * @param xu         X units
   * @param yu         Y units
   */
  ParserDEM( InputStreamReader isr, String filename, int size, double xu, double yu )
  {
    mIsr      = isr;
    mBr       = new BufferedReader( mIsr );
    mFilename = filename;
    mMaxSize  = size;
    xunit = xu;
    yunit = yu;
    mValid = readHeader( mFilename );
  }

  /** @return whether the DEM is valid
   */
  boolean valid() { return mValid; }

  /** @return DEM west bound
   */
  double west()  { return mEast1; }

  /** @return DEM east bound
   */
  double east()  { return mEast2; }

  /** @return DEM south bound
   */
  double south() { return mNorth1; }

  /** @return DEM north bound
   */
  double north() { return mNorth2; }

  /** @return Z value of a cell
   * @param i   X-index of the cell
   * @param j   Y-index of the cell
   */
  float Z( int i, int j ) { return mZ[j*mNr1+i]; }

  /** @return cell X size
   */
  double cellXSize() { return mDim1; }

  /** @return cell Y size
   */
  double cellYSize() { return mDim2; }

  /** @return number of cells in X direction
   */
  int dimX() { return mNr1; }

  /** @return number of cells in Y direction
   */
  int dimY() { return mNr2; }
  
  /** @return the array of Z values
   */
  float[] data() { return mZ; }

  /** read DEM data - default no read
   * @param xwest     West X coord
   * @param xeast     East X coord
   * @param ysouth    South Y coord
   * @param ynorth    North Y coord
   */
  boolean readData( double xwest, double xeast, double ysouth, double ynorth ) { return false; }

  /** read DEM header - default no read
   * @param filename    pathname of the file 
   */
  protected  boolean readHeader( String filename ) { mValid = false; return false; }

  // protected void makeNormal() 
  // {
  //   if ( mValid ) {
  //     mNormal = new float[ 3*mNr1*mNr2 ];
  //     initNormal();
  //   }
  // }

  /** close the input stream 
   */
  protected void tryCloseStream()
  {
    if ( mIsr != null ) try { 
      mIsr.close(); 
      mIsr = null;
      mBr  = null;
    } catch ( IOException e ) {
      TDLog.Error( e.getMessage() );
    }
  }

}

