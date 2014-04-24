package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import m4jdsl.ApplicationModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.SessionLayerEFSM;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractSessionLayerEFSMGenerator;

public class ApplicationModelGenerator {

    private final M4jdslFactory m4jdslFactory;
    private final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator;


    public ApplicationModelGenerator (
            final M4jdslFactory m4jdslFactory,
            final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator) {

        this.m4jdslFactory             = m4jdslFactory;
        this.sessionLayerEFSMGenerator = sessionLayerEFSMGenerator;
    }


    public ApplicationModel generateApplicationModel () throws GeneratorException {

        final ApplicationModel applicationModel = this.m4jdslFactory.createApplicationModel();
        final SessionLayerEFSM sessionLayerEFSM = this.sessionLayerEFSMGenerator.generateSessionLayerEFSM();

        applicationModel.setSessionLayerEFSM(sessionLayerEFSM);

        return applicationModel;
    }
}
