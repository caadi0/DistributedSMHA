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
	private int _queueID;
	private StateP _randomState;
	
	public RandomHeuristic(int queueID, Double bound, int numberOfPermutations)
	{
		this.bound = bound;
		this.numberOfPermutations = numberOfPermutations;
		this._queueID = queueID;
		System.out.println("I am Random Heuristic running on core number : "+queueID);
		hearEvent();
	}
	
	private void mergeStates(List<StateP> listOfStatestoMerge)
	{
		
	}

	private void hearEvent()
	{
		System.out.println("Hearing events for Queue ID : "+_queueID);
		if(_queueID == 3) {
			char[] messageToStart = new char[14];
			System.out.println("Bwaahahahahaaaaaaaaaaaa");
			Request request = MPI.COMM_WORLD.Irecv(messageToStart, 0, 14, MPI.CHAR, 0, Constants.STARTOPERATION);
			Status status = request.Wait();
			
			System.out.println("Queue ID : "+_queueID + " "+messageToStart.toString());
			System.out.println("Status " + status);
		}
	}

}
