/* @file DialogSurvey.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D drawing infos dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;

// import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
// import android.content.Context;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

class DialogSurvey extends MyDialog 
                   implements View.OnClickListener
{
  // private Button mBtnOk;

  private final TopoGL mTopoGl;
  private final Cave3DSurvey mSurvey;

  public DialogSurvey( TopoGL topogl, Cave3DSurvey survey )
  {
    super( topogl, null, R.string.DialogSurvey ); // null app
    mTopoGl = topogl;
    mSurvey = survey;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    initLayout( R.layout.cave3d_survey_dialog, mSurvey.name );

    TextView tv;
    tv = (TextView) findViewById( R.id.survey_legs );
    tv.setText( String.format( Locale.US, "%d", mSurvey.getShotNr() ) );
    tv = (TextView) findViewById( R.id.survey_legs_length );
    tv.setText( String.format( Locale.US, "%d", (int)(mSurvey.mLenShots) ) );
    tv = (TextView) findViewById( R.id.survey_splays );
    tv.setText( String.format( Locale.US, "%d", mSurvey.getSplayNr() ) );
    tv = (TextView) findViewById( R.id.survey_splays_length );
    tv.setText( String.format( Locale.US, "%d", (int)(mSurvey.mLenSplays) ) );

    ((Button) findViewById( R.id.button_close )).setOnClickListener( this );

  }

  @Override
  public void onClick(View view)
  {
    dismiss();
  }

}

