package ru.miacomsoft;

import ru.miacomsoft.EasyWebServer.WebServer;

import java.io.File;
import java.net.URL;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        WebServer web = new WebServer(Main.class);
        // web.initConfig(args[0]);
        web.config("DATABASE_NAME" , "jdbc:postgresql://127.0.0.1:5432/Panorama360");
        // web.config("DATABASE_USER_NAME" , "**********");
        // web.config("DATABASE_USER_PASS" , "**********");
        web.config("LOGIN_PAGE" , "login.html"); //  Страница авторизации в БД переход приисходит если пользователь расконектился
        web.config("PAGE_404" , "page_404.html"); //  Страница 404 отсутствие содержимого
        web.config("INDEX_PAGE" , "index.html");   // Путь стартовой страницы по умолчанию
        web.config("DEBUG" , "false"); // включение режима отладки (страница будет пересобираться при каждом обращении)
        web.config("CAHEBLE" , "true"); // Кэширование страниц (загрузка  страниц в оперативную память)
        // web.config("LENGTH_CAHE" , "test");  //Размер (байт) файла после которого отключается режим кэширования (если файл больше этого размера, тогда файл читается напрямую с жесткого диска)
        web.config("GZIPPABLE" , "false"); //  Сжатие статической страницы

        String os = web.getOS();
        if (os.equals("windows")) {
            web.config("WEBAPP_DIR", "Y:\\files\\home\\EasyWebServerGit\\www;Y:\\files\\home\\storage\\downloads"); //   путь к статичным ресурсам сервера
        }
        if (os.equals("linux")) {
            //web.config("WEBAPP_DIR" , "/data/data/com.termux/files/home/EasyWebServerGit/www;/data/data/com.termux/files/home/storage/downloads"); //   путь к статичным ресурсам сервера
            web.config("WEBAPP_DIR" , "/data/data/com.termux/files/home/EasyWebServerGit/www"); //   путь к статичным ресурсам сервера
        }
        web.config("DEFAULT_HOST" , "0.0.0.0");
        web.config("DEFAULT_PORT" , "9092"); //  порт на котором будет работать сервер
        web.config("APP_NAME" , "webpage"); //  Имя приложения (функции на SQL сервер будут иметь префикс этого имени)
        web.config("LOG_FILE" , "log.txt"); // путь к файлу логирования
        try {
            URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            String path = file.getPath();
            web.config("SERVER_HOM" , path);
            System.out.println("Путь к файлу Main.class: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        web.start();

    }
}