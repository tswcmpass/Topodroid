/* @file ReconnectTask.java
 *
 * @author marco corvi
 * @date nov 2013
 *
 * @brief TopoDroid Data download reconnection task
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.prefs.TDSetting;
import com.topodroid.utils.TDLog;
// import com.topodroid.dev.ConnectionState;

import android.os.AsyncTask;

class ReconnectTask extends AsyncTask< String, Integer, Integer >
{
  private DataDownloader mDownloader;
  private ReconnectTask  running;
  private ListerHandler  mLister;
  private int mDataType; // data type, passed to data_downloader try_connect()
  private int mDelay = 0;

  /** cstr
   * @param downloader   data downloader
   * @param lister       data lister
   * @param data_type    type of data to download
   * @param delay        ...
   */
  ReconnectTask( DataDownloader downloader, ListerHandler lister, int data_type, int delay )
  {
    mDownloader = downloader;
    mLister     = lister;
    mDataType = data_type;
    mDelay = delay;
    running = null;
  }

  /** execute the task in background
   * @param statuses   unused
   * @return result (?)
   */
  @Override
  protected Integer doInBackground( String... statuses )
  {
    if ( ! lock() ) return null;
    if ( TDSetting.mAutoReconnect && TDInstance.isContinuousMode() ) {
      // TDLog.v( "RECONNECT TASK running ...");
      while ( mDownloader.needReconnect() ) {
        try {
          if ( mDelay > 0 ) {
            // TDLog.v("SLEEP reconnect " + mDelay );
            Thread.sleep( mDelay );
          }
          // TDLog.v( "RECONNECT TASK status " + mDownloader.isDownloading() );
          mDownloader.tryConnect( mLister, mDataType ); 
        } catch ( InterruptedException e ) {
          TDLog.Error( e.getMessage() );
        }
      }
    }
    // TDLog.v( "RECONNECT TASK exits");
    return 0;
  }

  // @Override
  // protected void onProgressUpdate( Integer... values)
  // {
  //   super.onProgressUpdate( values );
  // }

  /** execute after the task: release the lock
   */
  @Override
  protected void onPostExecute( Integer res )
  {
    // mDownloader.notifyConnectionStatus( mDownloader.isConnected()? ConnectionState.CONN_CONNECTED : ConnectionState.CONN_WAITING );
    // TDLog.v( "Reconnect Task done with delay " + mDelay );
    unlock();
  }

  /** acquire the lock
   * @return true if successful
   */
  private synchronized boolean lock()
  {
    if ( running != null ) return false;
    running = this;
    return true;
  }

  /** release the lock
   */
  private synchronized void unlock()
  {
    if ( running == this ) running = null;
  }

}

