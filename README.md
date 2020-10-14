# OrthoMCLWebService
[WSF](https://github.com/VEuPathDB/WSF) Plugins used by the OrthoMCL website.  See [WSFPlugin/src](WSFPlugin/src/main/java/org/orthomcl/wsfplugin) for the list of plugins.

The content in this repo will be moved to [[https://github.com/VEuPathDB/OrthoMCLModel][OrthoMCLModel]] and this repo will be archived after VEuPathDB release 50 planned for January 2021.

## Dependencies

   + ant
   + Perl 5
   + Java 11+
   + External dependencies: see [pom.xml](pom.xml)
   + environment variables for GUS_HOME and PROJECT_HOME
   + Internal Dependencies: see [build.xml](build.xml)

## Installation instructions.

   + bld OrthoMCLWebService

## Manifest

   + WSFPlugin/config :: configuration for individual plugins
   + WSFPlugin/lib :: conifer and perl libraries for the plugins
   + WSFPlugin/src :: java source code for the plugins

