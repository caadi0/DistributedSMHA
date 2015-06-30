package common.queues;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import common.constants.Constants;
import common.model.StateP;
import common.utility.HeuristicSolverUtility;

public class PQueue extends java.util.PriorityQueue<StateP> 
{

	private static final long serialVersionUID = 1L;

	public static HashMap<Integer, PriorityQueue<StateP>> priorityQueueMap = new HashMap<Integer, PriorityQueue<StateP>>();

	public static class HeuristicComparator implements Comparator<StateP> 
	{
		public int compare(StateP o1, StateP o2) 
		{
			Double result;

			result = o1.getKey() - o2.getKey();
			if (result == 0.0) 
			{
				// Ties among minimal f values are resolved in favor of the
				// deepest node in the search tree
				// i.e. the closest node to the goal
				result = (double) (o2.getPathCost() - o1.getPathCost());
			}
			if (result > 0.0)
				return 1;

			return -1;
		}
	}

	public static java.util.PriorityQueue<StateP> createQueue() 
	{
		return new java.util.PriorityQueue<StateP>(10000,
				new HeuristicComparator());
	}

	public static java.util.PriorityQueue<StateP> getQueueForIndex(
			Integer index) 
	{
		PriorityQueue<StateP> pq = priorityQueueMap.get(index);
		if (pq == null) 
		{
			pq = createQueue();
			priorityQueueMap.put(index, pq);
		}
		return pq;
	}

	public static StateP getStateWithSameArrangementFromQueue(StateP node,
			Integer index) throws Exception 
	{
		PriorityQueue<StateP> q = getQueueForIndex(index);
		Iterator<StateP> iter = q.iterator();
		while (iter.hasNext()) 
		{
			StateP n = iter.next();
			if (n.equals(node))
				return n;
		}
		throw new Exception("Element not matched exception");
	}
	
	public static StateP getGoalStateFromQueue(
			Integer index, Double weight) throws Exception 
	{		
		PriorityQueue<StateP> q = getQueueForIndex(index);
		Iterator<StateP> iter = q.iterator();
		while (iter.hasNext()) 
		{
			StateP n = iter.next();
			if (n.equals(HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , weight)))
				return n;
		}
		throw new Exception("Element not matched exception");
	}
}
