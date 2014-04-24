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
import m4jdsl.RelativeFrequency;
import m4jdsl.WorkloadIntensity;
import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslPackageImpl;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.ApplicationModelGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorMixGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.BehaviorModelsGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.WorkloadIntensityGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.FlowSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.ProtocolLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.XmiEcoreHandler;

public class M4jdslModelGenerator {

    private final static String PKEY_XMI_OUTPUT_FILE = "xmioutputfile";

    final M4jdslFactory m4jdslFactory;


    public M4jdslModelGenerator () {

        M4jdslPackageImpl.init();
        this.m4jdslFactory = M4jdslFactory.eINSTANCE;
    }


    public WorkloadModel generateWorkloadModel (
            final Properties workloadIntensityProperties,
            final String flowsDirectoryPath,
            final String graphOutputPath) throws GeneratorException {

        // build generators for all components;

//        final IdGenerator idGenerator = new IdGenerator();

        final WorkloadIntensityGenerator workloadIntensityGenerator =
                new WorkloadIntensityGenerator(this.m4jdslFactory);

        final ServiceRepository serviceRepository =
                new ServiceRepository(this.m4jdslFactory);

        final ProtocolLayerEFSMGenerator protocolLayerEFSMGenerator =
                new ProtocolLayerEFSMGenerator(
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

        final BehaviorModelsGenerator behaviorModelGenerator =
                new BehaviorModelsGenerator(m4jdslFactory, serviceRepository, new IdGenerator("BM"));

        final BehaviorMixGenerator behaviorMixGenerator =
                new BehaviorMixGenerator(this.m4jdslFactory);

        // create components;

        final WorkloadModel workloadModel =
                this.m4jdslFactory.createWorkloadModel();

        final WorkloadIntensity workloadIntensity =
                workloadIntensityGenerator.generateWorkloadIntensity(workloadIntensityProperties);  // ~ exception;

        final ApplicationModel applicationModel =
                applicationModelGenerator.generateApplicationModel();

        final List<BehaviorModel> behaviorModels =
                behaviorModelGenerator.generateBehaviorModels();

        final HashMap<String, Double> behaviorMixEntries =
                new HashMap<String, Double>();

        behaviorMixEntries.put("name", 1.0);

        final BehaviorMix behaviorMix =
                behaviorMixGenerator.generateBehaviorMix(behaviorMixEntries, behaviorModels);

        // set components;

        workloadModel.setWorkloadIntensity(workloadIntensity);
        workloadModel.setApplicationModel(applicationModel);

        // TODO: Behavior Models need to be set by Behavior Model Extractor;
        workloadModel.setBehaviorMix(behaviorMix);

        for (final RelativeFrequency r : behaviorMix.getRelativeFrequencies()) {

            final BehaviorModel behaviorModel = r.getBehaviorModel();
            workloadModel.getBehaviorModels().add(behaviorModel);
        }

        return workloadModel;
    }

    private static void printUsage () {

        final String appName = M4jdslModelGenerator.class.getSimpleName();

        System.out.println("Usage: " + appName + " <generator.properties> <flowsDirPath> <graphOutputFile.dot>");
    }

    public static void main (final String[] argv) {

        if (argv.length < 3) {

            M4jdslModelGenerator.printUsage();

        } else {

            final String propertiesFilename = argv[0];
            final String flowsDirectoryPath = argv[1];
            final String graphFilePath      = argv[2];  // TODO: make this optional;

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

                final String outputFile = generatorProperties.
                        getProperty(M4jdslModelGenerator.PKEY_XMI_OUTPUT_FILE);

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