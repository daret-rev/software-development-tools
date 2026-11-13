import unittest
from geometric_lib import rectangle

class RectangleTestCase(unittest.TestCase):

    def test_positive_perimeter_integers(self):
        res = rectangle.perimeter(5, 7)
        self.assertEqual(res, 24)

    def test_positive_perimeter_float(self):
        res = rectangle.perimeter(5.7, 97.3)
        self.assertEqual(res, 206)

    def test_positive_perimeter_zero(self):
        res = rectangle.perimeter(0, 0)
        self.assertEqual(res, 0)

    def test_positive_perimeter_big_numbers(self):
        res = rectangle.perimeter(18446744073709551616, 18446744073709551616)
        self.assertEqual(res, 73786976294838206464)

    def test_negative_perimeter_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert rectangle.perimeter(-5, -5) in (None, False)

    def test_negative_perimeter_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert rectangle.perimeter("7", "9") in (None, False)


    def test_positive_area_integers(self):
        res = rectangle.area(5, 9)
        self.assertEqual(res, 45)

    def test_positive_area_float(self):
        res = round(rectangle.area(24.3, 7.7), 2)
        self.assertEqual(res, 187.11 )

    def test_positive_area_zero(self):
        res = rectangle.area(0, 9)
        self.assertEqual(res, 0)

    def test_positive_area_big_numbers(self):
        res = rectangle.area(18446744073709551616, 18446744073709551616)
        self.assertEqual(res, 340282366920938463463374607431768211456)

    def test_negative_area_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert rectangle.area(-5, -5) in (None, False)

    def test_negative_area_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert rectangle.area("7", "9") in (None, False)
