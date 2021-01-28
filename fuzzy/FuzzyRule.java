/**
 *  Desc:   This class implements a fuzzy rule of the form:
 *  
 *          IF fzVar1 AND fzVar2 AND ... fzVarn THEN fzVar.c
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy;

class FuzzyRule {

    /**
     * antecedent (usually a composite of several fuzzy sets and operators)
     * @param a 
     */
    private FuzzyTerm m_pAntecedent;
    /** 
     * consequence (usually a single fuzzy set, but can be several ANDed together)
     */
    private FuzzyTerm m_pConsequence;

    //it doesn't make sense to allow clients to copy rules
    private FuzzyRule(FuzzyRule fr) {
    }
    //private FuzzyRule& operator=(const FuzzyRule&);

    public FuzzyRule(FuzzyTerm ant, FuzzyTerm con) {
        m_pAntecedent = ant.Clone();
        m_pConsequence = con.Clone();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pAntecedent = null;
        m_pConsequence = null;
    }

    public void SetConfidenceOfConsequentToZero() {
        m_pConsequence.ClearDOM();
    }

    /**
     * this method updates the DOM (the confidence) of the consequent term with
     * the DOM of the antecedent term. 
     */
    public void Calculate() {
        m_pConsequence.ORwithDOM(m_pAntecedent.GetDOM());
    }
}