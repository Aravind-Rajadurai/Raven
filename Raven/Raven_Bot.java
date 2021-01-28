package Raven;

import static Raven.Constants.FrameRate;
import Raven.goals.composite.Goal_Think;
import static Raven.Raven_Messages.message_type.Msg_YouGotMeYouSOB;
import static Raven.Raven_ObjectEnumerations.type_bot;
import Raven.navigation.Raven_PathPlanner;
import static Raven.lua.Raven_Scriptor.script;
import static Raven.Raven_UserOptions.UserOptions;
import common.D2.Vector2D;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.div;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Vector2D.Vec2DDistanceSq;
import static common.D2.Vector2D.Vec2DNormalize;
import static common.D2.Transformation.WorldTransform;
import common.Time.Regulator;
import static common.Debug.DbgConsole.debug_con;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.NO_ADDITIONAL_INFO;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.ttos;
import static common.misc.utils.clamp;
import static common.misc.utils.DegsToRads;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import java.util.ArrayList;
import java.util.List;
import static Raven.Raven_Bot.Status.*;
import common.D2.C2DMatrix;
import common.Messaging.Telegram;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 */
public class Raven_Bot extends MovingEntity {

    protected enum Status {

        alive, dead, spawning
    };
    /**
     * alive, dead or spawning?
     */
    private Status m_Status;
    /**
     * a pointer to the world data
     */
    private Raven_Game m_pWorld;
    /**
     * this object handles the arbitration and processing of high level goals
     */
    private Goal_Think m_pBrain;
    /**
     * this is a class that acts as the bots sensory memory. Whenever this bot
     * sees or hears an opponent, a record of the event is updated in the
     * memory.
     */
    private Raven_SensoryMemory m_pSensoryMem;
    /**
     * the bot uses this object to steer
     */
    private Raven_Steering m_pSteering;
    /**
     * the bot uses this to plan paths
     */
    private Raven_PathPlanner m_pPathPlanner;
    /**
     * this is responsible for choosing the bot's current target
     */
    private Raven_TargetingSystem m_pTargSys;
    /**
     * this handles all the weapons. and has methods for aiming, selecting and
     * shooting them
     */
    private Raven_WeaponSystem m_pWeaponSys;
    //A regulator object limits the update frequency of a specific AI component
    private Regulator m_pWeaponSelectionRegulator;
    private Regulator m_pGoalArbitrationRegulator;
    private Regulator m_pTargetSelectionRegulator;
    private Regulator m_pTriggerTestRegulator;
    private Regulator m_pVisionUpdateRegulator;
    /**
     * the bot's health. Every time the bot is shot this value is decreased. If
     * it reaches zero then the bot dies (and respawns)
     */
    private int m_iHealth;
    /**
     * the bot's maximum health value. It starts its life with health at this
     * value
     */
    private int m_iMaxHealth;
    /**
     * each time this bot kills another this value is incremented
     */
    private int m_iScore;
    /**
     * the direction the bot is facing (and therefore the direction of aim).
     * Note that this may not be the same as the bot's heading, which always
     * points in the direction of the bot's movement
     */
    private Vector2D m_vFacing;
    /**
     * a bot only perceives other bots within this field of view
     */
    private double m_dFieldOfView;
    /**
     * to show that a player has been hit it is surrounded by a thick red circle
     * for a fraction of a second. This variable represents the number of
     * update-steps the circle gets drawn
     */
    private int m_iNumUpdatesHitPersistant;
    /**
     * set to true when the bot is hit, and remains true until
     * m_iNumUpdatesHitPersistant becomes zero. (used by the render method to
     * draw a thick red circle around a bot to indicate it's been hit)
     */
    private boolean m_bHit;
    /**
     * set to true when a human player takes over control of the bot
     */
    private boolean m_bPossessed;
    /**
     * a vertex buffer containing the bot's geometry
     */
    private List<Vector2D> m_vecBotVB = new ArrayList<Vector2D>();
    /**
     * the buffer for the transformed vertices
     */
    private List<Vector2D> m_vecBotVBTrans = new ArrayList<Vector2D>();

    /**
     * bots shouldn't be copied, only created or respawned
     */
    //Raven_Bot(const Raven_Bot&);
    //Raven_Bot& operator=(const Raven_Bot&);
    private Raven_Bot(Raven_Bot bot) {
        super(new Vector2D(), 0, new Vector2D(), 0, new Vector2D(), 0, new Vector2D(), 0, 0);
    }

