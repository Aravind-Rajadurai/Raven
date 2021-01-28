package Raven;

import Raven.triggers.Trigger_SoundNotify;
import static Raven.DEFINE.*;
import Raven.triggers.Trigger_OnButtonSendMsg;
import static Raven.Raven_UserOptions.UserOptions;
import static Raven.lua.Raven_Scriptor.script;
import static Raven.Raven_ObjectEnumerations.*;
import Raven.triggers.Trigger_HealthGiver;
import Raven.triggers.Trigger_WeaponGiver;
import common.D2.Vector2D;
import common.D2.Wall2D;
import static common.misc.Cgdi.gdi;
import static common.Debug.DbgConsole.debug_con;
import common.Triggers.TriggerSystem;
import static common.Game.EntityManager.EntityMgr;
import common.Graph.GraphEdgeTypes.NavGraphEdge;
import common.Graph.GraphNodeTypes.NavGraphNode;
import common.Graph.SparseGraph;
import static common.Graph.HandyGraphFunctions.CalculateAverageGraphEdgeLength;
import static common.Graph.HandyGraphFunctions.CreateAllPairsCostsTable;
import static common.Graph.HandyGraphFunctions.GraphHelper_DrawUsingGDI;
import common.Triggers.Trigger;
import static common.misc.utils.Maximum;
import common.misc.CellSpacePartition;
import common.misc.Cgdi;
import static common.misc.CppToJava.FindWindow;
import static common.misc.CppToJava.ResizeWindow;
import static common.misc.utils.RandInt;
import static common.misc.WindowUtils.ErrorBox;
import static common.misc.WindowUtils.Window;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 *
 * Author: Mat Buckland (www.ai-junkie.com)
 * 
* Desc: this class creates and stores all the entities that make up the Raven
 * game environment. (walls, bots, health etc)
 * 
* It can read a Raven map editor file and recreate the necessary geometry.
 */
public class Raven_Map {
    //uncomment to write object creation/deletion to debug console

    static {
        define(LOG_CREATIONAL_STUFF);
    }

    public static class GraphNode extends NavGraphNode<Trigger<Raven_Bot>> {

        public GraphNode(Scanner stream) {
            super(stream);
        }
    }

    public static class NavGraph extends SparseGraph<Raven_Map.GraphNode, NavGraphEdge> {

        public NavGraph(boolean digraph) {
            super(digraph);
        }
    }

    public static class CellSpace extends CellSpacePartition<NavGraphNode<Trigger<Raven_Bot>>> {

        public CellSpace(double width, //width of 2D space
                double height, //height...
                int cellsX, //number of divisions horizontally
                int cellsY, //and vertically
                int MaxEntitys) //maximum number of entities to partition
        {
            super(width, height, cellsX, cellsY, MaxEntitys,
                    new NavGraphNode<Trigger<Raven_Bot>>());
        }
    }
    //public typedef Trigger<Raven_Bot>                        TriggerType;
    //public typedef TriggerSystem<TriggerType>                TriggerSystem;
    /**
     * the walls that comprise the current map's architecture.
     */
    private List<Wall2D> m_Walls = new ArrayList<Wall2D>();
    /**
     * trigger are objects that define a region of space. When a raven bot
     * enters that area, it 'triggers' an event. That event may be anything from
     * increasing a bot's health to opening a door or requesting a lift.
     */
    private TriggerSystem m_TriggerSystem = new TriggerSystem();
    /**
     * this holds a number of spawn positions. When a bot is instantiated it
     * will appear at a randomly selected point chosen from this vector
     */
    private List<Vector2D> m_SpawnPoints = new ArrayList<Vector2D>();
    /**
     * a map may contain a number of sliding doors.
     */
    private List<Raven_Door> m_Doors = new ArrayList<Raven_Door>();
    /**
     * this map's accompanying navigation graph
     */
    private NavGraph m_pNavGraph;
    /**
     * the graph nodes will be partitioned enabling fast lookup
     */
    private CellSpace m_pSpacePartition;
    /**
     * the size of the search radius the cellspace partition uses when looking
     * for neighbors
     */
    private double m_dCellSpaceNeighborhoodRange;
    private int m_iSizeX;
    private int m_iSizeY;

    private void PartitionNavGraph() {
        if (m_pSpacePartition != null) {
            m_pSpacePartition = null;
        }

        m_pSpacePartition = new CellSpace(m_iSizeX,
                m_iSizeY,
                script.GetInt("NumCellsX"),
                script.GetInt("NumCellsY"),
                m_pNavGraph.NumNodes());

        //add the graph nodes to the space partition
        NavGraph.NodeIterator NodeItr = new NavGraph.NodeIterator<>(m_pNavGraph);
        for (NavGraphNode pN = NodeItr.begin(); !NodeItr.end(); pN = NodeItr.next()) {
            m_pSpacePartition.AddEntity(pN);
        }
    }
    /**
     * this will hold a pre-calculated lookup table of the cost to travel from
     * one node to any other.
     */
    private List<ArrayList<Double>> m_PathCosts = new ArrayList<ArrayList<Double>>();

