package common.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import common.constants.Constants;
import common.impl.Action;
import common.model.StateP;
import common.queues.PQueue;
import common.utility.HeuristicSolverUtility;

import mpi.MPI;

public class AnchorHeuristic {
	
	PriorityQueue<StateP> nodePriorityQueue = PQueue.createQueue();
	HashMap<Integer, StateP> listOfNodesMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> listOfExpandedNodesMap = new HashMap<Integer, StateP>();
	Boolean isRunning = false;
	
	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);
	
	public AnchorHeuristic()
	{
		StateP randomState = HeuristicSolverUtility.createRandom(Constants.DIMENSION , Constants.w1);
		System.out.println("Random State");
		HeuristicSolverUtility.printState(randomState);
		Double initialBound = Constants.w1 * ManhattanDistance.calculate(randomState);
		StateP initialNode = new StateP(randomState, Constants.w1);
		
		initialNode.setPathCost(0);
		initialNode.setHeuristicCost((double) ManhattanDistance.calculate(randomState));
		
		// Adding to the list
		nodePriorityQueue.add(initialNode);
		listOfNodesMap.put(initialNode.hashCode(), initialNode);
		
		isRunning = true;
		
		hearMergeEvent();
		for(int i = 0 ; i < Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
		{
			startChild(i, initialBound, Constants.CommunicationInterval);
		}
		run();
		String message = "get started";
//		MPI.COMM_WORLD.Isend(message,0,2000,MPI.OBJECT,3,Constants.STARTOPERATION);
	}
	
	private void startChild(int queueID, Double initialBound, int communicationInterval)
	{
		// Start Stuff here
	}
	
	private void run()
	{
		while(isRunning)
		{
			while (nodePriorityQueue.isEmpty() == false) 
			{
				StateP queueHead = nodePriorityQueue.remove();
				listOfExpandedNodesMap.put(queueHead.hashCode(), queueHead);
				StateP queueHeadState = queueHead;

				// If reached goal state
				if (queueHead.equals(goalState)) 
				{
					System.out.println("Path length using A* is : "
							+ HeuristicSolverUtility.printPathLength(queueHead));
					break;
				} 
				else 
				{
					List<Action> listOfPossibleActions = queueHeadState
							.getPossibleActions();
					Iterator<Action> actIter = listOfPossibleActions.iterator();
					while (actIter.hasNext()) {
						Action actionOnState = actIter.next();
						StateP newState = actionOnState.applyTo(queueHeadState);
						StateP newNode = new StateP(newState, Constants.w1);
						if (!listOfExpandedNodesMap.containsKey(newNode.hashCode())) {
							newNode.setHeuristicCost((double) ManhattanDistance
									.calculate(newState));
							newNode.setParent(queueHead);
							newNode.setAction(actionOnState);
							nodePriorityQueue.offer(newNode);
						}
					}
				}
			}
		}
	}
	
	private void hearEvent()
	{
//		MPI.COMM_WORLD.
		List<StateP> listOfReceivedNodes = new ArrayList<StateP>();
		isRunning = false;
		merge(listOfReceivedNodes);
	}
	
	private void hearMergeEvent()
	{
//		MPI.COMM_WORLD.
		List<StateP> listOfReceivedNodes = new ArrayList<StateP>();
		isRunning = false;
		merge(listOfReceivedNodes);
	}
	
	private void merge(List<StateP> listOfReceivedNodes)
	{
		Iterator<StateP> nodeIter = listOfReceivedNodes.iterator();
		while(nodeIter.hasNext())
		{
			StateP node = nodeIter.next();
			StateP existingNode = listOfNodesMap.get(node.hashCode());
			if(existingNode != null)
			{
				if(existingNode.getPathCost() < node.getPathCost())
				{
					// Nothing to do here
				}
				else
				{
					existingNode.setPathCost(node.getPathCost());
					existingNode.setParent(node.getParent());
				}
			}
			else
			{
				nodePriorityQueue.add(node);
				listOfNodesMap.put(node.hashCode(), node);
			}
		}
		isRunning = true;
		run();
	}
}
