#!/usr/bin/env python
from ants import *

class ErrorBot:
    def __init__(self):
        self.counter = 0
        self.limit = 30
    def do_turn(self, ants):
        if self.counter == self.limit:
            raise Exception('ErrorBot produces error now')
        self.counter = self.counter + 1

if __name__ == '__main__':
    try:
        import psyco
        psyco.full()
    except ImportError:
        pass
    try:
        Ants.run(ErrorBot())
    except KeyboardInterrupt:
        print('ctrl-c, leaving ...')
