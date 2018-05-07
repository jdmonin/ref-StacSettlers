/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2008-2010 Jeremy D Monin <jeremy@nand.net>
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
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.message;

import java.util.StringTokenizer;


/**
 * This message contains a text message for a SoC game.
 * Seen by {@link soc.server.SOCServer#SERVERNAME server} or by
 * human players on-screen, occasionally parsed by robots
 * if they're expecting something.
 *
 * @author Robert S Thomas
 */
public class SOCGameTextMsg extends SOCMessage
    implements SOCMessageForGame
{
    /**
     * our token seperator; not the normal {@link SOCMessage#sep2}
     */
    private static String sep2 = "" + (char) 0;

    /**
     * Name of game
     */
    private String game;

    /**
     * Nickname of sender
     */
    private String nickname;

    /**
     * Text message
     */
    private String text;

    /**
     * Create a GameTextMsg message.
     *
     * @param ga  name of game
     * @param nn  nickname of sender
     * @param tm  text message
     */
    public SOCGameTextMsg(String ga, String nn, String tm)
    {
        messageType = GAMETEXTMSG;
        game = ga;
        nickname = nn;
        text = tm;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the nickname
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * @return the text message
     */
    public String getText()
    {
        return text;
    }

    /**
     * GAMETEXTMSG sep game sep2 nickname sep2 text
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(game, nickname, text);
    }

    /**
     * GAMETEXTMSG sep game sep2 nickname sep2 text
     *
     * @param ga  the game name
     * @param nn  the nickname
     * @param tm  the text message
     * @return    the command string
     */
    public static String toCmd(String ga, String nn, String tm)
    {
        return GAMETEXTMSG + sep + ga + sep2 + nn + sep2 + tm;
    }

    /**
     * Parse the command String into a GameTextMsg message
     *
     * @param s   the String to parse
     * @return    a GameTextMsg message, or null of the data is garbled
     */
    public static SOCGameTextMsg parseDataStr(String s)
    {
        String ga;
        String nn;
        String tm;

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            nn = st.nextToken();
            tm = st.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCGameTextMsg(ga, nn, tm);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        String s = "SOCGameTextMsg:game=" + game + "|nickname=" + nickname + "|text=" + text;

        return s;
    }
    
    // This uses special separators, presumably to handle special chars in the message itself.  Only replace the first two commas - everything after that is in the message
    public static String stripAttribNames(String str) {
    	String ret = SOCMessage.stripAttribNames(str);
    	for (int i=0; i<2; i++) {
    		ret = ret.replaceFirst(SOCMessage.sep2, sep2);
    	}
    	return ret;
    }
    
    /**
     * Create an XML representation of the message.
     * This is intended to be used for exchanging information with the Toulouse parser.
     * @return string with an XML representation of the message
     */
    public String toXML() {
        return "<game-text-message><game>" + game + "</game><nickname>" + nickname + "</nickname><text>" + text + "</text></game-text-message>";
    }
}