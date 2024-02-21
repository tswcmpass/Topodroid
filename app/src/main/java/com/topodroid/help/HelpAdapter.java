/* @file HelpAdapter.java
 *
 * @author marco corvi
 * @date nov 2013
 *
 * @brief TopoDroid help dialog items adapter
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.help;

import java.util.ArrayList;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;

// import androidx.annotation.RecentlyNonNull;

class HelpAdapter extends ArrayAdapter< HelpEntry >
{
  private final ArrayList< HelpEntry > mItems;
  // private Context mContext;
  // private final HelpDialog mParent;

  /** cstr
   * @param ctx   context
   * @param id    resource ID
   * @param items list of entries to put on the adapter
   */
  HelpAdapter( Context ctx, /* HelpDialog parent, */ int id, ArrayList< HelpEntry > items )
  {
    super( ctx, id, items );
    // mParent = parent;

    if ( items != null ) { // always true
      mItems = items;
    } else {
      mItems = new ArrayList<>();
    }
  }

  /** add an entry to the adapter
   * @param item   entry to add
   */
  public void add( HelpEntry item ) 
  {
    mItems.add( item );
  }

  /** get the view to display an entry
   * @param pos    entry position
   * @param convertView convertible view (if not null)
   * @param parent      view container parent
   */
  // @RecentlyNonNull
  @Override
  public View getView( int pos, View convertView, ViewGroup parent )
  {
    HelpEntry b = mItems.get( pos );
    if ( b == null ) return convertView;
    return b.mView;
  }

}


