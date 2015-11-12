package common.algorithms;

import java.util.Arrays;
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
import common.utility.HeuristicSolverUtility;

public class RandomHeuristic {
	
	private int _queueID;
	private StateP _randomState;
	
	Request reqH;
	int[] sizeArray = new int[1];
	
	PriorityQueue<StateP> nodePriorityQueue;
	PriorityQueue<StateP> statesExpandedInLastIterationQueue;
	HashMap<Integer, StateP> listOfNodesMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> listOfExpandedNodesMap = new HashMap<Integer, StateP>();
	
	StateP goalState = HeuristicSolverUtility.generateGoalState(Constants.DIMENSION , Constants.w1);
	
	public RandomHeuristic(int queueID)
	{
		this._queueID = queueID;
		nodePriorityQueue = InadmissibleHeuristicQueue.createQueue();
		statesExpandedInLastIterationQueue = InadmissibleHeuristicQueue.createQueue();
		System.out.println("I am Random Heuristic running on core number : "+queueID);
		hearStartEvent();
	}
	
	private void mergeStates(StateP[] listOfStatestoMerge)
	{
		for(StateP node : listOfStatestoMerge)
		{
			
//			System.out.println("This is running Shipra " + ++i + "times in queue "+_queueID);
			if(node == null)
			{
				System.out.println("I am exiting because I am NULL");
				break;
			}
			StateP existingNode = listOfNodesMap.get(node.hashCode());
			if(existingNode != null)
			{
				if(existingNode.getPathCost() <= node.getPathCost())
				{
					// Nothing to do here
				}
				else
				{
//					System.out.println("Improving state from "+existingNode.getPathCost()+" to "+node.getPathCost());
					nodePriorityQueue.remove(existingNode);
					existingNode.setPathCost(node.getPathCost());
					existingNode.setParent(node.getParent());
					nodePriorityQueue.add(existingNode);
				}
			}
			else
			{
				node.setHeuristicCost(RandomHeuristicGenerator.generateRandomHeuristic(_queueID, node));
				nodePriorityQueue.add(node);
//				statesExpandedInLastIterationQueue.add(node);
				listOfNodesMap.put(node.hashCode(), node);
			}
			StateP parentNode = node.getParent();
			if(parentNode != null) {
				if(listOfNodesMap.containsKey(parentNode.hashCode())) {
					nodePriorityQueue.remove(parentNode);
					listOfNodesMap.remove(parentNode.hashCode());
					statesExpandedInLastIterationQueue.remove(parentNode);
				} else {
					// Maybe its yet to be added
				}
			} else {
				// THis would be the case of Anchor node
			}
		}
	}
	
	private void hearStartEvent()
	{
		StateP start[] = new StateP[1];
		
		Request request1 = MPI.COMM_WORLD.Irecv(start, 0, 1, MPI.OBJECT, 0, Constants.STARTOPERATION);
		request1.Wait();
		this._randomState = start[0];
		nodePriorityQueue.add(_randomState);
		statesExpandedInLastIterationQueue.add(_randomState);
		listOfNodesMap.put(_randomState.hashCode(), this._randomState);
		
		Double[] bound = new Double[1];
		Request request2 = MPI.COMM_WORLD.Irecv(bound, 0, 1, MPI.OBJECT, 0, Constants.BOUND);
		request2.Wait();
		
		System.out.println("Started Queue ID : "+_queueID);
		run();
	}

	private void hearMergeEvent()
	{
		
		if(reqH == null)
		{
			reqH = MPI.COMM_WORLD.Irecv(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
		}
		
		Status status = reqH.Test();
		if(status == null)
		{
			System.out.println("STATUS is NULL");
			return;
		}
		else
		{
			System.out.println("Incoming MESSAGE in random queue "+_queueID);
			reqH.Wait();
			reqH = null;
		}
			
		Integer size = sizeArray[0];

		System.out.println("Size received by random "+size);
		if(size != null && size > 0)
		{
			StateP[] arrayOfStates = new StateP[size];
			MPI.COMM_WORLD.Irecv(arrayOfStates, 0, size, MPI.OBJECT, 0, Constants.MERGE).Wait();
			mergeStates(arrayOfStates);
			Arrays.fill(arrayOfStates, null);
		}

	}
	
	private void sendStatesForMerging()
	{
		System.out.println("Sending states from Queue ID "+this._queueID);
		StateP[] arrayOfStates = statesExpandedInLastIterationQueue.toArray(new StateP[0]);
		
		int[] sizeArray = new int[1];
		sizeArray[0] = arrayOfStates.length;
		System.out.println("Broadcasted length from Queue ID "+this._queueID+" is "+sizeArray[0]);
		MPI.COMM_WORLD.Isend(sizeArray, 0, 1, MPI.INT, 0, Constants.SIZE);
		
		MPI.COMM_WORLD.Isend(arrayOfStates, 0, arrayOfStates.length, MPI.OBJECT, 0, Constants.MERGE);
		System.out.println("Do I get executed");
		statesExpandedInLastIterationQueue.clear();
		
	}
	
	private void run()
	{
			while (nodePriorityQueue.isEmpty() == false ) 
			{
				StateP queueHead = nodePriorityQueue.remove();
				System.out.println("Removed Value in Random Queue "+_queueID + " "+ queueHead.getPathCost() + " : "+queueHead.getHeuristicCost() + " ; ");
				listOfNodesMap.remove(queueHead.hashCode());
				if(statesExpandedInLastIterationQueue.contains(queueHead)) {
					statesExpandedInLastIterationQueue.remove(queueHead);
				}
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
						if (!listOfExpandedNodesMap.containsKey(newState.hashCode())) {
							newState.setHeuristicCost(RandomHeuristicGenerator.generateRandomHeuristic(_queueID, newState));
							newState.setParent(queueHead);
							newState.setAction(actionOnState);
							
							
							if(!listOfNodesMap.containsKey(newState.hashCode())) {
								nodePriorityQueue.offer(newState);
								
								System.out.println("Added Value in Random Queue "+_queueID + "  " +newState.getPathCost() + " : "+newState.getHeuristicCost() + " ; ");
								listOfNodesMap.put(newState.hashCode(), newState);
								statesExpandedInLastIterationQueue.offer(newState);
							} else {
								StateP existingNode = listOfNodesMap.get(newState.hashCode());
					
								if(existingNode.getPathCost() < newState.getPathCost()) {
									// Do nothing
								} else {
									nodePriorityQueue.remove(existingNode);
									existingNode.setPathCost(newState.getPathCost());
									existingNode.setParent(newState.getParent());
									existingNode.setAction(actionOnState);
									nodePriorityQueue.add(existingNode);
								}
								
								
							}
						}
					}
				}
			}
			// Apply bound check here
//			if(bound has exceeded) {
//				sendStatesForMerging();
//				hearMergeEvent();
//			}
	}

}
