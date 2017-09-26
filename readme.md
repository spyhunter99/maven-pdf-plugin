# A modified version of the Apache Maven PDF Plugin

Primary deltas

 - Allows for custom headers and footers
 - Headers/footers are centered on page
 - Seperate headers on the title pages
 - Adjustable, Multi-level Table of Contents (generated from your docs)
 - EXSUM, rendered before the TOC
 - Project logo is front and centered and is stretched to full width if possible
 - Title page has several new fields that can be populated
	- Date
	- Distribution statement
 
This repo contains the source for several doxia modules and for the original maven-pdf-plugin

ASF 2.0 licensed

```xml
<properties>
....
	<timestamp>${maven.build.timestamp}</timestamp>
	<maven.build.timestamp.format>yyyy-MM-dd HH:mm</maven.build.timestamp.format>
....
</properties>
<build>
	<plugins>
		<plugin>
			<groupId>com.github.spyhunter99</groupId>
			<artifactId>maven-pdf-plugin</artifactId>
			<version>1.5.0.3</version>
			<executions>
				<execution>
				  <id>pdf-user</id>
				  <phase>site</phase>
				  <goals>
					<goal>pdf</goal>
				  </goals>
				  <configuration>
					<outputDirectory>${project.reporting.outputDirectory}</outputDirectory>
					<includeReports>false</includeReports>
					<docDescriptor>src/site/pdf-user.xml</docDescriptor>
					
					<!-- optional, the title of a doc defined in pdf-user that is rendered before the TOC -->
					<executiveSummaryName>EXSUM</executiveSummaryName>
					<!-- optional, a header only on the title page-->
					<titleHeader>A custom header only on the title pages</titleHeader>
					
					<!-- optional, the distribution statement is printed on the title page towards the bottom, common use case:
							confidentiality statements, limited distribution, legalese and other mumbo jumbo.-->
					<distributionStatement>DISTRIBUTION STATEMENT: Feel free to give this document to anyone you will take it.</distributionStatement>
						
					<!-- optional, these header/footer definitions are used on all pages exception the title page -->
					<pdfFooter>Some contet 
						Second line content 
						Third line content 
						Version ${project.version} 
						${timestamp}</pdfFooter>
					<pdfHeader>Some contet 
						Second line content 
						Third line content 
						Version ${project.version} 
						${timestamp}</pdfHeader>
						
					<!-- optional, the cover page can have a pom defined date too -->
					<pdfCoverDate>1970-1-1</pdfCoverDate>
					
					<!-- optional, the max depth of the TOC -->
					<tocMaxDepthToPrint>4</tocMaxDepthToPrint>
				  </configuration>
				</execution>
			  
			</executions>
		</plugin>
	</plugins>
</build>
```
