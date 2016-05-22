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

package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;
import wessbas.commons.parser.SessionData;
import wessbas.commons.parser.UseCase;

public class HTTPProtocolLayerEFSMGenerator extends
		AbstractProtocolLayerEFSMGenerator {

	/* *************************** Global Variables *************************** */

	HashMap<String, HashSet<String>> parameterMap = new HashMap<String, HashSet<String>>();

	/* *************************** constructors *************************** */

	/**
	 * Constructor for a Protocol Layer EFSM with Http requests.
	 * 
	 * @param m4jdslFactory
	 *            instance for creating M4J-DSL model elements.
	 * @param idGenerator
	 *            instance for creating unique Protocol State IDs.
	 * @param requestIdGenerator
	 *            instance for creating unique request IDs.
	 */
	public HTTPProtocolLayerEFSMGenerator(final M4jdslFactory m4jdslFactory,
			final IdGenerator idGenerator,
			final IdGenerator requestIdGenerator,
			final ArrayList<SessionData> sessions) {
		super(m4jdslFactory, idGenerator, requestIdGenerator, sessions);
	}

	/* ************************** public methods ************************** */

	/**
	 * Creates a Protocol Layer EFSM.
	 * 
	 * @return the newly created Protocol Layer EFSM.
	 * 
	 * @throws GeneratorException
	 *             if any error during the generation process occurs.
	 */
	@Override
	public ProtocolLayerEFSM generateProtocolLayerEFSM(final String serviceName)
			throws GeneratorException {

		final ProtocolLayerEFSM protocolLayerEFSM = this
				.createEmptyProtocolLayerEFSM();

		final ProtocolExitState protocolExitState = protocolLayerEFSM
				.getExitState();

		Request request;

		// check if protocoll information are available.
		boolean generateProtocolInformation = true;
		if (this.sessions.get(0).getUseCases().get(0).getUri() == null) {
			generateProtocolInformation = false;
		}

		if (generateProtocolInformation) {

			ArrayList<UseCase> relatedUseCases = new ArrayList<UseCase>();
			String ip = "";
			int port = 0;
			String uri = "";
			String method = "";
			String encoding = "";
			String protocol = "";

			// get useCases for this serviceName
			for (SessionData sessionData : this.sessions) {
				for (UseCase useCase : sessionData.getUseCases()) {
					if (useCase.getName().equals(serviceName)) {
						relatedUseCases.add(useCase);
					}
				}
			}

			if (relatedUseCases.size() > 0) {
				// take the value form the first useCase
				ip = relatedUseCases.get(0).getIp();
				port = relatedUseCases.get(0).getPort();
				uri = relatedUseCases.get(0).getUri();
				method = relatedUseCases.get(0).getMethode();
				encoding = relatedUseCases.get(0).getEncoding();
				protocol = relatedUseCases.get(0).getProtocol()
						.equals("HTTP/1.1") ? "http" : "";
				initializeParameterMap(relatedUseCases);
			}

			String[][] requestParameter = new String[parameterMap.keySet()
					.size()][2];
			int i = 0;
			for (String key : parameterMap.keySet()) {
				HashSet<String> parameterValues = parameterMap.get(key);
				requestParameter[i][0] = key;
				requestParameter[i][1] = getValuesAsString(parameterValues, ";");
				i++;
			}

			// z.B.
			// http://localhost:8080/action-servlet/ActionServlet?action=sellInventory
			request = this.createRequest(
					AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_HTTP,
					new String[][] { // properties;
					{ "HTTPSampler.domain", ip },
							{ "HTTPSampler.port", Integer.toString(port) },
							{ "HTTPSampler.path", uri },
							{ "HTTPSampler.method", method },
							{ "HTTPSampler.encoding", encoding },
							{ "HTTPSampler.protocol", protocol } },
					requestParameter);

		} else {

			request = this.createRequest(
					AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_HTTP,
					new String[][] { // properties;
							{ "HTTPSampler.domain", "localhost" },
							{ "HTTPSampler.port", "8080" },
							{ "HTTPSampler.path",
									"action-servlet/ActionServlet" },
							{ "HTTPSampler.method", "GET" }, }, new String[][] { // parameters;
					{ "action", serviceName } });

		}

		String eId = request.getEId();
		eId = eId + " (" + serviceName + ")";
		request.setEId(eId);

		final ProtocolState protocolState = this.createProtocolState(request);
		final String guard = ""; // no SUT-specific guard available yet ...
		final String action = ""; // no SUT-specific action available yet ...
		final ProtocolTransition protocolTransition = this
				.createProtocolTransition(protocolExitState, guard, action);

		protocolState.getOutgoingTransitions().add(protocolTransition);
		protocolLayerEFSM.getProtocolStates().add(protocolState);
		protocolLayerEFSM.setInitialState(protocolState);

		parameterMap.clear();

		return protocolLayerEFSM;
	}

	/**
	 * @param parameterValues
	 * @param delimiter
	 * @return String
	 */
	private String getValuesAsString(final HashSet<String> parameterValues,
			final String delimiter) {
		String returnString = "";
		for (String value : parameterValues) {
			returnString += value.trim() + delimiter;
		}
		return returnString;
	}

	/**
	 * 
	 * http://stackoverflow.com/questions/13592236/parse-the-uri-string-into-
	 * name-value-collection-in-java.
	 * 
	 * @param queryString
	 * @return Map<String, List<String>>
	 * @throws UnsupportedEncodingException
	 */
	private static Map<String, List<String>> splitQuery(String queryString)
			throws UnsupportedEncodingException {

		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = queryString.split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? pair.substring(0, idx) : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? pair
					.substring(idx + 1) : null;
			query_pairs.get(key).add(value);
		}

		return query_pairs;
	}

	/**
	 * Init parameterMap.
	 * 
	 * @param relatedSessions
	 */
	private void initializeParameterMap(final ArrayList<UseCase> relatedUseCases) {
		for (UseCase useCase : relatedUseCases) {

			try {
				Map<String, List<String>> parameterRequest = splitQuery(useCase
						.getQueryString());
				for (String parameterName : parameterRequest.keySet()) {
					if (!parameterName.equals("<no-query-string>")) {
						List<String> parameterValues = parameterRequest
								.get(parameterName);
						for (String parameterValue : parameterValues) {
							addToParameters(parameterName, parameterValue);
						}
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add key value pairs to parameterMap.
	 * 
	 * @param key
	 * @param value
	 */
	private void addToParameters(String key, String value) {
		if (parameterMap.get(key) != null) {
			HashSet<String> valueString = parameterMap.get(key);
			valueString.add(value);
			parameterMap.put(key, valueString);
		} else {
			HashSet<String> valueString = new HashSet<String>();
			valueString.add(value);
			parameterMap.put(key, valueString);
		}
	}
}
