package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_adjust_range;
import Raven.goals.atomic.Goal;

/**
 * Author: Mat Buckland (ai-junkie.com)
 */
public class Goal_AdjustRange extends Goal<Raven_Bot> {

    private Raven_Bot m_pTarget;
    private double m_dIdealRange;

    public Goal_AdjustRange(Raven_Bot pBot) {
        super(pBot, goal_adjust_range);
        m_dIdealRange = 0;
    }

//---------------------------- Initialize -------------------------------------
//-----------------------------------------------------------------------------  
    @Override
    public void Activate() {
        m_pOwner.GetSteering().SetTarget(m_pOwner.GetTargetBot().Pos());
    }

//------------------------------ Process --------------------------------------
//-----------------------------------------------------------------------------
    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        /*
         if (m_pOwner.GetCurrentWeapon().isInIdealWeaponRange())
         {
         m_iStatus = completed;
         }
         */
        return m_iStatus;
    }

//---------------------------- Terminate --------------------------------------
//-----------------------------------------------------------------------------
    @Override
    public void Terminate() {
    }
}