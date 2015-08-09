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

public class AnchorHeuristic 
{

	PriorityQueue<StateP> anchorPriorityQueue = PQueue.createQueue();
	HashMap<Integer, StateP> currentStatesInQueueHashMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> expandedNodesHashMap = new HashMap<Integer, StateP>();

	Boolean isExpansionAlgorithmRunning = false;
	Integer _sendingInterval;
	Integer _listeningInterval;

	StateP goalState = HeuristicSolverUtility.generateGoalState(
			Constants.DIMENSION, Constants.w1);

	public AnchorHeuristic() {
		this._listeningInterval = Constants.CommunicationInterval;
		this._sendingInterval = Constants.CommunicationIntervalForAnchor;
		StateP initialRandomState = HeuristicSolverUtility.createRandom(
				Constants.DIMENSION, Constants.w1);

		initialRandomState.setPathCost(0);
		initialRandomState.setHeuristicCost((double) ManhattanDistance.calculate(initialRandomState));

		// Adding initial state to the list
		anchorPriorityQueue.add(initialRandomState);
		currentStatesInQueueHashMap.put(initialRandomState.hashCode(),
				initialRandomState);

		startAllChildren(initialRandomState);
		isExpansionAlgorithmRunning = true;
		run();

	}
	
	private void startAllChildren(StateP randomState) {
		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			startChild(randomState , i);
		}
	}

	private void startChild(StateP randomState, int queueID) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start, 0, 1, MPI.OBJECT, queueID,
				Constants.STARTOPERATION);
	}

	public void run() 
	{
		System.out.println("Running Anchor Queue");
		while (anchorPriorityQueue.isEmpty() == false
				&& isExpansionAlgorithmRunning) {
			StateP queueHead = anchorPriorityQueue.remove();
			expandedNodesHashMap.put(queueHead.hashCode(), queueHead);
			StateP queueHeadState = queueHead;

			// If reached goal state
			if (queueHead.equals(goalState)) {
				System.out.println("Path length using A* is : "
						+ HeuristicSolverUtility.printPathLength(queueHead));
				break;
			} else {
				List<Action> listOfPossibleActions = queueHeadState
						.getPossibleActions();
				Iterator<Action> actIter = listOfPossibleActions.iterator();
				while (actIter.hasNext()) {
					Action actionOnState = actIter.next();
					StateP newState = actionOnState.applyTo(queueHeadState);
					if (!expandedNodesHashMap.containsKey(newState.hashCode())) {
						newState.setHeuristicCost((double) ManhattanDistance
								.calculate(newState));
						newState.setParent(queueHead);
						newState.setAction(actionOnState);

						anchorPriorityQueue.offer(newState);
						currentStatesInQueueHashMap.put(newState.hashCode(),
								newState);
					}
				}
			}
			if(this._sendingInterval-- == 0)
			{
				for(int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
				{
					sendStatesForMerging(i);
				}
				this._sendingInterval = Constants.CommunicationIntervalForAnchor;
			}
			
			if(this._listeningInterval-- == 0)
			{
				hearMergeEvent();
				this._sendingInterval = Constants.CommunicationInterval;
			}
			System.out.println("Anchor Queue is running wild");
		}
		stopAllChildren();
		MPI.Finalize();
	}

	private void stopAllChildren() 
	{
		Boolean[] stop = new Boolean[1];
		stop[0] = true;
		MPI.COMM_WORLD.Bcast(stop, 0, 1, MPI.OBJECT, Constants.STOP);
	}

	private void hearMergeEvent() 
	{
		System.out.println("Did anchor receive any states ");
		int[] sizeArray = new int[1];
		MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, MPI.ANY_SOURCE,
				Constants.SIZE);

		Integer size = sizeArray[0];

		if(size != null && size > 0)
		{
			StateP[] arrayOfStates = new StateP[size];
			MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT,
					MPI.ANY_SOURCE, Constants.MERGE);

			System.out.println("Anchor received states for merging");
			isExpansionAlgorithmRunning = false;
			merge(arrayOfStates);
			Arrays.fill(arrayOfStates, null);
			isExpansionAlgorithmRunning = true;
		}
	}
	
	private void sendStatesForMerging(Integer queueID)
	{
		System.out.println("Sending states from Anchor ");
		StateP[] arrayOfStates = anchorPriorityQueue.toArray(new StateP[0]);
		
		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
		
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, queueID, Constants.SIZE);
		
		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, queueID, Constants.MERGE);
		
	}

	private void merge(StateP[] listOfReceivedNodes) {
		System.out.println("I reached here ttoo");
		for (StateP node : listOfReceivedNodes) {
			if (node == null) {
				break;
			}
			StateP existingNode = currentStatesInQueueHashMap.get(node
					.hashCode());
			System.out.println("I am merging states");
			if (existingNode != null) {
				if (existingNode.getPathCost() < node.getPathCost()) {
					// Nothing to do here
				} else {
					existingNode.setPathCost(node.getPathCost());
					existingNode.setParent(node.getParent());
				}
			} else {
				anchorPriorityQueue.add(node);
				currentStatesInQueueHashMap.put(node.hashCode(), node);
			}
		}
		isExpansionAlgorithmRunning = true;
		run();
	}
}
