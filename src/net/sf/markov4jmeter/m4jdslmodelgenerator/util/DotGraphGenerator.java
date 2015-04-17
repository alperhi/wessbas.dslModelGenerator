package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

/**
 * Class for generating DOT graphs which consist of states and transitions.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class DotGraphGenerator {


    /* *****************************  constants  **************************** */

    /* -------------------------  public constants  ------------------------- */

    /** Shape constant for a double circle representation of a state.  */
    public final static String STATE_SHAPE_DOUBLE_CIRCLE = "doublecircle";

    /** Shape constant for a point representation of a state.  */
    public final static String STATE_SHAPE_POINT = "point";

    /** Shape constant for a plain name representation of a state.  */
    public final static String STATE_SHAPE_NONE = "none";

    /** Shape constant for an ellipse representation of a state.  */
    public final static String STATE_SHAPE_ELLIPSE = "ellipse";

    /** Shape constant for a septagon representation of a state.  */
    public final static String STATE_SHAPE_SEPTAGON = "septagon";

    /** Style constant for a solid-line representation of a transition. */
    public final static String TRANSITION_STYLE_SOLID = "solid";


    /* -------------------------  private constants  ------------------------ */

    /** Template for a graph, which consists of states and transitions. */
    private final static String DIGRAPH_TEMPLATE =
            "digraph G {\r\n"
            + "graph [dpi = 600]"  // for higher resolution --> causes warnings;
            + "%s%s"  // for transitions and states being inserted here;
            + "}";

    /** Template for a named state with a label and a specific shape. */
    private final static String DIGRAPH_STATE_TEMPLATE =
            "\"%s\" [label=\"%s\",shape=%s];";

    /** Template for a transition with source/target states, style and label. */
    private final static String DIGRAPH_TRANSITION_TEMPLATE =
            "\"%s\"->\"%s\" [style=%s,label=\"%s\"];";


    /* *************************  global variables  ************************* */


    /** List of registered states. */
    private final LinkedList<State> states;

    /** List of registered transitions. */
    private final LinkedList<Transition> transitions;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a DOT Graph Generator.
     */
    public DotGraphGenerator () {

        this.states      = new LinkedList<State>();
        this.transitions = new LinkedList<Transition>();
    }


    /* **************************  public methods  ************************** */


    /**
     * Registers a state to be generated.
     *
     * @param name
     *     name of the state for identification purposes.
     * @param shape
     *     shape of the state, must be one of the <code>SHAPE</code> constants.
     * @param label
     *     label of the state for representation purposes.
     */
    public void addState (
            final String name,
            final String shape,
            final String label) {

        this.states.add( new State(name, shape, label) );
    }

    /**
     * Registers a state to be generated, labeled with the name of the state.
     *
     * @param name
     *     name of the state for identification purposes.
     * @param shape
     *     shape of the state, must be one of the <code>SHAPE</code> constants.
     */
    public void addState (
            final String name,
            final String shape) {

        this.addState(name, shape, name);
    }

    /**
     * Registers a transition to be generated.
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants.
     * @param label
     *     label of the transition for representation purposes.
     */
    public void addTransition(
            final String source,
            final String target,
            final String style,
            final String label) {

        this.transitions.add( new Transition(source, target, style, label) );
    }

    /**
     * Registers a transition to be generated, without a label.
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants.
     */
    public void addTransition(
            final String source,
            final String target,
            final String style) {

        this.addTransition(source, target, style, "");
    }

    /**
     * Flushes all registered states and transitions.
     */
    public void flush () {

        this.states.clear();
        this.transitions.clear();
    }

    /**
     * Returns the <code>String</code> representation of a DOT graph.
     *
     * @return  a valid <code>String</code> which represents a DOT graph.
     */
    public String getGraphString () {

        final String graphString = String.format(
                DotGraphGenerator.DIGRAPH_TEMPLATE,
                this.getStatesString(),
                this.getTransitionsString());

        return graphString;
    }

    /**
     * Writes the DOT graph to a specific output file.
     *
     * @param filePath  path to the output file.
     *
     * @throws SecurityException  if write access to the file is denied.
     * @throws IOException        if any writing error occurs.
     */
    public void writeGraphToFile (final String filePath)
            throws SecurityException, IOException {

        // might throw a FileNotFound- or SecurityException;
        final FileOutputStream fos = new FileOutputStream(filePath);

        final OutputStreamWriter osw = new OutputStreamWriter(fos);
        final BufferedWriter bufferedWriter = new BufferedWriter(osw);

        try {

            // might throw an IOException;
            bufferedWriter.write( this.getGraphString() );

        } finally {

            if (bufferedWriter != null) {

                try {

                    // might throw an IOException;
                    bufferedWriter.close();

                } catch (final IOException ex) {

                    // ignore exception, since this is the "finally" block;
                    // TODO: warning message should be written to log file;
                }
            }
        }
    }


    /* **************************  private methods  ************************* */


    /**
     * Returns a <code>String</code> representation of the registered states.
     *
     * @return  a valid <code>String</code> instance.
     */
    private String getStatesString () {

        final StringBuffer stringBuffer = new StringBuffer();

        for (final State state : this.states) {

            final String stateString = String.format(
                    DotGraphGenerator.DIGRAPH_STATE_TEMPLATE,
                    state.name,
                    state.label,
                    state.shape);

            stringBuffer.append(stateString).append("\r\n");
        }

        return stringBuffer.toString();
    }

    /**
     * Returns a <code>String</code> representation of the registered
     * transitions.
     *
     * @return  a valid <code>String</code> instance.
     */
    private String getTransitionsString () {

        final StringBuffer stringBuffer = new StringBuffer();

        for (final Transition transition : this.transitions) {

            final String transitionString = String.format(
                    DotGraphGenerator.DIGRAPH_TRANSITION_TEMPLATE,
                    transition.source,
                    transition.target,
                    transition.style,
                    transition.label);

            stringBuffer.append(transitionString).append("\r\n");
        }

        return stringBuffer.toString();
    }


    /* *************************  internal classes  ************************* */


    /**
     * Internal class which represents a state of a DOT graph.
     *
     * @author   Eike Schulz (esc@informatik.uni-kiel.de)
     * @version  1.0
     */
    private class State {

        final String name;
        final String shape;
        final String label;

        public State (
                final String name,
                final String shape,
                final String label) {

            this.name   = name;
            this.shape  = shape;
            this.label  = label;
        }
    }

    /**
     * Internal class which represents a transition of a DOT graph.
     *
     * @author   Eike Schulz (esc@informatik.uni-kiel.de)
     * @version  1.0
     */
    private class Transition {

        final String source;
        final String target;
        final String style;
        final String label;

        public Transition (
                final String source,
                final String target,
                final String style,
                final String label) {

            this.source = source;
            this.target = target;
            this.style  = style;
            this.label  = label;
        }
    }
}