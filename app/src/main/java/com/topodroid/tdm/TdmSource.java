/** @file TdmSource.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid Manager survey source object
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 * source survey in TopoDroid database (essentially it is TdmInput)
 */
package com.topodroid.tdm;

import android.view.View;
// import android.view.View.OnClickListener;
import android.widget.CheckBox;

class TdmSource extends TdmFile
                implements View.OnClickListener
{
  boolean mChecked;

  /** cstr
   * @param surveyname  name of the survey
   */
  public TdmSource( String surveyname )
  {
    super( null, surveyname );
    mChecked = false;
  }

  // void toggleChecked() { mChecked = ! mChecked; }

  /** @return true if the source is "checked" (selected)
   */
  boolean isChecked() { return mChecked; }

  /** react to user taps
   * @param v  tapped view
   * @note the tapped view is the source checkbox and a tap toggle the "checked" state
   */
  @Override
  public void onClick( View v ) 
  {
    mChecked = ! mChecked;
    ((CheckBox)v).setChecked( mChecked );
  }

}
