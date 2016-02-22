/***************************************************************************
 * Copyright (c) 2016 the WESSBAS project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/


package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import m4jdsl.Action;
import m4jdsl.ApplicationExitState;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.Guard;
import m4jdsl.GuardActionParameterList;
import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.SessionLayerEFSMState;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.CSVHandler;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.DotGraphGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.FlowDotGraphGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Class for building Session Layer EFSMs based on Flows.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class SessionLayerEFSMGenerator extends
        AbstractSessionLayerEFSMGenerator {


    /* *************************  global constants  ************************* */

    /** (Service-)name of the Application Layer's generic initial state. */
    public final static String INITIAL_STATE__SERVICE_NAME = "Init*";

    /** (Service-)name of the Application Layer's generic exit state. */
    private final static String EXIT_STATE_NAME = "$";
    
    /** This STring identifies an initial State. */
    private final static String INITIAL_STATE_IDENTIFIER = "*";

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


    /** <code>true</code> if and only if sessions can be exited at any time,
     *  which is generally given in Web applications; if this flag is set
     *  <code>true</code>, transitions to the exit state will be installed for
     *  all states of the Application Layer. */
    private final boolean sessionsCanBeExitedAnytime;
    
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
     * @param graphFilePath
     *     path of the DOT graph output file; might be <code>null</code>, if no
     *     graph shall be generated.
     * @param filenames 
     *     behaviorModel filenames.    
     */
    public SessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final boolean sessionsCanBeExitedAnytime,
            final String graphFilePath,
            final File[] filenames) {

        super(m4jdslFactory,
              serviceRepository,
              protocolLayerEFSMGenerator,
              idGenerator,
              graphFilePath,
              (graphFilePath != null) ? new FlowDotGraphGenerator() : null,
              filenames);

        this.sessionsCanBeExitedAnytime = sessionsCanBeExitedAnytime;

    }
 
    /**
     * Constructor for a Session Layer EFSM Generator.
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
     */
    public SessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final boolean sessionsCanBeExitedAnytime,
            final File[] filenames) {

        this(m4jdslFactory,
             serviceRepository,
             protocolLayerEFSMGenerator,
             idGenerator,
             sessionsCanBeExitedAnytime,
             null, filenames);  // no graph file path;
    }


    /* **************************  public methods  ************************** */


    /**
     * {@inheritDoc}
     * <p> This method creates an EFSM.
     */
    @Override
    public SessionLayerEFSM generateSessionLayerEFSM ()
            throws GeneratorException {

        // EFSM to be returned;
        final SessionLayerEFSM sessionLayerEFSM =
                this.createEmptySessionLayerEFSM(
                        SessionLayerEFSMGenerator.EXIT_STATE_NAME);
        
        final GuardActionParameterList guardActionParameterList = 
        		createGuardActionParamterList();
        
        sessionLayerEFSM.setGuardActionParameterList(guardActionParameterList);
        
        final HashMap<Service, ApplicationState> serviceAppStateHashMap =
                new HashMap<Service, ApplicationState>();

        HashMap<String, List<String>> allowedTransitions = this.getAllAllowedTransitions( this.fileNames );          
       
        final Service initialService = this.determineInitialService(
        		allowedTransitions,
                sessionLayerEFSM,
                serviceAppStateHashMap);

        this.installGenericExitState(
                sessionLayerEFSM,
                serviceAppStateHashMap);

        this.installStates(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                allowedTransitions,
                initialService);

        sessionLayerEFSM.setInitialState(
                serviceAppStateHashMap.get(initialService));

        this.installGenericTransitionsToExitState(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                allowedTransitions);

        this.installFlowTransitions(
                sessionLayerEFSM,
                serviceAppStateHashMap,
                allowedTransitions);

        return sessionLayerEFSM;
    }
    

    /* *********  private methods (Application States installation)  ******** */
    
    private Service determineInitialService (
            final HashMap<String, List<String>> allowedTransitions,
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap)
                    throws GeneratorException {

        final Service initialService;

        final LinkedList<Service> initialServices =
                this.findInitialServices(allowedTransitions);

        switch (initialServices.size()) {

            case 0 :  // no initial service detected;

                // ensure that at least one initial service is available;
                initialService = this.findFirstService(allowedTransitions);

                this.warn(
                        SessionLayerEFSMGenerator.WARNING_NO_INITIAL_STATE,
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
            final HashMap<String, List<String>> allowedTransitions) {

        // initial services to be returned;
        final LinkedList<Service> initialServices = new LinkedList<Service>();

        for (String fromState : allowedTransitions.keySet() ) {        	

        	if (fromState.contains(INITIAL_STATE_IDENTIFIER)) {
        		
        		String fromStateName = getFullyQualifiedName(fromState);  
        		
        		initialServices.add(this.createService(fromStateName));

                if (SessionLayerEFSMGenerator.DEBUG) {

                    this.printDebugInfo(
                            SessionLayerEFSMGenerator.
                            DEBUG_INFO__DETECTED_INITIAL_STATE,
                            fromStateName);
                }
        		
        	} 	
            
        }

        return initialServices;
    }    
    
    private Service findFirstService (
            final HashMap<String, List<String>> allowedTransitions) {
    	
    	Service service = null;
    	
        for (String fromState : allowedTransitions.keySet() ) {            	
        		
        		String fromStateName = getFullyQualifiedName(fromState);  
        		
        		service = this.createService(fromStateName);
              
                break;
            
        }

        return service;
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
                SessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;

        final Service initialService = this.createService(serviceName);

        // might throw a GeneratorException;
        final ApplicationState applicationInitialState =
                this.createApplicationState(
                        initialService,
                        this.createDefaultProtocolLayerEFSM(serviceName));  // FIXME: create specific Protocol Layer EFSM;

        applicationInitialState.setEId(
                SessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME);

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
                SessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_STATE,
                exitStateId,
                exitStateId);

        this.addDotState(
                exitStateId,
                DotGraphGenerator.STATE_SHAPE_DOUBLE_CIRCLE);
    }

    private void installStates (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final HashMap<String, List<String>> allowedTransitions,
            final Service initialService) throws GeneratorException {

        // collect all Application States indicated by nodes;
        // might throw a GeneratorException;
        this.collectApplicationStates(
        		allowedTransitions,
                serviceAppStateHashMap);

        final List<ApplicationState> applicationStates =
                sessionLayerEFSM.getApplicationStates();
        
        final List<Service> services = this.serviceRepository.getServices();

        // just ensure that all collected services are in the repository;
        if ( SessionLayerEFSMGenerator.DEBUG &&
             !this.isServiceRepositoryConsistent(
                     serviceAppStateHashMap.keySet(),
                     services) ) {

            System.err.println(
                    SessionLayerEFSMGenerator.
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
                    SessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_STATE,
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
            final HashMap<String, List<String>> allowedTransitions,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        for (final String fromState : allowedTransitions.keySet()) {

                // might throw a GeneratorException;
                this.registerApplicationState(
                        fromState,
                        serviceAppStateHashMap);

        }
    }

    private void registerApplicationState (
            final String nodeName,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) throws GeneratorException {

        final String serviceName =
                this.getFullyQualifiedName(nodeName);

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
            final HashMap<String, List<String>> allowedTransitions) {

        final String targetServiceName =
                SessionLayerEFSMGenerator.EXIT_STATE_NAME;

        for (final String fromString : allowedTransitions.keySet()) {

            if ( this.sessionsCanBeExitedAnytime ) {

                final String sourceServiceName = this.getFullyQualifiedName(
                		fromString);

                this.installApplicationTransition(
                        sourceServiceName,
                        targetServiceName,
                        null,  // guard, always empty by default;
                        null,  // action, always empty be default;
                        serviceAppStateHashMap,
                        sessionLayerEFSM.getExitState());
            }

        }
    }

    private void installFlowTransitions (
            final SessionLayerEFSM sessionLayerEFSM,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final HashMap<String, List<String>> allowedTransitions) {

        final HashMap<ApplicationState, HashSet<ApplicationTransition>>
        serviceAppTransitionsHashMap =
                new HashMap<ApplicationState, HashSet<ApplicationTransition>>();

        // collect all Application States indicated by nodes;
        this.collectApplicationTransitions(
        		allowedTransitions,
                serviceAppStateHashMap,
                serviceAppTransitionsHashMap, sessionLayerEFSM);
    }


    private void collectApplicationTransitions (
            final HashMap<String, List<String>> allowedTransitions,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final HashMap<ApplicationState, HashSet<ApplicationTransition>>
            serviceAppTransitionsHashMap, 
            final SessionLayerEFSM sessionLayerEFSM) {

    		for (final String fromState : allowedTransitions.keySet()) {
    	
               final String sourceServiceName =
                        this.getFullyQualifiedName(fromState);

                final List<String> toStates = allowedTransitions.get(fromState);

                if ( !toStates.isEmpty() ) {

                    for (final String toString : toStates) {
                    	
                    	if (!toString.equals(EXIT_STATE_NAME)) {
	
	                       // final String event  = transition.getEvent().getValue();                        
	                       // final List<Guard> guards  = getGuard(transition, sessionLayerEFSM);
	                       // final List<Action> actions = getAction(transition, sessionLayerEFSM);                                
	                        final String targetServiceName = this.getFullyQualifiedName(toString);		
	
                            this.installApplicationTransition(
                                    sourceServiceName,
                                    targetServiceName,
                                    null,
                                    null,
                                    serviceAppStateHashMap);
                    	}
	    
                  }       
             }
        }
    }

    private void installApplicationTransition (
            final String sourceServiceName,
            final String targetServiceName,
            final List<Guard> guards,
            final List<Action> actions,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap) {

        this.installApplicationTransition(
                sourceServiceName,
                targetServiceName,
                guards,
                actions,
                serviceAppStateHashMap,
                null);
    }

    private void installApplicationTransition (
            final String sourceServiceName,
            final String targetServiceName,
            final List<Guard> guards,
            final List<Action> actions,
            final HashMap<Service, ApplicationState> serviceAppStateHashMap,
            final ApplicationExitState applicationExitState) {

        final ApplicationState source = this.findApplicationStateByServiceName(
                sourceServiceName,
                serviceAppStateHashMap);

        final SessionLayerEFSMState target =
                SessionLayerEFSMGenerator.EXIT_STATE_NAME.equals(
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

        if (actions != null) {
        	for (Action action : actions) {
        		transition.getAction().add(action);
        	}        	 
        }
        
        if (guards != null) {
        	for (Guard guard : guards) {
        		transition.getGuard().add(guard);
        	}  
        }
        
        transition.setTargetState(target);

        source.getOutgoingTransitions().add(transition);

        this.printDebugInfo(
                SessionLayerEFSMGenerator.DEBUG_INFO__INSTALLED_TRANSITION,
                "",
                "",
                sourceServiceName,
                targetServiceName);

        this.addDotTransition(
                sourceServiceName,
                targetServiceName,
                DotGraphGenerator.TRANSITION_STYLE_SOLID,
                "",
                "");
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

    private String getFullyQualifiedName (
            final String nodeName) {

    	String returnString = nodeName.replace(INITIAL_STATE_IDENTIFIER, "");
    	
        return returnString;
    }

    private ProtocolLayerEFSM createDefaultProtocolLayerEFSM (
            final String serviceName) throws GeneratorException {

        // might throw a GeneratorException;
        final ProtocolLayerEFSM protocolLayerEFSM =
                this.protocolLayerEFSMGenerator.generateProtocolLayerEFSM(serviceName);

        return protocolLayerEFSM;
    }

    private void printDebugInfo (final String template, Object... args) {

        if (SessionLayerEFSMGenerator.DEBUG) {

            final String message = String.format(template, args);

            System.out.println("DEBUG INFO -- " + message);
        }
    }

    private void warn (final String template, Object... args) {

        final String message = String.format(template, args);

        System.out.println("WARNING: " + message);
    }
    
    /**
     * Identify all allowed transitions based on the behaviorModels .csv files. 
     * 
     * @param fileNames 
     * 		behaviorModel fileNames
     * @return allAllowedTransitions
     */
    private HashMap<String, List<String>> getAllAllowedTransitions(final File[] fileNames) {   
    	HashMap<String, List<String>> allowedTransitions = new HashMap<String, List<String>>();
    	for (File file : fileNames) {    		
            final String[][] behaviorInformation =
                    this.readBehaviorInformation( file.getAbsolutePath() );            
            for (int row = 1; row < behaviorInformation.length; row++) {
            	String fromState = behaviorInformation[row][0];     
            	String toState = "";
            	for (int col = 1; col < behaviorInformation[row].length; col++) {            		
            		if (!behaviorInformation[row][col].contains("0.0;")) {
            			toState = behaviorInformation[0][col];            			
            			if (allowedTransitions.containsKey(fromState)) {
            				List<String> toTransitions = allowedTransitions.get(fromState);
            				if (!toTransitions.contains(toState)) {
            					toTransitions.add(toState);
                				allowedTransitions.put(fromState, toTransitions);
            				}
            			} else {
            				List<String> toTransitions = new ArrayList<String>();
            				if (!toTransitions.contains(toState)) {
            					toTransitions.add(toState);
                				allowedTransitions.put(fromState, toTransitions);
            				}
            			}                    			
            		}            		
            	}            	
            }        		
    	}       
    	return allowedTransitions;
    }
    
    /**
     * Reads the behavior information from a given CSV file.
     *
     * @param filename
     *     CSV file which provides the behavior information as matrix.
     *
     * @return
     *     the matrix which has been read from the given CSV file, or
     *     <code>null</code> if the file could not be read.
     */
    private String[][] readBehaviorInformation (final String filename) {

        String[][] information;  // to be returned;

        try {
        	CSVHandler cSVHandler = new CSVHandler();
            information = cSVHandler.readValues(filename);

        } catch (final Exception ex) {

            information = null;  // null indicates an error;
        }

        return information;
    }
}
