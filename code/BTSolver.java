import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class BTSolver
{

	// =================================================================
	// Properties
	// =================================================================

	private ConstraintNetwork network;
	private SudokuBoard sudokuGrid;
	private Trail trail;

	private boolean hasSolution = false;

	public String varHeuristics;
	public String valHeuristics;
	public String cChecks;

	// =================================================================
	// Constructors
	// =================================================================

	public BTSolver ( SudokuBoard sboard, Trail trail, String val_sh, String var_sh, String cc )
	{
		this.network    = new ConstraintNetwork( sboard );
		this.sudokuGrid = sboard;
		this.trail      = trail;

		varHeuristics = var_sh;
		valHeuristics = val_sh;
		cChecks       = cc;
	}

	// =================================================================
	// Consistency Checks
	// =================================================================

	// Basic consistency check, no propagation done
	private boolean assignmentsCheck ( )
	{
		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				return false;

		return true;
	}

	// =================================================================
	// Arc Consistency
	// =================================================================
	public boolean arcConsistency ( )
    {
        List<Variable> toAssign = new ArrayList<Variable>();
        List<Constraint> RMC = network.getModifiedConstraints();
        for(int i = 0; i < RMC.size(); ++i)
        {
            List<Variable> LV = RMC.get(i).vars;
            for(int j = 0; j < LV.size(); ++j)
            {
                if(LV.get(j).isAssigned())
                {
                    List<Variable> Neighbors = network.getNeighborsOfVariable(LV.get(j));
                    int assignedValue = LV.get(j).getAssignment();
                    for(int k = 0; k < Neighbors.size(); ++k)
                    {
                        Domain D = Neighbors.get(k).getDomain();
                        if(D.contains(assignedValue))
                        {
                            if(D.size() == 1)
                                return false;
                            if(D.size() == 2)
                                toAssign.add(Neighbors.get(k));
                            trail.push(Neighbors.get(k));
                            Neighbors.get(k).removeValueFromDomain(assignedValue);
                        }
                    }
                }
            }
        }
        if(!toAssign.isEmpty())
        {
            for(int i = 0; i < toAssign.size(); ++i)
            {
                Domain D = toAssign.get(i).getDomain();
                ArrayList<Integer> assign = D.getValues();
                trail.push(toAssign.get(i));
                toAssign.get(i).assignValue(assign.get(0));
            }
            return arcConsistency();
        }
        return network.isConsistent();
    }


	/**
	 * Part 1 TODO: Implement the Forward Checking Heuristic
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 *
	 * Return: a pair of a HashMap and a Boolean. The map contains the pointers to all MODIFIED variables, mapped to their MODIFIED domain. 
	 *         The Boolean is true if assignment is consistent, false otherwise.
	 */
	public Map.Entry<HashMap<Variable,Domain>, Boolean> forwardChecking ( )
	{
		HashMap<Variable, Domain> modifiedVariableDomains = new HashMap<Variable, Domain>();
		HashMap<Variable, Integer> assignedVariables = new HashMap<Variable, Integer>();
		Boolean isConsistent = forwardCheckPropogation(modifiedVariableDomains, assignedVariables);
		return Pair.of(modifiedVariableDomains, isConsistent);
	}

	public Boolean forwardCheckPropogation(Map<Variable, Domain> modifiedVariables, Map<Variable, Integer> assignedVariables) {

		List<Constraint> modifiedConstraints = network.getModifiedConstraints();
		for (Constraint modifiedConstraint: modifiedConstraints) {
			// find assigned Variables
			List<Integer> assignedValues = new ArrayList<Integer>();
			for (Variable variable: modifiedConstraint.vars) {
				if (variable.isAssigned()) {
					assignedValues.add(variable.getAssignment());	
				}
			}
			// update unassigned
			for (Variable variable: modifiedConstraint.vars) {
				if (!variable.isAssigned()) {
					Boolean isVariableModified = false;
					List<Integer> domainValues = variable.getValues();
					if (modifiedVariables.containsKey(variable)) {
						isVariableModified = true;
					}
					for (Integer value: assignedValues) {
						if (domainValues.contains(value)) {
							// domain contains value that needs to be removed
							if (!isVariableModified) { // add and not present in modifiedVariablesMap
								trail.push(variable);
								isVariableModified = true;
							}

							variable.removeValueFromDomain(value);
							if (variable.size()==0) {
								variable.unassign();
								return false;
							} else if (variable.size()==1) {
								Integer assignedValue = variable.getValues().get(0);
								variable.assignValue(assignedValue);
								assignedVariables.put(variable, assignedValue);
							}
						}
					}
					if (isVariableModified) {
						modifiedVariables.put(variable, variable.getDomain());
					}
				}
			}
		}
		return true;
	}

	/**
	 * Part 2 TODO: Implement both of Norvig's Heuristics
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * (2) If a constraint has only one possible place for a value
	 *     then put the value there.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: a pair of a map and a Boolean. The map contains the pointers to all variables that were assigned during the whole 
	 *         NorvigCheck propagation, and mapped to the values that they were assigned. 
	 *         The Boolean is true if assignment is consistent, false otherwise.
	 */
	public Map.Entry<HashMap<Variable,Integer>,Boolean> norvigCheck ( )
	{
        HashMap<Variable, Domain> modifiedVariableDomains = new HashMap<Variable, Domain>();
		HashMap<Variable, Integer> assignedVariables = new HashMap<Variable, Integer>();

		Boolean isConsistent = norvigCheckPropogation(modifiedVariableDomains, assignedVariables);

		return Pair.of(assignedVariables, isConsistent);
	}

	public Boolean norvigCheckPropogation(Map<Variable, Domain> modifiedVariables, Map<Variable, Integer> assignedVariables) {

		// First perform Forward Checking Propogation
		Boolean isConsistent = forwardCheckPropogation(modifiedVariables, assignedVariables);
		if (!isConsistent) {
			return false;
		}
			
		Boolean newVariableAssigned = false;
		List<Constraint> allConstraints = network.getConstraints();
		for (Constraint constraint: allConstraints) {

			// first tabuate assigned values
			// generate values count map and value one variable map
			Set<Integer> assignedDomainValue = new HashSet<>();
			Map<Integer, Integer> domainValueCountMap = new HashMap<>();
			Map<Integer, Variable> anyVariableWithDomainValue = new HashMap<>();

			int totalDomainValues = sudokuGrid.getN();
			for (int i=1; i<=totalDomainValues; i++) {
				domainValueCountMap.put(i, 0);
			}
			for (Variable v:constraint.vars) {
				if (v.isAssigned()) {
					assignedDomainValue.add(v.getAssignment());
				} else {
					for (Integer domainValue: v) {
						if (domainValueCountMap.get(domainValue)==0) {
							anyVariableWithDomainValue.put(domainValue, v);
						}
						domainValueCountMap.put(domainValue, domainValueCountMap.get(domainValue) + 1);
					}
				}
			}
			
			// iterate through all domain_values and check if some unassigned value has 0 count map then fail
			// if some value has 1 count map then pick the variable from other map and assign
			for (Map.Entry<Integer, Integer> domainCountPair : domainValueCountMap.entrySet()) {

				Integer domainVal = domainCountPair.getKey();
				Integer domainValCount = domainCountPair.getValue();
				if (domainValCount == 0 && !assignedDomainValue.contains(domainVal)) {
					isConsistent = false;
				} else if (domainValCount == 1) {

					newVariableAssigned = true;
					Variable assignmentVar = anyVariableWithDomainValue.get(domainVal);
					if (!modifiedVariables.containsKey(assignmentVar)) {
						trail.push(assignmentVar);
					}
					assignmentVar.assignValue(domainVal); 	// assignment made
					assignedVariables.put(assignmentVar, domainVal);

					List<Variable> neighbors = network.getNeighborsOfVariable(assignmentVar);
					for (Variable neighbour:neighbors) {
						if (!neighbour.isAssigned() && neighbour.getValues().contains(domainVal)) {
							if (!modifiedVariables.containsKey(neighbour)) {
								trail.push(neighbour);
							}
							neighbour.removeValueFromDomain(domainVal);
						}
					}
				}
			}
		}

		// recursive call
		if (newVariableAssigned) {
			//isConsistent &= norvigCheckPropogation(modifiedVariables, assignedVariables);
		}
		return isConsistent;
	}

	/**
	 * Optional TODO: Implement your own advanced Constraint Propagation
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private boolean getTournCC ( )
	{
		return forwardChecking().getValue();
	}

	// =================================================================
	// Variable Selectors
	// =================================================================

	// Basic variable selector, returns first unassigned variable
	private Variable getfirstUnassignedVariable()
	{
		for ( Variable v : network.getVariables() )
			if ( ! v.isAssigned() )
				return v;

		// Everything is assigned
		return null;
	}

	/**
	 * Part 1 TODO: Implement the Minimum Remaining Value Heuristic
	 *
	 * Return: The unassigned variable with the smallest domain
	 */
	public Variable getMRV ( )
	{
        int minimumDomainSize = sudokuGrid.getN()+1; // Domain size will always be greater than this value
		Variable minDomainVariable = null;			//  Returns null if all variables have been assigned
		// iterate through all variables, for unassigned variables, calculate variable with minimum degree
		for (Variable v: network.getVariables() ) {
			if (!v.isAssigned()) {
				if (v.size() < minimumDomainSize) {
					minimumDomainSize = v.size();
					minDomainVariable = v;
				}
			}
		}
		return minDomainVariable;
	}

	/**
	 * Part 2 TODO: Implement the Minimum Remaining Value Heuristic
	 *                with Degree Heuristic as a Tie Breaker
	 *
	 * Return: The unassigned variable with the smallest domain and affecting the most unassigned neighbors.
	 *         If there are multiple variables that have the same smallest domain with the same number 
	 *         of unassigned neighbors, add them to the list of Variables.
	 *         If there is only one variable, return the list of size 1 containing that variable.
	 */
	public List<Variable> MRVwithTieBreaker ( )
	{
        Integer minimumDomainSize = sudokuGrid.getN() + 1;
		Integer maxDegree = 0;
		List<Variable> minDomainMaxDegreeVariables = new ArrayList<>();
		
		for (Variable v: network.getVariables()) {
			if (!v.isAssigned()) {
				if (v.size()<minimumDomainSize) {
					maxDegree=0;
					minimumDomainSize = v.size();
				} 
				if (v.size()==minimumDomainSize) {
					List<Variable> neighbours = network.getNeighborsOfVariable(v);
					Integer degree = 0;
					for (Variable neighbour: neighbours) {
						if (!neighbour.isAssigned()) {
							degree++;
						}
					}
					if (degree>maxDegree) {
						minDomainMaxDegreeVariables.clear();
						maxDegree = degree;
					}
					if (degree==maxDegree) {
						minDomainMaxDegreeVariables.add(v);
					}
				}

			}
		}

		if (minDomainMaxDegreeVariables.isEmpty()){
			minDomainMaxDegreeVariables.add(null);
		}
        return minDomainMaxDegreeVariables;
    }

	/**
	 * Optional TODO: Implement your own advanced Variable Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private Variable getTournVar ( )
	{
		return MRVwithTieBreaker().get(0);
	}

	// =================================================================
	// Value Selectors
	// =================================================================

	// Default Value Ordering
	public List<Integer> getValuesInOrder ( Variable v )
	{
		List<Integer> values = v.getDomain().getValues();

		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}

	/**
	 * Part 1 TODO: Implement the Least Constraining Value Heuristic
	 *
	 * The Least constraining value is the one that will knock the least
	 * values out of it's neighbors domain.
	 *
	 * Return: A list of v's domain sorted by the LCV heuristic
	 *         The LCV is first and the MCV is last
	 */
	public List<Integer> getValuesLCVOrder ( Variable v )
	{
        if (v.isAssigned()) {
			return null;
		}

		List<Variable> neighbors = network.getNeighborsOfVariable(v);
		Map<Integer, Integer> domainFrequencyCountMap = new HashMap<Integer, Integer>();
		//put keys in hash table
		for (Integer domainVal: v) {
			domainFrequencyCountMap.put(domainVal, 0);
		}
		//iterate through all neighbouring domain
		for (Variable neighbor: neighbors) {
			for (Integer neighborDomainVal: neighbor) {
				if (domainFrequencyCountMap.containsKey(neighborDomainVal)) {
					domainFrequencyCountMap.replace(neighborDomainVal, domainFrequencyCountMap.get(neighborDomainVal)+1);
				}
			}
		}

		// generate sorted domain key val pairs from Map
		List<Map.Entry<Integer, Integer> > domainFrequencyCountList = new LinkedList<Map.Entry<Integer, Integer> >(domainFrequencyCountMap.entrySet());
		Collections.sort(domainFrequencyCountList, new Comparator<Map.Entry<Integer, Integer> > () {
			public int compare (Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
				if (e1.getValue()==e2.getValue()) {
					return e1.getKey().compareTo(e2.getKey());
				}
				return e1.getValue().compareTo(e2.getValue());
			}
		});

		// extract keys into a new list from sorted keyValue pairs
		List<Integer> result = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> keyValPair: domainFrequencyCountList) {
			result.add(keyValPair.getKey());
		}
		return result;
	}

	/**
	 * Optional TODO: Implement your own advanced Value Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	public List<Integer> getTournVal ( Variable v )
	{
		return getValuesLCVOrder(v);
	}

	//==================================================================
	// Engine Functions
	//==================================================================

	public int solve (float time_left)
	{
		if(time_left <= 60.0) {
			return -1;
		}
		long startTime = System.nanoTime();
		if ( hasSolution )
			return 0;

		// Variable Selection
		Variable v = selectNextVariable();

		if ( v == null )
		{
			for ( Variable var : network.getVariables() )
			{
				// If all variables haven't been assigned
				if ( ! var.isAssigned() )
				{
					return 0;
				}
			}

			// Success
			hasSolution = true;
			return 0;
		}

		// Attempt to assign a value
		for ( Integer i : getNextValues( v ) )
		{
			// Store place in trail and push variable's state on trail
			trail.placeTrailMarker();
			trail.push( v );

			// Assign the value
			v.assignValue( i );

			// Propagate constraints, check consistency, recurse
			if ( checkConsistency() ) {
				long endTime = System.nanoTime();
                long elapsedTime = (endTime - startTime);
                float elapsedSecs = ((float)(endTime - startTime)) / 1000000000;
                float new_start_time = time_left - elapsedSecs;
				int check_status = solve(new_start_time);
				if(check_status == -1) {
				    return -1;
				}
			}

			// If this assignment succeeded, return
			if ( hasSolution )
				return 0;

			// Otherwise backtrack
			trail.undo();
		}
		return 0;
	}

	public boolean checkConsistency ( )
	{
		switch ( cChecks )
		{
			case "forwardChecking":
				return forwardChecking().getValue();

			case "norvigCheck":
				return norvigCheck().getValue();

			case "tournCC":
				return getTournCC();

			default:
				return assignmentsCheck();
		}
	}

	public Variable selectNextVariable ( )
	{
		switch ( varHeuristics )
		{
			case "MinimumRemainingValue":
				return getMRV();

			case "MRVwithTieBreaker":
				return MRVwithTieBreaker().get(0);

			case "tournVar":
				return getTournVar();

			default:
				return getfirstUnassignedVariable();
		}
	}

	public List<Integer> getNextValues ( Variable v )
	{
		switch ( valHeuristics )
		{
			case "LeastConstrainingValue":
				return getValuesLCVOrder( v );

			case "tournVal":
				return getTournVal( v );

			default:
				return getValuesInOrder( v );
		}
	}

	public boolean hasSolution ( )
	{
		return hasSolution;
	}

	public SudokuBoard getSolution ( )
	{
		return network.toSudokuBoard ( sudokuGrid.getP(), sudokuGrid.getQ() );
	}

	public ConstraintNetwork getNetwork ( )
	{
		return network;
	}
}
