package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

public class DotGraphGenerator {

    public final static String STATE_SHAPE_DOUBLE_CIRCLE = "doublecircle";
    public final static String STATE_SHAPE_POINT         = "point";
    public final static String STATE_SHAPE_NONE          = "none";
    public final static String STATE_SHAPE_ELLIPSE       = "ellipse";

    public final static String TRANSITION_STYLE_SOLID = "solid";

    private final static String DIGRAPH_TEMPLATE =
            "digraph G {\r\n"
            + "graph [dpi = 600]"  // for higher resolution --> causes warnings;
            + "%s%s"  // for transitions and states being inserted here;
            + "}";

    private final static String DIGRAPH_STATE_TEMPLATE =
            "\"%s\" [label=\"%s\",shape=%s];";

    private final static String DIGRAPH_TRANSITION_TEMPLATE =
            "\"%s\"->\"%s\" [style=%s,label=\"%s\"];";


    /* *************************  global variables  ************************* */


    private final LinkedList<State> states;
    private final LinkedList<Transition> transitions;


    /* ***************************  constructors  *************************** */


    public DotGraphGenerator () {

        this.states      = new LinkedList<State>();
        this.transitions = new LinkedList<Transition>();
    }


    /* **************************  public methods  ************************** */


    public void addState (
            final String name,
            final String shape,
            final String label) {

        this.states.add( new State(name, shape, label) );
    }

    public void addState (
            final String name,
            final String shape) {

        this.addState(name, shape, name);
    }

    public void addTransition(
            final String source,
            final String target,
            final String style,
            final String label) {

        this.transitions.add( new Transition(source, target, style, label) );
    }

    public void addTransition(
            final String source,
            final String target,
            final String style) {

        this.addTransition(source, target, style, "");
    }

    public void flush () {

        this.states.clear();
        this.transitions.clear();
    }

    public String getGraphString () {

        final String graphString = String.format(
                DotGraphGenerator.DIGRAPH_TEMPLATE,
                this.getStatesString(),
                this.getTransitionsString());

        return graphString;
    }

    public void writeGraphToFile (final String filePath)
            throws SecurityException, IOException {

        // might throw a FileNotFound- or SecurityException;
        final FileOutputStream fos = new FileOutputStream(filePath);

        final OutputStreamWriter osw = new OutputStreamWriter(fos);
        final BufferedWriter bufferedWriter = new BufferedWriter(osw);

        try {

            bufferedWriter.write( this.getGraphString() );

        } finally {

            if (bufferedWriter != null)

                try {

                    // might throw an IOException;
                    bufferedWriter.close();

                } catch (final Exception ex) {

                    // ignore exception, since this is the "finally" block;
                    // TODO: warning might be written to log file;
                }
        }
    }


    /* **************************  private methods  ************************* */


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