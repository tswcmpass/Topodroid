/* @file DrawingShp.java
 *
 * @author marco corvi
 * @date mar 2013
 *
 * @brief TopoDroid drawing: shapefile export
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.io.shp;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDFile;
import com.topodroid.utils.TDMath;
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.PlotType;
// import com.topodroid.num.NumStation;
import com.topodroid.TDX.Archiver;
import com.topodroid.TDX.GeoReference;
import com.topodroid.TDX.ICanvasCommand;
import com.topodroid.TDX.DrawingPath;
import com.topodroid.TDX.DrawingPointPath;
import com.topodroid.TDX.DrawingAudioPath;
import com.topodroid.TDX.DrawingPhotoPath;
import com.topodroid.TDX.DrawingPointLinePath;
import com.topodroid.TDX.DrawingLinePath;
import com.topodroid.TDX.DrawingAreaPath;
import com.topodroid.TDX.DrawingCommandManager;
import com.topodroid.TDX.DrawingStationName;
import com.topodroid.TDX.BrushManager;

import java.util.List;
import java.util.ArrayList;

// import java.io.File;
import java.io.OutputStream;
import java.io.IOException;

public class DrawingShp
{
  // @param fos        output stream
  // @param dirname    shp temporary dir (should compose "survey/shp/tmp")
  // @param plot       sketch items
  // @param type       sketch type
  // @param station    WGS84 data of the sketch origin 
  // @return true if successful
  public static boolean writeShp( OutputStream fos, String dirname, DrawingCommandManager plot, long type, GeoReference station )
  {
    // TDLog.v( "SHP sketch export dirname " + dirname );
    double xoff = 0;
    double yoff = 0;
    double xscale = ShpObject.SCALE;
    double yscale = ShpObject.SCALE;
    float cd = 1;
    float sd = 0;
    float decl = 0;
    if ( station != null && TDSetting.mShpGeoref ) {
      xoff = station.ge;
      yoff = station.gs;
      xscale = ShpObject.SCALE * station.eradius; // use only S-radius FIXME
      yscale = ShpObject.SCALE * station.sradius;
      decl = station.declination; // N.B. station.declination can include -convergence
      cd = TDMath.cosd( decl ); 
      sd = TDMath.sind( decl );
    }

    if ( ! TDFile.makeMSdir( dirname ) ) {
      TDLog.Error("mkdir " + dirname + " error");
      return false;
    }
    // TDLog.v( "mkdir created MS-dir " + dirname );
    ArrayList< String > files = new ArrayList<>();

    try {
      // centerline data: shape-file of segments (fields: type, from, to)
      // xoff+sh.x1, yoff+sh.y1  --  xoff+sh.x2, yoff+sh.y2
      ArrayList< DrawingPath > shots = new ArrayList<>();
      if ( PlotType.isSketch2D( type ) ) { 
        for ( DrawingPath sh : plot.getLegs() ) {
          if ( sh.mBlock != null ) shots.add( sh );
        }
        for ( DrawingPath sh : plot.getSplays() ) {
          if ( sh.mBlock != null ) shots.add( sh );
        }
      }
      ShpSegment shp_shot = new ShpSegment( dirname, "shot", files );

      shp_shot.writeSegments( shots, xoff, yoff, xscale, yscale, cd, sd );

      // points shapefile
      ArrayList< DrawingPointPath > points     = new ArrayList<>();
      ArrayList< DrawingPointPath > extras     = new ArrayList<>();
      ArrayList< DrawingPointLinePath > lines  = new ArrayList<>();
      ArrayList< DrawingPointLinePath > areas  = new ArrayList<>();
      ArrayList< Link > links = (TDSetting.mAutoXSections ? new ArrayList<>() : null );
      for ( ICanvasCommand cmd : plot.getCommands() ) {
        if ( cmd.commandType() != 0 ) continue;
        DrawingPath path = (DrawingPath)cmd;
        if ( path.mType == DrawingPath.DRAWING_PATH_POINT ) {
          DrawingPointPath point = (DrawingPointPath)path;
          if ( point instanceof DrawingAudioPath ) {
            extras.add( point );
          } else if ( point instanceof DrawingPhotoPath ) {
            extras.add( point );
          } else {
            if ( BrushManager.isPointSection( point.mPointType ) ) {
              extras.add( point );
            } else {
              points.add( point ); // xoff+cx, yoff+cy
            }
          }
        } else if ( path.mType == DrawingPath.DRAWING_PATH_LINE ) {
          lines.add( (DrawingLinePath)path );  // xoff+pt.x, yoff+pt.y
        } else if ( path.mType == DrawingPath.DRAWING_PATH_AREA ) {
          areas.add( (DrawingAreaPath)path );
        }
      }
      ShpPoint shp_point = new ShpPoint( dirname, "point", files );
      shp_point.writePoints( points, xoff, yoff, xscale, yscale, cd, sd, decl );
      ShpExtra shp_extra = new ShpExtra( dirname, "extra", files );
      shp_extra.writeExtras( extras, xoff, yoff, xscale, yscale, cd, sd, links );
      ShpPolyline shp_line = new ShpPolyline( dirname, "line", DrawingPath.DRAWING_PATH_LINE, files );
      shp_line.writeLines( lines, xoff, yoff, xscale, yscale, cd, sd );
      ShpPolyline shp_area = new ShpPolyline( dirname, "area", DrawingPath.DRAWING_PATH_AREA, files );
      shp_area.writeAreas( areas, xoff, yoff, xscale, yscale, cd, sd );

      if ( links != null && links.size() > 0 ) {
        ShpLink shp_link = new ShpLink( dirname, "link", files );
        shp_link.writeLinks( links, xoff, yoff, xscale, yscale, cd, sd );
      }

      // stations: xoff+name.cx, yoff+name.cy
      List< DrawingStationName > stations = plot.getStations();
      ShpStation shp_station = new ShpStation( dirname, "station", files );
      shp_station.writeStations( stations, xoff, yoff, xscale, yscale, cd, sd );

      // (new Archiver()).compressFiles( "shp", filename + ".shz", dirname, files );
      (new Archiver()).compressFiles( fos, dirname, files );

    } catch ( IOException e ) {
      TDLog.Error( "SHP io-exception " + e.getMessage() );
      return false;
    } finally {
      TDFile.deleteMSdir( dirname ); // delete temporary shape-dir
    }
    return true;
  }

}


