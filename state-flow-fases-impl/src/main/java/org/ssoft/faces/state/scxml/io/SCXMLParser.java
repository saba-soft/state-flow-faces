/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.scxml.io;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.state.ModelException;
import javax.faces.state.PathResolver;
import javax.faces.state.model.Action;
import javax.faces.state.model.Assign;
import javax.faces.state.model.Cancel;
import javax.faces.state.model.CustomAction;
import javax.faces.state.model.Data;
import javax.faces.state.model.Datamodel;
import javax.faces.state.model.Else;
import javax.faces.state.model.ElseIf;
import javax.faces.state.model.Raise;
import javax.faces.state.model.Executable;
import javax.faces.state.model.Exit;
import javax.faces.state.ExternalContent;
import javax.faces.state.model.Final;
import javax.faces.state.model.Finalize;
import javax.faces.state.model.History;
import javax.faces.state.model.If;
import javax.faces.state.model.Initial;
import javax.faces.state.model.Invoke;
import javax.faces.state.model.Log;
import javax.faces.state.NamespacePrefixesHolder;
import javax.faces.state.model.OnEntry;
import javax.faces.state.model.OnExit;
import javax.faces.state.model.Parallel;
import javax.faces.state.model.Param;
import javax.faces.state.PathResolverHolder;
import javax.faces.state.model.Send;
import javax.faces.state.model.State;
import javax.faces.state.model.StateChart;
import javax.faces.state.model.Transition;
import javax.faces.state.model.TransitionTarget;
import javax.faces.state.model.Var;
import javax.faces.state.utils.StateFlowHelper;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ExtendedBaseRules;
import org.apache.commons.digester.NodeCreateRule;
import org.apache.commons.digester.ObjectCreateRule;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.SetNextRule;
import org.apache.commons.digester.SetPropertiesRule;
import org.apache.commons.digester.WithDefaultsRulesWrapper;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class SCXMLParser {

    /**
     * The SCXML namespace that this Digester is built for. Any document that is
     * intended to be parsed by this digester <b>must</b>
     * bind the SCXML elements to this namespace.
     */
    private static final String NAMESPACE_SCXML
            = "http://www.w3.org/2005/07/scxml";

    /**
     * The namespace that defines any custom actions defined by the Commons
     * SCXML implementation. Any document that intends to use these custom
     * actions needs to ensure that they are in the correct namespace. Use of
     * actions in this namespace makes the document non-portable across
     * implementations.
     */
    private static final String NAMESPACE_COMMONS_SCXML
            = "http://commons.apache.org/scxml";

//    //---------------------- PUBLIC METHODS ----------------------//
//    /**
//     * <p>
//     * API for standalone usage where the SCXML document is a URL.</p>
//     *
//     * @param scxmlURL a canonical absolute URL to parse (relative URLs within
//     * the top level document are to be resovled against this URL).
//     * @param errHandler The SAX ErrorHandler
//     *
//     * @return SCXML The SCXML object corresponding to the file argument
//     *
//     * @throws IOException Underlying Digester parsing threw an IOException
//     * @throws SAXException Underlying Digester parsing threw a SAXException
//     * @throws ModelException If the resulting document model has flaws
//     *
//     * @see ErrorHandler
//     * @see PathResolver
//     */
//    public static StateChart parse(final URL scxmlURL,
//            final ErrorHandler errHandler)
//            throws IOException, SAXException, ModelException {
//
//        if (scxmlURL == null) {
//            throw new IllegalArgumentException(ERR_NULL_URL);
//        }
//
//        return parse(scxmlURL, errHandler, null);
//
//    }

    /**
     * <p>
     * API for standalone usage where the SCXML document is a URI. A
     * PathResolver must be provided.</p>
     *
     * @param pathResolver The PathResolver for this context
     * @param documentPath The String pointing to the absolute (real) path
     * of the SCXML document
     * @param errHandler The SAX ErrorHandler
     *
     * @return SCXML The SCXML object corresponding to the file argument
     *
     * @throws IOException Underlying Digester parsing threw an IOException
     * @throws SAXException Underlying Digester parsing threw a SAXException
     * @throws ModelException If the resulting document model has flaws
     *
     * @see ErrorHandler
     * @see PathResolver
     */
    public static StateChart parse(final URL documentPath,
            final ErrorHandler errHandler, final PathResolver pathResolver)
            throws IOException, SAXException, ModelException {

        return parse(documentPath, errHandler, pathResolver, null);

    }

    /**
     * <p>
     * API for standalone usage where the SCXML document is an InputSource. This
     * method may be used when the SCXML document is packaged in a Java archive,
     * or part of a compound document where the SCXML root is available as a
     * <code>org.w3c.dom.Element</code> or via a <code>java.io.Reader</code>.
     * </p>
     *
     * <p>
     * <em>Note:</em> Since there is no path resolution, the SCXML document must
     * not have external state sources.</p>
     *
     * @param documentInputSource The InputSource for the SCXML document
     * @param errHandler The SAX ErrorHandler
     *
     * @return SCXML The SCXML object corresponding to the file argument
     *
     * @throws IOException Underlying Digester parsing threw an IOException
     * @throws SAXException Underlying Digester parsing threw a SAXException
     * @throws ModelException If the resulting document model has flaws
     *
     * @see ErrorHandler
     */
    public static StateChart parse(final InputSource documentInputSource,
            final ErrorHandler errHandler)
            throws IOException, SAXException, ModelException {

        if (documentInputSource == null) {
            throw new IllegalArgumentException(ERR_NULL_ISRC);
        }

        return parse(documentInputSource, errHandler, null);

    }

//    /**
//     * <p>
//     * API for standalone usage where the SCXML document is a URL, and the
//     * document uses custom actions.</p>
//     *
//     * @param scxmlURL a canonical absolute URL to parse (relative URLs within
//     * the top level document are to be resovled against this URL).
//     * @param errHandler The SAX ErrorHandler
//     * @param customActions The list of {@link CustomAction}s this digester
//     * instance will process, can be null or empty
//     *
//     * @return SCXML The SCXML object corresponding to the file argument
//     *
//     * @throws IOException Underlying Digester parsing threw an IOException
//     * @throws SAXException Underlying Digester parsing threw a SAXException
//     * @throws ModelException If the resulting document model has flaws
//     *
//     * @see ErrorHandler
//     * @see PathResolver
//     */
//    public static StateChart parse(final URL scxmlURL,
//            final ErrorHandler errHandler, final List customActions)
//            throws IOException, SAXException, ModelException {
//
//        StateChart scxml = null;
//        Digester scxmlParser = SCXMLParser.newInstance(null, new FacesURLResolver(scxmlURL), customActions);
//        scxmlParser.setErrorHandler(errHandler);
//
//        try {
//            scxml = (StateChart) scxmlParser.parse(scxmlURL.toString());
//        } catch (RuntimeException rte) {
//            // Intercept runtime exceptions, only to log them with a
//            // sensible error message about failure in document parsing
//            MessageFormat msgFormat = new MessageFormat(ERR_DOC_PARSE_FAIL);
//            String errMsg = msgFormat.format(new Object[]{
//                String.valueOf(scxmlURL), rte.getMessage()
//            });
//            org.apache.commons.logging.Log log = LogFactory.
//                    getLog(SCXMLParser.class);
//            log.error(errMsg, rte);
//            throw rte;
//        }
//
//        if (scxml != null) {
//            ModelUpdater.updateSCXML(scxml);
//        }
//
//        return scxml;
//
//    }

    /**
     * <p>
     * API for standalone usage where the SCXML document is a URI. A
     * PathResolver must be provided.</p>
     *
     * @param pathResolver The PathResolver for this context
     * @param documentPath The String pointing to the absolute (real) path
     * of the SCXML document
     * @param errHandler The SAX ErrorHandler
     * @param customActions The list of {@link CustomAction}s this digester
     * instance will process, can be null or empty
     *
     * @return SCXML The SCXML object corresponding to the file argument
     *
     * @throws IOException Underlying Digester parsing threw an IOException
     * @throws SAXException Underlying Digester parsing threw a SAXException
     * @throws ModelException If the resulting document model has flaws
     *
     * @see ErrorHandler
     * @see PathResolver
     */
    public static StateChart parse(final URL documentPath,
            final ErrorHandler errHandler, final PathResolver pathResolver,
            final List customActions)
            throws IOException, SAXException, ModelException {

        if (documentPath == null) {
            throw new IllegalArgumentException(ERR_NULL_PATH);
        }

        StateChart scxml = null;
        Digester scxmlParser = SCXMLParser.newInstance(null, pathResolver,
                customActions);
        scxmlParser.setErrorHandler(errHandler);

        try {
            scxml = (StateChart) scxmlParser.parse(documentPath);
        } catch (RuntimeException rte) {
            // Intercept runtime exceptions, only to log them with a
            // sensible error message about failure in document parsing
            MessageFormat msgFormat = new MessageFormat(ERR_DOC_PARSE_FAIL);
            String errMsg = msgFormat.format(new Object[]{
                documentPath, rte.getMessage()
            });
            org.apache.commons.logging.Log log = LogFactory.
                    getLog(SCXMLParser.class);
            log.error(errMsg, rte);
            throw rte;
        }

        if (scxml != null) {
            ModelUpdater.updateSCXML(scxml);
        }

        return scxml;

    }

    /**
     * <p>
     * API for standalone usage where the SCXML document is an InputSource. This
     * method may be used when the SCXML document is packaged in a Java archive,
     * or part of a compound document where the SCXML root is available as a
     * <code>org.w3c.dom.Element</code> or via a <code>java.io.Reader</code>.
     * </p>
     *
     * <p>
     * <em>Note:</em> Since there is no path resolution, the SCXML document must
     * not have external state sources.</p>
     *
     * @param documentInputSource The InputSource for the SCXML document
     * @param errHandler The SAX ErrorHandler
     * @param customActions The list of {@link CustomAction}s this digester
     * instance will process, can be null or empty
     *
     * @return SCXML The SCXML object corresponding to the file argument
     *
     * @throws IOException Underlying Digester parsing threw an IOException
     * @throws SAXException Underlying Digester parsing threw a SAXException
     * @throws ModelException If the resulting document model has flaws
     *
     * @see ErrorHandler
     */
    public static StateChart parse(final InputSource documentInputSource,
            final ErrorHandler errHandler, final List customActions)
            throws IOException, SAXException, ModelException {

        Digester scxmlParser = SCXMLParser.newInstance(null, null,
                customActions);
        scxmlParser.setErrorHandler(errHandler);

        StateChart scxml = null;
        try {
            scxml = (StateChart) scxmlParser.parse(documentInputSource);
        } catch (RuntimeException rte) {
            // Intercept runtime exceptions, only to log them with a
            // sensible error message about failure in document parsing
            org.apache.commons.logging.Log log = LogFactory.
                    getLog(SCXMLParser.class);
            log.error(ERR_ISRC_PARSE_FAIL, rte);
            throw rte;
        }

        if (scxml != null) {
            ModelUpdater.updateSCXML(scxml);
        }

        return scxml;

    }

    /**
     * <p>
     * Obtain a SCXML digester instance for further customization.</p>
     * <b>API Notes:</b>
     * <ul>
     * <li>Use the digest() convenience methods if you do not need a custom
     * digester.</li>
     * <li>After the SCXML document is parsed by the customized digester, the
     * object model <b>must</b> be made executor-ready by calling
     * <code>updateSCXML(SCXML)</code> method in this class.</li>
     * </ul>
     *
     * @return Digester A newly configured SCXML digester instance
     *
     * @see SCXMLParser#updateSCXML(SCXML)
     */
    public static Digester newInstance() {

        return newInstance(null, null, null);

    }

    /**
     * <p>
     * Obtain a SCXML digester instance for further customization.</p>
     * <b>API Notes:</b>
     * <ul>
     * <li>Use the digest() convenience methods if you do not need a custom
     * digester.</li>
     * <li>After the SCXML document is parsed by the customized digester, the
     * object model <b>must</b> be made executor-ready by calling
     * <code>updateSCXML(SCXML)</code> method in this class.</li>
     * </ul>
     *
     * @param pr The PathResolver, may be null for standalone documents
     * @return Digester A newly configured SCXML digester instance
     *
     * @see SCXMLParser#updateSCXML(SCXML)
     */
    public static Digester newInstance(final PathResolver pr) {

        return newInstance(null, pr, null);

    }

    /**
     * <p>
     * Obtain a SCXML digester instance for further customization.</p>
     * <b>API Notes:</b>
     * <ul>
     * <li>Use the digest() convenience methods if you do not need a custom
     * digester.</li>
     * <li>After the SCXML document is parsed by the customized digester, the
     * object model <b>must</b> be made executor-ready by calling
     * <code>updateSCXML(SCXML)</code> method in this class.</li>
     * </ul>
     *
     * @param scxml The parent SCXML document if there is one (in case of state
     * templates for example), null otherwise
     * @param pr The PathResolver, may be null for standalone documents
     * @return Digester A newly configured SCXML digester instance
     *
     * @see SCXMLParser#updateSCXML(SCXML)
     */
    public static Digester newInstance(final StateChart scxml,
            final PathResolver pr) {

        return newInstance(scxml, pr, null);

    }

    /**
     * <p>
     * Obtain a SCXML digester instance for further customization.</p>
     * <b>API Notes:</b>
     * <ul>
     * <li>Use the digest() convenience methods if you do not need a custom
     * digester.</li>
     * <li>After the SCXML document is parsed by the customized digester, the
     * object model <b>must</b> be made executor-ready by calling
     * <code>updateSCXML(SCXML)</code> method in this class.</li>
     * </ul>
     *
     * @param scxml The parent SCXML document if there is one (in case of state
     * templates for example), null otherwise
     * @param pr The PathResolver, may be null for standalone documents
     * @param customActions The list of {@link CustomAction}s this digester
     * instance will process, can be null or empty
     * @return Digester A newly configured SCXML digester instance
     *
     * @see SCXMLParser#updateSCXML(SCXML)
     */
    public static Digester newInstance(final StateChart scxml,
            final PathResolver pr, final List customActions) {

        Digester digester = new Digester();
        digester.setNamespaceAware(true);
        //Uncomment next line after SCXML DTD is available
        //digester.setValidating(true);
        WithDefaultsRulesWrapper rules
                = new WithDefaultsRulesWrapper(initRules(scxml, pr, customActions));
        rules.addDefault(new IgnoredElementRule());
        digester.setRules(rules);
        return digester;
    }

    /**
     * <p>
     * Update the SCXML object model and make it SCXMLExecutor ready. This is
     * part of post-digester processing, and sets up the necessary object
     * references throughtout the SCXML object model for the parsed document.
     * Should be used only if a customized digester obtained using the
     * <code>newInstance()</code> methods is needed.</p>
     *
     * @param scxml The SCXML object (output from Digester)
     * @throws ModelException If the document model has flaws
     */
    public static void updateSCXML(final StateChart scxml)
            throws ModelException {
        ModelUpdater.updateSCXML(scxml);
    }

    //---------------------- PRIVATE CONSTANTS ----------------------//
    //// Patterns to get the digestion going, prefixed by XP_
    /**
     * Root &lt;scxml&gt; element.
     */
    private static final String XP_SM = "scxml";

    /**
     * &lt;state&gt; children of root &lt;scxml&gt; element.
     */
    private static final String XP_SM_ST = "scxml/state";

    /**
     * &lt;state&gt; children of root &lt;scxml&gt; element.
     */
    private static final String XP_SM_PAR = "scxml/parallel";

    /**
     * &lt;final&gt; children of root &lt;scxml&gt; element.
     */
    private static final String XP_SM_FIN = "scxml/final";

    //// Universal matches, prefixed by XPU_
    // State
    /**
     * &lt;state&gt; children of &lt;state&gt; elements.
     */
    private static final String XPU_ST_ST = "!*/state/state";

    /**
     * &lt;final&gt; children of &lt;state&gt; elements.
     */
    private static final String XPU_ST_FIN = "!*/state/final";

    /**
     * &lt;state&gt; children of &lt;parallel&gt; elements.
     */
    private static final String XPU_PAR_ST = "!*/parallel/state";

    // Parallel
    /**
     * &lt;parallel&gt; child of &lt;state&gt; elements.
     */
    private static final String XPU_ST_PAR = "!*/state/parallel";

    // If
    /**
     * &lt;if&gt; element.
     */
    private static final String XPU_IF = "!*/if";

    // Executables, next three patterns useful when adding custom actions
    /**
     * &lt;onentry&gt; element.
     */
    private static final String XPU_ONEN = "!*/onentry";

    /**
     * &lt;onexit&gt; element.
     */
    private static final String XPU_ONEX = "!*/onexit";

    /**
     * &lt;transition&gt; element.
     */
    private static final String XPU_TR = "!*/transition";

    /**
     * &lt;finalize&gt; element.
     */
    private static final String XPU_FIN = "!*/finalize";

    //// Path Fragments, constants prefixed by XPF_
    // Onentries and Onexits
    /**
     * &lt;onentry&gt; child element.
     */
    private static final String XPF_ONEN = "/onentry";

    /**
     * &lt;onexit&gt; child element.
     */
    private static final String XPF_ONEX = "/onexit";

    // Datamodel section
    /**
     * &lt;datamodel&gt; child element.
     */
    private static final String XPF_DM = "/datamodel";

    /**
     * Individual &lt;data&gt; elements.
     */
    private static final String XPF_DATA = "/data";

    // Initial
    /**
     * &lt;initial&gt; child element.
     */
    private static final String XPF_INI = "/initial";

    // Invoke, param and finalize
    /**
     * &lt;invoke&gt; child element of &lt;state&gt;.
     */
    private static final String XPF_INV = "/invoke";

    /**
     * &lt;param&gt; child element of &lt;invoke&gt;.
     */
    private static final String XPF_PRM = "/param";

    /**
     * &lt;finalize&gt; child element of &lt;invoke&gt;.
     */
    private static final String XPF_FIN = "/finalize";

    // History
    /**
     * &lt;history&gt; child element.
     */
    private static final String XPF_HIST = "/history";

    // Transition, target and exit
    /**
     * &lt;transition&gt; child element.
     */
    private static final String XPF_TR = "/transition";

    /**
     * &lt;exit&gt; child element, a Commons SCXML custom action.
     */
    private static final String XPF_EXT = "/exit";

    // Actions
    /**
     * &lt;assign&gt; child element.
     */
    private static final String XPF_ASN = "/assign";

    /**
     * &lt;event&gt; child element.
     */
    private static final String XPF_EVT = "/event";

    /**
     * &lt;send&gt; child element.
     */
    private static final String XPF_SND = "/send";

    /**
     * &lt;cancel&gt; child element.
     */
    private static final String XPF_CAN = "/cancel";

    /**
     * &lt;elseif&gt; child element.
     */
    private static final String XPF_EIF = "/elseif";

    /**
     * &lt;else&gt; child element.
     */
    private static final String XPF_ELS = "/else";

    // Custom Commons SCXML actions
    /**
     * &lt;var&gt; child element.
     */
    private static final String XPF_VAR = "/var";

    /**
     * &lt;log&gt; child element.
     */
    private static final String XPF_LOG = "/log";

    //// Other constants
    // Error messages
    /**
     * Null URL passed as argument.
     */
    private static final String ERR_NULL_URL = "Cannot parse null URL";

    /**
     * Null path passed as argument.
     */
    private static final String ERR_NULL_PATH = "Cannot parse null URL";

    /**
     * Null InputSource passed as argument.
     */
    private static final String ERR_NULL_ISRC = "Cannot parse null URL";

    /**
     * Parsing SCXML document has failed.
     */
    private static final String ERR_DOC_PARSE_FAIL = "Error parsing "
            + "SCXML document: \"{0}\", with message: \"{1}\"\n";

    /**
     * Parsing SCXML document InputSource has failed.
     */
    private static final String ERR_ISRC_PARSE_FAIL
            = "Could not parse SCXML InputSource";

    /**
     * Parser configuration error while registering data rule.
     */
    private static final String ERR_PARSER_CFG_DATA = "XML Parser "
            + "misconfiguration, error registering <data> element rule";

    /**
     * Parser configuration error while registering send rule.
     */
    private static final String ERR_PARSER_CFG_SEND = "XML Parser "
            + "misconfiguration, error registering <send> element rule";

    /**
     * Parser configuration error while registering body content rule for custom
     * action.
     */
    private static final String ERR_PARSER_CFG_CUSTOM = "XML Parser "
            + "misconfiguration, error registering custom action rules";

    /**
     * Error message while attempting to define a custom action which does not
     * extend the Commons SCXML Action base class.
     */
    private static final String ERR_CUSTOM_ACTION_TYPE = "Custom actions list"
            + " contained unknown object (not a Commons SCXML Action subtype)";

    /**
     * Error message when the URI in a &lt;state&gt;'s &quot;src&quot; attribute
     * does not point to a valid SCXML document, and thus cannot be parsed.
     */
    private static final String ERR_STATE_SRC
            = "Source attribute in <state src=\"{0}\"> cannot be parsed";

    /**
     * Error message when the target of the URI fragment in a &lt;state&gt;'s
     * &quot;src&quot; attribute is not defined in the referenced document.
     */
    private static final String ERR_STATE_SRC_FRAGMENT = "URI Fragment in "
            + "<state src=\"{0}\"> is an unknown state in referenced document";

    /**
     * Error message when the target of the URI fragment in a &lt;state&gt;'s
     * &quot;src&quot; attribute is not a &lt;state&gt; or &lt;final&gt; in the
     * referenced document.
     */
    private static final String ERR_STATE_SRC_FRAGMENT_TARGET = "URI Fragment"
            + " in <state src=\"{0}\"> does not point to a <state> or <final>";

    // String constants
    /**
     * Slash.
     */
    private static final String STR_SLASH = "/";

    //---------------------- PRIVATE UTILITY METHODS ----------------------//
    /*
     * Private utility functions for configuring digester rule base for SCXML.
     */
    /**
     * Initialize the Digester rules for the current document.
     *
     * @param scxml The parent SCXML document (or null)
     * @param pr The PathResolver
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     *
     * @return scxmlRules The rule set to be used for digestion
     */
    private static ExtendedBaseRules initRules(final StateChart scxml,
            final PathResolver pr, final List customActions) {

        ExtendedBaseRules scxmlRules = new ExtendedBaseRules();
        scxmlRules.setNamespaceURI(NAMESPACE_SCXML);

        //// SCXML
        scxmlRules.add(XP_SM, new ObjectCreateRule(StateChart.class));
        scxmlRules.add(XP_SM, new SetPropertiesRule());
        scxmlRules.add(XP_SM, new SetCurrentNamespacesRule());

        //// Datamodel at document root i.e. <scxml> datamodel
        addDatamodelRules(XP_SM + XPF_DM, scxmlRules, scxml, pr);

        //// States
        // Level one states
        addStateRules(XP_SM_ST, scxmlRules, customActions, scxml, pr);

        // Nested states
        addStateRules(XPU_ST_ST, scxmlRules, customActions, scxml, pr);

        // Orthogonal states
        addStateRules(XPU_PAR_ST, scxmlRules, customActions, scxml, pr);

        //// Parallels
        // Level one parallels
        addParallelRules(XP_SM_PAR, scxmlRules, customActions, scxml, pr);

        // Parallel children of composite states
        addParallelRules(XPU_ST_PAR, scxmlRules, customActions, scxml, pr);

        //// Finals
        // Level one finals
        addFinalRules(XP_SM_FIN, scxmlRules, customActions, scxml, pr);

        // Final children of composite states
        addFinalRules(XPU_ST_FIN, scxmlRules, customActions, scxml, pr);

        //// Ifs
        addIfRules(XPU_IF, scxmlRules, pr, customActions);

        //// Custom actions
        addCustomActionRules(XPU_ONEN, scxmlRules, customActions);
        addCustomActionRules(XPU_ONEX, scxmlRules, customActions);
        addCustomActionRules(XPU_TR, scxmlRules, customActions);
        addCustomActionRules(XPU_IF, scxmlRules, customActions);
        addCustomActionRules(XPU_FIN, scxmlRules, customActions);

        return scxmlRules;

    }

    /**
     * Add Digester rules for all &lt;state&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param scxml The parent SCXML document (or null)
     * @param pr The PathResolver
     */
    private static void addStateRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final StateChart scxml, final PathResolver pr) {
        scxmlRules.add(xp, new ObjectCreateRule(State.class));
        addStatePropertiesRules(xp, scxmlRules, customActions, pr, scxml);
        addDatamodelRules(xp + XPF_DM, scxmlRules, scxml, pr);
        addInvokeRules(xp + XPF_INV, scxmlRules, customActions, pr, scxml);
        addInitialRules(xp + XPF_INI, scxmlRules, customActions, pr, scxml);
        addHistoryRules(xp + XPF_HIST, scxmlRules, customActions, pr, scxml);
        addTransitionRules(xp + XPF_TR, scxmlRules, "addTransition",
                pr, customActions);
        addHandlerRules(xp, scxmlRules, pr, customActions);
        scxmlRules.add(xp, new UpdateModelRule(scxml));
        scxmlRules.add(xp, new SetNextRule("addChild"));
    }

    /**
     * Add Digester rules for all &lt;parallel&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param pr The {@link PathResolver} for this document
     * @param scxml The parent SCXML document (or null)
     */
    private static void addParallelRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final StateChart scxml, final PathResolver pr) {
        addSimpleRulesTuple(xp, scxmlRules, Parallel.class, null, null,
                "addChild");
        addDatamodelRules(xp + XPF_DM, scxmlRules, scxml, pr);
        addTransitionRules(xp + XPF_TR, scxmlRules, "addTransition",
                pr, customActions);
        addHandlerRules(xp, scxmlRules, pr, customActions);
        scxmlRules.add(xp, new UpdateModelRule(scxml));
    }

    /**
     * Add Digester rules for all &lt;final&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param scxml The parent SCXML document (or null)
     * @param pr The {@link PathResolver} for this document
     */
    private static void addFinalRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final StateChart scxml, final PathResolver pr) {
        addSimpleRulesTuple(xp, scxmlRules, Final.class, null, null,
                "addChild");
        addHandlerRules(xp, scxmlRules, pr, customActions);
        scxmlRules.add(xp, new UpdateModelRule(scxml));
    }

    /**
     * Add Digester rules for all &lt;state&gt; element attributes.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param pr The PathResolver
     * @param scxml The root document, if this one is src'ed in
     */
    private static void addStatePropertiesRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final PathResolver pr, final StateChart scxml) {
        scxmlRules.add(xp, new SetPropertiesRule(
                new String[]{"id", "final", "initial"},
                new String[]{"id", "final", "first"}));
        scxmlRules.add(xp, new DigestSrcAttributeRule(scxml,
                customActions, pr));
    }

    /**
     * Add Digester rules for all &lt;datamodel&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param pr The PathResolver
     * @param scxml The parent SCXML document (or null)
     */
    private static void addDatamodelRules(final String xp,
            final ExtendedBaseRules scxmlRules, final StateChart scxml,
            final PathResolver pr) {
        scxmlRules.add(xp, new ObjectCreateRule(Datamodel.class));
        scxmlRules.add(xp + XPF_DATA, new ObjectCreateRule(Data.class));
        scxmlRules.add(xp + XPF_DATA, new SetPropertiesRule());
        scxmlRules.add(xp + XPF_DATA, new SetCurrentNamespacesRule());
        scxmlRules.add(xp + XPF_DATA, new SetNextRule("addData"));
        try {
            scxmlRules.add(xp + XPF_DATA, new ParseDataRule(pr));
        } catch (ParserConfigurationException pce) {
            org.apache.commons.logging.Log log = LogFactory.
                    getLog(SCXMLParser.class);
            log.error(ERR_PARSER_CFG_DATA, pce);
        }
        scxmlRules.add(xp, new SetNextRule("setDatamodel"));
    }

    /**
     * Add Digester rules for all &lt;invoke&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of {@link CustomAction}s this digester
     * instance will process, can be null or empty
     * @param pr The PathResolver
     * @param scxml The parent SCXML document (or null)
     */
    private static void addInvokeRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final PathResolver pr, final StateChart scxml) {
        scxmlRules.add(xp, new ObjectCreateRule(Invoke.class));
        scxmlRules.add(xp, new SetPropertiesRule());
        scxmlRules.add(xp, new SetCurrentNamespacesRule());
        scxmlRules.add(xp, new SetPathResolverRule(pr));
        scxmlRules.add(xp + XPF_PRM, new ObjectCreateRule(Param.class));
        scxmlRules.add(xp + XPF_PRM, new SetPropertiesRule());
        scxmlRules.add(xp + XPF_PRM, new SetCurrentNamespacesRule());
        scxmlRules.add(xp + XPF_PRM, new SetNextRule("addParam"));
        scxmlRules.add(xp + XPF_FIN, new ObjectCreateRule(Finalize.class));
        scxmlRules.add(xp + XPF_FIN, new UpdateFinalizeRule());
        addActionRules(xp + XPF_FIN, scxmlRules, pr, customActions);
        scxmlRules.add(xp + XPF_FIN, new SetNextRule("setFinalize"));
        scxmlRules.add(xp, new SetNextRule("setInvoke"));
    }

    /**
     * Add Digester rules for all &lt;initial&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param pr The PathResolver
     * @param scxml The parent SCXML document (or null)
     */
    private static void addInitialRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final PathResolver pr, final StateChart scxml) {
        scxmlRules.add(xp, new ObjectCreateRule(Initial.class));
        addPseudoStatePropertiesRules(xp, scxmlRules, customActions, pr,
                scxml);
        scxmlRules.add(xp, new UpdateModelRule(scxml));
        addTransitionRules(xp + XPF_TR, scxmlRules, "setTransition",
                pr, customActions);
        scxmlRules.add(xp, new SetNextRule("setInitial"));
    }

    /**
     * Add Digester rules for all &lt;history&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param pr The PathResolver
     * @param scxml The parent SCXML document (or null)
     */
    private static void addHistoryRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final PathResolver pr, final StateChart scxml) {
        scxmlRules.add(xp, new ObjectCreateRule(History.class));
        addPseudoStatePropertiesRules(xp, scxmlRules, customActions, pr,
                scxml);
        scxmlRules.add(xp, new UpdateModelRule(scxml));
        scxmlRules.add(xp, new SetPropertiesRule(new String[]{"type"},
                new String[]{"type"}));
        addTransitionRules(xp + XPF_TR, scxmlRules, "setTransition",
                pr, customActions);
        scxmlRules.add(xp, new SetNextRule("addHistory"));
    }

    /**
     * Add Digester rules for all pseudo state (initial, history) element
     * attributes.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     * @param pr The PathResolver
     * @param scxml The root document, if this one is src'ed in
     */
    private static void addPseudoStatePropertiesRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions,
            final PathResolver pr, final StateChart scxml) {
        scxmlRules.add(xp, new SetPropertiesRule(new String[]{"id"},
                new String[]{"id"}));
        scxmlRules.add(xp, new DigestSrcAttributeRule(scxml, customActions,
                pr));
    }

    /**
     * Add Digester rules for all &lt;transition&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param setNextMethod The method name for adding this transition to its
     * parent (defined by the SCXML Java object model).
     * @param pr The {@link PathResolver} for this document
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     */
    private static void addTransitionRules(final String xp,
            final ExtendedBaseRules scxmlRules, final String setNextMethod,
            final PathResolver pr, final List customActions) {
        scxmlRules.add(xp, new ObjectCreateRule(Transition.class));
        scxmlRules.add(xp, new SetPropertiesRule(
                new String[]{"event", "cond", "target"},
                new String[]{"event", "cond", "next"}));
        scxmlRules.add(xp, new SetCurrentNamespacesRule());
        addActionRules(xp, scxmlRules, pr, customActions);

        // Add <exit> custom action rule in Commons SCXML namespace
        scxmlRules.setNamespaceURI(NAMESPACE_COMMONS_SCXML);
        scxmlRules.add(xp + XPF_EXT, new Rule() {
            @Override
            public void end(final String namespace, final String name) {
                Transition t = (Transition) getDigester().peek(1);
                TransitionTarget tt = (TransitionTarget) getDigester().
                        peek(2);
                if (tt instanceof Initial) {
                    org.apache.commons.logging.Log log = LogFactory.getLog(SCXMLParser.class);
                    log.warn("Ignored <exit> action in <initial>");
                } else {
                    State exitState = new State();
                    exitState.setFinal(true);
                    t.getTargets().add(exitState);
                }
            }
        });
        scxmlRules.setNamespaceURI(NAMESPACE_SCXML);

        scxmlRules.add(xp, new SetNextRule(setNextMethod));
    }

    /**
     * Add Digester rules for all &lt;onentry&gt; and &lt;onexit&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param pr The {@link PathResolver} for this document
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     */
    private static void addHandlerRules(final String xp,
            final ExtendedBaseRules scxmlRules, final PathResolver pr,
            final List customActions) {
        scxmlRules.add(xp + XPF_ONEN, new ObjectCreateRule(OnEntry.class));
        addActionRules(xp + XPF_ONEN, scxmlRules, pr, customActions);
        scxmlRules.add(xp + XPF_ONEN, new SetNextRule("setOnEntry"));
        scxmlRules.add(xp + XPF_ONEX, new ObjectCreateRule(OnExit.class));
        addActionRules(xp + XPF_ONEX, scxmlRules, pr, customActions);
        scxmlRules.add(xp + XPF_ONEX, new SetNextRule("setOnExit"));
    }

    /**
     * Add Digester rules for all actions (&quot;executable&quot; elements).
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param pr The {@link PathResolver} for this document
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     */
    private static void addActionRules(final String xp,
            final ExtendedBaseRules scxmlRules, final PathResolver pr,
            final List customActions) {
        // Actions in SCXML namespace
        addActionRulesTuple(xp + XPF_ASN, scxmlRules, Assign.class);
        scxmlRules.add(xp + XPF_ASN, new SetPathResolverRule(pr));
        addActionRulesTuple(xp + XPF_EVT, scxmlRules, Raise.class);
        addSendRulesTuple(xp + XPF_SND, scxmlRules);
        addActionRulesTuple(xp + XPF_CAN, scxmlRules, Cancel.class);
        addActionRulesTuple(xp + XPF_LOG, scxmlRules, Log.class);

        // Actions in Commons SCXML namespace
        scxmlRules.setNamespaceURI(NAMESPACE_COMMONS_SCXML);

        addActionRulesTuple(xp + XPF_VAR, scxmlRules, Var.class);
        addActionRulesTuple(xp + XPF_EXT, scxmlRules, Exit.class);

        // Reset namespace
        scxmlRules.setNamespaceURI(NAMESPACE_SCXML);
    }

    /**
     * Add custom action rules, if any custom actions are provided.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     */
    private static void addCustomActionRules(final String xp,
            final ExtendedBaseRules scxmlRules, final List customActions) {
        if (customActions == null || customActions.isEmpty()) {
            return;
        }
        for (Object item : customActions) {
            if (item == null || !(item instanceof CustomAction)) {
                org.apache.commons.logging.Log log = LogFactory.getLog(SCXMLParser.class);
                log.warn(ERR_CUSTOM_ACTION_TYPE);
            } else {
                CustomAction ca = (CustomAction) item;
                scxmlRules.setNamespaceURI(ca.getNamespaceURI());
                String xpfLocalName = STR_SLASH + ca.getLocalName();
                Class klass = ca.getActionClass();
                if (StateFlowHelper.implementationOf(klass, ExternalContent.class)) {
                    addCustomActionRulesTuple(xp + xpfLocalName, scxmlRules, klass, true);
                } else {
                    addCustomActionRulesTuple(xp + xpfLocalName, scxmlRules, klass, false);
                }
            }
        }
        scxmlRules.setNamespaceURI(NAMESPACE_SCXML);
    }

    /**
     * Add Digester rules that are specific to the &lt;send&gt; action element.
     *
     * @param xp The Digester style XPath expression of &lt;send&gt; element
     * @param scxmlRules The rule set to be used for digestion
     */
    private static void addSendRulesTuple(final String xp,
            final ExtendedBaseRules scxmlRules) {
        addActionRulesTuple(xp, scxmlRules, Send.class);
        try {
            scxmlRules.add(xp, new ParseExternalContentRule());
        } catch (ParserConfigurationException pce) {
            org.apache.commons.logging.Log log = LogFactory.getLog(SCXMLParser.class);
            log.error(ERR_PARSER_CFG_SEND, pce);
        }
    }

    /**
     * Add Digester rules for a simple custom action (no body content).
     *
     * @param xp The path to the custom action element
     * @param scxmlRules The rule set to be used for digestion
     * @param klass The <code>Action</code> class implementing the custom
     * action.
     * @param bodyContent Whether the custom rule has body content that should
     * be parsed using <code>NodeCreateRule</code>
     */
    private static void addCustomActionRulesTuple(final String xp,
            final ExtendedBaseRules scxmlRules, final Class klass,
            final boolean bodyContent) {
        addActionRulesTuple(xp, scxmlRules, klass);
        if (bodyContent) {
            try {
                scxmlRules.add(xp, new ParseExternalContentRule());
            } catch (ParserConfigurationException pce) {
                org.apache.commons.logging.Log log = LogFactory.
                        getLog(SCXMLParser.class);
                log.error(ERR_PARSER_CFG_CUSTOM, pce);
            }
        }
    }

    /**
     * Add Digester rules for all &lt;if&gt; elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param pr The {@link PathResolver} for this document
     * @param customActions The list of custom actions this digester needs to be
     * able to process
     */
    private static void addIfRules(final String xp,
            final ExtendedBaseRules scxmlRules, final PathResolver pr,
            final List customActions) {
        addActionRulesTuple(xp, scxmlRules, If.class);
        addActionRules(xp, scxmlRules, pr, customActions);
        addActionRulesTuple(xp + XPF_EIF, scxmlRules, ElseIf.class);
        addActionRulesTuple(xp + XPF_ELS, scxmlRules, Else.class);
    }

    /**
     * Add Digester rules that are common across all actions elements.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param klass The class in the Java object model to be instantiated in the
     * ObjectCreateRule for this action
     */
    private static void addActionRulesTuple(final String xp,
            final ExtendedBaseRules scxmlRules, final Class klass) {
        addSimpleRulesTuple(xp, scxmlRules, klass, null, null, "addAction");
        scxmlRules.add(xp, new SetExecutableParentRule());
        scxmlRules.add(xp, new SetCurrentNamespacesRule());
    }

    /**
     * Add the run of the mill Digester rules for any element.
     *
     * @param xp The Digester style XPath expression of the parent XML element
     * @param scxmlRules The rule set to be used for digestion
     * @param klass The class in the Java object model to be instantiated in the
     * ObjectCreateRule for this action
     * @param args The attributes to be mapped into the object model
     * @param props The properties that args get mapped to
     * @param addMethod The method that the SetNextRule should call
     */
    private static void addSimpleRulesTuple(final String xp,
            final ExtendedBaseRules scxmlRules, final Class klass,
            final String[] args, final String[] props,
            final String addMethod) {
        scxmlRules.add(xp, new ObjectCreateRule(klass));
        if (args == null) {
            scxmlRules.add(xp, new SetPropertiesRule());
        } else {
            scxmlRules.add(xp, new SetPropertiesRule(args, props));
        }
        scxmlRules.add(xp, new SetNextRule(addMethod));
    }

    /**
     * Discourage instantiation since this is a utility class.
     */
    private SCXMLParser() {
        super();
    }

    /**
     * Custom digestion rule for establishing necessary associations of this
     * TransitionTarget with the root SCXML object. These include: <br>
     * 1) Updation of the SCXML object's global targets Map <br>
     * 2) Obtaining a handle to the SCXML object's NotificationRegistry <br>
     */
    private static class UpdateModelRule extends Rule {

        /**
         * The root SCXML object.
         */
        private StateChart scxml;

        /**
         * Constructor.
         *
         * @param scxml The root SCXML object
         */
        UpdateModelRule(final StateChart scxml) {
            super();
            this.scxml = scxml;
        }

        /**
         * @see Rule#end(String, String)
         */
        @Override
        public final void end(final String namespace, final String name) {
            if (scxml == null) {
                scxml = (StateChart) getDigester()
                        .peek(getDigester().getCount() - 1);
            }
            TransitionTarget tt = (TransitionTarget) getDigester().peek();
            scxml.addTarget(tt);
        }
    }

    /**
     * Custom digestion rule for setting Executable parent of Action elements.
     */
    private static class SetExecutableParentRule extends Rule {

        /**
         * Constructor.
         */
        SetExecutableParentRule() {
            super();
        }

        /**
         * @see Rule#end(String, String)
         */
        @Override
        public final void end(final String namespace, final String name) {
            Action child = (Action) getDigester().peek();
            for (int i = 1; i < getDigester().getCount() - 1; i++) {
                Object ancestor = getDigester().peek(i);
                if (ancestor instanceof Executable) {
                    child.setParent((Executable) ancestor);
                    return;
                }
            }
        }
    }

    /**
     * Custom digestion rule for parsing bodies of <code>ExternalContent</code>
     * elements.
     *
     * @see ExternalContent
     */
    private static class ParseExternalContentRule extends NodeCreateRule {

        /**
         * Constructor.
         *
         * @throws ParserConfigurationException A JAXP configuration error
         */
        ParseExternalContentRule()
                throws ParserConfigurationException {
            super();
        }

        /**
         * @see Rule#end(String, String)
         */
        @Override
        public final void end(final String namespace, final String name) {
            Element bodyElement = (Element) getDigester().pop();
            NodeList childNodes = bodyElement.getChildNodes();
            List externalNodes = ((ExternalContent) getDigester().
                    peek()).getExternalNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                externalNodes.add(childNodes.item(i));
            }
        }
    }

    /**
     * Custom digestion rule for parsing bodies of &lt;data&gt; elements.
     */
    private static class ParseDataRule extends NodeCreateRule {

        /**
         * The PathResolver used to resolve the src attribute to the SCXML
         * document it points to.
         *
         * @see PathResolver
         */
        private PathResolver pr;

        /**
         * The "src" attribute, retained to check if body content is legal.
         */
        private String src;

        /**
         * The "expr" attribute, retained to check if body content is legal.
         */
        private String expr;

        /**
         * The XML tree for this data, parse as a Node, obtained from either the
         * "src" or the "expr" attributes.
         */
        private Node attrNode;

        /**
         * Constructor.
         *
         * @param pr The <code>PathResolver</code>
         * @throws ParserConfigurationException A JAXP configuration error
         */
        ParseDataRule(final PathResolver pr)
                throws ParserConfigurationException {
            super();
            this.pr = pr;
        }

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) throws Exception {
            super.begin(namespace, name, attributes);
            src = attributes.getValue("src");
            expr = attributes.getValue("expr");
            if (!StateFlowHelper.isStringEmpty(src)) {
                String path;
                if (pr == null) {
                    path = src;
                } else {
                    path = pr.resolvePath(src);
                }
                try {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.
                            newInstance();
                    DocumentBuilder db = dbFactory.newDocumentBuilder();
                    attrNode = db.parse(path);
                } catch (Throwable t) { // you read that correctly
                    org.apache.commons.logging.Log log = LogFactory.getLog(SCXMLParser.class);
                    log.error(t.getMessage(), t);
                }
            }
        }

        /**
         * @see Rule#end(String, String)
         */
        @Override
        public final void end(final String namespace, final String name) {
            Node bodyNode = (Node) getDigester().pop();
            Data data = ((Data) getDigester().peek());
            // Prefer "src" over "expr", "expr" over child nodes
            // "expr" can only be evaluated at execution time
            if (!StateFlowHelper.isStringEmpty(src)) {
                data.setNode(attrNode);
            } else if (StateFlowHelper.isStringEmpty(expr)) {
                // both "src" and "expr" are empty
                data.setNode(bodyNode);
            }
        }
    }

    /**
     * Custom digestion rule for external sources, that is, the src attribute of
     * the &lt;state&gt; element.
     */
    private static class DigestSrcAttributeRule extends Rule {

        /**
         * The PathResolver used to resolve the src attribute to the SCXML
         * document it points to.
         *
         * @see PathResolver
         */
        private final PathResolver pr;

        /**
         * The root document.
         */
        private StateChart root;

        /**
         * The list of custom actions the parent document is capable of
         * processing (and hence, the child should be, by transitivity).
         *
         * @see CustomAction
         */
        private final List customActions;

        /**
         * Constructor.
         *
         * @param pr The PathResolver
         * @param customActions The list of custom actions this digester needs
         * to be able to process
         *
         * @see PathResolver
         * @see CustomAction
         */
        DigestSrcAttributeRule(final List customActions,
                final PathResolver pr) {
            super();
            this.customActions = customActions;
            this.pr = pr;
        }

        /**
         * Constructor.
         *
         * @param root The root document, if this one is src'ed in
         * @param pr The PathResolver
         * @param customActions The list of custom actions this digester needs
         * to be able to process
         *
         * @see PathResolver
         * @see CustomAction
         */
        DigestSrcAttributeRule(final StateChart root,
                final List customActions, final PathResolver pr) {
            super();
            this.root = root;
            this.customActions = customActions;
            this.pr = pr;
        }

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) throws ModelException {
            String src = attributes.getValue("src");
            if (StateFlowHelper.isStringEmpty(src)) {
                return;
            }

            // 1) Digest the external SCXML file
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            Digester digester = getDigester();
            StateChart scxml = (StateChart) digester.peek(digester.getCount() - 1);
            StateChart parent = root;
            if (parent == null) {
                parent = scxml;
            }
            String path;
            PathResolver nextpr = null;
            if (pr == null) {
                path = src;
            } else {
                path = pr.resolvePath(src);
                nextpr = pr.getResolver(src);
            }
            String[] fragments = path.split("#", 2);
            String location = fragments[0];
            String fragment = null;
            if (fragments.length > 1) {
                fragment = fragments[1];
            }
            Digester externalSrcDigester;
            if (fragment != null) {
                // Cannot pull in all targets just yet, i.e. null parent
                externalSrcDigester = newInstance(null, nextpr,
                        customActions);
            } else {
                externalSrcDigester = newInstance(parent, nextpr,
                        customActions);
            }
            StateChart externalSCXML = null;
            try {
                externalSCXML = (StateChart) externalSrcDigester.parse(location);
            } catch (IOException | SAXException e) {
                MessageFormat msgFormat
                        = new MessageFormat(ERR_STATE_SRC);
                String errMsg = msgFormat.format(new Object[]{
                    path
                });
                throw new ModelException(errMsg + " : " + e.getMessage(), e);
            }

            // 2) Adopt the children and datamodel
            if (externalSCXML == null) {
                return;
            }
            State s = (State) digester.peek();
            if (fragment == null) {
                // All targets pulled in since its not a src fragment
                Initial ini = new Initial();
                Transition t = new Transition();
                t.setNext(externalSCXML.getInitial());
                ini.setTransition(t);
                s.setInitial(ini);
                Map children = externalSCXML.getChildren();
                Iterator childIter = children.values().iterator();
                while (childIter.hasNext()) {
                    s.addChild((TransitionTarget) childIter.next());
                }
                s.setDatamodel(externalSCXML.getDatamodel());
            } else {
                // Need to pull in descendent targets
                Object source = externalSCXML.getTargets().get(fragment);
                if (source == null) {
                    MessageFormat msgFormat
                            = new MessageFormat(ERR_STATE_SRC_FRAGMENT);
                    String errMsg = msgFormat.format(new Object[]{
                        path
                    });
                    throw new ModelException(errMsg);
                }
                if (source instanceof State) {
                    State include = (State) source;
                    s.setOnEntry(include.getOnEntry());
                    s.setOnExit(include.getOnExit());
                    s.setDatamodel(include.getDatamodel());
                    List histories = include.getHistory();
                    for (int i = 0; i < histories.size(); i++) {
                        History h = (History) histories.get(i);
                        s.addHistory(h);
                        parent.addTarget(h);
                    }
                    Iterator childIter = include.getChildren().values().iterator();
                    while (childIter.hasNext()) {
                        TransitionTarget tt = (TransitionTarget) childIter.next();
                        s.addChild(tt);
                        parent.addTarget(tt);
                        addTargets(parent, tt);
                    }
                    s.setInvoke(include.getInvoke());
                    s.setFinal(include.isFinal());
                    if (include.getInitial() != null) {
                        s.setInitial(include.getInitial());
                    }
                    Iterator transIter = include.getTransitionsList().iterator();
                    while (transIter.hasNext()) {
                        s.addTransition((Transition) transIter.next());
                    }
                } else {
                    MessageFormat msgFormat
                            = new MessageFormat(ERR_STATE_SRC_FRAGMENT_TARGET);
                    String errMsg = msgFormat.format(new Object[]{
                        path
                    });
                    throw new ModelException(errMsg);
                }
            }
        }

        /**
         * Add all the nested targets from given target to given parent state
         * machine.
         *
         * @param parent The state machine
         * @param tt The transition target to import
         */
        private static void addTargets(final StateChart parent, final TransitionTarget tt) {
            for (History history : tt.getHistory()) {
                parent.addTarget(history);
            }
            if (tt instanceof State) {
                Iterator childIter = ((State) tt).getChildren().values().iterator();
                while (childIter.hasNext()) {
                    TransitionTarget child = (TransitionTarget) childIter.next();
                    parent.addTarget(child);
                    addTargets(parent, child);
                }
            } else if (tt instanceof Parallel) {
                Iterator childIter = ((Parallel) tt).getChildren().iterator();
                while (childIter.hasNext()) {
                    TransitionTarget child = (TransitionTarget) childIter.next();
                    parent.addTarget(child);
                    addTargets(parent, child);
                }
            }
        }
    }

    /**
     * Custom digestion rule for setting PathResolver for runtime retrieval.
     */
    private static class SetPathResolverRule extends Rule {

        /**
         * The PathResolver to set.
         *
         * @see PathResolver
         */
        private final PathResolver pr;

        /**
         * Constructor.
         *
         * @param pr The PathResolver
         *
         * @see PathResolver
         */
        SetPathResolverRule(final PathResolver pr) {
            super();
            this.pr = pr;
        }

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) {
            PathResolverHolder prHolder = (PathResolverHolder) getDigester().
                    peek();
            prHolder.setPathResolver(pr);
        }
    }

    /**
     * Custom digestion rule for setting state parent of finalize.
     */
    private static class UpdateFinalizeRule extends Rule {

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) {
            Finalize finalize = (Finalize) getDigester().peek();
            // state/invoke/finalize --> peek(2)
            TransitionTarget tt = (TransitionTarget) getDigester().peek(2);
            finalize.setParent(tt);
        }
    }

    /**
     * Custom digestion rule for attaching a snapshot of current namespaces to
     * SCXML actions for deferred XPath evaluation.
     */
    private static class SetCurrentNamespacesRule extends Rule {

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) {
            NamespacePrefixesHolder nsHolder
                    = (NamespacePrefixesHolder) getDigester().peek();
            nsHolder.setNamespaces(getDigester().getCurrentNamespaces());
        }
    }

    /**
     * Custom digestion rule for attaching a snapshot of current namespaces to
     * SCXML actions for deferred XPath evaluation.
     */
    private static class SetCurrentFunctionMapperRule extends Rule {

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name, final Attributes attributes) {
            
            NamespacePrefixesHolder nsHolder = (NamespacePrefixesHolder) getDigester().peek();
            nsHolder.setNamespaces(getDigester().getCurrentNamespaces());
        }
    }
    
    
    /**
     * Custom digestion rule logging ignored elements.
     */
    private static class IgnoredElementRule extends Rule {

        /**
         * @see Rule#begin(String, String, Attributes)
         */
        @Override
        public final void begin(final String namespace, final String name,
                final Attributes attributes) {
            org.apache.commons.logging.Log log = LogFactory.
                    getLog(SCXMLParser.class);
            Locator l = digester.getDocumentLocator();
            String identifier = l.getSystemId();
            if (identifier == null) {
                identifier = l.getPublicId();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Ignoring element <").append(name).
                    append("> in namespace \"").append(namespace).
                    append("\" at ").append(identifier).append(":").
                    append(l.getLineNumber()).append(":").
                    append(l.getColumnNumber()).append(" and digester match \"").
                    append(digester.getMatch()).append("\"");
            log.warn(sb.toString());
        }
    }

}
