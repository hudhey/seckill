package com.ygh.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redis服务
 */
@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    /**
     * 从redis连接池获取redis实例<br>
     * 根据查询redis中存储的键对应的Java对象
     * @param prefix 前缀
     * @param key key
     * @param clazz 对象类型
     */
    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        //<T>表示这个方法是泛型方法，允许调用时指定具体类型
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //对key增加前缀，即可用于分类，也避免key重复
            String realKey = prefix.getPrefix() + key;
            String str = jedis.get(realKey);
//            T t = stringToBean(str, clazz);
//            return t;
            return stringToBean(str, clazz);
        } finally {
            returnToPool(jedis);
        }

    }

    /**
     * 存储对象
     */
    public <T> Boolean set(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() <= 0) {
                return false;
            }
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();//获取过期时间
            if (seconds <= 0) {
                jedis.set(realKey, str);
            } else {
                jedis.setex(realKey, seconds, str);
            }

            return true;
        } finally {
            returnToPool(jedis);
        }

    }

    /**
     * 删除
     */
    public boolean delete(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            long ret = jedis.del(realKey);
            return ret > 0;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 判断key是否存在
     */
    public <T> boolean exists(KeyPrefix prefix, String key) {
        //一致性：类中的其他方法是泛型方法，所有方法都可以使用相同的类型参数声明。
        //未来扩展：未来的版本可能扩展，使用T进行更复杂的操作，例如使用T进行类型检查或其他逻辑。
        //与其他方法的协作：与其他泛型方法一起使用时可能会需要T确保类型的一致性，尤其是在处理集合或其他数据结构时。
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 增加值
     * Redis Incr 命令将 key 中储存的数字值增一。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作
     */
    public <T> Long incr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 减少1
     * @param prefix 前缀
     * @param key 键
     */
    public <T> Long decr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }


    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == Integer.class || clazz == Long.class) {
            return String.valueOf(value);
        } else if (clazz == String.class) {
            return (String) value;
        } else {
            return JSON.toJSONString(value);
        }

    }

    /**
     * 把str转换成T类型的bean
     */
    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.isEmpty() || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else {
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
        }
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();//不是关闭，只是返回连接池
        }
    }

}
