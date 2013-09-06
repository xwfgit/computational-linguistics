package langmod;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LanguageModel {
	public static void main(String[] args){
		UnigramBigram langmod = new UnigramBigram(args[0]);
		//For alpha
		double bestAlpha = 0.0;
		double bestBeta = 0.0;
		if (args[4].equals("unigram")){
			langmod.parseTestData(args[2]);
			langmod.setAlpha(1);
			langmod.unigramLogProb();
			System.out.println("The log probability of '$testdata' according to a model trained on '$training', with smoothing parameters alpha set to 1: " + langmod.getUniLogProb());
			
			langmod.parseTestData(args[1]);
			bestAlpha = langmod.goldenSectionSearch(1.0, 100.0, 200.0, 0.01, true);
			langmod.parseTestData(args[2]);
			langmod.setAlpha(bestAlpha);
			langmod.unigramLogProb();
			System.out.println("The log probability of '$testdata' according to a model trained on '$training', with smoothing parameters alpha optimized according to the heldout data '$heldout' " + langmod.getUniLogProb());
		
			langmod.parseGoodBadData(args[3], true);
			System.out.println("The percentage of sentence pairs in '$goodbad' identified correctly, according to the model with optimized parameter " + langmod.getUniPercent() + " percent good sentence");
			
			System.out.println("The optimized value of alpha: " + bestAlpha);
		}
		// For beta
		else if (args[4].equals("bigram")){
			// Train best alpha
			langmod.parseTestData(args[1]);
			bestAlpha = langmod.goldenSectionSearch(1.0, 100.0, 200.0, 0.01, true);
			langmod.parseTestData(args[2]);
			langmod.setAlpha(bestAlpha);
			langmod.unigramLogProb();
			
			langmod.parseTestData(args[2]);
			langmod.bigramLogProb();
			System.out.println("The log probability of '$testdata' according to a model trained on '$training', with smoothing parameters alpha set to " + bestAlpha + " and beta set to 1: " + langmod.getBiLogProb());
			langmod.parseTestData(args[1]);
			bestBeta = langmod.goldenSectionSearch(1.0, 100.0, 200.0, 0.01, false);
			langmod.setBeta(bestBeta);
			langmod.parseTestData(args[2]);
			langmod.bigramLogProb();
			langmod.parseGoodBadData(args[3], false);
			System.out.println("The percentage of sentence pairs in '$goodbad' identified correctly, according to the model with optimized parameter " + langmod.getBipercent() + " percent good sentence");
			System.out.println("The log probability of '$testdata' according to a model trained on '$training', with smoothing parameters alpha and beta optimized according to the heldout data '$heldout' " + langmod.getBiLogProb());
			System.out.println("The optimized value of alpha: " + bestAlpha);
			System.out.println("The optimized value of beta: " + bestBeta);
		}
	}
}

class UnigramBigram{
	private String trainingData;
	
	private Map<String, Integer> unigramWordCountMap;
	private Map<String, Double> unigramThetaMap;
	private Map<String, Integer> bigramWordCountMap;
	private Map<String, Double> bigramThetaMap;
	
	private Map<String, Integer> testDataUniWordCountMap;
	private Map<String, Integer> testDataBiWordCountMap;
	
	private String symbol = "@";
	private int totalWord;
	private int totalCount = 0;
	private double alpha = 1.0;
	private double beta = 1.0;
	private double unigramUnknownWordTheta;
	private double uniLogProb = 0.0;
	private double biLogProb = 0.0;
	private double uniGoodbadPercent;
	private double biGoodbadPercent;
	private double phi = (1 + Math.sqrt(5)) / 2;
	private double resphi = 2 - phi;
	
	
	public UnigramBigram(String trainingData){
		this.trainingData = trainingData;
		unigramWordCountMap = new HashMap<String, Integer>();
		unigramThetaMap = new HashMap<String, Double>();
		
		bigramWordCountMap = new HashMap<String, Integer>();
		bigramThetaMap = new HashMap<String, Double>();
		
		parseTrainingData();
	}
	
