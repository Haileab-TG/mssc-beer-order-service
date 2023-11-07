package guru.sfg.beer.order.service.stateMachine.action;

import common.event.AllocateOrderRequestEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class SendAllocateOrderRequestAction implements Action<OrderState, OrderEvent> {
    private final JmsTemplate jmsClient;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        System.out.println("############### 1 ##############");
        UUID orderIdOrNull = context.getMessageHeaders()
                .get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class);
        Optional.ofNullable(orderIdOrNull)
                        .ifPresentOrElse(orderId -> {
                            System.out.println("############### 2 ##############");
                            Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(orderId);
                            beerOrderOptional
                                    .ifPresentOrElse(beerOrder -> {
                                        System.out.println("############### 3 ##############");
                                        jmsClient.convertAndSend(
                                                JmsConfig.ALLOCATE_ORDER_REQUEST_QUEUE,
                                                AllocateOrderRequestEvent.builder()
                                                        .beerOrderDto(
                                                                beerOrderMapper.beerOrderToDto(beerOrder)
                                                        )
                                                        .build()
                                        );

                                        System.out.println("############### 4 ##############");
                                        log.debug("SendAllocateOrderRequestAction : Allocate Order MQ message sent");
                                    }, () -> log.error("SendAllocateOrderRequestAction : BeerOrder with this ID not found in DB"));
                        }, () -> log.error("SendAllocateOrderRequestAction : BeerOrderID not found in SM event msg"));

    }
}
