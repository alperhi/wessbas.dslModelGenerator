This project contains the source code for the M4J-DSL Model Generator, which
builds models that comply to the M4J-DSL. Each model represents a Markov4JMeter
workload model, including workload intensity, Application Layer, Behavior Models
and Behavior Mix.


SYSTEM REQUIREMENTS
-------------------

The project has been developed with the use of the following tools:

  - Eclipse Kepler
  - JDK 1.7
  - Xtext 2.5.4

An Eclipse environment using this project should be configured accordingly. 


PROJECT CONTENT
---------------

Additionally to the standard folder structure of an Eclipse project, the
following folders are included:

  - folder "examples" contains some example data for flows, behavior information
    and models. The following sub-folders are included:

      o "behavior"      -- example behavior information as it might be retrieved
                           from monitoring data, including probabilities and
                           (normally distributed) think times.

      o "flows"         -- example Flows which have been retrieved from the
                           generation process of the b+m gear application
                           "CarShare".

      o "flows.subset1" -- closed subset of CarShare Flows, which appears to be
                           independent from the remaining Flows. This might be
                           used for test runs with less input data, as the full
                           amount of Flows requires much more processing time.

      o "flows.subset2" -- another closed subset of CarShare Flows.

      o "models"        -- some simple example workload models which comply to
                           the M4J-DSL. These models just serve for testing
                           purposes in terms of validation in the Eclipse Form
                           Editor.

  - folder "output" contains the output files of a test run, which generally
    consist of a workload model (.XMI) and a graph visualization file (.DOT).

The root directory contains two properties files for the M4J-DSL Model
Generator:

  - "workloadIntensity.properties" and
  - "behaviorModels.properties".

Both files demonstrate the definition of workload intensity and Behavior Models
respectively. 


USAGE
-----

Class "net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator" provides
the main() method, which requires a certain set of parameters to be passed via
command-line in a specific order. The following parameters need to be provided:

  <workloadIntensity.properties> -- path to the properties file with workload
                                    intensity definitions.

  <behaviorModels.properties>    -- path to the properties file with Behavior
                                    Models definitions.

  <flowsDirPath>                 -- path to the directory which contains the
                                    input Flows files.

  <xmiOutputFile>                -- path to the XMI output file.

  <graphOutputFile>              -- path to the graph output file.

An example parameter sequence (to be used in the Eclipse run configuration)
might look as follows:

  ./workloadIntensity.properties ./behaviorModels.properties
    ./examples/flows.subset2/ ./output/workloadmodel.xmi ./output/graph.dot

Starting the application with these parameters will produce regarding output
in the "output" folder.


DEVELOPMENT
-----------

The source code is comprehensively commented, and most of it should be
self-explaining. Two classes are not final yet, since details regarding to the
generation of Markov4JMeter models based of b+m gear Flows need to be cleared:

  - FlowSessionLayerEFSMGenerator:
    class for building Session Layer EFSMs based on Flows; the Session Layer
    EFSM structure will probably be changed in future releases.

  - JavaProtocolLayerEFSMGenerator:
    class for building Protocol Layer EFSMs based on Java requests; the
    construction of the Protocol Layer EFSMs still requires additional
    information regarding to states, transitions and request parameters.

Both classes are marked with "warning" signs in the Eclipse Package Explorer,
indicating their open issues.