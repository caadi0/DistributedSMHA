package common.algorithms;

import common.model.StateP;

public class GenericLinearConflict {
	
public static int calculate(StateP s, StateP goalState) {
		
		int heuristic = ManhattanDistance.calculate(s);
		
		heuristic += linearVerticalConflict(s);
		heuristic += linearHorizontalConflict(s);
		
		return heuristic;

	}

   private static int linearVerticalConflict(StateP s) {
		int dimension = s.getDimension();
		int linearConflict = 0;
		
		for (int row = 0; row < dimension; row++){
			byte max = -1;
			for (int column = 0;  column < dimension; column++){
				byte cellValue = s.getCellValue(row,column);
				//is tile in its goal row ?
				if (cellValue != 0 && (cellValue - 1) / dimension == row){
					if (cellValue > max){
						max = cellValue;
					}else {
						//linear conflict, one tile must move up or down to allow the other to pass by and then back up
						//add two moves to the manhattan distance
						linearConflict += 2;
					}
				}
				
			}
			
		}
		return linearConflict;
	}

   private static int linearHorizontalConflict(StateP s) {
		
		int dimension = s.getDimension();
		int linearConflict = 0;
		
		for (int column = 0; column < dimension; column++){
			byte max = -1;
			for (int row = 0;  row < dimension; row++){
				byte cellValue = s.getCellValue(row,column);
				//is tile in its goal row ?
				if (cellValue != 0 && cellValue % dimension == column + 1){
					if (cellValue > max){
						max = cellValue;
					}else {
						//linear conflict, one tile must move left or right to allow the other to pass by and then back up
						//add two moves to the manhattan distance
						linearConflict += 2;
					}
				}
				
			}
			
		}
		return linearConflict;
	}

}
