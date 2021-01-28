package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_negotiate_door;
import Raven.goals.atomic.Goal_TraverseEdge;
import Raven.navigation.PathEdge;
import common.D2.Vector2D;

public class Goal_NegotiateDoor extends Goal_Composite<Raven_Bot> {

    private PathEdge m_PathEdge;
    private boolean m_bLastEdgeInPath;

    //---------------------------- ctor -------------------------------------------
    //-----------------------------------------------------------------------------
    public Goal_NegotiateDoor(Raven_Bot pBot,
            PathEdge edge,
            boolean LastEdge) {
        super(pBot, goal_negotiate_door);
        m_PathEdge = new PathEdge(edge);
        m_bLastEdgeInPath = LastEdge;

    }

    //the usual suspects
    @Override
    public void Activate() {
        m_iStatus = active;

        //if this goal is reactivated then there may be some existing subgoals that
        //must be removed
        RemoveAllSubgoals();

        //get the position of the closest navigable switch
        Vector2D posSw = m_pOwner.GetWorld().GetPosOfClosestSwitch(m_pOwner.Pos(),
                m_PathEdge.DoorID());

        //because goals are *pushed* onto the front of the subgoal list they must
        //be added in reverse order.

        //first the goal to traverse the edge that passes through the door
        AddSubgoal(new Goal_TraverseEdge(m_pOwner, m_PathEdge, m_bLastEdgeInPath));

        //next, the goal that will move the bot to the beginning of the edge that
        //passes through the door
        AddSubgoal(new Goal_MoveToPosition(m_pOwner, m_PathEdge.Source()));

        //finally, the Goal that will direct the bot to the location of the switch
        AddSubgoal(new Goal_MoveToPosition(m_pOwner, posSw));
    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //process the subgoals
        m_iStatus = ProcessSubgoals();

        return m_iStatus;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
    }
}