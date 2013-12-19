/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.cphbusiness.group11.manualTest_MesaagingBank;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 *
 * @author Anth
 */
public class ManualTest_MessagingBank {
    
    private static final String QUEUE_NAME = "translator.group11.cphbusiness.messagingBank" ;
	private static final String REPLY_QUEUE_NAME = "normalizer.group11";

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

		AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder();
		propertiesBuilder.replyTo(REPLY_QUEUE_NAME);
		AMQP.BasicProperties properties = propertiesBuilder.build();

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
