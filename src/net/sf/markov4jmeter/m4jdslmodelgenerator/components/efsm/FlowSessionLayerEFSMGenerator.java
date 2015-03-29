package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import synoptic.invariants.BinaryInvariant;
import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.TemporalInvariantSet;
import synoptic.main.AbstractMain;
import synoptic.main.SynopticMain;
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

/**
 * Class for building Session Layer EFSMs based on Flows.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class FlowSessionLayerEFSMGenerator extends
        AbstractSessionLayerEFSMGenerator {


    /* *************************  global constants  ************************* */


    /** Name of each Flow's start node. */
    private final static String FLOW_START_NODE_NAME = "Start";

    /** (Service-)name of the Application Layer's generic initial state. */
    public final static String INITIAL_STATE__SERVICE_NAME = "Init*";

    /** (Service-)name of the Application Layer's generic exit state. */
    private final static String EXIT_STATE_NAME = "$";

    /** Error message for the case that the parsing of Flows fails for any
     *  reason. */
    private final static String ERROR_FLOWS_PARSING_FAILED =
            "could not parse flows properly (%s)";

    /** Error message for the case that a target node of a Flow transition does
     *  not refer to node which is defined in the related Flow. */
    private final static String ERROR_UNKNOWN_NODE =
            "transition target \"%s\" in node \"%s\" of flow \"%s\" is not "
            + "defined in that flow";

    /** Warning message for the case that no initial state has been detected. */
    private final static String WARNING_NO_INITIAL_STATE =
            "could not detect any initial state; "
            + "will choose first available state \"%s\"";
    

    /* ----------------------  debug messages/settings  --------------------- */


    /** Debug information message for a state installation. */
    private final static String DEBUG_INFO__INSTALLED_STATE =
            "installed state: \"%s\" (id: \"%s\")";

    /** Debug information message for a transition installation. */
    private final static String DEBUG_INFO__INSTALLED_TRANSITION =
            "installed transition: \"%s\" --[%s][%s]--> \"%s\"";

    /** Debug information message for an initial state detection. */
    private final static String DEBUG_INFO__DETECTED_INITIAL_STATE =
            "detected initial state: \"%s\"";

    /** Debug error message for the case that the service repository does not
     *  include an expected set of services. */
    private final static String DEBUG_ERROR__SERVICE_REPOSITORY_INCONSISTENT =
            "service repository is inconsistent";

    /** <code>true</code> if and only if debugging shall be enabled. */
    private final static boolean DEBUG = true;


    /* *************************  global variables  ************************* */


    /** Flow files to be read. */
    private final File[] flowFiles;

    /** <code>true</code> if and only if sessions can be exited at any time,
     *  which is generally given in Web applications; if this flag is set
     *  <code>true</code>, transitions to the exit state will be installed for
     *  all states of the Application Layer. */
    private final boolean sessionsCanBeExitedAnytime;

    /** <code>true</code> if and only if fully qualified state names shall be
     *  used; if this flag is <code>false</code>, plain Node names will be used
     *  as state names, without any related Flow names being added as prefixes.
     */
    private final boolean useFullyQualifiedNames;
    
    /**
     * Invariants from Synoptic.
     */
    private TemporalInvariantSet invariants = null; 


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Flow Session Layer EFSM Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param serviceRepository
     *     instance for storing the <code>Service</code> instances which are
     *     included in the Session Layer EFSM.
     * @param protocolLayerEFSMGenerator
     *     instance for creating Protocol Layer EFSMs.
     * @param idGenerator
     *     instance for creating unique Application State IDs.
     * @param sessionsCanBeExitedAnytime
     *     <code>true</code> if and only if sessions can be exited at any time,
     *     which is generally given in Web applications; if this flag is set
     *     <code>true</code>, transitions to the exit state will be installed
     *     for all states of the Application Layer.
     * @param useFullyQualifiedNames
     *     <code>true</code> if and only if fully qualified state names shall
     *     be used; if this flag is <code>false</code>, plain Node names will be
     *     used as state names, without any related Flow names being added as
     *     prefixes.
     * @param flowFiles
     *     Flow files to be read.
     * @param graphFilePath
     *     path of the DOT graph output file; might be <code>null</code>, if no
     *     graph shall be generated.
     */
    public FlowSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final boolean sessionsCanBeExitedAnytime,
            final boolean useFullyQualifiedNames,
            final File[] flowFiles,
            final String graphFilePath) {

        super(m4jdslFactory,
              serviceRepository,
              protocolLayerEFSMGenerator,
              idGenerator,
              graphFilePath,
              (graphFilePath != null) ? new FlowDotGraphGenerator() : null );

        this.sessionsCanBeExitedAnytime = sessionsCanBeExitedAnytime;
        this.useFullyQualifiedNames     = useFullyQualifiedNames;
        this.flowFiles = flowFiles;
    }

    /**
     * Constructor for a Flow Session Layer EFSM Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param serviceRepository
     *     instance for storing the <code>Service</code> instances which are
     *     included in the Session Layer EFSM.
     * @param protocolLayerEFSMGenerator
     *     instance for creating Protocol Layer EFSMs.
     * @param idGenerator
     *     instance for creating unique Application State IDs.
     * @param sessionsCanBeExitedAnytime
     *     <code>true</code> if and only if sessions can be exited at any time,
     *     which is generally given in Web applications; if this flag is set
     *     <code>true</code>, transitions to the exit state will be installed
     *     for all states of the Application Layer.
     * @param useFullyQualifiedNames
     *     <code>true</code> if and only if fully qualified state names shall
     *     be used; if this flag is <code>false</code>, plain Node names will be
     *     used as state names, without any related Flow names being added as
     *     prefixes.
     * @param flowFiles
     *     Flow files to be read.
     */
    public FlowSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final boolean sessionsCanBeExitedAnytime,
            final boolean useFullyQualifiedNames,
            final File[] flowFiles) {

        this(m4jdslFactory,
             serviceRepository,
             protocolLayerEFSMGenerator,
             idGenerator,
             sessionsCanBeExitedAnytime,
             useFullyQualifiedNames,
             flowFiles,
             null);  // no graph file path;
    }


    /* **************************  public methods  ************************** */


    /**
     * {@inheritDoc}
     * <p> This method creates an EFSM which builds on <b>b+m gear Flows</b>.
     */
    @Override
    public SessionLayerEFSM generateSessionLayerEFSM ()
            throws GeneratorException {
    	
    	// get invariant from synoptic
    	this.getTemporalInvariants();

        // EFSM to be returned;
        final SessionLayerEFSM sessionLayerEFSM =
                this.createEmptySessionLayerEFSM(
                        FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME);

        final HashMap<Service, ApplicationState> serviceAppStateHashMap =
                new HashMap<Service, ApplicationState>();

        // might throw a GeneratorException;
        final FlowRepository flowRepository = this.parseFlows();
        
       
        final Service initialService = this.determineInitialService(
                flowRepository,
                sessionLayerEFSM,
                serviceAppStateHashMap);

        this.installGenericExitState(
                sessionLayerEFSM,
                serviceAppStateHashMap);

        this.installFlowStates(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository,
                initialService);

        sessionLayerEFSM.setInitialState(
                serviceAppStateHashMap.get(initialService));

        if ( this.isGenericInitialService(initialService) ) {

            this.installGenericTransitionsFromInitialState(
                    sessionLayerEFSM,
                    serviceAppStateHashMap,
                    flowRepository);
        }

        this.installGenericTransitionsToExitState(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository);

        this.installFlowTransitions(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                flowRepository);

        return sessionLayerEFSM;
    }
    

    /* *********  private methods (Application States installation)  ******** */
    
	private void getTemporalInvariants() {    	
    	String[] args = new String[7];  
        args[0] = "-r";
        args[1] = "[$1]+;[0-9]*;(?<TYPE>[\\w_+-]*);(?<ip>[\\w+-]*).[\\w;.-]*";
        args[2] = "-m";
        args[3] = "\\k<ip>";
        args[4] = "-i";
//        args[5] = "-o";
//        args[6] = ""; //"C:/Users/voegele/Applications/eclipse-jee-kepler-SR2-win32-x86_64/eclipse/workspace/Synoptic/output/output";
//        args[5] = "-d";
//        args[6] = ""; // "C:/Program Files (x86)/Graphviz2.38/bin/gvedit.exe";
        args[5] = "--dumpInvariants=true";
        args[6] = "examples/specj/input/logFiles/SPECjlog.log";

        SynopticMain.getInstance();
		try {
			SynopticMain.main(args);
			this.invariants = AbstractMain.getInvariants();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	private String getGuard(final Transition transition) {
		List<String> guards = new ArrayList<String>();
		for (ITemporalInvariant invariant : this.invariants.getSet()) {
			if (invariant instanceof BinaryInvariant) {
				BinaryInvariant binaryInvariant = (BinaryInvariant) invariant;							
				// invariant AlwaysFollowedBy can not be used
				if (binaryInvariant.getLongName().equals("AlwaysPrecedes")) {
					if (transition.getTarget().getValue().equals(binaryInvariant.getSecond().toString())) {
						String guard = "${" + binaryInvariant.getFirst().toString()  + "}";
    					if (!guards.contains(guard)) {
    						guards.add(guard);
    					} 
					}    					
    			} else if (binaryInvariant.getLongName().equals("NeverFollowedBy")) {
    				if (transition.getTarget().getValue().equals(binaryInvariant.getSecond().toString())) {
    				    String guard = "!${" + binaryInvariant.getFirst().toString()  + "}";
    					if (!guards.contains(guard)) {
    						guards.add(guard);
    					} 
    			    }	
			   }
		   }
		}
		
		if (guards.size() == 0) {
			return "";
		} else if (guards.size() == 1) {
			return guards.get(0);
		} else {
			String returnString = "";
			for (int i = 0; i< guards.size(); i++) {			
				returnString += guards.get(i);
				if (i != guards.size() -1) {
					returnString += " && ";
				}				
			}	
			return returnString;
		}
	}      
	
	private String getAction(final Transition transition) {
		return transition.getTarget().getValue() + "=true";
	}

    private boolean isGenericInitialService (final Service initialService) {

        return FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME.equals(
                initialService.getName());
    }

    private Service determineInitialService (
            final FlowRepository flowRepository,
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap)
                    throws GeneratorException {

        final Service initialService;

        final LinkedList<Service> initialServices =
                this.findInitialServices(flowRepository);

        switch (initialServices.size()) {

            case 0 :  // no initial service detected;

                // ensure that at least one initial service is available;
                initialService = this.findFirstService(flowRepository);

                this.warn(
                        FlowSessionLayerEFSMGenerator.WARNING_NO_INITIAL_STATE,
                        initialService.getName());

                break;

            case 1 :  // unique initial service detected;

                initialService = initialServices.get(0);
                break;

            default : // n > 1  ->   multiple initial services detected;

                initialService = this.installGenericInitialState(
                        sessionLayerEFSM,
                        serviceAppStateHashMap);
        }

        return initialService;
    }

    private LinkedList<Service> findInitialServices (
            final FlowRepository flowRepository) {

        // initial services to be returned;
        final LinkedList<Service> initialServices = new LinkedList<Service>();

        for (final Flow flow : flowRepository.getFlows()) {

            final String flowName = flow.getName();

            for (final Node node : flow.getNodes()) {

                final String nodeName = node.getName();

                if ( this.isInitialNode(flowName, nodeName, flowRepository) ) {

                    final String serviceName =
                            this.getFullyQualifiedName(flowName, nodeName);

                    initialServices.add(this.createService(serviceName));

                    if (FlowSessionLayerEFSMGenerator.DEBUG) {

                        this.printDebugInfo(
                                FlowSessionLayerEFSMGenerator.
                                DEBUG_INFO__DETECTED_INITIAL_STATE,
                                serviceName);
                    }
                }
            }
        }

        return initialServices;
    }

    private boolean isInitialNode (
            final String flowName,
            final String nodeName,
            final FlowRepository flowRepository) {

        if (FlowSessionLayerEFSMGenerator.FLOW_START_NODE_NAME.equals(
                nodeName)) {

            return this.isUnreferencedFlow(flowName, flowRepository);

        } else {

            final Flow flow = this.findFlowByName(flowRepository, flowName);

            return this.isUnreferencedNode(flow, nodeName);
        }
    }

    private boolean isUnreferencedFlow (
            final String flowName,
            final FlowRepository flowRepository) {

        for (final Flow flow : flowRepository.getFlows()) {

            for (final Node node : flow.getNodes()) {

                if (node.getTransitions().size() == 0) {  // is node reference?

                    final String nodeName = node.getName();

                    if ( nodeName.equals(flowName) ) {

                        return false;  // found reference to flow;
                    }

                    // "Call" might be appended to node name optionally;
                    if ( nodeName.endsWith("Call") ) {

                        final String nodeNameNoCall =
                                this.removeCallSuffix(nodeName);

                        if ( nodeNameNoCall.equals(flowName) ) {

                            return false;  // found reference to flow;
                        }
                    }
                }
            }
        }

        return true;  // no reference to flow found;
    }

    private boolean isUnreferencedNode (
            final Flow flow,
            final String nodeName) {

        for (final Node node : flow.getNodes()) {

            for (final Transition transition : node.getTransitions()) {

                final String targetName = transition.getTarget().getValue();

                if ( targetName.equals(nodeName) ) {

                    return false;  // found reference to node;
                }
            }
        }

        return true;  // no transition targets the node in the given flow;
    }

    private Service findFirstService(final FlowRepository flowRepository) {

        for (final Flow flow : flowRepository.getFlows()) {

            final List<Node> nodes = flow.getNodes();

            if (nodes.size() > 0) {

                final String flowName = flow.getName();
                final String nodeName = nodes.get(0).getName();

                final String fullyQualifiedName =
                        this.getFullyQualifiedName(flowName, nodeName);

                return this.createService(fullyQualifiedName);
            }
        }

        return null;  // no node available (should never happen);
    }

    /**
     * Installs a (unique) generic initial state, if multiple initial states are
     * available.
     *
     * @param sessionLayerEFSM
     * @param serviceAppStateHashMap
     * @throws GeneratorException
     */
    private Service installGenericInitialState (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        final String serviceName =
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;

        final Service initialService = this.createService(serviceName);

        // might throw a GeneratorException;
        final ApplicationState applicationInitialState =
                this.createApplicationState(
                        initialService,
                        this.createDefaultProtocolLayerEFSM(serviceName));  // FIXME: create specific Protocol Layer EFSM;

        applicationInitialState.setEId(
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME);

        // (generic) initial state must be registered as a non-exit state;
        serviceAppStateHashMap.put(initialService, applicationInitialState);

        return initialService;
    }

    /**
     * Installs the generic exit state.
     *
     * @param sessionLayerEFSM
     * @param serviceAppStateHashMap
     * @throws GeneratorException
     */
    private void installGenericExitState (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        // exit state already exists in (default) Session Layer EFSM -> get ID;
        final String exitStateId = sessionLayerEFSM.getExitState().getEId();

        this.printDebugInfo(
                FlowSessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_STATE,
                exitStateId,
                exitStateId);

        this.addDotState(
                exitStateId,
                DotGraphGenerator.STATE_SHAPE_DOUBLE_CIRCLE);
    }

    private void installFlowStates (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final FlowRepository flowRepository,
            final Service initialService) throws GeneratorException {

        // collect all Application States indicated by nodes;
        // might throw a GeneratorException;
        this.collectApplicationStates(
                flowRepository,
                serviceAppStateHashMap);

        final List<ApplicationState> applicationStates =
                sessionLayerEFSM.getApplicationStates();

        final List<Service> services = this.serviceRepository.getServices();

        // just ensure that all collected services are in the repository;
        if ( FlowSessionLayerEFSMGenerator.DEBUG &&
             !this.isServiceRepositoryConsistent(
                     serviceAppStateHashMap.keySet(),
                     services) ) {

            System.err.println(
                    FlowSessionLayerEFSMGenerator.
                    DEBUG_ERROR__SERVICE_REPOSITORY_INCONSISTENT);
        }

        for (final Service service : services) {

            final ApplicationState as = serviceAppStateHashMap.get(service);

            applicationStates.add(as);

            final String shape = (service == initialService) ?
                    DotGraphGenerator.STATE_SHAPE_SEPTAGON :
                    DotGraphGenerator.STATE_SHAPE_ELLIPSE;

            this.addDotState(service.getName(), shape);

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
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        for (final Flow flow : flowRepository.getFlows()) {

            final String flowName = flow.getName();

            for (final Node node : flow.getNodes()) {

                final String nodeName = node.getName();

                // might throw a GeneratorException;
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
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        final String serviceName =
                this.getFullyQualifiedName(flowName, nodeName);

        final Service service = this.createService(serviceName);

        if ( !serviceAppStateHashMap.containsKey(service) ) {

            // might throw a GeneratorException;
            final ProtocolLayerEFSM protocolLayerEFSM =
                    this.createDefaultProtocolLayerEFSM(serviceName);  // FIXME: add generic Protocol Layer EFSM;

            // create new Application State for service;
            final ApplicationState applicationState =
                    this.createApplicationState(service, protocolLayerEFSM);

            // register new Application State;
            serviceAppStateHashMap.put(service, applicationState);
        }
    }


    /* ******  private methods (Application Transitions installation)  ****** */


    private void installGenericTransitionsFromInitialState (
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

        if (this.sessionsCanBeExitedAnytime) {

            this.installApplicationTransition(
                    sourceServiceName,
                    FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME,
                    "",  // guard, always empty by default;
                    "",  // action, always empty be default;
                    serviceAppStateHashMap,
                    sessionLayerEFSM.getExitState());
        }
    }

    /**
     * Installs transitions from all "End" states to the exit state.
     *
     * @param sessionLayerEFSM
     * @param serviceAppStateHashMap
     * @param flowRepository
     */
    private void installGenericTransitionsToExitState (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final FlowRepository flowRepository) {

        final String targetServiceName =
                FlowSessionLayerEFSMGenerator.EXIT_STATE_NAME;

        for (final Flow flow : flowRepository.getFlows()) {

            for (final Node node : flow.getNodes()) {

                if ( this.sessionsCanBeExitedAnytime ||
                     (node.getTransitions().isEmpty() &&
                      !this.isFlowReference(flowRepository, node)) ) {

                    final String sourceServiceName = this.getFullyQualifiedName(
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
                            // TODO: give warning message;
                        }
                    }

                } else {  // !transitions.isEmpty();

                    for (final Transition transition : transitions) {

                        final String event  = transition.getEvent().getValue();
                        final String guard  = getGuard(transition);
                        final String action = getAction(transition);
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

            final String flowNameNoCall = this.removeCallSuffix(flowName);

            refFlow = this.findFlowByName(flowRepository, flowNameNoCall);
        }

        return refFlow;  // might be null, if no flow has been found;
    }

    private String removeCallSuffix (final String flowName) {

        // second try; remove "Call" suffix from name;
        //final String flowNameNoCall = flowName.replaceFirst("Call$", "");
        final int endIndex = flowName.length() - 4;

        return flowName.substring(0, endIndex);
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

        this.addDotTransition(
                sourceServiceName,
                targetServiceName,
                DotGraphGenerator.TRANSITION_STYLE_SOLID,
                guard,
                action);
    }

    /**
     * Registers a transition to be generated; the transition label will be
     * built of a guard and an action, formatted as
     * [<i>guard</i>]/[<i>action</i>].
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants.
     * @param guard
     *     guard of the transition.
     * @param action
     *     action of the transition.
     */
    private void addDotTransition(
            final String source,
            final String target,
            final String style,
            final String guard,
            final String action) {

        if (this.dotGraphGenerator != null) {

            ((FlowDotGraphGenerator) this.dotGraphGenerator).addTransition(
                    source,
                    target,
                    style,
                    action,
                    guard);
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

        final FlowDSLParser parser = new FlowDSLParser();

        try {

            return (FlowRepository) parser.parse(flowFiles);

        } catch (final Exception ex) {

            final String message = String.format(
                    FlowSessionLayerEFSMGenerator.ERROR_FLOWS_PARSING_FAILED,
                    ex.getMessage());

            throw new GeneratorException(message);
        }
    }

    private String getFullyQualifiedName (
            final String flowName,
            final String nodeName) {

        return this.useFullyQualifiedNames ?
                flowName + "." + nodeName :
                nodeName;
    }

    private ProtocolLayerEFSM createDefaultProtocolLayerEFSM (
            final String serviceName) throws GeneratorException {

        // might throw a GeneratorException;
        final ProtocolLayerEFSM protocolLayerEFSM =
                this.protocolLayerEFSMGenerator.generateProtocolLayerEFSM(serviceName);

        return protocolLayerEFSM;
    }

    private void printDebugInfo (final String template, Object... args) {

        if (FlowSessionLayerEFSMGenerator.DEBUG) {

            final String message = String.format(template, args);

            System.out.println("DEBUG INFO -- " + message);
        }
    }

    private void warn (final String template, Object... args) {

        final String message = String.format(template, args);

        System.out.println("WARNING: " + message);
    }
}