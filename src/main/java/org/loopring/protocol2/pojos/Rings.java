package org.loopring.protocol2.pojos;

import lombok.Builder;

@Builder
public class Rings {
    public String feeRecipient;
    public String transactionOrigin;
    public String miner;
    public String sig;
    public Order[] orders;
    public int[][] rings;
}
