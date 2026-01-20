package ru.miacomsoft.EasyWebServer;

import ru.miacomsoft.EasyWebServer.util.callbackType.CallbackPage;
import ru.miacomsoft.EasyWebServer.util.callbackType.CallbackProcedure;
import ru.miacomsoft.EasyWebServer.util.structObject.JavaInnerClassObject;
import ru.miacomsoft.EasyWebServer.util.structObject.JavaTerminalClassObject;

import java.io.File;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.miacomsoft.EasyWebServer.PacketManager.getWebPage;
import static ru.miacomsoft.EasyWebServer.PacketManager.getPageJar;

public class WebServer implements Runnable {
    public static CallbackProcedure callbackProcedure = null;
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());
    private static boolean isRunServer = false;
    private static WebServer server;

    /**
     *
     */
    public WebServer() {
    }

    /**
     * @param mainClass
     */
    public WebServer(Class<?> mainClass) {
        System.out.println("Список классов страниц: " + getWebPage(mainClass));
    }

    /**
     *
     */
    public static void start() {
        server = new WebServer();
        Thread thread = new Thread(server);
        thread.start();
        Runtime.getRuntime().addShutdownHook(new ShutDown());
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public static void stop() {
        isRunServer = false;
    }

    /**
     *
     */
    static void shutDown() {
        try {
            LOGGER.info("Shutting down server...");
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (server) {
            server.notifyAll();
        }
    }

    /**
     * @param path
     * @param contentText
     */
    public void onPage(String path, StringBuffer contentText) {
        ServerResource.pagesListContent.put(path, contentText);
    }

    /**
     * @param path
     * @param contentText
     * @param mime
     */
    public void onPage(String path, StringBuffer contentText, String mime) {
        ServerResource.pagesListContent.put(path, contentText);
    }

    /**
     * @param callbackProcedure
     */
    public void onPage(CallbackProcedure callbackProcedure) {
        this.callbackProcedure = callbackProcedure;
    }

    /**
     * прописывание контента в Java коде
     *
     * @param path         - путь к вызываемому содержимому
     * @param callbackPage - JAVA код страницы
     */
    public void onPage(String path, CallbackPage callbackPage) {
        ServerResource.pagesList.put(path, callbackPage);
    }

    /**
     * @param path
     * @param file
     */
    public void onPage(String path, File file) {
        ServerResource.pagesListFile.put(path, file);
    }

    /**
     * @param args
     */
    public void initConfig(String args) {
        if (args.length() == 0) {
            ServerConstant.config = new ServerConstant("config.ini");
        } else {
            ServerConstant.config = new ServerConstant(args);
        }
    }

    /**
     * @param confPropName
     * @param confPropValue
     * @return
     */
    public Boolean config(String confPropName, String confPropValue) {
        return ServerConstant.config.setProp(confPropName, confPropValue);
    }

    /**
     * @param confPropName
     * @param confPropValue
     * @return
     */
    public Boolean config(String confPropName, Boolean confPropValue) {
        return ServerConstant.config.setProp(confPropName, confPropValue);
    }

    /**
     * Подключить к серверу сторонние Jar файлы ВЭБ страниц
     *
     * @param pathJarFile
     */
    public void addPageJar(String pathJarFile) {
        System.out.println("Список страниц из Jar файла " + getPageJar(pathJarFile));
    }

    @Override
    public void run() {
        int port = Integer.parseInt(ServerConstant.config.DEFAULT_PORT);
        try {
            isRunServer = true;
            viewWebResource(port);
            ServerSocket ss = new ServerSocket(port);
            while (isRunServer == true) {
                // ждем новое подключение Socket клиента
                Socket socket = ss.accept();
                // Запускаем обработку нового соединение в паралельном потоке и ждем следующее соединение
                new Thread(new ServerResourceHandler(socket)).start();
            }
        } catch (Exception ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Throwable ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Показать в консоли список доступных ресурсов через браузер
     *
     * @param port
     */
    private void viewWebResource(int port) {
        System.out.print("port: ");
        System.out.println(port);
        System.out.print("http://127.0.0.1:");
        System.out.print(port);
        System.out.println("/");
        System.out.println("-----------------------");
        String portStr = String.valueOf(port);
        for (Map.Entry<String, JavaInnerClassObject> entry : ServerResource.pagesJavaInnerClass.entrySet()) {
            String key = entry.getKey();
            JavaInnerClassObject page = entry.getValue();
            System.out.print("queryType: ");
            System.out.println(page.queryType);
            System.out.print("mime: ");
            System.out.println(page.mime);
            System.out.println(page.classNat);
            System.out.print("method: ");
            System.out.println(page.method);
            System.out.print("http://127.0.0.1:");
            System.out.print(portStr);
            System.out.println("/" + page.url);
            System.out.println("-----------------------");
        }
        if (!ServerResource.pagesJavaTerminalClass.isEmpty()) {
            System.out.println("-----------------------");
            System.out.println("------- Terminal ------");
            System.out.println("-----------------------");
            for (Map.Entry<String, JavaTerminalClassObject> entry : ServerResource.pagesJavaTerminalClass.entrySet()) {
                String key = entry.getKey();
                JavaTerminalClassObject page = entry.getValue();
                System.out.print("queryType: ");
                System.out.println(page.classNat);
                System.out.print("method: ");
                System.out.println(page.method);
                System.out.print("http://127.0.0.1:");
                System.out.print(portStr);
                System.out.println("/" + page.url);
                System.out.println("-----------------------");
            }
        }

    }

    /**
     * @param stringMessage
     */
    protected synchronized static void WriteToFile(String stringMessage) {
        if (ServerConstant.config.LOG_FILE.length() == 0) {
            System.err.println(stringMessage);
        } else {
            try {
                FileWriter filelog = new FileWriter(new File(ServerConstant.config.LOG_FILE), true);
                filelog.write(stringMessage);
                filelog.flush();
            } catch (Exception error) {
                System.err.println(error);
            }
        }
    }
    /**
     * Определяет тип операционной системы
     * Возвращает: "windows", "linux", "mac", "solaris", "bsd", "aix", или "unknown"
     */
    public static String getOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "linux";
        } else if (osName.contains("mac")) {
            return "mac";
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            return "solaris";
        } else if (osName.contains("bsd")) {
            return "bsd";
        } else {
            return "unknown";
        }
    }

    /**
     * Проверяет, является ли ОС Windows
     */
    public static boolean isWindows() {
        return getOS().equals("windows");
    }

    /**
     * Проверяет, является ли ОС Linux
     */
    public static boolean isLinux() {
        return getOS().equals("linux");
    }

    /**
     * Проверяет, является ли ОС Unix-подобной (Linux, Mac, BSD, Solaris)
     */
    public static boolean isUnixLike() {
        String os = getOS();
        return os.equals("linux") || os.equals("mac") ||
                os.equals("solaris") || os.equals("bsd") ||
                os.equals("aix");
    }
}


