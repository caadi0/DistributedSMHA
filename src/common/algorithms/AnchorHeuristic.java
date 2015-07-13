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

public class AnchorHeuristic {

	PriorityQueue<StateP> anchorPriorityQueue = PQueue.createQueue();
	HashMap<Integer, StateP> currentStatesInQueueHashMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> expandedNodesHashMap = new HashMap<Integer, StateP>();

	Boolean isExpansionAlgorithmRunning = false;

	StateP goalState = HeuristicSolverUtility.generateGoalState(
			Constants.DIMENSION, Constants.w1);

	public AnchorHeuristic() {
		StateP initialRandomState = HeuristicSolverUtility.createRandom(
				Constants.DIMENSION, Constants.w1);

		initialRandomState.setPathCost(0);
		initialRandomState.setHeuristicCost((double) ManhattanDistance
				.calculate(initialRandomState));

		Double initialBound = Constants.w1
				* initialRandomState.getHeuristicCost();

		// Adding initial state to the list
		anchorPriorityQueue.add(initialRandomState);
		currentStatesInQueueHashMap.put(initialRandomState.hashCode(),
				initialRandomState);

		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			startChild(i, initialBound, Constants.CommunicationInterval,
					initialRandomState);
		}

		hearMergeEvent();
		run();

	}
	
	private void startAllChildren(StateP randomState) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Bcast(start, 0, 1, MPI.OBJECT, Constants.STARTOPERATION);
	}

	private void startChild(int queueID, Double initialBound,
			int communicationInterval, StateP randomState) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start, 0, 1, MPI.OBJECT, queueID,
				Constants.STARTOPERATION);
	}

	public void run() {
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
		}
		stopAllChildren();
		MPI.Finalize();
	}

	private void stopChild(int queueID) {
		System.out.println("Sending stop");
		Boolean[] stop = new Boolean[1];
		MPI.COMM_WORLD.Isend(stop, 0, 1, MPI.OBJECT, queueID, Constants.STOP);
	}

	private void stopAllChildren() {
		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			stopChild(i);
		}
	}

	private void hearMergeEvent() {
		int[] sizeArray = new int[1];
		MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, MPI.ANY_SOURCE,
				Constants.SIZE);

		int size = sizeArray[0];

		StateP[] arrayOfStates = new StateP[size];
		MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT,
				MPI.ANY_SOURCE, Constants.MERGE);

		System.out.println("Anchor received states for merging");
		isExpansionAlgorithmRunning = false;
		merge(arrayOfStates);
		Arrays.fill(arrayOfStates, null);
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
