
public class SampleView {

	/**
	 * @param args
	 */
	int count=0;
	int count_maxNum=10;
	
	public SampleView(){
	}
	public SampleView(int input_count_maxNum){
		count_maxNum = input_count_maxNum;
	}

	
	public <T> void println(T theMessage){
		if (count<count_maxNum){
			System.out.println(theMessage);
		}//else doNothing
		++count;
	}

}
