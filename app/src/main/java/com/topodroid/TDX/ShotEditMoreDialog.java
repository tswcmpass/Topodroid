/* @file ShotEditMoreDialog.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid photo dialog (to enter the name of the photo)
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDString;
import com.topodroid.utils.TDStatus;
import com.topodroid.ui.MyCheckBox;
import com.topodroid.ui.MyHorizontalListView;
import com.topodroid.ui.MyHorizontalButtonView;
import com.topodroid.ui.MyDialog;
import com.topodroid.ui.TDLayout;
import com.topodroid.prefs.TDSetting;

// import android.app.Dialog;
import android.os.Bundle;

// import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;


import android.view.View;
// import android.view.ViewGroup.LayoutParams;
// import android.view.View.OnKeyListener;
// import android.view.KeyEvent;

import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;

class ShotEditMoreDialog extends MyDialog
                                implements View.OnClickListener
{
  private final ShotWindow mParent;
  private final DBlock mBlk;

  // private boolean audioCheck = false;
  // private boolean photoCheck = false; // not used: can take photo with "camera" app

  private TextView mTVstations;
  private TextView mTVdata;

  private RadioButton mRBfrom;
  private RadioButton mRBto;
  private RadioButton mRBat;
  private EditText mETat;
  private EditText mETleft;
  private EditText mETright;
  private EditText mETup;
  private EditText mETdown;
  private Button mBTlrud;
  private CheckBox mCBleg = null;

  // private MyCheckBox mButtonPlot;
  private MyCheckBox mButtonPhoto  = null;
  private MyCheckBox mButtonAudio  = null;
  private MyCheckBox mButtonSensor = null;
  private MyCheckBox mButtonShot   = null;
  private MyCheckBox mButtonSurvey = null;

  private MyCheckBox mButtonDelete = null;
  private MyCheckBox mButtonCheck  = null;

  private MyHorizontalListView mListView;
  private MyHorizontalButtonView mButtonView;
  private Button[] mButton;

  private Button mBtnCancel;

  /**
   * @param context   context
   * @param parent    parent shot list activity
   */
  ShotEditMoreDialog( Context context, ShotWindow parent, DBlock blk )
  {
    super( context, null, R.string.ShotEditMoreDialog ); // null app
    mParent  = parent;
    mBlk = blk;
    // TDLog.Log( TDLog.LOG_PHOTO, "Shot EditMore Dialog");
  }

