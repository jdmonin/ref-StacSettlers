/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package originalsmartsettlers.boardlayout;

import java.awt.*;

import originalsmartsettlers.util.*;

/**
 *
 * @author szityu
 */
public class Vertex implements HexTypeConstants 
{

    public Vector2d centerpoint;
    public Point screenCoord;
    public int debugLRstatus;
    
    public Vertex(Vector2d p)
    {
        centerpoint = p;
        
//        screenCoord = new Point[2];
        // screenCoord values are set externally by BoardLayout!
    }
}
