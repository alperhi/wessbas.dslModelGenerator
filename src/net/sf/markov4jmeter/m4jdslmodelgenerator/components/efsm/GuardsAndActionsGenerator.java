package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import m4jdsl.Action;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.Guard;
import m4jdsl.GuardActionParameter;
import m4jdsl.GuardActionParameterType;
import m4jdsl.M4jdslFactory;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.WorkloadModel;
import synoptic.invariants.AlwaysPrecedesInvariant;
import synoptic.invariants.BinaryInvariant;
import synoptic.invariants.CntAlwaysEqualsGreaterInvariant;
import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.NeverFollowedInvariant;
import synoptic.invariants.TemporalInvariantSet;
import synoptic.main.AbstractMain;
import synoptic.main.SynopticMain;

/**
 * Identified invariants using synoptic and the translates these invariants to guards and actions. 
 *
 * @author   Christian V�gele (voegele@fortiss.org)
 * @version  1.0
 */
public class GuardsAndActionsGenerator {
	
    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;
    
    /**
     * Is needed for neverFollowByInvariant.
     */
    private boolean foundPaths = false;
    
    /**
     * Invariants from Synoptic.
     */
    private TemporalInvariantSet invariants; 

    /* ***************************  constructors  *************************** */

    /**
     * Constructor for a Behavior Mix Generator.
     *
     * @param m4jdslFactory  instance for creating M4J-DSL model elements.
     */
    public GuardsAndActionsGenerator (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
    }
	
    /* **************************  public methods  ************************** */    

	 /**
     * Add guards and actions to workloadModel.
     * 
     * @param workloadModel
     */
    public void installGuardsAndActions(final WorkloadModel workloadModel) {    	
    	// init invariants
    	this.getTemporalInvariants();
    	this.filterInvariants();    	   	
    	SessionLayerEFSM sessionLayerEFSM = workloadModel.getApplicationModel().getSessionLayerEFSM();    	
    	// for each found invariant
      	for (ITemporalInvariant invariant : this.invariants.getSet()) {      		
    		if (invariant instanceof BinaryInvariant) {       			
    			BinaryInvariant binaryInvariant = (BinaryInvariant) invariant;		
				ApplicationState first = getApplicationState(binaryInvariant.getFirst().toString(), sessionLayerEFSM);
				ApplicationState second = getApplicationState(binaryInvariant.getSecond().toString(), sessionLayerEFSM);		
				List<ApplicationTransition> actionApplicationTransitions = getActionApplicationTransition(binaryInvariant.getFirst().toString(), sessionLayerEFSM);
				List<ApplicationTransition> guardApplicationTransitions = getGuardApplicationTransition(binaryInvariant.getSecond().toString(), sessionLayerEFSM);	
				
				// not all guards are needed. First check. 
				if (checkIfGuardsAreNeeded(guardApplicationTransitions, first, second, sessionLayerEFSM)) {
					if (binaryInvariant instanceof AlwaysPrecedesInvariant) {					
						installGuardsActionsAlwaysPrecedesInvariant(first, sessionLayerEFSM, actionApplicationTransitions, guardApplicationTransitions);
//					} else if (binaryInvariant instanceof NeverFollowedInvariant) {					
//						installGuardsActionsNeverFollowedInvariant(first, second, sessionLayerEFSM, actionApplicationTransitions, guardApplicationTransitions);	
					} else if (binaryInvariant instanceof CntAlwaysEqualsGreaterInvariant) {
						CntAlwaysEqualsGreaterInvariant cntAlwaysEqualsGreaterInvariant = (CntAlwaysEqualsGreaterInvariant) binaryInvariant;
						installGuardsActionsCntAlwaysEqualsGreaterInvariant(first, second, sessionLayerEFSM, actionApplicationTransitions, guardApplicationTransitions, cntAlwaysEqualsGreaterInvariant.getDiffMinimum() );
					}
				}				
    		}	
    	}       	
    }
    
