package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import m4jdsl.ApplicationExitState;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.SessionLayerEFSMState;
import net.sf.markov4jmeter.gear.flowdsl.Flow;
import net.sf.markov4jmeter.gear.flowdsl.FlowRepository;
import net.sf.markov4jmeter.gear.flowdsl.Node;
import net.sf.markov4jmeter.gear.flowdsl.Transition;
import net.sf.markov4jmeter.m4jdslmodelgenerator.FlowDSLParser;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.DotGraphGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.FlowDotGraphGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class FlowSessionLayerEFSMGenerator extends
        AbstractSessionLayerEFSMGenerator {

    private final static String FLOW_START_NODE_NAME = "Start";

    public final static String INITIAL_STATE__SERVICE_NAME = "Init*";
    private final static String EXIT_STATE_NAME = "$";

    private final static String FLOW_END_NODE_NAME   = "End";

    private final static String ERROR_FLOWS_PARSING_FAILED =
            "could not parse flows in directory \"%s\" properly (%s)";

    private final static String ERROR_UNKNOWN_NODE =
            "transition target \"%s\" in node \"%s\" of flow \"%s\" is "
            + "unknown in that flow";

    private final static String WARNING_GRAPH_OUTPUT_FILE_COULD_NOT_BE_WRITTEN =
            "graph output file could not be written to \"%s\"";

    private final static String DEBUG_INFO__INSTALLED_STATE =
            "installed state: \"%s\" (id: \"%s\")";

    private final static String DEBUG_INFO__INSTALLED_TRANSITION =
            "installed transition: \"%s\" --[%s][%s]--> \"%s\"";

    private final static String DEBUG_ERROR__SERVICE_REPOSITORY_INCONSISTENT =
            "service repository is inconsistent";

    private final static boolean DEBUG = true;


    /* *************************  global variables  ************************* */


    private final FlowDotGraphGenerator dotGraphGenerator;

    private final String flowsDirectoryPath;
    private final String graphFilePath;


    /* ***************************  constructors  *************************** */


    public FlowSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final String flowsDirectoryPath,
            final String graphFilePath) {

        super(m4jdslFactory,
              serviceRepository,
              protocolLayerEFSMGenerator,
              idGenerator);

        this.flowsDirectoryPath = flowsDirectoryPath;
        this.graphFilePath      = graphFilePath;

        this.dotGraphGenerator  =
                (graphFilePath != null) ? new FlowDotGraphGenerator() : null;
    }

    public FlowSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final String flowsDirectoryPath) {

        this(m4jdslFactory,
             serviceRepository,
             protocolLayerEFSMGenerator,
             idGenerator,
             flowsDirectoryPath,
             null);  // no graph file path;
    }


    /* **************************  public methods  ************************** */


    @Override
    public SessionLayerEFSM generateSessionLayerEFSM ()
            throws GeneratorException {

        // to be returned;
        final SessionLayerEFSM sessionLayerEFSM =
                this.createEmptySessionLayerEFSM(
                        FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME);

        final HashMap<Service, ApplicationState> serviceAppStateHashMap =
                new HashMap<Service, ApplicationState>();

        // might throw a GeneratorException;
        final FlowRepository flowRepository = this.parseFlows();

        this.installGenericInitialAndExitStates(
                sessionLayerEFSM,
                serviceAppStateHashMap);

        this.installFlowStates(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository);

        this.installGenericTransitions(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository);

        this.installFlowTransitions(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository);

        this.writeGraph(this.graphFilePath);

        return sessionLayerEFSM;
    }


    /* *********  private methods (Application States installation)  ******** */


    private void installGenericInitialAndExitStates (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        // exit state already exists in (default) Session Layer EFSM -> get ID;
        final String exitStateId = sessionLayerEFSM.getExitState().getEId();

        final Service initialService = this.createService(
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME);

        final ApplicationState applicationInitialState =
                this.createApplicationState(
                        initialService,
                        this.createDefaultProtocolLayerEFSM());  // FIXME: create generic Protocol Layer EFSM;

        applicationInitialState.setEId(
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME);

        // (generic) initial state must be registered as a non-exit state;
        serviceAppStateHashMap.put(initialService, applicationInitialState);
        sessionLayerEFSM.setInitialState(applicationInitialState);

        // note: initial state is not installed yet, but registered as initial
        // state; its installation will be done in installFlowStates();

        this.printDebugInfo(
                FlowSessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_STATE,
                exitStateId,
                exitStateId);

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.addState(
                    initialService.getName(),
                    DotGraphGenerator.STATE_SHAPE_POINT);

            this.dotGraphGenerator.addState(
                    exitStateId,
                    DotGraphGenerator.STATE_SHAPE_DOUBLE_CIRCLE);
        }
    }

    @SuppressWarnings("unused")  // ignore dead code, if DEBUG is set to false;
    private void installFlowStates (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final FlowRepository flowRepository) {

        // collect all Application States indicated by nodes;
        this.collectApplicationStates(
                flowRepository,
                serviceAppStateHashMap);

        final List<ApplicationState> applicationStates =
                sessionLayerEFSM.getApplicationStates();

        final List<Service> services = this.serviceRepository.getServices();

        // just ensure that all collected services are in the repository;
        if (FlowSessionLayerEFSMGenerator.DEBUG &&
            !this.isServiceRepositoryConsistent(
                    serviceAppStateHashMap.keySet(),
                    services)) {

            System.err.println(
                    FlowSessionLayerEFSMGenerator.
                    DEBUG_ERROR__SERVICE_REPOSITORY_INCONSISTENT);
        }

        for (final Service service : services) {

            final ApplicationState as = serviceAppStateHashMap.get(service);

            applicationStates.add(as);

            if (this.dotGraphGenerator != null) {

                this.dotGraphGenerator.addState(
                        service.getName(),
                        DotGraphGenerator.STATE_SHAPE_ELLIPSE);
            }

            this.printDebugInfo(
                    FlowSessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_STATE,
                    as.getService().getName(),
                    as.getEId());
        }
    }

    private boolean isServiceRepositoryConsistent (
            final Set<Service> servicesSet,
            final List<Service> services) {

        if (servicesSet.size() != services.size()) {

            return false;
        }

        for (final Service service : servicesSet) {

            if ( !services.contains(service) ) {

                return false;
            }
        }

        return true;
    }

    private void collectApplicationStates (
            final FlowRepository flowRepository,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        for (final Flow flow : flowRepository.getFlows()) {

            final String flowName = flow.getName();

            for (final Node node : flow.getNodes()) {

                final String nodeName = node.getName();

                this.registerApplicationState(
                        flowName,
                        nodeName,
                        serviceAppStateHashMap);
            }
        }
    }

    private void registerApplicationState (
            final String flowName,
            final String nodeName,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        final String serviceName =
                this.getFullyQualifiedName(flowName, nodeName);

        final Service service = this.createService(serviceName);

        if ( !serviceAppStateHashMap.containsKey(service) ) {

            final ProtocolLayerEFSM protocolLayerEFSM =
                    this.createDefaultProtocolLayerEFSM();  // FIXME: add generic Protocol Layer EFSM;

            // create new Application State for service;
            final ApplicationState applicationState =
                    this.createApplicationState(service, protocolLayerEFSM);

            // register new Application State;
            serviceAppStateHashMap.put(service, applicationState);
        }
    }


    /* ******  private methods (Application Transitions installation)  ****** */


    private void installGenericTransitions (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final FlowRepository flowRepository) {

        // install transitions from initial state to all "Start" states;

        String sourceServiceName =
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;

        for (final Flow flow : flowRepository.getFlows()) {

            final Node node = this.findNodeByName(
                    flow,
                    FlowSessionLayerEFSMGenerator.FLOW_START_NODE_NAME);

            if (node != null) {

                final String targetServiceName = this.getFullyQualifiedName(
                        flow.getName(),
                        node.getName());

                this.installApplicationTransition(
                        sourceServiceName,
                        targetServiceName,
                        "",  // guard, always empty by default;
                        "",  // action, always empty be default;
                        serviceAppStateHashMap);
            }
        }

        // install transitions from all "End" states to exit state;

        final String targetServiceName =
                FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME;

        for (final Flow flow : flowRepository.getFlows()) {

            for (final Node node : flow.getNodes()) {

                if ( node.getTransitions().isEmpty() &&
                     !this.isFlowReference(flowRepository, node)) {

                    sourceServiceName = this.getFullyQualifiedName(
                            flow.getName(),
                            node.getName());

                    this.installApplicationTransition(
                            sourceServiceName,
                            targetServiceName,
                            "",  // guard, always empty by default;
                            "",  // action, always empty be default;
                            serviceAppStateHashMap,
                            sessionLayerEFSM.getExitState());
                }
            }
        }
    }

    private boolean isFlowReference (
            final FlowRepository flowRepository,
            final Node node) {

        final String nodeName = node.getName();
        final Flow   refFlow  = this.findTargetFlow(flowRepository, nodeName);

        return refFlow != null;
    }

    private void installFlowTransitions (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final FlowRepository flowRepository) {

        final HashMap<ApplicationState, HashSet<ApplicationTransition>>
        serviceAppTransitionsHashMap =
                new HashMap<ApplicationState, HashSet<ApplicationTransition>>();

        // collect all Application States indicated by nodes;
        this.collectApplicationTransitions(
                flowRepository,
                serviceAppStateHashMap,
                serviceAppTransitionsHashMap);
    }


    private void collectApplicationTransitions (
            final FlowRepository flowRepository,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final HashMap<ApplicationState, HashSet<ApplicationTransition>>
            serviceAppTransitionsHashMap) {

        for (final Flow flow : flowRepository.getFlows()) {

            final String flowName = flow.getName();

            for (final Node node : flow.getNodes()) {

                final String nodeName = node.getName();

                final String sourceServiceName =
                        this.getFullyQualifiedName(flowName, nodeName);

                final List<Transition> transitions = node.getTransitions();

                if ( transitions.isEmpty() ) {

                    // node has no transitions --> reference to next Flow?
                    final Flow refFlow =
                            this.findTargetFlow(flowRepository, nodeName);

                    if (refFlow != null) {

                        // flow found --> find "Start" node;
                        final Node targetNode = this.findNodeByName(
                                refFlow,
                                FlowSessionLayerEFSMGenerator.FLOW_START_NODE_NAME);

                        if (targetNode != null) {

                            final String targetServiceName =
                                    this.getFullyQualifiedName(
                                            refFlow.getName(),
                                            targetNode.getName());

                            this.installApplicationTransition(
                                    sourceServiceName,
                                    targetServiceName,
                                    "",  // guard, always empty by default;
                                    "",  // action, always empty be default;
                                    serviceAppStateHashMap);
                        } else {

                            // no "Start" node in flow (should never happen);
                            // TODO: give warning message anyway;
                        }
                    }

                } else {  // !transitions.isEmpty();

                    for (final Transition transition : transitions) {

                        final String event  = transition.getEvent().getValue();
                        final String guard  = transition.getGuard().getValue();
                        final String action = transition.getAction().getValue();
                        final String target = transition.getTarget().getValue();

                        final Node targetNode =
                                this.findNodeByName(flow, target);

                        if (targetNode == null) {

                            // target node is unknown within flow;

                            final String message = String.format(
                                    FlowSessionLayerEFSMGenerator.
                                    ERROR_UNKNOWN_NODE,
                                    target,
                                    nodeName,
                                    flowName);

                            System.out.println(message);

                        } else {  // FIXME: check End node -> $

                            final String targetServiceName =
                                    this.getFullyQualifiedName(
                                            flowName,
                                            targetNode.getName());

                            this.installApplicationTransition(
                                    sourceServiceName,
                                    targetServiceName,
                                    this.getTransitionGuard(event, guard),
                                    this.getTransitionAction(action),
                                    serviceAppStateHashMap);
                        }
                    }
                }
            }
        }
    }

    private String getTransitionGuard (final String event, final String guard) {

        String str = "";

        if ( !"".equals(event) ) {

            str += ("${event}=\"" + event + "\"");
        }

        if ( !"".equals(guard) ) {

            if ( !"".equals(str) ) {

                str += " && ";
            }

            str += ("${guard}=\"" + guard + "\"");
        }

        return str;
    }

    private String getTransitionAction (final String action) {

        return !"".equals(action) ? ("${action}=\"" + action + "\"") : "";
    }

    private Flow findTargetFlow (
            final FlowRepository flowRepository,
            final String flowName) {

        Flow refFlow = this.findFlowByName(flowRepository, flowName);

        if (refFlow == null && flowName.endsWith("Call")) {

            // second try; remove "Call" suffix from name;
            //final String flowNameNoCall = flowName.replaceFirst("Call$", "");
            final int endIndex = flowName.length() - 4;
            final String flowNameNoCall = flowName.substring(0, endIndex);

            refFlow = this.findFlowByName(flowRepository, flowNameNoCall);
        }

        return refFlow;  // might be null, if no flow has been found;
    }

    private Node findNodeByName (final Flow flow, final String name) {

        for (final Node node : flow.getNodes()) {

            if ( name.equals(node.getName()) ) {

                return node;
            }
        }

        return null;  // no matching node found;
    }

    private Flow findFlowByName (
            final FlowRepository flowRepository,
            final String name) {

        for (final Flow flow : flowRepository.getFlows()) {

            if ( name.equals(flow.getName()) ) {

                return flow;
            }
        }

        return null;  // no matching flow found;
    }

    private void installApplicationTransition (
            final String sourceServiceName,
            final String targetServiceName,
            final String guard,
            final String action,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        this.installApplicationTransition(
                sourceServiceName,
                targetServiceName,
                guard,
                action,
                serviceAppStateHashMap,
                null);
    }

    private void installApplicationTransition (
            final String sourceServiceName,
            final String targetServiceName,
            final String guard,
            final String action,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final ApplicationExitState applicationExitState) {

        final ApplicationState source = this.findApplicationStateByServiceName(
                sourceServiceName,
                serviceAppStateHashMap);

        final SessionLayerEFSMState target =
                FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME.equals(
                        targetServiceName) ?
                                applicationExitState :
                                this.findApplicationStateByServiceName(
                                        targetServiceName,
                                        serviceAppStateHashMap);

        if (source == null || target == null) {

            System.out.println("=== transition installation failed (source: \"" + sourceServiceName + "\", target: \"" + targetServiceName + "\")");
            return;
        }

        final ApplicationTransition transition =
                this.m4jdslFactory.createApplicationTransition();

        transition.setAction(action);
        transition.setGuard(guard);
        transition.setTargetState(target);

        source.getOutgoingTransitions().add(transition);

        this.printDebugInfo(
                FlowSessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_TRANSITION,
                sourceServiceName,
                guard,
                action,
                targetServiceName);

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.addTransition(
                    sourceServiceName,
                    targetServiceName,
                    DotGraphGenerator.TRANSITION_STYLE_SOLID,
                    guard,
                    action);
        }
    }

    private ApplicationState findApplicationStateByServiceName (
            final String serviceName,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        for (final Service service : serviceAppStateHashMap.keySet()) {

            if ( service.getName().equals(serviceName) ) {

                return serviceAppStateHashMap.get(service);
            }
        }

        return null;  // no matching state found;
    }


    /* *********************  private methods (helpers)  ******************** */


    private FlowRepository parseFlows () throws GeneratorException {

        final File flowsDirectory = new File(this.flowsDirectoryPath);
        final FlowDSLParser parser = new FlowDSLParser();

        try {

            return (FlowRepository) parser.parse(flowsDirectory);

        } catch (final Exception ex) {

            final String message = String.format(
                    FlowSessionLayerEFSMGenerator.ERROR_FLOWS_PARSING_FAILED,
                    flowsDirectory.getAbsolutePath(),
                    ex.getMessage());

            throw new GeneratorException(message);
        }
    }

    private String getFullyQualifiedName (
            final String flowName,
            final String nodeName) {

        return flowName + "." + nodeName;
    }

    private ProtocolLayerEFSM createDefaultProtocolLayerEFSM () {

        final ProtocolLayerEFSM protocolLayerEFSM =
                this.protocolLayerEFSMGenerator.generateProtocolLayerEFSM();

        return protocolLayerEFSM;
    }

    private void printDebugInfo (final String template, Object... args) {

        if (FlowSessionLayerEFSMGenerator.DEBUG) {

            final String message = String.format(template, args);

            System.out.println("DEBUG INFO -- " + message);
        }
    }

    private void writeGraph (final String filePath) {

        if (this.dotGraphGenerator != null) {

            try {

                // might throw a Security- or IOException; throws a
                // NullPointerException, if "filePath" is null;
                this.dotGraphGenerator.writeGraphToFile(filePath);

            } catch (final SecurityException
                         | IOException
                         | NullPointerException ex) {

                final String message = String.format(
                        FlowSessionLayerEFSMGenerator.
                        WARNING_GRAPH_OUTPUT_FILE_COULD_NOT_BE_WRITTEN,
                        filePath);

                System.out.println(message);
            }
        }
    }
}