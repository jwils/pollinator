package com.clover.push;

import java.security.SecureRandom;
import java.util.UUID;

public class Ids {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final long MAXLONG = Long.MAX_VALUE;

  // http://www.crockford.com/wrmg/base32.html
  private final static char[] BASE_32_DIGITS = {
      '0', '1', '2', '3', '4', '5',
      '6', '7', '8', '9', 'A', 'B',
      'C', 'D', 'E', 'F', 'G', 'H',
      'J', 'K', 'M', 'N', 'P', 'Q',
      'R', 'S', 'T', 'V', 'W', 'X',
      'Y', 'Z'
  };


  private final static int[] BASE_32_LOOKUP = {
      0, 1, 2, 3, 4, 5, 6, 7, // '0', '1', '2', '3', '4', '5', '6', '7'
      8, 9, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // '8', '9', ':', ';', '<', '=', '>', '?'
      0xFF, 10, 11, 12, 13, 14, 15, 16, // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
      17, 1, 18, 19, 1, 20, 21, 0, // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'
      22, 23, 24, 25, 26, 0xFF, 27, 28, // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W'
      29, 30, 31, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // 'X', 'Y', 'Z', '[', '\', ']', '^', '_'
      0xFF, 10, 11, 12, 13, 14, 15, 16, // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g'
      17, 1, 18, 19, 1, 20, 21, 0, // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o'
      22, 23, 24, 25, 26, 0xFF, 27, 28, // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w'
      29, 30, 31, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF  // 'x', 'y', 'z', '{', '|', '}', '~', 'DEL'
  };


  public static String nextBase32Id() {
    return encodeBase32(uuid64());
  }

  public static byte[] uuid64() {
    byte[] bytes = new byte[8];
    RANDOM.nextBytes(bytes);
    return bytes;
  }

  public static byte[] uuid128() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return bytes;
  }

  public static String toUUID(byte[] byteArray) {
    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++)
      msb = (msb << 8) | (byteArray[i] & 0xff);
    for (int i = 8; i < 16; i++)
      lsb = (lsb << 8) | (byteArray[i] & 0xff);
    UUID result = new UUID(msb, lsb);

    return result.toString();
  }

  public static byte[] fromUUID(String uuid) {
    uuid = normalizeDeviceId(uuid);

    if ((uuid.length() % 2) != 0)
      throw new IllegalArgumentException("Input string must contain an even number of characters");

    final byte result[] = new byte[uuid.length()/2];
    final char enc[] = uuid.toCharArray();
    for (int i = 0; i < enc.length; i += 2) {
      StringBuilder curr = new StringBuilder(2);
      curr.append(enc[i]).append(enc[i + 1]);
      result[i/2] = (byte) Integer.parseInt(curr.toString(), 16);
    }
    return result;
  }

  public static String encodeBase32(byte[] bytes) {
    if (bytes == null)
      return null;
    StringBuilder sb = new StringBuilder((bytes.length * 8) / 5);
    int i = 0, index = 0, digit;
    int curByte, nextByte;

    while (i < bytes.length) {
      curByte = unsignByte(bytes, i); // unsign

      if (index > 3) { // we need at least 5 bits per char
        if (i + 1 < bytes.length) {
          nextByte = unsignByte(bytes, i + 1);
        } else {
          nextByte = 0;
        }
        digit = curByte & (0xFF >> index);
        index = (index + 5) % 8;
        digit <<= index;
        digit |= nextByte >> (8 - index);
        i++;
      } else {
        digit = (curByte >> (8 - (index + 5))) & 0x1F;
        index = (index + 5) % 8;
        if (index == 0) i++;
      }
      sb.append(BASE_32_DIGITS[digit]);
    }
    return sb.toString();
  }

  private static int unsignByte(byte[] bytes, int i) {
    return bytes[i] & 0xFF;
  }


  public static byte[] decodeBase32(String str) {
    return decodeBase32(str, str.length() * 5 / 8);
  }

  public static byte[] decodeBase32(String str, int blength) {
    int i, index = 0, lookup, offset = 0, digit = 0;

    byte[] bytes = new byte[blength];
    for (int p = 0; p < bytes.length; p++) bytes[p] = 0;

    for (i = 0; i < str.length(); i++) {
      lookup = str.charAt(i) - '0';

      if (lookup < 0 || lookup >= BASE_32_LOOKUP.length) {
        continue;
      }

      digit = BASE_32_LOOKUP[lookup];

      if (lookup == 0xFF) {
        continue;
      }

      if (index <= 3) {
        index = (index + 5) % 8;
        if (index == 0) {
          bytes[offset] |= digit;
          offset++;
          if (offset >= blength) break;
        } else {
          bytes[offset] |= digit << (8 - index);
        }
      } else {
        index = (index + 5) % 8;
        bytes[offset] |= digit >>> index;
        offset++;

        if (offset >= blength) break;
        bytes[offset] |= digit << (8 - index);
      }
    }

    return bytes;
  }

  public static void main(String[] args) {
    String s = nextBase32Id();
    System.out.println("ID " + s);

    s = s.toLowerCase();
    s = s.replaceAll("1", "L").replace('0', 'o').replace('1', 'i');
    System.out.println("Updated val: " + s);

    byte[] data = decodeBase32(s);
    String x = encodeBase32(data);
    System.out.println(x);

    long l1 = testB32("D7M8F8KZB196P");
    long l2 = testB32("D");
    System.out.println("(l1 & l2) == l2:" + ((l1 & l2) == l2));
  }

  public static long testB32(String d) {
    System.out.println("---");
    byte[] a = Ids.decodeBase32(d, 8);
    final long l = toLong(a);
    System.out.println("l val:" + l + " binary str:" + Long.toBinaryString(l));
    return l;
  }

  private static long toLong(byte[] by) {
    long value = 0L;
    for (int i = 0; i < by.length; i++) {
      value += ((long) by[i] & 0xffL) << (8 * i);
    }
    return value;
  }

  public static String b32_64bitUidTo8DigitIntAsString(String base32_64bitUid) {
    if (base32_64bitUid == null || base32_64bitUid.length() < 2) {
      return "1";
    }
    byte[] bytes = decodeBase32(base32_64bitUid);
    return Long.toString(toLong(bytes) & 0x0000000005F5E0FF);  // Convert to something guaranteed to be 8 digits or less
  }

  public static long nextPositiveLong() {
    long val, bits;
    do { // reject some values to improve distribution
      bits = RANDOM.nextLong() & 0x7fffffffffffffffL;
      val = bits % MAXLONG;
    } while (bits - val + (MAXLONG - 1) < 0);
    return val;
  }

  public static String normalizeDeviceId(String deviceId) {
    return deviceId.replace("-", "");
  }
}
