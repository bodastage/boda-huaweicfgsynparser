/*
 * Huawei bulk CM XML baseline syn backup data file parser.
 */
package com.bodastage.boda_huaweicfgsynparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

/**
 *
 * @author Bodastage <info@bodastage.com>
 * @version 1.1.0
 */
public class HuaweiCfgSynParser {

    static Logger logger = LoggerFactory.getLogger(HuaweiCfgSynParser.class);

    final static String VERSION = "1.3.0";

    /**
     * Mark that we are in a class tag.
     *
     * @since 1.0.0
     */
    private boolean inClass = false;

    /**
     * Marks when we are inside an attribute tag.
     *
     * @since 1.0.0
     */
    private boolean inAttributes = false;

    /**
     * Marks when we are inside an MO tag.
     *
     * @sice 1.0.0
     */
    private boolean inMO = false;

    /**
     * The base file name of the file being parsed.
     *
     * @since 1.0.0
     */
    private String baseFileName = "";

    /**
     * The holds the parameters and corresponding values for the moi tag
     * currently being processed.
     *
     * @since 1.0.0
     */
    private Map<String, String> moiParameterValueMap
            = new LinkedHashMap<String, String>();

    /**
     * This holds a map of the Managed Object Instances (MOIs) to the respective
     * csv print writers.
     *
     * @since 1.0.0
     */
    private Map<String, PrintWriter> moiPrintWriters
            = new LinkedHashMap<String, PrintWriter>();

    /**
     * Output directory.
     *
     * @since 1.0.0
     */
    private String outputDirectory = "/tmp";

    /**
     * Tag data.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String tagData = "";

    /**
     * Tracks Managed Object attributes to write to file. This is dictated by
     * the first instance of the MO found.
     *
     * @TODO: Handle this better.
     *
     * @since 1.0.0
     */
    private Map<String, Stack> moColumns = new LinkedHashMap<String, Stack>();

    /**
     * Parser start time.
     *
     * @since 1.0.4
     * @version 1.0.0
     */
    final long startTime = System.currentTimeMillis();

    /**
     * The file being parsed.
     *
     * @since 1.0.0
     */
    private String dataFile;

    /**
     * The file/directory to be parsed.
     *
     * @since 1.0.1
     */
    private String dataSource;

    /**
     * File format version tag for spec:fileHeader.
     *
     * @since 1.0.0
     */
    private String fileFormatVersion;

    /**
     * functionType attribute value on the spec:syndata tag.
     *
     * @since 1.0.0
     */
    private String functionType;

    /**
     * Id attribute value on the spec:syndata tag.
     *
     * @since 1.0.0
     */
    private String syndataId;

    /**
     * productversion attribute value on the spec:syndata tag.
     *
     * @since 1.0.0
     */
    private String productVersion;

    /**
     * nenrmversion attribute value on the spec:syndata tag.
     *
     * @since 1.0.0
     */
    private String neRMVersion;

    /**
     * neversion attribute value on the spec:syndata tag.
     *
     * @since 1.2.0
     */
    private String neVersion;

    /**
     * objId attribute value on the spec:syndata tag.
     *
     * @since 1.0.0
     */
    private String syndataObjId;

    /**
     * MOI.
     *
     * @since 1.0.0
     */
    private String moiXSIType;

    /**
     * Extraction date time.
     *
     * @since 1.1.0
     */
    private String varDateTime;

    /**
     * Parser states. Currently there are only 2: extraction and parsing
     *
     * @since 1.1.0
     */
    private int parserState = ParserStates.EXTRACTING_PARAMETERS;

    /**
     * The nodename value extracted from the Id value portion of the
     * spec:syncdata tag.
     *
     * @since 1.0.0
     */
    private String nodeName;

    /**
     * This is used when subsituting a parameter value with the value indicated
     * in comments.
     *
     * @since 1.0.0
     */
    private String previousTag;

    /**
     * File with a list of managed objects and parameters to extract.
     *
     * @since 1.1.0
     */
    private String parameterFile = null;

    public HuaweiCfgSynParser() {
    }

    /**
     * Set the parameter file name
     *
     * @param filename
     */
    public void setParameterFile(String filename) {
        parameterFile = filename;
    }

