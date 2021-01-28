/**
 * Desc: classes to implement graph algorithms that can be distributed over
 * multiple update-steps
 *
 * Any graphs passed to these functions must conform to the same interface used
 * by the SparseGraph
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.navigation;

import Raven.navigation.SearchTerminationPolicies.TerminationCondition;
import common.Graph.AStarHeuristicPolicies;
import common.Graph.GraphEdgeTypes.GraphEdge;
import common.Graph.GraphEdgeTypes.NavGraphEdge;
import common.Graph.SparseGraph;
import common.misc.PriorityQueue.IndexedPriorityQLow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final public class TimeSlicedGraphAlgorithms {

//these enums are used as return values from each search update method
    public static final int target_found = 0;
    public static final int target_not_found = 1;
    public static final int search_incomplete = 2;

    /**
     * base class to define a common interface for graph search algorithms
     */
    abstract public static class Graph_SearchTimeSliced<edge_type extends Object> {

        public enum SearchType {

            AStar, Dijkstra;
        };
        private SearchType m_SearchType;

        public Graph_SearchTimeSliced(SearchType type) {
            m_SearchType = type;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

        /**
         * When called, this method runs the algorithm through one search cycle.
         * The method returns an enumerated value (target_found,
         * target_not_found, search_incomplete) indicating the status of the
         * search
         */
        public abstract int CycleOnce();

        /**
         * returns the vector of edges that the algorithm has examined
         */
        public abstract List<edge_type> GetSPT();

        /**
         * returns the total cost to the target
         */
        public abstract double GetCostToTarget();

        /**
         * returns a list of node indexes that comprise the shortest path from
         * the source to the target
         */
        public abstract List<Integer> GetPathToTarget();

        /**
         * returns the path as a list of PathEdges
         */
        public abstract List<PathEdge> GetPathAsPathEdges();

        public SearchType GetType() {
            return m_SearchType;
        }
    }

    /**
     * a A* class that enables a search to be completed over multiple
     * update-steps
     */
    public static class Graph_SearchAStar_TS<graph_type extends SparseGraph, heuristic extends AStarHeuristicPolicies.Heuristic>
            extends Graph_SearchTimeSliced<GraphEdge> {
        //create typedefs for the node and edge types used by the graph
        //typedef typename graph_type::EdgeType Edge;
        //class Edge extends GraphEdge {}
        //typedef typename graph_type::NodeType Node;
        //class Node extends NavGraphNode {}

        private graph_type m_Graph;
        /**
         * indexed into my node. Contains the 'real' accumulative cost to that
         * node
         */
        private List<Double> m_GCosts = new ArrayList<Double>();
        /**
         * indexed into by node. Contains the cost from adding m_GCosts[n] to
         * the heuristic cost from n to the target node. This is the vector the
         * iPQ indexes into.
         */
        private ArrayList<Double> m_FCosts = new ArrayList<Double>();
        private List<GraphEdge> m_ShortestPathTree;
        private List<GraphEdge> m_SearchFrontier;
        private int m_iSource;
        private int m_iTarget;
        /**
         * create an indexed priority queue of nodes. The nodes with the lowest
         * overall F cost (G+H) are positioned at the front.
         */
        private IndexedPriorityQLow<Double> m_pPQ;
        heuristic Heuristic;

        public Graph_SearchAStar_TS(final graph_type G,
                int source,
                int target,
                heuristic h) {
            super(SearchType.AStar);
            Heuristic = h;

            m_Graph = G;
            m_ShortestPathTree = new ArrayList<GraphEdge>(Collections.nCopies(G.NumNodes(), (GraphEdge) null));
            m_SearchFrontier = new ArrayList<GraphEdge>(Collections.nCopies(G.NumNodes(), (GraphEdge) null));
            m_GCosts = new ArrayList<Double>(Collections.nCopies(G.NumNodes(), 0.0));
            m_FCosts = new ArrayList<Double>(Collections.nCopies(G.NumNodes(), 0.0));
            m_iSource = source;
            m_iTarget = target;
            //create the PQ   
            m_pPQ = new IndexedPriorityQLow<>(m_FCosts, m_Graph.NumNodes());

            //put the source node on the queue
            m_pPQ.insert(m_iSource);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            m_pPQ = null;
        }

        /**
         * When called, this method pops the next node off the PQ and examines
         * all its edges. The method returns an enumerated value (target_found,
         * target_not_found, search_incomplete) indicating the status of the
         * search
         */
        @Override
        public int CycleOnce() {
            //if the PQ is empty the target has not been found
            if (m_pPQ.empty()) {
                return target_not_found;
            }

            //get lowest cost node from the queue
            int NextClosestNode = m_pPQ.Pop();

            //put the node on the SPT
            m_ShortestPathTree.set(NextClosestNode, m_SearchFrontier.get(NextClosestNode));

            //if the target has been found exit
            if (NextClosestNode == m_iTarget) {
                return target_found;
            }

            //now to test all the edges attached to this node
            SparseGraph.ConstEdgeIterator ConstEdgeItr = new SparseGraph.ConstEdgeIterator(m_Graph, NextClosestNode);

            for (GraphEdge pE = ConstEdgeItr.begin(); !ConstEdgeItr.end(); pE = ConstEdgeItr.next()) {
                //calculate the heuristic cost from this node to the target (H)
                double HCost = Heuristic.Calculate(m_Graph, m_iTarget, pE.To());

                //calculate the 'real' cost to this node from the source (G)
                double GCost = m_GCosts.get(NextClosestNode) + pE.Cost();

                //if the node has not been added to the frontier, add it and update
                //the G and F costs
                if (m_SearchFrontier.get(pE.To()) == null) {
                    m_FCosts.set(pE.To(), GCost + HCost);
                    m_GCosts.set(pE.To(), GCost);

                    m_pPQ.insert(pE.To());

                    m_SearchFrontier.set(pE.To(), pE);
                } //if this node is already on the frontier but the cost to get here
                //is cheaper than has been found previously, update the node
                //costs and frontier accordingly.
                else if ((GCost < m_GCosts.get(pE.To())) && (m_ShortestPathTree.get(pE.To()) == null)) {
                    m_FCosts.set(pE.To(), GCost + HCost);
                    m_GCosts.set(pE.To(), GCost);

                    m_pPQ.ChangePriority(pE.To());

                    m_SearchFrontier.set(pE.To(), pE);
                }
            }

            //there are still nodes to explore
            return search_incomplete;
        }

        /**
         * returns the vector of edges that the algorithm has examined
         */
        //public List<edge_type> GetSPT() { return m_ShortestPahtTree; }
        @Override
        public List<GraphEdge> GetSPT() {
            return m_ShortestPathTree;
        }

        /**
         * returns a vector of node indexes that comprise the shortest path from
         * the source to the target
         */
        @Override
        public List<Integer> GetPathToTarget() {
            List<Integer> path = new LinkedList<Integer>();

            //just return an empty path if no target or no path found
            if (m_iTarget < 0) {
                return path;
            }

            int nd = m_iTarget;

            path.add(nd);

            while ((nd != m_iSource) && (m_ShortestPathTree.get(nd) != null)) {
                nd = m_ShortestPathTree.get(nd).From();

                path.add(0, nd);
            }

            return path;
        }

        /**
         * returns the path as a list of PathEdges
         */
        @Override
        public List<PathEdge> GetPathAsPathEdges() {
            List<PathEdge> path = new LinkedList<PathEdge>();

            //just return an empty path if no target or no path found
            if (m_iTarget < 0) {
                return path;
            }

            int nd = m_iTarget;

            while ((nd != m_iSource) && (m_ShortestPathTree.get(nd) != null)) {
                path.add(0,
                        new PathEdge(m_Graph.GetNode(m_ShortestPathTree.get(nd).From()).Pos(),
                        m_Graph.GetNode(m_ShortestPathTree.get(nd).To()).Pos(),
                        ((NavGraphEdge) m_ShortestPathTree.get(nd)).Flags(),
                        ((NavGraphEdge) m_ShortestPathTree.get(nd)).IDofIntersectingEntity()));

                nd = m_ShortestPathTree.get(nd).From();
            }

            return path;
        }

        /**
         * returns the total cost to the target
         */
        @Override
        public double GetCostToTarget() {
            return m_GCosts.get(m_iTarget);
        }
    }

    /**
     * Dijkstra's algorithm class modified to spread a search over multiple
     * update-steps
     */
    public static class Graph_SearchDijkstras_TS<graph_type extends SparseGraph,
            termination_condition extends TerminationCondition>
            extends Graph_SearchTimeSliced<GraphEdge> {
        //create typedefs for the node and edge types used by the graph
        //typedef typename graph_type::EdgeType Edge;
        //typedef typename graph_type::NodeType Node;

        private final graph_type m_Graph;
        /**
         * indexed into my node. Contains the accumulative cost to that node
         */
        private ArrayList<Double> m_CostToThisNode;
        private List<GraphEdge> m_ShortestPathTree;
        private List<GraphEdge> m_SearchFrontier;
        int m_iSource;
        int m_iTarget;
        /**
         * create an indexed priority queue of nodes. The nodes with the lowest
         * overall F cost (G+H) are positioned at the front.
         */
        private IndexedPriorityQLow<Double> m_pPQ;
        termination_condition TerminationCondition;

        public Graph_SearchDijkstras_TS(final graph_type G,
                int source,
                int target,
                termination_condition TerminationCondition) {
            super(SearchType.Dijkstra);
            this.TerminationCondition = TerminationCondition;
            m_Graph = G;
            m_ShortestPathTree = new ArrayList<>(Collections.nCopies(G.NumNodes(), (GraphEdge) null));
            m_SearchFrontier = new ArrayList<>(Collections.nCopies(G.NumNodes(), (GraphEdge) null));
            m_CostToThisNode = new ArrayList<Double>(Collections.nCopies(G.NumNodes(), 0.0));

            m_iSource = source;
            m_iTarget = target;
            //create the PQ         ,
            m_pPQ = new IndexedPriorityQLow<Double>(m_CostToThisNode, m_Graph.NumNodes());

            //put the source node on the queue
            m_pPQ.insert(m_iSource);
        }

        /**
         * <del>let the search class take care of tidying up memory (the wary
         * amongst you may prefer to use std::auto_ptr or similar to replace the
         * pointer to the termination condition)</del>
         */
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            m_pPQ = null;
        }

        /**
         * When called, this method pops the next node off the PQ and examines
         * all its edges. The method returns an enumerated value (target_found,
         * target_not_found, search_incomplete) indicating the status of the
         * search
         */
        @Override
        public int CycleOnce() {
            //if the PQ is empty the target has not been found
            if (m_pPQ.empty()) {
                return target_not_found;
            }

            //get lowest cost node from the queue
            int NextClosestNode = m_pPQ.Pop();

            //move this node from the frontier to the spanning tree
            m_ShortestPathTree.set(NextClosestNode, m_SearchFrontier.get(NextClosestNode));

            //if the target has been found exit
            if (TerminationCondition.isSatisfied(m_Graph, m_iTarget, NextClosestNode)) {
                //make a note of the node index that has satisfied the condition. This
                //is so we can work backwards from the index to extract the path from
                //the shortest path tree.
                m_iTarget = NextClosestNode;

                return target_found;
            }

            //now to test all the edges attached to this node
            SparseGraph.ConstEdgeIterator ConstEdgeItr = new SparseGraph.ConstEdgeIterator(m_Graph, NextClosestNode);
            for (GraphEdge pE = ConstEdgeItr.begin();
                    !ConstEdgeItr.end();
                    pE = ConstEdgeItr.next()) {
                //the total cost to the node this edge points to is the cost to the
                //current node plus the cost of the edge connecting them.
                double NewCost = m_CostToThisNode.get(NextClosestNode) + pE.Cost();

                //if this edge has never been on the frontier make a note of the cost
                //to get to the node it points to, then add the edge to the frontier
                //and the destination node to the PQ.
                if (m_SearchFrontier.get(pE.To()) == null) {
                    m_CostToThisNode.set(pE.To(), NewCost);

                    m_pPQ.insert(pE.To());

                    m_SearchFrontier.set(pE.To(), pE);
                } //else test to see if the cost to reach the destination node via the
                //current node is cheaper than the cheapest cost found so far. If
                //this path is cheaper, we assign the new cost to the destination
                //node, update its entry in the PQ to reflect the change and add the
                //edge to the frontier
                else if ((NewCost < m_CostToThisNode.get(pE.To()))
                        && (m_ShortestPathTree.get(pE.To()) == null)) {
                    m_CostToThisNode.set(pE.To(), NewCost);

                    //because the cost is less than it was previously, the PQ must be
                    //re-sorted to account for this.
                    m_pPQ.ChangePriority(pE.To());

                    m_SearchFrontier.set(pE.To(), pE);
                }
            }

            //there are still nodes to explore
            return search_incomplete;
        }

        /**
         * returns the vector of edges that the algorithm has examined
         */
        @Override
        public List<GraphEdge> GetSPT() {
            return m_ShortestPathTree;
        }

        /**
         * returns a vector of node indexes that comprise the shortest path from
         * the source to the target
         */
        @Override
        public List<Integer> GetPathToTarget() {
            List<Integer> path = new LinkedList<Integer>();

            //just return an empty path if no target or no path found
            if (m_iTarget < 0) {
                return path;
            }

            int nd = m_iTarget;

            path.add(nd);

            while ((nd != m_iSource) && (m_ShortestPathTree.get(nd) != null)) {
                nd = m_ShortestPathTree.get(nd).From();

                path.add(0, nd);
            }

            return path;
        }

        /**
         * returns the path as a list of PathEdges
         */
        @Override
        public List<PathEdge> GetPathAsPathEdges() {
            List<PathEdge> path = new LinkedList<PathEdge>();

            //just return an empty path if no target or no path found
            if (m_iTarget < 0) {
                return path;
            }

            int nd = m_iTarget;

            while ((nd != m_iSource) && (m_ShortestPathTree.get(nd) != null)) {
                path.add(0, new PathEdge(m_Graph.GetNode(m_ShortestPathTree.get(nd).From()).Pos(),
                        m_Graph.GetNode(m_ShortestPathTree.get(nd).To()).Pos(),
                        ((NavGraphEdge) m_ShortestPathTree.get(nd)).Flags(),
                        ((NavGraphEdge) m_ShortestPathTree.get(nd)).IDofIntersectingEntity()));

                nd = m_ShortestPathTree.get(nd).From();
            }

            return path;
        }

        /**
         * returns the total cost to the target
         */
        @Override
        public double GetCostToTarget() {
            return m_CostToThisNode.get(m_iTarget);
        }
    }
}