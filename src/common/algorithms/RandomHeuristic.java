package common.algorithms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import mpi.MPI;
import mpi.Request;
import mpi.Status;

import common.constants.Constants;
import common.impl.Action;
import common.impl.InadmissibleHeuristicQueue;
import common.impl.RandomHeuristicGenerator;
import common.model.StateP;
import common.queues.PQueue;
import common.utility.HeuristicSolverUtility;

public class RandomHeuristic {
	
	Boolean isRunning = false;
	
	private Double _bound;
	private int _numberOfPermutations;
	private int _queueID;
	private StateP _randomState;
	
	PriorityQueue<StateP> nodePriorityQueue;
	HashMap<Integer, StateP> listOfNodesMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> listOfExpandedNodesMap = new HashMap<Integer, StateP>();
	
	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);
	
	public RandomHeuristic(int queueID, Double bound, int numberOfPermutations)
	{
		this._bound = bound;
		this._numberOfPermutations = numberOfPermutations;
		this._queueID = queueID;
		nodePriorityQueue = InadmissibleHeuristicQueue.createQueue(_queueID);
		System.out.println("I am Random Heuristic running on core number : "+queueID);
		hearEvent();
		run();
	}
	
	private void mergeStates(StateP[] listOfStatestoMerge)
	{
		for(StateP node : listOfStatestoMerge)
		{
			if(node == null)
			{
				break;
			}
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
		this._numberOfPermutations = Constants.CommunicationInterval;
		run();
	}

	private void hearEvent()
	{
		StateP start[] = new StateP[1];
		
		Request request1 = MPI.COMM_WORLD.Irecv(start, 0, 1, MPI.OBJECT, 0, Constants.STARTOPERATION);
		request1.Wait();
		this._randomState = start[0];
		nodePriorityQueue.add(_randomState);
		listOfNodesMap.put(nodePriorityQueue.hashCode(), this._randomState);
		
		// Start SMHA*
		isRunning = true;
		run();
		
		int[] sizeArray = new int[1];
		Request request2 = MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
//		request2.Wait();
		
		int size = sizeArray[0];
		
		StateP[] arrayOfStates = new StateP[size];
		Request request3 = MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT, 0, Constants.MERGE);		
//		request3.Wait();
		
		isRunning = false;
		mergeStates(arrayOfStates);
		
		System.out.println("I got executed");
		Boolean[] stop = new Boolean[1];
		Request request4 = MPI.COMM_WORLD.Irecv(stop, 0, 1, MPI.BOOLEAN, 0, Constants.STOP);		
//		request4.Wait();
		isRunning = false;
		System.out.println("Stopped by MASTER");

	}
	
	private void sendStatesForMerging()
	{
		System.out.println("Sending states from slaves");
		StateP[] arrayOfStates = nodePriorityQueue.toArray(new StateP[0]);
		
		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
		Request request2 = MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
//		request2.Wait();
		Request request3 = MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, 0, Constants.MERGE);		

		
	}
	
	private void run()
	{
			while (nodePriorityQueue.isEmpty() == false && isRunning == true) 
			{
				StateP queueHead = nodePriorityQueue.remove();
				listOfExpandedNodesMap.put(queueHead.hashCode(), queueHead);
				StateP queueHeadState = queueHead;

				// If reached goal state
				if (queueHead.equals(goalState)) 
				{
					System.out.println("Path length using A* for QueueID "+_queueID+" is : "
							+ HeuristicSolverUtility.printPathLength(queueHead));
					HeuristicSolverUtility.printState(queueHead);
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
							newNode.setHeuristicCost(RandomHeuristicGenerator.generateRandomHeuristic(_queueID, newNode));
							newNode.setParent(queueHead);
							newNode.setAction(actionOnState);
							nodePriorityQueue.offer(newNode);
						}
					}
				}
//				System.out.println("I am running non stop "+_queueID);
				if(this._numberOfPermutations-- == 0)
				{
					sendStatesForMerging();
				}
//				System.out.println("Continuous Slaves");
			}
	}

}
