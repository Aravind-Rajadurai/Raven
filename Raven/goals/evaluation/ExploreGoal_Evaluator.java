package Raven.goals.evaluation;

import Raven.Raven_Bot;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.ttos;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class to calculate how desirable the goal of exploring is
 */
public class ExploreGoal_Evaluator extends Goal_Evaluator {

    public ExploreGoal_Evaluator(double bias) {
        super(bias);
    }

    @Override
    public double CalculateDesirability(Raven_Bot pBot) {
        double Desirability = 0.05;

        Desirability *= m_dCharacterBias;

        return Desirability;
    }

    @Override
    public void SetGoal(Raven_Bot pBot) {
        pBot.GetBrain().AddGoal_Explore();
    }

    @Override
    public void RenderInfo(Vector2D Position, Raven_Bot pBot) {
        gdi.TextAtPos(Position, "EX: " + ttos(CalculateDesirability(pBot), 2));
    }
}