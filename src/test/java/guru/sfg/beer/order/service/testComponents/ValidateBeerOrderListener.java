package guru.sfg.beer.order.service.testComponents;

import common.event.ValidateOrderRequestEvent;
import common.event.ValidateOrderResultEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateBeerOrderListener {
    private final JmsTemplate jmsClient;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_REQUEST_QUEUE)
    public void listener(ValidateOrderRequestEvent event){
        ValidateOrderResultEvent response = ValidateOrderResultEvent.builder()
                .isValid(true)
                .orderId(event.getBeerOrderDto().getId())
                .build();

        jmsClient.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE, response);
    }
}
