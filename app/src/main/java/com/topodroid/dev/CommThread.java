/* @file CommThread.java
 *
 * @author marco corvi
 * @date feb 2021 (extracted from TopoDroidComm)
 *
 * @brief TopoDroid bluetooth RFcomm communication thread
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev;

import com.topodroid.dev.distox.DistoX;
import com.topodroid.prefs.TDSetting;
import com.topodroid.utils.TDUtil;
// import com.topodroid.utils.TDLog;
import com.topodroid.TDX.ListerHandler;

// import android.os.Handler;

public class CommThread extends Thread
{
  int mType;

  private TopoDroidComm     mComm;
  private int toRead; // number of packet to read
  private int mTimeout; // data receiving timeout
  // private ILister mLister;
  ListerHandler mLister; // = null; // FIXME_LISTER
  // private long mLastShotId;   // last shot id

  private volatile boolean doWork = true;
  private final int mDataType;   // packet datatype

  public void cancelWork()
  {
    mComm.cancelWork();
    doWork = false;
  }

  /** 
   * @param type        communication type
   * @param comm        communication class
   * @param to_read     number of data to read (use -1 to read forever until timeout or an exception)
   * @param lister      optional data lister
   * @param data_type   packet datatype (either shot or calib)
   * @param timeout     data receiving timeout (UNUSED)
   */
  public CommThread( int type, TopoDroidComm comm, int to_read, ListerHandler lister, int data_type, int timeout ) // FIXME_LISTER
  {
    mType  = type;
    toRead = to_read;
    mComm  = comm;
    mLister   = lister;
    mDataType = data_type;
    mTimeout  = timeout; 
    // reset nr of read packets 
    mComm.setNrReadPackets( 0 );
    // mLastShotId = 0;
  }

  /** This thread blocks on read_Packet (socket read) and when a packet arrives 
   * it handles it
   */
  public void run()
  {
    doWork = true;
    mComm.setHasG( false );
    // TODO use mTimeout

    // TDLog.v("RF Comm Thread start");

    // TDLog.v( "DistoX-BLE", "TD comm: RF thread ... to_read " + toRead );
    if ( mType == TopoDroidComm.COMM_RFCOMM ) {
      while ( doWork && mComm.getNrReadPackets() != toRead ) {
        // TDLog.v( "RF comm loop: read " + mComm.getNrReadPackets() + " to-read " + toRead );
        
        int res = mComm.readingPacket( (toRead >= 0), mDataType );
        // TDLog.v( "RF comm read_packet returns " + res );
        if ( res == DataType.PACKET_NONE ) {
          if ( toRead == -1 ) {
            doWork = false;
          } else {
            // TDLog.Log( TDLog.LOG_COMM, "RF comm sleeping 1000 " );
            TDUtil.slowDown( TDSetting.mWaitConn, "RF comm thread sleep interrupt");
          }
        } else if ( res == DistoX.DISTOX_ERR_OFF ) {
          // TDLog.Error( "RF comm read_packet returns ERR_OFF " );
          // if ( TDSetting.mCommType == 1 && TDSetting.mAutoReconnect ) { // FIXME ACL_DISCONNECT
          //   mApp.mDataDownloader.setConnected( false );
          //   mApp.notifyStatus();
          //   closeSocket( );
          //   mApp.notifyDisconnected();
          // }
          doWork = false;
        } else {
          mComm.handleRegularPacket( res, mLister, mDataType );
        }
      }
    } else { // if ( mType == COMM_GATT ) 
      // Log.v("XBLE TD comm: proto read_packets");
      mComm.readingPacket( true, mDataType );
    }
    // TDLog.Log( TDLog.LOG_COMM, "RF comm thread run() exiting");
    mComm.doneCommThread();
    // mCommThread = null;

    // FIXME_COMM
    // mApp.notifyConnState( );
    // TDLog.v("RF Comm Thread exit");
  }
}
