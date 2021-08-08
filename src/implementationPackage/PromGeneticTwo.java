package implementationPackage;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import com.fasterxml.jackson.databind.ObjectMapper;

import implementationPackage.PromiseGeneticAlgorithm.PromiseClass;

public class PromGeneticTwo {
	public static class PromiseClassTwo extends AbstractProblem {
		
		int numberOfReviewers;
		
		String [] reviewerKeys;
		
		ArrayList<double []> reviewerMetricsList;
		

		public PromiseClassTwo(int numberOfVariables, LinkedHashMap<String, ArrayList> metricsMap) {
			super(numberOfVariables, 2);
			
			numberOfReviewers = metricsMap.keySet().size();
			
			
			ArrayList<double []> list = new ArrayList<>();
			
			String [] keyList = new String[metricsMap.keySet().size()];
			
			int curPosition = 0;
			for (String innerKey : metricsMap.keySet()) {
				keyList[curPosition] = innerKey;
				curPosition+=1;
				double [] metricsForList = {0.0,0.0,0.0,0.0,0.0};
				
				ArrayList actualMetrics = metricsMap.get(innerKey);
				
				int x = 0;
				for (Object met : actualMetrics) {
					if (met instanceof Double) {
						metricsForList[x] = ((Double) met).doubleValue();
					}
					else if(met instanceof Integer){
						Integer m = (Integer) met;
						double val = m.intValue();
						metricsForList[x] = (double) val;
					}
					x+=1;
				}
				list.add(metricsForList);
			}
			reviewerKeys = keyList;
			reviewerMetricsList = list;
		}

		@Override
		public void evaluate(Solution solution) {
			boolean[] d = EncodingUtils.getBinary(solution.getVariable(0));
			
			double f1 = 0.0;
			double f2 = 0.0;
			
			int total_k = 0;
			
			for(int i = 0; i < numberOfReviewers; i++) {
				if(d[i]) {
					double[] curRevMetrics = reviewerMetricsList.get(i);
										
					double totalVariables = EncodingUtils.getReal(solution.getVariable(1)) + EncodingUtils.getReal(solution.getVariable(2)) + 
							EncodingUtils.getReal(solution.getVariable(3)) + EncodingUtils.getReal(solution.getVariable(4));
					
					double variableMultValue = 1/totalVariables;
					
					double var1 = EncodingUtils.getReal(solution.getVariable(1))* variableMultValue;
					RealVariable v1 = (RealVariable) solution.getVariable(1);
					v1.setValue(var1);
					double var2 = EncodingUtils.getReal(solution.getVariable(2))* variableMultValue;
					RealVariable v2 = (RealVariable) solution.getVariable(2);
					v2.setValue(var2);
					double var3 = EncodingUtils.getReal(solution.getVariable(3))* variableMultValue;
					RealVariable v3 = (RealVariable) solution.getVariable(3);
					v3.setValue(var3);
					double var4 = EncodingUtils.getReal(solution.getVariable(4))* variableMultValue;
					RealVariable v4 = (RealVariable) solution.getVariable(4);
					v4.setValue(var4);

					
					double CO = curRevMetrics[0] * var1;
					double RE = curRevMetrics[1] * var2;
					double FPA = curRevMetrics[2] * var3;
					double RPR = curRevMetrics[3] * var4;
										
					double CPR = CO + RE + FPA + RPR;
					f1 = f1 + CPR;
					
					int RR = (int) curRevMetrics[4];
					
					total_k = total_k + RR + 1;
				} else { 
					double[] curRevMetrics = reviewerMetricsList.get(i);
					int RR = (int) curRevMetrics[4];
					
					total_k = total_k + RR;
					
				}
			}
			
			for(int i = 0; i < numberOfReviewers; i++) {
				double[] curRevMetrics = reviewerMetricsList.get(i);
				if(d[i]) {
					int RR = (int) curRevMetrics[4] + 1;
					
					double H_r = (double)RR / (double)total_k;
					
					double log_H_r = Math.log(H_r) / Math.log(2);
					
					double SRW = H_r*log_H_r;
					
					f2 = f2 + SRW;
				} else {
					int RR = (int) curRevMetrics[4];
					
					if (RR == 0) {
						continue;
					}
					
					double H_r = (double)RR / (double)total_k;
					
					double log_H_r = Math.log(H_r) / Math.log(2);
					
					double SRW = H_r*log_H_r;
					
					f2 = f2 + SRW;
				}
			}
			
			double log_R = (Math.log(numberOfReviewers)/Math.log(2));
			
			f1 = (-1) * f1;
			f2 = f2/log_R;
			
			solution.setObjective(0, f1);
			solution.setObjective(1, f2);
			
		}

