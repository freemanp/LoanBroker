/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.cphbusiness.group11.BankMessaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

/**
 *
 * @author Anth
 */
public class MessagingBank {

    private static final String EXCHANGE_NAME = "group11.messagingBank";
    private static QueueingConsumer.Delivery delivery;

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        while (true) {
            delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println(" [x] Received '" + message + "'");

            //process message
            processMessage(message);
        }
    }

    private static void processMessage(String message) throws Exception {
        // read XML

        // build reply message
        String replyMessage = "";


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //get reply-to queue
        BasicProperties props = delivery.getProperties();
        BasicProperties replyProps = new BasicProperties.Builder()
                .correlationId(props.getCorrelationId()).build();

        channel.basicPublish("", props.getReplyTo(), replyProps, replyMessage.getBytes());
        
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }
}