// -------------------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    // TDLog.Log(  TDLog.LOG_PHOTO, "Shot EditMore Dialog onCreate" );
    initLayout(R.layout.shot_edit_more_dialog, R.string.title_photo );

    int size = TDSetting.mSizeButtons; // TopoDroidApp.getScaledSize( mContext );

    boolean audioCheck = TDandroid.checkMicrophone( mContext );
    // boolean photoCheck = TDandroid.checkCamera( mContext );

    LinearLayout layout4 = (LinearLayout) findViewById( R.id.layout4 );
    layout4.setMinimumHeight( size + 20 );
    
    LinearLayout.LayoutParams lp = TDLayout.getLayoutParams( 0, 10, 20, 10 );

    mRBfrom  = (RadioButton)findViewById( R.id.station_from );
    mRBto    = (RadioButton)findViewById( R.id.station_to );
    mRBat    = (RadioButton)findViewById( R.id.station_at );
    mETat    = (EditText)findViewById( R.id.station_distance );
    mETleft  = (EditText)findViewById( R.id.shot_left );
    mETright = (EditText)findViewById( R.id.shot_right );
    mETup    = (EditText)findViewById( R.id.shot_up );
    mETdown  = (EditText)findViewById( R.id.shot_down );
    mBTlrud  = (Button)findViewById( R.id.btn_ok );

    int nr_buttons = 5; // ( mBlk.type() == DBlock.BLOCK_MAIN_LEG )? 7 : 6;
    mButton = new Button[nr_buttons];
    int pos = 0;

    // mButtonPlot   = new MyCheckBox( mContext, size, R.drawable.iz_plot, R.drawable.iz_plot ); 
    // mButtonPlot.setOnClickListener( this );
    
    // if ( photoCheck ) {
      mButtonPhoto  = new MyCheckBox( mContext, size, R.drawable.iz_camera, R.drawable.iz_camera ); 
      mButton[pos++] = mButtonPhoto;
      mButtonPhoto.setOnClickListener( this );
    // } else {
    //   -- nr_buttons;
    // }

    if ( audioCheck ) {
      mButtonAudio = new MyCheckBox( mContext, size, R.drawable.iz_audio, R.drawable.iz_audio ); 
      mButtonAudio.setOnClickListener( this );
      mButton[pos++] = mButtonAudio;
    } else {
      -- nr_buttons;
    }

    if ( TDSetting.mWithSensors ) {
      mButtonSensor = new MyCheckBox( mContext, size, R.drawable.iz_sensor, R.drawable.iz_sensor ); 
      mButtonSensor.setOnClickListener( this );
      mButton[pos++] = mButtonSensor;
    } else {
      // mButtonSensor = null;
      -- nr_buttons;
    }

    mButtonShot   = new MyCheckBox( mContext, size, R.drawable.iz_add_leg, R.drawable.iz_add_leg );
    mButtonShot.setOnClickListener( this );
    mButton[pos++] = mButtonShot;

    if ( TDLevel.overAdvanced ) {
      mButtonSurvey = new MyCheckBox( mContext, size, R.drawable.iz_split, R.drawable.iz_split );
      mButtonSurvey.setOnClickListener( this );
      mButton[pos++] = mButtonSurvey;
    } else {
      // mButtonSurvey = null;
      -- nr_buttons;
    }

    mListView = (MyHorizontalListView) findViewById(R.id.listview);
    // mListView.setEmptyPlaceholder( true );
    /* size = */ TopoDroidApp.setListViewHeight( mContext, mListView );
    mButtonView = new MyHorizontalButtonView( mButton );
    mListView.setAdapter( mButtonView.mAdapter );
    layout4.invalidate();

    LinearLayout layout4b = (LinearLayout) findViewById( R.id.layout4b );
    layout4b.setMinimumHeight( size + 20 );

    mButtonDelete = new MyCheckBox( mContext, size, R.drawable.iz_delete_transp, R.drawable.iz_delete_transp );
    mButtonDelete.setOnClickListener( this );
    // mCBleg = (CheckBox) findViewById( R.id.leg ); // delete whole leg
    layout4b.addView( mButtonDelete );
    mButtonDelete.setLayoutParams( lp );

    LinearLayout layout4c = (LinearLayout) findViewById( R.id.layout4c );
    if ( mBlk.isMainLeg() ) {
      mCBleg = new CheckBox( mContext );
      mCBleg.setText( R.string.delete_whole_leg );
      mCBleg.setChecked( false );
      layout4b.addView( mCBleg );
      mCBleg.setLayoutParams( lp );
      if ( TDLevel.overAdvanced && mBlk.isDistoX() ) {
        layout4c.setMinimumHeight( size + 20 );
        mButtonCheck  = new MyCheckBox( mContext, size, R.drawable.iz_compute_transp, R.drawable.iz_compute_transp );
        mButtonCheck.setOnClickListener( this );
        layout4c.addView( mButtonCheck );
        mButtonCheck.setLayoutParams( lp );
      } else {
        layout4c.setVisibility( View.GONE );
      }
    } else {
      layout4c.setVisibility( View.GONE );
    }
    
    mBtnCancel = (Button) findViewById( R.id.button_cancel );
    mBtnCancel.setOnClickListener( this );

    mTVstations = (TextView) findViewById( R.id.photo_shot_stations );
    mTVdata = (TextView) findViewById( R.id.photo_shot_data );
    mTVstations.setText( String.format( mContext.getResources().getString( R.string.shot_name ), mBlk.Name() ) );
    if ( TDInstance.datamode == SurveyInfo.DATAMODE_NORMAL ) {
      mTVdata.setText( mBlk.dataStringNormal( mContext.getResources().getString(R.string.shot_data) ) );
    } else { // SurveyInfo.DATAMODE_DIVING
      mTVdata.setText( mBlk.dataStringDiving( mContext.getResources().getString(R.string.shot_data) ) );
    }

    if ( mBlk.mFrom.length() > 0 ) {
      mRBfrom.setText( mBlk.mFrom );
      mRBfrom.setChecked( true );
      if ( mBlk.mTo.length() > 0 ) {
        mRBto.setText( mBlk.mTo );
        mETat.setText( TDString.ZERO );
      } else {
        mRBto.setVisibility( View.GONE );
        mRBat.setVisibility( View.GONE );
        mETat.setVisibility( View.GONE );
      }
      mBTlrud.setOnClickListener( this );
    } else {
      mRBfrom.setVisibility( View.GONE );
      mRBto.setVisibility( View.GONE );
      mRBat.setVisibility( View.GONE );
      mETat.setVisibility( View.GONE );
      mETleft.setVisibility( View.GONE );
      mETright.setVisibility( View.GONE );
      mETup.setVisibility( View.GONE );
      mETdown.setVisibility( View.GONE );
      mBTlrud.setVisibility( View.GONE );
    }


  }

  @Override
  public void onClick(View v) 
  {
    Button b = (Button) v;
    // TDLog.Log(  TDLog.LOG_INPUT, "PhotoSensorDialog onClick() " + b.getText().toString() );

    if ( b == mBTlrud ) { // AT-STATION LRUD
      float d = -1;
      long at = mBlk.mId;
      String station = null;
      String from = null;
      if ( mRBto.isChecked() ) { // TO
        station = mBlk.mTo;
      } else if ( mRBfrom.isChecked() ) { // FROM
        station = mBlk.mFrom;
      } else { 
	String dstr = mETat.getText().toString().replace(',','.');
	try { d = Float.parseFloat( dstr ); } catch ( NumberFormatException e ) {
      TDLog.Error("Non-number value");
    }
        // add a duplicate leg d, mBlk.mBearing, mBlk.mClino
	from = mBlk.mFrom;
	station = from + "-" + dstr;
        // at should be -1L in this case
        at = -1L;
      }
      if ( station != null ) {
        // try insert intermediate LRUD
        if ( mParent.insertLRUDatStation( at, station, mBlk.mBearing, mBlk.mClino, 
          mETleft.getText().toString().replace(',','.') ,
          mETright.getText().toString().replace(',','.') ,
          mETup.getText().toString().replace(',','.') ,
          mETdown.getText().toString().replace(',','.') 
          ) ) {
          if ( from != null ) {
            // TDLog.v("LRUD " + "insert dup leg from " + from + " station " + station ); 
            mParent.insertDuplicateLeg( from, station, d, mBlk.mBearing, mBlk.mClino, mBlk.getIntExtend() );
          }
        }
      }
      dismiss();
    // } else if ( b == mButtonPlot ) {       // PHOTO
    //   mParent.highlightBlock( mBlk );
    //   dismiss();
    } else if ( mButtonPhoto != null && b == mButtonPhoto ) {  // PHOTO
      mParent.askPhotoComment( mBlk );
      dismiss();
    } else if ( mButtonAudio != null && b == mButtonAudio ) {  // AUDIO
      mParent.startAudio( mBlk );
      dismiss();
    } else if ( mButtonSensor != null && b == mButtonSensor ) { // SENSOR
      mParent.askSensor( mBlk );
      dismiss();
    // } else if ( b == mButtonExternal ) {
    //   mParent.askExternal( );
    } else if ( b == mButtonShot ) {  // INSERT SHOT
      mParent.dialogInsertShotAt( mBlk );
      dismiss();
    } else if ( mButtonSurvey != null && b == mButtonSurvey ) { // SPLIT
      if ( TDLevel.overExpert ) {
        mParent.doSplitOrMoveSurvey( );
        dismiss();
      } else {
        TopoDroidAlertDialog.makeAlert( mParent, mParent.getResources(), R.string.survey_split,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int btn ) {
              mParent.doSplitOrMoveSurvey( null );  // null: split
              dismiss();
            }
          } );
        // mParent.askSurvey( );
      }
    } else if ( mButtonCheck != null && b == mButtonCheck ) { // CHECK
      TopoDroidAlertDialog.makeAlert( mParent, mParent.getResources(), R.string.shot_check,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick( DialogInterface dialog, int btn ) {
            mParent.doDeleteShot( mBlk.mId, mBlk, TDStatus.CHECK, true );
            dismiss();
          }
        } );
    } else if ( b == mButtonDelete ) { // DELETE
      TopoDroidAlertDialog.makeAlert( mParent, mParent.getResources(), R.string.shot_delete,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick( DialogInterface dialog, int btn ) {
            mParent.doDeleteShot( mBlk.mId, mBlk, TDStatus.DELETED, (mCBleg != null && mCBleg.isChecked()) );
            dismiss();
          }
        } );
      // mParent.doDeleteShot( mBlk.mId );

    } else if ( b == mBtnCancel ) {
      /* nothing */
      dismiss();
    }
  }

}

