package common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.impl.Action;
import common.impl.Move;

public class StateP implements State
{
	private StateP _parent;
	private Integer _pathCost = Integer.MAX_VALUE; 
	
	private byte[] allCells;
	private int dimension;
	private int hashCode = -1;
	private Double _weight;
	private Action nextAction;
	private Double _heuristicCost;
	
	public StateP(StateP state, Double weight) 
	{
		this._weight = weight;
		allCells = new byte[state.allCells.length];
		System.arraycopy(state.getAllCells(), 0, allCells, 0, allCells.length);
		dimension = (int) Math.sqrt(allCells.length);
	}

	public StateP(byte[] cells , Double weight) 
	{
		this._weight = weight;
		allCells = new byte[cells.length];
		System.arraycopy(cells, 0, allCells, 0, cells.length);
		dimension = (int) Math.sqrt(cells.length);
	}

	@Override
	public Double getHeuristicCost() 
	{
		return this._heuristicCost;
	}
	
	@Override
	public void setHeuristicCost(Double heuristicCost) 
	{
		this._heuristicCost = heuristicCost;
	}

	@Override
	public Integer getPathCost() 
	{
		return _pathCost;
	}

	@Override
	public StateP getParent() 
	{
		return this._parent;
	}

	@Override
	public Double getKey() 
	{
		return getPathCost() + this._weight * getHeuristicCost();
	}

	@Override
	public void setParent(State parent) 
	{
		this._parent = (StateP) parent;
		this._pathCost = this._parent.getPathCost() + 1;
	}
	
	public Double getWeight()
	{
		return this._weight;
	}
	
	private HoleCellLocation getEmptyCellLocation() 
	{
		for (int i = 0; i < allCells.length; i++) 
		{
			if (allCells[i] == 0) 
			{
				return new HoleCellLocation(i/dimension, i % dimension);
			}
		}
		throw new RuntimeException("No Empty cell found");
	}
	
	public byte getCellValue(HoleCellLocation cell) 
	{
		return getCellValue(cell.rowIndex, cell.columnIndex);
	}
	
	public byte getCellValue(int rowIndex, int columnIndex)
	{
		return allCells[rowIndex * dimension + columnIndex];
	}
	
	public byte[] getAllCells() 
	{
		return allCells;
	}
	
	public List<common.impl.Action> getPossibleActions() 
	{
		List<common.impl.Action> actions = new ArrayList<common.impl.Action>();

		HoleCellLocation emptyCell = getEmptyCellLocation();

		if (emptyCell.getRowIndex() > 0) 
		{
			HoleCellLocation upCell = new HoleCellLocation(emptyCell.getRowIndex() - 1,
					emptyCell.getColumnIndex());
			actions.add(new Action(upCell, Move.DOWN));
		}

		if (emptyCell.getRowIndex() < dimension - 1) 
		{
			HoleCellLocation upCell = new HoleCellLocation(emptyCell.getRowIndex() + 1,
					emptyCell.getColumnIndex());
			actions.add(new Action(upCell, Move.UP));
		}

		if (emptyCell.getColumnIndex() > 0) 
		{
			HoleCellLocation upCell = new HoleCellLocation(emptyCell.getRowIndex(),
					emptyCell.getColumnIndex() - 1);
			actions.add(new Action(upCell, Move.RIGHT));
		}

		if (emptyCell.getColumnIndex() < dimension - 1) 
		{
			HoleCellLocation upCell = new HoleCellLocation(emptyCell.getRowIndex(),
					emptyCell.getColumnIndex() + 1);
			actions.add(new Action(upCell, Move.LEFT));
		}

		return actions;
	}
	
	public void setCellValue(HoleCellLocation cell, byte value) 
	{
		allCells[cell.getRowIndex() * dimension + cell.getColumnIndex()] = value;
		reset();
	}
	
	private void reset()
	{
		hashCode = -1;
	}
	
	public Action getAction() 
	{
		return nextAction;
	}
	
	public void setAction(Action next)
	{
		this.nextAction = next;
	}
	
	@Override
	public int hashCode() 
	{
		hashCode = Arrays.hashCode(allCells);		
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StateP) {
			StateP s2 = (StateP) obj;
			return Arrays.equals(allCells, s2.allCells);
		}

		return false;
	}
	
	public static class HoleCellLocation 
	{

		private int rowIndex;
		private int columnIndex;

		public HoleCellLocation(int rowIndex, int columnIndex) 
		{
			this.rowIndex = rowIndex;
			this.columnIndex = columnIndex;
		}

		public int getRowIndex() 
		{
			return rowIndex;
		}

		public int getColumnIndex() 
		{
			return columnIndex;
		}
	}
	
	public int getDimension() 
	{
		return dimension;
	}

	@Override
	public void setPathCost(Integer pathCost) 
	{
		this._pathCost = pathCost;
	}

}
