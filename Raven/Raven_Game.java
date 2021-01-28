package Raven;
import static Raven.Main.TODO;
import Raven.armory.projectiles.Bolt;
import Raven.armory.projectiles.Pellet;
import Raven.armory.projectiles.Raven_Projectile;
import Raven.armory.projectiles.Rocket;
import Raven.armory.projectiles.Slug;
import static Raven.lua.Raven_Scriptor.script;
import static Raven.Raven_Messages.message_type.Msg_UserHasRemovedBot;
import static Raven.Raven_ObjectEnumerations.GetNameOfType;
import static Raven.Raven_ObjectEnumerations.type_blaster;
import static Raven.Raven_ObjectEnumerations.type_rail_gun;
import static Raven.Raven_ObjectEnumerations.type_rocket_launcher;
import static Raven.Raven_ObjectEnumerations.type_shotgun;
import static Raven.Raven_UserOptions.UserOptions;
import static Raven.DEFINE.*;
import Raven.goals.Raven_Goal_Types.GoalTypeToString;
import Raven.navigation.PathManager;
import Raven.navigation.Raven_PathPlanner;
import common.D2.Vector2D;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.Vector2D.isSecondInFOVOfFirst;
import static common.D2.Vector2D.POINTStoVector;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Vector2D.Vec2DDistanceSq;
import static common.D2.Vector2D.Vec2DNormalize;
import static common.D2.WallIntersectionTests.doWallsIntersectCircle;
import static common.D2.WallIntersectionTests.doWallsObstructLineSegment;
import static common.Debug.DbgConsole.debug_con;
import static common.Game.EntityFunctionTemplates.TagNeighbors;
import static common.Game.EntityManager.EntityMgr;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.Messaging.MessageDispatcher.SENDER_ID_IRRELEVANT;
import static common.misc.Stream_Utility_function.ttos;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.MaxDouble;
import static common.misc.WindowUtils.ErrorBox;
import static common.misc.WindowUtils.GetClientCursorPosition;
import static common.misc.WindowUtils.IS_KEY_PRESSED;
import common.windows.POINTS;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: this class creates and stores all the entities that make up the Raven
 * game environment. (walls, bots, health etc) and can read a Raven map file and
 * recreate the necessary geometry.
 *
 * this class has methods for updating the game entities and for rendering them.
 */
public class Raven_Game {

    static {
        // uncomment to write object creation/deletion to debug console
        // define(LOG_CREATIONAL_STUFF);
    }
    /**
     * the current game map
     */
    private Raven_Map m_pMap;
    /**
     * a list of all the bots that are inhabiting the map
     */
    private List<Raven_Bot> m_Bots = new LinkedList<Raven_Bot>();
    /**
     * the user may select a bot to control manually. This is a pointer to that
     * bot
     */
    private Raven_Bot m_pSelectedBot;
    /**
     * this list contains any active projectiles (slugs, rockets, shotgun
     * pellets, etc)
     */
    private List<Raven_Projectile> m_Projectiles = new LinkedList<Raven_Projectile>();
    /**
     * this class manages all the path planning requests
     */
    private PathManager<Raven_PathPlanner> m_pPathManager;
    /**
     * if true the game will be paused
     */
    private boolean m_bPaused;
    /**
     * if true a bot is removed from the game
     */
    private boolean m_bRemoveABot;
    /**
     * when a bot is killed a "grave" is displayed for a few seconds. This class
     * manages the graves
     */
    private GraveMarkers m_pGraveMarkers;

    /**
     * this iterates through each trigger, testing each one against each bot
     */
    //private void  UpdateTriggers();
    /**
     * deletes all entities, empties all containers and creates a new navgraph
     */
    private void Clear() {
        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("\n------------------------------ Clearup -------------------------------").print("");
        }
        //delete the bots
        Iterator<Raven_Bot> iterator = m_Bots.iterator();
        while (iterator.hasNext()) {
            Raven_Bot it = iterator.next();
            if (def(LOG_CREATIONAL_STUFF)) {
                debug_con.print("deleting entity id: ").print(it.ID()).print(" of type ")
                        .print(GetNameOfType(it.EntityType())).print("(").print(it.EntityType()).
                        print(")").print("");
            }
            iterator.remove();
        }
        //delete any active projectiles
        Iterator<Raven_Projectile> curW = m_Projectiles.iterator();
        while (curW.hasNext()) {
            if (def(LOG_CREATIONAL_STUFF)) {
                debug_con.print("deleting projectile id: ").print(curW.next().ID()).print("");
            }

            curW.remove();
        }

