import unittest
from geometric_lib import triangle

class TriangleTestCase(unittest.TestCase):

    def test_positive_perimeter_integers(self):
        res = triangle.perimeter(5, 4, 3)
        self.assertEqual(res, 12)

    def test_positive_perimeter_float(self):
        res = round(triangle.perimeter(5.4, 4.3, 3.2), 1)
        self.assertEqual(res, 12.9)

    def test_positive_perimeter_zero(self):
        res = triangle.perimeter(0, 0, 0)
        self.assertEqual(res, 0)

    def test_positive_perimeter_big_numbers(self):
        res = triangle.perimeter(18446744073709551616, 18446744073709551616, 18446744073709551616)
        self.assertEqual(res, 55340232221128654848)

    def test_negative_perimeter_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert triangle.perimeter(-5, -7, -9) in (None, False)


    def test_negative_perimeter_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert triangle.perimeter("7", "9", "4") in (None, False)

    def test_positive_area_integers(self):
        res = round(triangle.area(5, 7), 1)
        self.assertEqual(res, 17.5)

    def test_positive_area_float(self):
        res = round(triangle.area(9.8, 7.4), 2)
        self.assertEqual(res, 36.26)

    def test_positive_area_zero(self):
        res = triangle.area(0, 0)
        self.assertEqual(res, 0)

    def test_positive_area_big_numbers(self):
        res = triangle.area(18446744073709551616, 18446744073709551616)
        self.assertEqual(res, 170141183460469231731687303715884105728)

    def test_negative_area_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert triangle.area(-5, -4) in (None, False)

    def test_negative_area_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert triangle.area("7", "34") in (None, False)
