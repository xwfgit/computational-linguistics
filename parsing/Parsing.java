package parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Parsing{
	public static void main(String[] args){
		ParseTree parsetree = new ParseTree(args[0], args[1], args[2]);
	}
}
class tuple{
	public String lhs;
	public String word;
	public tuple c1;
	public tuple c2;
	public double u;
	public tuple(String word, String lhs, tuple c1, tuple c2, double u){
		this.lhs = lhs;
		this.word = word;
		this.c1 = c1;
		this.c2 = c2;
		this.u = u;
	}
}

class tuple_set{
	public Map<String, tuple> tupleSet = new HashMap<String, tuple>();
}
class ParseTree extends Thread{
	private Map<String, Map<String, Double>> likelyhood;
	private Map<String, Map<String, Map<String, Double>>> left_rule_right;
	private Map<String, Map<String, Double>> left_rule_right_unary;
	
	public ParseTree(String blt, String wsj, String tree){
		likelyhood = new HashMap<String, Map<String, Double>>();
		left_rule_right = new HashMap<String, Map<String, Map<String, Double>>>();
		left_rule_right_unary = new HashMap<String, Map<String, Double>>();
		cal_likelyhood(blt);
		generateTripleMap();
		run( blt,  wsj,  tree);
	}
	
	public void run(String blt, String wsj, String tree){
		
		generateTreeSet(wsj, tree);
	}
	public void output(tuple t, BufferedWriter writer){
		try{
			if (t != null){
				writer.write(" (" + t.lhs);
				if (t.c1 == null) {
					writer.write(" " + t.word);
				}else{
					output(t.c1, writer);
					output(t.c2, writer);
				}
				writer.write(")");
				
			}
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}
		
	}
	public void cal_cell(tuple_set top, tuple_set c1, tuple_set c2, ArrayList<String> tagList){
		
		Iterator<Map.Entry<String, tuple>> it1 = c1.tupleSet.entrySet().iterator();
		while(it1.hasNext()){
			Map.Entry<String, tuple> pair1 = it1.next();
			double c1_prob = pair1.getValue().u;
			String l = pair1.getKey();
			if (left_rule_right.containsKey(l)){
				Map<String, Map<String, Double>> head_r_u = left_rule_right.get(l);
				Iterator<Map.Entry<String, Map<String, Double>>> head_r_u_it = head_r_u.entrySet().iterator();
				while(head_r_u_it.hasNext()){
					Map.Entry<String, Map<String, Double>> head_r_pair = head_r_u_it.next();
					String head = head_r_pair.getKey();
					Map<String, Double> r_u = head_r_pair.getValue();
					Iterator<Map.Entry<String, Double>> r_u_it = r_u.entrySet().iterator();
					while(r_u_it.hasNext()){
						
						Map.Entry<String, Double> r_u_pair = r_u_it.next();
						String r = r_u_pair.getKey();
						double u = r_u_pair.getValue();
						if (c2.tupleSet.containsKey(r)){
							double prob = u * c1_prob * c2.tupleSet.get(r).u;
							if (!top.tupleSet.containsKey(head) || top.tupleSet.get(head).u < prob){
								if (!top.tupleSet.containsKey(head)){
									tagList.add(head);
								}
								top.tupleSet.put(head, new tuple(null, head, pair1.getValue(), c2.tupleSet.get(r), prob));
							}
						}
					}
				}
			}
		}
	}
	public void generateTreeSet(String wsj, String tree){
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		FileWriter output = null;
		BufferedWriter writer = null;
		try{
			String line;
			fileReader = new FileReader(wsj);
			bufferedReader = new BufferedReader(fileReader);
			output = new FileWriter(tree);
			writer = new BufferedWriter (output);
			
			while((line = bufferedReader.readLine()) != null){
				String[] words = line.split(" ");
				tuple_set[][] chart = new tuple_set[words.length][words.length];
				for (int l = 0; l < words.length; l ++){
					for (int i = 0; i < words.length - l; i ++){
						int j = i + l;
						tuple_set top = new tuple_set();
						ArrayList<String> tagList = new ArrayList<String>();
						if (l == 0){
							Map<String, Double> bottom_map = left_rule_right_unary.get(words[i]);
							Iterator<Map.Entry<String, Double>> it = bottom_map.entrySet().iterator();
							while(it.hasNext()){
								Map.Entry<String, Double> pair = it.next();
								String key = pair.getKey();
								double value = pair.getValue();
								tuple bottom = new tuple(words[i], key, null, null, value);
								top.tupleSet.put(key, bottom);
								tagList.add(key);
							}
						}else{
							for (int k = i; k < j; k ++){
								cal_cell(top, chart[i][k], chart[k + 1][j], tagList);
							}
						}
						int length = tagList.size();
						for (int index = 0; index < length; index ++){
							String tag = tagList.get(index);
							if (left_rule_right_unary.containsKey(tag)){
								Iterator<Map.Entry<String, Double>> unary_it = left_rule_right_unary.get(tag).entrySet().iterator();
								while(unary_it.hasNext()){
									Map.Entry<String, Double> unary_map = unary_it.next();
									String head = unary_map.getKey();
									double prob = unary_map.getValue() * top.tupleSet.get(tag).u;
									if (!top.tupleSet.containsKey(head) || top.tupleSet.get(head).u < prob){
//										if (!top.tupleSet.containsKey(head)){
											tagList.add(head);
											length = tagList.size();
//										}
										top.tupleSet.put(head, new tuple(null, head, top.tupleSet.get(tag), null, prob));
									}
								}
							}
						}
						chart[i][j] = top;
						
					}
					
				}
				
				int row = 0;
				int col = chart[0].length - 1;
				tuple t = chart[row][col].tupleSet.get("TOP");
				output(t, writer);
				writer.write("\n");
				writer.flush();
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
	public void cal_likelyhood(String blt){
		FileReader inputFile = null;
		BufferedReader reader = null;
		
		try{
			int total = 0;
			String prev = null;
			String line = null;
			Map<String, Double> curMap = new HashMap<String, Double> ();
			inputFile = new FileReader(blt);
			reader = new BufferedReader(inputFile);
			while((line = reader.readLine()) != null){
				
				String[] labels = line.split(" ");
				String lhs = labels[1];
				if (prev == null){
					prev = lhs;
				}
				if (!lhs.equals(prev)){
					Iterator<Map.Entry<String, Double>> it = curMap.entrySet().iterator();
					while(it.hasNext()){
						Map.Entry<String, Double> pair = it.next();
						String curString = pair.getKey();
						curMap.put(curString, pair.getValue() * 1.0 / total);
					}
					likelyhood.put(prev, curMap);
					total = 0;
					curMap = new HashMap<String, Double> ();
					prev = lhs;
				}
				int count = Integer.parseInt(labels[0]);
				total += count;
				String key = labels[3];
				if (labels.length == 5){
					key += " " + labels[4];
				}
				curMap.put(key, (double)count);
			}
			Iterator<Map.Entry<String, Double>> it = curMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String, Double> pair = it.next();
				String curString = pair.getKey();
				curMap.put(curString, pair.getValue() * 1.0 / total);
			}
			likelyhood.put(prev, curMap);
			
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
	
	public void generateTripleMap(){
		Iterator<Map.Entry<String, Map<String, Double>>> it = likelyhood.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Map<String, Double>> pair = it.next();
			String head = pair.getKey();
			Map<String, Double> l_r_u = pair.getValue();
			Iterator<Map.Entry<String, Double>> it2 = l_r_u.entrySet().iterator();
			while(it2.hasNext()){
				Map.Entry<String, Double> pair2 = it2.next();
				String l_r = pair2.getKey();
				double u = pair2.getValue();
				String[] list = l_r.split(" ");
				if (list.length == 1){
					Map<String, Double> unaryMap = new HashMap<String, Double>();
					if (left_rule_right_unary.containsKey(l_r)){
						unaryMap = left_rule_right_unary.get(l_r);
					}
					unaryMap.put(head, u);
					left_rule_right_unary.put(l_r, unaryMap);
				}else if (list.length == 2){
					String l = list[0];
					String r = list[1];
					Map<String, Map<String, Double>> rule_right_prob = new HashMap<String, Map<String, Double>>();
					if (left_rule_right.containsKey(l)){
						rule_right_prob = left_rule_right.get(l);
					}
					Map<String, Double> right_prob = new HashMap<String, Double>();
					if (rule_right_prob.containsKey(head)){
						right_prob = rule_right_prob.get(head);
					}
					right_prob.put(r, u);
					rule_right_prob.put(head, right_prob);
					
					left_rule_right.put(l, rule_right_prob);
				}
			}
		}
	}
}
