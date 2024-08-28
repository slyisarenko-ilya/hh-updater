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

При первом запуске, когда токен ещё не получен, если в системе не найдётся chrome для запуска selenium, то Selenium Manager попробует скачать версию из интернета. Если что-то не получится на этом этапе, то, возможно, придётся скачать ChromeBinary внучную по адресу https://googlechromelabs.github.io/chrome-for-testing/#stable и указать переменную окружения CHROME_BINARY=<path>/chrome 

### build executable jar
```
mvn clean compile assembly:single
```

Запускать из терминала т.к. требуется ввод параметров.

## run example
``` 
#!/bin/sh

cd "$(dirname "$0")"
# WEB_DRIVER_HEADLESS=false  # по-умолчанию false. Можно указать true, если хотим видеть как открывается форма авторизации hh, например, в целях отладки
# NUMBER_OF_FETCH_TOKEN_ATTEMPTS = 5 # если токен не задан или истёк, количество попыток его получения
java -jar resume-updater.jar   -Dwebdriver.chrome.whitelistedIps= 
```

run: 
```
./hh-updater.sh
```
or:
```
# CHROME_BINARY=<path>/chrome   # возможно, понадобится скачать chrome binary для соответствующий ОС https://googlechromelabs.github.io/chrome-for-testing/#stable
while true; do ./hh-updater.sh; sleep 3600; done
``` 


