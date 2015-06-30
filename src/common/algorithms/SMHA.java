package common.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import common.constants.Constants;
import common.impl.Action;
import common.impl.InadmissibleHeuristicQueue;
import common.impl.RandomHeuristicGenerator;
import common.model.StateP;
import common.queues.AnchorQueue;
import common.utility.HeuristicSolverUtility;

public class SMHA {
	
	Long startTime = null, endTime = null;

	private HashMap<Integer, Boolean> visited = new HashMap<Integer, Boolean>();
	private HashMap<Integer, Boolean> expandedByAnchor = new HashMap<Integer, Boolean>();
	private HashMap<Integer, Boolean> expandedByInadmissible = new HashMap<Integer, Boolean>();
	private StateP nGoal = null;
	private int pathLength = 0;
	
	public void SMHAstar(StateP randomState) 
	{
		startTime = System.nanoTime();
		StateP nStart = new StateP(randomState, Constants.w1);
		nStart.setPathCost(0);
		
		StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);

		System.out.println("Goal State");
		HeuristicSolverUtility.printState(goalState);
		nGoal = new StateP(goalState, Constants.w1);
		
		PriorityQueue<StateP> pq = AnchorQueue.createQueue();
		pq.add(nStart);	
		
		List<PriorityQueue<StateP>> pqList = new ArrayList<PriorityQueue<StateP>>();
		
		for(int i=0; i<Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++)
		{
			PriorityQueue<StateP> prq = InadmissibleHeuristicQueue.createQueue(i+1);
			prq.add(nStart);
			pqList.add(prq);
		}
		
		visited.put(nStart.hashCode(), true);
//		System.out.println("Visited:");
//		printState(nStart.getState());
		
		while(pq.isEmpty() == false) {
			
			int i = 0;
			for(PriorityQueue<StateP> p: pqList)
			{
				i++;
				PriorityQueue<StateP> selected = null;
				if(expandAnchor(pq.peek(), p.peek(), i))
				{
					selected = pq;
					expandedByAnchor.put(selected.peek().hashCode(), true);
//					System.out.println("Expanded by anchor:");
//					printState(selected.peek().getState());
					if(nGoal.getPathCost() <= anchorKey(selected.peek()))
					{
						pathLength = HeuristicSolverUtility.printPathLength(nGoal);
						System.out.println("path length using SMHA is :"+pathLength);
						endTime = System.nanoTime();
						System.out.println("time taken is: "+(endTime-startTime)/1000000);
						return;
					}
				}
				else
				{
					selected = p;
					expandedByInadmissible.put(selected.peek().hashCode(), true);
//					System.out.println("Expanded by inadmissible heuristic: ");
//					printState(selected.peek().getState());
					if(nGoal.getPathCost() <= inadmissibleNodeKey(selected.peek(), i))
					{
						pathLength = HeuristicSolverUtility.printPathLength(nGoal);
						System.out.println("path length using SMHA is :"+pathLength);
						endTime = System.nanoTime();
						System.out.println("time taken is: "+(endTime-startTime)/1000000);
						return;
					}
				}

				StateP node = selected.remove();
				
				expandNode(pq, pqList, node , i);
			}
	
		}
		System.out.println("anchor queue emptied");
		
		
	}
	
	private void expandNode(PriorityQueue<StateP> anchorPQ, List<PriorityQueue<StateP>> listPQ, StateP toBeExpanded , int heuristicID)
	{
		anchorPQ.remove(toBeExpanded);
		removeNodeForSimilarStateFromQueue(anchorPQ, toBeExpanded);
		for(PriorityQueue<StateP> pq: listPQ)
		{
			pq.remove(toBeExpanded);
			removeNodeForSimilarStateFromQueue(pq, toBeExpanded);
		}
		StateP state = toBeExpanded;
		List<Action> listOfPossibleActions = state.getPossibleActions();
		Iterator<Action> actIter = listOfPossibleActions.iterator();
		while(actIter.hasNext()) {
			Action actionOnState = actIter.next();
			StateP newState = actionOnState.applyTo(state);
			StateP newNode = new StateP(newState, Constants.w1);
//			if(visited.get(newState.hashCode()) == null)
//			{
//				 initialise cost to infinity and parent to null;
//			}
			visited.put(newNode.hashCode(), true);
//			System.out.println("Visited:");
//			printState(newState);
			if(newNode.getPathCost() > toBeExpanded.getPathCost()+1)
			{
				newNode.setParent(toBeExpanded);
				if(expandedByAnchor.get(newNode.hashCode()) == null)
				{
					removeNodeForSimilarStateFromQueue(anchorPQ, newNode);
					newNode.setHeuristicCost((double)ManhattanDistance.calculate(newNode));
					anchorPQ.add(newNode);
					if(expandedByInadmissible.get(newNode.hashCode()) == null)
					{
						newNode.setHeuristicCost(RandomHeuristicGenerator.generateRandomHeuristic(heuristicID, newNode));
						addOrUpdateNodeToInadmissibleQueues(listPQ, newNode);
					}
				}
				if(newNode.hashCode() == nGoal.hashCode())
//					nGoal.setCost(newNode.getCost());
					nGoal = newNode;
			}
			
		}
	}
	
	
	
	private static void removeNodeForSimilarStateFromQueue(PriorityQueue<StateP> pq, StateP searchNode)
	{
		List<StateP> removeList = new ArrayList<StateP>();
		for(StateP node: pq)
		{
			if(node.hashCode() == searchNode.hashCode())
				removeList.add(node);
		}
		pq.removeAll(removeList);
	}
	
	private void addOrUpdateNodeToInadmissibleQueues(List<PriorityQueue<StateP>> listPQ, StateP toBeAdded)
	{
		int heuristic = 0;
		for(PriorityQueue<StateP> pq: listPQ)
		{
			heuristic++;
			if(inadmissibleNodeKey(toBeAdded, heuristic) <= Constants.w2*anchorKey(toBeAdded))
			{
				removeNodeForSimilarStateFromQueue(pq, toBeAdded);
				pq.add(toBeAdded);
			}
		}
	}
	
	
	private Boolean expandAnchor(StateP anchor, StateP inadmissible, int heuristic)
	{
		if(inadmissible == null)
			return true;
		
		Boolean result = false;
		
		Double minKeyAnchor = anchorKey(anchor);
		Double minKeyInadmissible = inadmissibleNodeKey(inadmissible, heuristic);
		if(minKeyInadmissible <= Constants.w2*minKeyAnchor)
		{
			result = false;
		}
		else
		{
			result = true;
		}
		return result;
	}
	
	private Double anchorKey(StateP anchor)
	{
		return (anchor.getPathCost() + Constants.w1* ManhattanDistance.calculate(anchor));
	}
	
	private Double inadmissibleNodeKey(StateP inadmissible, int heuristic)
	{
		return inadmissible.getPathCost() +Constants.w1*RandomHeuristicGenerator.generateRandomHeuristic
				(heuristic, inadmissible);
	}
	
	public static void main(String args[])
	{
		StateP randomState = HeuristicSolverUtility.createRandom(Constants.DIMENSION, Constants.w1);
		
		System.out.println("Random State");
		HeuristicSolverUtility.printState(randomState);
		
		System.out.println(HeuristicSolverUtility.isStateSolvable(randomState));
		SMHA smha = new SMHA();
		smha.SMHAstar(randomState);
	}

}

