/**
 * Desc:   classes to implement fuzzy hedges 
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy.fuzzyHedges;

import common.fuzzy.FuzzyTerm;
import common.fuzzy.FzSet;
import common.fuzzy.setTypes.FuzzySet;
import static java.lang.Math.sqrt;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FzFairly extends FuzzyTerm {

    private FuzzySet m_Set;

    //prevent copying and assignment
    private FzFairly(FzFairly inst) {
        m_Set = inst.m_Set;
    }

//private FzFairly& operator=(const FzFairly&);
    public FzFairly(FzSet ft) {
        try {
            m_Set = ft.m_Set.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public double GetDOM() {
        return sqrt(m_Set.GetDOM());
    }

    @Override
    public FuzzyTerm Clone() {
        return new FzFairly(this);
    }

    @Override
    public void ClearDOM() {
        m_Set.ClearDOM();
    }

    @Override
    public void ORwithDOM(double val) {
        m_Set.ORwithDOM(sqrt(val));
    }
}