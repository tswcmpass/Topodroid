/* @file CalibActivity.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid calib activity
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDLocale;
import com.topodroid.utils.TDUtil;
import com.topodroid.math.TDMatrix;
import com.topodroid.math.TDVector;
import com.topodroid.ui.MyButton;
import com.topodroid.ui.MyHorizontalListView;
import com.topodroid.ui.MyHorizontalButtonView;
import com.topodroid.help.HelpDialog;
import com.topodroid.help.UserManualActivity;
// import com.topodroid.prefs.TDSetting;
import com.topodroid.prefs.TDPrefCat;
import com.topodroid.calib.CalibInfo;
import com.topodroid.calib.CalibAlgo;
import com.topodroid.calib.CalibResult;
import com.topodroid.calib.CalibCoeffDialog;

// import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.app.Activity;
import android.app.DatePickerDialog;
// import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.res.Configuration;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import android.view.View;
// import android.view.View.OnClickListener;
import android.view.KeyEvent;

// import android.graphics.Bitmap;
// import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class CalibActivity extends Activity
                           implements OnItemClickListener
                           , View.OnClickListener
                           , IExporter
{
  private static final int[] izonsno = { // icons when the calibration is not yet in the database 
                        0, // R.drawable.iz_save_no,
                        R.drawable.iz_open_no,
                        R.drawable.iz_read_no
                        // R.drawable.iz_export0_no
                     };
  private static final int[] izons = { // icons when the calibration is in the database
                        R.drawable.iz_save,
                        R.drawable.iz_open,
                        R.drawable.iz_read 
			// R.drawable.iz_empty // EMPTY
                     };

  private BitmapDrawable mBMopen;
  private BitmapDrawable mBMopen_no;
  private BitmapDrawable mBMread;
  private BitmapDrawable mBMread_no;
  
  private static final int[] menus = {
                        R.string.menu_export,
                        R.string.menu_delete,
                        R.string.menu_options,
                        R.string.menu_help
                     };

  private static final int[] help_icons = {
                        R.string.help_save_calib,
                        R.string.help_open_calib,
                        R.string.help_coeff
                      };
  private static final int[] help_menus = {
                        R.string.help_export_calib,
                        R.string.help_delete_calib,
                        R.string.help_prefs,
                        R.string.help_help
                      };

  private static final int HELP_PAGE = R.string.CalibActivity;

  private EditText mEditName;
  private Button mEditDate;
  // private TextView mEditDevice;
  private String mDeviceAddress;
  private EditText mEditComment;
  private TextView mTVdip;

  private MyDateSetListener mDateListener;

  private RadioButton mCBAlgoAuto;
  private RadioButton mCBAlgoLinear;
  private RadioButton mCBAlgoNonLinear;
  // private RadioButton mCBAlgoMinimum;


  private TopoDroidApp mApp;
  private boolean isSaved;

  /** set the buttons icon
   */
  private void setButtons( )
  {
    mButton1[1].setEnabled( isSaved );   // OPEN
    if ( 2 < mNrButton1 ) mButton1[2].setEnabled( isSaved ); // COEFF_READ
    if ( isSaved ) {
      TDandroid.setButtonBackground( mButton1[1], mBMopen );
      if ( 2 < mNrButton1 ) {
        TDandroid.setButtonBackground( mButton1[2], mBMread );
      }
    } else {
      TDandroid.setButtonBackground( mButton1[1], mBMopen_no );
      if ( 2 < mNrButton1 ) {
        TDandroid.setButtonBackground( mButton1[2], mBMread_no );
      }
    }
  }

