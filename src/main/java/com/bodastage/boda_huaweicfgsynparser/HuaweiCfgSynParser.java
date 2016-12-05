/*
 * Huawei bulk CM XML baseline syn backup data file parser.
 */
package com.bodastage.boda_huaweicfgsynparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

/**
 *
 * @author Bodastage <info@bodastage.com>
 * @version 1.0.0
 */
public class HuaweiCfgSynParser {
    
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
    private Map<String,String> moiParameterValueMap 
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
     * The file to be parsed.
     * 
     * @since 1.0.0
     */
    private String dataFile;
            
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
     * nermversion attribute value on the spec:syndata tag.
     * 
     * @since 1.0.0
     */
    private String neRMVersion;

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
    
    public HuaweiCfgSynParser(){}
    
    /**
     * The parser's entry point.
     * 
     * @param filename 
     */
    public void parse() 
    throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException
    {
            XMLInputFactory factory = XMLInputFactory.newInstance();

            XMLEventReader eventReader = factory.createXMLEventReader(
                    new FileReader(this.dataFile));
            baseFileName = getFileBasename(this.dataFile);

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
                }
            }
            
            
            //
            closeMOPWMap();
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
        if(qName.equals("syndata")){
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (attribute.getName().getLocalPart().equals("FunctionType")) {
                    this.functionType = attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("Id")) {
                    this.syndataId = attribute.getValue();
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
        
        //<parameter>
        if(inClass == true && inAttributes == false 
                && !qName.endsWith("attributes") && !qName.endsWith("class"))
        {
            moiXSIType = qName;
            return;
        }
        
        //<class>
        if(qName.equals("class")){
            inClass = true;
            return;
        }

        //<atributes>
        if(qName.equals("attributes")){
            inAttributes = true;
        }
        
        //<ManagedObjects>
        if(inClass == true && inAttributes == false){
            inMO = true;
            return;
        }
        
        //</fileFooter
        if(qName.equals("fileFooter")){
            String footerFile = outputDirectory + File.separatorChar +  "fileFooter.csv";
            PrintWriter pw = new PrintWriter( new File(footerFile));
            String headers = "FileName";
            String values = baseFileName;
            
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (attribute.getName().getLocalPart().equals("label")) {
                    headers += ",label";
                    values += ","+attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("ExportResult")) {
                    headers += ",ExportResult";
                    values += ","+attribute.getValue();
                }
                if (attribute.getName().getLocalPart().equals("dateTime")) {
                    headers += ",dateTime";
                    values += ","+attribute.getValue();
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
        
        String paramNames = "FileName,FunctionType,Id,productversion,nermversion,objId";
        String paramValues = baseFileName+","+functionType+","+syndataId
                +","+productVersion+","+neRMVersion+","+syndataObjId;
        
        //</class>
        if(qName.equals("class")){
            inClass = false;
            return;
        }
        
        if(inClass == true && inAttributes == true 
                && !qName.endsWith("attributes") && !qName.endsWith("class"))
        {
            
            moiParameterValueMap.put(qName, tagData);
            return;
        }
        
        if(qName.equals("attributes")){
                        
            //check if print writer doesn't exists and create it
            if(!moiPrintWriters.containsKey(moiXSIType)){
                String moiFile = outputDirectory + File.separatorChar + moiXSIType +  ".csv";
                 moiPrintWriters.put(moiXSIType, new PrintWriter(moiFile));

                 
                Stack moiAttributes = new Stack();
                Iterator<Map.Entry<String, String>> iter 
                        = moiParameterValueMap.entrySet().iterator();
                
                String pName = paramNames;
                while (iter.hasNext()) {
                    Map.Entry<String, String> me = iter.next();
                    moiAttributes.push(me.getKey());
                    pName += "," + me.getKey();
                }
                moColumns.put(moiXSIType, moiAttributes);
                moiPrintWriters.get(moiXSIType).println(pName);
                 
            }
            
            Stack moiAttributes = moColumns.get(moiXSIType);
            for(int i = 0; i< moiAttributes.size(); i++){
                String moiName = moiAttributes.get(i).toString();
                
                if( moiParameterValueMap.containsKey(moiName) ){
                    paramValues += "," + toCSVFormat(moiParameterValueMap.get(moiName));
                }else{
                    paramValues += ",";
                }   
            }
            
            PrintWriter pw = moiPrintWriters.get(moiXSIType);
            pw.println(paramValues);
            
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
        if(!characters.isWhiteSpace()){
            tagData = characters.getData(); 
        }
    }  
    
    /**
     * Get file base name.
     * 
     * @since 1.0.0
     */
     public String getFileBasename(String filename){
        try{
            return new File(filename).getName();
        }catch(Exception e ){
            return filename;
        }
    }
     
    /**
     * Print program's execution time.
     * 
     * @since 1.0.0
     */
    public void printExecutionTime(){
        float runningTime = System.currentTimeMillis() - startTime;
        
        String s = "Parsing completed. ";
        s = s + "Total time:";
        
        //Get hours
        if( runningTime > 1000*60*60 ){
            int hrs = (int) Math.floor(runningTime/(1000*60*60));
            s = s + hrs + " hours ";
            runningTime = runningTime - (hrs*1000*60*60);
        }
        
        //Get minutes
        if(runningTime > 1000*60){
            int mins = (int) Math.floor(runningTime/(1000*60));
            s = s + mins + " minutes ";
            runningTime = runningTime - (mins*1000*60);
        }
        
        //Get seconds
        if(runningTime > 1000){
            int secs = (int) Math.floor(runningTime/(1000));
            s = s + secs + " seconds ";
            runningTime = runningTime - (secs/1000);
        }
        
        //Get milliseconds
        if(runningTime > 0 ){
            int msecs = (int) Math.floor(runningTime/(1000));
            s = s + msecs + " milliseconds ";
            runningTime = runningTime - (msecs/1000);
        }

        
        System.out.println(s);
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
    public void setOutputDirectory(String directoryName ){
        this.outputDirectory = directoryName;
    }
     
    /**
     * Set name of file to parser.
     * 
     * @since 1.0.0
     * @version 1.0.0
     * @param directoryName 
     */
    public void setFileName(String filename ){
        this.dataFile = filename;
    }
}
