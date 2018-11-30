package org.loopring.protocol2.pojos;

import lombok.Builder;

@Builder
public class Order {
    public String owner;
    public String tokenS;
    public String tokenB;
    public long amountS;
    public long amountB;
    public int validSince;

    public Spendable tokenSpendableS;
    public Spendable tokenSpendableFee;

    public String dualAuthAddr;
    public String broker;
    public String orderInterceptor;
    public String walletAddr;
    public int validUtil;
    public String sig;
    public String dualAuthSig;
    public boolean allOrNone;
    public String feeToken;
    public long feeAmount;
    public int waiveFeePercentage;
    public int tokenSFeePercentage;
    public int tokenBFeePercentage;
    public String tokenRecipient;
    public int walletSplitPercentage;
    public int tokenTypeS;
    public int tokenTypeB;
    public int tokenTypeFee;
    public String trancheS;
    public String trancheB;
    public String transferDataS;

}
