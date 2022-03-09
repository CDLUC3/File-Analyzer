package edu.ucop.cdluc3.fileAnalyzer.importer;

import gov.nara.nwts.ftapp.ActionResult;
import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.Timer;
import gov.nara.nwts.ftapp.filetest.DefaultFileTest;
import gov.nara.nwts.ftapp.filter.MrcFilter;
import gov.nara.nwts.ftapp.ftprop.FTPropString;
import gov.nara.nwts.ftapp.ftprop.InitializationStatus;
import gov.nara.nwts.ftapp.importer.Importer;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

public class MarcETDLookup extends DefaultFileTest implements Importer
{
    // name and description of the Marc Inventory Importer
    public String toString()
    {
        return "MARC ETD Lookup";
    }
    
    public String getDescription()
    {
        return "The MARC ETD Lookup will augement records with eScholarship URL's";
        
    }
    
    public String getShortName()
    {
        return "ETD Lookup";
    }

    public enum Status {SKIP, PASS, FAIL}
    private HashMap<Status, MarcWriter> files = new HashMap<>();

    // resulting information to display
    private static enum ETDStatsItem implements StatsItemEnum
    {
        RecNum(StatsItem.makeStringStatsItem("RecNum or ISBN", 150)),
        Status(StatsItem.makeEnumStatsItem(Status.class, "Status")),
        Author(StatsItem.makeStringStatsItem("Author", 200)),
        Title(StatsItem.makeStringStatsItem("Title", 300)),
        ESchoLink(StatsItem.makeStringStatsItem("EScholLink", 200)),
        ;
        
        StatsItem si;
        
        ETDStatsItem (StatsItem si)
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

    
    public static StatsItemConfig details = StatsItemConfig.create(ETDStatsItem.class);
            
    private MarcFactory factory;
    public MarcETDLookup(FTDriver dt)
    {
        super(dt);
        factory = MarcFactory.newInstance();
    }

    int ftni = 0;
    
    // file import rules
    public ActionResult importFile(File selectedFile) throws IOException
    {
        Timer timer = new Timer();
        TreeMap<String, Stats> types = new TreeMap<String, Stats>();
        ftni = 0;
        files = new HashMap<>();
        processFile(selectedFile, types);
        
        return new ActionResult(selectedFile, selectedFile.getName(), this.toString(), details, types, true, timer.getDuration());
        
    }

    public void processFile(File selectedFile, TreeMap<String, Stats> types) throws IOException
    {
        String ext = selectedFile.getName().replaceAll("^.*\\.", "");
        String base = selectedFile.getName().replaceAll("\\.[^\\.]*", "");
        InputStream in = new FileInputStream(selectedFile);
        MarcReader reader = new MarcPermissiveStreamReader(in, true, true);
          
        while( reader.hasNext() )
        {
            ftni++;
            Status rstat = Status.SKIP;
            Record record = reader.next();
            
            String key = "";
            DataField df20 = (DataField) record.getVariableField("020");
            
            if (df20 != null) {
                key = df20.getSubfield('a').getData();
            } 
            if (key.isEmpty()) {
                key = "ni" + ftni;
            }

            Stats stat = Generator.INSTANCE.create(key);
            if (ftni == 2) {
                rstat = Status.PASS;
                String url = "https://escholarship.cdlib.org/pretend/url";
                DataField df = factory.newDataField("856", ' ', ' ');
                record.addVariableField(df);
                df.addSubfield(factory.newSubfield('a', url));
                stat.setVal(ETDStatsItem.ESchoLink, url);            
            }
            stat.setVal(ETDStatsItem.Status, rstat);            
            types.put(stat.key, stat);

            DataField df100 = (DataField) record.getVariableField("100");
            if (df100 != null) {
                stat.setVal(ETDStatsItem.Author, df100.getSubfield('a').getData());
            }
            DataField df245 = (DataField) record.getVariableField("245");
            if (df245 != null) {
                stat.setVal(ETDStatsItem.Title, df245.getSubfield('a').getData());
            }
            MarcWriter mw = files.get(rstat);
            if (mw == null) {
                mw = new MarcStreamWriter(new FileOutputStream(new File(selectedFile.getParentFile(), base + "." + rstat.name() + "." + ext)));
                files.put(rstat, mw);
            }
            mw.write(record);
            
        } // end while loop.
        for(MarcWriter mw: files.values()) {
            mw.close();
        }
    }

    @Override
    public Object fileTest(File f) {
        try {
            processFile(f, this.dt.types);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void refineResults() {
        this.dt.types.remove(DEFKEY);
    }
    @Override
    public InitializationStatus initValidate(File refFile) {
        return init();
    }

    @Override
    public boolean allowForceKey() {
        return false;
    }
    
    public void initFilters() {
        filters.add(new MrcFilter());
    }
    public StatsItemConfig getStatsDetails() {
        return MarcETDLookup.details;
    }
    
    public static final String DEFKEY = "";
    public String getKey(File f) {
        return DEFKEY;
    }

}