        //clear the containers
        m_Projectiles.clear();
        m_Bots.clear();

        m_pSelectedBot = null;

    }

    /**
     * attempts to position a spawning bot at a free spawn point. returns false
     * if unsuccessful
     */
    private boolean AttemptToAddBot(Raven_Bot pBot) {
        //make sure there are some spawn points available
        if (m_pMap.GetSpawnPoints().size() <= 0) {
            ErrorBox("Map has no spawn points!");
            return false;
        }

        //we'll make the same number of attempts to spawn a bot this update as
        //there are spawn points
        int attempts = m_pMap.GetSpawnPoints().size();

        while (--attempts >= 0) {
            //select a random spawn point
            Vector2D pos = m_pMap.GetRandomSpawnPoint();

            //check to see if it's occupied
            Iterator<Raven_Bot> it = m_Bots.iterator();

            boolean bAvailable = true;

            while (it.hasNext()) {
                Raven_Bot curBot = it.next();
                //if the spawn point is unoccupied spawn a bot
                if (Vec2DDistance(pos, curBot.Pos()) < curBot.BRadius()) {
                    bAvailable = false;
                }
            }

            if (bAvailable) {
                pBot.Spawn(pos);

                return true;
            }
        }

        return false;
    }

    /**
     * when a bot is removed from the game by a user all remaining bots must be
     * notified so that they can remove any references to that bot from their
     * memory
     */
    private void NotifyAllBotsOfRemoval(Raven_Bot pRemovedBot) {
        Iterator<Raven_Bot> it = m_Bots.iterator();
        while (it.hasNext()) {
            Raven_Bot curBot = it.next();
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    SENDER_ID_IRRELEVANT,
                    curBot.ID(),
                    Msg_UserHasRemovedBot,
                    pRemovedBot);
        }
    }

    //----------------------------- ctor ------------------------------------------
    //-----------------------------------------------------------------------------
    public Raven_Game() {
        m_pSelectedBot = null;
        m_bPaused = false;
        m_bRemoveABot = false;
        m_pMap = null;
        m_pPathManager = null;
        m_pGraveMarkers = null;
        //load in the default map
        LoadMap(script.GetString("StartMap"));
        
        TODO("Odstranit automaticky vyber bota");
        if(m_Bots.size() > 0)  {
            m_pSelectedBot = m_Bots.get(0);
        }
    }

