package edu.ucop.cdluc3.fileAnalyzer.importer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import gov.nara.nwts.ftapp.ActionResult;
import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.Timer;
import gov.nara.nwts.ftapp.importer.DefaultImporter;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

public class MarcRewriter extends DefaultImporter
{
	// name and description of the Marc Serializer importer
	public String toString()
	{
		return "MARC Rewrite";
	}
	
	public String getDescription()
	{
		return "Rewrite marc records to resolve python parsing issues.";
	}
	
	public String getShortName()
	{
		return "Rewriter";
	}

	
	public MarcRewriter(FTDriver dt)
	{
		super(dt);
	}
	
	
	
	private static enum SerializerStatsItem implements StatsItemEnum
	{
		Key(StatsItem.makeStringStatsItem("Key", 120)),
		Id(StatsItem.makeStringStatsItem("Id", 120)),
		Title(StatsItem.makeStringStatsItem("Title", 300))
		;
		
		StatsItem si;
		
		SerializerStatsItem (StatsItem si)
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
	
	public static StatsItemConfig details = StatsItemConfig.create(SerializerStatsItem.class);
	
	
	// output MARC records as Text
	public void WriteMarcText (String filenamebasetrim, String bibid, Record rec, Stats stat_ser)
	{	
		String filename = filenamebasetrim + "." + bibid + ".txt";
		//stat_ser.setVal(SerializerStatsItem.File_Created, "File Created: " + filename);
		
		try
		{
			File file = new File(filename);
			if (!file.exists()) {file.createNewFile();}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(rec.toString().trim().replaceAll("\n", "\r\n"));
			bw.close();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void WriteMarc (String filenamebasetrim, String bibid, Record rec, Stats stat_ser)
	{	
		String filename = filenamebasetrim + "." + bibid + ".out.mrc";
		//stat_ser.setVal(SerializerStatsItem.File_Created, "File Created: " + filename);
		
		try
		{
			File file = new File(filename);
			if (!file.exists()) {file.createNewFile();}
			MarcStreamWriter bw = new MarcStreamWriter(new FileOutputStream(file));
			bw.write(rec);
			bw.close();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
		
	// file import rules
	public ActionResult importFile(File selectedFile) throws IOException
	{
		Timer timer = new Timer();
		TreeMap<String, Stats> types = new TreeMap<String, Stats>();
		
		String filename_base = selectedFile.getAbsolutePath();
		String filename_base_trim = filename_base.substring(0, filename_base.lastIndexOf("."));
		String filename_out = filename_base.substring(0, filename_base.lastIndexOf(".")) + ".out.mrc";
		
		InputStream in = new FileInputStream(selectedFile);
		MarcReader reader = new MarcPermissiveStreamReader(in, true, true);
		MarcStreamWriter msw = new MarcStreamWriter(new FileOutputStream(filename_out), "utf-8");
			
		int rec = 0;
		while (reader.hasNext())
		{
			rec++;
			Record record = reader.next();
			
			String bib_id = "" + rec;
			
			for(DataField df: record.getDataFields()){
				if (df.getTag().equals("020")) {
					bib_id = df.getSubfield('a').getData();
				}
			}
			String key = String.format("%03d.%s", rec, bib_id);
			Stats stat = Generator.INSTANCE.create(key);
			types.put(stat.key, stat);
			stat.setVal(SerializerStatsItem.Id, bib_id);

			stat.setVal(SerializerStatsItem.Title, "-");
			for(DataField df: record.getDataFields()){
				if (df.getTag().equals("245")) {
					stat.setVal(SerializerStatsItem.Title, df.getSubfield('a').getData());
				}
			}
							
			WriteMarcText(filename_base_trim, key, record, stat);
			WriteMarc(filename_base_trim, key, record, stat);
			msw.write(record);
		
		}  // end of while loop
		msw.close();
		
		return new ActionResult(selectedFile, selectedFile.getName(), this.toString(), details, types, true, timer.getDuration());

	}  // end of ActionResult

}