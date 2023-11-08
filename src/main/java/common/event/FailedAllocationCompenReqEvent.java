package common.event;


import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class FailedAllocationCompenReqEvent {
    private UUID beerOrderId;
}
