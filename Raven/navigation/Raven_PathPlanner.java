package Raven.navigation;

import Raven.Raven_Bot;
import Raven.Raven_Map;
import static Raven.Raven_Messages.message_type.Msg_NoPathAvailable;
import static Raven.Raven_Messages.message_type.Msg_PathReady;
import static Raven.Raven_UserOptions.UserOptions;
import static Raven.DEFINE.*;
import Raven.navigation.SearchTerminationPolicies.FindActiveTrigger;
import Raven.navigation.TimeSlicedGraphAlgorithms.Graph_SearchTimeSliced;
import Raven.navigation.TimeSlicedGraphAlgorithms.Graph_SearchAStar_TS;
import Raven.navigation.TimeSlicedGraphAlgorithms.Graph_SearchDijkstras_TS;
import static Raven.navigation.TimeSlicedGraphAlgorithms.target_found;
import static Raven.navigation.TimeSlicedGraphAlgorithms.target_not_found;
import common.D2.Vector2D;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Vector2D.Vec2DDistanceSq;
import static common.Debug.DbgConsole.debug_con;
import static common.Graph.NodeTypeEnumerations.invalid_node_index;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SENDER_ID_IRRELEVANT;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.Messaging.MessageDispatcher.NO_ADDITIONAL_INFO;
import common.Graph.GraphEdgeTypes.NavGraphEdge;
import common.Graph.GraphNodeTypes.NavGraphNode;
import common.Graph.AStarHeuristicPolicies.Heuristic_Euclid;
import common.Triggers.Trigger;
import common.Triggers.TriggerSystem;
import static common.misc.utils.isEqual;
import static common.misc.utils.MaxDouble;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class to handle the creation of paths through a navigation graph
 */
public class Raven_PathPlanner {

    static {
        //Raven.DEFINE.define(Raven.DEFINE.SHOW_NAVINFO);
    }
    /**
     * for legibility
     */
    private int no_closest_node_found = -1;
    //for ease of use typdef the graph edge/node types used by the navgraph
    //public typedef Raven_Map::NavGraph::EdgeType           EdgeType; == NavGraphEdge
    //public typedef Raven_Map::NavGraph::NodeType           NodeType; == Raven_Map.GraphNode
    // typedef ArrayList<PathEdge> Path;
    /**
     * A pointer to the owner of this class
     */
    private Raven_Bot m_pOwner;
    /**
     * a reference to the navgraph
     */
    private final Raven_Map.NavGraph m_NavGraph;
    /**
     * a pointer to an instance of the current graph search algorithm.
     */
    private Graph_SearchTimeSliced<?> m_pCurrentSearch;
    /**
     * this is the position the bot wishes to plan a path to reach
     */
    private Vector2D m_vDestinationPos = new Vector2D();

    /**
     * returns the index of the closest visible and unobstructed graph node to
     * the given position
     */
    private int GetClosestNodeToPosition(Vector2D pos) {
        double ClosestSoFar = MaxDouble;
        int ClosestNode = no_closest_node_found;

        //when the cell space is queried this the the range searched for neighboring
        //graph nodes. This value is inversely proportional to the density of a 
        //navigation graph (less dense = bigger values)
        final double range = m_pOwner.GetWorld().GetMap().GetCellSpaceNeighborhoodRange();

        //calculate the graph nodes that are neighboring this position
        m_pOwner.GetWorld().GetMap().GetCellSpace().CalculateNeighbors(pos, range);

        //iterate through the neighbors and sum up all the position vectors
        for (NavGraphNode<Trigger<Raven_Bot>> pN = m_pOwner.GetWorld().GetMap().GetCellSpace().begin();
                !m_pOwner.GetWorld().GetMap().GetCellSpace().end();
                pN = m_pOwner.GetWorld().GetMap().GetCellSpace().next()) {
            //if the path between this node and pos is unobstructed calculate the
            //distance
            if (m_pOwner.canWalkBetween(pos, pN.Pos())) {
                double dist = Vec2DDistanceSq(pos, pN.Pos());

                //keep a record of the closest so far
                if (dist < ClosestSoFar) {
                    ClosestSoFar = dist;
                    ClosestNode = pN.Index();
                }
            }
        }

        return ClosestNode;
    }

