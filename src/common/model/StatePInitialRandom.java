package common.model;

import common.constants.Constants;

public class StatePInitialRandom extends StateP {
	
	private int[][] positionArray = new int[Constants.DIMENSION * Constants.DIMENSION][2];

	public StatePInitialRandom(byte[] cells, Double weight) {
		super(cells, weight);
		computeAllCellLocations();
	}
	
	public StatePInitialRandom(StateP state, Double weight) {
		super(state, weight);
		computeAllCellLocations();
	}
	
	private void computeAllCellLocations() {
		for(int row = 0; row < Constants.DIMENSION  ; row++) {
			for(int column = 0 ; column < Constants.DIMENSION ; column++) {
				positionArray[super.getCellValue(row, column)][0] = row;
				positionArray[super.getCellValue(row, column)][1] = column;
			}
		}
	}
	
	public int getRowPosition(int value) {
		return positionArray[value][0];
	}
	
	public int getColumnPosition(int value) {
		return positionArray[value][1];
	}

}
