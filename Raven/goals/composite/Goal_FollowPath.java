package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_follow_path;
import Raven.goals.atomic.Goal_TraverseEdge;
import Raven.navigation.PathEdge;
import common.Graph.GraphEdgeTypes.NavGraphEdge;
import static common.misc.Cgdi.gdi;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 */
public class Goal_FollowPath extends Goal_Composite<Raven_Bot> {

    /**
     * a local copy of the path returned by the path planner
     */
    private LinkedList<PathEdge> m_Path;

    //the usual suspects
    //------------------------------ ctor -----------------------------------------
    //-----------------------------------------------------------------------------
    public Goal_FollowPath(Raven_Bot pBot,
            List<PathEdge> path) {
        super(pBot, goal_follow_path);
        m_Path = new LinkedList<PathEdge>(path);
    }

    @Override
    public void Activate() {
        m_iStatus = active;

        //get a reference to the next edge
        PathEdge edge = m_Path.getFirst();

        //remove the edge from the path
        m_Path.removeFirst();

        //some edges specify that the bot should use a specific behavior when
        //following them. This switch statement queries the edge behavior flag and
        //adds the appropriate goals/s to the subgoal list.
        switch (edge.Behavior()) {
            case NavGraphEdge.normal: {
                AddSubgoal(new Goal_TraverseEdge(m_pOwner, edge, m_Path.isEmpty()));
            }
            break;

            case NavGraphEdge.goes_through_door: {

                //also add a goal that is able to handle opening the door
                AddSubgoal(new Goal_NegotiateDoor(m_pOwner, edge, m_Path.isEmpty()));
            }
            break;

            case NavGraphEdge.jump: {
                //add subgoal to jump along the edge
            }
            break;

            case NavGraphEdge.grapple: {
                //add subgoal to grapple along the edge
            }
            break;

            default:
                throw new RuntimeException("<Goal_FollowPath::Activate>: Unrecognized edge type");
        }
    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        m_iStatus = ProcessSubgoals();

        //if there are no subgoals present check to see if the path still has edges.
        //remaining. If it does then call activate to grab the next edge.
        if (m_iStatus == completed && !m_Path.isEmpty()) {
            Activate();
        }

        return m_iStatus;
    }

    @Override
    public void Render() {
        //render all the path waypoints remaining on the path list

        Iterator<PathEdge> it = m_Path.iterator();

        while (it.hasNext()) {
            PathEdge path = it.next();
            gdi.BlackPen();
            gdi.LineWithArrow(path.Source(), path.Destination(), 5);

            gdi.RedBrush();
            gdi.BlackPen();
            gdi.Circle(path.Destination(), 3);
        }

        //forward the request to the subgoals
        super.Render();
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
    }
}
