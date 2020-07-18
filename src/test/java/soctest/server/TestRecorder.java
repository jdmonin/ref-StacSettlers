/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2020 Jeremy D Monin <jeremy@nand.net>
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

package soctest.server;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import soc.game.SOCBoardLarge;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCMoveRobberResult;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;
import soc.message.SOCBuildRequest;
import soc.message.SOCChoosePlayer;
import soc.server.SOCGameListAtServer;
import soc.server.SOCServer;
import soc.server.genericServer.Connection;
import soc.server.savegame.SavedGameModel;
import soc.util.Version;
import soctest.server.RecordingTesterServer.QueueEntry;
import soctest.server.savegame.TestLoadgame;

/**
 * A few tests for {@link SOCServer#recordGameEvent(String, soc.message.SOCMessage)} and similar methods,
 * using {@link RecordingTesterServer} and its {@link QueueEntry} format.
 * Covers a few core game actions and message sequences. For more complete coverage of those,
 * you should periodically run {@code extraTest} {@code soctest.server.TestActionsMessages}.
 *<P>
 * Also has convenience methods like
 * {@link #connectLoadJoinResumeGame(RecordingTesterServer, String, String, int, boolean, boolean, boolean)}
 * and {@link #compareRecordsToExpected(List, String[][])} which other test classes can use.
 *
 * @since 2.4.10
 */
public class TestRecorder
{
    private static RecordingTesterServer srv;

    /**
     * Client name tracking, to prevent accidentally using same name in 2 test methods:
     * That would intermittently cause auth problems for a previously-stable test
     * if one logged out before/while the other was testing.
     */
    private static Set<String> clientNamesUsed = new HashSet<>();

    @BeforeClass
    public static void startStringportServer()
    {
        srv = new RecordingTesterServer();
        srv.setPriority(5);  // same as in SOCServer.main
        srv.start();

        // wait for startup
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {}
    }

    @AfterClass
    public static void shutdownServer()
    {
        // for clearer sequences in System.out, wait for other threads' prints to complete
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException e) {}

