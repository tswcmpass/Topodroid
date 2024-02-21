/* @file MyHorizontalButtonView.java
 *
 * @author marco corvi (adapted from 
 * http://sandyandroidtutorials.blogspot.it/2013/06/horizontal-listview-tutorial.html
 *
 * @date nov 2013
 *
 * @brief TopoDroid button bar
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.ui;

// import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
// import android.view.View.OnClickListener;

public class MyHorizontalButtonView
{
  private Button[] mButtons;

  public MyHorizontalButtonView( Button[] buttons )
  {
    mButtons  = buttons;
  }

  public void setButtons( Button[] buttons )
  {
    mButtons = buttons;
  }
 
  public BaseAdapter mAdapter = new BaseAdapter()
  {
    @Override
    public int getCount() {
      return mButtons.length;
    }

    @Override
    public Object getItem(int position) {
      return mButtons[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // View ret_val = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewitem, null);
      return (View)mButtons[position];
    }
  };
}

