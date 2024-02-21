/* @file CalibComputer.java
 *
 * @author marco corvi
 * @date nov 2013
 *
 * @brief TopoDroid calibration coefficient computation
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.calib;

import com.topodroid.utils.TDLog;
// import com.topodroid.prefs.TDSetting;
import com.topodroid.TDX.GMActivity;

import java.lang.ref.WeakReference;

import android.os.AsyncTask;


public class CalibComputer extends AsyncTask< String, Integer, Integer >
{
  public static final int CALIB_COMPUTE_CALIB  = 0;
  public static final int CALIB_COMPUTE_GROUPS = 1;
  public static final int CALIB_RESET_GROUPS   = 2;
  public static final int CALIB_RESET_AND_COMPUTE_GROUPS = 3;

  private final WeakReference<GMActivity> mParent; // FIXME LEAK
  private static CalibComputer running = null;
  private final long mStartId;
  private final int  mPolicy;   // group policy
  private final int mJob;

  public CalibComputer( GMActivity parent, long start, int policy, int job )
  {
    mParent  = new WeakReference<GMActivity>( parent );
    mStartId = start;
    mPolicy  = policy;
    mJob     = job;
  }

// -------------------------------------------------------------------
  @Override
  protected Integer doInBackground( String... statuses )
  {
    if ( ! lock() ) return null;
    GMActivity parent = mParent.get();
    if ( parent == null || parent.isFinishing() ) return 0;

    int ret = 0;
    if ( mJob == CALIB_RESET_GROUPS ) {
      parent.doResetGroups( mStartId );
    } else if ( mJob == CALIB_COMPUTE_GROUPS ) {
      ret = parent.doComputeGroups( mStartId, mPolicy );
    } else if ( mJob == CALIB_RESET_AND_COMPUTE_GROUPS ) {
      parent.doResetGroups( mStartId );
      ret = parent.doComputeGroups( mStartId, mPolicy );
    } else if ( mJob == CALIB_COMPUTE_CALIB ) {
      ret = parent.computeCalib();
    }
    return ret;
  }

  @Override
  protected void onProgressUpdate( Integer... values)
  {
    super.onProgressUpdate( values );
  }

  @Override
  protected void onPostExecute( Integer res )
  {
    if ( res != null ) {
      int r = res.intValue();
      try {
        if (mParent.get() != null && !mParent.get().isFinishing()) { // isFinishing MAY NullPointerException
          mParent.get().handleComputeCalibResult(mJob, r);  // MAY NullPointerException
        }
      } catch ( NullPointerException e ) {
        TDLog.Error( e.getMessage() );
      }
    }
    unlock();
  }

  private synchronized boolean lock()
  {
    if ( running != null ) return false;
    running = this;
    return true;
  }

  private synchronized void unlock()
  {
    if ( running == this ) running = null;
  }

}