	public void parseGoodBadData(String goodbadData, boolean isUni){
		FileReader inputFile = null;
		BufferedReader reader = null;
		try{
			String line;
			inputFile = new FileReader(goodbadData);
			reader = new BufferedReader(inputFile);
			int lineCount = 0;
			int goodCountUni = 0;
			int goodCountBi = 0;
			while((line = reader.readLine()) != null){
				double uniGood = 0.0;
				double uniBad = 0.0;
				double biGood = 0.0;
				double biBad = 0.0;
				line = symbol + " " + line + symbol;
				String[] list = line.split(" ");
				if (isUni){
					for (String s: list){
						uniGood += Math.log(calculateUniTheta(s));
					}
				}else{
					for (int i = 0; i < list.length - 1; i ++){
						String s = list[i] + " " + list[i + 1];
						biGood += Math.log(calculateBiLogProb(s));
					}
				}
				
				line = reader.readLine();
				line = symbol + " " + line + " " + symbol;
				list = line.split(" ");
				if (isUni){
					for (String s: list){
						uniBad += Math.log(calculateUniTheta(s));
					}
				}else{
					for (int i = 0; i < list.length - 1; i ++){
						String s = list[i] + " " + list[i + 1];
						biBad += Math.log(calculateBiLogProb(s));
					}
				}
				
				lineCount ++;
				if (isUni){
					if (uniGood > uniBad){
						goodCountUni ++;
					}
				}
				else{
					if (biGood > biBad){
						goodCountBi ++;
					}
				}
			}
			uniGoodbadPercent = 100 * goodCountUni * 1.0 / lineCount;
			biGoodbadPercent = 100 * goodCountBi * 1.0 / lineCount;
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (inputFile != null){
				try{
					inputFile.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (reader != null){
				try{
					reader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	public void parseTestData(String testData){
		testDataUniWordCountMap = new HashMap<String, Integer>();
		testDataBiWordCountMap = new HashMap<String, Integer>();
		FileReader inputFile = null;
		BufferedReader reader = null;
		try{
			String line;
			inputFile = new FileReader(testData);
			reader = new BufferedReader(inputFile);
			while((line = reader.readLine()) != null){
				line = symbol + " " + line + symbol;
				String[] list = line.split(" ");
				for (String s: list){
					if (testDataUniWordCountMap.containsKey(s)){
						testDataUniWordCountMap.put(s, testDataUniWordCountMap.get(s) + 1);
					}else{
						testDataUniWordCountMap.put(s, 1);
					}
				}
				for (int i = 0; i < list.length - 1; i ++){
					String s = list[i] + " " + list[i + 1];
					if (testDataBiWordCountMap.containsKey(s)){
						testDataBiWordCountMap.put(s, testDataBiWordCountMap.get(s) + 1);
					}else{
						testDataBiWordCountMap.put(s, 1);
					}
				}
			}
			
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (inputFile != null){
				try{
					inputFile.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (reader != null){
				try{
					reader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	public void parseTrainingData(){
		FileReader inputFile = null;
		BufferedReader reader = null;
		try{
			String line;
			inputFile = new FileReader(trainingData);
			reader = new BufferedReader(inputFile);
			while((line = reader.readLine()) != null){
				line = symbol + " " + line + symbol;
				String[] list = line.split(" ");
				for (String s: list){
					totalCount ++;
					if (unigramWordCountMap.containsKey(s)){
						unigramWordCountMap.put(s, unigramWordCountMap.get(s) + 1);
					}else{
						unigramWordCountMap.put(s, 1);
					}
				}

				for (int i = 0; i < list.length - 1; i ++){
					String s = list[i] + " " + list[i + 1];
					if (bigramWordCountMap.containsKey(s)){
						bigramWordCountMap.put(s, bigramWordCountMap.get(s) + 1);
					}else{
						bigramWordCountMap.put(s, 1);
					}
				}
			}
			totalWord = unigramWordCountMap.size() + 1;
			
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (inputFile != null){
				try{
					inputFile.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (reader != null){
				try{
					reader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public void bigramLogProb(){
		bigramThetaMap();
		biLogProb = 0.0;
		Iterator<Map.Entry<String, Integer>> it = testDataBiWordCountMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Integer> pairs = it.next();
			double value = calculateBiLogProb(pairs.getKey());
			biLogProb += Math.log(value) * pairs.getValue();
		}
	}
	public double calculateBiLogProb(String key){
		double res;
		if (bigramWordCountMap.containsKey(key)){
			res = bigramThetaMap.get(key);
		}else{
			res = generateBigramUnknownWordTheta(key);
		}
		return res;
	}
	private double generateBigramUnknownWordTheta(String bigramWord){
		String[] list = bigramWord.split(" ");
		double nom;
		double den = 0.0;
		if (unigramWordCountMap.containsKey(list[1])){
			nom = unigramThetaMap.get(list[1]) * beta;
		}else{
			nom = unigramUnknownWordTheta * beta;
		}
		if (unigramWordCountMap.containsKey(list[0])){
			den = unigramWordCountMap.get(list[0]);
			if (list[0].equals(symbol)){
				den /= 2;
			}
		}
		den += beta;
		return nom / den;
	}
	public void bigramThetaMap(){
		Iterator<Map.Entry<String, Integer>> it = bigramWordCountMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Integer> pairs = it.next();
			double value = generateBigramTrainingTheta(pairs.getKey(), pairs.getValue());
			bigramThetaMap.put(pairs.getKey(), value);
		}
	}
	private double generateBigramTrainingTheta(String key, int value){
		String[] list = key.split(" " );
		return (value + beta * unigramThetaMap.get(list[1])) / (unigramWordCountMap.get(list[0]) + beta);
	}
	
	public void unigramThetaMap(){
		Iterator<Map.Entry<String, Integer>> it = unigramWordCountMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Integer> pairs = it.next();
			String key = pairs.getKey();
			double value = generateUnigramTrainingTheta(pairs.getValue());
			unigramThetaMap.put(key, value);
		}
		generateUnigramUnknownWordTheta();
	}
	private double generateUnigramTrainingTheta(int value){
		return (value + alpha) / (totalCount + alpha * totalWord);
	}
	
	
	public void unigramLogProb(){
		unigramThetaMap();
		uniLogProb = 0.0;
		Iterator<Map.Entry<String, Integer>> it = testDataUniWordCountMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Integer> pairs = it.next();
			String key = pairs.getKey();
			int wordCount = pairs.getValue();
			double value = calculateUniTheta(key);
			uniLogProb += (Math.log(value) * wordCount);
		}
	}
	public double calculateUniTheta(String key){
		double res;
		if (unigramWordCountMap.containsKey(key)){
			res = unigramThetaMap.get(key);
		}else{
			res = unigramUnknownWordTheta;
		}
		return res;
	}
	
	/*
	 * The goldenSectionSearch method is adapted from wikipedia with the link below:
	 * http://en.wikipedia.org/wiki/Golden_section_search
	 */
	
	public double goldenSectionSearch(double a, double b, double c, double tau, boolean uni_bi) {
	    double x;
	    if (c - b > b - a)
	      x = b + resphi * (c - b);
	    else
	      x = b - resphi * (b - a);
	    if (Math.abs(c - a) < tau * (Math.abs(b) + Math.abs(x))) 
	      return (c + a) / 2; 
	    
	    double f1, f2;
		if (uni_bi) {
			setAlpha(x);
			unigramLogProb();
			f1 = getUniLogProb();
			
			setAlpha(b);
			unigramLogProb();
			f2 = getUniLogProb();
		} else {
			setBeta(x);
			bigramLogProb();
			f1 = getBiLogProb();
			
			setBeta(b);
			bigramLogProb();
			f2 = getBiLogProb();
		}
	    if (f2 < f1) {
	      if (c - b > b - a) return goldenSectionSearch(b, x, c, tau, uni_bi);
	      else return goldenSectionSearch(a, x, b, tau, uni_bi);
	    }
	    else {
	      if (c - b > b - a) return goldenSectionSearch(a, b, x, tau, uni_bi);
	      else return goldenSectionSearch(x, b, c, tau, uni_bi);
	    }
	  }
	
	public double getBiLogProb(){
		return biLogProb;
	}
	public double getUniLogProb(){
		return uniLogProb;
	}
	private void generateUnigramUnknownWordTheta(){
		unigramUnknownWordTheta = alpha / (totalCount + alpha * totalWord);
	}
	public double getUnigramUnknownWordTheta(){
		return unigramUnknownWordTheta;
	}
	public void setAlpha(double alpha){
		this.alpha = alpha;
	}
	public double getAlpha(){
		return alpha;
	}
	public void setBeta(double beta){
		this.beta = beta;
	}
	public double getBeta(){
		return beta;
	}
	public int getTotalWord(){
		return totalWord;
	}
	public int getTotalCount(){
		return totalCount;
	}
	public Map<String, Integer> getUnigramWordCountMap(){
		return unigramWordCountMap;
	}
	public void setSymbol(String symbol){
		this.symbol = symbol;
	}
	public String getSymbol(){
		return symbol;
	}
	public double getUniPercent(){
		return uniGoodbadPercent;
	}
	public double getBipercent(){
		return biGoodbadPercent;
	}
}