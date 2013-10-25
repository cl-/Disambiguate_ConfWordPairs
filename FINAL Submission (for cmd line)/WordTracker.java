import java.util.*;

public class WordTracker<E>{

	/**
	 * @param args
	 */
	public static int detWordIndex(String w1, String w2, IndexedString<String> tuple){
		int idx = -1;
		if (tuple.val.equals(w1)){ idx=0; }
		else if(tuple.val.equals(w2)) { idx=1; }
		else { System.out.println("WTRACK: WARNING! UNKNOWN WORD VALUE! ##"+tuple.refId+"::"+tuple.val);
		}
		return idx;
	}
	
	public static List<HashMap<String,Integer>> createWordCount(List<IndexedString<String>> tmpDict, String w1, String w2) {
		List<HashMap<String, Integer>> contextWordsDict = new ArrayList<HashMap<String, Integer>>();
		List<HashMap<Collocation, Integer>> collocationsDict = new ArrayList<HashMap<Collocation, Integer>>();
		contextWordsDict.add(new HashMap<String,Integer>());
		contextWordsDict.add(new HashMap<String,Integer>());
		for (IndexedString<String> tuple: tmpDict){
		 int idx = detWordIndex(w1,w2, tuple);
		 if(idx==-1){continue;}
		 for ( String word: tuple.surroundingWords() ){
			if (contextWordsDict.get(idx).containsKey(word)==false){
				contextWordsDict.get(idx).put(word, 1); }
			else{
				int count = contextWordsDict.get(idx).get(word) +1;
				contextWordsDict.get(idx).put(word, count);
			}
		 }
		}
		return contextWordsDict;
	}	
	public static List<HashMap<String,Integer>> createCollocationCount(List<IndexedString<String>> tmpDict, String w1, String w2) {
		List<HashMap<String, Integer>> contextWordsDict = new ArrayList<HashMap<String, Integer>>();
		List<HashMap<String, Integer>> collocationsDict = new ArrayList<HashMap<String, Integer>>();
		collocationsDict.add(new HashMap<String,Integer>());
		collocationsDict.add(new HashMap<String,Integer>());
		for (IndexedString<String> tuple: tmpDict){
		 int idx = -1;
		 if (tuple.val.equals(w1)){ idx=0; }
		 else if(tuple.val.equals(w2)) { idx=1; }
		 for ( Collocation colloc: tuple.collocationsList ){
			String collocStr = colloc.toString();
			if (collocationsDict.get(idx).containsKey(collocStr)==false){
				collocationsDict.get(idx).put(collocStr, 1); }
			else{
				int count = collocationsDict.get(idx).get(collocStr) +1;
				collocationsDict.get(idx).put(collocStr, count);
			}
		 }
		}
		return collocationsDict;
	}
	
	
	
	
	
	public static double useBayesianFormula(double s_count_NormWord_givenConfusableWord, double s_count_AllWords_givenConfWord, double probability_ConfusableWord ){ //uses SmoothedCount. Hence double.
		return (s_count_NormWord_givenConfusableWord / s_count_AllWords_givenConfWord) *probability_ConfusableWord;
	}
	public static double useDirectFormula(double s_count_NormWord_givenConfusableWord, double s_count_NormWords_acrossAllConfusableWords){ //uses SmoothedCount. Hence the variable type is double.
		return (s_count_NormWord_givenConfusableWord / s_count_NormWords_acrossAllConfusableWords);
	}
	
