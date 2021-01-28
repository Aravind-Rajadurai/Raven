package Raven.goals.evaluation;

import Raven.Raven_Bot;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.ttos;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class to calculate how desirable the goal of attacking the bot's
 * current target is
 */
public class AttackTargetGoal_Evaluator extends Goal_Evaluator {

    public AttackTargetGoal_Evaluator(double bias) {
        super(bias);
    }

    /**
     * returns a value between 0 and 1 that indicates the Rating of a bot (the
     * higher the score, the stronger the bot).
     */
    @Override
    public double CalculateDesirability(Raven_Bot pBot) {
        double Desirability = 0.0;

        //only do the calculation if there is a target present
        if (pBot.GetTargetSys().isTargetPresent()) {
            final double Tweaker = 1.0;

            Desirability = Tweaker
                    * Raven_Feature.Health(pBot)
                    * Raven_Feature.TotalWeaponStrength(pBot);

            //bias the value according to the personality of the bot
            Desirability *= m_dCharacterBias;
        }

        return Desirability;
    }

    @Override
    public void SetGoal(Raven_Bot pBot) {
        pBot.GetBrain().AddGoal_AttackTarget();
    }

    @Override
    public void RenderInfo(Vector2D Position, Raven_Bot pBot) {
        gdi.TextAtPos(Position, "AT: " + ttos(CalculateDesirability(pBot), 2));
        return;

        //String s = ttos(Raven_Feature.Health(pBot)) + ", " + ttos(Raven_Feature.TotalWeaponStrength(pBot));
        //gdi.TextAtPos(Vector2D.add(Position, new Vector2D(0, 12)), s);
    }
}