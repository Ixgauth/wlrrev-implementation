package implementationPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.moeaframework.Executor;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Selection;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.operator.GAVariation;
import org.moeaframework.core.operator.InjectedInitialization;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.real.PM;
import org.moeaframework.core.operator.real.SBX;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PromiseGeneticAlgorithm {
	
	public static class PromiseClass extends AbstractProblem {
		
		int numberOfReviewers;
		
		String [] reviewerKeys;
		
		ArrayList<double []> reviewerMetricsList;
		

		public PromiseClass(int numberOfVariables, LinkedHashMap<String, ArrayList> metricsMap) {
			super(numberOfVariables, 3);
			
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
					
					total_k = total_k + RR + 10;
				} else { 
					double[] curRevMetrics = reviewerMetricsList.get(i);
					int RR = (int) curRevMetrics[4];
					
					total_k = total_k + RR;
					
				}
			}
			
			for(int i = 0; i < numberOfReviewers; i++) {
				double[] curRevMetrics = reviewerMetricsList.get(i);
				if(d[i]) {
					int RR = (int) curRevMetrics[4] + 10;
					
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
			
			BinaryVariable var0 = (BinaryVariable) solution.getVariable(0);
			double f3 = 0;
			for (int i = 0; i < var0.getNumberOfBits(); i++) {
				if(var0.get(i) == true) {
					
					f3+=1.0/var0.getNumberOfBits();
				}
			}
			
			double log_R = (Math.log(numberOfReviewers)/Math.log(2));
			
			f1 = (-1) * f1;
			f2 = f2/log_R;
			
			solution.setObjective(0, f1);
			solution.setObjective(1, f2);
			solution.setObjective(2, f3);
			
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
		
	@SuppressWarnings("unchecked")
	public static void main(String [] args) {
		ObjectMapper mapper = new ObjectMapper();
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader("output_same_as_chrev_with_ks.json"));
			
//			Object pot_obj = parser.parse(new FileReader("output_potential_revs.json"));
			
			JSONObject jsonObject = (JSONObject) obj;
			
//			JSONObject potJSONObject = (JSONObject) pot_obj;
						
			HashMap<String,Object> change_id =  new ObjectMapper().readValue(jsonObject.get("change_id").toString(), HashMap.class);
			
			HashMap<String,Object> actual_reviewers =  new ObjectMapper().readValue(jsonObject.get("actual_reviewers").toString(), HashMap.class);
			
			HashMap<String,Object> metrics =  new ObjectMapper().readValue(jsonObject.get("metrics").toString(), HashMap.class);
			
			HashMap<String,Object> recommended =  new ObjectMapper().readValue(jsonObject.get("chrev_choice").toString(), HashMap.class);
			
			HashMap<String,Object> recommended_k_2 =  new ObjectMapper().readValue(jsonObject.get("recs_k_2").toString(), HashMap.class);
			
			HashMap<String,Object> recommended_k_3 =  new ObjectMapper().readValue(jsonObject.get("recs_k_3").toString(), HashMap.class);
			
			HashMap<String,Object> recommended_k_5 =  new ObjectMapper().readValue(jsonObject.get("recs_k_5").toString(), HashMap.class);

			int i = 0;
			
			int correct_guesses = 0;
			int total_number_guesses = 0;
			int total_num_reviewed = 0;
			int total_overlap_chrev = 0;
			int total_overlap_chrev_k_2 = 0;
			int total_overlap_chrev_k_3 = 0;
			int total_overlap_chrev_k_5 = 0;
			int total_skipped =0;
			int total_chrev_choices = 0;
			int total_chrev_choices_k_2 = 0;
			int total_chrev_choices_k_3 = 0;
			int total_chrev_choices_k_5 = 0;
			
			for(String key : change_id.keySet()) {
				i+=1;
//				if (i > 100) {
//					break;
//				}
				
				String cur_change_id = (String) change_id.get(key);

				LinkedHashMap<String, ArrayList> curMetrics = (LinkedHashMap<String, ArrayList>) metrics.get(key);
				
				PromiseClass cls = new PromiseClass(5, curMetrics);
				
				PromiseInitialization initialization = new PromiseInitialization(cls, 100);
				
				TournamentSelection selection = new TournamentSelection(2);
				
				Variation variation = new GAVariation(new SBX(0.9, 15.0), new PM(0.1, 20.0));
				
				Algorithm algorithm = new NSGAII(cls,new NondominatedSortingPopulation(),null, selection,variation, initialization);
				
				while (algorithm.getNumberOfEvaluations() < 1000) {
					algorithm.step();
				}
				
				NondominatedPopulation result = algorithm.getResult();
				
				double lowest_x = 0.0;
				double lowest_y = 0.0;
				double lowest_z = 0.0;
				
				System.out.println(change_id.get(key));
				for (Solution solution : result) {
					
					if (solution.getObjective(0) < lowest_x) {
						lowest_x = solution.getObjective(0);
					}
					if (solution.getObjective(1) < lowest_y) {
						lowest_y = solution.getObjective(1);
					}
					if (solution.getObjective(2) < lowest_z) {
						lowest_z = solution.getObjective(2);
					}
//					System.out.format("%.4f      %.4f   %.4f%n",
//							solution.getObjective(0),
//							solution.getObjective(1), solution.getObjective(2));
//					System.out.println(solution.getVariable(0));
				}
				double lowest_distance = 1000;
				Solution curSolution = null;
				for (Solution solution : result) {
					double dist1 = Math.pow(solution.getObjective(0) - lowest_x,2);
					double dist2 = Math.pow(solution.getObjective(1) - lowest_y,2);
					double dist3 = Math.pow(solution.getObjective(2) - lowest_z, 2);
					
					double distance = Math.sqrt(dist1 + dist2 + dist3);
					if (distance < lowest_distance) {
						lowest_distance = distance;
						curSolution = solution;
					}
				}
				if (curSolution == null) {
					System.out.println("no solution");
					total_skipped +=1;
					continue;
				}
				BinaryVariable finalV = (BinaryVariable) curSolution.getVariable(0);
				
				int total_chosen = 0;
				int total_found = 0;
				int total_correct = 0;
				int chrev_overlap = 0;
				int chrev_overlap_k_2 = 0;
				int chrev_overlap_k_3 = 0;
				int chrev_overlap_k_5 = 0;
				
				total_chrev_choices +=1;
				
				ArrayList chrev_k_2 = (ArrayList) recommended_k_2.get(key);
				total_chrev_choices_k_2 +=chrev_k_2.size();
				
				ArrayList chrev_k_3 = (ArrayList) recommended_k_3.get(key);
				total_chrev_choices_k_3 +=chrev_k_3.size();
				
				ArrayList chrev_k_5 = (ArrayList) recommended_k_5.get(key);
				total_chrev_choices_k_5 +=chrev_k_5.size();
				
				for (int j =0; j < finalV.getNumberOfBits(); j++) {
					if (finalV.get(j) == true) {
						total_chosen+=1;
						ArrayList act_revs = (ArrayList) actual_reviewers.get(key);
						total_correct = act_revs.size();
						for (Object str : act_revs) {
							if (str.toString().equals(cls.reviewerKeys[j])) {
								total_found+=1;
							}
						}
						
						Integer chrevs = (Integer) recommended.get(key);
						
						
						if (chrevs.toString().equals(cls.reviewerKeys[j])) {
							chrev_overlap+=1;
						}
						
						
						
						for (Object str : chrev_k_2) {
							if (str.toString().equals(cls.reviewerKeys[j])) {
								chrev_overlap_k_2+=1;
							}
						}
						
						
						for (Object str : chrev_k_3) {
							if (str.toString().equals(cls.reviewerKeys[j])) {
								chrev_overlap_k_3+=1;
							}
						}
						
						
						for (Object str : chrev_k_5) {
							if (str.toString().equals(cls.reviewerKeys[j])) {
								chrev_overlap_k_5+=1;
							}
						}
						
//						ArrayList cur_pot_revs = (ArrayList) pot_revs.get(cur_change_id);
//						total_correct+= cur_pot_revs.size();
//						for (Object str : cur_pot_revs) {
//							if (str.toString().equals(cls.reviewerKeys[j])) {
//								total_found+=1;
//							}
//						}
					}
				}
				correct_guesses += total_found;
				total_number_guesses += total_chosen;
				total_num_reviewed += total_correct;
				total_overlap_chrev += chrev_overlap;
				total_overlap_chrev_k_2 += chrev_overlap_k_2;
				total_overlap_chrev_k_3 += chrev_overlap_k_3;
				total_overlap_chrev_k_5 += chrev_overlap_k_5;
				
				System.out.println("total found: " + total_found);
				System.out.println("total chosen: " + total_chosen);
				System.out.println("total correct " + total_correct);
				System.out.println("overlap: " + chrev_overlap);
			}
			
			System.out.println(correct_guesses);
			System.out.println(total_number_guesses);
			System.out.println(total_num_reviewed);
			
			double precision = (double) correct_guesses / (double) total_number_guesses;
			double recall = (double) correct_guesses / (double) total_num_reviewed;
			double f_score = (2*precision*recall) / (precision + recall);
			
			
			System.out.println("Precision: " + precision);
			System.out.println("Recall: " + recall);
			System.out.println("F_score: " + f_score);
			System.out.println("Total overlap: " + total_overlap_chrev);
			System.out.println("Total overlap K=2: " + total_overlap_chrev_k_2);
			System.out.println("Total overlap K=3: " + total_overlap_chrev_k_3);
			System.out.println("Total overlap K=5: " + total_overlap_chrev_k_5);
			
			System.out.println("total skipped: " + total_skipped);
			double ratio_of_overlap = (double) total_overlap_chrev/total_chrev_choices;
			double ratio_of_overlap_k_2 = (double) total_overlap_chrev_k_2/total_chrev_choices_k_2;
			double ratio_of_overlap_k_3 = (double) total_overlap_chrev_k_3/total_chrev_choices_k_3;
			double ratio_of_overlap_k_5 = (double) total_overlap_chrev_k_5/total_chrev_choices_k_5;
			System.out.println("Total overlapped average: " + ratio_of_overlap);
			System.out.println("Total overlapped average: " + ratio_of_overlap_k_2);
			System.out.println("Total overlapped average: " + ratio_of_overlap_k_3);
			System.out.println("Total overlapped average: " + ratio_of_overlap_k_5);

		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

}
