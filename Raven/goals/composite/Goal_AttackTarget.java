package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_attack_target;
import Raven.goals.atomic.Goal_DodgeSideToSide;
import Raven.goals.atomic.Goal_SeekToPosition;
import common.D2.Vector2D;

/**
 * Author: Mat Buckland (ai-junkie.com)
 */
public class Goal_AttackTarget extends Goal_Composite<Raven_Bot> {

    public Goal_AttackTarget(Raven_Bot pOwner) {
        super(pOwner, goal_attack_target);
    }

    @Override
    public void Activate() {
        m_iStatus = active;

        //if this goal is reactivated then there may be some existing subgoals that
        //must be removed
        RemoveAllSubgoals();

        //it is possible for a bot's target to die whilst this goal is active so we
        //must test to make sure the bot always has an active target
        if (!m_pOwner.GetTargetSys().isTargetPresent()) {
            m_iStatus = completed;

            return;
        }

        //if the bot is able to shoot the target (there is LOS between bot and
        //target), then select a tactic to follow while shooting
        if (m_pOwner.GetTargetSys().isTargetShootable()) {
            //if the bot has space to strafe then do so
            Vector2D dummy = new Vector2D();
            if (m_pOwner.canStepLeft(dummy) || m_pOwner.canStepRight(dummy)) {
                AddSubgoal(new Goal_DodgeSideToSide(m_pOwner));
            } //if not able to strafe, head directly at the target's position 
            else {
                AddSubgoal(new Goal_SeekToPosition(m_pOwner, m_pOwner.GetTargetBot().Pos()));
            }
        } //if the target is not visible, go hunt it.
        else {
            AddSubgoal(new Goal_HuntTarget(m_pOwner));
        }
    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //process the subgoals
        m_iStatus = ProcessSubgoals();

        ReactivateIfFailed();

        return m_iStatus;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
        m_iStatus = completed;
    }
}