//------------------------------ dtor -----------------------------------------
//-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Clear();
        m_pPathManager = null;
        m_pMap = null;

        m_pGraveMarkers = null;
    }

    //the usual suspects
    public void Render() {
        m_pGraveMarkers.Render();

        //render the map
        m_pMap.Render();

        //render all the bots unless the user has selected the option to only 
        //render those bots that are in the fov of the selected bot
        if (m_pSelectedBot != null && UserOptions.m_bOnlyShowBotsInTargetsFOV) {
            List<Raven_Bot> VisibleBots = GetAllBotsInFOV(m_pSelectedBot);
            Iterator<Raven_Bot> it = VisibleBots.iterator();
            while (it.hasNext()) {
                it.next().Render();
            }

            if (m_pSelectedBot != null) {
                m_pSelectedBot.Render();
            }
        } else {
            //render all the entities
            Iterator<Raven_Bot> it = m_Bots.iterator();
            while (it.hasNext()) {
                Raven_Bot curBot = it.next();
                if (curBot.isAlive()) {
                    curBot.Render();
                }
            }
        }
        //render any projectiles
        Iterator<Raven_Projectile> curW = m_Projectiles.iterator();
        while (curW.hasNext()) {
            curW.next().Render();
        }

        // gdi.TextAtPos(300, Constants.WindowHeight - 70, "Num Current Searches: " + ttos(m_pPathManager.GetNumActiveSearches()));

        //render a red circle around the selected bot (blue if possessed)
        if (m_pSelectedBot != null) {
            if (m_pSelectedBot.isPossessed()) {
                gdi.BluePen();
                gdi.HollowBrush();
                gdi.Circle(m_pSelectedBot.Pos(), m_pSelectedBot.BRadius() + 1);
            } else {
                gdi.RedPen();
                gdi.HollowBrush();
                gdi.Circle(m_pSelectedBot.Pos(), m_pSelectedBot.BRadius() + 1);
            }


            if (UserOptions.m_bShowOpponentsSensedBySelectedBot) {
                m_pSelectedBot.GetSensoryMem().RenderBoxesAroundRecentlySensed();
            }

            //render a square around the bot's target
            if (UserOptions.m_bShowTargetOfSelectedBot && m_pSelectedBot.GetTargetBot() != null) {

                gdi.ThickRedPen();

                Vector2D p = m_pSelectedBot.GetTargetBot().Pos();
                double b = m_pSelectedBot.GetTargetBot().BRadius();

                gdi.Line(p.x - b, p.y - b, p.x + b, p.y - b);
                gdi.Line(p.x + b, p.y - b, p.x + b, p.y + b);
                gdi.Line(p.x + b, p.y + b, p.x - b, p.y + b);
                gdi.Line(p.x - b, p.y + b, p.x - b, p.y - b);
            }



            //render the path of the bot
            if (UserOptions.m_bShowPathOfSelectedBot) {
                m_pSelectedBot.GetBrain().Render();
            }

            //display the bot's goal stack
            if (UserOptions.m_bShowGoalsOfSelectedBot) {
                Vector2D p = new Vector2D(m_pSelectedBot.Pos().x - 50, m_pSelectedBot.Pos().y);

                m_pSelectedBot.GetBrain().RenderAtPos(p, GoalTypeToString.Instance());
            }

            if (UserOptions.m_bShowGoalAppraisals) {
                m_pSelectedBot.GetBrain().RenderEvaluations(5, 415);
            }

            if (UserOptions.m_bShowWeaponAppraisals) {
                m_pSelectedBot.GetWeaponSys().RenderDesirabilities();
            }

            if (IS_KEY_PRESSED(KeyEvent.VK_Q) && m_pSelectedBot.isPossessed()) {
                gdi.TextColor(255, 0, 0);
                gdi.TextAtPos(GetClientCursorPosition(), "Queuing");
            }
        }
    }

    /**
     * calls the update function of each entity
     */
    public void Update() {
        //don't update if the user has paused the game
        if (m_bPaused) {
            return;
        }

        m_pGraveMarkers.Update();

        //get any player keyboard input
        GetPlayerInput();

        //update all the queued searches in the path manager
        m_pPathManager.UpdateSearches();
        //update any doors
        Iterator<Raven_Door> curDoor = m_pMap.GetDoors().iterator();
        while (curDoor.hasNext()) {
            curDoor.next().Update();
        }

        //update any current projectiles
        Iterator<Raven_Projectile> it = m_Projectiles.iterator();
        while (it.hasNext()) {
            Raven_Projectile curW = it.next();
            //test for any dead projectiles and remove them if necessary
            if (!curW.isDead()) {
                curW.Update();
            } else {
                it.remove();
            }
        }

        //update the bots
        boolean bSpawnPossible = true;

        Iterator<Raven_Bot> botsIt = m_Bots.iterator();
        while (botsIt.hasNext()) {
            Raven_Bot curBot = botsIt.next();
            //if this bot's status is 'respawning' attempt to resurrect it from
            //an unoccupied spawn point
            if (curBot.isSpawning() && bSpawnPossible) {
                bSpawnPossible = AttemptToAddBot(curBot);
            } //if this bot's status is 'dead' add a grave at its current location 
            //then change its status to 'respawning'
            else if (curBot.isDead()) {
                //create a grave
                m_pGraveMarkers.AddGrave(curBot.Pos());

                //change its status to spawning
                curBot.SetSpawning();
            } //if this bot is alive update it.
            else if (curBot.isAlive()) {
                curBot.Update();
            }
        }

        //update the triggers
        m_pMap.UpdateTriggerSystem(m_Bots);

        //if the user has requested that the number of bots be decreased, remove
        //one
        if (m_bRemoveABot) {
            if (!m_Bots.isEmpty()) {
                Raven_Bot pBot = m_Bots.get(m_Bots.size() - 1);
                if (pBot == m_pSelectedBot) {
                    m_pSelectedBot = null;
                }
                NotifyAllBotsOfRemoval(pBot);
                m_Bots.remove(pBot);
                pBot = null;
            }

            m_bRemoveABot = false;
        }
    }

    /**
     * loads an environment from a file sets up the game environment from map
     * file
     */
    public boolean LoadMap(final String filename) {
        //clear any current bots and projectiles
        Clear();

        //out with the old
        m_pMap = null;
        m_pGraveMarkers = null;
        m_pPathManager = null;

        //in with the new
        m_pGraveMarkers = new GraveMarkers(script.GetDouble("GraveLifetime"));
        m_pPathManager = new PathManager<Raven_PathPlanner>(script.GetInt("MaxSearchCyclesPerUpdateStep"));
        m_pMap = new Raven_Map();

        //make sure the entity manager is reset
        EntityMgr.Reset();


        //load the new map data
        if (m_pMap.LoadMap(filename)) {
            AddBots(script.GetInt("NumBots"));

            return true;
        }

        return false;
    }

    /**
     * Adds a bot and switches on the default steering behavior
     */
    public void AddBots(int NumBotsToAdd) {
        while (NumBotsToAdd-- != 0) {
            //create a bot. (its position is irrelevant at this point because it will
            //not be rendered until it is spawned)
            Raven_Bot rb = new Raven_Bot(this, new Vector2D());

            //switch the default steering behaviors on
            rb.GetSteering().WallAvoidanceOn();
            rb.GetSteering().SeparationOn();

            m_Bots.add(rb);

            //register the bot with the entity manager
            EntityMgr.RegisterEntity(rb);


            if (def(LOG_CREATIONAL_STUFF)) {
                debug_con.print("Adding bot with ID ").print(ttos(rb.ID())).print("");
            }
        }
    }

    public void AddRocket(Raven_Bot shooter, Vector2D target) {
        target = new Vector2D(target); // work with copy
        Raven_Projectile rp = new Rocket(shooter, target);

        m_Projectiles.add(rp);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Adding a rocket ").print(rp.ID()).print(" at pos ").print(rp.Pos()).print("");
        }
    }

    public void AddRailGunSlug(Raven_Bot shooter, Vector2D target) {
        target = new Vector2D(target); // work with copy
        Raven_Projectile rp = new Slug(shooter, target);

        m_Projectiles.add(rp);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Adding a rail gun slug").print(rp.ID()).print(" at pos ").print(rp.Pos()).print("");
        }
    }

    public void AddShotGunPellet(Raven_Bot shooter, Vector2D target) {
        target = new Vector2D(target); //work with copy
        Raven_Projectile rp = new Pellet(shooter, target);

        m_Projectiles.add(rp);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Adding a shotgun shell ").print(rp.ID()).print(" at pos ").print(rp.Pos()).print("");
        }
    }

    public void AddBolt(Raven_Bot shooter, Vector2D target) {
        target = new Vector2D(target); //work with copy
        Raven_Projectile rp = new Bolt(shooter, target);

        m_Projectiles.add(rp);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Adding a bolt ").print(rp.ID()).print(" at pos ").print(rp.Pos()).print("");
        }
    }

    /**
     * removes the last bot to be added from the game
     */
    public void RemoveBot() {
        m_bRemoveABot = true;
    }

    /**
     * returns true if a bot cannot move from A to B without bumping into world
     * geometry. It achieves this by stepping from A to B in steps of size
     * BoundingRadius and testing for intersection with world geometry at each
     * point.
     */
    public boolean isPathObstructed(Vector2D A,
            Vector2D B,
            double BoundingRadius/* = 0 */) {
        Vector2D ToB = Vec2DNormalize(sub(B, A));

        Vector2D curPos = new Vector2D(A);

        while (Vec2DDistanceSq(curPos, B) > BoundingRadius * BoundingRadius) {
            //advance curPos one step
            curPos.add(mul(ToB, 0.5 * BoundingRadius));

            //test all walls against the new position
            if (doWallsIntersectCircle(m_pMap.GetWalls(), curPos, BoundingRadius)) {
                return true;
            }
        }

        return false;
    }

    /**
     * returns a vector of pointers to bots within the given bot's field of view
     */
    public List<Raven_Bot> GetAllBotsInFOV(final Raven_Bot pBot) {
        ArrayList<Raven_Bot> VisibleBots = new ArrayList<Raven_Bot>();
        Iterator<Raven_Bot> it = m_Bots.iterator();
        while (it.hasNext()) {
            Raven_Bot curBot = it.next();
            //make sure time is not wasted checking against the same bot or against a
            // bot that is dead or re-spawning
            if (curBot == pBot || !curBot.isAlive()) {
                continue;
            }

            //first of all test to see if this bot is within the FOV
            if (isSecondInFOVOfFirst(pBot.Pos(),
                    pBot.Facing(),
                    curBot.Pos(),
                    pBot.FieldOfView())) {
                //cast a ray from between the bots to test visibility. If the bot is
                //visible add it to the vector
                if (!doWallsObstructLineSegment(pBot.Pos(),
                        curBot.Pos(),
                        m_pMap.GetWalls())) {
                    VisibleBots.add(curBot);
                }
            }
        }

        return VisibleBots;
    }

    /**
     * returns true if the second bot is unobstructed by walls and in the field
     * of view of tshe first.
     */
    public boolean isSecondVisibleToFirst(final Raven_Bot pFirst,
            final Raven_Bot pSecond) {
        //if the two bots are equal or if one of them is not alive return false
        if (!(pFirst == pSecond) && pSecond.isAlive()) {
            //first of all test to see if this bot is within the FOV
            if (isSecondInFOVOfFirst(pFirst.Pos(),
                    pFirst.Facing(),
                    pSecond.Pos(),
                    pFirst.FieldOfView())) {
                //test the line segment connecting the bot's positions against the walls.
                //If the bot is visible add it to the vector
                if (!doWallsObstructLineSegment(pFirst.Pos(),
                        pSecond.Pos(),
                        m_pMap.GetWalls())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * returns true if the ray between A and B is unobstructed.
     */
    public boolean isLOSOkay(Vector2D A, Vector2D B) {
        return !doWallsObstructLineSegment(A, B, m_pMap.GetWalls());
    }

    /**
     * starting from the given origin and moving in the direction Heading this
     * method returns the distance to the closest wall
     */
    //public double  GetDistanceToClosestWall(Vector2D Origin, Vector2D Heading);
    /**
     * returns the position of the closest visible switch that triggers the door
     * of the specified ID
     */
    public Vector2D GetPosOfClosestSwitch(Vector2D botPos, int doorID) {
        List<Integer> SwitchIDs = new ArrayList<Integer>();

        //first we need to get the ids of the switches attached to this door
        Iterator<Raven_Door> it = m_pMap.GetDoors().iterator();
        while (it.hasNext()) {
            Raven_Door curDoor = it.next();
            if (curDoor.ID() == doorID) {
                SwitchIDs = curDoor.GetSwitchIDs();
                break;
            }
        }

        Vector2D closest = new Vector2D();
        double ClosestDist = MaxDouble;

        //now test to see which one is closest and visible
        Iterator<Integer> idIt = SwitchIDs.iterator();
        while (idIt.hasNext()) {
            BaseGameEntity trig = EntityMgr.GetEntityFromID(idIt.next());

            if (isLOSOkay(botPos, trig.Pos())) {
                double dist = Vec2DDistanceSq(botPos, trig.Pos());

                if (dist < ClosestDist) {
                    ClosestDist = dist;
                    closest = trig.Pos();
                }
            }
        }

        return closest;
    }

    /**
     * given a position on the map this method returns the bot found with its
     * bounding radius of that position.If there is no bot at the position the
     * method returns NULL
     */
    public Raven_Bot GetBotAtPosition(Vector2D CursorPos) {
        Iterator<Raven_Bot> it = m_Bots.iterator();
        while (it.hasNext()) {
            Raven_Bot curBot = it.next();
            if (Vec2DDistance(curBot.Pos(), CursorPos) < curBot.BRadius()*2) {
                if (curBot.isAlive()) {
                    return curBot;
                }
            }
        }

        return null;
    }

    public void TogglePause() {
        m_bPaused = !m_bPaused;
    }

    /**
     * this method is called when the user clicks the right mouse button.
     *
     * the method checks to see if a bot is beneath the cursor. If so, the bot
     * is recorded as selected.
     *
     * if the cursor is not over a bot then any selected bot/s will attempt to
     * move to that position.
     */
    public void ClickRightMouseButton(POINTS p) {
        Raven_Bot pBot = GetBotAtPosition(POINTStoVector(p));
        //if there is no selected bot just return;
        if (pBot == null && m_pSelectedBot == null) {
            return;
        }

        //if the cursor is over a different bot to the existing selection,
        //change selection
        if (pBot != null && pBot != m_pSelectedBot) {
            if (m_pSelectedBot != null) {
                m_pSelectedBot.Exorcise();
            }
            m_pSelectedBot = pBot;

            return;
        }

        //if the user clicks on a selected bot twice it becomes possessed(under
        //the player's control)
        if (pBot != null && pBot == m_pSelectedBot) {
            m_pSelectedBot.TakePossession();

            //clear any current goals
            m_pSelectedBot.GetBrain().RemoveAllSubgoals();
        }

        //if the bot is possessed then a right click moves the bot to the cursor
        //position
        if (m_pSelectedBot.isPossessed()) {
            //if the shift key is pressed down at the same time as clicking then the
            //movement command will be queued
            if (IS_KEY_PRESSED(KeyEvent.VK_Q)) {
                m_pSelectedBot.GetBrain().QueueGoal_MoveToPosition(POINTStoVector(p));
            } else {
                //clear any current goals
                m_pSelectedBot.GetBrain().RemoveAllSubgoals();

                m_pSelectedBot.GetBrain().AddGoal_MoveToPosition(POINTStoVector(p));
            }
        }
    }

    /**
     * this method is called when the user clicks the left mouse button. If
     * there is a possessed bot, this fires the weapon, else does nothing
     */
    public void ClickLeftMouseButton(POINTS p) {
        if (m_pSelectedBot != null && m_pSelectedBot.isPossessed()) {
            m_pSelectedBot.FireWeapon(POINTStoVector(p));
        }
    }

    /**
     * when called will release any possessed bot from user control
     */
    public void ExorciseAnyPossessedBot() {
        if (m_pSelectedBot != null) {
            m_pSelectedBot.Exorcise();
        }
    }

    /**
     * if a bot is possessed the keyboard is polled for user input and any
     * relevant bot methods are called appropriately
     */
    public void GetPlayerInput() {
        if (m_pSelectedBot != null && m_pSelectedBot.isPossessed()) {
            m_pSelectedBot.RotateFacingTowardPosition(GetClientCursorPosition());
        }
    }

    public Raven_Bot PossessedBot() {
        return m_pSelectedBot;
    }

    /**
     * changes the weapon of the possessed bot
     */
    public void ChangeWeaponOfPossessedBot(int weapon) {
        //ensure one of the bots has been possessed
        if (m_pSelectedBot != null) {
            switch (weapon) {
                case type_blaster:

                    PossessedBot().ChangeWeapon(type_blaster);
                    return;

                case type_shotgun:

                    PossessedBot().ChangeWeapon(type_shotgun);
                    return;

                case type_rocket_launcher:

                    PossessedBot().ChangeWeapon(type_rocket_launcher);
                    return;

                case type_rail_gun:

                    PossessedBot().ChangeWeapon(type_rail_gun);
                    return;

            }
        }
    }

    public Raven_Map GetMap() {
        return m_pMap;
    }

    public List<Raven_Bot> GetAllBots() {
        return m_Bots;
    }

    public PathManager<Raven_PathPlanner> GetPathManager() {
        return m_pPathManager;
    }

    public int GetNumBots() {
        return m_Bots.size();
    }

    public void TagRaven_BotsWithinViewRange(BaseGameEntity pRaven_Bot, double range) {
        TagNeighbors(pRaven_Bot, m_Bots, range);
    }
}