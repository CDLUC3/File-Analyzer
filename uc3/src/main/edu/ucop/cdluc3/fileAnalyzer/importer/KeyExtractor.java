package edu.ucop.cdluc3.fileAnalyzer.importer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;

import gov.nara.nwts.ftapp.ActionResult;
import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.Timer;
import gov.nara.nwts.ftapp.YN;
import gov.nara.nwts.ftapp.ftprop.FTPropEnum;
import gov.nara.nwts.ftapp.ftprop.FTPropString;
import gov.nara.nwts.ftapp.importer.DefaultImporter;
import gov.nara.nwts.ftapp.importer.DelimitedFileReader;
import gov.nara.nwts.ftapp.importer.DelimitedFileWriter;
import gov.nara.nwts.ftapp.importer.DelimitedFileImporter.Separator;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

public class KeyExtractor extends DefaultImporter {

	public static final String DELIM = "Delimiter";
	public static final String HEADROW = "HeadRow";
	public static final String KEYCOL = "KeyCol";
	public static final String LABEL = "Label";
	public static final String AUXCOL = "AuxCol";
	public static final String REGEX = "Regex";
	public static final String REPL = "Repl";
	public static final String LOWER = "Lower";

	public KeyExtractor(FTDriver dt) {
		super(dt);
		this.ftprops.add(new FTPropEnum(dt, this.getClass().getName(), DELIM, "delim",
				"Delimiter character separating fields", Separator.values(), Separator.Comma));
		this.ftprops.add(new FTPropEnum(dt, this.getClass().getName(), HEADROW, HEADROW,
				"Treat first row as header", YN.values(), YN.Y));
		this.ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  KEYCOL, KEYCOL,
				"Key Column (start at 1)", "1"));
		this.ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  AUXCOL, AUXCOL,
				"Aux Column (start at 1)", ""));
		this.ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  LABEL, LABEL,
				"Label", "Key"));
		this.ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  REGEX, REGEX,
				"Key Regex", ""));
		this.ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  REPL, REPL,
				"Key Repl", "Key"));
		this.ftprops.add(new FTPropEnum(dt, this.getClass().getName(), LOWER, LOWER,
				"Lower case", YN.values(), YN.N));
	}

	public String toString()
	{
		return "Key Extractor";
	}
	
	public String getDescription()
	{
		return "Extract a record key for comparison purposes";
	}
	
	public String getShortName()
	{
		return "Key";
	}

	
	private static enum KeyExtStatsItem implements StatsItemEnum
	{
		Key(StatsItem.makeStringStatsItem("Key", 120)),
		Label(StatsItem.makeStringStatsItem("Label", 120)),
		Aux(StatsItem.makeStringStatsItem("Aux", 300))
		;
		
		StatsItem si;
		
		KeyExtStatsItem (StatsItem si)
		{
			this.si = si;
		}
		
		public StatsItem si()
		{
			return si;
		}
		
	}
	
	public static enum Generator implements StatsGenerator
	{
		INSTANCE;
		public Stats create(String key)
		{
			return new Stats(details, key);
		}
	}
	
	public static StatsItemConfig details = StatsItemConfig.create(KeyExtStatsItem.class);
	
	public ActionResult importFile(File selectedFile) throws IOException {
		String label = getProperty(LABEL).toString();
		File outfile = new File(selectedFile.getParentFile(), label + ".keyext");
		DelimitedFileWriter dlw = new DelimitedFileWriter(outfile, ",");
		Separator fileSeparator = (Separator)getProperty(DELIM);
		Timer timer = new Timer();

		TreeMap<String,Stats> types = new TreeMap<String,Stats>();
		
		Integer keyCol = 0;
		Integer auxCol = null;
		
		try {
			keyCol = Integer.parseInt(getProperty(KEYCOL).toString()) - 1;
			auxCol = Integer.parseInt(getProperty(AUXCOL).toString()) - 1;
		} catch (Exception e) {
			System.out.println(e);
		}
		
		String regex = getProperty(REGEX).toString();
		String repl = getProperty(REPL).toString();
		
		DelimitedFileReader dfr = new DelimitedFileReader(selectedFile, fileSeparator.separator);
		boolean firstRow = (YN)getProperty(HEADROW) == YN.Y;
		boolean lower = (YN)getProperty(LOWER) == YN.Y;
		
		for(Vector<String> cols = dfr.getRow(); cols != null; cols = dfr.getRow()){
			if (firstRow) {
				firstRow = false;
				continue;
			}
			String key = keyCol < cols.size() ? cols.get(keyCol) : "";
			if (!regex.isEmpty()) {
				key = key.replaceAll(regex, repl);
			}
			if (lower) {
				key = key.toLowerCase();
			}
			String aux = "";
			if (auxCol != null) {
				if (auxCol < cols.size()) {
					aux = cols.get(auxCol);
				}
			}
			Stats stats = KeyExtractor.Generator.INSTANCE.create(key);
			stats.setVal(KeyExtStatsItem.Label, label);
			stats.setVal(KeyExtStatsItem.Aux, aux);
			dlw.writeField(key);
			dlw.writeField(label);
			dlw.writeField(aux, true);
			types.put(stats.key, stats);
		}
		dlw.close();
		
		return new ActionResult(selectedFile, selectedFile.getName(), this.toString(), details, types, true, timer.getDuration());
	}

}
