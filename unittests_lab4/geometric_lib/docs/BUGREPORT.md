# Отчет о дефектах (багах)

---

## BUG №1

- **Модули** - `circle.py`, `rectangle.py`, `square.py`, `triangle.py`
- **Серьёзность** - критическая
- **Приоритет** - высокий

### Краткое описание

Методы не валидируют данные и не проверяют их на отрицательность. Для расчетов периметра и объёма отрицательные меры являются недопустимыми и методы должны это контролировать, например, провоцировать ошибку.

#### Тесты для ознакомления

- [Circle perimeter](../tests/test_circle.py#L23) ( test_negative_perimeter_negative_numbers )
- [Circle area](../tests/test_circle.py#L59) ( test_negative_area_negative_numbers )
- [Rectangle perimeter](../tests/test_rectangle.py#L22) ( test_negative_perimeter_negative_numbers )
- [Rectangle area](../tests/test_rectangle.py#L58) ( test_negative_area_negative_numbers )
- [Square perimeter](../tests/test_square.py#L22) ( test_negative_perimeter_negative_numbers )
- [Square area](../tests/test_square.py#L57) ( test_negative_area_negative_numbers )
- [Triangle perimeter](../tests/test_triangle.py#L22) ( test_negative_perimeter_negative_numbers )
- [Triangle area](../tests/test_triangle.py#L57) ( test_negative_area_negative_numbers )

---

## BUG №2

- **Модули** - `rectangle.py`, `square.py`, `triangle.py`
- **Серьёзность** - критическая
- **Приоритет** - высокий

### Краткое описание

Методы не валидируют данные и пропускает ввод строки. Методы должны либо провоцировать ошибку, либо пытаться распарсить строки, если они содержат числа.

#### Тесты для ознакомления

- [Rectangle perimeter](../tests/test_rectangle.py#L32) ( test_negative_perimeter_string )
- [Square perimeter](../tests/test_square.py#L32) ( test_negative_perimeter_string )
- [Triangle perimeter](../tests/test_triangle.py#L32) ( test_negative_perimeter_string )

---

## BUG №3

- **Модули** - `circle.py`
- **Серьёзность** - Низкая
- **Приоритет** - Низкий

### Краткое описание

Потеря точности при вычислении периметра и площади круга с большими значениями радиуса, связана с float.

#### Тесты для ознакомления

- [Circle perimeter](../tests/test_circle.py#L19) ( test_positive_perimeter_big_numbers )
- [Circle area](../tests/test_circle.py#L55) ( test_positive_area_big_numbers )