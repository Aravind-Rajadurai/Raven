/*
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_blaster;
import static Raven.lua.Raven_Scriptor.script;
import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import common.fuzzy.FuzzyModule;
import common.fuzzy.FuzzyVariable;
import common.fuzzy.FzSet;
import common.fuzzy.fuzzyHedges.FzVery;
import static common.misc.Cgdi.gdi;

public class Blaster extends Raven_Weapon {

    /**
     * set up some fuzzy variables and rules
     */
    @Override
    protected void InitializeFuzzyModule() {
        FuzzyVariable DistToTarget = m_FuzzyModule.CreateFLV("DistToTarget");

        FzSet Target_Close = DistToTarget.AddLeftShoulderSet("Target_Close", 0, 25, 150);
        FzSet Target_Medium = DistToTarget.AddTriangularSet("Target_Medium", 25, 150, 300);
        FzSet Target_Far = DistToTarget.AddRightShoulderSet("Target_Far", 150, 300, 1000);

        FuzzyVariable Desirability = m_FuzzyModule.CreateFLV("Desirability");
        FzSet VeryDesirable = Desirability.AddRightShoulderSet("VeryDesirable", 50, 75, 100);
        FzSet Desirable = Desirability.AddTriangularSet("Desirable", 25, 50, 75);
        FzSet Undesirable = Desirability.AddLeftShoulderSet("Undesirable", 0, 25, 50);

        m_FuzzyModule.AddRule(Target_Close, Desirable);
        m_FuzzyModule.AddRule(Target_Medium, new FzVery(Undesirable));
        m_FuzzyModule.AddRule(Target_Far, new FzVery(Undesirable));
    }

    //--------------------------- ctor --------------------------------------------
    //-----------------------------------------------------------------------------
    public Blaster(Raven_Bot owner) {

        super(type_blaster,
                script.GetInt("Blaster_DefaultRounds"),
                script.GetInt("Blaster_MaxRoundsCarried"),
                script.GetDouble("Blaster_FiringFreq"),
                script.GetDouble("Blaster_IdealRange"),
                script.GetDouble("Bolt_MaxSpeed"),
                owner);

        //setup the vertex buffer
        final Vector2D weapon[] = {new Vector2D(0, -1),
            new Vector2D(10, -1),
            new Vector2D(10, 1),
            new Vector2D(0, 1)
        };
        final int NumWeaponVerts = weapon.length;


        for (int vtx = 0; vtx < NumWeaponVerts; ++vtx) {
            m_vecWeaponVB.add(weapon[vtx]);
        }

        //setup the fuzzy module
        InitializeFuzzyModule();
    }

    //------------------------------ ShootAt --------------------------------------
    @Override
    public void ShootAt(Vector2D pos) {
        if (isReadyForNextShot()) {
            //fire!
            m_pOwner.GetWorld().AddBolt(m_pOwner, pos);

            UpdateTimeWeaponIsNextAvailable();

            //add a trigger to the game so that the other bots can hear this shot
            //(provided they are within range)
            m_pOwner.GetWorld().GetMap().AddSoundTrigger(m_pOwner, script.GetDouble("Blaster_SoundRange"));
        }
    }

    //---------------------------- Desirability -----------------------------------
    @Override
    public double GetDesirability(double DistToTarget) {
        //fuzzify distance and amount of ammo
        m_FuzzyModule.Fuzzify("DistToTarget", DistToTarget);

        m_dLastDesirabilityScore = m_FuzzyModule.DeFuzzify("Desirability", FuzzyModule.DefuzzifyMethod.max_av);

        return m_dLastDesirabilityScore;
    }

    //-------------------------------- Render -------------------------------------
    @Override
    public void Render() {
        m_vecWeaponVBTrans = WorldTransform(m_vecWeaponVB,
                m_pOwner.Pos(),
                m_pOwner.Facing(),
                m_pOwner.Facing().Perp(),
                m_pOwner.Scale());

        gdi.GreenPen();
        gdi.ClosedShape(m_vecWeaponVBTrans);
    }
}
