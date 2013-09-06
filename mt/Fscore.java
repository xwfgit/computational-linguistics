package mt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Fscore {
	public static void main(String[] args){
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
			originFileReader = new FileReader(args[1]);
			outputFileReader = new FileReader(args[0]);
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
