package topicmod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Iterator;

public class TopicModel {
	public static void main(String[] args){
		EM em = new EM(50, 1000, args[0]);
		em.calculateParameters();
//		em.output();
		em.article17();
		em.mostProbableWord();
		
	}
}
class Word_Prob implements Comparable<Double>{
	private String word;
	private Double prob;
	public Word_Prob(String word, Double prob){
		this.word = word;
		this.prob = prob;
	}
	public String getWord(){
		return word;
	}
	public double getProb(){
		return prob;
	}
	public int compareTo(Double prob2){
		if (prob > prob2){
			return 1;
		}
		if (prob < prob2){
			return -1;
		}
		return 0;
	}
}

class ValueComparator implements Comparator<Word_Prob>{
	public int compare(Word_Prob x, Word_Prob y){
		if (x.compareTo(y.getProb()) < 0){
			return 1;
		}
		if (x.compareTo(y.getProb()) > 0){
			return -1;
		}
		return 0;
	}
}
class EM{
	private double[][] delta;
	private Map<String, Double[]> tau;
	private ArrayList<Integer> numberList;
	private int N;
	private double[] tau_sum;
	private int document_number;
	private Random rn;
	private ArrayList<String[]> corpus;
	private double[][] delta_n;
	private Map<String, Double[]> tau_n;
	private ArrayList<PriorityQueue<Word_Prob>> p_head;
	
	public EM(int N, int document_number, String news){
		this.N = N;
		this.document_number = document_number;
		this.delta = new double[this.document_number][N];
		this.delta_n = new double[this.document_number][N];
		numberList = new ArrayList<Integer>();
		rn = new Random();
		double init = 1.0;
		for (int i = 0; i < document_number; i ++){
			for (int j = 0; j < N; j ++){
				delta[i][j] = rn.nextDouble() * 0.02 + init;
			}
		}
		tau = new HashMap<String, Double[]>();
		corpus = new ArrayList<String[]>();
		parseFile(news);
	}
	
	public void em(){
		double[] delta_sum = new double[document_number];
		tau_sum = new double[N];
		delta_n = new double[document_number][N];
		tau_n = new HashMap<String, Double[]>();
		for (int i = 0; i < document_number; i ++){
			String[] wordList = corpus.get(i);
			for (int j = 0; j < wordList.length; j ++){
				double p = 0.0;
				double[] q = new double[N];
				// calculate p, temp q without division
				for (int k = 0; k < N; k ++){
					double product = delta[i][k] * tau.get(wordList[j])[k];
					p += product;
					q[k] = product;
				}
				//calculate q and set delta and tau
				Double[] word_topic = tau_n.get(wordList[j]);
				if (word_topic == null){
					word_topic = new Double[N];
				}
				
				for (int k = 0; k < N; k ++){
					q[k] = q[k] / p;
					delta_n[i][k] += q[k];
					delta_sum[i] += q[k];
					if (word_topic[k] == null){
						word_topic[k] = new Double(0.0);
					}
					word_topic[k] += q[k];
					tau_sum[k] += q[k];
				}
				tau_n.put(wordList[j], word_topic);
			}
		}
		for (int i = 0; i < document_number; i ++){
			for (int j = 0; j < N; j ++){
				delta[i][j] = delta_n[i][j] / delta_sum[i];
			}
		}
		
		Iterator<Map.Entry<String, Double[]>> temp = tau_n.entrySet().iterator();
		while(temp.hasNext()){
			Map.Entry<String, Double[]> pair = temp.next();
			String key = pair.getKey();
			Double[] prob = pair.getValue();
			for (int i = 0; i < N; i ++){
				prob[i] /= tau_sum[i];
			}
			tau.put(key, prob);
		}
	}
	
	public void parseFile(String news){
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		
		try{
			String document = new String();
			fileReader = new FileReader(news);
			bufferedReader = new BufferedReader(fileReader);
			double rand = rn.nextDouble();
//			double rand = 1.0;
			String line;
			while((line = bufferedReader.readLine()) != null ){
				if (line.length() > 0){
					String[] wordList = line.split(" ");
					if (line.charAt(0) != ' ' && wordList.length == 1){
						numberList.add(Integer.parseInt(wordList[0]));
						if (document.length() > 0){
							corpus.add(document.split(" "));
							document = new String();
						}
					}else{
						for (int i = 0; i < wordList.length; i ++){
							if (!tau.containsKey(wordList[i])){
								Double[] word_topic = new Double[N];
								for (int j = 0; j < N; j ++){
									word_topic[j] = new Double(rand);
								}
								tau.put(wordList[i], word_topic);
							}
						}
						document += line;
					}
				}
			}
			corpus.add(document.split(" "));
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
	public void output(){

		FileWriter output = null;
		BufferedWriter writer = null;
		int[] topic_count = new int[N];
		for (int i = 0; i < N; i ++){
			topic_count[i] = 0;
		}
		try{
			output = new FileWriter("article17.txt");
			writer = new BufferedWriter (output);
			for (int j = 0; j < document_number; j ++){
				double max = 0.0;
				int topic = -1;
				for (int i = 0; i < N; i ++){
					if (delta[j][i] > max){
						max = delta[j][i];
						topic = i;
					}
				}
				writer.write(Double.toString(max) + " " + topic);
				topic_count[topic] += 1;
				
				writer.newLine();
			}
			for (int i = 0; i < N; i ++){
				if (topic_count[i] == 1){
					 writer.write(Integer.toString(topic_count[i]));
					 writer.newLine();
				}
			}
			for (int i = 0; i < N; i ++){
				if (topic_count[i] == 0){
					 writer.write(Integer.toString(topic_count[i]));
					 writer.newLine();
				}
			}
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
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
	public void article17(){
		System.out.println("The probabilities of topics for article 17 ");
		for (int i = 0; i < N; i++) {
			System.out.println(Double.toString(delta[16][i]) + " ");
		}
	}
	public void mostProbableWord(){
		p_head = new ArrayList<PriorityQueue<Word_Prob>>();
		ValueComparator vCom = new ValueComparator();
		for (int i = 0; i < N; i++) {
			p_head.add(new PriorityQueue<Word_Prob>(10, vCom));
		}
		Iterator<Map.Entry<String, Double[]>> it = tau.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Double[]> pair = it.next();
			String key = pair.getKey();
			Double[] value = pair.getValue();
			for (int i = 0; i < N; i++) {
				Double prob = (value[i] + 5) / (tau_sum[i] + 5 * tau.size());
				Word_Prob tuple = new Word_Prob(key, prob);
				p_head.get(i).add(tuple);
			}
		}
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < 15; j++) {
				Word_Prob tuple = p_head.get(i).poll();
				System.out.print(tuple.getWord() + " " + tuple.getProb() + "\t");
			}
			System.out.println();
		}
	}
	public double loglikelyhood(){
		double logsum = 0.0;
		for (int i = 0; i < document_number; i ++){
			for (int j = 0; j < numberList.get(i); j ++){
				String[] wordList = corpus.get(i);
				double product = 0.0;
				Double[] word_topic = tau.get(wordList[j]);
				for (int k = 0; k < N; k ++ ){
					product += delta[i][k] * word_topic[k];
				}
				logsum += Math.log(product);
			}
		}
		return logsum;
	}
	public void calculateParameters(){
		double log = 0.0;
		for (int i = 0; i < 50; i ++){
			em();
			log = loglikelyhood();
		}
		System.out.println("The log likelyhood is: " + log);
	}
}