![Build status](https://travis-ci.org/bodastage/boda-huaweicfgsynparser.svg?branch=master)

# boda-huaweicfgsynparser
Parses HuaweI baseline bulk configuration management XML sync data to csv.

Below is the format of the expected input file:

```XML
<?xml version="1.0" encoding="ISO-8859-1"?>
<spec:BACKUPCFG xmlns:spec="http://www.huawei.com/specs/huawei_wl_bulkcm_xml_baseline_syn_1.0.0"
xmlns="http://www.huawei.com/specs/bsc6000_nrm_forSyn_collapse_1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://www.huawei.com/specs/huawei_wl_bulkcm_xml_baseline_syn_1.0.0
CfgSynData_collapse_spec.xsd">
	<spec:fileHeader fileFormatVersion="1.1.0" nenrmversion="XXXXXXXX" neversion="BTS3900 V100R011C10SPC262" syntype="synall" synlabel="XXXXXXXX" producttype="X"/>
	<spec:compatibleNrmVersionList>
		<spec:compatibleNrmVersion>XXXXXXXX</spec:compatibleNrmVersion>
	</spec:compatibleNrmVersionList>
	<spec:syndata FunctionType="NODE" Id="XXXXXXXX" productversion="BTS3900 XXXXXXXX" nermversion="XXXXXXXX" objId="-1">
		<spec:compatibleNrmVersionList>
			<spec:compatibleNrmVersion>XXXXXXXX</spec:compatibleNrmVersion>
		</spec:compatibleNrmVersionList>
		<class>
			<MO1>
				<attributes>
					<PARAMETER1>Value</PARAMETER1><!--Translated Value-->
				</attributes>
			</MO1>
		</class>
		<class>
			<MO2>
				<attributes>
					<PARAMETER1>Value</PARAMETER1><!--Translated Value-->
					<PARAMETER2>Value</PARAMETER2><!--Translated Value-->
				</attributes>
			</MO2 >
		</class>
    ...
	</spec:syndata>
	<spec:fileFooter label="XXXXXXXXXX" ExportResult="Success" dateTime="2090-08-13T09:41:59">
		<spec:moclistincluded></spec:moclistincluded>
	</spec:fileFooter>
</spec:BACKUPCFG>
```
# Usage
```
usage: java -jar boda-huaweicfgsynparser.jar
Parses Huawei AUTOBAK/CM Backup configuration data file to csv

 -c,--parameter-config <PARAMETER_CONFIG>   parameter configuration file
 -h,--help                                  show help
 -i,--input-file <INPUT_FILE>               input file or directory name
 -o,--output-directory <OUTPUT_DIRECTORY>   output directory name
 -v,--version                               display version

Examples:
java -jar boda-huaweicfgsynparser.jar -i cnaiv2_dump.xml -o out_folder
java -jar boda-huaweicfgsynparser.jar -i input_folder -o out_folder

Copyright (c) 2019 Bodastage Solutions(http://www.bodastage.com)
```


# Download and installation
The lastest compiled jar file is availabled in the dist directory. Alternatively, download it directly from [here](https://github.com/bodastage/boda-huaweicfgsynparser/raw/master/dist/boda-huaweicfgsynparser.jar).

# Requirements
To run the jar file, you need Java version 1.8 and above.

# Getting help
To report issues with the application or request new features use the issue [tracker](https://github.com/bodastage/boda-huaweicfgsynparser/issues). For help and customizations send an email to info@bodastage.com.

# Credits
[Bodastage Solutions](https://www.bodastage.com) - info@bodastage.com

# Contact
For any other concerns apart from issues and feature requests, send an email to info@bodastage.com.

# Licence
This project is licensed under the Apache 2.0 licence.  See LICENCE file for details.
