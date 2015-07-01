package common.algorithms;

import java.util.List;

import mpi.MPI;
import mpi.Request;
import mpi.Status;

import common.constants.Constants;
import common.model.StateP;

public class RandomHeuristic {
	
	private Double bound;
	private int numberOfPermutations;
	private int queueID;
	private StateP _randomState;
	
	public RandomHeuristic(int queueID, Double bound, int numberOfPermutations)
	{
		this.bound = bound;
		this.numberOfPermutations = numberOfPermutations;
		System.out.println("I am Random Heuristic running on core number : "+queueID);
//		hearEvent();
	}
	
	private void mergeStates(List<StateP> listOfStatestoMerge)
	{
		
	}

	private void hearEvent()
	{
		if(queueID == 3) {
			String messageToStart = "";
			Request request = MPI.COMM_WORLD.Irecv(messageToStart, 0, 2000, MPI.OBJECT, 1, Constants.STARTOPERATION);
			Status status = request.Wait();
			System.out.println("Queue ID : "+queueID + " "+messageToStart);
			System.out.println("Status " + status);
		}
	}

}
