package com.simple.log.function.diff;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.comparison.ComparisonService;
import de.danielbechler.diff.node.DiffNode;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * @author muzhantong
 */
@Slf4j
public class ObjectDifferUtils {

    private IDiffItemsToLogContentService diffItemsToLogContentService;

    public String diff(Object source, Object target) {
        if (source == null && target == null) {
            return StringUtils.EMPTY;
        }
        if (source == null || target == null) {
            try {
                Class<?> clazz = source == null ? target.getClass() : source.getClass();
                source = source == null ? clazz.getDeclaredConstructor().newInstance() : source;
                target = target == null ? clazz.getDeclaredConstructor().newInstance() : target;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                log.error("diff实例化对象失败, ", e);
                return StringUtils.EMPTY;
            }
        }
        if (!Objects.equals(source.getClass(), target.getClass())) {
            log.error("diff的两个对象类型不同, source.class={}, target.class={}", source.getClass().getName(), target.getClass().getName());
            return StringUtils.EMPTY;
        }
        ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
        DiffNode diffNode = objectDifferBuilder.differs()
                .register((differDispatcher, nodeQueryService) ->
                        new ArrayDiffer(differDispatcher, (ComparisonService) objectDifferBuilder.comparison(), objectDifferBuilder.identity()))
                .build()
                .compare(target, source);
        return diffItemsToLogContentService.toLogContent(diffNode, source, target);
    }

    public void setDiffItemsToLogContentService(IDiffItemsToLogContentService diffItemsToLogContentService) {
        this.diffItemsToLogContentService = diffItemsToLogContentService;
    }
}
