import java.util.Set;
import java.util.concurrent.*;

public class Sequence {

    // SnowFlake算法生成id的结果是一个64bit大小的整数
    // +--------------------------------------------------------------------------+
    // | 1 Bit Unused | 41 Bit Timestamp |  10 Bit NodeID  |   12 Bit Sequence ID |
    // +--------------------------------------------------------------------------+

    // 节点位数
    private final long nodeBits = 10L;
    // 序列位数
    private final long snBits = 12L;
    // 默认起始时间戳 2006-03-21 20:50:14 GMT
    private final long epoch = 1288834974657L;
    // 时间位移 22
    private long timestampShift = nodeBits + snBits;
    // 节点位移 12
    private long nodeShift = snBits;
    // 节点最大值 1023
    private long nodeMax = -1L ^ (-1L << nodeBits);
    // 序列最大值 4095
    private long snMax = -1L ^ (-1L << snBits);

    // 时间戳
    private long timestamp;
    // 机器id
    private long node;
    // 递增序列
    private long sn;

    public Sequence(long node) {
        this.node = node;
    }

    public synchronized long next() {
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
            return (now-epoch)<<timestampShift | node<<nodeShift | sn;
        }

    // 测试
    public static void main(String[] args) {

        Sequence sequence = new Sequence(0);

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
                long next = sequence.next();
                set.add(next);
                System.out.println("sequenceId: " + next);
            }
        }

        long startTime = System.currentTimeMillis();

        // 模拟10万并发
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
        System.out.println("执行耗时：" + ((endTime - startTime) / (1000 * 60)) + " min");

        if (set.size() != count) {
            System.out.println("失败! 有重复id");
            System.out.println("所有sequenceId个数：" + set.size());
        } else {
            System.out.println("成功");
        }

    }

}
