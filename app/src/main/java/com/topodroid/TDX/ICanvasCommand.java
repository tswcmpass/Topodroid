/* @file ICanvasCommand.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid drawing: canvas command interface
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

/* interface for the canvas commands
 */
public interface ICanvasCommand 
{
    /** @return the type of the command
     */
    int  commandType(); // command type: 0 DrawingPath, 1 EraseCommand, 3 RetraceCommand

    void draw(Canvas canvas, RectF bbox );
    void draw(Canvas canvas, Matrix mat, RectF bbox );
    void draw(Canvas canvas, Matrix mat, float scale, RectF bbox );
    void draw(Canvas canvas, Matrix mat, float scale, RectF bbox, int xor_color );

    // public void undoCommand();

    void flipXAxis(float z);
    void shiftPathBy( float x, float y );
    void scalePathBy( float z, Matrix m );
    void affineTransformPathBy( float[] mm, Matrix m );
    void computeBounds( RectF bounds, boolean b );

}
