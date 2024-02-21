/* @file DataDownloadTask.java
 *
 * @author marco corvi
 * @date feb 2012
 *
 * @brief TopoDroid batch data-download task
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.calib.CalibInfo;
import com.topodroid.dev.ConnectionState;

import java.lang.ref.WeakReference;

// import java.util.ArrayList;

import android.os.AsyncTask;
// import android.os.Handler;

class DataDownloadTask extends AsyncTask< String, Integer, Integer >
{
  private final WeakReference<TopoDroidApp> mApp; // FIXME LEAK
  private final WeakReference<GMActivity> mGMactivity; // can be null
  private static DataDownloadTask running = null; // static reference to this class - to lock/unlock
  private final ListerHandler mLister; // FIXME_LISTER
  private int mDataType;

  /** cstr
   * @param app       TopoDroid app
   * @param lister    data lister
   * @param data_type packet datatype
   */
  DataDownloadTask( TopoDroidApp app, ListerHandler lister, GMActivity gm_activity, int data_type ) // FIXME_LISTER
  {
    // TDLog.Error( "Data Download Task cstr" );
    // TDLog.v( "data download task cstr");
    mApp        = new WeakReference<TopoDroidApp>( app );
    mGMactivity = new WeakReference<GMActivity>( gm_activity );
    mLister = lister;
    mDataType = data_type;
  }

// -------------------------------------------------------------------
  /** task background execution
   * @param statuses   (unused)
   * @return the number of downloaded packets - ( 0 if there is no app )
   */
  @Override
  protected Integer doInBackground( String... statuses )
  {
    GMActivity gm = mGMactivity.get();
    TopoDroidApp app = mApp.get();
    if ( gm != null && ! gm.isFinishing() && app != null ) {
      int algo = gm.getAlgo();
      if ( algo == CalibInfo.ALGO_AUTO ) { 
        algo = app.getCalibAlgoFromDevice();
        if ( algo < CalibInfo.ALGO_AUTO ) { // could not get the algo from the device type
          algo = CalibInfo.ALGO_LINEAR; 
        }
        app.updateCalibAlgo( algo );
        gm.setAlgo( algo );
      }
      // app.setGMdownload( true ); // GM_DOWNLOAD
    }
    if ( ! lock() ) return null;
    int ret = ( app == null )? 0 : app.downloadDataBatch( mLister, mDataType );
    // app.setGMdownload( false ); // GM_DOWNLOAD
    return ret;
  }

  // @Override
  // protected void onProgressUpdate( Integer... values)
  // {
  //   super.onProgressUpdate( values );
  //   // TDLog.Log( TDLog.LOG_COMM, "onProgressUpdate " + values );
  // }

  /** post-execution: forward the result to the lister and some housekeeping
   * @param res   result of the background execution
   */
  @Override
  protected void onPostExecute( Integer res )
  {
    // TDLog.Log( TDLog.LOG_COMM, "onPostExecute res " + res );
    // TDLog.v( "BLE data download task: post execute: res " + res );
    TopoDroidApp app = mApp.get();
    if ( app != null ) {
      if ( res != null ) {
        int r = res.intValue();
        mLister.refreshDisplay( r, true );  // true: toast a message
        unlock();
      }
      app.mDataDownloader.setDownload( false );
      app.mDataDownloader.notifyConnectionStatus( mLister, ConnectionState.CONN_DISCONNECTED );
    }
  }

  /** lock the static reference
   * @return true if successful, false if already locked
   */
  private synchronized boolean lock()
  {
    // TDLog.v("DATA " + "data download task lock: running is " + ( (running == null )? "null" : (running == this)? "this" : "other") );
    if ( running != null ) return false;
    running = this;
    return true;
  }

  /** unlock the static reference
   */
  private synchronized void unlock()
  {
    // TDLog.v("DATA " + "data download task unlock: running is " + ( (running == null )? "null" : (running == this)? "this" : "other") );
    if ( running == this ) running = null;
  }

}
