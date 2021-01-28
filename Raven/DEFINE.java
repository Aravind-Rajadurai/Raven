/**
 * This class substitute C++ preprocessor. 
 * Workks only inside methods.
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package Raven;

import java.util.HashMap;

public class DEFINE {
   private static HashMap<Integer,Boolean> defined = new HashMap<Integer,Boolean>();
   
   public static final int DEBUG = 0;
   public static final int SHOW_MESSAGING_INFO = 1;
   public static final int SHOW_TARGET = 2;
   public static final int LOG_CREATIONAL_STUFF = 3;
   public static final int SHOW_NAVINFO = 4;
   public static final int SHOW_LAST_RECORDED_POSITION = 5;
   static { 
       //define(DEBUG);
   }
   public static boolean def(Integer D) {
       Boolean def =  defined.get(D);
       if(def == null) return false;
       return def;
   } 
   
   public static void define(Integer D) {
       defined.put(D, Boolean.TRUE);
   }
   
   public static void undef(Integer D) {
       defined.put(D, Boolean.FALSE);
   }
}
