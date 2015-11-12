package common.algorithms;
import common.model.StateP;
import common.model.StatePInitialRandom;

public class GenericManhattanDistance {
	
	public static Double calculate(StateP s, StatePInitialRandom goalState) 
	{
		int counter = 0;
		byte[] allCells = s.getAllCells();
		int dimension = s.getDimension();

		for (int i = 0; i < allCells.length; i++) 
		{
			int value = allCells[i];
			if (value == 0) 
			{
				continue;
			}

			int row = i / dimension;
			int column = i % dimension;
			int expectedRow = goalState.getRowPosition(value);
			int expectedColumn = goalState.getColumnPosition(value);

			int difference = Math.abs(row - expectedRow)
					+ Math.abs(column - expectedColumn);
			counter += difference;

		}
		return (double) counter;
	}
}