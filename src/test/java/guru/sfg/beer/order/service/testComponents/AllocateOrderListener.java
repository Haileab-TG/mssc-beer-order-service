package guru.sfg.beer.order.service.testComponents;

import common.event.AllocateOrderResultEvent;
import common.model.BeerOrderDto;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocateOrderListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_REQUEST_QUEUE)
    public void listner(AllocateOrderResultEvent event){
        BeerOrderDto beerOrderDto = event.getBeerOrderDto();
        beerOrderDto.getBeerOrderLines().forEach(  //mocking full allocation
                line -> line.setQuantityAllocated(line.getOrderQuantity())
        );
        jmsTemplate.convertAndSend(
                JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResultEvent.builder()
                        .allocationError(false)
                        .pendingInventory(false)
                        .beerOrderDto(beerOrderDto)
                        .build()
                );
        log.debug("TEST COMPONENT: Allocate Order approved Result MQ sent");
    }
}
