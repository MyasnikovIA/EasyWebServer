package ru.miacomsoft.EasyWebServer;

import ru.miacomsoft.EasyWebServer.util.callbackType.CallbackPage;
import ru.miacomsoft.EasyWebServer.util.onPage;
import ru.miacomsoft.EasyWebServer.util.onTerminal;
import ru.miacomsoft.EasyWebServer.util.queryType.*;
import ru.miacomsoft.EasyWebServer.util.structObject.JavaInnerClassObject;
import ru.miacomsoft.EasyWebServer.util.structObject.JavaTerminalClassObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerResource {
    public static Map<String, JavaInnerClassObject> pagesJavaInnerClass = new ConcurrentHashMap<>(10, (float) 0.5);
    public static Map<String, JavaTerminalClassObject> pagesJavaTerminalClass = new ConcurrentHashMap<>(10, (float) 0.5);
    public static Map<String, StringBuffer> pagesListContent = new ConcurrentHashMap<>(10, (float) 0.5);
    public static Map<String, CallbackPage> pagesList = new ConcurrentHashMap<>(10, (float) 0.5);
    public static Map<String, File> pagesListFile = new ConcurrentHashMap<>(10, (float) 0.5);
    public static Map<String, Class> componentListClass = new ConcurrentHashMap<>(10, (float) 0.5);
    public static List<Class<?>> classes = new ArrayList<>();

    ///Массив с интерфейсами аннотаций
    public static final Class<?>[] ANNOTATION_TYPES = {
            onPage.class,
            Get.class,
            Put.class,
            Post.class,
            Delete.class,
            onTerminal.class,
            Head.class,
            Patch.class,
            Options.class,
            Trace.class
    };
}