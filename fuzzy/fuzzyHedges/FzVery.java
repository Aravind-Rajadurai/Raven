/**
 * 
 * Desc:   classes to implement fuzzy hedges 
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy.fuzzyHedges;

import common.fuzzy.FuzzyTerm;
import common.fuzzy.FzSet;
import common.fuzzy.setTypes.FuzzySet;

public class FzVery extends FuzzyTerm {

    private FuzzySet m_Set;

    //prevent copying and assignment by clients
    private FzVery(FzVery inst) {
        m_Set = inst.m_Set;
    }
    // FzVery& operator=(const FzVery&);

    public FzVery(FzSet ft) {
        try {        
            m_Set = ft.m_Set.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
        
    }

    @Override
    public double GetDOM() {
        return m_Set.GetDOM() * m_Set.GetDOM();
    }

    @Override
    public FuzzyTerm Clone() {
        return new FzVery(this);
    }

    @Override
    public void ClearDOM() {
        m_Set.ClearDOM();
    }

    @Override
    public void ORwithDOM(double val) {
        m_Set.ORwithDOM(val * val);
    }
}
