package org.loopring.protocol2.utils;

import org.loopring.protocol2.pojos.Rings;
import org.loopring.protocol2.pojos.Order;

public class RingsEncoder {

    private static final int SERIALIZATION_VERSION = 0;
    private static final int ORDER_VERSION = 0;

    private String lrcAddress;

    public RingsEncoder(String lrcAddress) {
        this.lrcAddress = lrcAddress;
    }

    static class Bitstream {
        private String data;
        public String getData() {
            if (data == null || data.length() == 0) {
                return "0x0";
            } else {
                return "0x" + data;
            }
        }

        public int length() {
            return data.length() / 2;
        }

        public int addBigNumber(long num) {
            return addBigNumber(num, 32, true);
        }

        public int addBigNumber(long num, int numBytes, boolean forceAppend) {
            String hexStr = Long.toHexString(num);
            int paddingLength = numBytes * 2 - hexStr.length();
            String padding = new String(new char[paddingLength]).replace("\0", "0");
            String hexTotal = padding + hexStr;
            return insert(hexTotal, forceAppend);
        }

        public int addNumber(int num) {
            return this.addNumber(num, 4, true);
        }

        public int addNumber(int num, int numBytes) {
            return this.addNumber(num, numBytes, true);
        }

        public int addNumber(int num, int numBytes, boolean forceAppend) {
            String hex = Integer.toHexString(num);
            int hexLength = numBytes * 2;
            int paddingLength = hexLength - hex.length();
            String padding = new String(new char[paddingLength]).replace("\0", "0");
            String hexTotal = padding + hex;
            return addHex(hexTotal);
        }

        public int addHex(String hex) {
            return this.addHex(hex, true);
        }

        public int addHex(String hex, boolean forceAppend) {
            if (hex.startsWith("0x")) {
                return insert(hex.substring(2), forceAppend);
            } else {
                return insert(hex, forceAppend);
            }
        }

        public int addAddress(String addr, int numBytes, boolean forceAppend) {
            String addrPadded = padString(addr, numBytes * 2);
            return insert(addrPadded, forceAppend);
        }

        public int addRawBytes(String bsHex, boolean forceAppend) {
            return insert(bsHex.substring(2), forceAppend);
        }

        private String padString(String s, int targetLength) {
            if (s == null) s = "";

            if (s.length() > targetLength) {
                throw new IllegalArgumentException(s + " is too big to pad to target length:" +
                                                   targetLength);
            }

            while (s.length() < targetLength) {
                s = "0" + s;
            }
            return s;
        }

        private int insert(String s, boolean forceAppend) {
            int offset = data.length() / 2;
            if (!forceAppend) {
                int start = 0;
                while (start > 0) {
                    start = data.indexOf(s, start);
                    if (start >= 0) {
                        if (start % (2 * 4) == 0) {
                            return start / 2;
                        } else {
                            start ++;
                        }
                    }
                }
            }
            data += s;
            return offset;
        }
    }

    static class RingsSubmitParam {
        public int[][] ringSpecs;
        public Bitstream data;
        public Bitstream tables;

        public RingsSubmitParam() {
            data = new Bitstream();
            tables = new Bitstream();
        }
    }

    public String encode(Rings rings) {
        Bitstream stream = new Bitstream();
        int numSplendables = 6;
        RingsSubmitParam param = ringsToParam(rings);

        stream.addNumber(SERIALIZATION_VERSION, 2);
        stream.addNumber(rings.orders.length, 2);
        stream.addNumber(rings.rings.length, 2);
        stream.addNumber(numSplendables, 2);
        stream.addHex(param.tables.getData());
        for(int[] orderIndexes : rings.rings) {
            stream.addNumber(orderIndexes.length, 1);
            for (int orderIndex : orderIndexes) {
                stream.addNumber(orderIndex, 1);
            }
            stream.addNumber(0, 8 - orderIndexes.length);
        }
        stream.addNumber(0, 32);
        stream.addHex(param.data.getData());

        return stream.getData();
    }

    private RingsSubmitParam ringsToParam(Rings rings) {
        RingsSubmitParam param = new RingsSubmitParam();
        param.data.addNumber(0, 32);
        param.ringSpecs = rings.rings;
        createMiningTable(rings, param);

        for (Order order : rings.orders) {
            createOrderTable(order, param);
        }

        return param;
    }

    private boolean isEmptyOrNull(String s) {
        return s == null || s.length() == 0;
    }

