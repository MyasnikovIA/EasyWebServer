// Сохраняем разные типы данных
D3Api.setSession('number', 12345);
D3Api.setSession('string', 'hello');
D3Api.setSession('object', { name: 'John', age: 30 });
D3Api.setSession('array', [1, 2, 3]);

// Получаем данные (синхронно)
var num = D3Api.getSession('number');
console.log(num); // 12345 (не {value: 12345})

var str = D3Api.getSession('string');
console.log(str); // "hello" (не {value: "hello"})

var obj = D3Api.getSession('object');
console.log(obj); // { name: "John", age: 30 } (весь объект)

var arr = D3Api.getSession('array');
console.log(arr); // [1, 2, 3] (весь массив)

// Асинхронный режим
D3Api.getSession('number', function(err, value) {
    console.log(value); // 12345
});