    /**
     * Extract parameter list from parameter file
     *
     * @param filename
     */
    public void getParametersToExtract(String filename) throws FileNotFoundException, IOException {
        // logger.debug("getParameterToExtract(...");
        BufferedReader br = new BufferedReader(new FileReader(filename));
        for (String line; (line = br.readLine()) != null;) {
            String[] moAndParameters = line.split(":");
            String mo = moAndParameters[0];
            String[] parameters = moAndParameters[1].split(",");

            // logger.debug("Added mo " + mo);
            Stack parameterStack = new Stack();
            for (int i = 0; i < parameters.length; i++) {
                parameterStack.push(parameters[i]);

            }

            moColumns.put(mo, parameterStack);

        }

        //Move to the parameter value extraction stage
        //parserState = ParserStates.EXTRACTING_VALUES;
    }

    /**
     * Parser entry point
     *
     * @since 1.0.0
     * @version 1.1.0
     *
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void parse() throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //Extract parameters
        if (parserState == ParserStates.EXTRACTING_PARAMETERS) {
            processFileOrDirectory();

            parserState = ParserStates.EXTRACTING_VALUES;
        }

        //Extracting values
        if (parserState == ParserStates.EXTRACTING_VALUES) {
            processFileOrDirectory();
            parserState = ParserStates.EXTRACTING_DONE;
        }

        closeMOPWMap();

        printExecutionTime();
    }

    /**
     * Determines if the source data file is a regular file or a directory and
     * parses it accordingly
     *
     * @since 1.1.0
     * @version 1.0.0
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void processFileOrDirectory()
            throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //this.dataFILe;
        Path file = Paths.get(this.dataSource);
        boolean isRegularExecutableFile = Files.isRegularFile(file)
                & Files.isReadable(file);

        boolean isReadableDirectory = Files.isDirectory(file)
                & Files.isReadable(file);

        if (isRegularExecutableFile) {
            this.setFileName(this.dataSource);
            this.parseFile(this.dataSource);
        }

        if (isReadableDirectory) {

            File directory = new File(this.dataSource);

            //get all the files from a directory
            File[] fList = directory.listFiles();

            for (File f : fList) {
                resetInternalVariables();
                
                this.setFileName(f.getAbsolutePath());
                try {

                    //@TODO: Duplicate call in parseFile. Remove!
                    baseFileName = getFileBasename(this.dataFile);
                    if (parserState == ParserStates.EXTRACTING_PARAMETERS) {
                        logger.info("Extracting parameters from " + this.baseFileName + "...");
                    } else {
                        logger.info("Parsing " + this.baseFileName + "...");
                    }

                    resetInternalVariables();
                    //Parse
                    this.parseFile(f.getAbsolutePath());
                    if (parserState == ParserStates.EXTRACTING_PARAMETERS) {
                        logger.info("Done.");
                    } else {
                        logger.info("Done.");
                        //System.out.println(this.baseFileName + " successfully parsed.\n");
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage());
                    logger.info("Skipping file: " + this.baseFileName + "\n");
                }
            }
        }

    }

    /**
     * Parses a single file
     *
     * @since 1.0.0
     * @version 1.0.0
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     *
     */
    public void parseFile(String filename)
            throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader eventReader = factory.createXMLEventReader(
                new FileReader(filename));
        baseFileName = getFileBasename(filename);

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    startElementEvent(event);
                    break;
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.CHARACTERS:
                    characterEvent(event);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    endELementEvent(event);
                    break;
                case XMLStreamConstants.COMMENT:
                    if (moiParameterValueMap.containsKey(this.previousTag)) {
                        String comment
                                = ((javax.xml.stream.events.Comment) event).getText();
                        moiParameterValueMap.put(previousTag, comment);
                    }
                    break;
            }
        }

    }

    /**
     * Handle start element event.
     *
     * @param xmlEvent
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     */
    public void startElementEvent(XMLEvent xmlEvent) throws FileNotFoundException {
        StartElement startElement = xmlEvent.asStartElement();
        String qName = startElement.getName().getLocalPart();
        String prefix = startElement.getName().getPrefix();

        Iterator<Attribute> attributes = startElement.getAttributes();

        //<spec:syndata ..>
        if (qName.equals("syndata")) {
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (attribute.getName().getLocalPart().equals("FunctionType")) {
                    this.functionType = attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("Id")) {
                    this.syndataId = attribute.getValue();
                    String[] arr = this.syndataId.split("=");
                    if (arr.length > 1) {
                        this.nodeName = arr[1];
                    } else {
                        this.nodeName = this.syndataId;
                    }
                }
                if (attribute.getName().getLocalPart().equals("productversion")) {
                    this.productVersion = attribute.getValue();
                }                //nermversion
                if (attribute.getName().getLocalPart().equals("nermversion")) {
                    this.neRMVersion = attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("objId")) {
                    this.syndataObjId = attribute.getValue();
                }
            }
        }

        //spec:fileFooter
        if (qName.equals("fileFooter") && parserState == ParserStates.EXTRACTING_PARAMETERS) {
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (attribute.getName().getLocalPart().equals("dateTime")) {
                    this.varDateTime = attribute.getValue();
                }
            }
        }

        //<parameter>
        if (inClass == true && inAttributes == false
                && !qName.endsWith("attributes") && !qName.endsWith("class")) {

            moiXSIType = qName;
            return;
        }

        //<class>
        if (qName.equals("class")) {
            inClass = true;
            return;
        }

        //<atributes>
        if (qName.equals("attributes")) {
            inAttributes = true;
        }

        //<ManagedObjects>
        if (inClass == true && inAttributes == false) {
            inMO = true;
            return;
        }

        //</fileFooter
        if (qName.equals("fileFooter")) {

            //If the file footer is not in the paramter file dont create it
            if (parameterFile != null && !moColumns.containsKey("fileFooter")) {
                return;
            }

            String footerFile = outputDirectory + File.separatorChar + "fileFooter.csv";
            PrintWriter pw = new PrintWriter(new File(footerFile));
            String headers = "FILENAME,NODENAME";
            String values = baseFileName + "," + nodeName;

            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (attribute.getName().getLocalPart().equals("label")) {
                    headers += ",label";
                    values += "," + attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("ExportResult")) {
                    headers += ",ExportResult";
                    values += "," + attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("dateTime")) {
                    headers += ",dateTime";
                    values += "," + attribute.getValue();
                }
            }

            pw.println(headers);
            pw.println(values);
            pw.println();
            pw.close();
        }

    }

    public void endELementEvent(XMLEvent xmlEvent)
            throws FileNotFoundException, UnsupportedEncodingException {

        EndElement endElement = xmlEvent.asEndElement();
        String prefix = endElement.getName().getPrefix();
        String qName = endElement.getName().getLocalPart();

//        String paramNames = "FILENAME,DATETIME,NODENAME,FUNCTIONTYPE,SYNCDATAID,PRODUCTVERSION,NERMVERSION,SYNCDATAOBJID";
//        String paramValues = baseFileName+ "," + varDateTime + ","+nodeName+","+functionType+","+syndataId
//                +","+productVersion+","+neRMVersion+","+syndataObjId;
//        
        String paramNames = "";
        String paramValues = "";

        // logger.debug("Processing " + moiXSIType);
        //</class>
        if (qName.equals("class")) {
            inClass = false;
            return;
        }

        if (inClass == true && inAttributes == true
                && !qName.endsWith("attributes") && !qName.endsWith("class")) {

            moiParameterValueMap.put(qName, tagData);
            this.previousTag = qName;
            tagData = "";
            return;
        }

        //we are at </attributes
        if (qName.equals("attributes")) {

            //Parameter extraction when no parameter file is provided.
            if (ParserStates.EXTRACTING_PARAMETERS == parserState && parameterFile == null) {
                Stack moiAttributes = new Stack();

                if (moColumns.containsKey(moiXSIType)) {
                    moiAttributes = moColumns.get(moiXSIType);
                } else {
                    moColumns.put(moiXSIType, moiAttributes);
                }

                Iterator<Map.Entry<String, String>> iter
                        = moiParameterValueMap.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, String> me = iter.next();
                    if (!moiAttributes.contains(me.getKey())) {
                        moiAttributes.push(me.getKey());
                    }
                }
            }

            //Value extraction stage 
            if (ParserStates.EXTRACTING_VALUES == parserState) {

                //logger.debug("EXTRACTING_VALUES values state");
                if (parameterFile == null) {
                    //logger.debug("moiXSIType:" + moiXSIType);

                    paramNames = "FILENAME,DATETIME,NODENAME,SYNCDATAFUNCTIONTYPE,SYNCDATAID,SYNCDATAPRODUCTVERSION,SYNCDATANERMVERSION,SYNCDATAOBJID";
                    paramValues = baseFileName + "," + varDateTime + "," + nodeName + "," + functionType + "," + syndataId
                            + "," + productVersion + "," + neRMVersion + "," + syndataObjId;

                    //Get the parameter list
                    Stack moiAttributes = moColumns.get(moiXSIType);

                    //check if print writer doesn't exists and create it
                    //This runs once per file.
                    if (!moiPrintWriters.containsKey(moiXSIType)) {

                        //Create print writer
                        String moiFile = outputDirectory + File.separatorChar + moiXSIType + ".csv";
                        moiPrintWriters.put(moiXSIType, new PrintWriter(moiFile));

                        //Create the header
                        String pName = paramNames;
                        for (int i = 0; i < moiAttributes.size(); i++) {
                            String p = moiAttributes.get(i).toString();

                            pName += "," + p;
                        }


                        moColumns.put(moiXSIType, moiAttributes);
                        moiPrintWriters.get(moiXSIType).println(pName);
                    }

                    for (int i = 0; i < moiAttributes.size(); i++) {
                        String pName = moiAttributes.get(i).toString();

                        if (moiParameterValueMap.containsKey(pName)) {
                            paramValues += "," + toCSVFormat(moiParameterValueMap.get(pName));
                        } else {
                            paramValues += ",";
                        }
                    }

                    PrintWriter pw = moiPrintWriters.get(moiXSIType);
                    pw.println(paramValues);
                }

                //If parameter file is present
                if (parameterFile != null) {

                    // Skip MOs not in the parameter file 
                    if(!moColumns.containsKey(moiXSIType)){
                        inAttributes = false;
                        moiParameterValueMap.clear();
                        return;
                    }
                    
                    //Get the parameter list
                    Stack moiAttributes = moColumns.get(moiXSIType);

                    //check if print writer doesn't exists and create it
                    //This runs once per file.
                    if (!moiPrintWriters.containsKey(moiXSIType)) {

                        //Create print writer
                        String moiFile = outputDirectory + File.separatorChar + moiXSIType + ".csv";
                        moiPrintWriters.put(moiXSIType, new PrintWriter(moiFile));

                        //Create the header
                        String pName = paramNames;
                        for (int i = 0; i < moiAttributes.size(); i++) {
                            String p = moiAttributes.get(i).toString();

                            if (p.equals("FILENAME")) {
                                pName += ",FILENAME";
                            } else if (p.equals("DATETIME")) {
                                pName += ",DATETIME";
                            } else if (p.equals("NODENAME")) {
                                pName += ",NODENAME";
                            } else if (p.equals("SYNCDATAFUNCTIONTYPE")) {
                                pName += ",SYNCDATAFUNCTIONTYPE";
                            } else if (p.equals("SYNCDATAID")) {
                                pName += ",SYNCDATAID";
                            } else if (p.equals("SYNCDATARODUCTVERSION")) {
                                pName += ",SYNCDATAPRODUCTVERSION";
                            } else if (p.equals("SYNCDATANERMVERSION")) {
                                pName += ",SYNCDATANERMVERSION";
                            } else if (p.equals("SYNCDATAOBJID")) {
                                pName += ",SYNCDATAOBJID";
                            } else {
                                pName += "," + p;
                            }

                        }
                        //Remove leading commas
                        pName = pName.replaceAll("^,","");

                        moiPrintWriters.get(moiXSIType).println(pName);

                    }

                    for (int i = 0; i < moiAttributes.size(); i++) {
                        String pName = moiAttributes.get(i).toString();

                        if (moiParameterValueMap.containsKey(pName)) {
                            paramValues += "," + toCSVFormat(moiParameterValueMap.get(pName));
                        } else {

                            if (pName.equals("FILENAME")) {
                                paramValues += "," + baseFileName;
                            } else if (pName.equals("DATETIME")) {
                                paramValues += "," + varDateTime;
                            } else if (pName.equals("NODENAME")) {
                                paramValues += "," + nodeName;
                            } else if (pName.equals("SYNCDATAFUNCTIONTYPE")) {
                                paramValues += "," + functionType;
                            } else if (pName.equals("SYNCDATASYNCDATAID")) {
                                paramValues += "," + syndataId;
                            } else if (pName.equals("SYNCDATAPRODUCTVERSION")) {
                                paramValues += "," + productVersion;
                            } else if (pName.equals("SYNCDATANERMVERSION")) {
                                paramValues += "," + neRMVersion;
                            } else if (pName.equals("SYNCDATAOBJID")) {
                                paramValues += "," + syndataObjId;
                            } else {
                                paramValues += ",";
                            }
                        }
                    }
                    //Remove leading commas
                    paramValues = paramValues.replaceAll("^,","");

                    PrintWriter pw = moiPrintWriters.get(moiXSIType);
                    pw.println(paramValues);
                }

            }

            moiParameterValueMap.clear();
            inAttributes = false;
            return;
        }

    }

    /**
     * Handle character events.
     *
     * @param xmlEvent
     * @version 1.0.0
     * @since 1.0.0
     */
    public void characterEvent(XMLEvent xmlEvent) {
        Characters characters = xmlEvent.asCharacters();
        if (!characters.isWhiteSpace()) {
            tagData = characters.getData();
        }
    }

    /**
     * Get file base name.
     *
     * @since 1.0.0
     */
    public String getFileBasename(String filename) {
        try {
            return new File(filename).getName();
        } catch (Exception e) {
            return filename;
        }
    }

    /**
     * Print program's execution time.
     *
     * @since 1.0.0
     */
    public void printExecutionTime() {
        float runningTime = System.currentTimeMillis() - startTime;

        String s = "Parsing completed. ";
        s = s + "Total time:";

        //Get hours
        if (runningTime > 1000 * 60 * 60) {
            int hrs = (int) Math.floor(runningTime / (1000 * 60 * 60));
            s = s + hrs + " hours ";
            runningTime = runningTime - (hrs * 1000 * 60 * 60);
        }

        //Get minutes
        if (runningTime > 1000 * 60) {
            int mins = (int) Math.floor(runningTime / (1000 * 60));
            s = s + mins + " minutes ";
            runningTime = runningTime - (mins * 1000 * 60);
        }

        //Get seconds
        if (runningTime > 1000) {
            int secs = (int) Math.floor(runningTime / (1000));
            s = s + secs + " seconds ";
            runningTime = runningTime - (secs / 1000);
        }

        //Get milliseconds
        if (runningTime > 0) {
            int msecs = (int) Math.floor(runningTime / (1000));
            s = s + msecs + " milliseconds ";
            runningTime = runningTime - (msecs / 1000);
        }

        logger.info(s);
    }

    /**
     * Close file print writers.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    public void closeMOPWMap() {
        Iterator<Map.Entry<String, PrintWriter>> iter
                = moiPrintWriters.entrySet().iterator();
        while (iter.hasNext()) {
            iter.next().getValue().close();
        }
        moiPrintWriters.clear();
    }

    /**
     * Process given string into a format acceptable for CSV format.
     *
     * @since 1.0.0
     * @param s String
     * @return String Formated version of input string
     */
    public String toCSVFormat(String s) {
        String csvValue = s;

        //Check if value contains comma
        if (s.contains(",")) {
            csvValue = "\"" + s + "\"";
        }

        if (s.contains("\"")) {
            csvValue = "\"" + s.replace("\"", "\"\"") + "\"";
        }

        return csvValue;
    }

    /**
     * Set the output directory.
     *
     * @since 1.0.0
     * @version 1.0.0
     * @param directoryName
     */
    public void setOutputDirectory(String directoryName) {
        this.outputDirectory = directoryName;
    }

    /**
     * Set name of file to parser.
     *
     * @since 1.0.0
     * @version 1.0.0
     * @param directoryName
     */
    private void setFileName(String filename) {
        this.dataFile = filename;
    }

    /**
     * Set name of file to parser.
     *
     * @since 1.0.1
     * @version 1.0.0
     * @param dataSource
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Show parser help.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    static public void showHelp() {
        System.out.println("boda-huaweicfgsynparser 1.2.0 Copyright (c) 2018 Bodastage(http://www.bodastage.com)");
        System.out.println("Parses Huawei baseline bulk configuration sync data XML to csv.");
        System.out.println("Usage: java -jar boda-huaweicfgsynparser.jar <fileToParse.xml> <outputDirectory> [parameter_file.cfg]");
    }

    /**
     * Reset some variables before parsing next file
     *
     */
    public void resetInternalVariables() {
        inClass = false;
        inAttributes = false;
        inMO = false;
        baseFileName = "";
        moiXSIType = null;
        previousTag = null;
    }
    
    public static void main(String[] args) {
        Options options = new Options();
        CommandLine cmd = null;
        String outputDirectory = null;   
        String inputFile = null;
        String parameterConfigFile = null;
        Boolean showHelpMessage = false;
        Boolean showVersion = false;
        
        
        try {
            
            options.addOption( "v", "version", false, "display version" );
            options.addOption( Option.builder("i")
                    .longOpt( "input-file" )
                    .desc( "input file or directory name")
                    .hasArg()
                    .argName( "INPUT_FILE" ).build());
            options.addOption(Option.builder("o")
                    .longOpt( "output-directory" )
                    .desc( "output directory name")
                    .hasArg()
                    .argName( "OUTPUT_DIRECTORY" ).build());
            options.addOption(Option.builder("c")
                    .longOpt( "parameter-config" )
                    .desc( "parameter configuration file")
                    .hasArg()
                    .argName( "PARAMETER_CONFIG" ).build() );
            options.addOption( "h", "help", false, "show help" );
            
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse( options, args);

            if( cmd.hasOption("h")){
                showHelpMessage = true;
            }

            if( cmd.hasOption("v")){
                showVersion = true;
            }
            
            if(cmd.hasOption('o')){
                outputDirectory = cmd.getOptionValue("o"); 
            }
            
            if(cmd.hasOption('i')){
                inputFile = cmd.getOptionValue("i"); 
            }
            
            if(cmd.hasOption('c')){
                parameterConfigFile = cmd.getOptionValue("c"); 
            }
      
       }catch(IllegalArgumentException e){
           
       } catch (ParseException ex) {
            //java.util.logging.Logger.getLogger(HuaweiCfgSynParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    try{
            
            if(showVersion == true ){
                System.out.println(VERSION);
                System.out.println("Copyright (c) 2019 Bodastage Solutions(http://www.bodastage.com)");
                System.exit(0);
            }
            
            
            //show help
            if( showHelpMessage == true || 
                inputFile == null || 
                ( outputDirectory == null ) ){
                     HelpFormatter formatter = new HelpFormatter();
                     String header = "Parses Huawei AUTOBAK/CM Backup configuration data file to csv\n\n";
                     String footer = "\n";
                     footer += "Examples: \n";
                     footer += "java -jar boda-huaweicfgsynparser.jar -i cnaiv2_dump.xml -o out_folder\n";
                     footer += "java -jar boda-huaweicfgsynparser.jar -i input_folder -o out_folder\n";
                     footer += "\nCopyright (c) 2019 Bodastage Solutions(http://www.bodastage.com)";
                     formatter.printHelp( "java -jar boda-huaweicfgsynparser.jar", header, options, footer );
                     System.exit(0);
            }
            
            //Confirm that the output directory is a directory and has write 
            //privileges
            if(outputDirectory != null ){
                File fOutputDir = new File(outputDirectory);
                if (!fOutputDir.isDirectory()) {
                    System.err.println("ERROR: The specified output directory is not a directory!.");
                    System.exit(1);
                }

                if (!fOutputDir.canWrite()) {
                    System.err.println("ERROR: Cannot write to output directory!");
                    System.exit(1);
                }
            }
            
            
            HuaweiCfgSynParser parser = new HuaweiCfgSynParser();
            
            if(  parameterConfigFile != null ){
                File f = new File(parameterConfigFile);
                if(f.isFile()){
                    parser.setParameterFile(parameterConfigFile);
                    parser.getParametersToExtract(parameterConfigFile);
                    parser.parserState = ParserStates.EXTRACTING_VALUES;
                }
            }
            
            parser.setDataSource(inputFile);
            parser.setOutputDirectory(outputDirectory);
            parser.parse();

        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
}
