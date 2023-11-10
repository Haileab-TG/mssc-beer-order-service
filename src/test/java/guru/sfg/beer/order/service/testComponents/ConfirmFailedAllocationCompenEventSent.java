package guru.sfg.beer.order.service.testComponents;

import common.event.FailedAllocationCompenReqEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.Getter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ConfirmFailedAllocationCompenEventSent {
    private FailedAllocationCompenReqEvent failedAllocationCompenReqEvent;

    @JmsListener(destination = JmsConfig.FAILED_ALLOCATION_COMPEN_REQ_QUEUE)
    public void listener(FailedAllocationCompenReqEvent event){
        this.failedAllocationCompenReqEvent = event;
    }
}
