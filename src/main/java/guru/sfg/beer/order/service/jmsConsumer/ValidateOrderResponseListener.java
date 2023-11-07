package guru.sfg.beer.order.service.jmsConsumer;

import common.event.ValidateOrderResultEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ValidateOrderResponseListener {

    private final BeerOrderManager beerOrderManager;
    int count = 0;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listener(ValidateOrderResultEvent event){
        UUID beerOrderId = event.getOrderId();
        boolean isValid = event.isValid();
        System.out.println("############################# " + beerOrderId + " \n" + isValid);
        System.out.println("Number listener called " + count++);
        beerOrderManager.processValidationResult(beerOrderId, isValid);
    }
}
