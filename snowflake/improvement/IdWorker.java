import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.yxcg.site.common.id.constant.BizTagConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class IdWorker {

    // SnowFlake算法生成id的结果是一个64bit大小的整数
    // +----------------------------------------------------------------------+
    // | 9 Bit BizTagId | 40 Bit Timestamp | 5 Bit NodeID | 10 Bit SequenceID |
    // +----------------------------------------------------------------------+

    // 业务标识位数
    private final long bizBits = 9L;
    // 时间戳位数
    private final long timeBits = 40L;
    // 节点位数
    private final long nodeBits = 5L;
    // 序列位数
    private final long snBits = 10L;
    // 起始时间戳 2019-04-28 14:54:19 GMT
    private final long epoch = 1556434459241L;

    // 节点位移 10
    private long nodeShift = snBits;
    // 时间位移 15
    private long timestampShift = nodeBits + snBits;
    // 业务标识位移 55
    private long bizShift = timestampShift + timeBits;

    // 业务标识最大值 511
    private long bizMax = -1L ^ (-1L << bizBits);
    // 时间戳最大值
    private long timestampMax = -1L ^ (-1L << timeBits);
    // 序列最大值 1023
    private long snMax = -1L ^ (-1L << snBits);
    // 节点最大值 31
    private long nodeMax = -1L ^ (-1L << nodeBits);

    // 机器id
    private long node;
    // 上一次时间戳
    private long timestamp;
    // 上一次递增序列值
    private long sn;

    private static final Logger logger = LoggerFactory.getLogger(IdWorker.class);

    protected IdWorker() {
        super();
    }

    protected IdWorker(long node) {
        super();
        this.node = node;
    }

    // 生成id
    public synchronized long generate(long bizTag) {
        if (bizTag <= 0) {
            throw new RuntimeException(String.format("bizTag value must be greater than 0"));
        }
        if (node < 0 || node > nodeMax) {
            throw new RuntimeException(String.format("node can't be greater than %d or less than 0", nodeMax));
        }
        // 获取当前时间戳
        long now = System.currentTimeMillis();
        if (now < timestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", timestamp - now));
        } else if (timestamp == now) {
            // 如果是同一个时间则序列递增
            sn++;
            if (sn > snMax) {
                // 如果序列使用完，等待下一个时间戳
                while (now < timestamp) {
                    now = System.currentTimeMillis();
                }
                // 进入下一时间戳序列初始化
                sn = 0;
            }
        } else {
            // 不同时间戳序列初始化
            sn = 0;
        }
        timestamp = now;
        return bizTag<<bizShift | (now-epoch)<<timestampShift | node<<nodeShift | sn;
    }

    // 转换成id
    public long convert(long bizTag, long timestamp, long node, long sequence) {
        if (bizTag < 0 || bizTag > bizMax) {
            throw new RuntimeException("illegal bizTag");
        }
        if (timestamp - epoch < 0 || timestamp - epoch > timestampMax) {
            throw new RuntimeException("illegal timestamp");
        }
        if (node < 0 || node > nodeMax) {
            throw new RuntimeException("illegal node id");
        }
        if (sequence < 0 || sequence > snMax) {
            throw new RuntimeException("illegal sequence id");
        }

        return bizTag<<bizShift | (timestamp-epoch)<<timestampShift | node<<nodeShift | sequence;
    }

    // id反解
    public String reverse(long id) {
        if (id < 0) {
            throw new RuntimeException(String.format("id must be greater than 0"));
        }

        long bizTag = (id >>> bizShift) & bizMax;
        long timestamp = (id >>> timestampShift) & timestampMax;
        long node = (id >>> nodeShift) & nodeMax;
        long sequence = id & snMax;

        String bizTagName = null;

        //Class<BizTagConstant> clazz = BizTagConstant.class;
        //Field[] fields = clazz.getDeclaredFields();
        //if (fields != null && fields.length > 0) {
        //    for (Field field : fields) {
        //        boolean accessible = field.isAccessible();
        //        field.setAccessible(true);
        //        try {
        //            long val = field.getLong(clazz);
        //            field.setAccessible(accessible);
        //            if (bizTag == val) {
        //                bizTagName = field.getName();
        //                break;
        //            }
        //        } catch (IllegalAccessException e) {
        //            // e.printStackTrace();
        //        }
        //    }
        //}

        Map<String, Object> map = new HashMap<>();
        map.put("bizTag", bizTag);
        map.put("bizTagName", bizTagName);
        map.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timestamp + epoch)));
        map.put("node", node);
        map.put("sequence", sequence);

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = null;
        try {
            jsonStr = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.debug("json serizalize error");
            // e.printStackTrace();
        }
        return jsonStr;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 测试
    public static void main(String[] args) {

        testConvertAndReverse();
        // testConcurrency();

    }

    private static void testConvertAndReverse() {
        long timestamp = System.currentTimeMillis();
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(timestamp);
        System.out.println("current time: " + format);

        IdWorker idWorker1 = new IdWorker();
        long convert = idWorker1.convert(1L, timestamp, 9L, 1023L);
        System.out.println("generate id: " + convert);

        IdWorker idWorker2 = new IdWorker();
        String reverse = idWorker2.reverse(convert);
        System.out.println("reverse id: " + reverse);
    }

    private static boolean testConcurrency() {
        IdWorker idWorker = new IdWorker(0);

        Set<Long> set = new CopyOnWriteArraySet<>();  // HashSet不是线程安全的

        class MyRunnable implements Runnable{
            private CyclicBarrier cyclicBarrier;

            public MyRunnable(CyclicBarrier cyclicBarrier) {
                this.cyclicBarrier = cyclicBarrier;
            }

            @Override
            public void run() {
                try {
                    // 等待所有任务准备就绪
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                long next = idWorker.generate(1);
                set.add(next);
                System.out.println("Id: " + next);
            }
        }

        long startTime = System.currentTimeMillis();

        // 模拟10万并发
        // 需要等待所有线程准备完毕 非常耗时
        int count = 100000;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(count);
        ExecutorService executorService = Executors.newFixedThreadPool(count);
        for (int i = 0; i < count; i++) {
            executorService.execute(new MyRunnable(cyclicBarrier));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Execute time：" + ((endTime - startTime) / (1000 * 60)) + " min");

        if (set.size() != count) {
            System.out.println("FAILURE!!! There are duplicate ids");
            System.out.println("Count of unrepetable ids: " + set.size());
            return false;
        } else {
            System.out.println("SUCCESS!!!");
            return true;
        }
    }

}
