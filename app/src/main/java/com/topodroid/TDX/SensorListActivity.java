/* @file SensorListActivity.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid survey sensor listing
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDLocale;
import com.topodroid.utils.TDStatus;
import com.topodroid.utils.TDUtil;
import com.topodroid.help.UserManualActivity;

import java.util.List;
import java.util.ArrayList;

import android.os.Bundle;
// import android.os.Handler;
// import android.os.Message;

import android.app.Activity;

// import android.content.Context;
// import android.content.Intent;
// import android.content.res.ColorStateList;
import android.content.res.Configuration;

// import android.location.LocationManager;

import android.widget.TextView;
import android.widget.ListView;
// import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.KeyEvent;
// import android.view.View.OnClickListener;

public class SensorListActivity extends Activity
                                implements OnItemClickListener
{
  // private TopoDroidApp mApp;

  private ListView mList;
  // private int mListPos = -1;
  // private int mListTop = 0;
  private SensorAdapter   mDataAdapter;
  private long mShotId = -1;   // id of the shot

  private String mSaveData = "";
  private TextView mSaveTextView = null;
  private SensorInfo mSaveSensor = null;

  String mSensorComment;
  long   mSensorId;

  // -------------------------------------------------------------------

  private void updateDisplay( )
  {
    // TDLog.Log( TDLog.LOG_SENSOR, "updateDisplay() status: " + StatusName() + " forcing: " + force_update );
    if ( TopoDroidApp.mData != null && TDInstance.sid >= 0 ) {
      List< SensorInfo > list = TopoDroidApp.mData.selectAllSensors( TDInstance.sid, TDStatus.NORMAL );
      // TDLog.Log( TDLog.LOG_PHOTO, "update shot list size " + list.size() );
      updateSensorList( list );
      setTitle( TDInstance.survey );
    // } else {
    //   TDToast.makeBad( R.string.no_survey );
    }
  }

  private void updateSensorList( List< SensorInfo > list )
  {
    // TDLog.Log(TDLog.LOG_SENSOR, "updateSensorList size " + list.size() );
    mDataAdapter.clear();
    mList.setAdapter( mDataAdapter );
    if ( TDUtil.isEmpty(list) ) {
      TDToast.makeBad( R.string.no_sensors );
      finish();
    }
    for ( SensorInfo item : list ) {
      mDataAdapter.add( item );
    }
  }

  // ---------------------------------------------------------------
  // list items click

  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    // TDLog.Log( TDLog.LOG_INPUT, "SensorListActivity onItemClick id " + id);
    startSensorDialog( (TextView)view, position );
  }

  private void startSensorDialog( TextView tv, int pos )
  {
     mSaveSensor = mDataAdapter.get(pos);
     (new SensorEditDialog( this, this, mSaveSensor )).show();
  }

  // ---------------------------------------------------------------
  
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate( savedInstanceState );

    TDandroid.setScreenOrientation( this );

    setContentView(R.layout.sensor_list_activity);
    
    // mApp = (TopoDroidApp) getApplication();
    mDataAdapter = new SensorAdapter( this, R.layout.row, new ArrayList< SensorInfo >() );

    mList = (ListView) findViewById(R.id.list);
    mList.setAdapter( mDataAdapter );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );

    updateDisplay( );
  }

  // ------------------------------------------------------------------

  void dropSensor( SensorInfo sensor )
  {
    TopoDroidApp.mData.deleteSensor( sensor.sid, sensor.id );
    updateDisplay( ); // FIXME
  }

  void updateSensor( SensorInfo sensor, String comment )
  {
    // TDLog.Log( TDLog.LOG_SENSOR, "updateSensor comment " + comment );
    if ( TopoDroidApp.mData.updateSensor( sensor.sid, sensor.id, comment ) ) {
      // if ( app.mListRefresh ) {
      //   // This works but it refreshes the whole list
      //   mDataAdapter.notifyDataSetChanged();
      // } else {
      //   mSaveSensor.mComment = comment;
      // }
      updateDisplay(); // FIXME
    } else {
      TDToast.makeBad( R.string.no_db );
    }
  }

  @Override
  public boolean onKeyDown( int code, KeyEvent event )
  {
    switch ( code ) {
      case KeyEvent.KEYCODE_MENU:   // HARDWARE MENU (82)
        String help_page = getResources().getString( R.string.SensorListActivity );
        /* if ( help_page != null ) */ UserManualActivity.showHelpPage( this, help_page );
        return true;
      case KeyEvent.KEYCODE_BACK: // HARDWARE BACK (4)
        super.onBackPressed();
        return true;
      // case KeyEvent.KEYCODE_VOLUME_UP:   // (24)
      // case KeyEvent.KEYCODE_VOLUME_DOWN: // (25)
      default:
        // TDLog.Error( "key down: code " + code );
    }
    return false;
  }

  /** react to a change in the configuration
   * @param cfg   new configuration
   */
  @Override
  public void onConfigurationChanged( Configuration cfg )
  {
    super.onConfigurationChanged( cfg );
    TDLocale.resetTheLocale();
  }
}
