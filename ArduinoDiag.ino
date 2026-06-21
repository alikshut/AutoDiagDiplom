// Подключаем библиотеки для работы с I2C и LCD дисплеем
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// Создаём объект для управления LCD 20x4 с адресом 0x27 (если не работает — попробуй 0x3F)
LiquidCrystal_I2C lcd(0x27, 20, 4);

// Буфер для накопления входящих данных (собирает строку до символа \n)
String inputBuffer = "";
// Время последнего получения данных
unsigned long lastDataTime = 0;

void setup() {
  // Запускаем последовательный порт для связи с HC-05 на скорости 9600
  Serial.begin(9600);
  
  // Инициализация дисплея, включение подсветки и очистка экрана
  lcd.init();
  lcd.backlight();
  lcd.clear();
  // Выводим стартовое сообщение на первой строке
  lcd.setCursor(0, 0);
  lcd.print("System ready");
  
  // Запоминаем время запуска, чтобы отсчитывать 10 секунд бездействия
  lastDataTime = millis();
}

void loop() {
  // Если в Serial (HC-05) есть данные — читаем их по одному символу
  while (Serial.available()) {
    char c = Serial.read();      // Читаем символ
    if (c == '\n') {             // Если дошли до конца строки
      parseData(inputBuffer);    // Обрабатываем накопленную строку
      inputBuffer = "";          // Очищаем буфер для следующего сообщения
    } else {
      inputBuffer += c;          // Иначе добавляем символ в буфер
    }
  }
  
  // Если прошло больше 10 секунд с последнего получения данных
  if (millis() - lastDataTime > 10000) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("No data");        // Показываем сообщение об отсутствии данных
    lcd.setCursor(0, 1);
    lcd.print("                "); // Очищаем вторую строку
    lcd.setCursor(0, 2);
    lcd.print("                "); // Очищаем третью строку
  }
}

// Функция парсинга входящей строки формата T:95;F:8.5
void parseData(String data) {
  lastDataTime = millis(); // Сбрасываем таймер бездействия, т.к. данные пришли
  
  // Ищем позиции ключевых слов: "T:" и "F:"
  int tIndex = data.indexOf("T:");
  int fIndex = data.indexOf("F:");
  // Ищем точку с запятой, которая разделяет T и F
  int semicolon = data.indexOf(";", tIndex);
  
  // Если оба ключевых слова найдены
  if (tIndex != -1 && fIndex != -1) {
    // Извлекаем значение температуры: от T: до точки с запятой
    String tempStr = data.substring(tIndex + 2, semicolon);
    // Извлекаем значение расхода топлива: от F: до конца строки
    String fuelStr = data.substring(fIndex + 2);
    fuelStr.trim(); // Удаляем пробелы и символы в конце
    
    // Преобразуем строки в числа: температуру в целое, расход в число с запятой
    int temp = tempStr.toInt();
    float fuel = fuelStr.toFloat();
    
    // Очищаем дисплей и выводим данные на первые две строки
    lcd.clear();
    
    lcd.setCursor(0, 0);
    lcd.print("Temp: ");
    lcd.print(temp);
    lcd.print(" C");
    
    lcd.setCursor(0, 1);
    lcd.print("Fuel: ");
    lcd.print(fuel, 1); // Выводим с одним знаком после запятой
    lcd.print(" l/100km");
  }
}
