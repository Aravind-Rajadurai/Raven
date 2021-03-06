package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_move_to_position;
import Raven.goals.atomic.Goal_SeekToPosition;
import common.D2.Vector2D;
import common.Messaging.Telegram;
import static common.misc.Cgdi.gdi;

public class Goal_MoveToPosition extends Goal_Composite<Raven_Bot> {

    /**
     * the position the bot wants to reach
     */
    private Vector2D m_vDestination;

    public Goal_MoveToPosition(Raven_Bot pBot,
            Vector2D pos) {

        super(pBot, goal_move_to_position);
        m_vDestination = new Vector2D(pos);
    }

    //the usual suspects
    @Override
    public void Activate() {
        m_iStatus = active;

        //make sure the subgoal list is clear.
        RemoveAllSubgoals();

        //requests a path to the target position from the path planner. Because, for
        //demonstration purposes, the Raven path planner uses time-slicing when 
        //processing the path requests the bot may have to wait a few update cycles
        //before a path is calculated. Consequently, for appearances sake, it just
        //seeks directly to the target position whilst it's awaiting notification
        //that the path planning request has succeeded/failed
        if (m_pOwner.GetPathPlanner().RequestPathToPosition(m_vDestination)) {
            AddSubgoal(new Goal_SeekToPosition(m_pOwner, m_vDestination));
        }
    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //process the subgoals
        m_iStatus = ProcessSubgoals();

        //if any of the subgoals have failed then this goal re-plans
        ReactivateIfFailed();

        return m_iStatus;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
    }

    /**
     * this goal is able to accept messages
     */
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

    @Override
    public void Render() {
        //forward the request to the subgoals
        super.Render();

        //draw a bullseye
        gdi.BlackPen();
        gdi.BlueBrush();
        gdi.Circle(m_vDestination, 6);
        gdi.RedBrush();
        gdi.RedPen();
        gdi.Circle(m_vDestination, 4);
        gdi.YellowBrush();
        gdi.YellowPen();
        gdi.Circle(m_vDestination, 2);
    }
}