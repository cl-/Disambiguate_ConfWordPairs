import java.util.*; //String, Scanner
import java.io.*; //File
public class sctrain { //Having both train and test code in this file. Delete and separate later.
	/**
	 * @param args
	 *
	 * Bayesian classifier for context-sensitive spelling correction
	 * making use of the naive Bayes assumption
	 * supervised learning task from labeled training sentences
	 */
	
	public static void main(String[] args) throws Exception { //... Must have for FileReader, BufferedReader...sml. Checked Exception: http://stackoverflow.com/questions/6488339/using-filereader-causes-a-compiler-error-unhandled-exception-type-filenotfounde
		//Command Train: java sctrain word1 word2 train_file model_file
		//Command Test: java sctest word1 word2 test_file model_file answer_file

		//CODE-RUN CONFIGURATIONS
		int commandSource = 2; //SUBMISSION: change to 2
		int devPhase = 1; //Phases: {1:Training, 2:Testing, 3:Training&Testing}	

		//DATA-MANIP CONFIGURATIONS
		int currSetNum = 0;
		int[] collocationSizes = new int[]{ 2, 3 };
		//int[] collocationSizes = new int[]{ 1, 2 };
		double[] smoothingParams = new double[]{0.1};
		//double[] smoothingParams = new double[]{0.5, 1};
		double[] probabilityWeights = new double[]{ 10.0, 0.01 }; //contextWordProbability, collocationProbability
		boolean useLog = true;
		
		
		//Define the variables, the Confusion Set pair.
		String x = "x", stopWordsFilename="stopwd.txt",
			trainingDataFilename=x, learnedModelFilename=x, learnedModelFilename_machine=x,
			testDataFilename=x, correctAnswersFilename=x, calculatedAnswersFilename=x;
		String w1="", w2=""; //The Confusion Set.
		
		//Data Structure: WordProcessor -> IndexedString -> List<Collocation>
		WordProcessor wprocessor = new WordProcessor(stopWordsFilename, collocationSizes);
		List<IndexedString<String>> tmpDict = new ArrayList<IndexedString<String>>(); //is List<IndexedString> which each contains List<Collocations>
		List<HashMap<String, Integer>> contextWordsDict = new ArrayList<HashMap<String, Integer>>();
		List<HashMap<String, Integer>> collocationsDict = new ArrayList<HashMap<String, Integer>>();
		int[] count_ConfusableWord = new int[]{0,0};
		
		double[] probability_ConfusableWord = new double[2];
		List<HashMap<String, Double>> contextWordsProbability = new ArrayList<HashMap<String, Double>>();
		List<HashMap<String, Double>> collocationsProbability = new ArrayList<HashMap<String, Double>>();
		
		String[][] confusionSets = new String[][]{
				{"adapt","adopt"}, {"formally","formerly"}, {"raise","rise"} };
		//List<List<String>> confusionSets = new ArrayList<ArrayList<String>>();
		
		if (commandSource==1){
			w1 = confusionSets[currSetNum][0]; w2 = confusionSets[currSetNum][1];;
			String baseStringName = w1+'_'+w2;
			trainingDataFilename = baseStringName+".train";
			learnedModelFilename = baseStringName+".learnedModel";
			learnedModelFilename_machine = baseStringName+".learnedModel_machine";
			testDataFilename = baseStringName+".test";
			correctAnswersFilename = baseStringName+".answer";
			calculatedAnswersFilename = baseStringName+".answer_Calculated";
		}
		else if (commandSource==2){ //Assume the lecturer will input correctly during batch processing. Not doing defensive programming.
			//Scanner scanner = new Scanner(System.in);
			w1 = args[0]; //w1 = scanner.next().toLowerCase();
			w2 = args[1]; //w2 = scanner.next().toLowerCase();
			if ( allow(devPhase,1) ){
				trainingDataFilename = args[2]; //scanner.next();
				learnedModelFilename = args[3]; //scanner.next();
				learnedModelFilename_machine = learnedModelFilename+ "_machine";
			}else if( allow(devPhase,2) ){
				//String temp = scanner.next();
				learnedModelFilename = args[3]; //scanner.next();
				learnedModelFilename_machine = learnedModelFilename+ "_machine";
				testDataFilename = args[2]; //temp;
				calculatedAnswersFilename = args[4]; //scanner.next();
				if (args.length>5){
					correctAnswersFilename = args[5]; }
			}
		}//endof commandSource 2
		
		
		//Both devPhase 1 n 2
		//Read the training/test data
		System.out.println(System.getProperty("user.dir"));
		BufferedReader train_br = new BufferedReader(new FileReader(trainingDataFilename));
		Scanner train_sc = new Scanner(train_br); //Note: Scanner does parsing //http://www.daniweb.com/software-development/java/threads/170265/scanner-or-bufferedreader-
		//System.out.println(train_sc.hasNext()); //TESTING IF FILE IS FOUND AND HAS NEXT.
		//BufferedReader model_br = new BufferedReader(new FileReader(learnedModelFilename));
		//Scanner model_sc = new Scanner(model_br);
		BufferedReader test_br = new BufferedReader(new FileReader(testDataFilename));
		Scanner test_sc = new Scanner(test_br);
		//System.out.println("Test_sc: "+test_sc.hasNext()); //TESTING IF FILE IS FOUND AND HAS NEXT.
		BufferedReader correctAnswers_br = new BufferedReader(new FileReader(correctAnswersFilename));
		Scanner correctAnswers_sc = new Scanner(correctAnswers_br);
		
	if ( allow(devPhase,1) ){
		System.out.println();
		//===Bayesian Classifier - Naive Bayes assumption===
		//Standardize the Sentence
		//Remove sentence ID.
		//Remove punctuation and stopwords
		//Convert to lowercase
		//Look for the confusable word
		while (train_sc.hasNextLine()){
//			System.out.println(train_sc.nextLine()); i++;
			IndexedString<String> currTuple = wprocessor.processSentence(train_sc.nextLine());
			tmpDict.add(currTuple);
			int idx = WordTracker.detWordIndex(w1,w2, currTuple);
			if(idx==-1){continue;}
			count_ConfusableWord[idx] += 1;
			//break;
		} //System.out.println(i);
		
		//Count the words and the sequence
		//(Use a pair of offsets)
		contextWordsDict = WordTracker.createWordCount(tmpDict, w1, w2);
		collocationsDict = WordTracker.createCollocationCount(tmpDict, w1, w2);

		//Calculate Probabilities
		//with Smoothing
		int count_AllConfusable = count_ConfusableWord[0] + count_ConfusableWord[1]; //Checkpoint: this should be equal to the number of sentences
		probability_ConfusableWord[0] = 1.0* count_ConfusableWord[0]/count_AllConfusable;
		probability_ConfusableWord[1] = 1.0* count_ConfusableWord[1]/count_AllConfusable;
		for (double smParam: smoothingParams){
			//contextWordsProbability = WordTracker.calcWordProbability_addXSmoothing(contextWordsDict, smParam);
			//collocationsProbability = WordTracker.collocationProbability_addXSmoothing(collocationsDict, smParam);
			contextWordsProbability = WordTracker.calcWordBayesian_addXSmoothing(contextWordsDict, probability_ConfusableWord, smParam);
			collocationsProbability = WordTracker.calcCollocationBayesian_addXSmoothing(collocationsDict, probability_ConfusableWord, smParam);
		}
		
		//Write to MODEL file
		try{
			File targetFile = new File(learnedModelFilename_machine);
			if(!targetFile.exists()) {
			    targetFile.createNewFile();
			}
			FileOutputStream fileOut = new FileOutputStream(learnedModelFilename_machine, false); //set "append" to false
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			//out.writeObject(contextWordsProbability);
			//out.writeObject(collocationsProbability);
			List combinedList = new ArrayList();
			combinedList.add(contextWordsProbability);
			combinedList.add(collocationsProbability);
			//System.out.println(combinedList.get(1).toString());
			out.writeObject(combinedList);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in "+learnedModelFilename_machine+"\n");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}//endof try-catch
		
		File targetFile = new File(learnedModelFilename);
		if(!targetFile.exists()) {
		    targetFile.createNewFile();
		}
		PrintWriter writer = new PrintWriter(learnedModelFilename, "UTF-8");
		for (HashMap<String,Double> hashMap: contextWordsProbability){
		 for ( String word: hashMap.keySet() ){
			writer.println(word +"\t\t"+ hashMap.get(word)); //System.out.println(word +"\t\t"+ hashMap.get(word));
		 }
		}
		for (HashMap<String,Double> hashMap: collocationsProbability){
		 for ( String colloc: hashMap.keySet() ){
			//writer.println(colloc.wordsList.toString() +"\t\t"+ hashMap.get(colloc)); //System.out.println(colloc.wordsList.toString() +"\t\t"+ hashMap.get(colloc));
			writer.println(colloc +"\t\t"+ hashMap.get(colloc)); //System.out.println(colloc +"\t\t"+ hashMap.get(colloc));
		 }
		}
		System.out.printf("Human readable data is saved in "+learnedModelFilename+"\n");
		writer.close();
		writer = new PrintWriter("x", "UTF-8");
		writer.println(" ");
		writer.close();
	}//endof devPhase 1
	
	
	
	if ( allow(devPhase,2) ){
		System.out.println();
		//Read the Model
		List allProbabilityModels = new ArrayList();
		contextWordsProbability = new ArrayList<HashMap<String, Double>>();
		collocationsProbability = new ArrayList<HashMap<String, Double>>();
		
		boolean sameDataType = true;
		//while (sameDataType){
		 try{
			System.out.printf("Serialized data is read from "+learnedModelFilename_machine+"\n");
			FileInputStream fileIn = new FileInputStream(learnedModelFilename_machine);
			ObjectInputStream in_ = new ObjectInputStream(fileIn);
			List combinedList = (ArrayList) in_.readObject();
			//System.out.println(combinedList.toString());
			allProbabilityModels = new ArrayList(combinedList);
			contextWordsProbability = (List<HashMap<String, Double>>) combinedList.get(0);
			collocationsProbability = (List<HashMap<String, Double>>) combinedList.get(1);
			in_.close();
			fileIn.close();
		 }catch(IOException ioe){
			ioe.printStackTrace();
			sameDataType = false;
		 }//endof try-catch
		//}
		
		for (HashMap<String,Double> hashMap: contextWordsProbability){
		  for ( String word: hashMap.keySet() ){
			//System.out.println("@@ "+word +"\t\t"+ hashMap.get(word));
		  }
		}
		for (HashMap<String,Double> hashMap: collocationsProbability){
		 for ( String colloc: hashMap.keySet() ){
			//System.out.println("@@ "+colloc +"\t\t"+ hashMap.get(colloc)); //System.out.println("@@ "+colloc.wordsList.toString() +"\t\t"+ hashMap.get(colloc));
		 }
		}
		
		//Calculate Probabilities
		//Write Answer to File
		WordClassifier wclassifier = new WordClassifier(allProbabilityModels, probabilityWeights, w1, w2);
		Map<String,String> calculatedAnswers = new HashMap<String,String>();		

		PrintWriter writer = new PrintWriter(calculatedAnswersFilename, "UTF-8");
		while (test_sc.hasNextLine()){
			IndexedString<String> currTuple = wprocessor.processSentence(test_sc.nextLine());
			//String confusableWord = WordClassifier.whichConfusableWord(allProbabilityModels, probabilityWeights, w1, w2, currTuple);
			String confusableWord = wclassifier.whichConfusableWord(currTuple, useLog);
			calculatedAnswers.put(currTuple.refId, confusableWord);
			writer.println(currTuple.refId +" "+ confusableWord);
			//break;
		}
		System.out.println();
		System.out.println("Calculated answers are saved in "+calculatedAnswersFilename+"\n");
		writer.close();
		writer = new PrintWriter("x", "UTF-8");
		writer.println(" ");
		writer.close();
		
		if(correctAnswersFilename.equals(x)==false){
			double score_maxNum = 0, score = 0;
			int numLines=0;
			while (correctAnswers_sc.hasNextLine()){
				++numLines;
				String[] ansTokens = correctAnswers_sc.nextLine().trim().toLowerCase().split(" ");
				//System.out.println("SCTRAIN:: " +Arrays.toString(ansTokens));
				if (calculatedAnswers.containsKey(ansTokens[0]) == false){
					System.out.println("That's weird... ##"+ansTokens[0]+" does not exist in the Test Data Set but is in the list of Correct Answers." );
					continue;
				}
				++score_maxNum;
				if ( calculatedAnswers.get(ansTokens[0]).equals(ansTokens[1]) ){
					++score; }
				else{
					System.out.println("Wrong classification:: " +ansTokens[0] +"\t(wrong):" +calculatedAnswers.get(ansTokens[0]) +"\t(cor):"+ansTokens[1]  );
				}
			} System.out.println("SCTRAIN: CorrectAnswer has: "+numLines+ " lines in it.");
			System.out.println("FINAL SCORE::" +(score/score_maxNum)+ "\tMax::" +score_maxNum+ "\tCorrect::" +score  );
		}
		
	}//endof devPhase 2
	
	train_br.close(); test_br.close(); correctAnswers_br.close();  //model_br.close();
	}//endof main
	

	public static boolean allow(int currDevPhase, int allowedDevPhase){
		return (currDevPhase==allowedDevPhase || currDevPhase==3);
	}
	public static boolean allow(int currDevPhase, int[] allowedDevPhases){
		//return (currDevPhase==num || devPhase==3);
		for (int allowedphase: allowedDevPhases){
			if (currDevPhase==allowedphase){
				return true;
			}
		}//else
		return false;
	}

	

	
	
}
