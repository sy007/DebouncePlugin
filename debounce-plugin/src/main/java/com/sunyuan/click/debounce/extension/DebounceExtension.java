package com.sunyuan.click.debounce.extension;

import com.sunyuan.click.debounce.entity.MethodEntity;
import com.sunyuan.click.debounce.utils.CollectionUtil;
import com.sunyuan.click.debounce.utils.ConfigUtil;
import com.sunyuan.click.debounce.utils.LogUtil;
import com.sunyuan.click.debounce.utils.StringUtil;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import groovy.json.JsonBuilder;


/**
 * author : Sy007
 * date   : 2020/12/1
 * desc   : 提供给外部配置，用于筛选指定路径下的代码插桩以及需要Hook的方法
 * version: 1.0
 */
public class DebounceExtension {
    private static final String methodEntityEx = "In %s,the %s of %s cannot be empty.";
    private final NamedDomainObjectContainer<MethodEntity> methodEntities;
    public List<String> excludes;
    public boolean isDebug = false;
    public long debounceCheckTime = 1000L;
    private static final String GLOB_SYNTAX = "glob:";
    private final List<PathMatcher> excludedPathMatcher;

    public DebounceExtension(Project project) {
        methodEntities = project.container(MethodEntity.class);
        excludedPathMatcher = new ArrayList<>();
    }

    public void methodEntities(Action<? super NamedDomainObjectContainer<MethodEntity>> action) {
        action.execute(methodEntities);
    }

    public NamedDomainObjectContainer<MethodEntity> getMethodEntities() {
        return methodEntities;
    }


    public void init() {
        SortedMap<String, MethodEntity> methodEntitiesAsMap = methodEntities.getAsMap();
        methodEntitiesAsMap.forEach((identification, methodEntity) -> {
            if (StringUtil.isEmpty(methodEntity.getMethodName())) {
                throw new IllegalArgumentException(String.format(methodEntityEx, "methodEntities", "methodName", identification));
            }
            if (StringUtil.isEmpty(methodEntity.getMethodDesc())) {
                throw new IllegalArgumentException(String.format(methodEntityEx, "methodEntities", "methodDesc", identification));
            }
            if (StringUtil.isEmpty(methodEntity.getInterfaceName())) {
                throw new IllegalArgumentException(String.format(methodEntityEx, "methodEntities", "interfaceName", identification));
            }
            ConfigUtil.sConfigHookMethods.put(methodEntity.getMethodName() + methodEntity.getMethodDesc(), methodEntity);
        });
        ConfigUtil.sConfigHookMethods.forEach((methodName, methodEntity) -> {
            String interfaceName = methodEntity.getInterfaceName();
            ConfigUtil.sInterfaceSet.add(interfaceName);
        });
        toPathMatchers(excludes, excludedPathMatcher);
    }


    private void toPathMatchers(List<String> relativePaths, List<PathMatcher> matchers) {
        if (CollectionUtil.isEmpty(relativePaths)) {
            return;
        }
        for (String relativePath : relativePaths) {
            try {
                final FileSystem fs = FileSystems.getDefault();
                final PathMatcher matcher = fs.getPathMatcher(GLOB_SYNTAX + relativePath);
                matchers.add(matcher);
            } catch (IllegalArgumentException e) {
                LogUtil.warning(String.format("Ignoring relativePath '{%s}' glob pattern.Because something unusual happened here '{%s}'", relativePath, e));
            }
        }
    }


    private boolean matches(final Path path, final List<PathMatcher> matchers) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }


    public boolean match(String relativePath) {
        if (excludedPathMatcher.isEmpty()) {
            return true;
        }
        Path path = FileSystems.getDefault().getPath(relativePath);
        boolean excludeMatch = matches(path, excludedPathMatcher);
        return !excludeMatch;
    }

    public void printlnConfigInfo() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("isDebug", isDebug);
        configMap.put("debounceCheckTime", debounceCheckTime);
        configMap.put("excludes", excludes);
        configMap.put("methodEntities", methodEntities.getAsMap());
        String configJson = new JsonBuilder(configMap).toPrettyString();
        String head = "                                      Debounce  configInfo                       ";
        LogUtil.warning(head, configJson);
    }
}
