package Raven.goals.evaluation;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_health;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.clamp;
import static common.misc.Stream_Utility_function.ttos;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class to calculate how desirable the goal of fetching a health item is
 */
public class GetHealthGoal_Evaluator extends Goal_Evaluator {

    public GetHealthGoal_Evaluator(double bias) {
        super(bias);
    }

    @Override
    public double CalculateDesirability(Raven_Bot pBot) {
        //first grab the distance to the closest instance of a health item
        double Distance = Raven_Feature.DistanceToItem(pBot, type_health);

        //if the distance feature is rated with a value of 1 it means that the
        //item is either not present on the map or too far away to be worth 
        //considering, therefore the desirability is zero
        if (Distance == 1) {
            return 0;
        } else {
            //value used to tweak the desirability
            final double Tweaker = 0.2;

            //the desirability of finding a health item is proportional to the amount
            //of health remaining and inversely proportional to the distance from the
            //nearest instance of a health item.
            double Desirability = Tweaker * (1 - Raven_Feature.Health(pBot))
                    / (Raven_Feature.DistanceToItem(pBot, type_health));

            //ensure the value is in the range 0 to 1
            Desirability = clamp(Desirability, 0.0, 1.0);

            //bias the value according to the personality of the bot
            Desirability *= m_dCharacterBias;

            return Desirability;
        }
    }

    @Override
    public void SetGoal(Raven_Bot pBot) {
        pBot.GetBrain().AddGoal_GetItem(type_health);
    }

    @Override
    public void RenderInfo(Vector2D Position, Raven_Bot pBot) {
        gdi.TextAtPos(Position, "H: " + ttos(CalculateDesirability(pBot), 2));
        return;

        //String s = ttos(1 - Raven_Feature.Health(pBot)) + ", " + ttos(Raven_Feature.DistanceToItem(pBot, type_health));
        //gdi.TextAtPos(Vector2D.add(Position, new Vector2D(0, 15)), s);
    }
}