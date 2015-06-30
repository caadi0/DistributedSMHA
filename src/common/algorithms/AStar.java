package common.algorithms;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import common.constants.Constants;
import common.impl.Action;
import common.model.StateP;
import common.queues.PQueue;
import common.utility.HeuristicSolverUtility;

public class AStar {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		PrintStream out = new PrintStream(new FileOutputStream("C:\\Users\\AadityaComputer\\Desktop\\output.txt"));
		System.setOut(out);
		
		AStar astar = new AStar();
		astar.solveUsingAStar(HeuristicSolverUtility.createRandom
				(Constants.DIMENSION, Constants.AStarWeight));
	}
	
	HashMap<Integer, StateP> openMap = new HashMap<Integer, StateP>();
	HashMap<Integer, StateP> closedMap = new HashMap<Integer, StateP>();


	/**
	 * @param randomState
	 *            Solves the Problem using A star algorithm
	 */
	public void solveUsingAStar(StateP randomState) {

		if(Constants.debug)
		{
			System.out.println("random State");
			HeuristicSolverUtility.printState(randomState);
		}
		// Generate goal state
		StateP goalState = common.utility.HeuristicSolverUtility.generateGoalState
				(common.constants.Constants.DIMENSION , common.constants.Constants.AStarWeight);
		if(Constants.debug)
		{
			System.out.println("Goal State");
			HeuristicSolverUtility.printState(goalState);
		}

		PriorityQueue<StateP> openQueue = PQueue.createQueue();
		StateP n1 = new StateP(randomState, Constants.AStarWeight);
		n1.setPathCost(0);
		n1.setHeuristicCost((double) ManhattanDistance.calculate(randomState));
		openQueue.add(n1);
		openMap.put(n1.hashCode(), n1);

		while (openQueue.isEmpty() == false) {

			StateP queueHead = openQueue.remove();
			openMap.remove(queueHead.hashCode());
			closedMap.put(queueHead.hashCode(), queueHead);
			StateP queueHeadState = queueHead;
			
			if(Constants.debug)
			{
				System.out.println("Popped State");
				HeuristicSolverUtility.printState(queueHead);
				
			}

			// If reached goal state
			if (queueHead.equals(goalState)) {
				System.out.println(" Moves ");
				HeuristicSolverUtility.printPath(queueHead);
				System.out.println("Path length using A* is : "
						+ HeuristicSolverUtility.printPathLength(queueHead));
				break;
			} else {
				List<Action> listOfPossibleActions = queueHeadState
						.getPossibleActions();
				Iterator<Action> actIter = listOfPossibleActions.iterator();
				while (actIter.hasNext()) {
					Action actionOnState = actIter.next();
					StateP newState = actionOnState.applyTo(queueHeadState);
					newState.setHeuristicCost((double) ManhattanDistance
							.calculate(newState));
					newState.setParent(queueHead);
					newState.setAction(actionOnState);
					if (!closedMap.containsKey(newState.hashCode()) && !openMap.containsKey(newState.hashCode()))  
					{
						if(Constants.debug)
						{
							System.out.println("Newly inserted state into queue is :");
							HeuristicSolverUtility.printState(newState);
						}
						openQueue.offer(newState);
						openMap.put(newState.hashCode(), newState);
					}
					else if(closedMap.containsKey(newState.hashCode()))
					{
//						System.out.println("State found in Closed Map");
//						StateP stateFetchedFromClosedMap = closedMap.get(newState.hashCode());
//						if(stateFetchedFromClosedMap.getKey() > newState.getKey())
//						{
//							closedMap.remove(newState.hashCode());
//							openMap.put(newState.hashCode(), newState);
//							openQueue.add(newState);
//						}
					}
					else if(openMap.containsKey(newState.hashCode()))
					{
						System.out.println("State found in Open Map");
						StateP stateFetchedFromOpenMap = openMap.get(newState.hashCode());
						if(stateFetchedFromOpenMap.getPathCost() > newState.getPathCost())
						{
							openMap.remove(stateFetchedFromOpenMap.hashCode());
							openMap.put(newState.hashCode(), newState);
							openQueue.remove(stateFetchedFromOpenMap);
							openQueue.add(newState);
						}
					}
					
				}
			}
			HeuristicSolverUtility.printAllHeuriticValuesInQueue(openQueue);
//			HeuristicSolverUtility.printAllStatesInQueue(openQueue);
		}
//		HeuristicSolverUtility.printHashMap(closedMap);
//		HeuristicSolverUtility.printHashMap(openMap);
	}

}
