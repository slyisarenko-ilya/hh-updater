# hh-updater
Обновление резюме на headhunt в автоматическом режиме (на локальном компьютере)

Обновляет только первое найденное резюме вашей учётной записи hh.ru. 

Происходит авторизация по правилам, описанным на dev.hh.ru - oauth и вызов функций HH API.
Выполняет авторизацию и полуавтоматическое получение токена с сервера hh.ru.
Запрашивает требуемые данные у пользователя. 
Когда все данные получены - последующие запуски hh-updater работает автоматически. 
Полученного токена хватает на несколько недель.  

Использованные технологии: Selenium + ChromeDriver + Jetty + HttpClient + OAuthClient. 

Файл с настройками создаётся в рабочей директории проекта.

### build executable jar
```
mvn clean compile assembly:single
```

ChromeDriver, Chrome binary (https://googlechromelabs.github.io/chrome-for-testing/#stable)
Запускать из терминала т.к. требуется ввод параметров.

## run example
``` 
#!/bin/sh

cd "$(dirname "$0")"
CHROME_BINARY=~/<chrome-binary-path>/chrome
java -jar resume-updater.jar -ea -Dwebdriver.chrome.driver=~/<chrome-driver-path>/chromedriver  -Dwebdriver.chrome.whitelistedIps= 
```

run: ./hh-updater.sh
