/**
 * Desc: class to implement a shot gun
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_shotgun;
import static Raven.lua.Raven_Scriptor.script;
import static common.D2.Transformation.Vec2DRotateAroundOrigin;
import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.sub;
import common.fuzzy.FuzzyModule;
import common.fuzzy.FuzzyVariable;
import common.fuzzy.FzSet;
import common.fuzzy.fuzzyOperators.FzAND;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.RandInRange;

public class ShotGun extends Raven_Weapon {

    /**
     * /* set up some fuzzy variables and rules
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
        FzSet Ammo_Loads = AmmoStatus.AddRightShoulderSet("Ammo_Loads", 30, 60, 100);
        FzSet Ammo_Okay = AmmoStatus.AddTriangularSet("Ammo_Okay", 0, 30, 60);
        FzSet Ammo_Low = AmmoStatus.AddTriangularSet("Ammo_Low", 0, 0, 30);


        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Loads), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Okay), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Close, Ammo_Low), VeryDesirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Loads), VeryDesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Okay), Desirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Medium, Ammo_Low), Undesirable);

        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Loads), Desirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Okay), Undesirable);
        m_FuzzyModule.AddRule(new FzAND(Target_Far, Ammo_Low), Undesirable);
    }
    /**
     * how much shot the each shell contains
     */
    private int m_iNumBallsInShell;
    /**
     * how much the shot spreads out when a cartridge is discharged
     */
    private double m_dSpread;

    //--------------------------- ctor --------------------------------------------
    //-----------------------------------------------------------------------------
    public ShotGun(Raven_Bot owner) {

        super(type_shotgun,
                script.GetInt("ShotGun_DefaultRounds"),
                script.GetInt("ShotGun_MaxRoundsCarried"),
                script.GetDouble("ShotGun_FiringFreq"),
                script.GetDouble("ShotGun_IdealRange"),
                script.GetDouble("Pellet_MaxSpeed"),
                owner);

        m_iNumBallsInShell = script.GetInt("ShotGun_NumBallsInShell");
        m_dSpread = script.GetDouble("ShotGun_Spread");


        //setup the vertex buffer
        final Vector2D weapon[] = {
            new Vector2D(0, 0),
            new Vector2D(0, -2),
            new Vector2D(10, -2),
            new Vector2D(10, 0),
            new Vector2D(0, 0),
            new Vector2D(0, 2),
            new Vector2D(10, 2),
            new Vector2D(10, 0)
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
            //a shotgun cartridge contains lots of tiny metal balls called pellets. 
            //Therefore, every time the shotgun is discharged we have to calculate
            //the spread of the pellets and add one for each trajectory
            for (int b = 0; b < m_iNumBallsInShell; ++b) {
                //determine deviation from target using a bell curve type distribution
                double deviation = RandInRange(0, m_dSpread) + RandInRange(0, m_dSpread) - m_dSpread;

                Vector2D AdjustedTarget = sub(pos, m_pOwner.Pos());

                //rotate the target vector by the deviation
                Vec2DRotateAroundOrigin(AdjustedTarget, deviation);

                //add a pellet to the game world
                m_pOwner.GetWorld().AddShotGunPellet(m_pOwner, add(AdjustedTarget, m_pOwner.Pos()));
            }

            m_iNumRoundsLeft--;

            UpdateTimeWeaponIsNextAvailable();

            //add a trigger to the game so that the other bots can hear this shot
            //(provided they are within range)
            m_pOwner.GetWorld().GetMap().AddSoundTrigger(m_pOwner, script.GetDouble("ShotGun_SoundRange"));
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

        gdi.BrownPen();

        gdi.PolyLine(m_vecWeaponVBTrans);

    }
}
