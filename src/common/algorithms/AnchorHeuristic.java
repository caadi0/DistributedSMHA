package common.algorithms;

import java.util.Arrays;
import java.util.PriorityQueue;

import common.constants.Constants;
import common.model.StateP;
import common.queues.PQueue;
import common.utility.HeuristicSolverUtility;

import mpi.MPI;
import mpi.Request;
import mpi.Status;

public class AnchorHeuristic 
{

	PriorityQueue<StateP> anchorPriorityQueue = PQueue.createQueue();

	StateP goalState = HeuristicSolverUtility.generateGoalState(
			Constants.DIMENSION, Constants.w1);
	int[] sizeArray = new int[1];
	
	Request reqH;

	public AnchorHeuristic() {
		
		StateP initialRandomState = HeuristicSolverUtility.createRandom(
				Constants.DIMENSION, Constants.w1);

		initialRandomState.setPathCost(0);
		initialRandomState.setHeuristicCost((double) ManhattanDistance.calculate(initialRandomState));

		// Adding initial state to the list
		anchorPriorityQueue.add(initialRandomState);
		
		System.out.println("goal state is: "+initialRandomState.getPathCost() + " "+initialRandomState.getHeuristicCost());

		startAllChildren(initialRandomState, Constants.w2 * initialRandomState.getKey());
		run();

	}
	
	private void startAllChildren(StateP randomState , Double bound) {
		for (int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar; i++) {
			startChild(randomState , i, bound);
		}
	}

	private static void startChild(StateP randomState, int queueID, Double bound) {
		StateP[] start = new StateP[1];
		start[0] = randomState;
		MPI.COMM_WORLD.Isend(start, 0, 1, MPI.OBJECT, queueID,
				Constants.STARTOPERATION).Wait();
		
		double[] boundArray = new double[1];
		boundArray[0] = bound; 
		MPI.COMM_WORLD.Isend(boundArray, 0, 1, MPI.DOUBLE, queueID,
				Constants.BOUND).Wait();
		
	}

	public void run() 
	{
		System.out.println("Running Anchor Queue");
		while (true) {			
			for(int i = 1; i <= Constants.NumberOfInadmissibleHeuristicsForSMHAStar ; i++)
			{
//				System.out.println("Anchor Trying to listen");
//				hearMergeEvent(i);
			}
		}
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

	private void merge(StateP[] listOfReceivedNodes) {
		for (StateP node : listOfReceivedNodes) {
			if (node == null) {
				break;
			}
			
		}
	}
}
