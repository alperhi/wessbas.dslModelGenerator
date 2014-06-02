package net.sf.markov4jmeter.m4jdslmodelgenerator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import m4jdsl.ApplicationModel;
import m4jdsl.BehaviorMix;
import m4jdsl.BehaviorModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.WorkloadIntensity;
import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslPackageImpl;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.ApplicationModelGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorMixGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorModelsGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.WorkloadIntensityGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.FlowSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.JavaProtocolLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.XmiEcoreHandler;

public class M4jdslModelGenerator {

    private final static String PKEY_XMI_OUTPUT_FILE = "xmioutputfile";

    private final M4jdslFactory m4jdslFactory;


    public M4jdslModelGenerator () {

        M4jdslPackageImpl.init();
        this.m4jdslFactory = M4jdslFactory.eINSTANCE;
    }


    public WorkloadModel generateWorkloadModel (
            final Properties workloadIntensityProperties,
            final String flowsDirectoryPath,
            final String graphOutputPath) throws GeneratorException {

        // to be returned;
        final WorkloadModel workloadModel =
                this.m4jdslFactory.createWorkloadModel();

        final ServiceRepository serviceRepository =
                new ServiceRepository(this.m4jdslFactory);

        final HashMap<String, Double> behaviorMixEntries =
                new HashMap<String, Double>();

        behaviorMixEntries.put("Behavior Model 1", 1.0);

        // set components;

        this.installWorkloadIntensity(
                workloadModel,
                workloadIntensityProperties);

        this.installApplicationLayer(
                workloadModel,
                serviceRepository,
                flowsDirectoryPath,
                graphOutputPath);

        this.installBehaviorModels(
                workloadModel,
                serviceRepository);

        this.installBehaviorMix(
                workloadModel,
                workloadModel.getBehaviorModels(),
                behaviorMixEntries);

        return workloadModel;
    }

    private WorkloadModel installWorkloadIntensity (
            final WorkloadModel workloadModel,
            final Properties workloadIntensityProperties) throws GeneratorException {

        final WorkloadIntensityGenerator workloadIntensityGenerator =
                new WorkloadIntensityGenerator(this.m4jdslFactory);

        // might throw a GeneratorException;
        final WorkloadIntensity workloadIntensity =
                workloadIntensityGenerator.generateWorkloadIntensity(
                        workloadIntensityProperties);

        workloadModel.setWorkloadIntensity(workloadIntensity);
        return workloadModel;
    }

    private WorkloadModel installApplicationLayer (
            final WorkloadModel workloadModel,
            final ServiceRepository serviceRepository,
            final String flowsDirectoryPath,
            final String graphOutputPath) throws GeneratorException {

        final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator =
                new JavaProtocolLayerEFSMGenerator(
                        this.m4jdslFactory,
                        new IdGenerator("PS"),
                        new IdGenerator("R"));

        final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator =
                new FlowSessionLayerEFSMGenerator(
                        this.m4jdslFactory,
                        serviceRepository,
                        protocolLayerEFSMGenerator,
                        new IdGenerator("AS"),
                        flowsDirectoryPath,
                        graphOutputPath);

        final ApplicationModelGenerator applicationModelGenerator =
                new ApplicationModelGenerator(this.m4jdslFactory, sessionLayerEFSMGenerator);

        // might throw a GeneratorException;
        final ApplicationModel applicationModel =
                applicationModelGenerator.generateApplicationModel();

        workloadModel.setApplicationModel(applicationModel);
        return workloadModel;
    }

    private WorkloadModel installBehaviorModels (
            final WorkloadModel workloadModel,
            final ServiceRepository serviceRepository)
                    throws GeneratorException {

        final BehaviorModelsGenerator behaviorModelGenerator =
                new BehaviorModelsGenerator(
                        this.m4jdslFactory,
                        serviceRepository,
                        new IdGenerator("BM"));

        // might throw a GeneratorException;
        final List<BehaviorModel> behaviorModels =
                behaviorModelGenerator.generateBehaviorModels();

        for (final BehaviorModel behaviorModel : behaviorModels) {

            workloadModel.getBehaviorModels().add(behaviorModel);
        }

        return workloadModel;
    }