		@Override
		public Solution newSolution() {
			Solution solution = new Solution(numberOfVariables, numberOfObjectives);
			solution.setVariable(0, EncodingUtils.newBinary(numberOfReviewers));
			
			solution.setVariable(1, new RealVariable(0.0, 1.0));
			for( int i = 1; i < getNumberOfVariables(); i++) {
				solution.setVariable(i, new RealVariable(0.0, 1.0));
			}
			return solution;
		}
		
	}
	
	public static void main(String [] args) {
		ObjectMapper mapper = new ObjectMapper();
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader("output_with_metrics_1.json"));
			
			JSONObject jsonObject = (JSONObject) obj;
						
			HashMap<String,Object> change_id =  new ObjectMapper().readValue(jsonObject.get("change_id").toString(), HashMap.class);
			
			HashMap<String,Object> actual_reviewers =  new ObjectMapper().readValue(jsonObject.get("actual_reviewers").toString(), HashMap.class);
			
			HashMap<String,Object> metrics =  new ObjectMapper().readValue(jsonObject.get("metrics").toString(), HashMap.class);

			int i = 0;
			
			for(String key : change_id.keySet()) {
				i+=1;
				if (i > 2) {
					break;
				}
				
				LinkedHashMap<String, ArrayList> curMetrics = (LinkedHashMap<String, ArrayList>) metrics.get(key);
				
				PromiseClass cls = new PromiseClass(5, curMetrics);
				
				NondominatedPopulation result = new Executor()
						.withProblemClass(PromiseClassTwo.class,5, curMetrics)
						.withAlgorithm("NSGAII")
						.withProperty("operator", "sbx+hux+pm+bf")
						.withProperty("populationSize", 100)
						.withProperty("sbx.rate", 0.9)
						.withProperty("sbx.distributionIndex", 15.0)
						.withProperty("pm.rate", 0.1)
						.withProperty("pm.distributionIndex", 20.0)
						.withMaxEvaluations(10000).run();
				
				double lowest_x = 0.0;
				double lowest_y = 0.0;
				
				System.out.println(change_id.get(key));
				for (Solution solution : result) {
					
					if (solution.getObjective(0) < lowest_x) {
						lowest_x = solution.getObjective(0);
					}
					if (solution.getObjective(1) < lowest_y) {
						lowest_y = solution.getObjective(1);
					}
					System.out.format("%.4f      %.4f%n",
							solution.getObjective(0),
							solution.getObjective(1));
					System.out.println(solution.getVariable(0));
				}
				double lowest_distance = 1000;
				Solution curSolution = null;
				for (Solution solution : result) {
					double dist1 = Math.pow(solution.getObjective(0) - lowest_x,2);
					double dist2 = Math.pow(solution.getObjective(1) - lowest_y,2);
					
					double distance = Math.sqrt(dist1 + dist2);
					if (distance < lowest_distance) {
						lowest_distance = distance;
						curSolution = solution;
					}
				}
				BinaryVariable finalV = (BinaryVariable) curSolution.getVariable(0);
				
				int total_chosen = 0;
				int total_found = 0;
				
				for (int j =0; j < finalV.getNumberOfBits(); j++) {
					if (finalV.get(j) == true) {
						total_chosen+=1;
						ArrayList act_revs = (ArrayList) actual_reviewers.get(key);
						for (Object str : act_revs) {
							if (str.toString().equals(cls.reviewerKeys[j])) {
								total_found+=1;
							}
						}
					}
				}
				
				System.out.println("total found: " + total_found);
				System.out.println("total chosen: " + total_chosen);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
