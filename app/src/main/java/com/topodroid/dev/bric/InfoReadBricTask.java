/* @file InfoReadBricTask.java
 *
 * @author marco corvi
 * @date mar 2021 
 *
 * @brief TopoDroid BRIC info read task
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev.bric;

import com.topodroid.TDX.TopoDroidApp;
// import com.topodroid.TDX.R;
// import com.topodroid.TDX.TDToast;

import java.lang.ref.WeakReference;

import android.os.AsyncTask;

public class InfoReadBricTask extends AsyncTask<Void, Integer, Boolean>
{
  private final WeakReference<TopoDroidApp>   mApp; // FIXME LEAK
  private final WeakReference<BricInfoDialog> mDialog;

  /** cstr
   * @param app    application
   * @param dialog info display dialog
   */
  public InfoReadBricTask( TopoDroidApp app, BricInfoDialog dialog )
  {
    mApp      = new WeakReference<TopoDroidApp>( app );
    mDialog   = new WeakReference<BricInfoDialog>( dialog );
  }

  /** execute the task in background
   */
  @Override
  protected Boolean doInBackground(Void... v)
  {
    if ( mApp.get() == null ) return false;
    return mApp.get().getBricInfo( mDialog.get() );
  }

  // @Override
  // protected void onProgressUpdate(Integer... progress)
  // {
  // }

  // @Override
  // protected void onPostExecute( Boolean result )
  // {
  // }

}

