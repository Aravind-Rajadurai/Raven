package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_explore;
import Raven.goals.atomic.Goal_SeekToPosition;
import common.D2.Vector2D;
import common.Messaging.Telegram;

public class Goal_Explore extends Goal_Composite<Raven_Bot> {

    private Vector2D m_CurrentDestination = new Vector2D();
    /**
     * set to true when the destination for the exploration has been established
     */
    private boolean m_bDestinationIsSet;

    public Goal_Explore(Raven_Bot pOwner) {
        super(pOwner, goal_explore);
        m_bDestinationIsSet = false;
    }

    @Override
    public void Activate() {
        m_iStatus = active;

        //if this goal is reactivated then there may be some existing subgoals that
        //must be removed
        RemoveAllSubgoals();

        if (!m_bDestinationIsSet) {
            //grab a random location
            m_CurrentDestination = m_pOwner.GetWorld().GetMap().GetRandomNodeLocation();

            m_bDestinationIsSet = true;
        }

        //and request a path to that position
        m_pOwner.GetPathPlanner().RequestPathToPosition(m_CurrentDestination);

        //the bot may have to wait a few update cycles before a path is calculated
        //so for appearances sake it simple ARRIVES at the destination until a path
        //has been found
        AddSubgoal(new Goal_SeekToPosition(m_pOwner, m_CurrentDestination));
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

    @Override
    public boolean HandleMessage(final Telegram msg) {
        //first, pass the message down the goal hierarchy
        boolean bHandled = ForwardMessageToFrontMostSubgoal(msg);

        //if the msg was not handled, test to see if this goal can handle it
        if (bHandled == false) {
            switch (msg.Msg) {
                case Msg_PathReady:

                    //clear any existing goals
                    RemoveAllSubgoals();

                    AddSubgoal(new Goal_FollowPath(m_pOwner,
                            m_pOwner.GetPathPlanner().GetPath()));

                    return true; //msg handled


                case Msg_NoPathAvailable:

                    m_iStatus = failed;

                    return true; //msg handled

                default:
                    return false;
            }
        }

        //handled by subgoals
        return true;
    }
}