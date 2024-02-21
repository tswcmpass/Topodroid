/* @file DialogSurface.java
 *
 * @author marco corvi
 * @date mar 2020
 *
 * @brief DEM surface alpha dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.ui.MyDialog;
import com.topodroid.utils.TDLog;
// import com.topodroid.utils.TDLog;
// import com.topodroid.TDX.TDandroid;

import android.os.Bundle;
import android.content.Context;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;

class DialogSurface extends MyDialog 
                    implements View.OnClickListener
{
  private SeekBar mETalpha;
  // private Button mBtnLoadDEM;
  // private Button mBtnLoadTexture;
  private CheckBox mCBproj;
  private CheckBox mCBtexture;
  // private EditText mDemFile;
  // private EditText mTextureFile;

  private final TopoGL mTopoGl;

  private final boolean mHasLocation; // WITH-GPS
  private CheckBox mCBgps;
  private EditText mEast;
  private EditText mNorth;

  /** cstr
   * @param context  context
   * @param topogl      3D activity
   */
  public DialogSurface( Context context, TopoGL topogl )
  {
    super( context, null, R.string.DialogSurface ); // null app
    mTopoGl  = topogl;
    // mHasLocation = FeatureChecker.checkLocation( mContext ); // WITH-GPS
    mHasLocation = TDandroid.checkLocation( mContext ); // WITH-GPS
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    initLayout( R.layout.cave3d_surface_alpha_dialog, R.string.ctitle_surface_alpha );

    mETalpha = ( SeekBar ) findViewById(R.id.alpha);
    mETalpha.setProgress( (int)(GlSurface.mAlpha * 255) );

    TextView mDemFile = (TextView) findViewById( R.id.dem_file );
    
    if ( mTopoGl.mDEMname != null ) {
      mDemFile.setText( String.format( mContext.getResources().getString( R.string.dem_file ), mTopoGl.mDEMname ) );
    } else {
      mDemFile.setVisibility( View.GONE );
    }

    mCBproj = (CheckBox) findViewById( R.id.projection );
    mCBtexture = (CheckBox) findViewById( R.id.texture );

    mCBgps = (CheckBox) findViewById( R.id.gps ); // WITH-GPS
    mEast  = (EditText) findViewById( R.id.east );
    mNorth = (EditText) findViewById( R.id.north );

    Button btn_dem = (Button)findViewById( R.id.dem_load );
    TextView mTextureFile = (TextView) findViewById( R.id.texture_file );
    Button btn_texture = (Button)findViewById( R.id.texture_load );
    // TDLog.v("GL has surface " + mTopoGl.hasSurface() );
    if ( mTopoGl.hasSurface() ) {
      if ( mTopoGl.mTextureName != null ) {
        mTextureFile.setText( String.format( mContext.getResources().getString( R.string.texture_file ), mTopoGl.mTextureName ) );
        btn_texture.setText( R.string.texture );
      } else {
        mTextureFile.setVisibility( View.GONE );
      }
      btn_texture.setOnClickListener( this );
      btn_dem.setText( R.string.dem );
    } else {
      mTextureFile.setVisibility( View.GONE );
      btn_texture.setVisibility( View.GONE );
      mCBgps.setVisibility( View.GONE ); // WITH-GPS
      mEast.setVisibility( View.GONE );
      mNorth.setVisibility( View.GONE );
      // btn_dem.setText( R.string.dem_load );
    }

    mCBproj.setChecked( GlModel.surfaceLegsMode );
    mCBtexture.setChecked( GlModel.surfaceTexture );
    if ( mHasLocation ) { // WITH-GPS
      mCBgps.setChecked( mTopoGl.getGPSstatus() );
    } else {
      mCBgps.setVisibility( View.GONE );
    }

    findViewById( R.id.button_ok ).setOnClickListener( this );
    findViewById( R.id.button_cancel ).setOnClickListener( this );
    findViewById( R.id.dem_load ).setOnClickListener( this );

  }

  /** implement view tap listener
   * @param view tapped view
   */
  public void onClick(View view)
  {
    if ( view.getId() == R.id.dem_load ) {
      // (new DialogDEM( mContext, mTopoGl )).show();
      mTopoGl.selectDEMFile();
    } else if ( view.getId() == R.id.texture_load ) {
      // (new DialogTexture( mContext, mTopoGl )).show();
      mTopoGl.selectTextureFile();
    } else if ( view.getId() == R.id.button_ok ) {
      GlModel.surfaceLegsMode = mCBproj.isChecked();
      GlModel.surfaceTexture  = mCBtexture.isChecked();

      if ( mHasLocation ) { // WITH-GPS
        mTopoGl.setGPSstatus( mCBgps.isChecked() );
      }
      if ( mEast.getText() != null && mNorth.getText() != null ) {
        try {
          double e = Double.parseDouble( mEast.getText().toString() );
          double n = Double.parseDouble( mNorth.getText().toString() );
          mTopoGl.addGPSpoint( e, n );
        } catch ( NumberFormatException e ) {
          TDLog.Error( e.getMessage() );
        }
      }

      // TDLog.v( "Surface onClick()" );
      int alpha = mETalpha.getProgress();
      if ( 0 < alpha && alpha < 256 ) GlSurface.setAlpha( alpha/255.0f );

    // } else if ( view.getId() == R.id.button_cancel ) {
    //   // nothing
    }
    dismiss();
  }  

}

