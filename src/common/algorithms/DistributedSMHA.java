package common.algorithms;

import common.constants.Constants;

import mpi.MPI;

public class DistributedSMHA {

	public static void main(String[] args) {
		MPI.Init(args);
		int me = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		System.out.println("Total number of processes started are : "+size);
		System.out.println("Hi from <"+me+">");
		if(me == 0)
		{
			new AnchorHeuristic();
		}
		else 
		{
			new RandomHeuristic(me, 0.0, Constants.CommunicationInterval);
		}
	}	
}
