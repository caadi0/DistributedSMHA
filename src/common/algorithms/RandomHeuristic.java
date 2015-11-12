package common.algorithms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import sun.font.CreatedFontTracker;
import mpi.MPI;
import common.constants.Constants;
import common.impl.Action;
import common.impl.InadmissibleHeuristicQueue;
import common.impl.RandomHeuristicGenerator;
import common.model.StateP;
import common.model.StatePInitialRandom;
import common.utility.HeuristicSolverUtility;

public class RandomHeuristic {

	private int _queueID;
	private StateP _initialRandomState;
	private StatePInitialRandom _randomGoalState;
	private Double _bound;
	int[] sizeArray = new int[1];

	PriorityQueue<StateP> nodePriorityQueue;
	PriorityQueue<StateP> statesExpandedInLastIterationQueue;
	HashMap<Integer, StateP> currentStatesInQueueHashMap;
	HashMap<Integer, StateP> listOfExpandedNodesMap;

	StateP goalState = HeuristicSolverUtility.generateGoalState(
			Constants.DIMENSION, Constants.w1);

	public RandomHeuristic(int queueID) {
		this._queueID = queueID;

		// Initializing all the data structures
		nodePriorityQueue = InadmissibleHeuristicQueue.createQueue();
		statesExpandedInLastIterationQueue = InadmissibleHeuristicQueue
				.createQueue();

		// Hash code is the Key for these data structures
		currentStatesInQueueHashMap = new HashMap<Integer, StateP>();
		listOfExpandedNodesMap = new HashMap<Integer, StateP>();

		System.out.println("I am Random Heuristic running on core number : "
				+ queueID);
		hearStartEvent();
	}

	private void hearStartEvent() {
		// Receiving Initial Random State
		StateP start[] = new StateP[1];
		MPI.COMM_WORLD.Irecv(start, 0, 1, MPI.OBJECT, 0,
				Constants.STARTOPERATION).Wait();
		this._initialRandomState = start[0];

		// Adding initial State to all the Queues
		addToAllDatastructures(_initialRandomState);

		System.out.println("Initial State Received by Queue " + this._queueID
				+ " Waiting for Bound");
		// Bound Check
		Double[] bound = new Double[1];
		MPI.COMM_WORLD.Irecv(bound, 0, 1, MPI.OBJECT, 0, Constants.BOUND)
				.Wait();
		this._bound = bound[0];

		System.out.println("Started Queue ID : " + _queueID
				+ " with a bound of " + this._bound);
		run();
	}

	private void addToAllDatastructures(StateP state) {
		nodePriorityQueue.add(state);
		statesExpandedInLastIterationQueue.add(state);
		currentStatesInQueueHashMap.put(state.hashCode(), state);
	}

	private void sendStatesForMerging() {
		System.out.println("Sending states from Queue ID " + this._queueID);
		StateP[] arrayOfStates = statesExpandedInLastIterationQueue
				.toArray(new StateP[0]);

		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
		System.out.println("Broadcasted length from Queue ID " + this._queueID
				+ " is " + sizeArray[0]);
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE)
				.Wait();

		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length,
				MPI.OBJECT, 0, Constants.MERGE).Wait();
		System.out.println("Sent States expanded by Queue : " + this._queueID);

		statesExpandedInLastIterationQueue.clear();

	}

	private static Double receiveNewBound() {
		Double[] bound = new Double[1];
		MPI.COMM_WORLD.Irecv(bound, 0, 1, MPI.OBJECT, 0, Constants.BOUND)
				.Wait();
		System.out.println("New Bound Received is : " + bound[0]);
		return bound[0];
	}

	private void run() {
		// Generating Random State to create a mock Goal State
		this._randomGoalState = HeuristicSolverUtility.createRandom(Constants.DIMENSION, Constants.w1, Constants.DegreeOfRandomnessForRandomHeuristics);
		while (nodePriorityQueue.isEmpty() == false) {
			StateP queueHead = nodePriorityQueue.remove();

			if (queueHead.getKey() > this._bound) {
				sendStatesForMerging();
				this._bound = receiveNewBound();
				continue;
			}

			System.out.println("Removed Value in Random Queue " + _queueID
					+ " " + queueHead.getPathCost() + " : "
					+ queueHead.getHeuristicCost() + " ; ");
			currentStatesInQueueHashMap.remove(queueHead.hashCode());

			if (statesExpandedInLastIterationQueue.contains(queueHead)) {
				statesExpandedInLastIterationQueue.remove(queueHead);
			}

			listOfExpandedNodesMap.put(queueHead.hashCode(), queueHead);
			StateP queueHeadState = queueHead;

			if (queueHead.equals(this._randomGoalState)) {
				System.out.println("Path length using A* for QueueID "
						+ _queueID + " is : "
						+ HeuristicSolverUtility.printPathLength(queueHead));
				HeuristicSolverUtility.printState(queueHead);
				break;
			} else {
				List<Action> listOfPossibleActions = queueHeadState
						.getPossibleActions();
				Iterator<Action> actIter = listOfPossibleActions.iterator();
				while (actIter.hasNext()) {
					Action actionOnState = actIter.next();
					StateP newState = actionOnState.applyTo(queueHeadState);
					if (!listOfExpandedNodesMap
							.containsKey(newState.hashCode())) {
						newState.setHeuristicCost(GenericManhattanDistance.calculate(newState, _randomGoalState) );
						newState.setParent(queueHead);
						newState.setAction(actionOnState);

						if (!currentStatesInQueueHashMap.containsKey(newState
								.hashCode())) {
							nodePriorityQueue.offer(newState);

							System.out.println("Added Value in Random Queue "
									+ _queueID + "  " + newState.getPathCost()
									+ " : " + newState.getHeuristicCost()
									+ " ; ");
							currentStatesInQueueHashMap.put(
									newState.hashCode(), newState);
							statesExpandedInLastIterationQueue.offer(newState);
						} else {
							StateP existingNode = currentStatesInQueueHashMap
									.get(newState.hashCode());

							if (existingNode.getPathCost() < newState
									.getPathCost()) {
								// Do nothing
							} else {
								nodePriorityQueue.remove(existingNode);
								existingNode
										.setPathCost(newState.getPathCost());
								existingNode.setParent(newState.getParent());
								existingNode.setAction(actionOnState);
								nodePriorityQueue.add(existingNode);
							}

						}
					}
				}
			}
		}
	}

}
