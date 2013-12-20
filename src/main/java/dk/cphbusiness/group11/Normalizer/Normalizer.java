package dk.cphbusiness.group11.Normalizer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import dk.cphbusiness.group11.Normalizer.Banks.NormalizerBankJSON;
import dk.cphbusiness.group11.Normalizer.Banks.NormalizerBankXML;
import dk.cphbusiness.group11.Normalizer.Banks.NormalizerMessagingBank;
import dk.cphbusiness.group11.Normalizer.Banks.NormalizerWebServiceBank;

public abstract class Normalizer extends Thread {

	private static final String JSON_BANK_QUEUE = "normalizer.group11.bankJSON";
	private static final String XML_BANK_QUEUE = "normalizer.group11.bankXML";
	private static final String MESSAGING_BANK_QUEUE = "normalizer.group11.messagingBank";
	private static final String WEB_SERVICE_BANK_QUEUE = "normalizer.group11.webServiceBank";

	private static final String AGGREGATOR_QUEUE = "aggregator.group11";

	private QueueingConsumer.Delivery delivery;
	private QueueingConsumer consumer;

	public Normalizer(String queue) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("datdb.cphbusiness.dk");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

                channel.exchangeDeclare(queue, "fanout");
                String queueName = channel.queueDeclare(queue,false,  false, false, null).getQueue();
                channel.queueBind(queueName, queue, "");

		System.out.println(" [*] Waiting for messages on queue: " + queue);

		consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueName, true, consumer);

	}

	public static void main(String[] argv) throws Exception {
		NormalizerBankJSON bankJsonNormalizer = new NormalizerBankJSON(
				JSON_BANK_QUEUE);
		bankJsonNormalizer.start();
		NormalizerBankXML bankXmlNormalizer = new NormalizerBankXML(
				XML_BANK_QUEUE);
		bankXmlNormalizer.start();
		NormalizerMessagingBank bankMessagingNormalizer = new NormalizerMessagingBank(
				MESSAGING_BANK_QUEUE);
		bankMessagingNormalizer.start();
		NormalizerWebServiceBank bankWebServiceNormalizer = new NormalizerWebServiceBank(
				WEB_SERVICE_BANK_QUEUE);
		bankWebServiceNormalizer.start();

	}

	protected abstract String processMessage(String message) throws Exception;

	private void sendReply(String message) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("datdb.cphbusiness.dk");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.basicPublish("", AGGREGATOR_QUEUE, null, message.getBytes());

		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();
	}

	public void run() {
		while (true) {
			try {
				delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());

				System.out.println(" [x] Received '" + message + "'");

				// process message
				String reply = processMessage(message);

				sendReply(reply);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
