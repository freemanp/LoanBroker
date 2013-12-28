package dk.cphbusiness.group11.loanbroker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoanBrokerComponent{
    private String componentName;
    private Connection connection;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public LoanBrokerComponent() throws IOException{
        this("LoanBrokerComponent");
    }
    
    public LoanBrokerComponent(String componentName) throws IOException{
        this.componentName = componentName;
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        
        this.connection = factory.newConnection();
        
        log("initialized component");
    }
    
    public Channel getChannel(String queueName) throws IOException{
        Channel channel = this.connection.createChannel();
        channel.exchangeDeclare(queueName, "fanout");
        
        return channel;
    }
    
    public void log(String message){
        System.out.println(String.format("%s %s: %s", dateFormat.format(new Date()), this.componentName, message));
    }
    
    public void logException(Exception e){
        System.out.println("ERROR " + this.componentName + ": " + e.getMessage());
        e.printStackTrace();
    }
    
}