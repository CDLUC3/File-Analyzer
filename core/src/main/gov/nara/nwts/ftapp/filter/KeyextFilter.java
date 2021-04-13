package gov.nara.nwts.ftapp.filter;

/**
 * Filter for text files
 * @author TBrady
 *
 */
public class KeyextFilter extends DefaultFileTestFilter {
	public String getSuffix() {
		return ".keyext";
	}
    public String getName(){return "KeyExt";}

}
