package dk.cphbusiness.group11.Normalizer.Banks;
/*
 * expects:
 * 	{"interestRate":5.5,"ssn":1605789787}
 * returns:
 * 	<LoanResponse>
 *		<interestRate>5.5</interestRate>
 *		<ssn>1605789787</ssn>
 *		<bank>cphbusiness.bankJSON</bank>
 *	</LoanResponse>
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

import dk.cphbusiness.group11.Normalizer.Normalizer;

public class NormalizerBankJSON extends Normalizer {

	private static final String SSN_FIELD = "ssn";
	private static final String INTEREST_RATE_FIELD = "interestRate";
	private static final String BANK_FIELD = "bank";
	private static final String XML_ROOT_ELEMENT = "LoanResponse";
	
	private static final String BANK = "cphbusiness.bankJSON";

	public NormalizerBankJSON(String queue) throws Exception {
		super(queue);
	}

	@Override
	protected String processMessage(String message) throws Exception {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				message.getBytes());
		JsonReader jsonReader = Json.createReader(inputStream);
		JsonObject jsonObject = jsonReader.readObject();

		jsonReader.close();
		inputStream.close();

		String ssn = Integer.toString(jsonObject.getJsonNumber(SSN_FIELD).intValue());
		
		String interestRate = Double.toString(jsonObject.getJsonNumber(INTEREST_RATE_FIELD).doubleValue());

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		XMLEventWriter eventWriter = outputFactory
				.createXMLEventWriter(outputStream);
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();

		XMLEvent endLine = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");

		eventWriter.add(eventFactory.createStartDocument());

		eventWriter.add(endLine);
		eventWriter.add(eventFactory.createStartElement("", "",
				XML_ROOT_ELEMENT));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				INTEREST_RATE_FIELD));
		eventWriter.add(eventFactory.createCharacters(interestRate));
		eventWriter.add(eventFactory.createEndElement("", "",
				INTEREST_RATE_FIELD));
		eventWriter.add(endLine);
		
		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "",
				BANK_FIELD));
		eventWriter.add(eventFactory.createCharacters(BANK));
		eventWriter.add(eventFactory.createEndElement("", "",
				BANK_FIELD));
		eventWriter.add(endLine);

		eventWriter.add(tab);
		eventWriter.add(eventFactory.createStartElement("", "", SSN_FIELD));
		eventWriter.add(eventFactory.createCharacters(ssn));
		eventWriter.add(eventFactory.createEndElement("", "", SSN_FIELD));
		eventWriter.add(endLine);

		eventWriter
				.add(eventFactory.createEndElement("", "", XML_ROOT_ELEMENT));
		eventWriter.add(endLine);
		eventWriter.add(eventFactory.createEndDocument());
		eventWriter.close();

		return outputStream.toString();
	}
}
