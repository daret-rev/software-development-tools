# Документация проекта `geometric_lib`

---

Библиотека `geometric_lib` предоставляет функции для вычисления площади и периметра простых геометрических фигур: круга, прямоугольника, квадрата и треугольника. Используется только стандартный модуль `math`, внешние зависимости отсутствуют.

Если явно не указано иное, аргументы функций — числа типа `int` или `float`. Значения, описывающие размеры(длины), должны быть неотрицательными.

## Быстрый старт

```text
git clone https://github.com/daret-rev/geometric_lib
```

## Использование

```python
from geometric_lib import circle, square

print(circle.area(2))       # 12.566370614359172
print(square.perimeter(5))  # 20
```

Примеры из docstring можно проверить командой:

```bash
python -m doctest -v circle.py rectangle.py square.py triangle.py
```

## Функции

Подробное описание каждого модуля находится в отдельном файле.

### [Круг](circle.md)

| Функция                                            | Описание                     |
|----------------------------------------------------|------------------------------|
| [`circle.area(r)`](circle.md#circlearea)           | Площадь круга радиуса `r`    |
| [`circle.perimeter(r)`](circle.md#circleperimeter) | Длина окружности радиуса `r` |

### [Прямоугольник](rectangle.md)

| Функция                                                        | Описание                                       |
|----------------------------------------------------------------|------------------------------------------------|
| [`rectangle.area(a, b)`](rectangle.md#rectanglearea)           | Площадь прямоугольника со сторонами `a` и `b`  |
| [`rectangle.perimeter(a, b)`](rectangle.md#rectangleperimeter) | Периметр прямоугольника со сторонами `a` и `b` |

### [Квадрат](square.md)

| Функция                                            | Описание                          |
|----------------------------------------------------|-----------------------------------|
| [`square.area(a)`](square.md#squarearea)           | Площадь квадрата со стороной *a*  |
| [`square.perimeter(a)`](square.md#squareperimeter) | Периметр квадрата со стороной *a* |

### [Треугольник](triangle.md)

| Функция                                                        | Описание                                          |
|----------------------------------------------------------------|---------------------------------------------------|
| [`triangle.area(a, h)`](triangle.md#trianglearea)              | Площадь треугольника с основанием `a` и высотой `h` |
| [`triangle.perimeter(a, b, c)`](triangle.md#triangleperimeter) | Периметр треугольника со сторонами `a`, `b` и `c`  |

## История изменения проекта

- Создана папка `docs` и добавлена документация проекта.

- В код добавлено описание логики и сигнатуры функций
```text
commit 8513ef5d855fd5a1fcbcf4066c27251c9150d120
Author: daret <a20atam.com@gmail.com>
Date:   Sat Oct 3 22:15:58 2026 +0300

    В код добавлено описание логики и сигнатур функций.
```

- Добавлен новый модуль для вычислений треугольника - triangle
  Исправлен баг с вычислениями в rectangle
```text
commit 8a2f19693d1e96e4938ec56b8b879208a1a26351
Author: daret <a20atam.com@gmail.com>
Date:   Sat Oct 3 22:12:03 2026 +0300

    Добавлен модуль triangle. Исправлена ошибка в rectangle.perimeter
```

- Добавлен новый модуль для вычислений прямоугольника - rectangle
```
commit 99c3ce1f46c5cd38c9c879c0382dbfaa28f0e623
Author: daret <a20atam.com@gmail.com>
Date:   Sat Oct 3 22:11:02 2026 +0300

    Добавлен модуль rectangle
```

- Создан проект геометрической библиотеки
```
commit a94626918916328c3c1185ad1ee7f05638b3ed41
Author: daret <a20atam.com@gmail.com>
Date:   Sat Oct 3 22:09:53 2026 +0300

    Initial geometric_lib
```