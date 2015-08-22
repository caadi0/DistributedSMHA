package common.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import common.constants.Constants;
import common.impl.Action;
import common.model.StateP;

public class HeuristicSolverUtility 
{
	public static void printPath(StateP state) 
	{
		if(state.getParent() != null) 
		{
			printPath(state.getParent());
			// Print Action Taken
			System.out.println("Tile "+state.getAction().getMove());
		}
		printState(state);
	}
	
	public static Integer printPathLength(StateP state) 
	{
		if(state.getParent() != null) 
		{
			return 1 + printPathLength(state.getParent());
		}
		// Source Node Path Length = 0
		return 0;
	}
	
	/**
	 * @param state
	 * Prints state cells
	 */
	public static void printState(StateP state) 
	{
		int counter = 0;
		byte[] b = state.getAllCells();
		for(byte bt : b) 
		{
			System.out.print(String.format("%1$3s", bt));
			if((++counter)%Constants.DIMENSION == 0)
				System.out.println(" ");
		}
		System.out.println(" ");
		System.out.println(" ");
	}
	
	/**
	 * @param dimension
	 * @return generates random state of dimension * dimension
	 */
	public static StateP createRandom(int dimension , Double weight) 
	{
		StateP s = new StateP(generateGoalState(dimension , weight).getAllCells() , weight);
		Action old = null;
		
		for (int i = 0; i < Constants.DegreeOfRandomness; i++) 
		{
			List<Action> actions = s.getPossibleActions();
			// pick an action randomly
			Random random = new Random();
			int index = random.nextInt(actions.size());
			Action a = actions.get(index);
			if (old != null && old.isInverse(a))
			{
				if (index == 0)
				{
					index = 1;
				}
				else
				{
					index--;
				}
				a = actions.get(index);
			}
			s = a.applyTo(s);
			old = a;
		}
		return s;
	}
	
	/**
	 * @param dimension
	 * @return returns goal state of { dimension * dimension }
	 * eg. if dimension = 3 <p>
	 * Goal state : <p> 1 2 3 <p>
	 * 				    4 5 6 <p>
	 * 				    7 8 0 
	 */
	public static StateP generateGoalState(int dimension , Double weight)
	{		
		int nbrOfCells = dimension * dimension;
		byte[] goalCells = new byte[nbrOfCells];
		for (byte i = 1; i < goalCells.length; i++) 
		{
			goalCells[i - 1] = i;
		}
		goalCells[nbrOfCells - 1] = 0;
		
		return new StateP(goalCells, weight);
	}
	
	public static void printHashMap(HashMap<Integer, StateP> mapH)
	{
		Map<Integer, StateP> map = mapH;
		List<Integer> keys = new ArrayList(map.keySet());
		for (Integer key: keys) {
		    System.out.println(key + ": " + map.get(key));
		}
	}
	

	public static void printAllStatesInQueue(PriorityQueue<StateP> q) 
	{
		Iterator<StateP> qIter = q.iterator();
		System.out.print("States present in Queue  :   ");
		while(qIter.hasNext()) 
		{
			StateP n = qIter.next();
			printState(n);
		}
		System.out.println("");
	}
	
	public static void printHashCodeInQueue(PriorityQueue<StateP> q) 
	{
		Iterator<StateP> qIter = q.iterator();
		System.out.print("Hashcodes present in Queue  :   ");
		while(qIter.hasNext()) 
		{
			StateP n = qIter.next();
			System.out.print(n.hashCode() + " ");
		}
		System.out.println("");
	}
	
	public static void printAllCostsInQueue(PriorityQueue<StateP> q) 
	{
		Iterator<StateP> qIter = q.iterator();
		System.out.print("Cost values present in Queue  :   ");
		while(qIter.hasNext()) 
		{
			StateP n = qIter.next();
			System.out.print(n.getHeuristicCost() + " : " +n.hashCode() +" ; ");
		}
		System.out.println("");
	}
	
	public static Boolean isStateSolvable(StateP s)
	{
		int parity = 0;
		byte[] arr = s.getAllCells();
		for(int i = 0; i<arr.length;i++)
		{
			for(int j=i+1;j<arr.length;j++)
			{
				if(arr[i]>arr[j])
					parity++;
			}
		}
		if(parity%2 == 0)
			return true;
		return false;
	}
}
