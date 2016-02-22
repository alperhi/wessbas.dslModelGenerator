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


package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import m4jdsl.ApplicationModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.SessionLayerEFSM;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.AbstractSessionLayerEFSMGenerator;

/**
 * Generator class for creating M4J-DSL model components, which represent the
 * Application Model.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ApplicationModelGenerator {


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;

    /** Instance for creating Session Layer EFSMs. */
    private final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for an Application Model Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param sessionLayerEFSMGenerator
     *     instance for creating Session Layer EFSMs.
     */
    public ApplicationModelGenerator (
            final M4jdslFactory m4jdslFactory,
            final AbstractSessionLayerEFSMGenerator sessionLayerEFSMGenerator) {

        this.m4jdslFactory             = m4jdslFactory;
        this.sessionLayerEFSMGenerator = sessionLayerEFSMGenerator;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates an M4J-DSL model component which represents an Application
     * Layer.
     *
     * @return
     *     the newly created M4J-DSL model component.
     *
     * @throws GeneratorException
     *     in case the Session Layer EFSM cannot be created for any reason.
     */
    public ApplicationModel generateApplicationModel ()
            throws GeneratorException {

        final ApplicationModel applicationModel =
                this.m4jdslFactory.createApplicationModel();

        // might throw a GeneratorException;
        final SessionLayerEFSM sessionLayerEFSM =
                this.sessionLayerEFSMGenerator.
                generateSessionLayerEFSMAndWriteDotGraph();

        applicationModel.setSessionLayerEFSM(sessionLayerEFSM);

        return applicationModel;
    }
}
