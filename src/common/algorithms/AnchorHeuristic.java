package common.algorithms;

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
import mpi.Request;

public class AnchorHeuristic {
	
	PriorityQueue<StateP> nodePriorityQueue = PQueue.createQueue();
	HashMap<Integer, StateP> listOfNodesMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> listOfExpandedNodesMap = new HashMap<Integer, StateP>();
	Boolean isRunning = false;
	
	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);
	
	public AnchorHeuristic()
	{
		StateP randomState = HeuristicSolverUtility.createRandom(Constants.DIMENSION , Constants.w1);
		
		randomState.setPathCost(0);
		randomState.setHeuristicCost((double) ManhattanDistance.calculate(randomState));
		Double initialBound = Constants.w1 * randomState.getHeuristicCost();
		
		// Adding to the list
		nodePriorityQueue.add(randomState);
		listOfNodesMap.put(randomState.hashCode(), randomState);
		
		isRunning = true;
		
		for(int i = 1 ; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
		{
			startChild(i, initialBound, Constants.CommunicationInterval, randomState);
		}
		run();
		hearMergeEvent();
		
	}
	
	private void startChild(int queueID, Double initialBound, int communicationInterval, StateP randomState)
	{
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start , 0 , 1 , MPI.OBJECT , queueID , Constants.STARTOPERATION);
	}
	
	public void run()
	{
			while (nodePriorityQueue.isEmpty() == false && isRunning) 
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
						if (!listOfExpandedNodesMap.containsKey(newState.hashCode())) {
							newState.setHeuristicCost((double) ManhattanDistance
									.calculate(newState));
							newState.setParent(queueHead);
							newState.setAction(actionOnState);
							
							nodePriorityQueue.offer(newState);
							listOfNodesMap.put(newState.hashCode(), newState);
						}
					}
				}
			}
			stopAllChildren();
			MPI.Finalize();
	}
	
	private void stopChild(int queueID)
	{
		System.out.println("Sending stop");
		Boolean[] stop = new Boolean[1];
		MPI.COMM_WORLD.Isend(stop , 0 , 1 , MPI.OBJECT , queueID , Constants.STOP);
	}
	
	private void stopAllChildren() 
	{
		for(int i = 1 ; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
		{
			stopChild(i);
		}
	}
	
	private void hearMergeEvent()
	{
		int[] sizeArray = new int[1];
		Request request2 = MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, MPI.ANY_SOURCE, Constants.SIZE);
		request2.Wait();
		
		int size = sizeArray[0];
		
		StateP[] arrayOfStates = new StateP[size];
		Request request3 = MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT, 0, Constants.MERGE);		
		request3.Wait();
		
		isRunning = false;
		merge(arrayOfStates);
	}
	
	private void merge(StateP[] listOfReceivedNodes)
	{
		for(StateP node : listOfReceivedNodes)
		{
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
