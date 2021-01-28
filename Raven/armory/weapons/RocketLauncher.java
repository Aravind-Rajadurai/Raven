/*
 *  Desc:   class to implement a rocket launche
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_rocket_launcher;
import static Raven.lua.Raven_Scriptor.script;
import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import common.fuzzy.FuzzyModule;
import common.fuzzy.FuzzyVariable;
import common.fuzzy.FzSet;
import common.fuzzy.fuzzyOperators.FzAND;
import static common.misc.Cgdi.gdi;

public class RocketLauncher extends Raven_Weapon {

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

        FuzzyVariable AmmoStatus = m_FuzzyModule.CreateFLV("AmmoStatus");
        FzSet Ammo_Loads = AmmoStatus.AddRightShoulderSet("Ammo_Loads", 10, 30, 100);
        FzSet Ammo_Okay = AmmoStatus.AddTriangularSet("Ammo_Okay", 0, 10, 30);
        FzSet Ammo_Low = AmmoStatus.AddTriangularSet("Ammo_Low", 0, 0, 10);


        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Loads), Undesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Okay), Undesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Low), Undesirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Loads), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Okay), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Low), Desirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Loads), Desirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Okay), Undesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Low), Undesirable);
    }

    //--------------------------- ctor --------------------------------------------
    //-----------------------------------------------------------------------------
    public RocketLauncher(Raven_Bot owner) {

        super(type_rocket_launcher,
                script.GetInt("RocketLauncher_DefaultRounds"),
                script.GetInt("RocketLauncher_MaxRoundsCarried"),
                script.GetDouble("RocketLauncher_FiringFreq"),
                script.GetDouble("RocketLauncher_IdealRange"),
                script.GetDouble("Rocket_MaxSpeed"),
                owner);
        //setup the vertex buffer

        final Vector2D weapon[] = {new Vector2D(0, -3),
            new Vector2D(6, -3),
            new Vector2D(6, -1),
            new Vector2D(15, -1),
            new Vector2D(15, 1),
            new Vector2D(6, 1),
            new Vector2D(6, 3),
            new Vector2D(0, 3)
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
        if (NumRoundsRemaining() > 0 && isReadyForNextShot()) {
            //fire off a rocket!
            m_pOwner.GetWorld().AddRocket(m_pOwner, pos);

            m_iNumRoundsLeft--;

            UpdateTimeWeaponIsNextAvailable();

            //add a trigger to the game so that the other bots can hear this shot
            //(provided they are within range)
            m_pOwner.GetWorld().GetMap().AddSoundTrigger(m_pOwner, script.GetDouble("RocketLauncher_SoundRange"));
        }
    }

    //---------------------------- Desirability -----------------------------------
    @Override
    public double GetDesirability(double DistToTarget) {
        if (m_iNumRoundsLeft == 0) {
            m_dLastDesirabilityScore = 0;
        } else {
            //fuzzify distance and amount of ammo
            m_FuzzyModule.Fuzzify("DistToTarget", DistToTarget);
            m_FuzzyModule.Fuzzify("AmmoStatus", (double) m_iNumRoundsLeft);

            m_dLastDesirabilityScore = m_FuzzyModule.DeFuzzify("Desirability", FuzzyModule.DefuzzifyMethod.max_av);
        }

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

        gdi.RedPen();
        gdi.ClosedShape(m_vecWeaponVBTrans);
    }
}