    /**
     * smooths a path by removing extraneous edges. (may not remove all
     * extraneous edges)
     */
    private void SmoothPathEdgesQuick(List<PathEdge> path) {
        //create a couple of iterators and point them at the front of the path
        ListIterator<PathEdge> e1 = path.listIterator();
        ListIterator<PathEdge> e2 = path.listIterator();

        if (!e2.hasNext()) {
            return;
        }
        PathEdge e1e = e1.next();
        PathEdge e2e = e2.next();
        if (!e2.hasNext()) {
            return;
        }

        //while e2 is not the last edge in the path, step through the edges checking
        //to see if the agent can move without obstruction from the source node of
        //e1 to the destination node of e2. If the agent can move between those 
        //positions then the two edges are replaced with a single edge.
        while (e2.hasNext()) {
            //increment e2 so it points to the edge following e1 (and futher)
            e2e = e2.next();
            //check for obstruction, adjust and remove the edges accordingly
            if ((e2e.Behavior() == NavGraphEdge.normal)
                    && m_pOwner.canWalkBetween(e1e.Source(), e2e.Destination())) {
                e1e.SetDestination(e2e.Destination());
                e2.remove();
                //e1 = path.listIterator(e1.nextIndex()-1); // or ConcurrentModificationException
            } else {
                e1e = e2e;
                /*
                 We do not need e1 iterator in Java implementation of this method :)
                 do {
                 e1e = e1.next();
                 } while(e1e != e2e);
                 */
            }
        }
    }

    /**
     * smooths a path by removing extraneous edges. (removes *all* extraneous
     * edges)
     */
    private void SmoothPathEdgesPrecise(List<PathEdge> path) {
        //create a couple of iterators
        ListIterator<PathEdge> e1 = path.listIterator();
        ListIterator<PathEdge> e2 = path.listIterator();


        while (e1.hasNext() && e2.hasNext()) {
            PathEdge e1e = e1.next();
            e2.next();

            //while e2 is not the last edge in the path, step through the edges
            //checking to see if the agent can move without obstruction from the 
            //source node of e1 to the destination node of e2. If the agent can move
            //between those positions then the any edges between e1 and e2 are
            //replaced with a single edge.
            while (e2.hasNext()) {
                //point e2 to the edge immediately following e1 (and futher)
                PathEdge e2e = e2.next();

                //check for obstruction, adjust and remove the edges accordingly
                if ((e2e.Behavior() == NavGraphEdge.normal)
                        && m_pOwner.canWalkBetween(e1e.Source(), e2e.Destination())) {
                    e1e.SetDestination(e2e.Destination());
                    path.subList(e1.nextIndex(), e2.nextIndex()).clear();
                    e1 = path.listIterator(e1.nextIndex());
                    e2 = path.listIterator(e1.nextIndex());
                }
            }
        }
    }

