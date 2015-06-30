package common.model;

public interface State 
{
	public Double getHeuristicCost();
	
	public Integer getPathCost();
	
	public State getParent();
	
	public Double getKey();
	
	public void setParent(State parent);
	
	public void setHeuristicCost(Double heuristicCost);
	
	public void setPathCost(Integer pathCost);
}
