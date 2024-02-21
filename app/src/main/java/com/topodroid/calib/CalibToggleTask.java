/* @file CalibToggleTask.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid DistoX calib mode toggle task
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.calib;

import com.topodroid.utils.TDLog;
import com.topodroid.TDX.TopoDroidApp;
import com.topodroid.TDX.TDToast;
import com.topodroid.TDX.R;

import java.lang.ref.WeakReference;

// import android.app.Activity;
import android.os.AsyncTask;

// import android.widget.Button;

public class CalibToggleTask extends AsyncTask<Void, Integer, Boolean>
{
  private final WeakReference<TopoDroidApp> mApp; // FIXME LEAK
  private final WeakReference<ICoeffDisplayer> mParent;

  public CalibToggleTask( ICoeffDisplayer parent, TopoDroidApp app )
  {
    mParent = new WeakReference<ICoeffDisplayer>( parent );
    mApp    = new WeakReference<TopoDroidApp>( app );
  }

  @Override
  protected Boolean doInBackground(Void... v)
  {
    try {
      if (mApp.get() != null) return mApp.get().toggleCalibMode();
    } catch ( NullPointerException e ) {
      TDLog.Error( e.getMessage() );
    }
    return false;
  }

  // @Override
  // protected void onProgressUpdate(Integer... progress)
  // {
  // }

  @Override
  protected void onPostExecute( Boolean result )
  {
    if ( result ) {
      TDToast.make( R.string.toggle_ok );
    } else {
      TDToast.makeBad( R.string.toggle_failed );
    }
    try {
      if (mParent.get() != null && !mParent.get().isActivityFinishing()) {
        mParent.get().enableButtons(true);
      }
    } catch ( NullPointerException e ) {
      TDLog.Error( e.getMessage() );
    }
  }
}