// -------------------------------------------------------------------
  private Button[] mButton1;
  private int mNrButton1 = 0;
  // private Button[] mButton2;
  private MyHorizontalListView mListView;
  private MyHorizontalButtonView mButtonView1;
  private ListView   mMenu;
  private Button     mImage;
  private boolean onMenu;

  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);

    TDandroid.setScreenOrientation( this );

    mApp     = (TopoDroidApp)getApplication();
    setContentView(R.layout.calib_activity);
    mEditName    = (EditText) findViewById(R.id.calib_name);
    mEditDate    = (Button) findViewById(R.id.calib_date);
    // mEditDevice  = (TextView) findViewById(R.id.calib_device);
    mEditComment = (EditText) findViewById(R.id.calib_comment);
    mTVdip  = (TextView) findViewById(R.id.calib_dip);

    mDateListener = new MyDateSetListener( mEditDate );
    mEditDate.setOnClickListener( this );

    mCBAlgoAuto      = (RadioButton) findViewById( R.id.calib_algo_auto );
    mCBAlgoLinear    = (RadioButton) findViewById( R.id.calib_algo_linear );
    mCBAlgoNonLinear = (RadioButton) findViewById( R.id.calib_algo_non_linear );
    // mCBAlgoMinimum   = (RadioButton) findViewById( R.id.calib_algo_minimum );

    // if ( ! TDLevel.overTester ) {
    //   mCBAlgoMinimum.setVisibility( View.GONE );
    // }

    mListView = (MyHorizontalListView) findViewById(R.id.listview);
    mListView.setEmptyPlaceholder(true);
    /* int size = */ TopoDroidApp.setListViewHeight( getApplicationContext(), mListView );

    Resources res = getResources();
    mNrButton1 = 2 + ( TDLevel.overNormal? 1 : 0 );
    mButton1 = new Button[ mNrButton1 + 1 ];
    for ( int k=0; k < mNrButton1; ++k ) {
      mButton1[k] = MyButton.getButton( this, this, izons[k] );
      if ( k == 1 )      { mBMopen = MyButton.getButtonBackground( mApp, res, izons[k] ); }
      else if ( k == 2 ) { mBMread = MyButton.getButtonBackground( mApp, res, izons[k] ); }
    }
    mButton1[mNrButton1] = MyButton.getButton( this, null, R.drawable.iz_empty );
    mBMopen_no = MyButton.getButtonBackground( mApp, res, izonsno[1] );
    mBMread_no = MyButton.getButtonBackground( mApp, res, izonsno[2] );

    mButtonView1 = new MyHorizontalButtonView( mButton1 );
    // mButtonView2 = new MyHorizontalButtonView( mButton2 );
    mListView.setAdapter( mButtonView1.mAdapter );

    // TDLog.Log( TDLog.LOG_CALIB, "app TDInstance.cid " + TDInstance.cid );
    setNameEditable( TDInstance.cid >= 0 );
    if ( isSaved ) {
      CalibInfo info = mApp.getCalibInfo();
      mEditName.setText( info.name );
      // mEditName.setEditable( false );
      mEditDate.setText( info.date );
      if ( info.device != null && info.device.length() > 0 ) {
        mDeviceAddress = info.device;
      } else if ( TDInstance.deviceAddress() != null ) {
        mDeviceAddress = TDInstance.deviceAddress();
      }
      // mEditDevice.setText( mDeviceAddress );

      if ( info.comment != null && info.comment.length() > 0 ) {
        mEditComment.setText( info.comment );
      } else {
        mEditComment.setHint( R.string.description );
      }
      switch ( info.algo ) {
        // case 0: mCBAlgoAuto.setChecked( true ); break;
        case 1: mCBAlgoLinear.setChecked( true ); break;
        case 2: mCBAlgoNonLinear.setChecked( true ); break;
        // case 3: mCBAlgoMinimum.setChecked( true ); break;
        default: mCBAlgoAuto.setChecked( true ); break;
      }
      if ( info.dip < 180 ) {
        mTVdip.setText( String.format(Locale.US, getResources().getString( R.string.calib_dip), info.dip ) );
      }
    } else {
      mEditName.setHint( R.string.name );
      mEditDate.setText( TDUtil.currentDate() );
      mDeviceAddress = TDInstance.deviceAddress();
      // mEditDevice.setText( mDeviceAddress );

      mEditComment.setHint( R.string.description );
      mCBAlgoAuto.setChecked( true );
    }

    setButtons();

    mImage = (Button) findViewById( R.id.handle );
    mImage.setOnClickListener( this );
    TDandroid.setButtonBackground( mImage, MyButton.getButtonBackground( mApp, res, R.drawable.iz_menu ) );

    mMenu = (ListView) findViewById( R.id.menu );
    mMenu.setOnItemClickListener( this );
  }

  @Override
  public void onStart() 
  {
    super.onStart();
    TDLocale.resetTheLocale();
    setMenuAdapter( getResources() );
    closeMenu();
  }

  // ---------------------------------------------------------------

  /** respond to a user tap on a view
   * @param view   tapped view
   */
  @Override
  public void onClick(View view)
  {
    if ( onMenu ) {
      closeMenu();
      return;
    }

    // TDLog.Log( TDLog.LOG_INPUT, "onClick(View) " + view.toString() );
    Button b = (Button)view;
    // int id = v.getId(); FIXME 

    if ( b == mImage ) {
      if ( mMenu.getVisibility() == View.VISIBLE ) {
        mMenu.setVisibility( View.GONE );
        onMenu = false;
      } else {
        mMenu.setVisibility( View.VISIBLE );
        onMenu = true;
      }
      return;
    } else if ( b == mEditDate ) {
      String date = mEditDate.getText().toString();
      int y = TDUtil.dateParseYear( date );
      int m = TDUtil.dateParseMonth( date );
      int d = TDUtil.dateParseDay( date );
      new DatePickerDialog( this, mDateListener, y, m, d ).show();
      return;
    }

    int k = 0;
    if ( k < mNrButton1 && b == mButton1[k++] ) { // SAVE
      doSave();
    } else if ( k < mNrButton1 && b == mButton1[k++] ) { // OPEN GM
      if ( ! mApp.checkCalibrationDeviceMatch() ) {
        // FIXME use alert dialog
        TDToast.makeBad( R.string.calib_device_mismatch );
      }
      saveCalibAlgo();
      doOpen();
    } else if ( k < mNrButton1 && b == mButton1[k++] ) { // COEFF
      showCoeffs();
    // } else if ( k < mNrButton1 && b == mButton1[k++] ) {
    //   askDelete();
    }
  }

  /** open a dialog showing the calibration coefficients
   */
  private void showCoeffs()
  {
    String coeff_str = TopoDroidApp.mDData.selectCalibCoeff( TDInstance.cid );
    if ( coeff_str != null ) {
      byte[] coeff = CalibAlgo.stringToCoeff( coeff_str );
      TDMatrix mG = new TDMatrix();
      TDMatrix mM = new TDMatrix();
      TDVector vG = new TDVector();
      TDVector vM = new TDVector();
      TDVector nL = new TDVector();
      CalibAlgo.coeffToG( coeff, vG, mG );
      CalibAlgo.coeffToM( coeff, vM, mM );
      CalibAlgo.coeffToNL( coeff, nL );
   
      CalibResult res = new CalibResult();
      TopoDroidApp.mDData.selectCalibError( TDInstance.cid, res );
      (new CalibCoeffDialog( this, null, vG, mG, vM, mM, nL, null, res.delta_bh, res.error, res.stddev, res.max_error, res.iterations, res.dip, coeff /*, false */ )).show();
    } else {
      TDToast.make( R.string.calib_no_coeff );
    }
  }

  /** ask confirmation to delete this calibration
   */
  private void askDelete()
  {
    TopoDroidAlertDialog.makeAlert( this, getResources(), getResources().getString( R.string.calib_delete ) + " " + TDInstance.calib + " ?",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick( DialogInterface dialog, int btn ) {
          // TDLog.Log( TDLog.LOG_INPUT, "calib delete" );
          doDelete();
        }
      }
    );
  }

  /** open the calibration data - start the GM activity
   */
  private void doOpen()
  {
    Intent openIntent = new Intent( this, GMActivity.class );
    startActivity( openIntent );
  }

  /** save the calibration metadata
   */
  private void doSave( )
  {
    String name = mEditName.getText().toString().trim();
    // if ( name == null ) {
    //   String error = getResources().getString( R.string.error_name_required );
    //   mEditName.setError( error );
    //   return;
    // }
    name = TDUtil.noSpaces( name );
    if ( name.length() == 0 ) {
      String error = getResources().getString( R.string.error_name_required );
      mEditName.setError( error );
      return;
    }

    String date = mEditDate.getText().toString();
    String device = mDeviceAddress; // mEditDevice.getText().toString();
    String comment = mEditComment.getText().toString();
    /* if ( date != null ) */ { date    = date.trim(); }  // date != null always true
    if ( device  != null ) { device  = device.trim(); }
    /* if ( comment != null ) */ { comment = comment.trim(); } // comment != null always true

    if ( isSaved ) { // calib already saved
      TopoDroidApp.mDData.updateCalibInfo( TDInstance.cid, date, device, comment );
      TDToast.make( R.string.calib_updated );
    } else { // new calib
      name = TDUtil.noSpaces( name );
      if ( /* name != null && */ name.length() > 0 ) { // name != null always true
        if ( mApp.hasCalibName( name ) ) { // name already exists
          // TDToast.makeBad( R.string.calib_exists );
          String error = getResources().getString( R.string.calib_exists );
          mEditName.setError( error );
        } else {
          mApp.setCalibFromName( name );
          TopoDroidApp.mDData.updateCalibInfo( TDInstance.cid, date, device, comment );
          setNameEditable( true );
          TDToast.make( R.string.calib_saved );
        }
      } else {
        TDToast.makeBad( R.string.calib_no_name );
      }
    }
    saveCalibAlgo();
    setButtons();
  }

  /** store the choice of calib algo in the database
   */
  private void saveCalibAlgo()
  {
    int algo = 0;
    if ( mCBAlgoLinear.isChecked() )         algo = 1;
    else if ( mCBAlgoNonLinear.isChecked() ) algo = 2;
    // else if ( mCBAlgoMinimum.isChecked() )   algo = 3;
    TopoDroidApp.mDData.updateCalibAlgo( TDInstance.cid, algo );
  }
  
  /** make the name field editable
   * @param saved  whether the calibration is in the database
   */
  private void setNameEditable( boolean saved )
  {
    isSaved = saved;
    if ( isSaved ) {
      mEditName.setFocusable( false );
      mEditName.setClickable( false );
      mEditName.setKeyListener( null );
      // mEditDevice.setFocusable( false );
      // mEditDevice.setClickable( false );
      // mEditDevice.setKeyListener( null );
    }
  }

  /** delete this calibration
   */
  private void doDelete()
  {
    if ( TDInstance.cid < 0 ) return;
    TopoDroidApp.mDData.doDeleteCalib( TDInstance.cid );
    mApp.setCalibFromName( null );
    finish();
  }

  /** respond to a key event
   * @param code   key code
   * @param event  key event
   * @return true if the event has been consumed
   */
  @Override
  public boolean onKeyDown( int code, KeyEvent event )
  {
    switch ( code ) {
      case KeyEvent.KEYCODE_BACK: // HARDWARE BACK (4)
        super.onBackPressed();
        return true;
      case KeyEvent.KEYCODE_MENU:   // HARDWARE MENU (82)
        UserManualActivity.showHelpPage( this, getResources().getString( HELP_PAGE ));
        return true;
      // case KeyEvent.KEYCODE_VOLUME_UP:   // (24)
      // case KeyEvent.KEYCODE_VOLUME_DOWN: // (25)
      default:
        // TDLog.Error( "key down: code " + code );
    }
    return false;
  }

  // ---------------------------------------------------------

  /** initialize the menu
   * @param res   resources
   */
  private void setMenuAdapter( Resources res )
  {
    ArrayAdapter< String > menu_adapter = new ArrayAdapter<>(this, R.layout.menu );

    if ( TDLevel.overNormal ) menu_adapter.add( res.getString( menus[0] ) );
    if ( TDLevel.overBasic  ) menu_adapter.add( res.getString( menus[1] ) );
    menu_adapter.add( res.getString( menus[2] ) );
    menu_adapter.add( res.getString( menus[3] ) );
    mMenu.setAdapter( menu_adapter );
    mMenu.invalidate();
  }

  /** close the menu
   */
  private void closeMenu()
  {
    mMenu.setVisibility( View.GONE );
    onMenu = false;
  }

  /** handle a tap on a menu
   * @param pos   menu position
   */
  private void handleMenu( int pos )
  {
    closeMenu();
    // TDToast.make( item.toString() );
    int p = 0;
    if ( TDLevel.overNormal && p++ == pos ) { // EXPORT
      if ( TDInstance.calib != null ) {
        new ExportDialogCalib( this, this, TDConst.mCalibExportTypes, R.string.title_calib_export ).show();
      }

    } else if ( TDLevel.overBasic && p++ == pos ) { // DELETE 
      if ( TDInstance.calib != null ) {
        askDelete();
      }

    } else if ( p++ == pos ) { // OPTIONS
      Intent intent = new Intent( this, com.topodroid.prefs.TDPrefActivity.class );
      intent.putExtra( TDPrefCat.PREF_CATEGORY, TDPrefCat.PREF_CATEGORY_CALIB );
      startActivity( intent );

    } else if ( p == pos ) { // HELP
      new HelpDialog(this, izons, menus, help_icons, help_menus, mNrButton1, help_menus.length, getResources().getString( HELP_PAGE )).show();
    }
  }

  /** export this calibration 
   * @param type    export type (string)
   * @param name    calib filename (not used)
   * @param prefix  station-prefix (not used)
   * @param second  whether to export the second view (unused)
   * @note implements IExporter
   */
  public void doExport( String type, String name, String prefix, boolean second )
  {
    int index = TDConst.calibExportIndex( type );
    if ( index >= 0 ) {
      // if ( TDSetting.mExportUri ) {
      //   selectExportFromProvider( index ); // TODO
      // } else {
        doMyExport( index );
      // }
    }
  }

  /** export this calibration
   * @param exportType  integer export index 
   */
  private void doMyExport( int exportType )
  {
    if ( TDInstance.cid < 0 ) {
      TDToast.makeBad( R.string.no_calibration );
    } else {
      String filename = null;
      if ( exportType == TDConst.SURVEY_FORMAT_CSV ) {
        filename = mApp.exportCalibAsCsv();
      }
      if ( filename != null ) {
        TDToast.make( String.format( getString(R.string.saved_file_1), filename ) ); 
      } else {
        TDToast.makeBad( R.string.saving_file_failed );
      }
    }
  }

  /** respond to a tap on an item
   * @param parent   parent container
   * @param view     item
   * @param pos      item position
   * @param id       ...
   */
  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    // CharSequence item = ((TextView) view).getText();
    if ( mMenu == (ListView)parent ) {
      handleMenu( pos );
    }
  }

  /** react to a change in the configuration
   * @param new_cfg   new configuration
   */
  @Override
  public void onConfigurationChanged( Configuration new_cfg )
  {
    super.onConfigurationChanged( new_cfg );
    TDLocale.resetTheLocale();
  }

}
