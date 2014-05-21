package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.LinkedList;

import m4jdsl.BehaviorModel;
import m4jdsl.BehaviorModelExitState;
import m4jdsl.BehaviorModelState;
import m4jdsl.M4jdslFactory;
import m4jdsl.MarkovState;
import m4jdsl.Service;
import m4jdsl.ThinkTime;
import m4jdsl.Transition;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.FlowSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class BehaviorModelsGenerator {

    private final ServiceRepository serviceRepository;
    private final M4jdslFactory m4jdslFactory;
    private final IdGenerator idGenerator;


    public BehaviorModelsGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final IdGenerator idGenerator) {

        this.serviceRepository = serviceRepository;
        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator       = idGenerator;
    }


    public LinkedList<BehaviorModel> generateBehaviorModels () throws GeneratorException {

        final LinkedList<BehaviorModel> behaviorModels =
                new LinkedList<BehaviorModel>();

        final BehaviorModel behaviorModel = this.generateBehaviorModel();

        behaviorModels.add(behaviorModel);
        return behaviorModels;
    }

    public BehaviorModel generateBehaviorModel () throws GeneratorException {

        // TODO: retrieve structure from Behavior Model Extractor;

        final String initServiceName = FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;

        final BehaviorModel behaviorModel = this.createEmptyBehaviorModel();

        final BehaviorModelExitState behaviorModelExitState =
                behaviorModel.getExitState();

        boolean installedInitialState = false;

        for (final Service service : this.serviceRepository.getServices()) {

            final MarkovState markovState = this.createMarkovState(service);

            /* do NOT add transitions for -all- states, since constraint
             * 'mustBeOutgoingTransitionsCorrespondingToSessionLayer' will be
             * violated otherwise (not all states are connected with the exit
             * state, only the 'End' states for the flows);
             *
            final NormallyDistributedThinkTime normallyDistributedThinkTime =
                    m4jdslFactory.createNormallyDistributedThinkTime();

            normallyDistributedThinkTime.setMean(0);
            normallyDistributedThinkTime.setDeviation(0);

            final Transition transition = this.createTransition(
                    behaviorModelExitState, 1.0, normallyDistributedThinkTime);

            markovState.getOutgoingTransitions().add(transition);
            */
            behaviorModel.getMarkovStates().add(markovState);

            if ( service.getName().equals(initServiceName) ) {

                behaviorModel.setInitialState(markovState);
                installedInitialState = true;
            }
        }

        if ( !installedInitialState ) {

            throw new GeneratorException(
                    "initial service could not be detected for Behavior Model: " + initServiceName);
        }

        String name = "Behavior Model 1";
        String filename = "filename.csv";

        if ( !filename.toLowerCase().endsWith(".csv") ) {

            filename += ".csv";
        }


        behaviorModel.setFilename(filename);
        behaviorModel.setName(name);

        return behaviorModel;
    }

    private MarkovState createMarkovState (final Service service) {

        final MarkovState markovState = this.m4jdslFactory.createMarkovState();

        //        markovState.setEId(this.idGenerator.newId());
        markovState.setEId(this.idGenerator.getPrefix() + "_" + service.getName());  // use service name as ID for better readability;
        markovState.setService(service);

        return markovState;
    }

    private BehaviorModel createEmptyBehaviorModel () {

        final BehaviorModel behaviorModel =
                this.m4jdslFactory.createBehaviorModel();

        final BehaviorModelExitState behaviorModelExitState =
                this.createBehaviorModelExitState();

        behaviorModel.setExitState(behaviorModelExitState);

        return behaviorModel;
    }

    private BehaviorModelExitState createBehaviorModelExitState () {

        final BehaviorModelExitState behaviorModelExitState =
                this.m4jdslFactory.createBehaviorModelExitState();

        behaviorModelExitState.setEId(this.idGenerator.newId());

        return behaviorModelExitState;
    }

    private Transition createTransition (
            final BehaviorModelState targetState,
            final double probability,
            final ThinkTime thinkTime) {

        final Transition transition = this.m4jdslFactory.createTransition();

        transition.setTargetState(targetState);
        transition.setProbability(probability);
        transition.setThinkTime(thinkTime);

        return transition;
    }
}
