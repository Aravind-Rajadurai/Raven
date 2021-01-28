/**
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.navigation;

import common.D2.Vector2D;

abstract public class PathTarget {

	public enum target_type {

		item, position, invalid
	};
	private int m_iTargetItemType;
	private Vector2D m_vTargetPosition = new Vector2D();
	private target_type m_Type;

	public PathTarget() {
		m_iTargetItemType = -1;
		m_Type = target_type.invalid;
	}

	abstract public void SetTargetAsItem(int ItemType);

	abstract public void SetTargetAsPosition(Vector2D TargetPosition);

	public boolean isTargetAnItem() {
		return m_Type == target_type.item;
	}

	public boolean isTargetAPosition() {
		return m_Type == target_type.position;
	}

	public boolean isTargetValid() {
		return !(m_Type == target_type.invalid);
	}

	abstract public Vector2D GetTargetPosition();

	abstract public int GetTargetType();
}