    //stream constructors for loading from a file
    private void AddWall(Scanner in) {
        m_Walls.add(new Wall2D(in));
    }

    private void AddHealth_Giver(Scanner in) {
        Trigger_HealthGiver hg = new Trigger_HealthGiver(in);

        m_TriggerSystem.Register(hg);
        //let the corresponding navgraph node point to this object
        GraphNode node = m_pNavGraph.GetNode(hg.GraphNodeIndex());

        node.SetExtraInfo(hg);

        //register the entity 
        EntityMgr.RegisterEntity(hg);
    }

    private void AddWeapon_Giver(int type_of_weapon, Scanner in) {
        Trigger_WeaponGiver wg = new Trigger_WeaponGiver(in);

        wg.SetEntityType(type_of_weapon);

        //add it to the appropriate vectors
        m_TriggerSystem.Register(wg);

        //let the corresponding navgraph node point to this object
        GraphNode node = m_pNavGraph.GetNode(wg.GraphNodeIndex());

        node.SetExtraInfo(wg);

        //register the entity 
        EntityMgr.RegisterEntity(wg);
    }

    private void AddDoor(Scanner in) {
        Raven_Door pDoor = new Raven_Door(this, in);

        m_Doors.add(pDoor);

        //register the entity 
        EntityMgr.RegisterEntity(pDoor);
    }

    private void AddDoorTrigger(Scanner in) {
        Trigger_OnButtonSendMsg<Raven_Bot> tr = new Trigger_OnButtonSendMsg<Raven_Bot>(in);

        m_TriggerSystem.Register(tr);

        //register the entity 
        EntityMgr.RegisterEntity(tr);

    }

    private void AddSpawnPoint(Scanner in) {
        double x, y, dummy;

        dummy = in.nextDouble(); //dummy values are artifacts from the map editor
        x = in.nextDouble();
        y = in.nextDouble();
        dummy = in.nextDouble();
        dummy = in.nextDouble();

        m_SpawnPoints.add(new Vector2D(x, y));
    }

    /**
     * deletes all the current objects ready for a map load
     */
    private void Clear() {
        //delete the triggers
        m_TriggerSystem.Clear();

        //delete the doors
        m_Doors.clear();
        m_Walls.clear();
        m_SpawnPoints.clear();

        //delete the navgraph
        m_pNavGraph = null;

        //delete the partioning info
        m_pSpacePartition = null;
    }

//----------------------------- ctor ------------------------------------------
//-----------------------------------------------------------------------------
    public Raven_Map() {
        m_pNavGraph = null;
        m_pSpacePartition = null;
        m_iSizeY = 0;
        m_iSizeX = 0;
        m_dCellSpaceNeighborhoodRange = 0;

    }
//------------------------------ dtor -----------------------------------------
//-----------------------------------------------------------------------------

    protected void finalized() throws Throwable {
        super.finalize();
        Clear();
    }

    public void Render() {
        //render the navgraph
        if (UserOptions.m_bShowGraph) {
            GraphHelper_DrawUsingGDI(m_pNavGraph, Cgdi.grey, UserOptions.m_bShowNodeIndices);
        }
        //render any doors
        Iterator<Raven_Door> curDoor = m_Doors.iterator();
        while (curDoor.hasNext()) {
            curDoor.next().Render();
        }

        //render all the triggers
        m_TriggerSystem.Render();
        //render all the walls
        Iterator<Wall2D> curWall = m_Walls.iterator();
        while (curWall.hasNext()) {
            gdi.ThickBlackPen();
            curWall.next().Render();
        }

        for (Vector2D curSp : m_SpawnPoints) {
            gdi.GreyBrush();
            gdi.GreyPen();
            gdi.Circle(curSp, 7);
        }
    }

    /**
     * load and sets up the game environment from map file
     */
    public boolean LoadMap(final String filename) {
        FileInputStream stream;
        try {
            stream = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            ErrorBox("Bad Map Filename '" + filename + "' (at Raven_Map.java:" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ")");
            return false;
        }
        Scanner in = new Scanner(stream);
        // double values use . as delimiter in Raven maps, ie 3.1415 and not 3,1415
        in.useLocale(Locale.US);
        Clear();

        BaseGameEntity.ResetNextValidID();

        //first of all read and create the navgraph. This must be done before
        //the entities are read from the map file because many of the entities
        //will be linked to a graph node (the graph node will own a pointer
        //to an instance of the entity)
        m_pNavGraph = new NavGraph(false);

        m_pNavGraph.Load(in, Raven_Map.GraphNode.class, NavGraphEdge.class);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("NavGraph for ").print(filename).print(" loaded okay").print("");
        }

