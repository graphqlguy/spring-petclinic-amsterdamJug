package org.springframework.samples.petclinic.europepmc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * Calls the SOAP web service of Europe PMC. One call is one SOAP request: the operation
 * is the element in the body, and each argument is a child element of the same name. The
 * answer comes back as the <code>return</code> element of the response, read into a map
 * that keeps the element names, so a GraphQL field reads the element of the same name.
 * <p>
 * The WSDL declares 11 operations. This client writes the request by hand and reads the
 * answer with DOM.
 */
@Component
class EuropePmcSoapClient {

	static final String EUROPE_PMC = "The SOAP web service of Europe PMC";

	private static final Log logger = LogFactory.getLog(EuropePmcSoapClient.class);

	private static final String NAMESPACE = "http://webservice.cdb.ebi.ac.uk/";

	/**
	 * The elements the WSDL declares with <code>maxOccurs="unbounded"</code>. Each one
	 * becomes a list, also when an answer holds it once.
	 */
	private static final Set<String> REPEATED = Set.of("result", "citation", "reference", "fullTextId");

	private final WebServiceTemplate soap;

	private final String url;

	EuropePmcSoapClient(WebServiceTemplateBuilder builder, @Value("${petclinic.upstream.europepmc.url}") String url,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		this.url = url;
		HttpClientSettings timeouts = HttpClientSettings.defaults()
			.withConnectTimeout(connectTimeout)
			.withReadTimeout(readTimeout);
		this.soap = builder.httpMessageSenderFactory(WebServiceMessageSenderFactory.http(timeouts)).build();
	}

	/**
	 * Calls one operation and returns its <code>return</code> element as a map.
	 * @param operation the name of the operation in the WSDL, for example
	 * <code>searchPublications</code>
	 * @param arguments the child elements of the request, in the order of the WSDL. A
	 * null value leaves its element out
	 */
	Map<String, Object> call(String operation, Map<String, Object> arguments) {
		DOMResult response = new DOMResult();
		long started = System.nanoTime();
		try {
			this.soap.sendSourceAndReceiveToResult(this.url, new DOMSource(request(operation, arguments)), response);
		}
		catch (SoapFaultClientException ex) {
			// Europe PMC answers a fault for a query it cannot run, with the reason
			// as its fault string
			throw new InvalidArgumentException(
					"%s answered a fault: %s".formatted(EUROPE_PMC, ex.getFaultStringOrReason().strip()));
		}
		catch (WebServiceException ex) {
			// covers timeouts and connection failures
			throw new UpstreamException(EUROPE_PMC, ex);
		}
		Element result = childElements(((Document) response.getNode()).getDocumentElement()).get(0);
		Map<String, Object> answer = elements(result);
		logger.info("SOAP %s at %s returned hitCount %s (%d ms)".formatted(operation, this.url, answer.get("hitCount"),
				Duration.ofNanos(System.nanoTime() - started).toMillis()));
		return answer;
	}

	/**
	 * Builds the payload: the operation in the namespace of the service, and one
	 * unqualified child element for each argument.
	 */
	private static Document request(String operation, Map<String, Object> arguments) {
		Document document = newDocument();
		Element request = document.createElementNS(NAMESPACE, "web:" + operation);
		document.appendChild(request);
		arguments.forEach((name, value) -> {
			if (value != null) {
				Element element = document.createElementNS(null, name);
				element.setTextContent(String.valueOf(value));
				request.appendChild(element);
			}
		});
		return document;
	}

	/**
	 * Reads the child elements of an element into a map keyed by their names. An element
	 * with children becomes a map, and any other element becomes its text.
	 */
	private static Map<String, Object> elements(Element parent) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (Element child : childElements(parent)) {
			String name = child.getLocalName();
			Object value = childElements(child).isEmpty() ? child.getTextContent() : elements(child);
			if (REPEATED.contains(name)) {
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>) map.computeIfAbsent(name, (key) -> new ArrayList<>());
				values.add(value);
			}
			else {
				map.put(name, value);
			}
		}
		return map;
	}

	private static List<Element> childElements(Node parent) {
		List<Element> children = new ArrayList<>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element) {
				children.add(element);
			}
		}
		return children;
	}

	private static Document newDocument() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			return factory.newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
