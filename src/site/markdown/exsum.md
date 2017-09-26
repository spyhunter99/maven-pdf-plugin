This is the executive summary of a non-existing document.

The only thing it is good for, is to confirm that the exsum-page generation works in the maven-pdf-plugin (modified).

The EXSUM is a very brief document that is rendered before the table of contents.

You can define your own in your pom using the following snippet

````
 <plugin>
	<groupId>com.github.spyhunter99</groupId>
	<artifactId>maven-pdf-plugin</artifactId>
	<executions>
		<execution>
			<id>pdf</id>
			<phase>site</phase>
			<goals>
				<goal>pdf</goal>
			</goals>
			<configuration>
				<outputDirectory>${project.reporting.outputDirectory}</outputDirectory>
				<includeReports>false</includeReports>
				<docDescriptor>src/site/pdf.xml</docDescriptor>
				<executiveSummaryName>ExSumTitle</executiveSummaryName>
			</configuration>
		</execution>
````

Where `ExSumTitle` is the **title** of a document listed in `src/site/pdf.xml`