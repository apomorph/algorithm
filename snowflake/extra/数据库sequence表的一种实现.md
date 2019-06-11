由于通过一张sequence表生成业务主键

假设业务主键的类型是bigint 

而实际几乎所有数据的业务主键都通过它来生成 

每次生成一个业务主键会导致sequence表的自增主键+1

会导致sequence表本身的mysql自增主键增长迅速 

在大量数据的情况下会超出bigint的最大范围


问题：MySQL使用on duplicate key update时导致主键不连续自增
	  insert into.. on deplicate update 和replace into语句都会导致auto_increment+1 不管是否有没有真的执行insert操作
	  
show variables like '%innodb_autoinc_lock_mode%';

解决方案：

	1.先查询 再更新
		这种方式需要处理并发问题
	
	2.先更新 更新失败则插入
		如果是mybatis 通过update语句的返回值来判断更新是否成功可能不准 这种情况应该没问题

	2.修改数据库默认的自增锁模式(不推荐)
	修改mysql的配置innodb_autoinc_lock_mode值为0 即tradition模式
	但是这样会影响批量插入和并发插入的性能 
	因为在这个模式下 所有的insert语句都要在语句开始的时候获得一个表级的auto_inc锁，在语句结束时才释放锁
	这个锁是语句级的 不是事务级的 一个事务中可能包含多个insert语句(批量插入) 
	
	默认的innodb_autoinc_lock_mode值为1，即consecutive模式
	这个模式下 对于simple insert进行了优化
	由于simple insert一次性插入数据的条数可以确定 因此可以一次性生成几个连续的自增主键的值
	好处是auto_lock锁不用一直保持到语句结束 只要语句得到了响应的值之后就可以提前释放锁
	当然 这种情况下的自增值可能不是完全连续的 一般没有影响