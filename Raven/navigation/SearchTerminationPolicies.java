/**
 * Desc: class templates to define termination policies for Dijkstra's algorithm
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.navigation;

import common.Graph.Graph;
import common.Graph.GraphNodeTypes.NavGraphNode;

/**
 *
 * @author Petr
 */
public class SearchTerminationPolicies {

    public static interface TerminationCondition {

        public <graph_type extends Graph> boolean isSatisfied(final graph_type G, int target, int CurrentNodeIdx);
    }

    /**
     * the search will terminate when the currently examined graph node is the
     * same as the target node.
     */
    public static class FindNodeIndex implements TerminationCondition {

        @Override
        public <graph_type extends Graph> boolean isSatisfied(final graph_type G, int target, int CurrentNodeIdx) {
            return CurrentNodeIdx == target;
        }
    }

    /**
     * the search will terminate when the currently examined graph node is the
     * same as the target node.
     */
    public static class FindActiveTrigger<trigger_type> implements TerminationCondition {

        @Override
        public <graph_type extends Graph> boolean isSatisfied(final graph_type G, int target, int CurrentNodeIdx) {
            boolean bSatisfied = false;

            //get a reference to the node at the given node index
            final NavGraphNode node = G.GetNode(CurrentNodeIdx);

            //if the extrainfo field is pointing to a giver-trigger, test to make sure 
            //it is active and that it is of the correct type.
            if ((node.ExtraInfo() != null)
                    && node.ExtraInfo().isActive()
                    && (node.ExtraInfo().EntityType() == target)) {
                bSatisfied = true;
            }

            return bSatisfied;
        }
    }
}