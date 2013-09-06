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
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

public class Model2 {
	public static void main(String[] args){
		EM model2 = new EM(args[0], args[1]);
		model2.inference();
		model2.reverse_tauMap();
		Map<String, Map<String, Double>> tauMap = model2.getTauMap();
		Map<String, Map<Integer, Map<Integer, Double>>> sigmaMap = model2.getSigmaMap();
		FileWriter output = null;
		BufferedWriter writer = null;
		try{
			output = new FileWriter("tau.txt");
			writer = new BufferedWriter (output);
			Iterator<Entry<String, Map<String, Double>>> tau_it = tauMap.entrySet().iterator();
			while(tau_it.hasNext()){
				Map.Entry<String, Map<String, Double>> tau_pair = tau_it.next();
				String french = tau_pair.getKey();
				Map<String, Double> e_map = tau_pair.getValue();
				Iterator<Entry<String, Double>> emap_it = e_map.entrySet().iterator();
				double prob = Double.MIN_VALUE;
				String res = null;
				while(emap_it.hasNext()){
					Map.Entry<String, Double> emap_pair = emap_it.next();
					String english = emap_pair.getKey();
					if (emap_pair.getValue() > prob){
						prob = emap_pair.getValue();
						res = english;
					}
				}
				writer.write(french + " " + res + " " + prob);
				writer.newLine();
			}
			writer.flush();
			output = new FileWriter("distortion_probability.txt");
			writer = new BufferedWriter (output);
			ArrayList<String> french = model2.getFrench();
			ArrayList<String> english = model2.getEnglish();
			
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < french.size(); i ++){
				String[] frenchWord = french.get(i).split(" ");
				String[] englishWord = english.get(i).split(" ");
				int m = frenchWord.length;
				int l = englishWord.length;
				if (m <= 10 && l <= 10){
					if (!set.contains(l + " " + m)){
						Map<Integer, Map<Integer, Double>> i_j_map = sigmaMap.get(l + " " + m);
						for (int f = 0; f < m; f ++){
							Map<Integer, Double> j_map = i_j_map.get(f);
							for (int e = 0; e < l; e ++){
								writer.write((f + 1) + " " + (e + 1) + " " + j_map.get(e));
								writer.newLine();
							}
						}
						writer.newLine();
						set.add(l + " " + m);
					}
					
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
}

class EM{
	private ArrayList<String> french;
	private ArrayList<String> english;
	private Map<String, Map<String, Double>> tauMap;
	private Map<String, Map<String, Double>> tauMap_reverse;
	private Map<String, Map<Integer, Map<Integer, Double>>> sigmaMap;
	private Map<String, Map<String, Double>> e_fMap;
	private Map<String, Double> eMap;
	private Map<String, Map<Integer, Map<Integer, Double>>>  alignMap;
	private Map<String, Map<Integer, Double>> align_allMap;
	private Random rn;
	private Model1 model1;
	
	public EM(String englishFile, String frenchFile){
		french = new ArrayList<String>();
		english = new ArrayList<String>();
		tauMap = new HashMap<String, Map<String, Double>>();
		tauMap_reverse = new HashMap<String, Map<String, Double>>();
		sigmaMap = new HashMap<String, Map<Integer, Map<Integer, Double>>>();
		rn = new Random();
		parseFile(frenchFile, englishFile);
		model1 = new Model1(tauMap, french, english);
		model1.calculateFullText();
	}
	public ArrayList<String> getFrench(){
		return french;
	}
	public ArrayList<String> getEnglish(){
		return english;
	}
	public void reverse_tauMap(){
		Iterator<Entry<String, Map<String, Double>>> tau_it = tauMap.entrySet().iterator();
		while(tau_it.hasNext()){
			Map.Entry<String, Map<String, Double>> tau_pair = tau_it.next();
			String french = tau_pair.getKey();
			Map<String, Double> e_map = tau_pair.getValue();
			Iterator<Entry<String, Double>> emap_it = e_map.entrySet().iterator();
			while (emap_it.hasNext()) {
				Map.Entry<String, Double> emap_pair = emap_it.next();
				String english = emap_pair.getKey();
				double prob = emap_pair.getValue();
				Map<String, Double> fMap_reverse = tauMap_reverse.get(english);
				if (fMap_reverse == null){
					fMap_reverse = new HashMap<String, Double>();
				}
				fMap_reverse.put(french, prob);
				tauMap_reverse.put(english, fMap_reverse);
			}
		}
	}
	public void inference(){
		for (int i = 0; i < 5; i ++){
			em();
		}
	}
	public void em(){
		int n = french.size();
		e_fMap = new HashMap<String, Map<String, Double>>();
		alignMap = new HashMap<String, Map<Integer, Map<Integer, Double>>> ();
		eMap = new HashMap<String, Double>();
		align_allMap = new HashMap<String, Map<Integer, Double>>();
		for (int k = 0; k < n; k ++){
			String frenchSentence = french.get(k);
			String englishSentence = english.get(k);
			String[] frenchList = frenchSentence.split(" ");
			String[] englishList = englishSentence.split(" ");
			int m = frenchList.length;
			int l = englishList.length;
			
			Map<Integer, Map<Integer, Double>> i_j = sigmaMap.get(l + " " + m);
			
			Map<Integer, Map<Integer, Double>> align_i_j = alignMap.get(l + " " + m);
			Map<Integer, Double> align_all_i = align_allMap.get(l + " " + m);
			
			if (align_i_j == null){
				align_i_j = new HashMap<Integer, Map<Integer, Double>>();
			}
			if (align_all_i == null){
				align_all_i = new HashMap<Integer, Double>();
			}
			for (int i = 0; i < m; i ++){
				//Auxiliary func: sigma
				Map<String, Double> e_prob = tauMap.get(frenchList[i]);
				ArrayList<Double> sigmaList = new ArrayList<Double>();
				Map<Integer, Double> j_prob = i_j.get(i);
				double sum = 0.0;
				for (int j = 0; j < l; j ++){
					double temp = j_prob.get(j) * e_prob.get(englishList[j]);
					sigmaList.add(temp);
					sum += temp;
				}
				for (int j = 0; j < l; j ++){
					sigmaList.set(j, sigmaList.get(j) / sum);
				}
				
				//j,l,m
				Map<Integer, Double> align_j = align_i_j.get(i);
				if (align_j == null){
					align_j = new HashMap<Integer, Double>();
				}
				double sum_align = 0.0;
				for (int j = 0; j < l; j ++){
					// e_f
					Map<String, Double> f_map = e_fMap.get(englishList[j]);
					if (f_map == null || !f_map.containsKey(frenchList[i])){
						if (f_map == null){
							f_map = new HashMap<String, Double>();
						}
						f_map.put(frenchList[i], sigmaList.get(j));
						
					}else{
						f_map.put(frenchList[i], f_map.get(frenchList[i]) + sigmaList.get(j));
					}
					e_fMap.put(englishList[j], f_map);
					
					//e
					if (eMap.get(englishList[j]) != null){
						eMap.put(englishList[j], sigmaList.get(j) + eMap.get(englishList[j]));
					}else{
						eMap.put(englishList[j], sigmaList.get(j));
					}
					
					// j,i,l,m
					if (align_j.containsKey(j)){
						align_j.put(j, align_j.get(j) + sigmaList.get(j));
					}else{
						align_j.put(j, sigmaList.get(j));
					}
					
					//i,l,m
					sum_align += sigmaList.get(j);
				}
				align_i_j.put(i, align_j);
				alignMap.put(l + " " + m, align_i_j);
				if (align_all_i.containsKey(i)){
					align_all_i.put(i, align_all_i.get(i) + sum_align);
				}else{
					align_all_i.put(i, sum_align);
				}
				align_allMap.put(l + " " + m, align_all_i);
			}
		}
		
		Iterator<Entry<String, Map<String, Double>>> it = e_fMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Map<String, Double>> pair = it.next();
			String english_word = pair.getKey();
			Map<String, Double> value = pair.getValue();
			Iterator<Entry<String, Double>> it2 = value.entrySet().iterator();
			while(it2.hasNext()){
				Map.Entry<String, Double> pair2 = it2.next();
				String french_word = pair2.getKey();
				double prob = pair2.getValue();
				Map<String, Double> temp = tauMap.get(french_word);
				temp.put(english_word, prob / eMap.get(english_word));
				tauMap.put(french_word, temp);
			}
		}
		
		Iterator<Entry<String, Map<Integer, Map<Integer, Double>>>> it_align = alignMap.entrySet().iterator();
		while(it_align.hasNext()){
			Map.Entry<String, Map<Integer, Map<Integer, Double>>> pair_align = it_align.next();
			String l_m = pair_align.getKey();
			Map<Integer, Map<Integer, Double>> i_j_map = pair_align.getValue();
			Iterator<Entry<Integer, Map<Integer, Double>>> it_i_j = i_j_map.entrySet().iterator();
			while(it_i_j.hasNext()){
				Map.Entry<Integer, Map<Integer, Double>> pair_i_j = it_i_j.next();
				int i = pair_i_j.getKey();
				Map<Integer, Double> j_map = pair_i_j.getValue();
				Iterator<Entry<Integer, Double>> it_j = j_map.entrySet().iterator();
				while(it_j.hasNext()){
					Map.Entry<Integer, Double> j_pair = it_j.next();
					int j = j_pair.getKey();
					double prob = j_pair.getValue();
					
					Map<Integer, Map<Integer, Double>> sigma_i_j = sigmaMap.get(l_m);
					Map<Integer, Double> sigma_j = sigma_i_j.get(i);
					sigma_j.put(j, prob / align_allMap.get(l_m).get(i));
					sigma_i_j.put(i, sigma_j);
					sigmaMap.put(l_m, sigma_i_j);
				}
			}
		}
	}
	public void parseFile(String frenchFile, String englishFile){
		FileReader frenchFileReader = null;
		FileReader englishFileReader = null;
		BufferedReader frenchBufferedReader = null;
		BufferedReader englishBufferedReader = null;
		double random = rn.nextDouble();
		double tau = 1.0;
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
				
				int l = englishWord.length;
				int m = frenchWord.length;
				String l_m = l + " " + m;
				for(int i = 0; i < frenchWord.length; i ++){
					for(int j = 0; j < englishWord.length; j ++){
						if (tauMap.containsKey(frenchWord[i])){
							Map<String, Double> tau_f_o = tauMap.get(frenchWord[i]);
							if (!tau_f_o.containsKey(englishWord[j])){
								tau_f_o.put(englishWord[j], tau);
								tauMap.put(frenchWord[i], tau_f_o);
							}
						}else{
							Map<String, Double> tau_f_o = new HashMap<String, Double>();
							tau_f_o.put(englishWord[j], tau);
							tauMap.put(frenchWord[i], tau_f_o);
						}
						Map<Integer, Map<Integer, Double>> i_j = sigmaMap.get(l_m);
						if (i_j == null || !i_j.containsKey(i)){
							if (i_j == null){
								i_j = new HashMap<Integer, Map<Integer, Double>>();
							}
							Map<Integer, Double> j_prob = new HashMap<Integer, Double>();
							j_prob.put(j, random);
							i_j.put(i, j_prob);
							sigmaMap.put(l_m, i_j);
						}else{
							Map<Integer, Double> j_prob = i_j.get(i);
							if (!j_prob.containsKey(j)){
								j_prob.put(j, random);
								i_j.put(i, j_prob);
								sigmaMap.put(l_m, i_j);
							}
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
	public Map<String, Map<String, Double>> getTauMap(){
		return tauMap_reverse;
	}
	public Map<String, Map<Integer, Map<Integer, Double>>> getSigmaMap(){
		return sigmaMap;
	}
}

class Model1{
	private ArrayList<String> french;
	private ArrayList<String> english;
	private Map<String, Map<String, Double>> tauMap;
	private Map<String, Double> nfoMap;
	private Map<String, Map<String, Double>> nMap;
	
	public Model1(Map<String, Map<String, Double>> tauMap, ArrayList<String> french, ArrayList<String> english){
		this.french = french;
		this.english = english;
		this.tauMap = tauMap;
		nMap = new HashMap<String, Map<String, Double>>();
	}
	
	public Map<String, Map<String, Double>> getTauMap(){
		return tauMap;
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
}
