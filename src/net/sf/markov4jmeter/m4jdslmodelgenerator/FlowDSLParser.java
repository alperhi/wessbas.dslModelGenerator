package net.sf.markov4jmeter.m4jdslmodelgenerator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import net.sf.markov4jmeter.gear.FlowDSLStandaloneSetup;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.parser.ParseException;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Class for parsing Flow-DSL content.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.5
 */
public class FlowDSLParser {

    /** Error message for the case that any input could not be read from a
     *  <code>Reader</code> instance. */
    private final static String ERROR_PARSING =
            "could not parse input from \"%s\":\n%s";

    /** Error message for the case that <code>null</code> has been passed as
     *  a <code>Reader</code> instance. */
    private final static String ERROR_READER_IS_NULL =
            "null has been passed as Reader instance";


    /* *************************  global variables  ************************* */


    /** (Dependency injected) parser. */
    @Inject
    private IParser parser;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Flow-DSL Parser.
     */
    public FlowDSLParser() {

        init();
    }


    /* **************************  public methods  ************************** */


    /**
     * Parses the content which is provided by a given <code>Reader</code>
     * instance and returns the root node of the result tree.
     *
     * @param reader
     *     <code>Reader</code> instance which provides the content to be parsed.
     *
     * @return
     *     the root node of the result tree.
     *
     * @throws IOException
     *     if any I/O error occurs while parsing.
     * @throws IllegalArgumentException
     *     if <code>null</code> has been passed as a <code>Reader</code>
     *     instance.
     */
    public EObject parse (final Reader reader)
            throws IllegalArgumentException, IOException {

        if (reader == null) {

            throw new IllegalArgumentException(
                    FlowDSLParser.ERROR_READER_IS_NULL);
        }

        // might throw an IOException;
        final IParseResult parseResult = this.parser.parse(reader);

        if( parseResult.hasSyntaxErrors() ) {

            final String message = String.format(
                    FlowDSLParser.ERROR_PARSING,
                    reader.toString(),
                    this.getSyntaxErrors(parseResult));

            throw new ParseException(message);
        }

        return parseResult.getRootASTElement();
    }

    /**
     * Parses a set of Flow-DSL files and returns the root node of the result
     * tree.
     *
     * @param flowFiles
     *     files to be parsed.
     * @return
     *     the root node of the result tree.
     *
     * @throws IOException
     *     if any I/O error occurs while parsing.
     */
    public EObject parse (final File[] flowFiles) throws IOException {

        // all content Strings will be concatenated to a single String;
        // NOTE: a disadvantage of this approach is that possible syntax errors
        // detected by the parser cannot be assigned to an associated file;
        // alternatively, a HashMap with Flow names as keys and related content
        // as values could be used, for parsing each content individually;
        // TODO: solution for enhanced error information;
        final StringBuffer stringBuffer = new StringBuffer();

        String content;

        for (final File file : flowFiles) {

            // might throw an IOException;
            content = FileUtils.readFileToString(file);

            stringBuffer.append(content);
        }

        final StringReader stringReader =
                new StringReader(stringBuffer.toString());

        // might throw an IllegalArgument- or IOException (since the reader is
        // not null, an IllegalArgumentException should never be thrown here);
        return this.parse(stringReader);
    }


    /* **************************  private methods  ************************* */


    /**
     * Initializes the Flow-DSL Parser.
     */
    private void init () {

        final FlowDSLStandaloneSetup setup = new FlowDSLStandaloneSetup();
        final Injector injector = setup.createInjectorAndDoEMFRegistration();

        injector.injectMembers(this);
    }


    /**
     * Collects all syntax error messages contained in a given
     * <code>ParseResult</code> instance.
     *
     * @param parseResult
     *     <code>ParseResult</code> instance which contains syntax error
     *     messages to be collected.
     *
     * @return
     *     all collected syntax error messages, concatenated to a single
     *     <code>String</code> instance.
     */
    private String getSyntaxErrors (final IParseResult parseResult) {

        final StringBuffer stringBuffer = new StringBuffer();

        final Iterator<INode> iterator =
                parseResult.getSyntaxErrors().iterator();

        while (true) {

            stringBuffer.append( iterator.next().getText() );

            if ( iterator.hasNext() ) {

                stringBuffer.append("\n");

            } else {

                break;
            }
        }

        return stringBuffer.toString();
    }
}