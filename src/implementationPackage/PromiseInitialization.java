package implementationPackage;

import org.moeaframework.core.Initialization;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;

import implementationPackage.PromiseGeneticAlgorithm.PromiseClass;

public class PromiseInitialization implements Initialization {
	
	private PromiseClass problem;
	
	private int size;
	
	public PromiseInitialization(PromiseClass prob, int size) {
		super();
		this.problem = prob;
		this.size = size;
	}

	@Override
	public Solution[] initialize() {
		Solution [] result = new Solution[size];
		
		for (int i = 0; i < this.size; i++) {
			Solution curSolution = problem.newSolution();
			
			BinaryVariable b = (BinaryVariable) curSolution.getVariable(0);
			b.randomize();
			
			RealVariable r1 = (RealVariable) curSolution.getVariable(1);
			r1.setValue(0.25);
			RealVariable r2 = (RealVariable) curSolution.getVariable(2);
			r2.setValue(0.25);
			RealVariable r3 = (RealVariable) curSolution.getVariable(3);
			r3.setValue(0.25);
			RealVariable r4 = (RealVariable) curSolution.getVariable(4);
			r4.setValue(0.25);
			
			result[i] = curSolution;
			
		}
		
		return result;
	}
	
}
