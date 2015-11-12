package common.constants;

public class Constants 
{
	public static final Double AStarWeight = 10.0;
	
	public static final Boolean debug = true;
	public static final int CommunicationIntervalForAnchorToListen = 100;
	
	public static final Double w1 = 5.0;
	public static final Double w2 = 10.0;
	public static final int NumberOfInadmissibleHeuristicsForSMHAStar = 3;
	public static final Integer DIMENSION = 7;
	
	// Degree of Randomness
	public static final int DegreeOfRandomnessForRandomHeuristics = 60;
	public static final int DegreeOfRandomness = 5000;
	
	// Communication Constants
	public static final Integer STARTOPERATION = 1;
	public static final Integer MERGE = 2;
	public static final Integer RECEIVE = 3;
	public static final Integer UPDATE = 4;
	public static final Integer ENDOPERATION = 5;
	public static final int SIZE = 6;
	public static final int STOP = 7;
	public static final int BOUND = 8;
	
}
