package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_hunt_target;
import common.D2.Vector2D;
import static Raven.DEFINE.*;
import static common.misc.Cgdi.gdi;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: Causes a bot to search for its current target. Exits when target is in
 * view
 */
public class Goal_HuntTarget extends Goal_Composite<Raven_Bot> {

    /**
     * this value is set to true if the last visible position of the target bot
     * has been searched without success
     */
    private boolean m_bLVPTried;

    public Goal_HuntTarget(Raven_Bot pBot) {
        super(pBot, goal_hunt_target);
        m_bLVPTried = false;
    }

    //the usual suspects
    @Override
    public void Activate() {
        m_iStatus = active;

        //if this goal is reactivated then there may be some existing subgoals that
        //must be removed
        RemoveAllSubgoals();

        //it is possible for the target to die whilst this goal is active so we
        //must test to make sure the bot always has an active target
        if (m_pOwner.GetTargetSys().isTargetPresent()) {
            //grab a local copy of the last recorded position (LRP) of the target
            final Vector2D lrp = m_pOwner.GetTargetSys().GetLastRecordedPosition();

            //if the bot has reached the LRP and it still hasn't found the target
            //it starts to search by using the explore goal to move to random
            //map locations
            if (lrp.isZero() || m_pOwner.isAtPosition(lrp)) {
                AddSubgoal(new Goal_Explore(m_pOwner));
            } //else move to the LRP
            else {
                AddSubgoal(new Goal_MoveToPosition(m_pOwner, lrp));
            }
        } //if their is no active target then this goal can be removed from the queue
        else {
            m_iStatus = completed;
        }

    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        m_iStatus = ProcessSubgoals();

        //if target is in view this goal is satisfied
        if (m_pOwner.GetTargetSys().isTargetWithinFOV()) {
            m_iStatus = completed;
        }

        return m_iStatus;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
    }

    @Override
    public void Render() {
        //define(SHOW_LAST_RECORDED_POSITION);
        if (def(SHOW_LAST_RECORDED_POSITION)) {
            //render last recorded position as a green circle
            if (m_pOwner.GetTargetSys().isTargetPresent()) {
                gdi.GreenPen();
                gdi.RedBrush();
                gdi.Circle(m_pOwner.GetTargetSys().GetLastRecordedPosition(), 3);
            }
        }

    }
}