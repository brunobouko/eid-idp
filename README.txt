README for eID Identity Provider Project
========================================

=== 1. Introduction

This project contains the source code tree of the eID Identity Provider.
The source code is hosted at: http://code.google.com/p/eid-idp/


=== 2. Requirements

The following is required for compiling the eID Trust Service software:
* Sun Java 1.6.0_23
* Apache Maven 3.0.1


=== 3. Build

The project can be build via:
	mvn clean install

The deployable Java EE application can be found under:
	eid-idp-deploy

You can speed up the development build cycle by skipping the unit tests via:
	mvn -Dmaven.test.skip=true clean install


=== 4. Eclipse IDE

The Eclipse project files can be created via:
	mvn eclipse:eclipse

Afterwards simply import the projects in Eclipse via:
	File -> Import... -> General:Existing Projects into Workspace

First time you use an Eclipse workspace you might need to add the maven 
repository location. Do this via:
    mvn eclipse:add-maven-repo -Declipse.workspace=<location of your workspace>


=== 5. License

The license conditions can be found in the file: LICENSE.txt