    /* **************************  private methods  ************************* */
    
    /**
	 * getTemporalInvariants from synoptic package.
	 */
	private void getTemporalInvariants() {    	
    	String[] args = new String[7];  
        args[0] = "-r";
        args[1] = "(?<TYPE>.*)";
        args[2] = "-s";
        args[3] = "^---$";
        args[4] = "-i";
//        args[5] = "-o";
//        args[6] = "C:/Users/voegele/Applications/eclipse-jee-kepler-SR2-win32-x86_64/eclipse/workspace/Synoptic/output/output";
//        args[7] = "-d";
//        args[8] = "C:/Program Files (x86)/Graphviz2.38/bin/gvedit.exe";
        args[5] = "--dumpInvariants=true";
        args[6] = "C:/Users/voegele/git/wessbas.dslModelGenerator/examples/specj/input/logFiles/specjlog2.log";

        SynopticMain.getInstance();
		try {
			SynopticMain.main(args);
			this.invariants = AbstractMain.getInvariants();
			this.filterInvariants();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
    
//	private void getTemporalInvariants() {    	
//    	String[] args = new String[7];  
//        args[0] = "-r";
//        args[1] = "[$1]+;[0-9]*;(?<TYPE>[\\w_+-]*);(?<ip>[\\w+-]*).[\\w;.-]*";
//        args[2] = "-m";
//        args[3] = "\\k<ip>";
//        args[4] = "-i";
////        args[5] = "-o";
////        args[6] = "C:/Users/voegele/Applications/eclipse-jee-kepler-SR2-win32-x86_64/eclipse/workspace/Synoptic/output/output";
////        args[7] = "-d";
////        args[8] = "C:/Program Files (x86)/Graphviz2.38/bin/gvedit.exe";
//        args[5] = "--dumpInvariants=true";
//        args[6] = "examples/specj/input/logFiles/SPECjlog.log";
//
//        SynopticMain.getInstance();
//		try {
//			SynopticMain.main(args);
//			this.invariants = AbstractMain.getInvariants();
//			this.filterInvariants();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
    
	
	/**
	 * Invariants which are AlwaysPrecedesInvariant and CntAlwaysEqualsGreaterInvariant are redundant. Only keep 
	 * CntAlwaysEqualsGreaterInvariant.
	 */
	private void filterInvariants() {
		List<ITemporalInvariant> removeList = new ArrayList<ITemporalInvariant>();
		for (ITemporalInvariant invariant : this.invariants.getSet()) {
			if (invariant instanceof AlwaysPrecedesInvariant) {
				AlwaysPrecedesInvariant alwaysPrecedesInvariant = (AlwaysPrecedesInvariant) invariant;			
				String first = alwaysPrecedesInvariant.getFirst().toString();
				String second = alwaysPrecedesInvariant.getSecond().toString();			
				for (ITemporalInvariant invariantCompare : this.invariants.getSet()) {
					if (invariantCompare instanceof CntAlwaysEqualsGreaterInvariant) {
						CntAlwaysEqualsGreaterInvariant  cntAlwaysEqualsGreaterInvariant = (CntAlwaysEqualsGreaterInvariant) invariantCompare;			
						String firstCompare = cntAlwaysEqualsGreaterInvariant.getFirst().toString();
						String secondCompare = cntAlwaysEqualsGreaterInvariant.getSecond().toString();
						if (first.equals(firstCompare) && second.equals(secondCompare)) {
							removeList.add(invariant);
							break;
						}								
					}
				}								
			}
		}
		this.invariants.getSet().removeAll(removeList);	
	}  
	
    /**
     * Set guards and actions for AlwaysPrecedesInvariant.
     * 
     * @param first
     * @param sessionLayerEFSM
     * @param actionApplicationTransitions
     * @param guardApplicationTransitions
     */
    private void installGuardsActionsAlwaysPrecedesInvariant (final ApplicationState first,
    		final SessionLayerEFSM sessionLayerEFSM,
    		final List<ApplicationTransition> actionApplicationTransitions,
    		final List<ApplicationTransition> guardApplicationTransitions) {
    	GuardActionParameter guardActionParameter = createGuardActionParameter(first.getService().getName(),
				GuardActionParameterType.BOOLEAN,
				sessionLayerEFSM,
				first.getService().getName(),
				null);		
		for (ApplicationTransition applicationTransition:actionApplicationTransitions) {
			Action action = createAction(guardActionParameter);
			if (!actionAlreadyExists(applicationTransition, action)) {
				applicationTransition.getAction().add(action);
			}
		}						
		for (ApplicationTransition applicationTransition:guardApplicationTransitions) {
			Guard guard = createGuard(guardActionParameter, true);
			applicationTransition.getGuard().add(guard);
		}	
    }
    
    /**
     * Set guards and actions for NeverFollowedInvariant.
     * 
     * @param first
     * @param second
     * @param sessionLayerEFSM
     * @param actionApplicationTransitions
     * @param guardApplicationTransitions
     */
    private void installGuardsActionsNeverFollowedInvariant (final ApplicationState first,
    		final ApplicationState second,
    		final SessionLayerEFSM sessionLayerEFSM,
    		final List<ApplicationTransition> actionApplicationTransitions,
    		final List<ApplicationTransition> guardApplicationTransitions) {
    	this.foundPaths = false;
		pathsFromFirstToSecondExists(first, second, sessionLayerEFSM, new Stack<ApplicationState>());						
		if (this.foundPaths) {
			GuardActionParameter guardActionParameter = createGuardActionParameter(first.getService().getName(),
					GuardActionParameterType.BOOLEAN,
					sessionLayerEFSM,
					first.getService().getName(),
					null);		
			for (ApplicationTransition applicationTransition:actionApplicationTransitions) {
				Action action = createAction(guardActionParameter);
				if (!actionAlreadyExists(applicationTransition, action)) {
					applicationTransition.getAction().add(action);
				}
			}						
			for (ApplicationTransition applicationTransition:guardApplicationTransitions) {
				Guard guard = createGuard(guardActionParameter, false);
				applicationTransition.getGuard().add(guard);
			}	
		}		
    }
    
    /**
     * Set guards and actions for CntAlwaysEqualsGreaterInvariant.
     * 
     * @param first
     * @param second
     * @param sessionLayerEFSM
     * @param actionApplicationTransitions
     * @param guardApplicationTransitions
     * @param guardApplicationTransitions
     * 		minimum difference between a and b
     */
    private void installGuardsActionsCntAlwaysEqualsGreaterInvariant (final ApplicationState first,
    		final ApplicationState second,
    		final SessionLayerEFSM sessionLayerEFSM,
    		final List<ApplicationTransition> actionApplicationTransitions,
    		final List<ApplicationTransition> guardApplicationTransitions, 
    		final int diffMinimum) {
    	String variableName = first.getService().getName()+second.getService().getName();					
		GuardActionParameter guardActionParameter = createGuardActionParameter(
				variableName,
				GuardActionParameterType.INTEGER,
				sessionLayerEFSM,
				first.getService().getName(),
				second.getService().getName());
		for (ApplicationTransition applicationTransition:actionApplicationTransitions) {
			Action action = createAction(guardActionParameter);
			if (!actionAlreadyExists(applicationTransition, action)) {
				applicationTransition.getAction().add(action);
			}
		}
		for (ApplicationTransition applicationTransition:guardApplicationTransitions) {
			Action action = createAction(guardActionParameter);
			if (!actionAlreadyExists(applicationTransition, action)) {
				applicationTransition.getAction().add(action);
			}
		}					
		for (ApplicationTransition applicationTransition:guardApplicationTransitions) {
			Guard guard = createGuard(guardActionParameter, true, diffMinimum);
			applicationTransition.getGuard().add(guard);
		}
    }
    
    /**
     * Check if applicationTransition has already the action. 
     * 
     * @param applicationTransition
     * @param action
     * @return true if action already exists
     */
    private boolean actionAlreadyExists(final ApplicationTransition applicationTransition, final Action action) {		
		for (Action actionInstance : applicationTransition.getAction()) {
			if (actionInstance.getActionParameter().equals(action.getActionParameter())) {
				return true;
			}
		}
		return false;
    }
    
    /**
     * To set a NeverFollowedInvariant it must be checked if a paths from first to second exists. Otherwise the 
     * NeverFollowedInvariant must not ne set. 
     * 
     * @param first
     * @param second
     * @param sessionLayerEFSM
     * @return
     */
    private void pathsFromFirstToSecondExists(final ApplicationState first,
    		final ApplicationState second,
    		final SessionLayerEFSM sessionLayerEFSM,
    		final Stack<ApplicationState> currentPath) {    	
    	currentPath.push(first);
    	for (ApplicationTransition applicationTransition:first.getOutgoingTransitions()) {    
	    	if (applicationTransition.getTargetState() instanceof ApplicationState) {
	    		ApplicationState nextApplicationState = (ApplicationState) applicationTransition.getTargetState(); 	    		    			   			
    			if (nextApplicationState.equals(second)) {    				
    				this.foundPaths = true;		
    			} else {
    				if (!currentPath.contains(nextApplicationState)) {	
    					pathsFromFirstToSecondExists(nextApplicationState, second, sessionLayerEFSM, currentPath);
    				}
    			}    			  		
    		}
    	}   
    	currentPath.pop();
    }
    
    /**
     * This methods check whether guards and actions are needed for the invariant. 
     * 
     * @param guardApplicationTransitions
     * @param first
     * @param second
     * @param sessionLayerEFSM
     * @return
     */
    private boolean checkIfGuardsAreNeeded(final List<ApplicationTransition> guardApplicationTransitions,
    		final ApplicationState first,
    		final ApplicationState second,
    		final SessionLayerEFSM sessionLayerEFSM) {  
    	
    	// Error case: guardApplicationTransitions has no incoming, i.e. when target is initial state
    	if (guardApplicationTransitions.size() == 0) {
    		return false;    		
    	} 
    	 	
    	// if applicationstates are directly connected and target state has only one incoming transition
    	if (guardApplicationTransitions.size() == 1) {
			if (guardApplicationTransitions.get(0).getApplicationState().equals(first)) {
				return false;
			}		
    	} 
    	
    	return true;   	

    }
    
    /**
     * Return a list of ApplicationTransition which are incoming transitions to the serviceName. 
     * 
     * @param serviceName
     * @param sessionLayerEFSM
     * @return List<ApplicationTransition>
     */
    private List<ApplicationTransition> getActionApplicationTransition(final String serviceName,
    		final SessionLayerEFSM sessionLayerEFSM) {
    	List<ApplicationTransition> returnApplicationTransitionList = new ArrayList<ApplicationTransition>();
    	for (ApplicationState applicationState : sessionLayerEFSM.getApplicationStates()) {
          	for (ApplicationTransition applicationTransition : applicationState.getOutgoingTransitions()) {
          		if (applicationTransition.getTargetState() instanceof ApplicationState) {
          			String targetState = ((ApplicationState) applicationTransition.getTargetState()).getService().getName();
              		if (targetState.equals(serviceName)) {
              			returnApplicationTransitionList.add(applicationTransition);          			
              		}  
          		}          		        			
          	}
    	}
    	return returnApplicationTransitionList;
    }
    
    /**
     * Return a list of GuardTransitions which are incoming transitions to the serviceName. 
     * 
     * @param serviceName
     * @param sessionLayerEFSM
     * @return List<ApplicationTransition>
     */
    private List<ApplicationTransition> getGuardApplicationTransition(final String serviceName,
    		final SessionLayerEFSM sessionLayerEFSM) {
    	List<ApplicationTransition> returnApplicationTransitionList = new ArrayList<ApplicationTransition>();
    	for (ApplicationState applicationState : sessionLayerEFSM.getApplicationStates()) {
    		if (applicationState.getOutgoingTransitions().size() > 2) {
	          	for (ApplicationTransition applicationTransition : applicationState.getOutgoingTransitions()) {
	          		if (applicationTransition.getTargetState() instanceof ApplicationState) {
	          			String targetState = ((ApplicationState) applicationTransition.getTargetState()).getService().getName();
	              		if (targetState.equals(serviceName)) {
	              			returnApplicationTransitionList.add(applicationTransition);          			
	              		}  
	          		}          		        			
	          	}
    		}
    	}
    	return returnApplicationTransitionList;
    }
    
    /**
     * Get applicationstate.
     * 
     * @param serviceName
     * @param sessionLayerEFSM
     * @return
     */
    private ApplicationState getApplicationState(final String serviceName,
    		final SessionLayerEFSM sessionLayerEFSM) {
    	for (ApplicationState applicationState : sessionLayerEFSM.getApplicationStates()) {
          	if (applicationState.getService().getName().equals(serviceName)) {
          		return applicationState;
          	}
    	}
    	return null;
    }
    
    /**
     * Create new GuardActionParameter.
     * 
     * @param guardActionName
     * @param guardActionParameterType
     * @return new GuardActionParameter
     */
    private GuardActionParameter createGuardActionParameter (final String guardActionName,
    		final GuardActionParameterType guardActionParameterType,
    		final SessionLayerEFSM sessionLayerEFSM, 
    		final String sourceName,
    		final String targetName) {
    	
    	// search if parameter already exists
    	for (GuardActionParameter guardActionParameter : sessionLayerEFSM.getGuardActionParameterList().getGuardActionParameters()) {
    		if (guardActionParameter.getGuardActionParameterName().equals(guardActionName)) {
    			return guardActionParameter;
    		}
    	}     	
    	
    	// if not --> create
	 	final GuardActionParameter guardActionParameter = this.m4jdslFactory.createGuardActionParameter();
    	guardActionParameter.setGuardActionParameterName(guardActionName);
    	guardActionParameter.setParameterType(guardActionParameterType);
    	guardActionParameter.setSourceName(sourceName);
    	if (targetName != null) {
        	guardActionParameter.setTargetName(targetName);	
    	}
    	
       	sessionLayerEFSM.getGuardActionParameterList().getGuardActionParameters().add(guardActionParameter);
    	return guardActionParameter;  	   	
    }
    
    /**
     * Create new guard.
     * 
     * @param guardActionParameter
     * @param condition
     * @return  new Guard
     */
    private Guard createGuard (final GuardActionParameter guardActionParameter, final boolean negate) {
    	final Guard guard = this.m4jdslFactory.createGuard();
    	guard.setGuardParameter(guardActionParameter);
    	guard.setNegate(negate);
    	return guard;
    }
    
    /**
     * Create new guard.
     * 
     * @param guardActionParameter
     * @param condition
     * @return  new Guard
     */
    private Guard createGuard (final GuardActionParameter guardActionParameter, final boolean negate, final int diffMinimum) {
    	final Guard guard = this.m4jdslFactory.createGuard();
    	guard.setGuardParameter(guardActionParameter);
    	guard.setNegate(negate);
    	guard.setDiffMinimum(diffMinimum);
    	return guard;
    }
        
    /**
     * Create new Action.
     * 
     * @param guardActionParameter
     * @param condition
     * @return new Action
     */
    private Action createAction (final GuardActionParameter guardActionParameter) {
    	final Action action = this.m4jdslFactory.createAction();
    	action.setActionParameter(guardActionParameter);
    	return action;    	
    }
	
}
