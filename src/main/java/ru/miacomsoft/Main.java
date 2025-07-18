package ru.miacomsoft;

import ru.miacomsoft.EasyWebServer.WebServer;

import java.io.File;
import java.net.URL;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        WebServer web = new WebServer(Main.class);
        web.config("DATABASE_NAME" , "jdbc:postgresql://192.168.15.82:5432/Panorama360");
        web.config("DATABASE_USER_NAME" , "**********");
        web.config("DATABASE_USER_PASS" , "**********");
        web.config("LOGIN_PAGE" , "login.html"); //  Страница авторизации в БД переход приисходит если пользователь расконектился
        web.config("PAGE_404" , "page_404.html"); //  Страница 404 отсутствие содержимого
        web.config("INDEX_PAGE" , "test.html");   // Путь стартовой страницы по умолчанию
        web.config("DEBUG" , "true"); // включение режима отладки (страница будет пересобираться при каждом обращении)
        web.config("CAHEBLE" , "true"); // Кэширование страниц (загрузка  страниц в оперативную память)
        web.config("LENGTH_CAHE" , "test");  //Размер (байт) файла после которого отключается режим кэширования (если файл больше этого размера, тогда файл читается напрямую с жесткого диска)
        web.config("GZIPPABLE" , "false"); //  Сжатие статической страницы
        web.config("WEBAPP_DIR" , "www"); //   путь к статичным ресурсам сервера
        web.config("DEFAULT_HOST" , "0.0.0.0");
        web.config("DEFAULT_PORT" , "9093"); //  порт на котором будет работать сервер
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