	public static List<HashMap<String, Double>> calcWordBayesian_addXSmoothing(List<HashMap<String,Integer>> contextWordsDict, double[] probability_ConfusableWord, double smoothingParam) {
		//For each Normal Word, we have Probability(confusionWord |normalWord)
		//Get total count (during which we can check if one "confusion" side has a 0 count)
		//Just do add-X smoothing for each "confusion-word"
		HashMap<String, Double> count_ThisWord_acrossAllConfusableWords = new HashMap<String, Double>();	//HashMap<String, Double> contextWordsTotalCount = new HashMap<String, Double>();
		List<HashMap<String, Double>> contextWordsProbability = new ArrayList<HashMap<String, Double>>();
		//We are using 2 different types of formulas. Hence we take size and *2.
		int confusionVocabularySize = contextWordsDict.size();
		for (int i=0; i<confusionVocabularySize*2; ++i){
			contextWordsProbability.add(new HashMap<String,Double>());	}
		double smoothingTotal = smoothingParam *confusionVocabularySize;
		double[] s_count_AllWords_givenConfWord = new double[]{0,0};
		//Iterate through contextWordsDict
		for (HashMap<String,Integer> hashMap: contextWordsDict){
		 for ( String word: hashMap.keySet() ){
			if (count_ThisWord_acrossAllConfusableWords.containsKey(word)==false){ //so that we only go through one time.
				//Must be unique one time because we are counting s_count_AllWords_givenConfWord. Also because we dont want to do repeated redundant work.
				double count1 = contextWordsDict.get(0).containsKey(word)?  smoothingParam + contextWordsDict.get(0).get(word):   smoothingParam;
				double count2 = contextWordsDict.get(1).containsKey(word)?  smoothingParam + contextWordsDict.get(1).get(word):   smoothingParam;
				s_count_AllWords_givenConfWord[0] += count1;
				s_count_AllWords_givenConfWord[1] += count2;
				double count_thisWord_total = count1+count2-smoothingParam;
				count_ThisWord_acrossAllConfusableWords.put(word, count_thisWord_total);
				contextWordsProbability.get(2).put(word, useDirectFormula(count1, count_thisWord_total) );
				contextWordsProbability.get(3).put(word, useDirectFormula(count2, count_thisWord_total) );
			}
			else{ //doNothing
			}
		 }
		}
		for (HashMap<String,Integer> hashMap: contextWordsDict){
		 for ( String word: hashMap.keySet() ){
			if (contextWordsProbability.get(0).containsKey(word)==false){ //so that we only go through one time.
				//coz we dont want to do repeated redundant work.
				double count1 = contextWordsDict.get(0).containsKey(word)?  smoothingParam + contextWordsDict.get(0).get(word):   smoothingParam;
				double count2 = contextWordsDict.get(1).containsKey(word)?  smoothingParam + contextWordsDict.get(1).get(word):   smoothingParam;
				contextWordsProbability.get(0).put(word, useBayesianFormula(count1, (s_count_AllWords_givenConfWord[0]+s_count_AllWords_givenConfWord[1]), probability_ConfusableWord[0]) );
				contextWordsProbability.get(1).put(word, useBayesianFormula(count2, (s_count_AllWords_givenConfWord[0]+s_count_AllWords_givenConfWord[1]), probability_ConfusableWord[1]) );
			}
		 }
		}
		return contextWordsProbability;
	}
	public static List<HashMap<String, Double>> calcCollocationBayesian_addXSmoothing(List<HashMap<String,Integer>> collocationsDict, double[] probability_ConfusableWord, double smoothingParam) {
		//For each Normal Word, we have Probability(confusionWord |normalWord)
		//Get total count (during which we can check if one "confusion" side has a 0 count)
		//Just do add-X smoothing for each "confusion-word"
		HashMap<String, Double> count_ThisWord_acrossAllConfusableWords = new HashMap<String, Double>();	//HashMap<String, Double> contextWordsTotalCount = new HashMap<String, Double>();
		List<HashMap<String, Double>> collocationsProbability = new ArrayList<HashMap<String, Double>>();
		//We are using 2 different types of formulas. Hence we take size and *2.
		int confusionVocabularySize = collocationsDict.size();
		for (int i=0; i<confusionVocabularySize*2; ++i){
			collocationsProbability.add(new HashMap<String,Double>());	}
		double smoothingTotal = smoothingParam *confusionVocabularySize;
		double[] s_count_AllWords_givenConfWord = new double[]{0,0};
		//Iterate through contextWordsDict
		for (HashMap<String,Integer> hashMap: collocationsDict){
		 for ( String word: hashMap.keySet() ){
			if (count_ThisWord_acrossAllConfusableWords.containsKey(word)==false){ //so that we only go through one time.
				//Must be unique one time because we are counting s_count_AllWords_givenConfWord. Also because we dont want to do repeated redundant work.
				double count1 = collocationsDict.get(0).containsKey(word)?  smoothingParam + collocationsDict.get(0).get(word):   smoothingParam;
				double count2 = collocationsDict.get(1).containsKey(word)?  smoothingParam + collocationsDict.get(1).get(word):   smoothingParam;
				s_count_AllWords_givenConfWord[0] += count1;
				s_count_AllWords_givenConfWord[1] += count2;
				double count_thisWord_total = count1+count2;
				count_ThisWord_acrossAllConfusableWords.put(word, count_thisWord_total);
				collocationsProbability.get(2).put(word, useDirectFormula(count1, count_thisWord_total) );
				collocationsProbability.get(3).put(word, useDirectFormula(count2, count_thisWord_total) );
			}
			else{ //doNothing
			}
		 }
		}
		for (HashMap<String,Integer> hashMap: collocationsDict){
		 for ( String word: hashMap.keySet() ){
			if (collocationsProbability.get(0).containsKey(word)==false){ //so that we only go through one time.
				//coz we dont want to do repeated redundant work.
				double count1 = collocationsDict.get(0).containsKey(word)?  smoothingParam + collocationsDict.get(0).get(word):   smoothingParam;
				double count2 = collocationsDict.get(1).containsKey(word)?  smoothingParam + collocationsDict.get(1).get(word):   smoothingParam;
				collocationsProbability.get(0).put(word, useBayesianFormula(count1, s_count_AllWords_givenConfWord[0], probability_ConfusableWord[0]) );
				collocationsProbability.get(1).put(word, useBayesianFormula(count2, s_count_AllWords_givenConfWord[1], probability_ConfusableWord[1]) );
			}
		 }
		}
		return collocationsProbability;
	}
	


	
	
