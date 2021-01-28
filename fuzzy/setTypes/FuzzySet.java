/** 
 * Desc:   class to define an interface for a fuzzy set
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy.setTypes;

abstract public class FuzzySet implements Cloneable {

    /**
     * this will hold the degree of membership of a given value in this set 
     */
    protected double m_dDOM;
    /**
     * this is the maximum of the set's membership function. For instance, if
     * the set is triangular then this will be the peak point of the triangular.
     * if the set has a plateau then this value will be the mid point of the 
     * plateau. This value is set in the constructor to avoid run-time
     * calculation of mid-point values.
     */
    protected double m_dRepresentativeValue;

    public FuzzySet(double RepVal) {
        m_dDOM = 0.0;
        m_dRepresentativeValue = RepVal;
    }

    /**
     * return the degree of membership in this set of the given value. NOTE,
     * this does not set m_dDOM to the DOM of the value passed as the parameter.
     * This is because the centroid defuzzification method also uses this method
     * to determine the DOMs of the values it uses as its sample points.
     */
    public abstract double CalculateDOM(double val);

    /**
     * if this fuzzy set is part of a consequent FLV, and it is fired by a rule 
     * then this method sets the DOM (in this context, the DOM represents a
     * confidence level)to the maximum of the parameter value or the set's 
     * existing m_dDOM value
     */
    public void ORwithDOM(double val) {
        if (val > m_dDOM) {
            m_dDOM = val;
        }
    }

    //accessor methods
    public double GetRepresentativeVal() {
        return m_dRepresentativeValue;
    }

    public void ClearDOM() {
        m_dDOM = 0.0;
    }

    public double GetDOM() {
        return m_dDOM;
    }

    public void SetDOM(double val) {
        assert (val <= 1) && (val >= 0) : "<FuzzySet::SetDOM>: invalid value";
        m_dDOM = val;
    }
    
    @Override
    public FuzzySet clone() throws CloneNotSupportedException {
        return (FuzzySet) super.clone();
    }
}