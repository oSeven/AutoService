package com.os.auto.service.processor

/**
 *
 * Created on 2018/8/27.
 *
 * @author o.s
 */
class HashMultiMap<K, V> {

    private val hashMap by lazy { HashMap<K, MutableCollection<V>>() }

    fun keySet(): Set<K> {
        val set = HashSet<K>()
        hashMap.forEach {
            set.add(it.key)
        }
        return set
    }

    fun put(key: K, value: V) {
        if (hashMap[key] == null) {
            hashMap[key] = HashSet()
        }
        hashMap[key]?.add(value)
    }

    fun get(key: K): MutableCollection<V>? {
        return hashMap[key]
    }

}