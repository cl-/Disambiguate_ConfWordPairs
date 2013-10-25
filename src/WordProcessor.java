import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class WordProcessor {
	List<String> stopwords = new ArrayList<String>();
	int[] collocationSizes = new int[]{ 1,2 };
	
	public WordProcessor(String stopWordsFilename, int[]collocationSizes) throws Exception{
		WordProcessor_common_initializer(stopWordsFilename); //use common func coz cannot do a direct call to constructor WordProcessor(stopWordsFilename);
		collocationSizes = Arrays.copyOf(collocationSizes, collocationSizes.length);
	}
	public WordProcessor(String stopWordsFilename) throws Exception{
		WordProcessor_common_initializer(stopWordsFilename);
	}
	public void WordProcessor_common_initializer(String stopWordsFilename) throws Exception{ //must have throw Exception, else cannot do FileReader...
		BufferedReader stopword_br = new BufferedReader(new FileReader(stopWordsFilename));
		Scanner stopword_sc = new Scanner(stopword_br); //Note: Scanner does parsing //http://www.daniweb.com/software-development/java/threads/170265/scanner-or-bufferedreader-
		System.out.println("WordProcessor: Loading Stopwords...");
		while(stopword_sc.hasNext()){
			stopwords.add(stopword_sc.next());
			//System.out.println("Added: "+stopwords.get(stopwords.size()-1));
		} //---------------
		stopword_br = new BufferedReader(new FileReader("stop_punctuations.txt"));
		stopword_sc = new Scanner(stopword_br);
		while(stopword_sc.hasNext()){
			stopwords.add(stopword_sc.next());
		}
		//System.out.println(stopwords.toString());
	}
	
	public IndexedString<String> processSentence(String sentence, int[] cSizes){
		collocationSizes = cSizes; //IMPT: for rapid-testing of varying collocationSizes.
		return processSentence(sentence);
	}
	public IndexedString<String> processSentence(String sentence){
		//Standardize the Sentence -- standardizeSentence();
		//Remove sentence ID. (not doing it here)
		//Remove punctuation and stopwords
		//Convert to lowercase
		//Look for the confusable word
		List<String> currTokens = standardizeSentence(sentence);
		IndexedString<String> currTuple = createIndexedString(currTokens);
		for(int i=0; i<collocationSizes.length; ++i){
			List<Collocation> cList = createCollocationsList(currTuple, collocationSizes[i]);
			currTuple.collocationsList.addAll(cList);
		}
		return currTuple;
	}
	
	
	public List<String> standardizeSentence(String sentence){
		//Note: Beware of stopwords that may also be a target ConfusableWord.
		ArrayList<String>currTokens = new ArrayList<String>();
		String[] tmpTokens = sentence.trim().toLowerCase().split("\t");
		//function removeIgnoredWords(String sentence)
		currTokens.addAll( Arrays.asList(tmpTokens[1].split(" ")) );

		int leftBound = currTokens.indexOf(">>");
		int rightBound = currTokens.indexOf("<<");
		String confusableWord ="";
		if (leftBound == rightBound-2){
			confusableWord = currTokens.get(leftBound+1);}
		//else the ConfusableWord will remain empty. (eg during Test Phase, there is no ConfusableWord indicated.
		
		currTokens.removeAll(stopwords);
		//Do prepend here, so that there's no chance for index to be altered.
		currTokens.add(0, tmpTokens[0]);
		currTokens.add(1, confusableWord);
		return currTokens;
	}
	
	
	public IndexedString<String> createIndexedString(List<String> currTokens){
		IndexedString<String> indexed = new IndexedString<String>(currTokens.get(0), currTokens.get(1) ); //val is specified here already
		currTokens.remove(0); //remove index number string. only have word tokens now.
		currTokens.remove(0); //then remove the ConfusableWord that was prepended in front.
		//indexed.tokensList = currTokens;
		int leftBound = currTokens.indexOf(">>");
		int rightBound = currTokens.indexOf("<<");
		indexed.leftBound = leftBound;
		indexed.lTokensList = currTokens.subList(0, leftBound); //recall: not inclusive of 2nd param.
		indexed.rTokensList = currTokens.subList(rightBound+1, currTokens.size());
//		System.out.println( Integer.toString(indexed.leftBound) + "__" + Integer.toString(indexed.rightBound) );
//		System.out.println( currTokens.toString() );
//		System.out.println( indexed.lTokensList.toString() );
		return indexed;
	}
	
	
	public List<Collocation> createCollocationsList(IndexedString<String> tuple, int cSize){
		List<Collocation> cList = new ArrayList<Collocation>();
		cList.add( createCollocation(tuple,cSize,-1) );
		cList.add( createCollocation(tuple,cSize,1) );
		return cList;
	}
	public Collocation createCollocation(IndexedString<String> tuple, int cSize, int dir){
		List<String> subList = new ArrayList<String>();
		Collocation collocation = new Collocation();
		if (dir<0 && cSize<=tuple.lTokensList.size()){
			int listSize = tuple.lTokensList.size();
			int startPos = listSize-cSize;
			subList = tuple.lTokensList.subList(startPos, listSize);
			collocation =  new Collocation(-cSize, -1, subList);
		}
		else if(dir>0 && cSize<=tuple.rTokensList.size()){
			subList = tuple.rTokensList.subList(0, cSize); //(in real world: 1 to cSize+1), (in code: 0 to cSize)
			collocation =  new Collocation(1, cSize, subList);
		}
		return collocation;
	}

}