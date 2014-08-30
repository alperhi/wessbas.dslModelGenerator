package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.M4jdslFactory;
import m4jdsl.Parameter;
import m4jdsl.Property;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolLayerEFSMState;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Abstract base class for all Protocol Layer EFSM Generators. This class
 * provides methods for creating model elements, such as states and transitions.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public abstract class AbstractProtocolLayerEFSMGenerator {


    /* *****************************  constants  **************************** */


    /* ----------------------  request type constants  ---------------------- */

    /** Type constant for requests of type "HTTP". */
    public final static int REQUEST_TYPE_HTTP = 0;

    /** Type constant for requests of type "SOAP". */
    public final static int REQUEST_TYPE_SOAP = 1;

    /** Type constant for requests of type "Java". */
    public final static int REQUEST_TYPE_JAVA = 2;

    /** Type constant for requests of type "JUnit". */
    public final static int REQUEST_TYPE_JUNIT = 3;

    /** Type constant for requests of type "BeanShell". */
    public final static int REQUEST_TYPE_BEANSHELL = 4;


    /* --------------------------  error messages  -------------------------- */

    /** Error message for the case that an unknown request type has been
     *  detected. */
    private final static String ERROR_UNKNOWN_REQUEST_TYPE =
            "unknown request type detected (type %d)";

    /** Error message for the case that an invalid request property (with too
     *  few information) has been detected. */
    private final static String ERROR_INVALID_REQUEST_PROPERTY =
            "invalid request property detected (too few information submitted)";

    /** Error message for the case that an invalid request parameter (with too
     *  few information) has been detected. */
    private final static String ERROR_INVALID_REQUEST_PARAMETER =
            "invalid request parameter detected (too few information submitted)";


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    protected final M4jdslFactory m4jdslFactory;

    /** Instance for creating unique Protocol State IDs. */
    protected final IdGenerator idGenerator;

    /** Instance for creating unique request IDs. */
    protected final IdGenerator requestIdGenerator;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Protocol Layer EFSM Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param idGenerator
     *     instance for creating unique Protocol State IDs.
     * @param requestIdGenerator
     *     instance for creating unique request IDs.
     */
    public AbstractProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator) {

        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator        = idGenerator;
        this.requestIdGenerator = requestIdGenerator;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates a Protocol Layer EFSM.
     *
     * @param serviceName
     *     name of the associated service; this information might be required
     *     for being sent with a request.
     * @return
     *     the newly created Protocol Layer EFSM.
     *
     * @throws GeneratorException
     *     if any error during the generation process occurs.
     */
    public abstract ProtocolLayerEFSM generateProtocolLayerEFSM (
            final String serviceName) throws GeneratorException;


    /* *************************  protected methods  ************************ */


    /**
     * Creates an empty Protocol Layer EFSM, which is an instance that only
     * includes an exit state.
     *
     * @return  the newly created Protocol Layer EFSM.
     */
    protected ProtocolLayerEFSM createEmptyProtocolLayerEFSM () {

        final ProtocolLayerEFSM protocolLayerEFSM =
                this.m4jdslFactory.createProtocolLayerEFSM();

        final ProtocolExitState protocolExitState =
                this.createProtocolExitState();

        protocolLayerEFSM.setExitState(protocolExitState);

        return protocolLayerEFSM;
    }

    /**
     * Creates a Protocol State.
     *
     * @param request  request to be associated with the state.
     *
     * @return  the newly created Protocol State.
     */
    protected ProtocolState createProtocolState (final Request request) {

        final ProtocolState protocolState =
                this.m4jdslFactory.createProtocolState();

        final String id = this.idGenerator.newId();

        protocolState.setEId(id);
        protocolState.setRequest(request);

        return protocolState;
    }

    /**
     * Creates a Protocol Exit State, which is a Protocol State that represents
     * the exit state of an EFSM.
     *
     * @return  the newly created Protocol Exit State.
     */
    protected ProtocolExitState createProtocolExitState () {

        final ProtocolExitState protocolExitState =
                this.m4jdslFactory.createProtocolExitState();

        protocolExitState.setEId(this.idGenerator.newId());

        return protocolExitState;
    }

    /**
     * Creates a Protocol Transition, including guard and action.
     *
     * @param targetState  target state of the transition.
     * @param guard        guard of the transition.
     * @param action       action of the transition.
     *
     * @return  the newly created Protocol Transition.
     */
    protected ProtocolTransition createProtocolTransition (
            final ProtocolLayerEFSMState targetState,
            final String guard,
            final String action) {

        final ProtocolTransition protocolTransition =
                this.m4jdslFactory.createProtocolTransition();

        protocolTransition.setTargetState(targetState);
        protocolTransition.setGuard(guard);
        protocolTransition.setAction(action);

        return protocolTransition;
    }

    /**
     * Creates an M4J-DSL model component which represents a request (without
     * any properties and parameters).
     *
     * @param requestType
     *     type of request; this must be one of the
     *     <code>REQUEST_TYPE</code>-constants of class
     *     {@link AbstractProtocolLayerEFSMGenerator}.
     *
     * @return
     *     the newly created request.
     *
     * @throws GeneratorException
     *     if the specified request type is unknown.
     */
    protected Request createRequest (
            final int requestType) throws GeneratorException {

        return this.createRequest(requestType, null, null);
    }

    /**
     * Creates an M4J-DSL model component which represents a request.
     *
     * @param requestType
     *     type of request; this must be one of the
     *     <code>REQUEST_TYPE</code>-constants of class
     *     {@link AbstractProtocolLayerEFSMGenerator}.
     * @param properties
     *     properties of the request; this must be an <i>n</i>x2-dimensional
     *     array for <i>n</i> properties, with each entry consisting of a
     *     key/value pair. Optionally, <code>null</code> might be passed, if
     *     no properties shall be set.
     * @param parameters
     *     parameters of the request; this must be an <i>n</i>x2-dimensional
     *     array for <i>n</i> properties, with each entry consisting of a
     *     name/value pair. Optionally, <code>null</code> might be passed, if
     *     no parameters shall be set.
     *
     * @return
     *     the newly created request.
     *
     * @throws GeneratorException
     *     if the specified request type is unknown, or if any invalid
     *     properties/parameters have been passed.
     */
    protected Request createRequest (
            final int requestType,
            final String[][] properties,
            final String[][] parameters) throws GeneratorException {

        final Request request;  // to be returned;

        switch (requestType) {

            case AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_HTTP:
                request = this.m4jdslFactory.createHTTPRequest();
                break;

            case AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_SOAP:
                request = this.m4jdslFactory.createSOAPRequest();
                break;

            case AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_JAVA:
                request = this.m4jdslFactory.createJavaRequest();
                break;

            case AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_JUNIT:
                request = this.m4jdslFactory.createJUnitRequest();
                break;

            case AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_BEANSHELL:
                request = this.m4jdslFactory.createBeanShellRequest();
                break;

            default:

                final String message = String.format(
                        AbstractProtocolLayerEFSMGenerator.
                        ERROR_UNKNOWN_REQUEST_TYPE,
                        requestType);

                throw new GeneratorException(message);
        }

        request.setEId( this.requestIdGenerator.newId() );

        if (properties != null) {  // properties are optional;

            Property property;

            for (final String[] p : properties) {

                if (p.length < 2) {

                    throw new GeneratorException(
                            AbstractProtocolLayerEFSMGenerator.
                            ERROR_INVALID_REQUEST_PROPERTY);
                }

                property = this.m4jdslFactory.createProperty();
                property.setKey(p[0]);
                property.setValue(p[1]);

                request.getProperties().add(property);
            }
        }

        if (parameters != null) {  // parameters are optional;

            Parameter parameter;

            for (final String[] p : parameters) {

                if (p.length < 2) {

                    throw new GeneratorException(
                            AbstractProtocolLayerEFSMGenerator.
                            ERROR_INVALID_REQUEST_PARAMETER);
                }

                parameter = this.m4jdslFactory.createParameter();
                parameter.setName(p[0]);
                parameter.setValue(p[1]);

                request.getParameters().add(parameter);
            }
        }

        return request;
    }
}
