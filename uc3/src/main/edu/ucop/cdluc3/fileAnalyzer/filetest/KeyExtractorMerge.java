package edu.ucop.cdluc3.fileAnalyzer.filetest;

import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.filetest.DefaultFileTest;
import gov.nara.nwts.ftapp.filter.KeyextFilter;
import gov.nara.nwts.ftapp.ftprop.InitializationStatus;
import gov.nara.nwts.ftapp.importer.DelimitedFileReader;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;


public class KeyExtractorMerge extends DefaultFileTest {
	private TreeMap<String, StatsItemEnum> labels = new TreeMap<>();
    public String toString()
    {
        return "Key Extractor Merge";
    }
    
    public String getDescription()
    {
        return "Merge files created by the Key Extractor Importer";
        
    }
    
    public String getShortName()
    {
        return "KeyMerge";
    }

    
    // resulting information to display
    private static enum KeyMergeStatsItems implements StatsItemEnum
    {
        Key(StatsItem.makeStringStatsItem("Key", 200)),
        Count(StatsItem.makeIntStatsItem("Count").makeFilter(true)),
        AUX1(StatsItem.makeStringStatsItem("Aux1",100).setInitVal("")),
        AUX2(StatsItem.makeStringStatsItem("Aux2",100).setInitVal("")),
        AUX3(StatsItem.makeStringStatsItem("Aux3",100).setInitVal("")),
        AUX4(StatsItem.makeStringStatsItem("Aux4",100).setInitVal("")),
        ;
        
        StatsItem si;
        
        KeyMergeStatsItems (StatsItem si)
        {
            this.si = si;
        }
        
        public StatsItem si()
        {
            return si;
        }
    }
    
    public StatsItemEnum getLabelSI(String label) {
    	StatsItemEnum si = labels.get(label);
    	if (si == null) {
    		int sinum = labels.size() + 1;
			if (sinum == 1) {
				si = KeyMergeStatsItems.AUX1;
			} else if (sinum == 2) {
				si = KeyMergeStatsItems.AUX2;
			} else if (sinum == 3) {
				si = KeyMergeStatsItems.AUX3;
			} else if (sinum == 4) {
				si = KeyMergeStatsItems.AUX4;
			} 
    		si.si().setHeader(label);
    		labels.put(label, si);
    	}
    	return si;
    }
        
    public static enum Generator implements StatsGenerator
    {
        INSTANCE;
        public Stats create(String key)
        {
            return new Stats(details, key);
        }
    }

    
    public static StatsItemConfig details = StatsItemConfig.create(KeyMergeStatsItems.class);
    
    public KeyExtractorMerge(FTDriver dt)
    {
        super(dt);
    }

    @Override public InitializationStatus init() {
        InitializationStatus istat = super.init();
        labels = new TreeMap<>();
        return istat;
    }
    
    public void processFile(File selectedFile, TreeMap<String, Stats> types) throws IOException
    {
        DelimitedFileReader.parseFile(selectedFile, ",");
    }

    @Override
    public Object fileTest(File f) {
    	try {
			Vector<Vector<String>> data = DelimitedFileReader.parseFile(f, ",");
			for(Vector<String>row: data) {
				String key = row.get(0);
				Stats stats = getStats(key);		
				String label = row.get(1).intern();
				String aux = row.get(2);
				StatsItemEnum si = getLabelSI(label);
				stats.setVal(si, aux);
				stats.sumVal(KeyMergeStatsItems.Count, 1);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        return null;
    }

    @Override
    public void refineResults() {
        this.dt.types.remove(DEFKEY);
		getStatsDetails().createFilters(this.dt.types);
    }
    
    public void initFilters() {
        filters.add(new KeyextFilter());
    }
    public StatsItemConfig getStatsDetails() {
        return KeyExtractorMerge.details;
    }
    
    public static final String DEFKEY = "";
    public String getKey(File f) {
        return DEFKEY;
    }
    public Stats createStats(String key){ 
    	return Generator.INSTANCE.create(key);
    }
    
}