    /**
     * this method is called from the update method. It calculates and applies
     * the steering force for this time-step.
     */
    private void UpdateMovement() {
        //calculate the combined steering force
        Vector2D force = m_pSteering.Calculate();

        //if no steering force is produced decelerate the player by applying a
        //braking force
        if (m_pSteering.Force().isZero()) {
            final double BrakingRate = 0.8;

            m_vVelocity = mul(m_vVelocity, BrakingRate);
        }

        //calculate the acceleration
        Vector2D accel = div(force, m_dMass);

        //update the velocity
        m_vVelocity.add(accel);

        //make sure vehicle does not exceed maximum velocity
        m_vVelocity.Truncate(m_dMaxSpeed);

        //update the position
        m_vPosition.add(m_vVelocity);

        //if the vehicle has a non zero velocity the heading and side vectors must 
        //be updated
        if (!m_vVelocity.isZero()) {
            m_vHeading = Vec2DNormalize(m_vVelocity);

            m_vSide = m_vHeading.Perp();
        }
    }

    /**
     * initializes the bot's VB with its geometry
     */
    private void SetUpVertexBuffer() {
        //setup the vertex buffers and calculate the bounding radius

        final Vector2D bot[] = {new Vector2D(-3, 8),
            new Vector2D(3, 10),
            new Vector2D(3, -10),
            new Vector2D(-3, -8)};
        final int NumBotVerts = bot.length;

        m_dBoundingRadius = 0.0;
        double scale = script.GetDouble("Bot_Scale");

        for (int vtx = 0; vtx < NumBotVerts; ++vtx) {
            m_vecBotVB.add(bot[vtx]);

            //set the bounding radius to the length of the 
            //greatest extent
            if (abs(bot[vtx].x) * scale > m_dBoundingRadius) {
                m_dBoundingRadius = abs(bot[vtx].x * scale);
            }

            if (abs(bot[vtx].y) * scale > m_dBoundingRadius) {
                m_dBoundingRadius = abs(bot[vtx].y) * scale;
            }
        }
    }

    //-------------------------- ctor ---------------------------------------------
    public Raven_Bot(Raven_Game world, Vector2D pos) {

        super(new Vector2D(pos),
                script.GetDouble("Bot_Scale"),
                new Vector2D(0, 0),
                script.GetDouble("Bot_MaxSpeed"),
                new Vector2D(1, 0),
                script.GetDouble("Bot_Mass"),
                new Vector2D(script.GetDouble("Bot_Scale"), script.GetDouble("Bot_Scale")),
                script.GetDouble("Bot_MaxHeadTurnRate"),
                script.GetDouble("Bot_MaxForce"));

        m_iMaxHealth = script.GetInt("Bot_MaxHealth");
        m_iHealth = script.GetInt("Bot_MaxHealth");
        m_pPathPlanner = null;
        m_pSteering = null;
        m_pWorld = world;
        m_pBrain = null;
        m_iNumUpdatesHitPersistant = (int) (FrameRate * script.GetDouble("HitFlashTime"));
        m_bHit = false;
        m_iScore = 0;
        m_Status = spawning;
        m_bPossessed = false;
        m_dFieldOfView = DegsToRads(script.GetDouble("Bot_FOV"));

        SetEntityType(type_bot);

        SetUpVertexBuffer();

        //a bot starts off facing in the direction it is heading
        m_vFacing = m_vHeading;

        //create the navigation module
        m_pPathPlanner = new Raven_PathPlanner(this);

        //create the steering behavior class
        m_pSteering = new Raven_Steering(world, this);

        //create the regulators
        m_pWeaponSelectionRegulator = new Regulator(script.GetDouble("Bot_WeaponSelectionFrequency"));
        m_pGoalArbitrationRegulator = new Regulator(script.GetDouble("Bot_GoalAppraisalUpdateFreq"));
        m_pTargetSelectionRegulator = new Regulator(script.GetDouble("Bot_TargetingUpdateFreq"));
        m_pTriggerTestRegulator = new Regulator(script.GetDouble("Bot_TriggerUpdateFreq"));
        m_pVisionUpdateRegulator = new Regulator(script.GetDouble("Bot_VisionUpdateFreq"));

        //create the goal queue
        m_pBrain = new Goal_Think(this);

        //create the targeting system
        m_pTargSys = new Raven_TargetingSystem(this);

        m_pWeaponSys = new Raven_WeaponSystem(this,
                script.GetDouble("Bot_ReactionTime"),
                script.GetDouble("Bot_AimAccuracy"),
                script.GetDouble("Bot_AimPersistance"));

        m_pSensoryMem = new Raven_SensoryMemory(this, script.GetDouble("Bot_MemorySpan"));
    }

