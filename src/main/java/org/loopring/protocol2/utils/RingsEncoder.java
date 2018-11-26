package org.loopring.protocol2.utils;

import org.loopring.protocol2.pojos.Rings;

public class RingsEncoder {

    private static final int SERIALIZATION_VERSION = 0;

    static class Bitstream {
        private String data;
        public String getData() {
            if (data.length() == 0) {
                return "0x0";
            } else {
                return "0x" + data;
            }
        }

        public int addBigNumber(Long num) {
            return addBigNumber(num, 32, true);
        }

        public int addBigNumber(Long num, int numBytes, boolean forceAppend) {
            String hexStr = Long.toHexString(num);
            int paddingLength = numBytes * 2 - hexStr.length();
            String padding = new String(new char[paddingLength]).replace("\0", "0");
            String hexTotal = padding + hexStr;
            return insert(hexTotal, forceAppend);
        }

        public int addNumber(int num) {
            return this.addNumber(num, 4, true);
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

    public String encode(Rings rings) {
        Bitstream stream = new Bitstream();
        int numSplendables = 6;
        stream.addNumber(SERIALIZATION_VERSION, 2, true);
        stream.addNumber(rings.orders.length, 2, true);
        stream.addNumber(rings.orderIndexesOfRings.length, 2, true);
        stream.addNumber(numSplendables, 2, true);

        stream.addHex(getTablesData(rings));
        for(int[] orderIndexes : rings.orderIndexesOfRings) {
            stream.addNumber(orderIndexes.length, 1, true);
            for (int orderIndex : orderIndexes) {
                stream.addNumber(orderIndex, 1, true);
            }
            stream.addNumber(0, 8 - orderIndexes.length, true);
        }
        stream.addNumber(0, 32, true);
        stream.addHex(getData(rings));

        return stream.getData();
    }

    public String getTablesData(Rings rings) {
        return "";

    }

    public String getData(Rings rings) {
        return "";
    }

}
