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
	PriorityQueue<StateP> statesExpandedInLastIterationQueue = PQueue.createQueue();
	HashMap<Integer, StateP> currentStatesInQueueHashMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> expandedNodesHashMap = new HashMap<Integer, StateP>();

	Integer _sendingInterval;
	Integer _listeningInterval;

	StateP goalState = HeuristicSolverUtility.generateGoalState(
			Constants.DIMENSION, Constants.w1);
	int[] sizeArray = new int[1];
	
	Request reqH;

	public AnchorHeuristic() {
		this._listeningInterval = Constants.CommunicationInterval;
		this._sendingInterval = Constants.CommunicationIntervalForAnchor;
		StateP initialRandomState = HeuristicSolverUtility.createRandom(
				Constants.DIMENSION, Constants.w1);

		initialRandomState.setPathCost(0);
		initialRandomState.setHeuristicCost((double) LinearConflict.calculate(initialRandomState));

		// Adding initial state to the list
		anchorPriorityQueue.add(initialRandomState);
		statesExpandedInLastIterationQueue.add(initialRandomState);
		currentStatesInQueueHashMap.put(initialRandomState.hashCode(),
				initialRandomState);
		
		System.out.println("goal state is: "+goalState.hashCode());

		startAllChildren(initialRandomState);
		run();

	}
	
	private void startAllChildren(StateP randomState) {
		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			startChild(randomState , i);
		}
	}

	private static void startChild(StateP randomState, int queueID) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start, 0, 1, MPI.OBJECT, queueID,
				Constants.STARTOPERATION).Wait();
	}

	public void run() 
	{
		System.out.println("Running Anchor Queue");
		while (anchorPriorityQueue.isEmpty() == false) {
//			HeuristicSolverUtility.printAllCostsInQueue(anchorPriorityQueue);
			StateP queueHead = anchorPriorityQueue.remove();
			System.out.println("Removed Value in Anchor Queue "+ queueHead.getPathCost() + " : "+queueHead.getHeuristicCost() + " ; ");
			currentStatesInQueueHashMap.remove(queueHead.hashCode());
			if(statesExpandedInLastIterationQueue.contains(queueHead)) {
				statesExpandedInLastIterationQueue.remove(queueHead);
			}
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
//					System.out.println("Actions being performed are "+actionOnState.getMove().toString());
					StateP newState = actionOnState.applyTo(queueHeadState);
					if (!expandedNodesHashMap.containsKey(newState.hashCode())) {
						newState.setHeuristicCost((double) LinearConflict
								.calculate(newState));
						newState.setParent(queueHead);
						
						if(!currentStatesInQueueHashMap.containsKey(newState.hashCode())) {
							anchorPriorityQueue.offer(newState);
							statesExpandedInLastIterationQueue.offer(newState);
							currentStatesInQueueHashMap.put(newState.hashCode(),
									newState);
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
				}
			}
			if(this._sendingInterval-- == 0)
			{
				for(int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
				{
					sendStatesForMerging(i);
				}
				statesExpandedInLastIterationQueue.clear();
				this._sendingInterval = Constants.CommunicationIntervalForAnchor;
			}
			
			if(this._listeningInterval-- == 0)
			{
				for(int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
				{
					System.out.println("Anchor Trying to listen");
					hearMergeEvent(i);
				}
				this._listeningInterval = Constants.CommunicationInterval;
			}
			
//			System.out.println("Anchor Heuristic is executing");
		}
		stopAllChildren();
		MPI.Finalize();
	}

	private void stopAllChildren() 
	{
//		Boolean[] stop = new Boolean[1];
//		stop[0] = true;
//		MPI.COMM_WORLD.Bcast(stop, 0, 1, MPI.OBJECT, Constants.STOP);
	}

	private void hearMergeEvent(Integer queueID) 
	{

		if(reqH == null)
		{
			reqH = MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, queueID,
					Constants.SIZE);
		}
		
		Status status = reqH.Test();
		if(status == null)
		{
			System.out.println("STATUS is NULL");
			return;
		}
		else
		{
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
			Arrays.fill(arrayOfStates, null);
		}
	}
	
	private void sendStatesForMerging(Integer queueID)
	{
		StateP[] arrayOfStates = statesExpandedInLastIterationQueue.toArray(new StateP[0]);
		
		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
	
		System.out.println("Sending states from Anchor of size "+arrayOfStates.length);
		
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, queueID, Constants.SIZE);
		
		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, queueID, Constants.MERGE);
		
	}

	private void merge(StateP[] listOfReceivedNodes) {
		for (StateP node : listOfReceivedNodes) {
			if (node == null) {
				break;
			}
			StateP existingNode = currentStatesInQueueHashMap.get(node
					.hashCode());
//			System.out.println("I am merging states");
			if (existingNode != null) {
				if (existingNode.getPathCost() <= node.getPathCost()) {
					// Nothing to do here
				} else {
					anchorPriorityQueue.remove(existingNode);
//					System.out.println("Improving state from "+existingNode.getPathCost()+" to "+node.getPathCost());
					existingNode.setPathCost(node.getPathCost());
					existingNode.setParent(node.getParent());
					anchorPriorityQueue.add(existingNode);
				}
			} else {
//				System.out.println("Adding state");
				node.setHeuristicCost((double) LinearConflict.calculate(node));
				anchorPriorityQueue.add(node);
				statesExpandedInLastIterationQueue.add(node);
				currentStatesInQueueHashMap.put(node.hashCode(), node);
			}
			
			StateP parentNode = node.getParent();
			if(parentNode != null) {
				if(currentStatesInQueueHashMap.containsKey(parentNode.hashCode())) {
//					System.out.println("REMOVING PARENT");
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
