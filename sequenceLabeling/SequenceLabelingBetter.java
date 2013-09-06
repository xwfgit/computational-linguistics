package sequenceLabeling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SequenceLabelingBetter {
	public static void main(String[] args){
		EstimateParBetter viterbiPer = new EstimateParBetter(args[0]);
		viterbiPer.decode(args[1], args[2]);
	}
}
class EstimateParBetter{
	
	private Map<String, Map<String, Integer>> hidden_observation;
	private Map<String, Map<String, Integer>> hidden_hidden;
	private Map<String, Map<String, Double>> sigma;
	private Map<String, Map<String, Double>> tau;
	private Map<String, Integer> trainningMap;
	private Set<String> stateSet;
	private String previous;
	private String symbol;
	private String unknownSymbol;
	
	public EstimateParBetter(String input){
		hidden_observation = new HashMap<String, Map<String, Integer>>();
		hidden_hidden = new HashMap<String, Map<String, Integer>>();
		sigma = new HashMap<String, Map<String, Double>> ();
		tau = new HashMap<String, Map<String, Double>> ();
		trainningMap = new HashMap<String, Integer>();
		stateSet = new HashSet<String>();
		symbol = "@@";
		unknownSymbol = "@!";
		previous = symbol;
		parseFile(input);
		EstimateSigma();
		EstimateTau();
	}
	
