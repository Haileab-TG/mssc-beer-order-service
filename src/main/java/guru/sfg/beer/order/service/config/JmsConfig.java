package guru.sfg.beer.order.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@EnableJms
@Configuration
public class JmsConfig {
    public static final String VALIDATE_ORDER_REQ_QUEUE = "validate-order-request";
    public static final String VALIDATE_ORDER_RES_QUEUE = "validate-order-response";
    public static final String ALLOCATE_ORDER_REQ_QUEUE = "allocate-order-request";
    public static final String ALLOCATE_ORDER_RES_QUEUE = "allocate-order-response";
    public static final String FAILED_ALLOCATION_COMPEN_REQ_QUEUE = "failed-allocation-compensation-request";
    @Bean
    public MessageConverter messageConverter(){
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setTargetType(MessageType.TEXT);
        messageConverter.setTypeIdPropertyName("_type");
        return messageConverter;
    }
}
