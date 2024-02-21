/* @file BricProto.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief BRIC4 protocol
 * @mote the methods of the BRIC protocol are run on the queue consumer thread
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev.bric;

import com.topodroid.dev.Device;
import com.topodroid.dev.DataType;
import com.topodroid.dev.TopoDroidProtocol;
import com.topodroid.dev.ble.BleOperation;
import com.topodroid.dev.ble.BleCallback;
// import com.topodroid.dev.ble.BleUtils;
import com.topodroid.TDX.TopoDroidApp;
import com.topodroid.TDX.TDToast;
import com.topodroid.TDX.ListerHandler;
import com.topodroid.utils.TDLog;
import com.topodroid.prefs.TDSetting;

// import android.os.Looper;
// import android.os.Handler;
import android.content.Context;

// import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothGatt;
// import android.bluetooth.BluetoothGattService;
// import android.bluetooth.BluetoothGattCharacteristic;
// import android.bluetooth.BluetoothGattDescriptor;
// import android.bluetooth.BluetoothGattCallback;

// import java.util.ArrayList;
import java.util.Arrays;
// import java.util.List;
// import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BricProto extends TopoDroidProtocol
{
  private Device mDevice; // device of this communication
  private ConcurrentLinkedQueue< BleOperation > mOps;
  private final BricComm mComm;
  private final ListerHandler mLister;
  private byte[] mLastTime;       // content of LastTime payload
  private byte[] mLastPrim;   // used to check if the coming Prim is new
  private boolean mPrimToDo = false;
  private int mErr1; // first error code
  private int mErr2; // second error code
  private float mErrVal1;
  private float mErrVal2;
  // private float mErrSecondVal1;
  // private float mErrSecondVal2;
  private String mComment = null;

  private Context mContext; // unused
  BleCallback mCallback;    // unused

  // data struct
  private int   mLastIndex = 0x7ffffffe;
  private int   mIndex = -1;
  private long  mThisTime; // data timestamp [msec]
  long mTime = 0;          // timestamp of data that must be processed
  // float mDistance; // from TopoDroidProtocol double ...
  // float mBearing;
  // float mClino;
  // float mRoll;
  // float mDip;

  // unused
  // short mYear;
  // char  mMonth, mDay, mHour, mMinute, mSecond, mCentisecond;

  /** cstr
   * @param ctx     context
   * @param app     application (unused)
   * @param lister  data lister
   * @param device  bluetooth device
   * @param comm    BRIC comm object
   */
  public BricProto( Context ctx, TopoDroidApp app, ListerHandler lister, Device device, BricComm comm )
  {
    super( device, ctx );
    mLister = lister;
    mComm   = comm;
    // mIndex  = -1;
    // mLastIndex = 7ffffffe;
    mLastTime = null;
    mLastPrim = null; // new byte[20];
  }


  // DATA -------------------------------------------------------

  /* check if the bytes coincide with the last Prim
   * @return true if the bytes are equal to the last Prim
   * @note the last Prim is always filled with the new bytes on exit
   *       and mThisTime is set to the new timestamp
   */ 
  private boolean checkPrim( byte[] bytes )
  {
    if ( Arrays.equals( mLastPrim, bytes ) ) {
      return false;
    }
    mThisTime = BricConst.getTimestamp( bytes ); // first 8 bytes
    mLastPrim = Arrays.copyOf( bytes, 20 );
    return true;
  }

  /** add the primary data byte array
   * @param bytes  primary data values byte array
   */
  void addMeasPrim( byte[] bytes ) 
  {
    if ( checkPrim( bytes ) ) { // if Prim is new
      if ( mPrimToDo ) {        // and there is a previous Prim unprocessed
        processData();
      }
      mTime     = mThisTime;
      mDistance = BricConst.getDistance( bytes );
      mBearing  = BricConst.getAzimuth( bytes );
      mClino    = BricConst.getClino( bytes );
      mPrimToDo = true;
      mErr1 = 0;
      mErr2 = 0;
      // TDLog.v( "BRIC proto: meas_prim " +  mDistance + " " + mBearing + " " + mClino );
    } else {
      // TDLog.v( "BRIC proto: add Prim - repeated primary" );
    }
  }

  /** add the meta-info byte array
   * @param bytes  meta info byte array
   */
  void addMeasMeta( byte[] bytes ) 
  {
    mIndex   = BricConst.getIndex( bytes );
    mRoll    = BricConst.getRoll( bytes );
    mDip     = BricConst.getDip( bytes );
    mType    = BricConst.getType( bytes ); // 0: regular shot, 1: scan shot
    mSamples = BricConst.getSamples( bytes );
    // TDLog.v( "BRIC proto: added Meta " + mIndex + "/" + mLastIndex + " type " + mType );
    if ( mType == 0 ) { 
      if ( mIndex > mLastIndex+1 ) { // LOST SHOTS
        TDLog.e("BRIC proto: missed data, last " + mLastIndex + " current " + mIndex );
        if ( TDSetting.mBricMode != BricMode.MODE_PRIM_ONLY && TopoDroidApp.mMainActivity != null ) {
          final String lost = "missed " + (mIndex - mLastIndex -1) + " data";
          TopoDroidApp.mMainActivity.runOnUiThread( new Runnable() { 
            public void run() { TDToast.makeBad( lost ); }
          } );
        }
      }
    }
  }

  /** add the error byte array
   * @param bytes   error byte array
   */
  void addMeasErr( byte[] bytes ) 
  {
    // TDLog.v( "BRIC proto: added Err " );
    mErr1 = BricConst.firstErrorCode( bytes );
    mErr2 = BricConst.secondErrorCode( bytes );
    mErrVal1 = BricConst.firstErrorValue( bytes, mErr1 );
    mErrVal2 = BricConst.secondErrorValue( bytes, mErr2 );
    // mErrSecondVal1 = BricConst.firstErrorSecondValue( bytes, mErr1 );
    // mErrSecondVal2 = BricConst.secondErrorSecondValue( bytes, mErr2 );
    mComment = ( mErr1 > 0 || mErr2 > 0 )? BricConst.errorString( bytes ) : null;
  }
  
  /** process the data
   * TODO use mType
   */
  void processData()
  {
    // TDLog.v( "BRIC proto process data - prim todo " + mPrimToDo + " index " + mIndex + " type " + mType );
    if ( mPrimToDo ) {
      if ( TDSetting.mBricZeroLength || mDistance > 0.01 ) {
        // TDLog.v( "BRIC proto: process - PrimToDo true: " + mIndex + " prev " + mLastIndex );
        // mComm.handleRegularPacket( DataType.PACKET_DATA, mLister, DataType.DATA_SHOT );
        int index = ( TDSetting.mBricMode == BricMode.MODE_NO_INDEX )? -1 : mIndex;
        float clino = 0;
        float azimuth = 0;
        if ( mErr1 >= 14 || mErr2 >= 14 ) { 
          clino   = ( mErr1 == 14 )? mErrVal1 : (mErr2 == 14)? mErrVal2 : 0;
          azimuth = ( mErr1 == 15 )? mErrVal1 : (mErr2 == 15)? mErrVal2 : 0;
        }
        int data_type = ( mType == 1 )? DataType.DATA_SCAN : DataType.DATA_SHOT;
        if ( ! mComm.handleBricPacket( index, mLister, data_type, clino, azimuth, mComment ) ) {
          TDLog.e( "BRIC proto: skipped existing index " + index );
        }
      } else {
        TDLog.v( "BRIC proto: skipping 0-length data");
      }
      mPrimToDo = false;
      mLastIndex = mIndex;
    } else if ( mIndex == mLastIndex ) {
      TDLog.v( "BRIC proto: process - PrimToDo false: ... repeated " + mIndex);
    } else {
      TDLog.v( "BRIC proto: process - PrimToDo false: ... skip " + mIndex + " prev " + mLastIndex );
      // if ( TDSetting.mBricMode == BricMode.MODE_ALL_ZERO || TDSetting.mBricMode == BricMode.MODE_ZERO_NO_INDEX ) {
      //   int index = ( TDSetting.mBricMode == BricCModeMODE_ZERO_NO_INDEX )? -1 : mIndex;
      //   mComm.handleZeroPacket( mIndex, mLister, DataType.DATA_SHOT );
      // }
      mLastIndex = mIndex;
    }
  }

  /** add the primary values and process them
   * @param bytes   byte array with the primary data
   */
  void addMeasPrimAndProcess( byte[] bytes )
  {
    if ( checkPrim( bytes ) ) { // if Prim is new
      // TDLog.v( "BRIC proto: add Prim and process" );
      mTime     = mThisTime;
      mDistance = BricConst.getDistance( bytes );
      mBearing  = BricConst.getAzimuth( bytes );
      mClino    = BricConst.getClino( bytes );
      if ( TDSetting.mBricZeroLength || mDistance > 0.01 ) { 
        mComm.handleRegularPacket( DataType.PACKET_DATA, mLister, DataType.DATA_SHOT );
      } else { 
        TDLog.v( "BRIC proto: skipping 0-length data");
      }
    } else {
      TDLog.v( "BRIC proto: add & process - repeated prim: ... skip");
    }
  }

  /** save the last time byte array
   * @param bytes   byte array
   */
  void setLastTime( byte[] bytes )
  {
    // TDLog.v( "BRIC proto: set last time " + BleUtils.bytesToString( bytes ) );
    mLastTime = Arrays.copyOfRange( bytes, 0, bytes.length );
  }

  // UNUSED
  // /** reset the last time byte array to null
  //  */
  // void clearLastTime() { mLastTime = null; }

  // UNUSED
  // /** @return the last time byte array
  //  */
  // byte[] getLastTime() { return mLastTime; }

}