	public static List<HashMap<String, Double>> calcWordProbability_addXSmoothing(List<HashMap<String,Integer>> contextWordsDict, double smoothingParam) {
		//For each Normal Word, we have Probability(confusionWord |normalWord)
		//Get total count (during which we can check if one "confusion" side has a 0 count)
		//Just do add-X smoothing for each "confusion-word"
		int confusionVocabularySize = contextWordsDict.size();
		double smoothingTotal = smoothingParam *confusionVocabularySize;
		HashMap<String, Double> contextWordsTotalCount = new HashMap<String, Double>();
		List<HashMap<String, Double>> contextWordsProbability = new ArrayList<HashMap<String, Double>>();
		
		contextWordsProbability.add(new HashMap<String,Double>());
		contextWordsProbability.add(new HashMap<String,Double>());
		//Iterate through contextWordsDict
		for (HashMap<String,Integer> hashMap: contextWordsDict){
		 for ( String word: hashMap.keySet() ){
			if (contextWordsTotalCount.containsKey(word)==false){
				double count1 = contextWordsDict.get(0).containsKey(word)?  smoothingParam + contextWordsDict.get(0).get(word):   smoothingParam;
				double count2 = contextWordsDict.get(1).containsKey(word)?  smoothingParam + contextWordsDict.get(1).get(word):   smoothingParam;
				double currTotalCount = count1 + count2;
				contextWordsTotalCount.put(word, currTotalCount);
				contextWordsProbability.get(0).put(word, (count1/currTotalCount) );
				contextWordsProbability.get(1).put(word, (count2/currTotalCount) );
			}
			else{ //doNothing
			}
		 }
		}
		return contextWordsProbability;
	}
	public static List<HashMap<String, Double>> collocationProbability_addXSmoothing(List<HashMap<String,Integer>> collocationsDict, double smoothingParam) {
		//For each Normal Word, we have Probability(confusionWord |normalWord)
		//Get total count (during which we can check if one "confusion" side has a 0 count)
		//Just do add-X smoothing for each "confusion-word"
		int confusionVocabularySize = collocationsDict.size();
		double smoothingTotal = smoothingParam *confusionVocabularySize;
		HashMap<String, Double> collocationsTotalCount = new HashMap<String, Double>();
		List<HashMap<String, Double>> collocationsProbability = new ArrayList<HashMap<String, Double>>();

		collocationsProbability.add(new HashMap<String,Double>());
		collocationsProbability.add(new HashMap<String,Double>());
		//Iterate through contextWordsDict
		for (HashMap<String,Integer> hashMap: collocationsDict){
		 for ( String colloc: hashMap.keySet() ){
			if (collocationsTotalCount.containsKey(colloc)==false){
				double count1 = collocationsDict.get(0).containsKey(colloc)?  smoothingParam + collocationsDict.get(0).get(colloc):   smoothingParam;
				double count2 = collocationsDict.get(1).containsKey(colloc)?  smoothingParam + collocationsDict.get(1).get(colloc):   smoothingParam;
				double currTotalCount = count1 + count2;
				collocationsTotalCount.put(colloc, currTotalCount);
				collocationsProbability.get(0).put(colloc.toString(), (count1/currTotalCount) );
				collocationsProbability.get(1).put(colloc.toString(), (count2/currTotalCount) );
			}
			else{ //doNothing
			}
		 }
		}
		return collocationsProbability;
	}
	

}
