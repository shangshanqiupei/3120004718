
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;

public class SimHash
{
    private final String tokens;

    private final BigInteger intSimHash;

    String strSimHash;

    private int hashbits = 64;

    public SimHash(String tokens) throws IOException
    {
        this.tokens = tokens;
        this.intSimHash = this.simHash();
    }

    public SimHash(String tokens, int hashbits) throws IOException
    {
        this.tokens = tokens;
        this.hashbits = hashbits;
        this.intSimHash = this.simHash();
    }

    HashMap<String, Integer> wordMap = new HashMap<String, Integer>();

    public BigInteger simHash() throws IOException
    {
        // 定义特征向量/数组
        int[] v = new int[this.hashbits];

        String word = null;
        List<Term> terms = NLPTokenizer.segment(this.tokens);
        for (Term term : terms)
        {
            word = term.word;

            // 将每一个分词hash为一组固定长度的数列.比如 64bit 的一个整数.
            BigInteger t = this.hash(word);
            for (int i = 0; i < this.hashbits; i++)
            {
                BigInteger bitmask = new BigInteger("1").shiftLeft(i);

                // 建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
                // 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
                // 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
                if (t.and(bitmask).signum() != 0)
                {
                    // 这里是计算整个文档的所有特征的向量和
                    // 这里实际使用中需要 +- 权重，比如词频，而不是简单的 +1/-1，
                    v[i] += 1;
                }
                else
                {
                    v[i] -= 1;
                }
            }
        }

        BigInteger fingerprint = new BigInteger("0");
        StringBuffer simHashBuffer = new StringBuffer();
        for (int i = 0; i < this.hashbits; i++)
        {
            // 4、最后对数组进行判断,大于0的记为1,小于等于0的记为0,得到一个 64bit 的数字指纹/签名.
            if (v[i] >= 0)
            {
                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
                simHashBuffer.append("1");
            }
            else
            {
                simHashBuffer.append("0");
            }
        }
        this.strSimHash = simHashBuffer.toString();
        return fingerprint;
    }

    private BigInteger hash(String source)
    {
        if (source == null || source.length() == 0)
        {
            return new BigInteger("0");
        }
        else
        {
            char[] sourceArray = source.toCharArray();
            BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
            BigInteger m = new BigInteger("1000003");
            BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(
                    new BigInteger("1"));
            for (char item : sourceArray)
            {
                BigInteger temp = BigInteger.valueOf(item);
                x = x.multiply(m).xor(temp).and(mask);
            }
            x = x.xor(new BigInteger(String.valueOf(source.length())));
            if (x.equals(new BigInteger("-1")))
            {
                x = new BigInteger("-2");
            }
            return x;
        }
    }

    /**
     * 计算海明距离
     *
     * @param other
     *             被比较值
     * @return 海明距离
     */
    public int hammingDistance(SimHash other)
    {

        BigInteger x = this.intSimHash.xor(other.intSimHash);
        int tot = 0;

        while (x.signum() != 0)
        {
            tot += 1;
            x = x.and(x.subtract(new BigInteger("1")));
        }
        return tot;
    }

    public int getDistance(String str1, String str2)
    {
        int distance;
        if (str1.length() != str2.length())
        {
            distance = -1;
        }
        else
        {
            distance = 0;
            for (int i = 0; i < str1.length(); i++)
            {
                if (str1.charAt(i) != str2.charAt(i))
                {
                    distance++;
                }
            }
        }
        return distance;
    }

    public List<BigInteger> subByDistance(SimHash simHash, int distance)
    {
        // 分成几组来检查
        int numEach = this.hashbits / (distance + 1);
        List<BigInteger> characters = new ArrayList<BigInteger>();

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < this.intSimHash.bitLength(); i++)
        {
            // 当且仅当设置了指定的位时，返回 true
            boolean sr = simHash.intSimHash.testBit(i);

            if (sr)
            {
                buffer.append("1");
            }
            else
            {
                buffer.append("0");
            }

            if ((i + 1) % numEach == 0)
            {
                // 将二进制转为BigInteger
                BigInteger eachValue = new BigInteger(buffer.toString(), 2);
                buffer.delete(0, buffer.length());
                characters.add(eachValue);
            }
        }

        return characters;
    }

 /*   public static void main(String[] args) throws IOException
    {
        String s = "传统的 hash 算法只负责将原始内容尽量均匀随机地映射为一个签名值，"
                + "原理上相当于伪随机数产生算法。产生的两个签名，如果相等，说明原始内容在一定概 率 下是相等的；"
                + "如果不相等，除了说明原始内容不相等外，不再提供任何信息，因为即使原始内容只相差一个字节，"
                + "所产生的签名也很可能差别极大。从这个意义 上来 说，要设计一个 hash 算法，"
                + "对相似的内容产生的签名也相近，是更为艰难的任务，因为它的签名值除了提供原始内容是否相等的信息外，"
                + "还能额外提供不相等的 原始内容的差异程度的信息。";
        SimHash hash1 = new SimHash(s, 64);

        // 删除首句话，并加入两个干扰串
        s = "原理上相当于伪随机数产生算法。产生的两个签名，如果相等，说明原始内容在一定概 率 下是相等的；"
                + "如果不相等，除了说明原始内容不相等外，不再提供任何信息，因为即使原始内容只相差一个字节，"
                + "所产生的签名也很可能差别极大。从这个意义 上来 说，要设计一个 hash 算法，"
                + "对相似的内容产生的签名也相近，是更为艰难的任务，因为它的签名值除了提供原始内容是否相等的信息外，"
                + "干扰1还能额外提供不相等的 原始内容的差异程度的信息。";
        SimHash hash2 = new SimHash(s, 64);

        // 首句前添加一句话，并加入四个干扰串
        s = "imhash算法的输入是一个向量，输出是一个 f 位的签名值。为了陈述方便，"
                + "假设输入的是一个文档的特征集合，每个特征有一定的权重。"
                + "传统干扰4的 hash 算法只负责将原始内容尽量均匀随机地映射为一个签名值，"
                + "原理上这次差异有多大呢3相当于伪随机数产生算法。产生的两个签名，如果相等，"
                + "说明原始内容在一定概 率 下是相等的；如果不相等，除了说明原始内容不相等外，不再提供任何信息，"
                + "因为即使原始内容只相差一个字节，所产生的签名也很可能差别极大。从这个意义 上来 说，"
                + "要设计一个 hash 算法，对相似的内容产生的签名也相近，是更为艰难的任务，因为它的签名值除了提供原始"
                + "内容是否相等的信息外，干扰1还能额外提供不相等的 原始再来干扰2内容的差异程度的信息。";
        SimHash hash3 = new SimHash(s, 64);

        int dis12 = hash1.getDistance(hash1.strSimHash, hash2.strSimHash);
        System.out.println(hash1.strSimHash);
        System.out.println(hash2.strSimHash);
        System.out.println(dis12);
        System.out.println((100-dis12*100/64)+"%");

        System.out.println("============================================");

        int dis13 = hash1.getDistance(hash1.strSimHash, hash3.strSimHash);

        System.out.println(hash1.strSimHash);
        System.out.println(hash3.strSimHash);
        System.out.println(dis13);
        System.out.println((100-dis13*100/64)+"%");
    }*/
}
