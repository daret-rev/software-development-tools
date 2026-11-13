import unittest

from geometric_lib import circle

class CircleTestCase(unittest.TestCase):

    def test_positive_perimeter_integers(self):
        res = round(circle.perimeter(6), 5)
        self.assertEqual(res, 37.69911)

    def test_positive_perimeter_float(self):
        res = round(circle.perimeter(5.7), 5)
        self.assertEqual(res, 35.81416)

    def test_positive_perimeter_zero(self):
        res = circle.perimeter(0)
        self.assertEqual(res, 0)

    def test_positive_perimeter_big_numbers(self):
        res = circle.perimeter(18446744073709551616)
        self.assertEqual(res, 1.15904 *10**20)

    def test_negative_perimeter_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert circle.perimeter(-1) in (None, False)

    def test_negative_perimeter_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert circle.perimeter("5") in (None, False)

    def test_positive_area_integers(self):
        res = round(circle.area(7), 3)
        self.assertEqual(res, 153.938)

    def test_positive_area_float(self):
        res = round(circle.area(67.5), 4)
        self.assertEqual(res, 14313.8815)

    def test_positive_area_zero(self):
        res = circle.area(0)
        self.assertEqual(res, 0)

    def test_positive_area_big_numbers(self):
        res = circle.area(18446744073709551616)
        self.assertEqual(res, 1.06903 * 10**39)

    def test_negative_area_negative_numbers(self):
        with self.assertRaises((ValueError, TypeError)):
            assert circle.area(-7) in (None, False)

    def test_negative_area_string(self):
        with self.assertRaises((ValueError, TypeError)):
            assert circle.area("5") in (None, False)