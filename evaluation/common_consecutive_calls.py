working_dir = 'consecutive_calls'
cache_dir = 'cache'

averaging_factor = 1
only_busywait_when_shuffling = True
busywait_factor = 20
intervals = [5, 50]#, 100, 300, 500]

def genMethod(ident: int):
    return 'm{0}'.format(ident)
