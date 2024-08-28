#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

import unittest

import switch

path_to_file = ""


class Test(unittest.TestCase):

    def test_status_10(self):
        s = switch.Switch(10)
        self.assertTrue(s.__call__(10))

    def test_status_10_fail(self):
        s = switch.Switch(10)
        self.assertFalse(s.__call__(20))

    def test_init(self):
        s = switch.Switch(1)
        s.__init__(30)
        self.assertTrue(s.__call__(30))
        self.assertFalse(s.__call__(1))

    def test_enter(self):
        s = switch.Switch(1)
        s1 = s.__enter__()
        self.assertEqual(s, s1)

    def test_exit(self):
        s = switch.Switch(1)
        self.assertFalse(s.__exit__(TypeError, -1, {}))
