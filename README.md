#erechnung.gv.at-webservice-client

Utility classes and projects for the use with ER>B - the national Austrian Invoicing solution.
This project contains Webservice clients for both Webservice 1.2 and Webservice 2.0 and the JAXB
generated classes for the Webservice callback.


#Maven usage
Add the following to your pom.xml to use this artifact:
```
<dependency>
  <groupId>com.helger.erechnung.gv.at</groupId>
  <artifactId>webservice-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

#Tips and tricks
When importing this project into Eclipse, please ensure to run `mvn generate-sources` to generate all 
the JAXB classes required. They reside in `target/generated-sources/wsimport` and must be part of the
compilation.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
