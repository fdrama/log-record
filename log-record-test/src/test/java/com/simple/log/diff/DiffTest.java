package com.simple.log.diff;


import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.path.NodePath;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 */
@Slf4j
public class DiffTest {


    @Test
    public void test1() {

        Map<String, String> working = Collections.singletonMap("item", "foo");
        Map<String, String> base = Collections.singletonMap("item", "bar");
        DiffNode diff = ObjectDifferBuilder.buildDefault().compare(working, base);
        assert diff.hasChanges();
        assert diff.childCount() == 1;
        NodePath itemPath = NodePath.startBuilding().mapKey("item").build();
        assert diff.getChild(itemPath).getState() == DiffNode.State.CHANGED;
        diff.visit((node, visit) -> System.out.println(node.getPath() + " => " + node.getState()));

        diff.visit((node, visit) -> {
            final Object baseValue = node.canonicalGet(base);
            final Object workingValue = node.canonicalGet(working);
            final String message = node.getPath() + " changed from " + baseValue + " to " + workingValue;
            System.out.println(message);
        });
    }
}
