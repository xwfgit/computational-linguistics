package sequenceLabeling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Score {
	public static void main(String[] args){
		FileReader origin = null;
		BufferedReader originReader = null;
		FileReader mytag = null;
		BufferedReader mytagReader = null;
		try{
			int total = 0;
			int correct = 0;
			String originline;
			String mytagline;
			origin = new FileReader(args[0]);
			originReader = new BufferedReader(origin);
			mytag = new FileReader(args[1]);
			mytagReader = new BufferedReader(mytag);
			while((originline = originReader.readLine()) != null && (mytagline = mytagReader.readLine()) != null){
				String[] originline_list = originline.split(" ");
				String[] mytagline_list = mytagline.split(" ");
				if (originline_list.length != mytagline_list.length){
					System.out.println(originline);
					System.out.println(mytagline);
				}
				for (int i = 0; i < originline_list.length; i += 2){
					if (originline_list[i + 1].equals(mytagline_list[i + 1])){
						correct ++;
					}
					total ++;
				}
			}
			System.out.println(correct * 1.0 / total);
		}catch(FileNotFoundException e){
			throw new RuntimeException("File not found");
		}catch(IOException e){
			throw new RuntimeException("IO Error occured");
		}finally{
			if (origin != null){
				try{
					origin.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			
			if (originReader != null){
				try{
					originReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (mytag != null){
				try{
					mytag.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if (mytagReader != null){
				try{
					mytagReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}
}
