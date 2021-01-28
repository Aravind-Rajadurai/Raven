/**
 * Desc: A Singleton Scriptor class for use with the Raven project
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.lua;

import common.script.Scriptor;

public class Raven_Scriptor extends Scriptor {

    public static Raven_Scriptor script = new Raven_Scriptor();

    private Raven_Scriptor() {
        super();
        RunScriptFile("Raven/Params.lua");
    }

    //copy ctor and assignment should be private
    private Raven_Scriptor(final Raven_Scriptor rs) {
    }
    // Raven_Scriptor& operator=(const Raven_Scriptor&);

    public static Raven_Scriptor Instance() {
        return script;
    }
}