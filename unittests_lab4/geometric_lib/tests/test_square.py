import unittest
from geometric_lib import square

class SquareTestCase(unittest.TestCase):

    def test_positive_perimeter_integers(self):
        res = square.perimeter(4)
        self.assertEqual(res, 16)

    def test_positive_perimeter_float(self):
        res = square.perimeter(5.5)
        self.assertEqual(res, 22)

    def test_positive_perimeter_zero(self):
        res = square.perimeter(0)
        self.assertEqual(res, 0)

    def test_positive_perimeter_big_numbers(self):
        res = square.perimeter(18446744073709551616)
        self.assertEqual(res, 73786976294838206464)

    def test_negative_perimeter_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert square.perimeter(-5) in (None, False)

    def test_negative_perimeter_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert square.perimeter("7") in (None, False)

    def test_positive_area_integers(self):
        res = square.area(4)
        self.assertEqual(res, 16)

    def test_positive_area_float(self):
        res = round(square.area(6.7), 2)
        self.assertEqual(res, 44.89)

    def test_positive_area_zero(self):
        res = square.area(0)
        self.assertEqual(res, 0)

    def test_positive_area_big_numbers(self):
        res = square.area(18446744073709551616)
        self.assertEqual(res, 340282366920938463463374607431768211456)

    def test_negative_area_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert square.area(-5) in (None, False)

    def test_negative_area_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert square.area("7") in (None, False)