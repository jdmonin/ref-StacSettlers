/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2017 Ruud Poutsma <rtimon@gmail.com>
 * Portions of this file Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2017,2020 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/

package soc.game;


/**
 * The classic 4-player board with 19 land hexes, surrounded by a ring of
 * 18 sea hexes/ports. Implements {@link SOCBoard#BOARD_ENCODING_ORIGINAL}.
 * @since 2.0.00
 */
public class SOCBoard4p extends SOCBoard
{
    private static final long serialVersionUID = 2000L;  // last structural change v2.0.00

    /**
     * Land-hex coordinates in original classic board ({@link #BOARD_ENCODING_ORIGINAL}).
     * @since 1.1.08
     */
    public final static int[] HEXCOORDS_LAND_V1 =
    {
        0x33, 0x35, 0x37, 0x53, 0x55, 0x57, 0x59, 0x73, 0x75, 0x77, 0x79, 0x7B,
        0x95, 0x97, 0x99, 0x9B, 0xB7, 0xB9, 0xBB
    };

    /**
     * Land hex types on the original 4-player board layout (v1).
     * For more information see {@link #makeNewBoard_placeHexes(int[], int[], int[], SOCGameOption)}.
     * @since 2.0.00
     */
    public static final int[] makeNewBoard_landHexTypes_v1 =
        { DESERT_HEX, CLAY_HEX, CLAY_HEX, CLAY_HEX,
            ORE_HEX, ORE_HEX, ORE_HEX,
            SHEEP_HEX, SHEEP_HEX, SHEEP_HEX, SHEEP_HEX,
            WHEAT_HEX, WHEAT_HEX, WHEAT_HEX, WHEAT_HEX,
            WOOD_HEX, WOOD_HEX, WOOD_HEX, WOOD_HEX };

    /**
     * Dice numbers in the original 4-player board layout, in order along {@code numPath}
     * ({@link #makeNewBoard_numPaths_v1}).
     * For more information see {@link #makeNewBoard_placeHexes(int[], int[], int[], SOCGameOption)}.
     * @since 2.0.00
     */
    public static final int[] makeNewBoard_diceNums_v1 =
        { 5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8, 10, 9, 4, 5, 6, 3, 11 };

    /**
     * Each port's type, such as {@link #SHEEP_PORT}, on classic board.
     * Same order as {@link #PORTS_FACING_V1}. {@link #MISC_PORT} is 0.
     * @since 1.1.08
     */
    public final static int[] PORTS_TYPE_V1 =
        //{ 0, 0, 0, 0, CLAY_PORT, ORE_PORT, SHEEP_PORT, WHEAT_PORT, WOOD_PORT };
        { 0, SHEEP_PORT, 0, 0, CLAY_PORT, WOOD_PORT, 0, WHEAT_PORT, ORE_PORT }; //this is the correct order for the standard map from the rules book

    /**
     * Each port's hex number within {@link #hexLayout} on classic board.
     * Same order as {@link #PORTS_FACING_V1}:
     * Clockwise from upper-left (hex coordinate 0x17).
     * @since 1.1.08
     */
    final static int[] PORTS_HEXNUM_V1 = { 0, 2, 8, 21, 32, 35, 33, 22, 9 };

    /**
     * Each port's <em>facing</em> towards land, on the classic board.
     * Ordered clockwise from upper-left (hex coordinate 0x17).
     * Port Facing is the direction from the port hex/edge, to the land hex touching it
     * which will have 2 nodes where a port settlement/city can be built.
     * Facing 2 is east ({@link #FACING_E}), 3 is SE, 4 is SW, etc; see {@link #hexLayout}.
     * @since 1.1.08
     */
    final static int[] PORTS_FACING_V1 =
    {
        FACING_SE, FACING_SW, FACING_SW, FACING_W, FACING_NW, FACING_NW, FACING_NE, FACING_E, FACING_E
    };

    /**
     * Each port's 2 node coordinates on classic board.
     * Same order as {@link #PORTS_FACING_V1}:
     * Clockwise from upper-left (hex coordinate 0x17).
     * @since 1.1.08
     */
    final static int[] PORTS_EDGE_V1 =
    {
        0x27,  // Port touches the upper-left land hex, port facing land to its SouthEast
        0x5A,  // Touches middle land hex of top row, port facing SW
        0x9C,  // Touches rightmost land hex of row above middle, SW
        0xCC,  // Rightmost of middle-row land hex, W
        0xC9,  // Rightmost land hex below middle, NW
        0xA5,  // Port touches middle hex of bottom row, facing NW
        0x72,  // Leftmost of bottom row, NE
        0x42,  // Leftmost land hex of row below middle, E
        0x24   // Leftmost land hex above middle, facing E
    };

    /**
     * Possible number paths for 4-player original board.
     * {@link #makeNewBoard(SOCGameOptionSet)} randomly chooses one path (one 1-dimensional array)
     * to be used as <tt>numPath[]</tt> in
     * {@link #makeNewBoard_placeHexes(int[], int[], int[], SOCGameOption)}.
     */
    final static int[][] makeNewBoard_numPaths_v1 =
    {
        // Numbers are indexes within hexLayout (also in numberLayout) for each land hex.
        // See the hexLayout javadoc for how the indexes are arranged on the board layout.

        // counterclockwise from southwest
        {
            29, 30, 31, 26, 20, 13, 7, 6, 5, 10, 16, 23,  // outermost hexes
            24, 25, 19, 12, 11, 17,    18  // middle ring, center hex
        },

        // clockwise from southwest
        {
            29, 23, 16, 10, 5, 6, 7, 13, 20, 26, 31, 30,
            24, 17, 11, 12, 19, 25,    18
        },

        // counterclockwise from east corner
        {
            20, 13, 7, 6, 5, 10, 16, 23, 29, 30, 31, 26,
            19, 12, 11, 17, 24, 25,    18
        },

        // clockwise from east corner
        {
            20, 26, 31, 30, 29, 23, 16, 10, 5, 6, 7, 13,
            19, 25, 24, 17, 11, 12,    18
        },

        // counterclockwise from northwest
        {
            5, 10, 16, 23, 29, 30, 31, 26, 20, 13, 7, 6,
            11, 17, 24, 25, 19, 12,    18
        },

        // clockwise from northwest
        {
            5, 6, 7, 13, 20, 26, 31, 30, 29, 23, 16, 10,
            11, 12, 19, 25, 24, 17,    18
        }
    };

    public SOCBoard4p(final SOCGameOptionSet gameOpts)
    {
        super(gameOpts, 4, BOARD_ENCODING_ORIGINAL);

        minEdge = MINEDGE_V1;
        maxEdge = MAXEDGE_V1;
        minNode = MINNODE_V1;
    }

    @Override
    public int[] getPortsFacing()
    {
        return SOCBoard4p.PORTS_FACING_V1;
    }

    @Override
    public int[] getPortsEdges()
    {
        return SOCBoard4p.PORTS_EDGE_V1;
    }

    @Override
    public int[] getLandHexCoords()
    {
        return SOCBoard4p.HEXCOORDS_LAND_V1;
    }

    @Override
    public int getPortsCount()
    {
        return SOCBoard4p.PORTS_FACING_V1.length;
    }

}
