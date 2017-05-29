# A hacked version of the Apache Maven PDF Plugin

Primary deltas

 - Allows for custom headers and footers
 - Headers/footers are centered on page
 
This repo contains the source for several doxia modules and for the original maven-pdf-plugin

ASF 2.0 licensed

Setup your headers/footers

```xml
	<properties>
        <timestamp>${maven.build.timestamp}</timestamp>
        <maven.build.timestamp.format>yyyy-MM-dd HH:mm</maven.build.timestamp.format>
        <pdf.footer>Some contet 
            Second line content 
            Third line content 
            Version ${project.version} 
            ${timestamp}</pdf.footer>
        <pdf.header>Some contet 
            Second line content 
            Third line content 
            Version ${project.version} 
            ${timestamp}</pdf.header>
    </properties>
```

then add to your build via this

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>com.github.spyhunter99</groupId>
				<artifactId>maven-pdf-plugin</artifactId>
				<version>1.5.1</version>
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
                      </configuration>
                    </execution>
                  
                </executions>
            </plugin>
		</plugins>
	</build>
xml