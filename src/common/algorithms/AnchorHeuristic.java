package common.algorithms;

import java.util.Arrays;
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
import mpi.Status;

public class AnchorHeuristic 
{

	PriorityQueue<StateP> anchorPriorityQueue = PQueue.createQueue();
	HashMap<Integer, StateP> expandedNodesHashMap = new HashMap<Integer, StateP>();
	
	PriorityQueue<StateP> statesExpandedInLastIterationQueue = PQueue.createQueue();
	HashMap<Integer, StateP> currentStatesInQueueHashMap = new HashMap<Integer, StateP>();

	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION, Constants.w1);
	int[] sizeArray = new int[1];
	
	Double _bound = 0.0;
	private Integer intervalForAnchorToListen = Integer.MAX_VALUE;
	Request reqH;

	public AnchorHeuristic() 
	{
		StateP initialRandomState = HeuristicSolverUtility.createRandom(Constants.DIMENSION, Constants.w1);
		initialRandomState.setPathCost(0);
		initialRandomState.setHeuristicCost(getHeuristicValue(initialRandomState));
		
		addToAllDatastructures(initialRandomState);

		startAllChildren(initialRandomState);
		run();
	}
	
	private Double getHeuristicValue(StateP state) {
		return (double) LinearConflict.calculate(state);
	}
	
	private void addToAllDatastructures(StateP state) {
		anchorPriorityQueue.add(state);
		statesExpandedInLastIterationQueue.add(state);
		currentStatesInQueueHashMap.put(state.hashCode(), state);
	}
	
	private void startAllChildren(StateP randomState) {
		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			startChild(randomState , i);
		}
	}

	private void startChild(StateP randomState, int queueID) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start, 0, 1, MPI.OBJECT, queueID, Constants.STARTOPERATION).Wait();
		
		Double[] bound = new Double[1];
		bound[0] = getBound();
		MPI.COMM_WORLD.Isend(bound, 0, 1, MPI.OBJECT, queueID, Constants.BOUND).Wait();		
	}
	
	private Double getBound() {
		StateP topOfQueue = anchorPriorityQueue.peek();
		if(topOfQueue == null) {
			System.out.println("FATAL ERROR : Queue is empty");
			return 0.0;
		}
		return Constants.w2 * topOfQueue.getKey();
	}

	public void run() 
	{
		System.out.println("Running Anchor Queue");
		intervalForAnchorToListen = Constants.CommunicationIntervalForAnchorToListen;
		
		while (anchorPriorityQueue.isEmpty() == false) {
			
			StateP queueHead = anchorPriorityQueue.remove();
			System.out.println("Removed Value in Anchor Queue "+ queueHead.getPathCost() + " : "+queueHead.getHeuristicCost() + " ; ");
			
			currentStatesInQueueHashMap.remove(queueHead.hashCode());
			
			expandedNodesHashMap.put(queueHead.hashCode(), queueHead);

			// If reached goal state
			if (queueHead.equals(goalState)) {
				
				System.out.println("Path length using A* is : "+ HeuristicSolverUtility.printPathLength(queueHead));
				break;
				
			} else {
				List<Action> listOfPossibleActions = queueHead.getPossibleActions();
				Iterator<Action> actIter = listOfPossibleActions.iterator();
				
				while (actIter.hasNext()) {
					Action actionOnState = actIter.next();
					applyActionToState(actionOnState, queueHead);
					
				}
			}
			if (intervalForAnchorToListen-- == 0) {
				for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
					System.out.println("Anchor Trying to listen");
					hearMergeEvent(i);
				}
			}
		}
	}
	
	private void applyActionToState(Action action, StateP state) {
		StateP newState = action.applyTo(state);
		if (!expandedNodesHashMap.containsKey(newState.hashCode())) 
		{
			newState.setHeuristicCost(getHeuristicValue(newState));
			newState.setParent(state);
			
			if(!currentStatesInQueueHashMap.containsKey(newState.hashCode())) {
				addToAllDatastructures(newState);
			} else {
				System.out.println("KEY MATCH");
				StateP existingNode = currentStatesInQueueHashMap.get(newState.hashCode());
				if(existingNode.getPathCost() < newState.getPathCost()) {
					// Do nothing
				} else {
					anchorPriorityQueue.remove(existingNode);
					existingNode.setPathCost(newState.getPathCost());
					existingNode.setParent(newState.getParent());
					anchorPriorityQueue.add(existingNode);
				}
			}
		}
		else
		{
			// This state had already been expanded previously
		}
	}

	private void hearMergeEvent(Integer queueID) 
	{
		// TODO : Optimization could be to have Heuristic specific Request variable.
		if(reqH == null) {
			reqH = MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, queueID,
					Constants.SIZE);
		}
		
		Status status = reqH.Test();
		if(status == null) {
			return;
		} else {
			System.out.println("Incoming MESSAGE");
			reqH.Wait();
			reqH = null;
		}
			
		Integer size = sizeArray[0];

		System.out.println("Size received by anchor "+size);
		if(size != null && size > 0)
		{
			StateP[] arrayOfStates = new StateP[size];
			MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT,
					queueID, Constants.MERGE).Wait();

			System.out.println("Anchor received states for merging");
			merge(arrayOfStates);
			sendStatesForMerging(queueID);
			Arrays.fill(arrayOfStates, null);
		}
	}
	
	private void sendStatesForMerging(Integer queueID)
	{
		StateP[] arrayOfStates = statesExpandedInLastIterationQueue.toArray(new StateP[0]);
		
		sizeArray[0] = arrayOfStates.length;
	
		System.out.println("Sending states from Anchor of size "+arrayOfStates.length);
		
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, queueID, Constants.SIZE).Wait();
		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, queueID, Constants.MERGE).Wait();
		
		Double[] bound = new Double[1];
		bound[0] = getBound();
		MPI.COMM_WORLD.Isend(bound, 0, 1, MPI.DOUBLE, queueID, Constants.BOUND).Wait();
		
	}

	private void merge(StateP[] listOfReceivedNodes) {
		for (StateP node : listOfReceivedNodes) {
			
			if (node == null) {
				// Something is not right in this case
				break;
			}
			
			StateP existingNode = currentStatesInQueueHashMap.get(node.hashCode());
			if (existingNode != null) {
				if (existingNode.getPathCost() <= node.getPathCost()) {
					// Nothing to do here
				} else {
					anchorPriorityQueue.remove(existingNode);
					existingNode.setPathCost(node.getPathCost());
					existingNode.setParent(node.getParent());
					anchorPriorityQueue.add(existingNode);
				}
			} else {
				node.setHeuristicCost(getHeuristicValue(node));
				addToAllDatastructures(node);
			}
			
			StateP parentNode = node.getParent();
			if(parentNode != null) {
				if(currentStatesInQueueHashMap.containsKey(parentNode.hashCode())) {
					anchorPriorityQueue.remove(parentNode);
					statesExpandedInLastIterationQueue.remove(parentNode);
					currentStatesInQueueHashMap.remove(parentNode.hashCode());
				} else {
					// Maybe its yet to be added
				}
			} else {
				// THis would be the case of Anchor node
			}
		}
	}
}
