import os 
import os.path
import tensorflow as tf

from data_info import path_to_data


# get number of files .png
print(path_to_data)
names = [name for name in os.listdir(path_to_data)]
nr_files = len(names)
print(nr_files)

test_file = names[0]
data = tf.image.decode_png(
    test_file,
    channels=3,
    dtype=tf.uint8,
    name=None
)
print(data)
print(data.shape)

# it doesnt get the proper shape, so its set manually
data_resized = tf.image.resize_images(data, [128,128])
print(data_resized)

# 1, 49152
data_2d = tf.reshape(data_resized, [1, -1])
print(data_2d)
data_2d /= 255 #max rgb val

# load data
'''
tf.io.decode_png(
    contents,
    channels=3,
    dtype=tf.dtypes.uint8,
    name=None
)
'''

# get number of loaded data and check if it is ok