    /**
     * called by the search manager when a search has been terminated to free up
     * the memory used when an instance of the search was created. It clears up
     * the appropriate lists and memory in preparation for a new search request
     */
    private void GetReadyForNewSearch() {
        //unregister any existing search with the path manager
        m_pOwner.GetWorld().GetPathManager().UnRegister(this);

        //clean up memory used by any existing search
        m_pCurrentSearch = null;
    }

//---------------------------- ctor -------------------------------------------
//-----------------------------------------------------------------------------
    public Raven_PathPlanner(Raven_Bot owner) {
        m_pOwner = owner;
        m_NavGraph = m_pOwner.GetWorld().GetMap().GetNavGraph();
        m_pCurrentSearch = null;
    }

//-------------------------- dtor ---------------------------------------------
//-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        GetReadyForNewSearch();
    }

    /**
     * Given an item type, this method determines the closest reachable graph
     * node to the bot's position and then creates a instance of the time-sliced
     * Dijkstra's algorithm, which it registers with the search manager
     */
    public boolean RequestPathToItem(int ItemType) {
        //clear the waypoint list and delete any active search
        GetReadyForNewSearch();

        //find the closest visible node to the bots position
        int ClosestNodeToBot = GetClosestNodeToPosition(m_pOwner.Pos());

        //remove the destination node from the list and return false if no visible
        //node found. This will occur if the navgraph is badly designed or if the bot
        //has managed to get itself *inside* the geometry (surrounded by walls),
        //or an obstacle
        if (ClosestNodeToBot == no_closest_node_found) {
            if (def(SHOW_NAVINFO)) {
                debug_con.print("No closest node to bot found!").print("");
            }

            return false;
        }

        //create an instance of the search algorithm
        class t_con extends FindActiveTrigger<Trigger<Raven_Bot>> {
        };
        class DijSearch extends Graph_SearchDijkstras_TS<Raven_Map.NavGraph, t_con> {

            public DijSearch(final Raven_Map.NavGraph G,
                    int source,
                    int target,
                    t_con TC) {
                super(G, source, target, TC);
            }
        };

        m_pCurrentSearch = new DijSearch(m_NavGraph,
                ClosestNodeToBot,
                ItemType, new t_con());

        //register the search with the path manager
        m_pOwner.GetWorld().GetPathManager().Register(this);

        return true;
    }

    /**
     * Given a target, this method first determines if nodes can be reached from
     * the bot's current position and the target position. If either end point
     * is unreachable the method returns false.
     *
     * If nodes are reachable from both positions then an instance of the time-
     * sliced A* search is created and registered with the search manager. the
     * method then returns true.
     */
    public boolean RequestPathToPosition(Vector2D TargetPos) {
        if (def(SHOW_NAVINFO)) {
            debug_con.print("------------------------------------------------").print("");
        }
        GetReadyForNewSearch();

        //make a note of the target position.
        m_vDestinationPos = new Vector2D(TargetPos);

        //if the target is walkable from the bot's position a path does not need to
        //be calculated, the bot can go straight to the position by ARRIVING at
        //the current waypoint
        if (m_pOwner.canWalkTo(TargetPos)) {
            return true;
        }

        //find the closest visible node to the bots position
        int ClosestNodeToBot = GetClosestNodeToPosition(m_pOwner.Pos());

        //remove the destination node from the list and return false if no visible
        //node found. This will occur if the navgraph is badly designed or if the bot
        //has managed to get itself *inside* the geometry (surrounded by walls),
        //or an obstacle.
        if (ClosestNodeToBot == no_closest_node_found) {
            if (def(SHOW_NAVINFO)) {
                debug_con.print("No closest node to bot found!").print("");
            }

            return false;
        }

        if (def(SHOW_NAVINFO)) {
            debug_con.print("Closest node to bot is ").print(ClosestNodeToBot).print("");
        }

        //find the closest visible node to the target position
        int ClosestNodeToTarget = GetClosestNodeToPosition(TargetPos);

        //return false if there is a problem locating a visible node from the target.
        //This sort of thing occurs much more frequently than the above. For
        //example, if the user clicks inside an area bounded by walls or inside an
        //object.
        if (ClosestNodeToTarget == no_closest_node_found) {
            if (def(SHOW_NAVINFO)) {
                debug_con.print("No closest node to target (").print(ClosestNodeToTarget).print(") found!").print("");
            }

            return false;
        }

        if (def(SHOW_NAVINFO)) {
            debug_con.print("Closest node to target is ").print(ClosestNodeToTarget).print("");
        }

        //create an instance of a the distributed A* search class
        //typedef Graph_SearchAStar_TS<Raven_Map::NavGraph, Heuristic_Euclid> AStar;

        m_pCurrentSearch = new Graph_SearchAStar_TS<Raven_Map.NavGraph, Heuristic_Euclid>(m_NavGraph,
                ClosestNodeToBot,
                ClosestNodeToTarget, new Heuristic_Euclid());

        //and register the search with the path manager
        m_pOwner.GetWorld().GetPathManager().Register(this);

        return true;
    }

    /**
     * called by an agent after it has been notified that a search has
     * terminated successfully. The method extracts the path from
     * m_pCurrentSearch, adds additional edges appropriate to the search type
     * and returns it as a list of PathEdges.
     */
    public List<PathEdge> GetPath() {
        assert (m_pCurrentSearch != null) :
                "<Raven_PathPlanner::GetPathAsNodes>: no current search";

        List<PathEdge> path = m_pCurrentSearch.GetPathAsPathEdges();

        int closest = GetClosestNodeToPosition(m_pOwner.Pos());

        path.add(0, new PathEdge(m_pOwner.Pos(),
                GetNodePosition(closest),
                NavGraphEdge.normal));


        //if the bot requested a path to a location then an edge leading to the
        //destination must be added
        if (m_pCurrentSearch.GetType() == Graph_SearchTimeSliced.SearchType.AStar) {
            path.add(new PathEdge(path.get(path.size() - 1).Destination(),
                    m_vDestinationPos,
                    NavGraphEdge.normal));
        }

        //smooth paths if required
        if (UserOptions.m_bSmoothPathsQuick) {
            SmoothPathEdgesQuick(path);
        }

        if (UserOptions.m_bSmoothPathsPrecise) {
            SmoothPathEdgesPrecise(path);
        }

        return path;
    }

    /**
     * returns the cost to travel from the bot's current position to a specific
     * graph node. This method makes use of the pre-calculated lookup table
     * created by Raven_Game
     */
    public double GetCostToNode(int NodeIdx) {
        //find the closest visible node to the bots position
        int nd = GetClosestNodeToPosition(m_pOwner.Pos());

        //add the cost to this node
        double cost = Vec2DDistance(m_pOwner.Pos(),
                m_NavGraph.GetNode(nd).Pos());

        //add the cost to the target node and return
        return cost + m_pOwner.GetWorld().GetMap().CalculateCostToTravelBetweenNodes(nd, NodeIdx);
    }

    /**
     * returns the cost to the closest instance of the giver type. This method
     * makes use of the pre-calculated lookup table. Returns -1 if no active
     * trigger found
     */
    public double GetCostToClosestItem(int GiverType) {
        //find the closest visible node to the bots position
        int nd = GetClosestNodeToPosition(m_pOwner.Pos());

        //if no closest node found return failure
        if (nd == invalid_node_index) {
            return -1;
        }

        double ClosestSoFar = MaxDouble;
        //iterate through all the triggers to find the closest *active* trigger of
        //type GiverType
        final TriggerSystem.TriggerList triggers = m_pOwner.GetWorld().GetMap().GetTriggers();
        //  Raven_Map::TriggerSystem::TriggerList::const_iterator it;
        Iterator<Trigger> it = triggers.iterator();
        while (it.hasNext()) {
            Trigger tr = it.next();
            if ((tr.EntityType() == GiverType) && tr.isActive()) {
                double cost =
                        m_pOwner.GetWorld().GetMap().CalculateCostToTravelBetweenNodes(nd,
                        tr.GraphNodeIndex());

                if (cost < ClosestSoFar) {
                    ClosestSoFar = cost;
                }
            }
        }

        //return a negative value if no active trigger of the type found
        if (isEqual(ClosestSoFar, MaxDouble)) {
            return -1;
        }

        return ClosestSoFar;
    }

    /**
     * the path manager calls this to iterate once though the search cycle of
     * the currently assigned search algorithm. When a search is terminated the
     * method messages the owner with either the msg_NoPathAvailable or
     * msg_PathReady messages
     */
    public int CycleOnce() {
        assert (m_pCurrentSearch != null) : "<Raven_PathPlanner::CycleOnce>: No search object instantiated";

        int result = m_pCurrentSearch.CycleOnce();

        //let the bot know of the failure to find a path
        if (result == target_not_found) {
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    SENDER_ID_IRRELEVANT,
                    m_pOwner.ID(),
                    Msg_NoPathAvailable,
                    NO_ADDITIONAL_INFO);
        } //let the bot know a path has been found
        else if (result == target_found) {
            //if the search was for an item type then the final node in the path will
            //represent a giver trigger. Consequently, it's worth passing the pointer
            //to the trigger in the extra info field of the message. (The pointer
            //will just be NULL if no trigger)
            List<Integer> p = m_pCurrentSearch.GetPathToTarget();
            Object pTrigger =
                    m_NavGraph.GetNode(p.get(p.size() - 1)).ExtraInfo();

            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    SENDER_ID_IRRELEVANT,
                    m_pOwner.ID(),
                    Msg_PathReady,
                    pTrigger);
        }

        return result;
    }

    public Vector2D GetDestination() {
        return new Vector2D(m_vDestinationPos);
    }

    public void SetDestination(Vector2D NewPos) {
        m_vDestinationPos = new Vector2D(NewPos);
    }

    /**
     * used to retrieve the position of a graph node from its index. (takes into
     * account the enumerations 'non_graph_source_node' and
     * 'non_graph_target_node'
     */
    public Vector2D GetNodePosition(int idx) {
        return m_NavGraph.GetNode(idx).Pos();
    }
}