![Потча Роисси](https://user-images.githubusercontent.com/23572628/63599249-df7b2a00-c5c9-11e9-9b41-a0f46db47213.png)

# pochtaizv - Генератор извещений (форма 22) Почты России

## Введение
pochtaizv генерирует извещения в формате OpenDocument Graphic (.odg), такие файлы открываются в LibreOffice, Apache OpenOffice. 
По состоянию на 20 августа 2019 года шаблон, используемый в этом генераторе, максимально соответствует шаблону, который возвращает сервер Почты России. 

В программе __намеренно__ не используется публичный API Почты России, чтобы избежать привязки кодов отслеживания к определённому аккаунту.
Приблизительно с мая 2019 года при попытке заполнить извещение онлайн сайт Почты России принуждает зарегистрироваться, подтвердить телефон и электронную почту - это стало одной из причин написания данной программы.  

## Достоинства

* __Приватность__ - не надо регистрироваться на сайте Почты России, привязывать к аккаунту номер телефона и электронную почту. Нет привязки к API-ключу. Никакой "упрощёнки".
* __Автоматизация__ - pochtaizv хорошо интегрируется в скрипты, позволяет генерировать извещения, не выходя из консоли. Большинство полей бланка можно указать как параметры при запуске программы, а можно оставить полуавтоматический режим, в котором пользователь будет самостоятельно вводить недостающие данные.
* __Универсальность__ - работает везде, где установлена JRE 1.8 и выше.  

## Скачать

Загрузить исполняемые файлы можно в разделе [Releases](https://github.com/kerastinell/pochtaizv/releases)

## Запуск

* Windows (.exe): `pochtaizv [ПАРАМЕТР...]` в командной строке. Двойной клик по .exe также откроет окно командной строки.
* Linux (если установлен `jarwrapper`) (.jar): `pochtaizv.jar [ПАРАМЕТР]...`
* Универсальный метод, любая ОС (.jar): `java -jar pochtaizv.jar [ПАРАМЕТР]...`  

## Вариант использования (Windows)

1. Создать .bat файл, заранее указать все данные получателя в параметрах для pochtaizv, например:
```pochtaizv.exe --name="Обоев Рулон" --address="123456, г. Шестигорск" --id-type="паспорт" --id-series="1234" --id-number="567890" --id-issued-by="2-ое отделение МВД г. Девятипалатинск" --id-issue-date="2001-01-01" --registered-at=""```
2. При запуске .bat-файла останется лишь ввести код(ы) отслеживания 

## Разработка

Шаблон находится по пути `data/template.odg`. После его изменения необходимо выполнить задачу gradle: `refreshResources`.

В проекте используется Gradle 5.5.1

Для сборки программы, готовой к распространению, следует выполнить задачу gradle `release`. Готовые файлы появятся в директории `build/releases/версияПрограммы`.
Для создания .exe файла используется плагин Launch4J для gradle. 

Лично мне не удалось найти (значится в TODO) образцы извещений, содержащих плату за досыл. Парсинг платы за возврат отправления тоже желательно пересмотреть. 

## Лицензия
[MIT](https://github.com/kerastinell/pochtaizv/blob/master/LICENSE). Эта лицензия — разрешительная, без копилефта. Она разрешает использование и изменение кода практически любым образом, при условии, что текст самой лицензии и указание авторства никуда не исчезнут, даже если вы разобьете изначальный проект на части.