import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IndexedString<E> implements Serializable{ //need Serializable from java.io.* to easily write to file.
	public E refId; //sentence id
	public String val; //the correct disambiguated word from the Confusable Words Set
	public List<String> lTokensList = new ArrayList<String>();
	public List<String> rTokensList = new ArrayList<String>();
	public int leftBound, rightBound;
	public List<Collocation> collocationsList = new ArrayList<Collocation>();

	public IndexedString(E stringId, String string){
		refId = stringId;
		val = string;
	}
	
	public static List<String> splitTokens(List<String> currTokens){
		return currTokens;
	}

	public List<String> surroundingWords(){
		List<String> combinedList = new ArrayList<String>();
		for (String str: lTokensList){
			combinedList.add(str);
		}
		for (String str: rTokensList){
			combinedList.add(str);
		}
		//System.out.println(combinedList.toString());
		return combinedList;
	}
}	