	public void decode(String decodeFile, String outputFile){
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		FileWriter output = null;
		BufferedWriter writer = null;
		
		try{
			String line;
			fileReader = new FileReader(decodeFile);
			bufferedReader = new BufferedReader(fileReader);
			output = new FileWriter(outputFile);
			writer = new BufferedWriter (output);
			while((line = bufferedReader.readLine()) != null){
				String[] word_tag_list = line.split(" ");
				String[] word_list = new String[word_tag_list.length / 2];
				String[] tag_list = new String[word_tag_list.length / 2];
				for (int i = 0; i < word_tag_list.length; i += 2){
					word_list[i / 2] = word_tag_list[i];
					tag_list[i / 2] = word_tag_list[i + 1];
				}
				StringBuilder temp = new StringBuilder();
				for (String s: word_list){
					temp.append(s);
					temp.append(" ");
				}

				String[] decodedTag = viterbi(new String(temp));

				for (int i = 0; i < decodedTag.length; i ++){
					writer.write(word_list[i] + " " + decodedTag[i] + " ");
				}
				writer.newLine();
			}
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (fileReader != null){
				try{
					fileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			
			if (bufferedReader != null){
				try{
					bufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (writer != null){
				try{
					writer.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (output != null){
				try{
					output.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	public String[] viterbi(String sentence){
		String[] wordList = sentence.split(" ");
		int row = wordList.length;
		int col = stateSet.size();
		double[][] mu = new double[row + 1][col];
		int[][] backtracking = new int[row][col];
		for (int i = 0; i < row; i ++ ){
			String word = wordList[i];
			int j = 0;
			Iterator<String> curRow_it = stateSet.iterator();
			while(curRow_it.hasNext()){
				String curState = curRow_it.next();
				
				double tauProb = Double.MAX_VALUE;
				Map<String, Double> observationMap = tau.get(curState);
				
				if (observationMap.containsKey(word)){
					tauProb = observationMap.get(word);
				}else{
					int count = 0;
					if (trainningMap.containsKey(word)){
						count = trainningMap.get(word);
					}
					if (count <= 1){
						if (observationMap.containsKey(unknownSymbol)){
							tauProb = observationMap.get(unknownSymbol);
						}
					}
				}
				
				if (i == 0){
					Map<String, Double> hiddenMap = sigma.get(symbol);
					double sigmaProb = Double.MAX_VALUE;
					if (hiddenMap.containsKey(curState)){
						sigmaProb = hiddenMap.get(curState);
					}
					if (sigmaProb == Double.MAX_VALUE || tauProb == Double.MAX_VALUE){
						mu[i][j] = Double.MAX_VALUE;
					}else{
						mu[i][j] = sigmaProb + tauProb;
					}
				}else{
					Iterator<String> prevRow_it = stateSet.iterator();
					int k = 0;
					mu[i][j] = Double.MAX_VALUE;
					while(prevRow_it.hasNext()){
						String prevState = prevRow_it.next();
						Map<String, Double> hiddenMap = sigma.get(prevState);
						double sigmaProb = Double.MAX_VALUE;
						
						if (hiddenMap.containsKey(curState)){
							sigmaProb = hiddenMap.get(curState);
						}
						double muProb = Double.MAX_VALUE;
						
						if (sigmaProb == Double.MAX_VALUE || tauProb == Double.MAX_VALUE || mu[i - 1][k] == Double.MAX_VALUE ){
							muProb = Double.MAX_VALUE;
						}else{
							muProb = mu[i - 1][k] + sigmaProb + tauProb;
						}
						
						if (mu[i][j] - muProb >= 0.00001){
							mu[i][j] = muProb;
							backtracking[i][j] = k;
						}
						k ++;
						
					}
				}
				j ++;
			}
		}
		int i = 0;
		Iterator<String> curRow_it = stateSet.iterator();
		double min = Double.MAX_VALUE;
		int index = 0;
		while(curRow_it.hasNext()){
			String curState = curRow_it.next();
			Map<String, Double> hiddenMap = sigma.get(curState);
			if (hiddenMap.containsKey(symbol)){
				mu[row][i] = mu[row - 1][i] + hiddenMap.get(symbol);
				if (min > mu[row][i]){
					min = mu[row][i];
					index = i;
				}
			}else{
				mu[row][i] = Double.MAX_VALUE;
			}
			i ++;
		}
		String[] array = new String[stateSet.size()];

		String[] tags = new String[row];
		array = stateSet.toArray(array);
		tags[row - 1] = array[index];
		return backTracking(backtracking, backtracking[row - 1][index], row - 2, tags, array);
	}
	
	public String[] backTracking(int[][] backtracking, int j, int i, String[] tags, String[] state){
		
		if (i < 0){
			return tags;
		}else{
			tags[i] = state[j];
			return backTracking(backtracking, backtracking[i][j], i - 1, tags, state);
		}
	}
	public void EstimateSigma(){
		Iterator<Map.Entry<String, Map<String, Integer>>> hidden_it = hidden_hidden.entrySet().iterator();
		while(hidden_it.hasNext()){
			Map.Entry<String, Map<String, Integer>> sigma_pairs = hidden_it.next();
			Map<String, Integer> value = sigma_pairs.getValue();
			int count = 0;
			Iterator<Map.Entry<String, Integer>> count_it = value.entrySet().iterator();
			while(count_it.hasNext()){
				Map.Entry<String, Integer> count_pair = count_it.next();
				count += count_pair.getValue();
			}
			Map<String, Double> sigma_value = new HashMap<String, Double>();
			count_it = value.entrySet().iterator();
			while(count_it.hasNext()){
				Map.Entry<String, Integer> count_pair = count_it.next();
				sigma_value.put(count_pair.getKey(), -Math.log(count_pair.getValue() * 1.0 / count));
			}
			sigma.put(sigma_pairs.getKey(), sigma_value);
		}
	}
	
	
	public void EstimateTau(){
		Iterator<Map.Entry<String, Map<String, Integer>>> hidden_it = hidden_observation.entrySet().iterator();
		while(hidden_it.hasNext()){
			Map.Entry<String, Map<String, Integer>> tau_pairs = hidden_it.next();
			Map<String, Integer> value = tau_pairs.getValue();
			int count = 0;
			int unk = 0;
			Iterator<Map.Entry<String, Integer>> count_it = value.entrySet().iterator();
			while(count_it.hasNext()){
				Map.Entry<String, Integer> count_pair = count_it.next();
				count += count_pair.getValue();
//				System.out.println(count_pair.getKey());
				if (!count_pair.getKey().equals(symbol) && trainningMap.get(count_pair.getKey()) == 1){
					unk ++;
				}
			}
			Map<String, Double> tau_value = new HashMap<String, Double>();
			count_it = value.entrySet().iterator();
			while(count_it.hasNext()){
				Map.Entry<String, Integer> count_pair = count_it.next();
				if (!count_pair.getKey().equals(symbol) && trainningMap.get(count_pair.getKey()) > 1){
					tau_value.put(count_pair.getKey(), -Math.log((count_pair.getValue() * 1.0) / count));
				}
			}
			if (unk > 0){
				tau_value.put(unknownSymbol, -Math.log(unk * 1.0 / count));
			}
			tau.put(tau_pairs.getKey(), tau_value);
		}
	}

	public void parseFile(String input){
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try{
			String line;
			fileReader = new FileReader(input);
			bufferedReader = new BufferedReader(fileReader);
			
			while((line = bufferedReader.readLine()) != null){
				line = symbol + " " + symbol + " " + line + " " + symbol + " " + symbol;
				String[] wordList = line.split(" ");
				for (int i = 0; i < wordList.length; i += 2){
					String word = wordList[i];
					String tag = wordList[i + 1];
					if (trainningMap.containsKey(word)){
						trainningMap.put(word, trainningMap.get(word) + 1);
					}else{
						trainningMap.put(word, 1);
					}
					if (!word.equals(symbol)){
						stateSet.add(tag);
					}
					if (hidden_observation.containsKey(tag)){
						Map<String, Integer> observation = hidden_observation.get(tag);
						if (observation.containsKey(word)){
							observation.put(word, observation.get(word) + 1);
						}else{
							observation.put(word, 1);
						}
						hidden_observation.put(tag, observation);
					}else{
						Map<String, Integer> observation = new HashMap<String, Integer>();
						observation.put(word, 1);
						hidden_observation.put(tag, observation);
					}

					if (hidden_hidden.containsKey(previous)){
						Map<String, Integer> hidden = hidden_hidden.get(previous);
						if (hidden.containsKey(tag)){
							hidden.put(tag, hidden.get(tag) + 1);
						}else{
							hidden.put(tag, 1);
						}
						hidden_hidden.put(previous, hidden);
					}else{
						Map<String, Integer> hidden = new HashMap<String, Integer> ();
						hidden.put(tag, 1);
						hidden_hidden.put(previous, hidden);
					}
					previous = tag;
				}
			}
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (fileReader != null){
				try{
					fileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			
			if (bufferedReader != null){
				try{
					bufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			
		}
	}
}