package dk.cphbusiness.group11.GetBanks;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class GetBanks {

	private static final String HOST_NAME = "datdb.cphbusiness.dk";
	private static final String QUEUE_NAME = "group11.GetBanks";

	private static Channel channel;
	private static QueueingConsumer consumer;

	public static void main(String[] args) throws Exception {
		init();

		while (true) {
			consume();
		}

	}

	private static void consume() throws Exception {
		QueueingConsumer.Delivery delivery = consumer.nextDelivery();

		BasicProperties props = delivery.getProperties();
		BasicProperties replyProps = new BasicProperties.Builder()
				.correlationId(props.getCorrelationId()).build();

		String message = new String(delivery.getBody());
		
		System.out.println(message);

		MessageProcessor messageProcessor = new MessageProcessor(message);
		messageProcessor.processMessage();

		String response = messageProcessor.getResponse();

		channel.basicPublish("", props.getReplyTo(), replyProps,
				response.getBytes());

		channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

	}

	private static void init() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(HOST_NAME);
		factory.setPort(5672);
		factory.setUsername("student");
		factory.setPassword("cph");

		Connection connection = factory.newConnection();
		channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);

		channel.basicQos(1);

		consumer = new QueueingConsumer(channel);

		channel.basicConsume(QUEUE_NAME, false, consumer);
	}

}
