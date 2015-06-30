package common.impl;

import common.model.StateP;
import common.model.StateP.HoleCellLocation;

public class Action {

	private Move m;
	private HoleCellLocation cell;

	public Action(HoleCellLocation cell, Move m) 
	{
		this.m = m;
		this.cell = cell;
	}

	public HoleCellLocation getCellLocation() 
	{
		return cell;
	}

	public Move getMove() 
	{
		return m;
	}

	/**
	 * Apply this action to a state and return the new state
	 */
	public StateP applyTo(StateP s) 
	{
		byte value = s.getCellValue(cell);

		HoleCellLocation nextCell = m.getNextCellLocation(cell);

		StateP newState = new StateP(s.getAllCells() , s.getWeight());
		newState.setCellValue(nextCell, value);
		newState.setCellValue(cell, (byte) 0);

		return newState;
	}

	@Override
	public String toString() {
		return m + "(" + cell.getRowIndex() + "," + cell.getColumnIndex() + ")";
	}

	public boolean isInverse(Action a) {
		return a != null && m.getInverse() == a.getMove();
	}
}
