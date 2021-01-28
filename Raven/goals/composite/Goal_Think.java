package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_shotgun;
import static Raven.Raven_ObjectEnumerations.type_rail_gun;
import static Raven.Raven_ObjectEnumerations.type_rocket_launcher;
import static Raven.goals.Raven_Goal_Types.goal_attack_target;
import static Raven.goals.Raven_Goal_Types.goal_explore;
import static Raven.goals.Raven_Goal_Types.goal_think;
import Raven.goals.atomic.Goal;
import Raven.goals.evaluation.AttackTargetGoal_Evaluator;
import Raven.goals.evaluation.ExploreGoal_Evaluator;
import Raven.goals.evaluation.GetHealthGoal_Evaluator;
import Raven.goals.evaluation.GetWeaponGoal_Evaluator;
import Raven.goals.evaluation.Goal_Evaluator;
import common.D2.Vector2D;
import static common.misc.utils.RandInRange;
import common.misc.Cgdi;
import static common.misc.Cgdi.gdi;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 * 
* Desc: class to arbitrate between a collection of high level goals, and to
 * process those goals.
 */
public class Goal_Think extends Goal_Composite<Raven_Bot> {

    private class GoalEvaluators extends ArrayList<Goal_Evaluator> {
    }
    private GoalEvaluators m_Evaluators = new GoalEvaluators();

    public Goal_Think(Raven_Bot pBot) {
        super(pBot, goal_think);
        //these biases could be loaded in from a script on a per bot basis
        //but for now we'll just give them some random values
        final double LowRangeOfBias = 0.5;
        final double HighRangeOfBias = 1.5;

        double HealthBias = RandInRange(LowRangeOfBias, HighRangeOfBias);
        double ShotgunBias = RandInRange(LowRangeOfBias, HighRangeOfBias);
        double RocketLauncherBias = RandInRange(LowRangeOfBias, HighRangeOfBias);
        double RailgunBias = RandInRange(LowRangeOfBias, HighRangeOfBias);
        double ExploreBias = RandInRange(LowRangeOfBias, HighRangeOfBias);
        double AttackBias = RandInRange(LowRangeOfBias, HighRangeOfBias);

        //create the evaluator objects
        m_Evaluators.add(new GetHealthGoal_Evaluator(HealthBias));
        m_Evaluators.add(new ExploreGoal_Evaluator(ExploreBias));
        m_Evaluators.add(new AttackTargetGoal_Evaluator(AttackBias));
        m_Evaluators.add(new GetWeaponGoal_Evaluator(ShotgunBias,
                type_shotgun));
        m_Evaluators.add(new GetWeaponGoal_Evaluator(RailgunBias,
                type_rail_gun));
        m_Evaluators.add(new GetWeaponGoal_Evaluator(RocketLauncherBias,
                type_rocket_launcher));
    }

//----------------------------- dtor ------------------------------------------
//-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Iterator<Goal_Evaluator> it = m_Evaluators.iterator();
        while (it.hasNext()) {
            it.remove();
        }
    }

    /**
     * this method iterates through each goal evaluator and selects the one that
     * has the highest score as the current goal
     */
    public void Arbitrate() {
        double best = 0;
        Goal_Evaluator MostDesirable = null;

        //iterate through all the evaluators to see which produces the highest score
        Iterator<Goal_Evaluator> it = m_Evaluators.iterator();
        while (it.hasNext()) {
            Goal_Evaluator curDes = it.next();
            double desirabilty = curDes.CalculateDesirability(m_pOwner);

            if (desirabilty >= best) {
                best = desirabilty;
                MostDesirable = curDes;
            }
        }

        assert (MostDesirable != null) : "<Goal_Think::Arbitrate>: no evaluator selected";

        MostDesirable.SetGoal(m_pOwner);
    }

    /**
     * returns true if the goal type passed as a parameter is the same as this
     * goal or any of its subgoals
     */
    public boolean notPresent(int GoalType) {
        if (!m_SubGoals.isEmpty()) {
            return m_SubGoals.getFirst().GetType() != GoalType;
        }

        return true;
    }

    //the usual suspects
    /**
     * processes the subgoals
     */
    @Override
    public int Process() {
        ActivateIfInactive();

        int SubgoalStatus = ProcessSubgoals();

        if (SubgoalStatus == completed || SubgoalStatus == failed) {
            if (!m_pOwner.isPossessed()) {
                m_iStatus = inactive;
            }
        }

        return m_iStatus;
    }

    @Override
    public void Activate() {
        if (!m_pOwner.isPossessed()) {
            Arbitrate();
        }

        m_iStatus = active;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
    }

    //top level goal types
    public void AddGoal_MoveToPosition(Vector2D pos) {
        AddSubgoal(new Goal_MoveToPosition(m_pOwner, pos));
    }

    public void AddGoal_GetItem(int ItemType) {
        if (notPresent(Goal_GetItem.ItemTypeToGoalType(ItemType))) {
            RemoveAllSubgoals();
            AddSubgoal(new Goal_GetItem(m_pOwner, ItemType));
        }
    }

    public void AddGoal_Explore() {
        if (notPresent(goal_explore)) {
            RemoveAllSubgoals();
            AddSubgoal(new Goal_Explore(m_pOwner));
        }
    }

    public void AddGoal_AttackTarget() {
        if (notPresent(goal_attack_target)) {
            RemoveAllSubgoals();
            AddSubgoal(new Goal_AttackTarget(m_pOwner));
        }
    }

    /**
     * this adds the MoveToPosition goal to the *back* of the subgoal list.
     */
    public void QueueGoal_MoveToPosition(Vector2D pos) {
        m_SubGoals.add(new Goal_MoveToPosition(m_pOwner, pos));
    }

    /**
     * this renders the evaluations (goal scores) at the specified location
     */
    public void RenderEvaluations(int left, int top) {
        gdi.TextColor(Cgdi.black);

        Iterator<Goal_Evaluator> it = m_Evaluators.iterator();
        while (it.hasNext()) {
            Goal_Evaluator curDes = it.next();
            curDes.RenderInfo(new Vector2D(left, top), m_pOwner);

            left += 75;
        }
    }

    @Override
    public void Render() {
        Iterator<Goal<Raven_Bot>> it = m_SubGoals.iterator();
        while (it.hasNext()) {
            it.next().Render();;
        }
    }
}