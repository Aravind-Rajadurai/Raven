/**
 *  Desc:   abstract class to provide an interface for classes able to be
 *          used as terms in a fuzzy if-then rule base.
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy;

public abstract class FuzzyTerm {

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //all terms must implement a virtual constructor
    public abstract FuzzyTerm Clone();

    /**
     * retrieves the degree of membership of the term
     */
    public abstract double GetDOM();

    /**
     * clears the degree of membership of the term
     */
    public abstract void ClearDOM();

    /**
     * method for updating the DOM of a consequent when a rule fires
     */
    public abstract void ORwithDOM(double val);
}