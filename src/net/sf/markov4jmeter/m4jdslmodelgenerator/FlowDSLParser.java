package net.sf.markov4jmeter.m4jdslmodelgenerator;

import java.io.File;
import java.io.FileFilter;
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
 * Parser class for reading Flow-DSL content <code>Reader</code> instances.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 * @since    1.5
 */
public class FlowDSLParser {

    /** Suffix of Flow-DSL files. */
    private final static String FILE_SUFFIX = ".flows";

    /** Error message for the case that any input could not be read from a
     *  <code>Reader</code> instance. */
    private final static String ERROR_PARSING =
            "could not parse input from \"%s\":\n%s";

    /** Error message for the case that <code>null</code> has been passed as
     *  a <code>Reader</code> instance. */
    private final static String ERROR_READER_IS_NULL =
            "null has been passed as Reader instance";

    /** Error message for the case that a passed <code>File</code> instance
     *  is <code>null</code>. */
    private final static String ERROR_DIRECTORY_FILE_IS_NULL =
            "directory file is null";

    /** Error message for the case that a passed <code>File</code> instance
     *  which is expected to denote a directory does not fulfill it. */
    private final static String ERROR_DIRECTORY_FILE_DENOTES_NO_DIRECTORY =
            "file \"%s\" does not denote a directory";


    /* *************************  Global variables  ************************* */


    /** (Dependency injected) parser. */
    @Inject
    private IParser parser;


    /* ***************************  Constructors  *************************** */


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
     * Parses all Flow-DSL files of a given directory and returns the root node
     * of the result tree.
     *
     * @param directory
     *     a directory which contains Flow-DSL files, indicated by the suffix
     *     "<code>.flows</code>".
     *
     * @return
     *     the root node of the result tree.
     *
     * @throws IOException
     *     if any I/O error occurs while parsing.
     * @throws IllegalArgumentException
     *     if <code>null</code> has been passed as a <code>File</code> instance,
     *     or if the given file does not denote a directory.
     */
    public EObject parse (final File directory)
            throws IllegalArgumentException, IOException {

        // might throw an IllegalArgument- or IOException;
        final File[] flowFiles = this.readFilesFromDirectory(
                directory,
                FlowDSLParser.FILE_SUFFIX);

        // all content Strings will be concatenated to a single String;
        // TODO: a disadvantage of this approach is that possible syntax errors
        // detected by the parser cannot be mapped to an associated file;
        // alternatively, a HashMap with Flow names as keys with related content
        // as value could be used, for parsing each content individually;
        final StringBuffer stringBuffer = new StringBuffer();

        String content;

        for (final File file : flowFiles) {

            // might throw an IOException;
            content = FileUtils.readFileToString(file);

            stringBuffer.append(content);
        }

        final StringReader stringReader =
                new StringReader(stringBuffer.toString());

        // might throw an IllegalArgument- or IOException;
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

    /**
     * Collects all files with a specified suffix from a given directory.
     *
     * @param directory
     *     directory which contains the files to be collected.
     * @param suffix
     *     suffix of the files to be collected.
     *
     * @return
     *     the files whose suffix matches the specified one.
     *
     * @throws IOException
     *     if any I/O error occurs.
     * @throws IllegalArgumentException
     *     if <code>null</code> has been passed as a <code>File</code> instance,
     *     or if the given file does not denote a directory.
     */
    private File[] readFilesFromDirectory (
            final File directory,
            final String suffix) throws IllegalArgumentException, IOException {

        if (directory == null) {

            throw new IllegalArgumentException(
                    FlowDSLParser.ERROR_DIRECTORY_FILE_IS_NULL);
        }

        if ( !directory.isDirectory() ) {

            final String message = String.format(
                    FlowDSLParser.ERROR_DIRECTORY_FILE_DENOTES_NO_DIRECTORY,
                    directory.getAbsolutePath());

            throw new IllegalArgumentException(message);
        }

        return directory.listFiles(new FileFilter() {

            @Override
            public boolean accept (final File file) {

                return !file.isDirectory() && file.getName().endsWith(suffix);
            }
        });
    }
}