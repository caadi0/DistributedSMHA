package common.queues;

import java.util.Comparator;

import common.model.StateP;

public class AnchorQueue extends java.util.PriorityQueue<StateP>{
	
	private static final long serialVersionUID = 1L;

	public static class HeuristicComparator implements Comparator<StateP>{

		public int compare(StateP o1, StateP o2) {

			Double result;

			result = o1.getKey() - o2.getKey();
				
				if (result == 0){
					//Ties among minimal f values are resolved in favor of the deepest node in the search tree
					//i.e. the closest node to the goal
					result =  (double) (o2.getPathCost() - o1.getPathCost());			
					
				}
				if (result > 0.0)
					return 1;

				return -1;
		}
	}
	
	public static java.util.PriorityQueue<StateP> createQueue() {
		return new java.util.PriorityQueue<StateP>(10000, new HeuristicComparator());
	}

}
