/* @file ShpPoint.java
 *
 * @author marco corvi
 * @date mar 2019
 *
 * @brief TopoDroid drawing: shapefile 2D point
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.io.shp;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDString;
import com.topodroid.utils.TDUtil;
// import com.topodroid.num.NumStation;
// import com.topodroid.num.NumShot;
//  import com.topodroid.num.NumSplay;
import com.topodroid.TDX.DrawingPointPath;
import com.topodroid.TDX.DrawingAudioPath;
import com.topodroid.TDX.DrawingPhotoPath;
import com.topodroid.TDX.DrawingUtil;
import com.topodroid.TDX.BrushManager;
import com.topodroid.TDX.TDPath;
import com.topodroid.TDX.TDInstance;
import com.topodroid.ui.ExifInfo;


// import java.io.File;
// import java.io.FileOutputStream;
// import java.io.FileNotFoundException;
import java.io.IOException;   

// import java.nio.ByteBuffer;
// import java.nio.MappedByteBuffer;
import java.nio.ByteOrder;   
// import java.nio.channels.FileChannel;

import java.util.List;

public class ShpExtra extends ShpObject
{
  /** cstr
   * @param subdir   folder
   * @param path     filename
   * @param files    array of files to add names of written files
   */
  public ShpExtra( String subdir, String path, List< String > files ) // throws IOException
  {
    super( SHP_EXTRA, subdir, path, files );
  }

  /** write headers for EXTRA
   * @param pts    extra points
   * @param x0     X offset
   * @param y0     Y offset
   * @param xscale X scale
   * @param yscale Y scale
   * @param cd     cosine of declination angle
   * @param sd     sine of declination angle
   * @param links  array of links - to be filled if not null
   */
  public boolean writeExtras( List< DrawingPointPath > pts, double x0, double y0, double xscale, double yscale, double cd, double sd, List< Link > links ) throws IOException
  {
    int n_pts = (pts != null)? pts.size() : 0;
    // TDLog.v( "SHP write points " + n_pts );
    if ( n_pts == 0 ) return false;

    int n_fld = 7;
    String[] fields = new String[ n_fld ];
    fields[0] = "name";
    fields[1] = "orient";
    fields[2] = "scale";
    fields[3] = "levels";
    fields[4] = "scrap";
    fields[5] = "text";
    fields[6] = "file";
    byte[]   ftypes = { BYTEC, BYTEN, BYTEN, BYTEC, BYTEC, BYTEC, BYTEC };
    int[]    flens  = { SIZE_NAME, SIZE_ORIENT, SIZE_SCALE, SIZE_LEVELS, SIZE_SCRAP, SIZE_TEXT, SIZE_TEXT }; // 16, 6, 6, 6, 6, 128, 128

    int shpRecLen = getShpRecordLength( );
    int shxRecLen = getShxRecordLength( );
    int dbfRecLen = 1; // Bytes
    for (int k=0; k<n_fld; ++k ) dbfRecLen += flens[k]; 

    int shpLength = 50 + n_pts * shpRecLen; // [16-bit words]
    int shxLength = 50 + n_pts * shxRecLen;
    int dbfLength = 33 + n_fld * 32 + n_pts * dbfRecLen; // [Bytes]

    setBoundsPoints( pts, x0, y0, xscale, yscale, cd, sd );
    // TDLog.v( "POINT " + pts.size() + " len " + shpLength + " / " + shxLength + " / " + dbfLength );
    // TDLog.v( "bbox X " + xmin + " " + xmax );

    open();
    resetChannels( 2*shpLength+8, 2*shxLength+8, dbfLength );

    shpBuffer = writeShapeHeader( shpBuffer, SHP_POINT, shpLength );
    shxBuffer = writeShapeHeader( shxBuffer, SHP_POINT, shxLength );
    writeDBaseHeader( n_pts, dbfRecLen, n_fld, fields, ftypes, flens );
    // TDLog.v( "EXTRA done headers - nr " + pts.size() );

    int cnt = 0;
    for ( DrawingPointPath pt : pts ) {
      int offset = 50 + cnt * shpRecLen; 
      writeShpRecordHeader( cnt, shpRecLen );
      shpBuffer.order(ByteOrder.LITTLE_ENDIAN);   
      shpBuffer.putInt( SHP_POINT );
      // TDLog.v( "POINT " + cnt + ": " + pt.cx + " " + pt.cy + " cd " + cd + " sd " + sd + " scale " + xscale + " " + yscale );
      double x = DrawingUtil.declinatedX( pt.cx, pt.cy, cd, sd );
      double y = DrawingUtil.declinatedY( pt.cx, pt.cy, cd, sd );
      shpBuffer.putDouble( x0 + xscale * x );
      shpBuffer.putDouble( y0 - yscale * y );

      // TDLog.v( "POINT " + cnt + ": " + pt.getThName() +  " orient " +  (int)pt.mOrientation + " scale " +  pt.getScale() + " level " + pt.mLevel + " scrap " + pt.mScrap );
      writeShxRecord( offset, shpRecLen );
      fields[0] = pt.getThName( );
      if ( BrushManager.isPointPhoto( pt.mPointType ) ) {
        DrawingPhotoPath photo = (DrawingPhotoPath)pt;
        long photo_id = photo.getId(); // filepath = id.jpg
        String filepath = TDPath.getJpgFile( photo_id + ".jpg" );
        ExifInfo exif = new ExifInfo( filepath );
        fields[1] = new String( blankPadded( (int)(exif.azimuth()), SIZE_ORIENT ) );
        // TDLog.v("SHP photo id " + photo_id + " path " + filepath + " orientation " + exif.azimuth() );
      } else {
        fields[1] = new String( blankPadded( (int)pt.mOrientation, SIZE_ORIENT ) ); 
      }
      fields[2] = new String( blankPadded( pt.getScale(), SIZE_SCALE ) );
      fields[3] = Integer.toString( pt.mLevel );
      fields[4] = Integer.toString( pt.mScrap ); 
      fields[5] = pt.getPointText(); 
      fields[6] = "";
      if ( pt instanceof DrawingAudioPath ) {
        fields[6] = TDPath.getSurveyWavFilename( TDInstance.survey, Long.toString( ((DrawingAudioPath)pt).getId()) );
      } else if ( pt instanceof DrawingPhotoPath ) {
        fields[6] = TDPath.getSurveyJpgFilename( TDInstance.survey, Long.toString( ((DrawingPhotoPath)pt).getId()) );
      } else {
        if ( BrushManager.isPointSection( pt.pointType() ) ) {
          fields[6] = TDUtil.replacePrefix( TDInstance.survey, pt.getOption(TDString.OPTION_SCRAP) ); 
          if ( links != null && pt.mLink != null ) {
            links.add( new Link( pt ) );
          }
        }
      }
      if ( fields[3] == null ) fields[3] = "";
      if ( fields[4] == null ) fields[4] = "";
      if ( fields[5] == null ) fields[5] = "";
      writeDBaseRecord( n_fld, fields, flens );
      ++cnt;
    }
    // TDLog.v( "POINT done records");
    close();
    return true;
  }

  /** @return  record length [words]: 4 + 20/2 = 14
   */
  @Override protected int getShpRecordLength( ) { return 14; }
    
  /** Utility: set the bounding box of the set of geometries
   * @param pts    extra points
   * @param x0     X offset
   * @param y0     Y offset
   * @param xscale X scale
   * @param yscale Y scale
   * @param cd     cosine of declination angle
   * @param sd     sine of declination angle
   */
  private void setBoundsPoints( List< DrawingPointPath > pts, double x0, double y0, double xscale, double yscale, double cd, double sd ) 
  {
    if ( pts.size() == 0 ) {
      xmin = xmax = ymin = ymax = zmin = zmax = 0.0;
      return;
    }
    DrawingPointPath pt = pts.get(0);
    double x = DrawingUtil.declinatedX( pt.cx, pt.cy, cd, sd );
    double y = DrawingUtil.declinatedY( pt.cx, pt.cy, cd, sd );
    initBBox( x0 + xscale * x, y0 - yscale * y );
    for ( int k=pts.size() - 1; k>0; --k ) {
      pt = pts.get(k);
      x = DrawingUtil.declinatedX( pt.cx, pt.cy, cd, sd );
      y = DrawingUtil.declinatedY( pt.cx, pt.cy, cd, sd );
      updateBBox( x0 + xscale * x, y0 - yscale * y );
    }
  }
}
