package guru.sfg.beer.order.service.testComponents;

import common.event.DeallocateCancelledOrderReqEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.Getter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ConfirmSentDeallocateCancellOrderReqEvevnt {
    private DeallocateCancelledOrderReqEvent deallocateCancelledOrderReqEvent;

    @JmsListener(destination = JmsConfig.CANCELLED_ORDER_DEALLOCATE_REQ_QUEUE)
    public void listener(DeallocateCancelledOrderReqEvent event){
        this.deallocateCancelledOrderReqEvent = event;

    }
}
