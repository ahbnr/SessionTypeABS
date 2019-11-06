working_dir = 'models/complex/consecutive_calls'
cache_dir = 'cache'

averaging_factor = 10
intervals = [1, 2, 3, 4, 5, 10, 30, 50, 70, 80, 100, 300, 500, 700, 900]

def genMethod(ident: int):
    return 'm{0}'.format(ident)
