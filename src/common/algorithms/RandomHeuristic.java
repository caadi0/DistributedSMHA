package common.algorithms;

import java.util.List;

import common.model.StateP;

public class RandomHeuristic {
	
	private Double bound;
	private int numberOfPermutations;
	private int queueID;
	
	
	public RandomHeuristic(int queueID, Double bound, int numberOfPermutations)
	{
		this.bound = bound;
		this.numberOfPermutations = numberOfPermutations;
	}
	
	private void mergeStates(List<StateP> listOfStatestoMerge)
	{
		
	}
	
	public static void main(String[] args) {
		
	}

}
