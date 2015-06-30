package common.impl;

import java.util.Comparator;

import common.model.StateP;

public class InadmissibleHeuristicQueue 
{
public static class HeuristicComparator implements Comparator<StateP>{	
		
		int heuristic = 0;

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
		
		public void setHeuristic(int h)
		{
			heuristic = h;
		}
	}
	
	public static java.util.PriorityQueue<StateP> createQueue(int h) {
		HeuristicComparator hc = new HeuristicComparator();
		hc.setHeuristic(h);
		return new java.util.PriorityQueue<StateP>(10000, hc);
	}
}
