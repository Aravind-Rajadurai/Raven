package Raven.goals.evaluation;

import Raven.Raven_Bot;
import common.D2.Vector2D;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class template that defines an interface for objects that are able to
 * evaluate the desirability of a specific strategy level goal
 */
public abstract class Goal_Evaluator {

    /**
     * when the desirability score for a goal has been evaluated it is
     * multiplied by this value. It can be used to create bots with preferences
     * based upon their personality
     */
    protected double m_dCharacterBias;

    public Goal_Evaluator(double CharacterBias) {
        m_dCharacterBias = CharacterBias;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * returns a score between 0 and 1 representing the desirability of the
     * strategy the concrete subclass represents
     */
    public abstract double CalculateDesirability(Raven_Bot pBot);

    /**
     * adds the appropriate goal to the given bot's brain
     */
    public abstract void SetGoal(Raven_Bot pBot);

    /**
     * used to provide debugging/tweaking support
     */
    public abstract void RenderInfo(Vector2D Position, Raven_Bot pBot);
}
