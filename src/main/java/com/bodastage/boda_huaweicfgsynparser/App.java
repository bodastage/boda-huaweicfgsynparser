package com.bodastage.boda_huaweicfgsynparser;

import java.io.File;

/**
 * Huawei baseline cfg sync data xml export files Parser.
 * 
 * @version  1.0.0
 * @since 1.0.0
 */
public class App 
{
    public static void main( String[] args )
    {
        try{
            //show help
            if(args.length != 2 || (args.length == 1 && args[0] == "-h")){
                showHelp();
                System.exit(1);
            }
            //Get bulk CM XML file to parse.
            String filename = args[0];
            String outputDirectory = args[1];
            
            //Confirm that the output directory is a directory and has write 
            //privileges
            File fOutputDir = new File(outputDirectory);
            if(!fOutputDir.isDirectory()) {
                System.err.println("ERROR: The specified output directory is not a directory!.");
                System.exit(1);
            }
            
            if(!fOutputDir.canWrite()){
                System.err.println("ERROR: Cannot write to output directory!");
                System.exit(1);            
            }

            HuaweiCfgSynParser parser = new HuaweiCfgSynParser();
            parser.setFileName(filename);
            parser.setOutputDirectory(outputDirectory);
            parser.parse();
            parser.printExecutionTime();
        }catch(Exception e){
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Show parser help.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    static public void showHelp(){
        System.out.println("boda-huaweicfgsynparser 1.0.0. Copyright (c) 2016 Bodastage(http://www.bodastage.com)");
        System.out.println("Parses HuaweI baseline bulk configuration sync data XML to csv.");
        System.out.println("Usage: java -jar boda-huaweicfgsynparser.jar <fileToParse.xml> <outputDirectory>");
    }
}
