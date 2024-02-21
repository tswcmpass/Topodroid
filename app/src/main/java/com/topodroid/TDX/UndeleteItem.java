/* @file UndeleteItem.java
 *
 * @author marco corvi
 * @date july 2020
 *
 * @brief TopoDroid undelete items
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

class UndeleteItem 
{
  // item types:
  final static int UNDELETE_PLOT        = 0;
  final static int UNDELETE_SHOT        = 1;
  final static int UNDELETE_OVERSHOOT   = 2;
  final static int UNDELETE_CALIB_CHECK = 3;
  final static int UNDELETE_BLUNDER     = 4;

  long id;      // ID of the item to recover
  long id2;     // ID of the associated item (for plots)
  boolean flag;
  String  text;
  int     type;

  /** cstr
   * @param _id     item ID
   * @param txt     display text
   * @param _type   item type
   */
  UndeleteItem( long _id, String txt, int _type )
  {
    id   = _id;
    id2  = -1L;
    flag = false;
    text = txt;
    type = _type;
  }

  /** cstr
   * @param _id     item ID
   * @param _id2    associated item ID
   * @param txt     display text
   * @param _type   item type
   */
  UndeleteItem( long _id, long _id2, String txt, int _type )
  {
    id   = _id;
    id2  = _id2;
    flag = false;
    text = txt;
    type = _type;
  }

  /** toggle the item flag
   */
  void flipFlag() { flag = ! flag; }

  /** @return the display text
   */
  String getText() 
  {
    return (flag? "[+] " : "[ ] ") + text;
  }
}
