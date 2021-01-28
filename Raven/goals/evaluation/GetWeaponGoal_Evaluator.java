package Raven.goals.evaluation;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_rail_gun;
import static Raven.Raven_ObjectEnumerations.type_rocket_launcher;
import static Raven.Raven_ObjectEnumerations.type_shotgun;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.clamp;
import static common.misc.Stream_Utility_function.ttos;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: class to calculate how desirable the goal of fetching a weapon item is
 */
public class GetWeaponGoal_Evaluator extends Goal_Evaluator {

    private int m_iWeaponType;

    public GetWeaponGoal_Evaluator(double bias,
            int WeaponType) {
        super(bias);
        m_iWeaponType = WeaponType;
    }

    @Override
    public double CalculateDesirability(Raven_Bot pBot) {
        //grab the distance to the closest instance of the weapon type
        double Distance = Raven_Feature.DistanceToItem(pBot, m_iWeaponType);

        //if the distance feature is rated with a value of 1 it means that the
        //item is either not present on the map or too far away to be worth 
        //considering, therefore the desirability is zero
        if (Distance == 1) {
            return 0;
        } else {
            //value used to tweak the desirability
            final double Tweaker = 0.15;

            double Health, WeaponStrength;

            Health = Raven_Feature.Health(pBot);

            WeaponStrength = Raven_Feature.IndividualWeaponStrength(pBot,
                    m_iWeaponType);

            double Desirability = (Tweaker * Health * (1 - WeaponStrength)) / Distance;

            //ensure the value is in the range 0 to 1
            Desirability = clamp(Desirability, 0.0, 1.0);

            Desirability *= m_dCharacterBias;

            return Desirability;
        }
    }

    @Override
    public void SetGoal(Raven_Bot pBot) {
        pBot.GetBrain().AddGoal_GetItem(m_iWeaponType);
    }

    @Override
    public void RenderInfo(Vector2D Position, Raven_Bot pBot) {
        String s = "";
        switch (m_iWeaponType) {
            case type_rail_gun:
                s = "RG: ";
                break;
            case type_rocket_launcher:
                s = "RL: ";
                break;
            case type_shotgun:
                s = "SG: ";
                break;
        }

        gdi.TextAtPos(Position, s + ttos(CalculateDesirability(pBot), 2));
    }
}