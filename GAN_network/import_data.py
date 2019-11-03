import os 
import os.path
import tensorflow as tf
import numpy as np



class DataProvider:

    def __init__(self, path, train_ratio = 0.7):
        self.path = path
        self.data = None
        self.train_ratio = train_ratio
        self.test_ratio = 1 - train_ratio
        self.shuffled = False
        self.train = None
        self.test = None

    def load_data(self, size = 49152):
        names = [name for name in os.listdir(self.path)]
        nr_files = len(names)
        
        # nr_files = 10 # mock

        arr = np.empty((0, size), np.float32)

        for i in range(nr_files):
            res = self.load_content(self.path + names[i])
            arr = np.row_stack((arr, res))

        print(arr.shape)
        self.data = arr
        return arr

    def train_set(self, shuffle = True):
        if self.train is not None:
            return self.train
        if self.data is None:
            return None
        if shuffle and not self.shuffled:
            np.random.shuffle(self.data)
            self.shuffled = True
        from sklearn.model_selection import train_test_split
        self.train, self.test, _, _ = train_test_split(self.data, '1'*(len(self.data)), test_size = self.test_ratio, random_state = 0)
        return self.test
    
    def test_set(self, shuffle = True):
        if self.test is not None:
            return self.test
        if self.data is None:
            return None
        self.train_set(self, shuffle)
        return self.test_set

    def load_content(self, filepath, size = [128,128], norm = 255.0):
        file_content = tf.read_file(filepath)
        data = tf.image.decode_png(
            file_content,
            channels=3,
            dtype=tf.uint8,
            name=None
        )
        # it doesnt get the proper shape, so its set manually
        data_resized = tf.image.resize_images(data, size)
        # 1, 49152 (128x128x3)
        data_2d = tf.reshape(data_resized, [1, -1])
        data_2d_normed = tf.cast(data_2d, tf.float32) / norm
        with tf.Session() as sess:
            sess.run(file_content)
            sess.run(data)
            sess.run(data_resized)
            sess.run(data_2d)
            return sess.run(data_2d_normed)
    
    def clear(self):
        self.data = None
        self.shuffled = False
        self.train = None
        self.test = None

# load_data(path_to_data)

if __name__ == "__main__":
    from data_info import path_to_data
    dp = DataProvider(path_to_data)
    dp.load_data()
    r = dp.train_set()
    print(r)