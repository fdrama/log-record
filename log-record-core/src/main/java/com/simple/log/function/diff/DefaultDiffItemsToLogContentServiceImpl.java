package com.simple.log.function.diff;

import com.google.common.collect.Lists;

import com.simple.log.annoation.DiffLogField;
import com.simple.log.function.IConvertFunctionService;

import de.danielbechler.diff.node.DiffNode;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author muzhantong
 */
@Slf4j
@Setter
@Getter
public class DefaultDiffItemsToLogContentServiceImpl implements IDiffItemsToLogContentService {
    private final LogRecordProperties logRecordProperties;

    private final IConvertFunctionService convertFunctionService;

    public DefaultDiffItemsToLogContentServiceImpl(LogRecordProperties logRecordProperties, IConvertFunctionService convertFunctionService) {
        this.logRecordProperties = logRecordProperties;
        this.convertFunctionService = convertFunctionService;
    }

    @Override
    public String toLogContent(DiffNode diffNode, final Object sourceObject, final Object targetObject) {
        if (!diffNode.hasChanges()) {
            return StringUtils.EMPTY;
        }
        StringBuilder stringBuilder = new StringBuilder();
        diffNode.visit((node, visit) -> {
            String logContent = generateAllFieldLog(sourceObject, targetObject, node);
            if (StringUtils.isNotEmpty(logContent)) {
                stringBuilder.append(logContent).append(logRecordProperties.getFieldSeparator());
            }
        });
        return stringBuilder.toString().replaceAll(logRecordProperties.getFieldSeparator().concat("$"), "");
    }

    private String generateAllFieldLog(Object sourceObject, Object targetObject, DiffNode node) {
        if (node.isRootNode()) {
            return StringUtils.EMPTY;
        }
        DiffLogField diffLogFieldAnnotation = node.getFieldAnnotation(DiffLogField.class);
        if (diffLogFieldAnnotation == null || node.getValueTypeInfo() != null) {
            //自定义对象类型直接进入对象里面, diff
            return StringUtils.EMPTY;
        }
        // 是否是容器类型的字段
        boolean valueIsContainer = valueIsContainer(node, sourceObject, targetObject);
        //获取值的转换函数
        return getDiffLogContent(diffLogFieldAnnotation, node, sourceObject, targetObject, valueIsContainer);
    }

    private String getFieldLogName(DiffNode node, DiffLogField diffLogFieldAnnotation) {
        String filedLogName = diffLogFieldAnnotation.name();
        if (node.getParentNode() != null) {
            //获取对象的定语：比如：创建人的ID
            filedLogName = getParentFieldName(node) + filedLogName;
        }
        return filedLogName;
    }

    private boolean valueIsContainer(DiffNode node, Object sourceObject, Object targetObject) {
        if (sourceObject != null) {
            Object sourceValue = node.canonicalGet(sourceObject);
            if (sourceValue == null) {
                if (targetObject != null) {
                    return node.canonicalGet(targetObject) instanceof Collection || node.canonicalGet(targetObject).getClass().isArray();
                }
            } else {
                return sourceValue instanceof Collection || sourceValue.getClass().isArray();
            }
        }
        return false;
    }

    private String getParentFieldName(DiffNode node) {
        DiffNode parent = node.getParentNode();
        String fieldNamePrefix = "";
        while (parent != null) {
            DiffLogField diffLogFieldAnnotation = parent.getFieldAnnotation(DiffLogField.class);
            if (diffLogFieldAnnotation == null) {
                //父节点没有配置名称，不拼接
                parent = parent.getParentNode();
                continue;
            }
            fieldNamePrefix = diffLogFieldAnnotation.name().concat(logRecordProperties.getOfWord()).concat(fieldNamePrefix);
            parent = parent.getParentNode();
        }
        return fieldNamePrefix;
    }

    public String getDiffLogContent(DiffLogField diffLogField,
                                    DiffNode node,
                                    Object sourceObject,
                                    Object targetObject,
                                    boolean valueIsCollection) {

        String filedLogName = getFieldLogName(node, diffLogField);
        if (StringUtils.isEmpty(filedLogName)) {
            return StringUtils.EMPTY;
        }
        String functionName = diffLogField.function();

        //集合走单独的diff模板
        if (valueIsCollection) {
            Collection<Object> sourceList = getListValue(node, sourceObject);
            Collection<Object> targetList = getListValue(node, targetObject);
            Collection<Object> addItemList = listSubtract(targetList, sourceList);
            Collection<Object> delItemList = listSubtract(sourceList, targetList);
            String listAddContent = listToContent(addItemList, functionName);
            String listDelContent = listToContent(delItemList, functionName);
            return logRecordProperties.formatList(filedLogName, listAddContent, listDelContent);
        }
        DiffNode.State state = node.getState();
        switch (state) {
            case ADDED:
                return logRecordProperties.formatAdd(filedLogName,
                        getFiledValue(getFieldValue(node, targetObject), functionName));
            case CHANGED:
                return logRecordProperties.formatUpdate(filedLogName,
                        getFiledValue(getFieldValue(node, sourceObject), functionName),
                        getFiledValue(getFieldValue(node, targetObject), functionName));
            case REMOVED:
                return logRecordProperties.formatDeleted(filedLogName,
                        getFiledValue(getFieldValue(node, sourceObject), functionName));
            default:
                log.warn("diff log not support");
                return StringUtils.EMPTY;

        }
    }

    private Collection<Object> getListValue(DiffNode node, Object object) {
        Object fieldSourceValue = getFieldValue(node, object);
        if (fieldSourceValue != null && fieldSourceValue.getClass().isArray()) {
            return new ArrayList<>(Arrays.asList((Object[]) fieldSourceValue));
        }
        return fieldSourceValue == null ? Lists.newArrayList() : (Collection<Object>) fieldSourceValue;
    }

    private Collection<Object> listSubtract(Collection<Object> minuend, Collection<Object> subTractor) {
        Collection<Object> addItemList = new ArrayList<>(minuend);
        addItemList.removeAll(subTractor);
        return addItemList;
    }

    private String listToContent(Collection<Object> addItemList, String functionName) {
        StringBuilder listAddContent = new StringBuilder();
        if (!CollectionUtils.isEmpty(addItemList)) {
            for (Object item : addItemList) {
                listAddContent.append(getFiledValue(item, functionName)).append(logRecordProperties.getListItemSeparator());
            }
        }
        return listAddContent.toString().replaceAll(logRecordProperties.getListItemSeparator() + "$", "");
    }

    private String getFiledValue(Object canonicalGet, String functionName) {
        if (StringUtils.isEmpty(functionName)) {
            return canonicalGet.toString();
        }
        return convertFunctionService.convert(functionName, canonicalGet.toString());
    }

    private Object getFieldValue(DiffNode node, Object o2) {
        return node.canonicalGet(o2);
    }


}