    //-------------------------------- dtor ---------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        debug_con.print("deleting raven bot (id = ").print(ID()).print(")").print("");

        m_pBrain = null;
        m_pPathPlanner = null;
        m_pSteering = null;
        m_pWeaponSelectionRegulator = null;
        m_pTargSys = null;
        m_pGoalArbitrationRegulator = null;
        m_pTargetSelectionRegulator = null;
        m_pTriggerTestRegulator = null;
        m_pVisionUpdateRegulator = null;
        m_pWeaponSys = null;
        m_pSensoryMem = null;
    }

    //the usual suspects
    @Override
    public void Render() {
        //when a bot is hit by a projectile this value is set to a constant user
        //defined value which dictates how long the bot should have a thick red
        //circle drawn around it (to indicate it's been hit) The circle is drawn
        //as long as this value is positive. (see Render)
        m_iNumUpdatesHitPersistant--;


        if (isDead() || isSpawning()) {
            return;
        }

        gdi.BluePen();

        m_vecBotVBTrans = WorldTransform(m_vecBotVB,
                Pos(),
                Facing(),
                Facing().Perp(),
                Scale());

        gdi.ClosedShape(m_vecBotVBTrans);

        //draw the head
        gdi.BrownBrush();
        gdi.Circle(Pos(), 6.0 * Scale().x);


        //render the bot's weapon
        m_pWeaponSys.RenderCurrentWeapon();

        //render a thick red circle if the bot gets hit by a weapon
        if (m_bHit) {
            gdi.ThickRedPen();
            gdi.HollowBrush();
            gdi.Circle(m_vPosition, BRadius() + 1);

            if (m_iNumUpdatesHitPersistant <= 0) {
                m_bHit = false;
            }
        }

        gdi.TransparentText();
        gdi.TextColor(0, 255, 0);

        if (UserOptions.m_bShowBotIDs) {
            gdi.TextAtPos(Pos().x - 10, Pos().y - 20, ttos(ID()));
        }

        if (UserOptions.m_bShowBotHealth) {
            gdi.TextAtPos(Pos().x - 40, Pos().y - 5, "H:" + ttos(Health()));
        }

        if (UserOptions.m_bShowScore) {
            gdi.TextAtPos(Pos().x - 40, Pos().y + 10, "Scr:" + ttos(Score()));
        }
    }

    @Override
    public void Update() {
        //process the currently active goal. Note this is required even if the bot
        //is under user control. This is because a goal is created whenever a user 
        //clicks on an area of the map that necessitates a path planning request.
        m_pBrain.Process();

        //Calculate the steering force and update the bot's velocity and position
        UpdateMovement();

        //if the bot is under AI control but not scripted
        if (!isPossessed()) {
            //examine all the opponents in the bots sensory memory and select one
            //to be the current target
            if (m_pTargetSelectionRegulator.isReady()) {
                m_pTargSys.Update();
            }

            //appraise and arbitrate between all possible high level goals
            if (m_pGoalArbitrationRegulator.isReady()) {
                m_pBrain.Arbitrate();
            }

            //update the sensory memory with any visual stimulus
            if (m_pVisionUpdateRegulator.isReady()) {
                m_pSensoryMem.UpdateVision();
            }

            //select the appropriate weapon to use from the weapons currently in
            //the inventory
            if (m_pWeaponSelectionRegulator.isReady()) {
                m_pWeaponSys.SelectWeapon();
            }

            //this method aims the bot's current weapon at the current target
            //and takes a shot if a shot is possible
            m_pWeaponSys.TakeAimAndShoot();
        }
    }

    @Override
    public boolean HandleMessage(final Telegram msg) {
        //first see if the current goal accepts the message
        if (GetBrain().HandleMessage(msg)) {
            return true;
        }

        //handle any messages not handles by the goals
        switch (msg.Msg) {
            case Msg_TakeThatMF:

                //just return if already dead or spawning
                if (isDead() || isSpawning()) {
                    return true;
                }

                //the extra info field of the telegram carries the amount of damage
                ReduceHealth(Telegram.<Integer>DereferenceToType(msg.ExtraInfo));

                //if this bot is now dead let the shooter know
                if (isDead()) {
                    Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                            ID(),
                            msg.Sender,
                            Msg_YouGotMeYouSOB,
                            NO_ADDITIONAL_INFO);
                }

                return true;

            case Msg_YouGotMeYouSOB:

                IncrementScore();

                //the bot this bot has just killed should be removed as the target
                m_pTargSys.ClearTarget();

                return true;

            case Msg_GunshotSound:

                //add the source of this sound to the bot's percepts
                GetSensoryMem().UpdateWithSoundSource((Raven_Bot) msg.ExtraInfo);

                return true;

            case Msg_UserHasRemovedBot: {

                Raven_Bot pRemovedBot = (Raven_Bot) msg.ExtraInfo;

                GetSensoryMem().RemoveBotFromMemory(pRemovedBot);

                //if the removed bot is the target, make sure the target is cleared
                if (pRemovedBot == GetTargetSys().GetTarget()) {
                    GetTargetSys().ClearTarget();
                }

                return true;
            }


            default:
                return false;
        }
    }

    public void Write(OutputStream os) {/*not implemented*/

    }

    @Override
    public void Read(InputStream is) {/*not implemented*/

    }

    /**
     * given a target position, this method rotates the bot's facing vector by
     * an amount not greater than m_dMaxTurnRate until it directly faces the
     * target.
     *
     * @return true when the heading is facing in the desired direction
     */
    public boolean RotateFacingTowardPosition(Vector2D target) {
        Vector2D toTarget = Vec2DNormalize(sub(target, m_vPosition));

        double dot = m_vFacing.Dot(toTarget);

        //clamp to rectify any rounding errors
        dot = clamp(dot, -1.0, 1.0);

        //determine the angle between the heading vector and the target
        double angle = acos(dot);

        //return true if the bot's facing is within WeaponAimTolerance degs of
        //facing the target
        final double WeaponAimTolerance = 0.01; //2 degs approx

        if (angle < WeaponAimTolerance) {
            m_vFacing = toTarget;
            return true;
        }

        //clamp the amount to turn to the max turn rate
        if (angle > m_dMaxTurnRate) {
            angle = m_dMaxTurnRate;
        }

        //The next few lines use a rotation matrix to rotate the player's facing
        //vector accordingly
        C2DMatrix RotationMatrix = new C2DMatrix();

        //notice how the direction of rotation has to be determined when creating
        //the rotation matrix
        RotationMatrix.Rotate(angle * m_vFacing.Sign(toTarget));
        RotationMatrix.TransformVector2Ds(m_vFacing);

        return false;
    }

    //methods for accessing attribute data
    public int Health() {
        return m_iHealth;
    }

    public int MaxHealth() {
        return m_iMaxHealth;
    }

    public void ReduceHealth(int val) {
        m_iHealth -= val;

        if (m_iHealth <= 0) {
            SetDead();
        }

        m_bHit = true;

        m_iNumUpdatesHitPersistant = (int) (FrameRate * script.GetDouble("HitFlashTime"));
    }

    public void IncreaseHealth(int val) {
        m_iHealth += val;
        m_iHealth = clamp(m_iHealth, 0, m_iMaxHealth);
    }

    public void RestoreHealthToMaximum() {
        m_iHealth = m_iMaxHealth;
    }

    public int Score() {
        return m_iScore;
    }

    public void IncrementScore() {
        ++m_iScore;
    }

    public Vector2D Facing() {
        return new Vector2D(m_vFacing);
    }

    public double FieldOfView() {
        return m_dFieldOfView;
    }
    /**
     * @return true when a human player takes over control of the bot
     */
    public boolean isPossessed() {
        return m_bPossessed;
    }

    public boolean isDead() {
        return m_Status == Status.dead;
    }

    public boolean isAlive() {
        return m_Status == Status.alive;
    }

    public boolean isSpawning() {
        return m_Status == Status.spawning;
    }

    public void SetSpawning() {
        m_Status = Status.spawning;
    }

    public void SetDead() {
        m_Status = Status.dead;
    }

    public void SetAlive() {
        m_Status = Status.alive;
    }

    /**
     * returns a value indicating the time in seconds it will take the bot to
     * reach the given position at its current speed.
     */
    public double CalculateTimeToReachPosition(Vector2D pos) {
        return Vec2DDistance(Pos(), pos) / (MaxSpeed() * FrameRate);
    }

    /**
     * returns true if the bot is close to the given position
     */
    public boolean isAtPosition(Vector2D pos) {
        final double tolerance = 10.0;

        return Vec2DDistanceSq(Pos(), pos) < tolerance * tolerance;
    }

    //interface for human player
    /**
     * fires the current weapon at the given position
     */
    public void FireWeapon(Vector2D pos) {
        m_pWeaponSys.ShootAt(pos);
    }

    public void ChangeWeapon(int type) {
        m_pWeaponSys.ChangeWeapon(type);
    }

    /**
     * this is called to allow a human player to control the bot
     */
    public void TakePossession() {
        if (!(isSpawning() || isDead())) {
            m_bPossessed = true;

            debug_con.print("Player Possesses bot ").print(this.ID()).print("");
        }
    }

    /**
     * called when a human is exorcised from this bot and the AI takes control
     */
    public void Exorcise() {
        m_bPossessed = false;

        //when the player is exorcised then the bot should resume normal service
        m_pBrain.AddGoal_Explore();

        debug_con.print("Player is exorcised from bot ").print(this.ID()).print("");
    }

    /**
     * spawns the bot at the given position
     */
    public void Spawn(Vector2D pos) {
        SetAlive();
        m_pBrain.RemoveAllSubgoals();
        m_pTargSys.ClearTarget();
        SetPos(pos);
        m_pWeaponSys.Initialize();
        RestoreHealthToMaximum();
    }

    /**
     * returns true if this bot is ready to be tested against the world triggers
     */
    public boolean isReadyForTriggerUpdate() {
        return m_pTriggerTestRegulator.isReady();
    }

    /**
     * returns true if the bot has line of sight to the given position.
     */
    public boolean hasLOSto(Vector2D pos) {
        return m_pWorld.isLOSOkay(Pos(), pos);
    }

    /**
     * returns true if this bot can move directly to the given position without
     * bumping into any walls
     */
    public boolean canWalkTo(Vector2D pos) {
        return !m_pWorld.isPathObstructed(Pos(), pos, BRadius());
    }

    /**
     * similar to canWalkTo(). Returns true if the bot can move between the two
     * given positions without bumping into any walls
     */
    public boolean canWalkBetween(Vector2D from, Vector2D to) {
        return !m_pWorld.isPathObstructed(from, to, BRadius());
    }

