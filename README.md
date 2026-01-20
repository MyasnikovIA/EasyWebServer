# EasyWebServer
**фреймворк EasyWebServer** - легковесный веб-сервер на Java с расширенными возможностями для создания веб-приложений, REST API и систем управления устройствами.

# java -jar /data/data/com.termux/files/home/EasyWebServerGit/out/artifacts/EasyWebServer_jar/EasyWebServer.jar


## Основные возможности

### 1. **Веб-сервер с поддержкой HTTP/1.1**
- Многопоточная обработка запросов
- Поддержка методов: GET, POST, PUT, DELETE, HEAD, PATCH, OPTIONS, TRACE
- Статическая и динамическая маршрутизация
- Сессии и cookies
- Кэширование контента

### 2. **Система аннотаций для маршрутизации**
```java
@Get(url="api/data.java", ext="json")
public JSONObject getData(HttpExchange query) {
    return new JSONObject().put("status", "ok");
}

@Post(url="api/save.java", ext="json")  
public JSONObject saveData(HttpExchange query) {
    // обработка POST данных
    return new JSONObject().put("result", "saved");
}
```

### 3. **Терминальный режим для IoT устройств**
```java
@onTerminal(url="device_esp8266_terminal_message.java")
public void onPage(HttpExchange query) {
    // Длительное соединение для обмена данными с устройствами
    String deviceName = (String) query.headers.get("device_name");
    // Обмен сообщениями в реальном времени
}
```

### 4. **Динамическая компиляция Java кода**
- Выполнение Java кода из строк
- Горячая перекомпиляция при изменении файлов
- Интеграция с БД PostgreSQL

### 5. **Компонентная система для UI**
```html
<cmpDataset name="userData" query_type="sql">
    <![CDATA[
    SELECT * FROM users WHERE id = ${user_id}
    ]]>
    <var name="user_id" srctype="session" default="0"/>
</cmpDataset>

<cmpAction name="saveUser">
    <![CDATA[
    INSERT INTO users (name) VALUES (${name})
    ]]>
    <var name="name" src="user_name"/>
</cmpAction>
```

## Примеры использования

### 1. **REST API сервер**
```java
public class ApiController {
    
    @Get(url="api/users.java", ext="json")
    public JSONObject getUsers(HttpExchange query) {
        JSONArray users = query.SQL("SELECT * FROM users");
        return new JSONObject().put("users", users);
    }
    
    @Post(url="api/users.java", ext="json")
    public JSONObject createUser(HttpExchange query) {
        JSONObject data = new JSONObject(new String(query.postByte));
        // обработка данных
        return new JSONObject().put("id", newUserId);
    }
}
```

### 2. **Управление IoT устройствами**
```java
// Устройство подключается и слушает команды
// TERM /device_esp8266_terminal_message.java
// device_name: Drone_001

// Оператор отправляет команды
// TERM /device_esp8266_terminal_message_send.java  
// device_name: Operator_001
// device_name_connect: Drone_001

// Команды: up:100;dir:1;button_a:1
```

### 3. **Динамические SQL запросы**
```java
@Get(url="report.java", ext="json")
public JSONObject generateReport(HttpExchange query) {
    String sql = "SELECT date, SUM(sales) FROM sales WHERE date BETWEEN ? AND ?";
    JSONArray result = query.SQL(sql); // Автоматическое подключение к БД
    return new JSONObject().put("report", result);
}
```

### 4. **Компонентный UI**
```html
<!DOCTYPE html>
<html>
<body>
    <cmpForm name="userForm">
        <cmpInput name="userName" label="Имя пользователя"/>
        <cmpButton name="saveBtn" onclick="saveUser()">Сохранить</cmpButton>
    </cmpForm>
    
    <cmpDataset name="userList">
        SELECT id, name FROM users ORDER BY name
    </cmpDataset>
</body>
</html>
```

## Архитектура

### Основные компоненты:
- **WebServer** - главный класс сервера
- **HttpExchange** - обработчик HTTP запросов
- **ServerResourceHandler** - диспетчер ресурсов  
- **JavaStrExecut** - система динамической компиляции
- **PacketManager** - менеджер аннотаций и маршрутов
- **PostgreQuery** - работа с PostgreSQL

### Конфигурация:
```java
web.config("DATABASE_NAME", "jdbc:postgresql://host/db");
web.config("DEFAULT_PORT", "8080");
web.config("WEBAPP_DIR", "www");
web.config("DEBUG", "true");
```

## Преимущества

1. **Легковесность** - минимальные зависимости
2. **Гибкость** - аннотации + динамическая компиляция
3. **Производительность** - кэширование и многопоточность
4. **Универсальность** - веб-UI + REST API + IoT
5. **Батарейки в комплекте** - встроенная работа с БД, сессии, компоненты

Этот фреймворк идеально подходит для:
- Быстрого прототипирования веб-приложений
- Систем управления IoT устройствами
- Внутренних корпоративных систем
- REST API серверов
- Административных панелей
