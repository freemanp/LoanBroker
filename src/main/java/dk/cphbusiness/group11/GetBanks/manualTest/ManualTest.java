/*
 * REMEMBER TO LAUNCH GetBanks.java before
 */
package dk.cphbusiness.group11.GetBanks.manualTest;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class ManualTest {

	private static final String QUEUE_NAME = "translator.group11.cphbusiness.messagingBank" ;//"group11.GetBanks";
	private static final String REPLY_QUEUE_NAME = "normalizer.group11";//"group11.GetBanksReply";

	public static void main(String[] argv) throws Exception {
		send();
		receive();
	}

	private static void receive() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("datdb.cphbusiness.dk");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		String queueName = channel.queueDeclare(REPLY_QUEUE_NAME, false, false,
				false, null).getQueue();
		System.out.println(" [*] Waiting for the message");

		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueName, true, consumer);
		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		String message = new String(delivery.getBody());

		System.out.println(" [x] Received '" + message + "'");
	}

	private static void send() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("datdb.cphbusiness.dk");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);

		Builder propertiesBuilder = new BasicProperties.Builder();
		propertiesBuilder.replyTo(REPLY_QUEUE_NAME);
		BasicProperties properties = propertiesBuilder.build();

		String message = "<LoanRequest><ssn>1234567890</ssn>"
				+ "<creditScore>585</creditScore>"
				+ "<loanAmount>1000</loanAmount><loanDuration>36</loanDuration>"
				+ "</LoanRequest>";

		channel.basicPublish("", QUEUE_NAME, properties, message.getBytes());
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();
	}
}
