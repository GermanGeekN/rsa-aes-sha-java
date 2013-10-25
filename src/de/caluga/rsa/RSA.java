package de.caluga.rsa;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Stephan Bösebeck
 * Date: 24.09.13
 * Time: 23:38
 * <p/>
 * TODO: Add documentation here
 */
public class RSA {
    private BigInteger n, d, e;
    private int bitLen;

    private String id;

    public RSA() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RSA(int bitlen) {
        SecureRandom r = new SecureRandom();
        BigInteger p = new BigInteger(bitlen / 2, 100, r);
        BigInteger q = new BigInteger(bitlen / 2, 100, r);
        n = p.multiply(q);
        BigInteger m = (p.subtract(BigInteger.ONE))
                .multiply(q.subtract(BigInteger.ONE));
        while (true) {
            e = new BigInteger(bitlen, new SecureRandom());
            while (m.gcd(e).intValue() > 1) e = e.add(new BigInteger(bitlen, new SecureRandom()));
            try {
                d = e.modInverse(m);
                System.out.println("BitLength d: " + d.bitLength());
                break;
            } catch (Exception e) {
            }
        }
        this.bitLen = bitlen;
    }

    public int getBitLen() {
        return bitLen;
    }

    public void setBitLen(int bitLen) {
        this.bitLen = bitLen;
    }


    public RSA(BigInteger n, BigInteger d, BigInteger e, int bits) {
        this.n = n;
        this.d = d;
        this.e = e;
        this.bitLen = bits;
    }

    /**
     * encrypt using public key
     *
     * @param message
     * @return
     */
    public byte[] encrypt(byte[] message) {
        return encrypt(message, e, n);
    }

    private byte[] encrypt(byte[] message, BigInteger mp, BigInteger mod) {
        if (mp == null || mod == null) {
            throw new IllegalArgumentException("key not initialized");
        }
        List<BigInteger> bi = BigInteger.getIntegersOfBitLength(message, bitLen);
        List<Byte> ret = new ArrayList<Byte>();
        for (BigInteger b : bi) {
            BigInteger enc = crypt(b, mp, mod);
            byte[] encB = enc.bytes();
            for (byte eb : encB) {
                ret.add(eb);
            }
        }

        byte[] bytes = new byte[ret.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ret.get(i);
        }
        return bytes;
    }


    public byte[] decrypt(byte[] message) {
        return decrypt(message, d, n);

    }

    private byte[] decrypt(byte[] message, BigInteger mp, BigInteger mod) {
        List<BigInteger> bi = BigInteger.deSerializeInts(message);
        List<Byte> ret = new ArrayList<Byte>();
        List<BigInteger> decrypted = new ArrayList<BigInteger>();
        for (BigInteger toDec : bi) {
            BigInteger dec = crypt(toDec, mp, mod);
            decrypted.add(dec);
        }
        return BigInteger.dataFromBigIntArray(decrypted, bitLen - 4);
    }


    private BigInteger crypt(BigInteger message, BigInteger mp, BigInteger mod) {
        return message.modPow(mp, mod);
    }

    public BigInteger encrypt(BigInteger message) {
        return crypt(message, e, n);
    }

    public BigInteger decrypt(BigInteger message) {
        return crypt(message, d, n);
    }

    public byte[] getPrivateKey() {
        List<Byte> ret = new ArrayList<Byte>();
        for (byte b : BigInteger.valueOf(getBitLen()).bytes()) {
            ret.add(b);
        }
        for (byte b : n.bytes()) {
            ret.add(b);
        }
//        for (byte b : e.bytes()) {
//            ret.add(b);
//        }
        for (byte b : d.bytes()) {
            ret.add(b);
        }
        byte[] bytes = new byte[ret.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ret.get(i);
        }
        return bytes;

    }

    public void setPrivateKey(byte[] b) {
        List<BigInteger> lst = BigInteger.deSerializeInts(b);
        if (lst.size() != 3) {
            throw new IllegalArgumentException("Number of integers wrong");
        }
        if (bitLen != 0 && lst.get(0).intValue() != bitLen) {
            System.out.println("WARNING! bitlength mismatch!");
        }
        bitLen = lst.get(0).intValue();
        if (n != null && !n.equals(lst.get(1))) {
            throw new IllegalArgumentException("Key mismatch!");
        }
        n = lst.get(1);
        d = lst.get(2);
    }

    public void setPublicKey(byte[] b) {
        List<BigInteger> lst = BigInteger.deSerializeInts(b);
        if (lst.size() != 3) {
            throw new IllegalArgumentException("Number of integers wrong");
        }
        if (bitLen != 0 && lst.get(0).intValue() != bitLen) {
            System.out.println("WARNING! bitlength mismatch!");
        }
        bitLen = lst.get(0).intValue();
        if (n != null && !n.equals(lst.get(1))) {
            throw new IllegalArgumentException("Key mismatch!");
        }
        n = lst.get(1);
        e = lst.get(2);
    }

    public byte[] getPublicKey() {
        List<Byte> ret = new ArrayList<Byte>();
        for (byte b : BigInteger.valueOf(getBitLen()).bytes()) {
            ret.add(b);
        }
        for (byte b : n.bytes()) {
            ret.add(b);
        }
        for (byte b : e.bytes()) {
            ret.add(b);
        }
//        for (byte b : d.bytes()) {
//            ret.add(b);
//        }
        byte[] bytes = new byte[ret.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ret.get(i);
        }
        return bytes;

    }

    public boolean isValidSigned(byte[] signature, byte[] message) {
        byte[] sign = sign(message);
        if (sign.length != signature.length) {
            return false;
        }
        String signHex = Utils.getHex(sign);
        String signatureHex = Utils.getHex(signature);
        if (!signatureHex.equals(signHex)) {
            return false;
        }
        //decode with public key...
        byte[] decodedSignature = decrypt(signature, e, n);
        String md5 = new String(decodedSignature, Charset.forName("UTF8"));
        return md5.equals(Utils.getMd5String(message));
    }


    public byte[] sign(byte[] message) {
        String md5 = Utils.getMd5String(message);
        return encrypt(md5.getBytes(Charset.forName("UTF8")), d, n);
    }

    @Override
    public String toString() {
        return "RSA{ " +
                "id='" + id + "'" +
                "bitLen=" + bitLen +
                " n=" + n +
                ", d=" + d +
                ", e=" + e +
                '}';
    }

    public static RSA fromBytes(byte[] data) {
        List<BigInteger> lst = BigInteger.deSerializeInts(data);
        if (lst.size() != 4) {
            throw new IllegalArgumentException("byte array mismatch - " + lst.size());
        }
        BigInteger bitLen = lst.get(0);
        BigInteger n = lst.get(1);
        BigInteger e = lst.get(2);
        BigInteger d = lst.get(3);
        RSA ret = new RSA(n, d, e, bitLen.intValue());
        return ret;
    }


    public byte[] encrypt(String txt) {
        return encrypt(txt.getBytes(Charset.forName("UTF8")));
    }

    public byte[] bytes() {
        List<Byte> ret = new ArrayList<Byte>();
        for (byte b : BigInteger.valueOf(getBitLen()).bytes()) {
            ret.add(b);
        }
        for (byte b : n.bytes()) {
            ret.add(b);
        }
        for (byte b : e.bytes()) {
            ret.add(b);
        }
        for (byte b : d.bytes()) {
            ret.add(b);
        }
        byte[] bytes = new byte[ret.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ret.get(i);
        }
        return bytes;
    }


    public boolean hasPublicKey() {
        return n != null && e != null;
    }

    public boolean hasPrivateKey() {
        return n != null && d != null;
    }

}
