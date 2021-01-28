/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import Raven.Raven_Game;
import common.D2.Vector2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr
 */
public class ShotGunTest {
    
    private ShotGun instance;
    
    public ShotGunTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        instance = new ShotGun(new Raven_Bot(new Raven_Game(), new Vector2D()));
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of InitializeFuzzyModule method, of class ShotGun.
     */
    @Test
    public void testInitializeFuzzyModule() {
        instance.InitializeFuzzyModule();
    }

    /**
     * Test of ShootAt method, of class ShotGun.
     */
    @Test
    public void testShootAt() {
        Vector2D pos = new Vector2D(10, 10);
        instance.ShootAt(pos);
    }

    /**
     * Test of GetDesirability method, of class ShotGun.
     */
    @Test
    public void testGetDesirability() {
        double DistToTarget = 104.0;

        double expResult = 46.38;
        double result = instance.GetDesirability(DistToTarget);
        assertTrue(result > expResult);
    }

    /**
     * Test of Render method, of class ShotGun.
     */
    @Test
    public void testRender() {
       // instance.Render();
    }
}