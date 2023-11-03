package guru.sfg.beer.order.service.jmsConsumer;

import common.event.ValidateOrderResultEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ValidateOrderResponseListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listener(ValidateOrderResultEvent event){
        beerOrderManager.processValidationResult(event.getOrderId(), event.isValid());
    }
}
