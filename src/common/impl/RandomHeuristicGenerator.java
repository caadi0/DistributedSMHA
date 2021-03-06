package common.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import common.algorithms.LinearConflict;
import common.algorithms.ManhattanDistance;
import common.model.StateP;

public class RandomHeuristicGenerator 
{
	// Contains random numbers for each heuristic to multiply LC and MD
		private static HashMap<Integer, List<Double>> randomNumberMap = new HashMap<Integer, List<Double>>();

		public static Double generateRandomHeuristic(Integer heuristicID,
				StateP state) {
			List<Double> randNums = getRandomNumbersForHeuristic(heuristicID);

			if (heuristicID == 0)
				return (double) ManhattanDistance.calculate(state);
			return randNums.get(0) * ManhattanDistance.calculate(state)
					+ randNums.get(1) * LinearConflict.calculate(state);
		}

		
		private static List<Double> getRandomNumbersForHeuristic(Integer id) {
			List<Double> listOfRandomNumbers = new ArrayList<Double>();
			if(randomNumberMap.containsKey(id)) {
				listOfRandomNumbers = randomNumberMap.get(id);
			} else {
				listOfRandomNumbers.add(Math.random());
				listOfRandomNumbers.add(Math.random());
				randomNumberMap.put(id, listOfRandomNumbers);
			}
			return listOfRandomNumbers;
		}

}
