package common.algorithms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import mpi.MPI;
import mpi.Request;

import common.constants.Constants;
import common.impl.Action;
import common.impl.InadmissibleHeuristicQueue;
import common.impl.RandomHeuristicGenerator;
import common.model.StateP;
import common.utility.HeuristicSolverUtility;

public class RandomHeuristic {
	
	Boolean isRunning = false;
	
	private int _sendingInterval;
	private int _listeningInterval;
	private int _queueID;
	private StateP _randomState;
	
	PriorityQueue<StateP> nodePriorityQueue;
	HashMap<Integer, StateP> listOfNodesMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> listOfExpandedNodesMap = new HashMap<Integer, StateP>();
	
	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);
	
	public RandomHeuristic(int queueID, Double bound, int sendingInterval, int listeningInterval)
	{
		this._sendingInterval = sendingInterval;
		this._listeningInterval = listeningInterval;
		this._queueID = queueID;
		nodePriorityQueue = InadmissibleHeuristicQueue.createQueue(_queueID);
		System.out.println("I am Random Heuristic running on core number : "+queueID);
		hearStartEvent();
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
		this._sendingInterval = Constants.CommunicationInterval;
	}
	
	private void hearStartEvent()
	{
		StateP start[] = new StateP[1];
		
		Request request1 = MPI.COMM_WORLD.Irecv(start, 0, 1, MPI.OBJECT, 0, Constants.STARTOPERATION);
		request1.Wait();
		this._randomState = start[0];
		nodePriorityQueue.add(_randomState);
		listOfNodesMap.put(nodePriorityQueue.hashCode(), this._randomState);
		
		System.out.println("Start event heard on Queue ID : "+_queueID);
		isRunning = true;
		run();
	}

	private void hearMergeEvent()
	{
		
		int[] sizeArray = new int[1];
		MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
		
		Integer size = sizeArray[0];
		
		if(size != null && size > 0)
		{
			StateP[] arrayOfStates = new StateP[size];
			MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT, 0, Constants.MERGE);			
			isRunning = false;
			mergeStates(arrayOfStates);
			isRunning = true;
		}

	}
	
	private void sendStatesForMerging()
	{
		System.out.println("Sending states from Queue ID "+this._queueID);
		isRunning = false;
		StateP[] arrayOfStates = nodePriorityQueue.toArray(new StateP[0]);
		
		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
		
		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, 0, Constants.MERGE);
		isRunning = true;
		
	}
	
	private void run()
	{
			while (nodePriorityQueue.isEmpty() == false && isRunning == true) 
			{
				StateP queueHead = nodePriorityQueue.remove();
				listOfExpandedNodesMap.put(queueHead.hashCode(), queueHead);
				StateP queueHeadState = queueHead;

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
				
				if(this._sendingInterval-- == 0)
				{
					sendStatesForMerging();
					System.out.println("Sent states for merging");
					this._sendingInterval = Constants.CommunicationInterval;
				}
				
				if(this._listeningInterval-- == 0)
				{
					hearMergeEvent();
					this._sendingInterval = Constants.CommunicationIntervalForAnchor;
				}
				System.out.println("Queue is Running wild ID "+_queueID);
			}
	}

}
