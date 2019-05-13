package main

import (
	"fmt"
	"sync"
	"time"
)


//SnowFlake算法生成id的结果是一个64bit大小的整数
//+--------------------------------------------------------------------------+
//| 1 Bit Unused | 41 Bit Timestamp |  10 Bit NodeID  |   12 Bit Sequence ID |
//+--------------------------------------------------------------------------+


// ID .
type ID int64

const (
	// 节点位数
	nodeBits uint8 = 10
	// 序列位数
	snBits uint8 = 12
	// 默认起始时间戳 2006-03-21 20:50:14 GMT
	epoch int64 = int64(1288834974657)
	// 时间位移
	timestampShift uint8 = snBits + nodeBits
	// 节点位移
	nodeShift uint8 = snBits
	// 节点最大值
	nodeMax int64 = -1 ^ (-1 << nodeBits)
	// 序列最大值
	snMax int64 = -1 ^ (-1 << snBits)
)

// IDWorker .
type IDWorker struct {
	// 并发锁
	mutex *sync.Mutex
	// 时间戳
	timestamp int64
	// 机器id
	node int64
	// 递增序列
	sn int64
}

// Next .
func (w *IDWorker) Next() (ID, error) {
	w.mutex.Lock()
	defer w.mutex.Unlock()
	if w.node < 0 || w.node > nodeMax {
		return 0, fmt.Errorf("node number must between 0 and %v", nodeMax)
	}
	// 获取当前时间戳
	now := timestamp()
	if now < w.timestamp {
		return 0, fmt.Errorf("clock is moving backwards")
	} else if w.timestamp == now {
		// 如果是同一时间则序列递增
		w.sn++
		if w.sn > snMax {
			// 如果序列使用完，等待下一时间戳
			for now < w.timestamp {
				now = timestamp()
			}
			// 进入下一时间戳序列初始化
			w.sn = 0
		}
	} else {
		// 不同时间戳序列初始化
		w.sn = 0
	}
	w.timestamp = now
	return ID((now-epoch)<<timestampShift | w.node<<nodeShift | w.sn), nil
}

func timestamp() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}

// NewIDWorker .
func NewIDWorker(node int64) *IDWorker {
	return &IDWorker{node: node, mutex: &sync.Mutex{}}
}

func main() {
	//fmt.Println("Hello World!")
	
}