package guru.sfg.beer.order.service.jmsConsumer;

import common.event.AllocateOrderResultEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AllocateOrderResponseListener {
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RES_QUEUE)
    public void listener(AllocateOrderResultEvent event){
        beerOrderManager.processAllocationResult(
                event.getBeerOrderDto(),
                event.isPendingInventory(),
                event.isAllocationError()
        );
    }
}