//--------------------------- canStep Methods ---------------------------------
//  returns true if there is space enough to step in the indicated direction
//  If true PositionOfStep will be assigned the offset position
//-----------------------------------------------------------------------------
    public boolean canStepLeft(Vector2D PositionOfStep) {
        final double StepDistance = BRadius() * 2;

        //PositionOfStep = Pos() - Facing().Perp() * StepDistance - Facing().Perp() * BRadius();
        PositionOfStep.set(sub(sub(Pos(), mul(Facing().Perp(), StepDistance)), mul(Facing().Perp(), BRadius())));

        return canWalkTo(PositionOfStep);
    }

    public boolean canStepRight(Vector2D PositionOfStep) {
        final double StepDistance = BRadius() * 2;

        //PositionOfStep = Pos() + Facing().Perp() * StepDistance + Facing().Perp() * BRadius();
        PositionOfStep.set(add(Pos(), mul(Facing().Perp(), StepDistance), mul(Facing().Perp(), BRadius())));

        return canWalkTo(PositionOfStep);
    }

    public boolean canStepForward(Vector2D PositionOfStep) {
        final double StepDistance = BRadius() * 2;

        //PositionOfStep = Pos() + Facing() * StepDistance + Facing() * BRadius();
        PositionOfStep.set(add(Pos(), mul(Facing(), StepDistance), mul(Facing(), BRadius())));

        return canWalkTo(PositionOfStep);
    }

    public boolean canStepBackward(Vector2D PositionOfStep) {
        final double StepDistance = BRadius() * 2;

        //PositionOfStep = Pos() - Facing() * StepDistance - Facing() * BRadius();
        PositionOfStep.set(sub(sub(Pos(), mul(Facing(), StepDistance)), mul(Facing(), BRadius())));

        return canWalkTo(PositionOfStep);
    }

    public Raven_Game GetWorld() {
        return m_pWorld;
    }

    public Raven_Steering GetSteering() {
        return m_pSteering;
    }

    public Raven_PathPlanner GetPathPlanner() {
        return m_pPathPlanner;
    }

    public Goal_Think GetBrain() {
        return m_pBrain;
    }

    public Raven_TargetingSystem GetTargetSys() {
        return m_pTargSys;
    }

    public Raven_Bot GetTargetBot() {
        return m_pTargSys.GetTarget();
    }

    public Raven_WeaponSystem GetWeaponSys() {
        return m_pWeaponSys;
    }

    public Raven_SensoryMemory GetSensoryMem() {
        return m_pSensoryMem;
    }
}