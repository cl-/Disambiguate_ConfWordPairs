import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WordClassifier {

	/**
	 * @param args
	 */
	ArrayList probabilityModels;
	List<HashMap<String, Double>> contextWordsProbabilityModel = new ArrayList<HashMap<String, Double>>();
	List<HashMap<Collocation, Double>> collocationsProbabilityModel = new ArrayList<HashMap<Collocation, Double>>();
	double[] probabilityWeights;
	String w1, w2;
	String[] confusableWords;
	
	public WordClassifier(List inputProbabilityModels, double[] inputProbabilityWeights, String _w1, String _w2){
		probabilityModels = new ArrayList(inputProbabilityModels);
		contextWordsProbabilityModel = (List<HashMap<String, Double>>) probabilityModels.get(0);
		collocationsProbabilityModel = (List<HashMap<Collocation, Double>>) probabilityModels.get(1);
		probabilityWeights = inputProbabilityWeights;
		w1 = _w1;  w2 = _w2;
		confusableWords = new String[]{ _w1, _w2 };
	}
	
	
	public String whichConfusableWord(IndexedString<String> currTuple, boolean useLog){
		SampleView spvw = new SampleView(10);
		double[] probability = new double[]{ 1.0, 1.0 }; //probability of w1 and probability of w2
		double[] contextWordsProbability = new double[]{ 1.0, 1.0 };
		double[] collocationsProbability = new double[]{ 1.0, 1.0 };
		
		double[][] rawprob = calcRawProbabilities(currTuple, useLog, false);
		contextWordsProbability = rawprob[0];
		collocationsProbability = rawprob[1];
		if(!useLog){ for (int i=0; i<probability.length; ++i){
			probability[i] = contextWordsProbability[i] * probabilityWeights[0]; spvw.println(probability[i]);
			probability[i] += collocationsProbability[i] * probabilityWeights[1]; spvw.println(probability[i]);
		}}
		if(useLog){ for (int i=0; i<probability.length; ++i){
			probability[i] = contextWordsProbability[i] * probabilityWeights[0]; spvw.println(probability[i]);
			probability[i] += collocationsProbability[i] * probabilityWeights[1]; spvw.println(probability[i]);
			if (probability[i]!=0){
				probability[i] = 1/probability[i]; }
		}}
		return confusableWords[  argMaxIdx(probability[0],probability[1])  ];
	}
	
	
	public double[][] calcRawProbabilities(IndexedString<String> currTuple, boolean useLog, boolean tryPredict){
		//boolean tryPredict:: to allow reuse of calcRawProbabilities inside whatWouldItHaveBeen, such that there's no infinite-loop.
		double startingProbability = 1.001; //so that we can identify the Zero Probabilities, which would remain as 1.001, which is impossible for normal probabilities to reach since Probabilities always sum up to maximum of 1.0
		double[] contextWordsProbability = new double[]{ startingProbability, startingProbability };
		double[] collocationsProbability = new double[]{ startingProbability, startingProbability };
		double[] contextWordsLog = new double[]{ 0, 0 };
		double[] collocationsLog = new double[]{ 0, 0 };

		double prevProbability = startingProbability; //so that we only tryPredict whatWouldItHaveBeen only when prevProbability is different
		for (String word: currTuple.surroundingWords() ){
			if (contextWordsProbabilityModel.get(0).containsKey(word) == false){
				if (tryPredict && prevProbability!=contextWordsProbability[0]){
					whatWouldItHaveBeen(currTuple, contextWordsProbability,0, useLog);
					prevProbability = contextWordsProbability[0];
				}
				continue; //we ignore unknown/new words since it does not help us decide between the ConfusableWords in the ConfusionSet
			}
			//contextWordsProbability[0] *= probabilityModels.get(0).get(0).get(word);
			contextWordsProbability[0] *= contextWordsProbabilityModel.get(0).get(word);
			contextWordsProbability[1] *= contextWordsProbabilityModel.get(1).get(word);
			contextWordsLog[0] += Math.log( contextWordsProbabilityModel.get(0).get(word) );
			contextWordsLog[1] += Math.log( contextWordsProbabilityModel.get(1).get(word) );
		} contextWordsLog[0] *= -1;  contextWordsLog[1] *= -1;
		
		prevProbability = startingProbability;
		int matchedCollocations = 0;
		for (Collocation colloc: currTuple.collocationsList ){
			String collocStr = colloc.toString();
			if (collocationsProbabilityModel.get(0).containsKey(collocStr) == false){
				if (tryPredict && prevProbability!=collocationsProbability[0]){
					whatWouldItHaveBeen(currTuple, contextWordsProbability,1, useLog);
					prevProbability = collocationsProbability[0];
				}
				continue; //we ignore unknown/new words since it does not help us decide between the ConfusableWords in the ConfusionSet
			}else{System.out.print("SS");}
			collocationsProbability[0] *= collocationsProbabilityModel.get(0).get(collocStr);
			collocationsProbability[1] *= collocationsProbabilityModel.get(1).get(collocStr);
			collocationsLog[0] += Math.log( collocationsProbabilityModel.get(0).get(collocStr) );
			collocationsLog[1] += Math.log( collocationsProbabilityModel.get(1).get(collocStr) );
		} collocationsLog[0] *= -1;  collocationsLog[1] *= -1;
		
		double[][] allProbability = new double[2][];
		allProbability[0] = contextWordsProbability;
		allProbability[1] = collocationsProbability;
		double[][] allLog = new double[2][]; allLog[0] = contextWordsLog; allLog[1] = collocationsLog;
		if(useLog){
			allProbability = allLog;}
		for(int i=0; i<allProbability[0].length; ++i){
			if (allProbability[0][i]==startingProbability){
				allProbability[0][i] = 0.0; }
			if (allProbability[1][i]==startingProbability){
				allProbability[1][i] = 0.0; }
			allProbability[0][i] = allProbability[0][i]/startingProbability;
			allProbability[1][i] = allProbability[1][i]/startingProbability;;
		}
		return allProbability;
	}
	
	
	public void whatWouldItHaveBeen(IndexedString<String> currTuple, double[] thisProbabilitySet, int thisIndex, boolean useLog){
		System.out.println("WCLASSIF:: "+currTuple.refId +" [m"+thisIndex+"]: BreakLoop now::"+ Arrays.toString(thisProbabilitySet) );
		double[][] futureProbabilitySets = calcRawProbabilities(currTuple,useLog,false);
		double[] futureProbabilitySet_forThisModel = futureProbabilitySets[thisIndex];
		System.out.println("If we Continue loop, we will have::"+ Arrays.toString(futureProbabilitySet_forThisModel) +"\n" );
	}
	
	
	
	
	public static int argMaxIdx(double[] probabilities){
		return (probabilities[0]>probabilities[1])? 0 : 1;
	}
	public static int argMaxIdx(double probability1, double probability2){
		return (probability1>probability2)? 0 : 1;
	}
	

	
}
