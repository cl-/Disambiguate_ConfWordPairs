import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.*;

public class Collocation implements Serializable{ //need Serializable from java.io.* to easily write to file.
	static final long serialVersionUID = 1; //for Serializable version checking
	public int startPos;
	public int endPos;
	public List<String> wordsList = new ArrayList<String>();
	public Collocation(){
	}
	public Collocation(int inputStart, int inputEnd, List<String> wordTokens){
		startPos = inputStart;
		endPos = inputEnd;
		wordsList = new ArrayList<String>(wordTokens); //MUST have "conversion" or Serializable will fail... (probably because reference to another object)			//https://www.google.com.sg/search?q=java+arraylist+not+serializable	//http://stackoverflow.com/questions/1387954/how-to-serialize-a-list-in-java
		if ( Math.abs(endPos+1-startPos) != wordsList.size() ){
			System.out.println("ERROR: wrong Collocation positions:: "+Integer.toString(endPos)+" "+Integer.toString(startPos) );
		}
	}
	public String toString(){
		return Arrays.toString(new int[]{startPos,endPos})+ "  " +" "+wordsList.toString();
	}
}
