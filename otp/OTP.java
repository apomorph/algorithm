import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

/**
 * 
     动态密码工具类
     
     动态码算法（2FA认证）:
 	1、生成一个32位由base64编码的共享密钥
 	2、获取当前时间戳(从1970-01-01 00:00:00开始的秒数)，因为是以30秒为一个计算周期，所以用时间戳除以30
 	3、使用HMAC-SHA1对信息进行加密(秘钥为key，时间戳为输入)
 	4、对HMAC-SHA1生成20个字节40个16进制数据进行瘦身，生成6位数字密码
	 	20个字节40个16进制数据进行瘦身算法：
	 	最后4个比特数用来做索引下标，然后取从索引开始的4个比特，分别移位组成一个32位的无符号整型，100000取模 生成6位无符号整型密码
 	
 */
public class OTP {
	
	public static void main(String[] args) {
		
		for (int i=0; i<100; i++) {
			String code = genCode("iu0wIcaFQ41Xz33fTZOMMcWBVYs7Rn9n");
			System.out.println("动态密码：" + LocalDateTime.now() + "========" + code);
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 生成动态密码
	 * @param str
	 * @return
	 */
	public static String genCode(String str) {

		byte[] bytes1 = str.getBytes();

		// 获取当前时间戳
		long time = System.currentTimeMillis();
		byte[] bytes = LongToBytes(time/1000/30);
		//System.out.println(Arrays.toString(bytes));

		// 使用HMAC-SHA1对信息进行加密(秘钥为key，时间戳为输入)
		byte[] hmac = hamcsha1(bytes, bytes1);
		//System.out.println(Arrays.toString(hmac));
		//String s = Base64.getEncoder().encodeToString(hmac);
		//System.out.println(s);

		// PLAN 1
		// 对HMAC-SHA1生成20个字节40个16进制数据进行瘦身，生成6位数字密码
		// (最后4个比特数用来做索引下标，然后取从索引开始的4个比特，分别移位组成一个32位的无符号整型，1000000取模 生成6位无符号整型密码)
		byte b = hmac[hmac.length - 1];
		int index = b & 0x0F;                 //获取最后一个字节的低四位比特       b & 00001111

		// 把四个字节拼接成一个整型 并且是无符号的
		int value = (int) ( ((hmac[index] & 0x7F) << 24) | ((hmac[index+1] & 0xFF) << 16) | ((hmac[index+2] & 0xFF) << 8) | (hmac[index+3] & 0xFF));   
		int result1 = value % 1000000;
		// 不足六位补零
		String s1 = StringUtils.leftPad(String.valueOf(result1), 6, "0");
		
		/*
		// PLAN 2
		// 选取最后一个字节的低字节位的 4 位, 将这 4 位的二进制值转换为无标点数的整数值，得到 0 到 15（包含 0 和 15）之间的一个数, 这个数字作为 20 个字节中从 0 开始的偏移量
		ByteBuffer buffer = ByteBuffer.allocate(8);
		int offset = hmac[hmac.length - 1] & 0x0f;     //偏移量
		// 从指定偏移位开始，连续截取 4 个字节（ 32 位），最后返回 32 位中的后面 31 位
		for (int i = 0; i < 4; i++) {
			buffer.put(i, hmac[i + offset]);
		}
		int hotp = buffer.getInt(0) & 0x7fffffff;
		int result2 = hotp % 1000000;
		String s2 = StringUtils.leftPad(String.valueOf(result2), 6, "0");
		*/
		
		return s1;
	}

	/**
	 * HmacSHA1摘要算法
	 */
	public static byte[] hamcsha1(byte[] key, byte[] data) {
		try {
			SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA1");     
			Mac mac = Mac.getInstance(secretKey.getAlgorithm());
			mac.init(secretKey);                                              
			return mac.doFinal(data);                                  
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * long转byte数组
	 * @param values
	 * @return
	 */
	public static byte[] LongToBytes(long values) {
		byte[] buffer = new byte[8];
		for (int i = 0; i < 8; i++) {
			int offset = 64 - (i + 1) * 8;
			buffer[i] = (byte) ((values >> offset) & 0xff);
		}
		return buffer;
	}

}
