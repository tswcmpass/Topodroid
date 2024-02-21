/* @file Scanline.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid SQLite loader readfile helper
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;

class Scanline
{
  private String val;
  private int pos,  // string position
              len;  // string length

  /** cstr
   * @param line   text line
   * @param p      start position
   * @param l      end position, eg, length of the text line
   */
  Scanline( String line, int p, int l )
  {
    val = line;
    pos = p;
    len = l;
    skipSpaces();
  }

  /** skip the spaces
   */
  private void skipSpaces( )
  {
    while ( pos < len && val.charAt(pos) == ' ' ) ++ pos;
  }

  /** skip one comma (if present) and following spaces
   */
  private void skipCommaAndSpaces( )
  {
    if ( pos < len && val.charAt(pos) == ',' ) ++pos;
    while ( pos < len && val.charAt(pos) == ' ' ) ++ pos;
  }
  
  /** @return the position of the next quote
   * @param quote   quote character (either '"' or '\'')
   */
  private int nextQuote( int quote )
  {
    int next = pos;
    while ( next < len && val.charAt(next) != quote ) ++next; 
    return next;
  }

  /** @return the position of next comma or space
   *  the return value is guaranteed >= pos
   */
  private int nextCommaOrSpace()
  {
    int next = pos;
    while ( next < len && val.charAt(next) != ',' && val.charAt(next) != ' ' ) ++next; 
    return next;
  }

  /** @return the next item on the scanline as a string
   */
  String stringValue( ) 
  {
    int quote = val.charAt( pos ); // quote character
    ++pos; // skip quote character
    int next_pos = nextQuote( quote );
    String ret = (pos == next_pos )? "" : val.substring(pos, next_pos );
    // TDLog.v( "SCANLINE stringValue " + pos + " " + next_pos + " <" + ret + ">" );
    pos = (next_pos < len )? next_pos + 1 : len;
    skipCommaAndSpaces( );
    return ret;
  }

  /** @return the next item on the scanline as a long
   * @param ret default return value
   */
  long longValue( long ret )
  {
    int next_pos = nextCommaOrSpace( );
    // TDLog.v( "SCANLINE longValue " + pos + " " + next_pos + " " + len + " <" + val.substring(pos,next_pos) + ">" );
    if ( pos < next_pos ) {
      String toParse = val.substring( pos, next_pos ); // N.B. next_pos >= pos --> toParse != null
      if ( ! toParse.equals("\"null\"") ) {
        try {
          ret = Long.parseLong( val.substring( pos, next_pos ) );
        } catch ( NumberFormatException e ) {
          TDLog.Error( "longValue error: " + val.substring( pos, next_pos ) );
        }
      }
      pos = next_pos;
      skipCommaAndSpaces( );
    } else {
      TDLog.Error( "longValue pos error: " + val + " " + pos + " " + next_pos );
    }
    return ret;
  }

  /** @return the next item on the scanline as a double
   * @param ret default return value
   */
  double doubleValue( double ret )
  {
    int next_pos = nextCommaOrSpace( );
    if ( pos < next_pos ) {
      try {
        ret = Double.parseDouble( val.substring(pos, next_pos ) );
        // TDLog.Log( TDLog.LOG_DB, "doubleValue " + pos + " " + next_pos + " " + len + " <" + val.substring(pos,next_pos) + ">" );
      } catch ( NumberFormatException e ) {
        TDLog.Error( "doubleValue error: " + val.substring(pos, next_pos) );
      }
      pos = next_pos;
      skipCommaAndSpaces( );
    } else {
      TDLog.Error( "doubleValue pos error: " + val + " " + pos + " " + next_pos );
    }
    return ret;
  }

}
