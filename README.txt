(C) 2014 tobias.gierke@code-sourcery.de

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Uses
----

1.) http://www.jhlabs.com/java/maps/proj/ for map projection
2.) Map images from WikiMedia Commons
3.) IP geolocation REST API from http://freegeoip.net/

Building
--------

I couldn't find a working Maven artifact for the "proj" library so I built it from the SVN trunk sources at sourceforge and included in the /lib folder. 

Run

    mvn install:install-file -Dfile=lib/jmapprojlib-1.0.0-SNAPSHOT.jar -DgroupId=com.jhlabs -DartifactId=javaproj -Dversion=1.0-tgierke -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true

to install the file in your local repository.

Afterwards, just run "mvn package" (which creates a self-executable JAR) or "mvn compile exec:java" to run it.
