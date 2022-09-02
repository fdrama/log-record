package com.simple.log.function.diff;

import de.danielbechler.diff.node.DiffNode;

/**
 * @author muzhantong
 * create on 2022/1/3 8:26 下午
 */
public interface IDiffItemsToLogContentService {

    /**
     * 对象比对生成日志内容
     *
     * @param diffNode diffNode
     * @param o1       source
     * @param o2       new
     * @return
     */
    String toLogContent(DiffNode diffNode, final Object o1, final Object o2);
}