    private void createMiningTable(Rings rings, RingsSubmitParam param) {
        String feeRecipient = rings.feeRecipient;
        if (isEmptyOrNull(feeRecipient)) {
            feeRecipient = rings.transactionOrigin;
        }

        if (!feeRecipient.equals(rings.transactionOrigin)) {
            insertOffset(param, param.data.addAddress(rings.feeRecipient, 20, false));
        } else {
            insertDefault(param);
        }

        String miner = rings.miner;
        if (isEmptyOrNull(miner)) {
            miner = feeRecipient;
        }
        if (!miner.equals(feeRecipient)) {
            insertOffset(param, param.data.addAddress(rings.miner, 20, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(rings.sig) && miner != rings.transactionOrigin) {
            this.insertOffset(param, param.data.addHex(createBytes(rings.sig), false));
            addPadding(param);
        } else {
            insertDefault(param);
        }
    }

    private void createOrderTable(Order order, RingsSubmitParam param) {
        addPadding(param);
        insertOffset(param, ORDER_VERSION);
        insertOffset(param, param.data.addAddress(order.owner, 20, false));
        insertOffset(param, param.data.addAddress(order.tokenS, 20, false));
        insertOffset(param, param.data.addAddress(order.tokenB, 20, false));
        insertOffset(param, param.data.addBigNumber(order.amountS, 32, false));
        insertOffset(param, param.data.addBigNumber(order.amountB, 32, false));
        insertOffset(param, param.data.addNumber(order.validSince, 4, false));
        param.tables.addNumber(order.tokenSpendableS.index, 2);
        param.tables.addNumber(order.tokenSpendableFee.index, 2);

        if (!isEmptyOrNull(order.dualAuthAddr)) {
            insertOffset(param, param.data.addAddress(order.dualAuthAddr, 20, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(order.broker)) {
            insertOffset(param, param.data.addAddress(order.broker, 20, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(order.orderInterceptor)) {
            insertOffset(param, param.data.addAddress(order.orderInterceptor, 20, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(order.walletAddr)) {
            insertOffset(param, param.data.addAddress(order.walletAddr, 20, false));
        } else {
            insertDefault(param);
        }

        if (order.validUtil > 0) {
            insertOffset(param, param.data.addNumber(order.validUtil, 4, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(order.sig)) {
            insertOffset(param, param.data.addHex(createBytes(order.sig), false));
            addPadding(param);
        } else {
            insertDefault(param);
        }
        if (!isEmptyOrNull(order.dualAuthSig)) {
            insertOffset(param, param.data.addHex(createBytes(order.dualAuthSig), false));
            addPadding(param);
        } else {
            insertDefault(param);
        }

        param.tables.addNumber(order.allOrNone? 1 : 0, 2);

        if (!isEmptyOrNull(order.feeToken) && !order.feeToken.equals(lrcAddress)) {
            insertOffset(param, param.data.addAddress(order.feeToken, 20, false));
        } else {
            insertDefault(param);
        }

        if (order.feeAmount > 0) {
            insertOffset(param, param.data.addBigNumber(order.feeAmount, 32, false));
        } else {
            insertDefault(param);
        }

        param.tables.addNumber(order.waiveFeePercentage, 2);
        param.tables.addNumber(order.tokenSFeePercentage, 2);
        param.tables.addNumber(order.tokenBFeePercentage, 2);

        if (!isEmptyOrNull(order.tokenRecipient) && !order.tokenRecipient.equals(order.owner)) {
            insertOffset(param, param.data.addAddress(order.tokenRecipient, 20, false));
        } else {
            insertDefault(param);
        }

        param.tables.addNumber(order.walletSplitPercentage, 2);
        param.tables.addNumber(order.tokenTypeS, 2);
        param.tables.addNumber(order.tokenTypeB, 2);
        param.tables.addNumber(order.tokenTypeFee, 2);

        if (!isEmptyOrNull(order.trancheS) && !isZeroAddress(order.trancheS)) {
            insertOffset(param, param.data.addHex(order.trancheS, false));
        } else {
            insertDefault(param);
        }
        if (!isEmptyOrNull(order.trancheB) && !isZeroAddress(order.trancheB)) {
            insertOffset(param, param.data.addHex(order.trancheB, false));
        } else {
            insertDefault(param);
        }

        if (!isEmptyOrNull(order.transferDataS) && !order.transferDataS.equals("0x")) {
            insertOffset(param, param.data.addHex(createBytes(order.transferDataS), false));
            addPadding(param);
        } else {
            insertDefault(param);
        }
    }

    private boolean isZeroAddress(String addr) {
        String zeroAddress = "0x" + new String(new char[64]).replace("\0", "0");
        return addr == null || addr.equals("0x0") || addr.equals(zeroAddress);
    }

    private void insertOffset(RingsSubmitParam param, int offset) {
        if (offset % 4 != 0) {
            throw new IllegalArgumentException("invalid offset:" + offset);
        }
        param.tables.addNumber(offset / 4, 2);
    }

    private void insertDefault(RingsSubmitParam param) {
        param.tables.addNumber(0, 2);
    }

    private void addPadding(RingsSubmitParam param) {
        int len = param.data.length();
        if (len % 4 != 0) {
            param.data.addNumber(0, 4 - (len % 4));
        }
    }

    private String createBytes(String data) {
        Bitstream bitstream = new Bitstream();
        bitstream.addNumber((data.length() - 2) / 2, 32);
        bitstream.addRawBytes(data, true);
        return bitstream.getData();
    }

}
