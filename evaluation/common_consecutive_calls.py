working_dir = 'consecutive_calls'
cache_dir = 'cache'

averaging_factor = 1#10
intervals = [5, 50, 100, 300, 500]#[1, 2, 3, 4, 5, 10, 30, 50, 70, 80, 100, 300, 500]#, 100, 300, 500]

def genMethod(ident: int):
    return 'm{0}'.format(ident)
