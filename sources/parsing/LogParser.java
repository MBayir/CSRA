package parsing;

import java.util.Date;

import utils.Utils;

/**
 * Parses individual access log file.
 */
public class LogParser {
	
	public static String EXTERNAL_PLACEHOLDER = "****1";
	
	/**
	 * Domain name like cnn.com.
	 */
	private String domainName;
	
	public LogParser(String domainName) {
		this.domainName = domainName;
	}
	
	/**
	 * Gets the accessed url in indivual access record.
	 * 
	 * @param logRecord is the indivual log record.
	 */
	public String getUrl(String logRecord){
		int firstIndex = logRecord.indexOf("\"");
		int secondIndex = logRecord.indexOf("\"",(firstIndex+1));
		StringBuilder urlBuilder = new StringBuilder("");
		if (firstIndex != -1 && secondIndex != -1) {
			String tempString = logRecord.substring((firstIndex+1),secondIndex);
			String[] stringArray = tempString.split(" ");
			if(stringArray.length >= 2) {
				for (int i = 1; i < (stringArray.length - 1); i++) {
					urlBuilder.append(stringArray[i]);
				}
			}
		}
		return urlBuilder.toString().length() > 0 ? urlBuilder.toString() : null;
	}

	/**
	 * Get access time in unix time miliseconds format.
	 * 
	 * @param recordLine
	 * @return page visit time in miliseconds.
	 */
	public long getAccessTime(String recordLine){
		int index_first = recordLine.indexOf("[");
		int index_second = recordLine.indexOf("]");
		String tempString = recordLine.substring((index_first+1),index_second);
		String[] stringArray = tempString.split(" ");
		String timeString = null;
		//01/Apr/2008:03:05:14
		if(stringArray.length > 0) {
			timeString = stringArray[0];
			int firstSlash  	= timeString.indexOf("/");
			int secondSlash 	= timeString.indexOf("/",(firstSlash+1));
			int firtColumn  	= timeString.indexOf(":");
			int secondColumn 	= timeString.indexOf(":",(firtColumn+1));
			int thirdColumn		= timeString.indexOf(":",(secondColumn+1));
		
			int date  = Integer.parseInt(timeString.substring(0,firstSlash));
			int month = Utils.returnNumericMonth(timeString.substring((firstSlash+1),secondSlash));
			int year  = Integer.parseInt(timeString.substring((secondSlash+1), firtColumn));
			int hour  = Integer.parseInt(timeString.substring((firtColumn+1), secondColumn));
			int min	  = Integer.parseInt(timeString.substring((secondColumn+1), thirdColumn));
			int sec	  = Integer.parseInt(timeString.substring((thirdColumn+1), timeString.length()));
			Date objDate = new Date(year,month,date,hour,min);
			long minutes = (objDate.getTime()/(((1000))*60));
			return minutes;
		}
			return -1;
	}

	public String getIPNumber(String recordLine){
		String[] elements = recordLine.split("-");
		if(elements.length > 0) {
			return elements[0].substring(0,elements[0].length()-1);
		} else {
			return null;
		}
	}

	public String getRawUrl(String inputString){
		if(inputString.indexOf("?") != -1) {
			return inputString.substring(0, inputString.indexOf("?"));
		} else {
			return inputString;
		}
	}

	public String getReferrerField(String recordLine){
		int firstQute 	= recordLine.indexOf("\"");
		int secondQute 	= recordLine.indexOf("\"",(firstQute+1));
		int thirdQute	= recordLine.indexOf("\"",(secondQute+1));
		int fourthQute	= recordLine.indexOf("\"",(thirdQute+1));
		
		String tempString = recordLine.substring((thirdQute+1),fourthQute);
		if(tempString != null) {
			return tempString;
		} else {
			return null;
		}
	}
	
	public String omitExternal(String input){
		if (input.indexOf(domainName) == -1) {
			return EXTERNAL_PLACEHOLDER;
		}	
		int start = input.indexOf(domainName);
		if (input.length() > (start + domainName.length()))
		{
			if (input.charAt(start + domainName.length()) == '/')
			{
				start += 1;
			}
		}
		
		String result = input.substring(start + domainName.length());
		
		if(result.length() < 1) {
			result="/";
		}
		return result;
	}
}
