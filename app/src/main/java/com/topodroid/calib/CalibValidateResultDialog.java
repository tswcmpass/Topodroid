/* @file CalibValidateResultDialog.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid DistoX calibration validation results
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.calib;

import com.topodroid.utils.TDColor;
import com.topodroid.ui.MyDialog;
import com.topodroid.TDX.R;

// import android.app.Dialog;
import android.os.Bundle;

import android.content.Context;

import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View;
import android.view.View.OnClickListener;

// import android.graphics.Bitmap;

public class CalibValidateResultDialog extends MyDialog
                                implements OnClickListener
{
  // private ImageView hist0;
  // private ImageView hist1;
  // private ImageView hist2;
  private final String ave_std0;
  private final String ave_std1;
  // private final String std; // UNUSED
  private final String err1;
  private final String err2;
  private final String err_max;
  private final String title;
  private final float[] errors0;
  private final float[] errors1;
  private final float[] errors2;
  private Button mBtnClose;

  public CalibValidateResultDialog( Context context,
                             float[] errs0, float[] errs1, float[] errs2,
                             double a0, double s0, double a1, double s1,
                             double e1, double e2, double em, String n1, String n2 )
  {
    super( context, null, R.string.CalibValidateResultDialog ); // null app

    errors0 = errs0;
    errors1 = errs1;
    errors2 = errs2;
    ave_std0 = String.format( mContext.getResources().getString( R.string.calib_ave_std ), a0, s0 );
    ave_std1 = String.format( mContext.getResources().getString( R.string.calib_ave_std ), a1, s1 );
    err1  = String.format( mContext.getResources().getString( R.string.calib_validate_error ), e1 );
    err2 = String.format( mContext.getResources().getString( R.string.calib_stddev ), e2 );
    err_max = String.format( mContext.getResources().getString( R.string.calib_max_error ), em );
    title = String.format( mContext.getResources().getString( R.string.calib_validation ), n1, n2 );
  }

// -------------------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);

    initLayout(R.layout.calib_validate_result_dialog, title );

    mBtnClose = (Button) findViewById( R.id.button_close );
    mBtnClose.setOnClickListener( this );

    ((TextView)findViewById(R.id.avestd0)).setText( ave_std0 );
    ((TextView)findViewById(R.id.avestd1)).setText( ave_std1 );
    ((TextView)findViewById(R.id.error1)).setText( err1 );
    ((TextView)findViewById(R.id.error2)).setText( err2 );
    ((TextView)findViewById(R.id.error_max)).setText( err_max );

    ImageView hist0 = (ImageView) findViewById( R.id.histogram0 );
    ImageView hist1 = (ImageView) findViewById( R.id.histogram1 );
    ImageView hist2 = (ImageView) findViewById( R.id.histogram2 );

    hist0.setImageBitmap( CalibCoeffDialog.makeHistogramBitmap( errors0, 400, 100, 40, 5, TDColor.FIXED_BLUE ) );
    hist1.setImageBitmap( CalibCoeffDialog.makeHistogramBitmap( errors1, 400, 100, 40, 5, TDColor.FIXED_ORANGE ) );
    hist2.setImageBitmap( CalibCoeffDialog.makeHistogramBitmap( errors2, 400, 100, 40, 2, TDColor.LIGHT_GRAY ) );
  }

  @Override
  public void onClick( View v )
  {
    dismiss(); // only for mBtnClose
  }

}

