/**
 *  Desc:   class to provide a proxy for a fuzzy set. The proxy inherits from
 *          FuzzyTerm and therefore can be used to create fuzzy rules
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy;

import common.fuzzy.setTypes.FuzzySet;

public class FzSet extends FuzzyTerm {

    public final FuzzySet m_Set;

    public FzSet(FuzzySet fs) {
        m_Set = fs;
    }
    // copy ctor
    private FzSet(FzSet con) {
       m_Set = con.m_Set;
    }
  

    @Override
    public FuzzyTerm Clone() {
        return (FuzzyTerm) new FzSet(this);
    }
    
    @Override
    public double GetDOM() {
        return m_Set.GetDOM();
    }

    @Override
    public void ClearDOM() {
        m_Set.ClearDOM();
    }

    @Override
    public void ORwithDOM(double val) {
        m_Set.ORwithDOM(val);
    }
}