        //determine the average distance between graph nodes so that we can
        //partition them efficiently
        m_dCellSpaceNeighborhoodRange = CalculateAverageGraphEdgeLength(m_pNavGraph) + 1;

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Average edge length is ").print(CalculateAverageGraphEdgeLength(m_pNavGraph)).print("");
        }

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Neighborhood range set to ").print(m_dCellSpaceNeighborhoodRange).print("");
        }


        //load in the map size and adjust the client window accordingly
        m_iSizeX = in.nextInt();
        m_iSizeY = in.nextInt();

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Partitioning navgraph nodes...").print("");
        }

        //partition the graph nodes
        PartitionNavGraph();

        //get the handle to the game window and resize the client area to accommodate
        //the map
        //extern char * g_szApplicationName;
        //extern char * g_szWindowClassName;
        //HWND hwnd = FindWindow(g_szWindowClassName, g_szApplicationName);

         Window hwnd = FindWindow();

         final int ExtraHeightRqdToDisplayInfo = 0; // 50;
         ResizeWindow(hwnd, m_iSizeX, m_iSizeY + ExtraHeightRqdToDisplayInfo);

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print("Loading map...").print("");
        }


        //now create the environment entities
        while (in.hasNext()) {
            //get type of next map object
            int EntityType;

            EntityType = in.nextInt();

            if (def(LOG_CREATIONAL_STUFF)) {
                debug_con.print("Creating a ").print(GetNameOfType(EntityType)).print("");
            }

            //create the object
            switch (EntityType) {
                case type_wall:

                    AddWall(in);
                    break;

                case type_sliding_door:

                    AddDoor(in);
                    break;

                case type_door_trigger:

                    AddDoorTrigger(in);
                    break;

                case type_spawn_point:

                    AddSpawnPoint(in);
                    break;

                case type_health:

                    AddHealth_Giver(in);
                    break;

                case type_shotgun:

                    AddWeapon_Giver(type_shotgun, in);
                    break;

                case type_rail_gun:

                    AddWeapon_Giver(type_rail_gun, in);
                    break;

                case type_rocket_launcher:

                    AddWeapon_Giver(type_rocket_launcher, in);
                    break;

                default:

                    throw new RuntimeException("<Map::Load>: Attempting to load undefined object");

                //return false;

            }//end switch
        }

        if (def(LOG_CREATIONAL_STUFF)) {
            debug_con.print(filename).print(" loaded okay").print("");
        }

        //calculate the cost lookup table
        m_PathCosts = CreateAllPairsCostsTable(m_pNavGraph);

        return true;
    }

    /**
     * adds a wall and returns a pointer to that wall. (this method can be used
     * by objects such as doors to add walls to the environment)
     */
    public Wall2D AddWall(Vector2D from, Vector2D to) {
        Wall2D w = new Wall2D(from, to);

        m_Walls.add(w);

        return w;
    }

    /**
     * given the bot that has made a sound, this method adds a SoundMade trigger
     */
    public void AddSoundTrigger(Raven_Bot pSoundSource, double range) {
        m_TriggerSystem.Register(new Trigger_SoundNotify(pSoundSource, range));
    }

    /**
     * Uses the pre-calculated lookup table to determine the cost of traveling
     * from nd1 to nd2
     */
    public double CalculateCostToTravelBetweenNodes(int nd1, int nd2) {
        assert nd1 >= 0 && nd1 < m_pNavGraph.NumNodes()
                && nd2 >= 0 && nd2 < m_pNavGraph.NumNodes() :
                "<Raven_Map::CostBetweenNodes>: invalid index";

        return m_PathCosts.get(nd1).get(nd2);
    }

    /**
     * returns the position of a graph node selected at random
     */
    public Vector2D GetRandomNodeLocation() {

        NavGraph.ConstNodeIterator NodeItr = new NavGraph.ConstNodeIterator(m_pNavGraph);
        int RandIndex = RandInt(0, m_pNavGraph.NumActiveNodes() - 1);
        NavGraphNode pN = NodeItr.begin();
        while (--RandIndex > 0) {
            pN = NodeItr.next();
        }

        return pN.Pos();
    }

    /**
     * givena container of entities in the world this method updates them
     * against all the triggers
     */
    public void UpdateTriggerSystem(List<Raven_Bot> bots) {
        m_TriggerSystem.Update(bots);
    }

    public TriggerSystem.TriggerList GetTriggers() {
        return m_TriggerSystem.GetTriggers();
    }

    public List<Wall2D> GetWalls() {
        return m_Walls;
    }

    public NavGraph GetNavGraph() {
        return m_pNavGraph;
    }

    public List<Raven_Door> GetDoors() {
        return m_Doors;
    }

    public List<Vector2D> GetSpawnPoints() {
        return m_SpawnPoints;
    }

    public CellSpace GetCellSpace() {
        return m_pSpacePartition;
    }

    public Vector2D GetRandomSpawnPoint() {
        return m_SpawnPoints.get(RandInt(0, m_SpawnPoints.size() - 1));
    }

    public int GetSizeX() {
        return m_iSizeX;
    }

    public int GetSizeY() {
        return m_iSizeY;
    }

    public int GetMaxDimension() {
        return Maximum(m_iSizeX, m_iSizeY);
    }

    public double GetCellSpaceNeighborhoodRange() {
        return m_dCellSpaceNeighborhoodRange;
    }
}