        System.out.flush();
        System.out.println();
        srv.stopServer();
    }

    /**
     * Resume a game after loading it. Prints player connection details to {@link System#out},
     * calls {@link SOCServer#resumeReloadedGame(Connection, SOCGame)}.
     * @param ga  Loaded game to resume
     * @param server  Server to resume in
     * @param cliConn  Client connection; should already be a member of this game at server
     */
    public static void resumeLoadedGame(final SOCGame ga, final SOCServer server, final Connection cliConn)
    {
        if (server == null)
            throw new IllegalArgumentException("server");
        if (cliConn == null)
            throw new IllegalArgumentException("cliConn");

        final String gaName = ga.getName();
        final SOCGameListAtServer glas = server.getGameList();

        assertNotNull("cliConn is authed and named: game " + gaName, cliConn.getData());

        // output all at once, in case of parallel tests
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- Resuming loaded game: " + gaName + "\n");
        for (int pn = 0; pn < 4; ++pn)
        {
            if (ga.isSeatVacant(pn))
                continue;
            SOCPlayer pl = ga.getPlayer(pn);
            String plName = pl.getName();
            if ((plName == null) || plName.isEmpty())
            {
                sb.append("pn[" + pn + "] ** empty\n");
                continue;
            }
            Connection plConn = server.getConnection(plName);
            sb.append("pn[" + pn + "] name=" + plName + ", isRobot=" + pl.isRobot()
                + ", hasConn=" + (null != plConn) + ", isMember=" + glas.isMember(plConn, gaName) + "\n");
            if (plConn != null)
                assertEquals("pn[" + pn + "] connection named: game " + gaName, plName, plConn.getData());
        }
        System.out.flush();
        System.out.println(sb);
        System.out.flush();
        assertTrue("resume loaded game", server.resumeReloadedGame(cliConn, ga));
    }

    /**
     * Test the basics, to rule out problems with that if other tests fail:
     *<UL>
     * <LI> {@link RecordingTesterServer} is up
     * <LI> {@link SOCServer#recordGameEventsIsActive()} is true
     * <LI> Bots are connected to test server
     * <LI> {@link DisplaylessTesterClient} can connect and see server's version
     * <LI> Server sees test client connection
     *</UL>
     */
    @Test
    public void testBasics_ServerUpWithBotsConnectClient()
    {
        // unique client nickname, in case tests run in parallel
        final String CLIENT_NAME = "testServerUp";

        assertNotNull(srv);
        assertEquals(RecordingTesterServer.STRINGPORT_NAME, srv.getLocalSocketName());

        assertTrue("recordGameEvents shouldn't be stubbed out", srv.recordGameEventsIsActive());

        final int nConn = srv.getNamedConnectionCount();
        assertTrue
            ("some bots are connected; actual nConn=" + nConn, nConn >= RecordingTesterServer.NUM_STARTROBOTS);

        DisplaylessTesterClient tcli = new DisplaylessTesterClient
            (RecordingTesterServer.STRINGPORT_NAME, CLIENT_NAME);
        tcli.init();
        try { Thread.sleep(120); }
        catch(InterruptedException e) {}

        assertEquals("get version from test SOCServer", Version.versionNumber(), tcli.getServerVersion());
        Connection tcliAtServer = srv.getConnection(CLIENT_NAME);
        assertNotNull(tcliAtServer);

        tcli.destroy();
    }

    /**
     * Test the basics, to rule out problems with that if other tests fail:
     * Load a game, server should invite test client to join it because of player name.
     */
    @Test
    public void testBasics_Loadgame()
        throws IOException
    {
        assertNotNull(srv);
        testBasics_Loadgame(srv);
    }

    /**
     * Test the basics, to rule out problems with that if other tests fail:
     * Load a game, server should invite test client to join it because of player name.
     * Parameterized for use from other test/extraTest classes.
     *
     * @param server  Server to use
     * @throws IOException if problem occurs during {@link TestLoadgame#load(String, SOCServer)}
     */
    public static void testBasics_Loadgame(SOCServer server)
        throws IOException
    {
        if (server == null)
            throw new IllegalArgumentException("server");

        // unique client nickname, in case tests run in parallel
        final String CLIENT_NAME = "testLoadgame";

        assertNotNull(server);

        DisplaylessTesterClient tcli = new DisplaylessTesterClient
            (RecordingTesterServer.STRINGPORT_NAME, CLIENT_NAME);
        tcli.init();
        try { Thread.sleep(120); }
        catch(InterruptedException e) {}

        assertEquals("get version from test SOCServer", Version.versionNumber(), tcli.getServerVersion());

        SavedGameModel sgm = TestLoadgame.load("classic-botturn.game.json", server);
        assertNotNull(sgm);
        assertEquals("classic", sgm.gameName);
        assertEquals("debug", sgm.playerSeats[3].name);
        sgm.playerSeats[3].name = CLIENT_NAME;

        Connection tcliConn = server.getConnection(CLIENT_NAME);
        assertNotNull(tcliConn);
        String loadedName = server.createAndJoinReloadedGame(sgm, tcliConn, null);
        assertNotNull("reloaded game name", loadedName);

        // wait to join in client's thread
        try { Thread.sleep(120); }
        catch(InterruptedException e) {}

        assertTrue("debug cli member of reloaded game?", server.getGameList().isMember(tcliConn, loadedName));

        final SOCGame ga = server.getGame(loadedName);
        assertNotNull("game object at server", ga);
        final int PN = 3;
        assertEquals(1, ga.getCurrentPlayerNumber());
        final SOCPlayer cliPl = ga.getPlayer(PN);
        assertEquals(CLIENT_NAME, cliPl.getName());

        // leave game
        tcli.destroy();
    }

    /**
     * Common code to use when beginning a test:
     *<UL>
     * <LI> Assert {@code server} not null
     * <LI> Connect to test server with a new client
     * <LI> Optionally connect with another new client
     * <LI> Load 4-player game artifact {@code "message-seqs.game.json"}; server will connect client and robots to it
     * <LI> Confirm and retrieve {@link SOCGame} and client {@link SOCPlayer} info
     * <LI> Override all players' {@link SOCPlayer#isRobot()} flags to test varied server response message sequences
     * <LI> Optionally resume the game
     * <LI> Will be client player's turn (player number 3) and game state {@link SOCGame#PLAY1 PLAY1}
     *</UL>
     * When {@code clientAsRobot}, the client's locale and i18n manager are cleared to null as a bot's would be.
     *<P>
     * Limitation: Even when ! {@code othersAsRobot}, those fields can't be set non-null for robot clients because
     * those bot connections are also in other games which may have a different value for {@code othersAsRobot}.
     *
     * @param clientName  Unique client name to use for this client and game; will sit at player number 3
     * @param client2Name  Optional unique second client name to use, or null
     * @param client2PN    Player number if using {@code client2Name}: Either 1 or 2 (0 is vacant, 3 is first client).
     *     If {@code client2Name} is null, should be 0.
     * @param withResume  If true, call {@link #resumeLoadedGame(SOCGame, SOCServer, Connection)}
     * @param clientAsRobot  Whether to mark client player as robot before resuming game:
     *     Calls {@link SOCPlayer#setRobotFlag(boolean, boolean)}
     *     and {@link Connection#setI18NStringManager(soc.util.SOCStringManager, String)}
     * @param othersAsRobot  Whether to mark other players as robot before resuming game:
     *     Calls {@link SOCPlayer#setRobotFlag(boolean, boolean)}
     * @return  all the useful objects mentioned above
     * @throws IllegalArgumentException if {@code clientName} is null or too long
     *     (max length is {@link SOCServer#PLAYER_NAME_MAX_LENGTH})
     * @throws IllegalStateException if {@code clientName} or {@code client2Name} was already used for
     *     a different call to this method: Use unique names for each call to avoid intermittent auth problems.
     *     Also thrown if {@code client2PN} isn't 1 or 2 when {@code client2Name != null}.
     * @throws IOException if game artifact file can't be loaded
     */
    public static StartedTestGameObjects connectLoadJoinResumeGame
        (final RecordingTesterServer server, final String clientName, final String client2Name, final int client2PN,
         final boolean withResume,
         final boolean clientAsRobot, final boolean othersAsRobot)
        throws IllegalArgumentException, IllegalStateException, IOException
    {
        if (clientName == null)
            throw new IllegalArgumentException("clientName");
        if (clientName.length() > SOCServer.PLAYER_NAME_MAX_LENGTH)
            throw new IllegalArgumentException("clientName.length " + clientName.length()
                + ", max is " + SOCServer.PLAYER_NAME_MAX_LENGTH);
        if (client2Name == null)
        {
            if (client2PN != 0)
                throw new IllegalArgumentException("client2PN should be 0 when client2Name null");
        } else {
            if ((client2PN != 1) && (client2PN != 2))
                throw new IllegalArgumentException("client2PN: " + client2PN);
            if (client2Name.equals(clientName))
                throw new IllegalArgumentException("clientName == client2Name: " + clientName);
        }

        synchronized(clientNamesUsed)
        {
            if (clientNamesUsed.contains(clientName))
                throw new IllegalStateException("already used clientName " + clientName);

            clientNamesUsed.add(clientName);

            if (client2Name != null)
            {
                if (clientNamesUsed.contains(client2Name))
                    throw new IllegalStateException("already used client2Name " + client2Name);

                clientNamesUsed.add(client2Name);
            }
        }

        assertNotNull(server);

        final DisplaylessTesterClient tcli = new DisplaylessTesterClient
            (RecordingTesterServer.STRINGPORT_NAME, clientName);
        tcli.init();
        assertEquals(clientName, tcli.getNickname());

        try { Thread.sleep(120); }
        catch(InterruptedException e) {}
        assertEquals("get version from test SOCServer", Version.versionNumber(), tcli.getServerVersion());

        final DisplaylessTesterClient tcli2;
        if (client2Name != null)
        {
            tcli2 = new DisplaylessTesterClient
                (RecordingTesterServer.STRINGPORT_NAME, client2Name);
            tcli2.init();
            assertEquals(client2Name, tcli2.getNickname());

            try { Thread.sleep(120); }
            catch(InterruptedException e) {}
            assertEquals("get version from test SOCServer", Version.versionNumber(), tcli2.getServerVersion());
        } else {
            tcli2 = null;
        }

        SavedGameModel sgm = soctest.server.savegame.TestLoadgame.load("message-seqs.game.json", server);
        assertNotNull(sgm);
        assertEquals("message-seqs", sgm.gameName);
        assertEquals(4, sgm.playerSeats.length);
        assertEquals("debug", sgm.playerSeats[3].name);
        sgm.playerSeats[3].name = clientName;

        final Connection tcliConn = server.getConnection(clientName);
        assertNotNull(tcliConn);
        assertEquals("conn.getData==" + clientName, clientName, tcliConn.getData());

        final Connection tcli2Conn;
        if (client2Name != null)
        {
            tcli2Conn = server.getConnection(client2Name);
            assertNotNull(tcli2Conn);
            assertEquals("conn2.getData==" + client2Name, client2Name, tcli2Conn.getData());
        } else {
            tcli2Conn = null;
        }

        String loadedName = server.createAndJoinReloadedGame(sgm, tcliConn, null);
        assertNotNull("message-seqs game name", loadedName);

        // wait to join in client's thread
        try { Thread.sleep(120); }
        catch(InterruptedException e) {}

        assertTrue("debug cli member of reloaded game?", server.getGameList().isMember(tcliConn, loadedName));

        final SOCGame ga = server.getGame(loadedName);
        assertNotNull("game object at server", ga);
        assertTrue("game uses sea board", ga.getBoard() instanceof SOCBoardLarge);
        assertEquals(SOCGame.PLAY1, sgm.gameState);

        if (tcli2 != null)
        {
            tcli2.askJoinGame(loadedName);

            // wait to join in client's thread
            try { Thread.sleep(150); }
            catch(InterruptedException e) {}

            assertTrue("cli2 member of reloaded game?", server.getGameList().isMember(tcli2Conn, loadedName));

            tcli2.sitDown(ga, client2PN);

            try { Thread.sleep(90); }
            catch(InterruptedException e) {}

            // checks results soon: ga.getPlayer(client2PN) below
        }


        // has N7: roll no 7s, to simplify test assumptions
        SOCGameOption opt = ga.getGameOptions().get("N7");
        assertNotNull(opt);
        assertTrue(opt.getBoolValue());
        assertEquals(99, opt.getIntValue());

        final int PN = 3;
        assertEquals(PN, ga.getCurrentPlayerNumber());
        final SOCPlayer cliPl = ga.getPlayer(PN);
        assertEquals(clientName, cliPl.getName());
        final SOCPlayer cli2Pl = (client2Name != null) ? ga.getPlayer(client2PN) : null;
        if (client2Name != null)
            assertEquals(client2Name, cli2Pl.getName());

        cliPl.setRobotFlag(clientAsRobot, clientAsRobot);
        if (cli2Pl != null)
            cli2Pl.setRobotFlag(clientAsRobot, clientAsRobot);
        if (clientAsRobot)
        {
            tcliConn.setI18NStringManager(null, null);
            if (tcli2Conn != null)
                tcli2Conn.setI18NStringManager(null, null);
        }
        for (int pn = 0; pn < ga.maxPlayers; ++pn)
            if ((pn != PN) && (! ga.isSeatVacant(pn)) && ((client2Name == null) || (pn != client2PN)))
                ga.getPlayer(pn).setRobotFlag(othersAsRobot, othersAsRobot);

        if (withResume)
        {
            resumeLoadedGame(ga, server, tcliConn);
            try { Thread.sleep(120); }
            catch(InterruptedException e) {}

            assertEquals(SOCGame.PLAY1, ga.getGameState());
        }

        final Vector<QueueEntry> records = server.records.get(loadedName);
        assertNotNull("record queue for game", records);

        return new StartedTestGameObjects
            (tcli, tcli2, tcliConn, tcli2Conn, sgm, ga, (SOCBoardLarge) ga.getBoard(), cliPl, cli2Pl, records);
    }

    /**
     * Test loading {@code message-seqs.game.json} and recording a few basic game action sequences,
     * which also test the different {@link SOCServer#recordGameEvent(String, soc.message.SOCMessage)} methods:
     *<UL>
     * <LI> Build a road: Sent to all players
     * <LI> Buy a dev card: Some messages sent to 1 player, or all but 1
     * <LI> Choose and move robber, steal: Some messages sent to all but 2 players
     *</UL>
     */
    @Test
    public void testLoadAndBasicSequences()
        throws IOException
    {
        // unique client nickname, in case tests run in parallel
        final String CLIENT_NAME = "testBasicSequences";

        final StartedTestGameObjects objs = connectLoadJoinResumeGame(srv, CLIENT_NAME, null, 0, true, false, true);
        final DisplaylessTesterClient tcli = objs.tcli;
        // final SavedGameModel sgm = objs.sgm;
        final SOCGame ga = objs.gameAtServer;
        final SOCBoardLarge board = objs.board;
        final SOCPlayer cliPl = objs.clientPlayer;
        final Vector<QueueEntry> records = objs.records;

        /* sequence recording: build road */

        records.clear();
        final int ROAD_COORD = 0x40a;
        assertNull(board.roadOrShipAtEdge(ROAD_COORD));
        assertEquals(12, cliPl.getNumPieces(SOCPlayingPiece.ROAD));
        assertArrayEquals(new int[]{3, 3, 3, 4, 4}, cliPl.getResources().getAmounts(false));
        tcli.putPiece(ga, new SOCRoad(cliPl, ROAD_COORD, board));

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        assertNotNull("road built", board.roadOrShipAtEdge(ROAD_COORD));
        assertEquals(11, cliPl.getNumPieces(SOCPlayingPiece.ROAD));
        assertArrayEquals(new int[]{2, 3, 3, 4, 3}, cliPl.getResources().getAmounts(false));

        // for now, quick rough comparison of record contents
        StringBuilder comparesBuild = compareRecordsToExpected
            (records, new String[][]
            {
                {"all:SOCPlayerElements:", "|playerNum=3|actionType=LOSE|e1=1,e5=1"},
                {"all:SOCGameServerText:", "|text=" + CLIENT_NAME + " built a road."},
                {"all:SOCPutPiece:", "|playerNumber=3|pieceType=0|coord=40a"},
                {"all:SOCGameState:", "|state=20"}
            });

        /* sequence recording: buy dev card */

        records.clear();
        assertEquals(23, ga.getNumDevCards());
        assertEquals(5, cliPl.getInventory().getTotal());
        tcli.buyDevCard(ga);

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        assertEquals(22, ga.getNumDevCards());
        assertEquals(6, cliPl.getInventory().getTotal());

        StringBuilder comparesBuyCard = compareRecordsToExpected
            (records, new String[][]
            {
                {"all:SOCPlayerElements:", "|playerNum=3|actionType=LOSE|e2=1,e3=1,e4=1"},
                {"all:SOCGameElements:", "|e2=22"},
                {"p3:SOCDevCardAction:", "|playerNum=3|actionType=DRAW|cardType=5"},  // type known from savegame devCardDeck
                {"!p3:SOCDevCardAction:", "|playerNum=3|actionType=DRAW|cardType=0"},
                {"all:SOCSimpleAction:", "|pn=3|actType=1|v1=22|v2=0"},
                {"all:SOCGameState:", "|state=20"}
            });

        /* sequence recording: choose and move robber, steal from 1 player */

        records.clear();

        assertFalse(cliPl.hasPlayedDevCard());
        assertEquals(1, cliPl.getNumKnights());
        tcli.playDevCard(ga, SOCDevCardConstants.KNIGHT);

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        assertTrue(cliPl.hasPlayedDevCard());
        assertEquals(2, cliPl.getNumKnights());
        StringBuilder comparesSoldierMoveRobber = moveRobberStealSequence
            (tcli, ga, cliPl, records,
             new String[][]
                {
                    {"all:SOCGameServerText:", "|text=" + CLIENT_NAME + " played a Soldier card."},
                    {"all:SOCDevCardAction:", "|playerNum=3|actionType=PLAY|cardType=9"},
                    {"all:SOCPlayerElement:", "|playerNum=3|actionType=SET|elementType=19|amount=1"},
                    {"all:SOCPlayerElement:", "|playerNum=3|actionType=GAIN|elementType=15|amount=1"}
                });

        /* leave game, consolidate results */

        tcli.destroy();

        StringBuilder compares = new StringBuilder();
        if (comparesBuild != null)
        {
            compares.append("Build road: Message mismatch: ");
            compares.append(comparesBuild);
        }
        if (comparesBuyCard != null)
        {
            if (compares.length() > 0)
                compares.append("   ");
            compares.append("Buy dev card: Message mismatch: ");
            compares.append(comparesBuyCard);
        }
        if (comparesSoldierMoveRobber != null)
        {
            if (compares.length() > 0)
                compares.append("   ");
            compares.append("Play soldier Move robber: Message mismatch: ");
            compares.append(comparesSoldierMoveRobber);
        }

        if (compares.length() > 0)
        {
            System.err.println(compares);
            fail(compares.toString());
        }

    }

    /**
     * Robber hex to move to (0x305, decimal 773) during
     * {@link #moveRobberStealSequence(DisplaylessTesterClient, SOCGame, SOCPlayer, Vector, String[][])}.
     */
    public static final int MOVE_ROBBER_STEAL_SEQUENCE_NEW_HEX = 0x305;

    /**
     * Choose and move robber to a known location on board of savegame artifact {@code message-seqs},
     * steal from 1 player. Current game state should be {@link SOCGame#WAITING_FOR_ROBBER_OR_PIRATE}.
     * Robber hex should be 0x90a (decimal 2314); will move to {@link #MOVE_ROBBER_STEAL_SEQUENCE_NEW_HEX}.
     *
     * @param tcli  Test client connected to server and playing in {@code ga}
     * @param ga  Game loaded and started by
     *     {@link #connectLoadJoinResumeGame(RecordingTesterServer, String, String, int, boolean, boolean, boolean)}
     * @param cliPl  Client's Player object at test server
     * @param records {@code ga}'s message records at test server; doesn't call {@link Vector#clear()}.
     * @param seqPrefix null, or messages which start the sequence to be compared here
     * @return any discrepancies found by {@link #compareRecordsToExpected(List, String[][])}, or null if no differences
     */
    public static StringBuilder moveRobberStealSequence
        (final DisplaylessTesterClient tcli, final SOCGame ga, final SOCPlayer cliPl,
         final Vector<QueueEntry> records, final String[][] seqPrefix)
        throws UnsupportedOperationException
    {
        assertTrue(ga.hasSeaBoard);
        final SOCBoardLarge board = (SOCBoardLarge) ga.getBoard();

        final String clientName = tcli.getNickname();

        assertEquals("old robberHex", 2314, board.getRobberHex());  // 0x90a
        // victim's settlement should be sole adjacent piece
        {
            final int EXPECTED_VICTIM_SETTLEMENT_NODE = 0x405;

            Set<SOCPlayingPiece> pp = new HashSet<>();
            List<SOCPlayer> players = ga.getPlayersOnHex(MOVE_ROBBER_STEAL_SEQUENCE_NEW_HEX, pp);

            assertNotNull(players);
            assertEquals(1, players.size());
            assertEquals(1, players.get(0).getPlayerNumber());

            assertEquals(1, pp.size());
            SOCPlayingPiece vset = (SOCPlayingPiece) (pp.toArray()[0]);
            assertTrue(vset instanceof SOCSettlement);
            assertEquals(1, vset.getPlayerNumber());
            assertEquals(EXPECTED_VICTIM_SETTLEMENT_NODE, vset.getCoordinates());

            assertTrue(board.settlementAtNode(EXPECTED_VICTIM_SETTLEMENT_NODE) instanceof SOCSettlement);

            final int[] ROBBER_HEX_OTHER_NODES = {0x204, 0x205, 0x206, 0x406, 0x404};
            for (final int node : ROBBER_HEX_OTHER_NODES)
                assertNull(board.settlementAtNode(node));
        }

        assertEquals(SOCGame.WAITING_FOR_ROBBER_OR_PIRATE, ga.getGameState());
        tcli.choosePlayer(ga, SOCChoosePlayer.CHOICE_MOVE_ROBBER);

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        assertEquals(SOCGame.PLACING_ROBBER, ga.getGameState());
        tcli.moveRobber(ga, cliPl, MOVE_ROBBER_STEAL_SEQUENCE_NEW_HEX);

        try { Thread.sleep(70); }
        catch(InterruptedException e) {}
        assertEquals("new robberHex", MOVE_ROBBER_STEAL_SEQUENCE_NEW_HEX, board.getRobberHex());
        SOCMoveRobberResult robRes = ga.getRobberyResult();
        assertNotNull(robRes);
        final int resType = robRes.getLoot();
        assertTrue(resType > 0);

        final String[][] SEQ = new String[][]
            {
                {"all:SOCGameState:", "|state=54"},
                {"all:SOCGameServerText:", "|text=" + clientName + " must choose to move the robber or the pirate."},
                {"all:SOCGameState:", "|state=33"},
                {"all:SOCGameServerText:", "|text=" + clientName + " will move the robber."},
                {"all:SOCMoveRobber:", "|playerNumber=3|coord=305"},
                {"p3:SOCPlayerElement:", "|playerNum=3|actionType=GAIN|elementType=" + resType + "|amount=1"},
                {"p3:SOCPlayerElement:", "|playerNum=1|actionType=LOSE|elementType=" + resType + "|amount=1|news=Y"},
                {"p1:SOCPlayerElement:", "|playerNum=3|actionType=GAIN|elementType=" + resType + "|amount=1"},
                {"p1:SOCPlayerElement:", "|playerNum=1|actionType=LOSE|elementType=" + resType + "|amount=1|news=Y"},
                {"!p[3, 1]:SOCPlayerElement:", "|playerNum=3|actionType=GAIN|elementType=6|amount=1"},
                {"!p[3, 1]:SOCPlayerElement:", "|playerNum=1|actionType=LOSE|elementType=6|amount=1"},
                {"p3:SOCGameServerText:", "|text=You stole a", " from "},  // "an ore", "a sheep", etc
                {"p1:SOCGameServerText:", "|text=" + clientName + " stole a", " from you."},
                {"!p[3, 1]:SOCGameServerText:", "|text=" + clientName + " stole a resource from "},
                {"all:SOCGameState:", "|state=20"}
            };
        final String[][] expectedSeq;
        if (seqPrefix == null)
        {
            expectedSeq = seqPrefix;
        } else {
            expectedSeq = new String[seqPrefix.length + SEQ.length][];
            System.arraycopy(seqPrefix, 0, expectedSeq, 0, seqPrefix.length);
            System.arraycopy(SEQ, 0, expectedSeq, seqPrefix.length, SEQ.length);
        }

        return compareRecordsToExpected(records, expectedSeq);
    }

    /**
     * Connect with 2 clients, have one offer a trade to the other, decline it.
     */
    @Test
    public void testTradeDecline2Clients()
        throws IOException
    {
        final String CLIENT1_NAME = "testTradeDecline_p3", CLIENT2_NAME = "testTradeDecline_p2";
        final int PN_C1 = 3, PN_C2 = 2;

        final StartedTestGameObjects objs = connectLoadJoinResumeGame
            (srv, CLIENT1_NAME, CLIENT2_NAME, PN_C2, true, false, true);
        final DisplaylessTesterClient tcli1 = objs.tcli, tcli2 = objs.tcli2;
        // final SavedGameModel sgm = objs.sgm;
        final SOCGame ga = objs.gameAtServer;
        final String gaName = ga.getName();
        final SOCBoardLarge board = objs.board;
        final SOCPlayer cli1Pl = objs.clientPlayer;
        final Vector<QueueEntry> records = objs.records;

        records.clear();

        /* client 1: offer trade only to client 2 */

        final boolean[] OFFERED_TO = {false, false, true, false};
        final SOCResourceSet GIVING = new SOCResourceSet(0, 1, 0, 1, 0, 0),
            GETTING = new SOCResourceSet(0, 0, 1, 0, 0, 0);
        tcli1.offerTrade(ga, new SOCTradeOffer
            (gaName, PN_C1, OFFERED_TO, GIVING, GETTING));

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        SOCTradeOffer offer = cli1Pl.getCurrentOffer();
        assertNotNull(offer);
        assertEquals(PN_C1, offer.getFrom());
        assertArrayEquals(OFFERED_TO, offer.getTo());
        assertEquals(GIVING, offer.getGiveSet());
        assertEquals(GETTING, offer.getGetSet());
        assertTrue(offer.isWaitingReplyFrom(PN_C2));

        /* client 2: decline offered trade */

        tcli2.rejectOffer(ga);

        try { Thread.sleep(60); }
        catch(InterruptedException e) {}
        assertFalse(offer.isWaitingReplyFrom(PN_C2));

        StringBuilder compares = compareRecordsToExpected
            (records, new String[][]
            {
                {"all:SOCMakeOffer:", "|offer=game=" + gaName + "|from=" + PN_C1
                 + "|to=false,false,true,false|give=clay=0|ore=1|sheep=0|wheat=1|wood=0|unknown=0|get=clay=0|ore=0|sheep=1|wheat=0|wood=0|unknown=0"},
                {"all:SOCClearTradeMsg:", "|playerNumber=-1"},
                {"all:SOCRejectOffer:", "|playerNumber=" + PN_C2}
            });

        /* leave game, check results */

        tcli1.destroy();
        tcli2.destroy();

        if (compares != null)
        {
            compares.append("testTradeDecline2Clients: Message mismatch: ");
            compares.append(compares);

            System.err.println(compares);
            fail(compares.toString());
        }
    }

    /**
     * Compare game event records against expected sequence.
     * @param records  Game records from server
     * @param expected  Expected: Per-record lists of prefix, any other contained strings
     *     to ignore game name and variable fields.
     *     {@code expected[i]} which are {@code null} are skipped
     *     as if the array didn't contain the null and was 1 element shorter.
     * @return {@code null} if no differences, or the differences found
     */
    public static StringBuilder compareRecordsToExpected
        (final List<QueueEntry> records, final String[][] expected)
    {
        StringBuilder compares = new StringBuilder();

        int nExpected = 0;
        for (int i = 0; i < expected.length; ++i)
            if (expected[i] != null)
                ++nExpected;
        int n = records.size();
        if (n > nExpected)
            n = nExpected;

        if (records.size() != nExpected)
            compares.append("Length mismatch: Expected " + nExpected + ", got " + records.size());

        StringBuilder comp = new StringBuilder();
        for (int iExpected = 0, iRecords = 0; iRecords < n; ++iExpected)
        {
            final String[] exps = expected[iExpected];
            if (exps == null)
                // skip null; this loop iteration did ++iExpected but not ++iRecords
                continue;

            comp.setLength(0);
            final String recStr = records.get(iRecords).toString();
            if (! recStr.startsWith(exps[0]))
                comp.append("expected start " + exps[0] + ", saw " + recStr.substring(0, exps[0].length()));

            boolean failContains = false;
            for (int j = 1; j < exps.length; ++j)
            {
                if (! recStr.contains(exps[j]))
                {
                    comp.append(" expected " + exps[j]);
                    failContains = true;
                }
            }
            if (failContains)
                comp.append(", saw message " + recStr);

            if (comp.length() > 0)
                compares.append(" [" + iExpected + "]: " + comp);

            ++iRecords;
        }

        if (compares.length() == 0)
            return null;
        else
            return compares;
    }

    /** Comprehensive tests for {@link RecordingTesterServer.QueueEntry}: Constructors, toString */
    @Test
    public void testQueueEntry()
    {
        final SOCBuildRequest event = new SOCBuildRequest("testgame", 2);

        QueueEntry qe = new QueueEntry(event, -1);
        assertEquals(-1, qe.toPN);
        assertEquals(event, qe.event);
        assertNull(qe.excludedPN);
        assertEquals("all:SOCBuildRequest:game=testgame|pieceType=2", qe.toString());

        qe = new QueueEntry(event, new int[]{3});
        assertEquals(-1, qe.toPN);
        assertEquals(event, qe.event);
        assertArrayEquals(new int[]{3}, qe.excludedPN);
        assertEquals("!p3:SOCBuildRequest:game=testgame|pieceType=2", qe.toString());

        qe = new QueueEntry(event, new int[]{2,3,4});
        assertEquals(-1, qe.toPN);
        assertEquals(event, qe.event);
        assertArrayEquals(new int[]{2,3,4}, qe.excludedPN);
        assertEquals("!p[2, 3, 4]:SOCBuildRequest:game=testgame|pieceType=2", qe.toString());

        qe = new QueueEntry(null, -1);
        assertEquals(-1, qe.toPN);
        assertNull(qe.event);
        assertNull(qe.excludedPN);
        assertEquals("all:null", qe.toString());

        qe = new QueueEntry(null, 3);
        assertEquals(3, qe.toPN);
        assertNull(qe.event);
        assertNull(qe.excludedPN);
        assertEquals("p3:null", qe.toString());
    }

    /**
     * Data class for useful objects returned from
     * {@link TestRecorder#connectLoadJoinResumeGame(RecordingTesterServer, String, String, int, boolean, boolean, boolean)}.
     */
    public static final class StartedTestGameObjects
    {
        /** Client connected to test server; not null */
        public final DisplaylessTesterClient tcli;

        /** Optional second client connected to test server, or null */
        public final DisplaylessTesterClient tcli2;

        /** Client connection at server; not null */
        public final Connection tcliConn;

        /** Optional second client connection at server, or null */
        public final Connection tcli2Conn;

        public final SavedGameModel sgm;
        public final SOCGame gameAtServer;
        public final SOCBoardLarge board;
        public final SOCPlayer clientPlayer;
        public final SOCPlayer client2Player;
        public final Vector<QueueEntry> records;

        public StartedTestGameObjects
            (DisplaylessTesterClient tcli, DisplaylessTesterClient tcli2, Connection tcliConn, Connection tcli2Conn,
             SavedGameModel sgm, SOCGame gameAtServer, SOCBoardLarge board,
             SOCPlayer clientPlayer, SOCPlayer client2Player, Vector<QueueEntry> records)
        {
            this.tcli = tcli;
            this.tcli2 = tcli2;
            this.tcliConn = tcliConn;
            this.tcli2Conn = tcli2Conn;
            this.sgm = sgm;
            this.gameAtServer = gameAtServer;
            this.board = board;
            this.clientPlayer = clientPlayer;
            this.client2Player = client2Player;
            this.records = records;
        }
    }

}
