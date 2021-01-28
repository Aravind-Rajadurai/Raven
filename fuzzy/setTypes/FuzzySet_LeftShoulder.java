/**
 *  Desc:   definition of a fuzzy set that has a left shoulder shape. (the
 *          minimum value this variable can accept is *any* value less than the
 *          midpoint.
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.fuzzy.setTypes;

import static common.misc.utils.isEqual;

public class FuzzySet_LeftShoulder extends FuzzySet {

    //the values that define the shape of this FLV
    private double m_dPeakPoint;
    private double m_dRightOffset;
    private double m_dLeftOffset;

    public FuzzySet_LeftShoulder(double peak,
            double LeftOffset,
            double RightOffset) {

        super(((peak - LeftOffset) + peak) / 2);
        m_dPeakPoint = peak;
        m_dLeftOffset = LeftOffset;
        m_dRightOffset = RightOffset;
    }

    /**
     * this method calculates the degree of membership for a particular value
     */
    @Override
    public double CalculateDOM(double val) {
        //test for the case where the left or right offsets are zero
        //(to prevent divide by zero errors below)
        if ((isEqual(m_dRightOffset, 0.0) && (isEqual(m_dPeakPoint, val)))
                || (isEqual(m_dLeftOffset, 0.0) && (isEqual(m_dPeakPoint, val)))) {
            return 1.0;
        } //find DOM if right of center
        else if ((val >= m_dPeakPoint) && (val < (m_dPeakPoint + m_dRightOffset))) {
            double grad = 1.0 / -m_dRightOffset;
            return grad * (val - m_dPeakPoint) + 1.0;
        } //find DOM if left of center
        else if ((val < m_dPeakPoint) && (val >= m_dPeakPoint - m_dLeftOffset)) {
            return 1.0;
        } //out of range of this FLV, return zero
        else {
            return 0.0;
        }

    }
}