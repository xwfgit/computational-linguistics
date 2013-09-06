package mt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MathineTranslation {
	public static void main(String[] args){
		if (args[3].equals("verydumb")){
			TauCalculation test = new TauCalculation(args[0], args[1]); 
			test.calculateFullText();
			test.decoder(args[2]);
//			System.out.println(FScore(args[3], args[4], args[2]));
		}else if (args[3].equals("noisychannel")){
			NoisyChannel noisyChannel = new NoisyChannel(args[1], args[0]);
			noisyChannel.decoder(args[2]);
//			System.out.println(FScore(args[3], args[5], args[2]));
		}
		else{
			FScore(args[1], args[0]);
		}
	}
	public static double FScore(String originEnglishFile, String outputEnglishFile){
		FileReader originFileReader = null;
		FileReader outputFileReader = null;
		FileReader frenchFileReader = null;
		BufferedReader originBufferedReader = null;
		BufferedReader outputBufferedReader = null;
		BufferedReader frenchBufferedReader = null;
		int correctCount = 0;
		int totalCount_origin = 0;
		int totalCount_output = 0;
		
		try{
			String originSentence = null;
			String outputSentence = null;
			originFileReader = new FileReader(originEnglishFile);
			outputFileReader = new FileReader(outputEnglishFile);
			originBufferedReader = new BufferedReader(originFileReader);
			outputBufferedReader = new BufferedReader(outputFileReader);
			
			while((originSentence = originBufferedReader.readLine()) != null && (outputSentence = outputBufferedReader.readLine()) != null){
				
				String[] outputWord = outputSentence.split(" ");
				if (outputWord.length <= 10){
					String[] originWord = originSentence.split(" ");
					totalCount_origin += originWord.length;
					totalCount_output += outputWord.length;
					Set<String> originSet = new HashSet<String>();
					for (String s : originWord) {
						originSet.add(s);
					}
					for (int i = 0; i < outputWord.length; i++) {
						if (originSet.contains(outputWord[i])) {
							correctCount += 1;
						}
					}
				}
				
			}
			double precision = correctCount * 1.0 / totalCount_output;
			double recall = correctCount * 1.0 / totalCount_origin;
			double F = 2 * (precision * recall) / (precision + recall);
			System.out.println("FScore: " + F);
			return F;
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (originFileReader != null){
				try{
					originFileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (outputFileReader != null){
				try{
					outputFileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (frenchFileReader != null){
				try{
					frenchFileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (originBufferedReader != null){
				try{
					originBufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (outputBufferedReader != null){
				try{
					outputBufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (frenchBufferedReader != null){
				try{
					frenchBufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
}
class NoisyChannel{
	private Map<String, Double> bigramThetaMap;
	private Map<String, Map<String, Double>> tauMap;
	private Bigram bigram;
	private TauCalculation tau;
	private Map<String, String> f_ePreMap;
	
	public NoisyChannel(String englishFile, String frenchFile){
		tau = new TauCalculation(englishFile, frenchFile);
		tau.calculateFullText();
		tau.calculateTauMap_reverse();
		tauMap = tau.getTauMapReverse();
		f_ePreMap = new HashMap<String, String>();
		bigramThetaMap(englishFile);
				
	}
	public void bigramThetaMap(String englishFile){
		bigram = new Bigram(englishFile);
		bigram.setAlpha(1.573327);
		bigram.setBeta(111.396867);
		bigram.unigramThetaMap();
		bigram.bigramThetaMap();
		bigramThetaMap = bigram.getBigramThetaMap();
	}
	public void decoder(String decodeFile){
		
		String outputFile = "noisychannel.txt";
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
				String res = translater(line);
				System.out.println(res);
				writer.write(res);
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
	public String translater(String frenchSentence){
		String[] frenchWordList = frenchSentence.split(" ");
		String[] englishSentence = new String[frenchWordList.length + 1];
		englishSentence[0] = bigram.getSymbol();
		String largestString = null;
		for (int i = 0; i < frenchWordList.length; i ++){
			double largest = 0.0;
			String f_ePre = frenchWordList[i] + " " + englishSentence[i];
			if (f_ePreMap.containsKey(f_ePre)){
				englishSentence[i + 1] = f_ePreMap.get(f_ePre);
			}else{
				if (tauMap.containsKey(frenchWordList[i])){
					Map<String, Double> tau_o_f = tauMap.get(frenchWordList[i]);
					Iterator<Map.Entry<String, Double>> it = tau_o_f.entrySet().iterator();
					while(it.hasNext()){
						Map.Entry<String, Double> pairs = it.next();
						String key = pairs.getKey();
						double value = pairs.getValue();
						String bigramWord = englishSentence[i] + " " + key;
						
						double prob = 0.0;
						if (bigramThetaMap.containsKey(bigramWord)){
							prob = bigramThetaMap.get(bigramWord) * value;
						}else{
							prob = bigram.generateBigramUnknownWordTheta(bigramWord) * value;
						}
						
						if (prob > largest){
							largest = prob;
							largestString = key;
						}
					}
					englishSentence[i + 1] = largestString;
					f_ePreMap.put(f_ePre, englishSentence[i + 1]);
				}
				else{
					englishSentence[i + 1] = frenchWordList[i];
				}
			}
		}
		StringBuilder english = new StringBuilder();
		for(int i = 1; i < englishSentence.length; i ++){
			english.append(englishSentence[i]);
			english.append(" ");
		}
		return english.toString();
	}
}

class TauCalculation{
	private ArrayList<String> french;
	private ArrayList<String> english;
	private Map<String, Map<String, Double>> tauMap;
	private Map<String, Map<String, Double>> tauMap_reverse;
	private Map<String, Double> nfoMap;
	private Map<String, Map<String, Double>> nMap;
	private Map<String, String> largestTauMap;
	private int totalFrenchWord;
	
	public TauCalculation(String frenchFile, String englishFile){
		
		french = new ArrayList<String>();
		english = new ArrayList<String>();
		tauMap = new HashMap<String, Map<String, Double>>();
		tauMap_reverse = new HashMap<String, Map<String, Double>>();
		nMap = new HashMap<String, Map<String, Double>>();
		largestTauMap = new HashMap<String, String>();
		parseFile( frenchFile,  englishFile);
		totalFrenchWord = 0;
		
	}
	
	
	public void decoder(String decodeFile){
			
		Iterator<Map.Entry<String, Map<String, Double>>> it_f = tauMap.entrySet().iterator();
		while(it_f.hasNext()){
			Map.Entry<String, Map<String, Double>> pairs_out = it_f.next();
			Map<String, Double> f_oMap = pairs_out.getValue();
			Iterator<Map.Entry<String, Double>> it_f_o = f_oMap.entrySet().iterator();
			double largest = 0.0;
			String largestString = null;
			while(it_f_o.hasNext()){
				Map.Entry<String, Double> pairs = it_f_o.next();
				if(pairs.getValue() > largest){
					largest = pairs.getValue();
					largestString = pairs.getKey();
				}
			}
			largestTauMap.put(pairs_out.getKey(), largestString);
		}
			
		String outputFile = "verydumb.txt";
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
				String[] frenchWord = line.split(" ");
				totalFrenchWord += frenchWord.length;
				for (int i = 0; i < frenchWord.length; i++) {
					if (largestTauMap.containsKey(frenchWord[i])) {
						writer.write(largestTauMap.get(frenchWord[i]) + " ");
						System.out.print(largestTauMap.get(frenchWord[i]) + " ");
					} else {
						writer.write(frenchWord[i] + " ");
						System.out.print(frenchWord[i] + " ");
					}

				}
				writer.newLine();
				System.out.println();
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
	public Map<String, Map<String, Double>> getTauMap(){
		return tauMap;
	}
	public Map<String, Map<String, Double>> getTauMapReverse(){
		return tauMap_reverse;
	}
	public void calculateFullText(){
		for (int i = 0; i < 10; i ++){
			calculateAllSentence();
		}
	}
	public void calculateAllSentence(){
		nMap = new HashMap<String, Map<String, Double>>();
		nfoMap = new HashMap<String, Double>();
		
		for (int index = 0; index < french.size(); index ++){
			String frenchSentence = french.get(index);
			String englishSentence = english.get(index);
			String[] englishWordList = englishSentence.split(" ");
			String[] frenchWordList = frenchSentence.split(" ");
			
			Map<String, Double> pMap = new HashMap<String, Double>();
			for (int i = 0; i < englishWordList.length; i ++){
				double value = 0.0;
				for (int j = 0; j < frenchWordList.length; j ++){
					Map<String, Double> tau_f_o = tauMap.get(frenchWordList[j]);
					value += tau_f_o.get(englishWordList[i]);
				}
				pMap.put(englishWordList[i], value);
				
			}
			calculateN(frenchSentence, englishSentence, pMap);
		}
		Iterator<Map.Entry<String, Map<String, Double>>> it = nMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Map<String, Double>> pairs_out = it.next();
			String f = pairs_out.getKey();
			Map<String, Double> f_oMap = pairs_out.getValue();
			Iterator<Map.Entry<String, Double>> it2 = f_oMap.entrySet().iterator();
			while(it2.hasNext()){
				Map.Entry<String, Double> pairs = it2.next();
				calculateTau(f, pairs.getKey(), nfoMap);
			}
		}
	}
	
	public void calculateTauMap_reverse(){
		Iterator<Map.Entry<String, Map<String, Double>>> tauMap_it = tauMap.entrySet().iterator();
		while(tauMap_it.hasNext()){
			Map.Entry<String, Map<String, Double>> pairs = tauMap_it.next();
			Map<String, Double> tau_e_o_map = pairs.getValue();
			Iterator<Map.Entry<String, Double>> tau_e_o_it = tau_e_o_map.entrySet().iterator();
			while(tau_e_o_it.hasNext()){
				Map.Entry<String, Double> pairs2 = tau_e_o_it.next();
				String f = pairs2.getKey();
				double value = pairs2.getValue();
				if (value - 0.0001 >= 0){
					Map<String, Double> tau_f_o_map;
					if (tauMap_reverse.containsKey(f)){
						tau_f_o_map = tauMap_reverse.get(f);
					}else{
						tau_f_o_map = new HashMap<String, Double>();
					}
					tau_f_o_map.put(pairs.getKey(), value);
					tauMap_reverse.put(f, tau_f_o_map);
				}
			}
		}
	}

	public void calculateTau(String frenchWord, String englishWord, Map<String, Double> nfoMap){
		Map<String, Double> f_oMap = nMap.get(frenchWord);
		if (!nfoMap.containsKey(frenchWord)){
			double n_f_o = 0.0;
			Iterator<Map.Entry<String, Double>> it = f_oMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String, Double> pairs = it.next();
				n_f_o += pairs.getValue();
			}
			nfoMap.put(frenchWord, n_f_o);
		}
		double n_f_e = f_oMap.get(englishWord);
		Map<String, Double> tau_f_o;
		if (tauMap.containsKey(frenchWord)){
			tau_f_o = tauMap.get(frenchWord);
		}else{
			tau_f_o = new HashMap<String, Double>();
		}
		tau_f_o.put(englishWord, n_f_e / nfoMap.get(frenchWord));
		tauMap.put(frenchWord, tau_f_o);
	}
	public void calculateN(String frenchSentence, String englishSentence, Map<String, Double> pMap){
		String[] englishWordList = englishSentence.split(" ");
		String[] frenchWordList = frenchSentence.split(" ");
		
		for (int i = 0; i < frenchWordList.length; i ++){
			for (int j = 0; j < englishWordList.length; j ++){
				double n_f_e = tauMap.get(frenchWordList[i]).get(englishWordList[j]) / pMap.get(englishWordList[j]);
				if (nMap.containsKey(frenchWordList[i])){
					Map<String, Double> f_oMap = nMap.get(frenchWordList[i]);
					if (f_oMap.containsKey(englishWordList[j])){
						f_oMap.put(englishWordList[j], f_oMap.get(englishWordList[j]) + n_f_e);
					}else{
						f_oMap.put(englishWordList[j], n_f_e);
					}
					nMap.put(frenchWordList[i], f_oMap);
				}else{
					Map<String, Double> f_oMap = new HashMap<String, Double>();
					f_oMap.put(englishWordList[j], n_f_e);
					nMap.put(frenchWordList[i], f_oMap);
				}
				
			}
		}
	}
	public void parseFile(String frenchFile, String englishFile){
		FileReader frenchFileReader = null;
		FileReader englishFileReader = null;
		BufferedReader frenchBufferedReader = null;
		BufferedReader englishBufferedReader = null;
		
		try{
			String frenchSentence;
			String englishSentence;
			frenchFileReader = new FileReader(frenchFile);
			englishFileReader = new FileReader(englishFile);
			frenchBufferedReader = new BufferedReader(frenchFileReader);
			englishBufferedReader = new BufferedReader(englishFileReader);
			
			while((frenchSentence = frenchBufferedReader.readLine()) != null && (englishSentence = englishBufferedReader.readLine()) != null){
				french.add(frenchSentence);
				english.add(englishSentence);
				String[] frenchWord = frenchSentence.split(" ");
				String[] englishWord = englishSentence.split(" ");
				for(int i = 0; i < frenchWord.length; i ++){
					for(int j = 0; j < englishWord.length; j ++){
						if (tauMap.containsKey(frenchWord[i])){
							Map<String, Double> tau_f_o = tauMap.get(frenchWord[i]);
							if (!tau_f_o.containsKey(englishWord[j])){
								tau_f_o.put(englishWord[j], 1.0);
								tauMap.put(frenchWord[i], tau_f_o);
							}
							
						}else{
							Map<String, Double> tau_f_o = new HashMap<String, Double>();
							tau_f_o.put(englishWord[j], 1.0);
							tauMap.put(frenchWord[i], tau_f_o);
						}
					}
				}
			}
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (frenchFileReader != null){
				try{
					frenchFileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (englishFileReader != null){
				try{
					englishFileReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (frenchBufferedReader != null){
				try{
					frenchBufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (englishBufferedReader != null){
				try{
					englishBufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	public ArrayList<String> getFrenchText(){
		return french;
	}
	public ArrayList<String> getEnglishText(){
		return english;
	}
}