    private WorkloadModel installBehaviorMix (
            final WorkloadModel workloadModel,
            final List<BehaviorModel> behaviorModels,
            final HashMap<String, Double> behaviorMixEntries)
                    throws GeneratorException {

        final BehaviorMixGenerator behaviorMixGenerator =
                new BehaviorMixGenerator(this.m4jdslFactory);

        // might throw a GeneratorException;
        final BehaviorMix behaviorMix =
                behaviorMixGenerator.generateBehaviorMix(
                        behaviorMixEntries,
                        behaviorModels);

        workloadModel.setBehaviorMix(behaviorMix);
        return workloadModel;
    }

    private static void printUsage () {

        final String appName = M4jdslModelGenerator.class.getSimpleName();

        System.out.println("Usage: " + appName + " <generator.properties> <flowsDirPath> <behaviorDirPath> <xmioutputfile> <graphOutputFile.dot>");
    }

    public static void main (final String[] argv) {

        if (argv.length < 5) {

            M4jdslModelGenerator.printUsage();

        } else {

            // TODO: make "graphFilePath" optional, use a command-line parser;
            final String propertiesFilename    = argv[0];
            final String flowsDirectoryPath    = argv[1];
            final String behaviorDirectoryPath = argv[2];
            final String xmioutputfile         = argv[3];
            final String graphFilePath         = argv[4];

            final M4jdslModelGenerator m4jdslModelGenerator =
                    new M4jdslModelGenerator();

            try {

                // might throw a FileNotFound- or IOException;
                final Properties generatorProperties =
                        M4jdslModelGenerator.loadProperties(propertiesFilename);

                final WorkloadModel workloadModel =
                        m4jdslModelGenerator.generateWorkloadModel(
                                generatorProperties,
                                flowsDirectoryPath,
                                graphFilePath);


                //final String outputFile = generatorProperties.
                //        getProperty(M4jdslModelGenerator.PKEY_XMI_OUTPUT_FILE);
                final String outputFile = xmioutputfile;

                if (outputFile == null) {

                    throw new IOException("XMI output file is undefined");
                }

                // might throw an IOException;
                M4jdslModelGenerator.saveWorkloadModel(workloadModel, outputFile);

                System.out.println("Finished.");

            } catch (final Exception ex) {

                System.err.println(ex.getMessage() + ".\n");
                M4jdslModelGenerator.printUsage();
            }
        }
    }

    private static void saveWorkloadModel (
    		final WorkloadModel workloadModel,
    		final String outputXmiFile) throws IOException {

        try {

//            workloadModel = (WorkloadModel) XmiEcoreHandler.getInstance().xmiToEcore(
//                    "examples/models/WorkloadModel_java.xmi");

            // FIXME: implementation via XMI file writer (-> Test Plan generator);
            XmiEcoreHandler.getInstance().ecoreToXMI(workloadModel, outputXmiFile);

        } catch (final IOException ex) {

            System.err.println(ex.getMessage() + ".\n");
            M4jdslModelGenerator.printUsage();
        }
    }

    /**
     * Loads the key/value pairs from a specified properties file.
     *
     * @param filename  name of the properties file to be loaded.
     *
     * @throws FileNotFoundException
     *     in case the denoted file does not exist.
     * @throws IOException
     *     if any error while reading occurs.
     * @throws NullPointerException
     *     if <code>null</code> is passed as filename.
     */
    private static Properties loadProperties (final String filename)
            throws FileNotFoundException, IOException {

        final Properties properties = new Properties();

        // might throw a FileNotFoundException;
        final FileInputStream fileInputStream = new FileInputStream(filename);

        final BufferedInputStream bufferedInputStream =
                new BufferedInputStream(fileInputStream);

        try {

            // might throw an IO- or IllegalArgumentException;
            properties.load(bufferedInputStream);

        } finally {

            if (bufferedInputStream != null) {

                try {

                    bufferedInputStream.close();

                } catch (final IOException ex) {

                    // ignore IOException, since this is the "finally" block;
                    // TODO: error message could be written into log-file;
                }
            }
        }

        return properties;
    }
}