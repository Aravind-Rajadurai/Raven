package Raven.goals.atomic;

import Raven.Raven_Bot;
import static Raven.goals.atomic.Goal.active;
import static Raven.goals.Raven_Goal_Types.goal_wander;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 * 
* Desc: Causes a bot to wander until terminated
 */
public class Goal_Wander extends Goal<Raven_Bot> {

    public Goal_Wander(Raven_Bot pBot) {
        super(pBot, goal_wander);
    }

    //---------------------------- Initialize -------------------------------------
    //-----------------------------------------------------------------------------  
    @Override
    public void Activate() {
        m_iStatus = active;

        m_pOwner.GetSteering().WanderOn();
    }

    //------------------------------ Process --------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        return m_iStatus;
    }

    //---------------------------- Terminate --------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    public void Terminate() {
        m_pOwner.GetSteering().WanderOff();
    }
}