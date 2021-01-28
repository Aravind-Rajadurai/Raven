/**
 * Desc: class to implement a rail gun
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_rail_gun;
import static Raven.lua.Raven_Scriptor.script;
import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import common.fuzzy.FuzzyModule;
import common.fuzzy.FuzzyVariable;
import common.fuzzy.FzSet;
import common.fuzzy.fuzzyHedges.FzFairly;
import common.fuzzy.fuzzyHedges.FzVery;
import common.fuzzy.fuzzyOperators.FzAND;
import static common.misc.Cgdi.gdi;

public class RailGun extends Raven_Weapon {

    /**
     * set up some fuzzy variables and rules
     */
    @Override
    protected void InitializeFuzzyModule() {

        FuzzyVariable DistanceToTarget = m_FuzzyModule.CreateFLV("DistanceToTarget");

        FzSet Target_Close = DistanceToTarget.AddLeftShoulderSet("Target_Close", 0, 25, 150);
        FzSet Target_Medium = DistanceToTarget.AddTriangularSet("Target_Medium", 25, 150, 300);
        FzSet Target_Far = DistanceToTarget.AddRightShoulderSet("Target_Far", 150, 300, 1000);

        FuzzyVariable Desirability = m_FuzzyModule.CreateFLV("Desirability");

        FzSet VeryDesirable = Desirability.AddRightShoulderSet("VeryDesirable", 50, 75, 100);
        FzSet Desirable = Desirability.AddTriangularSet("Desirable", 25, 50, 75);
        FzSet Undesirable = Desirability.AddLeftShoulderSet("Undesirable", 0, 25, 50);

        FuzzyVariable AmmoStatus = m_FuzzyModule.CreateFLV("AmmoStatus");

        FzSet Ammo_Loads = AmmoStatus.AddRightShoulderSet("Ammo_Loads", 15, 30, 100);
        FzSet Ammo_Okay = AmmoStatus.AddTriangularSet("Ammo_Okay", 0, 15, 30);
        FzSet Ammo_Low = AmmoStatus.AddTriangularSet("Ammo_Low", 0, 0, 15);



        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Loads), new FzFairly(Desirable));
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Okay), new FzFairly(Desirable));
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Low), Undesirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Loads), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Okay), Desirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Low), Desirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Loads), new FzVery(VeryDesirable));
        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Okay), new FzVery(VeryDesirable));
        m_FuzzyModule.AddRule(new FzAND(Target_Far, new FzFairly(Ammo_Low)), VeryDesirable);
    }

    //--------------------------- ctor --------------------------------------------
    //-----------------------------------------------------------------------------
    public RailGun(Raven_Bot owner) {

        super(type_rail_gun,
                script.GetInt("RailGun_DefaultRounds"),
                script.GetInt("RailGun_MaxRoundsCarried"),
                script.GetDouble("RailGun_FiringFreq"),
                script.GetDouble("RailGun_IdealRange"),
                script.GetDouble("Slug_MaxSpeed"),
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
        if (NumRoundsRemaining() > 0 && isReadyForNextShot()) {
            //fire a round
            m_pOwner.GetWorld().AddRailGunSlug(m_pOwner, pos);

            UpdateTimeWeaponIsNextAvailable();

            m_iNumRoundsLeft--;

            //add a trigger to the game so that the other bots can hear this shot
            //(provided they are within range)
            m_pOwner.GetWorld().GetMap().AddSoundTrigger(m_pOwner, script.GetDouble("RailGun_SoundRange"));
        }
    }

    //---------------------------- Desirability -----------------------------------
    @Override
    public double GetDesirability(double DistToTarget) {
        if (m_iNumRoundsLeft == 0) {
            m_dLastDesirabilityScore = 0;
        } else {
            //fuzzify distance and amount of ammo
            m_FuzzyModule.Fuzzify("DistanceToTarget", DistToTarget);
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

        gdi.BluePen();

        gdi.ClosedShape(m_vecWeaponVBTrans);
    }
}
